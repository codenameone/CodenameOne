# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# Windows twin of create-app.sh: create a Codename One project with nothing
# installed first.
#
#   & ([scriptblock]::Create((irm https://www.codenameone.com/agents/create-app.ps1))) `
#       --name HabitTracker --package com.acme.habits
#   create-app.ps1 --list
#   create-app.ps1 --jdk-only
#
# Finds a JDK 17+ (JAVA_HOME, then java on PATH), else downloads Temurin 17 into
# $env:CN1_AGENT_HOME\jdk (default %USERPROFILE%\.codenameone\agent) with no
# admin rights; downloads the generator once per release; runs it. The last
# line printed on success is JAVA_HOME=<path>.
$ErrorActionPreference = 'Stop'
# Windows PowerShell 5.1 redraws a progress bar per chunk, which makes a JDK
# download take minutes instead of seconds.
$ProgressPreference = 'SilentlyContinue'
$Site = if ($env:CN1_AGENT_SITE) { $env:CN1_AGENT_SITE } else { 'https://www.codenameone.com/agents' }
$Repo = if ($env:CN1_MAVEN_REPO) { $env:CN1_MAVEN_REPO } else { 'https://repo.codenameone.com/maven2' }
$HomeDir = if ($env:CN1_AGENT_HOME) { $env:CN1_AGENT_HOME } else { Join-Path $env:USERPROFILE '.codenameone\agent' }
if (-not $env:CN1_TELEMETRY) { $env:CN1_TELEMETRY = '0' }

function Get-JavaMajor([string]$jdk) {
    $exe = Join-Path $jdk 'bin\java.exe'
    if (-not (Test-Path $exe) -or -not (Test-Path (Join-Path $jdk 'bin\javac.exe'))) { return 0 }
    $out = & $exe -version 2>&1 | Out-String
    if ($out -match 'version "(\d+)') { return [int]$Matches[1] }
    return 0
}

function Find-Jdk {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) { $candidates += (Split-Path (Split-Path $java.Source)) }
    $local = Join-Path $HomeDir 'jdk'
    if (Test-Path $local) { $candidates += (Get-ChildItem $local -Directory | ForEach-Object { $_.FullName }) }
    foreach ($c in $candidates) { if ((Get-JavaMajor $c) -ge 17) { return $c } }
    return $null
}

$jdk = Find-Jdk
if (-not $jdk) {
    $arch = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { 'aarch64' } else { 'x64' }
    $url = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/$arch/jdk/hotspot/normal/eclipse"
    $dest = Join-Path $HomeDir 'jdk'
    New-Item -ItemType Directory -Force $dest | Out-Null
    $zip = Join-Path $dest 'download.zip'
    Write-Host "create-app: no JDK 17+ found; downloading Temurin 17 for windows/$arch into $dest"
    Invoke-WebRequest -UseBasicParsing $url -OutFile $zip
    Expand-Archive -Force $zip $dest
    Remove-Item $zip
    $jdk = Find-Jdk
    if (-not $jdk) { throw "create-app: downloaded a JDK but found no bin\java.exe in $dest" }
}
Write-Host "create-app: using JDK at $jdk"
if ($args.Count -gt 0 -and $args[0] -eq '--jdk-only') { Write-Output "JAVA_HOME=$jdk"; exit 0 }

New-Item -ItemType Directory -Force $HomeDir | Out-Null
$props = (Invoke-WebRequest -UseBasicParsing "$Site/cn1-create.properties").Content
if ($props -is [byte[]]) { $props = [Text.Encoding]::UTF8.GetString($props) }
$version = ([regex]::Match($props, 'cn1\.version=(\S+)')).Groups[1].Value
$kit = ([regex]::Match($props, 'kit\.build=(\S+)')).Groups[1].Value
if (-not $version) { throw "create-app: no cn1.version in $Site/cn1-create.properties" }
if (-not $kit) { $kit = '0' }
$lib = Join-Path $HomeDir "lib\$version-$kit"
if (-not (Test-Path (Join-Path $lib '.complete'))) {
    Write-Host "create-app: downloading the project generator for Codename One $version"
    New-Item -ItemType Directory -Force $lib | Out-Null
    Invoke-WebRequest -UseBasicParsing "$Site/cn1-create.jar" -OutFile (Join-Path $lib 'cn1-create.jar')
    foreach ($a in 'codenameone-core', 'codenameone-javase') {
        Invoke-WebRequest -UseBasicParsing "$Repo/com/codenameone/$a/$version/$a-$version.jar" -OutFile (Join-Path $lib "$a.jar")
    }
    New-Item -ItemType File (Join-Path $lib '.complete') | Out-Null
}
$cpEntries = @('cn1-create.jar', 'codenameone-core.jar', 'codenameone-javase.jar' | ForEach-Object { Join-Path $lib $_ })
# A complete-app template is its own download (templates/<name>.jar), fetched
# only when asked for and cached under its own hash.
$template = ''
for ($i = 0; $i -lt $args.Count - 1; $i++) { if ($args[$i] -eq '--template') { $template = "$($args[$i + 1])".ToLowerInvariant() } }
if ($template) {
    $hash = ([regex]::Match($props, "template\.$([regex]::Escape($template))=(\S+)")).Groups[1].Value
    if ($hash) {
        $tdir = Join-Path $HomeDir 'templates'
        New-Item -ItemType Directory -Force $tdir | Out-Null
        $tjar = Join-Path $tdir "$template-$hash.jar"
        if (-not (Test-Path $tjar)) {
            Write-Host "create-app: downloading the $template template"
            Invoke-WebRequest -UseBasicParsing "$Site/templates/$template.jar" -OutFile "$tjar.part"
            Move-Item -Force "$tjar.part" $tjar
        }
        $cpEntries += $tjar
    }
}
$cp = $cpEntries -join ';'
& (Join-Path $jdk 'bin\java.exe') '-Djava.awt.headless=true' -cp $cp com.codename1.initializr.model.CreateProject @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output "JAVA_HOME=$jdk"
