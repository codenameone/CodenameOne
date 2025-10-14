#!/usr/bin/env python3
"""Utilities to normalize generated Gradle build files for Android tests."""
from __future__ import annotations

import argparse
import pathlib
import re
from typing import Tuple

REPOSITORIES_BLOCK = """\
repositories {
    google()
    mavenCentral()
}
"""


def _ensure_repositories(content: str) -> Tuple[str, bool]:
    """Ensure a repositories block exists with google() and mavenCentral()."""
    pattern = re.compile(r"(?ms)^\s*repositories\s*\{.*?\}")
    match = pattern.search(content)
    block_added = False

    if not match:
        # Append a canonical repositories block to the end of the file.
        if not content.endswith("\n"):
            content += "\n"
        content += REPOSITORIES_BLOCK
        return content, True

    block = match.group(0)
    if "google()" not in block or "mavenCentral()" not in block:
        lines = block.splitlines()
        header = lines[0]
        body = [line for line in lines[1:-1] if line.strip()]
        if "google()" not in block:
            body.append("    google()")
        if "mavenCentral()" not in block:
            body.append("    mavenCentral()")
        new_block = "\n".join([header, *sorted(set(body)), lines[-1]])
        content = content[: match.start()] + new_block + content[match.end() :]
        block_added = True
    return content, block_added


def _ensure_android_sdk(content: str, compile_sdk: int, target_sdk: int) -> Tuple[str, bool]:
    changed = False

    def insert_or_replace(pattern: str, repl: str, search_scope: str) -> Tuple[str, bool]:
        nonlocal content
        match = re.search(pattern, content, re.MULTILINE)
        if match:
            new_content = re.sub(pattern, repl, content, count=1, flags=re.MULTILINE)
            if new_content != content:
                content = new_content
                return content, True
            return content, False
        anchor = re.search(search_scope, content, re.MULTILINE)
        if not anchor:
            return content, False
        start = anchor.end()
        content = content[:start] + f"\n{repl}" + content[start:]
        return content, True

    # Ensure android block exists
    if re.search(r"(?m)^\s*android\s*\{", content) is None:
        if not content.endswith("\n"):
            content += "\n"
        content += (
            "\nandroid {\n"
            f"    compileSdkVersion {compile_sdk}\n"
            "    defaultConfig {\n"
            f"        targetSdkVersion {target_sdk}\n"
            "    }\n}"
        )
        return content, True

    new_content, changed_once = insert_or_replace(
        pattern=rf"(?m)^\s*compileSdkVersion\s+\d+",
        repl=f"    compileSdkVersion {compile_sdk}",
        search_scope=r"(?m)^\s*android\s*\{",
    )
    changed = changed or changed_once

    default_config = re.search(r"(?ms)^\s*defaultConfig\s*\{.*?^\s*\}", content)
    if default_config:
        block = default_config.group(0)
        if re.search(r"(?m)^\s*targetSdkVersion\s+\d+", block):
            updated = re.sub(
                r"(?m)^\s*targetSdkVersion\s+\d+",
                f"        targetSdkVersion {target_sdk}",
                block,
            )
        else:
            updated = re.sub(r"{", "{\n        targetSdkVersion %d" % target_sdk, block, count=1)
        if updated != block:
            content = content[: default_config.start()] + updated + content[default_config.end() :]
            changed = True
    else:
        content, changed_once = insert_or_replace(
            pattern=r"(?ms)(^\s*android\s*\{)",
            repl="    defaultConfig {\n        targetSdkVersion %d\n    }" % target_sdk,
            search_scope=r"(?m)^\s*android\s*\{",
        )
        changed = changed or changed_once

    return content, changed


def _ensure_instrumentation_runner(content: str) -> Tuple[str, bool]:
    runner = "androidx.test.runner.AndroidJUnitRunner"
    if runner in content:
        return content, False
    changed = False
    pattern = re.compile(r"(?m)^\s*testInstrumentationRunner\s*\".*?\"\s*$")
    if pattern.search(content):
        new_content = pattern.sub(f"        testInstrumentationRunner \"{runner}\"", content)
        return new_content, True
    default_config = re.search(r"(?ms)^\s*defaultConfig\s*\{", content)
    if default_config:
        insert_point = default_config.end()
        content = (
            content[:insert_point]
            + f"\n        testInstrumentationRunner \"{runner}\""
            + content[insert_point:]
        )
        changed = True
    else:
        android_block = re.search(r"(?ms)^\s*android\s*\{", content)
        if android_block:
            insert_point = android_block.end()
            snippet = (
                "\n    defaultConfig {\n"
                f"        testInstrumentationRunner \"{runner}\"\n"
                "    }"
            )
            content = content[:insert_point] + snippet + content[insert_point:]
            changed = True
    return content, changed


def _remove_legacy_use_library(content: str) -> Tuple[str, bool]:
    new_content, count = re.subn(
        r"(?m)^\s*useLibrary\s+'android\.test\.(?:base|mock|runner)'\s*$",
        "",
        content,
    )
    return new_content, bool(count)


def _ensure_test_dependencies(content: str) -> Tuple[str, bool]:
    module_view = re.sub(r"(?ms)^\s*(buildscript|pluginManagement)\s*\{.*?^\s*\}", "", content)
    uses_modern = re.search(
        r"(?m)^\s*(implementation|api|testImplementation|androidTestImplementation)\b",
        module_view,
    )
    configuration = "androidTestImplementation" if uses_modern else "androidTestCompile"
    dependencies = [
        "androidx.test.ext:junit:1.1.5",
        "androidx.test:runner:1.5.2",
        "androidx.test:core:1.5.0",
        "androidx.test.services:storage:1.4.2",
    ]
    missing = [dep for dep in dependencies if dep not in module_view]
    if not missing:
        return content, False
    block = "\n\ndependencies {\n" + "".join(
        f"    {configuration} \"{dep}\"\n" for dep in missing
    ) + "}\n"
    if not content.endswith("\n"):
        content += "\n"
    return content + block, True


def patch_app_build_gradle(path: pathlib.Path, compile_sdk: int, target_sdk: int) -> bool:
    text = path.read_text(encoding="utf-8")
    changed = False

    for transform in (
        lambda c: _ensure_android_sdk(c, compile_sdk, target_sdk),
        _ensure_instrumentation_runner,
        _remove_legacy_use_library,
        _ensure_test_dependencies,
    ):
        text, modified = transform(text)
        changed = changed or modified

    if changed:
        path.write_text(text if text.endswith("\n") else text + "\n", encoding="utf-8")
    return changed


def patch_root_build_gradle(path: pathlib.Path) -> bool:
    text = path.read_text(encoding="utf-8")
    text, changed = _ensure_repositories(text)
    if changed:
        path.write_text(text if text.endswith("\n") else text + "\n", encoding="utf-8")
    return changed


def main() -> int:
    parser = argparse.ArgumentParser(description="Normalize Gradle build files")
    parser.add_argument("--root", required=True, type=pathlib.Path, help="Path to root build.gradle")
    parser.add_argument("--app", required=True, type=pathlib.Path, help="Path to app/build.gradle")
    parser.add_argument("--compile-sdk", type=int, default=33)
    parser.add_argument("--target-sdk", type=int, default=33)
    args = parser.parse_args()

    modified_root = patch_root_build_gradle(args.root)
    modified_app = patch_app_build_gradle(args.app, args.compile_sdk, args.target_sdk)

    if modified_root:
        print(f"Patched {args.root}")
    if modified_app:
        print(f"Patched {args.app}")
    if not (modified_root or modified_app):
        print("Gradle files already normalized")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
