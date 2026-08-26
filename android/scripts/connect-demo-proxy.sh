#!/usr/bin/env bash
set -euo pipefail

android_sdk_root="${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}"
adb_bin="${android_sdk_root}/platform-tools/adb"
serial="${ANDROID_SERIAL:-emulator-5554}"

"${adb_bin}" -s "${serial}" reverse tcp:8081 tcp:8081

echo "Forwarded emulator localhost:8081 to the IAM-authenticated Mac proxy on port 8081."
echo "This bridge is demo-only and gives Cloud Run no authority over Android enforcement."
