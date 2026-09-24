package com.example.flowcampus.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlaceDetailScreen(
    place: CampusPlace,
    onBack: () -> Unit
) {

    val data = getMockDetail(place)

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
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 4.dp
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
                text = "Status ${place.name}",
                color = DarkText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {}
            ) {

                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Compartilhar",
                    tint = DarkText
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),

            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 25.dp
            ),

            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = data.people.toString(),
                        color = DarkText,
                        fontSize = 43.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 45.sp
                    )

                    Spacer(
                        modifier = Modifier.width(9.dp)
                    )

                    Text(
                        text = "pessoas agora",
                        color = GrayText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    StatusBadge(
                        status = place.status
                    )
                }
            }

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {

                    DetailInfoCard(
                        title = "Espera de",
                        value = "${data.waitingTime} min",
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )

                    DetailInfoCard(
                        title = "Horário Pico",
                        value = data.peakTime,
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )

                    DetailInfoCard(
                        title = "Capacidade",
                        value = data.capacity.toString(),
                        icon = Icons.Default.Groups,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Ocupação Por Tempo",
                        color = DarkText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Blue)
                        )

                        Spacer(
                            modifier = Modifier.width(3.dp)
                        )

                        Text(
                            text = "Hoje",
                            color = GrayText,
                            fontSize = 8.sp
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF63C58A))
                        )

                        Spacer(
                            modifier = Modifier.width(3.dp)
                        )

                        Text(
                            text = "Previsão IA",
                            color = GrayText,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            item {
                OccupationChart()
            }

            item {

                Text(
                    text = "Recomendação da IA",
                    color = DarkText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {

                AIRecommendationCard(
                    data = data
                )
            }

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "7-Day Trend",
                            color = DarkText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Weekly volume is down 8%",
                            color = GrayText,
                            fontSize = 9.sp
                        )
                    }

                    WeeklyTrendChart()
                }
            }
        }
    }
}

@Composable
fun DetailInfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.height(67.dp),

        shape = RoundedCornerShape(13.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = title,
                    color = GrayText,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = value,
                color = DarkText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun OccupationChart() {

    val values = listOf(
        25f,
        32f,
        48f,
        75f,
        66f,
        35f,
        25f,
        20f
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp),

            shape = RoundedCornerShape(17.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 14.dp,
                        bottom = 25.dp
                    )
            ) {

                val maxValue = 80f
                val chartHeight = size.height
                val barWidth = 10.dp.toPx()

                val spacing =
                    (size.width - barWidth * values.size) /
                            (values.size - 1)

                values.forEachIndexed { index, value ->

                    val barHeight =
                        (value / maxValue) * chartHeight

                    val x =
                        index * spacing

                    val y =
                        chartHeight - barHeight

                    val barColor =
                        if (index >= 5) {
                            Color(0xFF71C997)
                        } else {
                            Blue
                        }

                    drawRoundRect(
                        color = barColor,

                        topLeft = Offset(
                            x = x,
                            y = y
                        ),

                        size = Size(
                            width = barWidth,
                            height = barHeight
                        ),

                        cornerRadius = CornerRadius(
                            4.dp.toPx(),
                            4.dp.toPx()
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 20.dp
                ),

            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            listOf(
                "8am",
                "10am",
                "12pm",
                "14pm",
                "16pm",
                "18pm",
                "20pm",
                "22pm"
            ).forEach { label ->

                Text(
                    text = label,
                    color = Color(0xFF9CA3B5),
                    fontSize = 7.sp
                )
            }
        }
    }
}

@Composable
fun AIRecommendationCard(
    data: PlaceDetailMock
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5F8EE)),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF15A35B),
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = buildAnnotatedString {

                        append("A lotação cairá para ")

                        withStyle(
                            SpanStyle(
                                color = Color(0xFF13A35B),
                                fontWeight = FontWeight.Bold
                            )
                        ) {

                            append(
                                "Baixo (~${data.recommendationPeople} pessoas)"
                            )
                        }

                        append(" às ")

                        withStyle(
                            SpanStyle(
                                color = DarkText,
                                fontWeight = FontWeight.Bold
                            )
                        ) {

                            append(
                                data.recommendationTime
                            )
                        }
                    },

                    color = GrayText,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )

                Text(
                    text = "Melhor horário para visitar é daqui a 45 minutos.",
                    color = GrayText,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
fun WeeklyTrendChart() {

    val bars = listOf(
        22f,
        28f,
        18f,
        35f,
        42f,
        31f,
        26f
    )

    Canvas(
        modifier = Modifier
            .width(66.dp)
            .height(40.dp)
    ) {

        val barWidth = 5.dp.toPx()
        val spacing = 4.dp.toPx()
        val maxHeight = size.height

        bars.forEachIndexed { index, value ->

            val barHeight =
                maxHeight * (value / 45f)

            val x =
                index * (barWidth + spacing)

            val y =
                maxHeight - barHeight

            drawRoundRect(
                color = Color(0xFF9EA6BF),

                topLeft = Offset(
                    x = x,
                    y = y
                ),

                size = Size(
                    width = barWidth,
                    height = barHeight
                ),

                cornerRadius = CornerRadius(
                    3.dp.toPx(),
                    3.dp.toPx()
                )
            )
        }
    }
}