#!/usr/bin/env bash
set -euo pipefail

if (($# != 0)); then
  printf '%s\n' "run-production.sh does not accept arguments." >&2
  exit 2
fi

: "${KEYCLOAK_HOME:?Set KEYCLOAK_HOME to an existing Keycloak installation.}"
: "${KC_HOSTNAME:?Set KC_HOSTNAME to the canonical https:// hostname.}"
. "$(dirname "${BASH_SOURCE[0]}")/require-postgres.sh"
require_postgres
KC_BIN="${KEYCLOAK_HOME%/}/bin/kc.sh"
if [[ ! -x "$KC_BIN" ]]; then
  printf 'Keycloak executable is missing or not executable: %s\n' "$KC_BIN" >&2
  exit 2
fi

if [[ ! "$KC_HOSTNAME" =~ ^https://([A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z]([A-Za-z0-9-]*[A-Za-z0-9])?(:[0-9]{1,5})?$ ]]; then
  printf '%s\n' "KC_HOSTNAME must be a canonical HTTPS hostname without a path." >&2
  exit 2
fi

direct_tls=false
proxy_tls=false
if [[ -n "${KC_HTTPS_CERTIFICATE_FILE:-}" && -n "${KC_HTTPS_CERTIFICATE_KEY_FILE:-}" ]]; then
  direct_tls=true
fi
if [[ "${KC_HTTP_ENABLED:-}" == "true" \
  && ( "${KC_PROXY_HEADERS:-}" == "forwarded" || "${KC_PROXY_HEADERS:-}" == "xforwarded" ) \
  && -n "${KC_PROXY_TRUSTED_ADDRESSES:-}" ]]; then
  proxy_tls=true
fi

if [[ "$direct_tls" == "$proxy_tls" ]]; then
  printf '%s\n' "Configure exactly one TLS mode: protected PEM files, or a trusted TLS proxy." >&2
  exit 2
fi

if [[ "$direct_tls" == true ]]; then
  cert="$KC_HTTPS_CERTIFICATE_FILE"
  key="$KC_HTTPS_CERTIFICATE_KEY_FILE"
  if [[ ! -f "$cert" || ! -r "$cert" ]]; then
    printf '%s\n' "KC_HTTPS_CERTIFICATE_FILE must be a readable regular file." >&2
    exit 2
  fi
  if [[ ! -f "$key" || ! -r "$key" ]]; then
    printf '%s\n' "KC_HTTPS_CERTIFICATE_KEY_FILE must be a readable regular file." >&2
    exit 2
  fi
  key_owner="$(stat -c '%u' -- "$key")"
  key_mode="$(stat -c '%a' -- "$key")"
  if [[ "$key_owner" != "$(id -u)" ]] || (( (8#$key_mode & 077) != 0 )); then
    printf '%s\n' "TLS private key must be owned by this user and have no group/world permissions." >&2
    exit 2
  fi
  if [[ "${KC_HTTP_ENABLED:-false}" == "true" ]]; then
    printf '%s\n' "KC_HTTP_ENABLED cannot be true with direct TLS mode." >&2
    exit 2
  fi
  export KC_HTTP_ENABLED=false
  unset KC_PROXY_HEADERS KC_PROXY_TRUSTED_ADDRESSES
else
  if [[ -n "${KC_HTTPS_CERTIFICATE_FILE:-}" || -n "${KC_HTTPS_CERTIFICATE_KEY_FILE:-}" ]]; then
    printf '%s\n' "Do not mix PEM files with trusted proxy TLS mode." >&2
    exit 2
  fi
  case ",$KC_PROXY_TRUSTED_ADDRESSES," in
    *,0.0.0.0/0,*|*,::/0,*)
      printf '%s\n' "KC_PROXY_TRUSTED_ADDRESSES cannot trust all IPv4 or IPv6 addresses." >&2
      exit 2
      ;;
  esac
fi

if [[ "$direct_tls" == true ]]; then
  exec "$KC_BIN" start \
    --hostname="$KC_HOSTNAME" \
    --hostname-strict=true \
    --http-enabled=false \
    --https-certificate-file="$KC_HTTPS_CERTIFICATE_FILE" \
    --https-certificate-key-file="$KC_HTTPS_CERTIFICATE_KEY_FILE"
fi

exec "$KC_BIN" start \
  --hostname="$KC_HOSTNAME" \
  --hostname-strict=true \
  --http-enabled=true \
  --proxy-headers="$KC_PROXY_HEADERS" \
  --proxy-trusted-addresses="$KC_PROXY_TRUSTED_ADDRESSES"
