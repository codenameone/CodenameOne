$ErrorActionPreference = "Stop"
$dst = "C:\webview2sdk"
$lastError = $null

for ($attempt = 1; $attempt -le 3; $attempt++) {
    try {
        $idx = Invoke-RestMethod "https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/index.json"
        $ver = ($idx.versions | Where-Object { $_ -notmatch "-" } | Select-Object -Last 1)
        if (!$ver) { throw "NuGet returned no stable WebView2 SDK version" }

        Write-Output ("WebView2 SDK latest stable: " + $ver)
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        $nupkg = Join-Path $dst "webview2.nupkg"
        Invoke-WebRequest "https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/$ver/microsoft.web.webview2.$ver.nupkg" -OutFile $nupkg

        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $packageDir = Join-Path $dst "pkg"
        if (Test-Path $packageDir) { Remove-Item -Recurse -Force $packageDir }
        [System.IO.Compression.ZipFile]::ExtractToDirectory($nupkg, $packageDir)

        $hdr = Get-ChildItem -Path $packageDir -Recurse -Filter "WebView2.h" | Select-Object -First 1
        $libs = @(Get-ChildItem -Path $packageDir -Recurse -Filter "WebView2LoaderStatic.lib")
        if (!$hdr) { throw "Downloaded WebView2 package contains no WebView2.h" }
        if ($libs.Count -eq 0) { throw "Downloaded WebView2 package contains no static loader library" }

        Write-Output ("WebView2.h: " + $hdr.FullName)
        foreach ($lib in $libs) { Write-Output ("LoaderStatic: " + $lib.FullName) }
        return
    } catch {
        $lastError = $_
        Write-Warning "WebView2 SDK fetch attempt $attempt of 3 failed: $($_.Exception.Message)"
        if ($attempt -lt 3) { Start-Sleep -Seconds (5 * $attempt) }
    }
}

throw "Failed to fetch and validate the WebView2 SDK after 3 attempts: $lastError"
