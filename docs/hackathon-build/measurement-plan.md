# Measurement Plan

## Measurement Philosophy

LoopLock should not optimize for more blocked attempts, longer lock periods, shame, fear, or engagement. The hackathon measures whether the safety contract and end-to-end workflow work. Production evaluation would measure user-chosen protection, false positives, distress, and trust with lived-experience and clinical oversight.

## Hackathon Success Questions

1. Can a user create an informed, time-bound commitment?
2. Does deterministic local enforcement work without the cloud?
3. Does a new, pre-authorized loophole event become an additive rule?
4. Can the agent be proven unable to weaken protection?
5. Is the cloud workflow real, minimal, reproducible, and visible?
6. Can a judge understand the value and boundary in four minutes?

## MVP Scorecard

| Metric | Definition | Target | Evidence |
| --- | --- | --- | --- |
| Known-app block reliability | Successful blocks / 10 launches on reference emulator | 10/10 | Screen recording and local test log |
| Local block independence | Successful blocks in airplane mode / 5 launches | 5/5 | Manual verification |
| Install quarantine reliability | Quarantine present before launch / 10 installs or reinstalls | 10/10 | Event timestamps |
| Ratchet completion | Event reaches accepted local additive rule | 5/5 runs | Android, Cloud Run, Firestore IDs |
| Weakening rejection | Invalid actions rejected / test cases | 100% | Automated validator tests |
| Idempotency | Duplicate rules after three retries of same event | 0 | Integration test |
| Early-expiry regressions | Clock/restart tests that shorten commitment | 0 | State transition tests |
| Persistent raw identifiers | Raw package identifiers stored in Firestore | 0 | Firestore field inspection |
| Demo duration | Judged content length | <= 4:00 | Final video |
| Clean reproduction | Fresh-checkout successful setup | 1 complete independent run | README checklist |

## Event Taxonomy

Local events:

- `commitment_created`
- `commitment_activated`
- `block_attempted`
- `package_quarantined`
- `classification_queued`
- `classification_received`
- `rule_tightened`
- `proposal_rejected`
- `permission_changed`
- `commitment_expired`

Cloud events should use anonymous event and commitment IDs. Do not attach a person, contact, device advertising ID, full installed-app list, or raw local timeline.

## Product Hypotheses for Later Research

These are not hackathon claims:

| Hypothesis | Signal | Guardrail |
| --- | --- | --- |
| Calm-state commitments create useful friction during urges | User reports the policy helped preserve their earlier intention | Distress and “felt trapped” rate |
| The ratchet is more useful than a static list | Fewer successful workaround paths after an attempt | False-positive quarantine rate |
| Minimal accountability can help without surveillance | User-reported support and trust | Target-detail disclosure rate must remain zero by default |
| Delayed recovery feels fairer than an instant override or hidden lock | Recovery comprehension and trust | Support requests and early-exit distress |

Production research would require opt-in studies, qualitative interviews, safety review, and clear crisis/support routing. It should not infer gambling episodes solely from app events.

## Data Retention for the Demo

- Local timeline: stored on the emulator for the demo; resettable after the commitment.
- Firestore: minimal event/result proof with a short, documented retention window; delete after judging when no longer required.
- Cloud logs: do not log request bodies or raw package metadata.
- Video: use fake apps and anonymous demo identifiers only.

## Decision Rules

- If weakening rejection is below 100%, stop feature work and fix the validator.
- If block or quarantine reliability is below 90% by 23 August, narrow the supported emulator/API level and disclose it.
- If the agent adds latency but no visible product decision, simplify the workflow rather than add more agents.
- If privacy inspection finds raw persistent identifiers, remove them before recording.
- If the demo cannot fit four minutes, cut setup footage and P1 features, not safety proof.
