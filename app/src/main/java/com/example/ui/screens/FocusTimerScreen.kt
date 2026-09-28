package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.PomodoroPreset
import com.example.TimerPhase
import com.example.TimerStatus
import com.example.data.FocusSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    viewModel: MainViewModel,
    onNavigateToNotes: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val timerStatus by viewModel.timerStatus.collectAsStateWithLifecycle()
    val timerPhase by viewModel.timerPhase.collectAsStateWithLifecycle()
    val timerPreset by viewModel.timerPreset.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val targetDurationSeconds by viewModel.targetDurationSeconds.collectAsStateWithLifecycle()
    val currentSubject by viewModel.currentSubject.collectAsStateWithLifecycle()
    val completedSessionsInCycle by viewModel.completedSessionsInCycle.collectAsStateWithLifecycle()
    val showCompletionDialog by viewModel.showCompletionDialog.collectAsStateWithLifecycle()
    val lastSessionDurationMinutes by viewModel.lastSessionDurationMinutes.collectAsStateWithLifecycle()
    val gentleSoundEnabled by viewModel.gentleSoundEnabled.collectAsStateWithLifecycle()
    val gentleVibrationEnabled by viewModel.gentleVibrationEnabled.collectAsStateWithLifecycle()

    val allFocusSessions by viewModel.allFocusSessions.collectAsStateWithLifecycle()
    val todayFocusMinutes by viewModel.todayFocusMinutes.collectAsStateWithLifecycle()
    val totalFocusMinutes by viewModel.totalFocusMinutes.collectAsStateWithLifecycle()
    val savedTopics by viewModel.savedTopics.collectAsStateWithLifecycle()

    var showTopicDialog by remember { mutableStateOf(false) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showQuickNoteDialog by remember { mutableStateOf(false) }
    var quickNoteText by remember { mutableStateOf("") }

    // Play subtle audio/vibration feedback when timer completes
    LaunchedEffect(timerStatus) {
        if (timerStatus == TimerStatus.COMPLETED) {
            if (gentleSoundEnabled) {
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                } catch (_: Exception) { }
            }
            if (gentleVibrationEnabled) {
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        manager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 150, 100, 200),
                                intArrayOf(0, 180, 0, 220),
                                -1
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(longArrayOf(0, 150, 100, 200), -1)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = "Focus",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Focus & Flow Timer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Structured learning with low cognitive friction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sound / Vibration toggle chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { viewModel.toggleGentleSound() },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("toggle_sound_button")
                        ) {
                            Icon(
                                imageVector = if (gentleSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Tune,
                                contentDescription = if (gentleSoundEnabled) "Sound on" else "Sound off",
                                tint = if (gentleSoundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleGentleVibration() },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("toggle_vibration_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = if (gentleVibrationEnabled) "Haptic on" else "Haptic off",
                                tint = if (gentleVibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Active Timer") },
                        icon = { Icon(Icons.Default.Timer, contentDescription = "Active Timer") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Progress & Stats") },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = "Progress and Stats") }
                    )
                }
            }
        }

        // Tab Content
        if (selectedTab == 0) {
            TimerTabContent(
                viewModel = viewModel,
                timerStatus = timerStatus,
                timerPhase = timerPhase,
                timerPreset = timerPreset,
                remainingSeconds = remainingSeconds,
                targetDurationSeconds = targetDurationSeconds,
                currentSubject = currentSubject,
                completedSessionsInCycle = completedSessionsInCycle,
                savedTopics = savedTopics,
                onOpenTopicSelector = { showTopicDialog = true },
                onOpenCustomDuration = { showCustomDurationDialog = true },
                onOpenQuickNote = { showQuickNoteDialog = true },
                onNavigateToNotes = onNavigateToNotes
            )
        } else {
            ProgressTabContent(
                allSessions = allFocusSessions,
                todayMinutes = todayFocusMinutes,
                totalMinutes = totalFocusMinutes,
                onDeleteSession = { viewModel.deleteFocusSession(it) },
                onSelectTopicForTimer = { topic ->
                    viewModel.setSubject(topic)
                    selectedTab = 0
                }
            )
        }
    }

    // Post-session reflection / celebration dialog
    if (showCompletionDialog) {
        SessionCompletionDialog(
            durationMinutes = lastSessionDurationMinutes,
            subject = currentSubject,
            onSave = { mood, takeaway ->
                viewModel.recordCompletedSession(mood, takeaway)
            },
            onDismiss = {
                viewModel.dismissCompletionDialog()
            }
        )
    }

    // Change topic dialog
    if (showTopicDialog) {
        TopicSelectionDialog(
            currentSubject = currentSubject,
            savedTopics = savedTopics,
            onDismiss = { showTopicDialog = false },
            onSelect = {
                viewModel.setSubject(it)
                showTopicDialog = false
            }
        )
    }

    // Custom duration dialog
    if (showCustomDurationDialog) {
        CustomDurationDialog(
            currentFocusMin = viewModel.customFocusMinutes.collectAsStateWithLifecycle().value,
            currentBreakMin = viewModel.customBreakMinutes.collectAsStateWithLifecycle().value,
            onDismiss = { showCustomDurationDialog = false },
            onConfirm = { focus, rest ->
                viewModel.setCustomDurations(focus, rest)
                viewModel.setTimerPreset(PomodoroPreset.CUSTOM)
                showCustomDurationDialog = false
            }
        )
    }

    // Quick thought capture dialog
    if (showQuickNoteDialog) {
        AlertDialog(
            onDismissRequest = { showQuickNoteDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Jot Down Thought")
                }
            },
            text = {
                Column {
                    Text(
                        "Capture a fleeting idea without derailing your hyperfocus flow:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = quickNoteText,
                        onValueChange = { quickNoteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("What just sparked in your mind?") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickNoteText.isNotBlank()) {
                            onNavigateToNotes?.invoke(quickNoteText)
                            quickNoteText = ""
                        }
                        showQuickNoteDialog = false
                    }
                ) {
                    Text("Send to Notes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerTabContent(
    viewModel: MainViewModel,
    timerStatus: TimerStatus,
    timerPhase: TimerPhase,
    timerPreset: PomodoroPreset,
    remainingSeconds: Int,
    targetDurationSeconds: Int,
    currentSubject: String,
    completedSessionsInCycle: Int,
    savedTopics: List<String>,
    onOpenTopicSelector: () -> Unit,
    onOpenCustomDuration: () -> Unit,
    onOpenQuickNote: () -> Unit,
    onNavigateToNotes: ((String?) -> Unit)?
) {
    val progress = if (targetDurationSeconds > 0) {
        (remainingSeconds.toFloat() / targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
    } else 1f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "timerProgress"
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    val phaseColor by animateColorAsState(
        targetValue = when (timerPhase) {
            TimerPhase.FOCUS -> MaterialTheme.colorScheme.primary
            TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
            TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondary
        },
        label = "phaseColor"
    )

    val phaseBackground by animateColorAsState(
        targetValue = when (timerPhase) {
            TimerPhase.FOCUS -> MaterialTheme.colorScheme.primaryContainer
            TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
            TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondaryContainer
        },
        label = "phaseBg"
    )

    val phaseTextColor by animateColorAsState(
        targetValue = when (timerPhase) {
            TimerPhase.FOCUS -> MaterialTheme.colorScheme.onPrimaryContainer
            TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.onTertiaryContainer
            TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.onSecondaryContainer
        },
        label = "phaseText"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Topic Banner / Selector Chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTopicSelector() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "FOCUS TOPIC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentSubject,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Circular Timer Display
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(260.dp)
                    .padding(8.dp)
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    val arcSize = Size(diameter, diameter)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active progress arc
                    drawArc(
                        color = phaseColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Inner content inside circle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Phase badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = phaseBackground
                    ) {
                        Text(
                            text = timerPhase.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = phaseTextColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Cycle progress indicators (dots)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..4) {
                            val active = (completedSessionsInCycle % 4) >= i
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) phaseColor else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cycle ${((completedSessionsInCycle % 4) + 1).coerceAtMost(4)}/4",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Main Timer Play/Pause/Reset Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                FilledTonalIconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("reset_timer_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Primary Start / Pause Button
                Button(
                    onClick = {
                        if (timerStatus == TimerStatus.RUNNING) {
                            viewModel.pauseTimer()
                        } else {
                            viewModel.startTimer()
                        }
                    },
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("play_pause_timer_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = phaseColor
                    )
                ) {
                    Icon(
                        imageVector = if (timerStatus == TimerStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (timerStatus == TimerStatus.RUNNING) "Pause timer" else "Start timer",
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Skip Phase Button
                FilledTonalIconButton(
                    onClick = { viewModel.skipToNextPhase() },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("skip_phase_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip to next phase",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Neurodivergent Support Affordances
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hyperfocus "+5 Min Flow Extension"
                OutlinedButton(
                    onClick = { viewModel.extendTimer(5) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("extend_timer_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+5m Flow")
                }

                // Quick Idea Dump (capture thought without losing focus)
                OutlinedButton(
                    onClick = { onOpenQuickNote() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_note_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Jot Thought")
                }
            }
        }

        // Pacing Preset Selection Cards
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pacing Modes (Neurodivergent-Tailored)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Presets list
                PomodoroPreset.entries.forEach { preset ->
                    val isSelected = timerPreset == preset
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = timerStatus != TimerStatus.RUNNING) {
                                if (preset == PomodoroPreset.CUSTOM) {
                                    onOpenCustomDuration()
                                } else {
                                    viewModel.setTimerPreset(preset)
                                }
                            }
                            .testTag("preset_${preset.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProgressTabContent(
    allSessions: List<FocusSession>,
    todayMinutes: Int,
    totalMinutes: Int,
    onDeleteSession: (Int) -> Unit,
    onSelectTopicForTimer: (String) -> Unit
) {
    val completedSessions = remember(allSessions) {
        allSessions.filter { it.completed }
    }

    val topicsBreakdown = remember(completedSessions) {
        completedSessions.groupBy { it.subject }
            .mapValues { entry -> entry.value.sumOf { it.actualDurationMinutes } }
            .toList()
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Stat Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's Focus Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TODAY'S FOCUS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${todayMinutes}m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${completedSessions.filter { it.timestamp >= System.currentTimeMillis() - 86400000 }.size} sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // All-Time Focus Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TOTAL FOCUS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${totalMinutes}m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${completedSessions.size} total blocks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Neurodivergent Positive Reinforcement Milestones
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Learning Milestones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MilestoneBadge(
                            label = "First Spark",
                            target = "10m",
                            achieved = totalMinutes >= 10,
                            modifier = Modifier.weight(1f)
                        )
                        MilestoneBadge(
                            label = "Deep Explorer",
                            target = "45m",
                            achieved = totalMinutes >= 45,
                            modifier = Modifier.weight(1f)
                        )
                        MilestoneBadge(
                            label = "Centurion",
                            target = "100m",
                            achieved = totalMinutes >= 100,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Subject Breakdown
        if (topicsBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = "Focus by Learning Subject",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topicsBreakdown.forEach { (topic, minutes) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTopicForTimer(topic) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${minutes} min",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Past Sessions Log
        item {
            Text(
                text = "Session History & Reflections",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (completedSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No focus sessions recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start a 10-minute Quick Sprint to jumpstart your learning flow!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(completedSessions, key = { it.id }) { session ->
                SessionHistoryCard(
                    session = session,
                    onDelete = { onDeleteSession(session.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MilestoneBadge(
    label: String,
    target: String,
    achieved: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (achieved) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (achieved) Icons.Default.CheckCircle else Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = if (achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = target,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: FocusSession,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.timestamp) { dateFormat.format(Date(session.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${session.actualDurationMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete session",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                session.reflectionMood?.let { mood ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = mood,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            session.notesSummary?.let { summary ->
                if (summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 $summary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCompletionDialog(
    durationMinutes: Int,
    subject: String,
    onSave: (mood: String, takeaway: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMood by remember { mutableStateOf("In the Flow 🌊") }
    var takeawayText by remember { mutableStateOf("") }

    val moodOptions = listOf("In the Flow 🌊", "Steady Focus 🎯", "Challenging / Restless 🌪️")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Focus Session Complete!")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Awesome work! You completed $durationMinutes minutes focusing on \"$subject\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "How did your cognitive flow feel?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    moodOptions.forEach { mood ->
                        val selected = selectedMood == mood
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMood = mood }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = mood,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = takeawayText,
                    onValueChange = { takeawayText = it },
                    label = { Text("Quick Takeaway or Learning Nugget (Optional)") },
                    placeholder = { Text("What did you read, write, or uncover?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedMood, takeawayText) },
                modifier = Modifier.testTag("save_session_button")
            ) {
                Text("Log & Start Break")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip Reflection")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicSelectionDialog(
    currentSubject: String,
    savedTopics: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var customSubject by remember { mutableStateOf("") }
    val defaultTopics = listOf("Astrophysics", "Marine Life", "Language & Linguistics", "Neuroscience", "Ancient History", "Coding & Tech")
    val combinedTopics = (savedTopics + defaultTopics).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Focus Topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Anchor your session to a learning hyperfixation:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = customSubject,
                    onValueChange = { customSubject = it },
                    label = { Text("Custom Learning Topic") },
                    placeholder = { Text("e.g. Mycology, Quantum physics...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Or choose from your library:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    combinedTopics.forEach { topic ->
                        val isCurrent = currentSubject.equals(topic, ignoreCase = true)
                        FilterChip(
                            selected = isCurrent,
                            onClick = { onSelect(topic) },
                            label = { Text(topic) },
                            leadingIcon = if (isCurrent) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customSubject.isNotBlank()) {
                        onSelect(customSubject)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Set Topic")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CustomDurationDialog(
    currentFocusMin: Int,
    currentBreakMin: Int,
    onDismiss: () -> Unit,
    onConfirm: (focus: Int, rest: Int) -> Unit
) {
    var focusMinutes by remember { mutableIntStateOf(currentFocusMin) }
    var breakMinutes by remember { mutableIntStateOf(currentBreakMin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Pacing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Tailor your focus and break intervals to your cognitive bandwidth:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Focus Duration:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("$focusMinutes minutes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = focusMinutes.toFloat(),
                        onValueChange = { focusMinutes = it.toInt() },
                        valueRange = 5f..90f,
                        steps = 16
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Break Duration:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("$breakMinutes minutes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = breakMinutes.toFloat(),
                        onValueChange = { breakMinutes = it.toInt() },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(focusMinutes, breakMinutes) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
