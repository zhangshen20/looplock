package com.histopgambling.looplock.enforcement

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.histopgambling.looplock.LoopLockApp
import com.histopgambling.looplock.data.PolicyRepository
import com.histopgambling.looplock.domain.BETBURST_PACKAGE
import com.histopgambling.looplock.domain.LUCKYMIRROR_PACKAGE
import com.histopgambling.looplock.domain.InstallQuarantineResult
import com.histopgambling.looplock.network.ClassificationWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoopLockAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: PolicyRepository
    private lateinit var overlayController: BlockOverlayController
    private lateinit var installMonitor: InstallMonitor
    private val lastHandledElapsedMs = mutableMapOf<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as LoopLockApp
        repository = app.policyRepository
        overlayController = BlockOverlayController(this)
        installMonitor = InstallMonitor(
            context = this,
            repository = repository,
            scope = serviceScope,
            onQuarantineChanged = { eventId ->
                ClassificationWorkScheduler.enqueue(this, eventId)
                refreshServicePackageFilter()
            },
        )
        installMonitor.start()
        serviceScope.launch {
            val result = installMonitor.reconcile()
            if (result is InstallQuarantineResult.Quarantined) {
                ClassificationWorkScheduler.enqueue(this@LoopLockAccessibilityService, result.eventId)
            }
            ClassificationWorkScheduler.recover(this@LoopLockAccessibilityService, repository)
            refreshServicePackageFilter()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName != BETBURST_PACKAGE && packageName != LUCKYMIRROR_PACKAGE) return
        val nowElapsed = SystemClock.elapsedRealtime()
        if (nowElapsed - (lastHandledElapsedMs[packageName] ?: 0L) < EVENT_DEBOUNCE_MS) return
        lastHandledElapsedMs[packageName] = nowElapsed

        serviceScope.launch {
            if (!repository.isPackageBlocked(packageName)) return@launch
            val commitment = repository.getActiveCommitment() ?: return@launch
            repository.recordBlockAttempt(packageName)
            withContext(Dispatchers.Main) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                // The global action completes asynchronously. Waiting for the home transition
                // prevents Android from treating the newly-added accessibility overlay as part
                // of the outgoing app window.
                delay(HOME_TRANSITION_DELAY_MS)
                overlayController.show(packageName, commitment.endsWallMs)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::installMonitor.isInitialized) installMonitor.stop()
        if (::overlayController.isInitialized) overlayController.remove()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EVENT_DEBOUNCE_MS = 1_200L
        private const val HOME_TRANSITION_DELAY_MS = 350L
    }

    private suspend fun refreshServicePackageFilter() {
        val packages = repository.getActiveRulePackages().ifEmpty { listOf(BETBURST_PACKAGE) }
        withContext(Dispatchers.Main) {
            val updated = serviceInfo ?: return@withContext
            updated.packageNames = packages.toTypedArray()
            serviceInfo = updated
        }
    }
}
