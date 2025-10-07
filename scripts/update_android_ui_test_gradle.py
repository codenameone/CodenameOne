#!/usr/bin/env python3
"""Apply minimal instrumentation test configuration to the generated Gradle project."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys

COMPILE_SDK_LINE = "    compileSdkVersion 33\n"

TEST_OPTIONS_SNIPPET = """    testOptions {\n        animationsDisabled = true\n    }\n\n"""

DEFAULT_CONFIG_SNIPPET = """    defaultConfig {\n        testInstrumentationRunner \"androidx.test.runner.AndroidJUnitRunner\"\n    }\n\n"""

ANDROID_TEST_DEPENDENCIES = (
    "androidx.test:core:1.5.0",
    "androidx.test.ext:junit:1.1.5",
    "androidx.test:rules:1.5.0",
    "androidx.test:runner:1.5.2",
    "androidx.test.espresso:espresso-core:3.5.1",
    "androidx.test.uiautomator:uiautomator:2.2.0",
)


class GradleFile:
    def __init__(self, content: str) -> None:
        self.content = content
        self.configuration_used: str | None = None
        self.added_dependencies: list[str] = []

    def _iter_blocks(self):
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

    def ensure_compile_sdk(self) -> None:
        if re.search(r"compileSdkVersion\s+\d+", self.content):
            return
        android_block = self._find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        insert = self.content.find('\n', android_block[0]) + 1
        if insert <= 0:
            insert = android_block[0] + len("android {")
        self.content = self.content[:insert] + COMPILE_SDK_LINE + self.content[insert:]

    def ensure_test_options(self) -> None:
        if "animationsDisabled" in self.content:
            return
        test_block = self._find_block("testOptions", parent="android")
        if test_block:
            insert = self.content.find('\n', test_block[0]) + 1
            body = "        animationsDisabled = true\n"
            self.content = (
                self.content[:insert] + body + self.content[insert:test_block[1]] + self.content[test_block[1]:]
            )
            return
        android_block = self._find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        insert = self.content.find('\n', android_block[0]) + 1
        if insert <= 0:
            insert = android_block[0] + len("android {")
        self.content = self.content[:insert] + TEST_OPTIONS_SNIPPET + self.content[insert:]

    def ensure_instrumentation_runner(self) -> None:
        if "testInstrumentationRunner" in self.content:
            return
        default_block = self._find_block("defaultConfig", parent="android")
        if default_block:
            insert = self.content.find('\n', default_block[0]) + 1
            line = "        testInstrumentationRunner \"androidx.test.runner.AndroidJUnitRunner\"\n"
            self.content = (
                self.content[:insert] + line + self.content[insert:default_block[1]] + self.content[default_block[1]:]
            )
            return
        android_block = self._find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        insert = self.content.find('\n', android_block[0]) + 1
        if insert <= 0:
            insert = android_block[0] + len("android {")
        self.content = self.content[:insert] + DEFAULT_CONFIG_SNIPPET + self.content[insert:]

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
            if re.search(r"^\s*(implementation|api|compile|androidTestImplementation|androidTestCompile)\b", block_content, re.MULTILINE):
                preferred = (start, end)
                break
            if "classpath" in block_content and not re.search(r"^\s*(implementation|api|compile)\b", block_content, re.MULTILINE):
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
        self.ensure_compile_sdk()
        self.ensure_test_options()
        self.ensure_instrumentation_runner()
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


def process(path: pathlib.Path) -> None:
    editor = GradleFile(path.read_text(encoding="utf-8"))
    editor.apply()
    path.write_text(editor.content, encoding="utf-8")
    print(editor.summary())


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("gradle_file", type=pathlib.Path)
    args = parser.parse_args(argv)

    if not args.gradle_file.is_file():
        parser.error(f"Gradle file not found: {args.gradle_file}")

    process(args.gradle_file)
    return 0


if __name__ == "__main__":
    sys.exit(main())
