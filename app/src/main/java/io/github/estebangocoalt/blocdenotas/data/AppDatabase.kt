package io.github.estebangocoalt.blocdenotas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, ChecklistItemEntity::class, BoardColumnEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "bloc-de-notas.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        /** Versión 4: recordatorios con notificación. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderMinute INTEGER")
            }
        }

        /** Versión 3: tareas con fecha límite y estado «hecha». */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN completedAt INTEGER")
            }
        }

        /** Versión 2: tablero estilo Trello (columnas y posición de las tarjetas). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN columnId TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN columnPosition INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS board_columns (" +
                        "id TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, " +
                        "PRIMARY KEY(id))",
                )
            }
        }
    }
}
