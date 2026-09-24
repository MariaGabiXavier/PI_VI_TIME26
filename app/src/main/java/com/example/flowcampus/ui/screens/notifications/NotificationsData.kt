package com.example.flowcampus.ui.screens.notifications

import androidx.compose.ui.graphics.Color
import com.example.flowcampus.ui.screens.home.HighRed
import com.example.flowcampus.ui.screens.home.HighRedText
import com.example.flowcampus.ui.screens.home.LowGreen
import com.example.flowcampus.ui.screens.home.LowGreenText
import com.example.flowcampus.ui.screens.home.MediumOrange
import com.example.flowcampus.ui.screens.home.MediumOrangeText

// Cores extras que o Home ainda não tem, seguindo o mesmo padrão (fundo claro + texto forte)
val InfoBlue = Color(0xFFDCEAFE)
val InfoBlueText = Color(0xFF2F6FE0)

val InfoPurple = Color(0xFFEAE1FB)
val InfoPurpleText = Color(0xFF7C4FE0)

enum class NotificationCategory {
    ALERT,
    UPDATE
}

data class NotificationItem(
    val icon: NotificationIcon,
    val iconBackground: Color,
    val iconTint: Color,
    val title: String,
    val description: String,
    val time: String,
    val category: NotificationCategory
)

enum class NotificationIcon {
    OCCUPANCY,
    WAIT_TIME,
    QUIET_HOURS,
    EMPTY_ROOM,
    FORECAST
}

fun getMockNotifications(): List<NotificationItem> {

    return listOf(

        NotificationItem(
            icon = NotificationIcon.OCCUPANCY,
            iconBackground = HighRed,
            iconTint = HighRedText,
            title = "Refeitório Cheio",
            description = "Capacidade máxima atingida! Considere ir em outro ambiente/horário.",
            time = "2 min atrás",
            category = NotificationCategory.ALERT
        ),

        NotificationItem(
            icon = NotificationIcon.WAIT_TIME,
            iconBackground = MediumOrange,
            iconTint = MediumOrangeText,
            title = "Fila no Laboratório",
            description = "O tempo de espera está aumentando. Atualmente ~12 min.",
            time = "15 min atrás",
            category = NotificationCategory.ALERT
        ),

        NotificationItem(
            icon = NotificationIcon.QUIET_HOURS,
            iconBackground = InfoBlue,
            iconTint = InfoBlueText,
            title = "Horário Silencioso Iniciado",
            description = "As áreas de estudo silencioso da biblioteca estão ativas. Apenas 12 pessoas no Nível 3.",
            time = "1 hora atrás",
            category = NotificationCategory.UPDATE
        ),

        NotificationItem(
            icon = NotificationIcon.EMPTY_ROOM,
            iconBackground = LowGreen,
            iconTint = LowGreenText,
            title = "Sala de Estudo Vazia",
            description = "Sua sala de estudo favorita está com pouca gente. É o momento perfeito para ir!",
            time = "2 horas atrás",
            category = NotificationCategory.UPDATE
        ),

        NotificationItem(
            icon = NotificationIcon.FORECAST,
            iconBackground = InfoPurple,
            iconTint = InfoPurpleText,
            title = "Previsão de Pico",
            description = "Espera-se que a biblioteca esteja muito cheia hoje às 14h. Planeje sua tarefa para mais cedo.",
            time = "Ontem",
            category = NotificationCategory.UPDATE
        )
    )
}