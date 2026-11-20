package app.steptwelve.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules reminders with AlarmManager (inexact windows, no extra permissions).
 * WorkManager isn't used because it adds ACCESS_NETWORK_STATE to the manifest.
 */
class ReminderScheduler(private val context: Context) {

    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Daily outreach and follow-up reminders"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun syncDaily(enabled: Boolean, minuteOfDay: Int) {
        val pi = pendingIntent(ACTION_DAILY, REQUEST_DAILY, null)
        alarms.cancel(pi)
        if (enabled) {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, nextDaily(minuteOfDay), WINDOW_MS, pi)
        }
    }

    /** Schedules (or cancels, when [at] is null or past) the follow-up reminder for a note. */
    fun syncFollowUp(noteId: Long, at: Long?) {
        val pi = pendingIntent(ACTION_FOLLOW_UP, followUpRequestCode(noteId), noteId)
        alarms.cancel(pi)
        if (at != null && at > System.currentTimeMillis()) {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pi)
        }
    }

    private fun pendingIntent(action: String, requestCode: Int, noteId: Long?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        if (noteId != null) intent.putExtra(EXTRA_NOTE_ID, noteId)
        return PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val ACTION_DAILY = "app.steptwelve.action.DAILY_REMINDER"
        const val ACTION_FOLLOW_UP = "app.steptwelve.action.FOLLOW_UP_REMINDER"
        const val EXTRA_NOTE_ID = "note_id"
        private const val REQUEST_DAILY = 1
        private const val WINDOW_MS = 10 * 60 * 1000L

        private fun followUpRequestCode(noteId: Long) = (1_000 + noteId).toInt()

        fun nextDaily(minuteOfDay: Int, now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): Long {
            val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
            var next = ZonedDateTime.of(LocalDate.from(now), time, now.zone)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return next.toInstant().toEpochMilli()
        }
    }
}
