#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/deploy/docker-compose.test.yml"

if [[ "${1:-}" == "--build" ]]; then
  docker compose -f "${COMPOSE_FILE}" up -d --build
else
  docker compose -f "${COMPOSE_FILE}" up -d
fi

docker compose -f "${COMPOSE_FILE}" ps
