package app.steptwelve.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Short, calm "time since" label by calendar day: "today", "yesterday", "3d ago",
 * "2w ago", "4mo ago", "1y ago". Null [thenMillis] means no call on record.
 */
object SinceLabel {
    fun format(thenMillis: Long?, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        if (thenMillis == null) return "never"
        val then = Instant.ofEpochMilli(thenMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(then, today)
        return when {
            days <= 0 -> "today"
            days == 1L -> "yesterday"
            days < 14 -> "${days}d ago"
            days < 60 -> "${days / 7}w ago"
            else -> {
                val months = ChronoUnit.MONTHS.between(then, today)
                if (months < 12) "${months}mo ago" else "${ChronoUnit.YEARS.between(then, today)}y ago"
            }
        }
    }

    /** Convenience for tests and previews. */
    fun format(then: LocalDate?, today: LocalDate): String {
        val zone = ZoneId.of("UTC")
        return format(then?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(), today.atStartOfDay(zone).toInstant().toEpochMilli(), zone)
    }
}
