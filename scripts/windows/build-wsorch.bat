@echo off
rem Build + run the end-to-end websocket cn1ss capture test: starts a
rem Cn1ssScreenshotServer and runs the native app that streams its Form
rem screenshot over com.codename1.io WebSocket to the server.
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1
cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#capturesFormScreenshotOverWebSocket" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
