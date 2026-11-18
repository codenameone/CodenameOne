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
#
# Starts the prepared gallery in the simulator and requires its first frame.
#
#   first-frame-check.sh <prepared cn1 project> <maven repository>
#
# Compiling the gallery is not starting it. #6222 put a class initialization
# cycle into the Flutter runtime: every test passed, the gallery compiled, and
# it died in its first layout on every target. The benchmark found that six
# days later, at night, because the benchmark is the only thing that launched
# the gallery and it does not run on a change to the runtime. This is that
# launch, on the JVM, where it costs a minute.
#
# The application prints BENCH:FIRSTFRAME once its first form is on screen, and
# BENCH:FIRSTFRAME-INVALID in its place when that frame reported errors
# (Bench.java). Only the first is a pass. Needs a display: run it under
# xvfb-run on a machine without one.
set -uo pipefail

if [ "$#" -ne 2 ]; then
  echo "usage: $0 <prepared cn1 project> <maven repository>" >&2
  exit 2
fi
project="$1"
repo="$2"
limit="${FIRST_FRAME_TIMEOUT_S:-600}"
log="$(mktemp "${TMPDIR:-/tmp}/first-frame.XXXXXX")"

cd "$project" || exit 2
# In a group of its own (set -m), so the simulator Maven forks goes with it.
set -m
mvn -B verify -Psimulator -DskipTests -DskipComplianceCheck=true \
    -Dcodename1.platform=javase -Dmaven.repo.local="$repo" > "$log" 2>&1 &
build=$!
set +m

verdict="none"
waited=0
while [ "$waited" -lt "$limit" ]; do
  if grep -q 'BENCH:FIRSTFRAME-INVALID' "$log"; then
    verdict="invalid"
    break
  fi
  if grep -q 'BENCH:FIRSTFRAME' "$log"; then
    verdict="ok"
    break
  fi
  if ! kill -0 "$build" 2>/dev/null; then
    verdict="exited"
    break
  fi
  # perl, not sleep: the same loop runs where a foreground sleep is refused.
  perl -e 'select(undef, undef, undef, 2)'
  waited=$((waited + 2))
done
# An application that printed INVALID prints the errors on the lines after it.
[ "$verdict" = "invalid" ] && perl -e 'select(undef, undef, undef, 3)'

kill -TERM -- "-$build" 2>/dev/null
perl -e 'select(undef, undef, undef, 2)'
kill -KILL -- "-$build" 2>/dev/null
wait "$build" 2>/dev/null

if [ "$verdict" = "ok" ]; then
  grep 'BENCH:' "$log"
  echo "The gallery reached its first frame."
  rm -f "$log"
  exit 0
fi

case "$verdict" in
  invalid) echo "The gallery's first frame reported errors." >&2 ;;
  exited)  echo "The simulator exited before the gallery's first frame." >&2 ;;
  *)       echo "The gallery printed no first frame in ${limit}s." >&2 ;;
esac
echo "The last 200 lines it wrote:" >&2
tail -n 200 "$log" >&2
rm -f "$log"
exit 1
