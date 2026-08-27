# LoopLock Public Demo Run Sheet — Producer Cut

Status: event-locked technical dry run passed on 27 August 2026. A private synthesized narration rehearsal measured 2:29.9 at 145 words per minute, leaving 78.1 seconds for proof holds and transitions inside the 3:48 cut. The full screen-and-voice stopwatch rehearsal, fresh recording authorization, final privacy review, upload, publication, and final owner approval remain pending.

## Approved producer format

Use an edited, narrated screen demo with no webcam. Record only cropped, pre-staged windows at 1080p or higher. Target **3:48** of judged content so captions, transitions, and platform processing cannot push the evaluated story past four minutes.

The minimum reliable story is one evidence-locked ratchet:

1. Android blocks BetBurst from local state.
2. A separately authorized LuckyMirror install is quarantined locally before Cloud responds.
3. One bounded Google ADK/Gemini proposal is accepted only after Android validation.
4. The accepted rule still blocks offline.
5. A weakening `UNLOCK` proposal is rejected without changing the rule or commitment end.

Do not spend judged time on the accountability preview. It is a safe supporting feature, but it does not strengthen the central Taskmaster or trust-boundary proof.

## Final edit — target 3:48

| Time | Picture and exact proof | Narration | Edit note |
| --- | --- | --- | --- |
| **0:00–0:12** | Begin on the emulator. Launch **BetBurst Demo** and hold the LoopLock block overlay long enough to read it. Add `LOCAL BLOCK · NO CLOUD` in the edit. | “Static blockers remember a list. LoopLock remembers the loophole—and its AI can never unlock you. Here, Android blocks BetBurst locally.” | No title card before the proof. Use the block overlay as the first frame or reach it within two seconds. |
| **0:12–0:32** | Return to LoopLock. Frame `Protected`, the exact commitment end, `New-install quarantine — Pre-authorized`, and the local rules. | “This is a voluntary five-minute demo commitment, representing a longer product policy. The end time and LuckyMirror quarantine were chosen while calm. Consumer mode is bypassable; managed-device strength is future work.” | If `Protected` is not visible, stop. Never substitute an `Action required` frame. |
| **0:32–0:58** | In a clean, cropped terminal run `android/scripts/install-workaround-fixture.sh`. Launch **LuckyMirror Demo** and hold the local block overlay. Return to LoopLock and frame the `PACKAGE_ADDED: QUARANTINED_BEFORE_UPLOAD` row while it is still `QUEUED`. Highlight its eight-character event prefix. Add `QUARANTINE BEFORE CLOUD`. | “I install only our harmless LuckyMirror fixture. Before any network response, Android creates a quarantine and blocks the app. This queued row is the event Android will classify.” | The fixture must not exist before the active commitment. Do not expose the terminal username, home path, scrollback, or unrelated commands. |
| **0:58–1:28** | Jump cut over network wait. Show `Rules: USER_SELECTED, AGENT_TIGHTENED`, `Agent proposal accepted locally — additive rule retained offline`, the original `PACKAGE_ADDED` row now terminal, and the separate `AGENT_RESULT: AGENT_TIGHTENED` row. Add `GOOGLE ADK + GEMINI 3.5 FLASH`. | “After a cut over network wait, that same package event is terminal and the local rule is agent-tightened. A bounded Google ADK agent on private Cloud Run uses Gemini 3.5 Flash. It can return only `TIGHTEN` or `REVIEW`; the phone independently validates the proposal and keeps the commitment end unchanged.” | Highlight the original `PACKAGE_ADDED` prefix for the Cloud match. The separate `AGENT_RESULT` timeline row has its own audit ID; never present that ID as the Cloud document ID. |
| **1:28–2:10** | Show three sanitized proof frames: (1) the private Cloud Run service/revision and `australia-southeast1`; (2) `backend/src/looplock_agent/agent.py` beside `config.py`, with `google.adk.agents.Agent` and `gemini-3.5-flash` legible; (3) one terminal Firestore document. The Firestore document name must begin with the same eight characters shown on Android. Frame only `schema_version`, opaque `event_id`/`commitment_id`, `target_hash`, bounded status/result fields, and timestamps. Add `PRIVATE CLOUD RUN · DEMO-ONLY IAM BRIDGE`. | “This is the private Cloud Run service in Australia. It requires authentication; the Mac proxy on port 8081 is a demo-only bridge, not a production mobile login design. The code names Google ADK and Gemini 3.5 Flash. Firestore has one terminal document whose ID matches Android. It stores opaque IDs, a target hash, bounded result fields, and timestamps—not raw package data, an account, a prompt, or model reasoning.” | Hide account menus, email addresses, service-account addresses, billing, tokens, raw requests, prompts, reasoning, unrelated projects, and browser tabs. Do not claim that a console page proves a value that is not legible. |
| **2:10–2:36** | Visibly turn emulator Wi-Fi and mobile data off, then launch LuckyMirror and hold the local block overlay. Add `OFFLINE · LOCAL RULE`. | “Now Wi-Fi and mobile data are off. LuckyMirror is still blocked because enforcement reads only locally validated rules. Cloud can classify; it has no path to the rule store or enforcer.” | Capture the network-off state and the block in the same run. Do not use host-network loss as a substitute for the emulator state. |
| **2:36–3:02** | Return to LoopLock, run **Run rejected UNLOCK fixture**, and frame `UNLOCK rejected`, `ACTION_NOT_ALLOWED — quarantine retained`, and `Commitment end unchanged`. Add `UNLOCK REJECTED`. | “I send a deliberately invalid `UNLOCK` proposal through the real local validator. Android rejects it as `ACTION_NOT_ALLOWED`. The tightened rule remains, and the exact commitment end is unchanged. AI cannot unlock, delete, disable, or shorten protection.” | Keep the commitment-end value visible long enough to compare with the earlier status shot. |
| **3:02–3:40** | Show `docs/submission/architecture.png` full frame. Zoom only if the Android boundary, dashed proxy, Google Cloud boundary, and red prohibited path remain readable. | “The green Android boundary is the only enforcement authority. Dashed blue paths carry minimal events and bounded proposals through the demo-only proxy. The red barrier means no cloud-to-store or cloud-to-enforcer path. This is an Android-first consumer proof of concept using two harmless fixtures. Arbitrary app or site coverage, Play distribution, public mobile authentication, and managed-device control remain production work.” | Record this card separately; it does not depend on the five-minute commitment. |
| **3:40–3:48** | Hold the full architecture or a clean LoopLock wordmark. | “Every attempted workaround can become the next protection rule—but never the next way out.” | End cleanly. No Devpost or upload claim. |

