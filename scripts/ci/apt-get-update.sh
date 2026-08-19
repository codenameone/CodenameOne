#!/usr/bin/env bash
set -euo pipefail

# GitHub-hosted Ubuntu runners occasionally ship transiently broken Microsoft
# apt sources (azure-cli / packages.microsoft.com). These are not needed by our
# package installs, but a bad InRelease from them makes apt-get update fail
# before we can install normal Ubuntu packages such as xvfb or clang.
if [ -d /etc/apt/sources.list.d ]; then
  sudo find /etc/apt/sources.list.d -maxdepth 1 -type f \
    \( -iname '*microsoft*' -o -iname '*azure-cli*' \) \
    -print -delete || true
fi

# ForceIPv4: the ARM runners intermittently lose IPv6 routes to ports.ubuntu.com
# mid-job, and apt's IPv6-first dial then times out every mirror.
#
# Bounded, because Acquire::Retries only covers a request that *fails*. A mirror that
# accepts the connection and then stops sending never fails, so apt waits on it forever:
# on 2026-08-19 the Windows cross-build sat here for 59 minutes after "Get:5 noble-security
# InRelease" and was cancelled having installed nothing, which failed the screenshot job
# downstream for a missing artifact. The per-request Timeout options bound each transfer and
# the outer `timeout` bounds the whole run in case they do not.
#
# Two attempts rather than one: a genuinely broken source fails the same way twice and costs
# only the second attempt, while a stalled mirror is usually a different mirror next time.
apt_update() {
  timeout 300 sudo apt-get update \
    -o Acquire::ForceIPv4=true \
    -o Acquire::Retries=5 \
    -o Acquire::http::Timeout=30 \
    -o Acquire::https::Timeout=30
}

if ! apt_update; then
  echo "apt-get-update: first attempt failed or stalled; retrying once" >&2
  sleep 15
  apt_update
fi
