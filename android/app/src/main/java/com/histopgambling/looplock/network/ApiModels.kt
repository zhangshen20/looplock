package com.histopgambling.looplock.network

import com.histopgambling.looplock.domain.AgentProposal
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** The only raw metadata the Android demo may send to the bounded classifier. */
data class ClassificationUpload(
    val eventId: String,
    val commitmentId: String,
    val packageName: String,
    val label: String,
    val versionCode: Long,
) {
    init {
        require(isUuid(eventId)) { "eventId must be a UUID" }
        require(isUuid(commitmentId)) { "commitmentId must be a UUID" }
        require(PACKAGE_PATTERN.matches(packageName)) { "packageName is invalid" }
        require(label.isNotBlank() && label.length <= MAX_LABEL_LENGTH) { "label is invalid" }
        require(versionCode >= 1) { "versionCode must be positive" }
    }
}

/** Local identity used to bind target-free GET results back to the quarantined target. */
data class LocalClassificationBinding(
    val eventId: String,
    val commitmentId: String,
    val targetPackage: String,
) {
    init {
        require(isUuid(eventId)) { "eventId must be a UUID" }
        require(isUuid(commitmentId)) { "commitmentId must be a UUID" }
        require(PACKAGE_PATTERN.matches(targetPackage)) { "targetPackage is invalid" }
    }
}

sealed interface ClassificationTransportOutcome {
    /** A proposal only. The local monotonic validator remains the sole policy authority. */
    data class Terminal(
        val proposal: AgentProposal,
        /** Canonical closed JSON for the repository's existing local validator boundary. */
        val contractJson: String,
    ) : ClassificationTransportOutcome

    data class Processing(val eventId: String) : ClassificationTransportOutcome

    data object Retryable : ClassificationTransportOutcome

    data object PermanentConflict : ClassificationTransportOutcome

    data object NotFound : ClassificationTransportOutcome

    data object InvalidEventId : ClassificationTransportOutcome

    data class ProtocolRejected(val reason: ProtocolRejection) : ClassificationTransportOutcome
}

enum class ProtocolRejection {
    EMPTY_RESPONSE,
    MALFORMED_RESPONSE,
    IDENTITY_MISMATCH,
    TARGET_MISMATCH,
    INVALID_SEMANTICS,
    UNEXPECTED_HTTP_STATUS,
}

@Serializable
internal data class ClassificationRequestDto(
    @SerialName("schema_version") val schemaVersion: Int = SCHEMA_VERSION,
    @SerialName("event_id") val eventId: String,
    @SerialName("commitment_id") val commitmentId: String,
    val target: ClassificationTargetDto,
)

@Serializable
internal data class ClassificationTargetDto(
    val type: TargetType = TargetType.PACKAGE,
    @SerialName("package_name") val packageName: String,
    val label: String,
    @SerialName("version_code") val versionCode: Long,
)

@Serializable
internal data class TerminalResponseDto(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("event_id") val eventId: String,
    @SerialName("commitment_id") val commitmentId: String,
    val action: AgentAction,
    @SerialName("target_type") val targetType: TargetType,
    @SerialName("target_value") val targetValue: String,
    val classification: AgentClassification,
    val confidence: Double,
    @SerialName("reason_code") val reasonCode: AgentReasonCode,
    val reason: String,
)

@Serializable
internal data class ProcessingResponseDto(
    val status: ProcessingStatus,
    @SerialName("event_id") val eventId: String,
)

@Serializable
internal data class StatusResponseDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("commitment_id") val commitmentId: String,
    val status: AgentStatus,
    val classification: AgentClassification? = null,
    val confidence: Double? = null,
    @SerialName("reason_code") val reasonCode: AgentReasonCode? = null,
)

@Serializable
internal enum class AgentAction {
    TIGHTEN,
    REVIEW,
}

@Serializable
internal enum class AgentClassification {
    DEMO_GAMBLING_APP,
    UNKNOWN,
}

@Serializable
internal enum class AgentReasonCode {
    FIXTURE_MATCH,
    NEEDS_REVIEW,
    MODEL_UNAVAILABLE,
    INVALID_MODEL_OUTPUT,
}

@Serializable
internal enum class TargetType {
    PACKAGE,
}

@Serializable
internal enum class ProcessingStatus {
    @SerialName("processing")
    PROCESSING,
}

@Serializable
internal enum class AgentStatus {
    PROCESSING,
    TIGHTEN,
    REVIEW,
}

internal fun ClassificationUpload.toRequestDto() = ClassificationRequestDto(
    eventId = eventId,
    commitmentId = commitmentId,
    target = ClassificationTargetDto(
        packageName = packageName,
        label = label,
        versionCode = versionCode,
    ),
)

