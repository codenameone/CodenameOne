#!/usr/bin/env python3
"""Utility to inject Robolectric UI test settings into an Android Gradle module."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys


TEST_OPTIONS_INJECTION = (
    "    testOptions {\n"
    "        unitTests.includeAndroidResources = true\n"
    "        unitTests.all {\n"
    "            systemProperty 'http.agent', 'CodenameOneUiTest'\n"
    "        }\n"
    "    }\n\n"
)


def ensure_test_options(content: str) -> str:
    if "systemProperty 'http.agent'" in content:
        return content

    legacy_block = (
        "    testOptions {\n"
        "        unitTests.includeAndroidResources = true\n"
        "    }\n\n"
    )

    if legacy_block in content:
        return content.replace(legacy_block, TEST_OPTIONS_INJECTION)

    pattern = re.compile(r"(android\s*\{\s*\r?\n)")
    match = pattern.search(content)
    if not match:
        raise SystemExit("Unable to locate android block in Gradle file")

    start, end = match.span(1)
    return content[:end] + TEST_OPTIONS_INJECTION + content[end:]


def ensure_dependencies(content: str) -> str:
    if "org.robolectric:robolectric" in content:
        return content

    additions = (
        "    testImplementation 'junit:junit:4.13.2'\n"
        "    testImplementation 'androidx.test:core:1.5.0'\n"
        "    testImplementation 'org.robolectric:robolectric:4.11.1'\n"
        "    androidTestImplementation 'androidx.test.ext:junit:1.1.5'\n"
        "    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'\n"
        "    androidTestImplementation 'androidx.test:rules:1.5.0'\n"
        "    androidTestImplementation 'androidx.test:runner:1.5.2'\n"
    )

    pattern = re.compile(r"dependencies\s*\{\s*\r?\n", re.MULTILINE)
    matches = list(pattern.finditer(content))
    if not matches:
        raise SystemExit("Unable to locate dependencies block in Gradle file")

    target = matches[-1] if len(matches) == 1 else matches[1]
    insert_at = target.end()
    return content[:insert_at] + additions + content[insert_at:]


def process_gradle_file(path: pathlib.Path) -> None:
    content = path.read_text(encoding="utf-8")
    updated = ensure_test_options(content)
    updated = ensure_dependencies(updated)
    if updated != content:
        path.write_text(updated, encoding="utf-8")


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
