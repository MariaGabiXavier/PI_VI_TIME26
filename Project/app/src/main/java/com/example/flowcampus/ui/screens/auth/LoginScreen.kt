package com.example.flowcampus.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowcampus.ui.screens.home.BackgroundColor
import com.example.flowcampus.ui.screens.home.Blue
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    repository: AuthRepository = FakeAuthRepository
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun submit() {

        focusManager.clearFocus()

        emailError = AuthValidator.email(email)
        passwordError = if (password.isBlank()) "Informe sua senha" else null

        if (emailError != null || passwordError != null) return

        scope.launch {

            isLoading = true
            formError = null

            repository.login(email.trim(), password)
                .onSuccess {
                    onLoginSuccess()
                }
                .onFailure {
                    formError = it.message
                        ?: "Não foi possível entrar. Tente novamente."
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
                horizontal = 24.dp,
                vertical = 32.dp
            )
    ) {

        AuthHeader(
            title = "Bem-vindo de volta",
            subtitle = "Entre para acompanhar a lotação do campus em tempo real."
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        if (formError != null) {

            AuthFormError(
                message = formError!!
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

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
            imeAction = ImeAction.Next,
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
            placeholder = "Digite sua senha",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            imeAction = ImeAction.Done,
            error = passwordError,
            onImeDone = {
                submit()
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "Esqueci minha senha",
            color = Blue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    // TODO: navegar para a tela de recuperação de senha
                }
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        AuthPrimaryButton(
            text = "Entrar",
            loading = isLoading,
            onClick = {
                submit()
            }
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        AuthFooterLink(
            text = "Ainda não tem conta?",
            linkText = "Cadastre-se",
            onClick = onNavigateToRegister
        )
    }
}
