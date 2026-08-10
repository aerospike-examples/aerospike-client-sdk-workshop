#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/rust-server"

export AEROSPIKE_PORT="${AEROSPIKE_PORT:-3000}"
export AEROSPIKE_CLIENT_PROFILE="${AEROSPIKE_CLIENT_PROFILE:-reference}"

exec cargo run --release "$@"
