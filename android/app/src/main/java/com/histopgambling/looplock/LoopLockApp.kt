package com.histopgambling.looplock

import android.app.Application
import com.histopgambling.looplock.data.ConsentStore
import com.histopgambling.looplock.data.LoopLockDatabase
import com.histopgambling.looplock.data.PolicyRepository
import com.histopgambling.looplock.network.AgentTransportFactory
import com.histopgambling.looplock.network.ClassificationWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LoopLockApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { LoopLockDatabase.get(this) }
    val policyRepository by lazy { PolicyRepository(database, contentResolver) }
    val consentStore by lazy { ConsentStore(this) }
    val classificationTransport by lazy { AgentTransportFactory.create() }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            ClassificationWorkScheduler.recover(this@LoopLockApp, policyRepository)
        }
    }
}
