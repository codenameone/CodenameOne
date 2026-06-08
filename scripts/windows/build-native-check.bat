@echo off
rem Compile-check the WindowsPort native layer in the VM (runs only the
rem compilesWindowsPortNativeLayer test, which translates a windows app-type
rem dist and compiles each native source with clang-cl in real context).
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1
cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#compilesWindowsPortNativeLayer" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
