# Devpost Handoff — Owner Review Draft

Status: local draft only. It does not authorize updating the live project, uploading files, publishing a repository/video, or submitting.

Live event metadata was refreshed on 26 August 2026. Devpost reports submissions open until **31 August 2026 at 5:00 PM Pacific Time** (**1 September at 10:00 AM Melbourne**). A public YouTube/Vimeo video and an uploaded PNG/JPG/PDF architecture diagram are required; a hosted project URL is optional. The current live project remains the stale breathing/walking draft and has no video.

## Recommended project identity

**Name:** Hi, Stop Gambling — LoopLock

**Tagline:** An Android recovery firewall where every attempted loophole can become the next local protection rule—and AI can never unlock it.

**Category:** Taskmaster

## Description draft

### Inspiration

Static gambling blockers remember a list. The difficult moment comes when a person finds a new app, mirror, or other workaround and the blocker remains unchanged. LoopLock asks a more precise question: how can a phone preserve the decision someone made while calm, adapt to a new loophole, and still ensure automation can never weaken protection?

LoopLock is designed for voluntary adult self-use on a personally owned Android device. It is a research prototype, not treatment, crisis support, surveillance, or a promise of tamper-proof protection.

### What it does

The user reviews and starts a time-bound recovery commitment. Deterministic Android code blocks the selected harmless BetBurst fixture locally. If the user separately pre-authorized new-install quarantine, installing the harmless LuckyMirror fixture creates a quarantine rule before any network request.

A retryable background workflow sends one bounded event to a private Cloud Run service. A Google ADK agent using Gemini 3.5 Flash classifies only the controlled fixture metadata and can return only `TIGHTEN` or `REVIEW`. Firestore stores one minimal idempotent event/result record. The phone then applies a closed monotonic validator: a matching additive rule may be accepted; malformed, late, mismatched, duplicate, or weakening output retains quarantine and cannot modify the commitment.

The demo disables network access and proves the accepted LuckyMirror rule still blocks from local state. It also sends a deliberately invalid `UNLOCK` proposal through the real local validator and shows `ACTION_NOT_ALLOWED` with the same end time and retained rule.

### How we built it

- Native Kotlin, Jetpack Compose, Room, WorkManager, and a package-only Android accessibility service for the consumer proof of concept.
- Versioned JSON Schemas shared by Kotlin and Python for a closed event and proposal contract.
- FastAPI plus Google Agent Development Kit on a private Cloud Run service.
- Gemini 3.5 Flash through Vertex AI for bounded fixture classification.
- Firestore for minimal, idempotent, lease-fenced event/result state.
- An IAM-authenticated Mac proxy with `adb reverse` for demo connectivity; it is explicitly not the production mobile-authentication design.
- Harmless repository-owned fixture APKs with no gambling mechanics, accounts, payments, advertising, analytics, network access, or third-party assets.

The enforcement trust boundary is intentionally asymmetric: Android can create, persist, validate, and enforce rules. The agent can propose a tighter rule but has no tool, credential, endpoint, or code path that can unlock, delete, shorten, disable, or directly enforce one.

### Challenges

The hardest product decision was being honest about Android. A normal consumer app cannot prevent a user from revoking accessibility permission or uninstalling it, and Google Play policy imposes important limits on non-accessibility use of accessibility services and package visibility. LoopLock therefore reports `Action required` when permission is gone and treats managed-device strong mode as separate future work.

The hardest engineering work was making retries safe. Local quarantine must exist before upload; cloud processing must be idempotent; raw metadata must be scrubbed after terminal processing; duplicate or late responses must be no-ops; and wall-clock rollback must not cause early expiry. Those properties are tested across Kotlin, on-device Room tests, and Python.

### Accomplishments

