#!/usr/bin/env bash

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    printf '%s\n' 'Source this file in Bash: source database/postgresql/local-env.sh' >&2
    exit 1
fi

if ! read -r -s -p 'Mot de passe PostgreSQL du role admin : ' DB_PASSWORD; then
    printf '\n%s\n' 'Impossible de lire le mot de passe PostgreSQL.' >&2
    return 1
fi
printf '\n'
if [[ -z "$DB_PASSWORD" ]]; then
    printf '%s\n' 'Le mot de passe PostgreSQL ne peut pas etre vide.' >&2
    return 1
fi

export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=jolijob_db
export DB_USERNAME=admin
export DB_PASSWORD
export SPRING_PROFILES_ACTIVE=local
