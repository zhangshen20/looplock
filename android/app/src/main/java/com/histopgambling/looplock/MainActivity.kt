package com.histopgambling.looplock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.histopgambling.looplock.domain.BETBURST_PACKAGE
import com.histopgambling.looplock.domain.AccountabilityPreviewFactory
import com.histopgambling.looplock.domain.CommitmentStatus
import com.histopgambling.looplock.domain.DEMO_DURATION_MS
import com.histopgambling.looplock.domain.LUCKYMIRROR_PACKAGE
import com.histopgambling.looplock.domain.RuleSource
import com.histopgambling.looplock.domain.TimelineEntry
import com.histopgambling.looplock.domain.UnlockFixtureResult
import com.histopgambling.looplock.ui.LoopLockUiState
import com.histopgambling.looplock.ui.LoopLockViewModel
import com.histopgambling.looplock.ui.SetupStage
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val viewModel: LoopLockViewModel by viewModels {
        LoopLockViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            LoopLockScreen(
                state = state,
                statusLabel = viewModel.statusLabel(),
                onContinue = viewModel::continueToAccessibilityDisclosure,
                onAcceptDisclosure = viewModel::acceptAccessibilityDisclosure,
                onQuarantineChanged = viewModel::setQuarantineNewInstalls,
                onReview = viewModel::reviewPolicy,
                onEdit = viewModel::editPolicy,
                onActivate = viewModel::activateCommitment,
                onRunUnlockFixture = viewModel::runUnlockFixture,
                onOpenAccessibility = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }
}

@Composable
private fun LoopLockScreen(
    state: LoopLockUiState,
    statusLabel: String,
    onContinue: () -> Unit,
    onAcceptDisclosure: () -> Unit,
    onQuarantineChanged: (Boolean) -> Unit,
    onReview: () -> Unit,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
    onRunUnlockFixture: () -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("LoopLock", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                if (state.loading) {
                    Text("Loading local protection state…")
                    return@Column
                }
                when (state.stage) {
                    SetupStage.BOUNDARY_DISCLOSURE -> BoundaryDisclosure(onContinue)
                    SetupStage.ACCESSIBILITY_DISCLOSURE -> AccessibilityDisclosure(onAcceptDisclosure)
                    SetupStage.POLICY -> PolicyBuilder(state, onQuarantineChanged, onReview)
                    SetupStage.REVIEW -> CommitmentReview(state, onEdit, onActivate)
                    SetupStage.STATUS -> ProtectionStatus(
                        state,
                        statusLabel,
                        onRunUnlockFixture,
                        onOpenAccessibility,
                    )
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun BoundaryDisclosure(onContinue: () -> Unit) {
    SectionTitle("A calm-state commitment, not surveillance")
    Body("LoopLock is a research prototype for voluntary adult self-use on a personally owned Android device.")
    Bullet("It observes only the foreground package identity needed for your selected local rule.")
    Bullet("It does not read screen contents, typed text, messages, contacts, financial data, or your full app inventory.")
    Bullet("Consumer mode can be disabled or uninstalled. Managed-device protection is a separate future mode.")
    Bullet("AI may propose a tighter rule. It can never unlock, weaken, delete, or shorten protection.")
    Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth().testTag("continue_boundary"),
    ) { Text("Continue") }
}

@Composable
private fun AccessibilityDisclosure(onAccept: () -> Unit) {
    SectionTitle("Before Android asks for accessibility access")
    Body("LoopLock uses an accessibility service only to notice when a selected demo package reaches the foreground and to show a clearly labeled LoopLock block screen.")
    Bullet("Window content retrieval is disabled.")
    Bullet("The service does not tap controls, submit forms, capture screenshots, or prevent uninstall or permission changes.")
    Bullet("Android will show the system permission screen only after you activate a commitment and choose to open settings.")
    Button(
        onClick = onAccept,
        modifier = Modifier.fillMaxWidth().testTag("accept_accessibility_disclosure"),
    ) { Text("I understand") }
}

@Composable
private fun PolicyBuilder(
    state: LoopLockUiState,
    onQuarantineChanged: (Boolean) -> Unit,
    onReview: () -> Unit,
) {
    SectionTitle("Choose the demo policy")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Selected target", fontWeight = FontWeight.SemiBold)
            Text("BetBurst Demo")
            Text(BETBURST_PACKAGE, style = MaterialTheme.typography.bodySmall)
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Quarantine the LuckyMirror demo if installed", fontWeight = FontWeight.SemiBold)
                Text("Separate opt-in for the adaptive demo event", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = state.quarantineNewInstalls,
                onCheckedChange = onQuarantineChanged,
                modifier = Modifier.testTag("quarantine_switch"),
            )
        }
    }
    Body("Demo duration: 5 minutes. The intended product default is 24 hours.")
    Button(
        onClick = onReview,
        modifier = Modifier.fillMaxWidth().testTag("review_commitment"),
    ) { Text("Review commitment") }
}

