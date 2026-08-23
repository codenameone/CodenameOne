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

while IFS= read -r port; do
  git -C "${REPO_ROOT}" show "${base_ref}:${REPORT_PATH}/${port}.json" \
    > "${tmp_dir}/${port}.json" 2>/dev/null \
    || rm -f "${tmp_dir}/${port}.json"
done < <(jq -r '.ports[].id' "${MANIFEST}")

python3 "${SCRIPT_DIR}/port_status.py" provenance --base "${tmp_dir}"
