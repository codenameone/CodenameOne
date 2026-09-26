# Installs a cloud-built Windows desktop installer where a user's installer puts it (under
# Program Files (x86), a path with spaces that a standard user cannot write to), launches
# the installed app through its launch4j exe, and collects what the app reports: its status
# file and screenshot, launch4j's log, CEF's debug log and any HotSpot crash log.
#
# The app is launched twice: as the runner's own (administrator) account, and as a standard
# local user whose profile path contains a space, which is where JCEF installs Chromium.
# When the first launch produces no status file, its command line is replayed through the
# console java.exe so the JVM's stdout/stderr are kept.
param([string]$Url, [string]$Out)
$ErrorActionPreference = 'Continue'
New-Item -ItemType Directory -Force -Path $Out | Out-Null
$Out = (Resolve-Path $Out).Path
$InstallerDir = Join-Path $env:RUNNER_TEMP 'installer'
New-Item -ItemType Directory -Force -Path $InstallerDir | Out-Null
$name = [System.IO.Path]::GetFileName(([uri]$Url).AbsolutePath)
Invoke-WebRequest -Uri $Url -OutFile (Join-Path $InstallerDir $name)
if ($name -like '*.zip') { Expand-Archive (Join-Path $InstallerDir $name) -DestinationPath $InstallerDir -Force }
$app = 'C:\Program Files (x86)\CefMaps Test'

function Save-Screen($name) {
  Add-Type -AssemblyName System.Windows.Forms, System.Drawing
  $b = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
  $bmp = New-Object System.Drawing.Bitmap $b.Width, $b.Height
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.CopyFromScreen($b.Location, [System.Drawing.Point]::Empty, $b.Size)
  $bmp.Save((Join-Path $Out $name))
}

function Collect($tag, $userProfile) {
  foreach ($d in @($app, $userProfile)) {
    Get-ChildItem -Path $d -Filter 'hs_err_pid*.log' -Recurse -Depth 4 -ErrorAction SilentlyContinue |
      ForEach-Object { Copy-Item $_.FullName (Join-Path $Out "$tag-$($_.Name)"); Remove-Item $_.FullName }
  }
  foreach ($n in 'cefmaps-status.txt', 'cefmaps.png') {
    Get-ChildItem -Path $userProfile -Filter $n -Recurse -Depth 4 -ErrorAction SilentlyContinue |
      ForEach-Object { Copy-Item $_.FullName (Join-Path $Out "$tag-$n"); Remove-Item $_.FullName }
  }
  $jcef = Join-Path $userProfile '.codenameone\jcef'
  if (Test-Path $jcef) {
    Get-ChildItem $jcef -Recurse -Depth 3 | Select-Object FullName, Length |
      Out-File (Join-Path $Out "$tag-jcef-tree.txt")
  }
  foreach ($d in @($app, $jcef)) {
    Get-ChildItem -Path $d -Filter 'debug.log' -Recurse -Depth 4 -ErrorAction SilentlyContinue |
      ForEach-Object { Copy-Item $_.FullName (Join-Path $Out "$tag-cef-debug.log"); Remove-Item $_.FullName }
  }
}

# Starts the installed exe and waits for the app's own JVM: launch4j's gui header starts
# javaw.exe and exits without waiting for it.
function Run-Launcher($tag, $userProfile, $credential) {
  $startArgs = @{ FilePath = $exe.FullName; WorkingDirectory = $app; PassThru = $true; ArgumentList = '--l4j-debug-all' }
  if ($credential) { $startArgs.Credential = $credential; $startArgs.LoadUserProfile = $true }
  $run = Start-Process @startArgs
  $run.WaitForExit(30000) | Out-Null
  $deadline = (Get-Date).AddSeconds(150)
  $jvm = $null
  while ((Get-Date) -lt $deadline) {
    $jvm = Get-Process -Name javaw -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jvm) { break }
    Start-Sleep -Milliseconds 500
  }
  $exited = $false
  if ($jvm) {
    $exited = $jvm.WaitForExit([int][Math]::Max(1000, ($deadline - (Get-Date)).TotalMilliseconds))
    if (-not $exited) { Save-Screen "$tag-hung.png" }
  }
  Get-Process -Name java, javaw -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
  "launcherExitCode=$($run.ExitCode) jvmSeen=$([bool]$jvm) jvmExited=$exited" | Out-File "$Out\$tag-result.txt"
  Get-ChildItem $app -Filter 'launch4j.log' -ErrorAction SilentlyContinue |
    ForEach-Object { Copy-Item $_.FullName "$Out\$tag-launch4j.log"; Remove-Item $_.FullName }
  Collect $tag $userProfile
}

