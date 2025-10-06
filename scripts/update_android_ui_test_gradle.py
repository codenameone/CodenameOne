#!/usr/bin/env python3
"""Configure Android instrumentation UI test dependencies for the generated project."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys

TEST_OPTIONS_BLOCK = (
    "    testOptions {\n"
    "        animationsDisabled = true\n"
    "    }\n\n"
)

OLD_TEST_OPTIONS_BLOCK = (
    "    testOptions {\n"
    "        unitTests.includeAndroidResources = true\n"
    "        unitTests.all {\n"
    "            systemProperty 'http.agent', 'CodenameOneUiTest'\n"
    "        }\n"
    "    }\n\n"
)

LEGACY_TEST_OPTIONS_BLOCK = (
    "    testOptions {\n"
    "        unitTests.includeAndroidResources = true\n"
    "    }\n\n"
)

RUNNER_LINE = "        testInstrumentationRunner \"androidx.test.runner.AndroidJUnitRunner\"\n"

ANDROID_TEST_DEPENDENCIES = (
    "androidx.test:core:1.5.0",
    "androidx.test.ext:junit:1.1.5",
    "androidx.test:rules:1.5.0",
    "androidx.test:runner:1.5.2",
    "androidx.test.espresso:espresso-core:3.5.1",
    "androidx.test.uiautomator:uiautomator:2.2.0",
)

HELPER_SNIPPET = (
    "    if (!project.ext.has('cn1AddAndroidTestDependency')) {\n"
    "        project.ext.cn1AddAndroidTestDependency = { String notation ->\n"
    "            def parts = notation.split(':')\n"
    "            def coordinate = parts.size() >= 2 ? parts[0] + ':' + parts[1] : notation\n"
    "            def targetNames = ['androidTestImplementation', 'androidTestCompile'].findAll { cfgName ->\n"
    "                configurations.findByName(cfgName) != null\n"
    "            }\n"
    "            if (targetNames.isEmpty()) {\n"
    "                targetNames = ['androidTestImplementation']\n"
    "            }\n"
    "            targetNames.each { cfgName ->\n"
    "                def cfg = configurations.maybeCreate(cfgName)\n"
    "                def exists = cfg.dependencies.any { dep ->\n"
    "                    dep.group != null && dep.name != null && (dep.group + ':' + dep.name) == coordinate\n"
    "                }\n"
    "                if (!exists) {\n"
    "                    project.dependencies.add(cfgName, notation)\n"
    "                }\n"
    "            }\n"
    "        }\n"
    "    }\n"
)

TEST_DEPENDENCIES_TO_REMOVE = [
    re.compile(r"\s+testImplementation 'org\.robolectric:robolectric:[^']*'\n"),
    re.compile(r"\s+testImplementation 'androidx\.test:core:[^']*'\n"),
    re.compile(r"\s+testImplementation 'androidx\.test:runner:[^']*'\n"),
    re.compile(r"\s+androidTest(?:Implementation|Compile) 'androidx\.test:[^']*'\n"),
    re.compile(r"\s+androidTest(?:Implementation|Compile) 'androidx\.test\.[^']*'\n"),
]


class GradleEditor:
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
        if OLD_TEST_OPTIONS_BLOCK in self.content:
            self.content = self.content.replace(OLD_TEST_OPTIONS_BLOCK, TEST_OPTIONS_BLOCK, 1)
            return
        if LEGACY_TEST_OPTIONS_BLOCK in self.content:
            self.content = self.content.replace(LEGACY_TEST_OPTIONS_BLOCK, TEST_OPTIONS_BLOCK, 1)
            return
        android_block = self.find_block("android")
        if not android_block:
            raise SystemExit("Unable to locate android block in Gradle file")
        insert_position = self.content.find('\n', android_block[0]) + 1
        if insert_position <= 0:
            insert_position = android_block[0] + len("android {")
        self.content = self.content[:insert_position] + TEST_OPTIONS_BLOCK + self.content[insert_position:]

    def ensure_instrumentation_runner(self) -> None:
        default_block = self.find_block("defaultConfig")
        if default_block:
            block_content = self.content[default_block[0]:default_block[1]]
            if RUNNER_LINE.strip() in block_content:
                return
            insert_position = self.content.find('\n', default_block[0]) + 1
            self.content = (
                self.content[:insert_position]
                + RUNNER_LINE
                + self.content[insert_position:default_block[1]]
                + self.content[default_block[1]:]
            )
        else:
            android_block = self.find_block("android")
            if not android_block:
                raise SystemExit("Unable to locate android block in Gradle file")
            insert_position = self.content.find('\n', android_block[0]) + 1
            default_config_block = (
                "    defaultConfig {\n"
                + RUNNER_LINE
                + "    }\n\n"
            )
            self.content = (
                self.content[:insert_position]
                + default_config_block
                + self.content[insert_position:]
            )

    def ensure_dependencies(self) -> None:
        for pattern in TEST_DEPENDENCIES_TO_REMOVE:
            self.content = pattern.sub('\n', self.content)
        block = self.find_block("dependencies")
        if not block:
            raise SystemExit("Unable to locate dependencies block in Gradle file")
        block_body = self.content[block[0]:block[1]]
        insertion_point = block[1] - 1
        if "cn1AddAndroidTestDependency" not in block_body:
            block_text = self.content[block[0]:block[1]]
            newline_offset = block_text.find('\n')
            if newline_offset < 0:
                newline_offset = len(block_text)
            helper_insert = block[0] + newline_offset + 1
            self.content = (
                self.content[:helper_insert]
                + HELPER_SNIPPET
                + self.content[helper_insert:]
            )
            block = self.find_block("dependencies")
            block_body = self.content[block[0]:block[1]]
            insertion_point = block[1] - 1
        for dependency in ANDROID_TEST_DEPENDENCIES:
            declaration = f"    project.ext.cn1AddAndroidTestDependency('{dependency}')\n"
            if dependency in block_body:
                continue
            self.content = (
                self.content[:insertion_point]
                + declaration
                + self.content[insertion_point:]
            )
            insertion_point += len(declaration)
            block_body += declaration

    def updated(self) -> str:
        return self.content


def process_gradle_file(path: pathlib.Path) -> None:
    editor = GradleEditor(path.read_text(encoding="utf-8"))
    editor.ensure_test_options()
    editor.ensure_instrumentation_runner()
    editor.ensure_dependencies()
    path.write_text(editor.updated(), encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("gradle_file", type=pathlib.Path, help="Path to module build.gradle file")
    args = parser.parse_args(argv)

    if not args.gradle_file.is_file():
        parser.error(f"Gradle file not found: {args.gradle_file}")

    process_gradle_file(args.gradle_file)
    return 0


if __name__ == "__main__":
    sys.exit(main())
