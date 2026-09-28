package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.GeminiService
import com.example.data.SessionNote
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: MainViewModel, initialNoteText: String? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    
    // Notes are for matchId 0 for general prep, or we can fetch all notes.
    val notes by viewModel.getNotes(0).collectAsStateWithLifecycle(initialValue = emptyList())
    
    var noteText by remember { mutableStateOf(initialNoteText ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    
    // Recording state
    var isRecording by remember { mutableStateOf(false) }
    var currentAudioPath by remember { mutableStateOf<String?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    
    // Audio memo player state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingAudioNoteId by remember { mutableStateOf<Int?>(null) }
    
    // Gemini transcription state
    var isTranscribing by remember { mutableStateOf(false) }
    
    // Text-To-Speech (TTS) state for neurodivergent auditory accessibility
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var speakingTtsNoteId by remember { mutableStateOf<Int?>(null) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        }
    }

    val hasRecordPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    // Stop audio player helper
    val stopAudioPlayer: () -> Unit = {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("NotesScreen", "Stop audio player failed", e)
        }
        mediaPlayer = null
        playingAudioNoteId = null
    }

    // Stop TTS helper
    val stopTts: () -> Unit = {
        try {
            ttsInstance?.stop()
        } catch (e: Exception) {
            Log.e("NotesScreen", "Stop TTS failed", e)
        }
        speakingTtsNoteId = null
    }

    // Initialize TTS
    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(speechRate)
                isTtsReady = true
            } else {
                Log.e("NotesScreen", "TTS Initialization failed with code $status")
            }
        }
        
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    speakingTtsNoteId = utteranceId?.toIntOrNull()
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (speakingTtsNoteId == utteranceId?.toIntOrNull()) {
                        speakingTtsNoteId = null
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    if (speakingTtsNoteId == utteranceId?.toIntOrNull()) {
                        speakingTtsNoteId = null
                    }
                }
            }
        })
        ttsInstance = tts

        onDispose {
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            stopAudioPlayer()
            tts?.stop()
            tts?.shutdown()
            ttsInstance = null
        }
    }

    // Update speech rate when user changes it
    LaunchedEffect(speechRate, isTtsReady) {
        if (isTtsReady) {
            ttsInstance?.setSpeechRate(speechRate)
        }
    }

    val speakText = { id: Int, text: String ->
        if (isTtsReady && ttsInstance != null) {
            stopAudioPlayer()
            stopTts()
            ttsInstance?.setSpeechRate(speechRate)
            val params = Bundle()
            ttsInstance?.speak(text, TextToSpeech.QUEUE_FLUSH, params, id.toString())
            speakingTtsNoteId = id
        }
    }

    // Filter notes based on search query
    val filteredNotes = remember(notes, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            notes
        } else {
            notes.filter { it.content.contains(trimmed, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Notes", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Prep for your infodumps. Record voice notes, transcribe via Gemini, or search keywords across your knowledge nuggets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Note input field
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 130.dp),
                placeholder = { Text("Type, paste Apple Notes, or record voice notes...") }
            )
            
            // Action controls (Record, Read Draft Aloud, Save)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Record Voice button
                if (isRecording) {
                    Button(
                        onClick = {
                            try {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                mediaRecorder = null
                                isRecording = false
                            } catch (e: Exception) {
                                Log.e("NotesScreen", "Stop recording failed", e)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop Recording")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            if (!hasRecordPermission) {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                val audioFile = File(context.cacheDir, "audio_note_${System.currentTimeMillis()}.mp4")
                                currentAudioPath = audioFile.absolutePath
                                
                                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaRecorder()
                                }
                                
                                try {
                                    recorder.apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        setOutputFile(currentAudioPath)
                                        prepare()
                                        start()
                                    }
                                    mediaRecorder = recorder
                                    isRecording = true
                                } catch (e: IOException) {
                                    Log.e("NotesScreen", "Recording failed", e)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "Record Audio")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record")
                    }
                }

                // Listen to Draft Aloud button (if draft has text)
                if (noteText.isNotBlank()) {
                    val isDraftSpeaking = speakingTtsNoteId == -1
                    FilledTonalButton(
                        onClick = {
                            if (isDraftSpeaking) {
                                stopTts()
                            } else {
                                speakText(-1, noteText)
                            }
                        }
                    ) {
                        Icon(
                            if (isDraftSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isDraftSpeaking) "Stop reading draft" else "Listen to draft"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDraftSpeaking) "Stop" else "Listen")
                    }
                }
                
                // Save Note button
                Button(
                    onClick = {
                        if (noteText.isNotBlank() || currentAudioPath != null) {
                            coroutineScope.launch {
                                isTranscribing = true
                                var finalContent = noteText
                                val audioPath = currentAudioPath
                                
                                if (audioPath != null) {
                                    val transcription = GeminiService.transcribeAudio(File(audioPath))
                                    if (!transcription.startsWith("Error:")) {
                                        finalContent = if (finalContent.isNotBlank()) {
                                            "$finalContent\n\n[Transcript]: $transcription"
                                        } else {
                                            transcription
                                        }
                                    } else {
                                        finalContent = if (finalContent.isNotBlank()) {
                                            "$finalContent\n\n[Transcription Failed]"
                                        } else {
                                            "[Transcription Failed]"
                                        }
                                    }
                                }

                                val newNote = SessionNote(
                                    matchId = 0, // General notes
                                    content = finalContent,
                                    audioFilePath = audioPath
                                )
                                viewModel.saveNote(newNote)
                                noteText = ""
                                currentAudioPath = null
                                isTranscribing = false
                            }
                        }
                    },
                    enabled = !isTranscribing
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI...", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = "Save Note")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            }
            
            if (currentAudioPath != null && !isRecording) {
                Text(
                    "Voice memo captured. Tap 'Save' to transcribe and save.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            HorizontalDivider()
            
            // Saved Notes Header & Speed Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Hearing,
                        contentDescription = "Auditory Accessibility",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Saved Notes", style = MaterialTheme.typography.titleMedium)
                }

                // Speech Rate Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Speed:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(0.8f to "0.8x", 1.0f to "1.0x", 1.25f to "1.25x", 1.5f to "1.5x").forEach { (rate, label) ->
                        val selected = speechRate == rate
                        FilterChip(
                            selected = selected,
                            onClick = {
                                speechRate = rate
                                if (speakingTtsNoteId != null) {
                                    ttsInstance?.setSpeechRate(rate)
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Visual Keyword Search Bar
            if (notes.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search keywords in notes & transcripts...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search keywords")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Match count feedback bar when searching
                if (searchQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredNotes.size} note${if (filteredNotes.size != 1) "s" else ""} found with \"${searchQuery.trim()}\"",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { searchQuery = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            
            // Notes List
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No notes saved yet. Record a voice memo or paste text above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredNotes.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "No knowledge nuggets found for \"${searchQuery.trim()}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = { searchQuery = "" }) {
                            Text("Clear Search")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        val isThisTtsSpeaking = speakingTtsNoteId == note.id
                        val isThisAudioPlaying = playingAudioNoteId == note.id

                        // Count matching keyword occurrences for visual badges
                        val matchCount = remember(note.content, searchQuery) {
                            val trimmed = searchQuery.trim()
                            if (trimmed.isEmpty()) 0
                            else {
                                var count = 0
                                var idx = 0
                                while (idx < note.content.length) {
                                    val found = note.content.indexOf(trimmed, idx, ignoreCase = true)
                                    if (found != -1) {
                                        count++
                                        idx = found + trimmed.length
                                    } else {
                                        break
                                    }
                                }
                                count
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isThisTtsSpeaking) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else if (isThisAudioPlaying) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Header row inside card (Active badges & keyword match pills)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Active speech / audio status badge
                                    if (isThisTtsSpeaking) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Reading aloud",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Reading aloud (${speechRate}x)...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    } else if (isThisAudioPlaying) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = "Playing recording",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Playing recording...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    // Keyword match counter pill
                                    if (matchCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                text = "$matchCount match${if (matchCount != 1) "es" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (note.content.isNotBlank()) {
                                    // Highlighted text rendering
                                    val annotatedContent = remember(note.content, searchQuery, isThisTtsSpeaking) {
                                        buildHighlightedText(
                                            fullText = note.content,
                                            query = searchQuery,
                                            highlightColor = Color(0xFFFFD54F), // Vibrant golden highlight for keyword visibility
                                            highlightTextColor = Color(0xFF261900)
                                        )
                                    }

                                    Text(
                                        text = annotatedContent,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isThisTtsSpeaking) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Text-To-Speech (Read Aloud) Button
                                    if (note.content.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                if (isThisTtsSpeaking) {
                                                    stopTts()
                                                } else {
                                                    speakText(note.id, note.content)
                                                }
                                            },
                                            colors = if (isThisTtsSpeaking) {
                                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            } else {
                                                ButtonDefaults.filledTonalButtonColors()
                                            }
                                        ) {
                                            Icon(
                                                if (isThisTtsSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = if (isThisTtsSpeaking) "Stop reading aloud" else "Read aloud note"
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isThisTtsSpeaking) "Stop" else "Read Aloud")
                                        }
                                    }

                                    // Original audio recording playback
                                    if (note.audioFilePath != null) {
                                        OutlinedButton(
                                            onClick = {
                                                if (isThisAudioPlaying) {
                                                    stopAudioPlayer()
                                                } else {
                                                    stopTts()
                                                    stopAudioPlayer()
                                                    try {
                                                        val player = MediaPlayer().apply {
                                                            setDataSource(note.audioFilePath)
                                                            prepare()
                                                            start()
                                                            setOnCompletionListener {
                                                                mainHandler.post {
                                                                    playingAudioNoteId = null
                                                                    mediaPlayer?.release()
                                                                    mediaPlayer = null
                                                                }
                                                            }
                                                        }
                                                        mediaPlayer = player
                                                        playingAudioNoteId = note.id
                                                    } catch (e: IOException) {
                                                        Log.e("NotesScreen", "Audio playback failed", e)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                if (isThisAudioPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                                contentDescription = if (isThisAudioPlaying) "Stop audio recording" else "Play original recording"
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isThisAudioPlaying) "Stop Audio" else "Play Memo")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Builds an AnnotatedString highlighting all case-insensitive occurrences of [query] within [fullText].
 */
private fun buildHighlightedText(
    fullText: String,
    query: String,
    highlightColor: Color,
    highlightTextColor: Color
): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        return AnnotatedString(fullText)
    }

    return buildAnnotatedString {
        var startIndex = 0
        val spanStyle = SpanStyle(
            background = highlightColor,
            color = highlightTextColor,
            fontWeight = FontWeight.ExtraBold
        )

        while (startIndex < fullText.length) {
            val matchIndex = fullText.indexOf(trimmed, startIndex, ignoreCase = true)
            if (matchIndex == -1) {
                append(fullText.substring(startIndex))
                break
            } else {
                if (matchIndex > startIndex) {
                    append(fullText.substring(startIndex, matchIndex))
                }
                val endIndex = matchIndex + trimmed.length
                withStyle(spanStyle) {
                    append(fullText.substring(matchIndex, endIndex))
                }
                startIndex = endIndex
            }
        }
    }
}
