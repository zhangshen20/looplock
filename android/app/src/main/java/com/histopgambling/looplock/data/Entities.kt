package com.histopgambling.looplock.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey val id: String,
    val status: String,
    @ColumnInfo(name = "created_wall_ms") val createdWallMs: Long,
    @ColumnInfo(name = "starts_wall_ms") val startsWallMs: Long,
    @ColumnInfo(name = "ends_wall_ms") val endsWallMs: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "start_elapsed_ms") val startElapsedMs: Long,
    @ColumnInfo(name = "boot_count") val bootCount: Int,
    @ColumnInfo(name = "quarantine_new_installs") val quarantineNewInstalls: Boolean,
    @ColumnInfo(name = "consent_version") val consentVersion: Int,
)

@Entity(
    tableName = "rules",
    foreignKeys = [
        ForeignKey(
            entity = CommitmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commitment_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("commitment_id"),
        Index(value = ["commitment_id", "target_package"], unique = true),
    ],
)
data class RuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "commitment_id") val commitmentId: String,
    @ColumnInfo(name = "target_package") val targetPackage: String,
    @ColumnInfo(name = "target_version_code") val targetVersionCode: Long?,
    val source: String,
    @ColumnInfo(name = "created_wall_ms") val createdWallMs: Long,
    @ColumnInfo(name = "expires_wall_ms") val expiresWallMs: Long,
)

@Entity(
    tableName = "protection_events",
    foreignKeys = [
        ForeignKey(
            entity = CommitmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commitment_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("commitment_id"), Index("event_type")],
)
data class ProtectionEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "commitment_id") val commitmentId: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "target_hash") val targetHash: String,
    @ColumnInfo(name = "created_wall_ms") val createdWallMs: Long,
    @ColumnInfo(name = "result_code") val resultCode: String,
    @ColumnInfo(name = "upload_state") val uploadState: String = "LOCAL_ONLY",
)

@Entity(
    tableName = "classification_outbox",
    foreignKeys = [
        ForeignKey(
            entity = CommitmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commitment_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("commitment_id"),
        Index("target_hash"),
        Index(
            value = ["commitment_id", "target_hash", "first_install_wall_ms"],
            unique = true,
        ),
    ],
)
data class ClassificationOutboxEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "commitment_id") val commitmentId: String,
    @ColumnInfo(name = "target_hash") val targetHash: String,
    @ColumnInfo(name = "target_package") val targetPackage: String?,
    @ColumnInfo(name = "target_label") val targetLabel: String?,
    @ColumnInfo(name = "target_version_code") val targetVersionCode: Long?,
    @ColumnInfo(name = "first_install_wall_ms") val firstInstallWallMs: Long?,
    @ColumnInfo(name = "created_wall_ms") val createdWallMs: Long,
    @ColumnInfo(name = "uploaded_wall_ms") val uploadedWallMs: Long?,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    val state: String = "QUEUED",
)
