package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (FirebaseApp.getApps(this).isEmpty()) {
            val apiKey = BuildConfig.FIREBASE_API_KEY
            val appId = BuildConfig.FIREBASE_APP_ID
            val projectId = BuildConfig.FIREBASE_PROJECT_ID
            
            if (apiKey != "mock_api_key" && apiKey.isNotBlank() && 
                appId != "mock_app_id" && appId.isNotBlank() && 
                projectId != "mock_project_id" && projectId.isNotBlank()) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.d("MyApplication", "Firebase initialized with custom options")
                } catch (e: Exception) {
                    Log.e("MyApplication", "Failed to initialize Firebase", e)
                }
            } else {
                Log.w("MyApplication", "Firebase not initialized. Missing config in .env")
            }
        }
    }
}
