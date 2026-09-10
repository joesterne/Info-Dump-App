package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val db = Room.databaseBuilder(
      applicationContext,
      AppDatabase::class.java, "infodump-db"
    )
    .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
    .build()
    
    val repository = AppRepository(db.appDao())
    val authManager = AuthManager(applicationContext)
    val encryptedPrefsManager = EncryptedPrefsManager(applicationContext)
    val factory = MainViewModel.Factory(repository, authManager, encryptedPrefsManager)

    setContent {
      val viewModel: MainViewModel = viewModel(factory = factory)
      val settings by viewModel.settings.collectAsStateWithLifecycle()
      
      MyApplicationTheme(
        darkTheme = settings.isDarkMode,
        textSizeMultiplier = settings.textSizeMultiplier
      ) {
        Surface(modifier = Modifier.fillMaxSize()) {
          val navController = rememberNavController()
          AppNavigation(navController = navController, viewModel = viewModel)
        }
      }
    }
  }
}
