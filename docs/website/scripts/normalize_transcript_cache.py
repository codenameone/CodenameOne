#!/usr/bin/env python3
import argparse
from pathlib import Path

from video_refresh_lib import TRANSCRIPTS_DIR, load_transcript_meta, save_transcript


def main():
    parser = argparse.ArgumentParser(description="Normalize transcript cache files and backfill metadata.")
    parser.add_argument("--transcripts-dir", default=str(TRANSCRIPTS_DIR))
    parser.add_argument("--youtube-id", action="append", help="Normalize only specific YouTube IDs")
    args = parser.parse_args()

    base = Path(args.transcripts_dir)
    wanted_ids = set(args.youtube_id or [])
    updated = 0

    for text_path in sorted(base.glob("*.txt")):
        youtube_id = text_path.stem
        if wanted_ids and youtube_id not in wanted_ids:
            continue
        raw = text_path.read_text(encoding="utf-8", errors="replace")
        meta = load_transcript_meta(youtube_id)
        if not meta:
            meta = {
                "source": "fetched-manual",
                "status": "transcript-fetched",
                "quality": "needs-review",
            }
        else:
            meta = dict(meta)
            meta.setdefault("source", "fetched-manual")
            meta.setdefault("status", "transcript-fetched")
            meta.setdefault("quality", "needs-review")
        save_transcript(youtube_id, raw, meta)
        updated += 1
        print(f"normalized: {youtube_id}")

    print(f"completed: updated={updated}")


if __name__ == "__main__":
    main()
