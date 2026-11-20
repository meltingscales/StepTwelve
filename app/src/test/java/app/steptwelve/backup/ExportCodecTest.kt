package app.steptwelve.backup

import app.steptwelve.data.CallNote
import app.steptwelve.data.EntryType
import app.steptwelve.data.Outcome
import app.steptwelve.data.Settings
import app.steptwelve.data.SortOrder
import app.steptwelve.data.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExportCodecTest {
    private val notes = listOf(
        CallNote(1, 55, "k1", "Ann, \"the\" AA", "+15550001", 1_700_000_000_000, EntryType.PHONE_CALL,
            Outcome.ANSWERED, "Talked about step 4.\nGood call, ❤", 1_700_100_000_000, 1, 2),
        CallNote(2, null, "k2", "Bob SAA", "+15550002", 1_700_000_500_000, EntryType.IN_PERSON,
            null, "=SUM(A1)", null, 3, 4),
    )
    private val bundle = ExportBundle(
        manifest = ExportManifest(appVersion = "1.0.0", schemaVersion = ExportCodec.SCHEMA_VERSION, exportedAt = 123),
        notes = notes,
        tags = listOf(Tag(1, "aa", true), Tag(2, "sponsor", false)),
        settings = Settings(dailyGoal = 5, sortOrder = SortOrder.NAME, appLock = true),
    )

    private fun roundTrip(b: ExportBundle): ExportBundle {
        val out = ByteArrayOutputStream()
        ExportCodec.write(b, out)
        return ExportCodec.read(ByteArrayInputStream(out.toByteArray()))
    }

    @Test fun `export then import gives identical data`() {
        assertEquals(bundle, roundTrip(bundle))
    }

    @Test fun `zip has the documented files`() {
        val out = ByteArrayOutputStream()
        ExportCodec.write(bundle, out)
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            while (true) names += (zip.nextEntry ?: break).name
        }
        assertEquals(listOf("manifest.json", "notes.json", "notes.csv", "tags.json", "settings.json"), names)
    }

    @Test fun `csv escapes quotes commas newlines and formulas`() {
        val csv = ExportCodec.toCsv(notes)
        assertTrue(csv.contains("\"Ann, \"\"the\"\" AA\""))
        assertTrue(csv.contains("\"Talked about step 4.\nGood call, ❤\""))
        assertTrue(csv.contains("'=SUM(A1)"))
        assertEquals(3, csv.split("\r\n").filter { it.isNotEmpty() }.size)
    }

    @Test fun `newer schema is rejected`() {
        val newer = bundle.copy(manifest = bundle.manifest.copy(schemaVersion = ExportCodec.SCHEMA_VERSION + 1))
        val e = assertThrows(ImportException::class.java) { roundTrip(newer) }
        assertTrue(e.message!!.contains("newer version"))
    }

    @Test fun `non-export zip and garbage are rejected`() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { it.putNextEntry(ZipEntry("hello.txt")); it.write("hi".toByteArray()); it.closeEntry() }
        assertThrows(ImportException::class.java) { ExportCodec.read(ByteArrayInputStream(out.toByteArray())) }
        assertThrows(ImportException::class.java) { ExportCodec.read(ByteArrayInputStream("not a zip".toByteArray())) }
    }

    @Test fun `unknown fields from a future minor version are ignored`() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"app":"StepTwelve","appVersion":"9","schemaVersion":1,"exportedAt":1,"extra":true}""".toByteArray())
            zip.closeEntry()
        }
        val read = ExportCodec.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(emptyList<CallNote>(), read.notes)
        assertEquals(Settings(), read.settings)
    }
}
