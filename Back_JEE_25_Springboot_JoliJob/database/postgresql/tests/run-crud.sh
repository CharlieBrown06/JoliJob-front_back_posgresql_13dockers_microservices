#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v psql >/dev/null 2>&1; then
    printf '%s\n' 'psql is required to run the PostgreSQL CRUD tests.' >&2
    exit 1
fi

exec psql -X -W \
    --host="${DB_HOST:-localhost}" \
    --port="${DB_PORT:-5432}" \
    --username="${DB_USERNAME:-admin}" \
    --dbname="${DB_NAME:-jolijob_db}" \
    --set=ON_ERROR_STOP=1 \
    --pset=pager=off \
    --file="$script_dir/crud.sql"
