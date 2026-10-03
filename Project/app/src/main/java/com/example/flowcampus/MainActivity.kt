package com.example.flowcampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flowcampus.ui.screens.auth.FirebaseAuthRepository
import com.example.flowcampus.ui.screens.auth.LoginScreen
import com.example.flowcampus.ui.screens.auth.RegisterScreen
import com.example.flowcampus.ui.screens.home.HomeScreen
import com.example.flowcampus.ui.screens.onboarding.FlowCampusOnboarding
import com.example.flowcampus.ui.theme.FlowCampusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowCampusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authRepository = remember { FirebaseAuthRepository() }

                    // Já existe sessão salva? Vai direto para a Home.
                    val startDestination = remember {
                        if (authRepository.isLoggedIn()) "home" else "onboarding"
                    }

                    // Com sessão salva, busca o perfil no Firestore em segundo plano
                    LaunchedEffect(Unit) {
                        if (authRepository.isLoggedIn()) {
                            authRepository.loadCurrentUser()
                        }
                    }

                    NavHost(navController = navController, startDestination = startDestination) {

                        composable("onboarding") {
                            FlowCampusOnboarding(
                                onFinish = {
                                    navController.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("login") {
                            LoginScreen(
                                repository = authRepository,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                repository = authRepository,
                                onRegisterSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen()
                        }
                    }
                }
            }
        }
    }
}