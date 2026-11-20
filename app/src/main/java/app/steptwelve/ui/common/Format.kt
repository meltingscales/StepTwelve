package app.steptwelve.ui.common

import app.steptwelve.data.CallType
import app.steptwelve.data.EntryType
import app.steptwelve.data.Outcome
import app.steptwelve.domain.OutreachEvent
import app.steptwelve.domain.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object Format {
    private val zone get() = ZoneId.systemDefault()
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val dayYearFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

    fun time(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(timeFormat)

    fun day(date: LocalDate, today: LocalDate = LocalDate.now(zone)): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(if (date.year == today.year) dayFormat else dayYearFormat)
    }

    fun dayOf(millis: Long): String = day(millis.toLocalDate(zone))

    fun dateTime(millis: Long): String = "${dayOf(millis)}, ${time(millis)}"

    fun duration(seconds: Long): String = when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }

    fun outcome(outcome: Outcome): String = when (outcome) {
        Outcome.ANSWERED -> "Answered"
        Outcome.VOICEMAIL -> "Voicemail"
        Outcome.NO_ANSWER -> "No answer"
        Outcome.TEXTED -> "Texted instead"
    }

    fun entryType(type: EntryType): String = when (type) {
        EntryType.PHONE_CALL -> "Phone call"
        EntryType.OTHER_APP_CALL -> "Call (other app)"
        EntryType.TEXT -> "Text"
        EntryType.IN_PERSON -> "In person"
    }

    /** "Outgoing · 4m 12s", "Missed", "In person". */
    fun eventSummary(event: OutreachEvent): String = when (event) {
        is OutreachEvent.Call -> {
            val call = event.call
            when (call.type) {
                CallType.OUTGOING -> if (call.connected) "Outgoing · ${duration(call.durationSeconds)}" else "Outgoing · no answer"
                CallType.INCOMING -> if (call.connected) "Incoming · ${duration(call.durationSeconds)}" else "Incoming · not connected"
                CallType.MISSED -> "Missed"
                CallType.OTHER -> "Call"
            }
        }
        is OutreachEvent.NoteOnly -> entryType(event.note.entryType)
    }
}
