# Acceptance Criteria

## AC-01 — Informed activation

**Given** a first-time user has not accepted the disclosure  
**When** they open setup  
**Then** no accessibility or other sensitive permission is requested until the relevant in-app disclosure is shown and affirmatively accepted.

**Given** a user reviews a commitment  
**Then** the screen names the target package, exact start/end time, new-install quarantine choice, data sent, data not collected, consumer-mode bypass limits, and delayed recovery behavior.

## AC-02 — Commitment immutability

**Given** an active commitment  
**When** any UI, local function, or cloud proposal attempts to remove a rule, allow-list a target, disable quarantine, or move `ends_at` earlier  
**Then** the operation is rejected, the stored policy remains unchanged, and a `VALIDATION_REJECTED` event is recorded.

## AC-03 — Known app blocking

**Given** BetBurst Demo is selected and the service is active  
**When** the package is launched  
**Then** the LoopLock block experience appears before meaningful interaction with the demo app, exactly one attempt event is stored, and no cloud response is required.

**Given** the same state with airplane mode enabled  
**Then** the outcome is unchanged.

## AC-04 — Honest service state

**Given** the accessibility service is disabled or revoked  
**When** LoopLock returns to foreground  
**Then** status reads `Action required` and the product does not claim the device is protected.

## AC-05 — Pre-authorized install quarantine

**Given** `quarantine_new_installs=true` and the commitment is active  
**When** LuckyMirror Demo is installed  
**Then** a quarantine rule and event are written locally before any cloud request is made.

**Given** the option is false  
**Then** LoopLock neither quarantines the package nor uploads a classification event.

## AC-06 — Minimal agent request

**Given** a queued quarantine event  
**When** it is uploaded  
**Then** the request contains only anonymous session/event identifiers and the minimum demo package metadata needed for classification; it contains no contacts, messages, screen contents, keystrokes, financial data, or full app inventory.

## AC-07 — Closed agent output

**Given** a valid Cloud Run response  
**Then** it conforms to a versioned schema containing `action`, target, classification, confidence, reason, event ID, and commitment ID, where `action` is only `TIGHTEN` or `REVIEW`.

**Given** malformed, unknown, late, or weakening output  
**Then** the local result is `REVIEW`, quarantine remains, and no active rule is weakened.

## AC-08 — Idempotency

**Given** the same event is retried three times  
**Then** Firestore has one logical result, Android has one additive target rule, and the timeline has no misleading duplicate actions.

## AC-09 — Offline ratchet proof

**Given** LuckyMirror Demo received an accepted `TIGHTEN` rule  
**When** network access is disabled and the app is launched  
**Then** it remains blocked from local state.

## AC-10 — Expiry

**Given** the trusted commitment end has passed  
**When** state is refreshed  
**Then** status changes to `Expired`, active demo rules stop enforcing, and the historical timeline remains available locally.

**Given** the wall clock moves backward during commitment  
**Then** the policy does not expire earlier on the reference emulator.

## AC-11 — Privacy-preserving storage

**Given** a successful cloud classification  
**Then** Firestore contains anonymous identifiers, target hash, classification, confidence, action, and timestamps, and does not contain a personal account or long-form activity history.

## AC-12 — Accountability preview

**Given** the user opens escalation details  
**Then** the payload contains only attempt count, time window, and escalation level, package/destination names are omitted, and the UI states that nothing was sent.

## AC-13 — Reproducibility

**Given** a reviewer follows the README from a clean checkout with documented prerequisites and credentials  
**Then** they can run automated tests, start the backend, install the Android and demo APKs, activate a commitment, and reproduce the core flow.

## AC-14 — Submission proof

The final video is public, no longer than four minutes for judged content, in English or subtitled, and visibly demonstrates:

- the problem and value proposition;
- Gemini 3.5 Flash and Google ADK by name;
- the live package-event workflow;
- Cloud Run and Firestore evidence;
- offline local enforcement;
- rejection of a weakening command;
- the consumer/managed-device distinction.

## Release Blockers

Any of the following blocks the hackathon MVP from being called complete:

- an agent or API path can shorten or delete an active rule;
- the second demo app is usable before quarantine is installed;
- network loss removes enforcement;
- the UI claims protection when the required service is disabled;
- a secret is committed to the repository or embedded in the APK;
- the demo relies on a fake cloud success or an unsupported Android capability;
- the README, diagram, public video, or visible Google Cloud proof is missing.
