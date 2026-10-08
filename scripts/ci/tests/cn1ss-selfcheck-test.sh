#!/usr/bin/env bash
# Exercise the actual startup probe under the strict shell used by platform CI.
set -euo pipefail
source "$(cd "$(dirname "$0")/../.." && pwd)/lib/cn1ss.sh"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
health_status=0
curl() {
  case "${*: -1}" in
    */api/health) return "$health_status" ;;
    *) cat "$work/response" ;;
  esac
}

valid_response() {
  printf 'HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=\r\n\r\n'
}

valid_response > "$work/response"
cn1ss_ws_selfcheck 8765
# curl -i also includes bytes following the upgrade. grep -q can close its pipe
# before printf finishes writing them, making pipefail reject a valid handshake.
valid_response > "$work/response"
printf '%262144s' '' >> "$work/response"
if ! cn1ss_ws_selfcheck 8765 > "$work/log" 2>&1; then
  echo 'FAIL: valid handshake with trailing data was rejected' >&2
  exit 1
fi
printf 'HTTP/1.1 101 Switching Protocols\r\nSec-WebSocket-Accept: wrong\r\n\r\n' > "$work/response"
if cn1ss_ws_selfcheck 8765 > "$work/log" 2>&1; then
  echo 'FAIL: incorrect accept value was accepted' >&2
  exit 1
fi
: > "$work/response"
if cn1ss_ws_selfcheck 8765 > "$work/log" 2>&1; then
  echo 'FAIL: absent response was accepted' >&2
  exit 1
fi
valid_response > "$work/response"
health_status=22
if cn1ss_ws_selfcheck 8765 > "$work/log" 2>&1; then
  echo 'FAIL: failed health endpoint was accepted' >&2
  exit 1
fi
echo 'CN1SS startup probe tests passed'
