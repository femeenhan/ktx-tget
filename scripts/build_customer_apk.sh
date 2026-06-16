#!/usr/bin/env bash
# Build a customer APK locked to one Android device ID.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DIST_DIR="${ROOT}/dist"

usage() {
  echo "Usage: $(basename "$0") <device_id>"
  echo "  device_id  ANDROID_ID copied from the preview APK on the buyer's phone"
  echo "Example:"
  echo "  $(basename "$0") a1b2c3d4e5f67890"
  exit 1
}

if [[ $# -ne 1 ]]; then
  usage
fi

RAW_ID="$1"
DEVICE_ID="$(echo "${RAW_ID}" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')"
if [[ -z "${DEVICE_ID}" ]]; then
  echo "device_id is empty." >&2
  exit 1
fi
if [[ ! "${DEVICE_ID}" =~ ^[0-9a-f]{8,16}$ ]]; then
  echo "Warning: device_id format looks unusual: ${DEVICE_ID}" >&2
fi

cd "${ROOT}"
mkdir -p "${DIST_DIR}"

echo ">> gradlew assembleRelease -PallowedDeviceId=${DEVICE_ID}"
./gradlew assembleRelease "-PallowedDeviceId=${DEVICE_ID}"

APK_SRC="${ROOT}/app/build/outputs/apk/release/app-release-unsigned.apk"
if [[ ! -f "${APK_SRC}" ]]; then
  APK_SRC="${ROOT}/app/build/outputs/apk/release/app-release.apk"
fi
if [[ ! -f "${APK_SRC}" ]]; then
  echo "Release APK not found under app/build/outputs/apk/release/" >&2
  exit 1
fi

OUTPUT_NAME="ktx-tget-${DEVICE_ID}.apk"
cp "${APK_SRC}" "${DIST_DIR}/${OUTPUT_NAME}"
echo ">> Wrote ${DIST_DIR}/${OUTPUT_NAME}"
echo ">> Send this APK only to the buyer who owns device ID ${DEVICE_ID}."
