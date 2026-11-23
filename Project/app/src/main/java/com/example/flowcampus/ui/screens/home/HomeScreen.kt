package com.example.flowcampus.ui.screens.home

import kotlinx.coroutines.delay
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
import com.example.flowcampus.ui.screens.auth.UserRole
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
    val userProfile by AuthSession.user.collectAsState()
    val userId = userProfile?.uid
    val canManageEnvironments = userProfile?.role == UserRole.STAFF

    var environmentAction by remember(userId) {
        mutableStateOf<EnvironmentAction?>(null)
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

    val cameraReadingRepository = remember {
        CameraReadingRepository()
    }

    val cameraReadingsState = remember {
        mutableStateOf<Map<String, CameraReading?>>(emptyMap())
    }

    var currentTime by remember {
        mutableStateOf(System.currentTimeMillis())
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

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(5_000)
        }
    }

    val placesWithForecast = places.map { place ->

        // Última leitura da câmera deste ambiente
        val cameraReading = cameraReadingsState.value[place.firestoreId]

        val readingTimestamp = cameraReading
            ?.timestamp
            ?.toDate()
            ?.time

        // Leitura válida: de até 90 segundos atrás
        val isCameraReadingFresh =
            readingTimestamp != null &&
                    (currentTime - readingTimestamp) in -30_000L..90_000L

        val temCamera = isCameraReadingFresh && cameraReading != null

        // Com câmera: número que a câmera está vendo.
        // Sem câmera: 0 (a tela mostra "Sem câmera").
        val people = if (temCamera) {
            (cameraReading?.people ?: 0).coerceIn(0, place.capacity)
        } else {
            0
        }

        place.copy(
            people = people,
            minutes = 0,
            status = if (temCamera) {
                occupancyStatus(people, place.capacity)
            } else {
                "Sem câmera"
            }
        )
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

    DisposableEffect(cameraReadingRepository, places) {

        val listeners = places.mapNotNull { place ->

            val environmentId = place.firestoreId

            if (environmentId.isBlank()) {
                return@mapNotNull null
            }

            cameraReadingRepository.listenLatestReading(
                environmentId = environmentId,

                onUpdate = { reading ->
                    cameraReadingsState.value =
                        cameraReadingsState.value + (environmentId to reading)
                },

                onError = { error ->
                    Log.e(
                        "HomeScreen",
                        "Erro ao carregar leitura da câmera: $environmentId",
                        error
                    )
                }
            )
        }

        onDispose {
            listeners.forEach { listener ->
                listener.remove()
            }
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
        place.copy(favorite = place.favoriteId in activity.favoriteIds)
    }

    // Ids do Firestore dos ambientes favoritos (é o id que vai nas notificações)
    val favoriteEnvironmentIds = placesWithFavorites
        .filter { it.favorite }
        .map { it.firestoreId }
        .toSet()

    val allEnvironmentIds = placesWithFavorites
        .map { it.firestoreId }
        .toSet()

    // Só recebe push dos ambientes favoritos
    SetupPushNotifications(
        favoriteIds = favoriteEnvironmentIds,
        allIds = allEnvironmentIds
    )

    val unreadCount = rememberUnreadNotificationsCount(favoriteEnvironmentIds)

    val onFavoriteChange: (CampusPlace, Boolean) -> Unit = { place, favorite ->
        userId?.let { id ->
            profileRepository.setFavorite(
                userId = id,
                environmentId = place.favoriteId,
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
                    favoriteIds = favoriteEnvironmentIds,
                    onNotificationClick = { environmentId ->
                        val place = places.firstOrNull { it.firestoreId == environmentId }
                            ?: places.firstOrNull {
                                it.name.toEnvironmentId() == environmentId || it.favoriteId == environmentId
                            }
                        place?.let { selectedPlace = it }
                    }
                )
            }
        }

        AppScreen.PROFILE -> {
            val user = userProfile
            val action = environmentAction

            when {
                action != null && canManageEnvironments -> {
                    EnvironmentManagementScreen(
                        action = action,
                        places = places,
                        onBack = { environmentAction = null },
                        onSave = environmentRepository::saveEnvironment,
                        onDelete = environmentRepository::deleteEnvironment
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
                            canManageEnvironments = canManageEnvironments,
                            onManageEnvironment = { environmentAction = it },
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

        val placeForecast = aiForecast?.forecasts?.firstOrNull {
            it.environmentId == place.firestoreId ||
                    it.environmentName.equals(place.name, ignoreCase = true)
        }

        PlaceDetailScreen(
            place = place,
            forecast = placeForecast,
            onBack = {
                selectedPlace = null
            }
        )
    }
}
