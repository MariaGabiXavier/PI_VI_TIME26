package com.example.flowcampus.ui.screens.analytics

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.util.Log

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
    fun listenCameraAreas(
        onUpdate: (List<CameraArea>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return db.collection("camera_readings")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { readingsSnapshot, error ->

                if (error != null) {
                    Log.e("AnalyticsRepository", "Erro ao buscar camera_readings", error)
                    onError(error)
                    return@addSnapshotListener
                }

                val latestReadings = readingsSnapshot?.documents
                    .orEmpty()
                    .mapNotNull { document ->

                        val environmentId =
                            document.getString("environmentId")
                                ?: return@mapNotNull null

                        CameraArea(
                            environmentId = environmentId,
                            name = document.getString("environmentName")
                                ?: "Ambiente",
                            people = (document.get("people") as? Number)
                                ?.toInt() ?: 0,
                            capacity = 1
                        )
                    }
                    .distinctBy { it.environmentId }

                Log.d(
                    "AnalyticsRepository",
                    "Leituras de câmera encontradas: ${latestReadings.size}"
                )

                db.collection("environments")
                    .get()
                    .addOnSuccessListener { environmentsSnapshot ->

                        val capacities = environmentsSnapshot.documents
                            .associate { document ->
                                document.id to (
                                        (document.get("capacity") as? Number)
                                            ?.toInt() ?: 1
                                        )
                            }

                        val areas = latestReadings.map { reading ->
                            reading.copy(
                                capacity = capacities[reading.environmentId] ?: 1
                            )
                        }

                        onUpdate(areas)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            "AnalyticsRepository",
                            "Erro ao buscar capacidades dos ambientes",
                            exception
                        )
                        onError(exception)
                    }
            }
    }
}

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

private fun DocumentSnapshot.toInsight(): AnalyticsInsight? {

    val text = getString("text") ?: return null

    return buildAnalyticsInsight(
        text = text,
        highlight = getString("highlight").orEmpty(),
        icon = getString("icon"),
        tone = getString("tone")
    )
}
