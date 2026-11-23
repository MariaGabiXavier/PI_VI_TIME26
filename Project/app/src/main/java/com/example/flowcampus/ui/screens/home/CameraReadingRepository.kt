
package com.example.flowcampus.ui.screens.home

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.util.Date
import java.time.ZoneId
import java.time.LocalDate

data class CameraReading(
    val environmentId: String = "",
    val environmentName: String = "",
    val people: Int = 0,
    val occupancyPercent: Double = 0.0,
    val timestamp: Timestamp? = null,
    val source: String = "camera"
)

class CameraReadingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun listenLatestReading(
        environmentId: String,
        onUpdate: (CameraReading?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        Log.d(
            "CameraReading",
            "Iniciando consulta para ambiente: $environmentId"
        )

        return db.collection("camera_readings")
            .whereEqualTo("environmentId", environmentId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e(
                        "CameraReading",
                        "ERRO ao consultar camera_readings",
                        error
                    )

                    onError(error)
                    return@addSnapshotListener
                }

                Log.d(
                    "CameraReading",
                    "Consulta recebida. Documentos: ${snapshot?.size() ?: 0}"
                )

                val document = snapshot?.documents?.firstOrNull()

                if (document == null) {
                    Log.w(
                        "CameraReading",
                        "Nenhuma leitura encontrada para $environmentId"
                    )

                    onUpdate(null)
                    return@addSnapshotListener
                }

                val reading = CameraReading(
                    environmentId = document.getString("environmentId")
                        ?: environmentId,

                    environmentName = document.getString("environmentName")
                        ?: "",

                    people = (document.get("people") as? Number)
                        ?.toInt() ?: 0,

                    occupancyPercent = (
                        document.get("occupancyPercent") as? Number
                    )?.toDouble() ?: 0.0,

                    timestamp = document.getTimestamp("timestamp")
                )

                Log.d(
                    "CameraReading",
                    "LEITURA RECEBIDA: ${reading.environmentName} | " +
                        "Pessoas: ${reading.people} | " +
                        "Ocupação: ${reading.occupancyPercent}%"
                )

                onUpdate(reading)
            }
    }


    /**
     * Consulta o histórico do ambiente para alimentar somente o gráfico de ocupação.
     * Não altera nem substitui listenLatestReading(), usado pelos cards da Home.
     */
    fun listenTodayReadings(
        environmentId: String,
        onUpdate: (List<CameraReading>) -> Unit,
        onError: (Exception) -> Unit
    ): List<ListenerRegistration> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atTime(7, 0).atZone(zone).toInstant()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()
        val startTimestamp = Timestamp(Date.from(start))
        val endTimestamp = Timestamp(Date.from(end))

        val readingsByKey = mutableMapOf<String, CameraReading>()

        fun notifyUpdate() {
            onUpdate(
                readingsByKey.values.sortedBy {
                    it.timestamp?.toDate()?.time ?: 0L
                }
            )
        }

        val cameraListener = db.collection("camera_readings")
            .whereEqualTo("environmentId", environmentId)
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .whereLessThan("timestamp", endTimestamp)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                readingsByKey.keys.removeAll { it.startsWith("camera:") }
                snapshot?.documents.orEmpty().forEach { document ->
                    readingsByKey["camera:${document.id}"] = CameraReading(
                        environmentId = document.getString("environmentId") ?: environmentId,
                        environmentName = document.getString("environmentName") ?: "",
                        people = (document.get("people") as? Number)?.toInt() ?: 0,
                        occupancyPercent = (document.get("occupancyPercent") as? Number)?.toDouble() ?: 0.0,
                        timestamp = document.getTimestamp("timestamp")
                    )
                }
                notifyUpdate()
            }

        val simulatedListener = db.collection("occupancy_readings")
            .whereEqualTo("environmentId", environmentId)
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .whereLessThan("timestamp", endTimestamp)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                readingsByKey.keys.removeAll { it.startsWith("simulated:") }
                snapshot?.documents.orEmpty().forEach { document ->
                    readingsByKey["simulated:${document.id}"] = CameraReading(
                        environmentId = document.getString("environmentId") ?: environmentId,
                        environmentName = document.getString("environmentName") ?: "",
                        people = (document.get("people") as? Number)?.toInt() ?: 0,
                        occupancyPercent = (document.get("occupancyPercent") as? Number)?.toDouble() ?: 0.0,
                        timestamp = document.getTimestamp("timestamp"),
                        source = "simulated"
                    )
                }
                notifyUpdate()
            }

        return listOf(cameraListener, simulatedListener)
    }

}
