#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/python-server"

pip install -e . >/dev/null

export AEROSPIKE_PORT="${AEROSPIKE_PORT:-3000}"
export AEROSPIKE_CLIENT_PROFILE="${AEROSPIKE_CLIENT_PROFILE:-old-client}"

exec uvicorn aerospikeworkshop.main:app --host 0.0.0.0 --port "${SERVER_PORT:-8080}" "$@"
