# Dev/diagnostic helper: drives the native Windows port window through the Win32
# modal move/size loop (the same loop an interactive border-drag enters) by
# posting WM_SYSCOMMAND/SC_SIZE then arrow keys, then committing with Enter.
# Used to reproduce the resize freeze headlessly.
# Run via: powershell -NoProfile -Command "iex (Get-Content Y:\scripts\windows\trigger-modal-resize.ps1 -Raw)"
# Set MODAL_COMMIT=0 to leave the window IN the modal loop (do not press Enter).
$commit = $true
if ($env:MODAL_COMMIT -eq "0") { $commit = $false }

$sig = @"
using System;
using System.Runtime.InteropServices;
public class MR {
    public delegate bool EnumProc(IntPtr h, IntPtr l);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
    [DllImport("user32.dll")] public static extern int GetWindowThreadProcessId(IntPtr h, out int pid);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint m, IntPtr w, IntPtr l);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
}
"@
Add-Type -TypeDefinition $sig

$p = Get-Process WinFormApp -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $p) { Write-Output "PROCESS_NOT_RUNNING"; return }
$found = [IntPtr]::Zero
$cb = [MR+EnumProc]{
    param($h, $l)
    $pid2 = 0
    [MR]::GetWindowThreadProcessId($h, [ref]$pid2) | Out-Null
    if ($pid2 -eq $script:p.Id -and [MR]::IsWindowVisible($h)) { $script:found = $h; return $false }
    return $true
}
[MR]::EnumWindows($cb, [IntPtr]::Zero) | Out-Null
if ($found -eq [IntPtr]::Zero) { Write-Output "NO_WINDOW"; return }
[MR]::SetForegroundWindow($found) | Out-Null

$WM_SYSCOMMAND = 0x0112; $SC_SIZE = 0xF000
$WM_KEYDOWN = 0x0100; $WM_KEYUP = 0x0101
$VK_RIGHT = 0x27; $VK_DOWN = 0x28; $VK_RETURN = 0x0D

# Enter the modal move/size loop (keyboard sizing). The main thread is now stuck
# inside DefWindowProc's internal message loop -- exactly like a border drag.
[MR]::PostMessage($found, $WM_SYSCOMMAND, [IntPtr]$SC_SIZE, [IntPtr]0) | Out-Null
Start-Sleep -Milliseconds 600
# Pick the right edge and grow, then the bottom edge and grow.
foreach ($vk in @($VK_RIGHT,$VK_RIGHT,$VK_RIGHT,$VK_RIGHT,$VK_RIGHT,$VK_RIGHT,$VK_DOWN,$VK_DOWN,$VK_DOWN,$VK_DOWN)) {
    [MR]::PostMessage($found, $WM_KEYDOWN, [IntPtr]$vk, [IntPtr]0) | Out-Null
    [MR]::PostMessage($found, $WM_KEYUP, [IntPtr]$vk, [IntPtr]0) | Out-Null
    Start-Sleep -Milliseconds 120
}
if ($commit) {
    [MR]::PostMessage($found, $WM_KEYDOWN, [IntPtr]$VK_RETURN, [IntPtr]0) | Out-Null
    [MR]::PostMessage($found, $WM_KEYUP, [IntPtr]$VK_RETURN, [IntPtr]0) | Out-Null
    Write-Output ("MODAL_RESIZE_COMMITTED hwnd=" + $found)
} else {
    Write-Output ("MODAL_RESIZE_LEFT_OPEN hwnd=" + $found)
}
