package com.example.flowcampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.flowcampus.ui.screens.home.HomeScreen
import com.example.flowcampus.ui.screens.onboarding.FlowCampusOnboarding
import com.example.flowcampus.ui.theme.FlowCampusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowCampusTheme { // Substitua pelo seu tema gerado
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. Cria o controlador de rotas
                    val navController = rememberNavController()

                    // 2. Define o Onboarding como a tela inicial (startDestination)
                    NavHost(navController = navController, startDestination = "onboarding") {

                        // Rota do Onboarding
                        composable("onboarding") {
                            FlowCampusOnboarding(
                                onFinish = {
                                    // 3. Redireciona para a Home ao clicar em "Começar"
                                    navController.navigate("home") {
                                        // 4. Remove o Onboarding do histórico para o usuário não voltar a ele clicando em 'Voltar'
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Rota da Home (a sua tela principal)
                        composable("home") {
                            // Chama a HomeScreen que já existe no seu projeto
                            HomeScreen()
                        }
                    }
                }
            }
        }
    }
}