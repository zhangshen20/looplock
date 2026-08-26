package com.histopgambling.looplock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LoopLockDao {
    @Insert
    suspend fun insertCommitment(commitment: CommitmentEntity)

    @Insert
    suspend fun insertRule(rule: RuleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRuleIfAbsent(rule: RuleEntity): Long

    @Insert
    suspend fun insertEvent(event: ProtectionEventEntity)

    @Insert
    suspend fun insertOutbox(outbox: ClassificationOutboxEntity)

    @Query(
        """
        UPDATE rules
        SET source = 'AGENT_TIGHTENED', target_version_code = NULL
        WHERE commitment_id = :commitmentId
          AND target_package = :packageName
          AND source = 'QUARANTINE'
          AND expires_wall_ms = :commitmentEndsWallMs
        """,
    )
    suspend fun promoteQuarantineRule(
        commitmentId: String,
        packageName: String,
        commitmentEndsWallMs: Long,
    ): Int

    @Query(
        """
        UPDATE classification_outbox
        SET state = 'IN_FLIGHT',
            uploaded_wall_ms = COALESCE(uploaded_wall_ms, :attemptedWallMs),
            retry_count = retry_count + 1
        WHERE event_id = :eventId
          AND state IN ('QUEUED', 'IN_FLIGHT')
          AND target_package IS NOT NULL
          AND target_label IS NOT NULL
          AND target_version_code IS NOT NULL
        """,
    )
    suspend fun markOutboxInFlight(eventId: String, attemptedWallMs: Long): Int

    @Query(
        """
        UPDATE classification_outbox
        SET state = 'QUEUED'
        WHERE event_id = :eventId AND state = 'IN_FLIGHT'
        """,
    )
    suspend fun markOutboxQueuedForRetry(eventId: String): Int

    @Query(
        """
        UPDATE classification_outbox
        SET target_package = NULL,
            target_label = NULL,
            target_version_code = NULL,
            state = :terminalState
        WHERE event_id = :eventId AND state IN ('QUEUED', 'IN_FLIGHT')
        """,
    )
    suspend fun terminalizeAndScrubOutbox(eventId: String, terminalState: String): Int

    @Query(
        """
        UPDATE protection_events
        SET upload_state = :uploadState
        WHERE id = :eventId
        """,
    )
    suspend fun updateEventUploadState(eventId: String, uploadState: String): Int

    @Query("SELECT * FROM commitments WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveCommitment(): CommitmentEntity?

    @Query("SELECT * FROM commitments WHERE id = :commitmentId LIMIT 1")
    suspend fun getCommitment(commitmentId: String): CommitmentEntity?

    @Query(
        """
        SELECT * FROM rules
        WHERE commitment_id = :commitmentId AND target_package = :packageName
        LIMIT 1
        """,
    )
    suspend fun getRule(commitmentId: String, packageName: String): RuleEntity?

    @Query(
        """
        SELECT rules.target_package FROM rules
        INNER JOIN commitments ON commitments.id = rules.commitment_id
        WHERE commitments.status = 'ACTIVE'
          AND rules.expires_wall_ms = commitments.ends_wall_ms
        ORDER BY rules.created_wall_ms
        """,
    )
    suspend fun getActiveRulePackages(): List<String>

    @Query("SELECT * FROM commitments ORDER BY created_wall_ms DESC LIMIT 1")
    fun observeLatestCommitment(): Flow<CommitmentEntity?>

    @Query("SELECT * FROM commitments ORDER BY created_wall_ms DESC LIMIT 1")
    suspend fun getLatestCommitment(): CommitmentEntity?

    @Query("SELECT * FROM rules WHERE commitment_id = :commitmentId ORDER BY created_wall_ms")
    fun observeRules(commitmentId: String): Flow<List<RuleEntity>>

    @Query(
        "SELECT * FROM protection_events WHERE commitment_id = :commitmentId " +
            "ORDER BY created_wall_ms, id",
    )
    fun observeEvents(commitmentId: String): Flow<List<ProtectionEventEntity>>

    @Query(
        """
        SELECT rules.* FROM rules
        INNER JOIN commitments ON commitments.id = rules.commitment_id
        WHERE commitments.status = 'ACTIVE'
          AND rules.target_package = :packageName
          AND rules.expires_wall_ms = commitments.ends_wall_ms
        LIMIT 1
        """,
    )
    suspend fun getEnforcedRule(packageName: String): RuleEntity?

    @Query(
        """
        UPDATE commitments
        SET status = 'EXPIRED'
        WHERE id = :commitmentId AND status = 'ACTIVE'
        """,
    )
    suspend fun expireCommitment(commitmentId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM protection_events
        WHERE commitment_id = :commitmentId
          AND event_type = 'BLOCK_ATTEMPT'
          AND target_hash = :targetHash
          AND created_wall_ms >= :cutoffWallMs
        """,
    )
    suspend fun countRecentBlockAttempts(
        commitmentId: String,
        targetHash: String,
        cutoffWallMs: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM classification_outbox
        WHERE commitment_id = :commitmentId
          AND target_hash = :targetHash
          AND first_install_wall_ms = :firstInstallWallMs
        """,
    )
    suspend fun countInstallInstance(
        commitmentId: String,
        targetHash: String,
        firstInstallWallMs: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM protection_events
        WHERE commitment_id = :commitmentId AND event_type = :eventType
        """,
    )
    suspend fun countEvents(commitmentId: String, eventType: String): Int

    @Query("SELECT COUNT(*) FROM rules WHERE commitment_id = :commitmentId")
    suspend fun countRules(commitmentId: String): Int

    @Query("SELECT COUNT(*) FROM classification_outbox WHERE commitment_id = :commitmentId")
    suspend fun countOutbox(commitmentId: String): Int

    @Query("SELECT * FROM classification_outbox WHERE event_id = :eventId LIMIT 1")
    suspend fun getOutbox(eventId: String): ClassificationOutboxEntity?

    @Query(
        """
        SELECT event_id FROM classification_outbox
        WHERE state IN ('QUEUED', 'IN_FLIGHT')
          AND target_package IS NOT NULL
          AND target_label IS NOT NULL
          AND target_version_code IS NOT NULL
        ORDER BY created_wall_ms, event_id
        """,
    )
    suspend fun getRecoverableOutboxEventIds(): List<String>

    @Query(
        """
        SELECT * FROM classification_outbox
        WHERE commitment_id = :commitmentId
        ORDER BY created_wall_ms DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestOutbox(commitmentId: String): ClassificationOutboxEntity?

    @Query("SELECT * FROM protection_events WHERE id = :eventId LIMIT 1")
    suspend fun getEvent(eventId: String): ProtectionEventEntity?
}