@Composable
private fun CommitmentReview(
    state: LoopLockUiState,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
) {
    val previewStart = System.currentTimeMillis()
    val previewEnd = previewStart + DEMO_DURATION_MS
    SectionTitle("Review before the one-way ratchet starts")
    Fact("Target", "BetBurst Demo\n$BETBURST_PACKAGE")
    Fact("Start", formatTime(previewStart))
    Fact("End", formatTime(previewEnd))
    Fact("New-install quarantine", if (state.quarantineNewInstalls) "On — LuckyMirror demo only" else "Off")
    Fact("Data sent if quarantine is on", "Opaque event and commitment IDs plus minimal LuckyMirror demo package metadata")
    Fact("Never collected", "Screen contents, keystrokes, messages, contacts, financial data, full installed-app inventory")
    Fact("Recovery", "No active unlock. The visible recovery path takes effect only after the commitment ends.")
    Fact("Consumer-mode limit", "You can revoke accessibility access or uninstall LoopLock; the app will report Action required, not claim protection.")
    Body("After activation, the target, end time, and quarantine choice are read-only. AI cannot change them.")
    Button(
        onClick = onActivate,
        modifier = Modifier.fillMaxWidth().testTag("activate_commitment"),
    ) { Text("Start 5-minute commitment") }
    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit before starting") }
}

@Composable
private fun ProtectionStatus(
    state: LoopLockUiState,
    statusLabel: String,
    onRunUnlockFixture: () -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    val commitment = state.overview.commitment
    SectionTitle(statusLabel)
    if (commitment == null) {
        Body("No commitment is active.")
        return
    }
    Fact("Target", "BetBurst Demo")
    Fact("Commitment ends", formatTime(commitment.endsWallMs))
    Fact("New-install quarantine", if (commitment.quarantineNewInstalls) "Pre-authorized" else "Not authorized")
    Fact("Rules", state.overview.rules.joinToString { it.source.name })
    state.overview.rules
        .firstOrNull { it.packageName == LUCKYMIRROR_PACKAGE }
        ?.let {
            Fact(
                "LuckyMirror",
                if (it.source == RuleSource.AGENT_TIGHTENED) {
                    "Agent proposal accepted locally — additive rule retained offline"
                } else {
                    "Quarantined locally before classification"
                },
            )
            OutlinedButton(
                onClick = onRunUnlockFixture,
                modifier = Modifier.fillMaxWidth().testTag("run_unlock_fixture"),
            ) { Text("Run rejected UNLOCK fixture") }
        }
    when (val result = state.unlockFixtureResult) {
        is UnlockFixtureResult.Rejected -> Card(
            modifier = Modifier.fillMaxWidth().testTag("unlock_fixture_result"),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("UNLOCK rejected", fontWeight = FontWeight.SemiBold)
                Text("${result.reasonCode.name} — quarantine retained.")
                Text("Commitment end unchanged: ${formatTime(result.commitmentEndsWallMs)}")
            }
        }
        UnlockFixtureResult.NotReady -> Body("UNLOCK fixture not run: an active LuckyMirror quarantine is required.")
        null -> Unit
    }
    if (state.overview.timeline.isNotEmpty()) {
        HorizontalDivider()
        Text("Neutral timeline", fontWeight = FontWeight.SemiBold)
        state.overview.timeline.takeLast(6).forEach { TimelineRow(it) }
    }
    AccountabilityPreviewFactory.create(state.overview, System.currentTimeMillis())?.let { preview ->
        HorizontalDivider()
        Text("Accountability preview", fontWeight = FontWeight.SemiBold)
        Card(modifier = Modifier.fillMaxWidth().testTag("accountability_preview")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Demo only — not sent", fontWeight = FontWeight.SemiBold)
                Fact("Attempt count", preview.attemptCount.toString())
                Fact(
                    "Time window",
                    "${formatTime(preview.windowStartWallMs)} to ${formatTime(preview.windowEndWallMs)}",
                )
                Fact("Escalation level", preview.escalationLevel)
            }
        }
        Body("This preview contains no package or destination name and no contact has been configured.")
    }
    if (commitment.status == CommitmentStatus.ACTIVE && !state.accessibilityEnabled) {
        Body("Android accessibility access is off. The commitment is stored, but LoopLock cannot honestly claim that BetBurst is blocked.")
        Button(
            onClick = onOpenAccessibility,
            modifier = Modifier.fillMaxWidth().testTag("open_accessibility_settings"),
        ) { Text("Open Android accessibility settings") }
    } else if (commitment.status == CommitmentStatus.ACTIVE) {
        Body("Blocking is performed from local rules. Network access and AI are not required for BetBurst enforcement.")
    } else {
        Body("The commitment has ended. Historical local state remains available; active demo rules no longer enforce.")
    }
    HorizontalDivider()
    Text("Delayed recovery", fontWeight = FontWeight.SemiBold)
    Body("No active override exists. Recovery becomes available only after ${formatTime(commitment.endsWallMs)}; no request has been sent.")
    Body("Consumer mode remains bypassable by revoking permission or uninstalling the app. Managed-device controls are separate future work.")
}

@Composable
private fun TimelineRow(entry: TimelineEntry) {
    val shortId = entry.eventId.take(8)
    Text(
        "${entry.type.name}: ${entry.resultCode} · ${entry.uploadState} · $shortId",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

@Composable
private fun Body(text: String) = Text(text, style = MaterialTheme.typography.bodyLarge)

@Composable
private fun Bullet(text: String) = Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("•")
    Text(text, modifier = Modifier.weight(1f))
}

@Composable
private fun Fact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(value)
    }
    Spacer(Modifier.height(2.dp))
}

private fun formatTime(wallMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(wallMs))
