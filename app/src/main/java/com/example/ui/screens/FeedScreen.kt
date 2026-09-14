package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.MatchWithScore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: MainViewModel, onNavigateToChat: (Int) -> Unit) {
    val matchesWithScores by viewModel.allMatchesWithScores.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    var showSearchModal by remember { mutableStateOf(false) }
    var showWheelModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }

    val allAvailableTopics = remember(matchesWithScores) {
        val topics = matchesWithScores.map { it.profile.subject }.filter { it.isNotBlank() }.distinct()
        if (topics.size >= 3) topics else listOf("Trains", "Deep Sea Creatures", "Medieval History", "Quantum Physics", "Vintage Keyboards", "Space", "Dinosaurs", "Bugs")
    }

    val displayedMatches = if (activeSearchQuery.isNotBlank()) {
        matchesWithScores.filter { 
            it.profile.subject.contains(activeSearchQuery, ignoreCase = true) ||
            it.profile.tags.contains(activeSearchQuery, ignoreCase = true)
        }
    } else {
        matchesWithScores
    }

    if (showSearchModal) {
        AlertDialog(
            onDismissRequest = { showSearchModal = false },
            title = { Text("Search Topics") },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Enter a topic (e.g., Space)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    activeSearchQuery = searchQuery
                    showSearchModal = false
                }) {
                    Text("Search")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    searchQuery = ""
                    activeSearchQuery = ""
                    showSearchModal = false 
                }) {
                    Text("Clear Filter")
                }
            }
        )
    }

    if (showWheelModal) {
        TopicWheelDialog(
            topics = allAvailableTopics,
            onDismiss = { showWheelModal = false },
            onTopicSelected = { selectedTopic ->
                activeSearchQuery = selectedTopic
                showWheelModal = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (activeSearchQuery.isNotBlank()) "Matches: $activeSearchQuery" else "Matches", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.updateSettings(
                            isDarkMode = !settings.isDarkMode,
                            textSizeMultiplier = settings.textSizeMultiplier
                        )
                    }) {
                        Icon(
                            imageVector = if (settings.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = { showWheelModal = true }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Spin the wheel for a random topic")
                    }
                    IconButton(onClick = { 
                        searchQuery = activeSearchQuery
                        showSearchModal = true 
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "Open topic search filter")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val sampleSubjects = listOf("Trains", "Deep Sea Creatures", "Medieval History", "Quantum Physics", "Vintage Keyboards")
                    val isFixating = Math.random() > 0.5
                    viewModel.addMockMatch(
                        name = "User_${(1000..9999).random()}",
                        isHyperfixating = isFixating,
                        subject = sampleSubjects.random(),
                        tags = "adhd, autistic, learning"
                    )
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add a simulated match for testing")
            }
        }
    ) { paddingValues ->
        if (myProfile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Please set up your profile in Settings first.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (activeSearchQuery.isBlank()) {
                    item {
                        TopicOfTheDayCard(onTopicSelected = { activeSearchQuery = it })
                    }
                }

                if (displayedMatches.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (activeSearchQuery.isNotBlank()) {
                                    Text("No matches found for '$activeSearchQuery'.", style = MaterialTheme.typography.titleMedium)
                                    Button(onClick = { activeSearchQuery = "" }) {
                                        Text("Clear Filter")
                                    }
                                } else {
                                    Text("No matches yet.", style = MaterialTheme.typography.titleLarge)
                                    Text("Tap the + button to simulate finding people.", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                } else {
                    items(displayedMatches) { matchWithScore ->
                        MatchCard(matchWithScore = matchWithScore, onClick = { onNavigateToChat(matchWithScore.profile.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(matchWithScore: MatchWithScore, onClick: () -> Unit) {
    val match = matchWithScore.profile
    val score = matchWithScore.compatibilityScore
    val roleColor = if (match.isHyperfixating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
    val roleText = if (match.isHyperfixating) "Wants to Infodump" else "Wants to Listen"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick, onClickLabel = "Open chat with ${match.name}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (match.isOnline) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color.Gray)
                    )
                    Text(match.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                            Text("$score%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(String.format("%.1f", match.rating), style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Surface(
                color = roleColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = roleText,
                    color = roleColor,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Text("Subject: ${match.subject}", style = MaterialTheme.typography.bodyLarge)
            Text("Tags: ${match.tags}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
