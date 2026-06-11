package com.kastack.auraassistant.presentation.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToReminders: () -> Unit
) {
    Text("Home")
}