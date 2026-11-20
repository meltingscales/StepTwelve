package app.steptwelve.backup

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.steptwelve.StepTwelveApp
import app.steptwelve.data.CallNote
import app.steptwelve.data.EntryType
import app.steptwelve.data.Settings
import app.steptwelve.data.Tag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {
    private val app: StepTwelveApp = ApplicationProvider.getApplicationContext()
    private val container get() = app.container

    private fun note(text: String, time: Long) = CallNote(
        callLogId = null, contactLookupKey = "k", contactName = "Ann AA", normalizedNumber = "+15550001",
        timestamp = time, entryType = EntryType.IN_PERSON, outcome = null, note = text, followUpAt = null,
        createdAt = 1, updatedAt = 1,
    )

    @Test fun `export then merge import adds only new notes, replace swaps everything`() = runTest {
        val dao = container.database.noteDao()
        dao.insertAll(listOf(note("one", 1_000), note("two", 2_000)))
        val file = File(app.cacheDir, "export.zip")
        val uri = Uri.fromFile(file)

        container.backupManager.export(uri, "secret pass".toCharArray())
        assertTrue(file.length() > 0)
        val bundle = container.backupManager.preview(uri, "secret pass".toCharArray())
        assertEquals(2, bundle.notes.size)

        // Merging the same export adds nothing.
        assertEquals(0, container.backupManager.import(bundle, replace = false))
        assertEquals(2, dao.getAll().size)

        // A bundle with one new note and one duplicate adds one; new tags are merged in.
        val newer = bundle.copy(notes = bundle.notes + note("three", 3_000), tags = bundle.tags + Tag(label = "newtag"))
        assertEquals(1, container.backupManager.import(newer, replace = false))
        assertEquals(listOf("three", "two", "one"), dao.getAll().map { it.note })
        assertTrue(container.tagRepository.getAll().any { it.label == "newtag" })

        // Replace swaps notes, tags and settings.
        val replacement = bundle.copy(
            notes = listOf(note("only", 5_000)),
            tags = listOf(Tag(label = "aa")),
            settings = Settings(dailyGoal = 7),
        )
        assertEquals(1, container.backupManager.import(replacement, replace = true))
        assertEquals(listOf("only"), dao.getAll().map { it.note })
        assertEquals(listOf("aa"), container.tagRepository.getAll().map { it.label })
        assertEquals(7, container.settingsRepository.current().dailyGoal)
    }
}
