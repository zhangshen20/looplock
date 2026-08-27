# Submission Evidence Checklist — Draft

Status: engineering acceptance passed twice on 25 August 2026; item-9 safety regression passed on 26 August; an event-locked producer dry run passed on 27 August. Submission capture, final sanitization, public links, and owner review remain pending.

## Current producer staging — 27 August 2026

- [x] APKs rebuilt successfully and the API-36 reference emulator booted.
- [x] Residual LuckyMirror was removed and LoopLock local demo data was cleared; the settled app opens on the first disclosure.
- [x] The shot-by-shot producer cut targets 3:48 and its narration is approximately 340 spoken words.
- [x] After the application-ready wording update, a private synthesized narration rehearsal measured 2:29.6 at 145 words per minute, leaving 78.4 seconds for proof holds and transitions.
- [x] Local submission preflight passes; the public video URL is the expected remaining external-link gate.
- [x] Owner approved the no-webcam narrated format, local accessibility enablement, and one use of the existing private demo service for the dry run.
- [x] One technical dry run passed the hook mechanics, matching-event, offline, weakening-rejection, and privacy gates. Android event prefix `d6f897fa` matched its terminal Firestore document.
- [ ] One full screen-and-voice stopwatch rehearsal ends by 3:48 with every proof frame legible.
- [ ] Owner gives fresh authorization before recording or using the private service for a recording take.
- [ ] The final recording passes the review in this checklist.

## Ratchet run record

- [x] Run 1 starts without LuckyMirror installed.
- [x] Run 1 shows local quarantine before upload, one event ID, accepted additive rule, then an offline block.
- [x] Run 2 repeats from a clean commitment with no duplicate additive rule or misleading timeline entry.
- [x] The weakening fixture records rejection and leaves the commitment end unchanged.
- [x] Permission-disabled state says `Action required`, not `Protected`.

## Google proof

- [ ] Private Cloud Run service/revision and `australia-southeast1` are visible.
- [ ] Google ADK and Gemini 3.5 Flash are visible in configuration or sanitized logs.
- [ ] One Firestore document matches the opaque Android event ID and contains only approved hashes, bounded result fields, and timestamps.
- [ ] No public invocation grant is present.
- [ ] The IAM-authenticated Mac proxy is labeled demo-only on port 8081.

## Privacy and recording review

- [ ] No email address, access token, credential, billing detail, unrelated project, raw request body, package label, prompt, model reasoning, or user activity appears.
- [ ] Fixture apps are visibly labeled harmless demos; no real gambling service or third-party asset appears.
- [ ] The architecture shows no Cloud-to-enforcer or Cloud-to-local-store authority.
- [ ] Consumer-mode bypassability and managed-device future scope are stated.
- [ ] Judged content is public, in English or subtitled, and no longer than four minutes only after owner review.

## Reproducibility gate

- [x] A fresh clone of the public `main` branch passed `make submission-preflight` and `make verify` without undocumented files or credentials.
- [x] A clean-source rehearsal excluding build outputs, caches, `local.properties`, `.env`, and `.git` passed `make verify`, assembled all three APKs, and served `/healthz` on 26 August 2026.
- [x] Android and backend automated tests pass.
- [x] Initial installation excludes LuckyMirror; the later workaround script installs it only during the active commitment.
- [x] The public repository and raw README return HTTP 200 without GitHub credentials.
- [ ] The public video and final architecture link work in an incognito session.
- [x] Final repository secret scan passed immediately before publication.

## Devpost field gate

- [x] Live dates, deliverables, custom fields, criteria, and latest announcements were refreshed on 26 August 2026.
- [x] Local description draft replaces the stale breathing/walking companion with the truthful LoopLock implementation.
- [x] Draft consistently names Taskmaster, Gemini 3.5 Flash, Google ADK, Cloud Run, and Firestore.
- [x] Owner advanced the recommended defaults: individual, Australia, `08-19-26`, and no optional-prize/content entries.
- [x] Owner chose a public GitHub repository and public YouTube video; repository creation/publication was explicitly authorized and completed.
- [x] Upload-ready architecture file was visually approved by the owner.
- [ ] Public video is timed, processed, owner-reviewed, and checked while signed out.
