#!/usr/bin/env python3
"""Classify every Java snippet in the developer guide, the API reference and the
blog the way the inline Playground treats it when a reader clicks "Try it".

Every Java block on those pages carries a "Try it" button that opens it in an
editor with a live preview (docs/website/static/js/cn1-snippet-runner.js). A
snippet that compiles runs as it stands; one that needs the surrounding text --
a field, a variable, a missing import -- opens with a compile error the reader
has to fix first. This script compiles every snippet with the Playground's own
compiler (DocSnippetRunnabilityHarness) and holds the ones that do not run
against scripts/doc-snippet-baseline.txt: a ratchet of existing debt, not an
allow-list. A NEW snippet that does not run fails the check; make it
self-contained, the way the snippets that do run are.

Snippets are keyed by file and a hash of their code, so editing text around a
snippet does not move its key, and fixing one shows it as stale in the baseline.

  scripts/check-doc-snippets.py                    # check against the baseline
  scripts/check-doc-snippets.py --write-baseline   # after fixing snippets
  scripts/check-doc-snippets.py --report out.tsv   # every snippet and its status

Needs the Playground's common module built, as scripts/java-snippet-to-playground-uri.sh
does (extra Maven arguments in SNIPPET_MVN_ARGS).
"""
import argparse
import hashlib
import json
import os
import pathlib
import re
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
GUIDE = ROOT / "docs" / "developer-guide"
CORE = ROOT / "CodenameOne" / "src"
# The client API reference is rendered from both trees, so both carry "Try it".
CLDC = ROOT / "Ports" / "CLDC11" / "src"
BLOG = ROOT / "docs" / "website" / "content" / "blog"
BASELINE = ROOT / "scripts" / "doc-snippet-baseline.txt"
PLAYGROUND = ROOT / "scripts" / "cn1playground"
BUILD_DIR = PLAYGROUND / "common" / "target" / "snippet-cli"

SOURCE_JAVA = re.compile(r"^\[source,\s*java\b")
# Server code (the backend chapters, anything included from a backend/src tree) is
# marked with this role: a client Playground cannot run it, so it gets no button.
NO_TRY_ROLE = "role=cn1-no-try"
# A Markdown fence the site renders as Java: ```java, optionally with Hugo
# attributes ({class="cn1-no-try"} opts a block out, as the guide's role does).
MD_FENCE = re.compile(r"^(\s*)(`{3,}|~{3,})\s*java\s*(\{.*\})?\s*$")
# cn1-snippet-runner.js gives no button to code importing the backend API, which
# the browser Playground does not ship. Keep the two in step.
BACKEND_IMPORT = re.compile(r"^\s*import\s+(static\s+)?com\.codename1\.backend\b", re.M)
INCLUDE = re.compile(r"^include::([^\[]+)\[(.*)\]\s*$")
TAG_MARKER = re.compile(r"(tag|end)::([\w.-]+)\[\]")


def dedent(lines):
    indents = [len(l) - len(l.lstrip()) for l in lines if l.strip()]
    cut = min(indents) if indents else 0
    return [l[cut:] if l.strip() else "" for l in lines]


def include_lines(base_dir, target, attrs):
    path = (base_dir / target).resolve()
    if not path.is_file():
        return None
    lines = path.read_text(encoding="utf-8").splitlines()
    opts = dict(a.split("=", 1) for a in attrs.split(",") if "=" in a)
    tags = [t for t in opts.get("tags", opts.get("tag", "")).split(";") if t]
    if tags:
        picked, depth = [], {}
        for line in lines:
            m = TAG_MARKER.search(line)
            if m and m.group(2) in tags:
                depth[m.group(2)] = m.group(1) == "tag"
                continue
            if any(depth.values()):
                picked.append(line)
        lines = picked
    # Asciidoctor drops every tag directive line from what it includes.
    lines = [l for l in lines if not TAG_MARKER.search(l)]
    if opts.get("indent") == "0":
        lines = dedent(lines)
    return lines


