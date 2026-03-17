#!/usr/bin/env python3
"""
Download caption tracks for owned YouTube videos via the YouTube Data API.

This script uses OAuth for an installed application. It requires a Google OAuth
client secrets JSON file created in Google Cloud Console with the YouTube Data
API enabled.

Recommended venv packages:
    pip install google-auth google-auth-oauthlib

Typical usage from docs/website:
    .venv-video-refresh/bin/python3 scripts/download_youtube_captions.py \
      --client-secrets client_secret.json \
      --limit 10
"""

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

from video_refresh_lib import (
    INVENTORY_PATH,
    load_transcript_meta,
    normalize_transcript_text,
    save_transcript,
    transcript_has_meaningful_content,
    transcript_paths,
)

API_BASE = "https://www.googleapis.com/youtube/v3"
OAUTH_SCOPES = ["https://www.googleapis.com/auth/youtube.force-ssl"]


def load_inventory(path: Path):
    payload = json.loads(path.read_text(encoding="utf-8"))
    return payload["items"]


def dedupe_items(items: Iterable[Dict[str, object]]) -> List[Dict[str, object]]:
    seen = set()
    deduped = []
    for item in items:
        youtube_id = item.get("youtube_id")
        if not youtube_id or youtube_id in seen:
            continue
        seen.add(youtube_id)
        deduped.append(item)
    return deduped


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
        if args.only_missing and item["transcript_status"] != "transcript-missing":
            continue
        selected.append(item)
    selected = dedupe_items(selected)
    if args.limit:
        selected = selected[: args.limit]
    return selected


def get_credentials(client_secrets: Path, token_path: Path):
    try:
        from google.auth.transport.requests import Request
        from google.oauth2.credentials import Credentials
        from google_auth_oauthlib.flow import InstalledAppFlow
    except ImportError as err:
        raise RuntimeError(
            "Missing OAuth dependencies. Install google-auth and google-auth-oauthlib in your venv."
        ) from err

    credentials = None
    if token_path.exists():
        credentials = Credentials.from_authorized_user_file(str(token_path), OAUTH_SCOPES)
    if credentials and credentials.valid:
        return credentials
    if credentials and credentials.expired and credentials.refresh_token:
        credentials.refresh(Request())
    else:
        flow = InstalledAppFlow.from_client_secrets_file(str(client_secrets), OAUTH_SCOPES)
        credentials = flow.run_local_server(port=0)
    token_path.write_text(credentials.to_json(), encoding="utf-8")
    return credentials


def api_json_get(path: str, access_token: str, params: Dict[str, str]) -> Dict[str, object]:
    url = f"{API_BASE}/{path}?{urllib.parse.urlencode(params)}"
    request = urllib.request.Request(url)
    request.add_header("Authorization", f"Bearer {access_token}")
    request.add_header("Accept", "application/json")
    with urllib.request.urlopen(request) as response:
        return json.loads(response.read().decode("utf-8"))


def api_download(path: str, access_token: str, params: Dict[str, str]) -> str:
    query = dict(params)
    query["alt"] = "media"
    url = f"{API_BASE}/{path}?{urllib.parse.urlencode(query)}"
    request = urllib.request.Request(url)
    request.add_header("Authorization", f"Bearer {access_token}")
    with urllib.request.urlopen(request) as response:
        return response.read().decode("utf-8", errors="replace")


def format_http_error(err: urllib.error.HTTPError) -> str:
    try:
        body = err.read().decode("utf-8", errors="replace")
    except Exception:
        body = ""
    if not body:
        return f"http-{err.code}"
    try:
        payload = json.loads(body)
        return f"http-{err.code}: {json.dumps(payload, sort_keys=True)}"
    except Exception:
        compact = " ".join(body.split())
        return f"http-{err.code}: {compact}"


def score_caption_track(item: Dict[str, object], preferred_languages: List[str]) -> Tuple[int, int]:
    snippet = item.get("snippet", {})
    language = str(snippet.get("language", "")).lower()
    track_kind = str(snippet.get("trackKind", "")).lower()
    is_asr = track_kind == "asr"
    language_rank = preferred_languages.index(language) if language in preferred_languages else len(preferred_languages) + 1
    kind_rank = 1 if is_asr else 0
    return (language_rank, kind_rank)


