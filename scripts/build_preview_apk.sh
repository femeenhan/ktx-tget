#!/usr/bin/env bash
# Build the universal release APK (single APK for all buyers).
# Buyers activate with a license key inside the app — no per-device build needed.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DIST_DIR="${ROOT}/dist"
OUTPUT_NAME="ktx-tget-release.apk"

if ! grep -q "^licenseSecret=" "${ROOT}/local.properties" 2>/dev/null; then
  echo "Warning: licenseSecret not set in local.properties — using default secret." >&2
  echo "Add 'licenseSecret=your_secret_here' to local.properties before distributing." >&2
fi

cd "${ROOT}"
mkdir -p "${DIST_DIR}"

echo ">> gradlew assembleRelease"
./gradlew assembleRelease

APK_SRC="${ROOT}/app/build/outputs/apk/release/app-release-unsigned.apk"
if [[ ! -f "${APK_SRC}" ]]; then
  APK_SRC="${ROOT}/app/build/outputs/apk/release/app-release.apk"
fi
if [[ ! -f "${APK_SRC}" ]]; then
  echo "Release APK not found under app/build/outputs/apk/release/" >&2
  exit 1
fi

cp "${APK_SRC}" "${DIST_DIR}/${OUTPUT_NAME}"
ZIP_PATH="$("${SCRIPT_DIR}/package_apk_for_share.sh" "${DIST_DIR}/${OUTPUT_NAME}")"
echo ">> Wrote ${DIST_DIR}/${OUTPUT_NAME}"
echo ">> Wrote ${ZIP_PATH} (send this ZIP on KakaoTalk; .apk direct send is blocked)"
echo ""
echo "This is the ONLY APK you need to distribute to all buyers."
echo "To generate a license key for a buyer: ./scripts/generate_key.sh <device_id>"
