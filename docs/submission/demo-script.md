# Four-Minute Demo Script — Owner Review Draft

Status: engineering path verified twice; recording, timing, sanitization, upload, public-link check, and owner approval remain pending.

## Recording setup

1. Build all APKs and install only LoopLock and BetBurst:

   ```bash
   make android-build
   android/scripts/install-fixtures.sh
   ```

2. Confirm LuckyMirror is not installed. If it remains from rehearsal, remove only that harmless fixture before opening LoopLock:

   ```bash
   adb uninstall com.histopgambling.fixture.luckymirror
   ```

3. Start a fresh five-minute commitment with LuckyMirror quarantine enabled. Enable LoopLock in Android accessibility settings and confirm `Protected`.

4. Start the private IAM-authenticated Cloud Run proxy, then connect the emulator to port 8081:

   ```bash
   GOOGLE_CLOUD_PROJECT="looplock-hackathon-2026-v8k3" ./infra/start-demo-proxy.sh
   android/scripts/connect-demo-proxy.sh
   ```

5. Open only pre-arranged sanitized Cloud Console tabs. Hide email addresses, access tokens, billing details, raw request bodies, prompts, model reasoning, unrelated projects, and account menus.

## Judged cut: target 3:50

- **0:00–0:12 — Show it working first.** Launch BetBurst and show the local block immediately. Voiceover: “Static blockers remember a list. LoopLock remembers the loophole—and its AI can never unlock you.”
- **0:12–0:32 — Calm-state policy.** Return to LoopLock. Show the exact end time, `Protected`, pre-authorized LuckyMirror quarantine, and the local-only accountability preview. State that consumer mode is voluntary and bypassable.
- **0:32–1:05 — Real workaround event.** Run `android/scripts/install-workaround-fixture.sh`, launch LuckyMirror, and show that Android quarantined it locally before cloud classification. Keep one opaque event ID visible.
- **1:05–1:40 — Agent workflow.** Use a jump cut over network wait. Show the same event changing from queued/quarantined to accepted `AGENT_TIGHTENED`. Say: “A bounded Google ADK agent on Cloud Run uses Gemini 3.5 Flash to return only `TIGHTEN` or `REVIEW`; the phone decides whether it is safe.”
- **1:40–2:20 — Visible Google Cloud proof.** Show the private Cloud Run service/revision and region, then the matching minimal terminal Firestore document. Point out one event ID, target hash, bounded result, timestamps, private authentication, and absence of raw package/account data. Do not show credentials or model reasoning.
- **2:20–2:50 — Offline ratchet.** Disable emulator Wi-Fi and mobile data, relaunch LuckyMirror, and show the local block. State that Cloud has no enforcement authority.
- **2:50–3:20 — Adversarial safety proof.** In LoopLock, run the visible `UNLOCK` fixture. Show `ACTION_NOT_ALLOWED`, retained quarantine/tightened rule, and the unchanged commitment end.
- **3:20–3:48 — Architecture and limits.** Show the final diagram. Identify the Android trust boundary, dashed demo-only IAM proxy, and private Google Cloud classifier. State that arbitrary app/site coverage, Play distribution, and managed-device strength are production work—not hackathon claims.
- **3:48–3:55 — Close.** “Every attempted workaround can become the next protection rule, but never the next way out.”

## Edit and truthfulness rules

- Keep judged content at or below 4:00; target 3:50–3:55 to leave encoding margin.
- Use cuts to remove loading and waiting, but do not splice unrelated event IDs or imply a failed live event succeeded.
- Start already logged in; do not film sign-in, setup, password entry, or token generation.
- Use on-screen labels for `LOCAL BLOCK`, `QUARANTINE BEFORE CLOUD`, `GEMINI 3.5 FLASH + GOOGLE ADK`, `OFFLINE`, and `UNLOCK REJECTED`.
- The architecture and voiceover must label the localhost proxy as demo-only and must not imply Cloud can enforce, unlock, or write Android state.

## Honest fallback

If classification is unavailable, show the current event remaining locally quarantined and queued. A separately captured successful trace may explain the backend only if it is visibly labeled as prior evidence and uses its own matching event ID. Never inject or edit a fake cloud success.
