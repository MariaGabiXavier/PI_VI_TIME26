package com.example.flowcampus.ui.screens.home

import androidx.compose.ui.graphics.Color
import com.example.flowcampus.ui.theme.AppAmber
import com.example.flowcampus.ui.theme.AppBackground
import com.example.flowcampus.ui.theme.AppBlue
import com.example.flowcampus.ui.theme.AppGrayText
import com.example.flowcampus.ui.theme.AppGreen
import com.example.flowcampus.ui.theme.AppNavy
import com.example.flowcampus.ui.theme.AppRed
import java.time.LocalTime
import kotlin.math.roundToInt

val BackgroundColor = AppBackground
val Blue = AppBlue
val DarkText = AppNavy
val GrayText = AppGrayText
val BorderGray = Color(0xFF8D929B)

val HighRed = Color(0xFFFFE9E9)
val HighRedText = AppRed

val MediumOrange = Color(0xFFFFF2DD)
val MediumOrangeText = AppAmber

val LowGreen = Color(0xFFE4F8EC)
val LowGreenText = AppGreen

data class CampusPlace(
    val name: String,
    val people: Int,
    val minutes: Int,
    val status: String,
    val iconType: PlaceIcon,
    val favorite: Boolean,
    val capacity: Int = 40
)

enum class PlaceIcon {
    RESTAURANT,
    LIBRARY,
    BUS,
    LAB,
    STUDY,
    CLASSROOM
}

data class PlaceDetailMock(
    val people: Int,
    val waitingTime: Int,
    val peakTime: String,
    val capacity: Int,
    val recommendationPeople: Int,
    val recommendationTime: String,
    val hourlyPeople: List<Int> = emptyList(),
    val hours: List<Int> = emptyList(),
    val minutesUntilRecommendation: Int = 0
)

private data class SimulationProfile(
    val capacity: Int,
    val baseline: Int,
    val peakHour: Int,
    val peakPeople: Int
)

private fun profileFor(place: CampusPlace): SimulationProfile {
    val name = place.name.lowercase()
    val capacity = place.capacity.coerceAtLeast(1)

    return when {
        "refeit" in name || "cafeter" in name ->
            SimulationProfile(capacity, 35, 12, (capacity * 0.80).roundToInt().coerceAtLeast(1))

        "biblioteca" in name ->
            SimulationProfile(capacity, 18, 14, (capacity * 0.71).roundToInt().coerceAtLeast(1))

        "ônibus" in name || "onibus" in name ->
            SimulationProfile(capacity, 12, 13, (capacity * 0.74).roundToInt().coerceAtLeast(1))

        "a2" in name ->
            SimulationProfile(capacity, 8, 10, (capacity * 0.72).roundToInt().coerceAtLeast(1))

        "a4" in name ->
            SimulationProfile(capacity, 6, 15, (capacity * 0.65).roundToInt().coerceAtLeast(1))

        "estudo" in name ->
            SimulationProfile(capacity, 15, 16, (capacity * 0.78).roundToInt().coerceAtLeast(1))

        else ->
            SimulationProfile(capacity, 8, 12, (capacity * 0.72).roundToInt().coerceAtLeast(1))
    }
}

private fun simulatedPeopleAt(
    profile: SimulationProfile,
    hour: Int
): Int {
    val distance = kotlin.math.abs(hour - profile.peakHour)
    val peakFactor = (1.0 - distance * 0.17).coerceIn(0.18, 1.0)
    val offHoursFactor = if (hour < 8 || hour >= 22) 0.12 else 1.0

    return (
        profile.baseline +
            (profile.peakPeople - profile.baseline) *
            peakFactor * offHoursFactor
        ).roundToInt().coerceIn(0, profile.capacity)
}

fun occupancyStatus(people: Int, capacity: Int): String {
    if (capacity <= 0) return "Baixo"

    val ratio = people.toDouble() / capacity

    return when {
        ratio >= 0.70 -> "Alto"
        ratio >= 0.40 -> "Médio"
        else -> "Baixo"
    }
}

fun simulatePlace(
    place: CampusPlace,
    now: LocalTime = LocalTime.now()
): CampusPlace {
    val profile = profileFor(place)
    val people = simulatedPeopleAt(profile, now.hour)

    val futurePeople = (now.hour..minOf(now.hour + 3, 23))
        .map { simulatedPeopleAt(profile, it) }

    val minutesToLowOccupancy = futurePeople
        .indexOfFirst { it <= profile.capacity * 0.40 }
        .let { index -> if (index < 0) 0 else index * 60 }

    return place.copy(
        people = people,
        minutes = minutesToLowOccupancy.coerceIn(0, 180),
        status = occupancyStatus(people, profile.capacity),
        capacity = profile.capacity
    )
}


fun getMockDetail(
    place: CampusPlace,
    now: LocalTime = LocalTime.now()
): PlaceDetailMock {
    val profile = profileFor(place)

    // Dados do gráfico: ocupação simulada a cada duas horas.
    val hours = (8..22 step 2).toList()
    val values = hours.map { hour ->
        simulatedPeopleAt(profile, hour)
    }

    // Maior ocupação entre os horários apresentados no gráfico.
    val peakIndex = values.indices.maxByOrNull { values[it] }
    val peakHour = peakIndex?.let { hours[it] } ?: profile.peakHour

    // Procura o próximo horário com ocupação baixa.
    val futureHours = ((now.hour + 1)..23).toList()

    val lowOccupancyHour = futureHours.firstOrNull { hour ->
        simulatedPeopleAt(profile, hour) <= profile.capacity * 0.40
    }

    // Se não houver horário de baixa ocupação, escolhe o menos movimentado
    // entre os horários futuros, em vez de retornar zero indevidamente.
    val recommendationHour = lowOccupancyHour
        ?: futureHours.minByOrNull { hour ->
            simulatedPeopleAt(profile, hour)
        }
        ?: now.hour

    val recommendationPeople =
        simulatedPeopleAt(profile, recommendationHour)

    val minutesUntil = if (recommendationHour == now.hour) {
        0
    } else {
        (
            (recommendationHour - now.hour) * 60 - now.minute
        ).coerceAtLeast(1)
    }

    val waitingTime = minutesUntil

    return PlaceDetailMock(
        people = place.people,
        waitingTime = waitingTime,
        peakTime = String.format("%02d:00", peakHour),
        capacity = profile.capacity,
        recommendationPeople = recommendationPeople,
        recommendationTime = String.format(
            "%02d:00",
            recommendationHour
        ),
        hourlyPeople = values,
        hours = hours,
        minutesUntilRecommendation = minutesUntil
    )
}


enum class AppScreen {
    HOME,
    ANALYSES,
    ALERTS,
    PROFILE,
    ALL_PLACES
}