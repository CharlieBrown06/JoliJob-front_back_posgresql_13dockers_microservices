require_postgres() {
  if [[ "${KC_DB:-}" != "postgres" ]]; then
    printf '%s\n' "Set KC_DB=postgres; the development-file database is not allowed." >&2
    return 2
  fi
  if [[ ! "${KC_DB_URL:-}" =~ ^jdbc:postgresql://[^[:space:]]+$ ]]; then
    printf '%s\n' "Set KC_DB_URL to a PostgreSQL JDBC URL." >&2
    return 2
  fi
  if [[ -z "${KC_DB_USERNAME:-}" || -z "${KC_DB_PASSWORD:-}" ]]; then
    printf '%s\n' "Set KC_DB_USERNAME and KC_DB_PASSWORD through a protected environment or secret manager." >&2
    return 2
  fi
}
