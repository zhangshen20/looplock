package com.histopgambling.looplock

import com.histopgambling.looplock.domain.AgentProposal
import com.histopgambling.looplock.domain.AgentProposalJsonParser
import com.histopgambling.looplock.domain.AgentProposalValidator
import com.histopgambling.looplock.domain.CommitmentStatus
import com.histopgambling.looplock.domain.ProposalLocalSnapshot
import com.histopgambling.looplock.domain.ProposalParseResult
import com.histopgambling.looplock.domain.ProposalRejectionCode
import com.histopgambling.looplock.domain.ProposalValidationResult
import com.histopgambling.looplock.domain.RuleSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentProposalValidatorTest {
    private val eventId = "84f4d0dd-1317-4f18-ad21-c3ab975b2f30"
    private val commitmentId = "a5b36c91-352b-42cc-9c6a-f6ffdf12107e"
    private val packageName = "com.histopgambling.fixture.luckymirror"
    private val endWallMs = 301_000L
    private val nowWallMs = 2_000L

    private val snapshot = ProposalLocalSnapshot(
        eventId = eventId,
        commitmentId = commitmentId,
        commitmentStatus = CommitmentStatus.ACTIVE,
        commitmentEndsWallMs = endWallMs,
        quarantinedTargetPackage = packageName,
        localRuleSource = RuleSource.QUARANTINE,
        terminalResultRecorded = false,
    )

    private val valid = AgentProposal(
        schemaVersion = 1,
        eventId = eventId,
        commitmentId = commitmentId,
        action = "TIGHTEN",
        targetType = "PACKAGE",
        targetValue = packageName,
        classification = "DEMO_GAMBLING_APP",
        confidence = 0.98,
        reasonCode = "FIXTURE_MATCH",
        reason = "The package metadata matches the harmless betting-demo fixture.",
    )

    @Test
    fun validTightenUsesOnlyTheUnchangedLocalCommitmentEnd() {
        val result = AgentProposalValidator.validate(snapshot, valid, nowWallMs)

        assertEquals(
            ProposalValidationResult.Tighten(packageName, endWallMs),
            result,
        )
    }

    @Test
    fun validReviewRetainsQuarantine() {
        val result = AgentProposalValidator.validate(
            snapshot,
            valid.copy(
                action = "REVIEW",
                classification = "UNKNOWN",
                confidence = 0.21,
                reasonCode = "NEEDS_REVIEW",
            ),
            nowWallMs,
        )

        assertEquals(ProposalValidationResult.Review("NEEDS_REVIEW"), result)
    }

    @Test
    fun frozenUnlockAndExpiryFixturesAreRejectedSafely() {
        val unlock = AgentProposalValidator.parseAndValidate(
            snapshot,
            load("fixtures/invalid-unlock-response.json"),
            nowWallMs,
        )
        val expiry = AgentProposalValidator.parseAndValidate(
            snapshot,
            load("fixtures/invalid-expiry-response.json"),
            nowWallMs,
        )

        assertRejected(unlock, ProposalRejectionCode.ACTION_NOT_ALLOWED)
        assertRejected(expiry, ProposalRejectionCode.EXTRA_AUTHORITY_FIELD)
    }

    @Test
    fun everyWeakeningActionIsRejected() {
        listOf("ALLOW", "UNLOCK", "DELETE", "DISABLE", "REMOVE", "SHORTEN").forEach { action ->
            assertRejected(
                AgentProposalValidator.validate(snapshot, valid.copy(action = action), nowWallMs),
                ProposalRejectionCode.ACTION_NOT_ALLOWED,
            )
        }
    }

    @Test
    fun weakeningActionIsRejectedAfterTheRuleWasAlreadyTightened() {
        val completed = snapshot.copy(
            localRuleSource = RuleSource.AGENT_TIGHTENED,
            terminalResultRecorded = true,
        )

        assertRejected(
            AgentProposalValidator.validate(completed, valid.copy(action = "UNLOCK"), nowWallMs),
            ProposalRejectionCode.ACTION_NOT_ALLOWED,
        )
    }

    @Test
    fun malformedMissingWrongTypeAndExtraAuthorityFieldsFailClosed() {
        assertEquals(
            ProposalParseResult.Rejected(ProposalRejectionCode.MALFORMED_JSON),
            AgentProposalJsonParser.parse("not-json"),
        )
        assertEquals(
            ProposalParseResult.Rejected(ProposalRejectionCode.MISSING_FIELD),
            AgentProposalJsonParser.parse(
                load("fixtures/valid-tighten-response.json")
                    .replace(",\n  \"reason\": \"The package metadata matches the harmless betting-demo fixture.\"", ""),
            ),
        )
        assertEquals(
            ProposalParseResult.Rejected(ProposalRejectionCode.MALFORMED_JSON),
            AgentProposalJsonParser.parse(load("fixtures/valid-tighten-response.json").replace("\"schema_version\": 1", "\"schema_version\": \"1\"")),
        )
        listOf("expires_at", "allow", "delete", "disable", "new_end_wall_ms").forEach { field ->
            val withAuthority = load("fixtures/valid-tighten-response.json")
                .replace("\n}", ",\n  \"$field\": 1\n}")
            assertEquals(
                ProposalParseResult.Rejected(ProposalRejectionCode.EXTRA_AUTHORITY_FIELD),
                AgentProposalJsonParser.parse(withAuthority),
            )
        }
    }

    @Test
    fun lateInactiveDuplicateAndNonQuarantinedResultsFailClosed() {
        assertRejected(
            AgentProposalValidator.validate(snapshot, valid, endWallMs),
            ProposalRejectionCode.LATE_RESULT,
        )
        assertRejected(
            AgentProposalValidator.validate(snapshot.copy(commitmentStatus = CommitmentStatus.EXPIRED), valid, nowWallMs),
            ProposalRejectionCode.COMMITMENT_NOT_ACTIVE,
        )
        assertRejected(
            AgentProposalValidator.validate(snapshot.copy(terminalResultRecorded = true), valid, nowWallMs),
            ProposalRejectionCode.DUPLICATE_TERMINAL_RESULT,
        )
        listOf(null, RuleSource.USER_SELECTED, RuleSource.AGENT_TIGHTENED).forEach { source ->
            assertRejected(
                AgentProposalValidator.validate(snapshot.copy(localRuleSource = source), valid, nowWallMs),
                ProposalRejectionCode.TARGET_NOT_QUARANTINED,
            )
        }
    }

    @Test
    fun mismatchedEventCommitmentTargetAndTypeAreRejected() {
        repeat(25) { index ->
            assertRejected(
                AgentProposalValidator.validate(snapshot, valid.copy(eventId = "wrong-event-$index"), nowWallMs),
                ProposalRejectionCode.EVENT_MISMATCH,
            )
            assertRejected(
                AgentProposalValidator.validate(snapshot, valid.copy(commitmentId = "wrong-commitment-$index"), nowWallMs),
                ProposalRejectionCode.COMMITMENT_MISMATCH,
            )
            assertRejected(
                AgentProposalValidator.validate(snapshot, valid.copy(targetValue = "com.example.other$index"), nowWallMs),
                ProposalRejectionCode.TARGET_MISMATCH,
            )
        }
        assertRejected(
            AgentProposalValidator.validate(snapshot, valid.copy(targetType = "DOMAIN"), nowWallMs),
            ProposalRejectionCode.TARGET_TYPE_NOT_ALLOWED,
        )
    }

    @Test
    fun invalidSchemaConfidenceClassificationAndReasonAreRejected() {
        assertRejected(
            AgentProposalValidator.validate(snapshot, valid.copy(schemaVersion = 2), nowWallMs),
            ProposalRejectionCode.UNSUPPORTED_SCHEMA,
        )
        listOf(-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY).forEach { confidence ->
            assertRejected(
                AgentProposalValidator.validate(snapshot, valid.copy(confidence = confidence), nowWallMs),
                ProposalRejectionCode.INVALID_CONFIDENCE,
            )
        }
        assertRejected(
            AgentProposalValidator.validate(snapshot, valid.copy(classification = "LEGITIMATE"), nowWallMs),
            ProposalRejectionCode.INVALID_CLASSIFICATION,
        )
        assertRejected(
            AgentProposalValidator.validate(snapshot, valid.copy(action = "TIGHTEN", classification = "UNKNOWN"), nowWallMs),
            ProposalRejectionCode.INVALID_CLASSIFICATION,
        )
        assertRejected(
            AgentProposalValidator.validate(
                snapshot,
                valid.copy(action = "REVIEW", classification = "DEMO_GAMBLING_APP", reasonCode = "NEEDS_REVIEW"),
                nowWallMs,
            ),
            ProposalRejectionCode.INVALID_CLASSIFICATION,
        )
        assertRejected(
            AgentProposalValidator.validate(
                snapshot,
                valid.copy(action = "REVIEW", classification = "UNKNOWN", reasonCode = "FIXTURE_MATCH"),
                nowWallMs,
            ),
            ProposalRejectionCode.INVALID_CLASSIFICATION,
        )
        assertRejected(
            AgentProposalValidator.validate(snapshot, valid.copy(reasonCode = "MODEL_SAYS_UNLOCK"), nowWallMs),
            ProposalRejectionCode.INVALID_REASON_CODE,
        )
        assertRejected(
            AgentProposalValidator.validate(snapshot, valid.copy(reason = " "), nowWallMs),
            ProposalRejectionCode.INVALID_REASON,
        )
    }

    private fun assertRejected(result: ProposalValidationResult, code: ProposalRejectionCode) {
        assertTrue(result is ProposalValidationResult.Rejected)
        result as ProposalValidationResult.Rejected
        assertEquals(code, result.reasonCode)
        assertEquals("REVIEW", result.safeOutcome)
    }

    private fun load(path: String): String =
        checkNotNull(javaClass.classLoader?.getResource(path)) { "Missing contract fixture: $path" }
            .readText()
}
