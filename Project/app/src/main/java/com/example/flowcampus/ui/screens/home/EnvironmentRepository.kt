
package com.example.flowcampus.ui.screens.home

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class EnvironmentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun addEnvironment(
        name: String,
        category: PlaceIcon,
        building: String,
        capacity: Int,
        operatingHours: String,
        description: String,
        createdBy: String
    ): Result<Unit> {
        return try {
            require(name.isNotBlank()) {
                "Informe o nome do ambiente."
            }
            require(building.isNotBlank()) {
                "Informe o prédio."
            }
            require(capacity > 0) {
                "A capacidade deve ser maior que zero."
            }
            require(operatingHours.isNotBlank()) {
                "Informe o horário de funcionamento."
            }

            val environment = mapOf(
                "name" to name.trim(),
                "category" to category.name,
                "building" to building.trim(),
                "capacity" to capacity,
                "operatingHours" to operatingHours.trim(),
                "description" to description.trim(),
                "createdBy" to createdBy,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("environments")
                .add(environment)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenEnvironments(
        onUpdate: (List<CampusPlace>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("environments")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val places = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        val name = document.getString("name")
                            ?: return@mapNotNull null

                        val category = document.getString("category")
                            ?.let {
                                runCatching {
                                    PlaceIcon.valueOf(it)
                                }.getOrNull()
                            } ?: PlaceIcon.CLASSROOM

                        CampusPlace(
                            name = name,
                            people = 0,
                            minutes = 0,
                            status = "Sem dados",
                            iconType = category,
                            favorite = false
                        )
                    }

                onUpdate(places)
            }
    }
}
