#!/usr/bin/env sh
set -eu

: "${POSTGRES_CONTAINER:=jeep-postgres}"
: "${POSTGRES_USER:=jeep}"
: "${POSTGRES_DB:=jeep}"
: "${BACKUP_DIR:=./backups}"

mkdir -p "$BACKUP_DIR"
backup_file="$BACKUP_DIR/${POSTGRES_DB}-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker exec "$POSTGRES_CONTAINER" pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB" > "$backup_file"
chmod 600 "$backup_file"
printf 'Created %s\n' "$backup_file"
