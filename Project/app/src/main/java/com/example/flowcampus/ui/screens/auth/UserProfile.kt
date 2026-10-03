package com.example.flowcampus.ui.screens.auth

import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserRole(val label: String) {
    STUDENT("Estudante"),
    TEACHER("Professor"),
    STAFF("Colaborador")
}

/**
 * Dados do usuário salvos no Firestore em `users/{uid}`.
 * A senha NUNCA é salva aqui: ela fica apenas no Firebase Authentication.
 */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val createdAtMillis: Long? = null
) {

    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }

    /** Campos gravados no documento. `createdAt` usa a hora do servidor. */
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "name" to name,
        "email" to email,
        "role" to role.name,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

/** Usuário logado, mantido em memória para qualquer tela consultar. */
object AuthSession {

    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user.asStateFlow()

    fun set(profile: UserProfile) {
        _user.value = profile
    }

    fun clear() {
        _user.value = null
    }
}
