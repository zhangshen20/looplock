package com.histopgambling.looplock

import com.histopgambling.looplock.domain.AccountabilityPreviewFactory
import com.histopgambling.looplock.domain.Commitment
import com.histopgambling.looplock.domain.CommitmentOverview
import com.histopgambling.looplock.domain.CommitmentStatus
import com.histopgambling.looplock.domain.ProtectionEventType
import com.histopgambling.looplock.domain.ProtectionStatusLabel
import com.histopgambling.looplock.domain.ProtectionStatusResolver
import com.histopgambling.looplock.domain.TimelineEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SafetyUxTest {
    private val active = Commitment(
        id = "commitment",
        status = CommitmentStatus.ACTIVE,
        startsWallMs = 1_000,
        endsWallMs = 301_000,
        startElapsedMs = 10_000,
        bootCount = 7,
        quarantineNewInstalls = true,
        consentVersion = 1,
    )

    @Test
    fun protectionStatusNeverClaimsProtectedWhenAccessibilityIsOff() {
        assertEquals(ProtectionStatusLabel.ACTION_REQUIRED, ProtectionStatusResolver.resolve(active, false))
        assertEquals(ProtectionStatusLabel.PROTECTED, ProtectionStatusResolver.resolve(active, true))
        assertEquals(
            ProtectionStatusLabel.EXPIRED,
            ProtectionStatusResolver.resolve(active.copy(status = CommitmentStatus.EXPIRED), true),
        )
    }

    @Test
    fun accountabilityPreviewContainsOnlyBoundedNonTargetFacts() {
        val overview = CommitmentOverview(
            commitment = active,
            rules = emptyList(),
            timeline = listOf(
                event("one", ProtectionEventType.BLOCK_ATTEMPT),
                event("two", ProtectionEventType.BLOCK_ATTEMPT),
                event("three", ProtectionEventType.AGENT_RESULT),
            ),
        )

        val preview = AccountabilityPreviewFactory.create(overview, 90_000)!!

        assertEquals(2, preview.attemptCount)
        assertEquals(1_000, preview.windowStartWallMs)
        assertEquals(90_000, preview.windowEndWallMs)
        assertEquals("LEVEL_1", preview.escalationLevel)
        val fieldNames = preview::class.java.declaredFields.map { it.name }
        assertFalse(fieldNames.any { it.contains("package", true) || it.contains("target", true) || it.contains("destination", true) })
    }

    private fun event(id: String, type: ProtectionEventType) = TimelineEntry(
        eventId = id,
        type = type,
        resultCode = "LOCAL",
        uploadState = "LOCAL_ONLY",
        createdWallMs = 2_000,
    )
}
