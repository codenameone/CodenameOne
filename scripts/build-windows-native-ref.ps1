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

# Built, not published, and run straight out of the build output.
#
# `-t:Publish` dropped the PRI files: neither the app's resources.pri nor WinUI's
# Microsoft.UI.Xaml.Controls.pri reached the publish directory, so every control rendered
# unstyled. The build output has them, and for a self-contained RID build it is already a
# complete, runnable folder -- publishing bought nothing and lost the theme resources.
#
# OutDir is pinned rather than discovered so the path does not have to be reconstructed from
# the platform, RID and target framework, which is four things that can each drift.
Push-Location $Src
try {
  # Reported from INSIDE the project directory. Run from the repo root it reported 10.0.400
  # while global.json pins 9.0.x, because SDK resolution reads global.json from the current
  # directory upward -- a log line that was quietly answering a different question.
  Write-Log "dotnet SDK here: $(& dotnet --version)"
  & msbuild -t:Restore -p:Configuration=Release -p:RuntimeIdentifier=$Rid -p:Platform=$MsbuildPlatform -v:minimal
  if ($LASTEXITCODE -ne 0) { throw "msbuild restore failed with $LASTEXITCODE" }

  & msbuild -t:Build `
      -p:Configuration=Release `
      -p:RuntimeIdentifier=$Rid `
      -p:Platform=$MsbuildPlatform `
      -p:SelfContained=true `
      -p:OutDir="$publishDir\" `
      -v:minimal
  if ($LASTEXITCODE -ne 0) { throw "msbuild build failed with $LASTEXITCODE" }
} finally {
  Pop-Location
}

# WinUI's theme dictionary ships inside a .pri. Without one every control silently falls back
# to an unstyled default -- a square grey rectangle where a Fluent Button should be -- and the
# app has no way to tell: it asks for a Button and gets one, just not a styled one.
#
# Checked, not repaired. An earlier version of this copied the largest .pri it could find to
# resources.pri, which made the check pass while the controls stayed unstyled: it had grabbed
# WinUI's framework PRI and renamed it, which is not the same file and does not do the same
# job. A check that papers over what it finds is worse than no check.
$priFiles = @(Get-ChildItem -Path $publishDir -Filter '*.pri' -ErrorAction SilentlyContinue)
if ($priFiles.Count -eq 0) {
  Write-Log 'FAILED: no .pri in the build output.'
  Write-Log 'WinUI theme resources live there; without one the controls render unstyled and'
  Write-Log 'the reference would encode fallback visuals rather than Fluent ones.'
  Get-ChildItem $publishDir -File | Select-Object -First 30 -ExpandProperty Name | ForEach-Object { Write-Log "  $_" }
  exit 23
}
foreach ($f in $priFiles) { Write-Log "pri: $($f.Name) ($($f.Length) bytes)" }

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

if ($rc -eq 20) {
  Write-Log 'FAILED: the app refused to vouch for the capture; see the BLOCKER lines above.'
  Write-Log 'A blocker means the capture would have been quietly wrong, not that it crashed.'
  exit $rc
}
elseif ($rc -ne 0) {
  # Distinguished from a blocker because the two need completely different responses, and
  # the previous message asserted "not that it crashed" underneath an app that had in fact
  # crashed with 0xC000027B.
  $hex = '0x{0:X8}' -f ($rc -band 0xFFFFFFFF)
  Write-Log "FAILED: the app CRASHED with exit $rc ($hex)."
  if ($hex -eq '0xC000027B') {
    Write-Log 'That is STATUS_STOWED_EXCEPTION: an unhandled WinRT/XAML exception. The'
    Write-Log 'usual cause is touching XAML state before it is ready, or off the UI thread.'
  }
  exit $rc
}
Write-Log "Wrote $((Get-ChildItem $OutDir | Measure-Object).Count) file(s) to $OutDir"
