package com.example.flowcampus.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.components.FormPrimaryButton
import com.example.flowcampus.ui.screens.auth.UserProfile
import com.example.flowcampus.ui.screens.auth.UserRole
import com.example.flowcampus.ui.screens.home.Blue
import com.example.flowcampus.ui.screens.home.CampusPlace
import com.example.flowcampus.ui.screens.home.DarkText
import com.example.flowcampus.ui.screens.home.GrayText
import com.example.flowcampus.ui.screens.home.HighRedText
import com.example.flowcampus.ui.theme.AppAmber
import com.example.flowcampus.ui.theme.AppGreen

@Composable
fun ProfileScreen(
    user: UserProfile?,
    activity: UserActivity,
    favoritePlaces: List<CampusPlace>,
    onPlaceClick: (CampusPlace) -> Unit,
    onEditProfile: () -> Unit,
    onAddEnvironment: () -> Unit,
    onLogout: () -> Unit
) {

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    // Só funcionário (Colaborador) cadastra ambiente; para estudante o botão não aparece
    val isStaff = user?.role == UserRole.STAFF

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Meu Perfil",
                color = DarkText,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    showLogoutDialog = true
                }
            ) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sair da conta",
                    tint = GrayText
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        ProfileAvatar(
            initials = user?.initials ?: "?"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = user?.name ?: "Visitante",
            color = DarkText,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (user != null) {

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = user.email,
                color = GrayText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            // Sem RA (professor, colaborador ou estudante que ainda não preencheu) mostra o tipo de conta
            if (user.ra.isNotBlank()) {
                ProfileTag(
                    label = "RA",
                    value = user.ra
                )
            } else {
                ProfileTag(
                    value = user.role.label
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ProfileStatCard(
                value = activity.visitsThisMonth.toString(),
                label = "Visitas Este Mês",
                valueColor = DarkText,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            ProfileStatCard(
                value = formatTimeSaved(activity.timeSavedMinutes),
                label = "Tempo Economizado",
                valueColor = Blue,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            ProfileStatCard(
                value = activity.predictionsUsed.toString(),
                label = "Previsões Usadas",
                valueColor = AppGreen,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "Locais Favoritos",
            color = DarkText,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (favoritePlaces.isEmpty()) {

            EmptyFavoritesCard(
                modifier = Modifier.padding(horizontal = 24.dp)
            )

        } else {

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = favoritePlaces,
                    key = { it.name }
                ) { place ->

                    FavoritePlaceCard(
                        place = place,
                        onClick = {
                            onPlaceClick(place)
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        FormPrimaryButton(
            text = "Editar Perfil",
            onClick = onEditProfile,
            containerColor = AppAmber,
            enabled = user != null,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (isStaff) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            AddEnvironmentButton(
                onClick = onAddEnvironment
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }

    if (showLogoutDialog) {

        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = {
                showLogoutDialog = false
            }
        )
    }
}

@Composable
private fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,

        title = {
            Text(
                text = "Sair da conta?",
                color = DarkText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Text(
                text = "Você vai precisar entrar de novo para ver seus favoritos e alertas.",
                color = GrayText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },

        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = "Sair",
                    color = HighRedText,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancelar",
                    color = GrayText
                )
            }
        }
    )
}
