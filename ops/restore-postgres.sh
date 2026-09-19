#!/usr/bin/env sh
set -eu

: "${POSTGRES_CONTAINER:=jeep-postgres}"
: "${POSTGRES_USER:=jeep}"
: "${POSTGRES_DB:=jeep}"

backup_file="${1:?Usage: restore-postgres.sh path/to/backup.dump}"
test -f "$backup_file"
docker exec -i "$POSTGRES_CONTAINER" pg_restore --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$backup_file"
printf 'Restored %s into %s\n' "$backup_file" "$POSTGRES_DB"
