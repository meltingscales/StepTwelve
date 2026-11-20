package app.steptwelve.ui.timelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.steptwelve.data.CallType
import app.steptwelve.data.Outcome
import app.steptwelve.domain.OutreachContact
import app.steptwelve.domain.OutreachEvent
import app.steptwelve.domain.toLocalDate
import app.steptwelve.ui.MainViewModel
import app.steptwelve.ui.common.EventRow
import app.steptwelve.ui.common.Format
import app.steptwelve.ui.common.NoteTarget
import app.steptwelve.ui.contact.key
import java.time.LocalDate
import java.time.ZoneId

private enum class Range(val label: String, val days: Long?) {
    WEEK("7 days", 7), MONTH("30 days", 30), QUARTER("90 days", 90), ALL("All", null)
}

private enum class OutcomeFilter(val label: String) {
    ANY("Any"), REACHED_OUT("Reached out"), ANSWERED("Answered"), VOICEMAIL("Voicemail"),
    NO_ANSWER("No answer"), TEXTED("Texted"), WITH_NOTES("With notes"), INCOMING("Incoming"),
}

private data class TimelogItem(val contact: OutreachContact, val event: OutreachEvent)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelogScreen(viewModel: MainViewModel) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableStateOf(Range.WEEK) }
    var outcome by rememberSaveable { mutableStateOf(OutcomeFilter.ANY) }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val zone = ZoneId.systemDefault()

    val grouped = remember(snapshot, range, outcome, tag, query) {
        val from = range.days?.let { LocalDate.now(zone).minusDays(it - 1) }
        snapshot.contacts
            .filter { c -> (tag == null || tag in c.tags) && (query.isBlank() || c.contact.displayName.contains(query.trim(), true)) }
            .flatMap { c -> c.events.map { TimelogItem(c, it) } }
            .filter { from == null || !it.event.time.toLocalDate(zone).isBefore(from) }
            .filter { matches(it.event, outcome) }
            .sortedByDescending { it.event.time }
            .groupBy { it.event.time.toLocalDate(zone) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Timelog") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Filter by contact") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear filter") }
                        }
                    },
                    singleLine = true,
                )
            }
            item { ChipRow(Range.entries, { it == range }, { range = it }, { it.label }) }
            item { ChipRow(OutcomeFilter.entries, { it == outcome }, { outcome = it }, { it.label }) }
            if (snapshot.enabledTags.isNotEmpty()) {
                item {
                    ChipRow(listOf<String?>(null) + snapshot.enabledTags, { it == tag }, { tag = it }, { it ?: "All tags" })
                }
            }
            if (grouped.isEmpty()) {
                item {
                    Text(
                        "Nothing here for these filters.",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            grouped.forEach { (day, items) ->
                item(key = "day-$day") {
                    Text(
                        "${Format.day(day)} · ${items.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
                    )
                }
                items(items, key = { "${it.contact.contact.lookupKey}-${it.event.key()}" }) { item ->
                    EventRow(
                        event = item.event,
                        title = item.contact.contact.displayName,
                        showDay = false,
                        onClick = {
                            viewModel.openNote(
                                when (val e = item.event) {
                                    is OutreachEvent.Call -> NoteTarget(item.contact, call = e.call, existing = e.note)
                                    is OutreachEvent.NoteOnly -> NoteTarget(item.contact, existing = e.note)
                                }
                            )
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun matches(event: OutreachEvent, filter: OutcomeFilter): Boolean {
    val note = event.note
    return when (filter) {
        OutcomeFilter.ANY -> true
        OutcomeFilter.REACHED_OUT -> when (event) {
            is OutreachEvent.Call -> event.call.type == CallType.OUTGOING
            is OutreachEvent.NoteOnly -> event.note.isManual
        }
        OutcomeFilter.ANSWERED -> note?.outcome == Outcome.ANSWERED ||
            (note?.outcome == null && event is OutreachEvent.Call && event.call.connected)
        OutcomeFilter.VOICEMAIL -> note?.outcome == Outcome.VOICEMAIL
        OutcomeFilter.NO_ANSWER -> note?.outcome == Outcome.NO_ANSWER ||
            (note?.outcome == null && event is OutreachEvent.Call && event.call.type == CallType.OUTGOING && !event.call.connected)
        OutcomeFilter.TEXTED -> note?.outcome == Outcome.TEXTED
        OutcomeFilter.WITH_NOTES -> !note?.note.isNullOrBlank()
        OutcomeFilter.INCOMING -> event is OutreachEvent.Call && event.call.type != CallType.OUTGOING
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, isSelected: (T) -> Boolean, onSelect: (T) -> Unit, label: (T) -> String) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            FilterChip(selected = isSelected(option), onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}
