#!/usr/bin/env bash
#
# Fails when a rendered backend API reference documents a type that is plumbing.
#
#   scripts/check-backend-javadoc-internals.sh <reference-dir> <page-extension>
#
# The backend's public API is what an application is written against. What the
# build's generated code and the runtime's own classes call -- the wiring helpers,
# the woven aspects' runtime, the scheduler, the session manager, the MCP and
# management endpoints -- lives in com.codename1.impl.backend or is package-private,
# and reaches across packages only through BackendAccess and MetricsAccess.
# build_javadocs.sh and the website build already refuse any com/codename1/impl
# output; this names the individual types too, so making one of them public again
# in a documented package fails here instead of quietly publishing it as API.
set -euo pipefail

dir="${1:?usage: check-backend-javadoc-internals.sh <reference-dir> <page-extension>}"
ext="${2:?usage: check-backend-javadoc-internals.sh <reference-dir> <page-extension>}"

INTERNAL="AsyncTask
BackendAccess
BackendApplication
CronSchedule
DevTools
JsonCodec
ManagedBean
Management
McpArgs
McpServer
MetricsAccess
RequestLog
Scheduler
Sessions
TransactionSession
Wiring
WiringEnvironment
Backend.Application
Backend.Environment
Sessions.Db
Sessions.Memory"

found=0
while IFS= read -r name; do
  [ -n "$name" ] || continue
  hits="$(find "$dir" -name "$name.$ext" -print)"
  if [ -n "$hits" ]; then
    echo "check-backend-javadoc-internals: $name is internal and must not be documented as backend API:" >&2
    echo "$hits" >&2
    found=1
  fi
done <<< "$INTERNAL"
if [ "$found" != 0 ]; then
  echo "Move it to com.codename1.impl.backend or make it package-private; reach it through BackendAccess." >&2
  exit 1
fi
echo "check-backend-javadoc-internals: no internal type is documented in $dir"
