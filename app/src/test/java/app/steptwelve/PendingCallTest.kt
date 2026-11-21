package app.steptwelve

import app.steptwelve.data.CallRecord
import app.steptwelve.data.CallType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingCallTest {
    private val pending = PendingCall("lk", setOf("+15551234567"), startedAt = 1_000_000)

    private fun call(number: String = "+15551234567", date: Long = 1_005_000, type: CallType = CallType.OUTGOING) =
        CallRecord(id = 1, numberKey = number, date = date, durationSeconds = 30, type = type)

    @Test fun matchesOutgoingCallToContactAfterDialing() = assertTrue(pending.matches(call()))

    @Test fun allowsSmallClockSkewBeforeDialing() = assertTrue(pending.matches(call(date = 995_000)))

    @Test fun ignoresOlderCalls() = assertFalse(pending.matches(call(date = 900_000)))

    @Test fun ignoresOtherNumbers() = assertFalse(pending.matches(call(number = "+15550000000")))

    @Test fun ignoresIncomingCalls() = assertFalse(pending.matches(call(type = CallType.INCOMING)))
}
