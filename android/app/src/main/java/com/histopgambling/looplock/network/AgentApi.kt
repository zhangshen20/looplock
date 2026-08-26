package com.histopgambling.looplock.network

import com.histopgambling.looplock.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ClassificationTransport {
    suspend fun submit(upload: ClassificationUpload): ClassificationTransportOutcome

    suspend fun retrieve(binding: LocalClassificationBinding): ClassificationTransportOutcome
}

internal interface AgentApi {
    @POST("v1/classifications")
    suspend fun submit(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ClassificationRequestDto,
    ): Response<ResponseBody>

    @GET("v1/classifications/{event_id}")
    suspend fun retrieve(@Path("event_id") eventId: String): Response<ResponseBody>
}

class HttpClassificationTransport internal constructor(
    private val api: AgentApi,
    private val json: Json = STRICT_JSON,
) : ClassificationTransport {
    override suspend fun submit(upload: ClassificationUpload): ClassificationTransportOutcome {
        val response = try {
            api.submit(upload.eventId, upload.toRequestDto())
        } catch (_: IOException) {
            return ClassificationTransportOutcome.Retryable
        }
        return when (response.code()) {
            200 -> decodeTerminal(response, upload.binding())
            202 -> decodeProcessing(response, upload.eventId)
            409 -> response.closeWith(ClassificationTransportOutcome.PermanentConflict)
            429, in 500..599 -> response.closeWith(ClassificationTransportOutcome.Retryable)
            else -> response.closeWith(
                ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.UNEXPECTED_HTTP_STATUS),
            )
        }
    }

    override suspend fun retrieve(binding: LocalClassificationBinding): ClassificationTransportOutcome {
        val response = try {
            api.retrieve(binding.eventId)
        } catch (_: IOException) {
            return ClassificationTransportOutcome.Retryable
        }
        return when (response.code()) {
            200 -> decodeStatus(response, binding)
            404 -> response.closeWith(ClassificationTransportOutcome.NotFound)
            422 -> response.closeWith(ClassificationTransportOutcome.InvalidEventId)
            429, in 500..599 -> response.closeWith(ClassificationTransportOutcome.Retryable)
            else -> response.closeWith(
                ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.UNEXPECTED_HTTP_STATUS),
            )
        }
    }

    private fun decodeTerminal(
        response: Response<ResponseBody>,
        binding: LocalClassificationBinding,
    ): ClassificationTransportOutcome {
        val dto = when (val decoded = decode<TerminalResponseDto>(response)) {
            is DecodeResult.Valid -> decoded.value
            is DecodeResult.Rejected -> return ClassificationTransportOutcome.ProtocolRejected(decoded.reason)
        }
        return when (val mapped = dto.toProposal(binding)) {
            is MappingResult.Valid -> terminal(mapped.proposal)
            is MappingResult.Rejected -> ClassificationTransportOutcome.ProtocolRejected(mapped.reason)
        }
    }

    private fun decodeProcessing(
        response: Response<ResponseBody>,
        expectedEventId: String,
    ): ClassificationTransportOutcome {
        val dto = when (val decoded = decode<ProcessingResponseDto>(response)) {
            is DecodeResult.Valid -> decoded.value
            is DecodeResult.Rejected -> return ClassificationTransportOutcome.ProtocolRejected(decoded.reason)
        }
        return if (dto.eventId == expectedEventId) {
            ClassificationTransportOutcome.Processing(dto.eventId)
        } else {
            ClassificationTransportOutcome.ProtocolRejected(ProtocolRejection.IDENTITY_MISMATCH)
        }
    }

    private fun decodeStatus(
        response: Response<ResponseBody>,
        binding: LocalClassificationBinding,
    ): ClassificationTransportOutcome {
        val dto = when (val decoded = decode<StatusResponseDto>(response)) {
            is DecodeResult.Valid -> decoded.value
            is DecodeResult.Rejected -> return ClassificationTransportOutcome.ProtocolRejected(decoded.reason)
        }
        return dto.toOutcome(binding)
    }

    private inline fun <reified T> decode(response: Response<ResponseBody>): DecodeResult<T> {
        val body = response.body()
        if (body == null) {
            return DecodeResult.Rejected(ProtocolRejection.EMPTY_RESPONSE)
        }
        return try {
            DecodeResult.Valid(json.decodeFromString<T>(body.use { it.string() }))
        } catch (_: SerializationException) {
            DecodeResult.Rejected(ProtocolRejection.MALFORMED_RESPONSE)
        } catch (_: IllegalArgumentException) {
            DecodeResult.Rejected(ProtocolRejection.MALFORMED_RESPONSE)
        }
    }

    private fun ClassificationUpload.binding() = LocalClassificationBinding(
        eventId = eventId,
        commitmentId = commitmentId,
        targetPackage = packageName,
    )

    private fun terminal(proposal: com.histopgambling.looplock.domain.AgentProposal) =
        ClassificationTransportOutcome.Terminal(
            proposal = proposal,
            contractJson = json.encodeToString(proposal.toResponseDto()),
        )

    private fun Response<ResponseBody>.closeWith(
        outcome: ClassificationTransportOutcome,
    ): ClassificationTransportOutcome {
        body()?.close()
        errorBody()?.close()
        return outcome
    }
}

object AgentTransportFactory {
    fun create(
        baseUrl: String = BuildConfig.AGENT_BASE_URL,
        client: OkHttpClient = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .build(),
    ): ClassificationTransport {
        require(baseUrl.endsWith('/')) { "Retrofit base URL must end with /" }
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(STRICT_JSON.asConverterFactory("application/json".toMediaType()))
            .build()
        return HttpClassificationTransport(retrofit.create(AgentApi::class.java))
    }
}

internal val STRICT_JSON = Json {
    encodeDefaults = true
    ignoreUnknownKeys = false
    isLenient = false
    coerceInputValues = false
    explicitNulls = true
}

private sealed interface DecodeResult<out T> {
    data class Valid<T>(val value: T) : DecodeResult<T>
    data class Rejected(val reason: ProtocolRejection) : DecodeResult<Nothing>
}
