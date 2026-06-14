@echo off
rem Build + run the native Windows port's WebSocket screenshot-capture test
rem (CleanTargetIntegrationTest#capturesFormScreenshotOverWebSocket): renders the
rem Contacts UI offscreen, PNG-encodes it, streams it over the cn1ss WebSocket to
rem a local Cn1ssScreenshotServer, and writes cn1-windows-native.png. The PNG is
rem placed in CN1_SHOT_OUTPUT_DIR (default: the repo's developer-guide-windows-port).
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1

rem Recompile the Windows port Java (maven/windows/target/classes) so the test
rem picks up port source edits -- codenameone-core is only present as
rem target/classes (not installed as a jar), so compile directly with javac.
cd /d Y:\ || exit /b 1
if not exist "Y:\maven\windows\target\classes" mkdir "Y:\maven\windows\target\classes"
dir /s /b "Y:\Ports\WindowsPort\src\*.java" > "%TEMP%\winport-src.txt"
"%JAVA_HOME%\bin\javac" -nowarn --release 8 -d "Y:\maven\windows\target\classes" -cp "Y:\maven\core\target\classes" @"%TEMP%\winport-src.txt" || exit /b 1

if "%CN1_SHOT_OUTPUT_DIR%"=="" set CN1_SHOT_OUTPUT_DIR=Y:\developer-guide-windows-port
if not exist "%CN1_SHOT_OUTPUT_DIR%" mkdir "%CN1_SHOT_OUTPUT_DIR%"

cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#capturesFormScreenshotOverWebSocket" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
