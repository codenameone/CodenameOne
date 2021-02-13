@echo off
setlocal EnableDelayedExpansion
setlocal EnableExtensions


SET CMD=%1
if !CMD! EQU  (
  set CMD=simulator
)
!CMD!

goto :EOF
:simulator
mvn verify -Psimulator -DskipTests -Dcodename1.platform^=javase

goto :EOF
:desktop
mvn verify -Prun-desktop -DskipTests -Dcodename1.platform^=javase

goto :EOF
:settings
mvn cn:settings

goto :EOF
:help
echo run.sh [COMMAND]
echo Commands:
echo   simulator
echo     Runs app using Codename One Simulator
echo   desktop
echo     Runs app as a desktop app.