@echo off
setlocal DisableDelayedExpansion
setlocal EnableExtensions


pushd "%~dp0" || exit /b 1
set "MVNW=mvnw.cmd"

SET "CMD=%~1"
if "%CMD%"=="" (
  echo Starting the LOCAL simulator. For a cloud build, run .\build.bat javascript_cloud.
  set CMD=simulator
)
goto %CMD%

:simulator
call "%MVNW%" verify -Psimulator -DskipTests -Dcodename1.platform^=javase -e

goto :finish
:desktop
call "%MVNW%" verify -Prun-desktop -DskipTests -Dcodename1.platform^=javase -e

goto :finish
:settings
call "%MVNW%" cn1:settings -e

goto :finish
:certificatewizard
call "%MVNW%" cn1:certificatewizard -e

goto :finish
:update
call "%MVNW%" cn1:update -U -e

goto :finish
:help
echo run.bat [COMMAND]
echo Commands:
echo   simulator
echo     Runs app using Codename One Simulator
echo   desktop
echo     Runs app as a desktop app.
echo   settings
echo     Opens Codename One settings
echo   certificatewizard
echo     Opens the Certificate Wizard
echo   update
echo     Update Codename One libraries

:finish
set "CN1_EXIT_CODE=%errorlevel%"
popd
exit /b %CN1_EXIT_CODE%
