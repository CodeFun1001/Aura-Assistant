package com.kastack.auraassistant.presentation.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kastack.auraassistant.audio.SpeechState
import com.kastack.auraassistant.domain.models.AssistantState
import com.kastack.auraassistant.domain.models.Reminder
import com.kastack.auraassistant.presentation.components.AuraCircle
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.painterResource
import com.kastack.auraassistant.R

private val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
private val reminderTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToReminders: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val scrollOffset by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex * 1f +
                    listState.firstVisibleItemScrollOffset / 600f
        }
    }
    val auraAlpha by animateFloatAsState(
        targetValue = (1f - scrollOffset * 0.9f).coerceIn(0f, 1f),
        animationSpec = tween(80),
        label = "aura_alpha"
    )
    val auraTranslationY by animateFloatAsState(
        targetValue = -scrollOffset * 40f,
        animationSpec = tween(80),
        label = "aura_parallax"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.checkMicPermission()
            viewModel.startListening()
        }
    }

    LaunchedEffect(uiState.showKeyboard) {
        if (uiState.showKeyboard) focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item(key = "hero") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 56.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GreetingHeader(
                            name = uiState.userProfile.name,
                            assistantState = assistantState
                        )

                        Spacer(Modifier.height(32.dp))

                        Box(
                            modifier = Modifier
                                .alpha(auraAlpha)
                                .graphicsLayer { translationY = auraTranslationY },
                            contentAlignment = Alignment.Center
                        ) {
                            AuraCircle(
                                state = assistantState,
                                amplitude = amplitude,
                                size = 280.dp
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        AnimatedContent(
                            targetState = stateLabel(assistantState),
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                            },
                            label = "state_label"
                        ) { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                    }
                }

                item(key = "chips") {
                    QuickActionChips(
                        onNavigateToChat = onNavigateToChat,
                        onNavigateToReminders = onNavigateToReminders,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                if (uiState.todayReminders.isNotEmpty()) {
                    item(key = "reminders_header") {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Today",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(
                        items = uiState.todayReminders,
                        key = { it.id }
                    ) { reminder ->
                        ReminderChip(reminder = reminder)
                    }
                }

                item(key = "history_hint") {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "↑ Scroll for chat history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            HomeInputBar(
                inputText = uiState.inputText,
                showKeyboard = uiState.showKeyboard,
                assistantState = assistantState,
                hasMicPermission = uiState.hasMicPermission,
                focusRequester = focusRequester,
                onInputChange = viewModel::onInputChange,
                onSend = { viewModel.sendMessage() },
                onToggleKeyboard = viewModel::toggleKeyboard,
                onMicClick = {
                    if (uiState.hasMicPermission) {
                        if (assistantState is AssistantState.Listening) viewModel.stopListening()
                        else viewModel.startListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (assistantState is AssistantState.Error) {
                ErrorBanner(
                    state = assistantState as AssistantState.Error,
                    onRetry = viewModel::retry,
                    onDismiss = viewModel::dismissError,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                )
            }
        }
    }
}

@Composable
private fun GreetingHeader(name: String, assistantState: AssistantState) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeOfDay = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val displayName = name.ifBlank { null }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Text(
            text = if (displayName != null) "$timeOfDay, $displayName" else timeOfDay,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = dateFormat.format(Date()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun QuickActionChips(
    onNavigateToChat: () -> Unit,
    onNavigateToReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onNavigateToChat,
            label = { Text("Chat") },
            leadingIcon = { Icon(
                painter = painterResource(R.drawable.chat),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            ) }
        )
        AssistChip(
            onClick = onNavigateToReminders,
            label = { Text("Reminders") },
            leadingIcon = { Icon(
                painter = painterResource(R.drawable.alarm),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            ) }
        )
        AssistChip(
            onClick = onNavigateToChat,
            label = { Text("History") },
            leadingIcon = { Icon(
                painter = painterResource(R.drawable.history),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            ) }
        )
    }
}

@Composable
private fun ReminderChip(reminder: Reminder) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.alarm),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(reminder.title, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            reminderTimeFormat.format(Date(reminder.scheduledAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HomeInputBar(
    inputText: String,
    showKeyboard: Boolean,
    assistantState: AssistantState,
    hasMicPermission: Boolean,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = assistantState is AssistantState.Listening

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            AnimatedVisibility(
                visible = showKeyboard,
                enter = slideInVertically { it } + fadeIn(tween(200)),
                exit = slideOutVertically { it } + fadeOut(tween(150))
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text("Ask Aura anything…") },
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleKeyboard) {
                    AnimatedContent(
                        targetState = showKeyboard,
                        label = "keyboard_icon",
                        transitionSpec = {
                            scaleIn(tween(150)) + fadeIn() togetherWith scaleOut(tween(150)) + fadeOut()
                        }
                    ) { showing ->
                        Icon(
                            painter = painterResource(
                                id = if (showing)
                                    R.drawable.mic
                                else
                                    R.drawable.keyboard
                            ),
                            contentDescription = if (showing)
                                "Switch to voice"
                            else
                                "Switch to keyboard",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                AnimatedContent(
                    targetState = showKeyboard && inputText.isNotBlank(),
                    label = "primary_action",
                    transitionSpec = {
                        scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                fadeIn() togetherWith scaleOut(tween(120)) + fadeOut()
                    }
                ) { isSend ->
                    if (isSend) {
                        FilledIconButton(
                            onClick = onSend,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    } else {
                        val micScale by animateFloatAsState(
                            targetValue = if (isListening) 1.18f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "mic_scale"
                        )
                        FilledIconButton(
                            onClick = onMicClick,
                            modifier = Modifier
                                .size(56.dp)
                                .graphicsLayer { scaleX = micScale; scaleY = micScale },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isListening)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isListening)
                                        R.drawable.mic_off
                                    else
                                        R.drawable.mic
                                ),
                                contentDescription = if (isListening)
                                    "Stop listening"
                                else
                                    "Speak to Aura",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    state: AssistantState.Error,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (state.retryInput != null) {
                TextButton(onClick = onRetry) { Text("Retry") }
            } else {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun stateLabel(state: AssistantState): String = when (state) {
    is AssistantState.Idle -> "Tap mic to speak"
    is AssistantState.Listening -> "Listening…"
    is AssistantState.Typing -> "Typing…"
    is AssistantState.Validating -> "Checking…"
    is AssistantState.Processing -> "Thinking…"
    is AssistantState.Responding -> "Responding…"
    is AssistantState.Error -> "Something went wrong"
}
