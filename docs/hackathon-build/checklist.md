# Build Checklist

## Build Preferences Used for This Draft

- **Plan ownership:** PM-authored and owner-confirmed.
- **Build mode:** autonomous implementation with visible milestone checks.
- **Verification pauses:** owner-confirmed after local block, end-to-end ratchet, and submission dry run.
- **Git cadence:** commit after each verified checklist item once the owner initializes the repository.
- **Check-in cadence:** daily demoable increment, speed-oriented.

## Checklist

- [x] **1. Establish the environment, contracts, and harmless fixtures**
  Spec ref: `spec.md > Environment Readiness`, `spec.md > Repository Structure`, and `spec.md > Fixture Applications`
  What to build: Initialize the repository; install and pin the Android toolchain; freeze the versioned classification schema and monotonic-policy contract; create the Android app, one documented ARM64 emulator/API target, and BetBurst/LuckyMirror demo packages with no real gambling, accounts, payments, or third-party assets.
  Acceptance: Contract fixtures are shared by Android and backend tests; both fixture apps install and launch; repository contains their source and purpose.
  Verify: Clean build and install on the reference emulator; validate the same accepted/rejected contract fixtures in Kotlin and Python.

- [x] **2. Implement consent and commitment state**
  Spec ref: `spec.md > Android Application > App Shell and Screens`, `spec.md > Local Persistence`, and `spec.md > Commitment Clock`
  What to build: Boundary disclosure, permission disclosure, targeted app selection, strict-option consent, review screen, and immutable active commitment.
  Acceptance: `AC-01` and `AC-02` pass.
  Verify: Manual first-run walkthrough plus state-transition tests.

- [x] **3. Prove deterministic known-app blocking**
  Spec ref: `spec.md > Android Application > Accessibility Enforcement Service`
  What to build: Accessibility-based foreground package observation and neutral LoopLock block experience using only targeted demo packages.
  Acceptance: `AC-03` and `AC-04` pass; no broad package permission.
  Verify: Ten launch attempts plus five in airplane mode; inspect manifest permissions.

- [x] **4. Add install-event quarantine**
  Spec ref: `spec.md > Android Application > Install Monitor`
  What to build: Receive/reconcile the LuckyMirror install event and create a local quarantine rule only when pre-authorized.
  Acceptance: `AC-05` passes and quarantine timestamp precedes upload timestamp.
  Verify: Ten install/reinstall runs with the strict option on and one with it off.

- [x] **5. Build and attack the monotonic validator**
  Spec ref: `spec.md > Android Application > Monotonic Policy Validator`
  What to build: Versioned proposal schema and pure local validator that accepts only additive, event-scoped `TIGHTEN` and otherwise retains quarantine.
  Acceptance: `AC-02` and `AC-07` pass for valid, malformed, late, duplicate, mismatched, allow, delete, disable, and earlier-expiry cases.
  Verify: Automated unit/property tests; include a visible rejected `UNLOCK` fixture.

- [x] **6. Deploy the minimal ADK agent**
  Spec ref: `spec.md > Backend API`, `spec.md > ADK Agent`, and `spec.md > Cloud Run Deployment and Demo Connectivity`
  What to build: Private Cloud Run service using Google ADK and Vertex AI Gemini 3.5 Flash to classify harmless demo metadata into `TIGHTEN` or `REVIEW`, with IAM-authenticated demo connectivity and no credentials in the APK.
  Acceptance: Response matches the frozen schema; the runtime service account owns cloud credentials; instance and concurrency limits are configured.
  Verify: Call the deployed endpoint with fixture input and capture Cloud Run proof.

- [x] **7. Persist minimal idempotent cloud state**
  Spec ref: `spec.md > Firestore` and `spec.md > Security and Privacy`
  What to build: Firestore event/result record keyed by opaque event ID with target hash and no personal account or raw activity history.
  Acceptance: `AC-08` and `AC-11` pass.
  Verify: Send the same event three times and inspect the final Firestore document fields.

- [x] **8. Complete the Android-cloud ratchet**
  Spec ref: `spec.md > Android Application > WorkManager Classification Flow`
  What to build: Retryable event queue, result retrieval, validator application, local rule persistence, timeline, and late/duplicate handling.
  Acceptance: `AC-06` through `AC-09` pass; second launch blocks offline.
  Verify: Run the complete demo twice from a clean commitment.

- [x] **9. Finish safety UX and regression checks**
  Spec ref: `spec.md > Failure Strategy` and `spec.md > Verification Strategy`
  What to build: Service-health status, exact expiry, delayed recovery information, neutral timeline, and optional not-sent accountability preview.
  Acceptance: `AC-10` and `AC-12` pass; all release blockers remain clear.
  Verify: Permission revocation, clock rollback, restart, network failure, malformed response, and legitimate-app `REVIEW` tests.

- [ ] **10. Prepare the Devpost handoff**
  Spec ref: `spec.md > Demo and Submission Flow` and `delivery-sequence.md > Submission Readiness Checklist`
  What to build: Reproducible README, final diagram, public four-minute demo, screenshots, Cloud proof, truthful project copy, repository link, testing instructions, and final field inventory.
  Acceptance: `AC-13` and `AC-14` pass and the user has everything required to submit.
  Verify: Clean-checkout rehearsal, incognito link check, secret scan, four-minute timing, and user review. Do not submit autonomously.
