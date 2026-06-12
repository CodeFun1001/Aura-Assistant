package com.kastack.auraassistant.domain.usecases

import com.kastack.auraassistant.domain.models.Reminder
import com.kastack.auraassistant.domain.repositories.ReminderRepository
import com.kastack.auraassistant.notifications.ReminderScheduler
import java.util.Calendar
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedReminder(
    val title: String,
    val scheduledAt: Long
)

@Singleton
class CreateReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(input: String): ParsedReminder? {
        val parsed = parse(input) ?: return null

        val id = reminderRepository.insertReminder(
            Reminder(
                title       = parsed.title,
                scheduledAt = parsed.scheduledAt
            )
        )

        reminderScheduler.schedule(
            reminderId  = id,
            title       = parsed.title,
            triggerAtMs = parsed.scheduledAt
        )

        return parsed
    }

    private fun parse(input: String): ParsedReminder? {
        val lower = input.lowercase().trim()

        val atPattern = Pattern.compile(
            """remind me to (.+?) at (\d{1,2})(?::(\d{2}))?\s*(am|pm)?""",
            Pattern.CASE_INSENSITIVE
        )
        val atMatcher = atPattern.matcher(lower)
        if (atMatcher.find()) {
            val title    = atMatcher.group(1)?.trim()?.capitalise() ?: return null
            val hourRaw  = atMatcher.group(2)?.toIntOrNull() ?: return null
            val minute   = atMatcher.group(3)?.toIntOrNull() ?: 0
            val meridiem = atMatcher.group(4)?.lowercase()

            val hour24 = when {
                meridiem == "pm" && hourRaw < 12 -> hourRaw + 12
                meridiem == "am" && hourRaw == 12 -> 0
                else -> hourRaw
            }

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour24)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            return ParsedReminder(title, cal.timeInMillis)
        }

        val inPattern = Pattern.compile(
            """remind me to (.+?) in (\d+)\s*(minute|minutes|hour|hours)""",
            Pattern.CASE_INSENSITIVE
        )
        val inMatcher = inPattern.matcher(lower)
        if (inMatcher.find()) {
            val title  = inMatcher.group(1)?.trim()?.capitalise() ?: return null
            val amount = inMatcher.group(2)?.toLongOrNull() ?: return null
            val unit   = inMatcher.group(3)?.lowercase() ?: return null

            val offsetMs = when {
                unit.startsWith("hour") -> amount * 60 * 60 * 1000L
                else                   -> amount * 60 * 1000L
            }
            return ParsedReminder(title, System.currentTimeMillis() + offsetMs)
        }

        return null
    }

    private fun String.capitalise() =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}