#!/usr/bin/env python3
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple


ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "content"
DATA = ROOT / "data"
HOWDOI_DIR = CONTENT / "howdoi"
COURSES_DIR = CONTENT / "courses"
TRANSCRIPTS_DIR = ROOT / "video-transcripts"
INVENTORY_PATH = DATA / "video_refresh_inventory.json"
HOWDOI_INDEX_PATH = DATA / "howdoi_index.json"
COURSE_HUBS = [
    CONTENT / "course-01-java-for-mobile-devices.md",
    CONTENT / "course-02-deep-dive-mobile-development-with-codename-one.md",
    CONTENT / "course-03-build-real-world-full-stack-mobile-apps-java.md",
]

YOUTUBE_RE = re.compile(r"\{\{<\s*youtube\s+\"?([A-Za-z0-9_-]+)\"?\s*>\}\}")
HEADING_RE = re.compile(r"^(#{1,6})\s+(.*?)\s*$")


@dataclass
class MarkdownPage:
    path: Path
    meta: Dict[str, object]
    body: str


def split_frontmatter(text: str) -> Tuple[str, str]:
    if text.startswith("---\n"):
        end = text.find("\n---\n", 4)
        if end != -1:
            return text[4:end], text[end + 5 :]
    return "", text


def parse_frontmatter(frontmatter: str) -> Dict[str, object]:
    meta: Dict[str, object] = {}
    current_key: Optional[str] = None
    for raw_line in frontmatter.splitlines():
        line = raw_line.rstrip()
        if not line.strip():
            continue
        if line.startswith("- ") and current_key:
            meta.setdefault(current_key, [])
            assert isinstance(meta[current_key], list)
            meta[current_key].append(parse_scalar(line[2:].strip()))
            continue
        if ":" not in line:
            current_key = None
            continue
        key, value = line.split(":", 1)
        key = key.strip()
        value = value.strip()
        if value == "":
            meta[key] = []
            current_key = key
            continue
        meta[key] = parse_scalar(value)
        current_key = key
    return meta


def parse_scalar(value: str):
    if value.startswith('"') and value.endswith('"'):
        return value[1:-1]
    if value.startswith("'") and value.endswith("'"):
        return value[1:-1]
    if value.lower() == "true":
        return True
    if value.lower() == "false":
        return False
    if re.fullmatch(r"-?\d+", value):
        try:
            return int(value)
        except ValueError:
            return value
    return value


def load_markdown_page(path: Path) -> MarkdownPage:
    text = path.read_text(encoding="utf-8")
    frontmatter, body = split_frontmatter(text)
    return MarkdownPage(path=path, meta=parse_frontmatter(frontmatter), body=body)


def derive_public_url(page: MarkdownPage, page_kind: str) -> str:
    url = str(page.meta.get("url", "")).strip()
    if url:
        return url
    slug = str(page.meta.get("slug", page.path.stem))
    if page_kind == "howdoi":
        return f"/how-do-i/{slug}/"
    if page_kind == "course-hub":
        return f"/{slug}/"
    course_id = str(page.meta.get("course_id", ""))
    return f"/courses/{course_id}/{page.path.stem}/"


def find_youtube_id(body: str) -> Optional[str]:
    match = YOUTUBE_RE.search(body)
    return match.group(1) if match else None


def heading_title(line: str) -> Optional[Tuple[int, str]]:
    match = HEADING_RE.match(line)
    if not match:
        return None
    return len(match.group(1)), match.group(2).strip().lower()


def extract_section(body: str, section_name: str) -> Optional[str]:
    lines = body.splitlines()
    target_index = None
    target_level = None
    wanted = section_name.strip().lower()
    for index, line in enumerate(lines):
        heading = heading_title(line)
        if not heading:
            continue
        level, title = heading
        if title == wanted:
            target_index = index
            target_level = level
            break
    if target_index is None or target_level is None:
        return None

    collected: List[str] = []
    for line in lines[target_index + 1 :]:
        heading = heading_title(line)
        if heading and heading[0] <= target_level:
            break
        collected.append(line)
    text = "\n".join(collected).strip()
    return text or None


