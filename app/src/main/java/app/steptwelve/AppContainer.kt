package app.steptwelve

import android.content.Context
import app.steptwelve.data.AppDatabase
import app.steptwelve.data.CallLogRepository
import app.steptwelve.data.ContactsRepository
import app.steptwelve.data.PhoneNumberKeys
import app.steptwelve.data.TagRepository

/** Manual dependency injection: one instance per process, owned by [StepTwelveApp]. */
class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    val tagRepository = TagRepository(database.tagDao())
    private val phoneNumberKeys = PhoneNumberKeys(context)
    val contactsRepository = ContactsRepository(context.contentResolver, phoneNumberKeys)
    val callLogRepository = CallLogRepository(context.contentResolver, phoneNumberKeys)
}
