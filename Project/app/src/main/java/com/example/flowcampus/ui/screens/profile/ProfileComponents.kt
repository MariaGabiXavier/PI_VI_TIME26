package com.example.flowcampus.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.screens.home.CampusPlace
import com.example.flowcampus.ui.screens.home.DarkText
import com.example.flowcampus.ui.screens.home.GrayText
import com.example.flowcampus.ui.screens.home.MediumOrange
import com.example.flowcampus.ui.screens.home.StatusBadge
import com.example.flowcampus.ui.theme.AppAmber

// Mesmo cinza do avatar pequeno da Home
private val AvatarBackground = Color(0xFFD9DEE8)
private val TagBackground = Color(0xFFE3E6EE)

@Composable
fun ProfileAvatar(
    initials: String,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(AvatarBackground),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = initials,
            color = DarkText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Etiqueta cinza abaixo do e-mail, ex.: "RA: 26175800". */
@Composable
fun ProfileTag(
    value: String,
    label: String? = null
) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TagBackground)
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp
            )
    ) {

        Text(
            text = buildAnnotatedString {

                if (label != null) {

                    withStyle(
                        SpanStyle(fontWeight = FontWeight.SemiBold)
                    ) {
                        append("$label: ")
                    }
                }

                append(value)
            },
            color = GrayText,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun ProfileStatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
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

        // Alinhado no topo: em tela estreita o rótulo quebra em 2 linhas
        // e os números continuam na mesma altura nos 3 cards
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = 14.dp,
                    horizontal = 4.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = label,
                color = GrayText,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun FavoritePlaceCard(
    place: CampusPlace,
    onClick: () -> Unit
) {

    Card(
        onClick = onClick,

        // Cresce com o nome do local, até 200dp
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = 128.dp, max = 200.dp)
            .height(70.dp),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
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

            Box(
                modifier = Modifier.align(Alignment.End)
            ) {

                // Mesmo badge da Home, mas com a altura de linha do protótipo
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(lineHeight = 11.sp)
                ) {

                    StatusBadge(
                        status = place.status
                    )
                }
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = place.name,
                color = DarkText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyFavoritesCard(
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Text(
            text = "Toque no coração de um local na tela inicial para ele aparecer aqui.",
            color = GrayText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun AddEnvironmentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),

        shape = RoundedCornerShape(12.dp),

        border = BorderStroke(1.dp, AppAmber),

        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MediumOrange,
            contentColor = AppAmber
        ),

        contentPadding = PaddingValues(horizontal = 32.dp)
    ) {

        Text(
            text = "Adicionar Ambiente",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