def extract_transcript(body: str) -> Optional[str]:
    transcript = extract_section(body, "transcript")
    if not transcript:
        return None
    cleaned = normalize_transcript_text(transcript)
    return cleaned if transcript_has_meaningful_content(cleaned) else None


def extract_discussion(body: str) -> Optional[str]:
    return extract_section(body, "discussion")


def transcript_has_meaningful_content(text: str) -> bool:
    if not text:
        return False
    words = text.split()
    if len(words) < 20:
        return False
    alnum = sum(ch.isalnum() for ch in text)
    return alnum >= 80


def normalize_transcript_text(text: str) -> str:
    lines = [line.rstrip() for line in text.replace("\r\n", "\n").split("\n")]
    normalized: List[str] = []
    last_non_empty = None
    for raw in lines:
        line = re.sub(r"\s+", " ", raw).strip()
        if line == "---":
            continue
        if re.fullmatch(r"\d{1,2}:\d{2}(?::\d{2})?", line):
            continue
        if re.fullmatch(r"\d+\s*-->\s*\d+", line):
            continue
        if line == last_non_empty and line:
            continue
        if line:
            normalized.append(line)
            last_non_empty = line
        elif normalized and normalized[-1] != "":
            normalized.append("")
    return "\n".join(normalized).strip()


def transcript_paths(youtube_id: str) -> Tuple[Path, Path]:
    return TRANSCRIPTS_DIR / f"{youtube_id}.txt", TRANSCRIPTS_DIR / f"{youtube_id}.json"


def load_transcript_meta(youtube_id: str) -> Dict[str, object]:
    _, meta_path = transcript_paths(youtube_id)
    if not meta_path.exists():
        return {}
    return json.loads(meta_path.read_text(encoding="utf-8"))


def load_transcript_text(youtube_id: str) -> Optional[str]:
    text_path, _ = transcript_paths(youtube_id)
    if not text_path.exists():
        return None
    return text_path.read_text(encoding="utf-8").strip() or None


def save_transcript(youtube_id: str, text: str, meta: Dict[str, object]) -> None:
    TRANSCRIPTS_DIR.mkdir(parents=True, exist_ok=True)
    normalized = normalize_transcript_text(text)
    text_path, meta_path = transcript_paths(youtube_id)
    text_path.write_text(normalized.rstrip() + "\n", encoding="utf-8")
    meta = dict(meta)
    meta["youtube_id"] = youtube_id
    meta["line_count"] = len([line for line in normalized.splitlines() if line.strip()])
    meta["word_count"] = len(normalized.split())
    meta_path.write_text(json.dumps(meta, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def load_howdoi_index() -> Dict[str, Dict[str, object]]:
    if not HOWDOI_INDEX_PATH.exists():
        return {}
    items = json.loads(HOWDOI_INDEX_PATH.read_text(encoding="utf-8"))
    return {str(item["slug"]): item for item in items}


def stable_relpath(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def normalize_slug_from_lesson_filename(name: str) -> str:
    return re.sub(r"^\d+-", "", name)


def determine_priority(item: Dict[str, object]) -> int:
    text = " ".join(
        str(item.get(key, "")).lower()
        for key in ("lesson_title", "module_title", "slug", "course_id")
    )
    if any(
        token in text
        for token in (
            "hello-world",
            "hello world",
            "setup",
            "installation",
            "layout",
            "theme",
            "css",
            "build",
            "debug",
            "native interface",
            "native-code",
            "native code",
            "push",
        )
    ):
        return 0
    if str(item.get("content_type")) == "howdoi" or "course-01" in text:
        return 1
    return 2


def inventory_sort_key(item: Dict[str, object]):
    return (
        int(item.get("priority", 99)),
        str(item.get("content_type", "")),
        str(item.get("course_id", "")),
        str(item.get("source_path", "")),
    )


def build_transcript_section(transcript: Optional[str], meta: Dict[str, object]) -> str:
    lines = ["## Transcript", ""]
    if transcript:
        source = str(meta.get("source", "unknown"))
        lines.append(f"_Transcript source: {source}._")
        lines.append("")
        lines.append(transcript.strip())
    else:
        status = str(meta.get("status", "transcript-missing"))
        lines.append(f"_Transcript status: {status}._")
        lines.append("")
        lines.append("Transcript not yet available for this video.")
    return "\n".join(lines).strip()
