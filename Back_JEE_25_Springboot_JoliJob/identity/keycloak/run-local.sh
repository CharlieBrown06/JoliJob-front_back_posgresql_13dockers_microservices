#!/usr/bin/env bash
set -euo pipefail

if (($# != 0)); then
  printf '%s\n' "run-local.sh does not accept arguments." >&2
  exit 2
fi

: "${KEYCLOAK_HOME:?Set KEYCLOAK_HOME to an existing Keycloak installation.}"
. "$(dirname "${BASH_SOURCE[0]}")/require-postgres.sh"
require_postgres
KC_BIN="${KEYCLOAK_HOME%/}/bin/kc.sh"
if [[ ! -x "$KC_BIN" ]]; then
  printf 'Keycloak executable is missing or not executable: %s\n' "$KC_BIN" >&2
  exit 2
fi

exec "$KC_BIN" start-dev \
  --http-enabled=true \
  --http-host=127.0.0.1 \
  --http-port=8081 \
  --hostname=http://localhost:8081
