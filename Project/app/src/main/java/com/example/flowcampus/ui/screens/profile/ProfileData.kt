package com.example.flowcampus.ui.screens.profile

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * O que aparece no Perfil além dos dados de cadastro.
 * Fica no próprio documento `users/{uid}` (ver [ProfileRepository]).
 */
data class UserActivity(
    val favoriteIds: Set<String> = emptySet(),
    val visitsThisMonth: Int = 0,
    val timeSavedMinutes: Int = 0,
    val predictionsUsed: Int = 0
)

/** "2026-10": chave do mês no contador de visitas. */
fun currentMonthKey(): String {

    return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
}

/** 45 -> "45 min", 150 -> "2,5 h", 120 -> "2 h". */
fun formatTimeSaved(minutes: Int): String {

    if (minutes < 60) {
        return "$minutes min"
    }

    val hours = DecimalFormat(
        "0.#",
        DecimalFormatSymbols(Locale.forLanguageTag("pt-BR"))
    ).format(minutes / 60.0)

    return "$hours h"
}
