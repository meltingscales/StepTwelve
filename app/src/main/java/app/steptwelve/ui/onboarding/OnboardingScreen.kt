package app.steptwelve.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.steptwelve.ui.MainViewModel
import app.steptwelve.ui.refreshPermissions
import app.steptwelve.ui.tags.TagsViewModel

private const val STEPS = 6

/** First-run walkthrough: what the app does, the privacy promise, permissions and tags. */
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    val next = { step = (step + 1).coerceAtMost(STEPS - 1) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissions(context)
        next()
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(24.dp)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.size(24.dp))
                when (step) {
                    0 -> Page(Icons.Filled.WavingHand, "Welcome to StepTwelve",
                        "A calm helper for 12th-step outreach. See who you haven't talked to in a while, " +
                            "call or text them, and jot a note afterward.",
                        "Reaching out counts, whether or not anyone answers.")
                    1 -> Page(Icons.Filled.Lock, "Private by design",
                        "StepTwelve has no internet access, no ads and no tracking. Your notes stay in " +
                            "app-private storage on this phone and are never backed up to the cloud.",
                        "It only reads your contacts and call log. It never changes them, and nothing leaves " +
                            "the phone unless you export it yourself.")
                    2 -> Page(Icons.Filled.Contacts, "Find your program contacts",
                        "StepTwelve shows contacts whose names include a tag, like \"Sam AA\" or " +
                            "\"Jo (sponsor)\". To do that it needs to read your contacts.",
                        "Read-only. Your contacts are never changed or copied.")
                    3 -> TagsStep()
                    4 -> Page(Icons.Filled.History, "See when you last called",
                        "With call log access, StepTwelve shows how long it's been since you talked to " +
                            "each person and puts the most neglected at the top.",
                        "Optional and read-only. Without it, contacts, notes and manual entries still work.")
                    else -> Page(Icons.Filled.TaskAlt, "You're all set",
                        "Your daily goal starts at 3 reach-outs. You can change it, add reminders, or turn on " +
                            "the app lock and a discreet icon in Settings.",
                        "Be gentle with yourself. A missed day is just a missed day.")
                }
            }
            Dots(step)
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step in 1..4) TextButton(onClick = { step -= 1 }) { Text("Back") }
                Spacer(Modifier.weight(1f))
                when (step) {
                    2 -> {
                        TextButton(onClick = next) { Text("Not now") }
                        Button(onClick = { permission.launch(Manifest.permission.READ_CONTACTS) }) { Text("Allow") }
                    }
                    4 -> {
                        TextButton(onClick = next) { Text("Not now") }
                        Button(onClick = { permission.launch(Manifest.permission.READ_CALL_LOG) }) { Text("Allow") }
                    }
                    STEPS - 1 -> Button(onClick = onDone) { Text("Start") }
                    else -> Button(onClick = next) { Text(if (step == 0) "Get started" else "Next") }
                }
            }
        }
    }
}

@Composable
private fun Page(icon: ImageVector, title: String, body: String, footnote: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Text(body, style = MaterialTheme.typography.bodyLarge)
    Text(footnote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TagsStep(tagsViewModel: TagsViewModel = viewModel(factory = TagsViewModel.Factory)) {
    val tags by tagsViewModel.tags.collectAsStateWithLifecycle()
    var newTag by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    Icon(Icons.Filled.Sell, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
    Text("Choose your tags", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Add a tag to a contact's name in your contacts app, like \"Sam AA\". Whole words only: " +
            "\"aa\" matches \"John (AA)\" but not \"Aaron\".",
        style = MaterialTheme.typography.bodyLarge,
    )
    Column {
        tags.forEach { tag ->
            ListItem(
                headlineContent = { Text(tag.label) },
                trailingContent = { Switch(checked = tag.enabled, onCheckedChange = { tagsViewModel.setEnabled(tag, it) }) },
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newTag,
            onValueChange = { newTag = it; error = false },
            modifier = Modifier.weight(1f),
            label = { Text("Add a tag") },
            singleLine = true,
            isError = error,
            supportingText = if (error) ({ Text("Empty or already added.") }) else null,
        )
        TextButton(onClick = { tagsViewModel.add(newTag) { ok -> if (ok) newTag = "" else error = true } }) { Text("Add") }
    }
    Text("You can change these any time in Settings.", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Dots(step: Int) {
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "Step ${step + 1} of $STEPS" },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(STEPS) { i ->
            Box(
                Modifier.size(8.dp).background(
                    if (i == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape,
                )
            )
        }
    }
}
