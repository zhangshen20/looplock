# Build Notes and Decision Journal

## 20 August 2026 — Product framing

- Confirmed live event status, mandatory stack, deadline, required assets, form fields, judging weights, and current announcements through Devpost.
- Confirmed the live **Hi, Stop Gambling** project is a draft created 19 August 2026 and still contains the breathing/walking description. No Devpost content was changed.
- Confirmed the folder contains only `README.md`, is not a Git repository, and had no Devpost workflow state file.
- The Devpost guided-build state preconditions were therefore not met. Product artifacts were created in the expected `docs/hackathon-build/` location without creating or mutating `.devpost-hackathon-state.json`, registering, agreeing to rules, or submitting.

## Decisions

- Reframed the product around the one-way adaptive recovery firewall, not a recommendation companion.
- Narrowed the target to voluntary adult self-use on a personally owned Android device.
- Selected Taskmaster because a package event autonomously routes through quarantine, classification, validation, persistence, and local enforcement.
- Separated deterministic enforcement from agent classification.
- Closed agent authority to `TIGHTEN` and `REVIEW`.
- Chose new-install quarantine as the bounded adaptive event; broad app/site surveillance was rejected.
- Moved site blocking to stretch scope after reviewing VPN complexity and Play policy.
- Made consumer-mode bypasses explicit and kept managed-device controls as a separate production vision.
- Made all accountability behavior preview-only for the MVP.

## Owner-Confirmed Build Choices

- Confirmed on 20 August 2026.
- One primary Codex-assisted build lane over three to four calendar days, with roughly six to ten human hands-on hours. This supersedes the earlier ambiguous 35-hour wording.
- Internal submission target: 28 August.
- Voluntary adult self-use is the primary audience.
- App-first MVP is approved; site blocking remains stretch scope.
- Five-minute demo commitment represents a 24-hour product default.
- Two locally built, harmless betting-demo fixtures.
- Implementation should pause for owner review after local enforcement, the end-to-end ratchet, and the submission rehearsal.

## Active Shaping Status

The owner confirmed the target user, capacity assumption, app-first scope cut, 24-hour default, and milestone-review cadence. No material product choice remains open before the technical implementation specification.

## Deepening Rounds

- Scope: one evidence-led shaping pass based on the supplied product context, live event data, Android controls, and Play policies.
- PRD: one edge-case and safety pass covering permissions, offline state, duplicates, time changes, false positives, malformed output, expiry, and coercion.
- Checklist: one risk-first sequencing pass against the 28 August internal target.

## 20 August 2026 — Technical specification

- Selected native Kotlin/Compose, Room, DataStore, WorkManager, Retrofit/OkHttp, and a package-only AccessibilityService overlay.
- Selected a context-registered package-install receiver owned by the active accessibility service because modern Android restricts manifest delivery of implicit package broadcasts.
- Limited P0 package discovery and visibility to repository-owned fixture packages; no `QUERY_ALL_PACKAGES`.
- Selected Python 3.13, FastAPI, Google ADK 2.x, Vertex AI `gemini-3.5-flash`, private Cloud Run, and Firestore.
- Selected an IAM-authenticated `gcloud run services proxy` plus `adb reverse` for demo connectivity, avoiding credentials in the APK.
- Defined the agent's technical contribution as upgrading an installation-instance quarantine into a commitment-scoped package rule that persists across reinstall.
- Recorded environment gap: Android Studio, JDK, SDK, ADB, and emulator are not installed; Git, uv, Docker, and gcloud are available.
- Reframed the estimate as a three-to-four-day Codex-assisted window with six-to-ten human hours, rather than 35 human coding hours.
- Deepening rounds: one architecture and failure-mode pass. The self-review surfaced the agent-ornament risk, demo-only proxy boundary, and controlled-fixture limitation.

## Next Work

This entry was superseded after the owner approved the local Android toolchain. Cloud implementation still requires a confirmed project ID and credit/billing decision.

## 20 August 2026 — Build item 1: environment, contracts, and fixtures

