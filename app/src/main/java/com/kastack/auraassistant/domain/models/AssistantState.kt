package com.kastack.auraassistant.domain.models

sealed class AssistantState {
    data object Idle : AssistantState()
    data class Typing(val partial: String = "") : AssistantState()
    data class Validating(val input: String) : AssistantState()
    data class Processing(val input: String) : AssistantState()
    data class Responding(val response: String) : AssistantState()
    data class Error(val message: String, val retryInput: String? = null) : AssistantState()
    data object Listening : AssistantState()
}