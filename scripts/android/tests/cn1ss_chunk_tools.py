import argparse
import base64
import pathlib
import re
import sys
from typing import Iterable, List, Optional, Tuple

DEFAULT_TEST_NAME = "default"
CHUNK_PATTERN = re.compile(r"CN1SS:(?:(?P<test>[A-Za-z0-9_.-]+):)?(?P<index>\d{6}):(.*)")


def _iter_chunk_lines(path: pathlib.Path, test_filter: Optional[str] = None) -> Iterable[Tuple[str, int, str]]:
    text = path.read_text(encoding="utf-8", errors="ignore")
    for line in text.splitlines():
        match = CHUNK_PATTERN.search(line)
        if not match:
            continue
        test_name = match.group("test") or DEFAULT_TEST_NAME
        if test_filter is not None and test_name != test_filter:
            continue
        index = int(match.group("index"))
        payload = re.sub(r"[^A-Za-z0-9+/=]", "", match.group(3))
        if payload:
            yield test_name, index, payload


def count_chunks(path: pathlib.Path, test: Optional[str] = None) -> int:
    return sum(1 for _ in _iter_chunk_lines(path, test_filter=test))


def concatenate_chunks(path: pathlib.Path, test: Optional[str] = None) -> str:
    ordered = sorted(_iter_chunk_lines(path, test_filter=test), key=lambda item: item[1])
    return "".join(payload for _, _, payload in ordered)


def decode_chunks(path: pathlib.Path, test: Optional[str] = None) -> bytes:
    data = concatenate_chunks(path, test=test)
    if not data:
        return b""
    try:
        return base64.b64decode(data)
    except Exception:
        return b""


def list_tests(path: pathlib.Path) -> List[str]:
    seen = {test for test, _, _ in _iter_chunk_lines(path)}
    return sorted(seen)


def main(argv: List[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    p_count = subparsers.add_parser("count", help="Count CN1SS chunks in a file")
    p_count.add_argument("path", type=pathlib.Path)
    p_count.add_argument("--test", dest="test", default=None, help="Optional test name filter")

    p_extract = subparsers.add_parser("extract", help="Concatenate CN1SS payload chunks")
    p_extract.add_argument("path", type=pathlib.Path)
    p_extract.add_argument("--decode", action="store_true", help="Decode payload to binary PNG")
    p_extract.add_argument("--test", dest="test", default=None, help="Test name to extract (default=unnamed)")

    p_tests = subparsers.add_parser("tests", help="List distinct test names found in CN1SS chunks")
    p_tests.add_argument("path", type=pathlib.Path)

    args = parser.parse_args(argv)

    if args.command == "count":
        print(count_chunks(args.path, args.test))
        return 0

    if args.command == "extract":
        target_test: Optional[str]
        if args.test is None:
            target_test = DEFAULT_TEST_NAME
        else:
            target_test = args.test
        if args.decode:
            sys.stdout.buffer.write(decode_chunks(args.path, target_test))
        else:
            sys.stdout.write(concatenate_chunks(args.path, target_test))
        return 0

    if args.command == "tests":
        for name in list_tests(args.path):
            print(name)
        return 0

    return 1


if __name__ == "__main__":
    raise SystemExit(main())