- User approved installation of Android Studio and the SDK toolchain.
- Installed Android Studio 2026.1.3.8, command-line tools 22.0, API 36 platform revision 2, Build Tools 36.0.0, ADB 37.0.1, emulator 37.1.11, and the API 36 Google APIs ARM64 image.
- Created and booted the `LoopLock_API_36` Pixel 9 reference AVD.
- Initialized a local Git repository; nothing was committed, pushed, or published in this item.
- The guided build-project skill could not update journey state because `.devpost-hackathon-state.json` does not exist. Continued from the owner-confirmed local checklist without inventing registration or rules-agreement state.
- Froze JSON Schema v1 request/response contracts. Agent responses are closed to `TIGHTEN` and `REVIEW`; `UNLOCK` and server-selected expiry fixtures are rejected.
- Scaffolded the native Kotlin/Compose app plus the harmless BetBurst Demo and LuckyMirror Demo fixture APKs.
- Pinned AGP 9.2.1, Gradle 9.4.1, Kotlin/Compose compiler 2.4.10, API 36, Build Tools 36.0.0, and Compose BOM 2026.02.01.
- Verified 5/5 Python contract tests and all Kotlin contract tests.
- Built all three debug APKs, installed them on the reference emulator, and cold-launched each activity successfully.
- Visually checked screenshots: both fixtures prominently state they contain no betting, payments, accounts, ads, analytics, or network access.
- APK permission scan showed no permissions in either fixture and no `INTERNET` or `QUERY_ALL_PACKAGES` permission in LoopLock.
- Added reproducible `make` targets and Android helper scripts for the reference environment.
- Google Cloud deployment, API enablement, IAM, Firestore, and Vertex AI remain paused. The owner identified the intended Google account, but an email address is not a project ID and hackathon credits are still pending.

### Issues resolved

- The Homebrew command tools initially could not see SDK packages installed in the standard SDK root; installing the command tools into that root restored target discovery.
- AGP 9's new source-directory API requires a path string; corrected the contract-test resource wiring and reran the complete cached build successfully.

### Next build item

Implement informed consent and local commitment state. The first milestone pause remains after deterministic local enforcement, not after this foundation item.

## 20 August 2026 — Build items 2–3: consent, commitment, and local enforcement

- Added the first-run safety boundary and accessibility disclosures before any system permission screen is opened.
- Added the policy selection and review flow with the target package, exact start/end, quarantine choice, minimal cloud-data preview, never-collected data, delayed recovery, consumer-mode bypass limits, and explicit AI authority boundary.
- Added Room v1 entities for commitments, rules, and protection events. Active state is append-only from the application API: there is no delete, shorten, allow, or disable-quarantine repository path.
- Added a pure monotonic policy guard. Weakening attempts leave the policy unchanged and write a `VALIDATION_REJECTED` event.
- Added monotonic same-boot expiry evaluation with an explicit reboot fallback, plus unit coverage for clock movement and mutation rejection.
- Added honest service health: `Protected` appears only while both the commitment and the selected-app accessibility service are active; otherwise the UI reports `Action required` or `Expired`.
- Added the targeted accessibility service for BetBurst only. Window-content retrieval is disabled and the APK requests neither `INTERNET` nor `QUERY_ALL_PACKAGES`.
- Added a neutral `TYPE_ACCESSIBILITY_OVERLAY` block experience that identifies LoopLock, the local commitment rule, exact expiry, and that AI did not make the block decision.
- Found and fixed a real transition defect during manual testing: Android could remove an overlay during the asynchronous HOME transition. The service now waits 350 ms for that transition and does not interpret `onInterrupt` as permission revocation.
- Verified the full activation flow through Android's visible accessibility settings. Before access was enabled the app correctly reported `Action required`; after enabling it reported `Protected`.
- Verified 10/10 controlled BetBurst launches online and another 5/5 while airplane mode was visibly enabled. The local database contained 15 distinct `BLOCK_ATTEMPT` events, all with result `BLOCKED_LOCALLY`.
- Restored airplane mode to disabled after the test.
- Re-ran unit tests and the complete debug APK build after the overlay fix. Re-ran 2/2 Room repository instrumentation tests on the API 36 reference emulator.
- APK inspection found only Android's generated non-exported dynamic-receiver permission. The accessibility configuration is package-scoped to BetBurst, uses only `typeWindowStateChanged`, and declares `canRetrieveWindowContent=false`.
- No Google Cloud project, API, IAM, deployment, or billable resource was created. Credits are pending and the owner has supplied an account email but not a project ID.

