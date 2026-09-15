#!/usr/bin/env bash
###
# Validate every GitHub Actions workflow with actionlint.
#
# This exists because of a specific failure. A job-level `if:` that referenced the
# `matrix` context made GitHub reject the WHOLE file, and an invalid workflow file
# produces a failed check run on every push to every branch that contains it --
# regardless of that workflow's own triggers, and regardless of what the push touched.
# Two unrelated pull requests went red for hours.
#
# What made it expensive is that nothing local objected. The file is valid YAML, so
# `yaml.safe_load` parses it, `yamllint` passes it, and an editor shows nothing. The
# error surfaces only from GitHub's own expression parser, which is what actionlint
# reimplements:
#
#     Unrecognized named-value: 'matrix'
#
# So: YAML parsing is not workflow validation. This is.
#
# Usage:
#   scripts/check-workflows.sh              # every workflow
#   scripts/check-workflows.sh FILE ...     # just these
#
# ACTIONLINT_VERSION pins the tool. CI and a developer machine must run the same one,
# or a rule added upstream fails a build that passed locally an hour earlier.
###
set -euo pipefail

ACTIONLINT_VERSION="${ACTIONLINT_VERSION:-1.7.7}"

log() { echo "[check-workflows] $1" >&2; }

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$REPO_ROOT"

# A cached download rather than a package manager: actionlint is a single static binary,
# it is not in the Ubuntu or Homebrew default sets at a pinned version, and a CI job that
# installs a package manager first is slower than the check.
CACHE_DIR="${ACTIONLINT_CACHE:-${TMPDIR:-/tmp}/cn1-actionlint-$ACTIONLINT_VERSION}"
BIN="$CACHE_DIR/actionlint"

if [ ! -x "$BIN" ]; then
  case "$(uname -s)" in
    Darwin) os=darwin ;;
    Linux)  os=linux ;;
    *)      log "FAILED: unsupported host $(uname -s)"; exit 2 ;;
  esac
  case "$(uname -m)" in
    arm64|aarch64) arch=arm64 ;;
    x86_64|amd64)  arch=amd64 ;;
    *) log "FAILED: unsupported architecture $(uname -m)"; exit 2 ;;
  esac
  url="https://github.com/rhysd/actionlint/releases/download/v${ACTIONLINT_VERSION}/actionlint_${ACTIONLINT_VERSION}_${os}_${arch}.tar.gz"
  mkdir -p "$CACHE_DIR"
  log "Downloading actionlint ${ACTIONLINT_VERSION} for ${os}/${arch}"
  if ! curl -fsSL "$url" | tar -xz -C "$CACHE_DIR" actionlint; then
    log "FAILED: could not download actionlint from $url"
    exit 2
  fi
  chmod +x "$BIN"
fi

if [ "$#" -gt 0 ]; then
  TARGETS=("$@")
else
  # Explicit list rather than letting actionlint discover them, so an empty or moved
  # directory fails loudly instead of reporting success over nothing.
  # while-read rather than mapfile: mapfile is bash 4, and macOS ships bash 3.2.
  TARGETS=()
  while IFS= read -r f; do
    TARGETS+=("$f")
  done < <(find .github/workflows -maxdepth 1 \( -name '*.yml' -o -name '*.yaml' \) | sort)
  if [ "${#TARGETS[@]}" -eq 0 ]; then
    log "FAILED: no workflow files found under .github/workflows"
    exit 2
  fi
fi

log "Checking ${#TARGETS[@]} workflow file(s) with actionlint ${ACTIONLINT_VERSION}"

# shellcheck disable=SC2086
# -shellcheck= and -pyflakes= disable those sub-linters: the shell inside `run:` blocks is
# covered by the repo's own scripts and review, and enabling them here would turn this into
# a style gate for every existing workflow rather than a validity gate for all of them.
# -ignore drops ONE advisory: actionlint warns that a few third-party actions pinned at v1
# are older than the runner supports. That is worth knowing and it is not this gate's job --
# bumping someone else's action is a separate change, and a gate that fails on it would be
# ignored rather than fixed. Everything else is absolute: with this ignore and
# .github/actionlint.yaml in place the tree reports ZERO findings today, so any new one is
# a real regression rather than noise to be triaged.
if ! "$BIN" -shellcheck= -pyflakes= \
    -ignore 'the runner of "[^"]+" action is too old' \
    "${TARGETS[@]}"; then
  log "FAILED: actionlint reported problems (see above)."
  log "A workflow file GitHub rejects fails EVERY push on EVERY branch that contains it,"
  log "with no log -- so this is a hard gate rather than an advisory one."
  exit 1
fi

log "OK: ${#TARGETS[@]} workflow file(s) are valid."
