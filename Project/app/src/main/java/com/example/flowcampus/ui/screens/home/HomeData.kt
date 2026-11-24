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
    val firestoreId: String = "",
    val building: String = "",
    val operatingHours: String = "",
    val description: String = "",
    val favoriteId: String = ""
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
    val recommendationStatus: String = "Sem previsão",
    val recommendationIsObserved: Boolean = false
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
    now: OffsetDateTime = OffsetDateTime.now(ZoneId.of("America/Sao_Paulo"))
): PlaceDetailMock {
    val capacity = place.capacity.coerceAtLeast(1)
    // O Firestore e o serviço de IA usam o horário de Brasília.
    // Não depender do fuso configurado no emulador/celular.
    val zone = ZoneId.of("America/Sao_Paulo")
    val today = now.atZoneSameInstant(zone).toLocalDate()
    val nowLocal = now.atZoneSameInstant(zone)
    val currentHour = nowLocal.hour
    val chartHours = (7..22).toList()

    // A API gera a primeira previsão na próxima hora cheia.
    // A hora atual fica no lado dos dados observados (câmera ou seed).
    val firstForecastHour = currentHour + 1

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

    // O timestamp da API já identifica o início da hora prevista:
    // 17:00 representa a barra 17h-18h. Não subtrair uma hora.
    val forecastByHour = forecast?.points.orEmpty().mapNotNull { point ->
        val parsed = parseForecastTime(point.timestamp) ?: return@mapNotNull null
        val forecastTime = parsed.atZoneSameInstant(zone)
        if (
            forecastTime.toLocalDate() == today &&
            forecastTime.hour in 7..22 &&
            forecastTime.hour >= firstForecastHour
        ) {
            forecastTime.hour to (point to forecastTime)
        } else null
    }.groupBy({ it.first }, { it.second }).mapValues { (_, points) ->
        points.maxByOrNull { it.second.minute }
    }

    val values = mutableListOf<Int>()
    val hasData = mutableListOf<Boolean>()
    val isForecast = mutableListOf<Boolean>()

    chartHours.forEach { hour ->
        if (hour < firstForecastHour) {
            // Intervalo já iniciado/concluído: somente leitura real ou simulada.
            // Sem leitura, permanece vazio; não substituímos por previsão.
            val reading = actualByHour[hour]
            values += reading?.people?.coerceIn(0, capacity) ?: 0
            hasData += reading != null
            isForecast += false
        } else {
            // Intervalos ainda não iniciados: previsão da IA, se disponível.
            val prediction = forecastByHour[hour]?.first
            values += prediction?.predictedPeople?.coerceIn(0, capacity) ?: 0
            hasData += prediction != null
            isForecast += prediction != null
        }
    }

    // O pico é sempre o maior valor previsto pela IA para hoje. Em caso de
    // empate, maxByOrNull mantém o primeiro horário (o mais cedo).
    val todayForecastPoints = forecastByHour.values.mapNotNull { it }
    val peakPoint = todayForecastPoints.maxByOrNull {
        it.first.predictedPeople
    }
    val peakTime = peakPoint?.second?.let { "%02d:00".format(it.hour) } ?: "--"

    // Para recomendar, comparamos a ocupação observada na hora atual com
    // todas as previsões futuras de hoje. Se agora já for tão bom quanto
    // qualquer horário futuro, recomendamos ir agora (sem espera).
    val currentReading = actualByHour[currentHour]
    val currentPeople = currentReading?.people?.coerceIn(0, capacity)
    val bestFuturePoint = todayForecastPoints
        .filter { it.second.toInstant().isAfter(nowLocal.toInstant()) }
        .minByOrNull { it.first.predictedPeople }

    // Se não existe leitura na hora atual (ambiente sem câmera/sem registro),
    // não inventamos uma ocupação atual. Quando a melhor faixa prevista fica
    // antes ou no pico e ainda estamos antes do pico, não há motivo previsto
    // para esperar: a recomendação é ir agora. Se a melhor faixa fica depois
    // do pico, recomendamos esse horário futuro.
    val bestIsBeforeOrAtPeak = bestFuturePoint != null && peakPoint != null &&
        !bestFuturePoint.second.isAfter(peakPoint.second)
    val currentIsBeforePeak = peakPoint != null &&
        nowLocal.toLocalTime().isBefore(peakPoint.second.toLocalTime())
    val forecastSuggestsGoNow = currentPeople == null &&
        currentIsBeforePeak && bestIsBeforeOrAtPeak

    val recommendNow = (currentPeople != null &&
        (bestFuturePoint == null || currentPeople <= bestFuturePoint.first.predictedPeople)) ||
        forecastSuggestsGoNow

    val recommendationPeople: Int
    val recommendationPercent: Int?
    val recommendationTime: String
    val waitingTime: Int

    var recommendationIsObserved = false
    if (recommendNow) {
        if (currentPeople != null) {
            recommendationPeople = currentPeople
            recommendationPercent = ((currentPeople.toDouble() / capacity) * 100.0).roundToInt()
            recommendationIsObserved = true
        } else {
            recommendationPeople = bestFuturePoint?.first?.predictedPeople ?: 0
            recommendationPercent = bestFuturePoint?.first?.predictedOccupancyPercent
        }
        recommendationTime = "Agora"
        waitingTime = 0
    } else if (bestFuturePoint != null) {
        recommendationPeople = bestFuturePoint.first.predictedPeople
        recommendationPercent = bestFuturePoint.first.predictedOccupancyPercent
        recommendationTime = "%02d:00".format(bestFuturePoint.second.hour)
        waitingTime = Duration.between(
            nowLocal.toInstant(),
            bestFuturePoint.second.toInstant()
        ).toMinutes().coerceAtLeast(0).toInt()
    } else {
        recommendationPeople = 0
        recommendationPercent = null
        recommendationTime = "--"
        waitingTime = 0
    }

    val recommendationStatus = recommendationPercent?.let {
        when { it >= 70 -> "Alto"; it >= 40 -> "Médio"; else -> "Baixo" }
    } ?: "Sem previsão"

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
        recommendationStatus = recommendationStatus,
        recommendationIsObserved = recommendationIsObserved
    )
}


enum class AppScreen {
    HOME,
    ANALYSES,
    ALERTS,
    PROFILE,
    ALL_PLACES
}
