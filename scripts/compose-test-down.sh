#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/docker-compose.test.yml"

if [[ "${1:-}" == "--volumes" ]]; then
  docker compose -f "${COMPOSE_FILE}" down -v
else
  docker compose -f "${COMPOSE_FILE}" down
fi
