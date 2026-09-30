#!/usr/bin/env bash
# Refuses to publish a Codename One release the release gate has not verified.
# Run by .github/workflows/release.yml before anything is built.
#
# The gate lives in BuildDaemon (release-gate/, deploy/RELEASE-CHECKLIST.md):
# run_gate.py builds every target from every way users start a project, walks the
# new-user funnel, and pushes the annotated tag release-gate/<sha> here. The
# verify_gate.py and matrix.json beside this script are byte-identical copies of
# BuildDaemon's -- that repository is private -- and run_gate.py refuses to run while
# they differ.
#
# Everything is re-derived: the commit is the one being tagged, the time floor is its
# commit time, and each build is re-read from BuildCloud. The tag message contributes
# build ids only. There is no override: a release that cannot pass is not released.
#
# env: GATE_ACCOUNT, RELEASE_GATE_MATRIX_SHA256, RELEASE_GATE_ADMIN_TOKEN, GITHUB_REF_NAME
set -euo pipefail
fail() { echo "::error::release gate: $*"; exit 1; }

[ -n "${GATE_ACCOUNT:-}" ] || fail "repo variable RELEASE_GATE_ACCOUNT is not set"
[ -n "${RELEASE_GATE_MATRIX_SHA256:-}" ] || fail "repo variable RELEASE_GATE_MATRIX_SHA256 is not set"
[ -n "${RELEASE_GATE_ADMIN_TOKEN:-}" ] || fail "secret RELEASE_GATE_ADMIN_TOKEN is not set"

HERE="$(cd "$(dirname "$0")" && pwd)"
COMMIT="$(git rev-parse HEAD)"
TAG="release-gate/$COMMIT"
git fetch --quiet --force origin 'refs/tags/release-gate/*:refs/tags/release-gate/*' || true
git rev-parse -q --verify "refs/tags/$TAG" >/dev/null \
  || fail "no $TAG: run BuildDaemon release-gate/run_gate.py --kind cn1 --subject $COMMIT before tagging"
[ "$(git cat-file -t "refs/tags/$TAG")" = "tag" ] || fail "$TAG must be an annotated tag"

WORK="$(mktemp -d)"
git tag -l --format='%(contents:body)' "$TAG" > "$WORK/record.json"
jq -e . "$WORK/record.json" >/dev/null 2>&1 || fail "$TAG does not carry a gate record"

# The two most recent releases before this one, for the week-over-week funnel check.
VERSIONS="$(git tag -l '[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | grep -Fxv "${GITHUB_REF_NAME:-}" | head -2 | paste -sd, -)"
[ "$(tr ',' '\n' <<<"$VERSIONS" | grep -c .)" = 2 ] || fail "cannot find the two previous releases"

# The artifact-size baseline is the PREVIOUS RELEASE's own gate record -- the commit
# its version tag points at -- not the newest gate tag, which may belong to a candidate
# that was gated and then abandoned. A release from before the gate has none, and then
# there is no baseline to compare against.
PREV_ARGS=()
PREV_VERSION="${VERSIONS%%,*}"
PREV_COMMIT="$(git rev-parse -q --verify "refs/tags/$PREV_VERSION^{commit}" || true)"
if [ -n "$PREV_COMMIT" ] && git rev-parse -q --verify "refs/tags/release-gate/$PREV_COMMIT" >/dev/null; then
  git tag -l --format='%(contents:body)' "release-gate/$PREV_COMMIT" > "$WORK/prev.json"
  if jq -e . "$WORK/prev.json" >/dev/null 2>&1; then
    PREV_ARGS=(--previous-record "$WORK/prev.json")
  fi
fi

NOT_BEFORE="$(( $(git show -s --format=%ct "$COMMIT") * 1000 ))"
python3 "$HERE/verify_gate.py" \
  --record "$WORK/record.json" --matrix "$HERE/matrix.json" \
  --matrix-sha256 "$RELEASE_GATE_MATRIX_SHA256" \
  --kind cn1 --subject "$COMMIT" --commit "$COMMIT" --phase 1 \
  --not-before "$NOT_BEFORE" --gate-account "$GATE_ACCOUNT" \
  --funnel-versions "$VERSIONS" "${PREV_ARGS[@]}"