def choose_caption_track(items: List[Dict[str, object]], preferred_languages: List[str]) -> Optional[Dict[str, object]]:
    if not items:
        return None
    ordered = sorted(items, key=lambda item: score_caption_track(item, preferred_languages))
    return ordered[0]


def vtt_to_text(vtt_text: str) -> str:
    lines = vtt_text.replace("\r\n", "\n").split("\n")
    text_lines: List[str] = []
    for raw in lines:
        line = raw.strip()
        if not line:
            text_lines.append("")
            continue
        if line == "WEBVTT":
            continue
        if "-->" in line:
            continue
        if line.startswith("Kind:") or line.startswith("Language:"):
            continue
        if line.isdigit():
            continue
        line = urllib.parse.unquote(line)
        text_lines.append(line)
    return normalize_transcript_text("\n".join(text_lines))


def list_captions(access_token: str, youtube_id: str) -> List[Dict[str, object]]:
    payload = api_json_get(
        "captions",
        access_token,
        {
            "part": "id,snippet",
            "videoId": youtube_id,
            "maxResults": "50",
        },
    )
    return list(payload.get("items", []))


def download_caption(access_token: str, caption_id: str, tfmt: str) -> str:
    return api_download("captions/" + urllib.parse.quote(caption_id), access_token, {"tfmt": tfmt})


def fetch_owned_caption(access_token: str, youtube_id: str, preferred_languages: List[str]) -> Tuple[Optional[str], str]:
    try:
        items = list_captions(access_token, youtube_id)
    except urllib.error.HTTPError as err:
        return None, format_http_error(err)
    except Exception as err:  # pragma: no cover - network/runtime dependent
        return None, str(err)
    if not items:
        return None, "no-owned-captions"
    chosen = choose_caption_track(items, preferred_languages)
    if not chosen:
        return None, "no-matching-caption-track"
    caption_id = str(chosen["id"])
    try:
        vtt = download_caption(access_token, caption_id, "vtt")
    except urllib.error.HTTPError as err:
        return None, format_http_error(err)
    except Exception as err:  # pragma: no cover - network/runtime dependent
        return None, str(err)
    text = vtt_to_text(vtt)
    return (text if transcript_has_meaningful_content(text) else None), caption_id


def main():
    parser = argparse.ArgumentParser(description="Download owned YouTube captions via the YouTube Data API.")
    parser.add_argument("--inventory", default=str(INVENTORY_PATH))
    parser.add_argument("--client-secrets", required=True, help="OAuth client secrets JSON path")
    parser.add_argument("--token-file", default=".youtube-oauth-token.json", help="Cached OAuth token path")
    parser.add_argument("--slug", action="append", help="Process only a specific slug")
    parser.add_argument("--youtube-id", action="append", help="Process only a specific YouTube ID")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--language", action="append", default=["en", "en-us", "en-gb"], help="Preferred caption language in priority order")
    parser.add_argument("--only-missing", action="store_true", default=True)
    parser.add_argument("--include-existing", dest="only_missing", action="store_false")
    args = parser.parse_args()

    inventory_path = Path(args.inventory)
    client_secrets = Path(args.client_secrets)
    token_path = Path(args.token_file)

    credentials = get_credentials(client_secrets, token_path)
    access_token = credentials.token

    items = select_items(load_inventory(inventory_path), args)
    wrote = 0
    skipped = 0

    for item in items:
        youtube_id = str(item["youtube_id"])
        text_path, meta_path = transcript_paths(youtube_id)
        if text_path.exists() and meta_path.exists() and args.only_missing:
            print(f"cached transcript: {youtube_id}")
            skipped += 1
            continue
        text, detail = fetch_owned_caption(access_token, youtube_id, args.language)
        if not text:
            print(f"skipped transcript: {youtube_id} ({detail})")
            skipped += 1
            continue
        save_transcript(
            youtube_id,
            text,
            {
                "source": "fetched-owner-api",
                "status": "transcript-fetched",
                "quality": "needs-review",
                "fetch_method": "youtube-data-api",
                "caption_track_id": detail,
                "source_path": item["source_path"],
            },
        )
        print(f"fetched transcript: {youtube_id} via youtube-data-api")
        wrote += 1

    print(f"completed: wrote={wrote} skipped={skipped} processed={len(items)}")


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as err:
        print(str(err), file=sys.stderr)
        sys.exit(2)
