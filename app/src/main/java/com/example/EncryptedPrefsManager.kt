package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPrefsManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString("AUTH_TOKEN", token).apply()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean("ONBOARDING_COMPLETED", completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean("ONBOARDING_COMPLETED", false)
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString("AUTH_TOKEN", null)
    }

    fun clearAuthToken() {
        sharedPreferences.edit().remove("AUTH_TOKEN").apply()
    }
}
