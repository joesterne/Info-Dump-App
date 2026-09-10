package com.example

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): String? {
        val webClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID
        if (webClientId.isEmpty() || webClientId == "mock_client_id") {
            Log.e("AuthManager", "Please configure FIREBASE_WEB_CLIENT_ID in your Secrets")
            return null
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is GoogleIdTokenCredential) {
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                authResult.user?.uid
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign in failed", e)
            null
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }
}
