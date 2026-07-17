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
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

report="${1:-}"
if [ -z "${report}" ] || [ ! -f "${report}" ]; then
  echo "Usage: $0 <port-status-environment.json>" >&2
  exit 2
fi

if [ "${GITHUB_ACTIONS:-}" != "true" ] || [ "${GITHUB_REF:-}" != "refs/heads/master" ]; then
  echo "Not a master GitHub Actions run; skipping environment-evidence publication."
  exit 0
fi

repo="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
branch="port-status-data"
target="evidence/environment.json"

if ! gh api "repos/${repo}/git/ref/heads/${branch}" >/dev/null 2>&1; then
  default_branch="$(gh api "repos/${repo}" --jq .default_branch)"
  base_sha="$(gh api "repos/${repo}/git/ref/heads/${default_branch}" --jq .object.sha)"
  gh api --method POST "repos/${repo}/git/refs" -f ref="refs/heads/${branch}" -f sha="${base_sha}" >/dev/null 2>&1 || \
    gh api "repos/${repo}/git/ref/heads/${branch}" >/dev/null
fi

content="$(base64 < "${report}" | tr -d '\n')"
for attempt in 1 2 3; do
  existing_sha="$(gh api "repos/${repo}/contents/${target}?ref=${branch}" --jq .sha 2>/dev/null || true)"
  args=(--method PUT "repos/${repo}/contents/${target}" -f "message=Update nightly browser evidence" -f "content=${content}" -f "branch=${branch}")
  if [ -n "${existing_sha}" ]; then
    args+=(-f "sha=${existing_sha}")
  fi
  if gh api "${args[@]}" >/dev/null; then
    echo "Published ${target} on ${branch}."
    exit 0
  fi
  echo "Environment-evidence update raced another publisher; retrying (${attempt}/3)." >&2
  sleep 2
done

echo "Failed to publish ${target} after three attempts." >&2
exit 1
