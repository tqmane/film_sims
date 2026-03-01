package com.tqmane.filmsim.ui

import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tqmane.filmsim.data.ProUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthState(
    val isSignedIn: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proUserRepository: ProUserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isProUser: StateFlow<Boolean> = proUserRepository.isProUser
    val licenseMismatchVersion: StateFlow<String?> = proUserRepository.licenseMismatchVersion
    val isPermanentLicense: StateFlow<Boolean> = proUserRepository.isPermanentLicense
    val proCheckNetworkError: StateFlow<Boolean> = proUserRepository.proCheckNetworkError

    // Web client ID from google-services.json (client_type 3)
    companion object {
        const val WEB_CLIENT_ID =
            "566024328587-1e4rlh1dhh1cgvlvh25c2ap05qn7qntd.apps.googleusercontent.com"
    }

    init {
        // Restore session on app start
        val user = auth.currentUser
        if (user != null) {
            _authState.value = AuthState(
                isSignedIn = true,
                userName = user.displayName,
                userEmail = user.email,
                userPhotoUrl = user.photoUrl?.toString()
            )
            viewModelScope.launch { proUserRepository.checkProStatus(user.email) }
        }
    }

    /**
     * Process a Google ID token (obtained by the Activity via CredentialManager).
     * Handles Firebase authentication and pro status check.
     * Separates ViewModel logic from Activity-scoped CredentialManager API.
     */
    fun processGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                if (user != null) {
                    proUserRepository.checkProStatus(user.email)
                    _authState.value = AuthState(
                        isSignedIn = true,
                        userName = user.displayName,
                        userEmail = user.email,
                        userPhotoUrl = user.photoUrl?.toString(),
                        isLoading = false
                    )
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Authentication failed"
                    )
                }
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    /** Legacy Google Sign In fallback handler (called from Activity result callback). */
    fun handleLegacySignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(credential).await()
                    val user = authResult.user

                    if (user != null) {
                        proUserRepository.checkProStatus(user.email)
                        _authState.value = AuthState(
                            isSignedIn = true,
                            userName = user.displayName,
                            userEmail = user.email,
                            userPhotoUrl = user.photoUrl?.toString(),
                            isLoading = false
                        )
                    } else {
                        _authState.value = _authState.value.copy(isLoading = false, error = "Firebase authentication failed")
                    }
                } else {
                    _authState.value = _authState.value.copy(isLoading = false, error = "No ID token found")
                }
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun resetLoadingState() {
        _authState.value = _authState.value.copy(isLoading = false)
    }

    /** Sign out from Firebase and Google. Uses ApplicationContext for CredentialManager. */
    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) { /* ignore */ }
            proUserRepository.clearProStatus()
            _authState.value = AuthState()
        }
    }
}
