
package com.example.flowcampus.ui.screens.home

import com.example.flowcampus.ui.screens.auth.AuthSession
import com.example.flowcampus.ui.screens.auth.UserRole
import com.example.flowcampus.ui.screens.notifications.toEnvironmentId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class EnvironmentFormData(
    val name: String,
    val category: PlaceIcon?,
    val building: String,
    val capacity: Int,
    val operatingHours: String,
    val description: String
) {
    fun validationError(requireBuilding: Boolean, requireHours: Boolean): String? = when {
        name.isBlank() -> "Informe o nome do ambiente."
        category == null -> "Selecione uma categoria."
        requireBuilding && building.isBlank() -> "Informe o prédio."
        capacity <= 0 -> "Informe uma capacidade maior que zero."
        requireHours && operatingHours.isBlank() -> "Informe o horário de funcionamento."
        else -> null
    }
}

class EnvironmentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val environments = db.collection("environments")

    suspend fun saveEnvironment(
        environmentId: String?,
        data: EnvironmentFormData
    ): Result<Unit> = writeEnvironment { userId ->
        val fields = mapOf(
            "name" to data.name.trim(),
            "category" to (data.category?.name ?: error("Selecione uma categoria.")),
            "building" to data.building.trim(),
            "capacity" to data.capacity,
            "operatingHours" to data.operatingHours.trim(),
            "description" to data.description.trim()
        )

        if (environmentId == null) {
            data.validationError(requireBuilding = true, requireHours = true)?.let { error(it) }
            environments.add(
                fields + mapOf(
                    "createdBy" to userId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val reference = environments.document(environmentId)
            db.runTransaction { transaction ->
                val current = transaction.get(reference)
                check(current.exists()) { "Este ambiente não está mais disponível." }
                data.validationError(
                    requireBuilding = !current.getString("building").isNullOrBlank(),
                    requireHours = !current.getString("operatingHours").isNullOrBlank()
                )?.let { error(it) }

                // Favoritos antigos usam o nome original como identificador.
                val favoriteId = current.getString("favoriteId")
                    ?.takeIf { it.isNotBlank() }
                    ?: current.getString("name").orEmpty().toEnvironmentId()
                transaction.update(reference, fields + ("favoriteId" to favoriteId))
                Unit
            }.await()
        }
    }

    suspend fun deleteEnvironment(environmentId: String): Result<Unit> = writeEnvironment {
        environments.document(environmentId).delete().await()
    }

    private suspend fun writeEnvironment(
        write: suspend (String) -> Unit
    ): Result<Unit> = try {
        val profile = AuthSession.user.value
        check(profile != null && profile.role == UserRole.STAFF && profile.uid == auth.currentUser?.uid) {
            "Apenas colaboradores podem gerenciar ambientes."
        }
        write(profile.uid)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun listenEnvironments(
        onUpdate: (List<CampusPlace>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return environments
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

                        val capacity = (document.get("capacity") as? Number)
                            ?.toInt()
                            ?.coerceAtLeast(1)
                            ?: 40

                        CampusPlace(
                            name = name,
                            people = 0,
                            minutes = 0,
                            status = "Sem dados",
                            iconType = category,
                            favorite = false,
                            capacity = capacity,
                            firestoreId = document.id,
                            building = document.getString("building").orEmpty(),
                            operatingHours = document.getString("operatingHours").orEmpty(),
                            description = document.getString("description").orEmpty(),
                            favoriteId = document.getString("favoriteId")
                                ?.takeIf { it.isNotBlank() }
                                ?: name.toEnvironmentId()
                        )
                    }

                onUpdate(places)
            }
    }
}
