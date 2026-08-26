package com.histopgambling.looplock.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class HttpClassificationTransportTest {
    private lateinit var server: MockWebServer
    private lateinit var transport: ClassificationTransport

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        transport = AgentTransportFactory.create(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun postSendsOnlyMinimalDtoAndUsesEventIdAsIdempotencyKey() = runBlocking {
        server.enqueue(jsonResponse(200, terminalResponse()))

        val result = transport.submit(upload)

        assertTrue(result is ClassificationTransportOutcome.Terminal)
        result as ClassificationTransportOutcome.Terminal
        assertEquals("TIGHTEN", result.proposal.action)
        assertEquals(PACKAGE_NAME, result.proposal.targetValue)
        assertEquals(TERMINAL_KEYS, Json.parseToJsonElement(result.contractJson).jsonObject.keys)

        val recorded = server.takeRequest()
        assertEquals("/v1/classifications", recorded.requestUrl?.encodedPath)
        assertEquals(EVENT_ID, recorded.headers["Idempotency-Key"])
        val rawBody = recorded.body.readUtf8()
        val body = Json.parseToJsonElement(rawBody).jsonObject
        assertEquals(setOf("schema_version", "event_id", "commitment_id", "target"), body.keys)
        assertEquals(
            setOf("type", "package_name", "label", "version_code"),
            body.getValue("target").jsonObject.keys,
        )
        assertFalse(rawBody.contains("contact", ignoreCase = true))
    }

    @Test
    fun postMapsProcessingConflictAndServiceFailure() = runBlocking {
        server.enqueue(jsonResponse(202, """{"status":"processing","event_id":"$EVENT_ID"}"""))
        server.enqueue(MockResponse().setResponseCode(409))
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(ClassificationTransportOutcome.Processing(EVENT_ID), transport.submit(upload))
        assertEquals(ClassificationTransportOutcome.PermanentConflict, transport.submit(upload))
        assertEquals(ClassificationTransportOutcome.Retryable, transport.submit(upload))
    }

    @Test
    fun postRejectsUnknownFieldsActionsAndTargetSubstitution() = runBlocking {
        server.enqueue(jsonResponse(200, terminalResponse(extra = ",\"unlock\":true")))
        server.enqueue(jsonResponse(200, terminalResponse(action = "UNLOCK")))
        server.enqueue(jsonResponse(200, terminalResponse(target = "com.example.other")))

        assertProtocolRejected(transport.submit(upload), ProtocolRejection.MALFORMED_RESPONSE)
        assertProtocolRejected(transport.submit(upload), ProtocolRejection.MALFORMED_RESPONSE)
        assertProtocolRejected(transport.submit(upload), ProtocolRejection.TARGET_MISMATCH)
    }

    @Test
    fun getBindsTerminalResultToLocalTargetAndFixedReasonCopy() = runBlocking {
        server.enqueue(
            jsonResponse(
                200,
                """{
                    "event_id":"$EVENT_ID",
                    "commitment_id":"$COMMITMENT_ID",
                    "status":"TIGHTEN",
                    "classification":"DEMO_GAMBLING_APP",
                    "confidence":0.98,
                    "reason_code":"FIXTURE_MATCH"
                }""".trimIndent(),
            ),
        )

        val result = transport.retrieve(binding)

        assertTrue(result is ClassificationTransportOutcome.Terminal)
        result as ClassificationTransportOutcome.Terminal
        assertEquals(PACKAGE_NAME, result.proposal.targetValue)
        assertEquals("The metadata matches the harmless LuckyMirror fixture.", result.proposal.reason)
        val canonical = Json.parseToJsonElement(result.contractJson).jsonObject
        assertEquals(TERMINAL_KEYS, canonical.keys)
        assertEquals(PACKAGE_NAME, canonical.getValue("target_value").jsonPrimitive.content)
        assertEquals("/v1/classifications/$EVENT_ID", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun getMapsProcessingNotFoundInvalidIdAndServiceFailure() = runBlocking {
        server.enqueue(
            jsonResponse(
                200,
                """{
                    "event_id":"$EVENT_ID",
                    "commitment_id":"$COMMITMENT_ID",
                    "status":"PROCESSING",
                    "classification":null,
                    "confidence":null,
                    "reason_code":null
                }""".trimIndent(),
            ),
        )
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(422))
        server.enqueue(MockResponse().setResponseCode(503))

        assertEquals(ClassificationTransportOutcome.Processing(EVENT_ID), transport.retrieve(binding))
        assertEquals(ClassificationTransportOutcome.NotFound, transport.retrieve(binding))
        assertEquals(ClassificationTransportOutcome.InvalidEventId, transport.retrieve(binding))
        assertEquals(ClassificationTransportOutcome.Retryable, transport.retrieve(binding))
    }

    @Test
    fun getRejectsCloudIdentityMismatchAndInvalidStatusSemantics() = runBlocking {
        server.enqueue(
            jsonResponse(
                200,
                """{
                    "event_id":"43e8d400-9452-43c3-9c79-0630fe1eeccd",
                    "commitment_id":"$COMMITMENT_ID",
                    "status":"REVIEW",
                    "classification":"UNKNOWN",
                    "confidence":0.2,
                    "reason_code":"NEEDS_REVIEW"
                }""".trimIndent(),
            ),
        )
        server.enqueue(
            jsonResponse(
                200,
                """{
                    "event_id":"$EVENT_ID",
                    "commitment_id":"$COMMITMENT_ID",
                    "status":"REVIEW",
                    "classification":"DEMO_GAMBLING_APP",
                    "confidence":0.98,
                    "reason_code":"FIXTURE_MATCH"
                }""".trimIndent(),
            ),
        )

        assertProtocolRejected(transport.retrieve(binding), ProtocolRejection.IDENTITY_MISMATCH)
        assertProtocolRejected(transport.retrieve(binding), ProtocolRejection.INVALID_SEMANTICS)
    }

    @Test
    fun closedLocalRequestRejectsNonMinimalInvalidIdentityAndMetadata() {
        assertTrue(runCatching { upload.copy(eventId = "not-a-uuid") }.isFailure)
        assertTrue(runCatching { upload.copy(packageName = "not a package") }.isFailure)
        assertTrue(runCatching { upload.copy(label = " ") }.isFailure)
        assertTrue(runCatching { upload.copy(versionCode = 0) }.isFailure)
    }

    @Test
    fun networkFailureIsRetryableAndCannotProduceATerminalRuleProposal() = runBlocking {
        val unavailable = AgentTransportFactory.create(
            baseUrl = "http://127.0.0.1:1/",
            client = OkHttpClient.Builder()
                .callTimeout(500, TimeUnit.MILLISECONDS)
                .build(),
        )

        assertEquals(ClassificationTransportOutcome.Retryable, unavailable.submit(upload))
        assertEquals(ClassificationTransportOutcome.Retryable, unavailable.retrieve(binding))
    }

    private fun assertProtocolRejected(
        outcome: ClassificationTransportOutcome,
        reason: ProtocolRejection,
    ) {
        assertEquals(ClassificationTransportOutcome.ProtocolRejected(reason), outcome)
    }

    private fun jsonResponse(code: Int, body: String) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun terminalResponse(
        action: String = "TIGHTEN",
        target: String = PACKAGE_NAME,
        extra: String = "",
    ) = """{
        "schema_version":1,
        "event_id":"$EVENT_ID",
        "commitment_id":"$COMMITMENT_ID",
        "action":"$action",
        "target_type":"PACKAGE",
        "target_value":"$target",
        "classification":"DEMO_GAMBLING_APP",
        "confidence":0.98,
        "reason_code":"FIXTURE_MATCH",
        "reason":"The metadata matches the harmless LuckyMirror fixture."
        $extra
    }""".trimIndent()

    private companion object {
        const val EVENT_ID = "84f4d0dd-1317-4f18-ad21-c3ab975b2f30"
        const val COMMITMENT_ID = "a5b36c91-352b-42cc-9c6a-f6ffdf12107e"
        const val PACKAGE_NAME = "com.histopgambling.fixture.luckymirror"
        val TERMINAL_KEYS = setOf(
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

        val upload = ClassificationUpload(
            eventId = EVENT_ID,
            commitmentId = COMMITMENT_ID,
            packageName = PACKAGE_NAME,
            label = "LuckyMirror Demo",
            versionCode = 1,
        )
        val binding = LocalClassificationBinding(
            eventId = EVENT_ID,
            commitmentId = COMMITMENT_ID,
            targetPackage = PACKAGE_NAME,
        )
    }
}
