package app.steptwelve.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.steptwelve.data.OutreachSnapshot
import app.steptwelve.data.Settings
import app.steptwelve.data.SortOrder
import app.steptwelve.domain.Goals
import app.steptwelve.domain.Outreach
import app.steptwelve.domain.OutreachContact
import app.steptwelve.domain.SinceLabel
import app.steptwelve.ui.MainViewModel
import app.steptwelve.ui.common.icon
import app.steptwelve.ui.common.iconDescription
import app.steptwelve.ui.refreshPermissions
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenContact: (String) -> Unit,
    onOpenTags: () -> Unit,
) {
    val context = LocalContext.current
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissions(context)
    }
    var sortMenu by remember { mutableStateOf(false) }
    val sortOrder = settings?.sortOrder ?: SortOrder.NEGLECTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StepTwelve") },
                actions = {
                    if (snapshot.permissions.contacts == true) {
                        Box {
                            IconButton(onClick = { sortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort contacts")
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                listOf(
                                    SortOrder.NEGLECTED to "Longest since contact",
                                    SortOrder.NAME to "Name",
                                    SortOrder.RECENT to "Most recent",
                                ).forEach { (order, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        leadingIcon = { RadioButton(selected = order == sortOrder, onClick = null) },
                                        onClick = {
                                            sortMenu = false
                                            viewModel.updateSettings(context) { it.copy(sortOrder = order) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (snapshot.permissions.contacts) {
                null -> Unit
                false -> ContactsPermissionRationale(onAllow = { launcher.launch(Manifest.permission.READ_CONTACTS) })
                true -> ContactList(
                    snapshot = snapshot,
                    settings = settings ?: Settings(),
                    onOpenContact = onOpenContact,
                    onOpenTags = onOpenTags,
                    onAllowCallLog = { launcher.launch(Manifest.permission.READ_CALL_LOG) },
                )
            }
        }
    }
}

@Composable
private fun ContactsPermissionRationale(onAllow: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Find your program contacts", style = MaterialTheme.typography.headlineSmall)
        Text(
            "StepTwelve reads your contacts to show the people whose names include one of your " +
                "tags, like \"aa\" or \"sponsor\". It never changes your contacts.",
            textAlign = TextAlign.Center,
        )
        Text(
            "Everything stays on this phone. StepTwelve has no internet access, no ads and no tracking.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAllow) { Text("Allow contacts access") }
    }
}

@Composable
private fun ContactList(
    snapshot: OutreachSnapshot,
    settings: Settings,
    onOpenContact: (String) -> Unit,
    onOpenTags: () -> Unit,
    onAllowCallLog: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    val activeSelection = selected.filter { it in snapshot.enabledTags }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val now = System.currentTimeMillis()

    val visible = remember(snapshot, activeSelection, query, settings.sortOrder) {
        Outreach.sort(
            snapshot.contacts.filter { c ->
                (activeSelection.isEmpty() || c.tags.any { it in activeSelection }) &&
                    (query.isBlank() || c.contact.displayName.contains(query.trim(), ignoreCase = true))
            },
            settings.sortOrder,
        )
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            GoalHeader(snapshot.contacts, settings.dailyGoal, today, zone)
        }
        if (!snapshot.permissions.callLog) {
            item { CallLogPrompt(onAllow = onAllowCallLog) }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search names") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear search") }
                    }
                },
                singleLine = true,
            )
        }
        if (snapshot.enabledTags.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(snapshot.enabledTags) { tag ->
                        FilterChip(
                            selected = tag in activeSelection,
                            onClick = { selected = if (tag in activeSelection) activeSelection - tag else activeSelection + tag },
                            label = { Text(tag) },
                        )
                    }
                }
            }
        }
        when {
            snapshot.loading -> item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            visible.isEmpty() -> item {
                EmptyState(
                    hasTags = snapshot.enabledTags.isNotEmpty(),
                    filtered = query.isNotBlank() || activeSelection.isNotEmpty(),
                    onOpenTags = onOpenTags,
                )
            }
            else -> items(visible, key = { it.contact.lookupKey }) { contact ->
                ContactRow(
                    contact = contact,
                    showCallStatus = snapshot.permissions.callLog,
                    reachedOutToday = contact.reachedOutOn(today, zone),
                    now = now,
                    onClick = { onOpenContact(contact.contact.lookupKey) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: OutreachContact,
    showCallStatus: Boolean,
    reachedOutToday: Boolean,
    now: Long,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(contact.contact.displayName) },
        supportingContent = { Text(contact.tags.joinToString(" · ")) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                val since = SinceLabel.format(contact.lastContactAt, now)
                Text(
                    text = since,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = if (contact.lastContactAt == null) "Never in touch" else "Last in touch $since"
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (reachedOutToday) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Reached out today",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (showCallStatus) {
                        contact.lastCall?.let { call ->
                            Icon(
                                call.icon(),
                                contentDescription = "Last call: ${call.iconDescription()}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}

/** "2 / 3 today", a gentle week strip, and the weekly summary. No streaks, no guilt. */
@Composable
private fun GoalHeader(contacts: List<OutreachContact>, goal: Int, today: LocalDate, zone: ZoneId) {
    val todayCount = Goals.reachOutsOn(contacts, today, zone)
    val week = Goals.lastSevenDays(contacts, today, zone)
    val summary = Goals.weekly(contacts, today, zone)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "$todayCount / $goal today",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { contentDescription = "$todayCount of $goal reach-outs today" },
                    )
                    Text(
                        if (todayCount >= goal) "Goal reached. Nice work." else "Every reach-out counts, answered or not.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                WeekStrip(week, goal)
            }
            Text(
                "This week: ${plural(summary.callsMade, "call", "calls")} · " +
                    "${plural(summary.peopleReached, "person", "people")} reached · " +
                    plural(summary.notesWritten, "note", "notes"),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WeekStrip(days: List<Pair<LocalDate, Int>>, goal: Int) {
    val description = days.joinToString { (date, count) -> "${date.dayOfWeek.name.lowercase().take(3)} $count" }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "Last 7 days: $description" },
    ) {
        days.forEach { (date, count) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val fraction = (count.toFloat() / goal).coerceIn(0f, 1f)
                Box(Modifier.width(10.dp).height(28.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.width(10.dp).height((4 + 24 * fraction).dp)
                            .background(
                                if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(3.dp),
                            )
                    )
                }
                Text(date.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CallLogPrompt(onAllow: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "See when you last called each person. Read-only, stays on this phone.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onAllow) { Text("Allow") }
        }
    }
}

@Composable
private fun EmptyState(hasTags: Boolean, filtered: Boolean, onOpenTags: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val message = when {
            !hasTags -> "No tags are turned on. Add or enable a tag to see your program contacts."
            filtered -> "No contacts match this search or filter."
            else -> "No contacts match your tags yet. Add a tag like \"AA\" to a contact's name, or edit your tags."
        }
        Text(message, textAlign = TextAlign.Center)
        if (!filtered) Button(onClick = onOpenTags) { Text("Edit tags") }
    }
}

private fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"
