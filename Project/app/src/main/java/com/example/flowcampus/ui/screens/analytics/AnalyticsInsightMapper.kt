package com.example.flowcampus.ui.screens.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.flowcampus.ui.theme.AppAmber
import com.example.flowcampus.ui.theme.AppBlue
import com.example.flowcampus.ui.theme.AppGreen
import com.example.flowcampus.ui.theme.AppRed

/**
 * Converte uma frase de insight (vinda do Firestore OU da IA) no modelo da tela.
 *
 *   text      -> frase inteira
 *   highlight -> trecho da frase que aparece em negrito colorido
 *   icon      -> BOOK | CLOCK | HOME | BUS | FOOD | BULB (outro = lâmpada)
 *   tone      -> BLUE | RED | GREEN | AMBER
 */
fun buildAnalyticsInsight(
    text: String,
    highlight: String,
    icon: String?,
    tone: String?
): AnalyticsInsight {

    val color = toneColor(tone)
    val start = if (highlight.isBlank()) -1 else text.indexOf(highlight)

    return if (start >= 0) {
        AnalyticsInsight(
            icon = insightIcon(icon),
            iconColor = color,
            textBefore = text.substring(0, start),
            highlight = highlight,
            highlightColor = color,
            textAfter = text.substring(start + highlight.length)
        )
    } else {
        AnalyticsInsight(
            icon = insightIcon(icon),
            iconColor = color,
            textBefore = text,
            highlight = "",
            highlightColor = color,
            textAfter = ""
        )
    }
}

private fun toneColor(tone: String?): Color {

    return when (tone) {
        "RED" -> AppRed
        "GREEN" -> AppGreen
        "AMBER" -> AppAmber
        else -> AppBlue
    }
}

private fun insightIcon(icon: String?): ImageVector {

    return when (icon) {
        "BOOK" -> Icons.Default.MenuBook
        "CLOCK" -> Icons.Default.AccessTime
        "HOME" -> Icons.Default.Home
        "BUS" -> Icons.Default.DirectionsBus
        "FOOD" -> Icons.Default.Restaurant
        else -> Icons.Default.Lightbulb
    }
}
