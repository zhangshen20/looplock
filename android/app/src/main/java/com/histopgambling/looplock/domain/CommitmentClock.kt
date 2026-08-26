package com.histopgambling.looplock.domain

data class ClockReading(
    val wallMs: Long,
    val elapsedMs: Long,
    val bootCount: Int,
)

enum class ClockStatus { ACTIVE, EXPIRED, REBOOT_FALLBACK, INCONSISTENT }

data class ClockResult(
    val status: ClockStatus,
    val remainingMs: Long,
)

object CommitmentClock {
    fun evaluate(commitment: Commitment, reading: ClockReading): ClockResult {
        if (commitment.status == CommitmentStatus.EXPIRED) {
            return ClockResult(ClockStatus.EXPIRED, 0)
        }

        if (reading.bootCount == commitment.bootCount && reading.elapsedMs >= commitment.startElapsedMs) {
            val durationMs = commitment.endsWallMs - commitment.startsWallMs
            val remaining = durationMs - (reading.elapsedMs - commitment.startElapsedMs)
            return if (remaining <= 0) {
                ClockResult(ClockStatus.EXPIRED, 0)
            } else {
                ClockResult(ClockStatus.ACTIVE, remaining)
            }
        }

        if (reading.bootCount != commitment.bootCount) {
            val remaining = commitment.endsWallMs - reading.wallMs
            return if (remaining <= 0) {
                ClockResult(ClockStatus.EXPIRED, 0)
            } else {
                ClockResult(ClockStatus.REBOOT_FALLBACK, remaining)
            }
        }

        return ClockResult(
            status = ClockStatus.INCONSISTENT,
            remainingMs = (commitment.endsWallMs - reading.wallMs).coerceAtLeast(1),
        )
    }
}

