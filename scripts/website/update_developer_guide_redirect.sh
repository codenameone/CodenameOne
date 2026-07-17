#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
REDIRECTS_FILE="${REDIRECTS_FILE:-${REPO_ROOT}/docs/website/static/_redirects}"
RELEASES_URL="${RELEASES_URL:-https://api.github.com/repos/codenameone/CodenameOne/releases/latest}"
RELEASES_LIST_URL="${RELEASES_LIST_URL:-https://api.github.com/repos/codenameone/CodenameOne/releases?per_page=20}"
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
releases_json="${tmp_dir}/releases.json"
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
print(f"{tag} {asset_url}")
' "${release_json}"
)"

if [ -z "${asset_url}" ]; then
  if [ "${GITHUB_EVENT_NAME:-}" = "pull_request" ]; then
    asset_url="https://github.com/codenameone/CodenameOne/releases/download/${release_tag}/developer-guide.pdf"
    echo "developer-guide.pdf asset not found in latest release; using PR fallback ${asset_url}" >&2
  else
    echo "developer-guide.pdf asset not found in latest release ${release_tag}; looking for the newest stable release that contains it." >&2
    curl "${curl_args[@]}" -o "${releases_json}" "${RELEASES_LIST_URL}"

    read -r fallback_tag fallback_url <<<"$(
      python3 -c '
import json, sys
with open(sys.argv[1], "r", encoding="utf-8") as f:
    releases = json.load(f)
for release in releases:
    if release.get("draft") or release.get("prerelease"):
        continue
    tag = release.get("tag_name", "")
    if not tag:
        continue
    for asset in release.get("assets", []):
        if asset.get("name") == "developer-guide.pdf":
            url = asset.get("browser_download_url", "")
            if url:
                print(f"{tag} {url}")
                raise SystemExit(0)
' "${releases_json}"
    )"

    if [ -z "${fallback_url:-}" ]; then
      echo "No stable Codename One release with a developer-guide.pdf asset was found." >&2
      echo "Resolved developer-guide.pdf URL is empty." >&2
      exit 1
    fi

    release_tag="${fallback_tag}"
    asset_url="${fallback_url}"
    echo "Using developer-guide.pdf from release ${release_tag} until the latest release asset is available." >&2
  fi
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
