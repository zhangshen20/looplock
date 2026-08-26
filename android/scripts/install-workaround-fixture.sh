#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
android_sdk_root="${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}"
adb_bin="${android_sdk_root}/platform-tools/adb"
serial="${ANDROID_SERIAL:-emulator-5554}"

"${adb_bin}" -s "${serial}" install -r \
  "${repo_root}/android/fixtures/luckymirror/build/outputs/apk/debug/luckymirror-debug.apk"

echo "Installed LuckyMirror Demo on ${serial} as the active-commitment workaround fixture."
