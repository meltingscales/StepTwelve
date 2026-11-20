package app.steptwelve.data

import app.steptwelve.reminders.ReminderScheduler

/** Saves notes and keeps their follow-up reminders in sync. */
class NoteRepository(
    private val dao: NoteDao,
    private val reminders: ReminderScheduler,
) {
    suspend fun save(note: CallNote): Long {
        val now = System.currentTimeMillis()
        val stamped = if (note.id == 0L) note.copy(createdAt = now, updatedAt = now) else note.copy(updatedAt = now)
        val id = dao.upsert(stamped).let { if (it == -1L || note.id != 0L) note.id else it }
        reminders.syncFollowUp(id, stamped.followUpAt)
        return id
    }

    suspend fun delete(note: CallNote) {
        dao.delete(note.id)
        reminders.syncFollowUp(note.id, null)
    }
}
