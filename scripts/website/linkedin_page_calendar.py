#!/usr/bin/env python3
"""Private, atomic Page publication reservations. Never publishes or approves content.

One coordinator owns Page writes; other producers submit candidates to it.
Reservations persist across crashes until remotely reconciled. Calendar files
must stay outside the public repository. Native feed + scheduled queue evidence
is required immediately before reserving a slot for a public write.
"""
import argparse
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
import fcntl
import json
import os
from pathlib import Path

GAP = timedelta(hours=20)
FRESH = timedelta(minutes=15)


def instant(value):
    d = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if d.tzinfo is None:
        raise ValueError("A UTC offset is required")
    return d.astimezone(timezone.utc)


def conflicts(state, item_id, at):
    at = instant(at)
    return [s for s in state["slots"] if s["id"] != item_id and s["status"] != "cancelled"
            and abs(at - instant(s["at"])) < GAP]


def reserve(state, item_id, at, approval, now):
    audit = state.get("native_audit", {})
    if not audit or not timedelta(0) <= now - instant(audit["observed_at"]) <= FRESH:
        raise ValueError("Fresh native Page feed AND scheduled-post audit required (15 minutes)")
    target = instant(at)
    if target < now - timedelta(minutes=1):
        raise ValueError("Missed publication window; do not roll forward")
    if instant(audit["from"]) > target - GAP or instant(audit["through"]) < target + GAP:
        raise ValueError("Native audit must cover the 20 hours before AND after publication")
    existing = next((s for s in state["slots"] if s["id"] == item_id), None)
    if existing:
        if existing["at"] != at or existing["status"] == "cancelled":
            raise ValueError("Existing ID cannot be moved or revived; review a new version")
        raise ValueError("Item already recorded/reserved; verify remotely, never publish again")
    collisions = conflicts(state, item_id, at)
    if collisions:
        raise ValueError("Page cadence collision: " + ", ".join(s["id"] for s in collisions))
    if not approval.strip():
        raise ValueError("Human approval reference required; calendar is not authorization")
    row = {"id": item_id, "at": at, "status": "reserved", "approval_ref": approval,
           "recorded_at": now.isoformat()}
    state["slots"].append(row)
    return row


@contextmanager
def calendar(path):
    # Never start with an empty calendar just because the real file disappeared.
    path = Path(path)
    with path.with_suffix(path.suffix + ".lock").open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        with path.open() as f:
            state = json.load(f)
        if state.get("account") != "linkedin:5041007" or not isinstance(state.get("slots"), list):
            raise ValueError("Wrong or malformed Page calendar")
        yield state
        temp = path.with_suffix(path.suffix + f".{os.getpid()}.tmp")
        try:
            temp.write_text(json.dumps(state, indent=2) + "\n")
            temp.replace(path)
        finally:
            temp.unlink(missing_ok=True)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--calendar", required=True)
    sub = p.add_subparsers(dest="command", required=True)
    for cmd in ("check", "reserve", "record"):
        c = sub.add_parser(cmd)
        c.add_argument("--id", required=True)
        c.add_argument("--at", required=True)
        if cmd == "reserve": c.add_argument("--approval-ref", required=True)
        if cmd == "record":
            c.add_argument("--status", choices=["published", "scheduled", "cancelled"], required=True)
            c.add_argument("--evidence", required=True)
            c.add_argument("--url", required=True)
    c = sub.add_parser("audit")
    for name in ("observed-at", "from", "through", "evidence"):
        c.add_argument("--" + name, required=True)
    a = p.parse_args()
    now = datetime.now(timezone.utc)
    try:
        with calendar(a.calendar) as state:
            if a.command == "audit":
                if not timedelta(0) <= now - instant(a.observed_at) <= FRESH:
                    raise ValueError("Audit must describe a current native observation")
                if instant(a.__dict__["from"]) >= instant(a.through):
                    raise ValueError("Invalid audit coverage")
                state["native_audit"] = {"observed_at": a.observed_at, "from": a.__dict__["from"],
                                         "through": a.through, "evidence": a.evidence}
                result = state["native_audit"]
            elif a.command == "check":
                result = {"conflicts": conflicts(state, a.id, a.at), "planning_only": True}
            elif a.command == "reserve":
                result = reserve(state, a.id, a.at, a.approval_ref, now)
            else:
                instant(a.at)
                existing = next((s for s in state["slots"] if s["id"] == a.id), None)
                if existing is not None and existing["at"] != a.at:
                    raise ValueError("Timing changed; reconcile explicitly with a new observation ID")
                row = {"id": a.id, "at": a.at, "status": a.status, "url": a.url,
                       "evidence": a.evidence, "recorded_at": now.isoformat()}
                # Record outside publications even if they reveal a collision.
                if existing is not None: existing.update(row)
                else: state["slots"].append(row)
                result = row
            print(json.dumps(result, indent=2))
            if a.command == "check" and result["conflicts"]: return 1
        return 0
    except (ValueError, KeyError, OSError) as e:
        p.exit(2, str(e) + "\n")


if __name__ == "__main__":
    raise SystemExit(main())
