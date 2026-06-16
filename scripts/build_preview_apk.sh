#!/usr/bin/env bash
# Build a preview APK (no device ID) so buyers can read and copy their Android ID.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DIST_DIR="${ROOT}/dist"
OUTPUT_NAME="ktx-tget-preview.apk"

cd "${ROOT}"
mkdir -p "${DIST_DIR}"

echo ">> gradlew assembleRelease (preview, no allowedDeviceId)"
./gradlew assembleRelease -PallowedDeviceId=

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
echo ">> Wrote ${ZIP_PATH} (send this file on KakaoTalk; .apk direct send is blocked)"
echo ">> Or upload ${DIST_DIR}/${OUTPUT_NAME} to Google Drive and share the link."
echo ">> Gmail APK attachments are often blocked too."
echo ">> Buyers may need to turn OFF Play Protect scanning before install (see docs/buyer_setup_guide.md §1-2)."
echo ">> Send this APK so the buyer can install it, copy their device ID, and send it back."
