package com.tqmane.filmsim

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.tqmane.filmsim.di.UpdateCheckerWrapper
import com.tqmane.filmsim.ui.AuthViewModel
import com.tqmane.filmsim.ui.MainScreen
import com.tqmane.filmsim.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single Activity – acts only as the Compose host and DI entry-point.
 * All UI logic lives in [MainScreen] composable and [MainViewModel].
 *
 * CredentialManager is intentionally kept here because it requires an Activity
 * context (per the Jetpack Credential Manager API spec). The ViewModel only
 * handles the Firebase auth step after a token is obtained.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val authVm: AuthViewModel by viewModels()

    @Inject
    lateinit var updateChecker: UpdateCheckerWrapper

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { vm.loadImage(it) }
        }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                authVm.handleLegacySignInResult(result.data)
            } else {
                authVm.resetLoadingState()
            }
        }

    // ─── Lifecycle ──────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(
                viewModel = vm,
                authViewModel = authVm,
                onPickImage = { launchPicker() },
                onSignIn = { launchGoogleSignIn() },
                onSignOut = { authVm.signOut() }
            )
        }

        vm.checkForUpdates()
    }

    // ─── Google Sign-In (CredentialManager + legacy fallback) ───────────────

    /**
     * Launches Google Sign-In using Credential Manager (primary path).
     * Falls back to the legacy GoogleSignIn flow on devices where
     * CredentialManager is unavailable (e.g. some Chinese ROMs).
     *
     * The Activity context is required by [CredentialManager.getCredential].
     * After obtaining the ID token, processing is delegated to [AuthViewModel.processGoogleIdToken].
     */
    private fun launchGoogleSignIn() {
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@MainActivity)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(AuthViewModel.WEB_CLIENT_ID)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(this@MainActivity, request)
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)
                authVm.processGoogleIdToken(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                // CredentialManager failed – fall back to legacy Google Sign-In
                val gso = GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(AuthViewModel.WEB_CLIENT_ID)
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(this@MainActivity, gso)
                googleSignInLauncher.launch(client.signInIntent)
            }
        }
    }

    // ─── Image picker ───────────────────────────────────

    private fun launchPicker() =
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
}
