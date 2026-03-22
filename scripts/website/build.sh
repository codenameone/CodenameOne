#!/usr/bin/env bash
set -euo pipefail

mkdir -p ~/.codenameone
cp maven/UpdateCodenameOne.jar ~/.codenameone/

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
WEBSITE_DIR="${REPO_ROOT}/docs/website"

if [ ! -d "${WEBSITE_DIR}" ]; then
  echo "Website directory not found: ${WEBSITE_DIR}" >&2
  exit 1
fi

HUGO_BIN="${HUGO_BIN:-hugo}"
HUGO_ENVIRONMENT="${HUGO_ENVIRONMENT:-production}"
HUGO_MINIFY="${HUGO_MINIFY:-true}"
HUGO_BASEURL="${HUGO_BASEURL:-https://www.codenameone.com/}"
PYTHON_BIN="${PYTHON_BIN:-python3}"
WEBSITE_INCLUDE_JAVADOCS="${WEBSITE_INCLUDE_JAVADOCS:-false}"
WEBSITE_INCLUDE_DEVGUIDE="${WEBSITE_INCLUDE_DEVGUIDE:-auto}"
WEBSITE_INCLUDE_INITIALIZR="${WEBSITE_INCLUDE_INITIALIZR:-false}"
WEBSITE_INCLUDE_PLAYGROUND="${WEBSITE_INCLUDE_PLAYGROUND:-false}"
WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS="${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS:-auto}"
CN1_USER="${CN1_USER:-}"
CN1_TOKEN="${CN1_TOKEN:-}"

if [ "${WEBSITE_INCLUDE_INITIALIZR}" = "auto" ]; then
  if [ -n "${CN1_USER}" ] && [ -n "${CN1_TOKEN}" ]; then
    WEBSITE_INCLUDE_INITIALIZR="true"
  else
    WEBSITE_INCLUDE_INITIALIZR="false"
  fi
fi

bootstrap_local_cn1_snapshots() {
  if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" != "true" ]; then
    return
  fi

  echo "Bootstrapping local Codename One snapshot Maven artifacts..." >&2
  (
    cd "${REPO_ROOT}"
    SKIP_CN1_ARCHETYPES=1 ./scripts/setup-workspace.sh -q -DskipTests
  )
}

activate_bootstrapped_java17() {
  local env_file="${TMPDIR:-/tmp}/codenameone-tools/tools/env.sh"
  if [ ! -f "${env_file}" ]; then
    echo "Expected workspace environment file was not created: ${env_file}" >&2
    exit 1
  fi

  # shellcheck disable=SC1090
  source "${env_file}"
  if [ -z "${JAVA17_HOME:-}" ] || [ ! -x "${JAVA17_HOME}/bin/java" ]; then
    echo "Workspace bootstrap did not provide a usable JAVA17_HOME." >&2
    exit 1
  fi

  export JAVA_HOME="${JAVA17_HOME}"
  export PATH="${JAVA_HOME}/bin:${PATH}"
}

if [ "${WEBSITE_INCLUDE_PLAYGROUND}" = "auto" ]; then
  if [ -n "${CN1_USER}" ] && [ -n "${CN1_TOKEN}" ]; then
    WEBSITE_INCLUDE_PLAYGROUND="true"
  else
    WEBSITE_INCLUDE_PLAYGROUND="false"
  fi
fi

if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" = "auto" ]; then
  if [ "${WEBSITE_INCLUDE_PLAYGROUND}" = "true" ]; then
    WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS="true"
  else
    WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS="false"
  fi
fi

set_cn1_user_token() {
  local project_dir="$1"

  if [ -n "${CN1_USER}" ] && [ -n "${CN1_TOKEN}" ]; then
    if ! sh ./mvnw -q -U -pl javascript -am \
      cn1:set-user-token \
      -Dcodename1.platform=javascript \
      -Duser="${CN1_USER}" \
      -Dtoken="${CN1_TOKEN}"; then
      echo "cn1:set-user-token is unavailable in this plugin version for ${project_dir}; writing CN1 credentials directly to Java preferences." >&2
      local tmp_dir
      tmp_dir="$(mktemp -d)"
      cat > "${tmp_dir}/SetCn1Prefs.java" <<'JAVA'
import java.util.prefs.Preferences;

public class SetCn1Prefs {
    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: SetCn1Prefs <user> <token>");
        }
        Preferences prefs = Preferences.userRoot().node("/com/codename1/ui");
        prefs.put("user", args[0]);
        prefs.put("token", args[1]);
    }
}
JAVA
      javac "${tmp_dir}/SetCn1Prefs.java"
      java -cp "${tmp_dir}" SetCn1Prefs "${CN1_USER}" "${CN1_TOKEN}"
      rm -rf "${tmp_dir}"
    fi
  else
    echo "CN1_USER/CN1_TOKEN not provided; building ${project_dir} JavaScript without setting token." >&2
  fi
}

