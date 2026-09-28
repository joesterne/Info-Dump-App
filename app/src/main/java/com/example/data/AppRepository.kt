package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allMatches: Flow<List<Profile>> = appDao.getAllMatches()
    val archivedProfiles: Flow<List<Profile>> = appDao.getArchivedProfiles()
    val archivedSessions: Flow<List<SessionArchive>> = appDao.getArchivedSessions()
    val myProfile: Flow<Profile?> = appDao.getMyProfile()
    val settings: Flow<AppSettings?> = appDao.getSettings()

    suspend fun getProfileById(id: Int): Profile? = appDao.getProfileById(id)

    suspend fun insertProfile(profile: Profile) {
        appDao.insertProfile(profile)
    }

    suspend fun updateSettings(settings: AppSettings) {
        appDao.updateSettings(settings)
    }

    fun getChatsForMatch(matchId: Int): Flow<List<ChatMessage>> {
        return appDao.getChatsForMatch(matchId)
    }

    suspend fun insertChatMessage(chatMessage: ChatMessage) {
        appDao.insertChatMessage(chatMessage)
    }

    fun getNotesForMatch(matchId: Int): Flow<List<SessionNote>> {
        return appDao.getNotesForMatch(matchId)
    }

    suspend fun insertNote(note: SessionNote) {
        appDao.insertNote(note)
    }

    val allFocusSessions: Flow<List<FocusSession>> = appDao.getAllFocusSessions()

    suspend fun insertFocusSession(session: FocusSession) {
        appDao.insertFocusSession(session)
    }

    suspend fun deleteFocusSession(id: Int) {
        appDao.deleteFocusSession(id)
    }
}
