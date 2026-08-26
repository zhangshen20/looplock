package com.histopgambling.looplock.domain

enum class ProtectionStatusLabel(val displayName: String) {
    NOT_ACTIVE("Not active"),
    PROTECTED("Protected"),
    ACTION_REQUIRED("Action required"),
    EXPIRED("Expired"),
}

object ProtectionStatusResolver {
    fun resolve(commitment: Commitment?, accessibilityEnabled: Boolean): ProtectionStatusLabel = when {
        commitment == null -> ProtectionStatusLabel.NOT_ACTIVE
        commitment.status == CommitmentStatus.EXPIRED -> ProtectionStatusLabel.EXPIRED
        accessibilityEnabled -> ProtectionStatusLabel.PROTECTED
        else -> ProtectionStatusLabel.ACTION_REQUIRED
    }
}

/** Closed, local-only preview. It deliberately has no target, package, or destination field. */
data class AccountabilityPreview(
    val attemptCount: Int,
    val windowStartWallMs: Long,
    val windowEndWallMs: Long,
    val escalationLevel: String,
)

object AccountabilityPreviewFactory {
    fun create(overview: CommitmentOverview, nowWallMs: Long): AccountabilityPreview? {
        val commitment = overview.commitment ?: return null
        val attemptCount = overview.timeline.count { it.type == ProtectionEventType.BLOCK_ATTEMPT }
        return AccountabilityPreview(
            attemptCount = attemptCount,
            windowStartWallMs = commitment.startsWallMs,
            windowEndWallMs = nowWallMs.coerceIn(commitment.startsWallMs, commitment.endsWallMs),
            escalationLevel = when {
                attemptCount == 0 -> "NONE"
                attemptCount < 3 -> "LEVEL_1"
                else -> "LEVEL_2"
            },
        )
    }
}