internal fun TerminalResponseDto.toProposal(
    binding: LocalClassificationBinding,
): MappingResult {
    if (schemaVersion != SCHEMA_VERSION) return MappingResult.Rejected(ProtocolRejection.INVALID_SEMANTICS)
    if (eventId != binding.eventId || commitmentId != binding.commitmentId) {
        return MappingResult.Rejected(ProtocolRejection.IDENTITY_MISMATCH)
    }
    if (targetType != TargetType.PACKAGE || targetValue != binding.targetPackage) {
        return MappingResult.Rejected(ProtocolRejection.TARGET_MISMATCH)
    }
    if (!validTerminalSemantics(action, classification, confidence, reasonCode, reason)) {
        return MappingResult.Rejected(ProtocolRejection.INVALID_SEMANTICS)
    }
    return MappingResult.Valid(
        AgentProposal(
            schemaVersion = schemaVersion,
            eventId = eventId,
            commitmentId = commitmentId,
            action = action.name,
            targetType = targetType.name,
            targetValue = binding.targetPackage,
            classification = classification.name,
            confidence = confidence,
            reasonCode = reasonCode.name,
            reason = reason,
        ),
    )
}

internal fun StatusResponseDto.toOutcome(
    binding: LocalClassificationBinding,
): ClassificationTransportOutcome {
    if (eventId != binding.eventId || commitmentId != binding.commitmentId) {
        return ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.IDENTITY_MISMATCH)
    }
    if (status == AgentStatus.PROCESSING) {
        return if (classification == null && confidence == null && reasonCode == null) {
            ClassificationTransportOutcome.Processing(eventId)
        } else {
            ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.INVALID_SEMANTICS)
        }
    }

    val terminalClassification = classification
        ?: return ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.INVALID_SEMANTICS)
    val terminalConfidence = confidence
        ?: return ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.INVALID_SEMANTICS)
    val terminalReasonCode = reasonCode
        ?: return ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.INVALID_SEMANTICS)
    val action = when (status) {
        AgentStatus.TIGHTEN -> AgentAction.TIGHTEN
        AgentStatus.REVIEW -> AgentAction.REVIEW
        AgentStatus.PROCESSING -> error("handled above")
    }
    val fixedReason = FIXED_REASON_COPY.getValue(terminalReasonCode)
    if (!validTerminalSemantics(action, terminalClassification, terminalConfidence, terminalReasonCode, fixedReason)) {
        return ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.INVALID_SEMANTICS)
    }
    val proposal = AgentProposal(
        schemaVersion = SCHEMA_VERSION,
        eventId = binding.eventId,
        commitmentId = binding.commitmentId,
        action = action.name,
        targetType = TargetType.PACKAGE.name,
        targetValue = binding.targetPackage,
        classification = terminalClassification.name,
        confidence = terminalConfidence,
        reasonCode = terminalReasonCode.name,
        reason = fixedReason,
    )
    return ClassificationTransportOutcome.Terminal(
        proposal = proposal,
        contractJson = STRICT_JSON.encodeToString(proposal.toResponseDto()),
    )
}

internal sealed interface MappingResult {
    data class Valid(val proposal: AgentProposal) : MappingResult
    data class Rejected(val reason: ProtocolRejection) : MappingResult
}

internal fun AgentProposal.toResponseDto() = TerminalResponseDto(
    schemaVersion = schemaVersion,
    eventId = eventId,
    commitmentId = commitmentId,
    action = AgentAction.valueOf(action),
    targetType = TargetType.valueOf(targetType),
    targetValue = targetValue,
    classification = AgentClassification.valueOf(classification),
    confidence = confidence,
    reasonCode = AgentReasonCode.valueOf(reasonCode),
    reason = reason,
)

private fun validTerminalSemantics(
    action: AgentAction,
    classification: AgentClassification,
    confidence: Double,
    reasonCode: AgentReasonCode,
    reason: String,
): Boolean {
    if (!confidence.isFinite() || confidence !in 0.0..1.0) return false
    if (reason.isBlank() || reason.length > MAX_REASON_LENGTH) return false
    return when (action) {
        AgentAction.TIGHTEN ->
            classification == AgentClassification.DEMO_GAMBLING_APP && reasonCode == AgentReasonCode.FIXTURE_MATCH
        AgentAction.REVIEW ->
            classification == AgentClassification.UNKNOWN && reasonCode != AgentReasonCode.FIXTURE_MATCH
    }
}

private fun isUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess

private val PACKAGE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
private const val SCHEMA_VERSION = 1
private const val MAX_LABEL_LENGTH = 80
private const val MAX_REASON_LENGTH = 240

private val FIXED_REASON_COPY = mapOf(
    AgentReasonCode.FIXTURE_MATCH to "The metadata matches the harmless LuckyMirror fixture.",
    AgentReasonCode.NEEDS_REVIEW to "The metadata does not exactly match the harmless demo fixture.",
    AgentReasonCode.MODEL_UNAVAILABLE to "The classifier was unavailable; quarantine remains.",
    AgentReasonCode.INVALID_MODEL_OUTPUT to "The classifier did not return a valid bounded result.",
)
