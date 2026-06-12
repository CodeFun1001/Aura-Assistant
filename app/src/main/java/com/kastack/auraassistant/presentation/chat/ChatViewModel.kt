package com.kastack.auraassistant.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kastack.auraassistant.audio.SpeechRecognizerManager
import com.kastack.auraassistant.audio.SpeechState
import com.kastack.auraassistant.domain.models.AssistantState
import com.kastack.auraassistant.domain.models.UserProfile
import com.kastack.auraassistant.domain.repositories.ChatRepository
import com.kastack.auraassistant.domain.repositories.UserProfileRepository
import com.kastack.auraassistant.domain.usecases.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val inputText: String   = "",
    val showKeyboard: Boolean = false,
    val assistantState: AssistantState = AssistantState.Idle,
    val userProfile: UserProfile = UserProfile()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userProfileRepository: UserProfileRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    val speechRecognizerManager: SpeechRecognizerManager
) : ViewModel() {

    val pagedMessages = chatRepository
        .getPagedMessages()
        .cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    val speechState: StateFlow<SpeechState> = speechRecognizerManager.state

    private var pipelineJob: Job? = null

    init {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }

    fun onSpeechRecognised(text: String) {
        _uiState.update { it.copy(inputText = text, showKeyboard = true) }
        _assistantState.value = AssistantState.Idle
        speechRecognizerManager.resetToIdle()
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
        if (text.isNotBlank() && _assistantState.value is AssistantState.Idle) {
            _assistantState.value = AssistantState.Typing(text)
        } else if (text.isBlank()) {
            _assistantState.value = AssistantState.Idle
        }
    }

    fun toggleKeyboard() = _uiState.update { it.copy(showKeyboard = !it.showKeyboard) }
    fun hideKeyboard()   = _uiState.update { it.copy(showKeyboard = false) }

    fun sendMessage(text: String = _uiState.value.inputText, inputType: String = "text") {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _uiState.update { it.copy(inputText = "", showKeyboard = false) }

        pipelineJob?.cancel()

        pipelineJob = viewModelScope.launch {
            try {
                sendMessageUseCase(
                    input       = trimmed,
                    inputType   = inputType,
                    userProfile = _uiState.value.userProfile,
                    stateFlow   = _assistantState
                )
            } catch (e: TimeoutCancellationException) {
                _assistantState.value = AssistantState.Error(
                    message    = "Response took too long. Please try again.",
                    retryInput = trimmed
                )
            } catch (e: Exception) {
                if (e.message?.contains("StandaloneCoroutine was cancelled") == true) return@launch
                _assistantState.value = AssistantState.Error(
                    message    = "Something went wrong: ${e.message}",
                    retryInput = trimmed
                )
            }
        }
    }

    fun retry() {
        val current = _assistantState.value
        if (current is AssistantState.Error && current.retryInput != null) {
            _assistantState.value = AssistantState.Idle
            sendMessage(current.retryInput)
        }
    }

    fun dismissError() { _assistantState.value = AssistantState.Idle }

    fun startListening() {
        _assistantState.value = AssistantState.Listening
        speechRecognizerManager.startListening()
    }

    fun stopListening() {
        speechRecognizerManager.stopListening()
        if (_assistantState.value is AssistantState.Listening) {
            _assistantState.value = AssistantState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.destroy()
        pipelineJob?.cancel()
    }
}