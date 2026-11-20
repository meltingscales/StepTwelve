package app.steptwelve.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM Tag ORDER BY label")
    fun observeAll(): Flow<List<Tag>>

    /** Returns the new row id, or -1 if a tag with that label already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<Tag>)

    /** Throws SQLiteConstraintException if the new label collides with another tag. */
    @Update
    suspend fun update(tag: Tag)

    @Query("DELETE FROM Tag WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM Tag")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(tags: List<Tag>) {
        deleteAll()
        insertAll(tags)
    }
}
