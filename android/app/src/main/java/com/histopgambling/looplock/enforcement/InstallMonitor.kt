package com.histopgambling.looplock.enforcement

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.histopgambling.looplock.data.PolicyRepository
import com.histopgambling.looplock.domain.InstallQuarantineResult
import com.histopgambling.looplock.domain.LUCKYMIRROR_PACKAGE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InstallMonitor(
    private val context: Context,
    private val repository: PolicyRepository,
    private val scope: CoroutineScope,
    private val onQuarantineChanged: suspend (String) -> Unit,
) {
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!shouldHandle(intent)) return
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val result = reconcileOnce(context, repository)
                    if (result is InstallQuarantineResult.Quarantined) {
                        onQuarantineChanged(result.eventId)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // ACTION_PACKAGE_ADDED is a protected system broadcast. The receiver still
            // validates the exact targeted package and reads PackageManager state itself.
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        registered = true
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    suspend fun reconcile(): InstallQuarantineResult = reconcileOnce(context, repository)

    companion object {
        fun shouldHandle(intent: Intent): Boolean =
            intent.action == Intent.ACTION_PACKAGE_ADDED &&
                intent.data?.schemeSpecificPart == LUCKYMIRROR_PACKAGE &&
                !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        suspend fun reconcileOnce(
            context: Context,
            repository: PolicyRepository,
        ): InstallQuarantineResult {
            val packageInfo = try {
                context.packageManager.getPackageInfo(LUCKYMIRROR_PACKAGE, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                return InstallQuarantineResult.NotTargetPackage
            }
            val label = context.packageManager
                .getApplicationLabel(packageInfo.applicationInfo!!)
                .toString()
            return repository.quarantineInstalledPackage(
                packageName = LUCKYMIRROR_PACKAGE,
                label = label,
                versionCode = packageInfo.longVersionCode,
                firstInstallWallMs = packageInfo.firstInstallTime,
            )
        }
    }
}
