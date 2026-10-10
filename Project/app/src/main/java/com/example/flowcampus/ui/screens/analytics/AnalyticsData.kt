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

/**
 * Resumo de UM dia de UM ambiente.
 * Vem do Firestore: coleção `daily_stats`.
 */
data class DailyStat(
    val environmentName: String,
    val totalVisitors: Long,
    val avgOccupancyPercent: Int,
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

// ---------------------------------------------------------------------
// Cálculos da tela a partir dos dados do Firestore
// ---------------------------------------------------------------------

/** Abaixo de 40% verde, de 40% a 69% âmbar, 70% ou mais vermelho. */
fun occupancyColor(percent: Int): Color {

    return when {
        percent >= 70 -> AppRed
        percent >= 40 -> AppAmber
        else -> AppGreen
    }
}

/** Ocupação média de cada ambiente no período, da mais cheia para a mais vazia. */
fun buildAreaComparisons(
    stats: List<DailyStat>
): List<AreaComparison> {

    return stats
        .groupBy { it.environmentName }
        .map { (name, days) ->

            val average = days
                .map { it.avgOccupancyPercent }
                .average()
                .roundToInt()

            AreaComparison(
                name = name,
                value = average,
                maxValue = 100,
                color = occupancyColor(average)
            )
        }
        .sortedByDescending { it.value }
}

/** Os 3 cards do topo: visitantes, espera média e área mais cheia. */
fun buildSummaryStats(
    stats: List<DailyStat>
): List<SummaryStat> {

    val totalVisitors = stats.sumOf { it.totalVisitors }

    // espera média ponderada pelo número de visitantes
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