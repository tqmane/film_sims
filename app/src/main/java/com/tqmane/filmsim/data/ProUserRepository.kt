package com.tqmane.filmsim.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks whether the currently signed-in user's email exists in the
 * Firestore `pro_users` collection.
 *
 * Document structure:  pro_users/{email}  (document ID = Gmail address)
 */
@Singleton
class ProUserRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    /**
     * Query Firestore to see if [email] is a registered Pro user.
     * Updates [isProUser] accordingly.
     */
    suspend fun checkProStatus(email: String?) {
        if (email.isNullOrBlank()) {
            _isProUser.value = false
            return
        }
        try {
            val doc = firestore.collection("pro_users")
                .document(email.lowercase())
                .get()
                .await()
            _isProUser.value = doc.exists()
        } catch (_: Exception) {
            _isProUser.value = false
        }
    }

    /** Reset to non-pro (e.g. on sign-out). */
    fun clearProStatus() {
        _isProUser.value = false
    }
}
