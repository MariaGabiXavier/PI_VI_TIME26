package com.example.flowcampus.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.flowcampus.ui.screens.notifications.NotificationsScreen

@Composable
fun HomeScreen() {

    val places = listOf(

        CampusPlace(
            name = "Refeitório",
            people = 78,
            minutes = 15,
            status = "Alto",
            iconType = PlaceIcon.RESTAURANT,
            favorite = true
        ),

        CampusPlace(
            name = "Biblioteca",
            people = 34,
            minutes = 3,
            status = "Baixo",
            iconType = PlaceIcon.LIBRARY,
            favorite = true
        ),

        CampusPlace(
            name = "Ponto de ônibus",
            people = 52,
            minutes = 8,
            status = "Médio",
            iconType = PlaceIcon.BUS,
            favorite = true
        ),

        CampusPlace(
            name = "Lab A2",
            people = 21,
            minutes = 2,
            status = "Baixo",
            iconType = PlaceIcon.LAB,
            favorite = false
        ),

        CampusPlace(
            name = "Sala Estudo - 7A",
            people = 63,
            minutes = 10,
            status = "Médio",
            iconType = PlaceIcon.STUDY,
            favorite = false
        ),

        CampusPlace(
            name = "Sala Aula - 112",
            people = 15,
            minutes = 1,
            status = "Baixo",
            iconType = PlaceIcon.CLASSROOM,
            favorite = false
        )
    )

    var currentScreen by remember {
        mutableStateOf(AppScreen.HOME)
    }

    var selectedPlace by remember {
        mutableStateOf<CampusPlace?>(null)
    }

    when (currentScreen) {

        AppScreen.HOME -> {

            MainScaffold(
                selectedScreen = AppScreen.HOME,
                onScreenChange = {
                    currentScreen = it
                }
            ) {

                HomeContent(
                    places = places,
                    onPlaceClick = { place ->
                        selectedPlace = place
                    },
                    onSeeAllClick = {
                        currentScreen = AppScreen.ALL_PLACES
                    }
                )
            }
        }

        AppScreen.ANALYSES -> {

            MainScaffold(
                selectedScreen = AppScreen.ANALYSES,
                onScreenChange = {
                    currentScreen = it
                }
            ) {

                SimpleSectionScreen(
                    title = "Análises",
                    description = "Aqui ficarão as análises e informações sobre o campus."
                )
            }
        }

        AppScreen.ALERTS -> {

            MainScaffold(
                selectedScreen = AppScreen.ALERTS,
                onScreenChange = {
                    currentScreen = it
                }
            ) {

                NotificationsScreen()
            }
        }

        AppScreen.PROFILE -> {

            MainScaffold(
                selectedScreen = AppScreen.PROFILE,
                onScreenChange = {
                    currentScreen = it
                }
            ) {

                SimpleSectionScreen(
                    title = "Perfil",
                    description = "Aqui ficarão as informações do usuário."
                )
            }
        }

        AppScreen.ALL_PLACES -> {

            AllPlacesScreen(
                places = places,
                onBack = {
                    currentScreen = AppScreen.HOME
                },
                onPlaceClick = { place ->
                    selectedPlace = place
                }
            )
        }
    }

    selectedPlace?.let { place ->

        PlaceDetailScreen(
            place = place,
            onBack = {
                selectedPlace = null
            }
        )
    }
}