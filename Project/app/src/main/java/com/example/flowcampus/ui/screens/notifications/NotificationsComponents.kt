package com.example.flowcampus.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.screens.home.Blue
import com.example.flowcampus.ui.screens.home.BorderGray
import com.example.flowcampus.ui.screens.home.DarkText
import com.example.flowcampus.ui.screens.home.GrayText

@Composable
fun NotificationFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        shape = RoundedCornerShape(20.dp),

        color = if (selected) {
            Blue
        } else {
            Color.White
        },

        modifier = Modifier.clickable {
            onClick()
        }
    ) {

        Text(
            text = label,

            color = if (selected) {
                Color.White
            } else {
                DarkText
            },

            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,

            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        )
    }
}

@Composable
fun NotificationCard(notification: NotificationItem) {

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(notification.iconBackground, CircleShape),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = notification.icon.toImageVector(),
                    contentDescription = null,
                    tint = notification.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {

                Text(
                    text = notification.title,
                    color = DarkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.description,
                    color = GrayText,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = notification.time,
                    color = BorderGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// Mapeia cada categoria de notificação pro ícone correspondente
private fun NotificationIcon.toImageVector() = when (this) {
    NotificationIcon.OCCUPANCY -> Icons.Default.TrendingUp
    NotificationIcon.WAIT_TIME -> Icons.Default.AccessTime
    NotificationIcon.QUIET_HOURS -> Icons.Default.MenuBook
    NotificationIcon.EMPTY_ROOM -> Icons.Default.AutoAwesome
    NotificationIcon.FORECAST -> Icons.Default.Bolt
}