#!/usr/bin/env python3
import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


SOURCE_RE = re.compile(r"^\[source,([A-Za-z0-9_#+. -]+)(?:,.*)?\]\s*$")
INCLUDE_RE = re.compile(r"^include::([^\[]+)\[(.*)\]\s*$")
TAG_RE = re.compile(r"(?:^|,)tag=([^,\]]+)")
LISTING_DELIMITER_RE = re.compile(r"^-{4,}\s*$")
TAG_BLOCK_RE = re.compile(r"// tag::([^\[]+)\[\]\n(.*?)\n// end::\1\[\]", re.S)
JAVA_IDENTIFIER_RE = re.compile(r"^[A-Za-z_$][A-Za-z0-9_$]*\.java$")


def parse_args():
    parser = argparse.ArgumentParser(
        description="Validate developer-guide source blocks against demo-backed includes."
    )
    parser.add_argument(
        "--docs-dir",
        default="docs/developer-guide",
        help="Directory containing guide .adoc/.asciidoc files.",
    )
    return parser.parse_args()


def guide_files(docs_dir):
    return sorted(
        p for p in Path(docs_dir).iterdir()
        if p.is_file() and p.suffix in (".adoc", ".asciidoc")
    )


def extract_source_blocks(docs_dir):
    docs_dir = Path(docs_dir)
    inline = []
    includes = []
    errors = []

    for source in guide_files(docs_dir):
        rel_source = str(source)
        lines = source.read_text(encoding="utf-8", errors="replace").splitlines()
        i = 0
        while i < len(lines):
            source_match = SOURCE_RE.match(lines[i])
            if not source_match:
                i += 1
                continue

            language = source_match.group(1)
            start_line = i + 1
            j = i + 1
            while j < len(lines) and not lines[j].strip():
                j += 1

            if j >= len(lines):
                errors.append(f"{rel_source}:{start_line}: source block has no body")
                i += 1
                continue

            include_match = INCLUDE_RE.match(lines[j].strip())
            if include_match:
                target = include_match.group(1)
                attrs = include_match.group(2)
                includes.append(
                    {
                        "sourceFile": rel_source,
                        "line": j + 1,
                        "language": language,
                        "target": target,
                        "attrs": attrs,
                    }
                )
                i = j + 1
                continue

            delimiter = lines[j].strip()
            if not LISTING_DELIMITER_RE.match(delimiter):
                errors.append(
                    f"{rel_source}:{start_line}: source block is neither include-backed nor fenced with ----"
                )
                i = j + 1
                continue

            body_start = j + 1
            k = body_start
            while k < len(lines) and lines[k].strip() != delimiter:
                k += 1
            if k >= len(lines):
                errors.append(f"{rel_source}:{start_line}: source block is missing closing ----")
                i = j + 1
                continue

            code_lines = lines[body_start:k]
            nonempty_code_lines = [line.strip() for line in code_lines if line.strip()]
            if len(nonempty_code_lines) == 1:
                include_match = INCLUDE_RE.match(nonempty_code_lines[0])
                if include_match:
                    target = include_match.group(1)
                    attrs = include_match.group(2)
                    includes.append(
                        {
                            "sourceFile": rel_source,
                            "line": body_start + 1,
                            "language": language,
                            "target": target,
                            "attrs": attrs,
                        }
                    )
                    i = k + 1
                    continue

            code = "\n".join(code_lines)
            inline.append(
                {
                    "sourceFile": rel_source,
                    "line": start_line,
                    "language": language,
                    "preview": " ".join(code.strip().split())[:120],
                }
            )
            i = k + 1

    return inline, includes, errors


