#!/usr/bin/env bash
set -euo pipefail

# GitHub-hosted Ubuntu runners ship third-party apt sources that we never
# install from, and apt-get update fails as a WHOLE when any one of them serves
# a bad index -- it prints "they have been ignored, or old ones used instead"
# and then exits non-zero anyway. So a broken vendor mirror stops us installing
# xvfb or clang from Ubuntu's own archive, which was working the entire time.
#
# This started as a Microsoft-only rule (azure-cli / packages.microsoft.com).
# Naming vendors one at a time does not converge: Google's chrome-stable repo
# took out five jobs on one branch with a Hash Sum mismatch that outlasted all
# three retries below, and it is the runner image's repo, not ours -- the only
# browser any workflow here uses is the chromium Playwright downloads itself.
#
# So the rule is by ORIGIN rather than by name: a source list survives only if
# it points at an Ubuntu host. That has to be a keep-list rather than a
# drop-list, because on 24.04 Ubuntu's own archive moved INTO this directory as
# ubuntu.sources (deb822), and deleting it would leave apt with no distro at
# all -- a much worse failure than the one being fixed.
# The directory is a variable ONLY so the test beside this script can point the
# rule at a fixture; nothing in CI sets it.
APT_SOURCES_DIR="${CN1_APT_SOURCES_DIR:-/etc/apt/sources.list.d}"
if [ -d "$APT_SOURCES_DIR" ]; then
  for source in "$APT_SOURCES_DIR"/*; do
    [ -f "$source" ] || continue
    if grep -qE '(^|[/.])(archive|security|ports|azure\.archive)\.ubuntu\.com' "$source"; then
      continue
    fi
    echo "apt-get-update: dropping third-party source $source" >&2
    sudo rm -f "$source" || true
  done
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
