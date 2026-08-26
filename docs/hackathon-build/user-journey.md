# User Journey and Demo Storyboard

## Product Journey

| Stage | User state | User action | Product response | Safety requirement |
| --- | --- | --- | --- | --- |
| Consider | Calm but uncertain | Opens LoopLock | Explains prototype, limits, permissions, and data use | No fear, shame, or false guarantee |
| Configure | Intentionally preparing | Selects app, duration, and optional new-install quarantine | Shows exact consequences and recovery timing | No preselected strict option |
| Commit | Ready to externalize a decision | Confirms policy | Creates immutable active policy and consent receipt | No instant override hidden elsewhere |
| Urge | Impulsive | Opens selected demo app | Blocks locally and records a neutral event | Works offline; no cloud decision needed |
| Workaround | Tests a loophole | Installs a second demo app | Quarantines it under the pre-authorized policy | Quarantine occurs before cloud call |
| Adapt | Waiting briefly | Does nothing | Agent classifies; local validator adds or rejects rule | Agent cannot allow or unlock |
| Verify | Curious or skeptical | Relaunches with network off | New app remains blocked locally | Demonstrates actual ratchet |
| Reflect | Calm again | Reviews timeline | Shows factual events and delayed recovery | Minimal detail; no accusation |
| Expire | Commitment completes | Returns after end time | Policy expires visibly | No indefinite lock by accident |

## First-Run Journey

1. **Welcome:** “LoopLock is a research prototype that helps uphold a policy you choose while calm.”
2. **Limits:** “On a personal Android phone, you can revoke permissions or uninstall this app. LoopLock does not claim tamper-proof protection.”
3. **Data:** package name/label for the demo classification only; no screen text, keystrokes, contacts, messages, financial data, or full app inventory.
4. **Permission disclosure:** explain accessibility event use immediately before opening Android settings.
5. **Policy builder:** targeted list contains only the harmless demo package(s).
6. **Strict option:** new-install quarantine is off by default and must be affirmatively selected.
7. **Review:** show selected target, start/end time, behavior, data, delayed recovery, and service state.
8. **Activate:** require a final, deliberate action.

## Four-Minute Demo Storyboard

### 0:00–0:25 — Problem and promise

- Show the stale-blocker problem in one sentence: a static list does not learn from a workaround.
- State the proposition: “LoopLock turns a loophole into the next rule, but its AI can never unlock the phone.”

### 0:25–0:55 — Consent and policy

- Show the consumer-mode limitation disclosure.
- Select **BetBurst Demo**.
- Turn on “Quarantine apps installed during this commitment.”
- Activate a five-minute demo commitment.

### 0:55–1:20 — Deterministic first block

- Launch BetBurst.
- Show the local block and event timeline.
- Point out that this step makes no model call.

### 1:20–2:05 — Loophole becomes an event

- Install **LuckyMirror Demo** on the emulator while the commitment is active.
- Show immediate local quarantine.
- Show the event ID moving from queued to classified.

### 2:05–2:40 — Agent action and Cloud proof

- Show Cloud Run receiving the event, ADK invoking Gemini 3.5 Flash, and Firestore holding the minimal result.
- Show the closed result: `TIGHTEN`, package target, confidence, and short reason.
- Avoid displaying hidden chain-of-thought or personal data.

### 2:40–3:10 — The ratchet proves itself

- Turn off the network.
- Launch LuckyMirror again.
- Show it remains blocked from the local rule.

### 3:10–3:35 — Safety proof

- Feed a test `UNLOCK` or earlier-expiry command to the validator.
- Show `VALIDATION_REJECTED` and unchanged policy end time.

### 3:35–3:55 — Architecture and limits

- Display the architecture diagram.
- Say: consumer mode is bypassable; managed-device mode can be stronger but requires device-owner provisioning and is not this demo.

### 3:55–4:00 — Close

- “Static blockers remember a list. LoopLock remembers the loophole.”

## Emotional and Language Guidelines

- Prefer: “blocked,” “protected,” “review needed,” “commitment,” “attempt,” and “permission changed.”
- Avoid: “failure,” “relapse detected,” “cheating,” “caught,” “suspicious,” or diagnostic claims.
- Never imply the user is morally weak or that installing an unrelated app proves gambling intent.
- Always distinguish classification from fact: “This demo app matched the pre-authorized policy” rather than “You tried to gamble.”

## Demo Failure Recovery

- If cloud classification is slow, show the event as queued and explain that local quarantine is already active; then use a recorded successful cloud trace.
- If the package broadcast fails, install through the scripted emulator path and show the local event log; do not simulate a success screen without an underlying event.
- If accessibility redirect is inconsistent, cut to the tested reference emulator image and state the supported environment.
- If the agent returns malformed output, use that as the safety demo: quarantine remains and the validator records `REVIEW`.
