# LoopLock — an adaptive recovery firewall

LoopLock is an application-ready Android beta built around a simple promise: **static blockers remember a list; LoopLock remembers the loophole.** An adult voluntarily creates a time-bound policy while calm. Deterministic Android code blocks the selected demo app and can immediately quarantine a separately consented new install. A bounded Gemini agent may classify that event and propose one additive rule, but it can never unlock, delete, disable, or shorten protection.

This project targets the **Taskmaster** category of the [All Things Agentic Hackathon](https://allthingsagentichackathon.devpost.com/). It uses **Gemini 3.5 Flash**, **Google Agent Development Kit (ADK)**, **Cloud Run**, and **Firestore**. The live requirements were rechecked on 26 August 2026; submissions remain open until 31 August 2026 at 5:00 PM Pacific Time (1 September at 10:00 AM Melbourne).

> Product boundary: “application-ready” means the product builds, installs, persists its policy, enforces locally, integrates with its private agent service, and passes the documented tests on the supported Android reference environment. It does not mean globally available, Play-approved, clinically validated, or tamper-proof. Consumer mode can be disabled or uninstalled; strong managed-device controls require separate provisioning, governance, and product design.

## What the application-ready beta delivers

1. The user reviews a five-minute demo commitment that represents a longer production policy and explicitly opts into the LuckyMirror install-quarantine scenario.
2. Android locally blocks **BetBurst Demo** without a network or model call.
3. Installing **LuckyMirror Demo** during the commitment creates a local quarantine rule before upload.
4. A retryable worker sends one minimal, bounded event through a demo-only IAM-authenticated Mac proxy.
5. Google ADK invokes Gemini 3.5 Flash on Cloud Run; Firestore provides minimal idempotent event/result state.
6. The phone validates the bounded `TIGHTEN` or `REVIEW` response. Only an event-scoped additive rule can be accepted.
7. LuckyMirror remains blocked from local state while the emulator is offline.
8. A visible `UNLOCK` fixture is rejected and leaves the commitment end and rules unchanged.

The controlled end-to-end ratchet has passed twice from clean commitments against the private reference deployment. The demo fixtures are repository-owned and harmless: they contain no gambling mechanics, accounts, payments, ads, analytics, network permission, or third-party assets.

## Architecture

[Open the upload-ready architecture diagram](docs/submission/architecture.png) or read its [accessible architecture notes](docs/submission/architecture.md).

The Android device is the enforcement trust boundary. Cloud Run, ADK, Gemini, Firestore, and the demo proxy can return a bounded proposal; none can write the local rule store or call the enforcer. The proxy on localhost port 8081 is demo infrastructure, not a production mobile-authentication design.

## Repository map

| Path | Purpose |
| --- | --- |
| `android/app/` | Kotlin/Compose app, Room state, WorkManager transport, local validator, and accessibility-based package-only enforcer |
| `android/fixtures/` | Harmless BetBurst and LuckyMirror fixture APK sources |
| `backend/` | FastAPI service, bounded Google ADK agent, Firestore idempotency, container, and Python tests |
| `contracts/` | Shared versioned request/response JSON Schemas and accepted/rejected fixtures |
| `infra/` | Guarded private Cloud Run/Firestore scripts and the demo-only IAM proxy |
| `docs/hackathon-build/` | Scope, PRD, journey, backlog, acceptance criteria, risks, measurements, spec, checklist, and build evidence |
| `docs/submission/` | Architecture, demo script, evidence gates, and owner-review submission draft |

## Prerequisites

The verified reference environment is macOS on Apple Silicon with:

- Android Studio with its bundled JDK 17;
- Android SDK Platform and Build Tools 36, platform tools (`adb` on `PATH`), emulator, command-line tools, and `system-images;android-36;google_apis;arm64-v8a`;
- Python 3.13 and [uv](https://docs.astral.sh/uv/);
- Docker only for the production-shaped container check;
- Google Cloud CLI only for an authorized private-cloud rehearsal;
- an ARM64 emulator named `LoopLock_API_36` (the helper creates it).

The pinned Gradle wrapper, Python lockfile, and dependency versions live in the repository. Do not add credentials, `local.properties`, `.env`, service-account keys, screenshots with private information, or raw activity data to source control.

## 1. Clone and run automated tests

```bash
git clone https://github.com/zhangshen20/looplock.git
cd looplock
make verify
```

`make verify` runs the Python contract/API/guardrail suite and Android JVM contract, monotonic-policy, safety, persistence-mapper, worker, and transport tests. To run the API-36 on-device Room/policy suite after starting the emulator:

```bash
make android-connected-test
```

Verified on 26 August 2026: 27 Android JVM tests, 14 on-device tests, and 28 backend tests passed.

## 2. Create and start the reference emulator

Install the API-36 ARM64 Google APIs image in Android Studio's SDK Manager, then run:

```bash
android/scripts/create-emulator.sh
"$HOME/Library/Android/sdk/emulator/emulator" -avd LoopLock_API_36
```

Wait for Android to finish booting. If the emulator serial is not `emulator-5554`, export the actual value before using the scripts:

```bash
export ANDROID_SERIAL="emulator-5556"
```

## 3. Build and install the safe initial demo set

```bash
make install-fixtures
adb shell am start -W -n com.histopgambling.looplock/.MainActivity
```

The initial install deliberately contains only LoopLock and BetBurst. Keep LuckyMirror uninstalled until the active-commitment workaround step.

In LoopLock:

1. read and continue through the research and accessibility disclosures;
2. enable **Quarantine the LuckyMirror demo if installed**;
3. review and start the five-minute commitment;
4. open Android accessibility settings from LoopLock and explicitly enable its service;
5. return to LoopLock and confirm the status says `Protected`;
6. launch BetBurst Demo and observe the clearly labeled local block overlay.

Revoking accessibility access must change the status to `Action required`; the app must never claim protection when its consumer-mode enforcer is unavailable.

## 4. Run the backend locally

The health route starts without cloud credentials:

```bash
make backend-run
curl http://127.0.0.1:8080/healthz
```

Expected response:

```json
{"status":"ok","service":"looplock-agent"}
```

Build the production-shaped container if Docker is available:

```bash
make backend-container
docker run --rm -p 8080:8080 looplock-agent:local
curl http://127.0.0.1:8080/healthz
```

Classification intentionally requires Application Default Credentials, an approved Google Cloud project, Vertex AI, and Firestore. Tests inject closed fakes so local verification never fabricates a cloud success.

## 5. Reproduce the real private-cloud ratchet

This optional step requires an operator who already has IAM access to an approved deployment. The Android APK receives no Google credential. Do not expose the service publicly or copy a user credential into the repository or APK.

Terminal A:

```bash
GOOGLE_CLOUD_PROJECT="looplock-hackathon-2026-v8k3" ./infra/start-demo-proxy.sh
```

Terminal B:

```bash
android/scripts/connect-demo-proxy.sh
android/scripts/install-workaround-fixture.sh
```

The package-added receiver should create the local quarantine first, then WorkManager retries the bounded request through `adb reverse` to localhost port 8081. After a verified `TIGHTEN` appears in the local timeline:

1. disable emulator Wi-Fi and mobile data;
2. launch LuckyMirror Demo and confirm the local block still appears;
3. return to LoopLock and run **Run rejected UNLOCK fixture**;
4. confirm `ACTION_NOT_ALLOWED`, unchanged end time, and retained rule.

The reference deployment is a private Cloud Run service in `australia-southeast1` with Firestore in the same region. Deployment and database scripts are guarded because they enable APIs, change IAM, create resources, and may incur charges. Review [infra/README.md](infra/README.md) and obtain explicit owner approval before any redeploy or Cloud mutation.

## Privacy and platform limits

LoopLock observes only foreground package identity needed for targeted local rules. It does **not** retrieve accessibility window content, typed text, screenshots, messages, contacts, financial data, a full installed-app inventory, browsing history, or third-party gambling data. Firestore stores opaque IDs, a target hash, bounded result fields, and timestamps—not an account or raw activity history. Terminal Android processing scrubs raw package metadata from the outbox.

The application-ready consumer beta uses an explicitly enabled accessibility service and is bypassable. General public availability would require Google Play policy review, prominent disclosure, accessibility and privacy testing, false-positive operations, security review, lived-experience/clinical co-design, and a production mobile-authentication path. Package suspension, uninstall prevention, and stronger network controls belong to a separately provisioned managed-device mode and are not capabilities claimed by this APK.

## Submission evidence

- [Architecture diagram and text alternative](docs/submission/architecture.md)
- [Four-minute recording script](docs/submission/demo-script.md)
- [Shot-by-shot producer run sheet](docs/submission/demo-run-sheet.md)
- [Evidence and sanitization checklist](docs/submission/evidence-checklist.md)
- [Owner-review Devpost draft and field inventory](docs/submission/devpost-draft.md)
- [Detailed build evidence](docs/hackathon-build/build-notes.md)
- [Acceptance criteria](docs/hackathon-build/acceptance-criteria.md)

No repository, video, screenshot, hosted URL, or Devpost submission is published by the build scripts.
