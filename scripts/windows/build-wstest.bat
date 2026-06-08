@echo off
rem Build the WebSocket screenshot app for the native Windows port: it captures
rem the Form and streams the PNG to a cn1ss WebSocket server over WinSock.
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1
cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#buildsWebSocketShotApp" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
