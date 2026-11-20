package app.steptwelve.domain

import app.steptwelve.data.CallNote
import app.steptwelve.data.CallRecord
import app.steptwelve.data.CallType
import app.steptwelve.data.DeviceContact
import app.steptwelve.data.EntryType
import app.steptwelve.data.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class OutreachTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 20)
    private fun at(daysAgo: Long, hour: Int = 12) =
        today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private val tags = listOf("aa", "saa", "sponsor")
    private val ann = DeviceContact(1, "k-ann", "Ann AA", setOf("+15550001"), "555-0001")
    private val bob = DeviceContact(2, "k-bob", "Bob SAA", setOf("+15550002"), "555-0002")
    private val cal = DeviceContact(3, "k-cal", "Cal Sponsor", setOf("+15550003"), "555-0003")
    private val aaron = DeviceContact(4, "k-aaron", "Aaron", setOf("+15550004"), "555-0004")

    private var nextId = 100L
    private fun call(key: String, daysAgo: Long, type: CallType, duration: Long = 60, hour: Int = 12) =
        CallRecord(nextId++, key, at(daysAgo, hour), duration, type)

    private fun note(
        contact: DeviceContact, time: Long, type: EntryType, callId: Long? = null, createdDaysAgo: Long = 0,
    ) = CallNote(
        id = nextId++, callLogId = callId, contactLookupKey = contact.lookupKey, contactName = contact.displayName,
        normalizedNumber = contact.numberKeys.first(), timestamp = time, entryType = type, outcome = null,
        note = "n", followUpAt = null, createdAt = at(createdDaysAgo), updatedAt = at(createdDaysAgo),
    )

    @Test fun `build keeps only tagged contacts and attaches calls and notes`() {
        val c1 = call("+15550001", 2, CallType.OUTGOING)
        val c2 = call("+15550004", 1, CallType.OUTGOING)
        val n1 = note(ann, c1.date, EntryType.PHONE_CALL, callId = c1.id)
        val result = Outreach.build(listOf(ann, bob, aaron), tags, listOf(c1, c2), listOf(n1))
        assertEquals(listOf("Ann AA", "Bob SAA"), result.map { it.contact.displayName })
        assertEquals(listOf(c1), result[0].calls)
        assertEquals(listOf(n1), result[0].notes)
        assertEquals(n1, (result[0].events.single() as OutreachEvent.Call).note)
    }

    @Test fun `note matches call by number and time when the call id changed`() {
        val c = call("+15550001", 1, CallType.OUTGOING)
        val n = note(ann, c.date, EntryType.PHONE_CALL, callId = 99999)
        val ann2 = Outreach.build(listOf(ann), tags, listOf(c), listOf(n)).single()
        assertEquals(n, (ann2.events.single() as OutreachEvent.Call).note)
    }

    @Test fun `orphan and manual notes become note-only events`() {
        val manual = note(ann, at(3), EntryType.IN_PERSON)
        val orphan = note(ann, at(4), EntryType.PHONE_CALL, callId = 1)
        val c = Outreach.build(listOf(ann), tags, emptyList(), listOf(manual, orphan)).single()
        assertEquals(listOf(manual, orphan), c.events.map { (it as OutreachEvent.NoteOnly).note })
    }

    @Test fun `last contact counts reach-outs and connected incoming calls only`() {
        val missedIncoming = call("+15550001", 0, CallType.INCOMING, duration = 0)
        val outgoing = call("+15550001", 5, CallType.OUTGOING, duration = 0)
        val c = Outreach.build(listOf(ann), tags, listOf(missedIncoming, outgoing), emptyList()).single()
        assertEquals(outgoing.date, c.lastContactAt)
        assertFalse(c.reachedOutOn(today, zone))

        val answeredIncoming = call("+15550001", 0, CallType.INCOMING, duration = 30)
        val c2 = Outreach.build(listOf(ann), tags, listOf(answeredIncoming, outgoing), emptyList()).single()
        assertEquals(answeredIncoming.date, c2.lastContactAt)
    }

    @Test fun `neglected sort puts never-contacted first then oldest`() {
        val calls = listOf(call("+15550001", 1, CallType.OUTGOING), call("+15550002", 10, CallType.OUTGOING))
        val built = Outreach.build(listOf(ann, bob, cal), tags, calls, emptyList())
        assertEquals(listOf("Cal Sponsor", "Bob SAA", "Ann AA"),
            Outreach.sort(built, SortOrder.NEGLECTED).map { it.contact.displayName })
        assertEquals(listOf("Ann AA", "Bob SAA", "Cal Sponsor"),
            Outreach.sort(built, SortOrder.RECENT).map { it.contact.displayName })
        assertEquals(listOf("Ann AA", "Bob SAA", "Cal Sponsor"),
            Outreach.sort(built, SortOrder.NAME).map { it.contact.displayName })
        assertNull(built.first { it.contact == cal }.lastContactAt)
    }

    @Test fun `goal counts outgoing calls connected or not plus manual entries`() {
        val calls = listOf(
            call("+15550001", 0, CallType.OUTGOING, duration = 0),
            call("+15550002", 0, CallType.OUTGOING, duration = 120),
            call("+15550002", 0, CallType.INCOMING, duration = 120),
            call("+15550003", 1, CallType.OUTGOING),
        )
        val notes = listOf(note(cal, at(0), EntryType.TEXT))
        val built = Outreach.build(listOf(ann, bob, cal), tags, calls, notes)
        assertEquals(3, Goals.reachOutsOn(built, today, zone))
        assertTrue(built.first { it.contact == ann }.reachedOutOn(today, zone))
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 3), Goals.lastSevenDays(built, today, zone).map { it.second })
    }

    @Test fun `weekly summary`() {
        val calls = listOf(
            call("+15550001", 0, CallType.OUTGOING, duration = 0),   // not reached
            call("+15550002", 3, CallType.OUTGOING, duration = 60),  // reached
            call("+15550003", 8, CallType.OUTGOING, duration = 60),  // outside week
        )
        val notes = listOf(note(cal, at(2), EntryType.IN_PERSON, createdDaysAgo = 2), note(ann, at(9), EntryType.TEXT, createdDaysAgo = 9))
        val built = Outreach.build(listOf(ann, bob, cal), tags, calls, notes)
        assertEquals(WeeklySummary(callsMade = 2, peopleReached = 2, notesWritten = 1), Goals.weekly(built, today, zone))
    }
}
