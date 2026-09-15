<#
.SYNOPSIS
  Build and run the Windows (WinUI 3 / Fluent) native reference app.

.DESCRIPTION
  Counterpart to scripts/build-ios-native-ref.sh, in PowerShell because it has to pin Win32
  environment state; scripts/windows/*.ps1 is the established home for that in this repo.

  Unlike the mobile reference apps this one runs on a hosted CI runner, because that runner
  is the closest available thing to a default-configured Windows box. The catch is that
  `windows-latest` is Windows SERVER, where Mica silently degrades to a flat brush -- so the
  app itself asserts the environment and exits non-zero rather than emitting a reference
  nobody can trust. See .github/workflows/fidelity-desktop-native-ref.yml.

  Honours NATIVEREF_MODE (probe|capture) and CN1SS_FIDELITY_GOLDEN_SET.
#>
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Write-Log($msg) { Write-Host "[build-windows-native-ref] $msg" }

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Src      = Join-Path $RepoRoot 'scripts/fidelity-app/windows-native-ref'
$OutDir   = Join-Path $RepoRoot 'artifacts/desktop-native-ref/windows'
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$Rid = if ([System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture -eq 'Arm64') { 'win-arm64' } else { 'win-x64' }
Write-Log "Publishing for $Rid"

$publishDir = Join-Path $env:TEMP "cn1-win-ref-$(Get-Random)"
# Published from INSIDE the project directory on purpose: the SDK pin in global.json is
# resolved from the current directory upward, not from the project path, so invoking this
# from the repo root would silently select the runner's newest SDK instead.
Push-Location $Src
try {
  Write-Log "dotnet SDK in effect: $(& dotnet --version)"
  & dotnet publish -c Release -r $Rid --self-contained true -o $publishDir
  if ($LASTEXITCODE -ne 0) { throw "dotnet publish failed with $LASTEXITCODE" }
} finally {
  Pop-Location
}

# Pin what the reference must not drift on. Animations off so nothing is captured
# mid-transition; grayscale font smoothing rather than ClearType, because ClearType's
# colour fringes against Codename One's grayscale AA would be a permanent, unclosable
# text residual. Both are recorded in the manifest so the CN1 side can be matched to them.
Write-Log 'Pinning animation and font-smoothing state'
try {
  Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class Spi {
  [DllImport("user32.dll", SetLastError = true)]
  public static extern bool SystemParametersInfo(uint uiAction, uint uiParam, IntPtr pvParam, uint fWinIni);
}
'@ -ErrorAction Stop
  # SPI_SETCLIENTAREAANIMATION = 0x1043, SPI_SETFONTSMOOTHINGTYPE = 0x200B
  [void][Spi]::SystemParametersInfo(0x1043, 0, [IntPtr]::Zero, 0)
} catch {
  Write-Log "WARNING: could not pin system parameters: $_"
}

$env:NATIVEREF_OUT = $OutDir
if (-not $env:NATIVEREF_MODE) { $env:NATIVEREF_MODE = 'probe' }

Write-Log "Running (mode=$env:NATIVEREF_MODE)"
$exe = Join-Path $publishDir 'NativeRef.exe'
if (-not (Test-Path $exe)) { throw "NativeRef.exe was not produced in $publishDir" }

# The app is a WinExe, so its Console writes go to the parent console when there is one.
& $exe
$rc = $LASTEXITCODE

if ($rc -ne 0) {
  Write-Log "FAILED: the reference app exited $rc; see the BLOCKER lines above."
  Write-Log 'A blocker means the capture would have been quietly wrong, not that it crashed.'
  Write-Log 'On windows-latest (Windows Server) a Mica/transparency blocker is expected --'
  Write-Log 're-dispatch with windows_runner: windows-11-arm for a real Windows 11 client.'
  exit $rc
}
Write-Log "Wrote $((Get-ChildItem $OutDir | Measure-Object).Count) file(s) to $OutDir"
