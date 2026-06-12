package com.kastack.auraassistant.domain.usecases

import com.kastack.auraassistant.domain.models.AssistantState
import com.kastack.auraassistant.domain.models.ChatMessage
import com.kastack.auraassistant.domain.models.MessageMeta
import com.kastack.auraassistant.domain.models.Sender
import com.kastack.auraassistant.domain.models.UserProfile
import com.kastack.auraassistant.domain.repositories.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

private const val PROCESSING_TIMEOUT_MS = 8_000L

@Singleton
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val createReminderUseCase: CreateReminderUseCase,
    private val responseStyleGenerator: ResponseStyleGenerator
) {
    suspend operator fun invoke(
        input: String,
        inputType: String = "text",
        userProfile: UserProfile,
        stateFlow: MutableStateFlow<AssistantState>
    ) {
        val startTime = System.currentTimeMillis()

        stateFlow.value = AssistantState.Validating(input)
        delay(120)

        chatRepository.insertMessage(
            ChatMessage(
                content = input,
                sender = Sender.USER,
                meta = MessageMeta(inputType = inputType)
            )
        )

        stateFlow.value = AssistantState.Processing(input)

        val response = withTimeout(PROCESSING_TIMEOUT_MS) {
            generateResponse(input, userProfile)
        }

        val processingMs = System.currentTimeMillis() - startTime
        stateFlow.value = AssistantState.Responding(response)
        delay(200)

        chatRepository.insertMessage(
            ChatMessage(
                content = response,
                sender = Sender.ASSISTANT,
                meta = MessageMeta(inputType = "text", processingTimeMs = processingMs)
            )
        )

        stateFlow.value = AssistantState.Idle
    }

    private suspend fun generateResponse(input: String, profile: UserProfile): String {
        delay(1_200L + (200L..800L).random())

        if (input.contains("remind", ignoreCase = true)) {
            val parsed = createReminderUseCase(input)
            return if (parsed != null) {
                responseStyleGenerator.reminderConfirmation(
                    title = parsed.title,
                    scheduledAt = parsed.scheduledAt,
                    profile = profile
                )
            } else {
                responseStyleGenerator.style(
                    base = "I couldn't parse that reminder. Try: \"Remind me to call mom at 6 PM\"",
                    profile = profile
                )
            }
        }

        val name = profile.name.ifBlank { "there" }
        val base = when {
            input.contains("hello", ignoreCase = true) ||
                    input.contains("hi", ignoreCase = true) ->
                "Hey $name! I'm Aura — your offline assistant. How can I help you today?"

            input.contains("help", ignoreCase = true) ->
                "I can help you think through problems, set reminders, or just be a sounding board. What's on your mind?"

            input.contains("weather", ignoreCase = true) ->
                "I work fully offline $name, so I don't have live weather data. That said, I can help you plan around it!"

            input.length < 10 ->
                "Could you tell me a bit more, $name? I want to make sure I respond well."

            else ->
                "Understood, $name. I've noted that. Is there anything else you'd like to explore?"
        }

        return responseStyleGenerator.style(base, profile)
    }
}