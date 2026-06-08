# Codename One Windows build-VM toolchain bootstrap.
#
# Run ONCE in an ELEVATED (Administrator) PowerShell inside the Windows build VM:
#
#     powershell -ExecutionPolicy Bypass -File C:\Users\shai\cn1-bootstrap.ps1
#
# (The repo is read live from the Parallels shared folder Y: == the Mac working
# tree; this script is copied to the local profile so the elevated session can
# see it, since mapped drives are not visible across the UAC boundary.)
#
# Installs everything needed to build the native Windows port from ParparVM's
# "windows" clean target:
#   * Visual Studio Build Tools: MSVC ARM64 + x64 toolsets, Windows SDK,
#     clang-cl (LLVM), CMake + Ninja -- installed by driving vs_installer.exe
#     directly with --wait, which actually lays the components down (the
#     Chocolatey workload package's --add passthrough is unreliable and can
#     report success without installing anything).
#   * CMake / Ninja / Maven / Temurin JDK 17 on PATH via Chocolatey.
# clang-cl needs the MSVC CRT + Windows SDK to link, which is why the C++ Build
# Tools are required. The host is Windows on ARM (Apple Silicon + Parallels), so
# the C/C++ toolchain is installed for ARM64; CMake/Ninja/Maven/JDK only
# orchestrate or run the JVM, so x64-under-emulation builds of those are fine.
#
# Idempotent: safe to re-run. Builds are then driven from the Mac via
#   prlctl exec "Windows 11" --current-user ...

$ErrorActionPreference = 'Stop'
Write-Host '== Codename One Windows VM toolchain bootstrap =='

# --- Chocolatey -----------------------------------------------------------
if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host 'Installing Chocolatey...'
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-Expression ((New-Object Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    $env:Path += ";$env:ProgramData\chocolatey\bin"
}
choco feature enable -n allowGlobalConfirmation | Out-Null

# --- Visual Studio Build Tools shell (provides vs_installer.exe) ----------
if (-not (Test-Path "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vs_installer.exe")) {
    Write-Host 'Installing Visual Studio Build Tools shell...'
    choco install visualstudio2022buildtools -y --no-progress
}

# --- Install the real C++ components by driving the VS bootstrapper --------
# The bootstrapper (vs_BuildTools.exe) supports --wait, which blocks until the
# components are actually downloaded and installed, and modifies the existing
# instance via --installPath. (vs_installer.exe does NOT accept --wait and
# returns 87/invalid-parameter; the Chocolatey workload package's --add
# passthrough silently installs nothing -- hence driving the bootstrapper.)
# Exit 0/3010/1641 all mean success (3010/1641 = reboot advisable, bits placed).
$buildToolsPath = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\2022\BuildTools"
$bootstrapper = Join-Path $env:TEMP 'vs_BuildTools.exe'
Write-Host 'Downloading VS Build Tools bootstrapper...'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri 'https://aka.ms/vs/17/release/vs_BuildTools.exe' -OutFile $bootstrapper
$components = @(
    'Microsoft.VisualStudio.Workload.VCTools',
    'Microsoft.VisualStudio.Component.VC.Tools.ARM64',
    'Microsoft.VisualStudio.Component.VC.Tools.x86.x64',
    'Microsoft.VisualStudio.Component.VC.Llvm.Clang',
    'Microsoft.VisualStudio.Component.VC.Llvm.ClangToolset',
    'Microsoft.VisualStudio.Component.Windows11SDK.22621',
    'Microsoft.VisualStudio.Component.VC.CMake.Project'
)
$installArgs = @('--installPath', $buildToolsPath)
foreach ($c in $components) { $installArgs += '--add'; $installArgs += $c }
$installArgs += @('--includeRecommended', '--quiet', '--wait', '--norestart')
Write-Host 'Installing VC++ components (MSVC ARM64/x64 + clang-cl + Windows SDK + CMake/Ninja)...'
Write-Host '(this downloads several GB and can take 20-40 min)'
$p = Start-Process -FilePath $bootstrapper -ArgumentList $installArgs -Wait -PassThru
if ($p.ExitCode -notin 0, 3010, 1641) {
    throw "VS bootstrapper failed with exit code $($p.ExitCode). See %ProgramData%\Microsoft\VisualStudio\Packages\_Instances logs."
}
Write-Host "VC++ components installed (bootstrapper exit $($p.ExitCode))."

# --- Stand-alone CMake / Ninja / Maven / JDK 17 ---------------------------
Write-Host 'Installing CMake, Ninja, Maven, Temurin JDK 17...'
choco install cmake ninja maven temurin17 -y --no-progress

# --- JDK env vars (CompilerHelper reads JDK_*_HOME on Windows) ------------
$adoptium = 'C:\Program Files\Eclipse Adoptium'
if (Test-Path $adoptium) {
    $jdk17 = Get-ChildItem $adoptium -Directory -Filter 'jdk-17*' | Select-Object -First 1
    if ($jdk17) {
        [Environment]::SetEnvironmentVariable('JDK_17_HOME', $jdk17.FullName, 'Machine')
        [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk17.FullName, 'Machine')
        Write-Host "JDK_17_HOME = $($jdk17.FullName)"
    }
}

# --- Verify ---------------------------------------------------------------
Write-Host ''
Write-Host '== Verifying =='
$clang = Get-ChildItem "$buildToolsPath\VC\Tools\Llvm" -Recurse -Filter clang-cl.exe -ErrorAction SilentlyContinue | Select-Object -First 1
if ($clang) { Write-Host "clang-cl: $($clang.FullName)" } else { Write-Warning 'clang-cl NOT found' }
$msvc = Get-ChildItem "$buildToolsPath\VC\Tools\MSVC" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($msvc) { Write-Host "MSVC toolset: $($msvc.Name)" } else { Write-Warning 'MSVC toolset NOT found' }
if (Test-Path 'C:\Program Files (x86)\Windows Kits\10\Include') { Write-Host 'Windows SDK: present' } else { Write-Warning 'Windows SDK NOT found' }

Write-Host ''
Write-Host '== Toolchain install complete =='
Write-Host 'Open a NEW shell so PATH/env refresh. Builds are driven from the Mac.'
