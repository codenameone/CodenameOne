#!/usr/bin/env python3
"""Generate source-grounded video-builder packages for the past-year YouTube batch."""

from __future__ import annotations

import argparse
import json
import os
import re
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from widgets_video_story import (
    SLUG as WIDGETS_SLUG,
    scripts as widgets_scripts,
    write_assets as write_widgets_assets,
    write_thumbnail as write_widgets_thumbnail,
)
from pixel_fidelity_video_story import (
    SLUG as FIDELITY_SLUG,
    red_team as fidelity_red_team,
    scripts as fidelity_scripts,
    write_assets as write_fidelity_assets,
    write_thumbnail as write_fidelity_thumbnail,
)
from profiled_video_story import (
    profiles as story_profiles,
    red_team as profiled_red_team,
    scripts as profiled_scripts,
    write_assets as write_profiled_assets,
    write_thumbnail as write_profiled_thumbnail,
)


ROOT = Path(__file__).resolve().parents[3]
VIDEO_ROOT = ROOT / "scripts" / "video-builder"
PROJECT_ROOT = VIDEO_ROOT / "projects" / "past-year"
SELECTION = PROJECT_ROOT / "selection.json"
TRAILS = PROJECT_ROOT / "trails.json"
BLOG_ROOT = ROOT / "docs" / "website" / "content" / "blog"
SHARED_MODEL_DIR = VIDEO_ROOT / "examples" / "release-feature" / ".video-tools" / "kokoro"
PRONUNCIATION_INDEX = VIDEO_ROOT / "pronunciations" / "technical-en-us.json"

OUTPUT = {
    "frameRate": 30,
    "landscapeWidth": 1920,
    "landscapeHeight": 1080,
    "portraitWidth": 1080,
    "portraitHeight": 1920,
    "videoBitRate": 8_000_000,
}

PRONUNCIATIONS = json.loads(PRONUNCIATION_INDEX.read_text(encoding="utf-8"))["entries"]
STORY_PROFILES = story_profiles()


def trail_metadata(slug: str) -> dict:
    catalog = json.loads(TRAILS.read_text(encoding="utf-8"))
    for trail in catalog["trails"]:
        if slug in trail["slugs"]:
            return {
                "id": trail["id"],
                "title": trail["title"],
                "description": trail["description"],
                "position": trail["slugs"].index(slug) + 1,
                "total": len(trail["slugs"]),
                "privacyStatus": trail["privacyStatus"],
                "officialSeries": trail["officialSeries"],
            }
    raise ValueError(f"{slug}: missing from {TRAILS}")


def frontmatter(source: str) -> tuple[dict[str, str], str]:
    match = re.match(r"\A---\s*\n(.*?)\n---\s*\n", source, re.S)
    if not match:
        raise ValueError("missing YAML frontmatter")
    values: dict[str, str] = {}
    for line in match.group(1).splitlines():
        item = re.match(r"([A-Za-z_]+):\s*['\"]?(.*?)['\"]?\s*$", line)
        if item:
            values[item.group(1)] = item.group(2)
    return values, source[match.end():]


def clean_markdown(value: str) -> str:
    value = re.sub(r"\{\{<.*?>\}\}", "", value)
    value = re.sub(r"!\[([^]]*)\]\([^)]+\)", r"\1", value)
    value = re.sub(r"\[([^]]+)\]\([^)]+\)", r"\1", value)
    value = re.sub(r"<[^>]+>", "", value)
    value = value.replace("`", "").replace("*", "").replace("__", "")
    value = re.sub(r"\s+", " ", value).strip()
    return value


def sentences(value: str) -> list[str]:
    value = clean_markdown(value)
    return [part.strip() for part in re.split(r"(?<=[.!?])\s+", value) if len(part.strip()) > 25]


def limit_words(value: str, maximum: int) -> str:
    words = value.split()
    if len(words) <= maximum:
        return value[:-1] + "." if value.endswith(":") else value
    complete = re.split(r"(?<=[.!?])\s+", value.strip())
    selected: list[str] = []
    count = 0
    for sentence in complete:
        sentence_words = sentence.split()
        if selected and count + len(sentence_words) > maximum:
            # A terse setup sentence is often followed by the actual
            # explanation. Preserve that causal pair when the small overrun is
            # still comfortable for narration; otherwise a paragraph such as
            # "AR development has a miserable inner loop" loses every reason
            # that follows it.
            if count < maximum // 2 and count + len(sentence_words) <= maximum + 12:
                selected.append(sentence)
                count += len(sentence_words)
            break
        if not selected and len(sentence_words) <= maximum + 20:
            selected.append(sentence)
            count += len(sentence_words)
            continue
        if count + len(sentence_words) <= maximum:
            selected.append(sentence)
            count += len(sentence_words)
            continue
    if selected:
        result = " ".join(selected)
        return result[:-1] + "." if result.endswith(":") else result
    clipped = " ".join(words[:maximum])
    clause = max(clipped.rfind(","), clipped.rfind(";"), clipped.rfind(":"))
    if clause >= len(clipped) * 0.65:
        clipped = clipped[:clause]
    return clipped.rstrip(" ,;:") + "."


def article_parts(body: str) -> tuple[list[tuple[str, str]], list[str], list[str], str | None]:
    code_blocks = re.findall(r"```(?:[A-Za-z0-9_+.-]+)?\s*\n(.*?)```", body, re.S)
    links = re.findall(r"https?://[^\s)>\"]+", body)
    matches = list(re.finditer(r"^##+\s+(.+?)\s*$", body, re.M))
    sections: list[tuple[str, str]] = []
    code_heading: str | None = None
    primary_code: str | None = None
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(body)
        title = clean_markdown(match.group(1))
        if (title.lower() in {"discussion", "closing thoughts", "wrapping up", "conclusion"}
                or title.lower().startswith(("wrapping ", "in conclusion"))):
            continue
        section_body = body[match.end():end]
        if code_heading is None and "```" in section_body:
            code_heading = title
            section_code = re.search(
                r"```(?:[A-Za-z0-9_+.-]+)?\s*\n(.*?)```", section_body, re.S
            )
            if section_code:
                primary_code = section_code.group(1)
        prose_body = re.sub(r"```.*?```", "", section_body, flags=re.S)
        raw_paragraphs = re.split(r"\n\s*\n", prose_body)
        paragraphs = []
        for raw_paragraph in raw_paragraphs:
            stripped = raw_paragraph.strip()
            if not stripped or stripped.startswith(("#", "![", "{{<", "|", "---", "_", "- ", "* ")):
                continue
            paragraph = clean_markdown(stripped)
            if len(paragraph) > 55 and re.search(r"[.!?]", paragraph):
                paragraphs.append(paragraph)
        paragraph = next(iter(paragraphs), "")
        if paragraph and len(paragraph.split()) < 22 and len(paragraphs) > 1:
            paragraph = paragraphs[1]
        if paragraph:
            sections.append((title, paragraph))
    if primary_code is not None:
        code_blocks = [primary_code] + [block for block in code_blocks if block != primary_code]
    return sections, code_blocks, list(dict.fromkeys(links)), code_heading


def safe_title(value: str, maximum: int = 96) -> str:
    value = clean_markdown(value)
    if len(value) <= maximum:
        return value
    return value[: maximum - 1].rsplit(" ", 1)[0] + "…"


def unique(values: list[str]) -> list[str]:
    return list(dict.fromkeys(value for value in values if value))


def diagram_label(value: str) -> str:
    """Return a compact node label that remains readable in both layouts."""
    stop_words = {
        "a", "an", "and", "are", "as", "at", "be", "by", "can", "for",
        "from", "in", "into", "is", "not", "of", "on", "or", "that",
        "the", "this", "to", "with", "without",
    }
    words = clean_markdown(value).split()
    significant = [word for word in words if word.lower() not in stop_words]
    significant = significant or words
    first_two = " ".join(significant[:2])
    first_last = " ".join((significant[0], significant[-1])) if len(significant) > 1 else significant[0]
    if len(first_two) <= 14:
        compact = first_two
    elif len(first_last) <= 14:
        compact = first_last
    else:
        compact = significant[0]
    return safe_title(compact, 14).replace("[", "(").replace("]", ")")


def visual_callout(value: str) -> str:
    """Shorten prose without leaving unbreakable identifiers in a slide."""
    def artifact_name(match: re.Match[str]) -> str:
        name = match.group(1)
        if name.startswith("codenameone-"):
            name = name.removeprefix("codenameone-")
        return "the " + name.replace("-", " ") + " artifact"

    value = clean_markdown(value)
    value = re.sub(r"(?:\bthe\s+)?\b[\w.]+:([\w.-]+)\b(?:\s+artifact)?", artifact_name, value)
    value = re.sub(r"\b[\w.-]+\.properties\b", "the project settings file", value)
    value = re.sub(r"\b\S{25,}\b", "the documented API", value)
    return safe_title(value, 76)


def narration() -> dict:
    return {
        "provider": "kokoro",
        "voice": "af_heart:60,af_bella:40",
        "language": "en-us",
        "speed": 0.97,
        "model": ".video-tools/kokoro/kokoro-v1.0.onnx",
        "minimumGapMs": 250,
        "pronunciations": PRONUNCIATIONS,
    }


