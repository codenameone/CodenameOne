#!/usr/bin/env python3
"""Export image attachments from an xcresult bundle."""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from collections import deque
from typing import Deque, Dict, Iterable, List, Optional, Sequence, Set, Tuple


def ri_log(message: str) -> None:
    """Mirror the bash script's logging prefix."""
    print(f"[run-ios-ui-tests] {message}", file=sys.stderr)


def run_xcresult(args: Sequence[str]) -> str:
    cmd = ["xcrun", "xcresulttool", *args]
    try:
        result = subprocess.run(
            cmd,
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    except subprocess.CalledProcessError as exc:
        stderr = exc.stderr.decode("utf-8", "ignore").strip()
        ri_log(f"xcresulttool command failed: {' '.join(cmd)}\n{stderr}")
        raise
    return result.stdout.decode("utf-8")


def get_json(bundle_path: str, object_id: Optional[str] = None) -> Dict:
    args = ["get", "--legacy", "--path", bundle_path, "--format", "json"]
    if object_id:
        args.extend(["--id", object_id])
    output = run_xcresult(args)
    return json.loads(output or "{}")


def extract_id(ref: object) -> Optional[str]:
    if isinstance(ref, str):
        return ref
    if isinstance(ref, dict):
        if "id" in ref:
            return extract_id(ref["id"])
        if "_value" in ref:
            value = ref["_value"]
            return value if isinstance(value, str) else None
    return None


def collect_nodes(node) -> Tuple[List[Dict], List[str]]:
    attachments: List[Dict] = []
    refs: List[str] = []
    stack: List = [node]
    while stack:
        current = stack.pop()
        if isinstance(current, dict):
            attachment_block = current.get("attachments")
            if isinstance(attachment_block, dict):
                for item in attachment_block.get("_values", []):
                    if isinstance(item, dict):
                        attachments.append(item)
            for key, value in current.items():
                if key.endswith("Ref") and isinstance(value, dict):
                    ref_id = extract_id(value)
                    if isinstance(ref_id, str) and ref_id:
                        refs.append(ref_id)
                elif key.endswith("Refs") and isinstance(value, dict):
                    for entry in value.get("_values", []):
                        if isinstance(entry, dict):
                            ref_id = extract_id(entry)
                            if isinstance(ref_id, str) and ref_id:
                                refs.append(ref_id)
                if isinstance(value, (dict, list)):
                    stack.append(value)
        elif isinstance(current, list):
            stack.extend(current)
    return attachments, refs


def _looks_like_image(value: Optional[str]) -> bool:
    if not value:
        return False
    lower = value.lower()
    if lower.endswith((".png", ".jpg", ".jpeg")):
        return True
    keywords = ("png", "jpeg", "image", "screenshot")
    return any(keyword in lower for keyword in keywords)


def is_image_attachment(attachment: Dict) -> bool:
    filename = attachment.get("filename") or attachment.get("name")
    if _looks_like_image(filename):
        return True

    uti_candidates = [
        attachment.get("uniformTypeIdentifier"),
        attachment.get("contentType"),
        attachment.get("uti"),
    ]
    payload = (
        attachment.get("payloadRef")
        or attachment.get("inlinePayloadRef")
        or attachment.get("payload")
    )
    if isinstance(payload, dict):
        uti_candidates.extend(
            [
                payload.get("contentType"),
                payload.get("uti"),
                payload.get("uniformTypeIdentifier"),
            ]
        )

    return any(_looks_like_image(candidate) for candidate in uti_candidates)


def sanitize_filename(name: str) -> str:
    safe = re.sub(r"[^A-Za-z0-9_.-]", "_", name)
    return safe or "attachment"


def ensure_extension(name: str, uti: str) -> str:
    base, ext = os.path.splitext(name)
    if ext:
        return name
    uti = uti.lower()
    if "png" in uti:
        return f"{name}.png"
    if "jpeg" in uti:
        return f"{name}.jpg"
    return name


def export_attachment(
    bundle_path: str, attachment: Dict, destination_dir: str, used_names: Set[str]
) -> None:
    payload = (
        attachment.get("payloadRef")
        or attachment.get("inlinePayloadRef")
        or attachment.get("payload")
        or {}
    )
    attachment_id = extract_id(payload)
    if not attachment_id:
        return
    name = attachment.get("filename") or attachment.get("name") or attachment_id
    name = ensure_extension(name, attachment.get("uniformTypeIdentifier") or "")
    name = sanitize_filename(name)
    candidate = name
    counter = 1
    while candidate in used_names:
        base, ext = os.path.splitext(name)
        candidate = f"{base}_{counter}{ext}"
        counter += 1
    used_names.add(candidate)
    output_path = os.path.join(destination_dir, candidate)
    run_xcresult(
        [
            "export",
            "--legacy",
            "--path",
            bundle_path,
            "--id",
            attachment_id,
            "--type",
            "file",
            "--output-path",
            output_path,
        ]
    )
    ri_log(f"Exported attachment {candidate}")


def handle_attachments(
    bundle_path: str,
    items: Iterable[Dict],
    destination_dir: str,
    used_names: Set[str],
    seen_attachment_ids: Set[str],
) -> None:
    for attachment in items:
        payload = (
            attachment.get("payloadRef")
            or attachment.get("inlinePayloadRef")
            or attachment.get("payload")
            or {}
        )
        attachment_id = extract_id(payload)
        if not attachment_id or attachment_id in seen_attachment_ids:
            continue
        if not is_image_attachment(attachment):
            continue
        seen_attachment_ids.add(attachment_id)
        export_attachment(bundle_path, attachment, destination_dir, used_names)


def export_bundle(bundle_path: str, destination_dir: str) -> bool:
    os.makedirs(destination_dir, exist_ok=True)

    root = get_json(bundle_path)
    attachments, refs = collect_nodes(root)
    queue: Deque[str] = deque(refs)
    seen_refs: Set[str] = set()
    seen_attachment_ids: Set[str] = set()
    exported_names: Set[str] = set()

    handle_attachments(
        bundle_path, attachments, destination_dir, exported_names, seen_attachment_ids
    )

    while queue:
        ref_id = queue.popleft()
        if ref_id in seen_refs:
            continue
        seen_refs.add(ref_id)
        data = get_json(bundle_path, ref_id)
        items, nested_refs = collect_nodes(data)
        handle_attachments(
            bundle_path, items, destination_dir, exported_names, seen_attachment_ids
        )
        for nested in nested_refs:
            if nested not in seen_refs:
                queue.append(nested)

    if not exported_names:
        ri_log("No screenshot attachments were exported from xcresult bundle")
        return False
    return True


def main(argv: Sequence[str]) -> int:
    if len(argv) != 3:
        ri_log("Expected bundle path and destination directory arguments")
        return 1
    _, bundle_path, destination_dir = argv
    success = export_bundle(bundle_path, destination_dir)
    return 0 if success else 2


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
