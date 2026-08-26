package com.histopgambling.looplock.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.histopgambling.looplock.domain.GuardResult
import com.histopgambling.looplock.domain.ApplyAgentResult
import com.histopgambling.looplock.domain.ClockReading
import com.histopgambling.looplock.domain.CommitmentStatus
import com.histopgambling.looplock.domain.InstallQuarantineResult
import com.histopgambling.looplock.domain.LUCKYMIRROR_PACKAGE
import com.histopgambling.looplock.domain.PolicyMutation
import com.histopgambling.looplock.domain.ProtectionEventType
import com.histopgambling.looplock.domain.RuleSource
import com.histopgambling.looplock.domain.UnlockFixtureResult
import com.histopgambling.looplock.enforcement.InstallMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PolicyRepositoryInstrumentedTest {
    private lateinit var database: LoopLockDatabase
    private lateinit var repository: PolicyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LoopLockDatabase::class.java).build()
        repository = PolicyRepository(database, context.contentResolver)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun activationIsAtomicAndASecondActiveCommitmentIsRejected() = runBlocking {
        val first = repository.activateDemoCommitment(quarantineNewInstalls = true)
        val overview = repository.observeOverview().first { it.commitment != null }

        assertEquals(first.id, overview.commitment?.id)
        assertEquals(1, overview.rules.size)
        assertEquals(first.endsWallMs, overview.rules.single().expiresWallMs)
        try {
            repository.activateDemoCommitment(quarantineNewInstalls = false)
            fail("A second active commitment must be rejected")
        } catch (_: IllegalStateException) {
            // Expected: active commitments are append-only and singular.
        }
    }

    @Test
    fun weakeningAttemptLeavesPolicyUnchangedAndWritesAuditEvent() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = true)
        val before = repository.observeOverview().first { it.commitment != null }

        val result = repository.rejectMutation(PolicyMutation.ShortenCommitment(active.endsWallMs - 60_000))
        val after = repository.observeOverview().first { it.commitment != null }

        assertTrue(result is GuardResult.Rejected)
        assertEquals(before.commitment, after.commitment)
        assertEquals(before.rules, after.rules)
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.VALIDATION_REJECTED))
    }

    @Test
    fun preAuthorizedInstallAtomicallyCreatesQuarantineEventAndQueuedOutbox() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = true)

        val result = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = active.startsWallMs + 1,
        )

        assertTrue(result is InstallQuarantineResult.Quarantined)
        val quarantined = result as InstallQuarantineResult.Quarantined
        val overview = repository.observeOverview().first { it.rules.size == 2 }
        assertTrue(quarantined.ruleCreated)
        assertTrue(
            overview.rules.any {
                it.packageName == LUCKYMIRROR_PACKAGE && it.source == RuleSource.QUARANTINE
            },
        )
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.PACKAGE_ADDED))
        assertEquals(1, repository.countOutbox(active.id))

        val event = repository.getEvent(quarantined.eventId)!!
        val outbox = repository.getOutbox(quarantined.eventId)!!
        assertEquals("QUARANTINED_BEFORE_UPLOAD", event.resultCode)
        assertEquals("QUEUED", event.uploadState)
        assertEquals("QUEUED", outbox.state)
        assertEquals(event.createdWallMs, outbox.createdWallMs)
        assertEquals(active.startsWallMs + 1, outbox.firstInstallWallMs)
        assertNull(outbox.uploadedWallMs)
    }

    @Test
    fun installWithoutPreAuthorizationCreatesNothing() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = false)

        val result = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = active.startsWallMs + 1,
        )

        assertTrue(result is InstallQuarantineResult.NotAuthorized)
        assertEquals(1, repository.countRules(active.id))
        assertEquals(0, repository.countEvents(active.id, ProtectionEventType.PACKAGE_ADDED))
        assertEquals(0, repository.countOutbox(active.id))
    }

    @Test
    fun existingInstallAndDuplicateSignalDoNotCreateExtraWork() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = true)

        val existingResult = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = active.startsWallMs - 1,
        )
        assertTrue(existingResult is InstallQuarantineResult.InstalledBeforeCommitment)

        val first = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = active.startsWallMs + 1,
        )
        val duplicate = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = active.startsWallMs + 1,
        )

        assertTrue(first is InstallQuarantineResult.Quarantined)
        assertTrue(duplicate is InstallQuarantineResult.DuplicateSignal)
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.PACKAGE_ADDED))
        assertEquals(1, repository.countOutbox(active.id))
    }

    @Test
    fun installMonitorAcceptsOnlyNewLuckyMirrorInstallSignals() {
        val install = Intent(
            Intent.ACTION_PACKAGE_ADDED,
            Uri.parse("package:$LUCKYMIRROR_PACKAGE"),
        )
        val replacement = Intent(install).putExtra(Intent.EXTRA_REPLACING, true)
        val otherPackage = Intent(
            Intent.ACTION_PACKAGE_ADDED,
            Uri.parse("package:com.example.unrelated"),
        )

        assertTrue(InstallMonitor.shouldHandle(install))
        assertFalse(InstallMonitor.shouldHandle(replacement))
        assertFalse(InstallMonitor.shouldHandle(otherPackage))
    }

    @Test
    fun visibleUnlockFixtureRetainsQuarantineAndCommitmentEnd() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = true)
        repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = active.startsWallMs + 1,
        )
        val before = repository.observeOverview().first { it.rules.size == 2 }

        val result = repository.runUnlockFixture()
        val after = repository.observeOverview().first { it.rules.size == 2 }

        assertTrue(result is UnlockFixtureResult.Rejected)
        result as UnlockFixtureResult.Rejected
        assertEquals("ACTION_NOT_ALLOWED", result.reasonCode.name)
        assertEquals(active.endsWallMs, result.commitmentEndsWallMs)
        assertEquals(before.commitment, after.commitment)
        assertEquals(before.rules, after.rules)
        assertTrue(after.rules.any { it.packageName == LUCKYMIRROR_PACKAGE && it.source == RuleSource.QUARANTINE })
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.VALIDATION_REJECTED))
    }

    @Test
    fun validTightenAtomicallyPromotesExistingRuleScrubsMetadataAndIsIdempotent() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = true)
        val firstInstallWallMs = active.startsWallMs + 1
        val quarantine = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "L".repeat(100),
            versionCode = 1,
            firstInstallWallMs = firstInstallWallMs,
        ) as InstallQuarantineResult.Quarantined

        val pending = repository.prepareClassificationAttempt(quarantine.eventId)!!
        assertEquals(80, pending.targetLabel.length)
        assertEquals(PolicyRepository.sha256(LUCKYMIRROR_PACKAGE), pending.targetHash)
        assertEquals(listOf(quarantine.eventId), repository.getRecoverableClassificationEventIds())

        val result = repository.applyAgentResponse(
            eventId = quarantine.eventId,
            rawResponse = validResponse(quarantine.eventId, active.id),
            trustedNowWallMs = active.startsWallMs + 2,
        )

        assertTrue(result is ApplyAgentResult.Tightened)
        val overview = repository.observeOverview().first { overview ->
            overview.rules.any { it.source == RuleSource.AGENT_TIGHTENED }
        }
        val tightened = overview.rules.single { it.packageName == LUCKYMIRROR_PACKAGE }
        assertEquals(active.endsWallMs, tightened.expiresWallMs)
        assertEquals(2, overview.rules.size)
        assertTrue(repository.isPackageBlocked(LUCKYMIRROR_PACKAGE))
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.AGENT_RESULT))

        val terminal = repository.getOutbox(quarantine.eventId)!!
        assertEquals("TERMINAL_TIGHTEN", terminal.state)
        assertNull(terminal.targetPackage)
        assertNull(terminal.targetLabel)
        assertNull(terminal.targetVersionCode)
        assertEquals(PolicyRepository.sha256(LUCKYMIRROR_PACKAGE), terminal.targetHash)
        assertEquals(firstInstallWallMs, terminal.firstInstallWallMs)
        assertNotNull(terminal.uploadedWallMs)
        assertTrue(repository.getRecoverableClassificationEventIds().isEmpty())

        val duplicate = repository.applyAgentResponse(
            eventId = quarantine.eventId,
            rawResponse = validResponse(quarantine.eventId, active.id),
            trustedNowWallMs = active.startsWallMs + 3,
        )
        assertEquals(ApplyAgentResult.Duplicate("TERMINAL_TIGHTEN"), duplicate)
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.AGENT_RESULT))
        assertEquals(2, repository.countRules(active.id))
    }

    @Test
    fun reviewRetainsQuarantineScrubsMetadataAndPreservesInstallDeduplication() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = true)
        val firstInstallWallMs = active.startsWallMs + 1
        val quarantine = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = firstInstallWallMs,
        ) as InstallQuarantineResult.Quarantined

        val result = repository.applyAgentResponse(
            eventId = quarantine.eventId,
            rawResponse = reviewResponse(quarantine.eventId, active.id),
            trustedNowWallMs = active.startsWallMs + 2,
        )

        assertEquals(ApplyAgentResult.Review("NEEDS_REVIEW"), result)
        val overview = repository.observeOverview().first { it.rules.size == 2 }
        assertTrue(
            overview.rules.any {
                it.packageName == LUCKYMIRROR_PACKAGE && it.source == RuleSource.QUARANTINE
            },
        )
        val terminal = repository.getOutbox(quarantine.eventId)!!
        assertEquals("TERMINAL_REVIEW", terminal.state)
        assertNull(terminal.targetPackage)
        assertNull(terminal.targetLabel)
        assertNull(terminal.targetVersionCode)
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.AGENT_RESULT))

        val duplicateInstallSignal = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = firstInstallWallMs,
        )
        assertTrue(duplicateInstallSignal is InstallQuarantineResult.DuplicateSignal)
        assertEquals(1, repository.countOutbox(active.id))
    }

    @Test
    fun malformedAndLateResultsTerminalizeWithoutAddingOrWeakeningRules() = runBlocking {
        suspend fun arrange(): Triple<com.histopgambling.looplock.domain.Commitment, String, List<com.histopgambling.looplock.domain.RuleSummary>> {
            val active = repository.activateDemoCommitment(quarantineNewInstalls = true)
            val quarantine = repository.quarantineInstalledPackage(
                packageName = LUCKYMIRROR_PACKAGE,
                label = "LuckyMirror Demo",
                versionCode = 1,
                firstInstallWallMs = active.startsWallMs + 1,
            ) as InstallQuarantineResult.Quarantined
            val before = repository.observeOverview().first { it.rules.size == 2 }.rules
            return Triple(active, quarantine.eventId, before)
        }

        val (active, eventId, before) = arrange()
        val malformed = repository.applyAgentResponse(
            eventId = eventId,
            rawResponse = "not-json",
            trustedNowWallMs = active.startsWallMs + 2,
        )
        assertTrue(malformed is ApplyAgentResult.Rejected)
        assertEquals(before, repository.observeOverview().first { it.rules.size == 2 }.rules)
        assertEquals("TERMINAL_REJECTED", repository.getOutbox(eventId)?.state)
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.VALIDATION_REJECTED))
    }

    @Test
    fun lateResultRetainsQuarantineAndCommitmentTiming() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = true)
        val quarantine = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = active.startsWallMs + 1,
        ) as InstallQuarantineResult.Quarantined
        val before = repository.observeOverview().first { it.rules.size == 2 }

        val late = repository.applyAgentResponse(
            eventId = quarantine.eventId,
            rawResponse = validResponse(quarantine.eventId, active.id),
            trustedNowWallMs = active.endsWallMs,
        )

        assertTrue(late is ApplyAgentResult.Rejected)
        val after = repository.observeOverview().first { it.rules.size == 2 }
        assertEquals(before.commitment, after.commitment)
        assertEquals(before.rules, after.rules)
        assertEquals(active.endsWallMs, after.commitment?.endsWallMs)
        assertEquals("LATE_RESULT", after.timeline.last().resultCode)
        assertEquals("TERMINAL_REJECTED", repository.getOutbox(quarantine.eventId)?.state)
        assertEquals(1, repository.countEvents(active.id, ProtectionEventType.VALIDATION_REJECTED))
    }

    @Test
    fun inFlightRowsRemainRecoverableAfterAWorkerCrashAndCanBeRetried() = runBlocking {
        val active = repository.activateDemoCommitment(quarantineNewInstalls = true)
        val quarantine = repository.quarantineInstalledPackage(
            packageName = LUCKYMIRROR_PACKAGE,
            label = "LuckyMirror Demo",
            versionCode = 1,
            firstInstallWallMs = active.startsWallMs + 1,
        ) as InstallQuarantineResult.Quarantined

        val firstAttempt = repository.prepareClassificationAttempt(quarantine.eventId)!!
        assertEquals(1, firstAttempt.attemptNumber)
        assertEquals(listOf(quarantine.eventId), repository.getRecoverableClassificationEventIds())

        val repairedAttempt = repository.prepareClassificationAttempt(quarantine.eventId)!!
        assertEquals(2, repairedAttempt.attemptNumber)
        assertTrue(repository.markClassificationRetry(quarantine.eventId))
        assertEquals("QUEUED", repository.getOutbox(quarantine.eventId)?.state)
        assertEquals(listOf(quarantine.eventId), repository.getRecoverableClassificationEventIds())
    }

    @Test
    fun monotonicClockRollbackCannotDelayTrustedExpiryOrDuplicateExpiryEvent() = runBlocking {
        var reading = ClockReading(wallMs = 1_000_000, elapsedMs = 10_000, bootCount = 7)
        val clockRepository = PolicyRepository(database, context().contentResolver) { reading }
        val active = clockRepository.activateDemoCommitment(quarantineNewInstalls = false)

        reading = ClockReading(wallMs = 500_000, elapsedMs = 70_000, bootCount = 7)
        clockRepository.refreshExpiry()
        assertEquals(CommitmentStatus.ACTIVE, clockRepository.getActiveCommitment()?.status)
        assertTrue(clockRepository.isPackageBlocked(com.histopgambling.looplock.domain.BETBURST_PACKAGE))

        reading = ClockReading(
            wallMs = 400_000,
            elapsedMs = active.startElapsedMs + 5 * 60 * 1000L + 1,
            bootCount = 7,
        )
        clockRepository.refreshExpiry()
        val expired = clockRepository.observeOverview().first { it.commitment?.status == CommitmentStatus.EXPIRED }

        assertEquals(active.endsWallMs, expired.commitment?.endsWallMs)
        assertFalse(clockRepository.isPackageBlocked(com.histopgambling.looplock.domain.BETBURST_PACKAGE))
        assertTrue(clockRepository.getActiveRulePackages().isEmpty())
        assertEquals(1, clockRepository.countEvents(active.id, ProtectionEventType.COMMITMENT_EXPIRED))

        clockRepository.refreshExpiry()
        assertEquals(1, clockRepository.countEvents(active.id, ProtectionEventType.COMMITMENT_EXPIRED))
    }

    @Test
    fun activeCommitmentAndRulesReloadAfterDatabaseReopen() = runBlocking {
        val context = context()
        val databaseName = "looplock-restart-${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)
        var persistentDatabase = Room.databaseBuilder(
            context,
            LoopLockDatabase::class.java,
            databaseName,
        ).addMigrations(
            LoopLockDatabase.MIGRATION_1_2,
            LoopLockDatabase.MIGRATION_2_3,
            LoopLockDatabase.MIGRATION_3_4,
        ).build()
        try {
            val beforeRestart = PolicyRepository(persistentDatabase, context.contentResolver)
            val active = beforeRestart.activateDemoCommitment(quarantineNewInstalls = true)
            persistentDatabase.close()

            persistentDatabase = Room.databaseBuilder(
                context,
                LoopLockDatabase::class.java,
                databaseName,
            ).addMigrations(
                LoopLockDatabase.MIGRATION_1_2,
                LoopLockDatabase.MIGRATION_2_3,
                LoopLockDatabase.MIGRATION_3_4,
            ).build()
            val afterRestart = PolicyRepository(persistentDatabase, context.contentResolver)

            assertEquals(active.id, afterRestart.getActiveCommitment()?.id)
            assertEquals(listOf(com.histopgambling.looplock.domain.BETBURST_PACKAGE), afterRestart.getActiveRulePackages())
        } finally {
            if (persistentDatabase.isOpen) persistentDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun validResponse(eventId: String, commitmentId: String): String =
        """
        {
          "schema_version": 1,
          "event_id": "$eventId",
          "commitment_id": "$commitmentId",
          "action": "TIGHTEN",
          "target_type": "PACKAGE",
          "target_value": "$LUCKYMIRROR_PACKAGE",
          "classification": "DEMO_GAMBLING_APP",
          "confidence": 0.98,
          "reason_code": "FIXTURE_MATCH",
          "reason": "The package metadata matches the harmless betting-demo fixture."
        }
        """.trimIndent()

    private fun reviewResponse(eventId: String, commitmentId: String): String =
        """
        {
          "schema_version": 1,
          "event_id": "$eventId",
          "commitment_id": "$commitmentId",
          "action": "REVIEW",
          "target_type": "PACKAGE",
          "target_value": "$LUCKYMIRROR_PACKAGE",
          "classification": "UNKNOWN",
          "confidence": 0.21,
          "reason_code": "NEEDS_REVIEW",
          "reason": "The package metadata needs review."
        }
        """.trimIndent()
}
