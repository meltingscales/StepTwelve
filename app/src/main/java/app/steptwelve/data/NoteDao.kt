package app.steptwelve.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM CallNote ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallNote>>

    @Query("SELECT * FROM CallNote ORDER BY timestamp DESC")
    suspend fun getAll(): List<CallNote>

    @Query("SELECT * FROM CallNote WHERE id = :id")
    suspend fun get(id: Long): CallNote?

    @Query("SELECT * FROM CallNote WHERE followUpAt IS NOT NULL AND followUpAt > :now")
    suspend fun pendingFollowUps(now: Long): List<CallNote>

    @Upsert
    suspend fun upsert(note: CallNote): Long

    @Insert
    suspend fun insertAll(notes: List<CallNote>)

    @Query("DELETE FROM CallNote WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM CallNote")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(notes: List<CallNote>) {
        deleteAll()
        insertAll(notes)
    }
}