### Milestone review

Items 2 and 3 satisfy `AC-01` through `AC-04` on the controlled reference emulator. Pause here for owner review before implementing install-event quarantine (item 4).

## 20 August 2026 — Build item 4: install-event quarantine

- Changed the strict LuckyMirror quarantine option to default off, matching the consent requirement. The user must affirmatively enable it before activation.
- Added a context-registered `ACTION_PACKAGE_ADDED` monitor owned by the active accessibility service. It accepts only a non-replacement install signal for the repository-owned LuckyMirror package.
- Added targeted reconciliation on service connection and main-app resume. Reconciliation compares Android's `firstInstallTime` with the commitment start, so an app installed before commitment is not silently swept into quarantine.
- Added Room schema v3 with a local `classification_outbox`. The quarantine rule, neutral `PACKAGE_ADDED` event, and queued outbox row are written in one transaction.
- The outbox has no uploader in this item. Every verified row remained `QUEUED` with a null upload timestamp; no network permission or cloud request exists.
- Added installation-instance idempotency keyed by commitment, targeted package, and Android first-install timestamp. A broadcast and later resume reconciliation for the same installation collapse to one event; a genuine uninstall/reinstall is a new installation instance.
- Updated the accessibility service's package filter from active Room rules and generalized the local overlay to BetBurst or LuckyMirror. The service still listens only to the two repository fixtures and never retrieves window content.
- Added a visible status fact: `LuckyMirror — Quarantined locally — classification queued, not uploaded`.
- Verified the Room 1→2→3 migration path by upgrading an existing debug database; it opened at schema version 3 with the installation-instance unique index present.
- The first reliability run exposed 11 events for 10 installs because broadcast and reconciliation were only time-debounced. Replaced that approach with installation-instance identity and repeated the run from clean state.
- Corrected reliability result: 10 installs/reinstalls produced exactly 10 distinct `PACKAGE_ADDED` events, 10 distinct installation instances, 10 queued outbox rows with no upload timestamp, and one additive LuckyMirror quarantine rule.
- With quarantine off, a real LuckyMirror install followed by resume reconciliation produced zero LuckyMirror rules, zero package events, and zero outbox rows.
- Launched LuckyMirror after quarantine and visually verified the neutral LoopLock overlay blocked it from the local rule before any classification.
- Final verification passed: all Android unit/contract tests, 6/6 on-device Room/install tests, 5/5 Python contract tests, and the debug APK build.
- APK inspection still found no `INTERNET` or `QUERY_ALL_PACKAGES`; only Android's generated non-exported dynamic-receiver permission appears.
- No Cloud API, project, IAM, deployment, billable resource, Devpost field, or external publication was touched.

### Next build item

Build and attack the pure monotonic proposal validator (item 5). Cloud deployment remains paused until a project ID and credit/billing decision are confirmed.

## 20 August 2026 — Build item 5: monotonic agent-proposal validator

