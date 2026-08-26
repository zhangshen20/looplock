# Product Requirements Document

## Product Summary

LoopLock is a voluntary Android recovery firewall for adults experiencing online-gambling urges. The user makes a time-bound policy while calm. During that commitment, deterministic local code enforces selected rules and may quarantine newly installed apps if the user explicitly pre-authorized that behavior. A cloud agent can classify a new event and propose a narrower protection rule, but it cannot unlock, delete, shorten, or weaken anything.

The hackathon product is a research proof of concept, not treatment, crisis support, a production security product, or a promise of tamper-proof protection.

## Product Principles

1. **Calm-state consent comes first.** Every restrictive behavior is explained and chosen before commitment.
2. **Enforcement is deterministic and local.** Network loss must not remove an active rule.
3. **The ratchet moves one way.** During commitment, the policy can stay the same or become stricter, never weaker.
4. **AI advises within a closed action space.** Only `TIGHTEN` and `REVIEW` are valid agent outputs.
5. **False positives fail safely and visibly.** An uncertain app stays quarantined; it is not secretly allowed or permanently condemned.
6. **No covert surveillance.** Do not capture screens, typed text, messages, contacts, or full browsing history.
7. **Recovery is delayed, not hidden.** The user can see how and when a commitment ends and can request later review.
8. **Limitations are part of the product.** Consumer mode is bypassable if permissions are revoked or the app is removed.

## Primary User

An adult on a personally owned Android device who voluntarily wants extra friction during a self-chosen recovery interval.

## Core User Journey

1. Understand the prototype, data use, permissions, and consumer-mode limits.
2. Select a known demo app to block.
3. Optionally pre-authorize quarantine of apps installed during the commitment.
4. Review a plain-language policy summary and start a five-minute demo commitment.
5. Encounter a local block when launching the selected app.
6. Install a second demo app as an attempted workaround.
7. See that it is quarantined immediately, before cloud classification.
8. See the agent classification and additive rule in the local timeline.
9. Relaunch offline and remain blocked.
10. See the commitment end time and delayed recovery path without any instant override.

## Domain Model

### Commitment

- `commitment_id`
- `created_at`
- `starts_at`
- `ends_at`
- `status`: `DRAFT | ACTIVE | EXPIRED`
- `quarantine_new_installs`: boolean chosen before activation
- immutable consent receipt version

### Rule

- `rule_id`
- `commitment_id`
- `target_type`: `PACKAGE` for MVP
- `target_value`: package name
- `source`: `USER_SELECTED | QUARANTINE | AGENT_TIGHTENED`
- `created_at`
- `expires_at`: exactly the commitment end for MVP

### Protection event

- `event_id`
- `commitment_id`
- `event_type`: `BLOCK_ATTEMPT | PACKAGE_ADDED | AGENT_RESULT | VALIDATION_REJECTED | SERVICE_STATE`
- `target_hash`
- local timestamp
- upload state

Raw package metadata may be held only long enough to classify the demo event. Firestore stores the event hash and result, not a user account or behavioral history.

## Epic 1 — Informed Setup

### Story 1.1: Understand the boundary

As a prospective user, I want a short explanation of what LoopLock can observe, block, and fail to prevent so that my consent is meaningful.

Acceptance summary:

- The first run distinguishes research prototype, consumer mode, and managed-device vision.
- Accessibility and package-event use are disclosed immediately before permission requests.
- The UI states that permissions can be revoked and the app can be removed.
- No permission is requested before the disclosure is accepted.

### Story 1.2: Choose a policy while calm

As a user, I want to select a demo app, duration, and optional new-install quarantine so that the active policy reflects my choice.

Acceptance summary:

- The review screen names the app, start/end time, quarantine choice, delayed recovery, data sent, and data not collected.
- Starting requires a deliberate confirmation.
- The commitment becomes read-only after activation.

## Epic 2 — Deterministic Local Enforcement

### Story 2.1: Block a selected app

As a committed user, I want the selected demo app interrupted locally so that protection does not depend on cloud availability.

Acceptance summary:

- Launching the selected package while active presents the LoopLock block experience before the demo app can be used.
- The attempt is recorded locally once.
- Airplane mode does not change the outcome.
- After expiry, the demo policy no longer blocks.

### Story 2.2: See service health

As a user, I want to know whether required Android permissions are active so that the product does not imply protection it cannot provide.

Acceptance summary:

- Home screen shows `Protected`, `Action required`, or `Expired`.
- Revoking the service changes status to `Action required` without claiming the commitment is enforced.

## Epic 3 — Adaptive One-Way Ratchet

### Story 3.1: Quarantine a newly installed app

As a user who pre-authorized new-install quarantine, I want a new package held locally until it is reviewed so that installing a workaround does not create an immediate gap.

Acceptance summary:

- A package-added event during an active commitment creates a local quarantine rule before any network call.
- If quarantine was not pre-authorized, the package is not blocked and no cloud event is sent.
- Existing apps are not silently swept into quarantine.

### Story 3.2: Classify through the agent

As a user, I want LoopLock to interpret a quarantined demo app and propose the next rule so that the protection adapts without me guiding a chat.

Acceptance summary:

- Cloud Run invokes ADK with Gemini 3.5 Flash.
- Input is limited to anonymous session/event ID and demo package metadata.
- Output conforms to the closed schema: `TIGHTEN` or `REVIEW`, classification, confidence, and short reason.
- Firestore proves receipt and result without storing a personal account.

