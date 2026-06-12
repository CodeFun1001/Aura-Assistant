package com.kastack.auraassistant.presentation.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kastack.auraassistant.R
import com.kastack.auraassistant.domain.models.AssistantState
import com.kastack.auraassistant.domain.models.Reminder
import com.kastack.auraassistant.domain.models.SyncStatus
import com.kastack.auraassistant.presentation.components.AuraCircle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToReminders: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val amplitude      by viewModel.amplitude.collectAsStateWithLifecycle()
    val syncStatus     by viewModel.syncStatus.collectAsStateWithLifecycle()
    val todayReminders by viewModel.todayReminders.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.checkMicPermission()
            viewModel.startListening()
        }
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val THRESHOLD = 220f

    val swipeFraction = (-dragOffset / THRESHOLD).coerceIn(0f, 1f)
    val circleAlpha   = 1f - swipeFraction
    val circleOffsetY = dragOffset * 0.55f

    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "drag_spring"
    )

    LaunchedEffect(dragOffset) {
        if (dragOffset < -THRESHOLD) {
            dragOffset = 0f
            onNavigateToChat()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .systemBarsPadding()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffset > -THRESHOLD) dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f }
                    ) { _, dragAmount ->
                        if (dragAmount < 0 || dragOffset < 0f) {
                            dragOffset = (dragOffset + dragAmount).coerceIn(-THRESHOLD * 1.1f, 0f)
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GreetingText(name = uiState.userProfile.name)
                SyncStatusChip(syncStatus = syncStatus)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationY = circleOffsetY
                        alpha = circleAlpha.coerceIn(0f, 1f)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AuraCircle(
                    state     = assistantState,
                    amplitude = amplitude,
                    size      = 240.dp
                )

                AnimatedVisibility(
                    visible = assistantState is AssistantState.Idle && swipeFraction < 0.05f,
                    enter = fadeIn(tween(500)),
                    exit  = fadeOut(tween(300))
                ) {
                    Text(
                        "↑ swipe up to open chat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
                    )
                }

                AnimatedContent(
                    targetState = auraMood(assistantState, amplitude),
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "mood_label"
                ) { label ->
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (todayReminders.isNotEmpty()) {
                    TodayRemindersCard(reminders = todayReminders)
                }

                MicFab(
                    isListening = assistantState is AssistantState.Listening,
                    onClick = {
                        if (uiState.hasMicPermission) {
                            if (assistantState is AssistantState.Listening) viewModel.stopListening()
                            else viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateToChat) {
                        Icon(
                            painter = painterResource(R.drawable.chat),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Chat", style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = onNavigateToReminders) {
                        Icon(
                            painter = painterResource(R.drawable.alarm),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Reminders", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            val errState = assistantState
            AnimatedVisibility(
                visible = errState is AssistantState.Error,
                enter = slideInVertically { -it } + fadeIn(),
                exit  = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                if (errState is AssistantState.Error) {
                    ElevatedCard(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                errState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            if (errState.retryInput != null) {
                                TextButton(onClick = viewModel::retry) { Text("Retry") }
                            } else {
                                TextButton(onClick = viewModel::dismissError) { Text("OK") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncStatusChip(syncStatus: SyncStatus) {
    val (label, dotColor) = when (syncStatus) {
        is SyncStatus.Synced  -> "Synced"  to Color(0xFF10B981)
        is SyncStatus.Syncing -> "Syncing" to Color(0xFFF59E0B)
        is SyncStatus.Failed  -> "Failed"  to MaterialTheme.colorScheme.error
        is SyncStatus.Idle    -> return
    }

    SuggestionChip(
        onClick = {},
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val pulseAlpha by if (syncStatus is SyncStatus.Syncing) {
                    rememberInfiniteTransition(label = "sync_pulse")
                        .animateFloat(
                            initialValue = 0.4f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                            label = "pulse_alpha"
                        )
                } else {
                    remember { mutableFloatStateOf(1f) }
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer { alpha = pulseAlpha }
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = dotColor)
                    }
                }
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        },
        shape = RoundedCornerShape(50)
    )
}

private val reminderTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

@Composable
fun TodayRemindersCard(reminders: List<Reminder>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Today's Reminders",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            reminders.take(4).forEach { reminder ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "• ${reminder.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        reminderTimeFormat.format(Date(reminder.scheduledAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
            if (reminders.size > 4) {
                Text(
                    "+${reminders.size - 4} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
private fun GreetingText(name: String) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }
    val display = if (name.isNotBlank()) "$greeting, $name" else greeting
    Text(
        text = display,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun MicFab(isListening: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.20f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mic_scale"
    )
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CircleShape,
        containerColor = if (isListening) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (isListening) 12.dp else 6.dp
        )
    ) {
        Icon(
            painter = painterResource(if (isListening) R.drawable.mic_off else R.drawable.mic),
            contentDescription = if (isListening) "Stop listening" else "Speak to Aura",
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

private fun stateLabel(state: AssistantState): String = when (state) {
    is AssistantState.Idle       -> "Tap mic to speak"
    is AssistantState.Listening  -> "Listening…"
    is AssistantState.Typing     -> "Typing…"
    is AssistantState.Validating -> "Checking…"
    is AssistantState.Processing -> "Thinking…"
    is AssistantState.Responding -> "Responding…"
    is AssistantState.Error      -> "Something went wrong"
}

private fun auraMood(state: AssistantState, amplitude: Float): String {
    if (state !is AssistantState.Listening) return stateLabel(state)
    return when {
        amplitude < 0.05f -> "Aura is calm"
        amplitude < 0.15f -> "Aura is focused"
        amplitude < 0.30f -> "Aura is analytical"
        amplitude < 0.50f -> "Aura is engaged"
        amplitude < 0.70f -> "Aura is energetic"
        else              -> "Aura is excited"
    }
}