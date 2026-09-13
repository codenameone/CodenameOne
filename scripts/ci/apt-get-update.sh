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
# took out seven jobs on one branch with a Hash Sum mismatch that outlasted all
# three retries below, and it is the runner image's repo, not ours -- the only
# browser any workflow here uses is the chromium Playwright downloads itself.
#
# So the rule is by ORIGIN, and it is a KEEP-list: on 24.04 Ubuntu's own archive
# moved INTO this directory as ubuntu.sources (deb822), and deleting that leaves
# apt with no distribution at all. That is not hypothetical -- the first version
# of this rule did exactly that, and every package went "Unable to locate", a
# far worse failure than the vendor mirror it was fixing.
#
# The keep test cannot be "names an ubuntu.com host", because the runner's
# ubuntu.sources does not name one: it says
#
#     URIs: mirror+file:/etc/apt/apt-mirrors.txt
#
# and the hosts live in that other file. Any mirror indirection is therefore
# kept too, and so is anything named for the distribution itself.
apt_source_is_ubuntus() {
  # By name only for the distribution's OWN files. Deliberately not sources.list:
  # 24.04 ships that as an empty stub, so keying on its existence would make the
  # invariant below true whatever the prune did -- a check nothing can fail.
  case "$(basename "$1")" in
    ubuntu.sources|ubuntu.list) return 0 ;;
  esac
  grep -qE '(ubuntu\.com|mirror\+file:|^[[:space:]]*URIs:[[:space:]]*mirror:|mirror://)' "$1"
}

# Whether apt can still see a distribution anywhere: 22.04 keeps it in
# sources.list, 24.04 in sources.list.d/ubuntu.sources.
apt_has_a_distribution() {
  if [ -f "$APT_SOURCES_LIST" ] && apt_source_is_ubuntus "$APT_SOURCES_LIST"; then
    return 0
  fi
  for kept in "$APT_SOURCES_DIR"/*; do
    if [ -f "$kept" ] && apt_source_is_ubuntus "$kept"; then
      return 0
    fi
  done
  return 1
}

APT_SOURCES_DIR="${CN1_APT_SOURCES_DIR:-/etc/apt/sources.list.d}"
APT_SOURCES_LIST="${CN1_APT_SOURCES_LIST:-/etc/apt/sources.list}"
if [ -d "$APT_SOURCES_DIR" ]; then
  # Backed up first, because the whole point of the paragraph above is that
  # getting this rule wrong is unrecoverable in-job. Anything dropped can be put
  # back by the invariant below.
  apt_backup="$(mktemp -d)"
  cp -a "$APT_SOURCES_DIR"/. "$apt_backup"/ 2>/dev/null || true
  for source in "$APT_SOURCES_DIR"/*; do
    [ -f "$source" ] || continue
    if apt_source_is_ubuntus "$source"; then
      continue
    fi
    # The URI is echoed as well as the name so that a source dropped by mistake
    # says WHY in the log, rather than leaving the next person to guess at the
    # file's contents the way this rule's first version was written.
    echo "apt-get-update: dropping third-party source $source ($(grep -hoE '(https?|mirror[^[:space:]]*)://[^[:space:]]+' "$source" | head -1))" >&2
    sudo rm -f "$source" || true
  done

  # The invariant: a prune that leaves apt with no distribution is WRONG, and no
  # regex is trusted enough to skip checking. Restoring costs a vendor mirror
  # outage; not restoring costs every package on the runner.
  if ! apt_has_a_distribution; then
    echo "apt-get-update: the prune left no distribution source; restoring all of them" >&2
    sudo cp -a "$apt_backup"/. "$APT_SOURCES_DIR"/ 2>/dev/null || true
  fi
  rm -rf "$apt_backup"
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
