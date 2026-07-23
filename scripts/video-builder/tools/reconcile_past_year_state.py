#!/usr/bin/env python3
"""Reconcile resumable past-year state with a read-only YouTube Studio catalog."""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_STATE = ROOT / "projects" / "past-year" / "state.json"


def reconcile(state: dict, catalog: dict) -> tuple[dict, dict]:
    videos = {video["videoId"]: video for video in catalog["videos"]}
    retained = []
    missing = []
    now = datetime.now(timezone.utc).isoformat()
    for item in state["items"]:
        landscape_id = item.get("landscapeId")
        short_id = item.get("shortId")
        if not landscape_id and not short_id:
            continue
        landscape = videos.get(landscape_id)
        portrait = videos.get(short_id)
        if landscape and portrait:
            public = landscape["privacy"] == "public" and portrait["privacy"] == "public"
            item["status"] = "verified-public" if public else "verified-private"
            workflow = item.setdefault("workflow", {"schemaVersion": 1})
            workflow["remote"] = {
                **workflow.get("remote", {}),
                "status": item["status"],
                "reconciledAt": now,
                "landscapeId": landscape_id,
                "shortId": short_id,
                "visibility": {
                    "landscape": landscape["privacy"],
                    "portrait": portrait["privacy"],
                },
            }
            retained.append(item["slug"])
            continue

        item["previousRemote"] = {
            "landscapeId": landscape_id,
            "shortId": short_id,
            "verifiedAt": item.get("verifiedAt"),
            "status": item.get("status", "unknown"),
            "reconciledAt": now,
            "reason": "one or both recorded uploads are absent from the live channel catalog",
        }
        item["landscapeId"] = None
        item["shortId"] = None
        item["status"] = "authored"
        item.pop("uploadVerification", None)
        item.pop("playlistVerification", None)
        workflow = item.setdefault("workflow", {"schemaVersion": 1})
        workflow["remote"] = {
            "status": "missing-after-channel-reconciliation",
            "reconciledAt": now,
        }
        missing.append(item["slug"])
    summary = {"retained": retained, "missing": missing}
    state["channelReconciliation"] = {
        "at": now,
        "catalogChannelId": catalog["channelId"],
        "retainedCount": len(retained),
        "missingCount": len(missing),
    }
    return state, summary


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("catalog", type=Path)
    parser.add_argument("--state", type=Path, default=DEFAULT_STATE)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    state = json.loads(args.state.read_text(encoding="utf-8"))
    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    reconciled, summary = reconcile(state, catalog)
    if args.apply:
        args.state.write_text(json.dumps(reconciled, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"applied": args.apply, **summary}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