## Capture order

Capture proof in this order even though the final edit differs:

1. Record the architecture and source-code cards first; they do not depend on a commitment.
2. Prepare the sanitized Cloud Run and Firestore views, but do not expose them on the recording display yet.
3. Start the demo-only proxy and emulator bridge only after the owner authorizes use of the existing private service for that run.
4. Start the fresh five-minute commitment **last**, no more than 30 seconds before capture. Enable LoopLock through Android's visible accessibility settings and return to the app to confirm `Protected`.
5. Capture BetBurst, then tap the overlay's **Return to LoopLock** button. Do not launch another fixture while the prior overlay remains visible.
6. Turn emulator Wi-Fi and mobile data off. Install LuckyMirror, capture its local block, tap **Return to LoopLock**, and capture the original `PACKAGE_ADDED` row while it is `QUEUED`. Write down its eight-character prefix.
7. Turn emulator Wi-Fi and mobile data on. The connected-only worker should run without a failed attempt or retry backoff. Capture the terminal Android result and the Firestore document with the matching event ID.
8. Turn emulator networking off again, capture the retained LuckyMirror block, return through the overlay button, and run the rejected `UNLOCK` fixture. Restore normal emulator networking after the take.
9. Remove the emulator proxy bridge and stop the local proxy. Do not create another event merely to improve a cosmetic shot; reset to a fresh commitment if the evidence chain is no longer clear.

The earlier script activated the five-minute commitment before proxy and browser setup. That order is too fragile: setup delay can consume the commitment. Pre-stage everything and activate last.

## Reset and staging checklist

### Local reset

Run from the repository root in a dedicated terminal window with a neutral prompt and no private scrollback:

```bash
make android-build
adb -s emulator-5554 uninstall com.histopgambling.fixture.luckymirror || true
adb -s emulator-5554 shell pm clear com.histopgambling.looplock
android/scripts/install-fixtures.sh
adb -s emulator-5554 reverse --remove-all
```

Then verify:

- only LoopLock and BetBurst are installed from the demo set;
- LuckyMirror is absent;
- emulator Wi-Fi and mobile data are on for setup, then deliberately off before LuckyMirror installation and back on only after the queued event prefix is captured;
- LoopLock shows first-run disclosure, proving the app state is fresh;
- no prior LoopLock notification, overlay, or timeline is visible;
- the emulator clock and display scale make the commitment end readable.

Clearing LoopLock data force-stops the accessibility service. Re-enable it through Android's visible LoopLock service page after activation, then return to LoopLock and confirm `Protected`. Do not use a hidden secure-setting command as proof: a setting can appear enabled before the service is actually bound.

### Desktop privacy scene

- Record a cropped application/window region, not the entire desktop.
- Use one dedicated browser window containing only the sanitized Cloud Run and Firestore proof tabs.
- Hide or close Mail, messaging, calendar, password-manager, billing, account, unrelated Cloud project, and personal browser windows.
- Turn off notification previews and desktop widgets before capture.
- Hide the browser bookmarks bar, downloads shelf, profile/account menu, and any email-bearing avatar popover.
- Use a dedicated terminal with a neutral prompt such as `looplock-demo$`, enlarged text, and cleared scrollback.
- Keep the architecture image and the two short source files open before the commitment starts.
- Test microphone level with a ten-second local clip. Aim for clear speech with no clipping, keyboard noise, or system notification sound.
- Do not record until a slow sweep of every capture source reveals no email address, credential, token, billing detail, IAM identity, raw request, model reasoning, unrelated project, private tab, or personal activity.

