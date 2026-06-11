package com.kastack.auraassistant.domain.models

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Synced(val at: Long) : SyncStatus()
    data class Failed(val reason: String) : SyncStatus()
}