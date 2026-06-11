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
    private val chatRepository: ChatRepository
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

        val userMessage = ChatMessage(
            content = input,
            sender = Sender.USER,
            meta = MessageMeta(inputType = inputType)
        )
        chatRepository.insertMessage(userMessage)

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
                meta = MessageMeta(
                    inputType = "text",
                    processingTimeMs = processingMs
                )
            )
        )

        stateFlow.value = AssistantState.Idle
    }

    private suspend fun generateResponse(input: String, profile: UserProfile): String {
        delay(1_200L + (200L..800L).random())

        val name = profile.name.ifBlank { "there" }
        val traits = profile.traits

        val tone = when {
            "Analytical" in traits -> "analytically"
            "Curious" in traits -> "thoughtfully"
            "Focused" in traits -> "concisely"
            "Creative" in traits -> "creatively"
            "Calm" in traits -> "calmly"
            else -> "helpfully"
        }

        return when {
            input.contains("remind", ignoreCase = true) ->
                "Got it, $name. I'll remember that $tone. You can view your reminders by tapping the reminders section."
            input.contains("hello", ignoreCase = true) || input.contains("hi", ignoreCase = true) ->
                "Hey $name! I'm Aura. How can I help you today?"
            input.contains("help", ignoreCase = true) ->
                "Sure, $name. I'm your personal offline assistant. Ask me anything, set reminders, or just think out loud."
            input.contains("weather", ignoreCase = true) ->
                "I work fully offline, $name — no live weather data. But I can help you plan around it $tone!"
            input.length < 10 ->
                "I hear you, $name. Could you tell me more so I can respond $tone?"
            else ->
                "Understood, $name. I've noted that $tone. Is there anything else on your mind?"
        }
    }
}