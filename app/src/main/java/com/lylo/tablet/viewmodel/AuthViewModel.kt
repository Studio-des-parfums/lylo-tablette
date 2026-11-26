package com.lylo.tablet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lylo.tablet.data.LocalPrefs
import com.lylo.tablet.data.model.AuthError
import com.lylo.tablet.data.model.AuthUser
import com.lylo.tablet.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Portage de src/context/AuthContext.tsx.
 * Le login se déclenche depuis LoginModal après un succès Google Identity Services
 * (voir auth/GoogleSignInHelper.kt) qui fournit l'email vérifié à [login].
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = LocalPrefs(application)
    private val json = Json { ignoreUnknownKeys = true }

    private val _user = MutableStateFlow<AuthUser?>(loadSavedUser())
    val user: StateFlow<AuthUser?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isInitialized = MutableStateFlow(true)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _error = MutableStateFlow<AuthError?>(null)
    val error: StateFlow<AuthError?> = _error.asStateFlow()

    private val _loginModalOpen = MutableStateFlow(false)
    val loginModalOpen: StateFlow<Boolean> = _loginModalOpen.asStateFlow()

    private fun loadSavedUser(): AuthUser? {
        val raw = prefs.getString(LocalPrefs.KEY_AUTH_USER) ?: return null
        return runCatching { json.decodeFromString<AuthUser>(raw) }.getOrNull()
    }

    fun openLoginModal() {
        clearError()
        _loginModalOpen.value = true
    }

    fun closeLoginModal() {
        _loginModalOpen.value = false
    }

    fun clearError() {
        _error.value = null
    }

    /** Portage de login(email) : appelle GET /lookup?email=... */
    fun login(email: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val res = ApiClient.service.lookup(email)
                if (!res.isSuccessful || res.body() == null) {
                    _error.value = AuthError.UNAUTHORIZED
                    onResult(false)
                    return@launch
                }
                val data = res.body()!!
                if (data.table == "customers" && (data.sessionsAvailable ?: 0) == 0) {
                    _error.value = AuthError.NO_SESSIONS
                    onResult(false)
                    return@launch
                }
                val authUser = AuthUser(
                    email = data.email,
                    firstName = data.firstName,
                    lastName = data.lastName,
                    table = data.table,
                    id = data.id,
                    phone = data.phone,
                    daysAvailable = data.daysAvailable,
                    sessionsAvailable = data.sessionsAvailable,
                )
                _user.value = authUser
                prefs.putString(LocalPrefs.KEY_AUTH_USER, json.encodeToString(authUser))
                onResult(true)
            } catch (e: Exception) {
                _error.value = AuthError.ERROR
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        _user.value = null
        prefs.remove(LocalPrefs.KEY_AUTH_USER)
    }
}
