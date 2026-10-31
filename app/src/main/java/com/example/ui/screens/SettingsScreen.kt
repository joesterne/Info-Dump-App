package com.example.ui.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import kotlinx.coroutines.launch

data class TagOption(val name: String, val icon: ImageVector)

val predefinedTags = listOf(
    TagOption("Science", Icons.Filled.Science),
    TagOption("History", Icons.Filled.AccountBalance),
    TagOption("Technology", Icons.Filled.Computer),
    TagOption("Art", Icons.Filled.Brush),
    TagOption("Music", Icons.Filled.MusicNote),
    TagOption("Gaming", Icons.Filled.SportsEsports),
    TagOption("Literature", Icons.AutoMirrored.Filled.MenuBook),
    TagOption("Nature", Icons.Filled.Park),
    TagOption("Space", Icons.Filled.RocketLaunch),
    TagOption("Math", Icons.Filled.Calculate),
    TagOption("Trains", Icons.Filled.Train),
    TagOption("Deep Sea", Icons.Filled.Water)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var isHyperfixating by remember { mutableStateOf(true) }
    var subject by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var energyLevel by remember { mutableStateOf("Medium") }
    var showSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(myProfile) {
        myProfile?.let {
            name = it.name
            isHyperfixating = it.isHyperfixating
            subject = it.subject
            selectedTags = it.tags.split(",").map { tag -> tag.trim() }.filter { tag -> tag.isNotEmpty() }.toSet()
            energyLevel = it.energyLevel
        }
    }

    val currentUserUid by viewModel.currentUserUid.collectAsStateWithLifecycle()

    val isHighContrast = settings.isHighContrast

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleHighContrast() },
                        modifier = Modifier.testTag("top_bar_contrast_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Contrast,
                            contentDescription = if (isHighContrast) "Disable High-Contrast Mode" else "Enable High-Contrast Mode",
                            tint = if (isHighContrast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        viewModel.updateSettings(
                            isDarkMode = !settings.isDarkMode,
                            textSizeMultiplier = settings.textSizeMultiplier,
                            isHighContrast = settings.isHighContrast
                        )
                    }, modifier = Modifier.testTag("top_bar_theme_toggle")) {
                        Icon(
                            imageVector = if (settings.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Dark Mode"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Authentication Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = if (isHighContrast) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleMedium)
                    if (currentUserUid != null) {
                        Text("Signed in securely.", color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Sign Out")
                        }
                    } else {
                        Text("Sign in with Google to sync your data securely.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.signInWithGoogle() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Sign In with Google")
                        }
                    }
                }
            }

            Text(
                text = "Accessibility & Comfort",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = if (isHighContrast) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
                modifier = Modifier.fillMaxWidth().testTag("accessibility_settings_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    // High-Contrast Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Contrast,
                                contentDescription = null,
                                tint = if (isHighContrast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "High-Contrast Mode",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    if (isHighContrast) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text("AAA", modifier = Modifier.padding(horizontal = 4.dp))
                                        }
                                    }
                                }
                                Text(
                                    if (isHighContrast) "Active: WCAG AAA 7:1+ contrast & sharp borders reduce sensory processing fatigue."
                                    else "Sharp edges, deep backgrounds & vivid accents for sensory clarity.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isHighContrast,
                            onCheckedChange = { 
                                viewModel.toggleHighContrast()
                            },
                            modifier = Modifier.testTag("toggle_high_contrast")
                        )
                    }

                    HorizontalDivider(
                        color = if (isHighContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
                    )

                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (settings.isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    if (settings.isDarkMode) "Dark Theme" else "Light Theme",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    if (settings.isDarkMode) "Reduces eye strain in low-light environments" else "Crisp illumination for bright spaces",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = { 
                                viewModel.updateSettings(
                                    isDarkMode = it,
                                    textSizeMultiplier = settings.textSizeMultiplier,
                                    isHighContrast = settings.isHighContrast
                                ) 
                            },
                            modifier = Modifier.testTag("toggle_dark_mode")
                        )
                    }
                    
                    HorizontalDivider(
                        color = if (isHighContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // Dynamic Font Scaling Section
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val percentage = (settings.textSizeMultiplier * 100).toInt()
                        val readabilityTier = when {
                            settings.textSizeMultiplier <= 0.90f -> "Compact"
                            settings.textSizeMultiplier <= 1.10f -> "Standard"
                            settings.textSizeMultiplier <= 1.30f -> "Comfortable"
                            settings.textSizeMultiplier <= 1.55f -> "Large"
                            settings.textSizeMultiplier <= 1.85f -> "Extra Large"
                            else -> "Maximum"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FormatSize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column {
                                    Text(
                                        "Dynamic Font Scaling",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Text(
                                        "Proportional line-height avoids letter crowding for ADHD & Dyslexia.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            AssistChip(
                                onClick = { viewModel.setDynamicFontScale(1.0f) },
                                label = { Text("$percentage% · $readabilityTier") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("font_scale_badge")
                            )
                        }

                        // Presets Row
                        Text(
                            "Quick Scale Presets",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presets = listOf(
                                0.85f to "85% Compact",
                                1.0f to "100% Default",
                                1.20f to "120% Medium",
                                1.40f to "140% Large",
                                1.70f to "170% XL",
                                2.0f to "200% Max"
                            )
                            presets.forEach { (scaleValue, label) ->
                                val isSelected = kotlin.math.abs(settings.textSizeMultiplier - scaleValue) < 0.06f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setDynamicFontScale(scaleValue) },
                                    label = { Text(label) },
                                    border = if (isHighContrast) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("font_preset_${(scaleValue * 100).toInt()}")
                                )
                            }
                        }

                        // Stepper + Slider fine tuning
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    val next = (settings.textSizeMultiplier - 0.10f).coerceIn(0.75f, 2.0f)
                                    viewModel.setDynamicFontScale(next)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("decrease_font_size"),
                                enabled = settings.textSizeMultiplier > 0.76f
                            ) {
                                Icon(Icons.Filled.ZoomOut, contentDescription = "Decrease Font Size")
                            }

                            Slider(
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = "Adjust dynamic text scale percentage" }
                                    .testTag("font_size_slider"),
                                value = settings.textSizeMultiplier,
                                onValueChange = { 
                                    viewModel.setDynamicFontScale(it)
                                },
                                valueRange = 0.75f..2.0f,
                                steps = 24
                            )

                            IconButton(
                                onClick = { 
                                    val next = (settings.textSizeMultiplier + 0.10f).coerceIn(0.75f, 2.0f)
                                    viewModel.setDynamicFontScale(next)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("increase_font_size"),
                                enabled = settings.textSizeMultiplier < 1.99f
                            ) {
                                Icon(Icons.Filled.ZoomIn, contentDescription = "Increase Font Size")
                            }
                        }

                        // Real-time Accessibility Preview Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHighContrast) {
                                    if (settings.isDarkMode) androidx.compose.ui.graphics.Color(0xFF141414)
                                    else androidx.compose.ui.graphics.Color(0xFFFAFAFA)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = if (isHighContrast) 2.dp else 1.dp,
                                color = if (isHighContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("accessibility_preview_box")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "LIVE READING PREVIEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "$percentage% Scale",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "Hyperfocus: Deep Sea Creatures",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    "Hydrothermal vents support life through chemosynthesis without sunlight. Notice how line spacing automatically expands with font size to keep text legible and comfortable.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("#Oceanography") },
                                        border = if (isHighContrast) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                                    )
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("High Legibility") },
                                        border = if (isHighContrast) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "My Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Current Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Interest Tags",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        predefinedTags.forEach { tagOption ->
                            val isSelected = selectedTags.contains(tagOption.name)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTags = if (isSelected) {
                                        selectedTags - tagOption.name
                                    } else {
                                        selectedTags + tagOption.name
                                    }
                                },
                                label = { 
                                    Text(
                                        text = tagOption.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = tagOption.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    iconColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isHyperfixating) "I want to infodump!" else "I want to learn/listen!", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = isHyperfixating,
                            onCheckedChange = { isHyperfixating = it }
                        )
                    }

                    Text(
                        text = "Communication Bandwidth / Energy Level",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Low", "Medium", "High").forEach { level ->
                            FilterChip(
                                selected = energyLevel == level,
                                onClick = { energyLevel = level },
                                label = { Text(level) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.saveMyProfile(name, isHyperfixating, subject, selectedTags.joinToString(", "), energyLevel)
                            scope.launch {
                                showSavedMessage = true
                                kotlinx.coroutines.delay(2000)
                                showSavedMessage = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Profile")
                    }
                    
                    if (showSavedMessage) {
                        Text("Profile saved successfully!", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text(
                text = "Saved Topics",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val savedTopics by viewModel.savedTopics.collectAsStateWithLifecycle()
                    var newTopic by remember { mutableStateOf("") }
                    
                    Text("Keep a list of interests to return to for future infodump pairings. These sync to your own account when you are signed in.", style = MaterialTheme.typography.bodyMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTopic,
                            onValueChange = { newTopic = it },
                            placeholder = { Text("Enter a topic (e.g., Space)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (newTopic.isNotBlank()) {
                                    viewModel.addSavedTopic(newTopic.trim())
                                    newTopic = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add saved topic")
                        }
                    }

                    if (savedTopics.isEmpty()) {
                        Text("No topics saved yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            savedTopics.forEach { topic ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text(topic) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove $topic from saved topics",
                                            modifier = Modifier.size(16.dp).clickable(onClickLabel = "Remove $topic from saved topics") { viewModel.removeSavedTopic(topic) }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