- Added a pure Kotlin agent-proposal parser and validator with no Android, database, network, or clock access. The caller supplies an immutable local snapshot and trusted current time.
- Kept the version-1 response shape closed to the ten frozen fields. Missing or wrongly typed fields fail closed; any extra field, including expiry, allow, delete, disable, or alternate-end authority, is rejected before semantic validation.
- The validator accepts only an event- and commitment-matched `TIGHTEN` against an active, exact-package `QUARANTINE` rule. Its only possible expiry is the unchanged local commitment end.
- A valid `REVIEW` retains quarantine. Unsupported schema, wrong event/commitment/target/type, inactive or late result, non-quarantined target, duplicate terminal state, invalid confidence/classification/reason, and unknown or weakening action all return the safe outcome `REVIEW` without mutation.
- Added attack coverage for `ALLOW`, `UNLOCK`, `DELETE`, `DISABLE`, `REMOVE`, and `SHORTEN`, plus repeated event/commitment/target mutations, malformed JSON, missing/wrong types, authority-expanding fields, late results, duplicates, and invalid semantic combinations.
- Added an on-device `Run rejected UNLOCK fixture` control. It evaluates a real queued LuckyMirror quarantine, writes one neutral `VALIDATION_REJECTED` audit event, and has no policy-write path.
- Android unit and contract tests passed: 16 tests total, including 8 proposal-validator attack tests. The debug APK built successfully.
- Android instrumentation passed: 7/7 tests. The repository test proved the visible fixture retained the same commitment and two identical rules while adding exactly one rejection event.
- Python contract tests remained green at 5/5.
- Manually exercised the fixture on the API 36 emulator. The UI displayed `UNLOCK rejected`, `ACTION_NOT_ALLOWED — quarantine retained`, and the unchanged commitment end. Evidence image: `/private/tmp/looplock-unlock-rejected.png`.
- A direct read of the local demo database after the visible run showed one `USER_SELECTED` rule and one `QUARANTINE` rule sharing the unchanged end, with only `PACKAGE_ADDED / QUARANTINED_BEFORE_UPLOAD` and `VALIDATION_REJECTED / ACTION_NOT_ALLOWED` events.
- The screen honestly reported `Action required` because accessibility access was off; the validator proof did not claim active package blocking.
- No cloud API, project, IAM, billing, deployment, Devpost field, or external publication was touched.

### Next build item

Deploy the minimal private Cloud Run ADK/Gemini service (item 6). This remains blocked on a confirmed Google Cloud project ID and an explicit ready-to-use billing or hackathon-credit decision.

## 21 August 2026 — Build item 6 local portion: bounded ADK service

- Implemented one stateless FastAPI classification endpoint backed by a single Google ADK agent configured for Vertex AI `gemini-3.5-flash`.
- Pinned Python 3.13 and the resolved service dependencies, including Google ADK 2.7.1 and Google Gen AI 2.19.0, in `backend/uv.lock`.
- Kept the model output schema smaller than the frozen Android response: the model can emit only classification fields. The server, not the model, stamps event, commitment, target, and schema authority into the response.
- Gave the ADK agent no tools and only two possible actions: `TIGHTEN` and `REVIEW`. Instructions explicitly treat package metadata as untrusted and forbid user diagnosis, policy mutation, unlock, expiry, settings, or contact authority.
- Added a second deterministic server-side gate: only the exact repository-owned LuckyMirror package and label may preserve a `TIGHTEN`; all other metadata is downgraded to `REVIEW`. Invalid structured model output also becomes `REVIEW`.
- Added strict request parsing, matching event-ID idempotency headers, an 8 KiB request limit, disabled API documentation routes, lazy credential use, and an unauthenticated health route that makes no model call.
- Added a non-root, production-shaped container and removed its dependency on GitHub Container Registry after the first local build exposed an authorization failure there.
- Built the final ARM64 image successfully, confirmed it runs as UID 10001, and verified `/healthz` from both the local Python runtime and the container.
- Added guarded, repeatable private Cloud Run deployment and demo-proxy scripts. The mutation script refuses to run unless both an exact project ID and `LOOPLOCK_CLOUD_MUTATION_APPROVED=YES` are present. The proxy is labeled demo-only and no APK credential path exists.
- Python verification passed: 16/16 contract, model, guardrail, and API tests. Android safety regression verification also passed with `testDebugUnitTest` after the backend contract work.
- Secret scanning found no account email, private key, API key, client secret, or service-account key in tracked project content.
- Did not call Vertex AI, enable APIs, change IAM, create a service account, build in Google Cloud, deploy Cloud Run, alter billing, edit Devpost, or publish anything.

### Item 6 remains open

