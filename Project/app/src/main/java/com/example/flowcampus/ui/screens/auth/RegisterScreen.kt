package com.example.flowcampus.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.screens.home.BackgroundColor
import com.example.flowcampus.ui.screens.home.DarkText
import com.example.flowcampus.ui.screens.notifications.NotificationFilterChip
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    repository: AuthRepository = FakeAuthRepository
) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.STUDENT) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun submit() {

        focusManager.clearFocus()

        nameError = AuthValidator.name(name)
        emailError = AuthValidator.email(email)
        passwordError = AuthValidator.password(password)
        confirmError = AuthValidator.confirmPassword(password, confirmPassword)

        val hasError = listOf(
            nameError,
            emailError,
            passwordError,
            confirmError
        ).any { it != null }

        if (hasError) return

        scope.launch {

            isLoading = true
            formError = null

            repository.register(
                name = name.trim(),
                email = email.trim(),
                password = password,
                role = role
            )
                .onSuccess {
                    onRegisterSuccess()
                }
                .onFailure {
                    formError = it.message
                        ?: "Não foi possível criar a conta. Tente novamente."
                }

            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 12.dp,
                end = 24.dp,
                top = 8.dp,
                bottom = 32.dp
            )
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

        Column(
            modifier = Modifier.padding(start = 12.dp)
        ) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            AuthHeader(
                title = "Crie sua conta",
                subtitle = "Salve seus locais favoritos e receba alertas quando estiverem mais vazios."
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            if (formError != null) {

                AuthFormError(
                    message = formError!!
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            Text(
                text = "Eu sou",
                color = DarkText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                UserRole.entries.forEach { option ->

                    NotificationFilterChip(
                        label = option.label,
                        selected = option == role,
                        onClick = {
                            role = option
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            AuthTextField(
                label = "Nome completo",
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                placeholder = "Seu nome",
                leadingIcon = Icons.Default.PersonOutline,
                error = nameError
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            AuthTextField(
                label = "E-mail",
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                placeholder = "voce@universidade.edu.br",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                error = emailError
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            AuthTextField(
                label = "Senha",
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                placeholder = "Mínimo de 6 caracteres",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                error = passwordError
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            AuthTextField(
                label = "Confirmar senha",
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmError = null
                },
                placeholder = "Repita a senha",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                imeAction = ImeAction.Done,
                error = confirmError,
                onImeDone = {
                    submit()
                }
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            AuthPrimaryButton(
                text = "Criar conta",
                loading = isLoading,
                onClick = {
                    submit()
                }
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            AuthFooterLink(
                text = "Já tem uma conta?",
                linkText = "Entrar",
                onClick = onNavigateToLogin
            )
        }
    }
}
