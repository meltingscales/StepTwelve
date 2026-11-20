package app.steptwelve.domain

import app.steptwelve.data.CallNote
import app.steptwelve.data.CallRecord
import app.steptwelve.data.CallType
import app.steptwelve.data.DeviceContact
import app.steptwelve.data.SortOrder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One thing that happened with a contact: a call-log entry (maybe with a note) or a note on its own. */
sealed interface OutreachEvent {
    val time: Long
    val note: CallNote?

    data class Call(val call: CallRecord, override val note: CallNote?) : OutreachEvent {
        override val time get() = call.date
    }

    /** A manual entry, or a note whose call is no longer in the system call log. */
    data class NoteOnly(override val note: CallNote) : OutreachEvent {
        override val time get() = note.timestamp
    }
}

/** A tagged contact with their calls and notes, newest first. */
data class OutreachContact(
    val contact: DeviceContact,
    val tags: List<String>,
    val calls: List<CallRecord>,
    val notes: List<CallNote>,
) {
    val lastCall: CallRecord? get() = calls.firstOrNull()

    /** Times you reached out: outgoing calls (connected or not) and manual entries. */
    val reachOutTimes: List<Long> by lazy {
        (calls.filter { it.type == CallType.OUTGOING }.map { it.date } +
            notes.filter { it.isManual }.map { it.timestamp }).sortedDescending()
    }

    /** Last time you were in touch: any reach-out, or an incoming call that connected. */
    val lastContactAt: Long? by lazy {
        listOfNotNull(
            reachOutTimes.firstOrNull(),
            calls.firstOrNull { it.type == CallType.INCOMING && it.connected }?.date,
        ).maxOrNull()
    }

    fun reachedOutOn(day: LocalDate, zone: ZoneId): Boolean = reachOutTimes.any { it.toLocalDate(zone) == day }

    val events: List<OutreachEvent> by lazy {
        val used = mutableSetOf<Long>()
        val callEvents = calls.map { call ->
            val note = notes.firstOrNull { !it.isManual && it.id !in used && it.matches(call) }
            if (note != null) used += note.id
            OutreachEvent.Call(call, note)
        }
        val noteOnly = notes.filter { it.id !in used }.map { OutreachEvent.NoteOnly(it) }
        (callEvents + noteOnly).sortedByDescending { it.time }
    }
}

fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

object Outreach {

    /**
     * Joins contacts, tags, calls and notes into tagged contacts. Untagged contacts are left out.
     * With [matchExtraFields], tags may also come from the contact's note, company or title.
     */
    fun build(
        contacts: List<DeviceContact>,
        enabledTags: List<String>,
        calls: List<CallRecord>,
        notes: List<CallNote>,
        matchExtraFields: Boolean = false,
    ): List<OutreachContact> {
        val callsByKey = calls.groupBy { it.numberKey }
        return contacts.mapNotNull { contact ->
            val text = if (matchExtraFields) "${contact.displayName} ${contact.extraText}" else contact.displayName
            val tags = TagMatcher.matchingTags(text, enabledTags)
            if (tags.isEmpty()) return@mapNotNull null
            OutreachContact(
                contact = contact,
                tags = tags,
                calls = contact.numberKeys.flatMap { callsByKey[it].orEmpty() }.sortedByDescending { it.date },
                notes = notes
                    .filter { it.contactLookupKey == contact.lookupKey || it.normalizedNumber in contact.numberKeys }
                    .sortedByDescending { it.timestamp },
            )
        }
    }

    fun sort(contacts: List<OutreachContact>, order: SortOrder): List<OutreachContact> {
        val byName = compareBy<OutreachContact, String>(String.CASE_INSENSITIVE_ORDER) { it.contact.displayName }
        return when (order) {
            // Never-contacted first, then longest since last contact, so neglected people rise to the top.
            SortOrder.NEGLECTED -> contacts.sortedWith(
                compareBy<OutreachContact, Long?>(nullsFirst()) { it.lastContactAt }.then(byName)
            )
            SortOrder.NAME -> contacts.sortedWith(byName)
            SortOrder.RECENT -> contacts.sortedWith(
                compareBy<OutreachContact, Long?>(nullsLast(reverseOrder())) { it.lastContactAt }.then(byName)
            )
        }
    }
}
