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

# Dropped in as configuration rather than passed as options, because the install
# that follows this script in every caller is a separate apt invocation and used
# to inherit none of it. Three jobs on one branch hung here -- two of them until
# the six hour ceiling -- when a mirror accepted the connection and then stalled:
# without a timeout apt waits forever, and Retries never comes into play because
# nothing ever fails.
#
# ForceIPv4: the ARM runners intermittently lose IPv6 routes to ports.ubuntu.com
# mid-job, and apt's IPv6-first dial then times out every mirror.
sudo tee /etc/apt/apt.conf.d/99cn1-ci-timeouts >/dev/null <<'APTCONF'
Acquire::http::Timeout "30";
Acquire::https::Timeout "30";
Acquire::ftp::Timeout "30";
Acquire::Retries "5";
Acquire::ForceIPv4 "true";
APTCONF

# And a ceiling on the whole thing, so a hang that the per-connection timeouts
# somehow do not catch costs five minutes rather than the job. Inside sudo, so
# the signal reaches apt-get rather than the sudo wrapping it.
for attempt in 1 2 3; do
  if sudo timeout 300 apt-get update; then
    exit 0
  fi
  echo "apt-get-update: attempt ${attempt}/3 failed or timed out" >&2
  sleep $((attempt * 10))
done

echo "apt-get-update: apt-get update did not succeed in three attempts" >&2
exit 1
