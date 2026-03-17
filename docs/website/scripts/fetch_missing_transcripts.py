#!/usr/bin/env python3
import argparse
import json
import subprocess
import time
from pathlib import Path

from video_refresh_lib import (
    INVENTORY_PATH,
    extract_transcript,
    load_markdown_page,
    normalize_transcript_text,
    save_transcript,
    transcript_has_meaningful_content,
    transcript_paths,
)


def load_inventory(path: Path):
    payload = json.loads(path.read_text(encoding="utf-8"))
    return payload["items"]


def fetch_with_youtube_transcript_api(youtube_id: str):
    try:
        from youtube_transcript_api import YouTubeTranscriptApi
    except ImportError:
        return None, "youtube-transcript-api-not-installed"

    try:
        if hasattr(YouTubeTranscriptApi, "get_transcript"):
            transcript = YouTubeTranscriptApi.get_transcript(youtube_id, languages=["en"])
            text = "\n".join(chunk["text"].strip() for chunk in transcript if chunk.get("text"))
        else:
            transcript = YouTubeTranscriptApi().fetch(youtube_id, languages=["en"])
            text = "\n".join(getattr(chunk, "text", "").strip() for chunk in transcript if getattr(chunk, "text", ""))
    except Exception as err:  # pragma: no cover - network/runtime dependent
        return None, str(err)
    return text, "youtube_transcript_api"


def api_failure_is_authoritative(error_text: str) -> bool:
    text = (error_text or "").lower()
    return any(
        token in text
        for token in (
            "transcriptsdisabled",
            "notranscriptfound",
            "novideofound",
            "video unavailable",
            "the video is no longer available",
            "subtitles are disabled",
            "no transcripts were found",
        )
    )


def api_failure_is_ip_block(error_text: str) -> bool:
    text = (error_text or "").lower()
    return "requestblocked" in text or "ipblocked" in text or "youtube is blocking requests from your ip" in text


def fetch_with_yt_dlp(youtube_id: str):
    command = [
        "yt-dlp",
        "--skip-download",
        "--write-auto-sub",
        "--write-sub",
        "--sub-langs",
        "en.*",
        "--sub-format",
        "vtt",
        "--output",
        "-",
        f"https://www.youtube.com/watch?v={youtube_id}",
    ]
    try:
        result = subprocess.run(command, capture_output=True, text=True, check=False)
    except FileNotFoundError:
        return None, "yt-dlp-not-installed"
    if result.returncode != 0:
        return None, result.stderr.strip() or f"yt-dlp-exit-{result.returncode}"
    text = normalize_transcript_text(result.stdout)
    return text if transcript_has_meaningful_content(text) else None, "yt-dlp"


def select_items(items, args):
    selected = []
    wanted_slugs = set(args.slug or [])
    wanted_ids = set(args.youtube_id or [])
    for item in items:
        if item["page_kind"] == "course-hub":
            continue
        if wanted_slugs and item["slug"] not in wanted_slugs:
            continue
        if wanted_ids and item.get("youtube_id") not in wanted_ids:
            continue
        if args.only_missing:
            allowed_statuses = {"transcript-missing"}
            if args.retry_unavailable:
                allowed_statuses.add("unavailable")
            if item["transcript_status"] not in allowed_statuses:
                continue
        selected.append(item)
    if args.limit:
        selected = selected[: args.limit]
    return selected


def should_mark_unavailable(args, fetch_error: str, used_fallback: bool) -> bool:
    if not args.mark_unavailable:
        return False
    if used_fallback and not args.api_only:
        return False
    return api_failure_is_authoritative(fetch_error)


def should_stop_on_block(args, fetch_error: str) -> bool:
    return args.stop_on_ip_block and api_failure_is_ip_block(fetch_error)


def remove_existing_transcript_artifacts(youtube_id: str):
    text_path, meta_path = transcript_paths(youtube_id)
    if text_path.exists():
        text_path.unlink()
    if meta_path.exists():
        meta_path.unlink()


