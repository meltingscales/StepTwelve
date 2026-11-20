package app.steptwelve.data

import android.content.ContentResolver
import android.provider.ContactsContract
import kotlinx.coroutines.flow.Flow

data class DeviceContact(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    /** Comparable keys for the contact's phone numbers (see [PhoneNumberKeys]). */
    val numberKeys: Set<String> = emptySet(),
    /** First phone number as stored, used for Call and Text actions. */
    val primaryNumber: String? = null,
)

/** Reads contacts live from the system provider. Nothing is copied into the app's database. */
class ContactsRepository(
    private val resolver: ContentResolver,
    private val keys: PhoneNumberKeys,
) {

    /** Emits the full contact list now and again whenever the system contacts change. */
    fun observeContacts(): Flow<List<DeviceContact>> =
        // The authority URI also covers phone number (Data) changes.
        resolver.observeQuery(ContactsContract.AUTHORITY_URI) { queryContacts() }

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
                    numberKeys = numbers[cursor.getLong(idCol)]?.keys.orEmpty(),
                    primaryNumber = numbers[cursor.getLong(idCol)]?.firstRaw,
                )
            }
        }
        return contacts
    }

    private class Numbers(var firstRaw: String? = null, val keys: MutableSet<String> = mutableSetOf())

    /** Contact id -> keys for every phone number on that contact (primary number first). */
    private fun queryNumberKeys(): Map<Long, Numbers> {
        val result = HashMap<Long, Numbers>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC, ${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normalizedCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            while (cursor.moveToNext()) {
                val raw = cursor.getString(numberCol)
                val key = keys.key(raw, cursor.getString(normalizedCol)) ?: continue
                val numbers = result.getOrPut(cursor.getLong(idCol)) { Numbers() }
                if (numbers.firstRaw == null) numbers.firstRaw = raw
                numbers.keys += key
            }
        }
        return result
    }
}