Local implementation does not satisfy deployment acceptance. Completion still requires the owner to provide the exact project ID, confirm credits or billing are ready, explicitly authorize cloud mutations, call the real Gemini model through the private service, and capture sanitized Cloud Run proof of privacy, runtime identity, and resource limits.

## 25 August 2026 — Build item 6 completed: private ADK service

- Confirmed the exact Google Cloud project ID `looplock-hackathon-2026-v8k3`, active billing linkage, and available hackathon credit before mutation.
- After explicit owner approval, enabled only the item-6 APIs, created the keyless `looplock-agent-runtime` identity, and granted that identity only `roles/aiplatform.user`.
- Deployed the private `looplock-agent` Cloud Run service in `australia-southeast1`. Final revision `looplock-agent-00003-g74` is ready and receives 100% of traffic.
- Verified the service IAM policy has no public principal. An unauthenticated request returned HTTP 403; fixture verification used the authenticated demo-only local proxy.
- Verified the final revision configuration: zero minimum instances, maximum one instance, concurrency four, 512 MiB memory, one vCPU, and a 60-second timeout.
- Corrected the ADK 2.7.1 root-agent integration from unsupported `single_turn` mode to supported `chat` mode. Request isolation remains unchanged because every classification creates a fresh session and excludes prior contents.
- Added `/v1/health` for deployed checks because the Cloud Run front end reserves `/healthz`; retained `/healthz` for local container checks.
- Added `backend/.gcloudignore`, reducing the Cloud Run source upload from approximately 261 MiB to 220 KiB and excluding the local virtual environment, caches, and tests.
- Called the deployed private endpoint with `contracts/fixtures/valid-request.json`. The real Vertex AI response matched the frozen response schema and returned `TIGHTEN`, `DEMO_GAMBLING_APP`, confidence `1.0`, and `FIXTURE_MATCH` for the exact harmless LuckyMirror fixture.
- Final regression evidence: 16/16 backend tests passed and Android `testDebugUnitTest` completed successfully.
- No Firestore database, public access, service-account key, APK credential, Devpost edit, publication, or item-7 work was created.

### Next build item

Item 7, minimal idempotent Firestore state, remains unstarted and requires a separate approval before any cloud mutation.

## 25 August 2026 — Build item 7 local portion: minimal idempotent state

- Added an explicit Google Cloud Firestore 2.29.0 dependency and kept the service on Python 3.13 with a reproducible `uv.lock`.
- Added an async Firestore event store using Application Default Credentials from the existing keyless Cloud Run runtime identity; no API key, downloaded key, or APK database credential path exists.
- Implemented one document per opaque event UUID in `classification_events`. The closed stored schema contains schema version, event and commitment UUIDs, Android-compatible SHA-256 target hash, bounded status/result codes, confidence, timestamps, and a 60-second processing lease.
- Raw package name, label, version, account identity, prompt, model response text, and reason text are not persistent fields. The terminal reason returned to Android is reconstructed from a small server-owned reason-code map.
- Implemented transactional reservation, duplicate detection, active-lease `202 processing`, expired-lease reclaim, terminal result reuse, and event-identity conflict rejection. The same terminal document cannot be overwritten by a late worker.
- Added a safe status endpoint that exposes opaque IDs and bounded result codes without raw target metadata.
- Firestore reservation or completion failure returns `503`; the service never returns an unpersisted model result as a false success.
- Tightened the model draft semantics so a `REVIEW` cannot simultaneously claim a fixture match or a non-unknown classification.
- Added a separately guarded Firestore mutation script. It will enable the API, create the Standard Native-mode default database in `australia-southeast1`, grant only `roles/datastore.user` to the existing runtime identity, and redeploy privately with the existing Cloud Run caps—but it refuses to run without an item-specific approval flag.
- Local verification passed: 26/26 backend contract, privacy, idempotency, failure, and API tests; Android `testDebugUnitTest`; production-shaped container build; and credential-free container health check.
- The three-retry test produced identical responses from one logical event and invoked the classifier once. Persistent-field inspection found no raw target, account, prompt, or reasoning data.
- Secret scanning found no private key, API key, client secret, service-account key, or account email in project content.
- No Firestore API, database, IAM role, Cloud Run revision, billing change, public access, Devpost field, or publication was created in this local portion.

