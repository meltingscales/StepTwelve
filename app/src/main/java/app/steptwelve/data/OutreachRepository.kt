package app.steptwelve.data

import app.steptwelve.domain.Outreach
import app.steptwelve.domain.OutreachContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class Permissions(
    /** Null until the app has checked for the first time. */
    val contacts: Boolean? = null,
    val callLog: Boolean = false,
)

data class OutreachSnapshot(
    val permissions: Permissions = Permissions(),
    /** True until contacts have been read (or permission is known to be missing). */
    val loading: Boolean = true,
    val enabledTags: List<String> = emptyList(),
    /** Tagged contacts in name order. */
    val contacts: List<OutreachContact> = emptyList(),
) {
    fun contact(lookupKey: String): OutreachContact? = contacts.firstOrNull { it.contact.lookupKey == lookupKey }
}

/**
 * The single live view of outreach shared by every screen: tagged contacts joined with
 * the call log and the app's notes. Providers are only queried once permission is granted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OutreachRepository(
    scope: CoroutineScope,
    tagRepository: TagRepository,
    contactsRepository: ContactsRepository,
    callLogRepository: CallLogRepository,
    noteDao: NoteDao,
    settingsRepository: SettingsRepository,
) {
    private val permissions = MutableStateFlow(Permissions())

    fun updatePermissions(contacts: Boolean, callLog: Boolean) {
        permissions.value = Permissions(contacts, callLog)
    }

    private val enabledTags = tagRepository.tags.map { tags -> tags.filter { it.enabled }.map { it.label } }

    private val contacts = permissions.map { it.contacts == true }.distinctUntilChanged()
        .flatMapLatest { granted -> if (granted) contactsRepository.observeContacts() else flowOf(null) }

    private val calls = permissions.map { it.callLog }.distinctUntilChanged()
        .flatMapLatest { granted -> if (granted) callLogRepository.observeCalls() else flowOf(emptyList()) }

    val snapshot: StateFlow<OutreachSnapshot> =
        combine(
            combine(permissions, enabledTags, ::Pair),
            contacts,
            calls,
            noteDao.observeAll(),
            settingsRepository.settings.map { it.matchExtraFields }.distinctUntilChanged(),
        ) { (perms, tags), all, callList, notes, matchExtra ->
            OutreachSnapshot(
                permissions = perms,
                loading = all == null && perms.contacts != false,
                enabledTags = tags,
                contacts = if (all == null) emptyList() else Outreach.build(all, tags, callList, notes, matchExtra),
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), OutreachSnapshot())
}
