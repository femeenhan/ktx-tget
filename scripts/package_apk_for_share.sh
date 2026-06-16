#!/usr/bin/env bash
# Package an APK as a ZIP for messengers that block .apk (e.g. KakaoTalk, Gmail).
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $(basename "$0") <path-to.apk>" >&2
  exit 1
fi

APK_PATH="$1"
if [[ ! -f "${APK_PATH}" ]]; then
  echo "APK not found: ${APK_PATH}" >&2
  exit 1
fi

APK_DIR="$(cd "$(dirname "${APK_PATH}")" && pwd)"
APK_BASENAME="$(basename "${APK_PATH}" .apk)"
ZIP_PATH="${APK_DIR}/${APK_BASENAME}-kakao.zip"

rm -f "${ZIP_PATH}"
zip -j -q "${ZIP_PATH}" "${APK_PATH}"
echo "${ZIP_PATH}"
