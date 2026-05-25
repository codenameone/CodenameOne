#!/usr/bin/env bash
# Wrapper invoked by launchd to drive Medium / DZone syndication on a
# schedule. Steps:
#   1. Fast-forward the local repo to pick up new queue entries the
#      blog-syndication.yml workflow committed since last run.
#   2. Invoke syndicate_browser.py — it self-skips when there's
#      nothing pending (no Chrome window opens), so the schedule can
#      fire daily without spamming.
#   3. If syndication-state.json changed, commit just that one file
#      and push so the next run (and CI) sees the updated state.
#
# All output is appended to scripts/website/logs/syndication-YYYY-MM-DD.log
# for after-the-fact debugging when the run fires while you're away.
#
# Re-run by hand any time:
#   bash scripts/website/run-syndication.sh

set -euo pipefail

REPO_ROOT="/Users/shai/dev/cn3/CodenameOne"
VENV_PYTHON="$REPO_ROOT/scripts/website/.venv/bin/python"
STATE_FILE="$REPO_ROOT/scripts/website/syndication-state.json"
LOG_DIR="$REPO_ROOT/scripts/website/logs"

mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/syndication-$(date +%Y-%m-%d).log"

exec >> "$LOG_FILE" 2>&1

echo
echo "=========================================================="
echo "  Syndication run: $(date '+%Y-%m-%d %H:%M:%S %Z')"
echo "=========================================================="

cd "$REPO_ROOT"

# 1. Pull latest queue from CI. Fast-forward only — if the local repo
# has diverged (uncommitted state changes from a prior failed run),
# bail loudly rather than auto-merging.
echo
echo ">>> git pull --ff-only origin master"
if ! git pull --ff-only origin master; then
    echo "!!! pull failed; investigate before next run."
    exit 1
fi

# 2. Run the browser-driven syndication. syndicate_browser.py exits 0
# silently (no Chrome opened) when no tasks are pending.
echo
echo ">>> syndicate_browser.py"
if ! "$VENV_PYTHON" -u "$REPO_ROOT/scripts/website/syndicate_browser.py"; then
    echo "!!! syndicate_browser.py exited non-zero; check log above."
    # Continue to commit any state changes that did happen before the
    # failure — partial success is better than losing the record.
fi

# 3. Commit + push state changes if the runner produced any.
echo
if git diff --quiet -- "$STATE_FILE"; then
    echo "No state changes — nothing to commit."
else
    echo ">>> committing state changes"
    git add "$STATE_FILE"
    git commit -m "ci: record browser syndication results" --no-verify
    if ! git push origin master; then
        echo "!!! push failed; commit is local. Push manually with:"
        echo "    cd $REPO_ROOT && git push origin master"
        exit 1
    fi
    echo "Pushed state changes."
fi

echo
echo "=== Done $(date '+%Y-%m-%d %H:%M:%S %Z') ==="
