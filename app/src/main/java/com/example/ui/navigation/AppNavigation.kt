package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.MainViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen

@Composable
fun AppNavigation(navController: NavHostController, viewModel: MainViewModel) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "splash"

    val showBottomNav = currentRoute in listOf("feed", "archive", "settings")

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.List, contentDescription = "Matches feed") },
                        label = { Text("Feed") },
                        selected = currentRoute == "feed",
                        onClick = {
                            navController.navigate("feed") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Chat, contentDescription = "Past chat archive") },
                        label = { Text("Archive") },
                        selected = currentRoute == "archive",
                        onClick = {
                            navController.navigate("archive") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Profile and settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen(onSplashComplete = {
                    navController.navigate("feed") {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }
            composable("feed") {
                FeedScreen(viewModel = viewModel, onNavigateToChat = { matchId ->
                    navController.navigate("chat/$matchId")
                })
            }
            composable("archive") {
                com.example.ui.screens.ArchiveScreen(viewModel = viewModel, onNavigateToChat = { matchId ->
                    navController.navigate("chat/$matchId")
                })
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
            composable("chat/{matchId}") { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId")?.toIntOrNull() ?: 0
                ChatScreen(viewModel = viewModel, matchId = matchId, onBack = { navController.popBackStack() })
            }
        }
    }
}