build_javadocs_for_site() {
  if [ "${WEBSITE_INCLUDE_JAVADOCS}" != "true" ]; then
    return
  fi

  echo "Building fresh JavaDocs for website..." >&2
  (
    cd "${REPO_ROOT}"
    ./.github/scripts/build_javadocs.sh
  )

  rm -rf "${WEBSITE_DIR}/static/javadoc"
  mkdir -p "${WEBSITE_DIR}/static/javadoc" "${WEBSITE_DIR}/static/files" "${WEBSITE_DIR}/generated"
  cp -a "${REPO_ROOT}/CodenameOne/dist/javadoc/." "${WEBSITE_DIR}/static/javadoc/"
  cp "${REPO_ROOT}/CodenameOne/javadocs.zip" "${WEBSITE_DIR}/static/files/javadocs.zip"

  if [ -f "${WEBSITE_DIR}/static/javadoc/index.html" ]; then
    mv "${WEBSITE_DIR}/static/javadoc/index.html" "${WEBSITE_DIR}/static/javadoc/_index-raw.html"
  fi

  awk '
    BEGIN { in_body = 0 }
    /<body[^>]*>/ {
      in_body = 1
      sub(/^.*<body[^>]*>/, "")
      if (length($0) > 0) print
      next
    }
    in_body && /<\/body>/ {
      sub(/<\/body>.*/, "")
      if (length($0) > 0) print
      exit
    }
    in_body { print }
  ' "${WEBSITE_DIR}/static/javadoc/_index-raw.html" > "${WEBSITE_DIR}/generated/javadoc-content.html"

  if [ -f "${WEBSITE_DIR}/static/javadoc/resource-files/stylesheet.css" ]; then
    "${PYTHON_BIN}" - "${WEBSITE_DIR}/static/javadoc/resource-files/stylesheet.css" "${WEBSITE_DIR}/static/javadoc/resource-files/stylesheet-scoped.css" <<'PY'
import re
import sys

src_path, out_path = sys.argv[1], sys.argv[2]
src = open(src_path, "r", encoding="utf-8").read()
src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)

PREFIX = ".cn1-javadoc"

def split_selectors(text):
    out, cur = [], []
    depth_paren = depth_bracket = 0
    in_string = None
    escape = False
    for ch in text:
        if in_string:
            cur.append(ch)
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == in_string:
                in_string = None
            continue
        if ch in ("'", '"'):
            in_string = ch
            cur.append(ch)
            continue
        if ch == "(":
            depth_paren += 1
        elif ch == ")":
            depth_paren = max(0, depth_paren - 1)
        elif ch == "[":
            depth_bracket += 1
        elif ch == "]":
            depth_bracket = max(0, depth_bracket - 1)
        if ch == "," and depth_paren == 0 and depth_bracket == 0:
            out.append("".join(cur))
            cur = []
            continue
        cur.append(ch)
    out.append("".join(cur))
    return out

def transform_selector(sel):
    sel = sel.strip()
    if not sel:
        return sel
    if PREFIX in sel:
        return sel
    if sel in ("html", "body", ":root"):
        return PREFIX
    return f"{PREFIX} {sel}"

def extract_block(text, start):
    depth = 1
    i = start
    n = len(text)
    in_string = None
    escape = False
    while i < n:
        ch = text[i]
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == in_string:
                in_string = None
            i += 1
            continue
        if ch in ("'", '"'):
            in_string = ch
            i += 1
            continue
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return text[start:i], i + 1
        i += 1
    return text[start:], n

