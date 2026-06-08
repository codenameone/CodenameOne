# Dev helper: brings the native Windows port window to the foreground so it is
# focused for interactive use in the VM.
# Run via: powershell -NoProfile -Command "iex (Get-Content Y:\scripts\windows\foreground-window.ps1 -Raw)"
$sig = @"
using System;
using System.Runtime.InteropServices;
public class FW {
    public delegate bool EnumProc(IntPtr h, IntPtr l);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
    [DllImport("user32.dll")] public static extern int GetWindowThreadProcessId(IntPtr h, out int pid);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int n);
}
"@
Add-Type -TypeDefinition $sig

$p = Get-Process WinFormApp -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $p) { Write-Output "PROCESS_NOT_RUNNING"; return }
$found = [IntPtr]::Zero
$cb = [FW+EnumProc]{
    param($h, $l)
    $pid2 = 0
    [FW]::GetWindowThreadProcessId($h, [ref]$pid2) | Out-Null
    if ($pid2 -eq $script:p.Id -and [FW]::IsWindowVisible($h)) { $script:found = $h; return $false }
    return $true
}
[FW]::EnumWindows($cb, [IntPtr]::Zero) | Out-Null
if ($found -eq [IntPtr]::Zero) { Write-Output "NO_VISIBLE_WINDOW"; return }
[FW]::ShowWindow($found, 9) | Out-Null      # SW_RESTORE
[FW]::SetForegroundWindow($found) | Out-Null
Write-Output ("FOREGROUND hwnd=" + $found)
