#!/usr/bin/env bash
# Build the signed release APK and copy it to dist/ktx-tget-<versionName>.apk.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DIST_DIR="${ROOT}/dist"

cd "${ROOT}"
mkdir -p "${DIST_DIR}"

echo ">> gradlew assembleRelease"
./gradlew assembleRelease

APK_SRC="${ROOT}/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "${APK_SRC}" ]]; then
  echo "Signed release APK not found (is the keystore configured in local.properties?)" >&2
  exit 1
fi

VERSION_NAME="$(grep -m1 'versionName' app/build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/')"
OUT="${DIST_DIR}/ktx-tget-${VERSION_NAME}.apk"
cp "${APK_SRC}" "${OUT}"
echo ">> Wrote ${OUT}"
