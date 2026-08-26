package com.histopgambling.looplock.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AgentProposal(
    val schemaVersion: Int,
    val eventId: String,
    val commitmentId: String,
    val action: String,
    val targetType: String,
    val targetValue: String,
    val classification: String,
    val confidence: Double,
    val reasonCode: String,
    val reason: String,
)

data class ProposalLocalSnapshot(
    val eventId: String,
    val commitmentId: String,
    val commitmentStatus: CommitmentStatus,
    val commitmentEndsWallMs: Long,
    val quarantinedTargetPackage: String,
    val localRuleSource: RuleSource?,
    val terminalResultRecorded: Boolean,
)

enum class ProposalRejectionCode {
    MALFORMED_JSON,
    MISSING_FIELD,
    EXTRA_AUTHORITY_FIELD,
    UNSUPPORTED_SCHEMA,
    EVENT_MISMATCH,
    COMMITMENT_MISMATCH,
    COMMITMENT_NOT_ACTIVE,
    LATE_RESULT,
    TARGET_TYPE_NOT_ALLOWED,
    TARGET_MISMATCH,
    TARGET_NOT_QUARANTINED,
    DUPLICATE_TERMINAL_RESULT,
    ACTION_NOT_ALLOWED,
    INVALID_CLASSIFICATION,
    INVALID_CONFIDENCE,
    INVALID_REASON_CODE,
    INVALID_REASON,
}

sealed interface ProposalParseResult {
    data class Parsed(val proposal: AgentProposal) : ProposalParseResult
    data class Rejected(val reasonCode: ProposalRejectionCode) : ProposalParseResult
}

sealed interface ProposalValidationResult {
    data class Tighten(
        val targetPackage: String,
        val expiresWallMs: Long,
    ) : ProposalValidationResult

    data class Review(val reasonCode: String) : ProposalValidationResult

    data class Rejected(
        val reasonCode: ProposalRejectionCode,
        val safeOutcome: String = "REVIEW",
    ) : ProposalValidationResult
}

object AgentProposalJsonParser {
    private val requiredFields = setOf(
        "schema_version",
        "event_id",
        "commitment_id",
        "action",
        "target_type",
        "target_value",
        "classification",
        "confidence",
        "reason_code",
        "reason",
    )

    fun parse(raw: String): ProposalParseResult {
        val objectValue = runCatching { Json.parseToJsonElement(raw).jsonObject }
            .getOrElse { return ProposalParseResult.Rejected(ProposalRejectionCode.MALFORMED_JSON) }

        if (objectValue.keys.any { it !in requiredFields }) {
            return ProposalParseResult.Rejected(ProposalRejectionCode.EXTRA_AUTHORITY_FIELD)
        }
        if (objectValue.keys != requiredFields) {
            return ProposalParseResult.Rejected(ProposalRejectionCode.MISSING_FIELD)
        }

        val schemaVersion = objectValue.strictInt("schema_version")
            ?: return ProposalParseResult.Rejected(ProposalRejectionCode.MALFORMED_JSON)
        val confidence = objectValue.strictDouble("confidence")
            ?: return ProposalParseResult.Rejected(ProposalRejectionCode.MALFORMED_JSON)
        val strings = listOf(
            "event_id",
            "commitment_id",
            "action",
            "target_type",
            "target_value",
            "classification",
            "reason_code",
            "reason",
        ).associateWith { field ->
            objectValue.strictString(field)
                ?: return ProposalParseResult.Rejected(ProposalRejectionCode.MALFORMED_JSON)
        }

        return ProposalParseResult.Parsed(
            AgentProposal(
                schemaVersion = schemaVersion,
                eventId = strings.getValue("event_id"),
                commitmentId = strings.getValue("commitment_id"),
                action = strings.getValue("action"),
                targetType = strings.getValue("target_type"),
                targetValue = strings.getValue("target_value"),
                classification = strings.getValue("classification"),
                confidence = confidence,
                reasonCode = strings.getValue("reason_code"),
                reason = strings.getValue("reason"),
            ),
        )
    }

