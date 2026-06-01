# Codename One Windows build-VM toolchain bootstrap.
#
# Run ONCE in an ELEVATED (Administrator) PowerShell inside the Windows build VM:
#
#     powershell -ExecutionPolicy Bypass -File Y:\scripts\windows\bootstrap-vm-toolchain.ps1
#
# (Y: is the Parallels shared folder that maps to the Mac working tree, so there
# is nothing to clone -- the repo is read live from Y:\.)
#
# Installs everything needed to build the native Windows port from ParparVM's
# "windows" clean target: Visual Studio Build Tools (MSVC ARM64 toolset, Windows
# SDK, clang-cl, CMake, Ninja) plus a JDK and Maven. clang-cl needs the MSVC CRT
# and Windows SDK to link, which is why the C++ Build Tools are required.
#
# The host is Windows on ARM (Apple Silicon + Parallels); the C/C++ toolchain is
# installed for ARM64 so the produced .exe runs natively. CMake/Ninja/Maven/JDK
# only orchestrate the build or run the JVM, so an x64-under-emulation build of
# those is fine.
#
# After it finishes, open a NEW shell so the updated PATH / environment is in
# effect. The actual builds are then driven from the Mac via
#   prlctl exec "Windows 11" --current-user ...
# which runs as the logged-in user and can see Y:\.

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

# --- Visual Studio Build Tools: MSVC ARM64 + Windows SDK + clang-cl + CMake/Ninja
Write-Host 'Installing Visual Studio Build Tools (C++ / ARM64 / clang-cl)...'
choco install visualstudio2022buildtools -y --no-progress
choco install visualstudio2022-workload-vctools -y --no-progress --package-parameters `
  "--add Microsoft.VisualStudio.Component.VC.Tools.ARM64 --add Microsoft.VisualStudio.Component.VC.Tools.x86.x64 --add Microsoft.VisualStudio.Component.VC.Llvm.Clang --add Microsoft.VisualStudio.Component.VC.Llvm.ClangToolset --add Microsoft.VisualStudio.Component.VC.CMake.Project --includeRecommended"

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

# --- Report the VS developer environment used by the build step -----------
$vswhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
if (Test-Path $vswhere) {
    $vsdev = & $vswhere -latest -products * -find 'Common7\Tools\VsDevCmd.bat' 2>$null
    Write-Host "VsDevCmd.bat = $vsdev"
}

Write-Host ''
Write-Host '== Toolchain install complete =='
Write-Host 'Open a NEW shell so PATH/env refresh. Builds are driven from the Mac.'
