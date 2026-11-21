package app.steptwelve

import app.steptwelve.data.CallRecord
import app.steptwelve.data.CallType

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A call started from inside StepTwelve, waiting to show up in the call log. */
data class PendingCall(val lookupKey: String, val numberKeys: Set<String>, val startedAt: Long) {
    /** The call log row for this call: outgoing, to one of the contact's numbers, after dialing. */
    fun matches(call: CallRecord): Boolean =
        call.type == CallType.OUTGOING && call.numberKey in numberKeys && call.date >= startedAt - 10_000
}

/** Remembers the last call dialed from the app so the post-call prompt can offer a note. */
class PostCallTracker {
    private val _pending = MutableStateFlow<PendingCall?>(null)
    val pending: StateFlow<PendingCall?> = _pending

    fun dialed(call: PendingCall) {
        _pending.value = call
    }

    fun clear() {
        _pending.value = null
    }
}
