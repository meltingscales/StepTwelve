package app.steptwelve.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.test.core.app.ApplicationProvider
import app.steptwelve.domain.Outreach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Fake contacts provider: just enough of ContactsContract for the queries StepTwelve makes. */
class FakeContactsProvider : ContentProvider() {
    override fun onCreate() = true
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, args: Array<String>?, sort: String?): Cursor {
        val path = uri.path.orEmpty()
        return when {
            path.startsWith("/data/phones") -> MatrixCursor(projection).apply {
                addRow(arrayOf(1L, "(555) 000-0001", null)) // Ann, unformatted, no normalized value
                addRow(arrayOf(2L, "+1 555 000 0002", "+15550000002"))
                addRow(arrayOf(3L, "555-000-0003", "+15550000003"))
            }
            path.startsWith("/data") -> MatrixCursor(projection).apply {
                addRow(arrayOf(3L, "Tuesday home group, sponsor to Bob", null, Note.CONTENT_ITEM_TYPE))
                addRow(arrayOf(2L, "Acme", "SAA treasurer", Organization.CONTENT_ITEM_TYPE))
            }
            else -> MatrixCursor(projection).apply {
                addRow(arrayOf(1L, "k-ann", "Ann AA"))
                addRow(arrayOf(2L, "k-bob", "Bob"))
                addRow(arrayOf(3L, "k-cal", "Cal"))
                addRow(arrayOf(4L, "k-aaron", "Aaron"))
            }
        }
    }
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<String>?) = 0
}

class FakeCallLogProvider : ContentProvider() {
    override fun onCreate() = true
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, args: Array<String>?, sort: String?): Cursor =
        MatrixCursor(projection).apply {
            // _ID, NUMBER, CACHED_NORMALIZED_NUMBER, DATE, DURATION, TYPE
            addRow(arrayOf(10L, "555-000-0001", null, 3_000L, 0L, CallLog.Calls.OUTGOING_TYPE))
            addRow(arrayOf(11L, "+1 (555) 000-0002", "+15550000002", 2_000L, 95L, CallLog.Calls.INCOMING_TYPE))
            addRow(arrayOf(12L, "5559999", null, 1_000L, 0L, CallLog.Calls.MISSED_TYPE))
        }
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<String>?) = 0
}

@RunWith(RobolectricTestRunner::class)
class ProvidersTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var keys: PhoneNumberKeys

    @Before fun setUp() {
        Robolectric.buildContentProvider(FakeContactsProvider::class.java).create(ContactsContract.AUTHORITY)
        Robolectric.buildContentProvider(FakeCallLogProvider::class.java).create(CallLog.AUTHORITY)
        keys = PhoneNumberKeys(context)
    }

    @Test fun `contacts come with number keys, primary number and extra text`() = runTest {
        val contacts = ContactsRepository(context.contentResolver, keys).observeContacts().first()
        val ann = contacts.first { it.lookupKey == "k-ann" }
        // Same key as the call log's differently formatted "555-000-0001".
        assertEquals(setOf(keys.key("555-000-0001")), ann.numberKeys)
        assertEquals("(555) 000-0001", ann.primaryNumber)
        assertEquals("Acme SAA treasurer", contacts.first { it.lookupKey == "k-bob" }.extraText)
        assertEquals("", contacts.first { it.lookupKey == "k-aaron" }.extraText)
    }

    @Test fun `different number formats match calls to contacts`() = runTest {
        val contacts = ContactsRepository(context.contentResolver, keys).observeContacts().first()
        val calls = CallLogRepository(context.contentResolver, keys).observeCalls().first()
        assertEquals(listOf(CallType.OUTGOING, CallType.INCOMING, CallType.MISSED), calls.map { it.type })

        val tags = listOf("aa", "saa", "sponsor")
        val byName = Outreach.build(contacts, tags, calls, emptyList())
        assertEquals(listOf("Ann AA"), byName.map { it.contact.displayName })
        assertEquals(listOf(10L), byName.single().calls.map { it.id })

        val withExtras = Outreach.build(contacts, tags, calls, emptyList(), matchExtraFields = true)
        assertEquals(listOf("Ann AA", "Bob", "Cal"), withExtras.map { it.contact.displayName })
        assertEquals(listOf(11L), withExtras.first { it.contact.displayName == "Bob" }.calls.map { it.id })
        assertEquals(listOf("sponsor"), withExtras.first { it.contact.displayName == "Cal" }.tags)
    }
}
