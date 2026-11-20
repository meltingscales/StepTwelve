package app.steptwelve.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class EntryType { PHONE_CALL, OTHER_APP_CALL, TEXT, IN_PERSON }

enum class Outcome { ANSWERED, VOICEMAIL, NO_ANSWER, TEXTED }

/**
 * A note on a call-log entry, or a manual outreach entry (callLogId == null and
 * entryType != PHONE_CALL). Anchored by call ID plus timestamp and number, since
 * call-log IDs can change after a restore.
 */
@Serializable
@Entity(indices = [Index("contactLookupKey"), Index("timestamp")])
data class CallNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callLogId: Long?,
    val contactLookupKey: String,
    /** Contact display name when the note was written, so exports stay readable. */
    val contactName: String,
    val normalizedNumber: String,
    val timestamp: Long,
    val entryType: EntryType,
    val outcome: Outcome?,
    val note: String,
    val followUpAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isManual: Boolean get() = entryType != EntryType.PHONE_CALL

    /** True if this note belongs to [call]: same ID at the same time, or same number and time. */
    fun matches(call: CallRecord): Boolean =
        (callLogId == call.id && kotlin.math.abs(timestamp - call.date) < 60_000) ||
            (normalizedNumber == call.numberKey && timestamp == call.date)
}
