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
import java.time.ZoneId

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
    val hours: List<String> = emptyList(),
    val chartHasData: List<Boolean> = emptyList(),
    val chartIsForecast: List<Boolean> = emptyList(),
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
    realReadings: List<CameraReading> = emptyList(),
    now: OffsetDateTime = OffsetDateTime.now()
): PlaceDetailMock {
    val capacity = place.capacity.coerceAtLeast(1)
    val zone = ZoneId.systemDefault()
    val today = now.atZoneSameInstant(zone).toLocalDate()
    val currentHour = now.atZoneSameInstant(zone).hour
    val chartHours = (7..22).toList()

    // Cada leitura é agrupada pela hora local em que ocorreu. A câmera tem
    // prioridade sobre registros simulados, quando ambos existem na mesma hora.
    val readingsByHour = realReadings.mapNotNull { reading ->
        val instant = reading.timestamp?.toDate()?.toInstant() ?: return@mapNotNull null
        val local = instant.atZone(zone)
        if (local.toLocalDate() != today || local.hour !in 7..22) null
        else local.hour to reading
    }.groupBy({ it.first }, { it.second })

    val actualByHour = readingsByHour.mapValues { (_, readings) ->
        readings.filter { it.source != "simulated" }.maxByOrNull {
            it.timestamp?.toDate()?.time ?: 0L
        } ?: readings.maxByOrNull { it.timestamp?.toDate()?.time ?: 0L }
    }

    // O serviço pode devolver 48h. Aqui entram somente as previsões de hoje,
    // das 07h às 23h, e apenas a hora atual em diante.
    val forecastByHour = forecast?.points.orEmpty().mapNotNull { point ->
        val parsed = parseForecastTime(point.timestamp) ?: return@mapNotNull null
        val local = parsed.atZoneSameInstant(zone)
        if (local.toLocalDate() == today && local.hour in 7..22 && local.hour >= currentHour) {
            local.hour to (point to local)
        } else null
    }.groupBy({ it.first }, { it.second }).mapValues { (_, points) ->
        points.maxByOrNull { it.second.minute }
    }

    val values = mutableListOf<Int>()
    val hasData = mutableListOf<Boolean>()
    val isForecast = mutableListOf<Boolean>()

    chartHours.forEach { hour ->
        if (hour < currentHour) {
            // Hora encerrada: somente dado real. Sem leitura, barra vazia.
            val reading = actualByHour[hour]
            values += reading?.people?.coerceIn(0, capacity) ?: 0
            hasData += reading != null
            isForecast += false
        } else {
            // Hora atual e futuras: previsão da IA até que a hora termine.
            val prediction = forecastByHour[hour]?.first
            values += prediction?.predictedPeople?.coerceIn(0, capacity) ?: 0
            hasData += prediction != null
            isForecast += prediction != null
        }
    }

    val futurePoints = forecastByHour.values.mapNotNull { it }
    val peakPoint = futurePoints.maxByOrNull { it.first.predictedOccupancyPercent }
    val bestPoint = futurePoints.minByOrNull { it.first.predictedOccupancyPercent }
    val peakTime = peakPoint?.second?.let { "%02d:00".format(it.hour) } ?: "--"
    val recommendationTime = bestPoint?.second?.let { "%02d:00".format(it.hour) } ?: "--"
    val recommendationPeople = bestPoint?.first?.predictedPeople ?: 0
    val recommendationStatus = bestPoint?.first?.predictedOccupancyPercent?.let {
        when { it >= 70 -> "Alto"; it >= 40 -> "Médio"; else -> "Baixo" }
    } ?: "Sem previsão"
    val waitingTime = bestPoint?.second?.let {
        Duration.between(now, it.toOffsetDateTime()).toMinutes().coerceAtLeast(0).toInt()
    } ?: 0

    return PlaceDetailMock(
        people = place.people,
        waitingTime = waitingTime,
        peakTime = peakTime,
        capacity = capacity,
        recommendationPeople = recommendationPeople,
        recommendationTime = recommendationTime,
        hourlyPeople = values,
        hours = (7..23).map { "%02d:00".format(it) },
        chartHasData = hasData,
        chartIsForecast = isForecast,
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