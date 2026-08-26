package com.histopgambling.looplock.domain

const val BETBURST_PACKAGE = "com.histopgambling.fixture.betburst"
const val LUCKYMIRROR_PACKAGE = "com.histopgambling.fixture.luckymirror"
const val DEMO_DURATION_MS = 5 * 60 * 1000L
const val CONSENT_VERSION = 1

enum class CommitmentStatus { DRAFT, ACTIVE, EXPIRED }

enum class RuleSource { USER_SELECTED, QUARANTINE, AGENT_TIGHTENED }

enum class ProtectionEventType {
    BLOCK_ATTEMPT,
    PACKAGE_ADDED,
    AGENT_RESULT,
    VALIDATION_REJECTED,
    SERVICE_STATE,
    COMMITMENT_EXPIRED,
}

data class Commitment(
    val id: String,
    val status: CommitmentStatus,
    val startsWallMs: Long,
    val endsWallMs: Long,
    val startElapsedMs: Long,
    val bootCount: Int,
    val quarantineNewInstalls: Boolean,
    val consentVersion: Int,
)

data class CommitmentOverview(
    val commitment: Commitment?,
    val rules: List<RuleSummary>,
    val timeline: List<TimelineEntry> = emptyList(),
)

data class RuleSummary(
    val packageName: String,
    val source: RuleSource,
    val expiresWallMs: Long,
)

data class TimelineEntry(
    val eventId: String,
    val type: ProtectionEventType,
    val resultCode: String,
    val uploadState: String,
    val createdWallMs: Long,
)

sealed interface InstallQuarantineResult {
    data class Quarantined(val eventId: String, val ruleCreated: Boolean) : InstallQuarantineResult
    data object CommitmentInactive : InstallQuarantineResult
    data object NotAuthorized : InstallQuarantineResult
    data object NotTargetPackage : InstallQuarantineResult
    data object InstalledBeforeCommitment : InstallQuarantineResult
    data object DuplicateSignal : InstallQuarantineResult
    data object AlreadyTightened : InstallQuarantineResult
}

sealed interface UnlockFixtureResult {
    data class Rejected(
        val reasonCode: ProposalRejectionCode,
        val commitmentEndsWallMs: Long,
    ) : UnlockFixtureResult

    data object NotReady : UnlockFixtureResult
}

data class PendingClassification(
    val eventId: String,
    val commitmentId: String,
    val targetPackage: String,
    val targetLabel: String,
    val targetVersionCode: Long,
    val targetHash: String,
    val attemptNumber: Int,
)

sealed interface ApplyAgentResult {
    data class Tightened(
        val targetHash: String,
        val commitmentEndsWallMs: Long,
    ) : ApplyAgentResult

    data class Review(val reasonCode: String) : ApplyAgentResult

    data class Rejected(val reasonCode: ProposalRejectionCode) : ApplyAgentResult

    data class Duplicate(val terminalState: String) : ApplyAgentResult

    data object NotFound : ApplyAgentResult
}