### Cloud gate at the end of the local portion

At that checkpoint, completion still required explicit owner approval for the item-specific cloud mutations, followed by a real three-request test and inspection of the single Firestore document. Item 8 remained blocked until that proof passed.

## 25 August 2026 — Build item 7 completed: minimal idempotent Firestore state

- Received explicit owner authorization for the four scoped item-7 actions: enable the Firestore API; create the Standard Native-mode `(default)` database in `australia-southeast1`; grant the existing keyless runtime identity only `roles/datastore.user`; and redeploy the existing private capped service for three-retry verification and one-document inspection.
- A parallel read-only safety review initially returned NO-GO because the mutation script accepted any non-empty project and treated every database-description failure as absence. The script was fixed to bind the exact approved project and region, distinguish `NOT_FOUND` from other errors, and verify location, type, and edition fail-closed. Re-review returned GO with no remaining P0/P1 finding.
- Added lease-token fencing so a worker whose lease expired cannot persist after a concurrent worker reclaims the event. Terminal completion deletes processing-only lease fields.
- Set `ADK_CAPTURE_MESSAGE_CONTENT_IN_SPANS=false` on the deployed revision so bounded fixture metadata is not captured as tracing message content.
- Enabled `firestore.googleapis.com` and created `(default)` as `FIRESTORE_NATIVE`, `STANDARD`, in `australia-southeast1`. The installed `gcloud` version defaults new databases to Standard and does not accept the newer explicit edition flag; the script now relies on that documented default and verifies the resulting edition before IAM or deployment continues.
- Granted `looplock-agent-runtime@looplock-hackathon-2026-v8k3.iam.gserviceaccount.com` `roles/datastore.user`. Its only application roles are now `roles/aiplatform.user` and `roles/datastore.user`.
- Deployed private revision `looplock-agent-00004-ztf` with 100% traffic, zero minimum instances, one maximum instance, concurrency four, 512 MiB memory, one vCPU, and a 60-second timeout. Unauthenticated access returned HTTP 403 and the service IAM policy has no public principal.
- Sent `contracts/fixtures/valid-request.json` three times through the IAM-authenticated demo-only proxy. All three responses were identical: `TIGHTEN`, `DEMO_GAMBLING_APP`, confidence `1.0`, and `FIXTURE_MATCH`.
- Firestore collection inspection showed exactly one document keyed by the event UUID. Its fields are `schema_version`, `event_id`, `commitment_id`, `target_hash`, `status`, `classification`, `confidence`, `reason_code`, `created_at`, and `updated_at`; raw package name, label, version, account identity, prompt, model wording/reason text, and processing lease fields are absent.
- Final verification passed: 27/27 backend tests, Android `testDebugUnitTest`, rebuilt production container, local container health, Cloud Run privacy/caps/identity inspection, and one-document Firestore inspection.
- No public access, key material, Firebase Authentication, APK database access, Devpost edit, external publication, or additional product integration was created.

### Next build item

Item 8 is now unblocked: implement the Android-to-cloud queue/worker/result flow without giving the cloud or worker any authority to weaken local protection.

## 25 August 2026 — Build item 8 completed: Android-cloud ratchet

