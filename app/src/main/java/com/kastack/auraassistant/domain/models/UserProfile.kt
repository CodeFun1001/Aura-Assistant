package com.kastack.auraassistant.domain.models

data class UserProfile(
    val name: String = "",
    val age: String = "",
    val phone: String = "",
    val traits: List<String> = emptyList(),
    val isOnboardingComplete: Boolean = false
)