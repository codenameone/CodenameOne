# Dev helper: programmatically resizes the native Windows port window (does NOT
# enter the Win32 modal move/size loop, unlike an interactive border drag), to
# isolate resize-handling bugs from modal-loop bugs.
# Run via: powershell -NoProfile -Command "iex (Get-Content Y:\scripts\windows\resize-window.ps1 -Raw)"
# Tunables: RESIZE_W / RESIZE_H (new client-ish size, default 1024x720).
$W = [int]($env:RESIZE_W); if ($W -le 0) { $W = 1024 }
$H = [int]($env:RESIZE_H); if ($H -le 0) { $H = 720 }

$sig = @"
using System;
using System.Runtime.InteropServices;
public class RW {
    public delegate bool EnumProc(IntPtr h, IntPtr l);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
    [DllImport("user32.dll")] public static extern int GetWindowThreadProcessId(IntPtr h, out int pid);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
}
"@
Add-Type -TypeDefinition $sig

$p = Get-Process WinFormApp -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $p) { Write-Output "PROCESS_NOT_RUNNING"; return }
$found = [IntPtr]::Zero
$cb = [RW+EnumProc]{
    param($h, $l)
    $pid2 = 0
    [RW]::GetWindowThreadProcessId($h, [ref]$pid2) | Out-Null
    if ($pid2 -eq $script:p.Id -and [RW]::IsWindowVisible($h)) { $script:found = $h; return $false }
    return $true
}
[RW]::EnumWindows($cb, [IntPtr]::Zero) | Out-Null
if ($found -eq [IntPtr]::Zero) { Write-Output "NO_WINDOW"; return }
# SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE
[RW]::SetWindowPos($found, [IntPtr]::Zero, 0, 0, $W, $H, 0x0002 -bor 0x0004 -bor 0x0010) | Out-Null
Write-Output ("RESIZED hwnd=" + $found + " to " + $W + "x" + $H)
