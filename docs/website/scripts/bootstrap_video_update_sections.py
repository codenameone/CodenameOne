#!/usr/bin/env python3
import argparse
import json
import re
from pathlib import Path

from video_refresh_lib import INVENTORY_PATH, heading_title, split_frontmatter


TEMPLATE = """
## What This Covers

<!-- TODO(video-refresh): Summarize the concrete developer task this page solves today. -->

## Current Recommended Approach

<!-- TODO(video-refresh): Explain the current Codename One workflow and call out what changed since the video. -->

## Step-by-Step

<!-- TODO(video-refresh): Replace with an up-to-date guide and current commands/files. -->

## Common Pitfalls

<!-- TODO(video-refresh): Capture mistakes and outdated steps from the legacy video. -->

## Current Codename One Notes

<!-- TODO(video-refresh): Add version-sensitive notes, deprecations, or modern defaults. -->

## Further Reading

<!-- TODO(video-refresh): Add specific internal links for deeper docs. -->

## Video Update Script

### Recommended New Video Angle

<!-- TODO(video-refresh): Explain the framing for a replacement video. -->

### Target Audience

<!-- TODO(video-refresh): Beginner, intermediate, advanced, or legacy-maintainers. -->

### Runtime Goal

<!-- TODO(video-refresh): Suggest an expected target runtime. -->

### Script Outline

1. Problem statement
2. Current Codename One approach
3. Key concepts
4. Demo sequence
5. Common mistakes
6. Summary and next steps

### Slide Outline

1. Title / objective
2. Why this matters
3. Current workflow
4. Key APIs, tools, or files
5. Demo checkpoints
6. Pitfalls / compatibility notes
7. Further reading / CTA

### Demo Outline

<!-- TODO(video-refresh): Outline the live demo steps for recording. -->
""".strip()


def load_inventory(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))["items"]


def insert_after_first_youtube(body: str, block: str) -> str:
    match = re.search(r"\{\{<\s*youtube.*?>\}\}", body)
    if not match:
        return body.rstrip() + "\n\n" + block + "\n"
    return body[: match.end()].rstrip() + "\n\n" + block + "\n\n" + body[match.end() :].lstrip()


def main():
    parser = argparse.ArgumentParser(description="Bootstrap guide/script sections into selected video pages.")
    parser.add_argument("--inventory", default=str(INVENTORY_PATH))
    parser.add_argument("--slug", action="append", required=True)
    args = parser.parse_args()

    wanted_slugs = set(args.slug)
    inventory_path = Path(args.inventory)
    items = load_inventory(inventory_path)
    updated = 0

    for item in items:
        if item["slug"] not in wanted_slugs or item["page_kind"] == "course-hub":
            continue
        path = inventory_path.parents[1] / item["source_path"]
        text = path.read_text(encoding="utf-8")
        if "## What This Covers" in text:
            continue
        frontmatter, body = split_frontmatter(text)
        new_body = insert_after_first_youtube(body, TEMPLATE)
        path.write_text(f"---\n{frontmatter}\n---\n{new_body}", encoding="utf-8")
        updated += 1
        print(f"bootstrapped: {path}")

    print(f"completed: updated={updated}")


if __name__ == "__main__":
    main()
