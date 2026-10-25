#!/usr/bin/env bash
#
# Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
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
# Kotlin Multiplatform sources as a Codename One application, end to end, for
# every sample under scripts/kmp-compat-samples:
#   - cn1:import-kmp-project copies commonMain (and the JVM source set, minus
#     its desktop window) into common/src/main/kmp and writes the main class;
#   - the Maven build compiles them with the Compose compiler against
#     compose-compat and passes the bytecode compliance check;
#   - the Gradle conversion applies the Compose plugin, and both tools stage
#     the same upload for every target, carrying the Compose runtime and
#     compose-compat as they are -- androidx.compose is never relocated.
#
# Needs the reactor installed (mvn install) and a JDK 17+; see inc/gradle.sh.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
SAMPLES="$SCRIPTPATH/../../scripts/kmp-compat-samples"
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"
source "$SCRIPTPATH/inc/compat.sh"

WORKDIR="${CN1_COMPAT_WORKDIR:-$SCRIPTPATH/build}/kmp-compat"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
FAILED=0

check_kmp_upload() {
  local jar="$1"
  assert_zip_has "$jar" "(^|/)$MAIN_ENTRY\\.class\$"
  assert_zip_has "$jar" "^androidx/compose/runtime/Recomposer\\.class\$"
  assert_zip_has "$jar" "^com/codename1/compose/ComposeView\\.class\$"
  assert_zip_has "$jar" "^kotlinx/coroutines/.*\\.class\$"
  # The dispatcher kotlinx.coroutines finds for Dispatchers.Main.
  assert_zip_has "$jar" "^META-INF/services/kotlinx\\.coroutines\\.internal\\.MainDispatcherFactory\$"
  assert_zip_lacks "$jar" "^com/codename1/androidcompat/androidx/compose/"
}

for sample_dir in "$SAMPLES"/*/; do
  sample=$(basename "$sample_dir")
  echo "== $sample"
  W="$WORKDIR/$sample"
  mkdir -p "$W"
  PKG=com.acme.kmp
  compat_generate_app "$sample" "$W" $PKG KmpApp
  MAPP="$W/app"
  (cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-kmp-project" \
      "-Dcn1.kmp.import=$sample_dir") > "$W/import.log" 2>&1 \
    || { tail -40 "$W/import.log"; fail "$sample: import-kmp-project"; }
  kmp="$MAPP/common/src/main/kmp"
  [ -n "$(find "$kmp/commonMain" -name '*.kt' 2>/dev/null | head -1)" ] || fail "$sample: the import copied no commonMain sources"
  [ -z "$(grep -rl 'androidx\.compose\.ui\.window' "$kmp" 2>/dev/null)" ] \
    || fail "$sample: the import kept a source that opens a desktop window"
  grep -q 'left out: iosMain' "$W/import.log" || fail "$sample: the import did not report the iosMain source set"
  MAIN_ENTRY=com/acme/kmp/KmpApp

  compat_build_common "$sample" "$W" "$MAPP"
  grep -q 'bytecode-compliance' "$W/mvn-common.log" || fail "$sample: the compliance check did not run"
  classes="$MAPP/common/target/classes"
  [ -f "$classes/$MAIN_ENTRY.class" ] || fail "$sample: the build produced no $MAIN_ENTRY.class"
  # The Compose compiler ran: a composable takes the Composer it added.
  find "$classes" -name 'AppKt.class' -exec javap -p {} \; 2>/dev/null | grep -q 'androidx.compose.runtime.Composer' \
    || fail "$sample: App() was not compiled by the Compose compiler"

  compat_convert_to_gradle "$sample" "$W" "$MAPP"
  GAPP="$W/app-gradle"
  [ -d "$GAPP/src/main/kmp" ] || fail "$sample: the Gradle conversion dropped src/main/kmp"
  grep -q 'org.jetbrains.kotlin.plugin.compose' "$GAPP/build.gradle.kts" \
    || fail "$sample: the Gradle conversion did not apply the Compose compiler plugin"

  compat_stage_all "$sample" "$W" "$MAPP" "$GAPP" check_kmp_upload
done

[ "$FAILED" = 0 ] || { echo "kmp-compat-test: FAILED"; exit 1; }
echo "kmp-compat-test: OK"
