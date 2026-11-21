package app.steptwelve.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.steptwelve.AppContainer
import app.steptwelve.PendingCall
import app.steptwelve.data.CallNote
import app.steptwelve.data.CallRecord
import app.steptwelve.data.OutreachSnapshot
import app.steptwelve.data.Settings
import app.steptwelve.domain.OutreachContact
import app.steptwelve.ui.common.NoteTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Activity-scoped state shared by every screen. */
class MainViewModel(private val container: AppContainer) : ViewModel() {

    val snapshot: StateFlow<OutreachSnapshot> = container.outreachRepository.snapshot

    val settings: StateFlow<Settings?> =
        container.settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val locked: StateFlow<Boolean> = container.appLock.locked

    private val _noteTarget = MutableStateFlow<NoteTarget?>(null)
    val noteTarget: StateFlow<NoteTarget?> = _noteTarget

    private var postCallPoll: Job? = null

    init {
        // Fallback for a slow dialer: the live call log eventually shows the call.
        viewModelScope.launch {
            combine(container.postCallTracker.pending, snapshot, settings) { pending, snap, s -> Triple(pending, snap, s) }
                .collect { (pending, snap, s) ->
                    if (pending == null || s == null) return@collect
                    if (!s.postCallPrompt || System.currentTimeMillis() - pending.startedAt > 2 * 60 * 60_000L) {
                        container.postCallTracker.clear()
                        return@collect
                    }
                    snap.contact(pending.lookupKey)?.calls?.firstOrNull { pending.matches(it) }
                        ?.let { offerPostCallNote(pending, it) }
                }
        }
    }

    /**
     * Back from the dialer with a call pending: look for just that call, a few times while the
     * phone finishes writing it to the call log, instead of waiting for the full list to reload.
     */
    fun checkPendingCall() {
        val pending = container.postCallTracker.pending.value ?: return
        if (settings.value?.postCallPrompt == false) return
        postCallPoll?.cancel()
        postCallPoll = viewModelScope.launch {
            repeat(20) {
                if (container.postCallTracker.pending.value != pending) return@launch
                val call = withContext(Dispatchers.IO) {
                    runCatching { container.callLogRepository.outgoingSince(pending.startedAt - 10_000) }.getOrNull()
                }?.firstOrNull { pending.matches(it) }
                if (call != null) {
                    offerPostCallNote(pending, call)
                    return@launch
                }
                delay(500)
            }
        }
    }

    private fun offerPostCallNote(pending: PendingCall, call: CallRecord) {
        if (container.postCallTracker.pending.value != pending) return
        container.postCallTracker.clear()
        postCallPoll?.cancel()
        val contact = snapshot.value.contact(pending.lookupKey) ?: return
        if (_noteTarget.value == null && contact.notes.none { it.matches(call) }) {
            _noteTarget.value = NoteTarget(contact, call = call, title = "How did the call go?", postCall = true)
        }
    }

    override fun onCleared() {
        postCallPoll?.cancel()
    }

    fun updatePermissions(contacts: Boolean, callLog: Boolean) =
        container.outreachRepository.updatePermissions(contacts, callLog)

    fun onForeground() {
        viewModelScope.launch {
            val s = container.settingsRepository.current()
            container.appLock.onForeground(s.appLock, s.lockAfterMinutes)
        }
    }

    fun onBackground(context: Context) {
        container.appLock.onBackground()
        // Swapping launcher aliases closes the task launched from the old alias, so only do it
        // once the user has left the app.
        settings.value?.let { applyLauncherAlias(context.applicationContext, it.discreetIcon) }
    }

    fun unlock() = container.appLock.unlock()

    fun openNote(target: NoteTarget) {
        _noteTarget.value = target
    }

    fun dismissNote() {
        _noteTarget.value = null
    }

    fun saveNote(note: CallNote) {
        _noteTarget.value = null
        viewModelScope.launch { container.noteRepository.save(note) }
    }

    fun deleteNote(note: CallNote) {
        _noteTarget.value = null
        viewModelScope.launch { container.noteRepository.delete(note) }
    }

    /** Opens the dialer (ACTION_DIAL needs no CALL_PHONE permission) and arms the post-call prompt. */
    fun dial(context: Context, contact: OutreachContact) {
        val number = contact.contact.primaryNumber ?: return
        container.postCallTracker.dialed(
            PendingCall(contact.contact.lookupKey, contact.contact.numberKeys, System.currentTimeMillis())
        )
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
    }

    fun text(context: Context, contact: OutreachContact) {
        val number = contact.contact.primaryNumber ?: return
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)))
    }

    fun updateSettings(context: Context, transform: (Settings) -> Settings) {
        viewModelScope.launch {
            val old = container.settingsRepository.current()
            container.settingsRepository.update(transform)
            val new = container.settingsRepository.current()
            if (old.reminderEnabled != new.reminderEnabled || old.reminderMinuteOfDay != new.reminderMinuteOfDay) {
                container.reminderScheduler.syncDaily(new.reminderEnabled, new.reminderMinuteOfDay)
            }
            if (new.appLock && !old.appLock) container.appLock.unlock() // just authenticated to turn it on
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { MainViewModel(appContainer()) } }

        /** Switches the launcher entry between "StepTwelve" and the discreet "Circle Back". */
        fun applyLauncherAlias(context: Context, discreet: Boolean) {
            val pm = context.packageManager
            val current = pm.getComponentEnabledSetting(ComponentName(context, "app.steptwelve.LauncherDiscreet"))
            if ((current == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) == discreet) return
            fun set(alias: String, enabled: Boolean) = pm.setComponentEnabledSetting(
                ComponentName(context, "app.steptwelve.$alias"),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            // Enable the new entry before disabling the old one so there's always a launcher icon.
            set(if (discreet) "LauncherDiscreet" else "LauncherDefault", true)
            set(if (discreet) "LauncherDefault" else "LauncherDiscreet", false)
        }
    }
}