- Added a unique WorkManager job per classification event with connected-network constraints, bounded exponential retry, idempotency headers, POST/status handling, and strict closed-contract decoding.
- Restricted cleartext networking to emulator localhost in debug builds. Release remains cleartext-disabled; fixture APKs still have no Internet permission.
- Added Room-backed crash recovery and atomic result application. Only `QUARANTINE -> AGENT_TIGHTENED` is permitted; duplicate terminal responses are no-ops, late/inactive responses cannot reactivate protection, and the cloud or worker never writes enforcement rules directly.
- Terminal processing preserves only the stable target hash and installation timestamp needed for deduplication while clearing raw package, label, and version metadata.
- Added neutral timeline events for package quarantine, accepted tightening, local block attempts, and validation rejection. The visible adversarial fixture sends an `UNLOCK` proposal through the same local validator.
- Completed two clean emulator rehearsals through the real private Cloud Run/ADK/Gemini/Firestore path. Each produced exactly one terminal outbox event and one `AGENT_TIGHTENED` LuckyMirror rule with the unchanged local commitment end.
- In both rehearsals, disabling emulator Wi-Fi and mobile data did not change enforcement: Android reported a focused LoopLock `TYPE_ACCESSIBILITY_OVERLAY` over LuckyMirror and recorded `BLOCKED_LOCALLY`.
- In both rehearsals, the visible `UNLOCK` fixture produced `VALIDATION_REJECTED / ACTION_NOT_ALLOWED`; the tightened rule and commitment end were unchanged.
- The local terminal outbox contained no package, label, or version metadata. The demo proxy remains an IAM-authenticated Mac-only bridge on port 8081 and has no authority over Android enforcement or persistence.
- Final regression verification passed: 24 Android JVM tests, 12 API-36 on-device Room/policy tests, 28 backend tests, shell syntax, manifest-boundary inspection, whitespace checks, and the repository credential/email scan.

### Next build item

Item 9: finish the safety UX and regression matrix, especially permission revocation, clock rollback, restart, network failure, malformed response, and legitimate-app `REVIEW` behavior.

## 26 August 2026 — Build item 9 completed: safety UX and failure matrix

- Connected the existing monotonic clock policy to real repository expiry and enforcement reads. On the same boot, elapsed time is authoritative even if wall time moves backward or forward; the immutable wall end remains display/audit data.
- Expiry is now materialized once as `COMMITMENT_EXPIRED / EXPIRED_LOCALLY`. Rules and the historical timeline remain stored, but active-rule queries return nothing after trusted expiry.
- Added a one-second foreground refresh so an open status screen changes naturally from `Protected` or `Action required` to `Expired` without requiring a restart.
- Added a closed local accountability preview containing exactly attempt count, bounded commitment time window, and escalation level. It is visibly labeled `Demo only — not sent`; it has no package, target, destination, contact, or delivery field and performs no external action.
- Expanded delayed-recovery copy to show the exact availability time, state that no request was sent, repeat that no active override exists, and distinguish consumer bypassability from future managed-device controls.
- Added pure status tests proving an active commitment cannot display `Protected` when accessibility is off, plus payload-shape tests proving the accountability preview has no target fields.
- Added transport coverage proving connection failure is retryable and cannot fabricate a terminal proposal. Existing malformed-response and legitimate `REVIEW` tests continue to retain quarantine without adding or weakening rules.
- Added on-device tests proving wall-clock rollback cannot delay trusted elapsed-time expiry, the expiry event is idempotent, enforcement stops after expiry, and active commitment/rules reload after closing and reopening the Room database.
- Repeated Android's visible permission flow. With the service enabled and bound LoopLock displayed `Protected`; after confirming `Turn off` in system settings, Android reported no enabled/bound service and LoopLock displayed `Action required` without claiming enforcement.
- Observed a real five-minute commitment transition naturally at its exact displayed end. The screen changed to `Expired`, showed one neutral expiry event, retained history, and bounded the accountability window at the commitment end.
- Final verification passed: 27 Android JVM tests, 14 API-36 on-device tests, 28 backend tests, shell syntax, whitespace checks, sensitive-permission scan, and repository credential/email scan.
- No Cloud API, IAM, deployment, billing, Firestore, Devpost, repository publication, message delivery, or third-party action occurred.

### Next build item

Item 10: prepare the reproducible README, final evidence package, four-minute public demo, repository handoff, and truthful Devpost draft for owner review. Do not publish or submit autonomously.

## 26 August 2026 — Build item 10 local handoff prepared; publication gates remain