def process(css):
    out = []
    i = 0
    n = len(css)
    while i < n:
        j = i
        depth_paren = depth_bracket = 0
        in_string = None
        escape = False
        while j < n:
            ch = css[j]
            if in_string:
                if escape:
                    escape = False
                elif ch == "\\":
                    escape = True
                elif ch == in_string:
                    in_string = None
                j += 1
                continue
            if ch in ("'", '"'):
                in_string = ch
                j += 1
                continue
            if ch == "(":
                depth_paren += 1
            elif ch == ")":
                depth_paren = max(0, depth_paren - 1)
            elif ch == "[":
                depth_bracket += 1
            elif ch == "]":
                depth_bracket = max(0, depth_bracket - 1)
            if depth_paren == 0 and depth_bracket == 0 and ch in "{;":
                break
            j += 1
        if j >= n:
            out.append(css[i:])
            break
        prelude = css[i:j].strip()
        term = css[j]
        if term == ";":
            out.append(css[i:j + 1])
            i = j + 1
            continue
        block, next_i = extract_block(css, j + 1)
        low = prelude.lower()
        if low.startswith("@media") or low.startswith("@supports"):
            out.append(f"{prelude}{{{process(block)}}}")
        elif low.startswith("@"):
            out.append(f"{prelude}{{{block}}}")
        else:
            selectors = [transform_selector(s) for s in split_selectors(prelude)]
            out.append(f"{','.join(selectors)}{{{block}}}")
        i = next_i
    return "".join(out)

with open(out_path, "w", encoding="utf-8") as f:
    scoped = process(src)
    # Inline scoped stylesheet is injected on /javadoc/ pages, so make asset refs absolute.
    # This keeps icons/fonts resolvable from both /javadoc/ and deep class pages loaded in-page.
    import re
    def repl(m):
        raw = m.group(1).strip().strip('"\'')
        if (raw.startswith("data:") or raw.startswith("http://") or raw.startswith("https://")
                or raw.startswith("/") or raw.startswith("#")):
            return f"url({m.group(1)})"
        normalized = raw.lstrip("./")
        return f"url('/javadoc/resource-files/{normalized}')"
    scoped = re.sub(r"url\(([^)]+)\)", repl, scoped)
    f.write(scoped)
PY
  fi
}

