#!/usr/bin/env python3
import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path

from video_refresh_lib import (
    COURSE_HUBS,
    COURSES_DIR,
    HOWDOI_DIR,
    INVENTORY_PATH,
    determine_priority,
    derive_public_url,
    extract_section,
    extract_transcript,
    find_youtube_id,
    inventory_sort_key,
    load_howdoi_index,
    load_markdown_page,
    load_transcript_meta,
    load_transcript_text,
    normalize_slug_from_lesson_filename,
    stable_relpath,
)


def build_items():
    howdoi_index = load_howdoi_index()
    items = []

    for path in sorted(HOWDOI_DIR.glob("*.md")):
        page = load_markdown_page(path)
        slug = str(page.meta.get("slug", path.stem))
        youtube_id = str(page.meta.get("youtube_id", find_youtube_id(page.body) or "")) or None
        embedded_transcript = extract_transcript(page.body)
        transcript_meta = load_transcript_meta(youtube_id) if youtube_id else {}
        transcript_text = load_transcript_text(youtube_id) if youtube_id else None
        anomalies = []
        if slug not in howdoi_index:
            anomalies.append("missing-from-howdoi-index")
        if not youtube_id:
            anomalies.append("missing-youtube-embed")
        if page.meta.get("type") != "howdoi":
            anomalies.append("unexpected-type")
        if extract_section(page.body, "transcript") and not embedded_transcript:
            anomalies.append("transcript-heading-without-usable-content")
        if not page.meta.get("title"):
            anomalies.append("missing-title")
        if not page.meta.get("description"):
            anomalies.append("missing-description")

        transcript_source = transcript_meta.get("source")
        transcript_status = transcript_meta.get("status")
        if not transcript_source and embedded_transcript:
            transcript_source = "embedded"
        if not transcript_status:
            transcript_status = "transcript-fetched" if (embedded_transcript or transcript_text) else "transcript-missing"

        item = {
            "content_type": "howdoi",
            "page_kind": "howdoi",
            "course_id": "",
            "module_title": "",
            "lesson_title": str(page.meta.get("title", slug)),
            "slug": slug,
            "source_path": stable_relpath(path),
            "public_url": derive_public_url(page, "howdoi"),
            "youtube_id": youtube_id,
            "page_has_youtube": bool(youtube_id),
            "page_has_transcript": bool(embedded_transcript or transcript_text),
            "transcript_source": transcript_source or "",
            "transcript_status": transcript_status,
            "guide_status": "guide-in-progress" if extract_section(page.body, "what this covers") else "guide-not-started",
            "script_status": "script-drafted" if extract_section(page.body, "video update script") else "script-not-started",
            "priority": 0,
            "owner": "",
            "notes": "",
            "anomalies": anomalies,
        }
        item["priority"] = determine_priority(item)
        items.append(item)

    for path in sorted(COURSE_HUBS):
        page = load_markdown_page(path)
        slug = str(page.meta.get("slug", path.stem))
        anomalies = []
        if not page.meta.get("course_id"):
            anomalies.append("missing-course-id")
        if not page.meta.get("layout"):
            anomalies.append("missing-layout")
        item = {
            "content_type": "course",
            "page_kind": "course-hub",
            "course_id": str(page.meta.get("course_id", slug)),
            "module_title": "",
            "lesson_title": str(page.meta.get("title", slug)),
            "slug": slug,
            "source_path": stable_relpath(path),
            "public_url": derive_public_url(page, "course-hub"),
            "youtube_id": None,
            "page_has_youtube": False,
            "page_has_transcript": False,
            "transcript_source": "",
            "transcript_status": "transcript-missing",
            "guide_status": "guide-in-progress" if extract_section(page.body, "what this covers") else "guide-not-started",
            "script_status": "script-drafted" if extract_section(page.body, "video update script") else "script-not-started",
            "priority": 1,
            "owner": "",
            "notes": "",
            "anomalies": anomalies,
        }
        item["priority"] = determine_priority(item)
        items.append(item)

    for path in sorted(COURSES_DIR.rglob("*.md")):
        page = load_markdown_page(path)
        youtube_id = find_youtube_id(page.body)
        embedded_transcript = extract_transcript(page.body)
        transcript_meta = load_transcript_meta(youtube_id) if youtube_id else {}
        transcript_text = load_transcript_text(youtube_id) if youtube_id else None
        anomalies = []
        if not youtube_id:
            anomalies.append("missing-youtube-embed")
        if not page.meta.get("course_id"):
            anomalies.append("missing-course-id")
        if not page.meta.get("module_title"):
            anomalies.append("missing-module-title")
        if not page.meta.get("title"):
            anomalies.append("missing-title")
        if extract_section(page.body, "transcript") and not embedded_transcript:
            anomalies.append("transcript-heading-without-usable-content")

        transcript_source = transcript_meta.get("source")
        transcript_status = transcript_meta.get("status")
        if not transcript_source and embedded_transcript:
            transcript_source = "embedded"
        if not transcript_status:
            transcript_status = "transcript-fetched" if (embedded_transcript or transcript_text) else "transcript-missing"

        item = {
            "content_type": "course",
            "page_kind": "course-lesson",
            "course_id": str(page.meta.get("course_id", "")),
            "module_title": str(page.meta.get("module_title", "")),
            "lesson_title": str(page.meta.get("title", normalize_slug_from_lesson_filename(path.stem))),
            "slug": normalize_slug_from_lesson_filename(path.stem),
            "source_path": stable_relpath(path),
            "public_url": derive_public_url(page, "course-lesson"),
            "youtube_id": youtube_id,
            "page_has_youtube": bool(youtube_id),
            "page_has_transcript": bool(embedded_transcript or transcript_text),
            "transcript_source": transcript_source or "",
            "transcript_status": transcript_status,
            "guide_status": "guide-in-progress" if extract_section(page.body, "what this covers") else "guide-not-started",
            "script_status": "script-drafted" if extract_section(page.body, "video update script") else "script-not-started",
            "priority": 2,
            "owner": "",
            "notes": "",
            "anomalies": anomalies,
        }
        item["priority"] = determine_priority(item)
        items.append(item)

    by_youtube = defaultdict(list)
    for item in items:
        youtube_id = item.get("youtube_id")
        if youtube_id:
            by_youtube[str(youtube_id)].append(item)
    for youtube_id, grouped in by_youtube.items():
        if len(grouped) > 1:
            for item in grouped:
                item["anomalies"].append(f"duplicate-youtube-id:{youtube_id}")

    return sorted(items, key=inventory_sort_key)


