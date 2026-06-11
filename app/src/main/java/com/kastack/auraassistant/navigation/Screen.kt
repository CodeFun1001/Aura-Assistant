package com.kastack.auraassistant.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object OnboardingStep1 : Screen("onboarding/step1")
    data object OnboardingStep2 : Screen("onboarding/step2")
    data object OnboardingStep3 : Screen("onboarding/step3")
    data object Home : Screen("home")
    data object Chat : Screen("chat")
    data object Reminders : Screen("reminders")
}