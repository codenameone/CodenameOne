#!/usr/bin/env bash
#
# Assert the API URLs resolve on the Cloudflare Pages runtime, not merely on disk.
#
# This exists because everything else passed while the site was broken. The
# parity gate compares files, the link gate reads href attributes, and both were
# green on a build where clicking any class from the overview left the site
# entirely: Pages redirects /x.html to /x before an asset is considered
# (html_handling, confirmed with an empty project and no _redirects at all), the
# site's redirect table maps /*.html to /:splat/ on top of that, and the page
# published at the extension was one nobody could reach. Only asking the server
# shows that.
#
# Usage:
#   scripts/website/check-javadoc-urls.sh <public-dir> [port]
set -euo pipefail

PUBLIC_DIR="${1:?usage: check-javadoc-urls.sh <public-dir> [port]}"
PORT="${2:-8788}"
WRANGLER_VERSION="${WRANGLER_VERSION:-4.129.0}"

if [ ! -d "${PUBLIC_DIR}/javadoc" ]; then
  echo "check-javadoc-urls: ${PUBLIC_DIR}/javadoc does not exist; was the API generated?" >&2
  exit 1
fi

npx --yes "wrangler@${WRANGLER_VERSION}" pages dev "${PUBLIC_DIR}" \
  --port "${PORT}" --log-level error >/tmp/check-javadoc-urls.log 2>&1 &
server=$!
trap 'kill "${server}" 2>/dev/null || true' EXIT

for _ in $(seq 1 60); do
  if curl -fsS -o /dev/null "http://localhost:${PORT}/" 2>/dev/null; then
    break
  fi
  sleep 2
done

# Every shape a reader can arrive by. The .html spellings are what javadoc
# publishes and what 304 links in the guide and the site content use; the
# directory spellings are what roughly 2200 other links use.
URLS=(
  "/javadoc/"
  "/javadoc/com/codename1/ui/Component.html"
  "/javadoc/com/codename1/ui/Component/"
  "/javadoc/com/codename1/ui/package-summary.html"
  "/javadoc/com/codename1/ui/package-summary/"
  # com.codename1.ui.List is a type and com.codename1.ui.list is a package.
  # Their pages differ only by case, so this is where a case fold shows up.
  "/javadoc/com/codename1/ui/List.html"
  "/javadoc/com/codename1/ui/list/ListModel.html"
  "/javadoc/java/lang/String.html"
)

failures=0
for url in "${URLS[@]}"; do
  body="$(mktemp)"
  code="$(curl -sS -o "${body}" -w '%{http_code}' -L "http://localhost:${PORT}${url}")"
  final="$(curl -sS -o /dev/null -w '%{url_effective}' -L "http://localhost:${PORT}${url}")"
  title="$(grep -o '<title>[^<]*</title>' "${body}" | head -1 | sed 's/<[^>]*>//g' || true)"
  rm -f "${body}"

  if [ "${code}" != "200" ]; then
    echo "FAIL ${url} -> HTTP ${code} (${final})"
    failures=$((failures + 1))
    continue
  fi
  # A redirect that leaves the deployment is the failure this script was written
  # for: an alias built with the production baseURL sent preview readers to
  # www.codenameone.com, so the preview could never show the new pages at all.
  case "${final}" in
    "http://localhost:${PORT}"*) ;;
    *) echo "FAIL ${url} -> left the deployment: ${final}"; failures=$((failures + 1)); continue ;;
  esac
  if [ -z "${title}" ]; then
    echo "FAIL ${url} -> 200 but no title; not a rendered page"
    failures=$((failures + 1))
    continue
  fi
  echo "ok   ${url} -> ${title}"
done

if [ "${failures}" -gt 0 ]; then
  echo "${failures} API URL(s) do not resolve on the Pages runtime." >&2
  exit 1
fi
echo "OK: every API URL shape resolves on the Pages runtime"
