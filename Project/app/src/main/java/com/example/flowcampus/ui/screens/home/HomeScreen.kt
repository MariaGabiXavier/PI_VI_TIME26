
package com.example.flowcampus.ui.screens.home

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.flowcampus.ui.screens.analytics.AnalyticsScreen
import com.example.flowcampus.ui.screens.auth.AuthSession
import com.example.flowcampus.ui.screens.auth.UserRole
import com.example.flowcampus.ui.screens.notifications.NotificationsScreen

@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    val userProfile by AuthSession.user.collectAsState()
    val isStaff = userProfile?.role == UserRole.STAFF

    var showAddEnvironment by remember {
        mutableStateOf(false)
    }

    val environmentRepository = remember {
        EnvironmentRepository()
    }

    var places by remember {
        mutableStateOf<List<CampusPlace>>(emptyList())
    }

    DisposableEffect(environmentRepository) {
        val listener = environmentRepository.listenEnvironments(
            onUpdate = { updatedPlaces ->
                places = updatedPlaces
            },
            onError = { error ->
                Log.e(
                    "HomeScreen",
                    "Erro ao carregar ambientes do Firestore",
                    error
                )
            }
        )

        onDispose {
            listener.remove()
        }
    }

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
                AnalyticsScreen()
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
            if (showAddEnvironment) {
                AddEnvironmentScreen(
                    onBack = {
                        showAddEnvironment = false
                    },
                    
                    onSave = { name, category, building, capacity, operatingHours, description ->
                        val uid = userProfile?.uid

                        if (uid.isNullOrBlank()) {
                            Result.failure<Unit>(
                                IllegalStateException(
                                    "Não foi possível identificar o usuário conectado."
                                )
                            )
                        } else {
                            environmentRepository.addEnvironment(
                                name = name,
                                category = category,
                                building = building,
                                capacity = capacity,
                                operatingHours = operatingHours,
                                description = description,
                                createdBy = uid
                            )
                        }
                    }

                )
            } else {
                MainScaffold(
                    selectedScreen = AppScreen.PROFILE,
                    onScreenChange = {
                        currentScreen = it
                    }
                ) {
                    SimpleSectionScreen(
                        title = "Perfil",
                        description = "Aqui ficarão as informações do usuário.",
                        onLogout = onLogout,
                        isStaff = isStaff,
                        onAddEnvironment = {
                            showAddEnvironment = true
                        }
                    )
                }
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