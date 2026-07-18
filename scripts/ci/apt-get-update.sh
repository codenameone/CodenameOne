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
sudo apt-get update -o Acquire::ForceIPv4=true -o Acquire::Retries=5
