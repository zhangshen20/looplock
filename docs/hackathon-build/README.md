# LoopLock Product Pack

Start here:

1. [Evidence and decisions](evidence-and-decisions.md) — live event facts, Android/Play constraints, assumptions, and go/no-go gates.
2. [Scope](scope.md) — target user, job to be done, MVP, production vision, non-goals, and demo path.
3. [PRD](prd.md) — product behavior, stories, state rules, edge cases, and submission proof.
4. [Technical specification](spec.md) — stack, architecture, repository shape, contracts, data flow, testing, and deployment.
5. [User journey](user-journey.md) — first-run experience and four-minute demo storyboard.
6. [Architecture boundary](architecture-boundary.md) — deterministic local enforcement versus bounded cloud classification.
7. [Prioritized backlog](backlog.md) — P0/P1/P2 and explicit cuts.
8. [Acceptance criteria](acceptance-criteria.md) — testable release contract.
9. [Risk register](risks.md) — safety, Android, Play, privacy, delivery, and mode distinctions.
10. [Measurement plan](measurement-plan.md) — hackathon scorecard and later product hypotheses.
11. [Delivery sequence](delivery-sequence.md) — daily path to the 28 August internal target.
12. [Build checklist](checklist.md) — sequenced implementation and verification contract.
13. [Build notes](build-notes.md) — assumptions, decisions, and active-shaping history.

## Current Recommendation

Build the app-install ratchet first: selected demo app blocked locally; newly installed demo app quarantined; ADK/Gemini proposes an additive rule; the Android validator rejects weakening commands; the second launch remains blocked offline.

Do not put site blocking, live accountability messages, device-owner provisioning, or Play Store publication on the critical path.

## Status

- Product artifacts: drafted and consistency-checked.
- Owner decisions: confirmed — one primary Codex-assisted build lane over three to four calendar days, roughly six to ten human hands-on hours, voluntary adult self-use, app-first MVP, 24-hour product default, and milestone pauses.
- Technical specification: complete; core implementation and safety verification are complete.
- Build checklist: items 1–9 complete. The integrated ratchet, honest service state, monotonic expiry, delayed-recovery information, local-only accountability preview, and failure-mode regression matrix are verified.
- Next gate: item 10, the Devpost handoff, must finish the sanitized four-minute recording and Cloud screenshots, publish the owner-approved video, and replace the stale submission copy. Nothing has been submitted.
- Git repository: public at https://github.com/zhangshen20/looplock; `main` is checked from an unauthenticated client after publication.
- Google Cloud: private revision `looplock-agent-00004-ztf` is serving in project `looplock-hackathon-2026-v8k3`; the Standard Native-mode `(default)` Firestore database is in `australia-southeast1`, the keyless runtime identity has only `roles/aiplatform.user` and `roles/datastore.user`, and no public service access exists.
- Devpost: draft exists but was not edited or submitted in this work.
