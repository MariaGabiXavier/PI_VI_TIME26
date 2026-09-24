package com.example.flowcampus.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.screens.home.Blue
import com.example.flowcampus.ui.screens.home.DarkText

@Composable
fun NotificationsScreen() {

    var notifications by remember { mutableStateOf(getMockNotifications()) }

    var selectedFilter by remember {
        mutableStateOf("Tudo")
    }

    val filteredNotifications = when (selectedFilter) {
        "Alertas" -> notifications.filter { it.category == NotificationCategory.ALERT }
        "Atualizações" -> notifications.filter { it.category == NotificationCategory.UPDATE }
        else -> notifications
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
                    notifications = emptyList()
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(filteredNotifications) { notification ->
                NotificationCard(notification)
            }
        }
    }
}