## Dry-run call sheet

Use a stopwatch and call out these gates without recording:

- [ ] BetBurst block appears by `00:12`.
- [ ] `Protected`, exact end, and pre-authorized quarantine are legible by `00:32`.
- [ ] LuckyMirror is absent before install and locally blocked before any Cloud result.
- [ ] The original `PACKAGE_ADDED` event prefix is written on the call sheet.
- [ ] The same prefix appears at the start of the Firestore document name.
- [ ] `AGENT_TIGHTENED` is visible without implying that the agent enforced it.
- [ ] Emulator Wi-Fi and mobile data are visibly off before the second LuckyMirror launch.
- [ ] `ACTION_NOT_ALLOWED`, retained rule, and unchanged end are visible.
- [ ] Consumer-mode bypassability and managed-device future scope are spoken.
- [ ] The spoken run ends by `03:48`.

The private narration-only timing pass completed at `02:29.9` using a clear 145-word-per-minute system voice. It proves the script is not the timing bottleneck; it does not replace the full proof-frame rehearsal or owner review of the final voice track.

Record the actual elapsed time and any missed gate in a private producer note. Do not place raw recordings, private screenshots, or private Cloud evidence in Git.

### Technical dry-run result — 27 August 2026

- **Pass:** BetBurst and LuckyMirror both produced the clearly labeled local block overlay.
- **Pass:** with emulator networking off, LuckyMirror was `QUARANTINED_BEFORE_UPLOAD · QUEUED` with event prefix `d6f897fa` and zero retries.
- **Pass:** after networking was enabled, that same Android event became `TERMINAL_TIGHTEN`, the local rule became `AGENT_TIGHTENED`, and a separate agent-result audit row appeared.
- **Pass:** the Firestore document ID matched the full Android event ID and contained only the approved opaque IDs, target hash, bounded result fields, and timestamps.
- **Pass:** with emulator networking off again, LuckyMirror remained blocked; the `UNLOCK` fixture returned `ACTION_NOT_ALLOWED`, retained quarantine, and showed the unchanged commitment end.
- **Pass:** the Cloud Run service/revision and region, private invocation state, Google ADK import, and `gemini-3.5-flash` configuration were verified with read-only sanitized checks.
- **Fixed recording blocker:** proxy-only disconnection caused exponential retry and commitment expiry. Use Android network constraints for the queued shot, then enable networking before the first worker attempt.
- **Fixed recording blocker:** an old block overlay persists until **Return to LoopLock** is tapped. Clear it before launching the next fixture.
- **Pass:** a private narration-only timing pass completed at `02:29.9`, leaving `01:18.1` for proof holds and transitions in the target cut.
- **Pending:** the owner confirms the full screen-and-voice rehearsal and proof holds end by `03:48`.

## Fallback footage and stop conditions

| Failure | Truthful fallback | Release consequence |
| --- | --- | --- |
| Classification is slow or unavailable | Keep the current LuckyMirror event `QUEUED` and locally quarantined. Do not disconnect only the proxy and accumulate retry backoff. If prior successful footage is used, label it `PRIOR VERIFIED RUN` and show that prior event's own matching Android/Firestore ID. | A current-run mismatch fails the matching-event gate; do not publish as a passing final cut. |
| LuckyMirror was already installed | Remove only the harmless fixture, clear LoopLock demo state, and start a fresh commitment. | Stop the take. Never imply a pre-existing install was newly quarantined. |
| Accessibility is disabled | Show `Action required` only as an honest failure-state insert. Re-enable through Android's visible settings and restart with a fresh commitment. | Stop any take that calls the state protected. |
| Commitment expires | Show `Expired` honestly, then reset and start a new take. | Do not splice pre-expiry and post-expiry evidence into one implied live event. |
| Android and Firestore IDs differ | Keep both clips as diagnostic evidence only. | Hard fail until one matching event is captured. |
| Cloud Console cannot be sanitized | Use cropped, read-only command output for Cloud Run plus a separately sanitized Firestore document view. | Hard fail if private data remains visible or Google Cloud deployment is not legible. |
| Audio is noisy or narration overruns | Replace narration with a clean voiceover and accurate English captions. | Final judged cut must still end by `03:48`; never speed speech until it is hard to understand. |

## Owner gates

The owner must explicitly approve:

1. no-webcam narrated screen capture as the public format;
2. the exact sanitized Cloud identifiers and fields allowed on screen;
3. use of the existing private Cloud service for the event-locked recording run;
4. the final cut after duration, evidence, audio, and privacy review;
5. any upload or publication destination.

Nothing in this run sheet authorizes a Cloud/IAM change, paid resource, recording, upload, Devpost edit, or submission.
