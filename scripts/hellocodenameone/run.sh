#!/bin/bash
MVNW="./mvnw"

JAVA17_HOME=""
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ] && "$JAVA_HOME/bin/java" -version 2>&1 | head -n 1 | grep -q "\"17"; then
  JAVA17_HOME="$JAVA_HOME"
else
  for candidate in /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/java-17-openjdk /usr/lib/jvm/jdk-17 /usr/lib/jvm/*17*; do
    if [ -x "$candidate/bin/java" ] && "$candidate/bin/java" -version 2>&1 | head -n 1 | grep -q "\"17"; then
      JAVA17_HOME="$candidate"
      break
    fi
  done
fi

if [ -n "$JAVA17_HOME" ]; then
  export JAVA_HOME="$JAVA17_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

function simulator {
  
  "$MVNW" "verify" "-Psimulator" "-DskipTests" "-Dcodename1.platform=javase" "-e"
}
function desktop {
  
  "$MVNW" "verify" "-Prun-desktop" "-DskipTests" "-Dcodename1.platform=javase" "-e"
}
function settings {
  
  "$MVNW" "cn1:settings" "-e"
}
function update {
  
  "$MVNW" "cn1:update" "-U" "-e"
}
function help {
  "echo" "-e" "run.sh [COMMAND]"
  "echo" "-e" "Commands:"
  "echo" "-e" "  simulator"
  "echo" "-e" "    Runs app using Codename One Simulator"
  "echo" "-e" "  desktop"
  "echo" "-e" "    Runs app as a desktop app."
  "echo" "-e" "  settings"
  "echo" "-e" "    Opens Codename One settings"
  "echo" "-e" "  update"
  "echo" "-e" "    Update Codename One libraries"
}
CMD=$1

if [ "$CMD" == "" ]; then
  CMD="simulator"
fi
"$CMD" 
