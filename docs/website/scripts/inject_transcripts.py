#!/usr/bin/env python3
import argparse
import json
import re
from pathlib import Path

from video_refresh_lib import (
    INVENTORY_PATH,
    build_transcript_section,
    extract_discussion,
    extract_transcript,
    heading_title,
    load_markdown_page,
    load_transcript_meta,
    load_transcript_text,
    split_frontmatter,
)


def remove_section(body: str, section_name: str) -> str:
    lines = body.splitlines()
    target_index = None
    target_level = None
    wanted = section_name.strip().lower()
    for index, line in enumerate(lines):
        heading = heading_title(line)
        if not heading:
            continue
        level, title = heading
        if title == wanted:
            target_index = index
            target_level = level
            break
    if target_index is None or target_level is None:
        return body.strip() + "\n"

    kept = lines[:target_index]
    skip = False
    for line in lines[target_index + 1 :]:
        heading = heading_title(line)
        if heading and heading[0] <= target_level:
            skip = True
        if skip:
            kept.append(line)
    return "\n".join(kept).strip() + "\n"


def insert_before_discussion(body: str, block: str) -> str:
    discussion_match = re.search(r"(?im)^##\s+discussion\s*$", body)
    if discussion_match:
        text = body[: discussion_match.start()].rstrip() + "\n\n" + block.strip() + "\n\n" + body[discussion_match.start() :].lstrip()
    else:
        text = body.rstrip() + "\n\n" + block.strip() + "\n"
    text = re.sub(r"\n+---\s*\n*$", "\n", text.rstrip() + "\n")
    return text.rstrip() + "\n"


def load_inventory(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))["items"]


def main():
    parser = argparse.ArgumentParser(description="Inject cached transcripts into markdown pages.")
    parser.add_argument("--inventory", default=str(INVENTORY_PATH))
    parser.add_argument("--slug", action="append")
    parser.add_argument("--youtube-id", action="append")
    parser.add_argument("--include-unavailable-note", action="store_true")
    args = parser.parse_args()

    wanted_slugs = set(args.slug or [])
    wanted_ids = set(args.youtube_id or [])
    inventory_path = Path(args.inventory)
    items = load_inventory(inventory_path)
    updated = 0

    for item in items:
        if item["page_kind"] == "course-hub":
            continue
        if wanted_slugs and item["slug"] not in wanted_slugs:
            continue
        if wanted_ids and item.get("youtube_id") not in wanted_ids:
            continue

        youtube_id = item.get("youtube_id")
        transcript = load_transcript_text(youtube_id) if youtube_id else None
        meta = load_transcript_meta(youtube_id) if youtube_id else {}
        if not transcript and not (args.include_unavailable_note and meta.get("status") == "unavailable"):
            continue

        path = inventory_path.parents[1] / item["source_path"]
        text = path.read_text(encoding="utf-8")
        frontmatter, body = split_frontmatter(text)
        cleaned_body = remove_section(body, "transcript")
        block = build_transcript_section(transcript, meta)
        new_body = insert_before_discussion(cleaned_body, block)
        path.write_text(f"---\n{frontmatter}\n---\n{new_body}", encoding="utf-8")
        updated += 1
        print(f"updated transcript section: {path}")

    print(f"completed: updated={updated}")


if __name__ == "__main__":
    main()
