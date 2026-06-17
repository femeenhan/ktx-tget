#!/usr/bin/env bash
# DEPRECATED: per-device APK builds are no longer needed.
# The app now uses a single APK with in-app license key activation.
#
# To generate a license key for a buyer:
#   ./scripts/generate_key.sh <device_id>
#
# To build the universal APK:
#   ./scripts/build_preview_apk.sh

echo "This script is deprecated. Use generate_key.sh to create a license key instead." >&2
echo "  ./scripts/generate_key.sh <device_id>" >&2
exit 1
