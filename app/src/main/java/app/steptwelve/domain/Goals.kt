package app.steptwelve.domain

import java.time.LocalDate
import java.time.ZoneId

data class WeeklySummary(val callsMade: Int, val peopleReached: Int, val notesWritten: Int)

object Goals {

    /** Outgoing calls to tagged contacts (connected or not) plus manual entries on [day]. */
    fun reachOutsOn(contacts: List<OutreachContact>, day: LocalDate, zone: ZoneId): Int =
        contacts.sumOf { c -> c.reachOutTimes.count { it.toLocalDate(zone) == day } }

    /** Daily reach-out counts for the 7 days ending on [today], oldest first. */
    fun lastSevenDays(contacts: List<OutreachContact>, today: LocalDate, zone: ZoneId): List<Pair<LocalDate, Int>> =
        (6 downTo 0).map { back -> today.minusDays(back.toLong()).let { it to reachOutsOn(contacts, it, zone) } }

    /** The 7 days ending on [today]. */
    fun weekly(contacts: List<OutreachContact>, today: LocalDate, zone: ZoneId): WeeklySummary {
        val start = today.minusDays(6)
        fun inWeek(time: Long) = time.toLocalDate(zone).let { !it.isBefore(start) && !it.isAfter(today) }
        return WeeklySummary(
            callsMade = contacts.sumOf { c ->
                c.calls.count { it.type == app.steptwelve.data.CallType.OUTGOING && inWeek(it.date) }
            },
            peopleReached = contacts.count { c ->
                c.calls.any { it.connected && inWeek(it.date) } || c.notes.any { it.isManual && inWeek(it.timestamp) }
            },
            notesWritten = contacts.sumOf { c -> c.notes.count { inWeek(it.createdAt) } },
        )
    }
}
