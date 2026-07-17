#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UPDATER="${SCRIPT_DIR}/update_developer_guide_redirect.sh"

# GitHub sets this to pull_request while the website PR check runs. These
# fixtures validate production behavior, so do not inherit the outer event.
export GITHUB_EVENT_NAME=workflow_dispatch

tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

write_redirects() {
  cat > "$1" <<'REDIRECTS'
# Existing redirect content
/manual /developer-guide/ 301
REDIRECTS
}

cat > "${tmp_dir}/latest-with-pdf.json" <<'JSON'
{
  "tag_name": "7.0.260",
  "assets": [
    {
      "name": "developer-guide.pdf",
      "browser_download_url": "https://github.com/codenameone/CodenameOne/releases/download/7.0.260/developer-guide.pdf"
    }
  ]
}
JSON

cat > "${tmp_dir}/latest-without-pdf.json" <<'JSON'
{
  "tag_name": "7.0.260",
  "assets": []
}
JSON

cat > "${tmp_dir}/release-history.json" <<'JSON'
[
  {
    "tag_name": "7.0.260",
    "draft": false,
    "prerelease": false,
    "assets": []
  },
  {
    "tag_name": "7.0.259-rc1",
    "draft": false,
    "prerelease": true,
    "assets": [
      {
        "name": "developer-guide.pdf",
        "browser_download_url": "https://github.com/codenameone/CodenameOne/releases/download/7.0.259-rc1/developer-guide.pdf"
      }
    ]
  },
  {
    "tag_name": "7.0.259",
    "draft": false,
    "prerelease": false,
    "assets": [
      {
        "name": "developer-guide.pdf",
        "browser_download_url": "https://github.com/codenameone/CodenameOne/releases/download/7.0.259/developer-guide.pdf"
      }
    ]
  }
]
JSON

cat > "${tmp_dir}/no-release-with-pdf.json" <<'JSON'
[
  {
    "tag_name": "7.0.260",
    "draft": false,
    "prerelease": false,
    "assets": []
  }
]
JSON

latest_redirects="${tmp_dir}/latest-redirects"
write_redirects "${latest_redirects}"
REDIRECTS_FILE="${latest_redirects}" \
RELEASES_URL="file://${tmp_dir}/latest-with-pdf.json" \
RELEASES_LIST_URL="file://${tmp_dir}/missing-release-history.json" \
  "${UPDATER}"
grep -Fq '# Source release: 7.0.260' "${latest_redirects}"
grep -Fq '/files/developer-guide.pdf https://github.com/codenameone/CodenameOne/releases/download/7.0.260/developer-guide.pdf 302' "${latest_redirects}"
grep -Fq '/manual /developer-guide/ 301' "${latest_redirects}"

fallback_redirects="${tmp_dir}/fallback-redirects"
write_redirects "${fallback_redirects}"
REDIRECTS_FILE="${fallback_redirects}" \
RELEASES_URL="file://${tmp_dir}/latest-without-pdf.json" \
RELEASES_LIST_URL="file://${tmp_dir}/release-history.json" \
  "${UPDATER}"
grep -Fq '# Source release: 7.0.259' "${fallback_redirects}"
grep -Fq '/files/developer-guide.pdf https://github.com/codenameone/CodenameOne/releases/download/7.0.259/developer-guide.pdf 302' "${fallback_redirects}"
if grep -Fq '7.0.259-rc1/developer-guide.pdf' "${fallback_redirects}"; then
  echo "Prerelease developer guide was selected as the production fallback." >&2
  exit 1
fi

missing_redirects="${tmp_dir}/missing-redirects"
write_redirects "${missing_redirects}"
if REDIRECTS_FILE="${missing_redirects}" \
    RELEASES_URL="file://${tmp_dir}/latest-without-pdf.json" \
    RELEASES_LIST_URL="file://${tmp_dir}/no-release-with-pdf.json" \
    "${UPDATER}"; then
  echo "Updater succeeded without finding a valid developer-guide.pdf asset." >&2
  exit 1
fi
if ! cmp -s "${missing_redirects}" <(printf '%s\n' '# Existing redirect content' '/manual /developer-guide/ 301'); then
  echo "Updater modified redirects after failing to resolve an asset." >&2
  exit 1
fi

echo "Developer guide redirect updater tests passed."
