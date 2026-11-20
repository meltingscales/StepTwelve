package app.steptwelve.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.steptwelve.data.Tag
import app.steptwelve.data.TagRepository
import app.steptwelve.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagsViewModel(private val repository: TagRepository) : ViewModel() {

    val tags: StateFlow<List<Tag>> =
        repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** [onResult] gets false when the label is blank or already used. */
    fun add(label: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.add(label)) }
    }

    fun rename(tag: Tag, label: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.rename(tag, label)) }
    }

    fun setEnabled(tag: Tag, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(tag, enabled) }
    }

    fun delete(tag: Tag) {
        viewModelScope.launch { repository.delete(tag) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { repository.resetToDefaults() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { TagsViewModel(appContainer().tagRepository) }
        }
    }
}