    private fun JsonObject.strictString(field: String): String? =
        (get(field) as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.strictInt(field: String): Int? =
        (get(field) as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

    private fun JsonObject.strictDouble(field: String): Double? =
        (get(field) as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull
}

object AgentProposalValidator {
    private val packagePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    private val classifications = setOf("DEMO_GAMBLING_APP", "UNKNOWN")
    private val reasonCodes = setOf(
        "FIXTURE_MATCH",
        "NEEDS_REVIEW",
        "MODEL_UNAVAILABLE",
        "INVALID_MODEL_OUTPUT",
    )

    fun validate(
        snapshot: ProposalLocalSnapshot,
        proposal: AgentProposal,
        trustedNowWallMs: Long,
    ): ProposalValidationResult {
        if (proposal.schemaVersion != SUPPORTED_SCHEMA_VERSION) return rejected(ProposalRejectionCode.UNSUPPORTED_SCHEMA)
        if (proposal.eventId != snapshot.eventId) return rejected(ProposalRejectionCode.EVENT_MISMATCH)
        if (proposal.commitmentId != snapshot.commitmentId) return rejected(ProposalRejectionCode.COMMITMENT_MISMATCH)
        if (snapshot.commitmentStatus != CommitmentStatus.ACTIVE) return rejected(ProposalRejectionCode.COMMITMENT_NOT_ACTIVE)
        if (trustedNowWallMs >= snapshot.commitmentEndsWallMs) return rejected(ProposalRejectionCode.LATE_RESULT)
        if (proposal.targetType != "PACKAGE") return rejected(ProposalRejectionCode.TARGET_TYPE_NOT_ALLOWED)
        if (!packagePattern.matches(proposal.targetValue) || proposal.targetValue != snapshot.quarantinedTargetPackage) {
            return rejected(ProposalRejectionCode.TARGET_MISMATCH)
        }
        if (proposal.action !in setOf("TIGHTEN", "REVIEW")) return rejected(ProposalRejectionCode.ACTION_NOT_ALLOWED)
        if (snapshot.localRuleSource != RuleSource.QUARANTINE) return rejected(ProposalRejectionCode.TARGET_NOT_QUARANTINED)
        if (snapshot.terminalResultRecorded) return rejected(ProposalRejectionCode.DUPLICATE_TERMINAL_RESULT)
        if (proposal.classification !in classifications) return rejected(ProposalRejectionCode.INVALID_CLASSIFICATION)
        if (!proposal.confidence.isFinite() || proposal.confidence !in 0.0..1.0) {
            return rejected(ProposalRejectionCode.INVALID_CONFIDENCE)
        }
        if (proposal.reasonCode !in reasonCodes) return rejected(ProposalRejectionCode.INVALID_REASON_CODE)
        if (proposal.reason.isBlank() || proposal.reason.length > MAX_REASON_LENGTH) {
            return rejected(ProposalRejectionCode.INVALID_REASON)
        }

        return when (proposal.action) {
            "TIGHTEN" -> {
                if (proposal.classification != "DEMO_GAMBLING_APP" || proposal.reasonCode != "FIXTURE_MATCH") {
                    rejected(ProposalRejectionCode.INVALID_CLASSIFICATION)
                } else {
                    ProposalValidationResult.Tighten(
                        targetPackage = snapshot.quarantinedTargetPackage,
                        expiresWallMs = snapshot.commitmentEndsWallMs,
                    )
                }
            }
            "REVIEW" -> {
                if (proposal.classification != "UNKNOWN" || proposal.reasonCode == "FIXTURE_MATCH") {
                    rejected(ProposalRejectionCode.INVALID_CLASSIFICATION)
                } else {
                    ProposalValidationResult.Review(proposal.reasonCode)
                }
            }
            else -> error("Action closure checked above")
        }
    }

    fun parseAndValidate(
        snapshot: ProposalLocalSnapshot,
        raw: String,
        trustedNowWallMs: Long,
    ): ProposalValidationResult = when (val parsed = AgentProposalJsonParser.parse(raw)) {
        is ProposalParseResult.Parsed -> validate(snapshot, parsed.proposal, trustedNowWallMs)
        is ProposalParseResult.Rejected -> rejected(parsed.reasonCode)
    }

    private fun rejected(code: ProposalRejectionCode) = ProposalValidationResult.Rejected(code)

    private const val SUPPORTED_SCHEMA_VERSION = 1
    private const val MAX_REASON_LENGTH = 240
}