def validate_includes(includes):
    errors = []
    cache = {}
    java_snippets = []
    for item in includes:
        if not item["target"].startswith("../demos/"):
            errors.append(
                f"{item['sourceFile']}:{item['line']}: source include must target ../demos/: {item['target']}"
            )
            continue

        source_path = Path(item["sourceFile"])
        target_path = (source_path.parent / item["target"]).resolve()
        if not target_path.exists():
            errors.append(
                f"{item['sourceFile']}:{item['line']}: include target not found: {item['target']}"
            )
            continue

        if item["language"].strip().lower() == "java":
            if item["target"].endswith(".java.txt"):
                errors.append(
                    f"{item['sourceFile']}:{item['line']}: Java snippets must not be hidden in .java.txt files: "
                    f"{item['target']}"
                )
            if target_path.suffix == ".java" and not JAVA_IDENTIFIER_RE.match(target_path.name):
                errors.append(
                    f"{item['sourceFile']}:{item['line']}: Java snippet fixture must use a valid Java filename: "
                    f"{item['target']}"
                )
        if item["language"].strip().lower() in ("java", "kotlin") and (
            "/demos/common/src/main/snippets/" in item["target"]
            or item["target"].startswith("../demos/common/src/main/snippets/")
        ):
            errors.append(
                f"{item['sourceFile']}:{item['line']}: Java/Kotlin guide snippets in the common demo "
                f"must live under a compiled source root, not src/main/snippets: {item['target']}"
            )
        if item["language"].strip().lower() in ("javascript", "html") and (
            "/demos/common/" in item["target"] or item["target"].startswith("../demos/common/")
        ):
            errors.append(
                f"{item['sourceFile']}:{item['line']}: JavaScript/HTML snippets must live under "
                f"the JavaScript demo module, not common: {item['target']}"
            )

        tag_match = TAG_RE.search(item["attrs"])
        if not tag_match:
            continue

        tag = tag_match.group(1)
        text = cache.get(target_path)
        if text is None:
            text = target_path.read_text(encoding="utf-8", errors="replace")
            cache[target_path] = text
        if f"tag::{tag}[]" not in text:
            errors.append(
                f"{item['sourceFile']}:{item['line']}: include tag not found in {item['target']}: {tag}"
            )
        if f"end::{tag}[]" not in text:
            errors.append(
                f"{item['sourceFile']}:{item['line']}: include end tag not found in {item['target']}: {tag}"
            )
        if item["language"].strip().lower() == "java" and "/src/main/java/" not in str(target_path):
            java_snippets.append(
                {
                    "sourceFile": item["sourceFile"],
                    "line": item["line"],
                    "target": item["target"],
                    "targetPath": target_path,
                    "tag": tag,
                    "text": text,
                }
            )
    if os.environ.get("CN1_VALIDATE_JAVA_SNIPPET_COMPILE") == "1":
        errors.extend(validate_java_snippets(java_snippets))
    return errors


def extract_tag_body(text, tag):
    match = re.search(
        r"// tag::" + re.escape(tag) + r"\[\]\n(.*?)\n// end::" + re.escape(tag) + r"\[\]",
        text,
        re.S,
    )
    if not match:
        return None
    return match.group(1)


def split_java_snippet(body):
    imports = []
    code = []
    for line in body.splitlines():
        stripped = line.strip()
        if stripped.startswith("package "):
            continue
        if stripped.startswith("import "):
            imports.append(stripped)
            continue
        code.append(line)
    return imports, "\n".join(code).strip()


def normalize_java_snippet_for_compile(code):
    normalized = []
    for line in code.splitlines():
        line = re.sub(r"\s*<\d+>\s*$", "", line)
        stripped = line.strip()
        if stripped in ("...", "...."):
            normalized.append(line[: len(line) - len(line.lstrip())] + "/* omitted */")
        else:
            normalized.append(line.replace("....", "/* omitted */").replace("...", "null"))
    return "\n".join(normalized)