def guide_snippets():
    for doc in sorted(GUIDE.glob("*.asciidoc")):
        lines = doc.read_text(encoding="utf-8").splitlines()
        i = 0
        while i < len(lines):
            if SOURCE_JAVA.match(lines[i]) and NO_TRY_ROLE not in lines[i]:
                j = i + 1
                while j < len(lines) and not lines[j].strip():
                    j += 1
                if j < len(lines) and re.match(r"^-{4,}\s*$", lines[j]):
                    fence = lines[j].strip()
                    k = j + 1
                    body = []
                    while k < len(lines) and lines[k].strip() != fence:
                        m = INCLUDE.match(lines[k])
                        if m:
                            inc = include_lines(doc.parent, m.group(1), m.group(2))
                            body.extend(inc if inc is not None else [lines[k]])
                        else:
                            body.append(lines[k])
                        k += 1
                    # Callout markers are rendered as badges, not code.
                    code = "\n".join(re.sub(r"\s*(//\s*)?<\d+>\s*$", "", l) for l in body).rstrip()
                    yield str(doc.relative_to(ROOT)), i + 1, code
                    i = k
                elif j < len(lines) and INCLUDE.match(lines[j]):
                    # An undelimited block: asciidoctor renders the included lines
                    # themselves as the Java block.
                    m = INCLUDE.match(lines[j])
                    inc = include_lines(doc.parent, m.group(1), m.group(2)) or []
                    code = "\n".join(re.sub(r"\s*(//\s*)?<\d+>\s*$", "", l) for l in inc).rstrip()
                    yield str(doc.relative_to(ROOT)), i + 1, code
                    i = j
            i += 1


def blog_snippets():
    for post in sorted(BLOG.rglob("*.md")):
        lines = post.read_text(encoding="utf-8").splitlines()
        i = 0
        while i < len(lines):
            m = MD_FENCE.match(lines[i])
            if not m:
                i += 1
                continue
            indent, fence, attrs = m.group(1), m.group(2), m.group(3) or ""
            k = i + 1
            body = []
            while k < len(lines) and not (lines[k].strip().startswith(fence[0] * len(fence))
                                          and not lines[k].strip().strip(fence[0])):
                body.append(lines[k][len(indent):] if lines[k].startswith(indent) else lines[k])
                k += 1
            code = "\n".join(body).rstrip()
            if "cn1-no-try" not in attrs and not BACKEND_IMPORT.search(code):
                yield str(post.relative_to(ROOT)), i + 1, code
            i = k + 1


def javadoc_snippets():
    for tree in (CORE, CLDC):
        out = subprocess.run([str(ROOT / "scripts" / "extract-javadoc-java-snippets.sh"), str(tree)],
                             cwd=ROOT, text=True, capture_output=True, check=True).stdout
        for line in out.splitlines():
            if line.strip():
                r = json.loads(line)
                # Unpublished, so no page shows these snippets: the same filter
                # .github/scripts/build_javadocs.sh applies to its source list.
                if "/com/codename1/impl/" in "/" + r["sourceFile"]:
                    continue
                yield r["sourceFile"], r.get("startLine", 0), r.get("code", "")


