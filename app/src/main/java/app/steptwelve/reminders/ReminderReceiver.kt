package app.steptwelve.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.steptwelve.MainActivity
import app.steptwelve.R
import app.steptwelve.StepTwelveApp
import kotlinx.coroutines.launch

/**
 * Posts reminder notifications. The lock-screen (public) version is always generic, so it
 * never reveals a contact name or what the app is for.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as StepTwelveApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                val settings = container.settingsRepository.current()
                val appName = if (settings.discreetIcon) "Circle Back" else "StepTwelve"
                when (intent.action) {
                    ReminderScheduler.ACTION_DAILY -> {
                        notify(context, DAILY_NOTIFICATION_ID, appName, "Time for outreach calls?")
                        // Schedule tomorrow's.
                        container.reminderScheduler.syncDaily(settings.reminderEnabled, settings.reminderMinuteOfDay)
                    }
                    ReminderScheduler.ACTION_FOLLOW_UP -> {
                        val noteId = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1)
                        val note = container.database.noteDao().get(noteId)
                        if (note != null) {
                            notify(context, (FOLLOW_UP_BASE + noteId).toInt(), appName, "Follow up with ${note.contactName}")
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, id: Int, appName: String, privateText: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val title = "$appName reminder"
        val publicVersion = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .build()
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(privateText)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private companion object {
        const val DAILY_NOTIFICATION_ID = 1
        const val FOLLOW_UP_BASE = 1_000L
    }
}
