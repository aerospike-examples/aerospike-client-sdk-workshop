#!/usr/bin/env bash
# Run Aerospike Query Language (aql) without installing Aerospike Tools locally.
# aql ships in the aerospike/aerospike-tools image, not in aerospike-server.
#
# Usage:
#   ./scripts/aql.sh -c "SELECT * FROM test.products LIMIT 3"
#   ./scripts/aql.sh    # interactive session
set -euo pipefail

AEROSPIKE_HOST="${AEROSPIKE_HOST:-host.docker.internal}"
AEROSPIKE_PORT="${AEROSPIKE_PORT:-3000}"

DOCKER_ARGS=(--rm --add-host=host.docker.internal:host-gateway)

# Interactive mode needs a TTY; one-shot -c commands do not.
if [[ $# -eq 0 ]]; then
  DOCKER_ARGS+=(-it)
fi

docker run "${DOCKER_ARGS[@]}" aerospike/aerospike-tools \
  aql -h "$AEROSPIKE_HOST" -p "$AEROSPIKE_PORT" --no-config-file "$@"
