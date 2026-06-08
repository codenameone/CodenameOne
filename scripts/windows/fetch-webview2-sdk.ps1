$ErrorActionPreference = "Stop"
$idx = Invoke-RestMethod "https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/index.json"
$ver = ($idx.versions | Where-Object { $_ -notmatch "-" } | Select-Object -Last 1)
Write-Output ("WebView2 SDK latest stable: " + $ver)
$dst = "C:\webview2sdk"
New-Item -ItemType Directory -Force -Path $dst | Out-Null
$nupkg = Join-Path $dst "webview2.nupkg"
Invoke-WebRequest "https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/$ver/microsoft.web.webview2.$ver.nupkg" -OutFile $nupkg
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path (Join-Path $dst "pkg")) { Remove-Item -Recurse -Force (Join-Path $dst "pkg") }
[System.IO.Compression.ZipFile]::ExtractToDirectory($nupkg, (Join-Path $dst "pkg"))
$hdr = Get-ChildItem -Path (Join-Path $dst "pkg") -Recurse -Filter "WebView2.h" | Select-Object -First 1
$libs = Get-ChildItem -Path (Join-Path $dst "pkg") -Recurse -Filter "WebView2LoaderStatic.lib"
Write-Output ("WebView2.h: " + $hdr.FullName)
foreach ($l in $libs) { Write-Output ("LoaderStatic: " + $l.FullName) }
