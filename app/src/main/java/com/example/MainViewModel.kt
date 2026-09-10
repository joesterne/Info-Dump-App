package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.AppSettings
import com.example.data.ChatMessage
import com.example.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MatchWithScore(val profile: Profile, val compatibilityScore: Int)

class MainViewModel(
    private val repository: AppRepository,
    val authManager: AuthManager,
    val encryptedPrefsManager: EncryptedPrefsManager
) : ViewModel() {

    private val firestoreSyncManager = com.example.data.FirestoreSyncManager()

    private val _savedTopics = MutableStateFlow<List<String>>(emptyList())
    val savedTopics: StateFlow<List<String>> = _savedTopics.asStateFlow()

    private val _currentUserUid = MutableStateFlow<String?>(null)
    val currentUserUid: StateFlow<String?> = _currentUserUid.asStateFlow()

    init {
        viewModelScope.launch {
            _savedTopics.value = firestoreSyncManager.getSavedTopics()
            _currentUserUid.value = authManager.getCurrentUserUid()
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            val uid = authManager.signInWithGoogle()
            if (uid != null) {
                _currentUserUid.value = uid
                encryptedPrefsManager.saveAuthToken(uid)
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        encryptedPrefsManager.clearAuthToken()
        _currentUserUid.value = null
    }

    fun addSavedTopic(topic: String) {
        val current = _savedTopics.value.toMutableList()
        if (!current.contains(topic)) {
            current.add(topic)
            _savedTopics.value = current
            viewModelScope.launch {
                firestoreSyncManager.saveTopics(current)
            }
        }
    }

    fun removeSavedTopic(topic: String) {
        val current = _savedTopics.value.toMutableList()
        if (current.remove(topic)) {
            _savedTopics.value = current
            viewModelScope.launch {
                firestoreSyncManager.saveTopics(current)
            }
        }
    }

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    val myProfile: StateFlow<Profile?> = repository.myProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMatchesWithScores: StateFlow<List<MatchWithScore>> = combine(
        repository.allMatches,
        repository.myProfile
    ) { matches, myProfile ->
        if (myProfile == null) {
            matches.map { MatchWithScore(it, 0) }
        } else {
            matches.map { match ->
                MatchWithScore(match, calculateCompatibilityScore(myProfile, match))
            }.sortedByDescending { it.compatibilityScore }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedSessionsWithScores: StateFlow<List<SessionArchiveWithScore>> = combine(
        repository.archivedSessions,
        repository.myProfile
    ) { archived, myProfile ->
        if (myProfile == null) {
            archived.map { SessionArchiveWithScore(it.profile, 0, it.lastMessageTimestamp) }
        } else {
            archived.map { session ->
                SessionArchiveWithScore(
                    profile = session.profile,
                    compatibilityScore = calculateCompatibilityScore(myProfile, session.profile),
                    lastMessageTimestamp = session.lastMessageTimestamp
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateCompatibilityScore(me: Profile, match: Profile): Int {
        var score = 0
        
        // 1. Role compatibility: One wants to infodump, one wants to listen
        if (me.isHyperfixating != match.isHyperfixating) {
            score += 30
        }

        // 2. Energy Level / Bandwidth match
        if (me.energyLevel == match.energyLevel) {
            score += 15 // Perfect energy match
        } else if ((me.energyLevel == "Low" && match.energyLevel == "High") || (me.energyLevel == "High" && match.energyLevel == "Low")) {
            score -= 10 // Energy mismatch
        }

        // 3. Subject compatibility
        val mySubject = me.subject.trim().lowercase()
        val matchSubject = match.subject.trim().lowercase()
        if (mySubject.isNotEmpty() && mySubject == matchSubject) {
            score += 35
        } else if (mySubject.isNotEmpty() && matchSubject.contains(mySubject) || mySubject.contains(matchSubject)) {
            score += 20 // Partial match
        }

        // 4. Shared interest tags
        val myTags = me.tags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val matchTags = match.tags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val sharedTags = myTags.intersect(matchTags.toSet())
        score += sharedTags.size * 10
        
        // Cap score at 100 for presentation
        return score.coerceIn(0, 100)
    }

    val settings: StateFlow<AppSettings> = repository.settings
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateSettings(isDarkMode: Boolean, textSizeMultiplier: Float) {
        viewModelScope.launch {
            repository.updateSettings(AppSettings(id = 1, isDarkMode = isDarkMode, textSizeMultiplier = textSizeMultiplier))
        }
    }

    fun saveMyProfile(name: String, isHyperfixating: Boolean, subject: String, tags: String, energyLevel: String = "Medium") {
        viewModelScope.launch {
            val existing = myProfile.value
            val profile = Profile(
                id = existing?.id ?: 0,
                name = name,
                isHyperfixating = isHyperfixating,
                subject = subject,
                tags = tags,
                isMyProfile = true,
                rating = 5.0f,
                energyLevel = energyLevel
            )
            repository.insertProfile(profile)
            firestoreSyncManager.saveMyProfile(profile)
        }
    }

    fun addMockMatch(name: String, isHyperfixating: Boolean, subject: String, tags: String, energyLevel: String = "Medium") {
        viewModelScope.launch {
            val profile = Profile(
                name = name,
                isHyperfixating = isHyperfixating,
                subject = subject,
                tags = tags,
                isMyProfile = false,
                rating = 4.8f,
                isOnline = Math.random() > 0.4,
                energyLevel = energyLevel
            )
            repository.insertProfile(profile)
            firestoreSyncManager.saveMockUser(profile)
        }
    }

    fun getChats(matchId: Int): StateFlow<List<ChatMessage>> {
        return repository.getChatsForMatch(matchId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun recordMatchInteraction(matchId: Int) {
        viewModelScope.launch {
            val me = myProfile.value ?: return@launch
            val match = repository.getProfileById(matchId) ?: return@launch
            val score = calculateCompatibilityScore(me, match)
            val matchWithScore = MatchWithScore(match, score)
            firestoreSyncManager.saveMatchResult(me, matchWithScore)
        }
    }

    fun blockUser(matchId: Int) {
        viewModelScope.launch {
            val match = repository.getProfileById(matchId)
            if (match != null) {
                val blockedMatch = match.copy(isBlocked = true)
                repository.insertProfile(blockedMatch)
            }
        }
    }

    fun submitSessionFeedback(matchId: Int, quality: Int, clarity: Int) {
        viewModelScope.launch {
            val match = repository.getProfileById(matchId)
            if (match != null) {
                val feedbackAverage = (quality + clarity) / 2.0f
                val newRating = (match.rating + feedbackAverage) / 2.0f
                val updatedMatch = match.copy(rating = newRating)
                repository.insertProfile(updatedMatch)
            }
        }
    }

    fun sendTimeProposal(matchId: Int, proposedTime: Long) {
        viewModelScope.launch {
            repository.insertChatMessage(
                ChatMessage(
                    matchId = matchId,
                    message = "I've suggested a time for our session.",
                    isFromMe = true,
                    proposedTime = proposedTime
                )
            )
            _isTyping.value = true
            kotlinx.coroutines.delay(1500)
            repository.insertChatMessage(
                ChatMessage(
                    matchId = matchId,
                    message = "That time works for me! I'll put it on my calendar.",
                    isFromMe = false
                )
            )
            _isTyping.value = false
        }
    }

    fun sendMessage(matchId: Int, message: String) {
        viewModelScope.launch {
            repository.insertChatMessage(
                ChatMessage(matchId = matchId, message = message, isFromMe = true)
            )
            _isTyping.value = true
            // Mock auto-reply
            kotlinx.coroutines.delay(1500)
            repository.insertChatMessage(
                ChatMessage(matchId = matchId, message = "That is so fascinating! Tell me more.", isFromMe = false)
            )
            _isTyping.value = false
        }
    }

    suspend fun getProfile(id: Int): Profile? {
        return repository.getProfileById(id)
    }

    class Factory(
        private val repository: AppRepository,
        private val authManager: AuthManager,
        private val encryptedPrefsManager: EncryptedPrefsManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, authManager, encryptedPrefsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
