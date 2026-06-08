@echo off
rem End-to-end builder + screenshot test in the VM: translate a windows app-type
rem dist driving the WindowsNative bridge, link with clang-cl, run headless, and
rem verify the rendered PNG.
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1
cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#rendersOffscreenToPngWithDirect2D" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
