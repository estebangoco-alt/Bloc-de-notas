package io.github.estebangocoalt.blocdenotas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, ChecklistItemEntity::class, BoardColumnEntity::class, BoardEntity::class, AttachmentEntity::class],
    version = 11,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "bloc-de-notas.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .build()

        /** Versión 11: tareas que dependen de otras. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN dependsOn TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Versión 10: imágenes y notas de voz adjuntas. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS attachments (" +
                        "id TEXT NOT NULL, noteId TEXT NOT NULL, kind TEXT NOT NULL, fileName TEXT NOT NULL, " +
                        "position INTEGER NOT NULL, durationMs INTEGER NOT NULL DEFAULT 0, " +
                        "createdAt INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id), " +
                        "FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_noteId ON attachments (noteId)")
            }
        }

        /** Versión 9: pasillo de cada artículo de la lista de la compra. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE checklist_items ADD COLUMN aisle TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Versión 8: varios tableros (uno por proyecto). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS boards (" +
                        "id TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, " +
                        "PRIMARY KEY(id))",
                )
                db.execSQL("ALTER TABLE board_columns ADD COLUMN boardId TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Versión 7: subpáginas y papelera. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN parentId TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER")
            }
        }

        /** Versión 6: propiedades personalizadas (prioridad y etiquetas). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Versión 5: tareas que se repiten. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN repeatRule TEXT")
            }
        }

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
