# Four-Minute Demo Script — Owner Review Draft

Status: engineering path verified twice; local producer staging is in progress. Recording, final timing, sanitization, Cloud capture, upload, public-link check, and owner approval remain pending.

Detailed producer guidance: [demo-run-sheet.md](demo-run-sheet.md).

## Recording setup

1. Build all APKs, remove LuckyMirror, clear LoopLock's local demo state, and install only LoopLock and BetBurst:

   ```bash
   make android-build
   adb uninstall com.histopgambling.fixture.luckymirror || true
   adb shell pm clear com.histopgambling.looplock || true
   android/scripts/install-fixtures.sh
   ```

2. Confirm LuckyMirror is not installed and LoopLock opens on the first disclosure. Reinstalling with `-r` does not clear an old commitment, so the `pm clear` check is required before every fresh take.

   ```bash
   adb shell pm list packages com.histopgambling.fixture.luckymirror
   ```

3. Prepare the dedicated recording terminal, architecture/source cards, and sanitized Cloud proof views. Hide private tabs, account menus, email addresses, notifications, credentials, billing, IAM identities beyond the approved proof, raw requests, prompts, and model reasoning.

4. After fresh owner authorization to use the existing private service, start the IAM-authenticated Cloud Run proxy and connect the emulator to port 8081:

   ```bash
   GOOGLE_CLOUD_PROJECT="looplock-hackathon-2026-v8k3" ./infra/start-demo-proxy.sh
   android/scripts/connect-demo-proxy.sh
   ```

5. Start the fresh five-minute commitment **last**, no more than 30 seconds before capture, with LuckyMirror quarantine enabled. Enable LoopLock through Android's visible accessibility settings, return to the app, and confirm `Protected`.

## Judged cut: target 3:48

- **0:00–0:12 — Show it working first.** Launch BetBurst and show the local block immediately. Voiceover: “Static blockers remember a list. LoopLock remembers the loophole—and its AI can never unlock you.”
- **0:12–0:32 — Calm-state policy.** Return to LoopLock. Show the exact end time, `Protected`, and pre-authorized LuckyMirror quarantine. State that consumer mode is voluntary and bypassable.
- **0:32–0:58 — Real workaround event.** Return through the BetBurst overlay button, turn emulator networking off, run `android/scripts/install-workaround-fixture.sh`, and launch LuckyMirror. Show that Android quarantined it locally before cloud classification. Return through the LuckyMirror overlay button and highlight the original `PACKAGE_ADDED` row's eight-character opaque event prefix while it is queued. Turn networking on only after that shot.
- **0:58–1:28 — Agent workflow.** Use a jump cut over network wait. Show the original package event terminal, the local rule accepted as `AGENT_TIGHTENED`, and the separate agent-result audit row. Say: “A bounded Google ADK agent on Cloud Run uses Gemini 3.5 Flash to return only `TIGHTEN` or `REVIEW`; the phone decides whether it is safe.”
- **1:28–2:10 — Visible Google Cloud proof.** Show the private Cloud Run service/revision and region, the repository configuration naming Google ADK and Gemini 3.5 Flash, then the minimal terminal Firestore document whose full ID begins with the Android prefix. Point out the target hash, bounded result, timestamps, private authentication, and absence of raw package/account data. Do not show credentials or model reasoning.
- **2:10–2:36 — Offline ratchet.** Disable emulator Wi-Fi and mobile data, relaunch LuckyMirror, and show the local block. State that Cloud has no enforcement authority.
- **2:36–3:02 — Adversarial safety proof.** In LoopLock, run the visible `UNLOCK` fixture. Show `ACTION_NOT_ALLOWED`, retained quarantine/tightened rule, and the unchanged commitment end.
- **3:02–3:40 — Architecture and limits.** Show the final diagram. Identify the Android trust boundary, dashed demo-only IAM proxy, and private Google Cloud classifier. State that arbitrary app/site coverage, Play distribution, public mobile authentication, and managed-device strength are production work—not hackathon claims.
- **3:40–3:48 — Close.** “Every attempted workaround can become the next protection rule, but never the next way out.”

## Edit and truthfulness rules

- Keep judged content at or below 4:00; target 3:48 to leave room for captions and transitions.
- Use cuts to remove loading and waiting, but do not splice unrelated event IDs or imply a failed live event succeeded.
- Start already logged in; do not film sign-in, setup, password entry, or token generation.
- Use on-screen labels for `LOCAL BLOCK`, `QUARANTINE BEFORE CLOUD`, `GEMINI 3.5 FLASH + GOOGLE ADK`, `OFFLINE`, and `UNLOCK REJECTED`.
- The architecture and voiceover must label the localhost proxy as demo-only and must not imply Cloud can enforce, unlock, or write Android state.

## Honest fallback

If classification is unavailable, show the current event remaining locally quarantined and queued. A separately captured successful trace may explain the backend only if it is visibly labeled as prior evidence and uses its own matching event ID. Never inject or edit a fake cloud success.
