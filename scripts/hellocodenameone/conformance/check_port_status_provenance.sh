#!/usr/bin/env bash
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.

set -euo pipefail

# Refuse a port status report whose results were edited without a new run.
#
# These files are CI output. A branch never has a reason to change one -- not
# even the branch that registers a new test, because each port picks the test up
# on its next master run and the page shows the gap honestly until it does. The
# check exists because the previous contract *required* the edit, and the
# cheapest way to satisfy it was to type "pass" next to a test no port had run.
#
# Usage: check_port_status_provenance.sh <base-ref>

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
MANIFEST="${REPO_ROOT}/docs/website/data/port_status.json"
REPORT_PATH="docs/website/data/port_status_reports"
DATA_REF="refs/heads/port-status-data"
# Roughly a week of publications across all eleven ports, fetched in under a
# second. A snapshot older than that should be refreshed before review anyway,
# and the failure mode if it is not says exactly that.
DATA_DEPTH=200

base_ref="${1:-}"
if [ -z "${base_ref}" ]; then
  echo "Usage: $(basename "$0") <base-ref>" >&2
  exit 2
fi

if ! git -C "${REPO_ROOT}" rev-parse --verify --quiet "${base_ref}^{commit}" >/dev/null; then
  # A shallow clone routinely lacks the base commit. Fetching it is the caller's
  # job; without it there is nothing to compare against, and inventing a verdict
  # either way would be worse than saying so.
  echo "Base revision ${base_ref} is not available; skipping the provenance check." >&2
  exit 0
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

base_dir="${tmp_dir}/base"
published_dir="${tmp_dir}/published"
mkdir -p "${base_dir}" "${published_dir}"

# What the data branch has actually published turns this from "are these two
# strings different" into "did a run produce this report". Unreachable is not
# the same as forged, so a fetch failure drops the corroboration rather than
# failing the branch -- the inequality checks still apply either way.
have_published=1
if ! git -C "${REPO_ROOT}" fetch --quiet --no-tags --depth="${DATA_DEPTH}" origin "${DATA_REF}"; then
  echo "Port Status data branch is unavailable; checking provenance fields only." >&2
  have_published=0
fi

while IFS= read -r port; do
  if ! git -C "${REPO_ROOT}" show "${base_ref}:${REPORT_PATH}/${port}.json" \
      > "${base_dir}/${port}.json" 2>/dev/null; then
    # New port, or a base revision from before this file existed. Nothing to
    # compare against, so there is no edit to object to.
    rm -f "${base_dir}/${port}.json"
    continue
  fi
  if cmp -s "${base_dir}/${port}.json" "${REPO_ROOT}/${REPORT_PATH}/${port}.json"; then
    continue
  fi
  [ "${have_published}" -eq 1 ] || continue
  # Only for a report that changed: every version the branch has held, so a
  # refresh taken before the port ran again still matches an ancestor rather
  # than being rejected for not equalling today's tip.
  mkdir -p "${published_dir}/${port}"
  index=0
  while IFS= read -r revision; do
    git -C "${REPO_ROOT}" show "${revision}:ports/${port}.json" \
      > "${published_dir}/${port}/${index}.json" 2>/dev/null \
      || rm -f "${published_dir}/${port}/${index}.json"
    index=$((index + 1))
  done < <(git -C "${REPO_ROOT}" log --format=%H FETCH_HEAD -- "ports/${port}.json")
done < <(jq -r '.ports[].id' "${MANIFEST}")

if [ "${have_published}" -eq 1 ]; then
  python3 "${SCRIPT_DIR}/port_status.py" provenance \
    --base "${base_dir}" --published "${published_dir}"
else
  python3 "${SCRIPT_DIR}/port_status.py" provenance --base "${base_dir}"
fi
