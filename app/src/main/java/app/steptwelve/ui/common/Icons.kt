package app.steptwelve.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.ui.graphics.vector.ImageVector
import app.steptwelve.data.CallRecord
import app.steptwelve.data.CallType
import app.steptwelve.data.EntryType
import app.steptwelve.domain.OutreachEvent

/** Direction and outcome: made/received/missed, with "not connected" when duration was 0. */
fun CallRecord.icon(): ImageVector = when (type) {
    CallType.OUTGOING -> if (connected) Icons.AutoMirrored.Filled.CallMade else Icons.Filled.PhoneDisabled
    CallType.INCOMING -> if (connected) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMissed
    CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed
    CallType.OTHER -> Icons.Filled.Call
}

fun CallRecord.iconDescription(): String = when (type) {
    CallType.OUTGOING -> if (connected) "Outgoing call, connected" else "Outgoing call, no answer"
    CallType.INCOMING -> if (connected) "Incoming call, connected" else "Incoming call, not connected"
    CallType.MISSED -> "Missed call"
    CallType.OTHER -> "Call"
}

fun EntryType.icon(): ImageVector = when (this) {
    EntryType.PHONE_CALL -> Icons.Filled.Call
    EntryType.OTHER_APP_CALL -> Icons.Filled.PhoneInTalk
    EntryType.TEXT -> Icons.AutoMirrored.Filled.Message
    EntryType.IN_PERSON -> Icons.Filled.Groups
}

fun OutreachEvent.icon(): ImageVector = when (this) {
    is OutreachEvent.Call -> call.icon()
    is OutreachEvent.NoteOnly -> note.entryType.icon()
}
