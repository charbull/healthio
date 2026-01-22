package com.healthio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.healthio.ui.dashboard.HomeScreen
import com.healthio.ui.dashboard.HomeViewModel
import com.healthio.ui.settings.SettingsScreen
import com.healthio.ui.settings.SettingsViewModel
import com.healthio.ui.stats.StatsScreen
import com.healthio.ui.theme.HealthioTheme
import com.healthio.ui.vision.VisionScreen
import com.healthio.ui.workouts.WorkoutViewModel

import com.healthio.core.worker.ReminderScheduler

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.unit.dp



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        

        ReminderScheduler.scheduleAll(this)

        

        val isRationale = intent.action == "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" || 

                          intent.action == "android.health.connect.action.ACTION_SHOW_PERMISSIONS_RATIONALE"



        setContent {

            HealthioTheme {

                Surface(

                    modifier = Modifier.fillMaxSize(),

                    color = MaterialTheme.colorScheme.background

                ) {

                    if (isRationale) {

                        HealthConnectRationaleScreen { finish() }

                    } else {

                        val navController = rememberNavController()

                        

                        val homeViewModel: HomeViewModel = viewModel()

                        val settingsViewModel: SettingsViewModel = viewModel()

                        val workoutViewModel: WorkoutViewModel = viewModel()



                        NavHost(navController = navController, startDestination = "home") {

                            composable("home") {

                                HomeScreen(

                                    onNavigateToStats = { navController.navigate("stats") },

                                    onNavigateToSettings = { navController.navigate("settings") },

                                    onNavigateToVision = { navController.navigate("vision") },

                                    viewModel = homeViewModel,

                                    settingsViewModel = settingsViewModel,

                                    workoutViewModel = workoutViewModel

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

                            composable("vision") {

                                VisionScreen(

                                    onBack = { navController.popBackStack() }

                                )

                            }

                        }

                    }

                }

            }

        }

    }

}



@Composable

fun HealthConnectRationaleScreen(onContinue: () -> Unit) {

    Column(

        modifier = Modifier.fillMaxSize().padding(24.dp),

        verticalArrangement = Arrangement.Center,

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Text(

            text = "Health Connect Permissions",

            style = MaterialTheme.typography.headlineMedium

        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(

            text = "Healthio needs access to Health Connect to import your workouts and active calorie burn from apps like Garmin, Samsung Health, or Google Fit.",

            style = MaterialTheme.typography.bodyLarge

        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onContinue) {

            Text("Continue")

        }

    }

}
