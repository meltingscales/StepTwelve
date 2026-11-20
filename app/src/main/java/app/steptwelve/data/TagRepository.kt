package app.steptwelve.data

import android.database.sqlite.SQLiteConstraintException
import app.steptwelve.domain.TagMatcher
import kotlinx.coroutines.flow.Flow

class TagRepository(private val dao: TagDao) {

    val tags: Flow<List<Tag>> = dao.observeAll()

    /** Returns false if the label is blank or already exists. */
    suspend fun add(label: String): Boolean {
        val normalized = TagMatcher.normalizeTag(label)
        if (normalized.isEmpty()) return false
        return dao.insert(Tag(label = normalized)) != -1L
    }

    /** Returns false if the new label is blank or used by another tag. */
    suspend fun rename(tag: Tag, newLabel: String): Boolean {
        val normalized = TagMatcher.normalizeTag(newLabel)
        if (normalized.isEmpty()) return false
        if (normalized == tag.label) return true
        return try {
            dao.update(tag.copy(label = normalized))
            true
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    suspend fun setEnabled(tag: Tag, enabled: Boolean) = dao.update(tag.copy(enabled = enabled))

    suspend fun delete(tag: Tag) = dao.delete(tag.id)

    suspend fun getAll(): List<Tag> = dao.getAll()

    suspend fun replaceAll(tags: List<Tag>) = dao.replaceAll(tags)

    /** Adds tags whose labels don't exist yet. */
    suspend fun mergeAll(tags: List<Tag>) = dao.insertAll(tags)

    suspend fun resetToDefaults() = dao.replaceAll(DEFAULT_TAGS.map { Tag(label = it) })

    companion object {
        val DEFAULT_TAGS = listOf("12step", "saa", "aa", "newcomer", "sponsee", "sponsor")
    }
}
