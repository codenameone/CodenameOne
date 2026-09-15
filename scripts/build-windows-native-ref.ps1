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
# MSBuild wants the platform in its own spelling, which is not the RID's.
$MsbuildPlatform = if ($Rid -eq 'win-arm64') { 'ARM64' } else { 'x64' }
Write-Log "Publishing for $Rid (msbuild platform $MsbuildPlatform)"

$publishDir = Join-Path $env:TEMP "cn1-win-ref-$(Get-Random)"

# Visual Studio's MSBuild, not `dotnet publish` -- see the comment in NativeRef.csproj for
# why. The workflow puts it on PATH with microsoft/setup-msbuild.
$msbuild = (Get-Command msbuild -ErrorAction SilentlyContinue)
if (-not $msbuild) {
  throw "msbuild is not on PATH. The WindowsAppSDK packaging targets need Visual Studio's MSBuild; the .NET SDK's does not carry the AppxPackage tasks."
}
Write-Log "msbuild: $($msbuild.Source)"
Write-Log "dotnet SDK in effect: $(& dotnet --version)"

# Run from inside the project directory so the SDK pin in global.json applies: it resolves
# from the current directory upward, not from the project path.
Push-Location $Src
try {
  & msbuild -t:Restore -p:Configuration=Release -p:RuntimeIdentifier=$Rid -p:Platform=$MsbuildPlatform -v:minimal
  if ($LASTEXITCODE -ne 0) { throw "msbuild restore failed with $LASTEXITCODE" }

  & msbuild -t:Publish `
      -p:Configuration=Release `
      -p:RuntimeIdentifier=$Rid `
      -p:Platform=$MsbuildPlatform `
      -p:SelfContained=true `
      -p:PublishDir=$publishDir `
      -v:minimal
  if ($LASTEXITCODE -ne 0) { throw "msbuild publish failed with $LASTEXITCODE" }
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

# WinUI's theme dictionary ships inside resources.pri. Without it every control silently
# falls back to an unstyled default -- a square grey rectangle where a Fluent Button should
# be -- and the app has no way to tell: it asks for a Button and gets one, just not a styled
# one. Checked here because it is a property of the BUILD, and a build that quietly omits it
# must not go on to photograph the result.
$pri = Join-Path $publishDir 'resources.pri'
if (-not (Test-Path $pri)) {
  Write-Log 'FAILED: resources.pri was not produced.'
  Write-Log 'WinUI theme resources live there; without it the controls render unstyled and'
  Write-Log 'the reference would encode fallback visuals rather than Fluent ones.'
  Get-ChildItem $publishDir | Select-Object -First 25 -ExpandProperty Name | ForEach-Object { Write-Log "  $_" }
  exit 23
}
Write-Log "resources.pri present ($((Get-Item $pri).Length) bytes)"

$env:NATIVEREF_OUT = $OutDir
if (-not $env:NATIVEREF_MODE) { $env:NATIVEREF_MODE = 'probe' }

Write-Log "Running (mode=$env:NATIVEREF_MODE)"
$exe = Join-Path $publishDir 'NativeRef.exe'
if (-not (Test-Path $exe)) { throw "NativeRef.exe was not produced in $publishDir" }

# Start-Process -Wait, not `& $exe`. NativeRef is a WinExe (GUI subsystem), so PowerShell's
# call operator does not wait for it and $LASTEXITCODE is whatever ran before -- which is
# how run 34947076245 reported SUCCESS while the app itself had written
# "NATIVEREF:BLOCKER this window never became foreground" and exited 20. A verification
# harness that cannot fail is worth nothing, and this one could not.
# Redirected to files, then echoed. A WinExe's console writes do not reach the job log
# through Start-Process, so the previous run's BLOCKER line survived only inside the
# uploaded artifact -- diagnostics that can only be read by downloading an artifact are
# diagnostics most people will never read.
$outLog = Join-Path $publishDir 'nativeref.out.log'
$errLog = Join-Path $publishDir 'nativeref.err.log'
$proc = Start-Process -FilePath $exe -Wait -PassThru -NoNewWindow `
    -RedirectStandardOutput $outLog -RedirectStandardError $errLog
$rc = $proc.ExitCode
foreach ($f in @($outLog, $errLog)) {
  if (Test-Path $f) { Get-Content $f | ForEach-Object { Write-Host $_ } }
}
Write-Log "app exited $rc"

if ($rc -ne 0) {
  Write-Log "FAILED: the reference app exited $rc; see the BLOCKER lines above."
  Write-Log 'A blocker means the capture would have been quietly wrong, not that it crashed.'
  Write-Log 'On windows-latest (Windows Server) a Mica/transparency blocker is expected --'
  Write-Log 're-dispatch with windows_runner: windows-11-arm for a real Windows 11 client.'
  exit $rc
}
Write-Log "Wrote $((Get-ChildItem $OutDir | Measure-Object).Count) file(s) to $OutDir"
