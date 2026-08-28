#!/usr/bin/env bash
#
# Installs apt packages on a CI runner without letting a hung mirror eat the job.
#
# apt-get-update.sh already hardens the *update* (drops transiently broken Microsoft sources,
# forces IPv4, retries). The install itself had no bound, and on 2026-08-18 it hung: the
# archetype-smoke job sat in "Install xvfb" until the 30-minute job timeout cancelled it,
# having run nothing. A hung download should cost one attempt, not the whole build.
#
# Skips the apt round trip entirely when every requested package is already present, which is
# the common case for xvfb on GitHub's Ubuntu images.
#
# A 404 on a .deb gets one index refresh: that failure means the index names a version the
# mirror has already replaced, and no amount of retrying the same URL will find it.
#
# Usage: scripts/ci/apt-get-install.sh <packages...>
set -uo pipefail

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <packages...>" >&2
  exit 2
fi

missing=()
for pkg in "$@"; do
  if ! dpkg -s "$pkg" >/dev/null 2>&1; then
    missing+=("$pkg")
  fi
done
if [ "${#missing[@]}" -eq 0 ]; then
  echo "[apt-get-install] already installed: $*"
  exit 0
fi

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Refreshed once, outside the loop. It used to run on every attempt, and once the refresh
# itself became bounded-and-retried that multiplied: three attempts each spending up to the
# refresh's whole budget before trying to install. On 2026-08-19 a stalling mirror turned one
# call into eighteen minutes of index fetching, which is what actually exhausted the Windows
# cross-build's step budget -- the install never got a fair attempt. A stale index is not
# what makes an install fail twice in a row anyway; an unreachable archive is, and refetching
# it three times does not make it reachable.
bash "$here/apt-get-update.sh" || true
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
refreshed=0
for attempt in 1 2 3; do
  # A mirror that stops responding mid-download otherwise blocks until the job timeout.
  timeout 600 sudo apt-get install -y --no-install-recommends "${missing[@]}" 2>&1 | tee "$log"
  status=${PIPESTATUS[0]}
  if [ "$status" -eq 0 ]; then
    exit 0
  fi
  echo "[apt-get-install] attempt $attempt failed or timed out; retrying" >&2
  # One exception to "a stale index is not what makes an install fail twice",
  # which holds for an unreachable archive and not for this: a 404 on a .deb
  # means the index we fetched names a version the pool has already replaced --
  # a point release landing between the refresh and the install. Retrying the
  # same URL 404s forever, and apt says so itself ("maybe run apt-get update").
  # On 2026-08-25 that took out the arm64 Linux job on three openssl packages.
  #
  # Once, not per attempt, so the cost stays bounded: refetching an index three
  # times is what turned one call into eighteen minutes before.
  if [ "$refreshed" -eq 0 ] && grep -qE '404 +Not Found|Failed to fetch' "$log"; then
    echo "[apt-get-install] archive 404 -- the index is stale, refreshing it once" >&2
    bash "$here/apt-get-update.sh" || true
    refreshed=1
  fi
  sleep 15
done
echo "[apt-get-install] could not install: ${missing[*]}" >&2
exit 1