def harness_classpath():
    cp_file = BUILD_DIR / "classpath.txt"
    args = os.environ.get("SNIPPET_MVN_ARGS", "").split()
    pom = str(PLAYGROUND / "common" / "pom.xml")
    # Always rebuilt (incrementally): a stale harness or API stub library would
    # classify the snippets against an API that is no longer the Playground's.
    # Built through the Playground reactor with -am and installed, the way
    # tools/run-playground-smoke-tests.sh does: common depends on the cn1lib
    # modules beside it, and on a fresh runner nothing has installed them yet.
    subprocess.run(["mvn", "-q", "-B", "-nsu", *args, "-f", str(PLAYGROUND / "pom.xml"),
                    "-pl", "common", "-am", "-DskipTests", "install"], check=True)
    subprocess.run(["mvn", "-q", "-B", "-nsu", *args, "-f", pom, "-DskipTests", "test-compile"], check=True)
    if not cp_file.is_file():
        BUILD_DIR.mkdir(parents=True, exist_ok=True)
        subprocess.run(["mvn", "-q", "-B", "-nsu", *args, "-f", pom, "dependency:build-classpath",
                        "-Dmdep.includeScope=test", "-Dmdep.outputFile=" + str(cp_file)], check=True)
    common = PLAYGROUND / "common" / "target"
    return f"{common / 'test-classes'}:{common / 'classes'}:{cp_file.read_text().strip()}"


def classify(snippets):
    with tempfile.TemporaryDirectory() as tmp:
        names = {}
        for n, (key, src, line, code) in enumerate(snippets):
            name = f"s{n:05d}.java"
            names[name] = (key, src, line)
            pathlib.Path(tmp, name).write_text(code, encoding="utf-8")
        out = subprocess.run(["java", "-Djava.awt.headless=true", "-cp", harness_classpath(),
                              "com.codenameone.playground.DocSnippetRunnabilityHarness", tmp],
                             text=True, capture_output=True, check=True).stdout
    results = []
    for row in out.splitlines():
        name, status, err_line, message = (row.split("\t") + ["", "", ""])[:4]
        key, src, line = names[name]
        results.append((key, src, line, status, err_line, message))
    return results


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--write-baseline", action="store_true")
    ap.add_argument("--report", help="write every snippet's status to this TSV file")
    args = ap.parse_args()

    snippets, seen = [], set()
    for src, line, code in list(guide_snippets()) + list(javadoc_snippets()) + list(blog_snippets()):
        if not code.strip():
            continue
        key = f"{src}#{hashlib.sha1(code.encode('utf-8')).hexdigest()[:12]}"
        if key in seen:  # the same code twice in one file is one snippet to fix
            continue
        seen.add(key)
        snippets.append((key, src, line, code))

    results = classify(snippets)
    failing = {r[0]: r for r in results if r[3] != "runnable"}
    counts = {}
    for r in results:
        counts[r[3]] = counts.get(r[3], 0) + 1
    print(f"{len(results)} Java snippets: " + ", ".join(f"{v} {k}" for k, v in sorted(counts.items())))

    if args.report:
        with open(args.report, "w", encoding="utf-8") as f:
            f.write("key\tsource\tline\tstatus\terror line\tmessage\n")
            for r in results:
                f.write("\t".join(str(x) for x in r) + "\n")

    if args.write_baseline:
        BASELINE.write_text("".join(k + "\n" for k in sorted(failing)), encoding="utf-8")
        print(f"Wrote {len(failing)} entries to {BASELINE.relative_to(ROOT)}")
        return 0

    baseline = set()
    if BASELINE.is_file():
        baseline = {l.strip() for l in BASELINE.read_text(encoding="utf-8").splitlines()
                    if l.strip() and not l.startswith("#")}
    new = sorted(k for k in failing if k not in baseline)
    stale = sorted(baseline - set(failing))
    failed = False
    if stale:
        # Fatal, not a note: a key left behind would exempt the same broken code
        # if it were ever restored in that file.
        print(f"{len(stale)} baseline entries no longer fail (fixed or removed); "
              f"run with --write-baseline to drop them:")
        for k in stale:
            print(f"  {k}")
        failed = True
    if new:
        print(f"{len(new)} snippets do not run in the inline Playground and are not in the baseline:")
        for k in new:
            _, src, line, status, err_line, message = failing[k]
            print(f"  {src}:{line}: {status}: line {err_line}: {message}")
        print("Make each snippet self-contained: it should compile on its own, the way the "
              "Playground compiles it (see scripts/cn1playground/README.md for the shapes it runs).")
        failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
