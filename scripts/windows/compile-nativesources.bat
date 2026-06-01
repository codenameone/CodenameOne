@echo off
rem Syntax/semantic compile-check of the WindowsPort native sources with clang-cl
rem against the ParparVM runtime header and the Windows SDK (no link). Surfaces
rem errors in the hand-written Win32 / Direct2D / DirectWrite / WIC / WinHTTP C
rem without a full app translation. Driven from the Mac via:
rem   prlctl exec "Windows 11" --current-user cmd /c Y:\scripts\windows\compile-nativesources.bat
setlocal enabledelayedexpansion
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=arm64 -host_arch=arm64 -no_logo || exit /b 1
cd /d Y:\Ports\WindowsPort\nativeSources || exit /b 1
set FAILED=0
for %%f in (cn1_windows_window.c cn1_windows_graphics.c cn1_windows_text.c cn1_windows_image.c cn1_windows_io.c cn1_windows_net.c) do (
  echo ============================================================
  echo === %%f
  echo ============================================================
  clang-cl /c /std:c11 /W3 /D_CRT_SECURE_NO_WARNINGS /I. /I "Y:\vm\ByteCodeTranslator\src" %%f /Fo"%TEMP%\%%f.obj"
  if errorlevel 1 set FAILED=1
)
echo ============================================================
if "%FAILED%"=="1" ( echo COMPILE_CHECK_RESULT: FAILURES ) else ( echo COMPILE_CHECK_RESULT: ALL_OK )
exit /b %FAILED%
