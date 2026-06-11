package com.kastack.auraassistant.domain.models

data class ChatMessage(
    val id: Long = 0,
    val content: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis(),
    val meta: MessageMeta = MessageMeta()
)

enum class Sender { USER, ASSISTANT }

data class MessageMeta(
    val inputType: String = "text",
    val synced: Boolean = false,
    val processingTimeMs: Long = 0L
)