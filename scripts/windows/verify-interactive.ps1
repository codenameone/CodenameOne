# Dev verification: finds the running native Windows port window (by process id,
# which is more reliable than MainWindowHandle for a console-subsystem exe),
# posts a click, and captures the window to a PNG so interactivity can be
# confirmed from a dev script.
# Run via: powershell -NoProfile -Command "iex (Get-Content Y:\scripts\windows\verify-interactive.ps1 -Raw)"
# Tunables: POKE_X / POKE_Y (click point), CAP_OUT (png path).
$X = [int]($env:POKE_X); if ($X -le 0) { $X = 70 }
$Y = [int]($env:POKE_Y); if ($Y -le 0) { $Y = 126 }
$out = $env:CAP_OUT; if (-not $out) { $out = Join-Path $env:TEMP "cn1-verify.png" }

$sig = @"
using System;
using System.Text;
using System.Runtime.InteropServices;
public class WV {
    public delegate bool EnumProc(IntPtr h, IntPtr l);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr l);
    [DllImport("user32.dll")] public static extern int GetWindowThreadProcessId(IntPtr h, out int pid);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint m, IntPtr w, IntPtr l);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
}
"@
Add-Type -TypeDefinition $sig
Add-Type -AssemblyName System.Drawing

$p = Get-Process WinFormApp -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $p) { Write-Output "PROCESS_NOT_RUNNING"; return }

$found = [IntPtr]::Zero
$cb = [WV+EnumProc]{
    param($h, $l)
    $pid2 = 0
    [WV]::GetWindowThreadProcessId($h, [ref]$pid2) | Out-Null
    if ($pid2 -eq $script:p.Id -and [WV]::IsWindowVisible($h)) { $script:found = $h; return $false }
    return $true
}
[WV]::EnumWindows($cb, [IntPtr]::Zero) | Out-Null
Write-Output ("HWND=" + $found)
if ($found -eq [IntPtr]::Zero) { Write-Output "WINDOW_NOT_FOUND"; return }

[WV]::SetForegroundWindow($found) | Out-Null
$lp = [IntPtr](($Y -shl 16) -bor $X)
[WV]::PostMessage($found, 0x0201, [IntPtr]1, $lp) | Out-Null  # WM_LBUTTONDOWN
Start-Sleep -Milliseconds 90
[WV]::PostMessage($found, 0x0202, [IntPtr]0, $lp) | Out-Null  # WM_LBUTTONUP
Start-Sleep -Milliseconds 400

$r = New-Object WV+RECT
[WV]::GetClientRect($found, [ref]$r) | Out-Null
$w = $r.Right - $r.Left; $hgt = $r.Bottom - $r.Top
if ($w -le 0 -or $hgt -le 0) { $w = 800; $hgt = 600 }
$bmp = New-Object System.Drawing.Bitmap $w, $hgt
$g = [System.Drawing.Graphics]::FromImage($bmp)
$hdc = $g.GetHdc()
[WV]::PrintWindow($found, $hdc, 3) | Out-Null   # PW_RENDERFULLCONTENT
$g.ReleaseHdc($hdc); $g.Dispose()
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
Write-Output ("CLICKED " + $X + "," + $Y + " SAVED=" + $out)
