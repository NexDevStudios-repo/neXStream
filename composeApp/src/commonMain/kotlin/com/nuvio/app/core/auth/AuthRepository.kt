package com.nuvio.app.core.auth

import co.touchlab.kermit.Logger
import com.nuvio.app.core.storage.LocalAccountDataCleaner
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

object AuthRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("AuthRepository")

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var initialized = false

    @OptIn(ExperimentalUuidApi::class)
    fun initialize() {
        if (initialized) return
        initialized = true

        var savedAnonId = AuthStorage.loadAnonymousUserId()
        if (savedAnonId == null) {
            savedAnonId = Uuid.random().toString()
            AuthStorage.saveAnonymousUserId(savedAnonId)
        }

        _state.value = AuthState.Authenticated(
            userId = savedAnonId,
            email = null,
            isAnonymous = true,
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    fun signInAnonymously() {
        _error.value = null
        var userId = AuthStorage.loadAnonymousUserId()
        if (userId == null) {
            userId = Uuid.random().toString()
            AuthStorage.saveAnonymousUserId(userId)
        }
        _state.value = AuthState.Authenticated(
            userId = userId,
            email = null,
            isAnonymous = true,
        )
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        signInAnonymously()
        return Result.success(Unit)
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        signInAnonymously()
        return Result.success(Unit)
    }

    suspend fun signOut(): Result<Unit> {
        _error.value = null
        AuthStorage.clearAnonymousUserId()
        runCatching { LocalAccountDataCleaner.wipe() }
        _state.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    suspend fun prepareForServerSwitch(): Result<Unit> {
        _error.value = null
        AuthStorage.clearAnonymousUserId()
        _state.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    fun reinitialize() {
        initialized = false
        _state.value = AuthState.Loading
        initialize()
    }

    suspend fun signOutIfSessionInvalid(error: Throwable, source: String): Boolean {
        return false
    }

    suspend fun deleteAccount(): Result<Unit> {
        _error.value = null
        try {
            LocalAccountDataCleaner.wipe()
        } finally {
            _state.value = AuthState.Unauthenticated
        }
        return Result.success(Unit)
    }

    fun clearError() {
        _error.value = null
    }
}
