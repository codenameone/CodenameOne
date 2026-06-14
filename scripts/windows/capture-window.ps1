# Diagnostic helper: captures the native Windows port window to a PNG via
# PrintWindow, so the rendered UI can be inspected from dev scripts.
# Run via: powershell -NoProfile -Command "iex (Get-Content Y:\scripts\windows\capture-window.ps1 -Raw)"
# Output path is taken from $env:CAP_OUT (default %TEMP%\cn1-capture.png).
$out = $env:CAP_OUT; if (-not $out) { $out = Join-Path $env:TEMP "cn1-capture.png" }

$sig = @"
using System;
using System.Runtime.InteropServices;
public class Cap {
    [DllImport("user32.dll")]
    public static extern bool PrintWindow(IntPtr hwnd, IntPtr hdc, uint flags);
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")]
    public static extern bool GetClientRect(IntPtr hWnd, out RECT r);
}
"@
Add-Type -TypeDefinition $sig
Add-Type -AssemblyName System.Drawing

$p = Get-Process WinFormApp -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $p -or $p.MainWindowHandle -eq [IntPtr]::Zero) { Write-Output "WINDOW_NOT_FOUND"; return }
$h = $p.MainWindowHandle
$r = New-Object Cap+RECT
[Cap]::GetClientRect($h, [ref]$r) | Out-Null
$w = $r.Right - $r.Left; $hgt = $r.Bottom - $r.Top
if ($w -le 0 -or $hgt -le 0) { $w = 800; $hgt = 600 }
$bmp = New-Object System.Drawing.Bitmap $w, $hgt
$g = [System.Drawing.Graphics]::FromImage($bmp)
$hdc = $g.GetHdc()
# flag 3 = PW_RENDERFULLCONTENT, needed for hardware-rendered (D2D) client areas.
[Cap]::PrintWindow($h, $hdc, 3) | Out-Null
$g.ReleaseHdc($hdc)
$g.Dispose()
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output ("SAVED=" + $out + " (" + $w + "x" + $hgt + ")")
