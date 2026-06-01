@echo off
rem Build a real CN1 Form app through the native Windows path (full core + port).
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1
cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#buildsFullFormAppNative" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
