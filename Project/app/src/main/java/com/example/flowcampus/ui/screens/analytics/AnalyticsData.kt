package com.example.flowcampus.ui.screens.analytics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.flowcampus.ui.theme.AppAmber
import com.example.flowcampus.ui.theme.AppBlue
import com.example.flowcampus.ui.theme.AppGreen
import com.example.flowcampus.ui.theme.AppNavy
import com.example.flowcampus.ui.theme.AppRed
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

val AnalyticsTrack = Color(0xFFEDEEF4)
val AnalyticsTabBackground = Color(0xFFE7E9F2)

enum class AnalyticsPeriod {
    HOJE,
    SEMANA,
    MES
}

enum class AreaComparisonMode {
    VISITORS,
    OCCUPANCY
}

data class DailyStat(
    val environmentName: String,
    val totalVisitors: Long,
    val avgOccupancyPercent: Int,
    val peakOccupancyPercent: Int,
    val avgWaitMinutes: Double
)

data class SummaryStat(
    val label: String,
    val value: String,
    val valueColor: Color
)

data class AreaComparison(
    val name: String,
    val value: Int,
    val maxValue: Int,
    val color: Color
)

data class AnalyticsInsight(
    val icon: ImageVector,
    val iconColor: Color,
    val textBefore: String,
    val highlight: String,
    val highlightColor: Color,
    val textAfter: String
)

fun occupancyColor(percent: Int): Color {

    return when {
        percent >= 70 -> AppRed
        percent >= 40 -> AppAmber
        else -> AppGreen
    }
}

fun peakOccupancyColor(percent: Int): Color {
    return when {
        percent >= 80 -> AppRed
        percent >= 60 -> AppAmber
        else -> AppGreen
    }
}


fun buildAreaComparisons(
    stats: List<DailyStat>
): List<AreaComparison> {

    val visitorsByArea = stats
        .groupBy { it.environmentName }
        .mapValues { (_, days) ->
            days.sumOf { it.totalVisitors }
        }

    val totalVisitors = visitorsByArea.values.sum()

    return visitorsByArea
        .map { (name, visitors) ->

            val percentage = if (totalVisitors > 0) {
                (visitors.toDouble() / totalVisitors * 100)
                    .roundToInt()
            } else {
                0
            }

            AreaComparison(
                name = name,
                value = percentage,
                maxValue = 100,
                color = occupancyColor(percentage)
            )
        }
        .sortedByDescending { it.value }
}

fun buildOccupancyComparisons(
    stats: List<DailyStat>
): List<AreaComparison> {

    return stats
        .groupBy { it.environmentName }
        .map { (name, days) ->

            val peakOccupancy = days.maxOfOrNull {
                it.peakOccupancyPercent
            } ?: 0

            AreaComparison(
                name = name,
                value = peakOccupancy.coerceIn(0, 100),
                maxValue = 100,
                color = peakOccupancyColor(peakOccupancy)
            )
        }
        .sortedByDescending { it.value }
}

fun buildSummaryStats(
    stats: List<DailyStat>
): List<SummaryStat> {

    val totalVisitors = stats.sumOf { it.totalVisitors }

    val averageWait = if (totalVisitors > 0) {
        stats.sumOf { it.avgWaitMinutes * it.totalVisitors } / totalVisitors
    } else {
        0.0
    }

    val busiestArea = buildAreaComparisons(stats)
        .firstOrNull()
        ?.name
        ?: "-"

    val visitorsFormat = NumberFormat.getIntegerInstance(
        Locale.forLanguageTag("pt-BR")
    )

    return listOf(

        SummaryStat(
            label = "Visitantes Totais",
            value = visitorsFormat.format(totalVisitors),
            valueColor = AppNavy
        ),

        SummaryStat(
            label = "Média de Espera",
            value = "${averageWait.roundToInt()} min",
            valueColor = AppBlue
        ),

        SummaryStat(
            label = "Área mais Cheia",
            value = busiestArea,
            valueColor = AppRed
        )
    )
}