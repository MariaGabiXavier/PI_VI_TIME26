package com.example.flowcampus.ui.screens.onboarding

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.R // Certifique-se de que este import aponta para o R do seu projeto
import kotlinx.coroutines.launch

// 1. Classe de dados agora inclui o imageRes (ID do recurso de imagem)
data class OnboardingPageData(
    val title: String,
    val description: String,
    val buttonText: String,
    val themeColor: Color,
    val imageRes: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlowCampusOnboarding(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    // 2. Dados atualizados referenciando as imagens na pasta drawable
    val pages = listOf(
        OnboardingPageData(
            title = "Monitore o Campus em Tempo Real",
            description = "O FlowCampus utiliza câmeras e técnicas de Visão Computacional para monitorar continuamente a ocupação dos ambientes do campus.",
            buttonText = "Próximo >",
            themeColor = Color(0xFF1E88E5), // Azul
            imageRes = R.drawable.img_campus // Imagem dos prédios
        ),
        OnboardingPageData(
            title = "Previsões Feitas pela IA",
            description = "Nossos modelos de aprendizado de máquina preveem horários de pico, congestionamentos e tempos de espera, para que você possa planejar seu dia sem complicações.",
            buttonText = "Próximo >",
            themeColor = Color(0xFF43A047), // Verde
            imageRes = R.drawable.img_ia // Imagem do cérebro IA
        ),
        OnboardingPageData(
            title = "Navegue de Forma Mais Inteligente",
            description = "Receba alertas em tempo real quando seus ambientes de estudo/lazer favoritos estiverem mais vazios.",
            buttonText = "Começar →",
            themeColor = Color(0xFFFB8C00), // Laranja
            imageRes = R.drawable.img_mapa // Imagem do telemóvel com mapa
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Cabeçalho
        Row(
            modifier = Modifier.padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Logo",
                tint = Color(0xFF1E88E5),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FlowCampus",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1C1C5E)
            )
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingContent(pageData = pages[page])
        }

        // Rodapé
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicadores (Bolinhas)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    val color = if (isSelected) pages[pagerState.currentPage].themeColor else Color(0xFFE0E0E0)
                    val width = if (isSelected) 16.dp else 8.dp

                    Box(
                        modifier = Modifier
                            .size(width = width, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }

            // Botão
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish() // Chama a navegação para a Home quando chegar ao fim
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].themeColor
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = pages[pagerState.currentPage].buttonText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 3. Composable atualizado para renderizar o componente Image
@Composable
fun OnboardingContent(pageData: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Agora usamos a imagem real em vez do Box cinzento
        Image(
            painter = painterResource(id = pageData.imageRes),
            contentDescription = pageData.title,
            contentScale = ContentScale.Crop, // Garante que a imagem preencha a área mantendo o formato
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Mantém o formato quadrado como no design
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = pageData.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1C1C5E),
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = pageData.description,
            fontSize = 15.sp,
            color = Color(0xFF757575),
            lineHeight = 22.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FlowCampusOnboardingPreview() {
    // Passamos uma função vazia {} apenas para o preview não dar erro
    FlowCampusOnboarding(onFinish = {})
}