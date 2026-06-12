package com.kastack.auraassistant.presentation.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastack.auraassistant.audio.AudioRecorderManager
import com.kastack.auraassistant.audio.SpeechRecognizerManager
import com.kastack.auraassistant.audio.SpeechState
import com.kastack.auraassistant.data.sync.SyncManager
import com.kastack.auraassistant.domain.models.AssistantState
import com.kastack.auraassistant.domain.models.Reminder
import com.kastack.auraassistant.domain.models.SyncStatus
import com.kastack.auraassistant.domain.models.UserProfile
import com.kastack.auraassistant.domain.repositories.ReminderRepository
import com.kastack.auraassistant.domain.repositories.UserProfileRepository
import com.kastack.auraassistant.domain.usecases.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userProfile: UserProfile = UserProfile(),
    val inputText: String = "",
    val showKeyboard: Boolean = false,
    val hasMicPermission: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileRepository: UserProfileRepository,
    private val reminderRepository: ReminderRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val audioRecorderManager: AudioRecorderManager,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val syncManager: SyncManager               // ← Phase 8
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    val speechState: StateFlow<SpeechState> = speechRecognizerManager.state

    /** Extra 4 — exposes SyncManager's status to the UI */
    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    /** Extra 5 — today's reminders, updated in real-time from Room */
    val todayReminders: StateFlow<List<Reminder>> = reminderRepository
        .getTodayReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private var pipelineJob: Job? = null
    private var amplitudeJob: Job? = null

    init {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }

        viewModelScope.launch {
            speechRecognizerManager.state.collect { speechState ->
                when (speechState) {
                    is SpeechState.Result -> {
                        stopAmplitudeCollection()
                        if (_assistantState.value is AssistantState.Listening) {
                            _assistantState.value = AssistantState.Processing(speechState.text)
                        }
                        sendMessage(speechState.text, inputType = "voice")
                        speechRecognizerManager.resetToIdle()
                    }
                    is SpeechState.Error -> {
                        stopAmplitudeCollection()
                        if (_assistantState.value is AssistantState.Listening) {
                            _assistantState.value = AssistantState.Error(
                                message = speechState.msg,
                                retryInput = null
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        checkMicPermission()

        // Kick off a sync on launch
        syncManager.requestSync()
    }

    fun checkMicPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasMicPermission = granted) }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(inputText = text) }

    fun startListening() {
        if (!_uiState.value.hasMicPermission) return
        _assistantState.value = AssistantState.Listening
        speechRecognizerManager.startListening()
        startAmplitudeCollection()
    }

    fun stopListening() {
        speechRecognizerManager.stopListening()
        stopAmplitudeCollection()
        if (_assistantState.value is AssistantState.Listening) {
            _assistantState.value = AssistantState.Idle
        }
    }

    private fun startAmplitudeCollection() {
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch {
            @Suppress("MissingPermission")
            audioRecorderManager.amplitudeFlow().collect { amp ->
                _amplitude.value = amp
            }
        }
    }

    private fun stopAmplitudeCollection() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitude.value = 0f
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
                    message = "Response took too long — please try again.",
                    retryInput = trimmed
                )
            } catch (e: Exception) {
                if (e.message?.contains("cancelled", ignoreCase = true) == true) return@launch
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

    fun dismissError() {
        _assistantState.value = AssistantState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.destroy()
        pipelineJob?.cancel()
        amplitudeJob?.cancel()
    }
}