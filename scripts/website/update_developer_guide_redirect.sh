#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
REDIRECTS_FILE="${REPO_ROOT}/docs/website/static/_redirects"
RELEASES_URL="https://api.github.com/repos/codenameone/CodenameOne/releases/latest"
BEGIN_MARKER="# BEGIN: generated developer-guide.pdf redirect"
END_MARKER="# END: generated developer-guide.pdf redirect"

if [ ! -f "${REDIRECTS_FILE}" ]; then
  echo "Redirect file not found: ${REDIRECTS_FILE}" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required to fetch release metadata." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to parse release metadata." >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

release_json="${tmp_dir}/release.json"
new_redirects="${tmp_dir}/_redirects.new"

curl_args=(
  -fsSL
  -H "Accept: application/vnd.github+json"
  -H "X-GitHub-Api-Version: 2022-11-28"
)
if [ -n "${GITHUB_TOKEN:-}" ]; then
  curl_args+=(-H "Authorization: Bearer ${GITHUB_TOKEN}")
fi

echo "Fetching latest CodenameOne release metadata..." >&2
curl "${curl_args[@]}" -o "${release_json}" "${RELEASES_URL}"

read -r release_tag asset_url <<<"$(
  python3 -c '
import json, sys
with open(sys.argv[1], "r", encoding="utf-8") as f:
    payload = json.load(f)
tag = payload.get("tag_name", "")
if not tag:
    raise SystemExit("Latest release tag_name is missing.")
assets = payload.get("assets", [])
asset_url = ""
for asset in assets:
    if asset.get("name") == "developer-guide.pdf":
        asset_url = asset.get("browser_download_url", "")
        break
if not asset_url:
    raise SystemExit("developer-guide.pdf asset not found in latest release.")
print(f"{tag} {asset_url}")
' "${release_json}"
)"

if [ -z "${asset_url}" ]; then
  echo "Resolved developer-guide.pdf URL is empty." >&2
  exit 1
fi

awk -v begin="${BEGIN_MARKER}" -v end="${END_MARKER}" '
  $0 == begin { skip = 1; next }
  $0 == end { skip = 0; next }
  !skip { print }
' "${REDIRECTS_FILE}" > "${new_redirects}"

tmp_body="${tmp_dir}/_redirects.body"
cp "${new_redirects}" "${tmp_body}"

{
  printf '%s\n' "${BEGIN_MARKER}"
  printf '# Source release: %s\n' "${release_tag}"
  printf '/files/developer-guide.pdf %s 302\n' "${asset_url}"
  printf '/files/developer-guide.pdf/ %s 302\n' "${asset_url}"
  printf '%s\n\n' "${END_MARKER}"
  cat "${tmp_body}"
} > "${new_redirects}"

mv "${new_redirects}" "${REDIRECTS_FILE}"

if ! grep -Fq "/files/developer-guide.pdf ${asset_url} 302" "${REDIRECTS_FILE}"; then
  echo "Failed to write developer-guide redirect into ${REDIRECTS_FILE}" >&2
  exit 1
fi

echo "Updated redirect: /files/developer-guide.pdf -> ${asset_url}" >&2
