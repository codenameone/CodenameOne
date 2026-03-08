@echo off
setlocal EnableDelayedExpansion
setlocal EnableExtensions


set MVNW=mvnw.cmd

java -version 2>&1 | findstr /r /c:"version \"17\." /c:"version \"17\"" >nul
if errorlevel 1 if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if errorlevel 1 if exist "C:\Program Files\Eclipse Adoptium\jdk-17" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17"
if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

SET CMD=%1
if "%CMD%"=="" (
  set CMD=simulator
)
goto %CMD%

:simulator
!MVNW! verify -Psimulator -DskipTests -Dcodename1.platform^=javase -e

goto :EOF
:desktop
!MVNW! verify -Prun-desktop -DskipTests -Dcodename1.platform^=javase -e

goto :EOF
:settings
!MVNW! cn1:settings -e

goto :EOF
:update
!MVNW! cn1:update -U -e

goto :EOF
:help
echo run.bat [COMMAND]
echo Commands:
echo   simulator
echo     Runs app using Codename One Simulator
echo   desktop
echo     Runs app as a desktop app.
echo   settings
echo     Opens Codename One settings
echo   update
echo     Update Codename One libraries
