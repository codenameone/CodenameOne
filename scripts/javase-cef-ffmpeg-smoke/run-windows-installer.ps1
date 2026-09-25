# Installs a cloud-built Windows desktop installer, launches the installed app the way a
# user does (through its launch4j exe), and collects what the app reports: its status
# file and screenshot, launch4j's log, and any HotSpot crash log. When the launcher run
# does not produce a status file, the same command line is replayed through the console
# java.exe so the JVM's stdout/stderr are kept.
param([string]$Url, [string]$Out)
$ErrorActionPreference = 'Continue'
New-Item -ItemType Directory -Force -Path $Out | Out-Null
$Out = (Resolve-Path $Out).Path
$InstallerDir = Join-Path $env:RUNNER_TEMP 'installer'
New-Item -ItemType Directory -Force -Path $InstallerDir | Out-Null
$name = [System.IO.Path]::GetFileName(([uri]$Url).AbsolutePath)
Invoke-WebRequest -Uri $Url -OutFile (Join-Path $InstallerDir $name)
if ($name -like '*.zip') { Expand-Archive (Join-Path $InstallerDir $name) -DestinationPath $InstallerDir -Force }
$app = 'C:\cefmaps'

function Save-Screen($name) {
  Add-Type -AssemblyName System.Windows.Forms, System.Drawing
  $b = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
  $bmp = New-Object System.Drawing.Bitmap $b.Width, $b.Height
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.CopyFromScreen($b.Location, [System.Drawing.Point]::Empty, $b.Size)
  $bmp.Save((Join-Path $Out $name))
}

function Collect($tag) {
  $dirs = @($app, $env:TEMP, $env:USERPROFILE)
  foreach ($d in $dirs) {
    Get-ChildItem -Path $d -Filter 'hs_err_pid*.log' -Recurse -Depth 2 -ErrorAction SilentlyContinue |
      ForEach-Object { Copy-Item $_.FullName (Join-Path $Out "$tag-$($_.Name)") }
  }
  foreach ($n in 'cefmaps-status.txt', 'cefmaps.png') {
    Get-ChildItem -Path $env:USERPROFILE -Filter $n -Recurse -Depth 4 -ErrorAction SilentlyContinue |
      ForEach-Object { Copy-Item $_.FullName (Join-Path $Out "$tag-$n"); Remove-Item $_.FullName }
  }
  $jcef = Join-Path $env:USERPROFILE '.codenameone\jcef'
  if (Test-Path $jcef) {
    Get-ChildItem $jcef -Recurse -Depth 3 | Select-Object FullName, Length |
      Out-File (Join-Path $Out "$tag-jcef-tree.txt")
    Get-ChildItem $jcef -Filter 'debug.log' -Recurse -ErrorAction SilentlyContinue |
      ForEach-Object { Copy-Item $_.FullName (Join-Path $Out "$tag-cef-debug.log") }
  }
}

$setup = Get-ChildItem $InstallerDir -Filter *.exe -Recurse | Sort-Object Length -Descending | Select-Object -First 1
Write-Host "Installer: $($setup.FullName) ($($setup.Length) bytes)"
$p = Start-Process -FilePath $setup.FullName -PassThru -Wait -ArgumentList `
  '/VERYSILENT', '/SUPPRESSMSGBOXES', '/NORESTART', "/DIR=$app", "/LOG=$Out\install.log"
Write-Host "Installer exit: $($p.ExitCode)"
Get-ChildItem $app -Recurse -Depth 2 | Select-Object FullName, Length | Out-File "$Out\installed-tree.txt"

$exe = Get-ChildItem $app -Filter *.exe | Where-Object { $_.Name -notmatch '^unins' } | Select-Object -First 1
Write-Host "App: $($exe.FullName)"

# Run 1: the installed launcher, exactly as a user starts it.
$run = Start-Process -FilePath $exe.FullName -WorkingDirectory $app -PassThru -ArgumentList '--l4j-debug-all'
$exited = $run.WaitForExit(150000)
if (-not $exited) { Save-Screen 'launcher-hung.png'; Stop-Process -Id $run.Id -Force -ErrorAction SilentlyContinue }
Get-Process -Name java, javaw -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
"exited=$exited exitCode=$($run.ExitCode)" | Out-File "$Out\launcher-result.txt"
Get-ChildItem $app -Filter 'launch4j.log' | ForEach-Object { Copy-Item $_.FullName "$Out\launch4j.log" }
Collect 'launcher'

# Run 2: the same command line through the console java.exe, so stdout/stderr survive.
$l4j = Get-Content "$Out\launch4j.log" -ErrorAction SilentlyContinue
$cmd = $l4j | Where-Object { $_ -match '^Launcher:\s' } | Select-Object -Last 1
$largs = $l4j | Where-Object { $_ -match '^Launcher args:\s' } | Select-Object -Last 1
if (Test-Path "$Out\launcher-cefmaps-status.txt") {
  Write-Host 'The launcher run reported a status; no console replay needed.'
} elseif ($cmd -and $largs) {
  $java = ($cmd -replace '^Launcher:\s*', '').Trim('"') -replace 'javaw\.exe$', 'java.exe'
  $a = ($largs -replace '^Launcher args:\s*', '') -replace '--l4j-debug-all', ''
  Write-Host "Replaying: $java $a"
  $r = Start-Process -FilePath $java -ArgumentList $a -WorkingDirectory $app -PassThru `
    -RedirectStandardOutput "$Out\console-stdout.txt" -RedirectStandardError "$Out\console-stderr.txt"
  if (-not $r.WaitForExit(150000)) { Save-Screen 'console-hung.png'; Stop-Process -Id $r.Id -Force }
  "exitCode=$($r.ExitCode)" | Out-File "$Out\console-result.txt"
  Collect 'console'
} else {
  Write-Host 'launch4j.log has no command line to replay'
}

Get-ChildItem $Out | Select-Object Name, Length | Format-Table -AutoSize | Out-String | Write-Host
foreach ($f in 'launcher-cefmaps-status.txt', 'console-cefmaps-status.txt', 'launcher-result.txt', 'console-result.txt') {
  if (Test-Path "$Out\$f") { Write-Host "== $f"; Get-Content "$Out\$f" | Write-Host }
}

$status = "$Out\launcher-cefmaps-status.txt"
if (-not (Test-Path $status)) { Write-Error 'The installed app produced no status file.'; exit 1 }
if (-not (Select-String -Path $status -Pattern '^reason=tiles-loaded$' -Quiet)) {
  Write-Error 'The map did not finish loading in the installed app.'; exit 1
}
