package com.example.flowcampus.ui.screens.notifications

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

class NotificationsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun listenNotifications(
        onUpdate: (List<NotificationItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        val title = document.getString("title")
                            ?: return@mapNotNull null

                        val description = document.getString("description")
                            ?: return@mapNotNull null

                        val createdAt = document.getTimestamp("createdAt")?.toDate()
                            ?: return@mapNotNull null

                        val category = document.getString("category")
                            ?.let {
                                runCatching {
                                    NotificationCategory.valueOf(it)
                                }.getOrNull()
                            } ?: NotificationCategory.UPDATE

                        val icon = document.getString("icon")
                            ?.let {
                                runCatching {
                                    NotificationIcon.valueOf(it)
                                }.getOrNull()
                            } ?: NotificationIcon.OCCUPANCY

                        val tone = document.getString("tone")
                            ?.let {
                                runCatching {
                                    NotificationTone.valueOf(it)
                                }.getOrNull()
                            } ?: NotificationTone.BLUE

                        NotificationItem(
                            id = document.id,
                            icon = icon,
                            iconBackground = toneBackground(tone),
                            iconTint = toneTint(tone),
                            title = title,
                            description = description,
                            time = formatTimeAgo(createdAt),
                            category = category,
                            createdAt = createdAt
                        )
                    }

                onUpdate(notifications)
            }
    }

    fun listenClearedAt(
        userId: String,
        onUpdate: (Date?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onUpdate(
                    snapshot?.getTimestamp("notificationsClearedAt")?.toDate()
                )
            }
    }

    suspend fun clearNotifications(userId: String): Result<Unit> {
        return try {
            db.collection("users")
                .document(userId)
                .set(
                    mapOf("notificationsClearedAt" to Timestamp.now()),
                    SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}