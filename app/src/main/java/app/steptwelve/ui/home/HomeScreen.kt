package app.steptwelve.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.steptwelve.domain.SinceLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTags: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun refreshPermissions() = viewModel.onPermissionsChanged(
        contacts = granted(Manifest.permission.READ_CONTACTS),
        callLog = granted(Manifest.permission.READ_CALL_LOG),
    )

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermissions()
    }
    // Re-check on every resume: the user may grant or revoke access in system settings.
    LifecycleResumeEffect(Unit) {
        refreshPermissions()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StepTwelve") },
                actions = {
                    IconButton(onClick = onOpenTags) {
                        Icon(Icons.Filled.Settings, contentDescription = "Edit tags")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.hasContactsPermission) {
                null -> Unit
                false -> ContactsPermissionRationale(
                    onAllow = { launcher.launch(Manifest.permission.READ_CONTACTS) },
                )
                true -> ContactList(
                    state = state,
                    onToggleTag = viewModel::toggleTag,
                    onQueryChange = viewModel::setQuery,
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
                "tags, like \"aa\" or \"newcomer\". It never changes your contacts.",
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
    state: HomeUiState,
    onToggleTag: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenTags: () -> Unit,
    onAllowCallLog: () -> Unit,
) {
    // Read once per composition; labels are by calendar day so this is plenty fresh.
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize()) {
        if (!state.hasCallLogPermission) {
            CallLogPrompt(onAllow = onAllowCallLog)
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search names") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
        )
        if (state.enabledTags.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.enabledTags) { tag ->
                    FilterChip(
                        selected = tag in state.selectedTags,
                        onClick = { onToggleTag(tag) },
                        label = { Text(tag) },
                    )
                }
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.contacts.isEmpty() -> EmptyState(
                hasTags = state.enabledTags.isNotEmpty(),
                filtered = state.query.isNotBlank() || state.selectedTags.isNotEmpty(),
                onOpenTags = onOpenTags,
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.contacts, key = { it.contact.id }) { tagged ->
                    ListItem(
                        headlineContent = { Text(tagged.contact.displayName) },
                        supportingContent = { Text(tagged.tags.joinToString(" · ")) },
                        trailingContent = if (state.hasCallLogPermission) {
                            { LastCalledLabel(tagged.lastCalledAt, now) }
                        } else {
                            null
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LastCalledLabel(lastCalledAt: Long?, now: Long) {
    val label = SinceLabel.format(lastCalledAt, now)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics {
            contentDescription = if (lastCalledAt == null) "Never called" else "Last called $label"
        },
    )
}

@Composable
private fun CallLogPrompt(onAllow: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val message = when {
            !hasTags -> "No tags are turned on. Add or enable a tag to see your program contacts."
            filtered -> "No contacts match this search or filter."
            else -> "No contacts match your tags yet. Add a tag like \"AA\" to a contact's name, " +
                "or edit your tags."
        }
        Text(message, textAlign = TextAlign.Center)
        if (!filtered) {
            Row { Button(onClick = onOpenTags) { Text("Edit tags") } }
        }
    }
}
