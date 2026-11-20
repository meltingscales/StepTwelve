package app.steptwelve.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.steptwelve.StepTwelveApp
import kotlinx.coroutines.launch

/** Alarms don't survive reboots, app updates or clock changes, so re-create them. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val container = (context.applicationContext as StepTwelveApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                rescheduleAll(container)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )

        suspend fun rescheduleAll(container: app.steptwelve.AppContainer) {
            val settings = container.settingsRepository.current()
            container.reminderScheduler.syncDaily(settings.reminderEnabled, settings.reminderMinuteOfDay)
            container.database.noteDao().pendingFollowUps(System.currentTimeMillis()).forEach {
                container.reminderScheduler.syncFollowUp(it.id, it.followUpAt)
            }
        }
    }
}
