package com.example.flowcampus.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.theme.AppBlue
import com.example.flowcampus.ui.theme.AppGrayText
import com.example.flowcampus.ui.theme.AppNavy

@Composable
fun PeriodTabs(
    selected: AnalyticsPeriod,
    onSelectedChange: (AnalyticsPeriod) -> Unit
) {

    val options = listOf(
        AnalyticsPeriod.HOJE to "Hoje",
        AnalyticsPeriod.SEMANA to "Semana",
        AnalyticsPeriod.MES to "Mês"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AnalyticsTabBackground)
            .padding(4.dp)
    ) {

        options.forEach { (period, label) ->

            val isSelected = period == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Color.White else Color.Transparent
                    )
                    .clickable {
                        onSelectedChange(period)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = label,
                    color = if (isSelected) AppBlue else AppGrayText,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    modifier: Modifier = Modifier,
    stat: SummaryStat,
    valueFontSize: TextUnit = 18.sp
) {

    Card(
        modifier = modifier,

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(
                vertical = 14.dp,
                horizontal = 10.dp
            )
        ) {

            Text(
                text = stat.label,
                color = AppGrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = stat.value,
                color = stat.valueColor,
                fontSize = valueFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AreaComparisonCard(
    areas: List<AreaComparison>
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            areas.forEachIndexed { index, area ->

                AreaComparisonRow(
                    area = area
                )

                if (index != areas.lastIndex) {

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AreaComparisonRow(
    area: AreaComparison
) {

    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = area.name,
                color = AppNavy,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = area.value.toString(),
                color = AppNavy,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(AnalyticsTrack)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        fraction = area.value / area.maxValue.toFloat()
                    )
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(area.color)
            )
        }
    }
}

@Composable
fun InsightCard(
    insight: AnalyticsInsight
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(14.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(insight.iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = insight.icon,
                    contentDescription = null,
                    tint = insight.iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Text(
                color = AppNavy,
                fontSize = 13.sp,
                lineHeight = 18.sp,

                text = buildAnnotatedString {

                    withStyle(
                        SpanStyle(fontWeight = FontWeight.Normal)
                    ) {
                        append(insight.textBefore)
                    }

                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = insight.highlightColor
                        )
                    ) {
                        append(insight.highlight)
                    }

                    withStyle(
                        SpanStyle(fontWeight = FontWeight.Normal)
                    ) {
                        append(insight.textAfter)
                    }
                }
            )
        }
    }
}
