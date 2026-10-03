package com.example.flowcampus.ui.screens.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Autenticação com Firebase Authentication (e-mail/senha) e perfil no Firestore.
 *
 * Estrutura no Firestore (a coleção é criada sozinha na primeira gravação):
 *
 *   users/{uid}
 *     ├─ name: String
 *     ├─ email: String
 *     ├─ role: "STUDENT" | "TEACHER" | "STAFF"
 *     └─ createdAt: Timestamp
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private companion object {
        const val USERS = "users"
    }

    // ---------- Cadastro: cria a conta no Auth e salva o perfil no Firestore ----------

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<UserProfile> = safeCall {

        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Não foi possível criar o usuário.")

        val profile = UserProfile(
            uid = user.uid,
            name = name,
            email = email,
            role = role
        )

        try {
            saveProfile(profile)
        } catch (e: Exception) {
            // Evita conta "órfã" no Auth sem perfil no banco
            runCatching { user.delete().await() }
            throw e
        }

        AuthSession.set(profile)
        profile
    }

    // ---------- Login: autentica e busca o perfil no Firestore ----------

    override suspend fun login(
        email: String,
        password: String
    ): Result<UserProfile> = safeCall {

        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("Não foi possível entrar.")

        val profile = fetchOrCreateProfile(user)
        AuthSession.set(profile)
        profile
    }

    // ---------- Sessão salva (login automático) ----------

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun loadCurrentUser(): Result<UserProfile?> = safeCall {

        val user = auth.currentUser ?: return@safeCall null

        val profile = fetchOrCreateProfile(user)
        AuthSession.set(profile)
        profile
    }

    override fun logout() {
        auth.signOut()
        AuthSession.clear()
    }

    // ---------- Firestore ----------

    private suspend fun saveProfile(profile: UserProfile) {
        db.collection(USERS)
            .document(profile.uid)
            .set(profile.toFirestoreMap())
            .await()
    }

    /**
     * Busca `users/{uid}`. Se a conta existe no Auth mas não tem documento
     * (ex.: criada manualmente no console), cria um perfil básico.
     */
    private suspend fun fetchOrCreateProfile(user: FirebaseUser): UserProfile {

        val snapshot = db.collection(USERS).document(user.uid).get().await()

        snapshot.toUserProfile()?.let { return it }

        val email = user.email.orEmpty()

        val profile = UserProfile(
            uid = user.uid,
            name = user.displayName ?: email.substringBefore("@"),
            email = email,
            role = UserRole.STUDENT
        )

        saveProfile(profile)
        return profile
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile? {

        if (!exists()) return null

        val name = getString("name") ?: return null
        val email = getString("email") ?: return null

        val role = runCatching { UserRole.valueOf(getString("role").orEmpty()) }
            .getOrDefault(UserRole.STUDENT)

        return UserProfile(
            uid = id,
            name = name,
            email = email,
            role = role,
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
        )
    }

    // ---------- Erros ----------

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.toFriendlyMessage(), e))
        }

    private fun Throwable.toFriendlyMessage(): String = when (this) {

        is FirebaseAuthUserCollisionException ->
            "Este e-mail já está cadastrado."

        is FirebaseAuthWeakPasswordException ->
            "A senha é muito fraca. Use pelo menos 6 caracteres."

        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException ->
            "E-mail ou senha incorretos."

        is FirebaseNetworkException ->
            "Sem conexão. Verifique sua internet e tente novamente."

        is FirebaseFirestoreException ->
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Sem permissão para acessar o banco. Verifique as regras do Firestore."
            } else {
                "Erro ao acessar o banco de dados. Tente novamente."
            }

        else -> message ?: "Algo deu errado. Tente novamente."
    }
}
