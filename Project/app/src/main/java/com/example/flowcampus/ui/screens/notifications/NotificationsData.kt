package com.example.flowcampus.ui.screens.notifications

import androidx.compose.ui.graphics.Color
import com.example.flowcampus.ui.screens.home.HighRed
import com.example.flowcampus.ui.screens.home.HighRedText
import com.example.flowcampus.ui.screens.home.LowGreen
import com.example.flowcampus.ui.screens.home.LowGreenText
import com.example.flowcampus.ui.screens.home.MediumOrange
import com.example.flowcampus.ui.screens.home.MediumOrangeText
import java.util.Date
import java.util.concurrent.TimeUnit

val InfoBlue = Color(0xFFDCEAFE)
val InfoBlueText = Color(0xFF2F6FE0)

val InfoPurple = Color(0xFFEAE1FB)
val InfoPurpleText = Color(0xFF7C4FE0)

enum class NotificationCategory {
    ALERT,
    UPDATE
}

enum class NotificationIcon {
    OCCUPANCY,
    WAIT_TIME,
    QUIET_HOURS,
    EMPTY_ROOM,
    FORECAST
}
enum class NotificationTone {
    RED,
    ORANGE,
    BLUE,
    GREEN,
    PURPLE
}

data class NotificationItem(
    val id: String,
    val icon: NotificationIcon,
    val iconBackground: Color,
    val iconTint: Color,
    val title: String,
    val description: String,
    val time: String,
    val category: NotificationCategory,
    val createdAt: Date
)

fun toneBackground(tone: NotificationTone): Color {

    return when (tone) {
        NotificationTone.RED -> HighRed
        NotificationTone.ORANGE -> MediumOrange
        NotificationTone.BLUE -> InfoBlue
        NotificationTone.GREEN -> LowGreen
        NotificationTone.PURPLE -> InfoPurple
    }
}

fun toneTint(tone: NotificationTone): Color {

    return when (tone) {
        NotificationTone.RED -> HighRedText
        NotificationTone.ORANGE -> MediumOrangeText
        NotificationTone.BLUE -> InfoBlueText
        NotificationTone.GREEN -> LowGreenText
        NotificationTone.PURPLE -> InfoPurpleText
    }
}

fun formatTimeAgo(date: Date): String {

    val minutes = TimeUnit.MILLISECONDS.toMinutes(
        System.currentTimeMillis() - date.time
    )

    return when {
        minutes < 1 -> "Agora"
        minutes < 60 -> "$minutes min atrás"
        minutes < 120 -> "1 hora atrás"
        minutes < 24 * 60 -> "${minutes / 60} horas atrás"
        minutes < 48 * 60 -> "Ontem"
        else -> "${minutes / (24 * 60)} dias atrás"
    }
}