package app.steptwelve.data

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

data class DeviceContact(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    /** Comparable keys for the contact's phone numbers (see [PhoneNumberKeys]). */
    val numberKeys: Set<String> = emptySet(),
)

/** Reads contacts live from the system provider. Nothing is copied into the app's database. */
class ContactsRepository(
    private val resolver: ContentResolver,
    private val keys: PhoneNumberKeys,
) {

    /** Emits the full contact list now and again whenever the system contacts change. */
    fun observeContacts(): Flow<List<DeviceContact>> =
        callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            // The authority URI also covers phone number (Data) changes.
            resolver.registerContentObserver(ContactsContract.AUTHORITY_URI, true, observer)
            trySend(Unit)
            awaitClose { resolver.unregisterContentObserver(observer) }
        }
            .conflate()
            .map { queryContacts() }
            .flowOn(Dispatchers.IO)

    private fun queryContacts(): List<DeviceContact> {
        val numbers = queryNumberKeys()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        )
        val contacts = mutableListOf<DeviceContact>()
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} IS NOT NULL",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val keyCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            while (cursor.moveToNext()) {
                contacts += DeviceContact(
                    id = cursor.getLong(idCol),
                    lookupKey = cursor.getString(keyCol) ?: continue,
                    displayName = cursor.getString(nameCol) ?: continue,
                    numberKeys = numbers[cursor.getLong(idCol)].orEmpty(),
                )
            }
        }
        return contacts
    }

    /** Contact id -> number keys for every phone number on that contact. */
    private fun queryNumberKeys(): Map<Long, Set<String>> {
        val result = HashMap<Long, MutableSet<String>>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normalizedCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            while (cursor.moveToNext()) {
                val key = keys.key(cursor.getString(numberCol), cursor.getString(normalizedCol)) ?: continue
                result.getOrPut(cursor.getLong(idCol)) { mutableSetOf() } += key
            }
        }
        return result
    }
}