- Two clean end-to-end rehearsals passed through Android, the private Cloud Run service, Google ADK, Gemini 3.5 Flash, and Firestore.
- Local quarantine preceded upload, and an accepted additive rule continued blocking offline.
- Three identical cloud retries produced one minimal terminal Firestore document.
- A visible `UNLOCK` attempt was rejected without changing the commitment end or protection rules.
- The current suite passes 27 Android JVM tests, 14 API-36 on-device tests, and 28 backend tests.
- The product collects no screen content, typed text, screenshots, messages, contacts, financial data, full app inventory, or real gambling activity.

### What we learned

Agent safety is clearer when authority is designed out, not merely requested in a prompt. Gemini is valuable here because it interprets a new event inside a small closed action space. The phone remains responsible for policy monotonicity and enforcement. We also learned that privacy and idempotency reinforce each other: a stable opaque event ID and target hash are sufficient to prove retry behavior without storing an account or long-form activity history.

### What's next

Production work would require lived-experience and clinical co-design, false-positive review and appeals, security and privacy assessment, accessibility and Google Play policy review, broader curated app/domain intelligence, delayed recovery governance, and a separately designed managed-device mode. Site blocking and arbitrary installed-app discovery are not claims of this hackathon build.

### Data sources and third-party code

The working demo uses only repository-owned fake package names and metadata; it does not use a gambling provider, third-party gambling dataset, account, payment, or personal activity. Standard open-source dependencies are declared and pinned in Gradle and `backend/uv.lock`, including AndroidX/Jetpack, Kotlin serialization, Retrofit/OkHttp, FastAPI, Pydantic, Google ADK, and the Google Cloud Firestore client. The project was newly built during the submission period with Codex used as a coding and documentation assistant.

## Built with

`Android`, `Kotlin`, `Jetpack Compose`, `Room`, `WorkManager`, `Google ADK`, `Gemini 3.5 Flash`, `Vertex AI`, `Cloud Run`, `Firestore`, `FastAPI`, `Python`, `Docker`

## Submission field inventory

| Devpost field | Draft answer / owner action |
| --- | --- |
| Submitter type | Individual — owner advanced the recommended defaults on 26 August 2026 |
| Country of residence | Australia — owner advanced the recommended defaults on 26 August 2026 |
| Category | Taskmaster |
| Organization name | `N/A` if Devpost accepts it for a non-organization; otherwise owner supplies the truthful value |
| Project start date | `08-19-26` — owner advanced the recommended defaults on 26 August 2026 |
| Code repository URL | https://github.com/zhangshen20/looplock |
| Reproducible README | Yes, after clean-checkout gate is complete |
| Hosted project URL | Leave blank; mobile APK and private backend are not a public hosted product |
| Private testing instructions | Use the concise draft below |
| Google SDK | Agent Development Kit (ADK) |
| Google Cloud services | Cloud Run; Firestore |
| Architecture diagram | Upload `docs/submission/architecture.png` or `.pdf` after owner review |
| Google AI models | Gemini 3.5 Flash through Vertex AI |
| Demo video | Public YouTube chosen; recording/upload and incognito check remain pending |
| Optional startup prize | Do not opt in |
| Optional bonus content/social | Leave blank |

### Private testing-instructions draft

Clone the repository and run `make verify`. Start the documented API-36 ARM64 emulator, run `make install-fixtures`, and follow README steps 2–3 for the local consent and BetBurst block. The real classification endpoint is private and intentionally requires an authorized IAM operator; judges can inspect the contract/fake-backed tests without credentials. The public demo video shows the previously verified private Cloud Run/ADK/Gemini/Firestore path and matching event ID. No credential is embedded in the APK or repository.

## Owner approval gates

- Confirm the published repository is readable while signed out and record the final commit.
- Review every product, safety, test-count, and deployment claim against the final recording.
- Approve the final architecture upload, thumbnail/screenshots, public video, and exact repository link.
- Run the incognito checks and final secret/privacy scan.
- Update the stale live draft only after the required identity fields and links are ready.
- The owner—not an automated build step—performs the final Devpost submission.
