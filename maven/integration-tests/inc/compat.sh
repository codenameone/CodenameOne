# What the compatibility-layer tests (android-compat-test.sh,
# desktop-compat-test.sh) share: generating the application a sample is
# imported into, finding classes a relocation missed, and staging every
# target's upload jar with Maven and with Gradle to compare them.
#
# Source inc/env.sh and inc/gradle.sh first; this file uses mvn_local,
# run_gradle, fail and GRADLE_JDK from them. A script sets FAILED=0 before its
# loop and exits by it afterwards: the functions here set it to 1 for a failure
# worth reporting alongside the others, and call fail for one that leaves
# nothing further to check.

# The classpath of the build engine's class checker: the engine's own jar and
# the two ASM jars it reads classes with, at the version the reactor pins. All
# three are in the local repository once the reactor is installed.
compat_checker_classpath() {
  local pom="$SCRIPTPATH/../pom.xml" asm
  asm=$(sed -n '/<artifactId>asm-commons<\/artifactId>/{n;s/.*<version>\(.*\)<\/version>.*/\1/p;}' "$pom" | head -1)
  [ -n "$asm" ] || fail "could not read the ASM version from $pom"
  local engine="$CN1_REPO/com/codenameone/codenameone-build-engine/$CN1_VERSION/codenameone-build-engine-$CN1_VERSION.jar"
  local core="$CN1_REPO/org/ow2/asm/asm/$asm/asm-$asm.jar"
  local commons="$CN1_REPO/org/ow2/asm/asm-commons/$asm/asm-commons-$asm.jar"
  local jar
  for jar in "$engine" "$core" "$commons"; do
    [ -f "$jar" ] || fail "the class checker needs $jar; install the reactor first"
  done
  echo "$engine:$core:$commons"
}

# compat_unrelocated <classes dir or jar> <packages>
#
# Class files that still refer to a TYPE under one of <packages>, an ERE
# alternation of slash-separated package names without the trailing slash
# ("android|androidx"), one per line with the types named.
#
# Asked of com.codename1.maven.UnrelocatedTypes, which reads each class as the
# relocation does: supertypes, descriptors, signatures, instruction operands.
# A search of the bytes is not good enough -- an application may hold the text
# "javax/swing/JTable" in a string, and the relocation rightly leaves it.
compat_unrelocated() {
  local dir="$1" packages="$2" cp status=0
  cp=$(compat_checker_classpath) || exit 1
  # Word splitting turns the alternation into the checker's arguments.
  local IFS='|'
  # shellcheck disable=SC2086
  "$GRADLE_JDK/bin/java" -cp "$cp" com.codename1.maven.UnrelocatedTypes "$dir" $packages || status=$?
  # 1 is "found some", and they are on stdout; anything above is a failure of
  # the check itself, which must not read as a clean directory.
  [ $status -le 1 ] || fail "the class checker failed on $dir (status $status)"
}

# The jar a build run with codename1.stageOnly=true reported in <log>.
compat_staged_jar() {
  sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$1" | tail -1
}

# Bookkeeping, never read from the upload: directory entries and Maven's pom
# metadata and manifest (as gradle-maven-parity-test.sh); Kotlin's module file,
# which each tool names after its own project (app-common, DroidApp) and only
# Kotlin reflection reads; and the ORM enhancer's per-directory list, which
# Gradle keeps in each of its classes directories and the enhancer only ever
# reads from the directory.
COMPAT_IGNORED='/$|^META-INF/maven/|^META-INF/MANIFEST\.MF$|^META-INF/[^/]*\.kotlin_module$|^META-INF/cn1/orm-enhanced-dependencies\.list$'

# compat_generate_app <label> <work dir> <package> <main class name>
#
# Generates the archetype application as <work dir>/app.
compat_generate_app() {
  local label="$1" work="$2" pkg="$3" main="$4"
  (cd "$work" && mvn_local archetype:generate -DarchetypeArtifactId=cn1app-archetype -DarchetypeGroupId=com.codenameone \
    -DarchetypeVersion="$CN1_VERSION" -DartifactId=app -DgroupId=$pkg -Dpackage=$pkg \
    -Dversion=1.0-SNAPSHOT -DmainName=$main -DjavaVersion=17 -DinteractiveMode=false) > "$work/archetype.log" 2>&1 \
    || { tail -40 "$work/archetype.log"; fail "$label: archetype:generate"; }
}

# compat_build_common <label> <work dir> <maven app dir>
#
# Builds the common module, leaving its classes in common/target/classes.
compat_build_common() {
  local label="$1" work="$2" mapp="$3"
  (cd "$mapp" && JAVA_HOME="$GRADLE_JDK" mvn_local install -DskipTests -pl common -am) > "$work/mvn-common.log" 2>&1 \
    || { tail -60 "$work/mvn-common.log"; fail "$label: Maven build"; }
}

# compat_convert_to_gradle <label> <work dir> <maven app dir>
#
# Converts the application; the Gradle project is <work dir>/app-gradle, built
# with the plugin from this checkout.
compat_convert_to_gradle() {
  local label="$1" work="$2" mapp="$3"
  (cd "$mapp" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:convert-to-gradle") > "$work/convert.log" 2>&1 \
    || { cat "$work/convert.log"; fail "$label: convert-to-gradle"; }
  use_local_plugin "$work/app-gradle"
}

# compat_stage_all <label> <work dir> <maven app dir> <gradle app dir> <check>
#
# Stages the upload jar of every target (never sent) with both tools, calls
# <check> <jar> on each of the two jars, and compares their entry lists.
compat_stage_all() {
  local label="$1" work="$2" mapp="$3" gapp="$4" check="$5"
  local spec platform target task mlog glog mjar gjar jar
  for spec in "android android-device buildAndroid" "ios ios-device buildIos" "javascript javascript buildJavascript" \
              "javase mac-os-x-desktop buildMacDesktop"; do
    read -r platform target task <<< "$spec"
    mlog="$work/maven-$target.log"
    glog="$work/gradle-$target.log"
    (cd "$mapp" && JAVA_HOME="$GRADLE_JDK" mvn_local package -DskipTests -Dopen=false -Dcodename1.platform=$platform \
        -Dcodename1.buildTarget=$target -Dcodename1.stageOnly=true < /dev/null) > "$mlog" 2>&1 \
      || { tail -40 "$mlog"; fail "$label: Maven staging $target"; }
    run_gradle "$gapp" "$task" -Pcodename1.stageOnly=true > "$glog" 2>&1 || { tail -60 "$glog"; fail "$label: Gradle staging $target"; }
    mjar=$(compat_staged_jar "$mlog")
    gjar=$(compat_staged_jar "$glog")
    [ -f "$mjar" ] && [ -f "$gjar" ] || fail "$label: $target: a staged jar is missing (maven=$mjar gradle=$gjar)"
    for jar in "$mjar" "$gjar"; do
      "$check" "$jar"
    done
    unzip -Z1 "$mjar" | grep -Ev "$COMPAT_IGNORED" | sort > "$work/maven-$target.txt"
    unzip -Z1 "$gjar" | grep -Ev "$COMPAT_IGNORED" | sort > "$work/gradle-$target.txt"
    if diff "$work/maven-$target.txt" "$work/gradle-$target.txt" > "$work/diff-$target.txt"; then
      echo "   $target: $(wc -l < "$work/maven-$target.txt" | tr -d ' ') entries, Maven and Gradle identical"
    else
      echo "FAIL: $label: $target uploads differ (< Maven only, > Gradle only):"
      head -40 "$work/diff-$target.txt"
      FAILED=1
    fi
  done
}
