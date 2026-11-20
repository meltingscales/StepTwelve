package app.steptwelve.backup

import android.content.Context
import android.net.Uri
import app.steptwelve.AppContainer
import app.steptwelve.backup.ExportCodec.identity
import app.steptwelve.reminders.RescheduleReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Export/import through the Storage Access Framework. The app never picks a location itself. */
class BackupManager(private val context: Context, private val container: AppContainer) {

    fun suggestedFileName(): String = "steptwelve-export-${LocalDate.now()}.zip"

    suspend fun export(uri: Uri, password: CharArray? = null) = withContext(Dispatchers.IO) {
        val bundle = ExportBundle(
            manifest = ExportManifest(
                appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?",
                schemaVersion = ExportCodec.SCHEMA_VERSION,
                exportedAt = System.currentTimeMillis(),
            ),
            notes = container.database.noteDao().getAll(),
            tags = container.tagRepository.getAll(),
            settings = container.settingsRepository.current(),
        )
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Couldn't open the file for writing.")
        out.use { ExportCodec.write(bundle, it, password) }
    }

    /** Parses and validates without changing anything. Throws [ImportException]. */
    suspend fun preview(uri: Uri, password: CharArray? = null): ExportBundle = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw ImportException("Couldn't open the file.")
        input.use { ExportCodec.read(it, password) }
    }

    /** Returns how many notes and entries were added. */
    suspend fun import(bundle: ExportBundle, replace: Boolean): Int = withContext(Dispatchers.IO) {
        val noteDao = container.database.noteDao()
        val incoming = bundle.notes.map { it.copy(id = 0) }
        if (replace) {
            noteDao.replaceAll(incoming)
            container.tagRepository.replaceAll(bundle.tags.map { it.copy(id = 0) })
            container.settingsRepository.update { bundle.settings }
            RescheduleReceiver.rescheduleAll(container)
            incoming.size
        } else {
            val existing = noteDao.getAll().map { it.identity() }.toSet()
            val added = incoming.filter { it.identity() !in existing }
            noteDao.insertAll(added)
            container.tagRepository.mergeAll(bundle.tags.map { it.copy(id = 0) })
            RescheduleReceiver.rescheduleAll(container)
            added.size
        }
    }
}
