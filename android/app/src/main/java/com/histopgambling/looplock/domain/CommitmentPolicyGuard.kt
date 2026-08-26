package com.histopgambling.looplock.domain

sealed interface PolicyMutation {
    data class AddRule(val packageName: String, val expiresWallMs: Long) : PolicyMutation
    data class RemoveRule(val packageName: String) : PolicyMutation
    data class ShortenCommitment(val proposedEndWallMs: Long) : PolicyMutation
    data object DisableNewInstallQuarantine : PolicyMutation
    data class AllowPackage(val packageName: String) : PolicyMutation
}

sealed interface GuardResult {
    data object Accepted : GuardResult
    data class Rejected(val reasonCode: String) : GuardResult
}

object CommitmentPolicyGuard {
    fun evaluate(commitment: Commitment, mutation: PolicyMutation): GuardResult {
        if (commitment.status != CommitmentStatus.ACTIVE) {
            return GuardResult.Rejected("COMMITMENT_NOT_ACTIVE")
        }
        return when (mutation) {
            is PolicyMutation.AddRule ->
                if (mutation.expiresWallMs == commitment.endsWallMs) {
                    GuardResult.Accepted
                } else {
                    GuardResult.Rejected("EXPIRY_MISMATCH")
                }
            is PolicyMutation.RemoveRule -> GuardResult.Rejected("REMOVE_FORBIDDEN")
            is PolicyMutation.ShortenCommitment -> GuardResult.Rejected("SHORTEN_FORBIDDEN")
            PolicyMutation.DisableNewInstallQuarantine -> GuardResult.Rejected("QUARANTINE_WEAKENING_FORBIDDEN")
            is PolicyMutation.AllowPackage -> GuardResult.Rejected("ALLOW_FORBIDDEN")
        }
    }
}

