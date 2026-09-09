#!/usr/bin/env bash
# Runs the REAL apt-get-update.sh prune against a fixture directory.
#
# The rule it checks is one regex, and getting it wrong is not survivable: too
# loose and a broken vendor mirror still fails the job, too tight and Ubuntu's
# own 24.04 ubuntu.sources gets deleted, leaving apt with no distribution at
# all. Neither shows up until a runner is already broken, so the shapes below
# are the ones a real ubuntu-latest image ships.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

sources="$work/sources.list.d"
mkdir -p "$sources" "$work/bin"

# Ubuntu 24.04 keeps the distribution itself HERE, in deb822 form, pointed at a
# cloud mirror rather than archive.ubuntu.com.
printf 'Types: deb\nURIs: http://azure.archive.ubuntu.com/ubuntu/\nSuites: noble\n' \
    > "$sources/ubuntu.sources"
printf 'deb http://security.ubuntu.com/ubuntu noble-security main\n' \
    > "$sources/security.list"
printf 'deb http://ports.ubuntu.com/ubuntu-ports noble main\n' \
    > "$sources/ports.list"
# The three that have actually broken jobs here.
printf 'deb [arch=amd64] https://dl.google.com/linux/chrome-stable/deb/ stable main\n' \
    > "$sources/google-chrome.list"
printf 'deb https://packages.microsoft.com/repos/azure-cli/ noble main\n' \
    > "$sources/azure-cli.list"
printf 'deb http://ppa.launchpadcontent.net/git-core/ppa/ubuntu noble main\n' \
    > "$sources/git-core.list"

# The script is all sudo and apt-get past the prune, and neither exists on a
# developer machine. Stubbed so the prune runs for real and the rest is inert;
# apt-get succeeds so the script exits 0 after one attempt.
cat > "$work/bin/sudo" <<'STUB'
#!/usr/bin/env bash
exec "$@"
STUB
cat > "$work/bin/apt-get" <<'STUB'
#!/usr/bin/env bash
exit 0
STUB
cat > "$work/bin/timeout" <<'STUB'
#!/usr/bin/env bash
shift
exec "$@"
STUB
cat > "$work/bin/tee" <<'STUB'
#!/usr/bin/env bash
cat > /dev/null
STUB
chmod +x "$work/bin/"*

PATH="$work/bin:$PATH" CN1_APT_SOURCES_DIR="$sources" \
    bash "$here/../apt-get-update.sh" >/dev/null 2>&1

status=0
for keep in ubuntu.sources security.list ports.list; do
    if [ ! -f "$sources/$keep" ]; then
        echo "FAIL: $keep was deleted; apt would be left without it" >&2
        status=1
    fi
done
for drop in google-chrome.list azure-cli.list git-core.list; do
    if [ -f "$sources/$drop" ]; then
        echo "FAIL: $drop survived; a bad index there still fails apt-get update" >&2
        status=1
    fi
done

if [ "$status" -eq 0 ]; then
    echo "apt-get-update-test: the prune keeps Ubuntu's sources and drops the rest."
fi
exit "$status"
