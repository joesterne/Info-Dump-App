package com.example.ui.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    TagOption("Literature", Icons.Filled.MenuBook),
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
    var showSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(myProfile) {
        myProfile?.let {
            name = it.name
            isHyperfixating = it.isHyperfixating
            subject = it.subject
            selectedTags = it.tags.split(",").map { tag -> tag.trim() }.filter { tag -> tag.isNotEmpty() }.toSet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
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
            Text(
                text = "Accessibility & Comfort",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.isDarkMode,
                            onCheckedChange = { 
                                viewModel.updateSettings(isDarkMode = it, textSizeMultiplier = settings.textSizeMultiplier) 
                            }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Text Size: ${String.format("%.1fx", settings.textSizeMultiplier)}", style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            modifier = Modifier.semantics { contentDescription = "Adjust text size" },
                            value = settings.textSizeMultiplier,
                            onValueChange = { 
                                viewModel.updateSettings(isDarkMode = settings.isDarkMode, textSizeMultiplier = it) 
                            },
                            valueRange = 0.8f..2.0f,
                            steps = 11
                        )
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

                    Button(
                        onClick = {
                            viewModel.saveMyProfile(name, isHyperfixating, subject, selectedTags.joinToString(", "))
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
                    
                    Text("Keep a list of interests to return to for future infodump pairings. These are securely synced to your cloud account.", style = MaterialTheme.typography.bodyMedium)

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