build_developer_guide_for_site() {
  local include_devguide="${WEBSITE_INCLUDE_DEVGUIDE}"
  if [ "${include_devguide}" = "false" ]; then
    return
  fi

  if ! command -v asciidoctor >/dev/null 2>&1; then
    if [ "${include_devguide}" = "true" ]; then
      echo "Asciidoctor tooling is required when WEBSITE_INCLUDE_DEVGUIDE=true." >&2
      exit 1
    fi
    echo "Asciidoctor not found; skipping Developer Guide generation (set WEBSITE_INCLUDE_DEVGUIDE=false to silence)." >&2
    return
  fi

  echo "Building fresh Developer Guide for website..." >&2
  local output_root="${REPO_ROOT}/build/website-developer-guide"
  local html_out="${output_root}/html"
  local guide_dir="${WEBSITE_DIR}/static/developer-guide"
  local generated_dir="${WEBSITE_DIR}/generated"
  local guide_fragment_path="${generated_dir}/developer-guide-content.html"
  local source_dir="${REPO_ROOT}/docs/developer-guide"

  rm -rf "${output_root}" "${guide_dir}" "${WEBSITE_DIR}/static/manual" "${WEBSITE_DIR}/static/developer-guide-content"
  rm -f "${WEBSITE_DIR}/static/developer-guide.html"
  mkdir -p "${html_out}" "${guide_dir}" "${generated_dir}"

  (
    cd "${REPO_ROOT}"
    asciidoctor \
      --require rouge \
      -a linkcss \
      -a copycss \
      -a rouge-css=style \
      -D "${html_out}" \
      -o developer-guide-full.html \
      docs/developer-guide/developer-guide.asciidoc

  )

  awk '
    BEGIN { in_body = 0 }
    /<body[^>]*>/ {
      in_body = 1
      sub(/^.*<body[^>]*>/, "")
      if (length($0) > 0) print
      next
    }
    in_body && /<\/body>/ {
      sub(/<\/body>.*/, "")
      if (length($0) > 0) print
      exit
    }
    in_body { print }
  ' "${html_out}/developer-guide-full.html" > "${guide_fragment_path}"

  if [ -f "${html_out}/asciidoctor.css" ]; then
    cp "${html_out}/asciidoctor.css" "${guide_dir}/asciidoctor.css"
  elif command -v ruby >/dev/null 2>&1; then
    ruby -rasciidoctor -e 'print Asciidoctor::Stylesheets.instance.primary_stylesheet_data' \
      > "${guide_dir}/asciidoctor.css"
  fi

  if [ ! -s "${guide_dir}/asciidoctor.css" ]; then
    echo "Asciidoctor stylesheet could not be generated for Developer Guide." >&2
    exit 1
  fi

  "${PYTHON_BIN}" - "${guide_dir}/asciidoctor.css" "${guide_dir}/asciidoctor-scoped.css" <<'PY'
import sys

src_path, out_path = sys.argv[1], sys.argv[2]
src = open(src_path, "r", encoding="utf-8").read()

PREFIX = ".cn1-developer-guide"

def split_selectors(text):
    out, cur = [], []
    depth_paren = depth_bracket = 0
    in_string = None
    escape = False
    for ch in text:
        if in_string:
            cur.append(ch)
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == in_string:
                in_string = None
            continue
        if ch in ("'", '"'):
            in_string = ch
            cur.append(ch)
            continue
        if ch == "(":
            depth_paren += 1
        elif ch == ")":
            depth_paren = max(0, depth_paren - 1)
        elif ch == "[":
            depth_bracket += 1
        elif ch == "]":
            depth_bracket = max(0, depth_bracket - 1)
        if ch == "," and depth_paren == 0 and depth_bracket == 0:
            out.append("".join(cur))
            cur = []
            continue
        cur.append(ch)
    out.append("".join(cur))
    return out

def transform_selector(sel):
    sel = sel.strip()
    if not sel:
        return sel
    if PREFIX in sel:
        return sel
    if sel in ("html", "body", ":root"):
        return PREFIX
    return f"{PREFIX} {sel}"

def extract_block(text, start):
    depth = 1
    i = start
    n = len(text)
    in_string = None
    escape = False
    while i < n:
        ch = text[i]
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == in_string:
                in_string = None
            i += 1
            continue
        if ch in ("'", '"'):
            in_string = ch
            i += 1
            continue
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return text[start:i], i + 1
        i += 1
    return text[start:], n

def process(css):
    out = []
    i = 0
    n = len(css)
    while i < n:
        if css.startswith("/*", i):
            j = css.find("*/", i + 2)
            if j == -1:
                out.append(css[i:])
                break
            out.append(css[i:j + 2])
            i = j + 2
            continue
        j = i
        depth_paren = depth_bracket = 0
        in_string = None
        escape = False
        while j < n:
            ch = css[j]
            if in_string:
                if escape:
                    escape = False
                elif ch == "\\":
                    escape = True
                elif ch == in_string:
                    in_string = None
                j += 1
                continue
            if ch in ("'", '"'):
                in_string = ch
                j += 1
                continue
            if ch == "(":
                depth_paren += 1
            elif ch == ")":
                depth_paren = max(0, depth_paren - 1)
            elif ch == "[":
                depth_bracket += 1
            elif ch == "]":
                depth_bracket = max(0, depth_bracket - 1)
            if depth_paren == 0 and depth_bracket == 0 and ch in "{;":
                break
            j += 1
        if j >= n:
            out.append(css[i:])
            break
        prelude = css[i:j].strip()
        term = css[j]
        if term == ";":
            out.append(css[i:j + 1])
            i = j + 1
            continue
        block, next_i = extract_block(css, j + 1)
        low = prelude.lower()
        if low.startswith("@media") or low.startswith("@supports"):
            out.append(f"{prelude}{{{process(block)}}}")
        elif low.startswith("@"):
            out.append(f"{prelude}{{{block}}}")
        else:
            selectors = [transform_selector(s) for s in split_selectors(prelude)]
            out.append(f"{','.join(selectors)}{{{block}}}")
        i = next_i
    return "".join(out)

scoped = process(src)
with open(out_path, "w", encoding="utf-8") as f:
    f.write(scoped)
PY

  if [ ! -s "${guide_dir}/asciidoctor-scoped.css" ]; then
    echo "Scoped Asciidoctor stylesheet could not be generated for Developer Guide." >&2
    exit 1
  fi
  # Keep guide assets under /developer-guide/ so relative image links (e.g. img/foo.png) resolve.
  rsync -a \
    --exclude 'sketch/' \
    --exclude '*.asciidoc' \
    --exclude '*.adoc' \
    "${source_dir}/" "${guide_dir}/"
}

