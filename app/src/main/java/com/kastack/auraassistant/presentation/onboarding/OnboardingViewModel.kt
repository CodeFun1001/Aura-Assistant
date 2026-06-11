package com.kastack.auraassistant.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kastack.auraassistant.domain.models.UserProfile
import com.kastack.auraassistant.domain.repositories.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val name: String = "",
    val age: String = "",
    val phone: String = "",
    val otpInput: String = "",
    val isOtpSent: Boolean = false,
    val isOtpVerified: Boolean = false,
    val selectedTraits: List<String> = emptyList(),
    val nameError: String? = null,
    val ageError: String? = null,
    val phoneError: String? = null,
    val otpError: String? = null,
    val traitError: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update {
        it.copy(name = value, nameError = null)
    }

    fun onAgeChange(value: String) = _uiState.update {
        it.copy(age = value, ageError = null)
    }

    fun onPhoneChange(value: String) = _uiState.update {
        it.copy(phone = value, phoneError = null)
    }

    fun onOtpChange(value: String) = _uiState.update {
        it.copy(otpInput = value, otpError = null)
    }

    fun sendOtp() {
        val phone = _uiState.value.phone
        if (phone.length != 10) {
            _uiState.update { it.copy(phoneError = "Enter a valid 10-digit phone number") }
            return
        }
        _uiState.update { it.copy(isOtpSent = true, otpError = null) }
    }

    fun verifyOtp(): Boolean {
        return if (_uiState.value.otpInput == "1234") {
            _uiState.update { it.copy(isOtpVerified = true, otpError = null) }
            true
        } else {
            _uiState.update { it.copy(otpError = "Invalid OTP. Hint: 1234") }
            false
        }
    }

    fun toggleTrait(trait: String) {
        val current = _uiState.value.selectedTraits.toMutableList()
        if (current.contains(trait)) {
            current.remove(trait)
        } else if (current.size < 3) {
            current.add(trait)
        }
        _uiState.update { it.copy(selectedTraits = current, traitError = null) }
    }

    fun goToNextStep(): Boolean {
        return when (_uiState.value.currentStep) {
            0 -> {
                _uiState.update { it.copy(currentStep = 1) }
                true
            }
            1 -> {
                if (validateProfileStep()) {
                    _uiState.update { it.copy(currentStep = 2) }
                    true
                } else false
            }
            else -> false
        }
    }

    fun goToPreviousStep() {
        val step = _uiState.value.currentStep
        if (step > 0) _uiState.update { it.copy(currentStep = step - 1) }
    }

    private fun validateProfileStep(): Boolean {
        val state = _uiState.value
        var valid = true
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            valid = false
        }
        if (state.age.isBlank() || state.age.toIntOrNull() == null) {
            _uiState.update { it.copy(ageError = "Enter a valid age") }
            valid = false
        }
        if (state.phone.length != 10) {
            _uiState.update { it.copy(phoneError = "Enter a valid 10-digit number") }
            valid = false
        }
        if (!state.isOtpVerified) {
            _uiState.update { it.copy(otpError = "Please verify your phone number") }
            valid = false
        }
        return valid
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.selectedTraits.size < 3) {
            _uiState.update { it.copy(traitError = "Please select exactly 3 traits") }
            return
        }
        viewModelScope.launch {
            userProfileRepository.saveUserProfile(
                UserProfile(
                    name = state.name,
                    age = state.age,
                    phone = state.phone,
                    traits = state.selectedTraits,
                    isOnboardingComplete = true
                )
            )
            onComplete()
        }
    }
}