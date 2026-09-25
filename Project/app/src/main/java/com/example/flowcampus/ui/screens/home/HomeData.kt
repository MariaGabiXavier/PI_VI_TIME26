package com.example.flowcampus.ui.screens.home

import androidx.compose.ui.graphics.Color
import com.example.flowcampus.ui.theme.AppAmber
import com.example.flowcampus.ui.theme.AppBackground
import com.example.flowcampus.ui.theme.AppBlue
import com.example.flowcampus.ui.theme.AppGrayText
import com.example.flowcampus.ui.theme.AppGreen
import com.example.flowcampus.ui.theme.AppNavy
import com.example.flowcampus.ui.theme.AppRed

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
    val favorite: Boolean
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
    val recommendationTime: String
)

fun getMockDetail(
    place: CampusPlace
): PlaceDetailMock {

    return when (place.name) {

        "Refeitório" -> PlaceDetailMock(
            people = 78,
            waitingTime = 15,
            peakTime = "12:30 PM",
            capacity = 120,
            recommendationPeople = 25,
            recommendationTime = "14:30 PM"
        )

        "Biblioteca" -> PlaceDetailMock(
            people = 34,
            waitingTime = 3,
            peakTime = "14:00 PM",
            capacity = 80,
            recommendationPeople = 15,
            recommendationTime = "15:30 PM"
        )

        "Ponto de ônibus" -> PlaceDetailMock(
            people = 52,
            waitingTime = 8,
            peakTime = "13:00 PM",
            capacity = 70,
            recommendationPeople = 30,
            recommendationTime = "14:00 PM"
        )

        "Lab A2" -> PlaceDetailMock(
            people = 21,
            waitingTime = 2,
            peakTime = "10:30 AM",
            capacity = 40,
            recommendationPeople = 10,
            recommendationTime = "11:30 AM"
        )

        "Sala Estudo - 7A" -> PlaceDetailMock(
            people = 63,
            waitingTime = 10,
            peakTime = "16:00 PM",
            capacity = 80,
            recommendationPeople = 35,
            recommendationTime = "17:30 PM"
        )

        else -> PlaceDetailMock(
            people = 15,
            waitingTime = 1,
            peakTime = "12:00 PM",
            capacity = 40,
            recommendationPeople = 8,
            recommendationTime = "13:30 PM"
        )
    }
}

enum class AppScreen {
    HOME,
    ANALYSES,
    ALERTS,
    PROFILE,
    ALL_PLACES
}