def looks_like_member_snippet(code):
    in_block_comment = False
    first = ""
    for line in code.splitlines():
        stripped_line = line.strip()
        if in_block_comment:
            if "*/" in stripped_line:
                in_block_comment = False
            continue
        if not stripped_line or stripped_line.startswith("//"):
            continue
        if stripped_line.startswith("/*"):
            if "*/" not in stripped_line:
                in_block_comment = True
            continue
        if stripped_line.startswith("*"):
            continue
        first = stripped_line
        break
    return (
        first.startswith("@")
        or first.startswith("public ")
        or first.startswith("private ")
        or first.startswith("protected ")
        or first.startswith("abstract ")
        or first.startswith("interface ")
        or first.startswith("enum ")
    )


def java_compile_classpath():
    candidates = [
        Path("docs/demos/common/target/classes"),
        Path("maven/core/target/classes"),
        Path.home() / ".m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar",
        Path.home() / ".m2/repository/com/codenameone/java-runtime/8.0-SNAPSHOT/java-runtime-8.0-SNAPSHOT.jar",
    ]
    return os.pathsep.join(str(p) for p in candidates if p.exists())


def java_harness_source(class_name, imports, code, as_member):
    import_lines = "\n".join(imports)
    fixture_imports = """
import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;
"""
    fields = """
    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
"""
    helper_members, statements = split_top_level_java_members(code)
    if helper_members and statements:
        indented = "\n".join("        " + line for line in statements.splitlines())
        return (
            f"{fixture_imports}\n{import_lines}\nclass {class_name} {{\n{fields}\n"
            f"{helper_members}\n"
            f"    void snippet() throws Exception {{\n{indented}\n    }}\n}}\n"
        )
    if helper_members and not statements:
        return f"{fixture_imports}\n{import_lines}\nclass {class_name} {{\n{fields}\n{helper_members}\n}}\n"
    constructor_match = re.match(r"\s*public\s+([A-Z][A-Za-z0-9_$]*)\s*\(", code)
    if as_member and constructor_match:
        nested_class = constructor_match.group(1)
        return f"{fixture_imports}\n{import_lines}\nclass {class_name} {{\n{fields}\nclass {nested_class} {{\n{code}\n}}\n}}\n"
    if as_member:
        return f"{fixture_imports}\n{import_lines}\nclass {class_name} {{\n{fields}\n{code}\n}}\n"
    indented = "\n".join("        " + line for line in code.splitlines())
    return f"{fixture_imports}\n{import_lines}\nclass {class_name} {{\n{fields}\n    void snippet() throws Exception {{\n{indented}\n    }}\n}}\n"


def split_top_level_java_members(code):
    lines = code.splitlines()
    has_method_member = any(is_top_level_method_start(line) for line in lines)
    members = []
    statements = []
    i = 0
    found_member = False
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        if not stripped:
            (members if found_member else statements).append(line)
            i += 1
            continue
        if is_top_level_field(stripped, has_method_member):
            members.append(line)
            found_member = True
            i += 1
            continue
        if is_top_level_method_start(line):
            block = [line]
            depth = line.count("{") - line.count("}")
            i += 1
            while i < len(lines) and depth > 0:
                block.append(lines[i])
                depth += lines[i].count("{") - lines[i].count("}")
                i += 1
            members.extend(block)
            found_member = True
            continue
        statements.append(line)
        i += 1
    return "\n".join(members).strip(), "\n".join(statements).strip()


def is_top_level_field(stripped, has_method_member):
    if (
        stripped.startswith("public static final ")
        or stripped.startswith("private static final ")
        or stripped.startswith("static final ")
    ) and stripped.endswith(";"):
        return True
    if not has_method_member:
        return False
    return re.match(
        r"^(?:[A-Za-z_$][A-Za-z0-9_$.<>?, \[\]]+)\s+[A-Za-z_$][A-Za-z0-9_$]*\s*=.+;$",
        stripped,
    ) is not None


