#!/usr/bin/env bash
# Generate a license key for a buyer's device ID.
# The key is derived from the device ID using HMAC-SHA256 with your licenseSecret.
# Run once per buyer — no APK build needed.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

usage() {
  echo "Usage: $(basename "$0") <device_id>"
  echo "  device_id  16-char Android ID copied from the app's activation screen"
  echo "Example:"
  echo "  $(basename "$0") a1b2c3d4e5f67890"
  exit 1
}

if [[ $# -ne 1 ]]; then
  usage
fi

DEVICE_ID="$(echo "$1" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')"

if [[ -z "${DEVICE_ID}" ]]; then
  echo "Error: device_id is empty." >&2
  exit 1
fi

SECRET="$(grep "^licenseSecret=" "${ROOT}/local.properties" 2>/dev/null | cut -d'=' -f2- | tr -d '[:space:]')"
if [[ -z "${SECRET}" ]]; then
  echo "Error: licenseSecret not found in local.properties" >&2
  echo "Add this line to local.properties:" >&2
  echo "  licenseSecret=your_secret_here" >&2
  exit 1
fi

KEY="$(KTXTGET_DEVICE_ID="${DEVICE_ID}" KTXTGET_SECRET="${SECRET}" python3 -c "
import hmac, hashlib, os
device_id = os.environ['KTXTGET_DEVICE_ID']
secret = os.environ['KTXTGET_SECRET']
h = hmac.new(secret.encode('utf-8'), device_id.encode('utf-8'), hashlib.sha256).digest()
print('-'.join(h[:8].hex().upper()[i:i+4] for i in range(0, 16, 4)))
")"

echo "Device ID  : ${DEVICE_ID}"
echo "License Key: ${KEY}"
echo ""
echo "Send the License Key to the buyer. They enter it in the app to activate."
