#!/usr/bin/env python3
"""Keep MP4 files only for the newest verified private video pairs."""

from __future__ import annotations

import argparse
import json
import os
import tempfile
from pathlib import Path


VIDEO_ROOT = Path(__file__).resolve().parents[1]
STATE = VIDEO_ROOT / "projects" / "past-year" / "state.json"
OUTPUT_ROOT = Path(os.environ.get(
    "CN1_VIDEO_OUTPUT",
    Path(tempfile.gettempdir()) / "cn1-youtube-past-year",
)).resolve()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--keep", type=int, default=3)
    args = parser.parse_args()
    if args.keep < 1:
        parser.error("--keep must be positive")

    state = json.loads(STATE.read_text(encoding="utf-8"))
    verified = sorted(
        (item for item in state["items"]
         if item.get("status") == "verified-private" and item.get("verifiedAt")),
        key=lambda item: item["verifiedAt"],
        reverse=True,
    )
    removed: list[str] = []
    for item in verified[args.keep:]:
        output = Path(item["output"]).resolve()
        if output.parent != OUTPUT_ROOT:
            raise RuntimeError(f"refusing cleanup outside batch output root: {output}")
        for media in output.glob("*.mp4"):
            media.unlink()
            removed.append(str(media))
        item["cleanup"] = "local-media-removed"

    STATE.write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
    for path in removed:
        print(f"removed {path}")
    print(f"kept local MP4s for {min(args.keep, len(verified))} verified pairs")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
