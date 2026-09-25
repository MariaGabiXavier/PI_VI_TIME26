package com.example.flowcampus.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.theme.AppNavy

@Composable
fun AnalyticsScreen() {

    var selectedPeriod by remember {
        mutableStateOf(AnalyticsPeriod.HOJE)
    }

    val stats = getSummaryStats(selectedPeriod)
    val areas = getAreaComparisons(selectedPeriod)
    val insights = getInsights(selectedPeriod)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "Análises",
                color = AppNavy,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Compartilhar",
                tint = AppNavy
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        PeriodTabs(
            selected = selectedPeriod,
            onSelectedChange = {
                selectedPeriod = it
            }
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            stats.forEachIndexed { index, stat ->

                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    stat = stat,
                    valueFontSize = if (index == 2) 15.sp else 18.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Comparação entre Áreas",
            color = AppNavy,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        AreaComparisonCard(
            areas = areas
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Insights da IA",
            color = AppNavy,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            insights.forEach { insight ->

                InsightCard(
                    insight = insight
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}
