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
# When true, include posts whose front-matter date is in the future
# (e.g. weekly release posts staged for later in the week). Off by
# default so the live site only shows posts whose publish date has
# arrived; PR previews flip this on so reviewers can read the draft.
HUGO_BUILD_FUTURE="${HUGO_BUILD_FUTURE:-false}"
# When true, include posts marked draft: true.
HUGO_BUILD_DRAFTS="${HUGO_BUILD_DRAFTS:-false}"
PYTHON_BIN="${PYTHON_BIN:-python3}"
WEBSITE_INCLUDE_JAVADOCS="${WEBSITE_INCLUDE_JAVADOCS:-false}"
WEBSITE_INCLUDE_DEVGUIDE="${WEBSITE_INCLUDE_DEVGUIDE:-auto}"
WEBSITE_INCLUDE_INITIALIZR="${WEBSITE_INCLUDE_INITIALIZR:-false}"
WEBSITE_INCLUDE_PLAYGROUND="${WEBSITE_INCLUDE_PLAYGROUND:-false}"
WEBSITE_INCLUDE_SKINDESIGNER="${WEBSITE_INCLUDE_SKINDESIGNER:-true}"
WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS="${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS:-auto}"
WEBSITE_REFRESH_PORT_STATUS="${WEBSITE_REFRESH_PORT_STATUS:-false}"
WEBSITE_CN1_VERSION="${WEBSITE_CN1_VERSION:-auto}"

if [ "${WEBSITE_INCLUDE_INITIALIZR}" = "auto" ]; then
  WEBSITE_INCLUDE_INITIALIZR="true"
fi

