package app.steptwelve.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.steptwelve.domain.OutreachEvent
import app.steptwelve.domain.SinceLabel
import app.steptwelve.ui.MainViewModel
import app.steptwelve.ui.common.EventRow
import app.steptwelve.ui.common.NoteTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    lookupKey: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val contact = snapshot.contact(lookupKey)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact?.contact?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        if (contact == null) {
            Text(
                if (snapshot.loading) "" else "This contact no longer matches your tags.",
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${contact.tags.joinToString(" · ")} · last in touch " +
                            SinceLabel.format(contact.lastContactAt, System.currentTimeMillis()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val hasNumber = contact.contact.primaryNumber != null
                        Button(onClick = { viewModel.dial(context, contact) }, enabled = hasNumber) {
                            Icon(Icons.Filled.Call, contentDescription = null)
                            Text("  Call")
                        }
                        FilledTonalButton(onClick = { viewModel.text(context, contact) }, enabled = hasNumber) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                            Text("  Text")
                        }
                        FilledTonalButton(onClick = { viewModel.openNote(NoteTarget(contact)) }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("  Entry")
                        }
                    }
                }
                HorizontalDivider()
            }
            if (contact.events.isEmpty()) {
                item {
                    Text(
                        if (snapshot.permissions.callLog) "No calls or entries yet." else
                            "No entries yet. Allow call log access on the home screen to see calls here.",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(contact.events, key = { it.key() }) { event ->
                EventRow(
                    event = event,
                    onClick = {
                        viewModel.openNote(
                            when (event) {
                                is OutreachEvent.Call -> NoteTarget(contact, call = event.call, existing = event.note)
                                is OutreachEvent.NoteOnly -> NoteTarget(contact, existing = event.note)
                            }
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

fun OutreachEvent.key(): String = when (this) {
    is OutreachEvent.Call -> "call-${call.id}-${call.date}"
    is OutreachEvent.NoteOnly -> "note-${note.id}"
}
