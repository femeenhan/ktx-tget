#!/usr/bin/env bash
# Convenience wrapper — forwards to scripts/run_app.sh from project root.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${ROOT}/scripts/run_app.sh" "$@"
