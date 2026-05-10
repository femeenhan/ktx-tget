#!/usr/bin/env bash
# Build debug APK, install on a connected device, and launch MainActivity.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT}"

APP_ID="dev.ktxtget"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

usage() {
  echo "Usage: $(basename "$0") [options]"
  echo "  (default)     ./gradlew installDebug, then start MainActivity"
  echo "  --launch-only Skip Gradle; only adb start (app must already be installed)"
  echo "  --install-only Run installDebug only; do not start the app"
  echo "  --logcat       After launch, follow Logcat for tag KtxA11y (Ctrl+C to stop)"
  exit 0
}

LAUNCH_ONLY=false
INSTALL_ONLY=false
LOGCAT=false
for arg in "$@"; do
  case "${arg}" in
    -h|--help) usage ;;
    --launch-only) LAUNCH_ONLY=true ;;
    --install-only) INSTALL_ONLY=true ;;
    --logcat) LOGCAT=true ;;
    *) echo "Unknown option: ${arg}" >&2; usage ;;
  esac
done

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android platform-tools and add them to PATH." >&2
  exit 1
fi

DEVICE_COUNT="$(adb devices | awk '/\tdevice$/ {c++} END {print c+0}')"
if [[ "${DEVICE_COUNT}" -eq 0 ]]; then
  echo "No device in 'device' state. Connect a device and enable USB debugging." >&2
  exit 1
fi
if [[ "${DEVICE_COUNT}" -gt 1 ]] && [[ -z "${ANDROID_SERIAL:-}" ]]; then
  echo "Multiple devices attached. Set ANDROID_SERIAL or disconnect extras." >&2
  adb devices -l
  exit 1
fi

if [[ "${LAUNCH_ONLY}" == false ]]; then
  echo ">> gradlew installDebug"
  ./gradlew installDebug
fi

if [[ "${INSTALL_ONLY}" == true ]]; then
  echo ">> Skipping launch (--install-only)."
  exit 0
fi

echo ">> adb shell am start -n ${MAIN_ACTIVITY}"
adb shell am start -n "${MAIN_ACTIVITY}"

if [[ "${LOGCAT}" == true ]]; then
  echo ">> adb logcat KtxA11y:D *:S (stop with Ctrl+C)"
  adb logcat KtxA11y:D '*:S'
fi
