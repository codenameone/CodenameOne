#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
WEBSITE_STATIC_DIR="${REPO_ROOT}/docs/website/static"
OTA_DIR="${WEBSITE_STATIC_DIR}/OTA"
RELEASES_URL="https://api.github.com/repos/codenameone/codenameone-skins/releases/latest"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required to download OTA skins." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to parse release metadata." >&2
  exit 1
fi

if ! command -v tar >/dev/null 2>&1; then
  echo "tar is required to extract OTA skins." >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

release_json="${tmp_dir}/release.json"
archive_file="${tmp_dir}/ota.tar.gz"
extract_dir="${tmp_dir}/extract"

curl_args=(
  -fsSL
  -H "Accept: application/vnd.github+json"
  -H "X-GitHub-Api-Version: 2022-11-28"
)
if [ -n "${GITHUB_TOKEN:-}" ]; then
  curl_args+=(-H "Authorization: Bearer ${GITHUB_TOKEN}")
fi

echo "Fetching latest codenameone-skins release metadata..." >&2
curl "${curl_args[@]}" -o "${release_json}" "${RELEASES_URL}"

asset_url="$(
  python3 -c '
import json, re, sys
with open(sys.argv[1], "r", encoding="utf-8") as f:
    payload = json.load(f)
assets = payload.get("assets", [])
matches = []
for asset in assets:
    name = asset.get("name", "")
    if re.match(r"^ota-.*\.tar\.gz$", name):
        matches.append((name, asset.get("browser_download_url", "")))
if not matches:
    raise SystemExit("No ota-*.tar.gz asset found in latest release.")
matches.sort(key=lambda pair: pair[0], reverse=True)
print(matches[0][1])
' "${release_json}"
)"

if [ -z "${asset_url}" ]; then
  echo "Resolved OTA asset URL is empty." >&2
  exit 1
fi

echo "Downloading OTA skins archive..." >&2
curl "${curl_args[@]}" -o "${archive_file}" "${asset_url}"

mkdir -p "${extract_dir}"
tar -xzf "${archive_file}" -C "${extract_dir}"

skins_path="$(find "${extract_dir}" -type f -name Skins.xml | head -n 1)"
if [ -z "${skins_path}" ]; then
  echo "Skins.xml not found in OTA archive." >&2
  exit 1
fi

payload_root="$(dirname "${skins_path}")"

rm -rf "${OTA_DIR}"
mkdir -p "${OTA_DIR}"

(
  shopt -s dotglob nullglob
  cp -a "${payload_root}"/* "${OTA_DIR}/"
)

if [ ! -f "${OTA_DIR}/Skins.xml" ]; then
  echo "Failed to stage OTA/Skins.xml at ${OTA_DIR}/Skins.xml." >&2
  exit 1
fi

echo "Staged OTA skins into ${OTA_DIR}" >&2
