@echo off
rem Build a real CN1 Form app through the native Windows path (full core + port).
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1

rem Recompile the Windows port Java (maven/windows/target/classes) so the test,
rem which compiles the app against those classes, picks up port source edits.
rem Compile directly with javac against the already-built core classes --
rem codenameone-core is only present as target/classes (not installed as a jar),
rem so `mvn -pl windows` cannot resolve it.
cd /d Y:\ || exit /b 1
if not exist "Y:\maven\windows\target\classes" mkdir "Y:\maven\windows\target\classes"
dir /s /b "Y:\Ports\WindowsPort\src\*.java" > "%TEMP%\winport-src.txt"
"%JAVA_HOME%\bin\javac" -nowarn --release 8 -d "Y:\maven\windows\target\classes" -cp "Y:\maven\core\target\classes" @"%TEMP%\winport-src.txt" || exit /b 1

cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#buildsFullFormAppNative" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
