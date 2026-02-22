package com.tqmane.filmsim.ui

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tqmane.filmsim.data.ProUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val proUserRepository: ProUserRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isProUser: StateFlow<Boolean> = proUserRepository.isProUser

    // Web client ID from google-services.json (client_type 3)
    companion object {
        private const val WEB_CLIENT_ID =
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
     * Launch Google Sign-In via Credential Manager.
     * Must be called from an Activity context.
     */
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            try {
                val credentialManager = CredentialManager.create(activityContext)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activityContext, request)
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Authenticate with Firebase
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                    // checkProStatus が完了するまで isLoading=true のまま維持
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
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /** Sign out from Firebase and Google. */
    fun signOut(activityContext: Context) {
        viewModelScope.launch {
            try {
                auth.signOut()
                val credentialManager = CredentialManager.create(activityContext)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) { /* ignore */ }
            proUserRepository.clearProStatus()
            _authState.value = AuthState()
        }
    }
}
