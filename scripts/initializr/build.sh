#!/bin/bash
set -e
MVNW="./mvnw"
RUN_TESTS_DEFAULT="true"

function should_run_tests {
  if [ "$INITIALIZR_RUN_TESTS" == "" ]; then
    [ "$RUN_TESTS_DEFAULT" == "true" ]
    return
  fi
  [ "$INITIALIZR_RUN_TESTS" == "true" ]
}

function run_common_tests {
  if should_run_tests; then
    "$MVNW" "-pl" "common" "-am" "test" "-DskipTests=false" "-DfailIfNoTests=false" "-Dtest=GeneratorModelMatrixTest,GeneratorModelLocalizationPackagingTest" "-U" "-e"
  fi
}

function mac_desktop {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=javase" "-Dcodename1.buildTarget=mac-os-x-desktop" "-U" "-e"
}
function windows_desktop {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=javase" "-Dcodename1.buildTarget=windows-desktop" "-U" "-e"
}
function windows_device {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=win" "-Dcodename1.buildTarget=windows-device" "-U" "-e"
}
function uwp {
  
  "windows_device" 
}
function javascript {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=javascript" "-Dcodename1.buildTarget=javascript" "-U" "-e"
}
function android {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-device" "-U" "-e"
}
function xcode {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-source" "-U" "-e"
}
function ios_source {
  "xcode" 
}
function android_source {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=android" "-Dcodename1.buildTarget=android-source" "-U" "-e"
}
function ios {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-device" "-U" "-e"
}
function ios_release {
  run_common_tests

  
  "$MVNW" "package" "-DskipTests" "-Dcodename1.platform=ios" "-Dcodename1.buildTarget=ios-device-release" "-U" "-e"
}
function jar {
  run_common_tests

  
  "$MVNW" "-Pexecutable-jar" "package" "-Dcodename1.platform=javase" "-DskipTests" "-U" "-e"
}
function help {
  "echo" "-e" "build.sh [COMMAND]"
  "echo" "-e" "Local Build Commands:"
  "echo" "-e" "  The following commands will build the app locally (i.e. does NOT use the Codename One build server)"
  "echo" "-e" ""
  "echo" "-e" "  jar"
  "echo" "-e" "    Builds app as desktop app executable jar file to javase/target directory"
  "echo" "-e" "  android_source"
  "echo" "-e" "    Generates an android gradle project that can be opened in Android studio"
  "echo" "-e" "    *Requires android development tools installed."
  "echo" "-e" "    *Requires ANDROID_HOME environment variable"
  "echo" "-e" "    *Requires either GRADLE_HOME environment variable, or for gradle to be in PATH"
  "echo" "-e" "  ios_source"
  "echo" "-e" "    Generates an Xcode Project that you can open and build using Apple's development tools"
  "echo" "-e" "    *Requires a Mac with Xcode installed"
  "echo" "-e" ""
  "echo" "-e" "Build Server Commands:"
  "echo" "-e" "  The following commands will build the app using the Codename One build server, and require"
  "echo" "-e" "  a Codename One account.  See https://www.codenameone.com"
  "echo" "-e" ""
  "echo" "-e" "  ios"
  "echo" "-e" "    Builds iOS app."
  "echo" "-e" "  ios_release"
  "echo" "-e" "    Builds iOS app for submission to Apple appstore."
  "echo" "-e" "  android"
  "echo" "-e" "    Builds android app."
  "echo" "-e" "  mac_desktop"
  "echo" "-e" "    Builds Mac OS desktop app."
  "echo" "-e" "    *Mac OS Desktop builds are a Pro user feature."
  "echo" "-e" "  windows_desktop"
  "echo" "-e" "    Builds Windows desktop app."
  "echo" "-e" "    *Windows Desktop builds are a Pro user feature."
  "echo" "-e" "  windows_device"
  "echo" "-e" "    Builds UWP Windows app."
  "echo" "-e" "  javascript"
  "echo" "-e" "    Builds as a web app."
  "echo" "-e" "    *Javascript builds are an Enterprise user feature"
}
function settings {
  
  "$MVNW" "cn:settings" "-U" "-e"
}
CMD="$1"

if [ "$CMD" == "" ]; then
  CMD="jar"
fi
"$CMD" 