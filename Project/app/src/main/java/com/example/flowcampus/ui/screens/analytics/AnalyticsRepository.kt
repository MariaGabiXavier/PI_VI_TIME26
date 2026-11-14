package com.example.flowcampus.ui.screens.analytics

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

        val (startDate, endDate) = dateRange(period)

        return db.collection(DAILY_STATS)
            .whereGreaterThanOrEqualTo("dateKey", startDate)
            .whereLessThanOrEqualTo("dateKey", endDate)
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


private fun dateRange(period: AnalyticsPeriod): Pair<String, String> {

    val today = Calendar.getInstance()
    val endDate = formatDateKey(today)

    val start = today.clone() as Calendar

    when (period) {

        AnalyticsPeriod.HOJE -> {
        }

        AnalyticsPeriod.SEMANA -> {
            val daysSinceSunday =
                start.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

            start.add(Calendar.DAY_OF_YEAR, -daysSinceSunday)
        }

        AnalyticsPeriod.MES -> {
            start.set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    return Pair(formatDateKey(start), endDate)
}

private fun formatDateKey(calendar: Calendar): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .format(calendar.time)
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


private fun DocumentSnapshot.toInsight(): AnalyticsInsight? {

    val text = getString("text") ?: return null

    return buildAnalyticsInsight(
        text = text,
        highlight = getString("highlight").orEmpty(),
        icon = getString("icon"),
        tone = getString("tone")
    )
}
