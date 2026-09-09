#!/usr/bin/env bash
# Runs the REAL apt-get-update.sh prune against fixture directories.
#
# The first version of this test invented the contents of the runner's
# ubuntu.sources and asserted against the invention. The real file says
#
#     URIs: mirror+file:/etc/apt/apt-mirrors.txt
#
# and names no ubuntu.com host at all, so the rule deleted the distribution and
# every package on the runner became "Unable to locate" -- with this test
# passing the whole time. The fixtures below are copied from what the images
# really ship; do not "simplify" them back into a guess.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
script="$here/../apt-get-update.sh"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
status=0

mkdir -p "$work/bin"
# The script is all sudo and apt-get past the prune, and neither exists on a
# developer machine. Stubbed so the prune runs for real and the rest is inert.
printf '#!/usr/bin/env bash\nexec "$@"\n'   > "$work/bin/sudo"
printf '#!/usr/bin/env bash\nexit 0\n'      > "$work/bin/apt-get"
printf '#!/usr/bin/env bash\nshift\nexec "$@"\n' > "$work/bin/timeout"
printf '#!/usr/bin/env bash\ncat > /dev/null\n'  > "$work/bin/tee"
chmod +x "$work/bin/"*

run_prune() {   # run_prune <sources dir> <sources.list>
    PATH="$work/bin:$PATH" \
    CN1_APT_SOURCES_DIR="$1" CN1_APT_SOURCES_LIST="$2" \
        bash "$script" >/dev/null 2>&1
}

expect_kept() {
    if [ ! -f "$2/$1" ]; then
        echo "FAIL [$3] $1 was deleted; apt would lose it" >&2
        status=1
    fi
}
expect_dropped() {
    if [ -f "$2/$1" ]; then
        echo "FAIL [$3] $1 survived; a bad index there still fails apt-get update" >&2
        status=1
    fi
}

# ---- ubuntu-24.04, which is what ubuntu-latest is. The distribution lives in
# sources.list.d in deb822 form, behind a mirror indirection, and sources.list
# is an empty stub.
d="$work/noble"; mkdir -p "$d"
printf 'Types: deb\nURIs: mirror+file:/etc/apt/apt-mirrors.txt\nSuites: noble noble-updates noble-backports\nComponents: main restricted universe multiverse\n' \
    > "$d/ubuntu.sources"
printf 'Types: deb\nURIs: https://dl.google.com/linux/chrome-stable/deb/\nSuites: stable\nComponents: main\n' \
    > "$d/google-chrome.sources"
printf 'deb [arch=amd64] https://packages.microsoft.com/ubuntu/24.04/prod noble main\n' \
    > "$d/microsoft-prod.list"
printf '# Ubuntu sources have moved to /etc/apt/sources.list.d/ubuntu.sources\n' \
    > "$work/noble-sources.list"
run_prune "$d" "$work/noble-sources.list"
expect_kept   ubuntu.sources        "$d" noble
expect_dropped google-chrome.sources "$d" noble
expect_dropped microsoft-prod.list   "$d" noble

# ---- ubuntu-22.04, where the distribution is in sources.list instead and
# sources.list.d holds nothing but vendors. Nothing may be restored here: the
# invariant is satisfied from outside the directory.
d="$work/jammy"; mkdir -p "$d"
printf 'deb [arch=amd64] https://dl.google.com/linux/chrome-stable/deb/ stable main\n' \
    > "$d/google-chrome.list"
printf 'deb http://azure.archive.ubuntu.com/ubuntu/ jammy main restricted\n' \
    > "$work/jammy-sources.list"
run_prune "$d" "$work/jammy-sources.list"
expect_dropped google-chrome.list "$d" jammy

# ---- arm64 runners point at ports.ubuntu.com.
d="$work/ports"; mkdir -p "$d"
printf 'deb http://ports.ubuntu.com/ubuntu-ports noble main\n' > "$d/ports.list"
printf 'deb https://dl.google.com/linux/chrome-stable/deb/ stable main\n' > "$d/google-chrome.list"
run_prune "$d" /nonexistent
expect_kept    ports.list         "$d" ports
expect_dropped google-chrome.list "$d" ports

# ---- The invariant itself. A directory whose ONLY entry looks third-party to
# the rule -- as ubuntu.sources did when this was first written -- must come
# back rather than leave apt with nothing.
d="$work/invariant"; mkdir -p "$d"
printf 'Types: deb\nURIs: file:/some/local/mirror/\nSuites: noble\n' > "$d/only-source.sources"
run_prune "$d" /nonexistent
if [ ! -f "$d/only-source.sources" ]; then
    echo "FAIL [invariant] the last source was deleted; apt is left with no distribution" >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    echo "apt-get-update-test: vendors dropped, the distribution kept on every layout."
fi
exit "$status"
