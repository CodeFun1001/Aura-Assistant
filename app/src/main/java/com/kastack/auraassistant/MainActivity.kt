package com.kastack.auraassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kastack.auraassistant.navigation.AuraNavGraph
import com.kastack.auraassistant.navigation.Screen
import com.kastack.auraassistant.presentation.MainViewModel
import com.kastack.auraassistant.ui.theme.AuraAssistantTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraAssistantTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val onboardingComplete by viewModel.isOnboardingComplete.collectAsState()
                    val navController = rememberNavController()

                    if (onboardingComplete != null) {
                        val startDestination = if (onboardingComplete == true) {
                            Screen.Home.route
                        } else {
                            Screen.Onboarding.route
                        }
                        AuraNavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}