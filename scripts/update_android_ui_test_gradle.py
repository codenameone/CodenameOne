#!/usr/bin/env python3
"""Apply minimal instrumentation test configuration to the generated Gradle project."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys
from typing import Iterable, Optional

COMPILE_SDK = 35
MIN_SDK = 19
TARGET_SDK = 35
VERSION_CODE = 100
VERSION_NAME = "1.0"

ANDROID_TEST_DEPENDENCIES = (
    "androidx.test:core:1.5.0",
    "androidx.test.ext:junit:1.1.5",
    "androidx.test:rules:1.5.0",
    "androidx.test:runner:1.5.2",
    "androidx.test.espresso:espresso-core:3.5.1",
    "androidx.test.uiautomator:uiautomator:2.2.0",
)

# Snippet injected into the android { } block to keep instrumentation runs stable.
TEST_OPTIONS_SNIPPET: str = """
    testOptions {
        animationsDisabled = true
    }
"""


class GradleFile:
    def __init__(self, content: str) -> None:
        self.content = content
        self.configuration_used: str | None = None
        self.added_dependencies: list[str] = []
        self._package_name: str | None = None

    def set_package_name(self, package_name: Optional[str]) -> None:
        self._package_name = package_name

    def _iter_blocks(self) -> Iterable[tuple[str, int, int, list[str]]]:
        content = self.content
        length = len(content)
        stack: list[tuple[str, int]] = []
        i = 0
        while i < length:
            if content.startswith("//", i):
                end = content.find("\n", i)
                if end == -1:
                    break
                i = end + 1
                continue
            if content.startswith("/*", i):
                end = content.find("*/", i + 2)
                if end == -1:
                    break
                i = end + 2
                continue
            char = content[i]
            if char in ('"', "'"):
                quote = char
                i += 1
                while i < length:
                    if content[i] == "\\":
                        i += 2
                        continue
                    if content[i] == quote:
                        i += 1
                        break
                    i += 1
                continue
            if char == '}':
                if stack:
                    name, start = stack.pop()
                    parents = [entry[0] for entry in stack]
                    yield name, start, i + 1, parents
                i += 1
                continue
            match = re.match(r'[A-Za-z_][A-Za-z0-9_\.]*', content[i:])
            if match:
                name = match.group(0)
                j = i + len(name)
                while j < length and content[j].isspace():
                    j += 1
                if j < length and content[j] == '{':
                    stack.append((name, i))
                    i = j + 1
                    continue
            i += 1

    def _find_block(self, name: str, *, parent: str | None = None) -> tuple[int, int] | None:
        for block_name, start, end, parents in self._iter_blocks():
            if block_name != name:
                continue
            if parent is None:
                if not parents:
                    return start, end
            else:
                if parents and parents[-1] == parent:
                    return start, end
        return None

    def ensure_namespace(self) -> None:
        if not self._package_name:
            return
        android_block = self._find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        block_text = self.content[android_block[0]:android_block[1]]
        pattern = re.compile(r"^(\s*)namespace\s+['\"]([^'\"]+)['\"]\s*$", re.MULTILINE)

        def repl(match: re.Match[str]) -> str:
            indent = match.group(1)
            return f"{indent}namespace \"{self._package_name}\""

        if pattern.search(block_text):
            block_text = pattern.sub(repl, block_text, count=1)
        else:
            insert = block_text.find('\n', block_text.find('android'))
            if insert == -1:
                brace = block_text.find('{')
                if brace == -1:
                    raise SystemExit("Malformed android block")
                insert = brace + 1
            else:
                insert += 1
            namespace_line = f"    namespace \"{self._package_name}\"\n"
            block_text = block_text[:insert] + namespace_line + block_text[insert:]
        self.content = self.content[:android_block[0]] + block_text + self.content[android_block[1]:]

    def ensure_compile_sdk(self) -> None:
        pattern = re.compile(r"^(\s*)compileSdk(?:Version)?\s+\d+\s*$", re.MULTILINE)

        def repl(match: re.Match[str]) -> str:
            indent = match.group(1)
            return f"{indent}compileSdk {COMPILE_SDK}"

        if pattern.search(self.content):
            self.content = pattern.sub(repl, self.content, count=1)
            return
        android_block = self._find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        insert = self.content.find('\n', android_block[0]) + 1
        if insert <= 0:
            insert = android_block[0] + len("android {")
        line = f"    compileSdk {COMPILE_SDK}\n"
        self.content = self.content[:insert] + line + self.content[insert:]

    @staticmethod
    def _ensure_block_entry(block_text: str, key: str, replacement: str) -> str:
        pattern = re.compile(rf"^(\s*){key}\b.*$", re.MULTILINE)
        if pattern.search(block_text):
            def repl(match: re.Match[str]) -> str:
                indent = match.group(1)
                return f"{indent}{replacement}"

            return pattern.sub(repl, block_text, count=1)
        indent_match = re.search(r"\{\s*\n(\s*)", block_text)
        indent = indent_match.group(1) if indent_match else "        "
        brace_index = block_text.find('{')
        if brace_index == -1:
            return block_text
        insert_pos = block_text.find('\n', brace_index)
        if insert_pos == -1:
            block_text = block_text + '\n'
            insert_pos = len(block_text) - 1
        insert_pos += 1
        insertion = f"{indent}{replacement}\n"
        return block_text[:insert_pos] + insertion + block_text[insert_pos:]

    def ensure_default_config(self) -> None:
        replacements: list[tuple[str, str]] = [
            ("minSdk", f"minSdk {MIN_SDK}"),
            ("targetSdk", f"targetSdk {TARGET_SDK}"),
            ("versionCode", f"versionCode {VERSION_CODE}"),
            ("versionName", f"versionName \"{VERSION_NAME}\""),
            (
                "testInstrumentationRunner",
                "testInstrumentationRunner \"androidx.test.runner.AndroidJUnitRunner\"",
            ),
            ("multiDexEnabled", "multiDexEnabled true"),
        ]
        if self._package_name:
            replacements.insert(0, ("applicationId", f"applicationId \"{self._package_name}\""))
        block = self._find_block("defaultConfig", parent="android")
        if not block:
            lines = ["    defaultConfig {"]
            for _, line in replacements:
                lines.append(f"        {line}")
            lines.append("    }\n")
            snippet = "\n".join(lines)
            android_block = self._find_block("android")
            if not android_block:
                raise SystemExit("Unable to locate android block in Gradle file")
            insert = self.content.find('\n', android_block[0]) + 1
            if insert <= 0:
                insert = android_block[0] + len("android {")
            self.content = self.content[:insert] + snippet + self.content[insert:]
            return
        block_text = self.content[block[0]:block[1]]
        for key, replacement in replacements:
            block_text = self._ensure_block_entry(block_text, key, replacement)
        self.content = self.content[:block[0]] + block_text + self.content[block[1]:]

    def ensure_test_options(self) -> None:
        if "animationsDisabled" in self.content:
            return
        test_block = self._find_block("testOptions", parent="android")
        if test_block:
            insert = self.content.find('\n', test_block[0]) + 1
            body = "        animationsDisabled = true\n"
            self.content = (
                self.content[:insert]
                + body
                + self.content[insert:test_block[1]]
                + self.content[test_block[1]:]
            )
            return
        android_block = self._find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        insert = self.content.find('\n', android_block[0]) + 1
        if insert <= 0:
            insert = android_block[0] + len("android {")
        self.content = self.content[:insert] + TEST_OPTIONS_SNIPPET + self.content[insert:]

    def remove_dex_options(self) -> None:
        self.content = re.sub(r"\s*dexOptions\s*\{[^{}]*\}\s*", "\n", self.content)

    def convert_lint_options(self) -> None:
        block = self._find_block("lintOptions", parent="android")
        if not block:
            return
        block_text = self.content[block[0]:block[1]]
        start = block_text.find('{')
        end = block_text.rfind('}')
        if start == -1 or end == -1 or end <= start:
            self.content = (
                self.content[:block[0]]
                + "    lint {\n    }\n"
                + self.content[block[1]:]
            )
            return
        body = block_text[start + 1 : end]
        lines = []
        for line in body.splitlines():
            stripped = line.strip()
            if not stripped:
                lines.append("")
            else:
                lines.append(f"        {stripped}")
        joined = "\n".join(lines)
        if joined:
            joined = joined + "\n"
        replacement = f"    lint {{\n{joined}    }}\n"
        self.content = self.content[:block[0]] + replacement + self.content[block[1]:]

    def remove_jcenter(self) -> None:
        self.content = re.sub(r"^\s*jcenter\(\)\s*$\n?", "", self.content, flags=re.MULTILINE)

    def ensure_dependencies(self) -> None:
        block = self._find_dependencies_block()
        if not block:
            self._append_dependencies_block()
            block = self._find_dependencies_block()
            if not block:
                raise SystemExit("Unable to locate dependencies block in Gradle file after insertion")
        existing_block = self.content[block[0]:block[1]]
        configuration = self._select_configuration(existing_block, self.content)
        self.configuration_used = configuration
        insertion_point = block[1] - 1
        for coordinate in ANDROID_TEST_DEPENDENCIES:
            if self._has_dependency(existing_block, coordinate):
                continue
            combined = f"    {configuration} \"{coordinate}\"\n"
            self.content = (
                self.content[:insertion_point]
                + combined
                + self.content[insertion_point:]
            )
            insertion_point += len(combined)
            existing_block += combined
            self.added_dependencies.append(coordinate)

    @staticmethod
    def _has_dependency(block: str, coordinate: str) -> bool:
        escaped = re.escape(coordinate)
        return bool(
            re.search(rf"androidTestImplementation\s+['\"]{escaped}['\"]", block)
            or re.search(rf"androidTestCompile\s+['\"]{escaped}['\"]", block)
        )

    def _find_dependencies_block(self) -> tuple[int, int] | None:
        preferred: tuple[int, int] | None = None
        fallback: tuple[int, int] | None = None
        for name, start, end, parents in self._iter_blocks():
            if name != "dependencies":
                continue
            if parents and parents[-1] in {"buildscript", "pluginManagement"}:
                continue
            block_content = self.content[start:end]
            if re.search(
                r"^\s*(implementation|api|compile|androidTestImplementation|androidTestCompile)\b",
                block_content,
                re.MULTILINE,
            ):
                preferred = (start, end)
                break
            if "classpath" in block_content and not re.search(
                r"^\s*(implementation|api|compile)\b", block_content, re.MULTILINE
            ):
                continue
            if fallback is None:
                fallback = (start, end)
        return preferred or fallback

    def _append_dependencies_block(self) -> None:
        if not self.content.endswith("\n"):
            self.content += "\n"
        self.content += "\ndependencies {\n}\n"

    @staticmethod
    def _select_configuration(block: str, content: str) -> str:
        if re.search(r"^\s*androidTestImplementation\b", block, re.MULTILINE):
            return "androidTestImplementation"
        if re.search(r"^\s*androidTestCompile\b", block, re.MULTILINE):
            return "androidTestCompile"
        if re.search(r"^\s*androidTestImplementation\b", content, re.MULTILINE):
            return "androidTestImplementation"
        if re.search(r"^\s*androidTestCompile\b", content, re.MULTILINE):
            return "androidTestCompile"
        if re.search(r"^\s*implementation\b", block, re.MULTILINE) or re.search(
            r"^\s*implementation\b", content, re.MULTILINE
        ):
            return "androidTestImplementation"
        if re.search(r"^\s*compile\b", block, re.MULTILINE) or re.search(
            r"^\s*compile\b", content, re.MULTILINE
        ):
            return "androidTestCompile"
        return "androidTestImplementation"

    def apply(self) -> None:
        self.ensure_namespace()
        self.ensure_compile_sdk()
        self.ensure_default_config()
        self.ensure_test_options()
        self.remove_dex_options()
        self.convert_lint_options()
        self.remove_jcenter()
        self.ensure_dependencies()

    def summary(self) -> str:
        configuration = self.configuration_used or "<unknown>"
        if self.added_dependencies:
            deps = ", ".join(self.added_dependencies)
        else:
            deps = "none (already present)"
        return (
            "Instrumentation dependency configuration: "
            f"{configuration}; added dependencies: {deps}"
        )


def process(path: pathlib.Path, package_name: Optional[str]) -> None:
    content = path.read_text(encoding="utf-8")
    if "android {" not in content:
        raise SystemExit(
            "Selected Gradle file doesn't contain an android { } block. Check module path."
        )
    editor = GradleFile(content)
    editor.set_package_name(package_name)
    editor.apply()
    path.write_text(editor.content, encoding="utf-8")
    print(editor.summary())


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("gradle_file", type=pathlib.Path)
    parser.add_argument("--package-name", dest="package_name", default=None)
    args = parser.parse_args(argv)

    if not args.gradle_file.is_file():
        parser.error(f"Gradle file not found: {args.gradle_file}")

    process(args.gradle_file, args.package_name)
    return 0


if __name__ == "__main__":
    sys.exit(main())
