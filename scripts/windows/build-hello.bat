@echo off
rem Build the REAL hellocodenameone screenshot suite as a native Windows exe via
rem the ParparVM clean target (CleanTargetIntegrationTest#buildsHelloCodenameOneNative).
rem Translates hellocodenameone-common (Kotlin/Java app + all *ScreenshotTest
rem classes) + Kotlin stdlib + core + Windows port, and clang-cl-links it. This is
rem the link milestone -- it surfaces every native method the suite reaches that
rem the port has not implemented yet.
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1

rem WebView2 SDK for the BrowserComponent peer (cn1_windows_browser.cpp). Fetch it
rem if absent and point the translator's CMake at it via WEBVIEW2_SDK_DIR. The
rem build degrades gracefully (browser natives compile as stubs) if this is unset.
set WEBVIEW2_SDK_DIR=C:\webview2sdk\pkg\build\native
if not exist "%WEBVIEW2_SDK_DIR%\include\WebView2.h" powershell -NoProfile -Command "iex (Get-Content Y:\scripts\windows\fetch-webview2-sdk.ps1 -Raw)"

rem Recompile the Windows port Java against the already-built core classes.
cd /d Y:\ || exit /b 1
if not exist "Y:\maven\windows\target\classes" mkdir "Y:\maven\windows\target\classes"
dir /s /b "Y:\Ports\WindowsPort\src\*.java" > "%TEMP%\winport-src.txt"
"%JAVA_HOME%\bin\javac" -nowarn --release 8 -d "Y:\maven\windows\target\classes" -cp "Y:\maven\core\target\classes" @"%TEMP%\winport-src.txt" || exit /b 1

rem Recompile hellocodenameone-common Java against the fresh core. The prebuilt
rem common/target/classes can be stale vs source (the WebSocket screenshot
rem transport + newer *ScreenshotTest classes live here, and a full maven rebuild
rem needs the 8.0-SNAPSHOT plugin). Kotlin is unchanged, so javac against the
rem fresh core + the existing Kotlin classes is sufficient. Compile to the Kotlin
rem jvmTarget level (17) so the translator handles the bytecode.
dir /s /b "Y:\scripts\hellocodenameone\common\src\main\java\*.java" > "%TEMP%\cc-java.txt"
"%JAVA_HOME%\bin\javac" -nowarn -encoding UTF-8 --release 17 -cp "Y:\maven\core\target\classes;Y:\scripts\hellocodenameone\common\target\classes" -d "Y:\scripts\hellocodenameone\common\target\classes" @"%TEMP%\cc-java.txt" || exit /b 1

cd /d Y:\vm || exit /b 1
call mvn -B test -pl tests -am "-Dtest=CleanTargetIntegrationTest#buildsHelloCodenameOneNative" "-Dsurefire.failIfNoSpecifiedTests=false"
exit /b %ERRORLEVEL%
