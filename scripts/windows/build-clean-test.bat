@echo off
rem Build + run the ParparVM clean-target integration test inside the Windows
rem build VM, off the Parallels shared repo (Y:). Initialises the ARM64 Visual
rem Studio developer environment so clang-cl + the Windows SDK are on PATH, then
rem runs the same Maven invocation the Windows CI legs use. Drive it from the Mac:
rem   prlctl exec "Windows 11" --current-user cmd /c Y:\scripts\windows\build-clean-test.bat
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1
echo === clang-cl ===
clang-cl --version || exit /b 1
cd /d Y:\vm || exit /b 1
echo === build JavaAPI ===
call mvn -B clean package -pl JavaAPI -am -DskipTests || exit /b 1
echo === run CleanTargetIntegrationTest ===
call mvn -B test -pl tests -am -Dtest=CleanTargetIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
exit /b %ERRORLEVEL%
