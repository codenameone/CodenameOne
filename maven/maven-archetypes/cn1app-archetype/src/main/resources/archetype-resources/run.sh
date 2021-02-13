#!/bin/bash
function simulator {
  "mvn" "verify" "-Psimulator" "-DskipTests" "-Dcodename1.platform=javase"
}
function desktop {
  "mvn" "verify" "-Prun-desktop" "-DskipTests" "-Dcodename1.platform=javase"
}
function settings {
  "mvn" "cn:settings"
}
function help {
  "echo" "-e" "run.sh [COMMAND]"
  "echo" "-e" "Commands:"
  "echo" "-e" "  simulator"
  "echo" "-e" "    Runs app using Codename One Simulator"
  "echo" "-e" "  desktop"
  "echo" "-e" "    Runs app as a desktop app."
}
CMD=$1

if [ "$CMD" == "" ]; then
  CMD="simulator"
fi
"$CMD" 