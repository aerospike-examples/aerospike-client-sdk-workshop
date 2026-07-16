#!/usr/bin/env bash
# Run asinfo against the workshop Aerospike container (no local tools install required).
set -euo pipefail

CONTAINER="${AEROSPIKE_CONTAINER:-aerospike-workshop}"
AEROSPIKE_PORT="${AEROSPIKE_PORT:-3000}"

if ! docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null | grep -q true; then
  echo "Container '$CONTAINER' is not running." >&2
  echo "Check status: docker compose ps -a" >&2
  echo "View logs:    docker logs $CONTAINER" >&2
  exit 1
fi

docker exec "$CONTAINER" asinfo -p "$AEROSPIKE_PORT" "$@"
