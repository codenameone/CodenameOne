@echo off
rem Build + run the native Windows port headless screenshot capture test, which
rem renders a CN1 Form into an offscreen Direct2D/WIC bitmap and writes a PNG.
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1
cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#capturesFormScreenshotHeadless" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
