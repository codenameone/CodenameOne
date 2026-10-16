# shellcheck shell=bash
#
# Start and stop a local `wrangler pages dev` server so that the server a check
# talks to is provably the one it started, and is gone when the check ends.
#
# Source this file; do not execute it.
#
#   . scripts/website/pages-dev-server.sh
#   trap pages_dev_stop EXIT
#   pages_dev_start docs/website/public /tmp/pages-dev.log "<port, or empty>" [extra wrangler args]
#   curl "${PAGES_DEV_URL}/..."
#
# Why this exists. `npx wrangler pages dev ... & kill $!` kills npx and nothing
# else: wrangler's node process, esbuild and the two workerd processes it spawns
# (the proxy that owns --port, and the user worker on an ephemeral port) are
# left behind as orphans, sometimes all of them, sometimes only some. build.sh
# ran check-javadoc-urls.sh on 8788 that way, and the redirects step of
# website-docs.yml later started its own server on the same 8788. Its readiness
# curl was answered by build.sh's leftover workerd, test_redirects.py passed
# against that stranger before the new wrangler had even written its first log
# line, the new proxy workerd then died on "Address already in use", and when
# the leftover went away a few seconds later test_search.mjs got
# ERR_CONNECTION_REFUSED (run 38724184374). The runner's own cleanup listed
# the leftover workerd processes as orphans at the end of that job, and the
# green runs list orphaned wrangler node and esbuild processes from the same
# step.
#
# So:
#   - the server runs in its own process group, and pages_dev_stop signals the
#     whole group and waits for it to be gone, which takes workerd with it;
#   - with no port given, the OS picks a free one, so two checks never meet;
#   - a port that already answers is refused outright: a check that passes
#     against somebody else's server has tested nothing;
#   - readiness requires our process to still be alive, so a wrangler that
#     failed to bind can never be reported ready by whatever holds the port.

PAGES_DEV_PGID=""
PAGES_DEV_PORT=""
PAGES_DEV_URL=""
PAGES_DEV_LOG=""

pages_dev_free_port() {
  python3 -c 'import socket
s = socket.socket()
s.bind(("127.0.0.1", 0))
print(s.getsockname()[1])
s.close()'
}

pages_dev_start() {
  local dir="$1" log="$2" port="${3:-}"
  if [ $# -ge 3 ]; then shift 3; else shift $#; fi
  if [ -z "${port}" ]; then
    port="$(pages_dev_free_port)"
  fi
  PAGES_DEV_PORT="${port}"
  PAGES_DEV_URL="http://127.0.0.1:${port}"
  PAGES_DEV_LOG="${log}"

  if curl -s -o /dev/null --max-time 2 "${PAGES_DEV_URL}/" 2>/dev/null; then
    echo "pages-dev: ${PAGES_DEV_URL} already answers before wrangler was started;" \
      "refusing to run checks against a server this script did not start." >&2
    return 1
  fi

  # Monitor mode puts the background job in a process group of its own whose
  # id is the job's pid, so the whole tree can be signalled at once. stdin is
  # /dev/null because a job outside the foreground group must not read a tty.
  local had_monitor=0
  case "$-" in *m*) had_monitor=1 ;; esac
  set -m
  npx --yes "wrangler@${WRANGLER_VERSION:-4.129.0}" pages dev "${dir}" \
    --port "${port}" --ip 127.0.0.1 "$@" </dev/null >"${log}" 2>&1 &
  PAGES_DEV_PGID=$!
  [ "${had_monitor}" -eq 1 ] || set +m

  local _
  for _ in $(seq 1 120); do
    if ! kill -0 "${PAGES_DEV_PGID}" 2>/dev/null; then
      echo "pages-dev: wrangler exited before it was ready; log:" >&2
      tail -n 200 "${log}" >&2 || true
      return 1
    fi
    if curl -fsS -o /dev/null --max-time 5 "${PAGES_DEV_URL}/" 2>/dev/null; then
      # Answering is not enough on its own: a wrangler whose proxy lost the
      # bind keeps node alive for a moment, so look for the failure too.
      if grep -q 'Address already in use' "${log}" 2>/dev/null; then
        echo "pages-dev: wrangler could not bind ${PAGES_DEV_URL}; log:" >&2
        tail -n 200 "${log}" >&2 || true
        return 1
      fi
      return 0
    fi
    sleep 1
  done
  echo "pages-dev: wrangler pages dev did not become ready on ${PAGES_DEV_URL}; log:" >&2
  tail -n 200 "${log}" >&2 || true
  return 1
}

pages_dev_stop() {
  local pgid="${PAGES_DEV_PGID}"
  [ -n "${pgid}" ] || return 0
  PAGES_DEV_PGID=""
  kill -TERM -- "-${pgid}" 2>/dev/null || return 0
  local _
  for _ in $(seq 1 50); do
    kill -0 -- "-${pgid}" 2>/dev/null || return 0
    sleep 0.2
  done
  kill -KILL -- "-${pgid}" 2>/dev/null || true
  return 0
}
