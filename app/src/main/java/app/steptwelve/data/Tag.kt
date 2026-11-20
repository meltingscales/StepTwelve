package app.steptwelve.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A tag such as "saa". [label] is stored in normalized form (see TagMatcher.normalizeTag). */
@Serializable
@Entity(indices = [Index(value = ["label"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val enabled: Boolean = true,
)
