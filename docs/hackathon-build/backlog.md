# Prioritized Product Backlog

Priority rule: P0 creates the honest end-to-end demo. P1 improves safety and submission confidence. P2 is optional. Anything that threatens P0 is cut.

## P0 — Must Ship

| ID | Outcome | Why now | Dependency |
| --- | --- | --- | --- |
| P0-01 | Create Android project, reference emulator, and two harmless demo app packages | Establishes the real demo surface | None |
| P0-02 | Implement first-run boundary, data, and permission disclosures | Consent is part of the invention | P0-01 |
| P0-03 | Create policy builder and immutable active commitment | Defines the calm-state contract | P0-02 |
| P0-04 | Persist local rules and commitment across process restart | Makes enforcement independent of UI process | P0-03 |
| P0-05 | Detect foreground demo package and present local block experience | Proves deterministic enforcement | P0-04 |
| P0-06 | Detect package installation and apply pre-authorized local quarantine | Creates the bounded loophole event | P0-03 |
| P0-07 | Build the closed rule-proposal schema and local monotonic validator | Makes “AI cannot unlock” executable | P0-04 |
| P0-08 | Deploy ADK + Gemini 3.5 Flash classification endpoint to Cloud Run | Meets stack and agent requirement | P0-07 |
| P0-09 | Store minimal idempotent event/results in Firestore | Proves state and cloud operation | P0-08 |
| P0-10 | Connect queued Android event to cloud result and additive rule | Completes the ratchet loop | P0-06, P0-09 |
| P0-11 | Build neutral local timeline and demo diagnostics | Makes the workflow understandable | P0-05, P0-10 |
| P0-12 | Add automated validator/state tests and repeat demo test | Supports safety and reproducibility claims | P0-07, P0-10 |
| P0-13 | Produce README, architecture diagram, demo script, and Cloud proof | Mandatory submission evidence | All P0 build work |

## P1 — Should Ship

| ID | Outcome | Cut condition |
| --- | --- | --- |
| P1-01 | Delayed recovery request UI with exact end time | Cut if state transition tests are incomplete |
| P1-02 | Service-disabled warning and honest protection status | Do not cut if status currently overclaims protection |
| P1-03 | Accountability payload preview with “not sent” label | Cut before adding any real messaging integration |
| P1-04 | Auth/rate limit for Cloud Run classifier | Reduce demo exposure rather than leave an expensive endpoint open |
| P1-05 | Duplicate event and late-result handling | Promote to P0 if observed in integration tests |
| P1-06 | Accessibility and large-text pass for primary demo screens | Cut decorative polish first |

## P2 — Could Ship

| ID | Outcome | Time box |
| --- | --- | --- |
| P2-01 | Two-hour `VpnService` spike for one controlled domain | Stop at two hours; no production claim |
| P2-02 | Pub/Sub event decoupling | Only after direct endpoint is stable |
| P2-03 | Signed cloud proposals | Only after validator tests are complete |
| P2-04 | Hosted read-only demo status page | Only if it improves judge comprehension |
| P2-05 | Additional Google model for bonus points | Do not add solely for bonus points |

## Won't Do for This Submission

- iOS.
- Real gambling accounts, sites, transactions, or personal recovery data.
- Live messages to partners or contacts.
- Broad installed-app scanning with `QUERY_ALL_PACKAGES`.
- Full-device VPN coverage.
- Device-owner provisioning.
- Play Store release.
- Clinician dashboard or clinical claims.
- Agent-controlled permissions, settings, or unlock paths.
- Multi-agent orchestration without a demonstrated product need.

## Backlog Guardrails

- A feature cannot enter P0 unless it appears in the four-minute demo or is required for safety/reproducibility.
- A feature that uses a new sensitive permission needs a disclosure, a policy check, and a specific demo benefit.
- No backlog item may create an agent action that weakens policy.
- Submission work starts while the build is still in progress; it is not a final-night phase.
