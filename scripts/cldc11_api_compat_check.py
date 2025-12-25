#!/usr/bin/env python3
"""
Validate that the CLDC11 API surface is a binary compatible subset of Java SE 11
and the vm/JavaAPI project.

The script compares public and protected methods and fields using `javap -s -public`.
It expects pre-built class directories for the CLDC11 and JavaAPI projects.
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from dataclasses import dataclass, field
from typing import Dict, Iterable, List, Optional, Set, Tuple


Member = Tuple[str, str, bool, str]
"""
A member descriptor in the form `(name, descriptor, is_static, kind)`.
`kind` is either `method` or `field`.
"""


@dataclass
class ApiSurface:
    """Collection of public/protected API members for a class."""

    methods: Set[Member] = field(default_factory=set)
    fields: Set[Member] = field(default_factory=set)

    def add(self, member: Member) -> None:
        if member[3] == "method":
            self.methods.add(member)
        else:
            self.fields.add(member)

    def missing_from(self, other: "ApiSurface") -> Tuple[Set[Member], Set[Member]]:
        return self.methods - other.methods, self.fields - other.fields

    def extras_over(self, other: "ApiSurface") -> Tuple[Set[Member], Set[Member]]:
        return self.methods - other.methods, self.fields - other.fields


class JavapError(RuntimeError):
    pass


def discover_classes(root: str) -> List[str]:
    classes: List[str] = []
    for base, _, files in os.walk(root):
        for filename in files:
            if not filename.endswith(".class"):
                continue
            if filename in {"module-info.class", "package-info.class"}:
                continue
            full_path = os.path.join(base, filename)
            rel_path = os.path.relpath(full_path, root)
            binary_name = rel_path[:-6].replace(os.sep, ".")
            classes.append(binary_name)
    return classes


def run_javap(target: str, javap_cmd: str) -> str:
    proc = subprocess.run(
        [javap_cmd, "-public", "-s", target],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if proc.returncode != 0:
        raise JavapError(proc.stderr.strip() or proc.stdout.strip())
    return proc.stdout


def parse_members(javap_output: str) -> ApiSurface:
    api = ApiSurface()
    pending: Optional[Tuple[str, bool, str]] = None  # name, is_static, kind

    for raw_line in javap_output.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("Compiled from"):
            continue
        if line.endswith("{"):
            continue
        if line.startswith("descriptor:"):
            if pending is None:
                continue
            descriptor = line.split(":", 1)[1].strip()
            name, is_static, kind = pending
            api.add((name, descriptor, is_static, kind))
            pending = None
            continue

        if line.startswith("Runtime") or line.startswith("Signature:") or line.startswith("Exceptions:"):
            pending = None
            continue

        if "(" in line or line.endswith(";"):
            if line.startswith("//"):
                continue
            if line.endswith(" class"):
                continue
            if line.endswith("interface"):
                continue

            is_static = " static " in f" {line} "
            if "(" in line:
                name_section = line.split("(")[0].strip()
                name = name_section.split()[-1]
                kind = "method"
            else:
                name = line.rstrip(";").split()[-1]
                kind = "field"
            pending = (name, is_static, kind)

    return api


def collect_class_api_from_file(class_name: str, classes_root: str, javap_cmd: str) -> ApiSurface:
    class_path = os.path.join(classes_root, *class_name.split(".")) + ".class"
    output = run_javap(class_path, javap_cmd)
    return parse_members(output)


def collect_class_api_from_jdk(class_name: str, javap_cmd: str) -> ApiSurface:
    output = run_javap(class_name, javap_cmd)
    return parse_members(output)


def format_member(member: Member) -> str:
    name, descriptor, is_static, kind = member
    static_prefix = "static " if is_static else ""
    return f"{kind}: {static_prefix}{name} {descriptor}"


def ensure_subset(
    source_classes: List[str],
    source_root: str,
    target_lookup,
    target_label: str,
    javap_cmd: str,
) -> Tuple[bool, List[str]]:
    ok = True
    messages: List[str] = []

    for class_name in sorted(source_classes):
        try:
            source_api = collect_class_api_from_file(class_name, source_root, javap_cmd)
        except JavapError as exc:
            ok = False
            messages.append(f"Failed to read {class_name} from {source_root}: {exc}")
            continue

        target_api = target_lookup(class_name)
        if target_api is None:
            ok = False
            messages.append(f"Missing class in {target_label}: {class_name}")
            continue

        missing_methods, missing_fields = source_api.missing_from(target_api)
        if missing_methods or missing_fields:
            ok = False
            messages.append(f"Incompatibilities for {class_name} against {target_label}:")
            for member in sorted(missing_methods | missing_fields):
                messages.append(f"  - {format_member(member)}")

    return ok, messages


def collect_javaapi_map(javaapi_root: str, javap_cmd: str) -> Dict[str, ApiSurface]:
    classes = discover_classes(javaapi_root)
    api_map: Dict[str, ApiSurface] = {}
    for class_name in classes:
        api_map[class_name] = collect_class_api_from_file(class_name, javaapi_root, javap_cmd)
    return api_map


def write_extra_report(
    cldc_classes: Dict[str, ApiSurface],
    javaapi_classes: Dict[str, ApiSurface],
    report_path: str,
) -> None:
    lines: List[str] = [
        "Extra APIs present in vm/JavaAPI but not in CLDC11",
        "",
    ]

    extra_classes = sorted(set(javaapi_classes) - set(cldc_classes))
    if extra_classes:
        lines.append("Classes only in vm/JavaAPI:")
        lines.extend([f"  - {name}" for name in extra_classes])
        lines.append("")

    shared_classes = set(javaapi_classes) & set(cldc_classes)
    extra_members: List[str] = []
    for class_name in sorted(shared_classes):
        javaapi_api = javaapi_classes[class_name]
        cldc_api = cldc_classes[class_name]
        extra_methods, extra_fields = javaapi_api.extras_over(cldc_api)
        if not extra_methods and not extra_fields:
            continue
        extra_members.append(class_name)
        extra_members.append("")
        for member in sorted(extra_methods | extra_fields):
            extra_members.append(f"  + {format_member(member)}")
        extra_members.append("")

    if extra_members:
        lines.append("Additional members on classes shared between vm/JavaAPI and CLDC11:")
        lines.append("")
        lines.extend(extra_members)

    if len(lines) == 2:
        lines.append("No extra APIs detected.")

    os.makedirs(os.path.dirname(os.path.abspath(report_path)) or ".", exist_ok=True)
    with open(report_path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines).rstrip() + "\n")


def main(argv: Optional[Iterable[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cldc-classes", required=True, help="Path to compiled CLDC11 classes directory")
    parser.add_argument("--javaapi-classes", required=True, help="Path to compiled vm/JavaAPI classes directory")
    parser.add_argument("--extra-report", required=True, help="File path to write the extra API report")
    parser.add_argument(
        "--javap",
        default=os.path.join(os.environ.get("JAVA_HOME", ""), "bin", "javap"),
        help="Path to the javap executable from Java SE 11",
    )
    args = parser.parse_args(argv)

    javap_cmd = args.javap or "javap"

    cldc_classes = discover_classes(args.cldc_classes)
    if not cldc_classes:
        print(f"No class files found under {args.cldc_classes}", file=sys.stderr)
        return 1

    javaapi_map = collect_javaapi_map(args.javaapi_classes, javap_cmd)

    def jdk_lookup(name: str) -> Optional[ApiSurface]:
        try:
            return collect_class_api_from_jdk(name, javap_cmd)
        except JavapError:
            return None

    def javaapi_lookup(name: str) -> Optional[ApiSurface]:
        return javaapi_map.get(name)

    java_ok, java_messages = ensure_subset(
        cldc_classes,
        args.cldc_classes,
        jdk_lookup,
        "Java SE 11",
        javap_cmd,
    )

    api_ok, api_messages = ensure_subset(
        cldc_classes,
        args.cldc_classes,
        javaapi_lookup,
        "vm/JavaAPI",
        javap_cmd,
    )

    cldc_map = {name: collect_class_api_from_file(name, args.cldc_classes, javap_cmd) for name in cldc_classes}
    write_extra_report(cldc_map, javaapi_map, args.extra_report)

    messages = java_messages + api_messages
    if messages:
        print("\n".join(messages), file=sys.stderr)
    return 0 if java_ok and api_ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
