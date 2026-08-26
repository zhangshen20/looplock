# Project Scope

## Project Name

**Hi, Stop Gambling — LoopLock**

Submission shorthand: **LoopLock**.

## One-Line Summary

An Android-first recovery firewall that turns each pre-authorized loophole attempt into a stronger local protection rule, while making it technically impossible for the AI agent to unlock or weaken the commitment.

## Target User

### Primary user

An adult who recognizes a recurring online-gambling pattern, voluntarily wants externalized friction during high-urge periods, owns an Android phone, and can create a recovery policy while calm.

This user is not assumed to be technically sophisticated. They need plain consent, visible permissions, predictable consequences, a humane delayed recovery path, and no shame-based language.

### Not the primary user

- A parent covertly monitoring a child.
- A partner, clinician, employer, insurer, or government imposing controls.
- Someone in an acute crisis who needs emergency or clinical care.
- A person seeking a complete, tamper-proof consumer-device guarantee.

## Exact Job To Be Done

> When an urge pushes me to bypass the limits I chose while calm, help my phone preserve that earlier decision and learn from the workaround, without exposing my activity or letting automation weaken my protection.

## Problem

Static blockers fail at the moment that matters: the user finds a new app, mirror, browser route, or permission workaround, and the blocker remains unchanged. Recommendation companions can offer a pause, but they do not preserve the user's earlier decision. Existing device-control mechanisms also create a dangerous temptation to overclaim: a normal Android app cannot guarantee it is impossible to disable, revoke, or uninstall.

## Differentiated Value Proposition

**Static blockers remember a list. LoopLock remembers the loophole.**

The product combines:

1. a calm-state, time-bound policy;
2. deterministic local enforcement;
3. a one-way rule ratchet;
4. bounded agent classification;
5. pre-authorized, minimal accountability;
6. a delayed, explicit recovery path.

The AI is useful because it interprets new events and proposes the next protection rule. It is safe because it has no path to remove protection.

## Hackathon Time Budget

Planning assumption: one primary Codex-assisted build lane over three to four calendar days, with roughly six to ten human hands-on hours for environment setup, permissions, reviews, cloud authorization, and demo/submission work. The internal submission target remains 28 August 2026. This is the ruler for every scope decision; it is not an estimate of 35 hours of manual coding.

## Hackathon MVP

### Must ship

- Android onboarding with a clear research-prototype notice and permission disclosure.
- Selection of one harmless demo app to block.
- A five-minute demo commitment representing a 24-hour production commitment.
- Immutable commitment state while active: end time cannot move earlier and rules cannot be removed.
- Local detection and blocking of the selected demo app.
- Optional, separately consented policy to quarantine newly installed apps during the commitment.
- A harmless second demo app installed while the policy is active.
- Local quarantine before cloud classification.
- Minimal event sent to an ADK service on Cloud Run using Gemini 3.5 Flash.
- Agent result constrained to `TIGHTEN` or `REVIEW`.
- Local validator that applies only additive, in-scope rules and rejects weakening commands.
- Firestore record with an anonymous demo/session ID, event hash, classification, and timestamps; no account or raw behavioral history.
- Offline second-launch proof that the new rule is enforced locally.
- Local timeline showing what happened and why.
- Architecture diagram, reproducible README, test evidence, and visible Cloud Run proof.

### Should ship if the core loop is stable

- Simulated accountability escalation preview containing only attempt count, time window, and escalation level.
- Delayed recovery request UI that cannot complete before the commitment expires.
- Service-disabled warning and explicit statement of consumer-mode limits.
- Scripted malicious agent responses used to prove the validator rejects unlock or expiry-shortening requests.

### Stretch only

- Local site blocking through `VpnService` for one controlled demo domain.
- Pub/Sub between API ingestion and classification.
- Signed cloud rule proposals.
- A minimal hosted status page.

## Production Vision

- Broader app and domain rules with curated data and human review.
- On-device classification where practical.
- Robust delayed recovery, trusted-contact configuration, and consent receipts.
- Privacy-preserving accountability delivery.
- Clinical and lived-experience co-design, safety evaluation, and false-positive operations.
- Consumer mode with honest bypass limits.
- Separately designed managed-device strong mode using device-owner controls on appropriately provisioned devices.
- Store-policy review, legal review, data-protection assessment, accessibility testing, and incident response.

## Explicit Non-Goals Before the Deadline

- No iOS build.
- No claim of tamper-proof consumer enforcement.
- No production release or Play Store submission.
- No real gambling-site crawling, account integration, financial data, payment interception, or transaction control.
- No covert monitoring, screen content capture, keystroke capture, or contact-list upload.
- No agent ability to unlock, shorten commitments, safe-list apps, or suppress local events.
- No clinician dashboard, partner surveillance, or live accountability message.
- No diagnosis, treatment claim, crisis intervention, or promise to prevent gambling harm.
- No multi-agent system merely for architectural spectacle.

## Core Demo Path

1. User selects **BetBurst Demo** and creates a five-minute commitment.
2. User launches it; LoopLock blocks locally and records an event.
3. User installs **LuckyMirror Demo** as a workaround.
4. Android reports a new package; LoopLock quarantines it immediately under the user's pre-authorized rule.
5. The event reaches the ADK service on Cloud Run; Gemini classifies the app from its harmless demo metadata.
6. The agent proposes `TIGHTEN(package=LuckyMirror Demo)`.
7. The local validator accepts the additive rule and persists it.
8. Network is disabled; the user launches LuckyMirror again and it is still blocked.
9. The timeline shows the ratchet and the Cloud Run/Firestore evidence proves the agent workflow ran.
10. A deliberately invalid `UNLOCK` proposal is rejected to make the safety boundary visible.

## Definition of Done

The project is done when the complete demo can be performed twice on the reference emulator, the second launch remains blocked offline, every weakening-command test passes, setup is reproducible from a clean checkout, and all mandatory Devpost assets are ready without relying on unsupported claims.
