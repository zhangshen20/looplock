# Evidence and Product Decisions

Last checked: 26 August 2026 (Melbourne). Live Devpost event responses were refreshed around `2026-08-25T14:25Z`.

## Outcome

LoopLock is viable as an application-ready hackathon beta when presented as an event-driven Taskmaster workflow with a strict safety split:

- deterministic Android code blocks or quarantines;
- the cloud agent may classify an event and propose an additive protection rule;
- a local validator rejects every command that could unlock, delete, shorten, or weaken a commitment;
- the demo proves one complete ratchet loop rather than broad device coverage.

The current Devpost draft is not aligned. A live read on 26 August confirmed it is titled **Hi, Stop Gambling**, remains a draft, was created on 19 August 2026, and still describes breathing and walking prompts. It has no video and has not been submitted. This document does not authorize editing it.

## Live Event Facts

Source of truth: [All Things Agentic Hackathon on Devpost](https://allthingsagentichackathon.devpost.com/). If this document conflicts with Devpost, Devpost prevails.

### Deadline and eligibility

- Submission deadline: **31 August 2026 at 5:00 PM Pacific Time**, which is **1 September 2026 at 10:00 AM Melbourne time**.
- Devpost currently reports submissions open.
- The project was created inside the submission period. The form asks for its start date and states: “Please note that Projects must be NEWLY created by the Entrant during the Submission Period. (MM-DD-YY)”
- The official rules say: “New Projects Only: Projects must be newly created during the Submission Period. Participants may use standard development tools, including frameworks, libraries, starter templates, and AI coding assistants, but must disclose any other pre-existing code or work incorporated into the Project. The work described and submitted must have been built during the Submission Period.”
- Eligibility requires being above the legal age of majority in the entrant's jurisdiction, with specified geographic, sanctions, conflict-of-interest, and contest-entity exclusions. The entrant must personally verify the full official rules before submission.

### Mandatory build stack

Every track requires all three:

1. Gemini 3.5 or newer through Gemini API or Vertex AI.
2. At least one Google agent framework: Google ADK, Google GenAI SDK, Antigravity SDK, or Genkit.
3. At least one Google Cloud infrastructure service such as Cloud Run, Cloud SQL, Firestore, GKE, or Pub/Sub.

Recommended implementation for this project: **Gemini 3.5 Flash + Google ADK + Cloud Run + Firestore**. Pub/Sub is optional and should not be added unless the direct event flow is already stable.

### Track and judging

Recommended track: **Taskmaster**. Devpost describes it as a complete workflow that takes action rather than just producing text. The official rules also ask whether a Taskmaster agent intercepts and completes a multi-step background workflow and solves a distinctive, personal friction.

Judging weights:

- **Innovation & Operational Utility — 40%:** autonomous, high-value action with little hand-holding.
- **Architectural Discipline & Tech Stack — 30%:** decoupling, state, credentials, and failure handling.
- **Demo & Production Readiness — 30%:** working demo, diagram, reproducibility, and visible Google Cloud proof.

### Required submission proof

- Category selection.
- Text description covering features, technologies, data sources, and learnings.
- Public or private GitHub/GitLab/Bitbucket repository. A private repository must be shared with the addresses specified by Devpost.
- Step-by-step reproducible setup instructions in `README.md`.
- Architecture diagram upload.
- Public YouTube or Vimeo demo, capped at about four minutes; only the first four minutes are evaluated.
- The video must show the problem, value proposition, agent doing real work, named Gemini model and framework, and visible proof of Google Cloud deployment.
- The required architecture upload accepts PDF, PPT/PPTX, PNG, or JPEG; SVG source alone is not accepted by the form.
- A hosted project URL is optional but strongly encouraged. The backend does not need to remain live during judging if the submission clearly proves it was deployed.

Organizer guidance says judges may rely entirely on the video, description, and repository. The internal target is therefore **submit by 28 August**, with the final deadline retained only as recovery buffer.

The 24 August organizer checklist additionally recommends showing the working product in the first 10–15 seconds, cutting loading/waiting and live typing, disclosing pre-existing or third-party code, and leaving linked materials untouched after the deadline until winners are announced.

## Android and Policy Evidence

- Consumer `VpnService` requires user consent and can be revoked by the user. Always-on and lockdown modes exist, but a normal user can still change the configuration; a device/profile owner can enforce stronger settings. [Android VpnService](https://developer.android.com/reference/android/net/VpnService)
- Suspending packages and blocking uninstall are device/profile-owner capabilities, not ordinary consumer-app powers. [Android DevicePolicyManager](https://developer.android.com/reference/android/app/admin/DevicePolicyManager)
- Google Play permits `AccessibilityService` for non-accessibility uses only with declaration, prominent disclosure, affirmative consent, and policy-compliant behavior. It cannot be used to prevent uninstall/disable outside authorized parental or enterprise management, evade platform controls, or deceptively manipulate UI. [Google Play sensitive APIs policy](https://support.google.com/googleplay/android-developer/answer/16558241)
- Installed-app inventory is sensitive. Broad `QUERY_ALL_PACKAGES` visibility is restricted; the MVP should use targeted package declarations and explicit demo packages. [Google Play package visibility policy](https://support.google.com/googleplay/android-developer/answer/10158779)
- `VpnService` use requires a Play declaration and must match an allowed core purpose such as firewall, device security, app-usage tracking, parental control, or enterprise management. Store acceptance remains a risk, not an assumption. [Google Play VpnService policy](https://support.google.com/googleplay/android-developer/answer/12564964)

## Product Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Primary user | Adult who voluntarily wants protection during a self-chosen recovery commitment | Avoids coercive parent/employer/partner use and keeps consent legible |
| Job to be done | Preserve a calm-state decision during an urge and make the next workaround harder without exposing private behavior | More precise than “help people stop gambling” |
| Invention | A one-way protection ratchet: attempted loopholes become additive rules | Distinct from static blocklists and recommendation companions |
| MVP enforcement | Application-ready consumer beta using explicit accessibility permission and a foreground blocking experience | Working installable Android product within the supported reference environment |
| Adaptive event | A newly installed demo app is locally quarantined, then classified asynchronously | Bounded event; avoids surveilling every foreground app or every destination |
| Agent authority | `TIGHTEN` or `REVIEW` only; never `ALLOW`, `UNLOCK`, `DELETE`, or shorter expiry | Makes the safety boundary testable in code |
| Site blocking | Production vision; stretch only after the app ratchet is stable | DNS/HTTPS coverage and Play-policy work are too risky for the critical path |
| Accountability | Local preview and pre-authorized payload schema only; no live messaging in MVP | Demonstrates design without contacting anyone or leaking activity |
| Cloud stack | Gemini 3.5 Flash + ADK on Cloud Run; Firestore stores minimal event/result records | Satisfies the mandatory stack with a small, legible architecture |
| Track | Taskmaster | The install event triggers quarantine, classification, rule validation, persistence, and enforcement |

## Owner-Confirmed Build Assumptions

Confirmed on 20 August 2026:

1. One primary Codex-assisted build lane has **three to four calendar days**, with roughly **six to ten human hands-on hours**, before the 28 August internal submission target. Build, download, emulator, and cloud-deployment elapsed time sits outside that hands-on estimate.
2. The primary user is an adult using a personal Android device voluntarily.
3. The product default commitment is **24 hours**; the demo commitment lasts five minutes for filming.
4. The first demo uses two harmless, locally built fake betting apps; no real gambling account, payment, or personal activity is used.
5. Site-level blocking remains stretch scope unless a two-hour feasibility spike proves reliable browser coverage without breaking normal traffic.
6. Implementation uses milestone pauses after local enforcement, the end-to-end ratchet, and the submission rehearsal.

## Go / No-Go Gates

- **Go by 22 August:** the reference Android emulator reliably redirects a selected demo app before it can be used, and the service status is visible.
- **Go by 23 August:** the install/quarantine event survives loss of network and is later classified through Cloud Run.
- **Go by 24 August:** the local validator proves that weakening commands are rejected and a second launch remains blocked offline.
- **Cut cloud adaptation, preserve base block by 25 August** if classification is unstable. A smaller truthful demo beats a simulated agent claim.
- **Cut site blocking immediately** if it threatens the app ratchet, safety tests, video, or reproducibility.
