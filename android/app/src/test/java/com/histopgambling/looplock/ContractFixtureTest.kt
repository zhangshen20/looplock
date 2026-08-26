package com.histopgambling.looplock

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractFixtureTest {
    private val json = Json
    private val responseKeys = setOf(
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

    @Test
    fun validGoldenResponsesAreAccepted() {
        assertTrue(isClosedResponse(load("fixtures/valid-tighten-response.json")))
        assertTrue(isClosedResponse(load("fixtures/valid-review-response.json")))
    }

    @Test
    fun weakeningAndAuthorityExpandingResponsesAreRejected() {
        assertFalse(isClosedResponse(load("fixtures/invalid-unlock-response.json")))
        assertFalse(isClosedResponse(load("fixtures/invalid-expiry-response.json")))
    }

    @Test
    fun validGoldenRequestUsesOnlyTheFrozenFields() {
        val request = Json.parseToJsonElement(load("fixtures/valid-request.json")).jsonObject
        assertTrue(request.keys == setOf("schema_version", "event_id", "commitment_id", "target"))
        assertTrue(
            request.getValue("target").jsonObject.keys ==
                setOf("type", "package_name", "label", "version_code"),
        )
    }

    private fun isClosedResponse(raw: String): Boolean {
        val response = Json.parseToJsonElement(raw).jsonObject
        val action = response["action"]?.jsonPrimitive?.content
        return response.keys == responseKeys && action in setOf("TIGHTEN", "REVIEW")
    }

    private fun load(path: String): String =
        checkNotNull(javaClass.classLoader?.getResource(path)) { "Missing contract fixture: $path" }
            .readText()
}

