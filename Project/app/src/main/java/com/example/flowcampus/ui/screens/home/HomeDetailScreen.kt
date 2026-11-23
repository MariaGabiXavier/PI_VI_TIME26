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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.example.flowcampus.ai.AiEnvironmentForecast
import androidx.compose.ui.text.style.TextAlign

@Composable
fun PlaceDetailScreen(
    place: CampusPlace,
    forecast: AiEnvironmentForecast?,
    onBack: () -> Unit
) {
    var realReadings by remember(place.firestoreId) {
        mutableStateOf<List<CameraReading>>(emptyList())
    }
    val repository = remember { CameraReadingRepository() }

    DisposableEffect(repository, place.firestoreId) {
        val listeners = repository.listenTodayReadings(
            environmentId = place.firestoreId,
            onUpdate = { readings -> realReadings = readings },
            onError = { error ->
                android.util.Log.e("OccupationChart", "Erro ao carregar histórico", error)
            }
        )
        onDispose { listeners.forEach { it.remove() } }
    }

    val data = getPlaceDetail(place, forecast, realReadings)

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
                        text = if (place.status == "Sem câmera") "--" else place.people.toString(),
                        color = DarkText,
                        fontSize = 43.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 45.sp
                    )

                    Spacer(
                        modifier = Modifier.width(9.dp)
                    )

                    Text(
                        text = if (place.status == "Sem câmera") "sem câmera neste ambiente" else "pessoas agora",
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF1685F8)))
                        Spacer(Modifier.width(3.dp))
                        Text("Hoje", color = GrayText, fontSize = 11.sp)
                        Spacer(Modifier.width(7.dp))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF63C58A)))
                        Spacer(Modifier.width(3.dp))
                        Text("Previsão IA", color = GrayText, fontSize = 11.sp)
                    }
                }
            }

            item {
                OccupationChart(data = data)
            }

            item {

                Text(
                    text = "Recomendação da IA",
                    color = DarkText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {

                AIRecommendationCard(
                    data = data
                )
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
                    fontSize = 11.sp,
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}



@Composable
fun OccupationChart(data: PlaceDetailMock) {
    val capacity = data.capacity.coerceAtLeast(1)

    val rawStep = kotlin.math.ceil(capacity / 8.0)
        .toInt()
        .coerceAtLeast(1)

    val magnitude = Math.pow(
        10.0,
        Math.floor(Math.log10(rawStep.toDouble()))
    ).toInt().coerceAtLeast(1)

    val normalized = rawStep.toDouble() / magnitude

    val step = (
        when {
            normalized <= 1.0 -> 1
            normalized <= 2.0 -> 2
            normalized <= 5.0 -> 5
            else -> 10
        } * magnitude
    )

    val ticks = (0..capacity step step).toList().let {
        if (it.lastOrNull() == capacity) it else it + capacity
    }

    val count = minOf(
        data.hourlyPeople.size,
        data.hours.size,
        data.chartHasData.size
    )

    Column(Modifier.fillMaxWidth()) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp),
            shape = RoundedCornerShape(17.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {

            Row(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = 8.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    )
            ) {

                Column(
                    modifier = Modifier
                        .width(30.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    ticks.reversed().forEach { tick ->
                        Text(
                            text = tick.toString(),
                            color = Color(0xFF9CA3B5),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.width(7.dp))

                Canvas(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    val chartHeight = size.height
                    val slotWidth = if (count > 0) {
                        size.width / count
                    } else {
                        size.width
                    }

                    val barWidth = (slotWidth * 0.62f)
                        .coerceAtLeast(2.dp.toPx())

                    ticks.forEach { tick ->
                        val y = chartHeight -
                            (tick.toFloat() / capacity) * chartHeight

                        drawLine(
                            color = Color(0xFFE9EDF4),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    repeat(count) { index ->

                        if (!data.chartHasData[index]) return@repeat

                        val people = data.hourlyPeople[index]
                            .coerceIn(0, capacity)

                        if (people <= 0) return@repeat

                        val barHeight =
                            (people.toFloat() / capacity) * chartHeight

                        val x = index * slotWidth +
                            (slotWidth - barWidth) / 2f

                        drawRoundRect(
                            color = if (
                                data.chartIsForecast.getOrElse(index) { false }
                            ) {
                                Color(0xFF63C58A)
                            } else {
                                Color(0xFF1685F8)
                            },
                            topLeft = Offset(
                                x,
                                chartHeight - barHeight
                            ),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(
                                4.dp.toPx(),
                                4.dp.toPx()
                            )
                        )
                    }
                }
            }
        }

        // Horários: cada um ocupa uma coluna igual e fica centralizado.
        // O início e o fim acompanham exatamente a área das barras.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 45.dp,
                    end = 12.dp,
                    top = 7.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            data.hours.take(count).forEach { hour ->

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hour.take(2) + "h",
                        color = Color(0xFF8F98AD),
                        fontSize = 8.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }
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
            .height(70.dp),

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
                            "${data.recommendationStatus} (~${data.recommendationPeople} pessoas)"
                        )
                    }

                    append(" às ")

                    withStyle(
                        SpanStyle(
                            color = DarkText,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(data.recommendationTime)
                    }
                },
                color = GrayText,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )

            Text(
                text = if (data.recommendationTime == "--") {
                    "Não há previsão disponível para este ambiente."
                } else {
                    "Horário sugerido em aproximadamente " +
                        "${data.minutesUntilRecommendation} minutos."
                },
                color = GrayText,
                fontSize = 12.sp,
                lineHeight = 16.sp
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