package com.histopgambling.looplock.data

import android.content.ContentResolver
import android.os.SystemClock
import android.provider.Settings
import androidx.room.withTransaction
import com.histopgambling.looplock.domain.BETBURST_PACKAGE
import com.histopgambling.looplock.domain.ApplyAgentResult
import com.histopgambling.looplock.domain.AgentProposal
import com.histopgambling.looplock.domain.AgentProposalValidator
import com.histopgambling.looplock.domain.CONSENT_VERSION
import com.histopgambling.looplock.domain.Commitment
import com.histopgambling.looplock.domain.CommitmentOverview
import com.histopgambling.looplock.domain.CommitmentPolicyGuard
import com.histopgambling.looplock.domain.CommitmentClock
import com.histopgambling.looplock.domain.CommitmentStatus
import com.histopgambling.looplock.domain.ClockReading
import com.histopgambling.looplock.domain.ClockStatus
import com.histopgambling.looplock.domain.DEMO_DURATION_MS
import com.histopgambling.looplock.domain.GuardResult
import com.histopgambling.looplock.domain.InstallQuarantineResult
import com.histopgambling.looplock.domain.LUCKYMIRROR_PACKAGE
import com.histopgambling.looplock.domain.PolicyMutation
import com.histopgambling.looplock.domain.PendingClassification
import com.histopgambling.looplock.domain.ProposalLocalSnapshot
import com.histopgambling.looplock.domain.ProposalValidationResult
import com.histopgambling.looplock.domain.ProtectionEventType
import com.histopgambling.looplock.domain.RuleSource
import com.histopgambling.looplock.domain.RuleSummary
import com.histopgambling.looplock.domain.TimelineEntry
import com.histopgambling.looplock.domain.UnlockFixtureResult
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

