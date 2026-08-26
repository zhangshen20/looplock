package com.histopgambling.looplock

import com.histopgambling.looplock.domain.Commitment
import com.histopgambling.looplock.domain.CommitmentPolicyGuard
import com.histopgambling.looplock.domain.CommitmentStatus
import com.histopgambling.looplock.domain.GuardResult
import com.histopgambling.looplock.domain.PolicyMutation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitmentPolicyGuardTest {
    private val active = Commitment(
        id = "commitment",
        status = CommitmentStatus.ACTIVE,
        startsWallMs = 1_000,
        endsWallMs = 301_000,
        startElapsedMs = 500,
        bootCount = 4,
        quarantineNewInstalls = true,
        consentVersion = 1,
    )

    @Test
    fun additiveRuleMustUseUnchangedCommitmentEnd() {
        assertEquals(
            GuardResult.Accepted,
            CommitmentPolicyGuard.evaluate(active, PolicyMutation.AddRule("com.example.safe", active.endsWallMs)),
        )
        assertEquals(
            GuardResult.Rejected("EXPIRY_MISMATCH"),
            CommitmentPolicyGuard.evaluate(active, PolicyMutation.AddRule("com.example.safe", active.endsWallMs - 1)),
        )
    }

    @Test
    fun allWeakeningMutationsAreRejected() {
        val attempts = listOf(
            PolicyMutation.RemoveRule("com.example.target"),
            PolicyMutation.ShortenCommitment(active.endsWallMs - 1),
            PolicyMutation.DisableNewInstallQuarantine,
            PolicyMutation.AllowPackage("com.example.target"),
        )
        attempts.forEach { mutation ->
            assertTrue(CommitmentPolicyGuard.evaluate(active, mutation) is GuardResult.Rejected)
        }
    }
}

