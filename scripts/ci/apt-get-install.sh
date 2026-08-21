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
for attempt in 1 2 3; do
  # A mirror that stops responding mid-download otherwise blocks until the job timeout.
  if timeout 600 sudo apt-get install -y --no-install-recommends "${missing[@]}"; then
    exit 0
  fi
  echo "[apt-get-install] attempt $attempt failed or timed out; retrying" >&2
  sleep 15
done
echo "[apt-get-install] could not install: ${missing[*]}" >&2
exit 1
