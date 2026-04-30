#!/usr/bin/env python3
"""
Classify JavaScript ParparVM browser log failures into a stable top blocker line.

Output format:
  TOP_BLOCKER=<category>|<methodId>|<receiverClass>
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


PARPAR_ERROR_PREFIX = "BROWSER:PARPAR_ERROR:"
MISSING_VIRTUAL_RE = re.compile(r"Missing virtual method ([^\s\"\\]+) on ([^\s\"\\]+)")


def normalize(value: str | None) -> str:
    if value is None or value == "":
        return "none"
    return str(value).strip() or "none"


def parse_virtual_from_message(message: str) -> tuple[str, str]:
    match = MISSING_VIRTUAL_RE.search(message or "")
    if not match:
        return ("none", "none")
    return (normalize(match.group(1)), normalize(match.group(2)))


def classify(log_text: str) -> tuple[str, str, str]:
    if "CN1SS:SUITE:FINISHED" in log_text:
        return ("none", "none", "none")

    first_parpar_error = None
    for line in log_text.splitlines():
        if PARPAR_ERROR_PREFIX in line:
            first_parpar_error = line
            break

    if first_parpar_error is None:
        parsed_method, parsed_receiver = parse_virtual_from_message(log_text)
        if parsed_method != "none":
            if parsed_receiver in ("null", "undefined"):
                return ("missing_receiver", parsed_method, parsed_receiver)
            return ("missing_class_method", parsed_method, parsed_receiver)
        if "Timed out waiting for CN1SS:SUITE:FINISHED" in log_text:
            return ("timeout_no_error", "none", "none")
        if "BROWSER:ERROR:" in log_text:
            return ("browser_error", "none", "none")
        if "BROWSER:REJECTION:" in log_text:
            return ("browser_rejection", "none", "none")
        return ("unknown", "none", "none")

    payload_raw = first_parpar_error.split(PARPAR_ERROR_PREFIX, 1)[1].strip()
    category = "runtime_error"
    method_id = "none"
    receiver_class = "none"
    message = ""

    try:
        payload = json.loads(payload_raw)
    except Exception:
        payload = None

    if isinstance(payload, dict):
        message = str(payload.get("message", "") or "")
        vf = payload.get("virtualFailure")
        if isinstance(vf, dict):
            category = normalize(vf.get("category"))
            method_id = normalize(vf.get("methodId"))
            receiver_class = normalize(vf.get("receiverClass"))

    if method_id == "none" or receiver_class == "none":
        parsed_method, parsed_receiver = parse_virtual_from_message(message or payload_raw)
        if method_id == "none":
            method_id = parsed_method
        if receiver_class == "none":
            receiver_class = parsed_receiver

    if category == "runtime_error" and method_id != "none":
        if receiver_class in ("null", "undefined"):
            category = "missing_receiver"
        else:
            category = "missing_class_method"

    return (category, method_id, receiver_class)


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("Usage: classify-javascript-parpar-blocker.py <browser-log>", file=sys.stderr)
        return 2
    log_path = Path(argv[1])
    if not log_path.exists():
        print("TOP_BLOCKER=missing_log|none|none")
        return 0
    log_text = log_path.read_text(encoding="utf-8", errors="replace")
    category, method_id, receiver_class = classify(log_text)
    print(f"TOP_BLOCKER={category}|{method_id}|{receiver_class}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
