package app.steptwelve.ui.tags

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.steptwelve.data.Tag
import app.steptwelve.data.TagRepository

private sealed interface TagDialog {
    data object Add : TagDialog
    data class Rename(val tag: Tag) : TagDialog
    data class Delete(val tag: Tag) : TagDialog
    data object Reset : TagDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    viewModel: TagsViewModel = viewModel(factory = TagsViewModel.Factory),
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<TagDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { dialog = TagDialog.Reset }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset to default tags")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = TagDialog.Add },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add tag") },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                Text(
                    "Contacts whose name contains a whole word matching an enabled tag appear on " +
                        "the home screen. \"aa\" matches \"John (AA)\" but not \"Aaron\".",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(tags, key = { it.id }) { tag ->
                ListItem(
                    headlineContent = { Text(tag.label) },
                    leadingContent = {
                        Switch(
                            checked = tag.enabled,
                            onCheckedChange = { viewModel.setEnabled(tag, it) },
                        )
                    },
                    trailingContent = {
                        Column {
                            IconButton(onClick = { dialog = TagDialog.Rename(tag) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Rename ${tag.label}")
                            }
                            IconButton(onClick = { dialog = TagDialog.Delete(tag) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${tag.label}")
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        TagDialog.Add -> TagLabelDialog(
            title = "Add tag",
            initial = "",
            onDismiss = { dialog = null },
            onSave = { label, onResult -> viewModel.add(label, onResult) },
        )
        is TagDialog.Rename -> TagLabelDialog(
            title = "Rename tag",
            initial = d.tag.label,
            onDismiss = { dialog = null },
            onSave = { label, onResult -> viewModel.rename(d.tag, label, onResult) },
        )
        is TagDialog.Delete -> ConfirmDialog(
            title = "Delete \"${d.tag.label}\"?",
            text = "Contacts are not changed. They just stop appearing for this tag.",
            confirm = "Delete",
            onDismiss = { dialog = null },
            onConfirm = { viewModel.delete(d.tag); dialog = null },
        )
        TagDialog.Reset -> ConfirmDialog(
            title = "Reset tags?",
            text = "Replaces your tags with the defaults: ${TagRepository.DEFAULT_TAGS.joinToString(", ")}.",
            confirm = "Reset",
            onDismiss = { dialog = null },
            onConfirm = { viewModel.resetToDefaults(); dialog = null },
        )
    }
}

@Composable
private fun TagLabelDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (label: String, onResult: (Boolean) -> Unit) -> Unit,
) {
    var label by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it; error = false },
                singleLine = true,
                isError = error,
                supportingText = {
                    Text(if (error) "Tag is empty or already exists." else "Spaces and dashes are ignored.")
                },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(label) { ok -> if (ok) onDismiss() else error = true }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
