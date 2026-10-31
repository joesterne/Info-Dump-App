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

    // This stores the signed-in user's UID - it was named "auth token"
    // although no token was ever saved here.
    fun saveUserUid(uid: String) {
        sharedPreferences.edit().putString("USER_UID", uid).apply()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean("ONBOARDING_COMPLETED", completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean("ONBOARDING_COMPLETED", false)
    }

    fun getUserUid(): String? {
        return sharedPreferences.getString("USER_UID", null)
    }

    fun clearUserUid() {
        sharedPreferences.edit().remove("USER_UID").apply()
    }
}
