#!/usr/bin/env python3
"""Helpers for extracting CN1SS chunked screenshot payloads."""
from __future__ import annotations

import argparse
import base64
import pathlib
import re
import sys
from typing import Iterable, List, Tuple

CHUNK_PATTERN = re.compile(r"CN1SS:(\d{6}):(.*)")


def _iter_chunk_lines(path: pathlib.Path) -> Iterable[Tuple[int, str]]:
    text = path.read_text(encoding="utf-8", errors="ignore")
    for line in text.splitlines():
        match = CHUNK_PATTERN.search(line)
        if not match:
            continue
        index = int(match.group(1))
        payload = re.sub(r"[^A-Za-z0-9+/=]", "", match.group(2))
        if payload:
            yield index, payload


def count_chunks(path: pathlib.Path) -> int:
    return sum(1 for _ in _iter_chunk_lines(path))


def concatenate_chunks(path: pathlib.Path) -> str:
    ordered = sorted(_iter_chunk_lines(path), key=lambda item: item[0])
    return "".join(payload for _, payload in ordered)


def decode_chunks(path: pathlib.Path) -> bytes:
    data = concatenate_chunks(path)
    if not data:
        return b""
    try:
        return base64.b64decode(data)
    except Exception:
        return b""


def main(argv: List[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    p_count = subparsers.add_parser("count", help="Count CN1SS chunks in a file")
    p_count.add_argument("path", type=pathlib.Path)

    p_extract = subparsers.add_parser("extract", help="Concatenate CN1SS payload chunks")
    p_extract.add_argument("path", type=pathlib.Path)
    p_extract.add_argument("--decode", action="store_true", help="Decode payload to binary PNG")

    args = parser.parse_args(argv)

    if args.command == "count":
        print(count_chunks(args.path))
        return 0

    if args.command == "extract":
        if args.decode:
            sys.stdout.buffer.write(decode_chunks(args.path))
        else:
            sys.stdout.write(concatenate_chunks(args.path))
        return 0

    return 1


if __name__ == "__main__":
    raise SystemExit(main())
