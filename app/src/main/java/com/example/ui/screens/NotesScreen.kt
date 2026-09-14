package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.data.SessionNote
import com.example.data.GeminiService
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: MainViewModel, initialNoteText: String? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Notes are for matchId 0 for general prep, or we can fetch all notes.
    val notes by viewModel.getNotes(0).collectAsStateWithLifecycle(initialValue = emptyList())
    
    var noteText by remember { mutableStateOf(initialNoteText ?: "") }
    
    // Recording state
    var isRecording by remember { mutableStateOf(false) }
    var currentAudioPath by remember { mutableStateOf<String?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, handle in UI
        }
    }

    val hasRecordPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
            mediaPlayer?.release()
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Prep for your infodumps. Record voice notes or import text.", style = MaterialTheme.typography.bodyMedium)
            
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier.fillMaxWidth().weight(0.3f),
                placeholder = { Text("Type or paste your notes here...") },
                maxLines = 5
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop Recording")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
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
                        }
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "Record Audio")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Record Voice")
                    }
                }
                
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = "Save Note")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
            
            if (currentAudioPath != null && !isRecording) {
                Text("Audio recorded ready to save.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }

            HorizontalDivider()
            
            Text("Saved Notes", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(0.7f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (note.content.isNotBlank()) {
                                Text(note.content, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (note.audioFilePath != null) {
                                Button(
                                    onClick = {
                                        if (isPlaying) {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            isPlaying = false
                                        } else {
                                            try {
                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(note.audioFilePath)
                                                    prepare()
                                                    start()
                                                    setOnCompletionListener {
                                                        isPlaying = false
                                                    }
                                                }
                                                isPlaying = true
                                            } catch (e: IOException) {
                                                Log.e("NotesScreen", "Playback failed", e)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = "Play/Stop")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isPlaying) "Stop" else "Play Audio Note")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
