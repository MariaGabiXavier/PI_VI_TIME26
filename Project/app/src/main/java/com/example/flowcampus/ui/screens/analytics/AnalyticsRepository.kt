
package com.example.flowcampus.ui.screens.analytics

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class AnalyticsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private companion object {
        const val SIMULATED_READINGS = "occupancy_readings"
        const val CAMERA_READINGS = "camera_readings"
        const val AI_INSIGHTS = "ai_insights"
        const val MAX_INSIGHTS = 3L
    }

    fun listenCombinedStats(
        period: AnalyticsPeriod,
        onUpdate: (List<DailyStat>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        val (startDate, endDate) = timestampRange(period)

        android.util.Log.d(
            "AnalyticsRepository",
            "PERÍODO: $period | " +
                    "INÍCIO: ${startDate.toDate()} | " +
                    "FIM: ${endDate.toDate()}"
        )

        var simulatedDocs: List<DocumentSnapshot>? = null
        var cameraDocs: List<DocumentSnapshot>? = null

        var simulatedError: Exception? = null
        var cameraError: Exception? = null

        fun publishIfReady() {
            val simulated = simulatedDocs ?: return
            val camera = cameraDocs ?: return

            if (simulatedError != null) {
                onError(simulatedError!!)
                return
            }

            if (cameraError != null) {
                onError(cameraError!!)
                return
            }

            android.util.Log.d(
                "AnalyticsRepository",
                "Leituras simuladas: ${simulated.size} | " +
                        "Leituras das câmeras: ${camera.size}"
            )

            val combined = combineReadings(simulated, camera)

            android.util.Log.d(
                "AnalyticsRepository",
                "Resultado após unificação: ${combined.size} registros"
            )

            onUpdate(combined)
        }

        val simulatedListener = db.collection(SIMULATED_READINGS)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    simulatedError = error
                    simulatedDocs = emptyList()
                } else {
                    simulatedError = null
                    simulatedDocs = snapshot?.documents.orEmpty()

                    simulatedDocs?.forEach { doc ->
                        android.util.Log.d(
                            "AnalyticsRepository",
                            "SIMULADO: ${doc.id} | " +
                                    "timestamp: ${doc.getTimestamp("timestamp")?.toDate()}"
                        )
                    }
                }

                publishIfReady()
            }

        val cameraListener = db.collection(CAMERA_READINGS)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    cameraError = error
                    cameraDocs = emptyList()
                } else {
                    cameraError = null
                    cameraDocs = snapshot?.documents.orEmpty()

                    cameraDocs?.forEach { doc ->
                        android.util.Log.d(
                            "AnalyticsRepository",
                            "CÂMERA: ${doc.id} | " +
                                    "timestamp: ${doc.getTimestamp("timestamp")?.toDate()}"
                        )
                    }
                }

                publishIfReady()
            }

        return object : ListenerRegistration {
            override fun remove() {
                simulatedListener.remove()
                cameraListener.remove()
            }
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

private data class RawReading(
    val environmentId: String,
    val environmentName: String,
    val people: Int,
    val capacity: Int,
    val occupancyPercent: Double,
    val entries: Long,
    val waitMinutes: Double,
    val timestamp: Long
)

private data class HourKey(
    val environmentId: String,
    val hour: Long
)

private data class CombinedHour(
    val environmentName: String,
    val people: Int,
    val occupancyPercent: Int,
    val entries: Long,
    val waitMinutes: Double
)

private fun DocumentSnapshot.toRawReading(
    isCamera: Boolean
): RawReading? {

    val environmentId = getString("environmentId")
        ?: return null

    val timestamp = getTimestamp("timestamp")
        ?: return null

    val people = (get("people") as? Number)
        ?.toInt() ?: 0

    val occupancy = (get("occupancyPercent") as? Number)
        ?.toDouble() ?: 0.0

    val savedCapacity = (get("capacity") as? Number)
        ?.toInt() ?: 0

    val estimatedCapacity = if (occupancy > 0.0) {
        (people * 100.0 / occupancy).roundToInt()
    } else {
        0
    }

    return RawReading(
        environmentId = environmentId,
        environmentName = getString("environmentName")
            ?: "Ambiente",
        people = people,
        capacity = if (savedCapacity > 0) {
            savedCapacity
        } else {
            estimatedCapacity
        },
        occupancyPercent = occupancy,
        entries = if (isCamera) {
            0L
        } else {
            (get("entries") as? Number)?.toLong() ?: 0L
        },
        waitMinutes = if (isCamera) {
            0.0
        } else {
            (get("waitMinutes") as? Number)?.toDouble() ?: 0.0
        },
        timestamp = timestamp.toDate().time
    )
}

private fun median(values: List<Int>): Int {
    if (values.isEmpty()) return 0

    val sorted = values.sorted()
    val middle = sorted.size / 2

    return if (sorted.size % 2 == 0) {
        ((sorted[middle - 1] + sorted[middle]) / 2.0)
            .roundToInt()
    } else {
        sorted[middle]
    }
}

private fun combineReadings(
    simulatedDocuments: List<DocumentSnapshot>,
    cameraDocuments: List<DocumentSnapshot>
): List<DailyStat> {

    val simulated = simulatedDocuments
        .mapNotNull { it.toRawReading(isCamera = false) }

    val camera = cameraDocuments
        .mapNotNull { it.toRawReading(isCamera = true) }

    fun hourKey(reading: RawReading): HourKey {
        val calendar = Calendar.getInstance(
            TimeZone.getTimeZone("America/Sao_Paulo")
        )
        calendar.timeInMillis = reading.timestamp
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return HourKey(
            environmentId = reading.environmentId,
            hour = calendar.timeInMillis
        )
    }

    // Uma leitura simulada por ambiente e hora.
    val simulatedByHour = simulated
        .groupBy { hourKey(it) }
        .mapValues { (_, readings) ->
            readings.maxByOrNull { it.timestamp }!!
        }

    // A câmera pode registrar várias capturas na mesma hora.
    // Usamos a mediana para não somar repetidamente as mesmas pessoas.
    val cameraByHour = camera
        .groupBy { hourKey(it) }
        .mapValues { (_, readings) ->

            val latest = readings.maxByOrNull { it.timestamp }!!

            latest.copy(
                people = median(readings.map { it.people }),
                capacity = median(
                    readings.map { it.capacity }.filter { it > 0 }
                ),
                occupancyPercent = readings
                    .map { it.occupancyPercent }
                    .average()
            )
        }

    val allKeys = simulatedByHour.keys + cameraByHour.keys

    val combinedHours = allKeys.mapNotNull { key ->

        val simulatedReading = simulatedByHour[key]
        val cameraReading = cameraByHour[key]

        val people =
            (simulatedReading?.people ?: 0) +
                    (cameraReading?.people ?: 0)

        val capacity =
            simulatedReading?.capacity?.takeIf { it > 0 }
                ?: cameraReading?.capacity?.takeIf { it > 0 }
                ?: 0

        val occupancy = if (capacity > 0) {
            (people.toDouble() / capacity * 100)
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            maxOf(
                simulatedReading?.occupancyPercent?.roundToInt() ?: 0,
                cameraReading?.occupancyPercent?.roundToInt() ?: 0
            ).coerceIn(0, 100)
        }

        val name = simulatedReading?.environmentName
            ?: cameraReading?.environmentName
            ?: return@mapNotNull null

        CombinedHour(
            environmentName = name,
            people = people,
            occupancyPercent = occupancy,
            entries = simulatedReading?.entries ?: 0L,
            waitMinutes = simulatedReading?.waitMinutes ?: 0.0
        )
    }

    // Agrupa os resultados combinados por ambiente e dia.

    val result = mutableListOf<DailyStat>()

    val combinedByKey = allKeys.associateWith { key ->

        val simulatedReading = simulatedByHour[key]
        val cameraReading = cameraByHour[key]

        val people =
            (simulatedReading?.people ?: 0) +
                    (cameraReading?.people ?: 0)

        val capacity =
            simulatedReading?.capacity?.takeIf { it > 0 }
                ?: cameraReading?.capacity?.takeIf { it > 0 }
                ?: 0

        val occupancy = if (capacity > 0) {
            (people.toDouble() / capacity * 100)
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            maxOf(
                simulatedReading?.occupancyPercent?.roundToInt() ?: 0,
                cameraReading?.occupancyPercent?.roundToInt() ?: 0
            ).coerceIn(0, 100)
        }

        CombinedHour(
            environmentName = simulatedReading?.environmentName
                ?: cameraReading?.environmentName
                ?: "Ambiente",
            people = people,
            occupancyPercent = occupancy,
            entries = simulatedReading?.entries ?: 0L,
            waitMinutes = simulatedReading?.waitMinutes ?: 0.0
        )
    }

    combinedByKey.entries
        .groupBy { entry ->
            val dateFormat = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
            }

            val date = dateFormat.format(Date(entry.key.hour))

            "${entry.key.environmentId}|$date"
        }
        .values
        .forEach { entries ->

            val readings = entries.map { it.value }

            val totalPeople = readings.sumOf {
                it.people.toLong()
            }

            val averageOccupancy = readings
                .map { it.occupancyPercent }
                .average()
                .roundToInt()

            val peakOccupancy = readings.maxOfOrNull {
                it.occupancyPercent
            } ?: 0

            val totalEntries = readings.sumOf { it.entries }

            val averageWait = if (totalEntries > 0) {
                readings.sumOf {
                    it.waitMinutes * it.entries
                } / totalEntries
            } else {
                0.0
            }

            result.add(
                DailyStat(
                    environmentName = readings.first().environmentName,
                    totalVisitors = totalPeople,
                    avgOccupancyPercent = averageOccupancy,
                    peakOccupancyPercent = peakOccupancy,
                    avgWaitMinutes = averageWait,
                    waitWeight = totalEntries
                )
            )
        }

    return result

}


private fun timestampRange(
    period: AnalyticsPeriod
): Pair<Timestamp, Timestamp> {

    val timeZone = TimeZone.getTimeZone("America/Sao_Paulo")

    val end = Calendar.getInstance(timeZone)
    val start = end.clone() as Calendar

    when (period) {

        AnalyticsPeriod.HOJE -> Unit

        AnalyticsPeriod.SEMANA -> {
            val daysSinceSunday =
                start.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

            start.add(Calendar.DAY_OF_YEAR, -daysSinceSunday)
        }

        AnalyticsPeriod.MES -> {
            start.set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)

    return Timestamp(start.time) to Timestamp(end.time)
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
