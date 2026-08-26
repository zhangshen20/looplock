package com.histopgambling.looplock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.histopgambling.looplock.LoopLockApp
import com.histopgambling.looplock.domain.CommitmentOverview
import com.histopgambling.looplock.domain.CommitmentStatus
import com.histopgambling.looplock.domain.ProtectionStatusResolver
import com.histopgambling.looplock.domain.UnlockFixtureResult
import com.histopgambling.looplock.enforcement.ServiceHealth
import com.histopgambling.looplock.enforcement.InstallMonitor
import com.histopgambling.looplock.domain.InstallQuarantineResult
import com.histopgambling.looplock.network.ClassificationWorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class SetupStage {
    BOUNDARY_DISCLOSURE,
    ACCESSIBILITY_DISCLOSURE,
    POLICY,
    REVIEW,
    STATUS,
}

data class LoopLockUiState(
    val stage: SetupStage = SetupStage.BOUNDARY_DISCLOSURE,
    val loading: Boolean = true,
    val quarantineNewInstalls: Boolean = false,
    val overview: CommitmentOverview = CommitmentOverview(null, emptyList()),
    val accessibilityEnabled: Boolean = false,
    val unlockFixtureResult: UnlockFixtureResult? = null,
    val error: String? = null,
)

class LoopLockViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LoopLockApp
    private val repository = app.policyRepository
    private val consentStore = app.consentStore
    private val _state = MutableStateFlow(LoopLockUiState())
    val state: StateFlow<LoopLockUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.refreshExpiry()
            val consentAccepted = consentStore.isCurrentDisclosureAccepted.first()
            _state.update { current ->
                current.copy(
                    stage = if (consentAccepted) SetupStage.POLICY else SetupStage.BOUNDARY_DISCLOSURE,
                    loading = false,
                )
            }
        }
        viewModelScope.launch {
            repository.observeOverview().collect { overview ->
                _state.update { current ->
                    current.copy(
                        overview = overview,
                        stage = if (overview.commitment != null) SetupStage.STATUS else current.stage,
                    )
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (_state.value.overview.commitment?.status == CommitmentStatus.ACTIVE) {
                    repository.refreshExpiry()
                }
            }
        }
    }

    fun continueToAccessibilityDisclosure() {
        _state.update { it.copy(stage = SetupStage.ACCESSIBILITY_DISCLOSURE) }
    }

    fun acceptAccessibilityDisclosure() {
        viewModelScope.launch {
            consentStore.acceptCurrentDisclosure()
            _state.update { it.copy(stage = SetupStage.POLICY) }
        }
    }

    fun setQuarantineNewInstalls(enabled: Boolean) {
        _state.update { it.copy(quarantineNewInstalls = enabled) }
    }

    fun reviewPolicy() {
        _state.update { it.copy(stage = SetupStage.REVIEW) }
    }

    fun editPolicy() {
        _state.update { it.copy(stage = SetupStage.POLICY) }
    }

    fun activateCommitment() {
        viewModelScope.launch {
            runCatching { repository.activateDemoCommitment(_state.value.quarantineNewInstalls) }
                .onSuccess {
                    _state.update { state -> state.copy(stage = SetupStage.STATUS, error = null) }
                }
                .onFailure { failure ->
                    _state.update { state -> state.copy(error = failure.message ?: "Activation failed") }
                }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            repository.refreshExpiry()
            val result = InstallMonitor.reconcileOnce(getApplication(), repository)
            if (result is InstallQuarantineResult.Quarantined) {
                ClassificationWorkScheduler.enqueue(getApplication(), result.eventId)
            }
            ClassificationWorkScheduler.recover(getApplication(), repository)
            _state.update { current ->
                current.copy(accessibilityEnabled = ServiceHealth.isEnabled(getApplication()))
            }
        }
    }

    fun runUnlockFixture() {
        viewModelScope.launch {
            val result = repository.runUnlockFixture()
            _state.update { current -> current.copy(unlockFixtureResult = result) }
        }
    }

    fun statusLabel(): String {
        val current = _state.value
        return ProtectionStatusResolver.resolve(
            current.overview.commitment,
            current.accessibilityEnabled,
        ).displayName
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoopLockViewModel(app) as T
    }
}
