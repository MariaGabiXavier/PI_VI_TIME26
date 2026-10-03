package com.example.flowcampus.ui.screens.auth

import android.util.Patterns
import kotlinx.coroutines.delay

/**
 * Contrato de autenticação usado pelas telas de Login e Cadastro.
 * Implementações: [FirebaseAuthRepository] (produção) e [FakeAuthRepository] (testes/previews).
 */
interface AuthRepository {

    suspend fun login(email: String, password: String): Result<UserProfile>

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<UserProfile>

    /** Há uma sessão ativa salva no dispositivo? (não faz chamada de rede) */
    fun isLoggedIn(): Boolean

    /** Carrega do banco o perfil do usuário da sessão ativa (login automático). */
    suspend fun loadCurrentUser(): Result<UserProfile?>

    fun logout()
}

/** Implementação sem backend: sempre retorna sucesso. */
object FakeAuthRepository : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        delay(700)
        val profile = UserProfile("fake", email.substringBefore("@"), email, UserRole.STUDENT)
        AuthSession.set(profile)
        return Result.success(profile)
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<UserProfile> {
        delay(900)
        val profile = UserProfile("fake", name, email, role)
        AuthSession.set(profile)
        return Result.success(profile)
    }

    override fun isLoggedIn(): Boolean = false

    override suspend fun loadCurrentUser(): Result<UserProfile?> = Result.success(null)

    override fun logout() = AuthSession.clear()
}

/** Validações de formulário. Retornam a mensagem de erro, ou null se válido. */
object AuthValidator {

    fun name(value: String): String? = when {
        value.isBlank() -> "Informe seu nome"
        value.trim().length < 3 -> "O nome precisa ter pelo menos 3 caracteres"
        else -> null
    }

    fun email(value: String): String? = when {
        value.isBlank() -> "Informe seu e-mail"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "Digite um e-mail válido"
        else -> null
    }

    fun password(value: String): String? = when {
        value.isBlank() -> "Crie uma senha"
        value.length < 6 -> "A senha precisa ter pelo menos 6 caracteres"
        else -> null
    }

    fun confirmPassword(password: String, confirmation: String): String? = when {
        confirmation.isBlank() -> "Confirme sua senha"
        password != confirmation -> "As senhas não coincidem"
        else -> null
    }
}