- Refreshed the live Devpost event overview, dates, submission requirements, judging criteria, custom fields, organizer announcements, and the existing project. Submissions remain open until 31 August at 5:00 PM PT; the live project is still the unsubmitted breathing/walking draft with no video.
- Replaced the placeholder root README with judge-facing architecture, safety limits, prerequisites, pinned test/build commands, emulator setup, harmless initial fixture installation, local backend health check, and an honest optional private-cloud ratchet path.
- Added `make verify` and `make android-connected-test` entry points without changing production behavior.
- Produced a final UML-like trust-boundary diagram as accessible SVG plus upload-ready 1600×1000 PNG and PDF. It gives the Android device sole enforcement authority, uses dashed transport for the IAM-authenticated Mac proxy, and explicitly bars Cloud-to-store/enforcer authority.
- Recut the four-minute script to show a real local block in the first 12 seconds, then one matching event through quarantine, ADK/Gemini/Cloud Run/Firestore, offline enforcement, and weakening rejection. Recording and owner review remain pending.
- Drafted truthful Devpost name/tagline/description, Built With inventory, third-party dependency disclosure, testing instructions, and every current custom field. Submitter identity, country, start date, repository visibility, video URL, and owner approval remain intentionally unresolved.
- Rehearsed from a clean source copy excluding `.git`, build outputs, caches, `local.properties`, `.env`, and cloud configuration. Fresh Python dependencies installed from the lockfile; 28 backend tests and 27 Android JVM tests passed; all three debug APKs assembled; and the documented local `/healthz` returned the expected service response.
- No Devpost project edit, architecture upload, repository publication, video upload, Cloud mutation, IAM change, message, or submission occurred.

### Remaining gates before item 10 can be complete

- owner confirms submitter type, country, project start date, and repository visibility;
- repository is published or required private access is granted, then checked signed out;
- sanitized demo and Cloud proof are recorded, edited to at most four minutes, uploaded publicly, and checked signed out;
- owner visually approves the diagram and reviews every final claim;
- final secret/privacy scan passes immediately before publication and the user submits.

## 26 August 2026 — Item 10 owner defaults advanced; external actions still gated

- The owner's `next` response advanced the recommended branch choices from the prior checkpoint: individual entrant, Australia, project start date `08-19-26`, public GitHub repository, public YouTube video, and no Startup Prize or optional promotional-content entry.
- These choices do not authorize an external write. Repository creation/push, video upload/publication, Devpost editing, architecture upload, and submission remain separately approval-gated.
- Added `make submission-preflight`, which verifies required local artifacts, the reviewed 1600×1000 diagram exports, shell syntax, sensitive Android boundaries, ignore rules, and credential patterns. It reports repository/video URLs as pending in local mode and fails on them in final-publication mode.
- Publication audit found no Git remote. GitHub CLI identifies the intended account as `zhangshen20`, but its stored token is invalid. Local Git identity is also incomplete: `user.name` contains an email address and `user.email` is unset. No login, identity change, staging, commit, remote creation, push, or publication was attempted.

## 26 August 2026 — Public repository publication authorized

- The owner approved the architecture and explicitly authorized GitHub login, a repository-local GitHub noreply identity, creation of the public repository `zhangshen20/looplock`, and push.
- The reproducible README and Devpost draft now use the intended public URL. Publication still requires account verification, the final credential/privacy scan, and a signed-out availability check.
- This authorization does not include public video upload, Devpost editing, architecture upload, or submission.

## 26 August 2026 — Public repository published and checked

- Authenticated GitHub CLI as the intended account `zhangshen20`, configured the repository-only identity `zhangshen20 <zhangshen20@users.noreply.github.com>`, created public repository `zhangshen20/looplock`, and pushed `main`.
- Verified both the repository page and raw `README.md` returned HTTP 200 without supplying GitHub credentials.
- A fresh public clone exposed that `submission-preflight.sh` required ignored local paths to exist. Updated the ignore verification to use `git check-ignore --no-index` and probe a child path for directory-only patterns, so a pristine clone validates the ignore contract correctly.
- Public video upload, Devpost editing, architecture upload, and submission remain unperformed and unauthorized.
