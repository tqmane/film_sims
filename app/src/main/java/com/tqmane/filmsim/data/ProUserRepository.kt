package com.tqmane.filmsim.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.tqmane.filmsim.util.SecurityManager

/**
 * Checks whether the currently signed-in user's email exists in the
 * Firestore `pro_users` or `android` collection.
 *
 * Firestore構成:
 *   pro_users/{docId} または android/{docId} (docIdは任意のID)
 *   emails: array  ← Proユーザーのメールアドレスのリスト
 *
 * 例:
 *   android/ID_list
 *     emails: ["example@example.com", "example2@example.com"]
 *
 * Firestoreセキュリティルール:
 *   match /pro_users/{docId} {
 *     allow list: if request.auth != null;  // array-contains クエリに必要
 *     allow write: if false;
 *   }
 *   match /android/{docId} {
 *     allow list: if request.auth != null;
 *     allow write: if false;
 *   }
 */
@Singleton
class ProUserRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ProUserRepository"
    }

    private val firestore = FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), "login")

    private val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _licenseMismatchVersion = MutableStateFlow<String?>(null)
    val licenseMismatchVersion: StateFlow<String?> = _licenseMismatchVersion.asStateFlow()

    private val _isPermanentLicense = MutableStateFlow(false)
    val isPermanentLicense: StateFlow<Boolean> = _isPermanentLicense.asStateFlow()

    /** True when the last pro check failed due to a network/Firestore error (not "non-Pro"). */
    private val _proCheckNetworkError = MutableStateFlow(false)
    val proCheckNetworkError: StateFlow<Boolean> = _proCheckNetworkError.asStateFlow()

    /**
     * Firestore の pro_users および android コレクションを email フィールドで検索する。
     * ドキュメントが見つかれば Pro ユーザーと判定。
     */
    suspend fun checkProStatus(email: String?) {
        Log.d(TAG, "checkProStatus called: email='$email'")
        if (email.isNullOrBlank()) {
            _isProUser.value = false
            _licenseMismatchVersion.value = null
            _isPermanentLicense.value = false
            return
        }
        val normalizedEmail = email.trim().lowercase()
        Log.d(TAG, "Querying pro_users and android where email == '$normalizedEmail'")

        var hadNetworkError = false
        try {
            val proUsersDocs = try {
                firestore.collection("pro_users")
                    .whereArrayContains("emails", normalizedEmail)
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query pro_users: ${e.message}")
                hadNetworkError = true
                emptyList()
            }

            val androidDocs = try {
                firestore.collection("android")
                    .whereArrayContains("emails", normalizedEmail)
                    .get()
                    .await()
                    .documents
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query android: ${e.message}")
                hadNetworkError = true
                emptyList()
            }

            val allDocs = proUsersDocs + androidDocs
            
            var found = false
            var mismatchVersion: String? = null
            var isPermanent = false
            
            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                ""
            }

            for (doc in allDocs) {
                val docId = doc.id
                if (docId == "ID_list") {
                    found = true
                    mismatchVersion = null
                    isPermanent = true
                    break
                } else if (docId == currentVersion) {
                    found = true
                    mismatchVersion = null
                } else {
                    // Record the mismatched version we found
                    if (!found) mismatchVersion = docId
                }
            }

            Log.d(TAG, "Query result: found=$found, mismatchVersion=$mismatchVersion, docCount=${allDocs.size}")
            
            // SECURITY CHECK: Verify app environment integrity before enabling Pro features
            if (found && !SecurityManager.isEnvironmentTrusted(context)) {
                Log.e(TAG, "Environment trust check failed! Denying Pro access.")
                found = false
            }
            
            _isProUser.value = found
            _licenseMismatchVersion.value = if (!found) mismatchVersion else null
            _isPermanentLicense.value = isPermanent
            // Only report network error if we got no docs at all (both queries failed)
            _proCheckNetworkError.value = hadNetworkError && allDocs.isEmpty()
            
        } catch (e: Exception) {
            Log.e(TAG, "Firestore query FAILED: ${e.javaClass.simpleName}: ${e.message}")
            _isProUser.value = false
            _licenseMismatchVersion.value = null
            _isPermanentLicense.value = false
            _proCheckNetworkError.value = true
        } finally {
            Log.d(TAG, "Final isProUser=${_isProUser.value}, mismatchVersion=${_licenseMismatchVersion.value}")
        }
    }

    fun clearProStatus() {
        _isProUser.value = false
        _licenseMismatchVersion.value = null
        _isPermanentLicense.value = false
        _proCheckNetworkError.value = false
    }
}