bootstrap_local_cn1_snapshots() {
  if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" != "true" ]; then
    return
  fi

  # Each site app (initializr/playground/skindesigner) calls this; the full
  # reactor build is expensive, so run setup-workspace.sh only once per build.
  if [ "${__CN1_SNAPSHOTS_BOOTSTRAPPED:-}" = "true" ]; then
    return
  fi

  echo "Bootstrapping local Codename One snapshot Maven artifacts..." >&2
  (
    cd "${REPO_ROOT}"
    SKIP_CN1_ARCHETYPES=1 ./scripts/setup-workspace.sh -q -DskipTests
  )
  __CN1_SNAPSHOTS_BOOTSTRAPPED="true"
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
  WEBSITE_INCLUDE_PLAYGROUND="true"
fi

if [ "${WEBSITE_INCLUDE_SKINDESIGNER}" = "auto" ]; then
  WEBSITE_INCLUDE_SKINDESIGNER="true"
fi

if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" = "auto" ]; then
  WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS="false"
fi

if [ "${WEBSITE_CN1_VERSION}" = "auto" ]; then
  WEBSITE_CN1_VERSION="$(perl -0777 -ne 'if (m|<properties>.*?<cn1\.version>([^<]+)</cn1\.version>|s) { print $1; }' "${REPO_ROOT}/scripts/cn1playground/pom.xml")"
  if [ -z "${WEBSITE_CN1_VERSION}" ]; then
    echo "Unable to determine cn1.version from scripts/cn1playground/pom.xml" >&2
    exit 1
  fi
fi

build_javadocs_for_site() {
  if [ "${WEBSITE_INCLUDE_JAVADOCS}" != "true" ]; then
    return
  fi

  local jdk_home="${JDK_25_HOME:-${JAVA_HOME:-}}"
  local javadoc_bin="javadoc"
  if [ -n "${jdk_home}" ] && [ -x "${jdk_home}/bin/javadoc" ]; then
    javadoc_bin="${jdk_home}/bin/javadoc"
  fi

  # The downloadable zip, still produced by the standard doclet. The two
  # renderings share one source of truth and neither replaces the other: the zip
  # is what someone unpacks next to an offline IDE, and the Hugo pages are what
  # the website serves.
  echo "Building JavaDoc archive..." >&2
  (
    cd "${REPO_ROOT}"
    ./.github/scripts/build_javadocs.sh
  )
  mkdir -p "${WEBSITE_DIR}/static/files"
  cp "${REPO_ROOT}/CodenameOne/javadocs.zip" "${WEBSITE_DIR}/static/files/javadocs.zip"

  # The doclet needs a JDK new enough to hand it markdown documentation comments
  # as DocTree.Kind.MARKDOWN, which is JDK 23 and later. Its own pom pins
  # release 25 and it is deliberately outside the JDK 8 reactor, so it is built
  # here with an explicit -f.
  echo "Building the Hugo JavaDoc doclet..." >&2
  (
    cd "${REPO_ROOT}"
    JAVA_HOME="${jdk_home}" mvn -q -B \
      -f maven/javadoc-hugo-doclet/pom.xml \
      -Dmaven.repo.local="${REPO_ROOT}/.m2-repo" \
      package
  )

  local doclet_classes="${REPO_ROOT}/maven/javadoc-hugo-doclet/target/classes"
  if [ ! -d "${doclet_classes}" ]; then
    echo "Hugo doclet did not build: ${doclet_classes} is missing." >&2
    exit 1
  fi

  # build_javadocs.sh already staged a source tree with the ImplementationFactory
  # stub that com.codename1.ui.Display imports but that does not exist in the
  # repository. Reusing it keeps the two renderings reading identical sources.
  local temp_sources="${REPO_ROOT}/CodenameOne/build/tempJavaSources"
  if [ ! -d "${temp_sources}" ]; then
    echo "Expected staged JavaDoc sources at ${temp_sources}." >&2
    exit 1
  fi

  local content_dir="${WEBSITE_DIR}/content/javadoc"
  rm -rf "${content_dir}"
  mkdir -p "${content_dir}" "${WEBSITE_DIR}/static"

  local sources_argfile="${REPO_ROOT}/CodenameOne/build/javadoc-sources.txt"
  if [ ! -s "${sources_argfile}" ]; then
    echo "Expected the JavaDoc source list at ${sources_argfile}." >&2
    exit 1
  fi

  echo "Generating Hugo API content..." >&2
  # No -Xdoclint here: it is a standard doclet option and javadoc rejects it
  # outright when another doclet is in use.
  #
  # --release 8 for the same reason build_javadocs.sh passes it. Ports/CLDC11
  # declares java.lang, java.util and java.io, and on a modular JDK every one of
  # those files fails with "package exists in another module: java.base".
  # Compiling for a release that predates modules is what lets the framework's
  # own java.* classes be the documented ones.
  "${javadoc_bin}" \
    -doclet com.codename1.doclet.hugo.HugoDoclet \
    -docletpath "${doclet_classes}" \
    --release 8 \
    -d "${content_dir}" \
    --search-index "${WEBSITE_DIR}/static/javadoc-search.json" \
    -sourcepath "${temp_sources}:${REPO_ROOT}/Ports/CLDC11/src" \
    -quiet \
    -protected \
    "@${sources_argfile}"

  # The same guard build_javadocs.sh keeps over the standard doclet. A partial
  # generation is the failure mode that ships quietly, so assert the core type
  # and the internal package separately rather than trusting the exit status.
  if [ ! -f "${content_dir}/com/codename1/ui/Component.md" ]; then
    echo "Hugo API generation produced no com.codename1.ui content; aborting." >&2
    exit 1
  fi
  if [ -e "${content_dir}/com/codename1/impl" ]; then
    echo "Hugo API generation emitted com.codename1.impl; the internal package must stay excluded." >&2
    exit 1
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

  local build_date
  build_date="$(date +%Y-%m-%d)"

  # The guide includes a build hint table that is rendered from
  # maven/build-hint-catalog rather than checked in, so it has to exist before
  # asciidoctor resolves the include.
  "${REPO_ROOT}/scripts/gen-build-hint-table.sh"

  (
    cd "${REPO_ROOT}"
    asciidoctor \
      --require rouge \
      -a linkcss \
      -a copycss \
      -a rouge-css=style \
      -a revdate="${build_date}" \
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

  # The initializr builds the JavaScript app with the local ParparVM target
  # (codename1.buildTarget=local-javascript). That builder lives in the repo's
  # 8.0-SNAPSHOT plugin, not the pinned release, so bootstrap the local
  # snapshots and build with -Dcn1.localWorkspace=true (the cn1-local-workspace
  # profile then overrides cn1.version/cn1.plugin.version to the repo build).
  bootstrap_local_cn1_snapshots

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

    local initializr_workspace_args=()
    if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" = "true" ]; then
      # Local ParparVM JS build runs the translator + javac; use the
      # bootstrapped JDK 17 (matching the Playground/Skin Designer path).
      activate_bootstrapped_java17
      initializr_workspace_args+=(-Dcn1.localWorkspace=true)
    elif [ -n "${JAVA_HOME_8_X64:-}" ]; then
      export JAVA_HOME="${JAVA_HOME_8_X64}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
    fi

    # Ensure attached classifier artifact initializr-ZipSupport:jar:common is present
    # in the local Maven repo before building modules that depend on it (e.g. initializr-common).
    run_initializr_mvn -q -U -pl cn1libs/ZipSupport -am \
      "${initializr_workspace_args[@]}" \
      -DskipTests \
      -Dcodename1.platform=javascript \
      install

    run_initializr_mvn -q -U -pl javascript -am \
      "${initializr_workspace_args[@]}" \
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

  # The cloud result.zip is flat (index.html at the root), but the local
  # ParparVM build (codename1.buildTarget=local-javascript) wraps the bundle in
  # a single top-level directory (e.g. Initializr-js/). Flatten that wrapper so
  # the served layout is identical regardless of which builder produced the zip.
  if [ ! -f "${output_dir}/index.html" ]; then
    local inner_dir
    inner_dir="$(find "${output_dir}" -mindepth 1 -maxdepth 1 -type d | head -n1 || true)"
    if [ -n "${inner_dir}" ] && [ -f "${inner_dir}/index.html" ]; then
      ( cd "${inner_dir}" && tar cf - . ) | ( cd "${output_dir}" && tar xf - )
      rm -rf "${inner_dir}"
    fi
  fi

  if [ ! -f "${output_dir}/index.html" ]; then
    echo "Initializr website bundle is missing index.html after extraction." >&2
    exit 1
  fi

  # The Initializr page (layouts/_default/initializr.html) shows the app icon
  # from /initializr-app/icon.png. The cloud bundle shipped one; the local
  # ParparVM bundle does not, so copy the project icon in when it is absent.
  if [ ! -f "${output_dir}/icon.png" ] && [ -f "${REPO_ROOT}/scripts/initializr/common/icon.png" ]; then
    cp "${REPO_ROOT}/scripts/initializr/common/icon.png" "${output_dir}/icon.png"
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
    ./update-cn1-version.sh "${WEBSITE_CN1_VERSION}"
    if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" = "true" ]; then
      activate_bootstrapped_java17
    fi

    run_playground_mvn() {
      if command -v xvfb-run >/dev/null 2>&1; then
        xvfb-run -a sh ./mvnw "$@"
      else
        sh ./mvnw "$@"
      fi
    }

    local playground_workspace_args=()
    if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" = "true" ]; then
      playground_workspace_args+=(-Dcn1.localWorkspace=true)
    fi

    # The Playground builds the JavaScript app with the local ParparVM target
    # (codename1.buildTarget=local-javascript, the module default). Its
    # bean-shell registry keeps nearly the whole Codename One API reachable, so
    # the JS Rapid Type Analysis tree-shaking pass cannot prune much yet runs
    # for over an hour -- disable it (parparvm.js.rta.off). The un-pruned bundle
    # is large, so raise the translator heap above the 512m default to avoid an
    # OutOfMemoryError mid-emit. CN1_TRANSLATOR_OPTS reaches the forked
    # translator JVM (the Maven process's -D properties do not).
    export CN1_TRANSLATOR_OPTS="${CN1_TRANSLATOR_OPTS:--Dparparvm.js.rta.off -Xmx6g}"

    run_playground_mvn -q -U -pl javascript -am \
      "${playground_workspace_args[@]}" \
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

  # The local ParparVM build (codename1.buildTarget=local-javascript) wraps the
  # bundle in a single top-level directory (e.g. CN1Playground-js/). Flatten
  # that wrapper so index.html lands at the served root, matching the old flat
  # cloud bundle. (Same handling as build_initializr_for_site.)
  if [ ! -f "${output_dir}/index.html" ]; then
    local inner_dir
    inner_dir="$(find "${output_dir}" -mindepth 1 -maxdepth 1 -type d | head -n1 || true)"
    if [ -n "${inner_dir}" ] && [ -f "${inner_dir}/index.html" ]; then
      ( cd "${inner_dir}" && tar cf - . ) | ( cd "${output_dir}" && tar xf - )
      rm -rf "${inner_dir}"
    fi
  fi

  if [ ! -f "${output_dir}/index.html" ]; then
    echo "Playground website bundle is missing index.html after extraction." >&2
    exit 1
  fi

  # The Playground page references /playground-app/icon.png. The cloud bundle
  # shipped that icon; the local ParparVM bundle does not, so copy the project
  # icon in when it is absent (same handling as build_initializr_for_site).
  if [ ! -f "${output_dir}/icon.png" ] && [ -f "${REPO_ROOT}/scripts/cn1playground/common/icon.png" ]; then
    cp "${REPO_ROOT}/scripts/cn1playground/common/icon.png" "${output_dir}/icon.png"
  fi
}


build_skindesigner_for_site() {
  if [ "${WEBSITE_INCLUDE_SKINDESIGNER}" != "true" ]; then
    return
  fi

  bootstrap_local_cn1_snapshots

  echo "Building Skin Designer JavaScript bundle for website..." >&2
  (
    cd "${REPO_ROOT}/scripts/skindesigner"
    ./tools/sync-zipsupport-from-initializr.sh
    if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" = "true" ]; then
      activate_bootstrapped_java17
    fi

    run_skindesigner_mvn() {
      if command -v xvfb-run >/dev/null 2>&1; then
        xvfb-run -a sh ./mvnw "$@"
      else
        sh ./mvnw "$@"
      fi
    }

    local skindesigner_workspace_args=()
    if [ "${WEBSITE_BOOTSTRAP_CN1_SNAPSHOTS}" = "true" ]; then
      skindesigner_workspace_args+=(-Dcn1.localWorkspace=true)
    fi

    # Ensure attached classifier artifact skindesigner-ZipSupport:jar:common
    # is present in the local Maven repo before building skindesigner-common.
    run_skindesigner_mvn -q -U -pl cn1libs/ZipSupport -am \
      "${skindesigner_workspace_args[@]}" \
      -DskipTests \
      -Dcodename1.platform=javascript \
      install

    run_skindesigner_mvn -q -U -pl javascript -am \
      "${skindesigner_workspace_args[@]}" \
      -DskipTests \
      -Dautomated=true \
      -Dcodename1.platform=javascript \
      package
  )

  local output_dir="${WEBSITE_DIR}/static/skindesigner-app"
  local result_zip="${REPO_ROOT}/scripts/skindesigner/javascript/target/result.zip"
  if [ ! -f "${result_zip}" ]; then
    result_zip="$(ls -1 "${REPO_ROOT}"/scripts/skindesigner/javascript/target/skindesigner-javascript-*.zip 2>/dev/null | head -n1 || true)"
  fi

  if [ -z "${result_zip}" ] || [ ! -f "${result_zip}" ]; then
    echo "Could not locate Skin Designer JavaScript build zip output in scripts/skindesigner/javascript/target." >&2
    exit 1
  fi

  rm -rf "${output_dir}"
  mkdir -p "${output_dir}"
  unzip -q -o "${result_zip}" -d "${output_dir}"

  # The cloud result.zip is flat (index.html at the root), but the local
  # ParparVM build (codename1.buildTarget=local-javascript) wraps the bundle in
  # a single top-level directory (e.g. SkinDesigner-js/). Flatten that wrapper
  # so the served layout is identical regardless of which builder produced the
  # zip. (Same handling as build_initializr_for_site / build_playground_for_site.)
  if [ ! -f "${output_dir}/index.html" ]; then
    local inner_dir
    inner_dir="$(find "${output_dir}" -mindepth 1 -maxdepth 1 -type d | head -n1 || true)"
    if [ -n "${inner_dir}" ] && [ -f "${inner_dir}/index.html" ]; then
      ( cd "${inner_dir}" && tar cf - . ) | ( cd "${output_dir}" && tar xf - )
      rm -rf "${inner_dir}"
    fi
  fi

  if [ ! -f "${output_dir}/index.html" ]; then
    echo "Skin Designer website bundle is missing index.html after extraction." >&2
    exit 1
  fi
}

if ! command -v "${HUGO_BIN}" >/dev/null 2>&1; then
  echo "Hugo binary not found. Install Hugo (extended) and retry." >&2
  exit 1
fi

if [ "${WEBSITE_REFRESH_PORT_STATUS}" = "true" ]; then
  "${REPO_ROOT}/scripts/website/sync_port_status_reports.sh"
fi
"${PYTHON_BIN}" "${REPO_ROOT}/scripts/hellocodenameone/conformance/port_status.py" validate

build_javadocs_for_site
build_developer_guide_for_site
build_initializr_for_site
build_playground_for_site
build_skindesigner_for_site

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

BUILD_FUTURE_FLAG=""
if [ "${HUGO_BUILD_FUTURE}" = "true" ]; then
  BUILD_FUTURE_FLAG="--buildFuture"
fi

BUILD_DRAFTS_FLAG=""
if [ "${HUGO_BUILD_DRAFTS}" = "true" ]; then
  BUILD_DRAFTS_FLAG="--buildDrafts"
fi

HUGO_ENV="${HUGO_ENVIRONMENT}" "${HUGO_BIN}" \
  --cleanDestinationDir \
  --gc \
  --baseURL "${HUGO_BASEURL}" \
  ${MINIFY_FLAG} \
  ${BUILD_FUTURE_FLAG} \
  ${BUILD_DRAFTS_FLAG}

if command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
  "${PYTHON_BIN}" "${WEBSITE_DIR}/scripts/generate_lunr_index.py"
else
  echo "Warning: python3 not found; skipping lunr index generation." >&2
fi

# The site and the downloadable zip are two renderings of one set of sources, and
# the split only works while they agree on addresses. Compare them here rather
# than trusting the generator: a fragment that drifts is a dead link from the
# developer guide, from a blog post, or from somewhere outside the project, and
# nothing else in the build would notice.
if [ "${WEBSITE_INCLUDE_JAVADOCS}" = "true" ]; then
  if command -v "${PYTHON_BIN}" >/dev/null 2>&1; then
    "${PYTHON_BIN}" "${REPO_ROOT}/scripts/website/check-javadoc-parity.py" \
      "${REPO_ROOT}/CodenameOne/dist/javadoc" \
      "${WEBSITE_DIR}/public/javadoc"

    # Comparing files is not enough. Cloudflare Pages redirects /x.html to /x
    # before it considers an asset, so a page can be present on disk, pass every
    # comparison, and still be unreachable on the deployed site -- which is
    # exactly what shipped once. This asks the Pages runtime instead.
    if [ "${WEBSITE_CHECK_JAVADOC_URLS:-true}" = "true" ]; then
      "${REPO_ROOT}/scripts/website/check-javadoc-urls.sh" "${WEBSITE_DIR}/public"
    fi
  else
    echo "Warning: python3 not found; skipping JavaDoc URL parity check." >&2
  fi
fi
