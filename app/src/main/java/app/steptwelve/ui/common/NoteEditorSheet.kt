package app.steptwelve.ui.common

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.steptwelve.data.CallNote
import app.steptwelve.data.CallRecord
import app.steptwelve.data.EntryType
import app.steptwelve.data.Outcome
import app.steptwelve.domain.OutreachContact
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** What the note editor is editing: a note on a call, a manual entry, or an existing note. */
data class NoteTarget(
    val contact: OutreachContact,
    val call: CallRecord? = null,
    val existing: CallNote? = null,
    /** Title override, e.g. "How did the call go?" for the post-call prompt. */
    val title: String? = null,
    /** The post-call prompt: closes only via Skip or Save, so a stray tap can't dismiss it. */
    val postCall: Boolean = false,
) {
    val isManual: Boolean get() = existing?.isManual ?: (call == null)
}

private val followUpChoices = listOf(1 to "1 day", 3 to "3 days", 7 to "1 week", 14 to "2 weeks")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorSheet(
    target: NoteTarget,
    onDismiss: () -> Unit,
    onSave: (CallNote) -> Unit,
    onDelete: (CallNote) -> Unit,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val existing = target.existing
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !target.postCall },
    )

    var entryType by remember {
        mutableStateOf(existing?.entryType ?: if (target.call != null) EntryType.PHONE_CALL else EntryType.IN_PERSON)
    }
    var timestamp by remember {
        mutableLongStateOf(existing?.timestamp ?: target.call?.date ?: System.currentTimeMillis())
    }
    var outcome by remember { mutableStateOf(existing?.outcome) }
    var text by remember { mutableStateOf(existing?.note.orEmpty()) }
    var followUpAt by remember { mutableStateOf(existing?.followUpAt?.takeIf { it > System.currentTimeMillis() }) }
    var followUpDays by remember { mutableStateOf<Int?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun pickFollowUp(days: Int?) {
        followUpDays = days
        followUpAt = days?.let {
            LocalDate.now(zone).plusDays(it.toLong()).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        }
        if (days != null && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                target.title ?: when {
                    existing != null -> "Edit note"
                    target.isManual -> "Add manual entry"
                    else -> "Add note"
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                buildString {
                    append(target.contact.contact.displayName)
                    target.call?.let { append(" · ").append(Format.eventSummary(app.steptwelve.domain.OutreachEvent.Call(it, null))) }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (target.isManual) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(EntryType.OTHER_APP_CALL, EntryType.TEXT, EntryType.IN_PERSON).forEach { type ->
                        FilterChip(
                            selected = entryType == type,
                            onClick = { entryType = type },
                            label = { Text(Format.entryType(type)) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDatePicker = true }) { Text(Format.dayOf(timestamp)) }
                    OutlinedButton(onClick = { showTimePicker = true }) { Text(Format.time(timestamp)) }
                }
            } else {
                Text(Format.dateTime(timestamp), style = MaterialTheme.typography.bodyMedium)
            }

            Text("How did it go?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Outcome.entries.forEach { o ->
                    FilterChip(
                        selected = outcome == o,
                        onClick = { outcome = if (outcome == o) null else o },
                        label = { Text(Format.outcome(o)) },
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note") },
                minLines = 3,
            )

            Text("Remind me to call again", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = followUpAt == null, onClick = { pickFollowUp(null) }, label = { Text("No reminder") })
                if (followUpAt != null && followUpDays == null) {
                    FilterChip(selected = true, onClick = {}, label = { Text(Format.dayOf(followUpAt!!)) })
                }
                followUpChoices.forEach { (days, label) ->
                    FilterChip(selected = followUpDays == days, onClick = { pickFollowUp(days) }, label = { Text(label) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                if (existing != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = onDismiss) { Text(if (target.postCall) "Skip" else "Cancel") }
                Button(onClick = {
                    val contact = target.contact.contact
                    val base = existing ?: CallNote(
                        callLogId = target.call?.id,
                        contactLookupKey = contact.lookupKey,
                        contactName = contact.displayName,
                        normalizedNumber = target.call?.numberKey ?: contact.numberKeys.firstOrNull().orEmpty(),
                        timestamp = timestamp,
                        entryType = entryType,
                        outcome = null,
                        note = "",
                        followUpAt = null,
                        createdAt = 0,
                        updatedAt = 0,
                    )
                    onSave(
                        base.copy(
                            entryType = entryType,
                            timestamp = timestamp,
                            outcome = outcome,
                            note = text.trim(),
                            followUpAt = followUpAt,
                        )
                    )
                }) { Text("Save") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val initial = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utcMillis ->
                        val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        val time = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()
                        timestamp = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val current = Instant.ofEpochMilli(timestamp).atZone(zone)
        val state = rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    timestamp = current.toLocalDate().atTime(LocalTime.of(state.hour, state.minute))
                        .atZone(zone).toInstant().toEpochMilli()
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (existing.isManual) "Delete this entry?" else "Delete this note?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { onDelete(existing) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
