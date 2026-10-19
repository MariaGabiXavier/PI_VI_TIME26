package com.example.flowcampus.ui.screens.home

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.flowcampus.ui.screens.analytics.AnalyticsScreen
import com.example.flowcampus.ui.screens.auth.AuthSession
import com.example.flowcampus.ui.screens.notifications.NotificationsScreen
import com.example.flowcampus.ui.screens.notifications.rememberUnreadNotificationsCount
import com.example.flowcampus.ui.screens.notifications.toEnvironmentId
import androidx.compose.runtime.LaunchedEffect
import com.example.flowcampus.ai.AiForecast
import com.example.flowcampus.ai.AiRepository
import com.example.flowcampus.ui.screens.notifications.SetupPushNotifications
import com.example.flowcampus.ui.screens.profile.EditProfileScreen
import com.example.flowcampus.ui.screens.profile.ProfileRepository
import com.example.flowcampus.ui.screens.profile.ProfileScreen
import com.example.flowcampus.ui.screens.profile.UserActivity

@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    SetupPushNotifications()
    val userProfile by AuthSession.user.collectAsState()
    val userId = userProfile?.uid

    var showAddEnvironment by remember {
        mutableStateOf(false)
    }

    var showEditProfile by remember {
        mutableStateOf(false)
    }

    val environmentRepository = remember {
        EnvironmentRepository()
    }

    val aiRepository = remember {
        AiRepository()
    }

    val profileRepository = remember {
        ProfileRepository()
    }

    var places by remember {
        mutableStateOf<List<CampusPlace>>(emptyList())
    }

    var activity by remember {
        mutableStateOf(UserActivity())
    }

    var aiForecast by remember {
        mutableStateOf<AiForecast?>(null)
    }

    LaunchedEffect(aiRepository) {
        aiRepository.fetchForecast()
            .onSuccess { forecast ->
                aiForecast = forecast
            }
            .onFailure { error ->
                Log.w(
                    "HomeScreen",
                    "Não foi possível carregar as previsões da IA",
                    error
                )
            }
    }

    
    val placesWithForecast = places.map { place ->
        // Gera dados simulados para ambientes sem previsão da IA.
        val simulatedPlace = simulatePlace(place)

        val prediction = aiForecast
            ?.forEnvironment(place.name)
            ?.points
            ?.firstOrNull()

        if (prediction == null) {
            // Sem previsão disponível: usa a simulação.
            simulatedPlace
        } else {
            // Com previsão: usa a quantidade prevista pela IA.
            val people = prediction.predictedPeople
                .coerceIn(0, simulatedPlace.capacity)

            simulatedPlace.copy(
                people = people,
                status = occupancyStatus(
                    people = people,
                    capacity = simulatedPlace.capacity
                )
            )
        }
    }

    val unreadCount = rememberUnreadNotificationsCount()

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

    // Favoritos e números do Perfil ficam no documento do usuário
    DisposableEffect(userId) {
        activity = UserActivity()

        val listener = userId?.let { id ->
            profileRepository.listenActivity(
                userId = id,
                onUpdate = { updatedActivity ->
                    activity = updatedActivity
                },
                onError = { error ->
                    Log.e(
                        "HomeScreen",
                        "Erro ao carregar dados do perfil",
                        error
                    )
                }
            )
        }

        onDispose {
            listener?.remove()
        }
    }

    // Favoritos por cima da lista que já tem a previsão da IA
    val placesWithFavorites = placesWithForecast.map { place ->
        place.copy(favorite = place.name.toEnvironmentId() in activity.favoriteIds)
    }

    val onFavoriteChange: (CampusPlace, Boolean) -> Unit = { place, favorite ->
        userId?.let { id ->
            profileRepository.setFavorite(
                userId = id,
                environmentId = place.name.toEnvironmentId(),
                favorite = favorite
            )
        }
    }

    var currentScreen by remember {
        mutableStateOf(AppScreen.HOME)
    }

    var selectedPlace by remember {
        mutableStateOf<CampusPlace?>(null)
    }

    // Abrir o status de um local conta como visita no Perfil
    LaunchedEffect(selectedPlace) {
        if (selectedPlace != null && userId != null) {
            profileRepository.registerVisit(userId)
        }
    }

    BackHandler(enabled = selectedPlace != null) {
        selectedPlace = null
    }

    when (currentScreen) {

        AppScreen.HOME -> {
            MainScaffold(
                selectedScreen = AppScreen.HOME,
                onScreenChange = {
                    currentScreen = it
                },
                alertsBadgeCount = unreadCount
            ) {
                HomeContent(
                    places = placesWithFavorites,
                    onPlaceClick = { place ->
                        selectedPlace = place
                    },
                    onSeeAllClick = {
                        currentScreen = AppScreen.ALL_PLACES
                    },
                    onFavoriteChange = onFavoriteChange
                )
            }
        }

        AppScreen.ANALYSES -> {
            MainScaffold(
                selectedScreen = AppScreen.ANALYSES,
                onScreenChange = {
                    currentScreen = it
                },
                alertsBadgeCount = unreadCount
            ) {
                AnalyticsScreen()
            }
        }

        AppScreen.ALERTS -> {
            MainScaffold(
                selectedScreen = AppScreen.ALERTS,
                onScreenChange = {
                    currentScreen = it
                },
                alertsBadgeCount = unreadCount
            ) {
                NotificationsScreen(
                    onNotificationClick = { environmentId ->
                        places.firstOrNull { it.name.toEnvironmentId() == environmentId }
                            ?.let { selectedPlace = it }
                    }
                )
            }
        }

        AppScreen.PROFILE -> {
            val user = userProfile

            when {
                showAddEnvironment -> {
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
                }

                showEditProfile && user != null -> {
                    EditProfileScreen(
                        user = user,
                        onBack = {
                            showEditProfile = false
                        },
                        onSave = { name, ra ->
                            profileRepository.updateProfile(
                                profile = user,
                                name = name,
                                ra = ra
                            )
                        }
                    )
                }

                else -> {
                    MainScaffold(
                        selectedScreen = AppScreen.PROFILE,
                        onScreenChange = {
                            currentScreen = it
                        },
                        alertsBadgeCount = unreadCount
                    ) {
                        ProfileScreen(
                            user = user,
                            activity = activity,
                            favoritePlaces = placesWithFavorites.filter { it.favorite },
                            onPlaceClick = { place ->
                                selectedPlace = place
                            },
                            onEditProfile = {
                                showEditProfile = true
                            },
                            onAddEnvironment = {
                                showAddEnvironment = true
                            },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }

        AppScreen.ALL_PLACES -> {
            AllPlacesScreen(
                places = placesWithFavorites,
                onBack = {
                    currentScreen = AppScreen.HOME
                },
                onPlaceClick = { place ->
                    selectedPlace = place
                },
                onFavoriteChange = onFavoriteChange
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