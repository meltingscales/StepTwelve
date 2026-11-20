package app.steptwelve.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Tag::class, CallNote::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun noteDao(): NoteDao

    companion object {
        /**
         * Lives in app-private internal storage, which is credential-encrypted by default.
         * Excluded from every backup path by the manifest's backup rules.
         */
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "steptwelve.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        TagRepository.DEFAULT_TAGS.forEach { label ->
                            db.execSQL("INSERT INTO Tag (label, enabled) VALUES (?, 1)", arrayOf(label))
                        }
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v2 added "sponsee" and "sponsor" to the default tags; give them to existing installs too. */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("sponsee", "sponsor").forEach { label ->
                    db.execSQL("INSERT OR IGNORE INTO Tag (label, enabled) VALUES (?, 1)", arrayOf(label))
                }
            }
        }
    }
}
