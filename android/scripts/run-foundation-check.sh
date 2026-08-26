#!/usr/bin/env bash
set -euo pipefail

android_sdk_root="${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}"
adb_bin="${android_sdk_root}/platform-tools/adb"
serial="${ANDROID_SERIAL:-emulator-5554}"

"${adb_bin}" -s "${serial}" shell am start -W -n com.histopgambling.looplock/.MainActivity
"${adb_bin}" -s "${serial}" shell am start -W -n com.histopgambling.fixture.betburst/.MainActivity
"${adb_bin}" -s "${serial}" shell am start -W -n com.histopgambling.fixture.luckymirror/.MainActivity

"${adb_bin}" -s "${serial}" shell pm list packages | grep 'histopgambling'

