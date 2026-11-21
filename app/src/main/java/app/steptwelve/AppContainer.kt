package app.steptwelve

import android.content.Context
import app.steptwelve.data.AppDatabase
import app.steptwelve.data.CallLogRepository
import app.steptwelve.data.ContactsRepository
import app.steptwelve.data.NoteRepository
import app.steptwelve.data.OutreachRepository
import app.steptwelve.data.PhoneNumberKeys
import app.steptwelve.data.SettingsRepository
import app.steptwelve.data.TagRepository
import app.steptwelve.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/** Manual dependency injection: one instance per process, owned by [StepTwelveApp]. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database = AppDatabase.create(context)
    val settingsRepository = SettingsRepository(context)
    val tagRepository = TagRepository(database.tagDao())
    val reminderScheduler = ReminderScheduler(context)
    val noteRepository = NoteRepository(database.noteDao(), reminderScheduler)
    private val phoneNumberKeys = PhoneNumberKeys(context)
    val callLogRepository = CallLogRepository(context.contentResolver, phoneNumberKeys)
    val outreachRepository = OutreachRepository(
        scope = appScope,
        tagRepository = tagRepository,
        contactsRepository = ContactsRepository(context.contentResolver, phoneNumberKeys),
        callLogRepository = callLogRepository,
        noteDao = database.noteDao(),
        settingsRepository = settingsRepository,
    )
    val postCallTracker = PostCallTracker()
    val appLock = AppLock()
    val backupManager = app.steptwelve.backup.BackupManager(context, this)
}
