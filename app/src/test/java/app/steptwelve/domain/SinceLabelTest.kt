package app.steptwelve.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SinceLabelTest {
    private val today = LocalDate.of(2026, 11, 20)

    private fun daysAgo(n: Long) = SinceLabel.format(today.minusDays(n), today)

    @Test fun `never called`() = assertEquals("never", SinceLabel.format(null, today))

    @Test fun `recent days`() {
        assertEquals("today", daysAgo(0))
        assertEquals("yesterday", daysAgo(1))
        assertEquals("2d ago", daysAgo(2))
        assertEquals("13d ago", daysAgo(13))
    }

    @Test fun `weeks months years`() {
        assertEquals("2w ago", daysAgo(14))
        assertEquals("8w ago", daysAgo(59))
        assertEquals("2mo ago", SinceLabel.format(today.minusMonths(2), today))
        assertEquals("11mo ago", SinceLabel.format(today.minusMonths(11), today))
        assertEquals("1y ago", SinceLabel.format(today.minusMonths(12), today))
    }

    @Test fun `uses calendar days not 24h periods`() {
        val zone = ZoneId.of("UTC")
        val lateYesterday = today.minusDays(1).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val earlyToday = today.atTime(0, 30).atZone(zone).toInstant().toEpochMilli()
        assertEquals("yesterday", SinceLabel.format(lateYesterday, earlyToday, zone))
    }
}
