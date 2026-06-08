@echo off
rem Run the native Windows hellocodenameone exe against a Cn1ssScreenshotServer
rem and let the Cn1ssDeviceRunner suite emit screenshots over the WebSocket.
rem Screenshots land in OUT (default: repo developer-guide-windows-port/hello-shots).
rem Arg 1 (optional): seconds to let the suite run (default 180).
setlocal
set WAIT=%1
if "%WAIT%"=="" set WAIT=180
set OUT=Y:\developer-guide-windows-port\hello-shots
if not exist "%OUT%" mkdir "%OUT%"
del /q "%OUT%\*.png" 2>nul
del /q "%TEMP%\hello-out.txt" 2>nul

rem Compile the shared cn1ss screenshot server.
set SRV=%TEMP%\cn1ss-srv
if not exist "%SRV%" mkdir "%SRV%"
"%JAVA_HOME%\bin\javac" -d "%SRV%" -sourcepath Y:\scripts\common\java Y:\scripts\common\java\Cn1ssScreenshotServer.java || exit /b 1

rem Start the server (background, log captured) and wait until it binds
rem 127.0.0.1:8765 (it prints a CN1SS_SERVER_PORT readiness line).
del /q "%TEMP%\cn1ss-server.log" 2>nul
start "cn1ss-server" /b cmd /c ""%JAVA_HOME%\bin\java" -cp "%SRV%" Cn1ssScreenshotServer --port 8765 --out "%OUT%" > %TEMP%\cn1ss-server.log 2>&1"
set /a tries=0
:waitserver
ping -n 2 127.0.0.1 >nul
findstr /C:"CN1SS_SERVER_PORT" "%TEMP%\cn1ss-server.log" >nul 2>&1 && goto serverup
set /a tries+=1
if %tries% LSS 15 goto waitserver
echo WARNING: server readiness line not seen; proceeding anyway
:serverup
echo ====SERVER LOG (startup)====
type "%TEMP%\cn1ss-server.log" 2>nul

rem Run the suite exe from its own dir (so it finds windowsNativeTheme.res),
rem capturing its CN1SS markers to a log.
cd /d C:\Users\shai\cn1-hello
start "hello" /b cmd /c "WinHelloMain.exe > %TEMP%\hello-out.txt 2>&1"

rem Let the suite run.
ping -n %WAIT% 127.0.0.1 >nul

rem Stop everything.
taskkill /F /IM WinHelloMain.exe >nul 2>&1
for /f "tokens=2" %%p in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| find "PID:"') do taskkill /F /PID %%p >nul 2>&1

echo ====CN1SS MARKERS====
findstr /C:"CN1SS:" "%TEMP%\hello-out.txt" 2>nul
echo ====PNG COUNT====
dir /b "%OUT%\*.png" 2>nul | find /c /v ""
echo ====PNG NAMES====
dir /b "%OUT%\*.png" 2>nul
exit /b 0
