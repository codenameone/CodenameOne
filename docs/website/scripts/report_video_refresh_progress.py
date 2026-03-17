#!/usr/bin/env python3
import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path

from video_refresh_lib import INVENTORY_PATH


def load_inventory(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))["items"]


def print_counter(title, counter):
    print(title)
    for key in sorted(counter):
        print(f"  {key}: {counter[key]}")
    print()


def main():
    parser = argparse.ArgumentParser(description="Report video refresh progress.")
    parser.add_argument("--inventory", default=str(INVENTORY_PATH))
    args = parser.parse_args()

    items = load_inventory(Path(args.inventory))
    by_kind = Counter(item["page_kind"] for item in items)
    by_priority = Counter(item["priority"] for item in items)
    by_transcript = Counter(item["transcript_status"] for item in items)
    by_guide = Counter(item["guide_status"] for item in items)
    by_script = Counter(item["script_status"] for item in items)
    by_course = Counter(item["course_id"] or item["page_kind"] for item in items)

    print_counter("By Page Kind", by_kind)
    print_counter("By Priority", by_priority)
    print_counter("By Transcript Status", by_transcript)
    print_counter("By Guide Status", by_guide)
    print_counter("By Script Status", by_script)
    print_counter("By Course", by_course)


if __name__ == "__main__":
    main()