class PolicyRepository(
    private val database: LoopLockDatabase,
    private val contentResolver: ContentResolver,
    private val clockReading: () -> ClockReading = {
        ClockReading(
            wallMs = System.currentTimeMillis(),
            elapsedMs = SystemClock.elapsedRealtime(),
            bootCount = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0),
        )
    },
) {
    private val dao = database.loopLockDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeOverview(): Flow<CommitmentOverview> =
        dao.observeLatestCommitment().flatMapLatest { entity ->
            if (entity == null) {
                flowOf(CommitmentOverview(null, emptyList(), emptyList()))
            } else {
                combine(
                    dao.observeRules(entity.id),
                    dao.observeEvents(entity.id),
                ) { rules, events ->
                    CommitmentOverview(
                        commitment = entity.toDomain(),
                        rules = rules.map { it.toSummary() },
                        timeline = events.map { it.toTimelineEntry() },
                    )
                }
            }
        }

    suspend fun activateDemoCommitment(quarantineNewInstalls: Boolean): Commitment {
        val now = clockReading()
        val nowWallMs = now.wallMs
        val nowElapsedMs = now.elapsedMs
        val endWallMs = nowWallMs + DEMO_DURATION_MS
        val commitment = CommitmentEntity(
            id = UUID.randomUUID().toString(),
            status = CommitmentStatus.ACTIVE.name,
            createdWallMs = nowWallMs,
            startsWallMs = nowWallMs,
            endsWallMs = endWallMs,
            durationMs = DEMO_DURATION_MS,
            startElapsedMs = nowElapsedMs,
            bootCount = now.bootCount,
            quarantineNewInstalls = quarantineNewInstalls,
            consentVersion = CONSENT_VERSION,
        )
        val selectedRule = RuleEntity(
            id = UUID.randomUUID().toString(),
            commitmentId = commitment.id,
            targetPackage = BETBURST_PACKAGE,
            targetVersionCode = null,
            source = RuleSource.USER_SELECTED.name,
            createdWallMs = nowWallMs,
            expiresWallMs = endWallMs,
        )

        database.withTransaction {
            check(dao.getActiveCommitment() == null) { "An active commitment already exists" }
            dao.insertCommitment(commitment)
            dao.insertRule(selectedRule)
        }
        return commitment.toDomain()
    }

    suspend fun refreshExpiry() = database.withTransaction {
        val active = dao.getActiveCommitment() ?: return@withTransaction
        val now = clockReading()
        if (CommitmentClock.evaluate(active.toDomain(), now).status != ClockStatus.EXPIRED) {
            return@withTransaction
        }
        if (dao.expireCommitment(active.id) == 1) {
            dao.insertEvent(
                ProtectionEventEntity(
                    id = UUID.randomUUID().toString(),
                    commitmentId = active.id,
                    eventType = ProtectionEventType.COMMITMENT_EXPIRED.name,
                    targetHash = sha256("COMMITMENT"),
                    // The monotonic clock is authoritative on the same boot. Using the
                    // immutable local end preserves timeline order after wall-clock rollback.
                    createdWallMs = active.endsWallMs,
                    resultCode = "EXPIRED_LOCALLY",
                ),
            )
        }
    }

    suspend fun isPackageBlocked(packageName: String): Boolean {
        refreshExpiry()
        return dao.getEnforcedRule(packageName) != null
    }

    suspend fun getActiveCommitment(): Commitment? = dao.getActiveCommitment()?.toDomain()

    suspend fun getActiveRulePackages(): List<String> {
        refreshExpiry()
        return dao.getActiveRulePackages()
    }

    suspend fun quarantineInstalledPackage(
        packageName: String,
        label: String,
        versionCode: Long,
        firstInstallWallMs: Long,
    ): InstallQuarantineResult {
        refreshExpiry()
        return database.withTransaction {
        if (packageName != LUCKYMIRROR_PACKAGE) {
            return@withTransaction InstallQuarantineResult.NotTargetPackage
        }

        val nowWallMs = System.currentTimeMillis()
        val active = dao.getActiveCommitment()
            ?: return@withTransaction InstallQuarantineResult.CommitmentInactive
        if (!active.quarantineNewInstalls) {
            return@withTransaction InstallQuarantineResult.NotAuthorized
        }
        if (firstInstallWallMs < active.startsWallMs) {
            return@withTransaction InstallQuarantineResult.InstalledBeforeCommitment
        }
        val targetHash = sha256(packageName)
        if (dao.countInstallInstance(active.id, targetHash, firstInstallWallMs) > 0) {
            return@withTransaction InstallQuarantineResult.DuplicateSignal
        }

        val existing = dao.getRule(active.id, packageName)
        if (existing?.source == RuleSource.AGENT_TIGHTENED.name) {
            return@withTransaction InstallQuarantineResult.AlreadyTightened
        }

        val ruleCreated = if (existing == null) {
            dao.insertRuleIfAbsent(
                RuleEntity(
                    id = UUID.randomUUID().toString(),
                    commitmentId = active.id,
                    targetPackage = packageName,
                    targetVersionCode = versionCode,
                    source = RuleSource.QUARANTINE.name,
                    createdWallMs = nowWallMs,
                    expiresWallMs = active.endsWallMs,
                ),
            ) != -1L
        } else {
            false
        }

        val eventId = UUID.randomUUID().toString()
        dao.insertEvent(
            ProtectionEventEntity(
                id = eventId,
                commitmentId = active.id,
                eventType = ProtectionEventType.PACKAGE_ADDED.name,
                targetHash = targetHash,
                createdWallMs = nowWallMs,
                resultCode = "QUARANTINED_BEFORE_UPLOAD",
                uploadState = "QUEUED",
            ),
        )
        dao.insertOutbox(
            ClassificationOutboxEntity(
                eventId = eventId,
                commitmentId = active.id,
                targetHash = targetHash,
                targetPackage = packageName,
                targetLabel = label.trim().take(MAX_TARGET_LABEL_LENGTH)
                    .ifEmpty { LUCKYMIRROR_FALLBACK_LABEL },
                targetVersionCode = versionCode,
                firstInstallWallMs = firstInstallWallMs,
                createdWallMs = nowWallMs,
                uploadedWallMs = null,
            ),
        )
            InstallQuarantineResult.Quarantined(eventId, ruleCreated)
        }
    }

    suspend fun getRecoverableClassificationEventIds(): List<String> =
        dao.getRecoverableOutboxEventIds()

    suspend fun prepareClassificationAttempt(eventId: String): PendingClassification? {
        refreshExpiry()
        return database.withTransaction {
            val attemptedWallMs = System.currentTimeMillis()
            if (dao.markOutboxInFlight(eventId, attemptedWallMs) != 1) {
                return@withTransaction null
            }
            val outbox = dao.getOutbox(eventId) ?: error("Claimed outbox row disappeared")
            val commitment = dao.getCommitment(outbox.commitmentId)
            if (commitment == null || commitment.status != CommitmentStatus.ACTIVE.name) {
                terminalRejected(
                    outbox,
                    com.histopgambling.looplock.domain.ProposalRejectionCode.COMMITMENT_NOT_ACTIVE,
                    attemptedWallMs,
                )
                return@withTransaction null
            }
            dao.updateEventUploadState(eventId, OUTBOX_IN_FLIGHT)
            outbox.toPendingClassification()
        }
    }

    suspend fun markClassificationRetry(eventId: String): Boolean = database.withTransaction {
        val queued = dao.markOutboxQueuedForRetry(eventId) == 1
        if (queued) dao.updateEventUploadState(eventId, OUTBOX_QUEUED)
        queued
    }

    suspend fun applyAgentResponse(
        eventId: String,
        rawResponse: String,
        trustedNowWallMs: Long? = null,
    ): ApplyAgentResult {
        if (trustedNowWallMs == null) refreshExpiry()
        return database.withTransaction {
        val auditNowWallMs = trustedNowWallMs ?: clockReading().wallMs
        val outbox = dao.getOutbox(eventId) ?: return@withTransaction ApplyAgentResult.NotFound
        if (outbox.state.startsWith(OUTBOX_TERMINAL_PREFIX)) {
            return@withTransaction ApplyAgentResult.Duplicate(outbox.state)
        }
        val targetPackage = outbox.targetPackage
            ?: return@withTransaction terminalRejected(
                outbox = outbox,
                reasonCode = com.histopgambling.looplock.domain.ProposalRejectionCode.MISSING_FIELD,
                nowWallMs = auditNowWallMs,
            )
        val commitment = dao.getCommitment(outbox.commitmentId)
            ?: return@withTransaction terminalRejected(
                outbox = outbox,
                reasonCode = com.histopgambling.looplock.domain.ProposalRejectionCode.COMMITMENT_MISMATCH,
                nowWallMs = auditNowWallMs,
            )
        val rule = dao.getRule(outbox.commitmentId, targetPackage)
        val validationNowWallMs = trustedNowWallMs ?: trustedWallNow(commitment.toDomain())
        val validation = AgentProposalValidator.parseAndValidate(
            snapshot = ProposalLocalSnapshot(
                eventId = outbox.eventId,
                commitmentId = outbox.commitmentId,
                commitmentStatus = CommitmentStatus.valueOf(commitment.status),
                commitmentEndsWallMs = commitment.endsWallMs,
                quarantinedTargetPackage = targetPackage,
                localRuleSource = rule?.source?.let(RuleSource::valueOf),
                terminalResultRecorded = false,
            ),
            raw = rawResponse,
            trustedNowWallMs = validationNowWallMs,
        )

        when (validation) {
            is ProposalValidationResult.Tighten -> {
                check(validation.expiresWallMs == commitment.endsWallMs) {
                    "Validated tightening changed commitment timing"
                }
                if (
                    dao.promoteQuarantineRule(
                        commitmentId = outbox.commitmentId,
                        packageName = validation.targetPackage,
                        commitmentEndsWallMs = commitment.endsWallMs,
                    ) != 1
                ) {
                    return@withTransaction terminalRejected(
                        outbox = outbox,
                        reasonCode = com.histopgambling.looplock.domain.ProposalRejectionCode.TARGET_NOT_QUARANTINED,
                        nowWallMs = validationNowWallMs,
                    )
                }
                terminalizeWithEvent(
                    outbox = outbox,
                    terminalState = OUTBOX_TERMINAL_TIGHTEN,
                    eventType = ProtectionEventType.AGENT_RESULT,
                    resultCode = "AGENT_TIGHTENED",
                    nowWallMs = validationNowWallMs,
                )
                ApplyAgentResult.Tightened(outbox.targetHash, commitment.endsWallMs)
            }

            is ProposalValidationResult.Review -> {
                terminalizeWithEvent(
                    outbox = outbox,
                    terminalState = OUTBOX_TERMINAL_REVIEW,
                    eventType = ProtectionEventType.AGENT_RESULT,
                    resultCode = validation.reasonCode,
                    nowWallMs = validationNowWallMs,
                )
                ApplyAgentResult.Review(validation.reasonCode)
            }

            is ProposalValidationResult.Rejected -> terminalRejected(
                outbox = outbox,
                reasonCode = validation.reasonCode,
                nowWallMs = validationNowWallMs,
            )
        }
        }
    }

    suspend fun recordBlockAttempt(packageName: String): Boolean {
        refreshExpiry()
        return database.withTransaction {
        val active = dao.getActiveCommitment() ?: return@withTransaction false
        if (dao.getEnforcedRule(packageName) == null) return@withTransaction false

        val now = System.currentTimeMillis()
        val hash = sha256(packageName)
        if (dao.countRecentBlockAttempts(active.id, hash, now - BLOCK_DEBOUNCE_MS) > 0) {
            return@withTransaction false
        }
        dao.insertEvent(
            ProtectionEventEntity(
                id = UUID.randomUUID().toString(),
                commitmentId = active.id,
                eventType = ProtectionEventType.BLOCK_ATTEMPT.name,
                targetHash = hash,
                createdWallMs = now,
                resultCode = "BLOCKED_LOCALLY",
            ),
        )
            true
        }
    }

    suspend fun rejectMutation(mutation: PolicyMutation): GuardResult = database.withTransaction {
        val active = dao.getActiveCommitment()?.toDomain()
            ?: return@withTransaction GuardResult.Rejected("COMMITMENT_NOT_ACTIVE")
        val result = CommitmentPolicyGuard.evaluate(active, mutation)
        if (result is GuardResult.Rejected) {
            dao.insertEvent(
                ProtectionEventEntity(
                    id = UUID.randomUUID().toString(),
                    commitmentId = active.id,
                    eventType = ProtectionEventType.VALIDATION_REJECTED.name,
                    targetHash = sha256("LOCAL_POLICY"),
                    createdWallMs = System.currentTimeMillis(),
                    resultCode = result.reasonCode,
                ),
            )
        }
        result
    }

    suspend fun runUnlockFixture(): UnlockFixtureResult = database.withTransaction {
        val active = dao.getActiveCommitment()
            ?: return@withTransaction UnlockFixtureResult.NotReady
        val outbox = dao.getLatestOutbox(active.id)
            ?: return@withTransaction UnlockFixtureResult.NotReady
        val rule = dao.getRule(active.id, LUCKYMIRROR_PACKAGE)
            ?: return@withTransaction UnlockFixtureResult.NotReady

        val proposal = AgentProposal(
            schemaVersion = 1,
            eventId = outbox.eventId,
            commitmentId = active.id,
            action = "UNLOCK",
            targetType = "PACKAGE",
            targetValue = rule.targetPackage,
            classification = "DEMO_GAMBLING_APP",
            confidence = 0.98,
            reasonCode = "FIXTURE_MATCH",
            reason = "This weakening action must be rejected.",
        )
        val result = AgentProposalValidator.validate(
            snapshot = ProposalLocalSnapshot(
                eventId = outbox.eventId,
                commitmentId = active.id,
                commitmentStatus = CommitmentStatus.valueOf(active.status),
                commitmentEndsWallMs = active.endsWallMs,
                quarantinedTargetPackage = rule.targetPackage,
                localRuleSource = RuleSource.valueOf(rule.source),
                terminalResultRecorded = outbox.state.startsWith(OUTBOX_TERMINAL_PREFIX),
            ),
            proposal = proposal,
            trustedNowWallMs = System.currentTimeMillis(),
        )
        if (result !is ProposalValidationResult.Rejected) {
            error("The UNLOCK safety fixture must never be accepted")
        }

        dao.insertEvent(
            ProtectionEventEntity(
                id = UUID.randomUUID().toString(),
                commitmentId = active.id,
                eventType = ProtectionEventType.VALIDATION_REJECTED.name,
                targetHash = outbox.targetHash,
                createdWallMs = System.currentTimeMillis(),
                resultCode = result.reasonCode.name,
            ),
        )
        UnlockFixtureResult.Rejected(
            reasonCode = result.reasonCode,
            commitmentEndsWallMs = active.endsWallMs,
        )
    }

    suspend fun countEvents(commitmentId: String, type: ProtectionEventType): Int =
        dao.countEvents(commitmentId, type.name)

    suspend fun countRules(commitmentId: String): Int = dao.countRules(commitmentId)

    suspend fun countOutbox(commitmentId: String): Int = dao.countOutbox(commitmentId)

    suspend fun getOutbox(eventId: String): ClassificationOutboxEntity? = dao.getOutbox(eventId)

    suspend fun getEvent(eventId: String): ProtectionEventEntity? = dao.getEvent(eventId)

    private suspend fun terminalRejected(
        outbox: ClassificationOutboxEntity,
        reasonCode: com.histopgambling.looplock.domain.ProposalRejectionCode,
        nowWallMs: Long,
    ): ApplyAgentResult.Rejected {
        terminalizeWithEvent(
            outbox = outbox,
            terminalState = OUTBOX_TERMINAL_REJECTED,
            eventType = ProtectionEventType.VALIDATION_REJECTED,
            resultCode = reasonCode.name,
            nowWallMs = nowWallMs,
        )
        return ApplyAgentResult.Rejected(reasonCode)
    }

    private suspend fun terminalizeWithEvent(
        outbox: ClassificationOutboxEntity,
        terminalState: String,
        eventType: ProtectionEventType,
        resultCode: String,
        nowWallMs: Long,
    ) {
        check(dao.terminalizeAndScrubOutbox(outbox.eventId, terminalState) == 1) {
            "Outbox terminalization lost its idempotency race"
        }
        check(dao.updateEventUploadState(outbox.eventId, terminalState) == 1) {
            "Outbox source event is missing"
        }
        dao.insertEvent(
            ProtectionEventEntity(
                id = UUID.randomUUID().toString(),
                commitmentId = outbox.commitmentId,
                eventType = eventType.name,
                targetHash = outbox.targetHash,
                createdWallMs = nowWallMs,
                resultCode = resultCode,
                uploadState = "LOCAL_ONLY",
            ),
        )
    }

    private fun trustedWallNow(commitment: Commitment): Long {
        val now = clockReading()
        val evaluated = CommitmentClock.evaluate(commitment, now)
        return when (evaluated.status) {
            ClockStatus.ACTIVE -> commitment.endsWallMs - evaluated.remainingMs
            ClockStatus.EXPIRED -> commitment.endsWallMs
            ClockStatus.REBOOT_FALLBACK,
            ClockStatus.INCONSISTENT,
            -> now.wallMs.coerceAtMost(commitment.endsWallMs - 1)
        }
    }

    companion object {
        private const val BLOCK_DEBOUNCE_MS = 1_500L
        private const val MAX_TARGET_LABEL_LENGTH = 80
        private const val LUCKYMIRROR_FALLBACK_LABEL = "LuckyMirror Demo"
        private const val OUTBOX_QUEUED = "QUEUED"
        private const val OUTBOX_IN_FLIGHT = "IN_FLIGHT"
        private const val OUTBOX_TERMINAL_PREFIX = "TERMINAL_"
        private const val OUTBOX_TERMINAL_TIGHTEN = "TERMINAL_TIGHTEN"
        private const val OUTBOX_TERMINAL_REVIEW = "TERMINAL_REVIEW"
        private const val OUTBOX_TERMINAL_REJECTED = "TERMINAL_REJECTED"

        fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}

private fun ClassificationOutboxEntity.toPendingClassification(): PendingClassification =
    PendingClassification(
        eventId = eventId,
        commitmentId = commitmentId,
        targetPackage = checkNotNull(targetPackage),
        targetLabel = checkNotNull(targetLabel),
        targetVersionCode = checkNotNull(targetVersionCode),
        targetHash = targetHash,
        attemptNumber = retryCount,
    )

private fun CommitmentEntity.toDomain() = Commitment(
    id = id,
    status = CommitmentStatus.valueOf(status),
    startsWallMs = startsWallMs,
    endsWallMs = endsWallMs,
    startElapsedMs = startElapsedMs,
    bootCount = bootCount,
    quarantineNewInstalls = quarantineNewInstalls,
    consentVersion = consentVersion,
)

private fun RuleEntity.toSummary() = RuleSummary(
    packageName = targetPackage,
    source = RuleSource.valueOf(source),
    expiresWallMs = expiresWallMs,
)

private fun ProtectionEventEntity.toTimelineEntry() = TimelineEntry(
    eventId = id,
    type = ProtectionEventType.valueOf(eventType),
    resultCode = resultCode,
    uploadState = uploadState,
    createdWallMs = createdWallMs,
)
