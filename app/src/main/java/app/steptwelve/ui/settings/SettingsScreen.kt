package app.steptwelve.ui.settings

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.steptwelve.StepTwelveApp
import app.steptwelve.backup.ExportBundle
import app.steptwelve.backup.ImportException
import app.steptwelve.backup.PasswordRequiredException
import app.steptwelve.data.Settings
import app.steptwelve.ui.MainViewModel
import app.steptwelve.ui.hasPermission
import app.steptwelve.ui.lock.authenticate
import app.steptwelve.ui.lock.canUseAppLock
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onOpenTags: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as StepTwelveApp).container
    val backup = container.backupManager
    val settings = viewModel.settings.collectAsStateWithLifecycle().value ?: Settings()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun update(transform: (Settings) -> Settings) = viewModel.updateSettings(context, transform)

    var showTimePicker by remember { mutableStateOf(false) }
    var showLockAfter by remember { mutableStateOf(false) }
    var showExportWarning by remember { mutableStateOf(false) }
    var importPreview by remember { mutableStateOf<ExportBundle?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf<CharArray?>(null) }
    var passwordImportUri by remember { mutableStateOf<Uri?>(null) }
    var importPasswordWrong by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) update { it.copy(reminderEnabled = true) }
        else scope.launch { snackbar.showSnackbar("Reminders need notification permission.") }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) scope.launch {
            val password = exportPassword
            exportPassword = null
            val message = runCatching { backup.export(uri, password) }
                .fold({ if (password != null) "Password-protected export saved." else "Export saved." }, { "Export failed: ${it.message}" })
            password?.fill(' ')
            snackbar.showSnackbar(message)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            try {
                importPreview = backup.preview(uri)
            } catch (e: PasswordRequiredException) {
                importPasswordWrong = false
                passwordImportUri = uri
            } catch (e: ImportException) {
                snackbar.showSnackbar(e.message ?: "Couldn't read that file.")
            } catch (e: Exception) {
                snackbar.showSnackbar("Couldn't read that file.")
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Section("Outreach")
            ListItem(
                headlineContent = { Text("Daily goal") },
                supportingContent = { Text("Outgoing calls and manual entries count, answered or not.") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { update { it.copy(dailyGoal = it.dailyGoal - 1) } }, enabled = settings.dailyGoal > 1) {
                            Icon(Icons.Filled.Remove, contentDescription = "Lower goal")
                        }
                        Text("${settings.dailyGoal}", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { update { it.copy(dailyGoal = it.dailyGoal + 1) } }) {
                            Icon(Icons.Filled.Add, contentDescription = "Raise goal")
                        }
                    }
                },
            )
            SwitchItem(
                "Ask how the call went",
                "After you call someone from StepTwelve, offer to add a note.",
                settings.postCallPrompt,
            ) { on -> update { it.copy(postCallPrompt = on) } }
            NavItem("Tags", "Choose which contacts appear.", onOpenTags)
            SwitchItem(
                "Match notes and company too",
                "Also find tags in each contact's note, company or job title, not just the name.",
                settings.matchExtraFields,
            ) { on -> update { it.copy(matchExtraFields = on) } }

            Section("Reminders")
            SwitchItem(
                "Daily reminder",
                "A gentle nudge. The notification just says \"reminder\", nothing more.",
                settings.reminderEnabled,
            ) { on ->
                if (on && Build.VERSION.SDK_INT >= 33 && !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    update { it.copy(reminderEnabled = on) }
                }
            }
            if (settings.reminderEnabled) {
                NavItem("Reminder time", formatMinuteOfDay(settings.reminderMinuteOfDay)) { showTimePicker = true }
            }

            Section("Privacy")
            SwitchItem(
                "App lock",
                "Require fingerprint, face or your screen lock to open StepTwelve.",
                settings.appLock,
            ) { on ->
                if (!on) {
                    authenticate(context, "Turn off app lock", onSuccess = { update { it.copy(appLock = false) } })
                } else if (!canUseAppLock(context)) {
                    scope.launch { snackbar.showSnackbar("Set up a screen lock on your phone first.") }
                } else {
                    authenticate(context, "Turn on app lock", onSuccess = { update { it.copy(appLock = true) } })
                }
            }
            if (settings.appLock) {
                NavItem("Lock after", lockAfterLabel(settings.lockAfterMinutes)) { showLockAfter = true }
            }
            SwitchItem(
                "Screen privacy",
                "Block screenshots and hide StepTwelve in the recent apps view.",
                settings.screenPrivacy,
            ) { on -> update { it.copy(screenPrivacy = on) } }
            SwitchItem(
                "Discreet icon",
                "Show the app as \"Circle Back\" with a neutral icon. Takes effect when you leave the app.",
                settings.discreetIcon,
            ) { on -> update { it.copy(discreetIcon = on) } }

            Section("Your data")
            NavItem("Export to ZIP", "Notes, manual entries, tags and settings. Saved wherever you choose.") {
                showExportWarning = true
            }
            NavItem("Import from ZIP", "Restore or move from another phone.") {
                importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            }

            Section("About")
            NavItem("About StepTwelve", "Privacy, version and licenses") { showAbout = true }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(settings.reminderMinuteOfDay / 60, settings.reminderMinuteOfDay % 60)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    update { it.copy(reminderMinuteOfDay = state.hour * 60 + state.minute) }
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }

    if (showLockAfter) {
        AlertDialog(
            onDismissRequest = { showLockAfter = false },
            title = { Text("Lock after") },
            text = {
                Column {
                    listOf(0, 1, 5, 15).forEach { minutes ->
                        Row(
                            Modifier.clickable { update { it.copy(lockAfterMinutes = minutes) }; showLockAfter = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.lockAfterMinutes == minutes, onClick = null)
                            Text(lockAfterLabel(minutes), Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLockAfter = false }) { Text("Close") } },
        )
    }

    if (showExportWarning) {
        var protect by remember { mutableStateOf(false) }
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val passwordOk = !protect || (password.length >= 6 && password == confirm)
        AlertDialog(
            onDismissRequest = { showExportWarning = false },
            title = { Text("Export your data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This file contains your outreach notes. Once it leaves the app it is not protected by " +
                            "StepTwelve. Store it somewhere safe."
                    )
                    Row(
                        Modifier.fillMaxWidth().clickable { protect = !protect },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Protect with a password", Modifier.weight(1f))
                        Switch(checked = protect, onCheckedChange = null)
                    }
                    if (protect) {
                        PasswordField(password, { password = it }, "Password")
                        PasswordField(confirm, { confirm = it }, "Confirm password")
                        Text(
                            when {
                                password.length < 6 -> "At least 6 characters. There's no way to recover a forgotten password."
                                password != confirm -> "Passwords don't match."
                                else -> "AES-256 encrypted. There's no way to recover a forgotten password."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = passwordOk, onClick = {
                    showExportWarning = false
                    exportPassword = if (protect) password.toCharArray() else null
                    exportLauncher.launch(backup.suggestedFileName())
                }) { Text("Choose location") }
            },
            dismissButton = { TextButton(onClick = { showExportWarning = false }) { Text("Cancel") } },
        )
    }

    passwordImportUri?.let { uri ->
        var password by remember(uri) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { passwordImportUri = null },
            title = { Text("Enter the export password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (importPasswordWrong) Text("That password didn't work.", color = MaterialTheme.colorScheme.error)
                    PasswordField(password, { password = it }, "Password")
                }
            },
            confirmButton = {
                TextButton(enabled = password.isNotEmpty(), onClick = {
                    val chars = password.toCharArray()
                    scope.launch {
                        try {
                            importPreview = backup.preview(uri, chars)
                            passwordImportUri = null
                        } catch (e: PasswordRequiredException) {
                            importPasswordWrong = true
                        } catch (e: Exception) {
                            passwordImportUri = null
                            snackbar.showSnackbar(e.message ?: "Couldn't read that file.")
                        } finally {
                            chars.fill(' ')
                        }
                    }
                }) { Text("Open") }
            },
            dismissButton = { TextButton(onClick = { passwordImportUri = null }) { Text("Cancel") } },
        )
    }

    importPreview?.let { bundle ->
        fun runImport(replace: Boolean) {
            importPreview = null
            confirmReplace = false
            scope.launch {
                val message = runCatching { backup.import(bundle, replace) }
                    .fold(
                        { added -> if (replace) "Restored $added notes and entries." else "Added $added new notes and entries." },
                        { "Import failed: ${it.message}" },
                    )
                snackbar.showSnackbar(message)
            }
        }
        if (!confirmReplace) {
            AlertDialog(
                onDismissRequest = { importPreview = null },
                title = { Text("Import this export?") },
                text = {
                    Text(
                        "${bundle.notes.size} notes and entries, ${bundle.tags.size} tags.\n\n" +
                            "Merge adds anything you don't already have. Replace swaps your notes, tags and " +
                            "settings for the ones in the file."
                    )
                },
                confirmButton = { TextButton(onClick = { runImport(replace = false) }) { Text("Merge") } },
                dismissButton = {
                    Row {
                        TextButton(onClick = { importPreview = null }) { Text("Cancel") }
                        TextButton(onClick = { confirmReplace = true }) { Text("Replace") }
                    }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = { confirmReplace = false },
                title = { Text("Replace everything?") },
                text = { Text("Your current notes, entries and tags will be deleted. This can't be undone.") },
                confirmButton = { TextButton(onClick = { runImport(replace = true) }) { Text("Replace") } },
                dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancel") } },
            )
        }
    }

    if (showAbout) {
        val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("StepTwelve $version") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A private, offline outreach helper for 12-step fellowships.")
                    Text(
                        "No internet permission, no ads, no analytics. Your notes stay in app-private storage " +
                            "on this phone and are never backed up to the cloud. Only you can export them."
                    )
                    Text("StepTwelve is released into the public domain under the Unlicense.")
                    Text(
                        "Built with AndroidX, Jetpack Compose, Kotlin and kotlinx.serialization " +
                            "(Apache License 2.0).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Section(title: String) {
    HorizontalDivider()
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
private fun SwitchItem(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun NavItem(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    )
}

private fun formatMinuteOfDay(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private fun lockAfterLabel(minutes: Int) = when (minutes) {
    0 -> "Immediately"
    1 -> "After 1 minute"
    else -> "After $minutes minutes"
}
