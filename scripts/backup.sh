#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
BACKUP_PREFIX="${BACKUP_PREFIX:-srm_credit}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5656}"
PGDATABASE="${PGDATABASE:-srm_credit}"
PGUSER="${PGUSER:-postgres}"
PGSCHEMA="${PGSCHEMA:-public}"
PG_DUMP_BIN="${PG_DUMP_BIN:-pg_dump}"
USE_DOCKER="${USE_DOCKER:-0}"

if ! [[ "$RETENTION_DAYS" =~ ^[0-9]+$ ]] || (( RETENTION_DAYS < 1 )); then
  printf '%s\n' "RETENTION_DAYS must be a positive integer" >&2
  exit 1
fi

if ! [[ "$PGSCHEMA" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
  printf '%s\n' "PGSCHEMA must be a valid PostgreSQL identifier" >&2
  exit 1
fi

if [[ "$USE_DOCKER" == "1" ]]; then
  if ! command -v docker >/dev/null 2>&1; then
    printf '%s\n' "docker is required when USE_DOCKER=1" >&2
    exit 1
  fi
  PG_DUMP_CMD=(docker compose exec -T -e "PGHOST=localhost" -e "PGPORT=5432" -e "PGDATABASE=$PGDATABASE" -e "PGUSER=$PGUSER" -e "PGPASSWORD=${PGPASSWORD-}" postgres pg_dump)
  PG_DUMP_MODE="stream"
else
  if ! command -v "$PG_DUMP_BIN" >/dev/null 2>&1; then
    printf '%s\n' "$PG_DUMP_BIN was not found; install PostgreSQL client tools or use USE_DOCKER=1" >&2
    exit 1
  fi
  PG_DUMP_CMD=("$PG_DUMP_BIN" --host="$PGHOST" --port="$PGPORT" --username="$PGUSER")
  PG_DUMP_MODE="file"
fi

umask 077
mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_FILE="$BACKUP_DIR/${BACKUP_PREFIX}_${TIMESTAMP}.dump"
TEMP_FILE="$BACKUP_DIR/.${BACKUP_PREFIX}_${TIMESTAMP}.dump.tmp.$$"

cleanup() {
  rm -f "$TEMP_FILE"
}

trap cleanup EXIT INT TERM

PG_DUMP_ARGS=(
  --format=custom \
  --schema="$PGSCHEMA" \
  --no-owner \
  --no-acl \
  --dbname="$PGDATABASE"
)

if [[ "$PG_DUMP_MODE" == "stream" ]]; then
  "${PG_DUMP_CMD[@]}" "${PG_DUMP_ARGS[@]}" > "$TEMP_FILE"
else
  "${PG_DUMP_CMD[@]}" "${PG_DUMP_ARGS[@]}" --file="$TEMP_FILE"
fi

chmod 600 "$TEMP_FILE"
mv "$TEMP_FILE" "$BACKUP_FILE"

find "$BACKUP_DIR" -type f -name "${BACKUP_PREFIX}_*.dump" -mtime +"$RETENTION_DAYS" -delete

printf '%s\n' "$BACKUP_FILE"
