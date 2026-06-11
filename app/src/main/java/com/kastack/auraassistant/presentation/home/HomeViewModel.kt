package com.kastack.auraassistant.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastack.auraassistant.audio.AudioRecorderManager
import com.kastack.auraassistant.audio.SpeechRecognizerManager
import com.kastack.auraassistant.audio.SpeechState
import com.kastack.auraassistant.domain.models.AssistantState
import com.kastack.auraassistant.domain.models.Reminder
import com.kastack.auraassistant.domain.models.UserProfile
import com.kastack.auraassistant.domain.repositories.ReminderRepository
import com.kastack.auraassistant.domain.repositories.UserProfileRepository
import com.kastack.auraassistant.domain.usecases.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userProfile: UserProfile = UserProfile(),
    val inputText: String = "",
    val showKeyboard: Boolean = false,
    val todayReminders: List<Reminder> = emptyList(),
    val hasMicPermission: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileRepository: UserProfileRepository,
    private val reminderRepository: ReminderRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    val audioRecorderManager: AudioRecorderManager,
    val speechRecognizerManager: SpeechRecognizerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    val speechState: StateFlow<SpeechState> = speechRecognizerManager.state

    private var pipelineJob: Job? = null
    private var amplitudeJob: Job? = null

    init {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
        viewModelScope.launch {
            reminderRepository.getTodayReminders().collect { reminders ->
                _uiState.update { it.copy(todayReminders = reminders) }
            }
        }
        viewModelScope.launch {
            speechRecognizerManager.state.collect { speechState ->
                if (speechState is SpeechState.Result) {
                    stopListening()
                    sendMessage(speechState.text, inputType = "voice")
                }
            }
        }
        checkMicPermission()
    }

    fun checkMicPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasMicPermission = hasPermission) }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(inputText = text) }

    fun toggleKeyboard() = _uiState.update { it.copy(showKeyboard = !it.showKeyboard) }

    fun startListening() {
        if (!_uiState.value.hasMicPermission) return
        _assistantState.value = AssistantState.Listening
        speechRecognizerManager.startListening()

        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch {
            @Suppress("MissingPermission")
            audioRecorderManager.amplitudeFlow().collect { amp ->
                _amplitude.value = amp
            }
        }
    }

    fun stopListening() {
        speechRecognizerManager.stopListening()
        amplitudeJob?.cancel()
        _amplitude.value = 0f
        if (_assistantState.value is AssistantState.Listening) {
            _assistantState.value = AssistantState.Idle
        }
    }

    fun sendMessage(text: String = _uiState.value.inputText, inputType: String = "text") {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _uiState.update { it.copy(inputText = "", showKeyboard = false) }
        pipelineJob?.cancel()

        pipelineJob = viewModelScope.launch {
            try {
                sendMessageUseCase(
                    input = trimmed,
                    inputType = inputType,
                    userProfile = _uiState.value.userProfile,
                    stateFlow = _assistantState
                )
            } catch (e: TimeoutCancellationException) {
                _assistantState.value = AssistantState.Error(
                    message = "Response took too long.",
                    retryInput = trimmed
                )
            } catch (e: Exception) {
                if (e.message?.contains("cancelled") == true) return@launch
                _assistantState.value = AssistantState.Error(
                    message = "Something went wrong.",
                    retryInput = trimmed
                )
            }
        }
    }

    fun retry() {
        val s = _assistantState.value
        if (s is AssistantState.Error && s.retryInput != null) {
            _assistantState.value = AssistantState.Idle
            sendMessage(s.retryInput)
        }
    }

    fun dismissError() { _assistantState.value = AssistantState.Idle }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.destroy()
        audioRecorderManager.stopRecording()
        pipelineJob?.cancel()
        amplitudeJob?.cancel()
    }
}