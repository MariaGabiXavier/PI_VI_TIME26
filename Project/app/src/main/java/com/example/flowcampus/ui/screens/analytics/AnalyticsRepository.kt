package com.example.flowcampus.ui.screens.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.flowcampus.ui.theme.AppAmber
import com.example.flowcampus.ui.theme.AppBlue
import com.example.flowcampus.ui.theme.AppGreen
import com.example.flowcampus.ui.theme.AppRed
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Lê do Firestore os dados da tela Análises.
 *
 *   daily_stats/{ambienteId}_{yyyy-MM-dd}   -> resumo diário de cada ambiente
 *   ai_insights/{id}                        -> frases da seção "Insights da IA"
 *
 * Os dois são "listeners": quando o documento muda no Firestore
 * (ex.: a IA grava um insight novo), a tela atualiza sozinha.
 */
class AnalyticsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private companion object {
        const val DAILY_STATS = "daily_stats"
        const val AI_INSIGHTS = "ai_insights"
        const val MAX_INSIGHTS = 3L
    }

    fun listenDailyStats(
        period: AnalyticsPeriod,
        onUpdate: (List<DailyStat>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        val daysBack = when (period) {
            AnalyticsPeriod.HOJE -> 0
            AnalyticsPeriod.SEMANA -> 6
            AnalyticsPeriod.MES -> 29
        }

        return db.collection(DAILY_STATS)
            .whereGreaterThanOrEqualTo("dateKey", dateKey(daysBack))
            .whereLessThanOrEqualTo("dateKey", dateKey(0))
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val stats = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toDailyStat() }

                onUpdate(stats)
            }
    }

    fun listenInsights(
        onUpdate: (List<AnalyticsInsight>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return db.collection(AI_INSIGHTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(MAX_INSIGHTS)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val insights = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toInsight() }

                onUpdate(insights)
            }
    }
}

/** "2026-10-10" de hoje (daysBack = 0) ou de N dias atrás. */
private fun dateKey(daysBack: Int): String {

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysBack)

    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

private fun DocumentSnapshot.toDailyStat(): DailyStat? {

    val name = getString("environmentName") ?: return null

    return DailyStat(
        environmentName = name,
        totalVisitors = getLong("totalVisitors") ?: 0L,
        avgOccupancyPercent = getLong("avgOccupancyPercent")?.toInt() ?: 0,
        avgWaitMinutes = getDouble("avgWaitMinutes") ?: 0.0
    )
}

/**
 * Campos do documento em `ai_insights`:
 *   text      -> frase inteira
 *   highlight -> trecho da frase que aparece em negrito colorido
 *   icon      -> BOOK | CLOCK | HOME | BUS | FOOD (outro valor = lâmpada)
 *   tone      -> BLUE | RED | GREEN | AMBER
 */
private fun DocumentSnapshot.toInsight(): AnalyticsInsight? {

    val text = getString("text") ?: return null
    val highlight = getString("highlight").orEmpty()
    val color = toneColor(getString("tone"))

    val start = if (highlight.isBlank()) -1 else text.indexOf(highlight)

    return if (start >= 0) {
        AnalyticsInsight(
            icon = insightIcon(getString("icon")),
            iconColor = color,
            textBefore = text.substring(0, start),
            highlight = highlight,
            highlightColor = color,
            textAfter = text.substring(start + highlight.length)
        )
    } else {
        AnalyticsInsight(
            icon = insightIcon(getString("icon")),
            iconColor = color,
            textBefore = text,
            highlight = "",
            highlightColor = color,
            textAfter = ""
        )
    }
}

private fun toneColor(tone: String?): Color {

    return when (tone) {
        "RED" -> AppRed
        "GREEN" -> AppGreen
        "AMBER" -> AppAmber
        else -> AppBlue
    }
}

private fun insightIcon(icon: String?): ImageVector {

    return when (icon) {
        "BOOK" -> Icons.Default.MenuBook
        "CLOCK" -> Icons.Default.AccessTime
        "HOME" -> Icons.Default.Home
        "BUS" -> Icons.Default.DirectionsBus
        "FOOD" -> Icons.Default.Restaurant
        else -> Icons.Default.Lightbulb
    }
}