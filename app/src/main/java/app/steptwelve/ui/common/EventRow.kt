package app.steptwelve.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import app.steptwelve.domain.OutreachEvent

/** One call or entry, with its note inline. [title] is the contact name in the timelog. */
@Composable
fun EventRow(
    event: OutreachEvent,
    onClick: () -> Unit,
    title: String? = null,
    showDay: Boolean = true,
) {
    val note = event.note
    val description = when (event) {
        is OutreachEvent.Call -> event.call.iconDescription()
        is OutreachEvent.NoteOnly -> Format.entryType(event.note.entryType)
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(event.icon(), contentDescription = description) },
        overlineContent = { Text(if (showDay) Format.dateTime(event.time) else Format.time(event.time)) },
        headlineContent = { Text(title ?: Format.eventSummary(event)) },
        supportingContent = {
            Column {
                if (title != null) Text(Format.eventSummary(event))
                val details = listOfNotNull(
                    note?.outcome?.let(Format::outcome),
                    note?.followUpAt?.takeIf { it > System.currentTimeMillis() }?.let { "Follow up ${Format.dayOf(it)}" },
                )
                if (details.isNotEmpty()) Text(details.joinToString(" · "), color = MaterialTheme.colorScheme.primary)
                when {
                    !note?.note.isNullOrBlank() -> Text(note!!.note)
                    note == null && event is OutreachEvent.Call -> Text(
                        "Add a note",
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
