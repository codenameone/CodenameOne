@echo off
rem Build the REAL hellocodenameone screenshot suite as a native Windows exe via
rem the ParparVM clean target (CleanTargetIntegrationTest#buildsHelloCodenameOneNative).
rem Translates hellocodenameone-common (Kotlin/Java app + all *ScreenshotTest
rem classes) + Kotlin stdlib + core + Windows port, and clang-cl-links it. This is
rem the link milestone -- it surfaces every native method the suite reaches that
rem the port has not implemented yet.
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1

rem Recompile the Windows port Java against the already-built core classes.
cd /d Y:\ || exit /b 1
if not exist "Y:\maven\windows\target\classes" mkdir "Y:\maven\windows\target\classes"
dir /s /b "Y:\Ports\WindowsPort\src\*.java" > "%TEMP%\winport-src.txt"
"%JAVA_HOME%\bin\javac" -nowarn --release 8 -d "Y:\maven\windows\target\classes" -cp "Y:\maven\core\target\classes" @"%TEMP%\winport-src.txt" || exit /b 1

cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#buildsHelloCodenameOneNative" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
