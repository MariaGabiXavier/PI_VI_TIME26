package com.example.flowcampus.ui.screens.profile

import com.example.flowcampus.ui.screens.auth.AuthSession
import com.example.flowcampus.ui.screens.auth.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Dados do Perfil no Firestore. Tudo fica no documento do próprio usuário,
 * junto com o que o cadastro já salva:
 *
 *   users/{uid}
 *     ├─ ra: String
 *     ├─ favorites: [ambienteId, ...]        -> mesmo id das notificações (nome sem acento)
 *     ├─ visits: { "2026-10": 12, ... }      -> status de locais abertos em cada mês
 *     ├─ predictionsUsed: Number             -> previsões da IA carregadas em Análises
 *     └─ timeSavedMinutes: Number
 *
 * As gravações usam merge para não apagar os outros campos do documento.
 */
class ProfileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private companion object {
        const val USERS = "users"
    }

    fun listenActivity(
        userId: String,
        onUpdate: (UserActivity) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val favorites = (snapshot?.get("favorites") as? List<*>)
                    .orEmpty()
                    .filterIsInstance<String>()
                    .toSet()

                onUpdate(
                    UserActivity(
                        favoriteIds = favorites,
                        visitsThisMonth = snapshot?.getLong("visits.${currentMonthKey()}")?.toInt() ?: 0,
                        timeSavedMinutes = snapshot?.getLong("timeSavedMinutes")?.toInt() ?: 0,
                        predictionsUsed = snapshot?.getLong("predictionsUsed")?.toInt() ?: 0
                    )
                )
            }
    }

    fun setFavorite(
        userId: String,
        environmentId: String,
        favorite: Boolean
    ) {
        val change = if (favorite) {
            FieldValue.arrayUnion(environmentId)
        } else {
            FieldValue.arrayRemove(environmentId)
        }

        db.collection(USERS)
            .document(userId)
            .set(
                mapOf("favorites" to change),
                SetOptions.merge()
            )
    }

    fun registerVisit(userId: String) {
        db.collection(USERS)
            .document(userId)
            .set(
                mapOf("visits" to mapOf(currentMonthKey() to FieldValue.increment(1))),
                SetOptions.merge()
            )
    }

    fun registerPredictionUsed(userId: String) {
        db.collection(USERS)
            .document(userId)
            .set(
                mapOf("predictionsUsed" to FieldValue.increment(1)),
                SetOptions.merge()
            )
    }

    suspend fun updateProfile(
        profile: UserProfile,
        name: String,
        ra: String
    ): Result<Unit> {
        return try {
            db.collection(USERS)
                .document(profile.uid)
                .set(
                    mapOf(
                        "name" to name,
                        "ra" to ra
                    ),
                    SetOptions.merge()
                )
                .await()

            // Atualiza também quem já está mostrando o usuário (Home, Perfil)
            AuthSession.set(profile.copy(name = name, ra = ra))

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
