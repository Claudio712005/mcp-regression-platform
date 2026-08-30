#!/usr/bin/env bash
set -euo pipefail

PLATFORM_URL="${PLATFORM_URL:-http://localhost:8080}"
DEMO_USER="${DEMO_USER:-architect}"
DEMO_PASSWORD="${DEMO_PASSWORD:-${PLATFORM_ARCHITECT_PASSWORD:-architect-password}}"
DEFAULT_BFF="fintech-bff-account"

usage() {
  cat <<'USAGE'
Usage:
  ./demo.sh regression [bff]        Run the readiness report for a BFF
  ./demo.sh scenario <name>         Switch the simulated environment scenario
  ./demo.sh scenarios               List the available scenarios
  ./demo.sh mcp                     List the MCP tools exposed over Streamable HTTP
  ./demo.sh token [user]            Print a bearer token

Scenarios: healthy, service-down, contract-mismatch, authentication-failure, high-latency, timeout
USAGE
}

require() {
  command -v "$1" >/dev/null 2>&1 || { echo "missing required command: $1" >&2; exit 1; }
}

token() {
  local user="${1:-$DEMO_USER}"
  local password="$DEMO_PASSWORD"
  case "$user" in
    dev) password="${PLATFORM_DEV_PASSWORD:-dev-password}" ;;
    qa) password="${PLATFORM_QA_PASSWORD:-qa-password}" ;;
    architect) password="${PLATFORM_ARCHITECT_PASSWORD:-architect-password}" ;;
  esac
  curl -sS -X POST "$PLATFORM_URL/auth/token" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$user\",\"password\":\"$password\"}" \
    | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

regression() {
  local bff="${1:-$DEFAULT_BFF}"
  local jwt
  jwt="$(token "$DEMO_USER")"
  [ -n "$jwt" ] || { echo "authentication failed" >&2; exit 1; }
  curl -sS "$PLATFORM_URL/api/regression/$bff/readiness" \
    -H "Authorization: Bearer $jwt" \
    -H 'Accept: text/plain'
  echo
}

scenario() {
  local name="$1"
  local jwt
  jwt="$(token architect)"
  [ -n "$jwt" ] || { echo "authentication failed" >&2; exit 1; }
  curl -sS -X PUT "$PLATFORM_URL/internal/demo/scenario/$name" \
    -H "Authorization: Bearer $jwt"
  echo
}

scenarios() {
  local jwt
  jwt="$(token architect)"
  curl -sS "$PLATFORM_URL/internal/demo/scenario" -H "Authorization: Bearer $jwt"
  echo
}

mcp_tools() {
  local jwt
  jwt="$(token "$DEMO_USER")"
  local session
  session="$(curl -sS -D - -o /dev/null -X POST "$PLATFORM_URL/mcp" \
    -H "Authorization: Bearer $jwt" \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json, text/event-stream' \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"demo-sh","version":"1.0"}}}' \
    | tr -d '\r' | sed -n 's/^[Mm]cp-[Ss]ession-[Ii]d: //p')"
  [ -n "$session" ] || { echo "MCP initialize failed" >&2; exit 1; }
  curl -sS -X POST "$PLATFORM_URL/mcp" \
    -H "Authorization: Bearer $jwt" \
    -H "Mcp-Session-Id: $session" \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json, text/event-stream' \
    -d '{"jsonrpc":"2.0","method":"notifications/initialized"}' >/dev/null
  curl -sS -X POST "$PLATFORM_URL/mcp" \
    -H "Authorization: Bearer $jwt" \
    -H "Mcp-Session-Id: $session" \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json, text/event-stream' \
    -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
    | tr ',' '\n' | sed -n 's/.*"name":"\([a-z_]*\)".*/  - \1/p'
}

require curl

case "${1:-}" in
  regression) shift; regression "${1:-}" ;;
  scenario) shift; [ $# -ge 1 ] || { usage; exit 1; }; scenario "$1" ;;
  scenarios) scenarios ;;
  mcp) mcp_tools ;;
  token) shift; token "${1:-$DEMO_USER}" ;;
  *) usage ;;
esac
