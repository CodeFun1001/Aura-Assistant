package com.kastack.auraassistant.domain.models

data class Reminder(
    val id: Long = 0,
    val title: String,
    val scheduledAt: Long,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)