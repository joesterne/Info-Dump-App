package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isHyperfixating: Boolean, // true = wants to infodump, false = wants to listen
    val subject: String,
    val tags: String, // comma separated
    val rating: Float = 5.0f,
    val isMyProfile: Boolean = false,
    val isBlocked: Boolean = false,
    val isOnline: Boolean = false,
    val energyLevel: String = "Medium", // Low, Medium, High
    val archiveTags: String = "" // Custom tags for archived sessions
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int, // references Profile.id
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean,
    val proposedTime: Long? = null
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val isDarkMode: Boolean = true,
    val textSizeMultiplier: Float = 1.0f
)

@Entity(tableName = "session_notes")
data class SessionNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val content: String, // Text content (can be empty if just audio)
    val audioFilePath: String?, // Path to the recorded audio file, if any
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val sessionType: String = "FOCUS", // "FOCUS", "SHORT_BREAK", "LONG_BREAK"
    val targetDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val completed: Boolean = true,
    val reflectionMood: String? = null,
    val notesSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
