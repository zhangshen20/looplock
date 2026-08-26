package com.histopgambling.looplock.network

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.histopgambling.looplock.LoopLockApp
import com.histopgambling.looplock.data.PolicyRepository
import java.util.UUID
import java.util.concurrent.TimeUnit

class ClassificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID)
            ?.takeIf(::isUuid)
            ?: return Result.failure()
        val app = applicationContext as LoopLockApp
        val pending = app.policyRepository.prepareClassificationAttempt(eventId)
            ?: return Result.success()
        val upload = runCatching {
            ClassificationUpload(
                eventId = pending.eventId,
                commitmentId = pending.commitmentId,
                packageName = pending.targetPackage,
                label = pending.targetLabel,
                versionCode = pending.targetVersionCode,
            )
        }.getOrElse {
            app.policyRepository.applyAgentResponse(eventId, INVALID_LOCAL_CONTRACT)
            return Result.success()
        }

        val submitted = app.classificationTransport.submit(upload)
        val outcome = if (submitted is ClassificationTransportOutcome.Processing) {
            app.classificationTransport.retrieve(
                LocalClassificationBinding(
                    eventId = pending.eventId,
                    commitmentId = pending.commitmentId,
                    targetPackage = pending.targetPackage,
                ),
            )
        } else {
            submitted
        }
        return handleOutcome(outcome, eventId, app.policyRepository)
    }

    private suspend fun handleOutcome(
        outcome: ClassificationTransportOutcome,
        eventId: String,
        repository: PolicyRepository,
    ): Result = when (outcome) {
        is ClassificationTransportOutcome.Terminal -> {
            repository.applyAgentResponse(eventId, outcome.contractJson)
            Result.success()
        }
        ClassificationTransportOutcome.Retryable,
        ClassificationTransportOutcome.NotFound,
        is ClassificationTransportOutcome.Processing,
        -> {
            repository.markClassificationRetry(eventId)
            Result.retry()
        }
        ClassificationTransportOutcome.PermanentConflict,
        ClassificationTransportOutcome.InvalidEventId,
        is ClassificationTransportOutcome.ProtocolRejected,
        -> {
            repository.applyAgentResponse(eventId, INVALID_REMOTE_CONTRACT)
            Result.success()
        }
    }

    companion object {
        internal const val KEY_EVENT_ID = "event_id"
        private const val INVALID_LOCAL_CONTRACT = "{\"local_contract\":\"invalid\"}"
        private const val INVALID_REMOTE_CONTRACT = "{\"remote_contract\":\"rejected\"}"

        private fun isUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess
    }
}

object ClassificationWorkScheduler {
    fun enqueue(context: Context, eventId: String) {
        require(runCatching { UUID.fromString(eventId) }.isSuccess) { "eventId must be a UUID" }
        val request = OneTimeWorkRequestBuilder<ClassificationWorker>()
            .setInputData(Data.Builder().putString(ClassificationWorker.KEY_EVENT_ID, eventId).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(workName(eventId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(eventId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun recover(context: Context, repository: PolicyRepository) {
        repository.getRecoverableClassificationEventIds().forEach { enqueue(context, it) }
    }

    private fun workName(eventId: String) = "looplock-classification-$eventId"
}
