#!/usr/bin/env python3
"""Apply minimal instrumentation test configuration to the generated Gradle project."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys

TEST_OPTIONS_SNIPPET = """    testOptions {\n        animationsDisabled = true\n    }\n\n"""

DEFAULT_CONFIG_SNIPPET = """    defaultConfig {\n        testInstrumentationRunner \"androidx.test.runner.AndroidJUnitRunner\"\n    }\n\n"""

CONFIGURATION_GUARDS = (
    "configurations.maybeCreate(\"androidTestImplementation\")\n",
    "configurations.maybeCreate(\"androidTestCompile\")\n",
)

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

    def find_block(self, name: str) -> tuple[int, int] | None:
        pattern = re.compile(rf"(^\s*{re.escape(name)}\s*\{{)", re.MULTILINE)
        match = pattern.search(self.content)
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

    def ensure_configuration_guards(self) -> None:
        missing = [snippet for snippet in CONFIGURATION_GUARDS if snippet not in self.content]
        if not missing:
            return
        insert = 0
        plugins_block = self.find_block("plugins")
        if plugins_block:
            insert = plugins_block[1] + 1
        self.content = (
            self.content[:insert] + "".join(missing) + "\n" + self.content[insert:]
        )

    def ensure_dependencies(self) -> None:
        block = self.find_block("dependencies")
        if not block:
            raise SystemExit("Unable to locate dependencies block in Gradle file")
        insertion_point = block[1] - 1
        existing_block = self.content[block[0]:block[1]]
        for coordinate in ANDROID_TEST_DEPENDENCIES:
            if self._has_dependency(existing_block, coordinate):
                continue
            line = (
                f"    add(\"androidTestImplementation\", \"{coordinate}\")\n"
                f"    add(\"androidTestCompile\", \"{coordinate}\")\n"
            )
            combined = "".join(line)
            self.content = (
                self.content[:insertion_point]
                + combined
                + self.content[insertion_point:]
            )
            insertion_point += len(combined)
            existing_block += combined

    @staticmethod
    def _has_dependency(block: str, coordinate: str) -> bool:
        escaped = re.escape(coordinate)
        return bool(
            re.search(rf"androidTestImplementation\s+['\"]{escaped}['\"]", block)
            or re.search(rf"androidTestCompile\s+['\"]{escaped}['\"]", block)
            or re.search(rf"add\(\"androidTestImplementation\",\s*['\"]{escaped}['\"]\)", block)
            or re.search(rf"add\(\"androidTestCompile\",\s*['\"]{escaped}['\"]\)", block)
        )

    def apply(self) -> None:
        self.ensure_test_options()
        self.ensure_instrumentation_runner()
        self.ensure_configuration_guards()
        self.ensure_dependencies()


def process(path: pathlib.Path) -> None:
    editor = GradleFile(path.read_text(encoding="utf-8"))
    editor.apply()
    path.write_text(editor.content, encoding="utf-8")


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
