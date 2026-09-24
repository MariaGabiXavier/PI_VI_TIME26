package com.example.flowcampus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.flowcampus.ui.screens.home.AppScreen
import com.example.flowcampus.ui.screens.home.BorderGray
import com.example.flowcampus.ui.screens.home.BottomNavigationItem

@Composable
fun BottomNavigation(
    selectedScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {

        HorizontalDivider(
            color = BorderGray,
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .padding(horizontal = 18.dp),

            horizontalArrangement = Arrangement.SpaceAround,

            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomNavigationItem(
                icon = Icons.Default.Home,
                label = "Início",
                selected = selectedScreen == AppScreen.HOME,
                onClick = {
                    onScreenChange(AppScreen.HOME)
                }
            )

            BottomNavigationItem(
                icon = Icons.Default.ShowChart,
                label = "Análises",
                selected = selectedScreen == AppScreen.ANALYSES,
                onClick = {
                    onScreenChange(AppScreen.ANALYSES)
                }
            )

            BottomNavigationItem(
                icon = Icons.Default.NotificationsNone,
                label = "Alertas",
                selected = selectedScreen == AppScreen.ALERTS,
                onClick = {
                    onScreenChange(AppScreen.ALERTS)
                }
            )

            BottomNavigationItem(
                icon = Icons.Default.PersonOutline,
                label = "Perfil",
                selected = selectedScreen == AppScreen.PROFILE,
                onClick = {
                    onScreenChange(AppScreen.PROFILE)
                }
            )
        }
    }
}
