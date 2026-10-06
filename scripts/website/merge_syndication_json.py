#!/usr/bin/env python3
"""Three-way merge JSON state written by concurrent syndication publishers."""

from __future__ import annotations

import argparse
import copy
import json
import sys
from pathlib import Path


class MergeConflict(ValueError):
    """Raised when both sides changed the same JSON value differently."""


MISSING = object()


def display_path(path: tuple[str, ...]) -> str:
    return ".".join(path) if path else "<root>"


def merge_id_list(
    base: list,
    local: list,
    remote: list,
    path: tuple[str, ...],
) -> list:
    def keyed(items: list) -> dict[str, object] | None:
        result: dict[str, object] = {}
        for item in items:
            if not isinstance(item, dict) or not isinstance(item.get("id"), str):
                return None
            item_id = item["id"]
            if item_id in result:
                raise MergeConflict(
                    f"duplicate id {item_id!r} at {display_path(path)}"
                )
            result[item_id] = item
        return result

    base_by_id = keyed(base)
    local_by_id = keyed(local)
    remote_by_id = keyed(remote)
    if base_by_id is None or local_by_id is None or remote_by_id is None:
        raise MergeConflict(f"conflicting list changes at {display_path(path)}")

    order = list(remote_by_id)
    order.extend(item_id for item_id in local_by_id if item_id not in remote_by_id)
    merged = []
    for item_id in order:
        value = merge_value(
            base_by_id.get(item_id, MISSING),
            local_by_id.get(item_id, MISSING),
            remote_by_id.get(item_id, MISSING),
            (*path, item_id),
        )
        if value is not MISSING:
            merged.append(value)
    return merged


def merge_value(
    base: object,
    local: object,
    remote: object,
    path: tuple[str, ...] = (),
) -> object:
    if local == remote:
        return copy.deepcopy(local)
    if local == base:
        return copy.deepcopy(remote)
    if remote == base:
        return copy.deepcopy(local)
    if local is MISSING or remote is MISSING:
        raise MergeConflict(f"conflicting changes at {display_path(path)}")

    dict_values = (base, local, remote)
    if all(value is MISSING or isinstance(value, dict) for value in dict_values):
        base_dict = {} if base is MISSING else base
        local_dict = {} if local is MISSING else local
        remote_dict = {} if remote is MISSING else remote
        assert isinstance(base_dict, dict)
        assert isinstance(local_dict, dict)
        assert isinstance(remote_dict, dict)

        # A stale run may recover the same publication from the platform API.
        # Its publication time can differ from the original local receipt time.
        # Only reconcile that metadata when both durable identity fields match.
        same_publication = (
            len(path) == 3 and path[0] == "posts"
            and local_dict.get("id") is not None
            and local_dict.get("id") == remote_dict.get("id")
            and isinstance(local_dict.get("url"), str)
            and bool(local_dict["url"])
            and local_dict["url"] == remote_dict.get("url")
        )

        keys = list(remote_dict)
        keys.extend(key for key in local_dict if key not in remote_dict)
        merged = {}
        for key in keys:
            if (same_publication and key == "syndicated_at"
                    and isinstance(local_dict.get(key), str)
                    and isinstance(remote_dict.get(key), str)
                    and local_dict[key] != base_dict.get(key, MISSING)
                    and remote_dict[key] != base_dict.get(key, MISSING)):
                merged[key] = remote_dict[key]
                continue
            value = merge_value(
                base_dict.get(key, MISSING),
                local_dict.get(key, MISSING),
                remote_dict.get(key, MISSING),
                (*path, key),
            )
            if value is not MISSING:
                merged[key] = value
        return merged

    if isinstance(base, list) and isinstance(local, list) and isinstance(remote, list):
        return merge_id_list(base, local, remote, path)

    raise MergeConflict(f"conflicting changes at {display_path(path)}")


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True)
    parser.add_argument("--local", required=True)
    parser.add_argument("--remote", required=True)
    parser.add_argument("--output", required=True)
    return parser.parse_args(argv)


def load_json(path: str) -> object:
    with Path(path).open(encoding="utf-8") as handle:
        return json.load(handle)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    try:
        merged = merge_value(
            load_json(args.base),
            load_json(args.local),
            load_json(args.remote),
        )
    except (OSError, json.JSONDecodeError, MergeConflict) as error:
        print(f"Unable to merge syndication JSON: {error}", file=sys.stderr)
        return 1

    Path(args.output).write_text(
        json.dumps(merged, indent=2) + "\n",
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