def is_top_level_method_start(line):
    if line.startswith((" ", "\t")):
        return False
    stripped = line.strip()
    if "{" not in stripped:
        return False
    return re.match(
        r"^(public|private|protected)?\s*([A-Za-z_$][A-Za-z0-9_$<>, ?\[\]]+\s+)+[A-Za-z_$][A-Za-z0-9_$]*\s*\([^;]*\)\s*(throws\s+[A-Za-z0-9_., ]+\s*)?\{",
        stripped,
    ) is not None


def write_junit_stubs(src_dir):
    junit_dir = src_dir / "org/junit/jupiter/api"
    junit_dir.mkdir(parents=True, exist_ok=True)
    (junit_dir / "Test.java").write_text(
        "package org.junit.jupiter.api;\npublic @interface Test {}\n",
        encoding="utf-8",
    )
    (junit_dir / "Assertions.java").write_text(
        "package org.junit.jupiter.api;\n"
        "public final class Assertions {\n"
        "  public static void assertEquals(Object expected, Object actual) {}\n"
        "  public static void assertTrue(boolean value) {}\n"
        "  public static void assertFalse(boolean value) {}\n"
        "}\n",
        encoding="utf-8",
    )


def validate_java_snippets(java_snippets):
    if not java_snippets:
        return []
    javac = shutil.which("javac")
    if javac is None:
        return ["javac not found; cannot compile developer-guide Java snippets."]

    errors = []
    with tempfile.TemporaryDirectory(prefix="cn1-guide-java-snippets-") as tmp:
        tmp_dir = Path(tmp)
        src_dir = tmp_dir / "src"
        classes_dir = tmp_dir / "classes"
        src_dir.mkdir()
        classes_dir.mkdir()
        write_junit_stubs(src_dir)
        snippet_locations = {}

        for index, item in enumerate(java_snippets, start=1):
            body = extract_tag_body(item["text"], item["tag"])
            if body is None:
                continue
            imports, code = split_java_snippet(body)
            code = normalize_java_snippet_for_compile(code)
            if not code:
                continue

            class_name = f"GuideSnippet{index}"
            source = src_dir / f"{class_name}.java"
            source.write_text(
                java_harness_source(class_name, imports, code, looks_like_member_snippet(code)),
                encoding="utf-8",
            )
            snippet_locations[source.name] = item

        sources = sorted(str(p) for p in src_dir.rglob("*.java"))
        cmd = [
            javac,
            "--release",
            "17",
            "-proc:none",
            "-Xmaxerrs",
            "200",
            "-cp",
            java_compile_classpath(),
            "-d",
            str(classes_dir),
            *sources,
        ]
        result = subprocess.run(cmd, text=True, capture_output=True)
        if result.returncode != 0:
            reported = set()
            for line in result.stderr.splitlines():
                match = re.search(r"([^/\s]+\.java):\d+:", line)
                if not match:
                    continue
                item = snippet_locations.get(match.group(1))
                if item is None:
                    continue
                key = (item["sourceFile"], item["line"], item["target"], item["tag"])
                if key in reported:
                    continue
                reported.add(key)
                errors.append(
                    f"{item['sourceFile']}:{item['line']}: Java snippet does not compile: "
                    f"{item['target']} tag={item['tag']}\n{line}"
                )
                if len(reported) >= 50:
                    break
            if not errors:
                first_error = "\n".join(result.stderr.splitlines()[:20])
                errors.append(f"Developer guide Java snippet compilation failed.\n{first_error}")
    return errors


def main():
    args = parse_args()
    inline, includes, errors = extract_source_blocks(args.docs_dir)
    errors.extend(validate_includes(includes))

    if inline:
        errors.append(
            f"{len(inline)} inline source block(s) found. "
            "Move all guide snippets into docs/demos and include them by tag."
        )
        for record in inline[:50]:
            errors.append(
                f"{record['sourceFile']}:{record['line']}: [{record['language']}] {record['preview']}"
            )

    print(f"Validated {len(includes)} include-backed source block(s).")

    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
