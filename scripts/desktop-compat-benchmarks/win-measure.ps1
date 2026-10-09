# The Windows runtime half of the desktop-compat benchmarks.
#
#   win-measure.ps1 -Label jvm_default -Out parts\run.jvm_default.json `
#                   -Exe path\to\java.exe -Arguments '-cp','app\*','Main'
#
# UNVALIDATED OUTSIDE CI: nobody has a Windows desktop to run it on by hand, so
# the first numbers it produces are the workflow's. It writes the same shape of
# file x11bench.py does, with fewer fields, and report.py prints the missing
# ones as empty cells:
#
#   start-up     process start until the process has a main window (a window
#                handle, not a painted frame -- the earlier of the two events
#                the Linux leg reports), warm only
#   idle RAM     working set (rss_kb) and private bytes (uss_kb) after the idle
#                window; Windows has no proportional figure, so pss_kb is absent
#   idle CPU     processor time over the second half of the idle window
#   peak RSS     peak working set at the end of a session
#
# No scripted input: "after interaction" cells stay empty on Windows.
param(
    [Parameter(Mandatory = $true)][string]$Label,
    [Parameter(Mandatory = $true)][string]$Out,
    [Parameter(Mandatory = $true)][string]$Exe,
    [string[]]$Arguments = @(),
    [int]$Runs = 7,
    [int]$Sessions = 3,
    [double]$Idle = 10,
    [int]$TimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'

function Start-Measured([double]$IdleSeconds) {
    $info = New-Object System.Diagnostics.ProcessStartInfo
    $info.FileName = $Exe
    $info.Arguments = ($Arguments | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }) -join ' '
    $info.UseShellExecute = $false
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    $process = [System.Diagnostics.Process]::Start($info)
    $result = @{ ok = $false }
    try {
        while ($watch.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
            if ($process.HasExited) {
                $result.error = "exited with $($process.ExitCode) before showing a window"
                return $result
            }
            $process.Refresh()
            if ($process.MainWindowHandle -ne [IntPtr]::Zero) {
                $result.window_ms = [math]::Round($watch.Elapsed.TotalMilliseconds, 1)
                $result.ok = $true
                break
            }
            Start-Sleep -Milliseconds 4
        }
        if (-not $result.ok) {
            $result.error = "no window within $TimeoutSeconds s"
            return $result
        }
        if ($IdleSeconds -gt 0) {
            Start-Sleep -Milliseconds ([int]($IdleSeconds * 500))
            $process.Refresh()
            $before = $process.TotalProcessorTime.TotalSeconds
            $mark = [System.Diagnostics.Stopwatch]::StartNew()
            Start-Sleep -Milliseconds ([int]($IdleSeconds * 500))
            $process.Refresh()
            $cpu = 100.0 * ($process.TotalProcessorTime.TotalSeconds - $before) / $mark.Elapsed.TotalSeconds
            $result.idle = @{
                rss_kb      = [int64]($process.WorkingSet64 / 1024)
                uss_kb      = [int64]($process.PrivateMemorySize64 / 1024)
                threads     = $process.Threads.Count
                cpu_percent = [math]::Round($cpu, 2)
            }
            $result.peak_rss_kb = [int64]($process.PeakWorkingSet64 / 1024)
        }
        return $result
    }
    finally {
        if (-not $process.HasExited) {
            $process.Kill()
            $process.WaitForExit(15000) | Out-Null
        }
    }
}

function Get-Median($values) {
    $sorted = @($values | Sort-Object)
    if ($sorted.Count -eq 0) { return $null }
    $middle = [int][math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2 -eq 1) { return $sorted[$middle] }
    return ($sorted[$middle - 1] + $sorted[$middle]) / 2.0
}

Start-Measured 0 | Out-Null
$warm = @(1..$Runs | ForEach-Object { Start-Measured 0 })
$sessions = @(1..$Sessions | ForEach-Object { Start-Measured $Idle })
$good = @($sessions | Where-Object { $_.ok -and $_.idle })
$times = @($warm | Where-Object { $_.ok } | ForEach-Object { $_.window_ms })

$summary = $null
if ($times.Count -gt 0) {
    $summary = @{
        median = [math]::Round((Get-Median $times), 1)
        min    = ($times | Measure-Object -Minimum).Minimum
        max    = ($times | Measure-Object -Maximum).Maximum
        runs   = $times.Count
    }
}
$idleResult = @{}
foreach ($field in 'rss_kb', 'uss_kb', 'threads', 'cpu_percent') {
    $idleResult[$field] = Get-Median @($good | ForEach-Object { $_.idle[$field] })
}
$errors = @(($warm + $sessions) | Where-Object { $_.error } | ForEach-Object { $_.error })

$document = @{
    label        = $Label
    command      = @($Exe) + $Arguments
    idle_seconds = $Idle
    ok           = ($good.Count -gt 0)
    errors       = $errors
    startup_warm = @{ window_ms = $summary; first_paint_ms = $null }
    idle         = $idleResult
    peak_rss_kb  = Get-Median @($good | ForEach-Object { $_.peak_rss_kb })
}
$document | ConvertTo-Json -Depth 6 | Set-Content -Encoding ascii -Path $Out
Write-Host "${Label}: window $($summary.median) ms, idle RSS $($idleResult.rss_kb) kB, CPU $($idleResult.cpu_percent)%"
