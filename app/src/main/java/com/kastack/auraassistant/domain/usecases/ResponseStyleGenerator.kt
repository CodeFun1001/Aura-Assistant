package com.kastack.auraassistant.domain.usecases

import com.kastack.auraassistant.domain.models.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponseStyleGenerator @Inject constructor() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun style(base: String, profile: UserProfile): String {
        var result = base
        val traits = profile.traits

        for (trait in traits) {
            result = applyTrait(trait, result, profile)
        }
        return result
    }

    fun reminderConfirmation(title: String, scheduledAt: Long, profile: UserProfile): String {
        val time = timeFormat.format(Date(scheduledAt))
        val base = "Got it — I've set a reminder: \"$title\" at $time."
        return style(base, profile)
    }

    private fun applyTrait(trait: String, text: String, profile: UserProfile): String {
        val name = profile.name.ifBlank { "you" }
        return when (trait) {

            "Focused" -> {
                text.split(". ").firstOrNull()?.trimEnd('.') ?: text
            }

            "Curious" -> {
                val insight = curiosityInsight(text)
                if (insight != null) "$text\n\n💡 $insight" else text
            }

            "Analytical" -> {
                val clauses = text.split(Regex("[.!?] ")).filter { it.isNotBlank() }
                if (clauses.size >= 2) {
                    clauses.mapIndexed { i, s -> "${i + 1}. ${s.trim().trimEnd('.')}." }
                        .joinToString("\n")
                } else text
            }

            "Creative" -> {
                val metaphor = creativeReframe(text)
                if (metaphor != null) "$text\n\n✨ $metaphor" else text
            }

            "Calm" -> {
                text
                    .replace(Regex("\\!"), ".")
                    .replace("urgent", "worth noting")
                    .replace("must", "might")
                    .replace("immediately", "when you're ready")
            }

            "Energetic" -> "$text 🚀"

            "Strategic" -> {
                "Here's the outcome to aim for: $text"
            }

            "Empathetic" -> {
                "I hear you, $name. $text"
            }

            "Minimalist" -> {
                val words = text.split(" ")
                if (words.size > 15) words.take(15).joinToString(" ") + "…"
                else text
            }

            "Ambitious" -> {
                "$text This could also be a step toward a bigger goal — worth keeping in mind."
            }

            "Playful" -> {
                val quip = playfulSuffix()
                "$text $quip"
            }

            "Disciplined" -> {
                "$text\n\nNext step: decide on one concrete action before moving on."
            }

            else -> text
        }
    }

    private fun curiosityInsight(text: String): String? = when {
        text.contains("reminder", ignoreCase = true) ->
            "Studies show that time-blocking reminders improve follow-through by ~40%."
        text.contains("help", ignoreCase = true) ->
            "Breaking a problem into 3 sub-questions often reveals the answer faster."
        text.contains("think", ignoreCase = true) ->
            "Writing thoughts out loud improves clarity more than just thinking internally."
        else -> null
    }

    private fun creativeReframe(text: String): String? = when {
        text.contains("reminder", ignoreCase = true) ->
            "Think of reminders as breadcrumbs you leave for your future self."
        text.contains("offline", ignoreCase = true) ->
            "Like a trusted notebook — always there, no signal required."
        else -> null
    }

    private fun playfulSuffix(): String {
        val options = listOf(
            "(No pressure though 😄)",
            "(You've got this!)",
            "(Easy peasy, lemon squeezy 🍋)",
            "(Consider it done ✓)"
        )
        return options.random()
    }
}