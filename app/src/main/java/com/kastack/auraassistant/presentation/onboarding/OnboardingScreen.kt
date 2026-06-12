package com.kastack.auraassistant.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kastack.auraassistant.ui.theme.AuraPurpleLight
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope   = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = uiState.currentStep,
        pageCount = { 3 }
    )

    LaunchedEffect(uiState.currentStep) {
        if (pagerState.currentPage != uiState.currentStep) {
            pagerState.animateScrollToPage(uiState.currentStep)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (page != uiState.currentStep) {
            if (page > uiState.currentStep) {
                val advanced = viewModel.goToNextStep()
                if (!advanced) {
                    scope.launch {
                        pagerState.animateScrollToPage(uiState.currentStep)
                    }
                }
            } else {
                viewModel.goToPreviousStep()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> ValuePropsStep(
                    onNext = {
                        scope.launch {
                            if (viewModel.goToNextStep()) {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    }
                )
                1 -> ProfileStep(
                    uiState = uiState,
                    onNameChange = viewModel::onNameChange,
                    onAgeChange  = viewModel::onAgeChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onOtpChange  = viewModel::onOtpChange,
                    onSendOtp    = viewModel::sendOtp,
                    onVerifyOtp  = viewModel::verifyOtp,
                    onNext = {
                        scope.launch {
                            if (viewModel.goToNextStep()) {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    },
                    onBack = {
                        scope.launch {
                            viewModel.goToPreviousStep()
                            pagerState.animateScrollToPage(0)
                        }
                    }
                )
                2 -> PersonalityStep(
                    uiState = uiState,
                    onToggleTrait = viewModel::toggleTrait,
                    onBack = {
                        scope.launch {
                            viewModel.goToPreviousStep()
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    onComplete = { viewModel.completeOnboarding(onOnboardingComplete) }
                )
            }
        }

        StepDotsIndicator(
            currentStep = pagerState.currentPage,
            totalSteps  = 3,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp)
        )
    }
}

@Composable
private fun StepDotsIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val width by animateDpAsState(
                targetValue = if (isActive) 24.dp else 8.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "dot_width_$index"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .background(
                        color = if (isActive) AuraPurpleLight
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

private val valueProps = listOf(
    "🧠" to "Your thoughts, kept private",
    "⚡" to "Instant responses, offline",
    "🎯" to "Personalised to your traits",
    "🔒" to "Zero data leaves your device"
)

@Composable
fun ValuePropsStep(onNext: () -> Unit) {
    var visibleIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (visibleIndex < valueProps.size - 1) {
            kotlinx.coroutines.delay(1200)
            visibleIndex++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 80.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Meet Aura",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AuraPurpleLight
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your intelligent offline companion",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))

        valueProps.forEachIndexed { index, (emoji, text) ->
            AnimatedVisibility(
                visible = index <= visibleIndex,
                enter = fadeIn(tween(600)) + slideInVertically { 40 }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        AnimatedVisibility(
            visible = visibleIndex == valueProps.size - 1,
            enter = fadeIn(tween(800))
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Get Started  →", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ProfileStep(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(top = 80.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Tell us about you",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "This stays on your device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
        )
        Spacer(Modifier.height(8.dp))

        AuraTextField(
            value = uiState.name, onValueChange = onNameChange,
            label = "Your Name", error = uiState.nameError
        )
        AuraTextField(
            value = uiState.age, onValueChange = onAgeChange,
            label = "Age", error = uiState.ageError,
            keyboardType = KeyboardType.Number
        )

        Row(verticalAlignment = Alignment.Top) {
            AuraTextField(
                value = uiState.phone, onValueChange = onPhoneChange,
                label = "Phone", error = uiState.phoneError,
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onSendOtp,
                enabled = !uiState.isOtpVerified,
                modifier = Modifier.padding(top = 8.dp)
            ) { Text(if (uiState.isOtpSent) "Resend" else "Send OTP") }
        }

        if (uiState.isOtpSent && !uiState.isOtpVerified) {
            Row(verticalAlignment = Alignment.Top) {
                AuraTextField(
                    value = uiState.otpInput, onValueChange = onOtpChange,
                    label = "Enter OTP", error = uiState.otpError,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onVerifyOtp() },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Verify") }
            }
        }

        if (uiState.isOtpVerified) {
            Text(
                "✅ Phone verified",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Continue") }
        }
    }
}

private val allTraits = listOf(
    "Focused", "Curious", "Analytical", "Creative", "Calm",
    "Energetic", "Strategic", "Empathetic", "Minimalist", "Ambitious",
    "Playful", "Disciplined"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalityStep(
    uiState: OnboardingUiState,
    onToggleTrait: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(top = 80.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Your Personality",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Pick 3 traits — Aura adapts its responses to match your style.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
        )
        Text(
            "${uiState.selectedTraits.size} / 3 selected",
            color = AuraPurpleLight,
            fontWeight = FontWeight.SemiBold
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allTraits.forEach { trait ->
                val selected  = trait in uiState.selectedTraits
                val canSelect = selected || uiState.selectedTraits.size < 3
                TraitChip(
                    label    = trait,
                    selected = selected,
                    enabled  = canSelect,
                    onClick  = { onToggleTrait(trait) }
                )
            }
        }

        uiState.traitError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onComplete, modifier = Modifier.weight(1f)) { Text("Let's Go 🚀") }
        }
    }
}

@Composable
fun TraitChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val background   = if (selected) AuraPurpleLight else Color.Transparent
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor  = if (selected) AuraPurpleLight else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .background(background, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.4f)
    ) {
        Text(label, color = contentColor, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AuraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}