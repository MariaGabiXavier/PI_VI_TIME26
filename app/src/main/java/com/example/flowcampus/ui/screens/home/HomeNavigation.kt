package com.example.flowcampus.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.ArrowBack

@Composable
fun AllPlacesScreen(
    places: List<CampusPlace>,
    onBack: () -> Unit,
    onPlaceClick: (CampusPlace) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 20.dp,
                    top = 5.dp,
                    bottom = 8.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = DarkText
                )
            }

            Text(
                text = "Todos os locais",
                color = DarkText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),

            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),

            horizontalArrangement = Arrangement.spacedBy(10.dp),

            verticalArrangement = Arrangement.spacedBy(10.dp),

            contentPadding = PaddingValues(
                bottom = 20.dp
            )
        ) {

            items(places) { place ->

                CampusStatusCard(
                    place = place,
                    onClick = {
                        onPlaceClick(place)
                    }
                )
            }
        }
    }
}

@Composable
fun SimpleSectionScreen(
    title: String,
    description: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp)
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = title,
            color = DarkText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = description,
            color = GrayText,
            fontSize = 13.sp
        )
    }
}

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

@Composable
fun BottomNavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 14.dp,
                vertical = 5.dp
            ),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,

            tint = if (selected) {
                Blue
            } else {
                Color(0xFF9CA3B5)
            },

            modifier = Modifier.size(21.dp)
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = label,

            color = if (selected) {
                Blue
            } else {
                Color(0xFF9CA3B5)
            },

            fontSize = 9.sp,

            fontWeight = if (selected) {
                FontWeight.Medium
            } else {
                FontWeight.Normal
            }
        )
    }
}