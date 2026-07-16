#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/website"

if [[ ! -d node_modules ]]; then
  echo "Installing website dependencies (first run only)..."
  npm install
fi

if lsof -i :5173 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "UI dev server already running at http://localhost:5173"
  echo "If the page looks broken, stop the old process and restart:"
  echo "  lsof -i :5173 -sTCP:LISTEN"
  echo "  kill <PID>"
  exit 0
fi

echo "Starting UI dev server at http://localhost:5173"
echo "Make sure a backend is running on http://localhost:8080 (e.g. ./scripts/run-rust.sh)"
exec npm run dev
