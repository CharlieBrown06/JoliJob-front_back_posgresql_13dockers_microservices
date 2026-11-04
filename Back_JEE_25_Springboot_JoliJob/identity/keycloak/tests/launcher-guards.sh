#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
tmp="$(mktemp -d "${TMPDIR:-/tmp}/jolijob-keycloak-launcher-tests.XXXXXX")"
trap 'rm -rf -- "$tmp"' EXIT

mkdir -p "$tmp/keycloak/bin"
printf '%s\n' \
  '#!/usr/bin/env bash' \
  'set -euo pipefail' \
  '[[ "${KC_DB:-}" == postgres ]]' \
  '[[ "${KC_DB_URL:-}" == jdbc:postgresql://db.example.test:5432/keycloak ]]' \
  '[[ "${KC_DB_USERNAME:-}" == test-user ]]' \
  '[[ "${KC_DB_PASSWORD:-}" == test-password ]]' \
  'printf "%s\n" "$@" > "$FAKE_KC_ARGS_FILE"' \
  > "$tmp/keycloak/bin/kc.sh"
chmod 755 "$tmp/keycloak/bin/kc.sh"

fail() {
  printf 'FAILED: %s\n' "$1" >&2
  exit 1
}

expect_rejected() {
  local description="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    fail "$description was accepted"
  fi
}

db_env=(
  env
  "KEYCLOAK_HOME=$tmp/keycloak"
  "FAKE_KC_ARGS_FILE=$tmp/args"
  "KC_DB=postgres"
  "KC_DB_URL=jdbc:postgresql://db.example.test:5432/keycloak"
  "KC_DB_USERNAME=test-user"
  "KC_DB_PASSWORD=test-password"
)

expect_rejected "local launcher without PostgreSQL configuration" \
  env -u KC_DB -u KC_DB_URL -u KC_DB_USERNAME -u KC_DB_PASSWORD \
  "KEYCLOAK_HOME=$tmp/keycloak" "$ROOT/run-local.sh"
expect_rejected "local launcher with a file database" \
  env "KEYCLOAK_HOME=$tmp/keycloak" "KC_DB=dev-file" \
  "KC_DB_URL=jdbc:postgresql://db.example.test:5432/keycloak" \
  "KC_DB_USERNAME=test-user" "KC_DB_PASSWORD=test-password" \
  "$ROOT/run-local.sh"
expect_rejected "local launcher arguments" \
  "${db_env[@]}" "$ROOT/run-local.sh" "--http-host=0.0.0.0"

"${db_env[@]}" "$ROOT/run-local.sh"
expected_local=$'start-dev\n--http-enabled=true\n--http-host=127.0.0.1\n--http-port=8081\n--hostname=http://localhost:8081'
[[ "$(<"$tmp/args")" == "$expected_local" ]] || fail "local launcher did not pin the loopback listener and issuer"

expect_rejected "production launcher without PostgreSQL configuration" \
  env -u KC_DB -u KC_DB_URL -u KC_DB_USERNAME -u KC_DB_PASSWORD \
  "KEYCLOAK_HOME=$tmp/keycloak" "KC_HOSTNAME=https://identity.example.test" \
  "$ROOT/run-production.sh"
expect_rejected "production launcher without TLS configuration" \
  "${db_env[@]}" "KC_HOSTNAME=https://identity.example.test" \
  "$ROOT/run-production.sh"
expect_rejected "production launcher with an HTTP hostname" \
  "${db_env[@]}" "KC_HOSTNAME=http://identity.example.test" \
  "KC_HTTPS_CERTIFICATE_FILE=$tmp/cert.pem" \
  "KC_HTTPS_CERTIFICATE_KEY_FILE=$tmp/key.pem" "$ROOT/run-production.sh"

: > "$tmp/cert.pem"
: > "$tmp/key.pem"
chmod 600 "$tmp/key.pem"
"${db_env[@]}" "KC_HOSTNAME=https://identity.example.test" \
  "KC_HTTPS_CERTIFICATE_FILE=$tmp/cert.pem" \
  "KC_HTTPS_CERTIFICATE_KEY_FILE=$tmp/key.pem" \
  "$ROOT/run-production.sh"
expected_direct=$'start\n--hostname=https://identity.example.test\n--hostname-strict=true\n--http-enabled=false\n--https-certificate-file='"$tmp"$'/cert.pem\n--https-certificate-key-file='"$tmp"$'/key.pem'
[[ "$(<"$tmp/args")" == "$expected_direct" ]] || fail "direct TLS production configuration was not passed safely"

expect_rejected "production launcher trusting all proxy addresses" \
  "${db_env[@]}" "KC_HOSTNAME=https://identity.example.test" \
  "KC_HTTP_ENABLED=true" "KC_PROXY_HEADERS=forwarded" \
  "KC_PROXY_TRUSTED_ADDRESSES=0.0.0.0/0" "$ROOT/run-production.sh"
"${db_env[@]}" "KC_HOSTNAME=https://identity.example.test" \
  "KC_HTTP_ENABLED=true" "KC_PROXY_HEADERS=xforwarded" \
  "KC_PROXY_TRUSTED_ADDRESSES=10.0.1.5,10.0.1.6" \
  "$ROOT/run-production.sh"
expected_proxy=$'start\n--hostname=https://identity.example.test\n--hostname-strict=true\n--http-enabled=true\n--proxy-headers=xforwarded\n--proxy-trusted-addresses=10.0.1.5,10.0.1.6'
[[ "$(<"$tmp/args")" == "$expected_proxy" ]] || fail "trusted-proxy settings were not passed safely"

printf '%s\n' "Launcher guard tests passed."
