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

    // All per-user documents are keyed by the signed-in user's UID. They used
    // to be written to one hardcoded "my_profile" document, so every user's
    // profile/topics overwrote everyone else's.
    suspend fun saveMyProfile(uid: String?, profile: Profile) {
        val db = firestore ?: return
        if (uid.isNullOrBlank()) {
            Log.w("FirestoreSync", "Not signed in - profile kept local only")
            return
        }
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
            db.collection("users").document(uid).set(data).await()
            Log.d("FirestoreSync", "Successfully saved my profile to Firestore")
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Error saving profile: ${e.message}")
        }
    }

    suspend fun saveMatchResult(uid: String?, myProfile: Profile, matchWithScore: MatchWithScore) {
        val db = firestore ?: return
        try {
            val data = mapOf(
                "uid" to uid,
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
            // Auto-generated document ID: the display name is a field, never
            // the key (same-name profiles overwrote each other, and a "/"
            // in a name corrupted the document path).
            db.collection("users").document().set(data).await()
            Log.d("FirestoreSync", "Successfully saved mock user to Firestore")
        } catch(e: Exception) {
            Log.w("FirestoreSync", "Error saving mock user: ${e.message}")
        }
    }

    suspend fun saveTopics(uid: String?, topics: List<String>) {
        val db = firestore
        if (db == null || uid.isNullOrBlank()) {
            mockSavedTopics.clear()
            mockSavedTopics.addAll(topics)
            Log.d("FirestoreSync", "Successfully saved topics to mock local storage")
            return
        }
        try {
            db.collection("users").document(uid!!)
              .collection("saved_topics").document("list")
              .set(mapOf("topics" to topics)).await()
            Log.d("FirestoreSync", "Successfully saved topics to Firestore")
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Error saving topics: ${e.message}")
        }
    }

    suspend fun getSavedTopics(uid: String?): List<String> {
        val db = firestore
        if (db == null || uid.isNullOrBlank()) {
            return mockSavedTopics.toList()
        }
        return try {
            val snapshot = db.collection("users").document(uid!!)
                             .collection("saved_topics").document("list").get().await()
            val topics = snapshot.get("topics") as? List<String>
            topics ?: emptyList()
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Error getting topics (client may be offline): ${e.message}")
            emptyList()
        }
    }
}