def build_summary(items):
    counts = Counter()
    anomalies = Counter()
    for item in items:
        counts[f'content_type:{item["content_type"]}'] += 1
        counts[f'page_kind:{item["page_kind"]}'] += 1
        counts[f'priority:{item["priority"]}'] += 1
        counts[f'transcript_status:{item["transcript_status"]}'] += 1
        counts[f'guide_status:{item["guide_status"]}'] += 1
        counts[f'script_status:{item["script_status"]}'] += 1
        if item["page_has_youtube"]:
            counts["video_backed_pages"] += 1
        if item["page_has_transcript"]:
            counts["pages_with_transcripts"] += 1
        for anomaly in item["anomalies"]:
            anomalies[anomaly] += 1
    return {
        "counts": dict(sorted(counts.items())),
        "anomalies": dict(sorted(anomalies.items())),
    }


def main():
    parser = argparse.ArgumentParser(description="Build video refresh inventory.")
    parser.add_argument("--output", default=str(INVENTORY_PATH), help="Output JSON path")
    parser.add_argument("--stdout", action="store_true", help="Print inventory to stdout")
    args = parser.parse_args()

    items = build_items()
    payload = {
        "summary": build_summary(items),
        "items": items,
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, indent=2, sort_keys=False) + "\n", encoding="utf-8")

    if args.stdout:
        print(json.dumps(payload, indent=2, sort_keys=False))
    else:
        print(f"Wrote {len(items)} inventory entries to {output}")


if __name__ == "__main__":
    main()
