package com.kastack.auraassistant.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastack.auraassistant.domain.models.Reminder
import com.kastack.auraassistant.domain.repositories.ReminderRepository
import com.kastack.auraassistant.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemindersUiState(
    val reminders: List<Reminder> = emptyList()
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = reminderRepository
        .getAllReminders()
        .map { RemindersUiState(reminders = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RemindersUiState()
        )

    fun addReminder(title: String, scheduledAt: Long) {
        viewModelScope.launch {
            val id = reminderRepository.insertReminder(
                Reminder(title = title, scheduledAt = scheduledAt)
            )
            reminderScheduler.schedule(
                reminderId  = id,
                title       = title,
                triggerAtMs = scheduledAt
            )
        }
    }

    fun markComplete(id: Long) {
        viewModelScope.launch {
            reminderRepository.markCompleted(id)
            reminderScheduler.cancel(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            reminderRepository.markCompleted(id)
            reminderScheduler.cancel(id)
        }
    }
}