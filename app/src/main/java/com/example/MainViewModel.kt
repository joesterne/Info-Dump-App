package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.AppSettings
import com.example.data.ChatMessage
import com.example.data.FocusSession
import com.example.data.Profile
import com.example.data.SessionNote
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MatchWithScore(val profile: Profile, val compatibilityScore: Int)

enum class PomodoroPreset(
    val title: String,
    val description: String,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val longBreakMinutes: Int = 15
) {
    QUICK_SPRINT("Quick Sprint", "10m Focus · 3m Break (Low inertia for starting)", 10, 3, 10),
    CLASSIC("Classic Flow", "25m Focus · 5m Break (Balanced standard)", 25, 5, 15),
    HYPERFOCUS("Deep Dive", "45m Focus · 10m Break (Uninterrupted infodump flow)", 45, 10, 20),
    CUSTOM("Custom Length", "Personalized focus & rest pacing", 20, 5, 15)
}

enum class TimerPhase(val displayName: String) {
    FOCUS("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

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

    fun getNotes(matchId: Int): StateFlow<List<SessionNote>> {
        return repository.getNotesForMatch(matchId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun saveNote(note: SessionNote) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
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

    fun updateArchiveTags(matchId: Int, tags: String) {
        viewModelScope.launch {
            val match = repository.getProfileById(matchId)
            if (match != null) {
                val updatedMatch = match.copy(archiveTags = tags)
                repository.insertProfile(updatedMatch)
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

    // --- Pomodoro Focus Timer ---
    val allFocusSessions: StateFlow<List<FocusSession>> = repository.allFocusSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayFocusMinutes: StateFlow<Int> = allFocusSessions.map { sessions ->
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        sessions.filter { it.sessionType == "FOCUS" && it.timestamp >= startOfDay }
            .sumOf { it.actualDurationMinutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalFocusMinutes: StateFlow<Int> = allFocusSessions.map { sessions ->
        sessions.filter { it.sessionType == "FOCUS" }.sumOf { it.actualDurationMinutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _timerPreset = MutableStateFlow(PomodoroPreset.CLASSIC)
    val timerPreset: StateFlow<PomodoroPreset> = _timerPreset.asStateFlow()

    private val _timerPhase = MutableStateFlow(TimerPhase.FOCUS)
    val timerPhase: StateFlow<TimerPhase> = _timerPhase.asStateFlow()

    private val _timerStatus = MutableStateFlow(TimerStatus.IDLE)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    private val _targetDurationSeconds = MutableStateFlow(25 * 60)
    val targetDurationSeconds: StateFlow<Int> = _targetDurationSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _customFocusMinutes = MutableStateFlow(25)
    val customFocusMinutes: StateFlow<Int> = _customFocusMinutes.asStateFlow()

    private val _customBreakMinutes = MutableStateFlow(5)
    val customBreakMinutes: StateFlow<Int> = _customBreakMinutes.asStateFlow()

    private val _currentSubject = MutableStateFlow("Infodump Exploration")
    val currentSubject: StateFlow<String> = _currentSubject.asStateFlow()

    private val _completedSessionsInCycle = MutableStateFlow(0)
    val completedSessionsInCycle: StateFlow<Int> = _completedSessionsInCycle.asStateFlow()

    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()

    private val _lastSessionDurationMinutes = MutableStateFlow(25)
    val lastSessionDurationMinutes: StateFlow<Int> = _lastSessionDurationMinutes.asStateFlow()

    private val _gentleSoundEnabled = MutableStateFlow(true)
    val gentleSoundEnabled: StateFlow<Boolean> = _gentleSoundEnabled.asStateFlow()

    private val _gentleVibrationEnabled = MutableStateFlow(true)
    val gentleVibrationEnabled: StateFlow<Boolean> = _gentleVibrationEnabled.asStateFlow()

    private var timerJob: Job? = null

    fun setTimerPreset(preset: PomodoroPreset) {
        if (_timerStatus.value == TimerStatus.RUNNING) return
        _timerPreset.value = preset
        updateDurationForCurrentPhase()
    }

    fun setCustomDurations(focusMin: Int, breakMin: Int) {
        _customFocusMinutes.value = focusMin.coerceIn(1, 180)
        _customBreakMinutes.value = breakMin.coerceIn(1, 60)
        if (_timerPreset.value == PomodoroPreset.CUSTOM && _timerStatus.value != TimerStatus.RUNNING) {
            updateDurationForCurrentPhase()
        }
    }

    fun setSubject(subject: String) {
        _currentSubject.value = subject.ifBlank { "General Learning" }
    }

    fun toggleGentleSound() {
        _gentleSoundEnabled.value = !_gentleSoundEnabled.value
    }

    fun toggleGentleVibration() {
        _gentleVibrationEnabled.value = !_gentleVibrationEnabled.value
    }

    private fun updateDurationForCurrentPhase() {
        val totalSecs = when (_timerPhase.value) {
            TimerPhase.FOCUS -> {
                val mins = if (_timerPreset.value == PomodoroPreset.CUSTOM) _customFocusMinutes.value else _timerPreset.value.focusMinutes
                mins * 60
            }
            TimerPhase.SHORT_BREAK -> {
                val mins = if (_timerPreset.value == PomodoroPreset.CUSTOM) _customBreakMinutes.value else _timerPreset.value.breakMinutes
                mins * 60
            }
            TimerPhase.LONG_BREAK -> {
                val mins = if (_timerPreset.value == PomodoroPreset.CUSTOM) 15 else _timerPreset.value.longBreakMinutes
                mins * 60
            }
        }
        _targetDurationSeconds.value = totalSecs
        _remainingSeconds.value = totalSecs
    }

    fun startTimer() {
        if (_timerStatus.value == TimerStatus.RUNNING) return
        _timerStatus.value = TimerStatus.RUNNING
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                if (_timerStatus.value == TimerStatus.RUNNING) {
                    _remainingSeconds.value -= 1
                }
            }
            handleTimerCompletion()
        }
    }

    fun pauseTimer() {
        if (_timerStatus.value == TimerStatus.RUNNING) {
            _timerStatus.value = TimerStatus.PAUSED
        }
    }

    fun resumeTimer() {
        if (_timerStatus.value == TimerStatus.PAUSED) {
            startTimer()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.IDLE
        updateDurationForCurrentPhase()
    }

    // Neurodivergent superpower: In the flow? Add +5 minutes smoothly!
    fun extendTimer(minutes: Int = 5) {
        val additionalSecs = minutes * 60
        _targetDurationSeconds.value += additionalSecs
        _remainingSeconds.value += additionalSecs
        if (_timerStatus.value == TimerStatus.IDLE || _timerStatus.value == TimerStatus.COMPLETED) {
            _timerStatus.value = TimerStatus.PAUSED
        }
    }

    fun skipToNextPhase() {
        timerJob?.cancel()
        advanceToNextPhase()
    }

    private fun handleTimerCompletion() {
        _timerStatus.value = TimerStatus.COMPLETED
        val completedDurationMins = (_targetDurationSeconds.value / 60).coerceAtLeast(1)
        _lastSessionDurationMinutes.value = completedDurationMins

        if (_timerPhase.value == TimerPhase.FOCUS) {
            val nextCycle = (_completedSessionsInCycle.value + 1)
            _completedSessionsInCycle.value = nextCycle
            // Trigger celebration / reflection dialog so user can log notes
            _showCompletionDialog.value = true
        } else {
            advanceToNextPhase()
        }
    }

    fun recordCompletedSession(mood: String, notesSummary: String) {
        viewModelScope.launch {
            val session = FocusSession(
                subject = _currentSubject.value,
                sessionType = "FOCUS",
                targetDurationMinutes = _lastSessionDurationMinutes.value,
                actualDurationMinutes = _lastSessionDurationMinutes.value,
                completed = true,
                reflectionMood = mood,
                notesSummary = notesSummary.ifBlank { null },
                timestamp = System.currentTimeMillis()
            )
            repository.insertFocusSession(session)
            _showCompletionDialog.value = false
            advanceToNextPhase()
        }
    }

    fun dismissCompletionDialog() {
        viewModelScope.launch {
            val session = FocusSession(
                subject = _currentSubject.value,
                sessionType = "FOCUS",
                targetDurationMinutes = _lastSessionDurationMinutes.value,
                actualDurationMinutes = _lastSessionDurationMinutes.value,
                completed = true,
                reflectionMood = "Steady",
                notesSummary = null,
                timestamp = System.currentTimeMillis()
            )
            repository.insertFocusSession(session)
            _showCompletionDialog.value = false
            advanceToNextPhase()
        }
    }

    private fun advanceToNextPhase() {
        _timerStatus.value = TimerStatus.IDLE
        if (_timerPhase.value == TimerPhase.FOCUS) {
            if (_completedSessionsInCycle.value % 4 == 0 && _completedSessionsInCycle.value > 0) {
                _timerPhase.value = TimerPhase.LONG_BREAK
            } else {
                _timerPhase.value = TimerPhase.SHORT_BREAK
            }
        } else {
            _timerPhase.value = TimerPhase.FOCUS
        }
        updateDurationForCurrentPhase()
    }

    fun deleteFocusSession(id: Int) {
        viewModelScope.launch {
            repository.deleteFocusSession(id)
        }
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
