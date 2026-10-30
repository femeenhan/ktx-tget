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

# 정식 키로 서명됐는지 확인한다 (debug 키 서명 APK는 기존 설치본에 덮어쓸 수 없다).
SDK_DIR="$(grep -m1 '^sdk.dir=' local.properties | cut -d= -f2- || true)"
SDK_DIR="${SDK_DIR:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}"
BUILD_TOOLS="$(ls -1 "${SDK_DIR}/build-tools" 2>/dev/null | sort -V | tail -1)"
APKSIGNER="${SDK_DIR}/build-tools/${BUILD_TOOLS}/apksigner"
if [[ -z "${BUILD_TOOLS}" || ! -x "${APKSIGNER}" ]]; then
  echo "apksigner not found under ${SDK_DIR}/build-tools" >&2
  exit 1
fi
CERTS="$("${APKSIGNER}" verify --print-certs "${APK_SRC}")"
if ! grep -q "CN=bigseasons" <<<"${CERTS}"; then
  echo "APK is not signed with the release key (CN=bigseasons missing):" >&2
  echo "${CERTS}" >&2
  exit 1
fi
echo ">> Signature OK: $(grep -m1 'Signer #1 certificate DN' <<<"${CERTS}")"

VERSION_NAME="$(grep -m1 'versionName' app/build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/')"
OUT="${DIST_DIR}/ktx-tget-${VERSION_NAME}.apk"
cp "${APK_SRC}" "${OUT}"
echo ">> Wrote ${OUT}"
