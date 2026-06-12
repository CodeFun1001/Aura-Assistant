package com.kastack.auraassistant.presentation.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kastack.auraassistant.R
import com.kastack.auraassistant.audio.SpeechState
import com.kastack.auraassistant.domain.models.AssistantState
import com.kastack.auraassistant.presentation.components.AuraLoadingIndicator
import com.kastack.auraassistant.presentation.components.AuraMessageBubble
import com.kastack.auraassistant.presentation.components.AuraTypingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val speechState    by viewModel.speechState.collectAsStateWithLifecycle()
    val pagedMessages  = viewModel.pagedMessages.collectAsLazyPagingItems()
    val listState      = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(pagedMessages.itemCount) {
        if (pagedMessages.itemCount > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(uiState.showKeyboard) {
        if (uiState.showKeyboard) focusRequester.requestFocus()
    }

    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Result) {
            val recognised = (speechState as SpeechState.Result).text
            viewModel.onSpeechRecognised(recognised)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Aura", fontWeight = FontWeight.SemiBold)
                        Text(
                            text  = assistantStateLabel(assistantState),
                            style = MaterialTheme.typography.labelSmall,
                            color = assistantStateColor(assistantState)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText      = uiState.inputText,
                showKeyboard   = uiState.showKeyboard,
                assistantState = assistantState,
                speechState    = speechState,
                focusRequester = focusRequester,
                onInputChange  = viewModel::onInputChange,
                onSend         = { viewModel.sendMessage() },
                onToggleKeyboard = viewModel::toggleKeyboard,
                onMicStart     = viewModel::startListening,
                onMicStop      = viewModel::stopListening,
                onRetry        = viewModel::retry,
                onDismissError = viewModel::dismissError
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                pagedMessages.loadState.refresh is LoadState.Loading -> {
                    AuraLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }
                pagedMessages.loadState.refresh is LoadState.Error -> {
                    ChatErrorState(
                        message = "Couldn't load messages.",
                        onRetry = { pagedMessages.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                pagedMessages.itemCount == 0 &&
                        pagedMessages.loadState.refresh is LoadState.NotLoading -> {
                    ChatEmptyState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        state          = listState,
                        reverseLayout  = true,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier       = Modifier.fillMaxSize()
                    ) {
                        val showTyping = assistantState is AssistantState.Typing    ||
                                assistantState is AssistantState.Processing ||
                                assistantState is AssistantState.Responding  ||
                                assistantState is AssistantState.Validating
                        if (showTyping) {
                            item(key = "typing_indicator") {
                                AuraTypingIndicator(modifier = Modifier.animateItem())
                            }
                        }

                        items(
                            count = pagedMessages.itemCount,
                            key   = pagedMessages.itemKey { it.id }
                        ) { index ->
                            val message = pagedMessages[index]
                            if (message != null) {
                                AuraMessageBubble(
                                    content   = message.content,
                                    sender    = message.sender,
                                    timestamp = message.timestamp,
                                    modifier  = Modifier.animateItem()
                                )
                            }
                        }

                        if (pagedMessages.loadState.append is LoadState.Loading) {
                            item { AuraLoadingIndicator(modifier = Modifier.padding(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    showKeyboard: Boolean,
    assistantState: AssistantState,
    speechState: SpeechState,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onMicStart: () -> Unit,
    onMicStop: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit
) {
    val isListening = assistantState is AssistantState.Listening ||
            speechState is SpeechState.Listening
    val isError     = assistantState is AssistantState.Error

    Column {
        AnimatedVisibility(
            visible = isError,
            enter = slideInVertically { it } + fadeIn(),
            exit  = slideOutVertically { it } + fadeOut()
        ) {
            if (assistantState is AssistantState.Error) {
                Surface(
                    color    = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = assistantState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (assistantState.retryInput != null) {
                            TextButton(onClick = onRetry) {
                                Icon(Icons.Default.Refresh, contentDescription = null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Retry")
                            }
                        } else {
                            TextButton(onClick = onDismissError) { Text("Dismiss") }
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleKeyboard,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (showKeyboard) R.drawable.mic else R.drawable.keyboard
                            ),
                            contentDescription = if (showKeyboard) "Switch to voice"
                            else "Switch to keyboard",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = showKeyboard,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness    = Spring.StiffnessMedium
                            )
                        ) { it / 2 } + fadeIn(tween(180)),
                        exit = slideOutVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) { it / 2 } + fadeOut(tween(150)),
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value         = inputText,
                            onValueChange = onInputChange,
                            placeholder   = { Text("Message Aura…") },
                            shape         = RoundedCornerShape(24.dp),
                            maxLines      = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSend() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    AnimatedContent(
                        targetState = showKeyboard && inputText.isNotBlank(),
                        label       = "action_btn",
                        transitionSpec = {
                            scaleIn(tween(150)) + fadeIn() togetherWith
                                    scaleOut(tween(150)) + fadeOut()
                        }
                    ) { showSend ->
                        if (showSend) {
                            FilledIconButton(
                                onClick  = onSend,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        } else {
                            val micScale by animateFloatAsState(
                                targetValue  = if (isListening) 1.18f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label        = "mic_scale"
                            )
                            FilledIconButton(
                                onClick = if (isListening) onMicStop else onMicStart,
                                modifier = Modifier
                                    .size(40.dp)
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
                                        if (isListening) R.drawable.mic_off else R.drawable.mic
                                    ),
                                    contentDescription = if (isListening) "Stop" else "Speak"
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isListening || speechState is SpeechState.Processing,
                    enter = fadeIn(tween(200)),
                    exit  = fadeOut(tween(150))
                ) {
                    Text(
                        text = when {
                            speechState is SpeechState.Processing -> "Recognising speech…"
                            else -> "Listening — tap mic to stop"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✨", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap the mic or keyboard to start a conversation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChatErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun assistantStateLabel(state: AssistantState): String = when (state) {
    is AssistantState.Idle       -> "Ready"
    is AssistantState.Listening  -> "Listening…"
    is AssistantState.Typing     -> "Typing…"
    is AssistantState.Validating -> "Checking…"
    is AssistantState.Processing -> "Thinking…"
    is AssistantState.Responding -> "Responding…"
    is AssistantState.Error      -> "Error"
}

@Composable
private fun assistantStateColor(state: AssistantState): Color = when (state) {
    is AssistantState.Idle       -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    is AssistantState.Listening  -> MaterialTheme.colorScheme.primary
    is AssistantState.Typing,
    is AssistantState.Validating -> MaterialTheme.colorScheme.secondary
    is AssistantState.Processing -> Color(0xFFF59E0B)
    is AssistantState.Responding -> Color(0xFF10B981)
    is AssistantState.Error      -> MaterialTheme.colorScheme.error
}