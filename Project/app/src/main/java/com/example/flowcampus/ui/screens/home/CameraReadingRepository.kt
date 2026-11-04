
package com.example.flowcampus.ui.screens.home

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp

data class CameraReading(
    val environmentId: String = "",
    val environmentName: String = "",
    val people: Int = 0,
    val occupancyPercent: Double = 0.0,
    val timestamp: Timestamp? = null
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
}
