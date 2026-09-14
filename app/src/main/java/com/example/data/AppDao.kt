package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Profiles
    @Query("SELECT * FROM profiles WHERE isMyProfile = 0 AND isBlocked = 0 ORDER BY id DESC")
    fun getAllMatches(): Flow<List<Profile>>

    @Query("""
        SELECT p.* FROM profiles p 
        WHERE p.isMyProfile = 0 AND p.isBlocked = 0 
        AND EXISTS (SELECT 1 FROM chat_messages c WHERE c.matchId = p.id)
        ORDER BY (SELECT MAX(timestamp) FROM chat_messages c WHERE c.matchId = p.id) DESC
    """)
    fun getArchivedProfiles(): Flow<List<Profile>>

    @Query("""
        SELECT p.*, (SELECT MAX(timestamp) FROM chat_messages c WHERE c.matchId = p.id) as lastMessageTimestamp 
        FROM profiles p 
        WHERE p.isMyProfile = 0 AND p.isBlocked = 0 
        AND EXISTS (SELECT 1 FROM chat_messages c WHERE c.matchId = p.id)
        ORDER BY lastMessageTimestamp DESC
    """)
    fun getArchivedSessions(): Flow<List<SessionArchive>>

    @Query("SELECT * FROM profiles WHERE isMyProfile = 1 LIMIT 1")
    fun getMyProfile(): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Int): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    // Chats
    @Query("SELECT * FROM chat_messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getChatsForMatch(matchId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(chatMessage: ChatMessage)

    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: AppSettings)

    // Notes
    @Query("SELECT * FROM session_notes WHERE matchId = :matchId ORDER BY timestamp DESC")
    fun getNotesForMatch(matchId: Int): Flow<List<SessionNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SessionNote)
}
