#!/usr/bin/env python3
"""Apply minimal instrumentation test configuration to the generated Gradle project."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys

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

    def find_block(self, name: str, start: int = 0) -> tuple[int, int] | None:
        pattern = re.compile(rf"(^\s*{re.escape(name)}\s*\{{)", re.MULTILINE)
        match = pattern.search(self.content, start)
        if not match:
            return None
        start = match.start()
        index = match.end()
        depth = 1
        while index < len(self.content):
            char = self.content[index]
            if char == '{':
                depth += 1
            elif char == '}':
                depth -= 1
                if depth == 0:
                    return start, index + 1
            index += 1
        return None

    def ensure_test_options(self) -> None:
        if "animationsDisabled" in self.content:
            return
        test_block = self.find_block("testOptions")
        if test_block:
            insert = self.content.find('\n', test_block[0]) + 1
            body = "        animationsDisabled = true\n"
            self.content = (
                self.content[:insert] + body + self.content[insert:test_block[1]] + self.content[test_block[1]:]
            )
            return
        android_block = self.find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        insert = self.content.find('\n', android_block[0]) + 1
        if insert <= 0:
            insert = android_block[0] + len("android {")
        self.content = self.content[:insert] + TEST_OPTIONS_SNIPPET + self.content[insert:]

    def ensure_instrumentation_runner(self) -> None:
        if "testInstrumentationRunner" in self.content:
            return
        default_block = self.find_block("defaultConfig")
        if default_block:
            insert = self.content.find('\n', default_block[0]) + 1
            line = "        testInstrumentationRunner \"androidx.test.runner.AndroidJUnitRunner\"\n"
            self.content = (
                self.content[:insert] + line + self.content[insert:default_block[1]] + self.content[default_block[1]:]
            )
            return
        android_block = self.find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        insert = self.content.find('\n', android_block[0]) + 1
        if insert <= 0:
            insert = android_block[0] + len("android {")
        self.content = self.content[:insert] + DEFAULT_CONFIG_SNIPPET + self.content[insert:]

    def ensure_dependencies(self) -> None:
        block = self._find_dependencies_block()
        if not block:
            raise SystemExit("Unable to locate dependencies block in Gradle file")
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
        search_start = 0
        while True:
            block = self.find_block("dependencies", start=search_start)
            if not block:
                return None
            block_text = self.content[block[0]:block[1]]
            has_android_entries = re.search(
                r"^\s*(implementation|api|compile|androidTestImplementation|androidTestCompile)\b",
                block_text,
                re.MULTILINE,
            )
            has_only_classpath = (
                not has_android_entries
                and re.search(r"^\s*classpath\b", block_text, re.MULTILINE)
            )
            if has_only_classpath:
                search_start = block[1]
                continue
            return block

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
