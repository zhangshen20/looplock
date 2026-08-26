package com.histopgambling.looplock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.security.MessageDigest

@Database(
    entities = [
        CommitmentEntity::class,
        RuleEntity::class,
        ProtectionEventEntity::class,
        ClassificationOutboxEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class LoopLockDatabase : RoomDatabase() {
    abstract fun loopLockDao(): LoopLockDao

    companion object {
        @Volatile private var instance: LoopLockDatabase? = null

        fun get(context: Context): LoopLockDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LoopLockDatabase::class.java,
                    "looplock.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `classification_outbox` (
                        `event_id` TEXT NOT NULL,
                        `commitment_id` TEXT NOT NULL,
                        `target_package` TEXT NOT NULL,
                        `target_label` TEXT NOT NULL,
                        `target_version_code` INTEGER NOT NULL,
                        `created_wall_ms` INTEGER NOT NULL,
                        `uploaded_wall_ms` INTEGER,
                        `retry_count` INTEGER NOT NULL,
                        `state` TEXT NOT NULL,
                        PRIMARY KEY(`event_id`),
                        FOREIGN KEY(`commitment_id`) REFERENCES `commitments`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_classification_outbox_commitment_id` " +
                        "ON `classification_outbox` (`commitment_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_classification_outbox_target_package` " +
                        "ON `classification_outbox` (`target_package`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `classification_outbox` " +
                        "ADD COLUMN `first_install_wall_ms` INTEGER",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_classification_outbox_commitment_id_target_package_first_install_wall_ms` " +
                        "ON `classification_outbox` " +
                        "(`commitment_id`, `target_package`, `first_install_wall_ms`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `classification_outbox_v4` (
                        `event_id` TEXT NOT NULL,
                        `commitment_id` TEXT NOT NULL,
                        `target_hash` TEXT NOT NULL,
                        `target_package` TEXT,
                        `target_label` TEXT,
                        `target_version_code` INTEGER,
                        `first_install_wall_ms` INTEGER,
                        `created_wall_ms` INTEGER NOT NULL,
                        `uploaded_wall_ms` INTEGER,
                        `retry_count` INTEGER NOT NULL,
                        `state` TEXT NOT NULL,
                        PRIMARY KEY(`event_id`),
                        FOREIGN KEY(`commitment_id`) REFERENCES `commitments`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `classification_outbox_v4` (
                        `event_id`, `commitment_id`, `target_hash`, `target_package`,
                        `target_label`, `target_version_code`, `first_install_wall_ms`,
                        `created_wall_ms`, `uploaded_wall_ms`, `retry_count`, `state`
                    )
                    SELECT `event_id`, `commitment_id`, '', `target_package`,
                        `target_label`, `target_version_code`, `first_install_wall_ms`,
                        `created_wall_ms`, `uploaded_wall_ms`, `retry_count`, `state`
                    FROM `classification_outbox`
                    """.trimIndent(),
                )

                db.query("SELECT `event_id`, `target_package` FROM `classification_outbox_v4`").use { cursor ->
                    val eventIdIndex = cursor.getColumnIndexOrThrow("event_id")
                    val packageIndex = cursor.getColumnIndexOrThrow("target_package")
                    val statement = db.compileStatement(
                        "UPDATE `classification_outbox_v4` SET `target_hash` = ? WHERE `event_id` = ?",
                    )
                    while (cursor.moveToNext()) {
                        statement.clearBindings()
                        statement.bindString(1, sha256(cursor.getString(packageIndex)))
                        statement.bindString(2, cursor.getString(eventIdIndex))
                        statement.executeUpdateDelete()
                    }
                }

                db.execSQL("DROP TABLE `classification_outbox`")
                db.execSQL("ALTER TABLE `classification_outbox_v4` RENAME TO `classification_outbox`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_classification_outbox_commitment_id` " +
                        "ON `classification_outbox` (`commitment_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_classification_outbox_target_hash` " +
                        "ON `classification_outbox` (`target_hash`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_classification_outbox_commitment_id_target_hash_first_install_wall_ms` " +
                        "ON `classification_outbox` " +
                        "(`commitment_id`, `target_hash`, `first_install_wall_ms`)",
                )
            }
        }

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
