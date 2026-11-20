package app.steptwelve.data

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.CallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Reads the system call log live and read-only. Nothing is copied into the app's database. */
class CallLogRepository(
    private val resolver: ContentResolver,
    private val keys: PhoneNumberKeys,
) {

    /**
     * Emits number key -> time (epoch ms) of the most recent outgoing call to that number,
     * connected or not, since reaching out is what counts. Re-emits when the call log changes.
     */
    fun observeLastOutgoing(): Flow<Map<String, Long>> =
        callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            resolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
            trySend(Unit)
            awaitClose { resolver.unregisterContentObserver(observer) }
        }
            .conflate()
            .map { queryLastOutgoing() }
            .flowOn(Dispatchers.IO)

    private fun queryLastOutgoing(): Map<String, Long> {
        val result = HashMap<String, Long>()
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NORMALIZED_NUMBER, CallLog.Calls.DATE),
            "${CallLog.Calls.TYPE} = ?",
            arrayOf(CallLog.Calls.OUTGOING_TYPE.toString()),
            "${CallLog.Calls.DATE} DESC",
        )?.use { cursor ->
            val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val normalizedCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NORMALIZED_NUMBER)
            val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            while (cursor.moveToNext()) {
                val key = keys.key(cursor.getString(numberCol), cursor.getString(normalizedCol)) ?: continue
                // Rows are newest first, so the first time we see a key is its latest call.
                result.putIfAbsent(key, cursor.getLong(dateCol))
            }
        }
        return result
    }
}
