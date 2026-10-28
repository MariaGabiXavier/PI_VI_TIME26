package com.example.flowcampus.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox

@Composable
fun AllPlacesScreen(
    places: List<CampusPlace>,
    onBack: () -> Unit,
    onPlaceClick: (CampusPlace) -> Unit,
    onFavoriteChange: (CampusPlace, Boolean) -> Unit = { _, _ -> }
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
                    },
                    onFavoriteChange = { favorite ->
                        onFavoriteChange(place, favorite)
                    }
                )
            }
        }
    }
}


@Composable
fun BottomNavigationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showBadge: Boolean = false
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
        BadgedBox(
            badge = {
                if (showBadge) {
                    Badge(containerColor = Blue)
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Blue else Color(0xFF9CA3B5),
                modifier = Modifier.size(21.dp)
            )
        }
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