build_initializr_for_site() {
  if [ "${WEBSITE_INCLUDE_INITIALIZR}" != "true" ]; then
    return
  fi

  echo "Building Initializr JavaScript bundle for website..." >&2
  (
    cd "${REPO_ROOT}/scripts/initializr"

    run_initializr_mvn() {
      if command -v xvfb-run >/dev/null 2>&1; then
        xvfb-run -a sh ./mvnw "$@"
      else
        sh ./mvnw "$@"
      fi
    }

    if [ -n "${JAVA_HOME_8_X64:-}" ]; then
      export JAVA_HOME="${JAVA_HOME_8_X64}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
    fi

    # Ensure attached classifier artifact initializr-ZipSupport:jar:common is present
    # in the local Maven repo before building modules that depend on it (e.g. initializr-common).
    run_initializr_mvn -q -U -pl cn1libs/ZipSupport -am \
      -DskipTests \
      -Dcodename1.platform=javascript \
      install

    set_cn1_user_token "Initializr"

    run_initializr_mvn -q -U -pl javascript -am \
      -DskipTests \
      -Dautomated=true \
      -Dcodename1.platform=javascript \
      package
  )

  local output_dir="${WEBSITE_DIR}/static/initializr-app"
  local result_zip="${REPO_ROOT}/scripts/initializr/javascript/target/result.zip"
  if [ ! -f "${result_zip}" ]; then
    result_zip="$(ls -1 "${REPO_ROOT}"/scripts/initializr/javascript/target/initializr-javascript-*.zip 2>/dev/null | head -n1 || true)"
  fi

  if [ -z "${result_zip}" ] || [ ! -f "${result_zip}" ]; then
    echo "Could not locate Initializr JavaScript build zip output in scripts/initializr/javascript/target." >&2
    exit 1
  fi

  rm -rf "${output_dir}"
  mkdir -p "${output_dir}"
  unzip -q -o "${result_zip}" -d "${output_dir}"

  if [ ! -f "${output_dir}/index.html" ]; then
    echo "Initializr website bundle is missing index.html after extraction." >&2
    exit 1
  fi
}

build_playground_for_site() {
  if [ "${WEBSITE_INCLUDE_PLAYGROUND}" != "true" ]; then
    return
  fi

  bootstrap_local_cn1_snapshots

  echo "Building Playground JavaScript bundle for website..." >&2
  (
    cd "${REPO_ROOT}/scripts/cn1playground"
    activate_bootstrapped_java17

    run_playground_mvn() {
      if command -v xvfb-run >/dev/null 2>&1; then
        xvfb-run -a sh ./mvnw "$@"
      else
        sh ./mvnw "$@"
      fi
    }

    set_cn1_user_token "Playground"
    run_playground_mvn -q -U -pl javascript -am \
      -DskipTests \
      -Dautomated=true \
      -Dcodename1.platform=javascript \
      package
  )

  local output_dir="${WEBSITE_DIR}/static/playground-app"
  local result_zip="${REPO_ROOT}/scripts/cn1playground/javascript/target/result.zip"
  if [ ! -f "${result_zip}" ]; then
    result_zip="$(ls -1 "${REPO_ROOT}"/scripts/cn1playground/javascript/target/cn1playground-javascript-*.zip 2>/dev/null | head -n1 || true)"
  fi

  if [ -z "${result_zip}" ] || [ ! -f "${result_zip}" ]; then
    echo "Could not locate Playground JavaScript build zip output in scripts/cn1playground/javascript/target." >&2
    exit 1
  fi

  rm -rf "${output_dir}"
  mkdir -p "${output_dir}"
  unzip -q -o "${result_zip}" -d "${output_dir}"

  if [ ! -f "${output_dir}/index.html" ]; then
    echo "Playground website bundle is missing index.html after extraction." >&2
    exit 1
  fi
}

if ! command -v "${HUGO_BIN}" >/dev/null 2>&1; then
  echo "Hugo binary not found. Install Hugo (extended) and retry." >&2
  exit 1
fi

build_javadocs_for_site
build_developer_guide_for_site
build_initializr_for_site
build_playground_for_site

cd "${WEBSITE_DIR}"

if command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
  "${PYTHON_BIN}" "${WEBSITE_DIR}/scripts/generate_cn1libs.py"
else
  echo "Warning: python3 not found; skipping cn1libs refresh." >&2
fi

MINIFY_FLAG=""
if [ "${HUGO_MINIFY}" = "true" ]; then
  MINIFY_FLAG="--minify"
fi

HUGO_ENV="${HUGO_ENVIRONMENT}" "${HUGO_BIN}" \
  --cleanDestinationDir \
  --gc \
  --baseURL "${HUGO_BASEURL}" \
  ${MINIFY_FLAG}

if command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
  "${PYTHON_BIN}" "${WEBSITE_DIR}/scripts/generate_lunr_index.py"
else
  echo "Warning: python3 not found; skipping lunr index generation." >&2
fi
