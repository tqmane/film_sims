package com.tqmane.filmsim

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.core.os.bundleOf
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.ktx.Firebase
import com.google.firebase.analytics.ktx.analytics
import com.tqmane.filmsim.di.UpdateCheckerWrapper
import com.tqmane.filmsim.ui.AuthViewModel
import com.tqmane.filmsim.ui.EditorViewModel
import com.tqmane.filmsim.ui.editor.EditorScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single Activity – acts only as the Compose host and DI entry-point.
 * All UI logic lives in [EditorScreen] composable and [EditorViewModel].
 *
 * CredentialManager is intentionally kept here because it requires an Activity
 * context (per the Jetpack Credential Manager API spec). The ViewModel only
 * handles the Firebase auth step after a token is obtained.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val EVENT_IMAGE_OPENED = "image_opened"
        private const val PARAM_SOURCE = "source"
        private const val PARAM_SCHEME = "scheme"
        private const val SOURCE_PICKER = "picker"
        private const val SOURCE_SHARE = "share"
    }

    private val vm: EditorViewModel by viewModels()
    private val authVm: AuthViewModel by viewModels()

    @Inject
    lateinit var updateChecker: UpdateCheckerWrapper

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                logImageOpened(SOURCE_PICKER, it)
                vm.loadImage(it)
            }
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
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        setContent {
            EditorScreen(
                viewModel = vm,
                authViewModel = authVm,
                onPickImage = { launchPicker() },
                onSignIn = { launchGoogleSignIn() },
                onSignOut = { authVm.signOut() }
            )
        }

        vm.checkForUpdates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
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
                val webClientId = getString(R.string.default_web_client_id)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
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
                val webClientId = getString(R.string.default_web_client_id)
                val gso = GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
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

    private fun handleIncomingIntent(incomingIntent: Intent) {
        val sharedImageUri = extractIncomingImageUri(incomingIntent) ?: return
        logImageOpened(SOURCE_SHARE, sharedImageUri)
        vm.loadImage(sharedImageUri)
    }

    private fun extractIncomingImageUri(incomingIntent: Intent): Uri? {
        val uri = when (incomingIntent.action) {
            Intent.ACTION_SEND -> {
                IntentCompat.getParcelableExtra(incomingIntent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: incomingIntent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(
                    incomingIntent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )?.firstOrNull()
                    ?: incomingIntent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }

            else -> null
        } ?: return null

        val mimeType = incomingIntent.type ?: contentResolver.getType(uri)
        return if (mimeType == null || mimeType.startsWith("image/")) uri else null
    }

    private fun logImageOpened(source: String, uri: Uri) {
        Firebase.analytics.logEvent(
            EVENT_IMAGE_OPENED,
            bundleOf(
                PARAM_SOURCE to source,
                PARAM_SCHEME to (uri.scheme ?: "unknown")
            )
        )
    }
}
