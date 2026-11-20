package app.steptwelve.data

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.steptwelve.reminders.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private fun note(id: Long = 0, followUpAt: Long? = null, text: String = "hi") = CallNote(
        id = id, callLogId = 7, contactLookupKey = "k", contactName = "Ann AA", normalizedNumber = "+15550001",
        timestamp = 1_000, entryType = EntryType.PHONE_CALL, outcome = Outcome.ANSWERED, note = text,
        followUpAt = followUpAt, createdAt = 0, updatedAt = 0,
    )

    @Test fun `migrating 1 to 3 keeps tags, adds new defaults and the notes table`() {
        // Schemas are packaged into debug assets only, so this runs in testDebugUnitTest.
        assumeTrue(context.assets.list("app.steptwelve.data.AppDatabase").orEmpty().isNotEmpty())
        helper.createDatabase("migration-test", 1).use { db ->
            db.execSQL("INSERT INTO Tag (label, enabled) VALUES ('aa', 1)")
            db.execSQL("INSERT INTO Tag (label, enabled) VALUES ('mytag', 0)")
        }
        val db = helper.runMigrationsAndValidate("migration-test", 3, true, AppDatabase.MIGRATION_1_2)
        val labels = mutableListOf<String>()
        db.query("SELECT label FROM Tag ORDER BY label").use { c -> while (c.moveToNext()) labels += c.getString(0) }
        assertEquals(listOf("aa", "mytag", "sponsee", "sponsor"), labels)
        db.query("SELECT COUNT(*) FROM CallNote").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.close()
    }

    @Test fun `fresh database seeds the default tags`() = runTest {
        context.deleteDatabase("steptwelve.db")
        val db = AppDatabase.create(context)
        assertEquals(TagRepository.DEFAULT_TAGS.sorted(), db.tagDao().getAll().map { it.label })
        db.close()
    }

    @Test fun `tag repository normalizes labels and rejects duplicates`() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = TagRepository(db.tagDao())
        assertTrue(repo.add(" 12 - Step "))
        assertEquals(false, repo.add("12step"))
        assertEquals(false, repo.add("  "))
        val tag = repo.tags.first().single()
        assertEquals("12step", tag.label)
        assertTrue(repo.add("aa"))
        assertEquals(false, repo.rename(tag, "AA"))
        assertTrue(repo.rename(tag, "Twelve Step"))
        assertEquals(listOf("aa", "twelvestep"), repo.getAll().map { it.label })
        db.close()
    }

    @Test fun `saving notes keeps ids and schedules or cancels follow-up alarms`() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = NoteRepository(db.noteDao(), ReminderScheduler(context))
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        val future = System.currentTimeMillis() + 86_400_000

        val id = repo.save(note(followUpAt = future))
        assertNotEquals(0L, id)
        assertEquals(1, alarms.scheduledAlarms.size)
        val saved = db.noteDao().get(id)!!
        assertTrue(saved.createdAt > 0)

        assertEquals(id, repo.save(saved.copy(note = "edited", followUpAt = null)))
        assertEquals("edited", db.noteDao().get(id)!!.note)
        assertEquals(1, db.noteDao().getAll().size)
        assertEquals(0, alarms.scheduledAlarms.size)

        repo.save(saved.copy(followUpAt = future))
        assertEquals(listOf(id), db.noteDao().pendingFollowUps(System.currentTimeMillis()).map { it.id })
        repo.delete(saved)
        assertNull(db.noteDao().get(id))
        assertEquals(0, alarms.scheduledAlarms.size)
        db.close()
    }
}
