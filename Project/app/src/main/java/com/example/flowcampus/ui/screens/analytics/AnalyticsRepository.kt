package com.example.flowcampus.ui.screens.analytics

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
 * Documento de `ai_insights` -> insight da tela (a conversao fica em
 * AnalyticsInsightMapper.kt, compartilhada com a IA).
 */
private fun DocumentSnapshot.toInsight(): AnalyticsInsight? {

    val text = getString("text") ?: return null

    return buildAnalyticsInsight(
        text = text,
        highlight = getString("highlight").orEmpty(),
        icon = getString("icon"),
        tone = getString("tone")
    )
}
