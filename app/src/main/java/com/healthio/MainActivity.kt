package com.healthio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.healthio.ui.dashboard.HomeScreen
import com.healthio.ui.stats.StatsScreen
import com.healthio.ui.theme.HealthioTheme

import com.healthio.ui.settings.SettingsScreen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthio.ui.dashboard.HomeViewModel
import com.healthio.ui.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Create shared ViewModels scoped to the Activity/Graph
                    val homeViewModel: HomeViewModel = viewModel()
                    val settingsViewModel: SettingsViewModel = viewModel()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToStats = { navController.navigate("stats") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                viewModel = homeViewModel,
                                settingsViewModel = settingsViewModel
                            )
                        }
                        composable("stats") {
                            StatsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                viewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}