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

# compat_unrelocated <classes dir> <packages>
#
# Class files that still name one of <packages>, an ERE alternation of
# slash-separated package names without the trailing slash
# ("android|androidx"). A relocated name is preceded by "/"
# (com/codename1/androidcompat/android/...), an unrelocated one by the constant
# pool's length bytes or by the "L" of a descriptor.
compat_unrelocated() {
  local dir="$1" packages="$2"
  find "$dir" -name '*.class' -print0 | while IFS= read -r -d '' f; do
    if LC_ALL=C grep -aEq "(^|[^/a-zA-Z0-9_\$])L?($packages)/[A-Za-z]" "$f"; then
      echo "${f#$dir/}"
    fi
  done
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
