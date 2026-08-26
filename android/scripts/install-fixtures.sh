#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
android_sdk_root="${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}"
adb_bin="${android_sdk_root}/platform-tools/adb"
serial="${ANDROID_SERIAL:-emulator-5554}"

"${adb_bin}" -s "${serial}" install -r "${repo_root}/android/app/build/outputs/apk/debug/app-debug.apk"
"${adb_bin}" -s "${serial}" install -r "${repo_root}/android/fixtures/betburst/build/outputs/apk/debug/betburst-debug.apk"

echo "Installed the initial demo set (LoopLock and BetBurst Demo) on ${serial}."
echo "Keep LuckyMirror Demo uninstalled until the active-commitment workaround step."
echo "Then run android/scripts/install-workaround-fixture.sh."
