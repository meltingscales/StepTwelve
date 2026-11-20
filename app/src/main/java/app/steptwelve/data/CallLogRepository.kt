package app.steptwelve.data

import android.content.ContentResolver
import android.provider.CallLog
import kotlinx.coroutines.flow.Flow

enum class CallType { INCOMING, OUTGOING, MISSED, OTHER }

data class CallRecord(
    /** CallLog.Calls._ID. Can change after a call-log restore, so notes also keep number + time. */
    val id: Long,
    val numberKey: String,
    val date: Long,
    val durationSeconds: Long,
    val type: CallType,
) {
    /** Duration 0 means the call never connected. */
    val connected: Boolean get() = durationSeconds > 0
}

/** Reads the system call log live and read-only. Nothing is copied into the app's database. */
class CallLogRepository(
    private val resolver: ContentResolver,
    private val keys: PhoneNumberKeys,
) {

    /** All calls, newest first. Re-emits whenever the call log changes. */
    fun observeCalls(): Flow<List<CallRecord>> =
        resolver.observeQuery(CallLog.Calls.CONTENT_URI) { queryCalls() }

    private fun queryCalls(): List<CallRecord> {
        val calls = mutableListOf<CallRecord>()
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NORMALIZED_NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE,
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val normalizedCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NORMALIZED_NUMBER)
            val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            while (cursor.moveToNext()) {
                val key = keys.key(cursor.getString(numberCol), cursor.getString(normalizedCol)) ?: continue
                calls += CallRecord(
                    id = cursor.getLong(idCol),
                    numberKey = key,
                    date = cursor.getLong(dateCol),
                    durationSeconds = cursor.getLong(durationCol),
                    type = when (cursor.getInt(typeCol)) {
                        CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> CallType.MISSED
                        else -> CallType.OTHER
                    },
                )
            }
        }
        return calls
    }
}
