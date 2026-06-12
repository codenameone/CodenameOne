#!/usr/bin/env bash
# Portable (Linux / macOS) fetch of the Microsoft.Web.WebView2 NuGet SDK, the
# bash counterpart of fetch-webview2-sdk.ps1 for the Linux cross-build runner.
# The SDK is a plain NuGet package (a zip of headers + the static loader libs);
# there is nothing Windows-specific about downloading it, so a cross-compile host
# can obtain the exact same headers + x64/arm64 WebView2LoaderStatic.lib the
# Windows runners use. lld-link links the Windows static lib like any other.
#
# Downloads the latest stable version, lays out build/native (include/ + x64/ +
# arm64/) and prints the resolved directory as the final stdout line:
#     WEBVIEW2_SDK_DIR=<path>
# so a workflow can capture it into $GITHUB_ENV. All progress goes to stderr.
#
# Usage: fetch-webview2-sdk.sh [destination-dir]
#   destination-dir defaults to $WEBVIEW2_DEST, else $RUNNER_TEMP/webview2sdk,
#   else /tmp/webview2sdk.
set -euo pipefail

dst="${1:-${WEBVIEW2_DEST:-${RUNNER_TEMP:-/tmp}/webview2sdk}}"
mkdir -p "$dst"

idx_url="https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/index.json"
echo "Fetching WebView2 SDK version index..." >&2
# The flatcontainer index lists versions ascending. Match only fully-numeric
# quoted version tokens ("1.0.x.y") so prerelease versions ("1.0.x-prerelease",
# whose '-' breaks the pattern before the closing quote) are excluded; take the
# last == latest stable. No jq/python dependency.
ver="$(curl -fsSL "$idx_url" | tr ',' '\n' | grep -oE '"[0-9][0-9.]*"' | tr -d '"' | tail -1 || true)"
if [ -z "$ver" ]; then
  echo "ERROR: could not resolve a stable WebView2 SDK version from $idx_url" >&2
  exit 1
fi
echo "WebView2 SDK latest stable: $ver" >&2

nupkg="$dst/webview2.nupkg"
curl -fsSL -o "$nupkg" \
  "https://api.nuget.org/v3-flatcontainer/microsoft.web.webview2/$ver/microsoft.web.webview2.$ver.nupkg"

rm -rf "$dst/pkg"
mkdir -p "$dst/pkg"
unzip -q -o "$nupkg" -d "$dst/pkg"

hdr="$(find "$dst/pkg" -name WebView2.h -print -quit)"
if [ -z "$hdr" ]; then
  echo "ERROR: WebView2.h not found in the downloaded package" >&2
  exit 1
fi
# include/WebView2.h -> include -> build/native (the dir holding include/ + x64/)
native_dir="$(cd "$(dirname "$hdr")/.." && pwd)"
echo "WebView2.h: $hdr" >&2
find "$dst/pkg" -name WebView2LoaderStatic.lib -print >&2 || true
if [ ! -f "$native_dir/x64/WebView2LoaderStatic.lib" ]; then
  echo "ERROR: x64/WebView2LoaderStatic.lib missing under $native_dir" >&2
  exit 1
fi

echo "WEBVIEW2_SDK_DIR=$native_dir"
