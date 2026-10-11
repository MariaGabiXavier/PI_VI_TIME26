package com.example.flowcampus.ui.screens.notifications

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.screens.home.Blue
import com.example.flowcampus.ui.screens.home.DarkText
import com.example.flowcampus.ui.screens.home.GrayText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun rememberCurrentUserId(): String? {

    var userId by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid)
    }

    DisposableEffect(Unit) {

        val auth = FirebaseAuth.getInstance()

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            userId = firebaseAuth.currentUser?.uid
        }

        auth.addAuthStateListener(authListener)

        onDispose {
            auth.removeAuthStateListener(authListener)
        }
    }

    return userId
}
@Composable
fun rememberUnreadNotificationsCount(): Int {

    val repository = remember { NotificationsRepository() }

    val userId = rememberCurrentUserId()

    var notifications by remember {
        mutableStateOf<List<NotificationItem>>(emptyList())
    }

    var clearedAt by remember {
        mutableStateOf<Date?>(null)
    }

    var seenAt by remember {
        mutableStateOf<Date?>(null)
    }

    DisposableEffect(Unit) {

        val listener = repository.listenNotifications(
            onUpdate = {
                notifications = it
            },
            onError = { }
        )

        onDispose {
            listener.remove()
        }
    }

    DisposableEffect(userId) {

        val currentUserId = userId

        clearedAt = null
        seenAt = null

        val listener = currentUserId?.let { id ->
            repository.listenUserState(
                userId = id,
                onUpdate = { cleared, seen ->
                    clearedAt = cleared
                    seenAt = seen
                },
                onError = { }
            )
        }

        onDispose {
            listener?.remove()
        }
    }

    val clearedDate = clearedAt
    val seenDate = seenAt

    return notifications.count { notification ->
        isUnread(notification, clearedDate, seenDate)
    }
}

@Composable
fun NotificationsScreen(
    onNotificationClick: (String) -> Unit = {}
) {

    val repository = remember { NotificationsRepository() }

    val scope = rememberCoroutineScope()

    val userId = rememberCurrentUserId()

    var notifications by remember {
        mutableStateOf<List<NotificationItem>>(emptyList())
    }

    var clearedAt by remember {
        mutableStateOf<Date?>(null)
    }

    var seenAt by remember {
        mutableStateOf<Date?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var hasError by remember {
        mutableStateOf(false)
    }

    var selectedFilter by remember {
        mutableStateOf("Tudo")
    }

    DisposableEffect(Unit) {

        val listener = repository.listenNotifications(
            onUpdate = {
                notifications = it
                isLoading = false
                hasError = false
            },
            onError = {
                Log.e("Notifications", "Erro ao ler notificações", it)
                isLoading = false
                hasError = true
            }
        )

        onDispose {
            listener.remove()
        }
    }

    DisposableEffect(userId) {

        val currentUserId = userId

        clearedAt = null
        seenAt = null

        val listener = currentUserId?.let { id ->
            repository.listenUserState(
                userId = id,
                onUpdate = { cleared, seen ->
                    clearedAt = cleared
                    seenAt = seen
                },
                onError = { }
            )
        }

        onDispose {
            listener?.remove()

            currentUserId?.let { id ->
                repository.markNotificationsAsSeen(id)
            }
        }
    }

    val clearedDate = clearedAt
    val seenDate = seenAt

    val visibleNotifications = notifications.filter { notification ->
        isVisible(notification, clearedDate)
    }

    val filteredNotifications = when (selectedFilter) {
        "Alertas" -> visibleNotifications.filter { it.category == NotificationCategory.ALERT }
        "Atualizações" -> visibleNotifications.filter { it.category == NotificationCategory.UPDATE }
        else -> visibleNotifications
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Notificações",
                color = DarkText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Limpar",
                color = Blue,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    userId?.let { id ->
                        scope.launch {
                            repository.clearNotifications(id)
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            listOf("Tudo", "Alertas", "Atualizações").forEach { filter ->

                NotificationFilterChip(
                    label = filter,
                    selected = filter == selectedFilter,
                    onClick = {
                        selectedFilter = filter
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading || hasError || filteredNotifications.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = when {
                        isLoading -> "Carregando..."
                        hasError -> "Não foi possível carregar as notificações."
                        else -> "Nenhuma notificação por aqui."
                    },
                    color = GrayText,
                    fontSize = 13.sp
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = filteredNotifications,
                    key = { it.id }
                ) { notification ->

                    NotificationCard(
                        notification = notification,
                        isUnread = isUnread(notification, clearedDate, seenDate),
                        onClick = {
                            if (notification.environmentId.isNotBlank()) {
                                onNotificationClick(notification.environmentId)
                            }
                        }
                    )
                }
            }
        }
    }
}