def narration_duration(text: str, minimum: int = 10_000) -> int:
    """Budget natural Kokoro speech plus a short visual breathing space."""
    # This is only the pre-synthesis budget. The runner replaces it with the
    # measured clip length plus its configured tail before rendering. Leave
    # enough headroom here for acronym and class-name pronunciation expansions.
    estimate = len(text.split()) * 650 + 3_000
    return max(minimum, ((estimate + 499) // 500) * 500)


def bullets_scene(scene_id: str, title: str, items: list[str], spoken: str,
                  duration: int | None = None) -> dict:
    spoken = limit_words(spoken, 42)
    return {
        "id": scene_id,
        "durationMs": duration or narration_duration(spoken),
        "narration": {"text": spoken},
        "actions": [
            {
                "type": "bullets.show", "id": f"{scene_id}-bullets", "atMs": 100,
                "durationMs": 2_700, "title": safe_title(title, 110),
                "items": [safe_title(item, 120) for item in items[:4]],
                "bounds": {"x": 0.025, "y": 0.05, "width": 0.95, "height": 0.88},
                "orientation": {"portrait": {"x": 0.07, "y": 0.10, "width": 0.86, "height": 0.78}},
            },
            {
                "type": "transition", "target": f"{scene_id}-bullets", "atMs": 100,
                "durationMs": 650, "effects": {"landscape": "wipe-left", "portrait": "wipe-up"},
                "easing": "ease-out",
            },
        ],
    }


def diagram_scene(scene_id: str, title: str, labels: list[str], spoken: str,
                  duration: int | None = None) -> dict:
    # The JavaSE diagram renderer deliberately uses large text. Three concise
    # nodes retain that readability in landscape and give portrait nodes enough
    # vertical space; longer graphs become illegible when scaled into a video.
    labels = [diagram_label(label) for label in labels[:3]]
    while len(labels) < 3:
        labels.append(["Input", "Mechanism", "Result"][len(labels)])
    horizontal = "flowchart LR\n" + "\n".join(
        f"N{i}[{label}] --> N{i + 1}[{labels[i + 1]}]" for i, label in enumerate(labels[:-1])
    )
    vertical = horizontal.replace("flowchart LR", "flowchart TD")
    spoken = limit_words(spoken, 44)
    return {
        "id": scene_id,
        "durationMs": duration or narration_duration(spoken),
        "narration": {"text": spoken},
        "actions": [
            {"type": "text.show", "id": f"{scene_id}-title", "atMs": 0, "text": safe_title(title, 100),
             "uiid": "VideoTitle", "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.03, "y": 0.025, "width": 0.94, "height": 0.14},
             "orientation": {"portrait": {"x": 0.08, "y": 0.035, "width": 0.84, "height": 0.16}}},
            {"type": "diagram.show", "id": f"{scene_id}-diagram", "atMs": 600, "durationMs": 3_200,
             "texts": {"landscape": horizontal, "portrait": vertical},
             "bounds": {"x": 0.025, "y": 0.20, "width": 0.95, "height": 0.67},
             "orientation": {"portrait": {"x": 0.07, "y": 0.23, "width": 0.86, "height": 0.66}}},
            {"type": "transition", "target": f"{scene_id}-diagram", "atMs": 600,
             "durationMs": 700, "effect": "morph", "easing": "ease-in-out"},
        ],
    }


def code_scene(scene_id: str, heading: str, paragraph: str, language: str = "java") -> dict:
    # The code is already visibly framed as source evidence.  Narrate the
    # article's explanation of that code instead of spending the entire scene
    # announcing that a listing is on screen.
    spoken = limit_words(paragraph, 44)
    callouts = sentences(paragraph)[:2] or [paragraph]
    return {
        "id": scene_id,
        "durationMs": narration_duration(spoken),
        "narration": {"text": spoken},
        "actions": [
            {"type": "text.show", "id": f"{scene_id}-title", "atMs": 0, "text": safe_title(heading, 100),
             "uiid": "VideoTitle", "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.03, "y": 0.02, "width": 0.94, "height": 0.12},
             "orientation": {"portrait": {"x": 0.08, "y": 0.02, "width": 0.84, "height": 0.11}}},
            {"type": "code.show", "id": f"{scene_id}-code", "atMs": 300, "language": language,
             "path": "excerpt.txt", "bounds": {"x": 0.04, "y": 0.17, "width": 0.55, "height": 0.70},
             "orientation": {"portrait": {"x": 0.06, "y": 0.15, "width": 0.88, "height": 0.34}}},
            {"type": "bullets.show", "id": f"{scene_id}-notes", "atMs": 800,
             "durationMs": 900, "title": "What to notice",
             "items": [visual_callout(item) for item in callouts],
             "bounds": {"x": 0.61, "y": 0.17, "width": 0.35, "height": 0.70},
             "orientation": {"portrait": {"x": 0.07, "y": 0.54, "width": 0.86, "height": 0.34}}},
            {"type": "transition", "target": f"{scene_id}-code", "atMs": 300,
             "durationMs": 600, "effects": {"landscape": "slide-right", "portrait": "slide-up"},
             "easing": "ease-out"},
            {"type": "focus.show", "id": f"{scene_id}-focus", "target": f"{scene_id}-code",
             "atMs": 4_000, "relativeBounds": {"x": 0.02, "y": 0.05, "width": 0.96, "height": 0.34},
             "color": "50d8ff"},
        ],
    }


def write_thumbnail(project: Path, title: str, focus: str) -> None:
    headline = focus if len(clean_markdown(focus)) <= 56 else title
    line1, line2 = split_thumbnail(headline)
    image = Image.new("RGB", (1280, 720))
    pixels = image.load()
    for y in range(720):
        for x in range(1280):
            t = (x / 1279 + y / 719) / 2
            pixels[x, y] = (round(7 + 11 * t), round(20 + 38 * t), round(38 + 46 * t))
    draw = ImageDraw.Draw(image, "RGBA")
    draw.ellipse((930, -110, 1310, 270), fill=(25, 194, 255, 42))
    draw.rounded_rectangle((70, 72, 80, 642), radius=5, fill=(32, 212, 255, 255))
    regular = "/System/Library/Fonts/Supplemental/Arial.ttf"
    bold = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
    draw.text((112, 100), "CODENAME ONE", font=ImageFont.truetype(bold, 34), fill=(82, 217, 255, 255))
    headline_font = fitted_font(draw, [line1, line2], bold, 72, 48, 1060)
    draw.text((112, 238), line1, font=headline_font, fill=(255, 255, 255, 255))
    draw.text((112, 330), line2, font=headline_font, fill=(255, 255, 255, 255))
    subtitle = focus if headline == title else title
    subtitle_font = fitted_font(draw, [subtitle], regular, 29, 20, 1060)
    draw.text((112, 560), subtitle, font=subtitle_font, fill=(185, 215, 230, 255))
    image.save(project / "thumbnail.jpg", quality=91, optimize=True)


def write_ar_vr_thumbnail(project: Path) -> None:
    """Build the AR/VR thumbnail from the real simulator asset used in the video."""
    image = Image.new("RGB", (1280, 720), (6, 15, 28))
    pixels = image.load()
    for y in range(720):
        for x in range(1280):
            horizontal = x / 1279
            vertical = y / 719
            pixels[x, y] = (
                round(5 + 6 * vertical),
                round(15 + 24 * horizontal + 6 * vertical),
                round(29 + 36 * horizontal + 10 * vertical),
            )

    draw = ImageDraw.Draw(image, "RGBA")
    regular = "/System/Library/Fonts/Supplemental/Arial.ttf"
    bold = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
    label_font = ImageFont.truetype(bold, 25)
    headline_font = ImageFont.truetype(bold, 82)
    subhead_font = ImageFont.truetype(bold, 50)
    chip_font = ImageFont.truetype(bold, 27)

    # A restrained brand signature leaves the actual AR problem as the focal point.
    brand_label = "CODENAME ONE"
    label_bbox = draw.textbbox((0, 0), brand_label, font=label_font)
    label_width = label_bbox[2] - label_bbox[0]
    label_height = label_bbox[3] - label_bbox[1]
    brand_pill = (72, 58, 72 + label_width + 128, 112)
    brand_x = brand_pill[0] + (brand_pill[2] - brand_pill[0] - label_width) / 2 - label_bbox[0]
    brand_y = brand_pill[1] + (brand_pill[3] - brand_pill[1] - label_height) / 2 - label_bbox[1]
    rendered_label_bbox = draw.textbbox((brand_x, brand_y), brand_label, font=label_font)
    horizontal_clearance = min(
        rendered_label_bbox[0] - brand_pill[0],
        brand_pill[2] - rendered_label_bbox[2],
    )
    if horizontal_clearance < 60:
        raise ValueError(
            f"Brand label clearance is only {horizontal_clearance}px; expected at least 60px"
        )
    draw.rounded_rectangle(brand_pill, radius=24, fill=(15, 43, 66, 235),
                           outline=(69, 216, 255, 190), width=2)
    draw.text((brand_x, brand_y), brand_label, font=label_font, fill=(183, 235, 255, 255))
    draw.text((72, 168), "DEBUG AR", font=headline_font, fill=(255, 255, 255, 255))
    draw.text((72, 266), "AT YOUR DESK", font=subhead_font, fill=(73, 220, 255, 255))
    draw.text((75, 347), "One Java API", font=ImageFont.truetype(bold, 31),
              fill=(255, 210, 92, 255))

    chips = (("ARKit", 72, 420), ("ARCore", 236, 420), ("JavaSE", 430, 420))
    for label, x, y in chips:
        width = draw.textbbox((0, 0), label, font=chip_font)[2] + 50
        draw.rounded_rectangle((x, y, x + width, y + 58), radius=17,
                               fill=(18, 31, 50, 245), outline=(255, 201, 77, 220), width=3)
        draw.text((x + 25, y + 13), label, font=chip_font, fill=(255, 255, 255, 255))

    draw.line((76, 545, 700, 545), fill=(73, 220, 255, 135), width=3)
    draw.text((75, 570), "RUN THE HARD STATES ON DEMAND", font=ImageFont.truetype(bold, 27),
              fill=(199, 222, 235, 255))

    # Frame the authentic blog/demo capture as a desktop simulator window.
    card = (830, 40, 1208, 683)
    draw.rounded_rectangle((804, 20, 1234, 704), radius=34, fill=(46, 215, 255, 36))
    draw.rounded_rectangle(card, radius=26, fill=(14, 24, 39, 255),
                           outline=(77, 221, 255, 255), width=5)
    draw.rounded_rectangle((830, 40, 1208, 92), radius=22, fill=(27, 45, 66, 255))
    draw.rectangle((830, 70, 1208, 105), fill=(27, 45, 66, 255))
    for index, color in enumerate(((255, 95, 86, 255), (255, 193, 73, 255), (75, 214, 112, 255))):
        cx = 858 + index * 30
        draw.ellipse((cx, 58, cx + 14, 72), fill=color)
    draw.text((1005, 57), "JavaSE Simulator", font=ImageFont.truetype(bold, 20),
              fill=(220, 235, 244, 255))

    simulator = Image.open(project / "ar-simulator-model.png").convert("RGB")
    simulator = simulator.crop((0, 245, simulator.width, simulator.height))
    simulator = simulator.resize((348, 566), Image.Resampling.LANCZOS)
    image.paste(simulator, (845, 102))
    draw.rounded_rectangle((938, 116, 1182, 167), radius=16, fill=(4, 15, 28, 218),
                           outline=(73, 220, 255, 230), width=2)
    draw.text((962, 128), "LIVE ROOM STATE", font=ImageFont.truetype(bold, 21),
              fill=(255, 255, 255, 255))
    image.save(project / "thumbnail.jpg", quality=92, optimize=True)


def split_thumbnail(value: str) -> tuple[str, str]:
    words = safe_title(value, 86).split()
    midpoint = min(
        range(1, len(words)),
        key=lambda index: max(len(" ".join(words[:index])), len(" ".join(words[index:]))),
    ) if len(words) > 1 else 1
    return " ".join(words[:midpoint]), " ".join(words[midpoint:])


def fitted_font(draw: ImageDraw.ImageDraw, lines: list[str], path: str,
                maximum: int, minimum: int, width: int) -> ImageFont.FreeTypeFont:
    for size in range(maximum, minimum - 1, -2):
        font = ImageFont.truetype(path, size)
        if all(draw.textbbox((0, 0), line, font=font)[2] <= width for line in lines):
            return font
    return ImageFont.truetype(path, minimum)


def refresh_thumbnail(entry: dict, output_root: Path) -> None:
    raw = (BLOG_ROOT / f"{entry['slug']}.md").read_text(encoding="utf-8")
    meta, _ = frontmatter(raw)
    project = PROJECT_ROOT / entry["slug"]
    if entry["slug"] == "ar-vr-support-simulation":
        write_ar_vr_thumbnail(project)
    elif entry["slug"] == WIDGETS_SLUG:
        write_widgets_thumbnail(project)
    elif entry["slug"] == FIDELITY_SLUG:
        write_fidelity_thumbnail(project)
    elif entry["slug"] in STORY_PROFILES:
        capture_names = [
            f"capture-{index}{Path(relative).suffix.lower()}"
            for index, relative in enumerate(STORY_PROFILES[entry["slug"]]["captures"], 1)
        ]
        write_profiled_thumbnail(project, STORY_PROFILES[entry["slug"]], capture_names)
    else:
        write_thumbnail(project, meta["title"], entry["focus"])
    output = output_root / entry["slug"]
    output.mkdir(parents=True, exist_ok=True)
    (output / "thumbnail.jpg").write_bytes((project / "thumbnail.jpg").read_bytes())


def build_scripts(slug: str, title: str, description: str, focus: str,
                  sections: list[tuple[str, str]], code_blocks: list[str],
                  code_heading: str | None) -> tuple[dict, dict, list[tuple[int, str]]]:
    section_titles = [title for title, _ in sections]
    section_texts = [text for _, text in sections]
    while len(section_titles) < 5:
        section_titles.append(["The problem", "The mechanism", "Where it applies", "The result", "What to try next"][len(section_titles)])
        section_texts.append(description)
    hook_items = [focus, section_titles[0], section_titles[1]]
    hook_spoken = limit_words(description + " " + focus + ".", 44)
    short_hook_spoken = limit_words(description, 36)
    long_scenes = [
        {
            "id": "hook", "durationMs": narration_duration(hook_spoken, 12_000),
            "narration": {"text": hook_spoken},
            "actions": [
                {"type": "intro.show", "id": "welcome", "atMs": 0, "durationMs": 2_050,
                 "title": "Codename One", "subtitle": safe_title(title, 100),
                 "bounds": {"x": 0.04, "y": 0.08, "width": 0.92, "height": 0.80},
                 "orientation": {"portrait": {"x": 0.08, "y": 0.18, "width": 0.84, "height": 0.58}}},
                {"type": "bullets.show", "id": "hook-points", "atMs": 1_900, "durationMs": 2_600,
                 "title": safe_title(title, 110), "items": [safe_title(item, 120) for item in hook_items],
                 "bounds": {"x": 0.025, "y": 0.05, "width": 0.95, "height": 0.88},
                 "orientation": {"portrait": {"x": 0.07, "y": 0.10, "width": 0.86, "height": 0.78}}},
                {"type": "transition", "target": "hook-points", "atMs": 1_900, "durationMs": 650,
                 "effect": "wipe-up", "easing": "ease-out"},
                {"type": "layer.hide", "target": "welcome", "atMs": 2_700},
            ],
        },
        bullets_scene("problem", section_titles[0], [section_titles[1], section_titles[2], focus], section_texts[0]),
        diagram_scene("mechanism", section_titles[1], section_titles[:5], section_texts[1]),
    ]
    if code_blocks:
        source_heading = code_heading if code_heading in section_titles else section_titles[0]
        source_index = section_titles.index(source_heading)
        long_scenes.append(code_scene("source", source_heading, section_texts[source_index]))
        boundary_index = 2
        result_index = 3
    else:
        long_scenes.append(bullets_scene("source", section_titles[2], section_titles[2:5], section_texts[2]))
        boundary_index = 3
        result_index = 4
    long_scenes.extend([
        bullets_scene("tradeoffs", "Where this helps", section_titles[boundary_index:boundary_index + 3],
                      section_texts[boundary_index]),
        diagram_scene("result", "From mechanism to practical result",
                      [section_titles[0], section_titles[boundary_index], section_titles[result_index]],
                      section_texts[result_index]),
        {
            "id": "outro", "durationMs": 15_000,
            "narration": {"text": "The full source and implementation details are linked below. Which mechanism should we unpack next?"},
            "actions": [{"type": "outro.show", "id": "next", "atMs": 0, "durationMs": 1_100,
                         "eyebrow": "CODENAME ONE · TECHNICAL DEEP DIVE",
                         "title": "Choose the next detail", "subtitle": "The complete source and implementation links are below.",
                         "prompt": f"Comment: {safe_title(section_titles[1], 52)} or {safe_title(section_titles[2], 52)}?",
                         "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}}],
        },
    ])
    short_scenes = [
        {
            "id": "short-hook", "durationMs": narration_duration(short_hook_spoken, 12_000),
            "narration": {"text": short_hook_spoken},
            "actions": [
                {"type": "intro.show", "id": "short-welcome", "atMs": 0, "durationMs": 1_650,
                 "title": "Codename One", "subtitle": safe_title(title, 100),
                 "bounds": {"x": 0.08, "y": 0.18, "width": 0.84, "height": 0.58}},
                {"type": "bullets.show", "id": "short-hook-points", "atMs": 1_500, "durationMs": 2_300,
                 "title": safe_title(focus, 120), "items": [safe_title(section_titles[0], 100), safe_title(section_titles[1], 100)],
                 "bounds": {"x": 0.07, "y": 0.10, "width": 0.86, "height": 0.78}},
                {"type": "transition", "target": "short-hook-points", "atMs": 1_500,
                 "durationMs": 550, "effect": "wipe-up", "easing": "ease-out"},
                {"type": "layer.hide", "target": "short-welcome", "atMs": 2_300},
            ],
        },
        bullets_scene("short-problem", section_titles[0], [section_titles[1], section_titles[2], section_titles[3]], section_texts[0]),
        diagram_scene("short-mechanism", section_titles[1], section_titles[:4], section_texts[1]),
        bullets_scene("short-scope", "What the developer gains",
                      [section_titles[2], section_titles[3], section_titles[4]], section_texts[2]),
        {
            "id": "short-outro", "durationMs": 15_000,
            "narration": {"text": "Tap the related video for the complete explanation and source links. Then comment with the mechanism you want unpacked next."},
            "actions": [{"type": "outro.show", "id": "short-next", "atMs": 0, "durationMs": 1_000,
                         "eyebrow": "CODENAME ONE · DEEP DIVE", "title": "See the complete explanation",
                         "subtitle": "The related video carries the code, tradeoffs, and source links.",
                         "prompt": f"Comment: {safe_title(section_titles[1], 48)} or {safe_title(section_titles[2], 48)}?",
                         "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}}],
        },
    ]
    long_script = {"schemaVersion": 1, "id": slug, "title": title, "output": OUTPUT,
                   "narration": narration(), "scenes": long_scenes}
    short_script = {"schemaVersion": 1, "id": f"{slug}-short", "title": focus, "output": OUTPUT,
                    "narration": narration(), "scenes": short_scenes}
    chapter_times: list[tuple[int, str]] = []
    cursor = 0
    for scene in long_scenes:
        chapter_times.append((cursor, safe_title(scene["id"].replace("-", " ").title(), 60)))
        cursor += scene["durationMs"]
    return long_script, short_script, chapter_times


def ar_diagram_svgs() -> dict[str, str]:
    """Large, orientation-specific story diagrams that remain legible in video."""
    common = 'fill="#171c26" stroke="#ffc857" stroke-width="5" rx="24"'
    label = 'fill="#f3f7fb" font-family="sans-serif" font-weight="700" text-anchor="middle"'
    line = 'stroke="#50d8ff" stroke-width="8"'
    return {
        "interaction-landscape.svg": f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 500">
<rect x="20" y="155" width="250" height="150" {common}/><rect x="325" y="155" width="250" height="150" {common}/>
<rect x="630" y="155" width="250" height="150" {common}/><rect x="935" y="155" width="250" height="150" {common}/>
<line x1="270" y1="230" x2="325" y2="230" {line}/><line x1="575" y1="230" x2="630" y2="230" {line}/><line x1="880" y1="230" x2="935" y2="230" {line}/>
<polygon points="325,230 294,212 294,248" fill="#50d8ff"/><polygon points="630,230 599,212 599,248" fill="#50d8ff"/><polygon points="935,230 904,212 904,248" fill="#50d8ff"/>
<text x="145" y="220" font-size="43" {label}>TAP</text><text x="145" y="268" font-size="27" {label}>the floor</text>
<text x="450" y="220" font-size="40" {label}>HIT TEST</text><text x="450" y="268" font-size="27" {label}>find a plane</text>
<text x="755" y="220" font-size="43" {label}>ANCHOR</text><text x="755" y="268" font-size="27" {label}>fix a point</text>
<text x="1060" y="220" font-size="43" {label}>MODEL</text><text x="1060" y="268" font-size="27" {label}>place it</text>
</svg>''',
        "interaction-portrait.svg": f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<rect x="100" y="20" width="600" height="180" {common}/><rect x="100" y="280" width="600" height="180" {common}/>
<rect x="100" y="540" width="600" height="180" {common}/><rect x="100" y="800" width="600" height="180" {common}/>
<line x1="400" y1="200" x2="400" y2="280" {line}/><line x1="400" y1="460" x2="400" y2="540" {line}/><line x1="400" y1="720" x2="400" y2="800" {line}/>
<polygon points="400,280 380,244 420,244" fill="#50d8ff"/><polygon points="400,540 380,504 420,504" fill="#50d8ff"/><polygon points="400,800 380,764 420,764" fill="#50d8ff"/>
<text x="400" y="105" font-size="48" {label}>TAP THE FLOOR</text><text x="400" y="365" font-size="48" {label}>HIT TEST A PLANE</text>
<text x="400" y="625" font-size="48" {label}>CREATE AN ANCHOR</text><text x="400" y="885" font-size="48" {label}>PLACE THE MODEL</text>
</svg>''',
        "difficulty-landscape.svg": f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 650">
<rect x="420" y="20" width="360" height="110" {common}/>
<text x="600" y="88" font-size="42" {label}>ONE AR FEATURE</text>
<line x1="600" y1="130" x2="290" y2="260" {line}/><polygon points="290,260 310,224 326,264" fill="#50d8ff"/>
<line x1="600" y1="130" x2="910" y2="260" {line}/><polygon points="910,260 874,264 890,224" fill="#50d8ff"/>
<rect x="60" y="260" width="460" height="130" {common}/><rect x="680" y="260" width="460" height="130" {common}/>
<text x="290" y="318" font-size="38" {label}>ARKit path</text><text x="290" y="360" font-size="27" {label}>native classes + coordinates</text>
<text x="910" y="318" font-size="38" {label}>ARCore path</text><text x="910" y="360" font-size="27" {label}>different classes + coordinates</text>
<line x1="290" y1="390" x2="460" y2="500" {line}/><line x1="910" y1="390" x2="740" y2="500" {line}/>
<polygon points="460,500 421,493 443,462" fill="#50d8ff"/><polygon points="740,500 757,462 779,493" fill="#50d8ff"/>
<rect x="320" y="500" width="560" height="125" {common}/>
<text x="600" y="555" font-size="37" {label}>BACK TO THE DEVICE</text><text x="600" y="598" font-size="27" {label}>reproduce, patch, rebuild, repeat</text>
</svg>''',
        "difficulty-portrait.svg": f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<rect x="100" y="20" width="600" height="120" {common}/><text x="400" y="94" font-size="44" {label}>ONE AR FEATURE</text>
<line x1="400" y1="140" x2="205" y2="275" {line}/><line x1="400" y1="140" x2="595" y2="275" {line}/>
<polygon points="205,275 220,237 244,270" fill="#50d8ff"/><polygon points="595,275 556,270 580,237" fill="#50d8ff"/>
<rect x="20" y="275" width="370" height="185" {common}/><rect x="410" y="275" width="370" height="185" {common}/>
<text x="205" y="345" font-size="40" {label}>ARKit path</text><text x="205" y="397" font-size="27" {label}>native classes</text><text x="205" y="431" font-size="27" {label}>and coordinates</text>
<text x="595" y="345" font-size="40" {label}>ARCore path</text><text x="595" y="397" font-size="27" {label}>different classes</text><text x="595" y="431" font-size="27" {label}>and coordinates</text>
<line x1="205" y1="460" x2="300" y2="680" {line}/><line x1="595" y1="460" x2="500" y2="680" {line}/>
<polygon points="300,680 270,653 307,638" fill="#50d8ff"/><polygon points="500,680 493,638 530,653" fill="#50d8ff"/>
<rect x="70" y="680" width="660" height="220" {common}/><text x="400" y="765" font-size="42" {label}>BACK TO THE DEVICE</text>
<text x="400" y="825" font-size="30" {label}>reproduce, patch, rebuild</text><text x="400" y="865" font-size="30" {label}>then try the room again</text>
</svg>''',
        "portable-landscape.svg": f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1100 700">
<rect x="280" y="25" width="540" height="130" {common}/><text x="550" y="105" font-size="48" {label}>ONE JAVA API</text>
<line x1="550" y1="155" x2="185" y2="350" {line}/><line x1="550" y1="155" x2="550" y2="350" {line}/><line x1="550" y1="155" x2="915" y2="350" {line}/>
<polygon points="185,350 199,310 225,345" fill="#50d8ff"/><polygon points="550,350 530,314 570,314" fill="#50d8ff"/><polygon points="915,350 875,345 901,310" fill="#50d8ff"/>
<rect x="25" y="350" width="320" height="180" {common}/><rect x="390" y="350" width="320" height="180" {common}/><rect x="755" y="350" width="320" height="180" {common}/>
<text x="185" y="430" font-size="42" {label}>ARKit</text><text x="185" y="480" font-size="27" {label}>native iOS</text>
<text x="550" y="430" font-size="42" {label}>ARCore</text><text x="550" y="480" font-size="27" {label}>native Android</text>
<text x="915" y="420" font-size="35" {label}>JavaSE</text><text x="915" y="465" font-size="35" {label}>simulator</text>
<text x="550" y="630" font-size="34" fill="#50d8ff" font-family="sans-serif" font-weight="700" text-anchor="middle">WRITE ONCE · DEBUG THE SAME FLOW</text>
</svg>''',
        "portable-portrait.svg": f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<rect x="80" y="20" width="640" height="130" {common}/><text x="400" y="103" font-size="48" {label}>ONE JAVA API</text>
<line x1="400" y1="150" x2="400" y2="260" {line}/><line x1="400" y1="150" x2="400" y2="500" {line}/><line x1="400" y1="150" x2="400" y2="740" {line}/>
<rect x="100" y="260" width="600" height="150" {common}/><rect x="100" y="500" width="600" height="150" {common}/><rect x="100" y="740" width="600" height="150" {common}/>
<text x="400" y="330" font-size="44" {label}>ARKit</text><text x="400" y="375" font-size="27" {label}>native iOS</text>
<text x="400" y="570" font-size="44" {label}>ARCore</text><text x="400" y="615" font-size="27" {label}>native Android</text>
<text x="400" y="810" font-size="40" {label}>JavaSE simulator</text><text x="400" y="855" font-size="27" {label}>same flow at your desk</text>
<polygon points="400,260 380,224 420,224" fill="#50d8ff"/><polygon points="400,500 380,464 420,464" fill="#50d8ff"/><polygon points="400,740 380,704 420,704" fill="#50d8ff"/>
<text x="400" y="965" font-size="31" fill="#50d8ff" font-family="sans-serif" font-weight="700" text-anchor="middle">WRITE ONCE · DEBUG THE SAME FLOW</text>
</svg>''',
    }


def ar_vr_scripts(title: str) -> tuple[dict, dict, list[tuple[int, str]]]:
    """Hand-authored hero journey: context, struggle, portable intervention, proof, victory."""
    def cue(text: str, at: int = 650, cue_id: str = "voice", caption: str | None = None) -> dict:
        action = {"type": "narration.cue", "id": cue_id, "atMs": at, "text": text}
        if caption is not None:
            action["caption"] = caption
        return action

    identity_scene = {
        "id": "place-it-in-the-room", "durationMs": 14_500,
        "storyBeats": ["desire", "identity"],
        "actions": [
            {"type": "image.show", "id": "opening-demo", "role": "demo", "kind": "capture",
             "atMs": 0, "path": "ar-simulator-model.png",
             "bounds": {"x": 0.65, "y": 0.08, "width": 0.25, "height": 0.84},
             "orientation": {"portrait": {"x": 0.18, "y": 0.26, "width": 0.64, "height": 0.58}}},
            {"type": "layer.animate", "target": "opening-demo", "atMs": 800,
             "durationMs": 5_500, "fromX": -0.015, "toX": 0.015,
             "fromScale": 1.0, "toScale": 1.08, "easing": "ease-in-out"},
            {"type": "text.show", "id": "opening-title", "atMs": 150,
             "text": "Tap the floor. Place the model.", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 3,
             "bounds": {"x": 0.05, "y": 0.19, "width": 0.50, "height": 0.30},
             "orientation": {"portrait": {"x": 0.07, "y": 0.06, "width": 0.86, "height": 0.15}}},
            {"type": "text.show", "id": "opening-copy", "atMs": 650,
             "text": "The object stays in the room as you move.", "uiid": "VideoBody",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.07, "y": 0.54, "width": 0.44, "height": 0.14},
             "orientation": {"portrait": {"x": 0.12, "y": 0.86, "width": 0.76, "height": 0.07}}},
            {"type": "image.show", "id": "brand-mark", "role": "brand", "kind": "illustration",
             "atMs": 400, "path": "codename-one-logo.png",
             "bounds": {"x": 0.04, "y": 0.04, "width": 0.09, "height": 0.035},
             "orientation": {"portrait": {"x": 0.06, "y": 0.02, "width": 0.15, "height": 0.022}}},
            {"type": "text.show", "id": "brand-promise", "role": "brand", "atMs": 700,
             "text": "Native apps in Java. A UI you control.", "uiid": "VideoBody",
             "bounds": {"x": 0.04, "y": 0.91, "width": 0.38, "height": 0.05},
             "orientation": {"portrait": {"x": 0.06, "y": 0.945, "width": 0.75, "height": 0.03}}},
            {"type": "pointer.show", "id": "opening-touch", "atMs": 1_100, "style": "touch",
             "area": "opening-demo", "x": 0.50, "y": 0.72},
            {"type": "pointer.click", "target": "opening-touch", "atMs": 2_300, "durationMs": 320},
            cue("Your user taps the floor and a 3D model stays in the room as they move. Codename One makes that native augmented reality interaction in Java, with a user interface you control."),
        ],
    }

    problem_scene = {
        "id": "the-simple-interaction", "durationMs": 12_000, "storyBeats": ["problem"],
        "actions": [
            {"type": "text.show", "id": "problem-title", "atMs": 0,
             "text": "The feature sounds simple", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.05, "y": 0.03, "width": 0.90, "height": 0.16},
             "orientation": {"portrait": {"x": 0.08, "y": 0.04, "width": 0.84, "height": 0.16}}},
            {"type": "svg.show", "id": "interaction-flow", "atMs": 500,
             "paths": {"landscape": "interaction-landscape.svg", "portrait": "interaction-portrait.svg"},
             "bounds": {"x": 0.04, "y": 0.22, "width": 0.92, "height": 0.64},
             "orientation": {"portrait": {"x": 0.09, "y": 0.20, "width": 0.82, "height": 0.72}}},
            {"type": "transition", "target": "interaction-flow", "atMs": 500,
             "durationMs": 850, "effect": "morph", "easing": "ease-out"},
            cue("From the product side, this is one gesture: take the tap, find a plane, create an anchor, and attach the model. That is the whole feature the user sees."),
        ],
    }

    difficulty_scene = {
        "id": "two-platform-loops", "durationMs": 13_000, "storyBeats": ["difficulty"],
        "actions": [
            {"type": "text.show", "id": "difficulty-title", "atMs": 0,
             "text": "The same interaction splits by platform", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.03, "width": 0.92, "height": 0.16},
             "orientation": {"portrait": {"x": 0.08, "y": 0.04, "width": 0.84, "height": 0.16}}},
            {"type": "svg.show", "id": "platform-maze", "atMs": 500,
             "paths": {"landscape": "difficulty-landscape.svg",
                       "portrait": "difficulty-portrait.svg"},
             "bounds": {"x": 0.04, "y": 0.22, "width": 0.92, "height": 0.68},
             "orientation": {"portrait": {"x": 0.07, "y": 0.22, "width": 0.86, "height": 0.70}}},
            {"type": "transition", "target": "platform-maze", "atMs": 500,
             "durationMs": 800, "effect": "morph", "easing": "ease-in-out"},
            cue("Now the simple interaction becomes two implementations. ARKit and ARCore use different classes and coordinate rules, and each change goes back through a device build."),
        ],
    }

    failure_scene = {
        "id": "the-room-fights-back", "durationMs": 14_000, "storyBeats": ["difficulty"],
        "actions": [
            {"type": "text.show", "id": "failure-title", "atMs": 0,
             "text": "Then the room changes underneath the code", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.03, "width": 0.92, "height": 0.16},
             "orientation": {"portrait": {"x": 0.07, "y": 0.04, "width": 0.86, "height": 0.15}}},
            {"type": "image.show", "id": "failure-demo", "kind": "capture", "atMs": 250,
             "path": "ar-simulator-model.png",
             "bounds": {"x": 0.57, "y": 0.20, "width": 0.29, "height": 0.72},
             "orientation": {"portrait": {"x": 0.21, "y": 0.24, "width": 0.58, "height": 0.51}}},
            {"type": "layer.animate", "target": "failure-demo", "atMs": 800,
             "durationMs": 7_000, "fromX": -0.015, "toX": 0.02,
             "fromScale": 1.0, "toScale": 1.10, "easing": "ease-in-out"},
            {"type": "text.show", "id": "failure-context", "atMs": 700,
             "text": "The bug may need one room, one angle, and one moment.", "uiid": "VideoBody",
             "responsive": True, "maxLines": 3,
             "bounds": {"x": 0.06, "y": 0.23, "width": 0.42, "height": 0.20},
             "orientation": {"portrait": {"x": 0.08, "y": 0.77, "width": 0.84, "height": 0.10}}},
            {"type": "text.show", "id": "tracking-state", "atMs": 2_700,
             "text": "TRACKING DEGRADED", "uiid": "VideoBody", "color": "ffc857",
             "bounds": {"x": 0.07, "y": 0.55, "width": 0.34, "height": 0.07},
             "orientation": {"portrait": {"x": 0.12, "y": 0.87, "width": 0.76, "height": 0.04}}},
            {"type": "layer.hide", "target": "tracking-state", "atMs": 5_200},
            {"type": "text.show", "id": "plane-state", "atMs": 5_200,
             "text": "PLANES MERGED", "uiid": "VideoBody", "color": "50d8ff",
             "bounds": {"x": 0.07, "y": 0.55, "width": 0.34, "height": 0.07},
             "orientation": {"portrait": {"x": 0.12, "y": 0.87, "width": 0.76, "height": 0.04}}},
            {"type": "layer.hide", "target": "plane-state", "atMs": 7_700},
            {"type": "text.show", "id": "image-state", "atMs": 7_700,
             "text": "REFERENCE IMAGE FOUND", "uiid": "VideoBody", "color": "78e08f",
             "bounds": {"x": 0.07, "y": 0.55, "width": 0.40, "height": 0.07},
             "orientation": {"portrait": {"x": 0.12, "y": 0.87, "width": 0.76, "height": 0.04}}},
            cue("The expensive bugs live in changing world state: tracking degrades, planes merge, or a reference image enters the frame. They appear on a device, then disappear before a breakpoint can explain them."),
        ],
    }

    intervention_scene = {
        "id": "portable-intervention", "durationMs": 15_000, "storyBeats": ["intervention"],
        "actions": [
            {"type": "text.show", "id": "intervention-title", "atMs": 0,
             "text": "One Java API. iOS, Android, simulator.", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.03, "width": 0.92, "height": 0.16},
             "orientation": {"portrait": {"x": 0.08, "y": 0.03, "width": 0.84, "height": 0.14}}},
            {"type": "svg.show", "id": "portable-api", "atMs": 450,
             "paths": {"landscape": "portable-landscape.svg",
                       "portrait": "portable-portrait.svg"},
             "bounds": {"x": 0.03, "y": 0.21, "width": 0.58, "height": 0.70},
             "orientation": {"portrait": {"x": 0.07, "y": 0.18, "width": 0.86, "height": 0.43}}},
            {"type": "image.show", "id": "portable-capture", "kind": "capture", "atMs": 900,
             "path": "ar-simulator-model.png",
             "bounds": {"x": 0.67, "y": 0.20, "width": 0.25, "height": 0.72},
             "orientation": {"portrait": {"x": 0.25, "y": 0.63, "width": 0.50, "height": 0.34}}},
            {"type": "transition", "target": "portable-capture", "atMs": 900,
             "durationMs": 750, "effects": {"landscape": "slide-right", "portrait": "slide-up"},
             "easing": "ease-out"},
            cue("Codename One puts one Java API in front of ARKit, ARCore, and the JavaSE simulator. You write the interaction once, then debug the same flow at your desk."),
        ],
    }

    proof_scene = {
        "id": "tap-to-anchor", "durationMs": 19_000, "storyBeats": ["proof"],
        "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"},
        "actions": [
            {"type": "text.show", "id": "code-title", "role": "title", "atMs": 0,
             "text": "Write the interaction once", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2},
            {"type": "code.show", "id": "placement-code", "role": "code", "atMs": 250,
             "language": "java", "path": "excerpt.txt"},
            {"type": "transition", "target": "placement-code", "atMs": 250,
             "durationMs": 650, "effects": {"landscape": "wipe-left", "portrait": "slide-up"},
             "easing": "ease-out"},
            {"type": "image.show", "id": "placement-demo", "role": "demo", "kind": "capture",
             "path": "ar-simulator-model.png", "atMs": 450},
            {"type": "transition", "target": "placement-demo", "atMs": 450,
             "durationMs": 700, "effects": {"landscape": "slide-right", "portrait": "slide-up"},
             "easing": "ease-out"},
            cue("The same Java handler receives the tap on iOS, Android, and JavaSE. It normalizes the coordinates, hit-tests a plane, creates an anchor, and attaches the model."),
            {"type": "focus.show", "id": "hit-test-focus", "target": "placement-code",
             "atMs": 3_600, "relativeBounds": {"x": 0.02, "y": 0.39, "width": 0.96, "height": 0.58},
             "label": "One handler on iOS, Android, and JavaSE", "color": "50d8ff"},
            {"type": "pointer.show", "id": "tap", "atMs": 4_000, "style": "touch",
             "area": "placement-demo", "x": 0.50, "y": 0.72},
            {"type": "pointer.click", "target": "tap", "atMs": 5_800, "durationMs": 320},
            {"type": "layer.animate", "target": "placement-demo", "atMs": 4_100,
             "durationMs": 2_200, "fromY": 0.015, "toY": -0.015,
             "fromScale": 1.0, "toScale": 1.08, "easing": "ease-out"},
            {"type": "text.show", "id": "anchor-state", "atMs": 5_900,
             "text": "ANCHOR CREATED", "uiid": "VideoBody", "color": "78e08f",
             "bounds": {"x": 0.72, "y": 0.82, "width": 0.22, "height": 0.06},
             "orientation": {"portrait": {"x": 0.28, "y": 0.89, "width": 0.44, "height": 0.035}}},
            {"type": "layer.hide", "target": "anchor-state", "atMs": 7_500},
            {"type": "replay", "atMs": 9_000, "fromMs": 3_500, "toMs": 6_300,
             "rewindDurationMs": 550, "rewindFps": 8, "playbackRate": 0.62,
             "label": "Again: one handler -> hit test -> anchor"},
        ],
    }

    simulator_victory = {
        "id": "debug-loop-restored", "durationMs": 14_000, "storyBeats": ["victory"],
        "actions": [
            {"type": "image.show", "id": "simulator-capture", "kind": "capture", "atMs": 0,
             "path": "ar-simulator-model.png",
             "bounds": {"x": 0.57, "y": 0.07, "width": 0.31, "height": 0.84},
             "orientation": {"portrait": {"x": 0.18, "y": 0.31, "width": 0.64, "height": 0.58}}},
            {"type": "text.show", "id": "victory-title", "atMs": 150,
             "text": "The developer gets the debug loop back", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 3,
             "bounds": {"x": 0.05, "y": 0.12, "width": 0.44, "height": 0.24},
             "orientation": {"portrait": {"x": 0.08, "y": 0.04, "width": 0.84, "height": 0.18}}},
            {"type": "text.show", "id": "victory-copy", "atMs": 1_000,
             "text": "Force tracking loss\nRe-run plane detection\nBreak in the same Java process",
             "uiid": "VideoBody", "responsive": True, "maxLines": 5,
             "bounds": {"x": 0.07, "y": 0.45, "width": 0.39, "height": 0.34},
             "orientation": {"portrait": {"x": 0.10, "y": 0.20, "width": 0.80, "height": 0.13}}},
            {"type": "layer.animate", "target": "simulator-capture", "atMs": 1_400,
             "durationMs": 4_000, "fromX": 0.012, "toX": -0.012,
             "fromScale": 1.02, "toScale": 1.10, "easing": "ease-in-out"},
            {"type": "pointer.show", "id": "simulator-look", "atMs": 2_000, "style": "mouse",
             "area": "simulator-capture", "x": 0.40, "y": 0.48},
            {"type": "pointer.move", "target": "simulator-look", "area": "simulator-capture",
             "atMs": 2_100, "durationMs": 1_600, "x": 0.65, "y": 0.42},
            cue("The simulator turns room state into test input. Force tracking loss, re-run plane detection, trigger a reference image, and stop on a breakpoint in the same Java process."),
        ],
    }

    vr_victory = {
        "id": "portable-vr", "durationMs": 15_500, "storyBeats": ["victory"],
        "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"},
        "actions": [
            {"type": "text.show", "id": "pano-title", "role": "title", "atMs": 0,
             "text": "The same Java project reaches virtual reality", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2},
            {"type": "code.show", "id": "pano-code", "role": "code", "atMs": 200,
             "language": "java", "path": "media360.txt"},
            {"type": "image.show", "id": "pano-demo", "role": "demo", "kind": "capture",
             "path": "vr-360-panorama.png", "atMs": 250},
            cue("Virtual reality keeps the same portability. Media360View is core code on the GPU pipeline. Drag in the simulator or use motion sensors on the device with the same Java component."),
            {"type": "pointer.show", "id": "drag", "atMs": 4_400, "style": "touch",
             "area": "pano-demo", "x": 0.36, "y": 0.48},
            {"type": "pointer.move", "target": "drag", "area": "pano-demo", "atMs": 4_500,
             "durationMs": 1_100, "x": 0.66, "y": 0.42},
            {"type": "layer.animate", "target": "pano-demo", "atMs": 4_400,
             "durationMs": 2_400, "fromX": -0.035, "toX": 0.035,
             "fromScale": 1.05, "toScale": 1.12, "easing": "ease-in-out"},
            {"type": "layer.animate", "target": "pano-demo", "atMs": 8_400,
             "durationMs": 2_600, "toX": -0.025, "toScale": 1.06, "easing": "ease-in-out"},
            {"type": "focus.show", "id": "pano-focus", "target": "pano-code", "atMs": 7_000,
             "relativeBounds": {"x": 0.02, "y": 0.02, "width": 0.96, "height": 0.82},
             "label": "One component in the shared Java source", "color": "50d8ff"},
        ],
    }

    outro_scene = {
        "id": "outro", "durationMs": 15_000, "storyBeats": ["outro"],
        "actions": [
            {"type": "outro.show", "id": "next", "atMs": 0, "durationMs": 1_100,
             "eyebrow": "CODENAME ONE · AUGMENTED AND VIRTUAL REALITY",
             "title": "One codebase. Native AR. A debug loop you control.",
             "subtitle": "ARKit + ARCore + JavaSE simulator + portable VR",
             "prompt": "Where does your AR debugging loop waste the most time?",
             "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}},
            cue("One Java codebase now reaches native augmented reality on iOS and Android, a simulator you can inspect, and portable virtual reality. Where does your augmented reality debugging loop waste the most time?"),
        ],
    }

    long_scenes = [
        identity_scene, problem_scene, difficulty_scene, failure_scene, intervention_scene,
        proof_scene, simulator_victory, vr_victory, outro_scene,
    ]

    short_difficulty_scene = {
        "id": "short-difficulty", "durationMs": 17_000,
        "storyBeats": ["difficulty"],
        "actions": [
            {"type": "text.show", "id": "short-problem-title", "atMs": 0,
             "text": "One gesture meets two SDKs and a moving room", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 3,
             "bounds": {"x": 0.06, "y": 0.03, "width": 0.88, "height": 0.16}},
            {"type": "svg.show", "id": "short-platform-maze", "atMs": 350,
             "path": "difficulty-portrait.svg",
             "bounds": {"x": 0.08, "y": 0.20, "width": 0.84, "height": 0.43}},
            {"type": "image.show", "id": "short-room", "kind": "capture", "atMs": 650,
             "path": "ar-simulator-model.png",
             "bounds": {"x": 0.28, "y": 0.66, "width": 0.44, "height": 0.29}},
            {"type": "layer.animate", "target": "short-room", "atMs": 1_200,
             "durationMs": 6_000, "fromX": -0.02, "toX": 0.02,
             "fromScale": 1.0, "toScale": 1.10, "easing": "ease-in-out"},
            cue("The same gesture splits across ARKit and ARCore, then the physical room adds tracking loss, changing planes, and states that vanish before inspection."),
        ],
    }

    short_scenes = [
        {**identity_scene},
        {**problem_scene, "id": "short-simple-interaction"},
        short_difficulty_scene,
        {**intervention_scene, "id": "short-intervention"},
        {**proof_scene, "id": "short-proof"},
        {**simulator_victory, "id": "short-victory"},
        {
            "id": "short-outro", "durationMs": 15_000, "storyBeats": ["outro"],
            "actions": [
                {"type": "outro.show", "id": "short-next", "atMs": 0, "durationMs": 1_000,
                 "eyebrow": "CODENAME ONE · AUGMENTED REALITY",
                 "title": "Write once. Debug at your desk.",
                 "subtitle": "Native ARKit and ARCore through one Java API",
                 "prompt": "See the code and virtual reality path in the related video.",
                 "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}},
                cue("The related video shows the complete code, the simulator loop, and the portable virtual reality path."),
            ],
        },
    ]
    editorial = {"storyType": "code-and-capture", "status": "approved",
                 "proof": ["actual JavaSE simulator capture", "source code", "actual Media360View capture"],
                 "humanBeat": "A simple tap-to-place feature becomes two SDK implementations, then fails only in changing physical-room states.",
                 "visualIdentity": "The placed model remains visible while the story expands from one user gesture into platform and room state, then contracts back into one Java flow.",
                 "bespokeVisualization": "Tap, hit test, anchor, and model first form one interaction; the same line then forks into ARKit and ARCore before converging on the JavaSE simulator.",
                 "heroJourney": {
                     "desire": "A user taps a floor and a 3D model remains anchored in the room as the camera moves.",
                     "problem": "The developer must turn one visible gesture into hit testing, an anchor, and a placed model.",
                     "difficulty": "The interaction splits across ARKit and ARCore while physical tracking states resist reproduction.",
                     "intervention": "The developer uses the Codename One AR API and JavaSE simulator as one portable loop.",
                     "victory": "One Java handler reaches iOS and Android while changing room states become reproducible simulator input."
                 }}
    long_script = {"schemaVersion": 1, "id": "ar-vr-support-simulation", "title": title,
                   "editorial": editorial, "output": OUTPUT, "narration": narration(), "scenes": long_scenes}
    short_script = {"schemaVersion": 1, "id": "ar-vr-support-simulation-short",
                    "title": "Debug augmented reality without walking around the room",
                    "editorial": editorial, "output": OUTPUT, "narration": narration(), "scenes": short_scenes}
    return long_script, short_script, chapter_times(long_script)


def ar_story_svgs_v2() -> dict[str, str]:
    """Three-act AR story art, including attributed public evidence cards."""
    result = {
        filename: svg
        for filename, svg in ar_diagram_svgs().items()
        if filename.startswith("portable-")
    }
    result.update({
        "ar-intro-landscape.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 650">
<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#50d8ff"/><stop offset="1" stop-color="#8b5cf6"/></linearGradient></defs>
<path d="M130 500 C310 220 850 180 1070 465" fill="none" stroke="#263449" stroke-width="3"/>
<ellipse cx="600" cy="470" rx="410" ry="105" fill="#111827" stroke="#50d8ff" stroke-width="5"/>
<path d="M290 470 L430 340 L600 405 L770 270 L920 470" fill="none" stroke="url(#g)" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
<circle cx="430" cy="340" r="24" fill="#ffc857"/><circle cx="600" cy="405" r="24" fill="#50d8ff"/><circle cx="770" cy="270" r="24" fill="#8b5cf6"/>
<rect x="485" y="95" width="230" height="135" rx="28" fill="#171c26" stroke="#f3f7fb" stroke-width="4"/>
<circle cx="600" cy="162" r="35" fill="none" stroke="#ffc857" stroke-width="6"/><path d="M600 125 V199 M563 162 H637" stroke="#ffc857" stroke-width="5"/>
<text x="600" y="625" fill="#cbd5e1" font-family="sans-serif" font-size="30" text-anchor="middle">digital objects · physical space · one application</text>
</svg>''',
        "ar-intro-portrait.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#50d8ff"/><stop offset="1" stop-color="#8b5cf6"/></linearGradient></defs>
<ellipse cx="400" cy="720" rx="325" ry="120" fill="#111827" stroke="#50d8ff" stroke-width="6"/>
<path d="M105 720 L250 530 L400 625 L550 390 L695 720" fill="none" stroke="url(#g)" stroke-width="14" stroke-linecap="round" stroke-linejoin="round"/>
<circle cx="250" cy="530" r="27" fill="#ffc857"/><circle cx="400" cy="625" r="27" fill="#50d8ff"/><circle cx="550" cy="390" r="27" fill="#8b5cf6"/>
<rect x="270" y="95" width="260" height="170" rx="30" fill="#171c26" stroke="#f3f7fb" stroke-width="5"/>
<circle cx="400" cy="180" r="42" fill="none" stroke="#ffc857" stroke-width="7"/><path d="M400 134 V226 M354 180 H446" stroke="#ffc857" stroke-width="6"/>
<text x="400" y="930" fill="#cbd5e1" font-family="sans-serif" font-size="31" text-anchor="middle">digital objects in physical space</text>
</svg>''',
        "industry-fragmentation-landscape.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 650">
<style>.h{font:700 38px sans-serif;fill:#f3f7fb}.b{font:28px sans-serif;fill:#cbd5e1}.box{fill:#171c26;stroke-width:4;rx:22}</style>
<text x="280" y="60" class="h" text-anchor="middle">iOS path</text><text x="920" y="60" class="h" text-anchor="middle">Android path</text>
<rect x="60" y="95" width="440" height="470" class="box" stroke="#ffc857"/><rect x="700" y="95" width="440" height="470" class="box" stroke="#50d8ff"/>
<text x="105" y="160" class="h">API</text><text x="105" y="204" class="b">ARKit</text><text x="105" y="280" class="h">Language</text><text x="105" y="324" class="b">Swift / Objective-C</text><text x="105" y="400" class="h">Tooling</text><text x="105" y="444" class="b">Xcode + iOS device</text><text x="105" y="520" class="h">Debug loop</text><text x="105" y="554" class="b">camera · sensors · room</text>
<text x="745" y="160" class="h">API</text><text x="745" y="204" class="b">ARCore</text><text x="745" y="280" class="h">Language</text><text x="745" y="324" class="b">Kotlin / Java / C</text><text x="745" y="400" class="h">Tooling</text><text x="745" y="444" class="b">Android Studio + device</text><text x="745" y="520" class="h">Debug loop</text><text x="745" y="554" class="b">camera · sensors · room</text>
<path d="M575 110 V570" stroke="#ef4444" stroke-width="5" stroke-dasharray="12 12"/><text x="600" y="625" class="b" text-anchor="middle">one feature · two development environments</text>
</svg>''',
        "industry-fragmentation-portrait.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<style>.h{font:700 35px sans-serif;fill:#f3f7fb}.b{font:26px sans-serif;fill:#cbd5e1}.box{fill:#171c26;stroke-width:4;rx:22}</style>
<rect x="55" y="55" width="690" height="390" class="box" stroke="#ffc857"/><text x="95" y="115" class="h">iOS path · ARKit</text><text x="95" y="180" class="b">Swift / Objective-C</text><text x="95" y="230" class="b">Xcode + iOS device</text><text x="95" y="280" class="b">camera + sensors + physical room</text><text x="95" y="350" class="h">its own debug loop</text>
<path d="M90 500 H710" stroke="#ef4444" stroke-width="5" stroke-dasharray="12 12"/>
<rect x="55" y="555" width="690" height="390" class="box" stroke="#50d8ff"/><text x="95" y="615" class="h">Android path · ARCore</text><text x="95" y="680" class="b">Kotlin / Java / C</text><text x="95" y="730" class="b">Android Studio + Android device</text><text x="95" y="780" class="b">camera + sensors + physical room</text><text x="95" y="850" class="h">another debug loop</text>
</svg>''',
        "evidence-stackoverflow-landscape.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 650">
<rect width="1200" height="650" rx="28" fill="#f8f9f9"/><rect width="1200" height="82" rx="28" fill="#242729"/><circle cx="55" cy="41" r="19" fill="#f48024"/><text x="92" y="54" font-family="sans-serif" font-size="31" font-weight="700" fill="white">Stack Overflow</text>
<text x="75" y="155" font-family="sans-serif" font-size="26" fill="#6a737c">Developer question</text><text x="75" y="215" font-family="sans-serif" font-size="42" font-weight="700" fill="#232629">Can ARKit run in the simulator?</text>
<text x="75" y="315" font-family="sans-serif" font-size="34" fill="#3b4045">“ARKit uses the camera as well as gyroscope and accelerometer.</text><text x="75" y="365" font-family="sans-serif" font-size="34" fill="#3b4045">So, you won't be able to use it in the simulator.”</text>
<rect x="75" y="445" width="1050" height="2" fill="#d6d9dc"/><text x="75" y="505" font-family="sans-serif" font-size="27" fill="#6a737c">Accepted answer · independent community evidence</text><text x="75" y="575" font-family="monospace" font-size="22" fill="#0074cc">stackoverflow.com/questions/45034240</text>
</svg>''',
        "evidence-stackoverflow-portrait.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<rect width="800" height="1000" rx="30" fill="#f8f9f9"/><rect width="800" height="105" rx="30" fill="#242729"/><circle cx="58" cy="52" r="22" fill="#f48024"/><text x="100" y="67" font-family="sans-serif" font-size="34" font-weight="700" fill="white">Stack Overflow</text>
<text x="65" y="185" font-family="sans-serif" font-size="27" fill="#6a737c">Developer question</text><text x="65" y="255" font-family="sans-serif" font-size="43" font-weight="700" fill="#232629">Can ARKit run</text><text x="65" y="310" font-family="sans-serif" font-size="43" font-weight="700" fill="#232629">in the simulator?</text>
<text x="65" y="445" font-family="sans-serif" font-size="35" fill="#3b4045">“ARKit uses the camera</text><text x="65" y="495" font-family="sans-serif" font-size="35" fill="#3b4045">as well as gyroscope</text><text x="65" y="545" font-family="sans-serif" font-size="35" fill="#3b4045">and accelerometer.</text><text x="65" y="620" font-family="sans-serif" font-size="35" fill="#3b4045">So, you won't be able</text><text x="65" y="670" font-family="sans-serif" font-size="35" fill="#3b4045">to use it in the simulator.”</text>
<text x="65" y="810" font-family="sans-serif" font-size="27" fill="#6a737c">Accepted answer · community evidence</text><text x="65" y="900" font-family="monospace" font-size="21" fill="#0074cc">stackoverflow.com/questions/45034240</text>
</svg>''',
        "evidence-reddit-landscape.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 650">
<rect width="1200" height="650" rx="28" fill="#0f1115"/><rect width="1200" height="82" rx="28" fill="#1a1a1b"/><circle cx="55" cy="41" r="20" fill="#ff4500"/><text x="92" y="54" font-family="sans-serif" font-size="31" font-weight="700" fill="white">r/augmentedreality</text>
<text x="75" y="155" font-family="sans-serif" font-size="26" fill="#818384">AR Foundation debugging</text><text x="75" y="235" font-family="sans-serif" font-size="39" font-weight="700" fill="#d7dadc">“Debugging is virtually impossible, to say the least.”</text>
<text x="75" y="335" font-family="sans-serif" font-size="31" fill="#d7dadc">Test ARCore on Android. Test ARKit on iOS.</text><text x="75" y="385" font-family="sans-serif" font-size="31" fill="#d7dadc">The physical device stays inside the development loop.</text>
<rect x="75" y="465" width="1050" height="2" fill="#343536"/><text x="75" y="525" font-family="sans-serif" font-size="27" fill="#818384">Independent practitioner discussion</text><text x="75" y="585" font-family="monospace" font-size="21" fill="#4fbcff">reddit.com/r/augmentedreality/comments/by4v4p/</text>
</svg>''',
        "evidence-reddit-portrait.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<rect width="800" height="1000" rx="30" fill="#0f1115"/><rect width="800" height="105" rx="30" fill="#1a1a1b"/><circle cx="58" cy="52" r="22" fill="#ff4500"/><text x="100" y="67" font-family="sans-serif" font-size="32" font-weight="700" fill="white">r/augmentedreality</text>
<text x="65" y="185" font-family="sans-serif" font-size="28" fill="#818384">AR Foundation debugging</text><text x="65" y="300" font-family="sans-serif" font-size="43" font-weight="700" fill="#d7dadc">“Debugging is</text><text x="65" y="360" font-family="sans-serif" font-size="43" font-weight="700" fill="#d7dadc">virtually impossible,</text><text x="65" y="420" font-family="sans-serif" font-size="43" font-weight="700" fill="#d7dadc">to say the least.”</text>
<text x="65" y="565" font-family="sans-serif" font-size="32" fill="#d7dadc">Test ARCore on Android.</text><text x="65" y="615" font-family="sans-serif" font-size="32" fill="#d7dadc">Test ARKit on iOS.</text><text x="65" y="690" font-family="sans-serif" font-size="32" fill="#d7dadc">The device stays inside</text><text x="65" y="740" font-family="sans-serif" font-size="32" fill="#d7dadc">the development loop.</text>
<text x="65" y="875" font-family="sans-serif" font-size="26" fill="#818384">Independent practitioner discussion</text><text x="65" y="935" font-family="monospace" font-size="19" fill="#4fbcff">reddit.com/r/augmentedreality/</text>
</svg>''',
        "evidence-arcore-landscape.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 650">
<rect width="1200" height="650" rx="28" fill="#ffffff"/><rect width="1200" height="82" rx="28" fill="#f1f3f4"/><text x="65" y="54" font-family="sans-serif" font-size="31" font-weight="700" fill="#202124">Google for Developers · ARCore emulator</text>
<text x="75" y="155" font-family="sans-serif" font-size="38" font-weight="700" fill="#202124">Simulation is its own environment to configure</text>
<rect x="75" y="225" width="235" height="120" rx="18" fill="#e8f0fe"/><rect x="345" y="225" width="235" height="120" rx="18" fill="#e6f4ea"/><rect x="615" y="225" width="235" height="120" rx="18" fill="#fef7e0"/><rect x="885" y="225" width="235" height="120" rx="18" fill="#fce8e6"/>
<text x="193" y="280" font-family="sans-serif" font-size="28" text-anchor="middle" fill="#1967d2">API 27+</text><text x="193" y="320" font-family="sans-serif" font-size="23" text-anchor="middle" fill="#3c4043">x86 / x86_64</text><text x="463" y="280" font-family="sans-serif" font-size="28" text-anchor="middle" fill="#137333">VirtualScene</text><text x="463" y="320" font-family="sans-serif" font-size="23" text-anchor="middle" fill="#3c4043">camera setting</text><text x="733" y="280" font-family="sans-serif" font-size="28" text-anchor="middle" fill="#b06000">AR services</text><text x="733" y="320" font-family="sans-serif" font-size="23" text-anchor="middle" fill="#3c4043">install / update</text><text x="1003" y="280" font-family="sans-serif" font-size="28" text-anchor="middle" fill="#c5221f">ABI match</text><text x="1003" y="320" font-family="sans-serif" font-size="23" text-anchor="middle" fill="#3c4043">app + emulator</text>
<text x="75" y="445" font-family="sans-serif" font-size="31" fill="#3c4043">Even before app code runs, the simulated camera and AR runtime must agree.</text><text x="75" y="560" font-family="monospace" font-size="22" fill="#1967d2">developers.google.com/ar/develop/java/emulator</text>
</svg>''',
        "evidence-arcore-portrait.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<rect width="800" height="1000" rx="30" fill="#ffffff"/><rect width="800" height="105" rx="30" fill="#f1f3f4"/><text x="55" y="67" font-family="sans-serif" font-size="29" font-weight="700" fill="#202124">Google Developers · ARCore</text>
<text x="55" y="180" font-family="sans-serif" font-size="40" font-weight="700" fill="#202124">Simulation needs its</text><text x="55" y="230" font-family="sans-serif" font-size="40" font-weight="700" fill="#202124">own environment</text>
<rect x="55" y="300" width="690" height="105" rx="18" fill="#e8f0fe"/><text x="90" y="365" font-family="sans-serif" font-size="31" fill="#1967d2">API 27+ · x86 / x86_64 image</text><rect x="55" y="435" width="690" height="105" rx="18" fill="#e6f4ea"/><text x="90" y="500" font-family="sans-serif" font-size="31" fill="#137333">VirtualScene camera setting</text><rect x="55" y="570" width="690" height="105" rx="18" fill="#fef7e0"/><text x="90" y="635" font-family="sans-serif" font-size="31" fill="#b06000">Install or update AR services</text><rect x="55" y="705" width="690" height="105" rx="18" fill="#fce8e6"/><text x="90" y="770" font-family="sans-serif" font-size="31" fill="#c5221f">App and emulator ABI must match</text>
<text x="55" y="910" font-family="monospace" font-size="20" fill="#1967d2">developers.google.com/ar/develop/java/emulator</text>
</svg>''',
        "resolution-map-landscape.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 650">
<style>.p{font:700 28px sans-serif;fill:#ffc857}.s{font:700 28px sans-serif;fill:#50d8ff}.d{font:24px sans-serif;fill:#cbd5e1}.r{fill:#171c26;stroke:#34445d;stroke-width:3;rx:16}</style>
<text x="250" y="55" class="p" text-anchor="middle">THE FRICTION</text><text x="900" y="55" class="s" text-anchor="middle">WHAT CHANGES</text>
<rect x="35" y="85" width="1130" height="78" class="r"/><text x="70" y="133" class="p">APIs</text><text x="440" y="133" class="d">→</text><text x="520" y="133" class="s">one com.codename1.ar surface</text>
<rect x="35" y="177" width="1130" height="78" class="r"/><text x="70" y="225" class="p">Languages</text><text x="440" y="225" class="d">→</text><text x="520" y="225" class="s">shared Java application code</text>
<rect x="35" y="269" width="1130" height="78" class="r"/><text x="70" y="317" class="p">Tools</text><text x="440" y="317" class="d">→</text><text x="520" y="317" class="s">one project and build pipeline</text>
<rect x="35" y="361" width="1130" height="78" class="r"/><text x="70" y="409" class="p">Debugging</text><text x="440" y="409" class="d">→</text><text x="520" y="409" class="s">breakpoints in the JavaSE process</text>
<rect x="35" y="453" width="1130" height="78" class="r"/><text x="70" y="501" class="p">Simulation</text><text x="440" y="501" class="d">→</text><text x="520" y="501" class="s">planes, tracking, light and images as input</text>
<rect x="35" y="545" width="1130" height="78" class="r"/><text x="70" y="593" class="p">Hardware</text><text x="440" y="593" class="d">→</text><text x="520" y="593" class="s">capability checks + native backends at build</text>
</svg>''',
        "resolution-map-portrait.svg": '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1000">
<style>.p{font:700 29px sans-serif;fill:#ffc857}.s{font:700 28px sans-serif;fill:#50d8ff}.r{fill:#171c26;stroke:#34445d;stroke-width:3;rx:17}</style>
<rect x="35" y="25" width="730" height="140" class="r"/><text x="70" y="78" class="p">APIs</text><text x="70" y="128" class="s">one com.codename1.ar surface</text><rect x="35" y="185" width="730" height="140" class="r"/><text x="70" y="238" class="p">Languages</text><text x="70" y="288" class="s">shared Java application code</text><rect x="35" y="345" width="730" height="140" class="r"/><text x="70" y="398" class="p">Tools</text><text x="70" y="448" class="s">one project and build pipeline</text><rect x="35" y="505" width="730" height="140" class="r"/><text x="70" y="558" class="p">Debugging</text><text x="70" y="608" class="s">breakpoints in JavaSE</text><rect x="35" y="665" width="730" height="140" class="r"/><text x="70" y="718" class="p">Simulation</text><text x="70" y="768" class="s">room state becomes input</text><rect x="35" y="825" width="730" height="140" class="r"/><text x="70" y="878" class="p">Hardware</text><text x="70" y="928" class="s">capability checks + native builds</text>
</svg>''',
    })
    return result


def ar_vr_scripts_v2(title: str) -> tuple[dict, dict, list[tuple[int, str]]]:
    """Who we are, why AR/VR is hard, then a point-by-point proved resolution."""
    def cue(text: str, at: int = 650, caption: str | None = None,
            cue_id: str = "voice") -> dict:
        action = {"type": "narration.cue", "id": cue_id, "atMs": at, "text": text,
                  "overflow": "extend"}
        if caption is not None:
            action["caption"] = caption
        return action

    splash = {
        "id": "who-and-what", "durationMs": 10_000, "storyBeats": ["identity"],
        "actions": [
            {"type": "image.show", "id": "brand-mark", "role": "brand", "kind": "illustration",
             "atMs": 250, "path": "codename-one-logo.png",
             "bounds": {"x": 0.045, "y": 0.045, "width": 0.12, "height": 0.04},
             "orientation": {"portrait": {"x": 0.06, "y": 0.025, "width": 0.18, "height": 0.027}}},
            {"type": "svg.show", "id": "welcome-graphic", "atMs": 150,
             "paths": {"landscape": "ar-intro-landscape.svg", "portrait": "ar-intro-portrait.svg"},
             "bounds": {"x": 0.58, "y": 0.09, "width": 0.37, "height": 0.80},
             "orientation": {"portrait": {"x": 0.12, "y": 0.39, "width": 0.76, "height": 0.48}}},
            {"type": "transition", "target": "welcome-graphic", "atMs": 150,
             "durationMs": 900, "effect": "morph", "easing": "ease-out"},
            {"type": "text.show", "id": "splash-title", "role": "title", "atMs": 350,
             "text": "Augmented reality\nand virtual reality", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 3,
             "bounds": {"x": 0.055, "y": 0.22, "width": 0.50, "height": 0.30},
             "orientation": {"portrait": {"x": 0.07, "y": 0.09, "width": 0.86, "height": 0.23}}},
            {"type": "text.show", "id": "splash-promise", "atMs": 800,
             "text": "Native apps in Java · one codebase", "uiid": "VideoBody",
             "bounds": {"x": 0.06, "y": 0.61, "width": 0.46, "height": 0.07},
             "orientation": {"portrait": {"x": 0.12, "y": 0.32, "width": 0.76, "height": 0.05}}},
            cue("Codename One lets developers build native apps in Java from one codebase. Today, we are looking at why augmented reality and virtual reality development is unusually hard."),
        ],
    }
    fragmentation = {
        "id": "one-feature-two-worlds", "durationMs": 15_000, "storyBeats": ["problem"],
        "actions": [
            {"type": "text.show", "id": "fragment-title", "atMs": 0,
             "text": "One feature. Two development worlds.", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.025, "width": 0.92, "height": 0.14}},
            {"type": "svg.show", "id": "fragmentation", "atMs": 450,
             "paths": {"landscape": "industry-fragmentation-landscape.svg", "portrait": "industry-fragmentation-portrait.svg"},
             "bounds": {"x": 0.035, "y": 0.19, "width": 0.93, "height": 0.75},
             "orientation": {"portrait": {"x": 0.06, "y": 0.18, "width": 0.88, "height": 0.76}}},
            cue("Before any framework enters the picture, a basic augmented reality feature already splits. ARKit and ARCore expose different APIs, live in different toolchains, and pull developers toward different languages and build environments."),
        ],
    }
    simulator_evidence = {
        "id": "the-simulator-problem", "durationMs": 14_000, "storyBeats": ["difficulty"],
        "actions": [
            {"type": "text.show", "id": "sim-problem-title", "atMs": 0,
             "text": "Even the feedback loop depends on hardware", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.025, "width": 0.92, "height": 0.14}},
            {"type": "svg.show", "id": "stackoverflow-proof", "role": "evidence", "kind": "capture", "atMs": 450,
             "paths": {"landscape": "evidence-stackoverflow-landscape.svg", "portrait": "evidence-stackoverflow-portrait.svg"},
             "sourceTitle": "Is it possible to run ARKit in Simulator?",
             "sourceUrl": "https://stackoverflow.com/questions/45034240/is-it-possible-to-run-arkit-in-simulator/45034297",
             "bounds": {"x": 0.06, "y": 0.20, "width": 0.88, "height": 0.72},
             "orientation": {"portrait": {"x": 0.08, "y": 0.18, "width": 0.84, "height": 0.76}}},
            cue("The camera, gyroscope, accelerometer, and physical room are part of the program. That is why a normal simulator cannot simply invent the state that ARKit expects."),
        ],
    }
    debugging_evidence = {
        "id": "debugging-on-two-devices", "durationMs": 14_000, "storyBeats": ["difficulty"],
        "actions": [
            {"type": "text.show", "id": "debug-title", "atMs": 0,
             "text": "A bug can belong to one device, one room, one moment", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.025, "width": 0.92, "height": 0.14}},
            {"type": "svg.show", "id": "reddit-proof", "role": "evidence", "kind": "capture", "atMs": 450,
             "paths": {"landscape": "evidence-reddit-landscape.svg", "portrait": "evidence-reddit-portrait.svg"},
             "sourceTitle": "ARKit 3 with AR Foundation in Unity3D",
             "sourceUrl": "https://www.reddit.com/r/augmentedreality/comments/by4v4p/arkit_3_with_ar_foundation_in_unity3d_getting/",
             "bounds": {"x": 0.06, "y": 0.20, "width": 0.88, "height": 0.72},
             "orientation": {"portrait": {"x": 0.08, "y": 0.18, "width": 0.84, "height": 0.76}}},
            cue("Now reproduce that failure twice: once through the iOS device loop and once through the Android device loop. Tracking loss, changing planes, lighting, and image detection can disappear before a debugger catches them."),
        ],
    }
    environment_evidence = {
        "id": "simulation-is-an-environment", "durationMs": 13_000, "storyBeats": ["difficulty"],
        "actions": [
            {"type": "text.show", "id": "environment-title", "atMs": 0,
             "text": "Simulation is not a checkbox", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.025, "width": 0.92, "height": 0.14}},
            {"type": "svg.show", "id": "google-proof", "role": "evidence", "kind": "capture", "atMs": 450,
             "paths": {"landscape": "evidence-arcore-landscape.svg", "portrait": "evidence-arcore-portrait.svg"},
             "sourceTitle": "Run AR apps in Android Emulator",
             "sourceUrl": "https://developers.google.com/ar/develop/java/emulator",
             "bounds": {"x": 0.06, "y": 0.20, "width": 0.88, "height": 0.72},
             "orientation": {"portrait": {"x": 0.08, "y": 0.18, "width": 0.84, "height": 0.76}}},
            cue("Even a supported emulator brings its own system image, virtual camera, AR services, and architecture requirements. The setup is another environment to maintain before application code can be tested."),
        ],
    }
    intervention = {
        "id": "one-development-loop", "durationMs": 15_000, "storyBeats": ["intervention"],
        "actions": [
            {"type": "text.show", "id": "resolution-title", "atMs": 0,
             "text": "Now collapse the duplicated work", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.025, "width": 0.92, "height": 0.14}},
            {"type": "svg.show", "id": "resolution-map", "atMs": 350,
             "paths": {"landscape": "resolution-map-landscape.svg", "portrait": "resolution-map-portrait.svg"},
             "bounds": {"x": 0.04, "y": 0.18, "width": 0.92, "height": 0.77},
             "orientation": {"portrait": {"x": 0.06, "y": 0.17, "width": 0.88, "height": 0.79}}},
            {"type": "transition", "target": "resolution-map", "atMs": 350,
             "durationMs": 900, "effect": "morph", "easing": "ease-out"},
            cue("This is where the Codename One augmented reality API changes the shape of the work. One Java API fronts the native iOS and Android backends, while the JavaSE simulator keeps the same application logic inside a desktop debug loop."),
        ],
    }
    running_ar = {
        "id": "run-the-same-ar-code", "durationMs": 29_000, "storyBeats": ["proof"],
        "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"},
        "actions": [
            {"type": "text.show", "id": "running-title", "role": "title", "atMs": 0,
             "text": "The sample is running, not posing for a screenshot", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2},
            {"type": "code.show", "id": "ar-code", "role": "code", "atMs": 200,
             "language": "java", "path": "excerpt.txt"},
            {"type": "demo.mount", "id": "ar-running", "role": "demo", "atMs": 250,
             "class": "com.codename1.videobuilder.demos.ARDemoScene", "animated": False},
            {"type": "demo.action", "atMs": 3_100, "name": "placeModel", "arguments": {}},
            {"type": "demo.action", "atMs": 6_750, "name": "scaleModel", "arguments": {"scale": 1.8}},
            {"type": "demo.action", "atMs": 10_000, "name": "moveModel", "arguments": {"x": 0.24}},
            {"type": "demo.action", "atMs": 13_000, "name": "trackingLimited", "arguments": {}},
            {"type": "demo.action", "atMs": 16_000, "name": "trackingNormal", "arguments": {}},
            {"type": "focus.show", "id": "ar-focus", "target": "ar-code", "atMs": 4_500,
             "relativeBounds": {"x": 0.02, "y": 0.34, "width": 0.96, "height": 0.60},
             "label": "same Java handler on iOS, Android and JavaSE", "color": "50d8ff"},
            {"type": "replay", "atMs": 19_000, "fromMs": 12_200, "toMs": 16_600,
             "rewindDurationMs": 520, "rewindFps": 9, "playbackRate": 0.58,
             "label": "Again: force the failure, then recover"},
            cue("Here is the compiled sample. The code creates a session, places an anchored model, changes the model, forces degraded tracking, and restores it. Those are application and simulator state changes, not motion applied to a picture."),
            cue("Now watch the failure itself. The simulator reports limited tracking, rewinds the state change, and plays the recovery again slowly enough to inspect.",
                at=17_200, cue_id="replay-voice"),
        ],
    }
    running_vr = {
        "id": "the-vr-side-runs-too", "durationMs": 18_000, "storyBeats": ["proof", "victory"],
        "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"},
        "actions": [
            {"type": "text.show", "id": "vr-title", "role": "title", "atMs": 0,
             "text": "Virtual reality stays in the same Java project", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2},
            {"type": "code.show", "id": "vr-code", "role": "code", "atMs": 150,
             "language": "java", "path": "media360.txt"},
            {"type": "demo.mount", "id": "vr-running", "role": "demo", "atMs": 200,
             "class": "com.codename1.videobuilder.demos.Media360DemoScene", "animated": False},
            {"type": "demo.action", "atMs": 3_500, "name": "lookRight", "arguments": {"yaw": 42}},
            {"type": "demo.action", "atMs": 6_500, "name": "lookUp", "arguments": {"pitch": 18}},
            {"type": "demo.action", "atMs": 9_500, "name": "stereo", "arguments": {}},
            {"type": "demo.action", "atMs": 13_000, "name": "recenter", "arguments": {}},
            cue("The virtual reality side is also live. Media360View changes its yaw, pitch, and stereo state through the component API. The same Java project now covers native augmented reality and portable panorama viewing."),
        ],
    }
    victory = {
        "id": "what-the-developer-gets-back", "durationMs": 15_000, "storyBeats": ["victory"],
        "actions": [
            {"type": "svg.show", "id": "portable-proof", "atMs": 250,
             "paths": {"landscape": "portable-landscape.svg", "portrait": "portable-portrait.svg"},
             "bounds": {"x": 0.05, "y": 0.10, "width": 0.90, "height": 0.80},
             "orientation": {"portrait": {"x": 0.08, "y": 0.10, "width": 0.84, "height": 0.80}}},
            {"type": "text.show", "id": "victory-title", "atMs": 0,
             "text": "One codebase. Native backends. A desktop debug loop.", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 2,
             "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.14}},
            cue("The APIs, languages, tools, debugging loop, simulation states, and hardware checks no longer become six separate application problems. The developer works in one Java codebase, then ships through the native ARKit and ARCore backends."),
        ],
    }
    outro = {
        "id": "outro", "durationMs": 15_000, "storyBeats": ["outro"],
        "actions": [
            {"type": "outro.show", "id": "next", "atMs": 0, "durationMs": 1_100,
             "eyebrow": "CODENAME ONE · AUGMENTED + VIRTUAL REALITY",
             "title": "Build the experience once. Test the hard states at your desk.",
             "subtitle": "One Java API · native ARKit and ARCore · JavaSE simulation · Media360View",
             "prompt": "Which AR debugging state costs your team the most time?",
             "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}},
            cue("That is the practical change: one portable implementation, native augmented reality on both mobile platforms, and hard-to-reproduce room states available at the desk. Which augmented reality debugging state costs your team the most time?"),
        ],
    }

    editorial = {
        "storyType": "live-demo", "status": "approved",
        "proof": ["independent Stack Overflow and Reddit complaints", "official ARCore emulator requirements",
                  "compiled JavaSE AR session", "compiled Media360View"],
        "humanBeat": "A developer loses the normal edit-run-debug loop because two SDKs and a physical room become part of every test.",
        "visualIdentity": "Two platform lanes multiply the work, then collapse into one Java lane whose state changes are visible in a running simulator.",
        "bespokeVisualization": "A sourced evidence wall establishes the split before a problem-to-resolution map closes every row with code and live state.",
        "heroJourney": {
            "identity": "Codename One is introduced as a native-app Java platform and the topic is augmented and virtual reality.",
            "problem": "AR development splits across APIs, languages, tools, devices, sensors, and physical environments.",
            "difficulty": "Independent developers describe simulation and debugging as unavailable, fragile, or effectively impossible in common loops.",
            "intervention": "The Codename One AR API and JavaSE simulation collapse duplicated application work into one Java development loop.",
            "victory": "The developer ships through native ARKit and ARCore backends while reproducing difficult states at the desk."
        },
        "problemDimensions": ["apis", "tools", "languages", "debugging", "hardware", "simulation"],
        "resolutionMap": [
            {"problem": "apis", "solution": "one com.codename1.ar API", "proof": "excerpt.txt and native backend diagram"},
            {"problem": "tools", "solution": "one Codename One project and build pipeline", "proof": "compiled demo module"},
            {"problem": "languages", "solution": "shared Java application code", "proof": "syntax-highlighted Java source"},
            {"problem": "debugging", "solution": "breakpoints in the JavaSE process", "proof": "live tracking state transitions"},
            {"problem": "hardware", "solution": "AR.isSupported and native backend selection", "proof": "source API and native target diagram"},
            {"problem": "simulation", "solution": "virtual room state as controllable input", "proof": "compiled JavaSE AR backend actions"}
        ]
    }
    long_scenes = [splash, fragmentation, simulator_evidence, debugging_evidence, environment_evidence,
                   intervention, running_ar, running_vr, victory, outro]
    short_scenes = [
        {**splash, "id": "short-who-and-what"},
        {**fragmentation, "id": "short-one-feature-two-worlds", "durationMs": 12_000},
        {**simulator_evidence, "id": "short-simulator-proof", "durationMs": 12_000},
        {**debugging_evidence, "id": "short-debugging-proof", "durationMs": 12_000},
        {**intervention, "id": "short-one-development-loop", "durationMs": 13_000},
        {**running_ar, "id": "short-running-ar", "durationMs": 29_000},
        {**victory, "id": "short-victory", "durationMs": 13_000},
        {**outro, "id": "short-outro", "durationMs": 12_000},
    ]
    long_script = {"schemaVersion": 1, "id": "ar-vr-support-simulation", "title": title,
                   "editorial": editorial, "output": OUTPUT, "narration": narration(), "scenes": long_scenes}
    short_script = {"schemaVersion": 1, "id": "ar-vr-support-simulation-short",
                    "title": "Why augmented reality development fights your debug loop",
                    "editorial": editorial, "output": OUTPUT, "narration": narration(), "scenes": short_scenes}
    return long_script, short_script, chapter_times(long_script)


def timestamp(ms: int) -> str:
    seconds = ms // 1000
    return f"{seconds // 60:02d}:{seconds % 60:02d}"


def apply_measured_timings(project: Path, long_script: dict, short_script: dict) -> None:
    timing_path = project / "timings.json"
    if not timing_path.exists():
        return
    timings = json.loads(timing_path.read_text(encoding="utf-8"))
    for key, script in (("landscape", long_script), ("portrait", short_script)):
        measured = timings.get(key, {})
        for scene in script["scenes"]:
            if scene["id"] in measured:
                scene["durationMs"] = int(measured[scene["id"]])


def chapter_times(script: dict) -> list[tuple[int, str]]:
    result: list[tuple[int, str]] = []
    cursor = 0
    total = sum(int(scene["durationMs"]) for scene in script["scenes"])
    last_chapter = -10_000
    for scene in script["scenes"]:
        # Measured narration can shift scene boundaries. YouTube requires each
        # chapter, including the final one, to span at least ten seconds, so
        # merge adjacent beats rather than emitting invalid upload metadata.
        if cursor - last_chapter >= 10_000 and (cursor == 0 or total - cursor >= 10_000):
            result.append((cursor, safe_title(scene["id"].replace("-", " ").title(), 60)))
            last_chapter = cursor
        cursor += scene["durationMs"]
    return result


def apply_visual_overrides(script: dict, overrides: dict[str, dict]) -> None:
    """Replace generated headings, bullets, and diagram labels for one-off stories."""
    for scene in script["scenes"]:
        replacement = overrides.get(scene["id"])
        if not replacement:
            continue
        labels = replacement.get("labels")
        if labels:
            labels = [diagram_label(label) for label in labels[:3]]
            while len(labels) < 3:
                labels.append(["Input", "Mechanism", "Result"][len(labels)])
            horizontal = "flowchart LR\n" + "\n".join(
                f"N{i}[{label}] --> N{i + 1}[{labels[i + 1]}]"
                for i, label in enumerate(labels[:-1])
            )
        for action in scene["actions"]:
            if "title" in replacement and action["type"] in {
                "bullets.show", "text.show"
            }:
                key = "title" if action["type"] == "bullets.show" else "text"
                action[key] = safe_title(replacement["title"], 110)
            if "items" in replacement and action["type"] == "bullets.show":
                action["items"] = [
                    safe_title(item, 120) for item in replacement["items"][:4]
                ]
            if labels and action["type"] == "diagram.show":
                action["texts"] = {
                    "landscape": horizontal,
                    "portrait": horizontal.replace("flowchart LR", "flowchart TD"),
                }
            if "prompt" in replacement and action["type"] == "outro.show":
                action["prompt"] = safe_title(replacement["prompt"], 110)


def generate(entry: dict, output_root: Path, ignore_timings: bool = False) -> dict:
    slug = entry["slug"]
    blog = BLOG_ROOT / f"{slug}.md"
    raw = blog.read_text(encoding="utf-8")
    meta, body = frontmatter(raw)
    title = meta["title"]
    description = clean_markdown(meta.get("description", ""))
    sections, code_blocks, links, code_heading = article_parts(body)
    preferred_code_section = entry.get("codeSection")
    if preferred_code_section:
        headings = list(re.finditer(r"^##+\s+(.+?)\s*$", body, re.M))
        selected_code = None
        for index, heading in enumerate(headings):
            if clean_markdown(heading.group(1)) != preferred_code_section:
                continue
            end = headings[index + 1].start() if index + 1 < len(headings) else len(body)
            selected_code = re.search(
                r"```(?:[A-Za-z0-9_+.-]+)?\s*\n(.*?)```",
                body[heading.end():end],
                re.S,
            )
            break
        if not selected_code:
            raise ValueError(
                f"{slug}: codeSection heading missing or has no code: "
                f"{preferred_code_section}"
            )
        selected_block = selected_code.group(1)
        code_blocks = [selected_block] + [
            block for block in code_blocks if block != selected_block
        ]
        code_heading = preferred_code_section
    preferred_sections = entry.get("sectionOrder", [])
    if preferred_sections:
        by_title = {title: (title, text) for title, text in sections}
        missing = [title for title in preferred_sections if title not in by_title]
        if missing:
            raise ValueError(f"{slug}: sectionOrder headings not found: {missing}")
        chosen = set(preferred_sections)
        sections = [by_title[title] for title in preferred_sections] + [
            section for section in sections if section[0] not in chosen
        ]
    if len(sections) < 2:
        raise ValueError(f"{slug}: fewer than two substantive sections")
    project = PROJECT_ROOT / slug
    project.mkdir(parents=True, exist_ok=True)
    model_dir = project / ".video-tools" / "kokoro"
    model_dir.mkdir(parents=True, exist_ok=True)
    for filename in ("kokoro-v1.0.onnx", "voices-v1.0.bin"):
        source = SHARED_MODEL_DIR / filename
        target = model_dir / filename
        if not target.exists():
            os.link(source, target)
    if code_blocks:
        excerpt = "\n".join(code_blocks[0].strip().splitlines()[:18]) + "\n"
    else:
        excerpt = "// This article is explained with diagrams and source-grounded slides.\n"
    (project / "excerpt.txt").write_text(excerpt, encoding="utf-8")
    if slug == "ar-vr-support-simulation":
        (project / "excerpt.txt").write_text(
            "view.addPointerReleasedListener(e -> {\n"
            "    float xn =\n"
            "        (e.getX() - view.getAbsoluteX())\n"
            "        / (float) view.getWidth();\n"
            "    float yn =\n"
            "        (e.getY() - view.getAbsoluteY())\n"
            "        / (float) view.getHeight();\n"
            "    session.hitTest(xn, yn)\n"
            "        .ready(hits -> {\n"
            "            if (hits.length == 0) return;\n"
            "            ARAnchor anchor =\n"
            "                hits[0].createAnchor();\n"
            "            ARNode node = new ARNode(\n"
            "                ARModel.fromGltf(modelBytes));\n"
            "            anchor.setNode(node);\n"
            "        });\n"
            "});\n",
            encoding="utf-8",
        )
        asset_root = ROOT / "docs" / "website" / "static" / "blog" / slug
        for filename in ("ar-simulator-model.png", "vr-360-panorama.png"):
            (project / filename).write_bytes((asset_root / filename).read_bytes())
        (project / "codename-one-logo.png").write_bytes(
            (ROOT / "docs" / "website" / "static" / "uploads" / "Codename-One-White-Logo.png").read_bytes()
        )
        for filename, source in ar_story_svgs_v2().items():
            (project / filename).write_text(source + "\n", encoding="utf-8")
        (project / "media360.txt").write_text(
            "Media360View panorama = new Media360View();\n"
            "panorama.setImage(\n"
            "    EncodedImage.create(\"/panorama.jpg\"));\n"
            "form.add(BorderLayout.CENTER, panorama);\n",
            encoding="utf-8",
        )
        long_script, short_script, _ = ar_vr_scripts_v2(title)
    elif slug == WIDGETS_SLUG:
        write_widgets_assets(project, ROOT)
        long_script, short_script = widgets_scripts(title, OUTPUT, narration())
    elif slug == FIDELITY_SLUG:
        write_fidelity_assets(project, ROOT)
        long_script, short_script = fidelity_scripts(title, OUTPUT, narration())
    elif slug in STORY_PROFILES:
        profile = STORY_PROFILES[slug]
        capture_names = write_profiled_assets(project, ROOT, profile)
        long_script, short_script = profiled_scripts(
            slug, title, OUTPUT, narration(), profile, capture_names
        )
    else:
        long_script, short_script, _ = build_scripts(
            slug, title, description, entry["focus"], sections, code_blocks, code_heading
        )
        blocked = {"storyType": "visual-explainer", "status": "blocked",
                   "proof": ["automatic article template; requires story review"],
                   "humanBeat": "requires an article-specific human consequence",
                   "visualIdentity": "requires an article-specific recurring visual motif",
                   "bespokeVisualization": "requires a visualization authored for this article",
                   "heroJourney": {
                       "desire": "requires an article-specific desired outcome",
                       "problem": "requires an article-specific developer problem",
                       "difficulty": "requires concrete stakes and escalating difficulty",
                       "intervention": "requires a source-backed Codename One intervention",
                       "victory": "requires a proved developer transformation"
                   }}
        long_script["editorial"] = blocked
        short_script["editorial"] = blocked
    narration_overrides = entry.get("narrationOverrides", {})
    if narration_overrides:
        for script in (long_script, short_script):
            for scene in script["scenes"]:
                replacement = narration_overrides.get(scene["id"])
                if replacement:
                    scene["narration"]["text"] = replacement
                    scene["durationMs"] = narration_duration(replacement, scene["durationMs"])
    visual_overrides = entry.get("visualOverrides", {})
    if visual_overrides:
        apply_visual_overrides(long_script, visual_overrides)
        apply_visual_overrides(short_script, visual_overrides)
    if not ignore_timings:
        apply_measured_timings(project, long_script, short_script)
    chapters = chapter_times(long_script)
    (project / "video.json").write_text(json.dumps(long_script, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    (project / "short.json").write_text(json.dumps(short_script, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    source_url = f"https://www.codenameone.com/blog/{slug}/"
    seo_title = title
    if title.lower().startswith("codename one "):
        seo_title = entry["focus"][:1].upper() + entry["focus"][1:]
    if slug in STORY_PROFILES:
        seo_title = STORY_PROFILES[slug].get("youtubeTitle", seo_title)
    query = entry["query"]
    phrase_tags = [
        phrase for phrase in (
            "App Store submission", "Google Play submission", "Huawei AppGallery",
            "Live Activities", "Dynamic Island", "Material 3", "Liquid Glass",
        ) if all(
            word.lower() in (query + " " + title).lower() for word in phrase.split()
        )
    ]
    query_parts = [
        part for part in query.split()
        if part.lower() not in {"codename", "one", "java", "app", "store", "google", "play"}
        and len(part) >= 4
    ]
    video_tags = unique(["Codename One", "Java", query] + phrase_tags + query_parts[:5])
    evidence_lines = ["# Evidence map", "", f"Source: `{blog.relative_to(ROOT)}`", f"Canonical: {source_url}", "",
                      f"## Thesis", "", entry["focus"], "", "## Supported beats", ""]
    for heading, paragraph in sections[:6]:
        evidence_lines.extend([f"- **{heading}:** {limit_words(paragraph, 45)}"])
    if links:
        evidence_lines.extend(["", "## Referenced evidence", ""] + [f"- {link}" for link in links[:12]])
    if slug == "ar-vr-support-simulation":
        evidence_lines.extend([
            "", "## Independent problem evidence", "",
            "- Stack Overflow: https://stackoverflow.com/questions/45034240/is-it-possible-to-run-arkit-in-simulator/45034297",
            "- Reddit: https://www.reddit.com/r/augmentedreality/comments/by4v4p/arkit_3_with_ar_foundation_in_unity3d_getting/",
            "- Google ARCore emulator requirements: https://developers.google.com/ar/develop/java/emulator",
            "- Google AR platform environments: https://developers.google.com/ar/",
            "- Apple ARKit device support: https://developer.apple.com/documentation/arkit/verifying-device-support-and-user-permission",
        ])
    elif slug == WIDGETS_SLUG:
        evidence_lines.extend([
            "", "## Independent problem evidence", "",
            "- Apple WidgetKit timeline documentation: https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date",
            "- Apple ActivityKit documentation: https://developer.apple.com/documentation/activitykit",
            "- Android app widgets overview: https://developer.android.com/develop/ui/views/appwidgets/overview",
            "- Android RemoteViews documentation: https://developer.android.com/reference/android/widget/RemoteViews",
            "", "## Product proof", "",
            "- `samples/samples/SurfacesSample/SurfacesSample.java`",
            "- `samples/samples/SurfacesSample/surfaces.json`",
            "- `CodenameOne/src/com/codename1/surfaces/`",
            "- `ports/JavaSE/src/com/codename1/impl/javase/JavaSEWidgetBridge.java`",
        ])
    elif slug == FIDELITY_SLUG:
        evidence_lines.extend([
            "", "## Independent problem evidence", "",
            "- Apple Liquid Glass adoption guidance: https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass",
            "- Android Material 2 to Material 3 migration guidance: https://developer.android.com/develop/ui/compose/designsystems/material2-material3",
            "", "## Product proof", "",
            "- `scripts/fidelity-app/goldens/ios-26-metal/`",
            "- `scripts/fidelity-app/goldens/android-m3/`",
            "- `scripts/fidelity-app/goldens/ios-26-metal-frames/`",
            "- `CodenameOne/src/com/codename1/ui/TabSelectionMorph.java`",
            "- `maven/core-unittests/src/test/java/com/codename1/ui/TabSelectionMorphTest.java`",
        ])
    elif slug in STORY_PROFILES:
        evidence_lines.extend(["", "## Independent problem evidence", ""])
        for item in STORY_PROFILES[slug]["evidence"]:
            evidence_lines.append(
                f"- {item['sourceTitle']}: {item['sourceUrl']} — {item['body']}"
            )
        evidence_lines.extend(["", "## Product proof", ""])
        evidence_lines.extend(
            f"- `{relative}`" for relative in STORY_PROFILES[slug]["captures"]
        )
    (project / "evidence.md").write_text("\n".join(evidence_lines) + "\n", encoding="utf-8")
    if slug == "ar-vr-support-simulation":
        red_team = f"""# Positioning red-team: {title}

## Verdict

SHIP

## Thesis

AR and VR development fragments an ordinary application feature across APIs, languages, tools, devices, sensors, and physical environments. Codename One collapses the application work into one Java codebase and makes difficult room states controllable in the JavaSE simulator.

## Act 1 audit

- The first sentence says what Codename One is: native apps in Java from one codebase.
- The topic is named in full: augmented reality and virtual reality.
- The logo is a small signature. The topic-specific spatial graphic and title carry the frame.
- The splash makes no feature claim beyond the basic platform identity.

## Act 2 audit

- Codename One is absent after the splash until the intervention.
- The problem is built before the solution: different APIs, languages, tools, device loops, hardware inputs, and simulator requirements.
- The difficulty is concrete rather than adjectival: the camera, sensors, room, architecture, runtime, and device are part of reproduction.
- A hostile newcomer can explain why AR development is hard without knowing Codename One exists.

## Act 3 audit

- Every act-two row has a matching resolution: API, language, tools, debugging, simulation, and hardware support.
- The resolution is shown before it is celebrated: exact Java code, a compiled AR session, forced tracking loss and recovery, anchored-model changes, and a compiled Media360View.
- The portability claim is limited to the shipped native ARKit and ARCore backends plus JavaSE simulation described in the source.
- The developer remains the protagonist; the script never announces that Codename One “comes to the rescue.”

## Evidence audit

- The Stack Overflow card attributes a public question and accepted answer about ARKit simulator limits.
- The Reddit card attributes a practitioner complaint about cross-platform AR debugging.
- The Google card summarizes official emulator prerequisites and links the exact documentation.
- Product proof comes from repository source and compiled demo code, not a generated claim card.

## Interaction audit

- The old picture-drag treatment is removed.
- `ARDemoScene` opens the actual AR API and mutates anchors, nodes, and JavaSE tracking state.
- `Media360DemoScene` mounts the actual component and changes yaw, pitch, stereo, and recenter state.
- No pointer choreography remains; the running application state and status readouts show each action.

## Rejected versions

The previous desire-first cut failed this contract because it introduced the product before establishing the industry problem, treated screenshot movement as interaction, and had no point-by-point problem-to-resolution map. It would receive `REWRITE`, not `SHIP`, under the current gate.

## Highest-leverage risk

The independent evidence is rendered as attributed evidence cards rather than literal browser screenshots. Do not let their polish make them look like invented testimonials: retain the source name, short quotation or factual summary, and visible URL in every card.
"""
    elif slug == WIDGETS_SLUG:
        red_team = f"""# Positioning red-team: {title}

## Verdict

SHIP

## Act 1 audit

- Codename One is a small corner signature, while the escaped delivery ETA carries the frame.
- The narration identifies Codename One and names widgets, Live Activities, and Dynamic Island within eight seconds.
- The opening creates a question: how can a useful experience continue after the app process stops?

## Act 2 audit

- The customer desire comes before architecture: see the delivery ETA without repeatedly reopening the app.
- Codename One is quarantined after the splash until the intervention.
- The difficulty is concrete: no process, component tree, event dispatch thread, listener, or shared platform lifecycle.
- Apple and Android documentation are independently attributed before any product proof.

## Act 3 audit

- The intervention is a mechanism, not a rescue slogan: serialize a focused layout and dated state timeline.
- Real source appears immediately, followed by a compiled demo using WidgetTimeline, LiveActivity, resize, updates, and the cold-start action queue.
- The one-model claim is limited to the shipped system-surface mappings described by the article and repository source.
- The developer remains the protagonist and gains a repeatable desktop test loop.

## Evidence audit

- The sample excerpts are extracted directly from `SurfacesSample.java` when the package is generated.
- The widget and Dynamic Island captures come from the canonical article assets.
- Backend behavior is described as implemented mapping; captures are not presented as live on-device footage.
- No roadmap or unshipped limitation appears in the narration.

## Interaction audit

- The demo compiles against the public Surfaces API and calls actual publish, start, update, and dispatch methods.
- Timeline advance, resize, Live Activity progress, and cold-start delivery produce visibly different states.
- The single replay rewinds a meaningful timeline transition and plays it slowly; it does not move a static picture.
- Code focus callouts point to the exact state, timed-text, region, and action-handler lines being discussed.

## Hostile-viewer check

- A newcomer can state the problem before the solution appears: useful state must survive a dead app and incompatible platform surface rules.
- A skeptical developer sees actual source and compiled state changes, not a corporate feature list.
- The focused surface-node constraint is admitted positively and precisely instead of implying full app components run outside the process.

## Highest-leverage risk

The demo is deterministic instrumentation around real public API calls, not a recording of native iOS and Android shells. Keep the authentic article captures visible and never call the JavaSE rendering on-device footage.
"""
    elif slug == FIDELITY_SLUG:
        red_team = fidelity_red_team(title)
    elif slug in STORY_PROFILES:
        red_team = profiled_red_team(title, STORY_PROFILES[slug])
    else:
        red_team = f"""# Positioning red-team: {title}

## Verdict

REWRITE

## Thesis

{entry['focus']}.

## Failures found and resolved

1. A release-summary reading would give a stranger no reason to care. The script now leads with the mechanism and practical consequence.
2. The source contains several features or steps. The video follows one causal spine and keeps secondary material as evidence, not a feature list.
3. Product claims can outrun the implementation. Every narrated claim is limited to the source passages recorded in `evidence.md`.
4. Long diagram labels overflow at video scale. Both orientations use three compact causal nodes and require full-resolution frame review before upload.
5. Fixed narration budgets create dead air. The batch runner measures local TTS and fits each scene with a short tail before rendering.

## Hostile-reader simulation

- “This is another framework feature announcement.” The hook starts with the engineering problem and mechanism.
- “What actually changed?” The middle scenes use the article's concrete implementation evidence instead of broad product adjectives.
- “What does the developer gain?” A dedicated victory beat states the source-backed outcome after the proof.

## Boldness ruling

Lead with the supported mechanism. Do not generalize it into universal performance, fidelity, security, or portability superiority.

## Highest-leverage correction

Keep the source-backed technical consequence early and retain the full hero journey before approval.
"""
    (project / "red-team.md").write_text(red_team, encoding="utf-8")
    if slug == "ar-vr-support-simulation":
        write_ar_vr_thumbnail(project)
    elif slug == WIDGETS_SLUG:
        write_widgets_thumbnail(project)
    elif slug == FIDELITY_SLUG:
        write_fidelity_thumbnail(project)
    elif slug in STORY_PROFILES:
        capture_names = [
            f"capture-{index}{Path(relative).suffix.lower()}"
            for index, relative in enumerate(STORY_PROFILES[slug]["captures"], 1)
        ]
        write_profiled_thumbnail(project, STORY_PROFILES[slug], capture_names)
    else:
        write_thumbnail(project, title, entry["focus"])
    trail = trail_metadata(slug)
    chapter_text = "\n".join(f"{timestamp(at)} {name}" for at, name in chapters)
    common_links = "\n".join(links[:6])
    landscape_description = f"""{description}

This video explains {entry['focus'].lower()}.

Full article and source material:
{source_url}?utm_source=youtube&utm_medium=video&utm_campaign=past-year-video-library

{common_links}

Chapters
{chapter_text}

Which part should we unpack next?

Series: {trail['title']} — part {trail['position']} of {trail['total']}

#Java #CodenameOne
"""
    short_description = f"""{description}

Full article:
{source_url}?utm_source=youtube&utm_medium=short&utm_campaign=past-year-video-library

Watch the related video for the complete mechanism, source links, and limitations.

Series: {trail['title']}

#Java #CodenameOne #Shorts
"""
    if slug == "ar-vr-support-simulation":
        seo_title = "One Java AR API for ARKit, ARCore, and Desktop Simulation"
        landscape_description = f"""AR development usually splits across ARKit, ARCore, devices, and separate debugging loops. See how Codename One runs the same Java AR code on iOS, Android, and a controllable JavaSE simulated room.

The running sample places and moves an AR model, changes detected planes, resets tracking, and exercises VR rendering from the same Java project.

Full article and source material:
{source_url}?utm_source=youtube&utm_medium=video&utm_campaign=ar-vr-java

https://github.com/codenameone/CodenameOne/pull/5335

Chapters
00:00 AR in one Java codebase
00:14 Why AR development fragments
00:32 Why a simulator is not enough
00:45 Device-only debugging slows the loop
01:01 Simulate the room, not just the screen
01:17 One Java development loop
01:37 Run the same AR code
02:06 VR in the same project
02:24 What changes for the developer
02:43 Test the hard states at your desk

What AR state is hardest for you to reproduce on demand?

Series: {trail['title']} — part {trail['position']} of {trail['total']}

#Java #AugmentedReality #CodenameOne
"""
        short_description = f"""ARKit and ARCore normally mean separate tools and device-only debugging. Codename One runs the same Java AR flow on both platforms and in a controllable desktop room.

Full article:
{source_url}?utm_source=youtube&utm_medium=short&utm_campaign=ar-vr-java

The related landscape video shows the running AR and VR samples, source code, and complete debugging loop.

#Java #AugmentedReality #CodenameOne #Shorts
"""
    elif slug == WIDGETS_SLUG:
        seo_title = "Java Widgets, Live Activities, and Dynamic Island From One Timeline"
        landscape_description = f"""A delivery ETA should stay useful after the app closes. See how one Java surface model becomes a widget, Live Activity, Dynamic Island presentation, Android ongoing notification, and desktop preview.

The compiled sample publishes a four-entry WidgetTimeline, advances time, changes widget size, starts and updates a Live Activity, and delivers a surface action across a cold start.

Full article and source material:
{source_url}?utm_source=youtube&utm_medium=video&utm_campaign=java-system-surfaces

https://github.com/codenameone/CodenameOne/blob/master/samples/samples/SurfacesSample/SurfacesSample.java

Chapters
{chapter_text}

What should your app keep useful after it closes?

Series: {trail['title']} — part {trail['position']} of {trail['total']}

#Java #LiveActivities #CodenameOne
"""
        short_description = f"""The app process stopped, but the delivery ETA keeps moving. One Java timeline can feed widgets, Live Activities, Dynamic Island, Android notifications, and desktop surfaces.

Full article:
{source_url}?utm_source=youtube&utm_medium=short&utm_campaign=java-system-surfaces

The related landscape video shows the real source, compiled timeline, Live Activity updates, and cold-start action delivery.

#Java #LiveActivities #CodenameOne #Shorts
"""
    elif slug == FIDELITY_SLUG:
        seo_title = "How Native-Reference Tests Keep Cross-Platform UI Pixel-Accurate"
        landscape_description = f"""A platform update can change standard controls after your team has tested and shipped a screen. See how Codename One versions native iOS and Android reference captures, compares every component state, and turns UI fidelity into a one-way regression gate.

The compiled sample runs the shipped TabSelectionMorph geometry through fixed progress points. Authentic report captures show native controls on the left and Codename One on the right.

Full article and source material:
{source_url}?utm_source=youtube&utm_medium=video&utm_campaign=native-ui-fidelity

https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass
https://developer.android.com/develop/ui/compose/designsystems/material2-material3
https://github.com/codenameone/CodenameOne/pull/5274

Chapters
{chapter_text}

Which component should the fidelity suite test next?

Series: {trail['title']} — part {trail['position']} of {trail['total']}

#Java #UITesting #CodenameOne
"""
        short_description = f"""Your UI can move even when your application code does not. Codename One versions native iOS and Android reference captures, then fails continuous integration when known fidelity regresses.

Full article:
{source_url}?utm_source=youtube&utm_medium=short&utm_campaign=native-ui-fidelity

The related landscape video shows the authentic native comparisons, compiled motion model, metrics, and complete regression pipeline.

#Java #UITesting #CodenameOne #Shorts
"""
    (project / "landscape-description.txt").write_text(landscape_description, encoding="utf-8")
    (project / "short-description.txt").write_text(short_description, encoding="utf-8")
    account = "shai" if slug in {"we-will-not-sabotage-your-code", "funding-open-source-without-the-bait-and-switch", "metal-and-skins"} else "codenameone"
    promotion = f"""# Private-review promotion draft

Account: {account}
Status: withheld until video publication is authorized

{entry['focus']}.

The video follows the developer problem, the intervention, and the source evidence without turning the post into a feature list.

Video URL: populated from the private upload before publication review
Source: {source_url}
"""
    (project / "promotion.md").write_text(promotion, encoding="utf-8")
    output = output_root / slug
    output.mkdir(parents=True, exist_ok=True)
    long_duration = sum(scene["durationMs"] for scene in long_script["scenes"])
    profile_end_screen = STORY_PROFILES.get(slug)
    fixed_end_screen = slug in {"ar-vr-support-simulation", WIDGETS_SLUG, FIDELITY_SLUG}
    end_screen_video = ({
        "type": "specific-video",
        "videoId": profile_end_screen["endScreenVideoId"],
        "title": profile_end_screen["endScreenVideoTitle"],
        "placement": "right-safe-area",
    } if profile_end_screen else {
        "type": "specific-video", "videoId": "1_Cu1-aVLf8",
        "title": "Java AOT vs JIT: How ParparVM Reached HotSpot Performance",
        "placement": "right-safe-area",
    })
    youtube = {
        "schemaVersion": 2,
        "source": {"slug": slug, "path": str(blog.relative_to(ROOT)), "url": source_url},
        "intent": {"audience": "JVM engineering team lead", "type": "informational",
                   "primaryQuery": entry["query"], "promise": entry["focus"], "demand": "hypothesis"},
        "distribution": {
            "trail": trail,
            "shortsPlaylist": json.loads(TRAILS.read_text(encoding="utf-8"))["shortsPlaylist"],
            "publicationPlan": {
                "landscapePlaylistRole": "official-series",
                "portraitPlaylistRole": "shorts",
                "shortRelatedVideo": "landscape",
                "cards": "add one relevant trail card only after the target playlist is public",
                "endScreen": ("use subscribe plus the explicitly selected related video, aligned to the rendered safe area"
                              if fixed_end_screen or profile_end_screen
                              else "use subscribe plus best-for-viewer while private; consider the next trail video after publication")
            }
        },
        "uploads": {
            "landscape": {
                "video": str(output / f"{slug}-landscape.mp4"), "captions": str(output / f"{slug}.srt"),
                "thumbnail": "thumbnail.jpg",
                "youtube": {"title": safe_title(seo_title, 98), "descriptionFile": "landscape-description.txt",
                            "privacyStatus": "private", "madeForKids": False, "containsSyntheticMedia": True,
                            "categoryId": "28", "defaultLanguage": "en",
                            "tags": (["Codename One", "Java augmented reality", "ARKit Java",
                                      "ARCore Java", "AR simulator", "JavaSE"]
                                     if slug == "ar-vr-support-simulation" else
                                     ["Codename One", "Java widgets", "Live Activities", "Dynamic Island",
                                      "WidgetKit", "ActivityKit", "Android widgets", "JavaSE"]
                                     if slug == WIDGETS_SLUG else
                                     ["Codename One", "UI fidelity testing", "visual regression testing",
                                      "Liquid Glass", "Material 3", "pixel perfect UI", "Java"]
                                     if slug == FIDELITY_SLUG else video_tags),
                            "chapters": [{"atMs": at, "title": name} for at, name in chapters],
                            "engagement": {"primaryCta": "comment",
                                           "commentPrompt": ("What AR state is hardest for you to reproduce on demand?"
                                                             if slug == "ar-vr-support-simulation" else
                                                             "What should your app keep useful after it closes?"
                                                             if slug == WIDGETS_SLUG
                                                             else "Which component should the fidelity suite test next?"
                                                             if slug == FIDELITY_SLUG
                                                             else f"Which part of {safe_title(title, 80)} should we unpack next?"),
                                           "cards": [{"type": "playlist", "target": "trail",
                                                      "atMs": min(max(10_000, long_duration // 2),
                                                                  long_duration - 1_000),
                                                      "state": "pending-publication",
                                                      "reason": "cards require a public playlist"}],
                                           "endScreen": {"startAtMs": long_duration - 15_000,
                                                         "elements": ([
                                                             {"type": "subscribe", "placement": "bottom-left"},
                                                             end_screen_video,
                                                         ] if fixed_end_screen or profile_end_screen else [
                                                             {"type": "subscribe", "placement": "bottom-left"},
                                                             {"type": "best-for-viewer", "placement": "right"}
                                                         ])}}}
            },
            "portrait": {
                "video": str(output / f"{slug}-short-portrait.mp4"), "captions": str(output / f"{slug}-short.srt"),
                "youtube": {"title": ("ARKit + ARCore, One Java API #Shorts"
                                       if slug == "ar-vr-support-simulation" else
                                       "The App Stopped. Why Is the ETA Still Moving? #Shorts"
                                       if slug == WIDGETS_SLUG
                                       else "Your UI Changed Without a Code Change #Shorts"
                                       if slug == FIDELITY_SLUG
                                       else safe_title(seo_title, 87) + " #Shorts"),
                            "descriptionFile": "short-description.txt", "privacyStatus": "private",
                            "madeForKids": False, "containsSyntheticMedia": True, "categoryId": "28",
                            "defaultLanguage": "en", "tags": (unique(["Codename One", "Java augmented reality",
                                                                       "ARKit Java", "ARCore Java", "AR simulator",
                                                                       "JavaSE", "Shorts"])
                                                                     if slug == "ar-vr-support-simulation" else
                                                                     unique(["Codename One", "Java widgets", "Live Activities",
                                                                             "Dynamic Island", "WidgetKit", "ActivityKit",
                                                                             "Android widgets", "JavaSE", "Shorts"])
                                                                     if slug == WIDGETS_SLUG
                                                                     else unique(["Codename One", "UI fidelity testing",
                                                                                  "visual regression testing", "Liquid Glass",
                                                                                  "Material 3", "Java", "Shorts"])
                                                                     if slug == FIDELITY_SLUG
                                                                     else unique(video_tags + ["Shorts"])),
                            "engagement": {"primaryCta": "related-video", "relatedVideoTarget": "landscape",
                                           "commentPrompt": ("What AR state is hardest for you to reproduce on demand?"
                                                             if slug == "ar-vr-support-simulation" else
                                                             "What should your app keep useful after it closes?"
                                                             if slug == WIDGETS_SLUG
                                                             else "Which component should the fidelity suite test next?"
                                                             if slug == FIDELITY_SLUG
                                                             else "Which mechanism should we unpack next?"),
                                           "cards": [],
                                           "relatedVideoState": "pending-landscape-publication"}}
            }
        }
    }
    for name in ("thumbnail.jpg", "landscape-description.txt", "short-description.txt", "red-team.md"):
        (output / name).write_bytes((project / name).read_bytes())
    (output / "youtube.json").write_text(json.dumps(youtube, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return {
        "slug": slug,
        "wave": entry["wave"],
        "status": "authored",
        "project": str(project.relative_to(ROOT)),
        "output": str(output),
        "landscapeId": None,
        "shortId": None,
        "cleanup": "pending",
        "workflow": {
            "schemaVersion": 1,
            "local": {
                "status": "authored",
                "generatedAt": datetime.now(timezone.utc).isoformat(),
                "videoScript": str(project / "video.json"),
                "shortScript": str(project / "short.json"),
            },
            "remote": {"status": "not-uploaded"},
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(tempfile.gettempdir()) / "cn1-youtube-past-year",
    )
    parser.add_argument("--wave", type=int)
    parser.add_argument("--slug")
    parser.add_argument("--ignore-timings", action="store_true")
    parser.add_argument("--force", action="store_true",
                        help="regenerate packages already recorded as verified private")
    parser.add_argument("--thumbnail-only", action="store_true")
    args = parser.parse_args()
    selection = json.loads(SELECTION.read_text(encoding="utf-8"))
    selected = selection["selected"]
    if args.wave is not None:
        selected = [entry for entry in selected if entry["wave"] == args.wave]
    if args.slug:
        selected = [entry for entry in selected if entry["slug"] == args.slug]
    state_path = PROJECT_ROOT / "state.json"
    prior = json.loads(state_path.read_text(encoding="utf-8")) if state_path.exists() else {"schemaVersion": 1, "items": []}
    by_slug = {item["slug"]: item for item in prior["items"]}
    for entry in selected:
        if args.thumbnail_only:
            refresh_thumbnail(entry, args.output.resolve())
            print(f"refreshed thumbnail {entry['slug']}")
            continue
        old = by_slug.get(entry["slug"], {})
        if old.get("status") == "verified-private" and not args.force:
            print(f"skipped verified {entry['slug']}")
            continue
        refreshed = {**old, **generate(entry, args.output.resolve(), args.ignore_timings)}
        if old.get("landscapeId") or old.get("shortId"):
            refreshed["previousRemote"] = {
                "landscapeId": old.get("landscapeId"),
                "shortId": old.get("shortId"),
                "verifiedAt": old.get("verifiedAt"),
                "status": old.get("status", "unknown"),
            }
            refreshed["workflow"]["remote"] = {
                "status": "stale-after-regeneration",
                "reason": "new scripts and media require a new remote verification",
            }
        by_slug[entry["slug"]] = refreshed
        print(f"generated {entry['slug']}")
    by_slug.setdefault("beating-hotspot-performance", {
        "slug": "beating-hotspot-performance", "wave": 0, "status": "uploaded-private",
        "landscapeId": "1_Cu1-aVLf8", "shortId": "u8eMeIrSWno", "cleanup": "retained"
    })
    state = {"schemaVersion": 2, "channel": {"id": "UCb-4T86pwSFyN66s7QJI90A", "name": "Codename One"},
             "items": sorted(by_slug.values(), key=lambda item: (item.get("wave", 99), item["slug"]))}
    state_path.write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
