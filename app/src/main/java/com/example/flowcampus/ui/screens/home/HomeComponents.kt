package com.example.flowcampus.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScaffold(
    selectedScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    content: @Composable () -> Unit
) {

    Scaffold(
        containerColor = BackgroundColor,

        bottomBar = {

            BottomNavigation(
                selectedScreen = selectedScreen,
                onScreenChange = onScreenChange
            )
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(BackgroundColor)
        ) {

            content()
        }
    }
}

@Composable
fun HomeContent(
    places: List<CampusPlace>,
    onPlaceClick: (CampusPlace) -> Unit,
    onSeeAllClick: () -> Unit
) {

    var searchText by remember {
        mutableStateOf("")
    }

    val filteredPlaces = places.filter { place ->

        place.name.contains(
            searchText,
            ignoreCase = true
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 18.dp
                )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Bom Dia 👋",
                        color = GrayText,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Alex Mercer",
                        color = DarkText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFFD9DEE8)),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "AM",
                        color = DarkText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            SearchBar(
                value = searchText,
                onValueChange = {
                    searchText = it
                }
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Campus Status",
                color = DarkText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Ver Tudo",
                color = Blue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    onSeeAllClick()
                }
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (filteredPlaces.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Nenhum local encontrado",
                    color = GrayText,
                    fontSize = 13.sp
                )
            }

        } else {

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(filteredPlaces) { place ->

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
}

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Pesquisar",
            tint = androidx.compose.ui.graphics.Color(0xFF9DA5BA),
            modifier = Modifier.size(20.dp)
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,

            textStyle = TextStyle(
                color = DarkText,
                fontSize = 11.sp
            ),

            modifier = Modifier.weight(1f),

            decorationBox = { innerTextField ->

                if (value.isEmpty()) {

                    Text(
                        text = "Pesquisar refeitório, biblioteca, laboratório",
                        color = androidx.compose.ui.graphics.Color(0xFFA4AABD),
                        fontSize = 11.sp
                    )
                }

                innerTextField()
            }
        )

        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Filtros",
            tint = Blue,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun CampusStatusCard(
    place: CampusPlace,
    onClick: () -> Unit
) {

    var isFavorite by remember {
        mutableStateOf(place.favorite)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(31.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFFF3F7FD)),
                    contentAlignment = Alignment.Center
                ) {

                    PlaceIconView(
                        type = place.iconType
                    )
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                StatusBadge(
                    status = place.status
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = place.name,
                color = DarkText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = "${place.people} pessoas",
                color = GrayText,
                fontSize = 9.sp
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFF8C93A7),
                    modifier = Modifier.size(13.dp)
                )

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                Text(
                    text = "~${place.minutes} min",
                    color = GrayText,
                    fontSize = 9.sp
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        isFavorite = !isFavorite
                    },
                    modifier = Modifier.size(25.dp)
                ) {

                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },

                        contentDescription = "Favorito",

                        tint = if (isFavorite) {
                            androidx.compose.ui.graphics.Color(0xFFEF3F45)
                        } else {
                            androidx.compose.ui.graphics.Color(0xFFA8AEC0)
                        },

                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String
) {

    val background: androidx.compose.ui.graphics.Color
    val textColor: androidx.compose.ui.graphics.Color

    when (status) {

        "Alto" -> {
            background = HighRed
            textColor = HighRedText
        }

        "Médio" -> {
            background = MediumOrange
            textColor = MediumOrangeText
        }

        else -> {
            background = LowGreen
            textColor = LowGreenText
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .padding(
                horizontal = 8.dp,
                vertical = 3.dp
            )
    ) {

        Text(
            text = status,
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlaceIconView(
    type: PlaceIcon
) {

    val icon = when (type) {

        PlaceIcon.RESTAURANT ->
            Icons.Default.LocalCafe

        PlaceIcon.LIBRARY ->
            Icons.Default.MenuBook

        PlaceIcon.BUS ->
            Icons.Default.LocalShipping

        PlaceIcon.LAB ->
            Icons.Default.School

        PlaceIcon.STUDY ->
            Icons.Default.Groups

        PlaceIcon.CLASSROOM ->
            Icons.Default.Home
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Blue,
        modifier = Modifier.size(17.dp)
    )
}