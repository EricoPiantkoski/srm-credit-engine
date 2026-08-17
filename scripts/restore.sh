#!/usr/bin/env bash
set -Eeuo pipefail

PGDATABASE="${PGDATABASE:-srm_credit}"
PGUSER="${PGUSER:-postgres}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5656}"
PGSCHEMA="${PGSCHEMA:-public}"
PG_RESTORE_BIN="${PG_RESTORE_BIN:-pg_restore}"
PSQL_BIN="${PSQL_BIN:-psql}"
USE_DOCKER="${USE_DOCKER:-0}"

if (( $# != 1 )); then
  printf 'Usage: %s <dump-file>\n' "$0" >&2
  exit 64
fi

DUMP_FILE="$1"

if [[ ! -f "$DUMP_FILE" ]]; then
  printf '%s\n' "dump file not found: $DUMP_FILE" >&2
  exit 1
fi

if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  printf '%s\n' "restoration is destructive; set CONFIRM_RESTORE=YES to continue" >&2
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
  PG_RESTORE_CMD=(docker compose exec -T -e "PGHOST=localhost" -e "PGPORT=5432" -e "PGDATABASE=$PGDATABASE" -e "PGUSER=$PGUSER" -e "PGPASSWORD=${PGPASSWORD-}" postgres pg_restore)
  PSQL_CMD=(docker compose exec -T -e "PGHOST=localhost" -e "PGPORT=5432" -e "PGDATABASE=$PGDATABASE" -e "PGUSER=$PGUSER" -e "PGPASSWORD=${PGPASSWORD-}" postgres psql)
  PG_RESTORE_MODE="stream"
else
  if ! command -v "$PG_RESTORE_BIN" >/dev/null 2>&1; then
    printf '%s\n' "$PG_RESTORE_BIN was not found; install PostgreSQL client tools or use USE_DOCKER=1" >&2
    exit 1
  fi
  if ! command -v "$PSQL_BIN" >/dev/null 2>&1; then
    printf '%s\n' "$PSQL_BIN was not found; install PostgreSQL client tools or use USE_DOCKER=1" >&2
    exit 1
  fi
  PG_RESTORE_CMD=("$PG_RESTORE_BIN" --host="$PGHOST" --port="$PGPORT" --username="$PGUSER")
  PSQL_CMD=("$PSQL_BIN" --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE")
  PG_RESTORE_MODE="file"
fi

PG_RESTORE_ARGS=(
  --clean \
  --if-exists \
  --no-owner \
  --no-acl \
  --exit-on-error \
  --single-transaction \
  --schema="$PGSCHEMA" \
  --dbname="$PGDATABASE"
)

if [[ "$PG_RESTORE_MODE" == "stream" ]]; then
  "${PG_RESTORE_CMD[@]}" "${PG_RESTORE_ARGS[@]}" < "$DUMP_FILE"
else
  "${PG_RESTORE_CMD[@]}" "${PG_RESTORE_ARGS[@]}" "$DUMP_FILE"
fi

for table in taxa_cambio recebivel liquidacao audit_log usuario; do
  count="$("${PSQL_CMD[@]}" -v ON_ERROR_STOP=1 -Atqc "SELECT count(*) FROM \"$PGSCHEMA\".\"$table\";")"
  printf '%s=%s\n' "$table" "$count"
done

printf '%s\n' "restore completed: $DUMP_FILE"