def update_unavailable_meta(meta_path: Path, youtube_id: str, source_path: str, fetch_error: str):
    meta_path.parent.mkdir(parents=True, exist_ok=True)
    meta = {
        "youtube_id": youtube_id,
        "source": "unavailable",
        "status": "unavailable",
        "quality": "unavailable",
        "source_path": source_path,
        "fetch_error": fetch_error or "authoritative-api-failure",
    }
    meta_path.write_text(json.dumps(meta, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="Fetch or extract transcripts for video pages.")
    parser.add_argument("--inventory", default=str(INVENTORY_PATH))
    parser.add_argument("--slug", action="append", help="Process only a specific slug")
    parser.add_argument("--youtube-id", action="append", help="Process only a specific YouTube ID")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--only-missing", action="store_true", default=True)
    parser.add_argument("--include-existing", dest="only_missing", action="store_false")
    parser.add_argument("--retry-unavailable", action="store_true", help="Reprocess items currently marked unavailable")
    parser.add_argument("--embedded-only", action="store_true", help="Only extract embedded transcripts")
    parser.add_argument("--allow-fetch", action="store_true", help="Attempt network-based transcript fetching")
    parser.add_argument("--api-only", action="store_true", help="Use youtube-transcript-api only and skip yt-dlp fallback")
    parser.add_argument("--mark-unavailable", action="store_true", help="Write unavailable metadata only for authoritative API failures")
    parser.add_argument("--delay-seconds", type=float, default=0.0, help="Sleep between network fetch attempts")
    parser.add_argument("--stop-on-ip-block", action="store_true", help="Stop immediately if YouTube starts blocking transcript requests")
    args = parser.parse_args()

    inventory_path = Path(args.inventory)
    items = select_items(load_inventory(inventory_path), args)
    written = 0
    unavailable = 0
    skipped = 0

    for item in items:
        youtube_id = item.get("youtube_id")
        if not youtube_id:
            continue
        text_path, meta_path = transcript_paths(youtube_id)
        if text_path.exists() and meta_path.exists() and args.only_missing:
            continue
        if args.retry_unavailable and item["transcript_status"] == "unavailable":
            remove_existing_transcript_artifacts(youtube_id)

        page = load_markdown_page(Path(inventory_path.parents[1] / item["source_path"]))
        embedded = extract_transcript(page.body)
        if embedded:
            save_transcript(
                youtube_id,
                embedded,
                {
                    "source": "embedded",
                    "status": "transcript-fetched",
                    "quality": "needs-review",
                    "source_path": item["source_path"],
                },
            )
            written += 1
            print(f"embedded transcript: {youtube_id} <- {item['source_path']}")
            continue

        if args.embedded_only:
            continue

        fetched = None
        source = None
        fetch_error = None
        used_fallback = False
        if args.allow_fetch:
            if args.delay_seconds > 0:
                time.sleep(args.delay_seconds)
            fetched, source = fetch_with_youtube_transcript_api(youtube_id)
            if not fetched:
                fetch_error = source
                if should_stop_on_block(args, fetch_error or ""):
                    print(f"stopping on ip block: {youtube_id} ({fetch_error})")
                    break
                if not args.api_only:
                    used_fallback = True
                    if args.delay_seconds > 0:
                        time.sleep(args.delay_seconds)
                    fetched, source = fetch_with_yt_dlp(youtube_id)
                    if not fetched:
                        fetch_error = source

        if fetched:
            save_transcript(
                youtube_id,
                fetched,
                {
                    "source": "fetched-automated",
                    "status": "transcript-fetched",
                    "quality": "needs-review",
                    "fetch_method": source,
                    "source_path": item["source_path"],
                },
            )
            written += 1
            print(f"fetched transcript: {youtube_id} via {source}")
        elif should_mark_unavailable(args, fetch_error or "", used_fallback):
            update_unavailable_meta(meta_path, youtube_id, item["source_path"], fetch_error or "")
            unavailable += 1
            print(f"unavailable transcript: {youtube_id} ({fetch_error or 'authoritative-api-failure'})")
        else:
            skipped += 1
            print(f"skipped transcript: {youtube_id} ({fetch_error or 'not fetched'})")

    print(f"completed: wrote={written} unavailable={unavailable} skipped={skipped} processed={len(items)}")


if __name__ == "__main__":
    main()
