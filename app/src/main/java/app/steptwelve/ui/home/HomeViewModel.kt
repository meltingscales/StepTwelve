package app.steptwelve.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.steptwelve.data.CallLogRepository
import app.steptwelve.data.ContactsRepository
import app.steptwelve.data.DeviceContact
import app.steptwelve.data.TagRepository
import app.steptwelve.domain.TagMatcher
import app.steptwelve.ui.appContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TaggedContact(
    val contact: DeviceContact,
    val tags: List<String>,
    /** Most recent outgoing call to any of the contact's numbers, or null if none on record. */
    val lastCalledAt: Long?,
)

data class HomeUiState(
    /** Null until the screen has checked the permission for the first time. */
    val hasContactsPermission: Boolean? = null,
    val hasCallLogPermission: Boolean = false,
    val loading: Boolean = true,
    val enabledTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    val query: String = "",
    val contacts: List<TaggedContact> = emptyList(),
)

private data class Filters(val selectedTags: Set<String>, val query: String)

private data class Permissions(val contacts: Boolean?, val callLog: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    tagRepository: TagRepository,
    contactsRepository: ContactsRepository,
    callLogRepository: CallLogRepository,
) : ViewModel() {

    private val permissions = MutableStateFlow(Permissions(contacts = null, callLog = false))
    private val selectedTags = MutableStateFlow(emptySet<String>())
    private val query = MutableStateFlow("")

    private val filters = combine(selectedTags, query, ::Filters)

    private val enabledTags = tagRepository.tags.map { tags -> tags.filter { it.enabled }.map { it.label } }

    private val contacts = permissions
        .map { it.contacts == true }
        .flatMapLatest { granted -> if (granted) contactsRepository.observeContacts() else flowOf(null) }

    private val lastOutgoing = permissions
        .map { it.callLog }
        .flatMapLatest { granted -> if (granted) callLogRepository.observeLastOutgoing() else flowOf(emptyMap()) }

    private val taggedContacts = combine(contacts, enabledTags, lastOutgoing) { all, tags, lastCalls ->
        all?.mapNotNull { contact ->
            val matched = TagMatcher.matchingTags(contact.displayName, tags)
            if (matched.isEmpty()) {
                null
            } else {
                TaggedContact(
                    contact = contact,
                    tags = matched,
                    lastCalledAt = contact.numberKeys.mapNotNull { lastCalls[it] }.maxOrNull(),
                )
            }
        }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(permissions, enabledTags, filters, taggedContacts) { perms, tags, f, tagged ->
            // Drop selections for tags that were deleted or disabled.
            val activeSelection = f.selectedTags intersect tags.toSet()
            HomeUiState(
                hasContactsPermission = perms.contacts,
                hasCallLogPermission = perms.callLog,
                loading = tagged == null,
                enabledTags = tags,
                selectedTags = activeSelection,
                query = f.query,
                contacts = tagged.orEmpty().filter { contact ->
                    (activeSelection.isEmpty() || contact.tags.any { it in activeSelection }) &&
                        (f.query.isBlank() || contact.contact.displayName.contains(f.query.trim(), ignoreCase = true))
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onPermissionsChanged(contacts: Boolean, callLog: Boolean) {
        permissions.value = Permissions(contacts, callLog)
    }

    fun toggleTag(tag: String) {
        selectedTags.value = selectedTags.value.let { if (tag in it) it - tag else it + tag }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HomeViewModel(container.tagRepository, container.contactsRepository, container.callLogRepository)
            }
        }
    }
}
