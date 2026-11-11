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
import com.example.flowcampus.ai.AiEnvironmentForecast
import java.time.Duration
import java.time.OffsetDateTime

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
    val capacity: Int = 40,
    val firestoreId: String = ""
)

enum class PlaceIcon {
    RESTAURANT,
    LIBRARY,
    BUS,
    LAB,
    STUDY,
    CLASSROOM
}


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


data class PlaceDetailMock(
    val people: Int,
    val waitingTime: Int,
    val peakTime: String,
    val capacity: Int,
    val recommendationPeople: Int,
    val recommendationTime: String,
    val hourlyPeople: List<Int> = emptyList(),
    val hours: List<Int> = emptyList(),
    val minutesUntilRecommendation: Int = 0,
    val recommendationStatus: String = "Sem previsão"
)

private fun parseForecastTime(value: String): OffsetDateTime? {
    return try {
        OffsetDateTime.parse(value)
    } catch (e: Exception) {
        null
    }
}

fun getPlaceDetail(
    place: CampusPlace,
    forecast: AiEnvironmentForecast?,
    now: OffsetDateTime = OffsetDateTime.now()
): PlaceDetailMock {

    val capacity = place.capacity.coerceAtLeast(1)

    // Sem previsão disponível: não inventa dados.
    if (forecast == null || forecast.points.isEmpty()) {
        return PlaceDetailMock(
            people = place.people,
            waitingTime = 0,
            peakTime = "--",
            capacity = capacity,
            recommendationPeople = 0,
            recommendationTime = "--"
        )
    }

    val validPoints = forecast.points.mapNotNull { point ->
        val timestamp = parseForecastTime(point.timestamp)
        if (timestamp != null) {
            point to timestamp
        } else {
            null
        }
    }

    if (validPoints.isEmpty()) {
        return PlaceDetailMock(
            people = place.people,
            waitingTime = 0,
            peakTime = "--",
            capacity = capacity,
            recommendationPeople = 0,
            recommendationTime = "--"
        )
    }

    // Horários previstos para hoje.
    val todayPoints = validPoints
        .filter { it.second.toLocalDate() == now.toLocalDate() }
        .sortedBy { it.second }

    // Gráfico: previsão a cada duas horas, das 08h às 22h.
    val chartPoints = todayPoints.filter { (_, time) ->
        time.hour in 8..22 && time.hour % 2 == 0
    }

    val hours = chartPoints.map { it.second.hour }
    val hourlyPeople = chartPoints.map { it.first.predictedPeople }

    // Horário de maior ocupação previsto pela IA.
    val peakPoint = validPoints.maxByOrNull {
        it.first.predictedOccupancyPercent
    }

    val peakTime = peakPoint?.second?.let {
        "%02d:%02d".format(it.hour, it.minute)
    } ?: "--"

    // Melhor horário futuro previsto pela IA.
    val bestPoint = validPoints
        .filter { it.second.isAfter(now) }
        .minByOrNull { it.first.predictedOccupancyPercent }

    val recommendationTime = bestPoint?.second?.let {
        "%02d:%02d".format(it.hour, it.minute)
    } ?: "--"

    val recommendationPeople = bestPoint?.first?.predictedPeople ?: 0

    val recommendationStatus = bestPoint?.first?.predictedOccupancyPercent?.let {
        when {
            it >= 70 -> "Alto"
            it >= 40 -> "Médio"
            else -> "Baixo"
        }
    } ?: "Sem previsão"

    // Tempo até o melhor horário.
    val waitingTime = bestPoint?.second?.let {
        Duration.between(now, it).toMinutes()
            .coerceAtLeast(0)
            .toInt()
    } ?: 0

    return PlaceDetailMock(
        people = place.people,
        waitingTime = waitingTime,
        peakTime = peakTime,
        capacity = capacity,
        recommendationPeople = recommendationPeople,
        recommendationTime = recommendationTime,
        hourlyPeople = hourlyPeople,
        hours = hours,
        minutesUntilRecommendation = waitingTime,
        recommendationStatus = recommendationStatus
    )
}


enum class AppScreen {
    HOME,
    ANALYSES,
    ALERTS,
    PROFILE,
    ALL_PLACES
}