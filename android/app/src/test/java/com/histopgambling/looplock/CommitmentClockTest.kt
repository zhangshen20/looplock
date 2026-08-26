package com.histopgambling.looplock

import com.histopgambling.looplock.domain.ClockReading
import com.histopgambling.looplock.domain.ClockStatus
import com.histopgambling.looplock.domain.Commitment
import com.histopgambling.looplock.domain.CommitmentClock
import com.histopgambling.looplock.domain.CommitmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitmentClockTest {
    private val active = Commitment(
        id = "commitment",
        status = CommitmentStatus.ACTIVE,
        startsWallMs = 1_000_000,
        endsWallMs = 1_300_000,
        startElapsedMs = 10_000,
        bootCount = 7,
        quarantineNewInstalls = true,
        consentVersion = 1,
    )

    @Test
    fun wallClockRollbackCannotExpireSameBootCommitmentEarly() {
        val result = CommitmentClock.evaluate(
            active,
            ClockReading(wallMs = 500_000, elapsedMs = 70_000, bootCount = 7),
        )
        assertEquals(ClockStatus.ACTIVE, result.status)
        assertEquals(240_000, result.remainingMs)
    }

    @Test
    fun elapsedTimeExpiresSameBootCommitment() {
        val result = CommitmentClock.evaluate(
            active,
            ClockReading(wallMs = 900_000, elapsedMs = 310_001, bootCount = 7),
        )
        assertEquals(ClockStatus.EXPIRED, result.status)
        assertEquals(0, result.remainingMs)
    }

    @Test
    fun rebootUsesExplicitFallbackStatus() {
        val result = CommitmentClock.evaluate(
            active,
            ClockReading(wallMs = 1_100_000, elapsedMs = 1_000, bootCount = 8),
        )
        assertEquals(ClockStatus.REBOOT_FALLBACK, result.status)
        assertTrue(result.remainingMs > 0)
    }
}

