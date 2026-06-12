package com.kastack.auraassistant.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeechState {
    data object Idle       : SpeechState()
    data object Listening  : SpeechState()
    data object Processing : SpeechState()
    data class  Result(val text: String)              : SpeechState()
    data class  Error(val code: Int, val msg: String) : SpeechState()
}

@Singleton
class SpeechRecognizerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechState.Listening
        }

        override fun onBeginningOfSpeech() {
            _state.value = SpeechState.Listening
        }

        override fun onEndOfSpeech() {
            _state.value = SpeechState.Processing
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = matches
                ?.firstOrNull { it.isNotBlank() }
                ?.trim()

            _state.value = if (!best.isNullOrBlank()) {
                SpeechState.Result(best)
            } else {
                SpeechState.Error(-1, "No speech detected — try again")
            }
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH             -> "No speech matched — please try again"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT       -> "Listening timed out — please try again"
                SpeechRecognizer.ERROR_AUDIO                -> "Audio recording error"
                SpeechRecognizer.ERROR_NETWORK              -> "Network unavailable"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT      -> "Network timeout"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY      -> "Recognizer busy — please wait"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
                SpeechRecognizer.ERROR_CLIENT               -> "Client error — restarting"
                SpeechRecognizer.ERROR_SERVER               -> "Server error"
                else                                        -> "Speech error (code $error)"
            }
            _state.value = SpeechState.Error(error, msg)
        }
    }

    fun startListening(languageCode: String = "en-IN") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SpeechState.Error(-1, "Speech recognition is not available on this device")
            return
        }

        destroyRecognizer()

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            sr.setRecognitionListener(listener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
        }

        _state.value = SpeechState.Listening
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        if (_state.value is SpeechState.Listening) {
            _state.value = SpeechState.Processing
        }
    }

    fun resetToIdle() {
        _state.value = SpeechState.Idle
    }

    fun destroy() {
        destroyRecognizer()
        _state.value = SpeechState.Idle
    }

    private fun destroyRecognizer() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }
}