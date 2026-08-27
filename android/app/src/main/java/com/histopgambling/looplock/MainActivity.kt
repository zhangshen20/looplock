package com.histopgambling.looplock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.histopgambling.looplock.domain.AccountabilityPreviewFactory
import com.histopgambling.looplock.domain.BETBURST_PACKAGE
import com.histopgambling.looplock.domain.CommitmentStatus
import com.histopgambling.looplock.domain.DEMO_DURATION_MS
import com.histopgambling.looplock.domain.LUCKYMIRROR_PACKAGE
import com.histopgambling.looplock.domain.ProtectionEventType
import com.histopgambling.looplock.domain.RuleSource
import com.histopgambling.looplock.domain.TimelineEntry
import com.histopgambling.looplock.domain.UnlockFixtureResult
import com.histopgambling.looplock.ui.DeepTeal
import com.histopgambling.looplock.ui.Ink
import com.histopgambling.looplock.ui.LoopLockTheme
import com.histopgambling.looplock.ui.LoopLockUiState
import com.histopgambling.looplock.ui.LoopLockViewModel
import com.histopgambling.looplock.ui.Midnight
import com.histopgambling.looplock.ui.SetupStage
import com.histopgambling.looplock.ui.SkyBlue
import com.histopgambling.looplock.ui.SoftCream
import com.histopgambling.looplock.ui.SoftLavender
import com.histopgambling.looplock.ui.Sunrise
import com.histopgambling.looplock.ui.TranquilTeal
import com.histopgambling.looplock.ui.WarmCoral
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val dark = state.stage == SetupStage.BOUNDARY_DISCLOSURE || state.stage == SetupStage.STATUS
    LoopLockTheme(dark = dark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBrush(dark)),
            ) {
                DecorativeGlow(dark)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    BrandLockup(dark)
                    if (state.loading) {
                        Spacer(Modifier.height(48.dp))
                        LoopMark(Modifier.size(150.dp).align(Alignment.CenterHorizontally))
                        Text(
                            "Loading your local protection…",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                        )
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
                    state.error?.let { ErrorCard(it) }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

private fun screenBrush(dark: Boolean): Brush = if (dark) {
    Brush.verticalGradient(listOf(Midnight, Color(0xFF09223B), Color(0xFF0A1930)))
} else {
    Brush.verticalGradient(listOf(SoftCream, Color.White, Color(0xFFF0F3FF)))
}

@Composable
private fun DecorativeGlow(dark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .background(
                Brush.radialGradient(
                    colors = if (dark) {
                        listOf(TranquilTeal.copy(alpha = 0.18f), Color.Transparent)
                    } else {
                        listOf(Sunrise.copy(alpha = 0.25f), Color.Transparent)
                    },
                    center = Offset(780f, 40f),
                    radius = 720f,
                ),
            ),
    )
}

@Composable
private fun BrandLockup(dark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LoopMark(Modifier.size(32.dp), compact = true)
        Text("LoopLock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(50),
            color = if (dark) Color.White.copy(alpha = 0.08f) else DeepTeal.copy(alpha = 0.08f),
        ) {
            Text(
                "APPLICATION-READY",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (dark) SkyBlue else DeepTeal,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ColumnScope.BoundaryDisclosure(onContinue: () -> Unit) {
    Eyebrow("A CALM-STATE COMMITMENT")
    Text("Your pause\nstarts here", style = MaterialTheme.typography.headlineLarge)
    Text(
        "Keep the decision you made while calm—even when an urge looks for another route.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LoopMark(Modifier.size(190.dp).align(Alignment.CenterHorizontally))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrustChip("Local\nblocking", Modifier.weight(1f))
        TrustChip("Minimal\ndata", Modifier.weight(1f))
        TrustChip("AI cannot\nunlock", Modifier.weight(1f))
    }
    GlassCard(title = "Built around your consent") {
        DisclosureRow("Voluntary adult self-use", "For a personally owned Android device; not covert monitoring.")
        DisclosureRow("Only selected-app identity", "No screen contents, typed text, messages, contacts, financial data, or full app inventory.")
        DisclosureRow("Honest consumer mode", "You can disable the service or uninstall LoopLock. Managed-device protection is separate future work.")
    }
    BoundaryCard(
        title = "The one-way safety rule",
        body = "AI may propose a tighter rule. It can never unlock, weaken, delete, or shorten your protection.",
    )
    PrimaryAction("Continue", "continue_boundary", onContinue)
}

@Composable
private fun ColumnScope.AccessibilityDisclosure(onAccept: () -> Unit) {
    Eyebrow("STEP 2 · ANDROID ACCESS")
    Text("One setting powers the local block", style = MaterialTheme.typography.headlineMedium)
    Text(
        "LoopLock uses Android accessibility only to notice when a selected demo app reaches the foreground and show its own block screen.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LoopMark(Modifier.size(132.dp).align(Alignment.CenterHorizontally))
    ElevatedInfoCard(title = "What the service does") {
        DisclosureRow("Sees a selected package identity", "It does not retrieve window content.")
        DisclosureRow("Shows a LoopLock block screen", "It does not tap controls, submit forms, or capture screenshots.")
        DisclosureRow("Leaves Android controls visible", "It does not prevent uninstall or permission changes.")
    }
    SoftInfoCard(
        title = "You stay informed",
        body = "Android opens its system permission screen only after you activate a commitment and choose to open settings.",
    )
    PrimaryAction("I understand", "accept_accessibility_disclosure", onAccept)
}

@Composable
private fun PolicyBuilder(
    state: LoopLockUiState,
    onQuarantineChanged: (Boolean) -> Unit,
    onReview: () -> Unit,
) {
    Eyebrow("STEP 3 · CREATE YOUR POLICY")
    Text("Build your calm-state plan", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Start with one clear boundary. During the commitment it can stay the same or become stronger—never weaker.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
    SelectionCard(
        badge = "BB",
        badgeColor = TranquilTeal,
        title = "BetBurst Demo",
        subtitle = "Selected local block target",
        detail = BETBURST_PACKAGE,
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (state.quarantineNewInstalls) Modifier.border(1.5.dp, TranquilTeal, RoundedCornerShape(24.dp))
                else Modifier,
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Badge("+", SoftLavender)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Quarantine new demo installs", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Optional, pre-authorized protection for the LuckyMirror workaround demo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (state.quarantineNewInstalls) "On · the adaptive ratchet is enabled" else "Off by default · choose deliberately",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.quarantineNewInstalls) DeepTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.quarantineNewInstalls,
                onCheckedChange = onQuarantineChanged,
                modifier = Modifier.testTag("quarantine_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = DeepTeal,
                    uncheckedThumbColor = Color.White,
                ),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("5 min", "Demo commitment", Modifier.weight(1f), light = true)
        MetricCard("24 hr", "Product default", Modifier.weight(1f), light = true)
    }
    BoundaryCard(
        title = "AI is outside the lock",
        body = "Android blocks from local rules. The agent can propose only TIGHTEN or REVIEW.",
        light = true,
    )
    PrimaryAction("Review commitment", "review_commitment", onReview)
}

@Composable
private fun CommitmentReview(
    state: LoopLockUiState,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
) {
    val previewStart = System.currentTimeMillis()
    val previewEnd = previewStart + DEMO_DURATION_MS
    Eyebrow("FINAL REVIEW")
    Text("Lock in a calmer choice", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Read this once more before the one-way ratchet starts.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
    ElevatedInfoCard(title = "Your commitment") {
        ReviewFact("Target", "BetBurst Demo", BETBURST_PACKAGE)
        ReviewFact("Duration", "5 minutes", "${formatClock(previewStart)} → ${formatClock(previewEnd)}")
        ReviewFact(
            "New-install quarantine",
            if (state.quarantineNewInstalls) "On · LuckyMirror demo only" else "Off",
            "Read-only after activation",
        )
    }
    BoundaryCard(
        title = "One-way ratchet",
        body = "After activation, the target, end time, and quarantine choice are read-only. AI cannot change them or remove protection early.",
        light = true,
    )
    ElevatedInfoCard(title = "Privacy and recovery") {
        DisclosureRow("Sent only if quarantine is on", "Opaque event and commitment IDs plus minimal LuckyMirror demo package metadata.")
        DisclosureRow("Never collected", "Screen contents, keystrokes, messages, contacts, financial data, or full app inventory.")
        DisclosureRow("Delayed recovery", "No active unlock. Recovery takes effect only after the commitment ends.")
        DisclosureRow("Consumer-mode limit", "You can revoke accessibility access or uninstall LoopLock; status changes to Action required.")
    }
    PrimaryAction("Start 5-minute commitment", "activate_commitment", onActivate)
    OutlinedButton(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(18.dp),
    ) { Text("Edit before starting") }
}

@Composable
private fun ColumnScope.ProtectionStatus(
    state: LoopLockUiState,
    statusLabel: String,
    onRunUnlockFixture: () -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    val commitment = state.overview.commitment
    val protected = statusLabel == "Protected"
    val accent = when (statusLabel) {
        "Protected" -> TranquilTeal
        "Action required" -> WarmCoral
        else -> SoftLavender
    }
    Eyebrow("YOUR PROTECTION")
    LoopMark(
        Modifier.size(184.dp).align(Alignment.CenterHorizontally),
        accent = accent,
        showCheck = protected,
    )
    Text(
        statusLabel,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium,
        color = accent,
    )
    Text(
        when (statusLabel) {
            "Protected" -> "Your commitment is holding."
            "Action required" -> "Your commitment is saved, but Android access needs attention."
            "Expired" -> "Your commitment reached its planned end."
            else -> "No commitment is active."
        },
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (commitment == null) return

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(state.overview.rules.size.toString(), "rules retained", Modifier.weight(1f))
        MetricCard(formatClock(commitment.endsWallMs), "planned end", Modifier.weight(1f))
    }
    GlassCard(title = "Protection details") {
        StatusFact("Selected target", "BetBurst Demo")
        StatusFact(
            "New-install quarantine",
            if (commitment.quarantineNewInstalls) "Pre-authorized" else "Not authorized",
        )
        StatusFact("Rule sources", state.overview.rules.joinToString { friendlyRuleSource(it.source) })
    }
    state.overview.rules
        .firstOrNull { it.packageName == LUCKYMIRROR_PACKAGE }
        ?.let { luckyRule ->
            BoundaryCard(
                title = if (luckyRule.source == RuleSource.AGENT_TIGHTENED) {
                    "The loophole became the next rule"
                } else {
                    "LuckyMirror is quarantined locally"
                },
                body = if (luckyRule.source == RuleSource.AGENT_TIGHTENED) {
                    "The agent proposal passed Android's local validator. The additive rule remains available offline."
                } else {
                    "Local protection started before classification. The agent still has no enforcement authority."
                },
            )
            OutlinedButton(
                onClick = onRunUnlockFixture,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("run_unlock_fixture"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftLavender),
            ) { Text("Run rejected UNLOCK fixture") }
        }
    when (val result = state.unlockFixtureResult) {
        is UnlockFixtureResult.Rejected -> SafetyResultCard(
            title = "UNLOCK rejected",
            body = "${result.reasonCode.name} · quarantine retained",
            detail = "Commitment end unchanged: ${formatTime(result.commitmentEndsWallMs)}",
        )
        UnlockFixtureResult.NotReady -> SoftInfoCard(
            title = "Fixture not ready",
            body = "An active LuckyMirror quarantine is required.",
            dark = true,
        )
        null -> Unit
    }
    if (state.overview.timeline.isNotEmpty()) {
        SectionDivider("Recent activity")
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.065f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                state.overview.timeline.takeLast(6).reversed().forEachIndexed { index, entry ->
                    TimelineRow(entry)
                    if (index != state.overview.timeline.takeLast(6).lastIndex) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
    AccountabilityPreviewFactory.create(state.overview, System.currentTimeMillis())?.let { preview ->
        SectionDivider("Accountability preview")
        GlassCard(title = "Demo only · nothing sent", tag = "accountability_preview") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniFact("Attempts", preview.attemptCount.toString(), Modifier.weight(1f))
                MiniFact("Level", friendlyEscalation(preview.escalationLevel), Modifier.weight(1f))
            }
            Text(
                "Window ${formatClock(preview.windowStartWallMs)}–${formatClock(preview.windowEndWallMs)} · no package, destination, or contact included.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (commitment.status == CommitmentStatus.ACTIVE && !state.accessibilityEnabled) {
        SafetyResultCard(
            title = "Android access is off",
            body = "The commitment is stored, but LoopLock cannot honestly claim that BetBurst is blocked.",
            detail = "Open Android settings to restore the selected-app service.",
            warning = true,
            tag = "accessibility_warning",
        )
        PrimaryAction("Open Android accessibility settings", "open_accessibility_settings", onOpenAccessibility)
    } else if (commitment.status == CommitmentStatus.ACTIVE) {
        SoftInfoCard(
            title = "Local by design",
            body = "Blocking reads local rules. Network access and AI are not required for BetBurst enforcement.",
            dark = true,
        )
    } else {
        SoftInfoCard(
            title = "History remains, enforcement has ended",
            body = "Active demo rules no longer enforce after expiry.",
            dark = true,
        )
    }
    SectionDivider("Delayed recovery")
    GlassCard(title = "Recovery stays visible") {
        Text(
            "No active override exists. Recovery becomes available only after ${formatTime(commitment.endsWallMs)}; no request has been sent.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Consumer mode remains bypassable by revoking permission or uninstalling the app. Managed-device controls are separate future work.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoopMark(
    modifier: Modifier,
    compact: Boolean = false,
    accent: Color = TranquilTeal,
    showCheck: Boolean = true,
) {
    Canvas(modifier) {
        val min = size.minDimension
        val stroke = if (compact) min * 0.11f else min * 0.065f
        val inset = stroke * 0.8f
        drawCircle(
            brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.2f), Color.Transparent)),
            radius = min * 0.48f,
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(accent, SkyBlue, SoftLavender, accent)),
            startAngle = -72f,
            sweepAngle = 304f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2f, size.height - inset * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min * if (compact) 0.24f else 0.22f
        val shield = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.78f, cy - r * 0.58f)
            lineTo(cx + r * 0.68f, cy + r * 0.35f)
            quadraticTo(cx, cy + r * 1.05f, cx - r * 0.68f, cy + r * 0.35f)
            lineTo(cx - r * 0.78f, cy - r * 0.58f)
            close()
        }
        drawPath(shield, color = accent.copy(alpha = 0.9f), style = Stroke(width = stroke * 0.55f, cap = StrokeCap.Round))
        if (showCheck && !compact) {
            drawLine(
                Color.White,
                Offset(cx - r * 0.35f, cy + r * 0.02f),
                Offset(cx - r * 0.05f, cy + r * 0.32f),
                stroke * 0.55f,
                StrokeCap.Round,
            )
            drawLine(
                Color.White,
                Offset(cx - r * 0.05f, cy + r * 0.32f),
                Offset(cx + r * 0.46f, cy - r * 0.27f),
                stroke * 0.55f,
                StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun Eyebrow(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
)

@Composable
private fun PrimaryAction(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun TrustChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.075f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(TranquilTeal))
            Spacer(Modifier.height(7.dp))
            Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GlassCard(title: String, tag: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.07f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ElevatedInfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun DisclosureRow(title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BoundaryCard(title: String, body: String, light: Boolean = false) {
    val background = if (light) {
        Brush.linearGradient(listOf(Color(0xFFE6F7F4), Color(0xFFF1ECFF)))
    } else {
        Brush.linearGradient(listOf(TranquilTeal.copy(alpha = 0.14f), SoftLavender.copy(alpha = 0.13f)))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .border(1.dp, TranquilTeal.copy(alpha = if (light) 0.26f else 0.35f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (light) Ink else MaterialTheme.colorScheme.onSurface)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = if (light) Color(0xFF355167) else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SoftInfoCard(title: String, body: String, dark: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) Color.White.copy(alpha = 0.065f) else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectionCard(badge: String, badgeColor: Color, title: String, subtitle: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Badge(badge, badgeColor)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = DeepTeal)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(shape = RoundedCornerShape(50), color = TranquilTeal.copy(alpha = 0.12f)) {
                Text("SELECTED", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.bodySmall, color = DeepTeal, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(
            Brush.linearGradient(listOf(color, color.copy(alpha = 0.65f))),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (color == SoftLavender) Midnight else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier, light: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (light) Color.White else Color.White.copy(alpha = 0.07f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (light) DeepTeal.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = if (light) Ink else TranquilTeal, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewFact(label: String, value: String, detail: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.padding(top = 5.dp).size(9.dp).clip(CircleShape).background(TranquilTeal))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusFact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(0.44f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(0.56f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(timelineColor(entry.type)))
            Box(Modifier.size(width = 2.dp, height = 32.dp).background(Color.White.copy(alpha = 0.1f)))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(friendlyEvent(entry.type), style = MaterialTheme.typography.labelLarge)
            Text(
                "${formatClock(entry.createdWallMs)} · ${entry.resultCode.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${entry.uploadState.replace('_', ' ')} · event ${entry.eventId.take(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = SkyBlue,
            )
        }
    }
}

@Composable
private fun SectionDivider(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.12f))
    }
}

@Composable
private fun MiniFact(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.055f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = TranquilTeal)
        }
    }
}

@Composable
private fun SafetyResultCard(
    title: String,
    body: String,
    detail: String,
    warning: Boolean = false,
    tag: String = "unlock_fixture_result",
) {
    val color = if (warning) WarmCoral else SoftLavender
    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.13f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.38f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = color)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))) {
        Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
    }
}

private fun friendlyRuleSource(source: RuleSource): String = when (source) {
    RuleSource.USER_SELECTED -> "Chosen locally"
    RuleSource.QUARANTINE -> "Local quarantine"
    RuleSource.AGENT_TIGHTENED -> "Agent-tightened · validated locally"
}

private fun friendlyEvent(type: ProtectionEventType): String = when (type) {
    ProtectionEventType.BLOCK_ATTEMPT -> "App launch blocked locally"
    ProtectionEventType.PACKAGE_ADDED -> "New demo install quarantined"
    ProtectionEventType.AGENT_RESULT -> "Agent proposal checked locally"
    ProtectionEventType.VALIDATION_REJECTED -> "Unsafe proposal rejected"
    ProtectionEventType.SERVICE_STATE -> "Protection permission changed"
    ProtectionEventType.COMMITMENT_EXPIRED -> "Commitment completed"
}

private fun timelineColor(type: ProtectionEventType): Color = when (type) {
    ProtectionEventType.BLOCK_ATTEMPT -> TranquilTeal
    ProtectionEventType.PACKAGE_ADDED -> SkyBlue
    ProtectionEventType.AGENT_RESULT -> SoftLavender
    ProtectionEventType.VALIDATION_REJECTED -> WarmCoral
    ProtectionEventType.SERVICE_STATE -> Sunrise
    ProtectionEventType.COMMITMENT_EXPIRED -> Color(0xFF9FB2C2)
}

private fun friendlyEscalation(value: String): String = when (value) {
    "NONE" -> "None"
    "LEVEL_1" -> "Level 1"
    "LEVEL_2" -> "Level 2"
    else -> "Review"
}

private fun formatClock(wallMs: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(wallMs))

private fun formatTime(wallMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(wallMs))
