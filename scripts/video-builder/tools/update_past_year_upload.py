#!/usr/bin/env python3
"""Record verified private YouTube uploads in the resumable batch state."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / "projects" / "past-year" / "state.json"
VIDEO_ID = re.compile(r"^[A-Za-z0-9_-]{11}$")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while True:
            block = stream.read(1024 * 1024)
            if not block:
                break
            digest.update(block)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("slug")
    parser.add_argument("--landscape-id", required=True)
    parser.add_argument("--short-id", required=True)
    parser.add_argument("--trail-playlist-id")
    parser.add_argument("--shorts-playlist-id")
    parser.add_argument("--end-screen-video-id")
    args = parser.parse_args()
    for value in (args.landscape_id, args.short_id):
        if not VIDEO_ID.fullmatch(value):
            parser.error(f"invalid YouTube video id: {value}")
    if args.end_screen_video_id and not VIDEO_ID.fullmatch(args.end_screen_video_id):
        parser.error(f"invalid end-screen video id: {args.end_screen_video_id}")

    state = json.loads(STATE.read_text(encoding="utf-8"))
    item = next((candidate for candidate in state["items"] if candidate["slug"] == args.slug), None)
    if item is None:
        parser.error(f"slug is not present in state: {args.slug}")

    output = Path(item["output"])
    qa_path = output / "qa-report.json"
    if not qa_path.is_file():
        parser.error(f"QA report is missing: {qa_path}")
    qa = json.loads(qa_path.read_text(encoding="utf-8"))
    if qa.get("status") != "passed":
        parser.error(f"QA report has not passed: {qa_path}")
    for key in ("landscape", "portrait"):
        media = qa["media"][key]
        path = Path(media["path"])
        if not path.is_file() or sha256(path) != media["sha256"]:
            parser.error(f"{key} media no longer matches the passed QA report: {path}")

    item.update({
        "status": "verified-private",
        "landscapeId": args.landscape_id,
        "shortId": args.short_id,
        "cleanup": "retained",
        "verifiedAt": datetime.now(timezone.utc).isoformat(),
        "uploadVerification": {
            "landscape": {
                "privacy": "private",
                "captions": "verified-authored-srt",
                "checks": "clear",
                "thumbnail": "verified-custom",
                "endScreen": ("verified-subscribe-and-specific-video"
                              if args.end_screen_video_id else
                              "verified-subscribe-and-best-for-viewer"),
                "endScreenTargetId": args.end_screen_video_id,
            },
            "portrait": {
                "privacy": "private",
                "captions": "verified-authored-srt",
                "checks": "clear",
                "relatedVideo": "pending-until-landscape-is-unlisted-or-public",
            },
        },
    })
    if args.trail_playlist_id and args.shorts_playlist_id:
        item["playlistVerification"] = {
            "trail": {
                "id": args.trail_playlist_id,
                "privacy": "private",
                "officialSeries": True,
                "videoAssigned": True,
            },
            "shorts": {
                "id": args.shorts_playlist_id,
                "privacy": "private",
                "officialSeries": False,
                "videoAssigned": True,
            },
        }
    reports = {}
    for name in (args.slug, f"{args.slug}-short"):
        report_path = output / f"{name}-report.json"
        if report_path.is_file():
            reports[name] = json.loads(report_path.read_text(encoding="utf-8"))
    if len(reports) == 2:
        item["renders"] = {
            "landscape": reports[args.slug]["outputs"][0],
            "portrait": reports[f"{args.slug}-short"]["outputs"][0],
        }
    for key in ("landscape", "portrait"):
        media = qa["media"][key]
        render = item.setdefault("renders", {}).setdefault(key, {})
        render.update({
            "path": media["path"],
            "width": media["video"]["width"],
            "height": media["video"]["height"],
            "frameRate": 30,
            "durationMs": round(media["durationSeconds"] * 1000),
            "sha256": media["sha256"],
        })
    workflow = item.setdefault("workflow", {"schemaVersion": 1})
    workflow["remote"] = {
        "status": "verified-private",
        "verifiedAt": item["verifiedAt"],
        "landscapeId": args.landscape_id,
        "shortId": args.short_id,
        "mediaHashes": {
            key: qa["media"][key]["sha256"] for key in ("landscape", "portrait")
        },
        "captions": "verified-authored-srt",
        "thumbnail": "verified-custom",
        "endScreenTargetId": args.end_screen_video_id,
        "trailPlaylistId": args.trail_playlist_id,
        "shortsPlaylistId": args.shorts_playlist_id,
    }
    STATE.write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
    print(f"verified {args.slug}: {args.landscape_id} / {args.short_id}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
