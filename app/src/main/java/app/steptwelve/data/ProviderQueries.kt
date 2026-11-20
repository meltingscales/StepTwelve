package app.steptwelve.data

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** Runs [query] now and again whenever content under [uri] changes, on the IO dispatcher. */
fun <T> ContentResolver.observeQuery(uri: Uri, query: () -> T): Flow<T> =
    callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        registerContentObserver(uri, true, observer)
        trySend(Unit)
        awaitClose { unregisterContentObserver(observer) }
    }
        .conflate()
        .map { query() }
        .flowOn(Dispatchers.IO)
