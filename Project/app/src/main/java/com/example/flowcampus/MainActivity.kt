package com.example.flowcampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.flowcampus.ui.screens.home.HomeScreen
import com.example.flowcampus.ui.theme.FlowCampusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FlowCampusTheme {
                HomeScreen()
            }
        }
    }
}