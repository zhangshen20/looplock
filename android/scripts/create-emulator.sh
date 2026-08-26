#!/usr/bin/env bash
set -euo pipefail

android_sdk_root="${ANDROID_SDK_ROOT:-${HOME}/Library/Android/sdk}"
android_jdk="${ANDROID_STUDIO_JDK:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
avd_name="LoopLock_API_36"
image="system-images;android-36;google_apis;arm64-v8a"

if [[ ! -x "${android_sdk_root}/cmdline-tools/latest/bin/avdmanager" ]]; then
  echo "Android command-line tools are missing from ${android_sdk_root}."
  exit 1
fi

if [[ ! -d "${android_sdk_root}/system-images/android-36/google_apis/arm64-v8a" ]]; then
  echo "Install ${image} before creating the reference emulator."
  exit 1
fi

if "${android_sdk_root}/emulator/emulator" -list-avds | grep -qx "${avd_name}"; then
  echo "${avd_name} already exists."
  exit 0
fi

ANDROID_SDK_ROOT="${android_sdk_root}" JAVA_HOME="${android_jdk}" \
  "${android_sdk_root}/cmdline-tools/latest/bin/avdmanager" create avd \
  --name "${avd_name}" \
  --package "${image}" \
  --device pixel_9

