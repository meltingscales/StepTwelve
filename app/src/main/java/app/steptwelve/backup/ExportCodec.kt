package app.steptwelve.backup

import app.steptwelve.data.CallNote
import app.steptwelve.data.Settings
import app.steptwelve.data.Tag
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class ExportManifest(
    val app: String = "StepTwelve",
    val appVersion: String,
    val schemaVersion: Int,
    val exportedAt: Long,
)

/** Everything StepTwelve creates. Contacts and the call log are never exported. */
data class ExportBundle(
    val manifest: ExportManifest,
    val notes: List<CallNote>,
    val tags: List<Tag>,
    val settings: Settings,
)

class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Reads and writes the export ZIP: manifest.json, notes.json, notes.csv, tags.json, settings.json. */
object ExportCodec {
    const val SCHEMA_VERSION = 1
    private const val MAX_ENTRY_BYTES = 32L * 1024 * 1024

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun write(bundle: ExportBundle, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("manifest.json", json.encodeToString(ExportManifest.serializer(), bundle.manifest))
            entry("notes.json", json.encodeToString(ListSerializer(CallNote.serializer()), bundle.notes))
            entry("notes.csv", toCsv(bundle.notes))
            entry("tags.json", json.encodeToString(ListSerializer(Tag.serializer()), bundle.tags))
            entry("settings.json", json.encodeToString(Settings.serializer(), bundle.settings))
        }
    }

    fun read(input: InputStream): ExportBundle {
        val files = HashMap<String, String>()
        try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name in KNOWN_FILES) files[entry.name] = readLimited(zip)
                }
            }
        } catch (e: java.io.IOException) {
            throw ImportException("This file isn't a readable ZIP.", e)
        }
        val manifestText = files["manifest.json"] ?: throw ImportException("This isn't a StepTwelve export (no manifest.json).")
        try {
            val manifest = json.decodeFromString(ExportManifest.serializer(), manifestText)
            if (manifest.app != "StepTwelve") throw ImportException("This isn't a StepTwelve export.")
            if (manifest.schemaVersion > SCHEMA_VERSION) {
                throw ImportException("This export was made by a newer version of StepTwelve. Update the app first.")
            }
            if (manifest.schemaVersion < 1) throw ImportException("Unknown export version ${manifest.schemaVersion}.")
            return ExportBundle(
                manifest = manifest,
                notes = files["notes.json"]?.let { json.decodeFromString(ListSerializer(CallNote.serializer()), it) }.orEmpty(),
                tags = files["tags.json"]?.let { json.decodeFromString(ListSerializer(Tag.serializer()), it) }.orEmpty(),
                settings = files["settings.json"]?.let { json.decodeFromString(Settings.serializer(), it) } ?: Settings(),
            )
        } catch (e: SerializationException) {
            throw ImportException("The export file is damaged or incomplete.", e)
        } catch (e: IllegalArgumentException) {
            throw ImportException("The export file is damaged or incomplete.", e)
        }
    }

    private fun readLimited(input: InputStream): String {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0L
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            total += n
            if (total > MAX_ENTRY_BYTES) throw ImportException("The export file is too large.")
            buffer.write(chunk, 0, n)
        }
        return buffer.toString(Charsets.UTF_8.name())
    }

    fun toCsv(notes: List<CallNote>): String = buildString {
        appendCsvRow(listOf("date_time_utc", "contact", "number", "type", "outcome", "note", "follow_up_utc"))
        notes.sortedBy { it.timestamp }.forEach { n ->
            appendCsvRow(
                listOf(
                    Instant.ofEpochMilli(n.timestamp).toString(),
                    n.contactName,
                    n.normalizedNumber,
                    n.entryType.name.lowercase(),
                    n.outcome?.name?.lowercase().orEmpty(),
                    n.note,
                    n.followUpAt?.let { Instant.ofEpochMilli(it).toString() }.orEmpty(),
                )
            )
        }
    }

    private fun StringBuilder.appendCsvRow(fields: List<String>) {
        fields.joinTo(this, ",") { field ->
            // Quote everything that needs it; neutralize spreadsheet formula injection.
            val safe = if (field.firstOrNull() in FORMULA_CHARS) "'$field" else field
            if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${safe.replace("\"", "\"\"")}\"" else safe
        }
        append("\r\n")
    }

    /** Merge key for duplicates: the same entry imported twice. */
    fun CallNote.identity() = listOf(normalizedNumber, timestamp, entryType, note, contactLookupKey)

    private val KNOWN_FILES = setOf("manifest.json", "notes.json", "tags.json", "settings.json")
    private val FORMULA_CHARS = setOf('=', '+', '-', '@')
}
