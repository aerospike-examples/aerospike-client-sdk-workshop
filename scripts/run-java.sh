#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/spring-server"

mvn clean package -DskipTests -q
exec java -jar target/aerospike-client-sdk-workshop-1.0.0.jar \
  --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-old-client}" \
  --aerospike.port="${AEROSPIKE_PORT:-3000}" \
  "$@"
