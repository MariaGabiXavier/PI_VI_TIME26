package com.example.flowcampus.ui.screens.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.flowcampus.ui.theme.AppAmber
import com.example.flowcampus.ui.theme.AppBlue
import com.example.flowcampus.ui.theme.AppGreen
import com.example.flowcampus.ui.theme.AppNavy
import com.example.flowcampus.ui.theme.AppRed

val AnalyticsTrack = Color(0xFFEDEEF4)
val AnalyticsTabBackground = Color(0xFFE7E9F2)

enum class AnalyticsPeriod {
    HOJE,
    SEMANA,
    MES
}

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

fun getSummaryStats(
    period: AnalyticsPeriod
): List<SummaryStat> {

    return when (period) {

        AnalyticsPeriod.HOJE -> listOf(

            SummaryStat(
                label = "Visitantes Totais",
                value = "2.847",
                valueColor = AppNavy
            ),

            SummaryStat(
                label = "Média de Espera",
                value = "6 min",
                valueColor = AppBlue
            ),

            SummaryStat(
                label = "Área mais Cheia",
                value = "Refeitório",
                valueColor = AppRed
            )
        )

        AnalyticsPeriod.SEMANA -> listOf(

            SummaryStat(
                label = "Visitantes Totais",
                value = "18.930",
                valueColor = AppNavy
            ),

            SummaryStat(
                label = "Média de Espera",
                value = "8 min",
                valueColor = AppBlue
            ),

            SummaryStat(
                label = "Área mais Cheia",
                value = "Refeitório",
                valueColor = AppRed
            )
        )

        AnalyticsPeriod.MES -> listOf(

            SummaryStat(
                label = "Visitantes Totais",
                value = "76.210",
                valueColor = AppNavy
            ),

            SummaryStat(
                label = "Média de Espera",
                value = "7 min",
                valueColor = AppBlue
            ),

            SummaryStat(
                label = "Área mais Cheia",
                value = "Biblioteca",
                valueColor = AppRed
            )
        )
    }
}

fun getAreaComparisons(
    period: AnalyticsPeriod
): List<AreaComparison> {

    return when (period) {

        AnalyticsPeriod.HOJE -> listOf(
            AreaComparison("Refeitório", 78, 100, AppRed),
            AreaComparison("Livraria", 34, 100, AppGreen),
            AreaComparison("Lab A2", 52, 100, AppAmber),
            AreaComparison("Sala Estudo - 7A", 63, 100, AppAmber)
        )

        AnalyticsPeriod.SEMANA -> listOf(
            AreaComparison("Refeitório", 82, 100, AppRed),
            AreaComparison("Livraria", 41, 100, AppGreen),
            AreaComparison("Lab A2", 47, 100, AppAmber),
            AreaComparison("Sala Estudo - 7A", 58, 100, AppAmber)
        )

        AnalyticsPeriod.MES -> listOf(
            AreaComparison("Refeitório", 74, 100, AppRed),
            AreaComparison("Livraria", 39, 100, AppGreen),
            AreaComparison("Lab A2", 55, 100, AppAmber),
            AreaComparison("Sala Estudo - 7A", 60, 100, AppAmber)
        )
    }
}

fun getInsights(
    period: AnalyticsPeriod
): List<AnalyticsInsight> {

    return listOf(

        AnalyticsInsight(
            icon = Icons.Default.MenuBook,
            iconColor = AppBlue,
            textBefore = "A biblioteca costuma ficar ",
            highlight = "40% mais cheia",
            highlightColor = AppBlue,
            textAfter = " na terça-feira."
        ),

        AnalyticsInsight(
            icon = Icons.Default.AccessTime,
            iconColor = AppRed,
            textBefore = "O horário de pico do refeitório está começando ",
            highlight = "30 min mais cedo",
            highlightColor = AppRed,
            textAfter = " essa semana."
        ),

        AnalyticsInsight(
            icon = Icons.Default.Home,
            iconColor = AppGreen,
            textBefore = "A sala de Estudo é constantemente ",
            highlight = "menos usada",
            highlightColor = AppGreen,
            textAfter = " após às 18:00."
        )
    )
}