### Story 3.3: Reject weakening proposals

As a user, I need the phone—not the model—to decide whether a proposal is safe so that prompt errors cannot weaken protection.

Acceptance summary:

- The local validator rejects unknown actions and all changes that remove rules, shorten expiry, change commitment start/end, or allow-list a target.
- A rejected proposal is logged and shown in the demo timeline.
- The agent has no tool, endpoint, or credential that writes directly to the local rule store.

## Epic 4 — Humane Accountability and Recovery

### Story 4.1: Understand what happened

As a user, I want a factual local timeline so that I can see protection events without shame or opaque “AI reasoning.”

Acceptance summary:

- Entries use neutral language: blocked, quarantined, classified, tightened, review needed, or permission changed.
- Raw model reasoning and hidden chain-of-thought are not displayed or stored.
- No destination or package name is included in an accountability preview by default.

### Story 4.2: Preview pre-authorized escalation

As a user, I want to know what an accountability contact would receive so that escalation is never covert.

Acceptance summary:

- MVP displays a preview only; it sends nothing.
- Payload contains only a time window, attempt count, and escalation level.
- The UI clearly labels the feature “Demo only — not sent.”

### Story 4.3: Use the recovery path

As a user, I want a visible delayed recovery path so that I am not trapped by an opaque system.

Acceptance summary:

- The app shows the exact commitment end time.
- A recovery request cannot weaken the active demo policy.
- The request may be recorded for review after expiry; no hidden master override exists.

## Epic 5 — Submission Proof

### Story 5.1: Reproduce and verify

As a judge, I want to understand, run, and inspect the system so that the claims are credible.

Acceptance summary:

- README starts from a clean machine and names prerequisites, secrets, emulator setup, backend deployment, demo APKs, and verification steps.
- Automated tests cover the rule validator and state transitions.
- The diagram separates local enforcement from cloud classification.
- The video names Gemini 3.5 Flash, ADK, Cloud Run, and Firestore and shows Cloud proof.

## Required Screens

1. Welcome and safety boundary.
2. Permission disclosures and system-setting launch.
3. App selection using a targeted demo-package list.
4. Commitment review and activation.
5. Protection status/home.
6. Block experience.
7. Quarantine/classification status.
8. Local event timeline.
9. Delayed recovery information.
10. Demo diagnostics showing backend event ID and cloud result.

## State and Transition Rules

- `DRAFT -> ACTIVE` only after explicit confirmation and required permission checks.
- `ACTIVE -> EXPIRED` only when trusted elapsed time reaches `ends_at`; reboot or wall-clock rollback must not shorten the interval in the reference test.
- An active rule may be added or made equal/more restrictive.
- No active rule may be deleted, disabled, allow-listed, or assigned an earlier expiry.
- Network failure queues classification but does not remove quarantine.
- `REVIEW` leaves quarantine unchanged.
- Missing/invalid agent output is treated as `REVIEW`.

## Edge Cases

- **Permission denied before activation:** do not start; explain what is missing.
- **Permission revoked during commitment:** show `Action required`; never claim the app is still enforcing.
- **Network unavailable:** quarantine locally, queue event, and retry later with idempotent event ID.
- **Cloud duplicate:** do not create duplicate rules or timeline events.
- **App uninstalled/reinstalled:** package target remains blocked until commitment expiry.
- **Legitimate new app:** remains quarantined under the explicit strict setting; user sees `Review needed`, not a gambling accusation.
- **Gemini returns malformed or weakening output:** reject locally and retain quarantine.
- **Clock changed:** use monotonic elapsed time plus persisted wall time for the demo; flag inconsistent time rather than expiring early.
- **Accessibility service restarts:** reload active rules from local storage before reporting `Protected`.
- **Unknown package label or metadata:** do not infer; return `REVIEW`.
- **Commitment expires during classification:** ignore late proposals for that commitment and preserve the audit record.

## Non-Functional Requirements

- No secret or service-account key in the APK or repository.
- Cloud endpoint authenticated or protected by a short-lived demo mechanism; it must not be an unrestricted expensive public classifier.
- Local rules persist across app process restart and device reboot on the reference emulator.
- The block experience appears before meaningful interaction with the demo app in repeat tests.
- Every cloud event is idempotent.
- No screen content, keystrokes, contacts, messages, payment data, or full installed-app list is collected.
- UI copy is neutral and does not diagnose, shame, or promise harm prevention.

## What We Would Add With More Time

- Curated domain and app intelligence with human appeals.
- Local VPN site blocking with explicit browser scope.
- On-device models for classification.
- Cryptographically signed proposals and stronger attestation.
- Real accountability delivery with contact consent and revocation policy.
- Lived-experience research, clinician review, false-positive operations, and accessibility audit.
- Managed-device mode with separate enrollment, governance, and anti-coercion design.

## Submission Proof Points

- The agent reacts to a real Android package event without a conversational prompt.
- Local quarantine protects immediately, even before the cloud responds.
- The model can tighten but cannot unlock.
- The tightened rule works offline.
- The architecture visibly uses Gemini 3.5 Flash, ADK, Cloud Run, and Firestore.
- The demo is candid about consumer-mode bypasses and Play-policy uncertainty.
