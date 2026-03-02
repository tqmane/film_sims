package com.tqmane.filmsim.ui.auth

/**
 * Authentication state for the signed-in user.
 */
data class AuthState(
    val isSignedIn: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
