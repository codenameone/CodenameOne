#!/usr/bin/env bash
set -euo pipefail

pj_log() { echo "[compare-javascript-bundles] $1"; }

usage() {
  cat <<'EOF' >&2
Usage: compare-javascript-bundles.sh --legacy <zip-or-dir> --parparvm <zip-or-dir> [--summary-out <file>]

Compares the current legacy JavaScript playground artifact with a ParparVM
artifact and reports total bundle size plus key payload file sizes. Inputs may
be directories or archives (.zip/.war/.jar).
EOF
}

LEGACY_INPUT=""
PARPARVM_INPUT=""
SUMMARY_OUT=""

while [ $# -gt 0 ]; do
  case "$1" in
    --legacy)
      shift
      LEGACY_INPUT="${1:-}"
      ;;
    --parparvm)
      shift
      PARPARVM_INPUT="${1:-}"
      ;;
    --summary-out)
      shift
      SUMMARY_OUT="${1:-}"
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      pj_log "Unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
  shift
done

if [ -z "$LEGACY_INPUT" ] || [ -z "$PARPARVM_INPUT" ]; then
  usage
  exit 2
fi

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
WORK_DIR="$(mktemp -d "${TMPDIR}/cn1playground-jscmp-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1playground-jscmp")"

cleanup() {
  rm -rf "$WORK_DIR" 2>/dev/null || true
}
trap cleanup EXIT

file_size() {
  local file="$1"
  if stat --version >/dev/null 2>&1; then
    stat --printf='%s' "$file"
  else
    stat -f '%z' "$file"
  fi
}

materialize_input() {
  local input="$1"
  local name="$2"
  local dest="$WORK_DIR/$name"
  mkdir -p "$dest"
  if [ -d "$input" ]; then
    printf '%s\n' "$input"
    return 0
  fi
  if [ ! -f "$input" ]; then
    pj_log "Input not found: $input" >&2
    return 1
  fi
  case "$input" in
    *.zip|*.war|*.jar)
      unzip -qq "$input" -d "$dest"
      printf '%s\n' "$dest"
      ;;
    *)
      pj_log "Unsupported input type: $input" >&2
      return 1
      ;;
  esac
}

locate_bundle_root() {
  local root="$1"
  if [ -f "$root/translated_app.js" ] || [ -f "$root/worker.js" ]; then
    printf '%s\n' "$root"
    return 0
  fi
  local candidate=""
  candidate="$(find "$root" -type d \( -name '*-js' -o -name dist \) | while read -r dir; do
    if [ -f "$dir/translated_app.js" ] || [ -f "$dir/worker.js" ]; then
      printf '%s\n' "$dir"
      break
    fi
  done)"
  if [ -n "$candidate" ]; then
    printf '%s\n' "$candidate"
    return 0
  fi
  printf '%s\n' "$root"
}

sum_matching_bytes() {
  local root="$1"
  shift
  local total=0
  while IFS= read -r -d '' file; do
    local bytes
    bytes="$(file_size "$file")"
    total=$((total + bytes))
  done < <(find "$root" -type f \( "$@" \) -print0)
  printf '%s\n' "$total"
}

maybe_file_size() {
  local path="$1"
  if [ -f "$path" ]; then
    file_size "$path"
  else
    echo 0
  fi
}

emit_metrics() {
  local label="$1"
  local root="$2"
  local translated runtime worker bridge html css total js_count js_bytes
  translated="$(maybe_file_size "$root/translated_app.js")"
  runtime="$(maybe_file_size "$root/parparvm_runtime.js")"
  worker="$(maybe_file_size "$root/worker.js")"
  bridge="$(maybe_file_size "$root/browser_bridge.js")"
  html="$(sum_matching_bytes "$root" -name '*.html')"
  css="$(sum_matching_bytes "$root" -name '*.css')"
  js_bytes="$(sum_matching_bytes "$root" -name '*.js')"
  js_count="$(find "$root" -type f -name '*.js' | wc -l | tr -d ' ')"
  total="$(sum_matching_bytes "$root" -true)"
  cat <<EOF
${label}_root: $root
${label}_total_bytes: $total
${label}_js_file_count: $js_count
${label}_js_bytes: $js_bytes
${label}_html_bytes: $html
${label}_css_bytes: $css
${label}_translated_app_bytes: $translated
${label}_runtime_bytes: $runtime
${label}_worker_bytes: $worker
${label}_browser_bridge_bytes: $bridge
EOF
}

ratio_line() {
  local legacy="$1"
  local current="$2"
  local label="$3"
  if [ "$legacy" -le 0 ]; then
    echo "${label}_ratio: n/a"
    return 0
  fi
  awk -v a="$current" -v b="$legacy" -v name="$label" 'BEGIN { printf "%s_ratio: %.3f\n", name, a / b }'
}

LEGACY_MAT="$(materialize_input "$LEGACY_INPUT" legacy)"
PARPARVM_MAT="$(materialize_input "$PARPARVM_INPUT" parparvm)"
LEGACY_ROOT="$(locate_bundle_root "$LEGACY_MAT")"
PARPARVM_ROOT="$(locate_bundle_root "$PARPARVM_MAT")"

LEGACY_REPORT="$(emit_metrics legacy "$LEGACY_ROOT")"
PARPARVM_REPORT="$(emit_metrics parparvm "$PARPARVM_ROOT")"

LEGACY_TOTAL="$(printf '%s\n' "$LEGACY_REPORT" | awk -F': ' '/legacy_total_bytes/ {print $2; exit}')"
PARPARVM_TOTAL="$(printf '%s\n' "$PARPARVM_REPORT" | awk -F': ' '/parparvm_total_bytes/ {print $2; exit}')"
LEGACY_JS="$(printf '%s\n' "$LEGACY_REPORT" | awk -F': ' '/legacy_js_bytes/ {print $2; exit}')"
PARPARVM_JS="$(printf '%s\n' "$PARPARVM_REPORT" | awk -F': ' '/parparvm_js_bytes/ {print $2; exit}')"

REPORT="$LEGACY_REPORT
$PARPARVM_REPORT
$(ratio_line "$LEGACY_TOTAL" "$PARPARVM_TOTAL" total_bytes)
$(ratio_line "$LEGACY_JS" "$PARPARVM_JS" js_bytes)"

printf '%s\n' "$REPORT"

if [ -n "$SUMMARY_OUT" ]; then
  mkdir -p "$(dirname "$SUMMARY_OUT")" 2>/dev/null || true
  printf '%s\n' "$REPORT" > "$SUMMARY_OUT"
  pj_log "Wrote bundle comparison summary to $SUMMARY_OUT"
fi
