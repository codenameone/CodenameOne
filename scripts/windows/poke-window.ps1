# Diagnostic helper: posts synthetic mouse clicks to the native Windows port
# window so input handling can be exercised headlessly from dev scripts.
# Run via:  powershell -NoProfile -Command "iex (Get-Content Y:\scripts\windows\poke-window.ps1 -Raw)"
# Tunable through environment variables POKE_TITLE / POKE_X / POKE_Y / POKE_COUNT.
$Title = $env:POKE_TITLE; if (-not $Title) { $Title = "Codename One" }
$X = [int]($env:POKE_X); if ($X -le 0) { $X = 400 }
$Y = [int]($env:POKE_Y); if ($Y -le 0) { $Y = 260 }
$Count = [int]($env:POKE_COUNT); if ($Count -le 0) { $Count = 4 }

$sig = @"
using System;
using System.Runtime.InteropServices;
public class Win {
    [DllImport("user32.dll", SetLastError=true, CharSet=CharSet.Auto)]
    public static extern IntPtr FindWindow(string lpClassName, string lpWindowName);
    [DllImport("user32.dll")]
    public static extern bool PostMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);
}
"@
Add-Type -TypeDefinition $sig

$h = [Win]::FindWindow($null, $Title)
if ($h -eq [IntPtr]::Zero) {
    # FindWindow can miss the wide-title window; fall back to the process handle.
    $p = Get-Process WinFormApp -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($p -and $p.MainWindowHandle -ne [IntPtr]::Zero) { $h = $p.MainWindowHandle }
}
Write-Output ("HWND=" + $h)
if ($h -eq [IntPtr]::Zero) {
    Write-Output "WINDOW_NOT_FOUND"
} else {
    [Win]::SetForegroundWindow($h) | Out-Null
    $lParam = [IntPtr](($Y -shl 16) -bor $X)
    for ($i = 0; $i -lt $Count; $i++) {
        [Win]::PostMessage($h, 0x0201, [IntPtr]1, $lParam) | Out-Null  # WM_LBUTTONDOWN
        Start-Sleep -Milliseconds 80
        [Win]::PostMessage($h, 0x0202, [IntPtr]0, $lParam) | Out-Null  # WM_LBUTTONUP
        Start-Sleep -Milliseconds 150
    }
    Write-Output ("CLICKS_SENT=" + $Count + " at " + $X + "," + $Y)
}