$setup = Get-ChildItem $InstallerDir -Filter *.exe -Recurse | Sort-Object Length -Descending | Select-Object -First 1
Write-Host "Installer: $($setup.FullName) ($($setup.Length) bytes)"
$p = Start-Process -FilePath $setup.FullName -PassThru -Wait -ArgumentList `
  '/VERYSILENT', '/SUPPRESSMSGBOXES', '/NORESTART', "/DIR=$app", "/LOG=$Out\install.log"
Write-Host "Installer exit: $($p.ExitCode)"
Get-ChildItem $app -Recurse -Depth 2 | Select-Object FullName, Length | Out-File "$Out\installed-tree.txt"
$exe = Get-ChildItem $app -Filter *.exe | Where-Object { $_.Name -notmatch '^unins' } | Select-Object -First 1
Write-Host "App: $($exe.FullName)"

# Run 1: the runner's own account.
Run-Launcher 'admin' $env:USERPROFILE $null

# The same command line through the console java.exe, only when run 1 reported nothing.
$l4j = Get-Content "$Out\admin-launch4j.log" -ErrorAction SilentlyContinue
$cmd = $l4j | Where-Object { $_ -match '^Launcher:\s' } | Select-Object -Last 1
$largs = $l4j | Where-Object { $_ -match '^Launcher args:\s' } | Select-Object -Last 1
if (Test-Path "$Out\admin-cefmaps-status.txt") {
  Write-Host 'The admin run reported a status; no console replay needed.'
} elseif ($cmd -and $largs) {
  $java = ($cmd -replace '^Launcher:\s*', '').Trim('"') -replace 'javaw\.exe$', 'java.exe'
  $a = ($largs -replace '^Launcher args:\s*', '') -replace '--l4j-debug-all', ''
  Write-Host "Replaying: $java $a"
  $r = Start-Process -FilePath $java -ArgumentList $a -WorkingDirectory $app -PassThru `
    -RedirectStandardOutput "$Out\console-stdout.txt" -RedirectStandardError "$Out\console-stderr.txt"
  if (-not $r.WaitForExit(150000)) { Save-Screen 'console-hung.png'; Stop-Process -Id $r.Id -Force }
  "exitCode=$($r.ExitCode)" | Out-File "$Out\console-result.txt"
  Collect 'console' $env:USERPROFILE
}

# Run 2: a standard (non-administrator) user whose profile path contains a space.
$userName = 'CN1 Test'
$password = ConvertTo-SecureString ('Cn1!' + [guid]::NewGuid().ToString('N')) -AsPlainText -Force
New-LocalUser -Name $userName -Password $password -PasswordNeverExpires | Out-Null
$credential = New-Object System.Management.Automation.PSCredential($userName, $password)
Run-Launcher 'user' "C:\Users\$userName" $credential

Get-ChildItem $Out | Select-Object Name, Length | Format-Table -AutoSize | Out-String | Write-Host
$failed = $false
foreach ($tag in 'admin', 'user') {
  foreach ($f in "$tag-result.txt", "$tag-cefmaps-status.txt") {
    if (Test-Path "$Out\$f") { Write-Host "== $f"; Get-Content "$Out\$f" | Write-Host }
  }
  $status = "$Out\$tag-cefmaps-status.txt"
  if (-not (Test-Path $status)) {
    Write-Host "::error::The $tag run of the installed app produced no status file."; $failed = $true
  } elseif (-not (Select-String -Path $status -Pattern '^reason=(tiles-loaded|page-complete)$' -Quiet)) {
    Write-Host "::error::The map did not finish loading in the $tag run of the installed app."; $failed = $true
  }
}
if ($failed) { exit 1 }
