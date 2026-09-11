package com.example.data

import android.util.Log
import com.example.MatchWithScore
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager {
    
    private val firestore: FirebaseFirestore? by lazy {
        try {
            Firebase.firestore
        } catch (e: IllegalStateException) {
            Log.w("FirestoreSync", "Firebase not initialized. Missing config in .env? Using in-memory mock.")
            null
        }
    }

    private val mockSavedTopics = mutableListOf<String>()

    suspend fun saveMyProfile(profile: Profile) {
        val db = firestore ?: return
        try {
            val data = mapOf(
                "name" to profile.name,
                "isHyperfixating" to profile.isHyperfixating,
                "subject" to profile.subject,
                "tags" to profile.tags,
                "rating" to profile.rating,
                "energyLevel" to profile.energyLevel,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("users").document("my_profile").set(data).await()
            Log.d("FirestoreSync", "Successfully saved my profile to Firestore")
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Error saving profile: ${e.message}")
        }
    }

    suspend fun saveMatchResult(myProfile: Profile, matchWithScore: MatchWithScore) {
        val db = firestore ?: return
        try {
            val data = mapOf(
                "myName" to myProfile.name,
                "matchName" to matchWithScore.profile.name,
                "matchSubject" to matchWithScore.profile.subject,
                "compatibilityScore" to matchWithScore.compatibilityScore,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("match_history").add(data).await()
            Log.d("FirestoreSync", "Successfully saved match result to Firestore")
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Error saving match: ${e.message}")
        }
    }
    
    suspend fun saveMockUser(profile: Profile) {
        val db = firestore ?: return
        try {
            val data = mapOf(
                "name" to profile.name,
                "isHyperfixating" to profile.isHyperfixating,
                "subject" to profile.subject,
                "tags" to profile.tags,
                "rating" to profile.rating,
                "energyLevel" to profile.energyLevel
            )
            db.collection("users").document(profile.name).set(data).await()
            Log.d("FirestoreSync", "Successfully saved mock user to Firestore")
        } catch(e: Exception) {
            Log.w("FirestoreSync", "Error saving mock user: ${e.message}")
        }
    }

    suspend fun saveTopics(topics: List<String>) {
        val db = firestore
        if (db == null) {
            mockSavedTopics.clear()
            mockSavedTopics.addAll(topics)
            Log.d("FirestoreSync", "Successfully saved topics to mock local storage")
            return
        }
        try {
            db.collection("users").document("my_profile")
              .collection("saved_topics").document("list")
              .set(mapOf("topics" to topics)).await()
            Log.d("FirestoreSync", "Successfully saved topics to Firestore")
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Error saving topics: ${e.message}")
        }
    }

    suspend fun getSavedTopics(): List<String> {
        val db = firestore
        if (db == null) {
            return mockSavedTopics.toList()
        }
        return try {
            val snapshot = db.collection("users").document("my_profile")
                             .collection("saved_topics").document("list").get().await()
            val topics = snapshot.get("topics") as? List<String>
            topics ?: emptyList()
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Error getting topics (client may be offline): ${e.message}")
            emptyList()
        }
    }
}
