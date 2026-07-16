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

# Website CI resolves the durable per-port reports before Hugo renders the
# static table. The checked-in reports remain a complete fallback, so a
# temporary fetch failure never produces an empty public page.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MANIFEST="${REPO_ROOT}/docs/website/data/port_status.json"
REPORT_DIR="${REPO_ROOT}/docs/website/data/port_status_reports"
DATA_REF="refs/heads/port-status-data"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to refresh Port Status reports." >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

if ! git -C "${REPO_ROOT}" fetch --quiet --no-tags --depth=1 origin "${DATA_REF}"; then
  echo "Port Status data branch is unavailable; using the checked-in reports." >&2
  exit 0
fi

synced=0
while IFS= read -r port; do
  candidate="${tmp_dir}/${port}.json"
  if ! git -C "${REPO_ROOT}" show "FETCH_HEAD:ports/${port}.json" > "${candidate}" 2>/dev/null; then
    echo "No persisted ${port} report; keeping the checked-in report." >&2
    continue
  fi
  if ! jq -e --arg port "${port}" --slurpfile contract "${MANIFEST}" '
      .schema_version == $contract[0].schema_version and
      .port == $port and
      ((.tests | keys | sort) == ([$contract[0].features[].tests[]] | sort)) and
      .performance.status == "complete" and
      (.performance.binary_size_bytes | type == "number" and . > 0) and
      .performance.memory.kind == "managed-heap" and
      (.performance.memory.minimum_bytes | type == "number") and
      (.performance.memory.peak_bytes | type == "number") and
      (.performance.memory.peak_bytes >= .performance.memory.minimum_bytes) and
      ([.performance.benchmarks[].duration_ns | type] | all(. == "number")) and
      ([.performance.benchmarks[].duration_ns] | all(. >= 0)) and
      ((.performance.benchmarks | keys) == ($contract[0].performance_benchmarks | sort))
    ' "${candidate}" >/dev/null; then
    echo "Persisted ${port} report does not match the current contract; keeping the checked-in report." >&2
    continue
  fi
  cp "${candidate}" "${REPORT_DIR}/${port}.json"
  synced=$((synced + 1))
done < <(jq -r '.ports[].id' "${MANIFEST}")

echo "Resolved ${synced} Port Status reports from ${DATA_REF}; remaining ports use checked-in reports."

environment_candidate="${tmp_dir}/environment.json"
environment_target="${REPO_ROOT}/docs/website/data/port_status_environment.json"
if git -C "${REPO_ROOT}" show "FETCH_HEAD:evidence/environment.json" > "${environment_candidate}" 2>/dev/null && \
   jq -e '
      .schema_version == 1 and
      (.generated_at | type == "string") and
      ([.browsers[].id] | sort) == ["chromium", "firefox", "webkit"] and
      all(.browsers[]; (.engine_version | type == "string") and (.status == "pass" or .status == "fail"))
   ' "${environment_candidate}" >/dev/null; then
  cp "${environment_candidate}" "${environment_target}"
  echo "Resolved nightly browser evidence from ${DATA_REF}."
else
  echo "No valid persisted browser evidence; using the checked-in first-run snapshot." >&2
fi
