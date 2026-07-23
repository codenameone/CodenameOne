#!/usr/bin/env python3
"""Deterministic story package builder for article-specific video profiles."""

from __future__ import annotations

import html
import hashlib
import json
import shutil
import textwrap
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps


PROFILE_PATH = Path(__file__).resolve().parents[1] / "projects" / "past-year" / "story-profiles.json"


def profiles() -> dict[str, dict]:
    return json.loads(PROFILE_PATH.read_text(encoding="utf-8"))["profiles"]


def cue(text: str, at: int = 250, cue_id: str | None = None) -> dict:
    action = {
        "type": "narration.cue",
        "id": cue_id or "cue-" + hashlib.sha256(f"{at}\0{text}".encode("utf-8")).hexdigest()[:12],
        "atMs": at,
        "text": text,
        "caption": text,
    }
    return action


def _lines(value: str, width: int, maximum: int = 4) -> list[str]:
    wrapped = textwrap.wrap(value, width=width, break_long_words=False, break_on_hyphens=False)
    return wrapped[:maximum]


def _svg_text(lines: list[str], x: int, y: int, size: int, line_height: int,
              anchor: str = "middle", fill: str = "#f4f8fb", weight: int = 700) -> str:
    result = []
    for index, line in enumerate(lines):
        result.append(
            f'<text x="{x}" y="{y + index * line_height}" text-anchor="{anchor}" '
            f'font-family="Arial,sans-serif" font-size="{size}" font-weight="{weight}" '
            f'fill="{fill}">{html.escape(line)}</text>'
        )
    return "".join(result)


def _frame(width: int, height: int, content: str) -> str:
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}">'
        '<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">'
        '<stop stop-color="#071427"/><stop offset="1" stop-color="#12334a"/>'
        '</linearGradient><filter id="glow"><feGaussianBlur stdDeviation="10"/></filter></defs>'
        f'<rect width="{width}" height="{height}" rx="34" fill="url(#bg)"/>'
        f'{content}</svg>'
    )


def _node(x: int, y: int, w: int, h: int, title: str, subtitle: str = "",
          accent: str = "#50d8ff") -> str:
    title_lines = _lines(title, max(8, w // 23), 2)
    subtitle_lines = _lines(subtitle, 28, 2) if subtitle else []
    title_y = y + h // 2 - (len(title_lines) - 1) * 24 - (18 if subtitle_lines else 0)
    body = (
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="26" fill="#111d2d" '
        f'stroke="{accent}" stroke-width="4"/>'
        + _svg_text(title_lines, x + w // 2, title_y, 31 if w < 300 else 37, 48)
    )
    if subtitle_lines:
        body += _svg_text(subtitle_lines, x + w // 2, y + h - 42 - (len(subtitle_lines) - 1) * 30,
                          24, 31, fill="#b9d4e4", weight=400)
    return body


def _arrow(x1: int, y1: int, x2: int, y2: int, accent: str = "#ffad57") -> str:
    return (
        f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{accent}" '
        'stroke-width="8" stroke-linecap="round"/>'
        f'<circle cx="{x2}" cy="{y2}" r="12" fill="{accent}"/>'
    )


def _motif_svg(profile: dict, orientation: str, phase: str) -> str:
    landscape = orientation == "landscape"
    width, height = (1200, 720) if landscape else (800, 1180)
    motif = profile["motif"]
    # Scene titles are rendered by the native responsive text layer.  Keeping
    # them out of the illustration prevents duplicate headlines and lets the
    # same motif scale cleanly beneath portrait and landscape title regions.
    content = ""

    if motif == "split-toolbox":
        if phase in {"opening", "problem"}:
            content += _node(360 if landscape else 120, 210, 480 if landscape else 560, 250,
                             "SETTINGS", "projects · accounts · signing · builds · extensions", "#ff9c52")
            for index, label in enumerate(("PROJECT", "ACCOUNT", "SIGNING", "BUILDS")):
                x = 80 + index * 280 if landscape else 70 + (index % 2) * 350
                y = 520 if landscape else 560 + (index // 2) * 210
                content += _node(x, y, 230 if landscape else 310, 130, label, "mixed together", "#ff6b67")
        else:
            labels = ("PROJECT SETTINGS", "ACCOUNT", "CERTIFICATES", "BUILDS")
            if landscape:
                for index, label in enumerate(labels):
                    x = 45 + index * 290
                    content += _node(x, 290, 240, 160, label, "focused tool", "#50d8ff" if index == 0 else "#7ce6ad")
                content += _arrow(160, 520, 1040, 520)
                content += _svg_text(["codenameone_settings.properties remains authoritative"], 600, 590, 28, 32, fill="#ffd27c")
            else:
                for index, label in enumerate(labels):
                    content += _node(100, 210 + index * 205, 600, 150, label, "focused tool", "#50d8ff" if index == 0 else "#7ce6ad")
                content += _svg_text(["project file stays authoritative"], 400, 1060, 28, 32, fill="#ffd27c")
    elif motif == "store-conveyor":
        labels = ("GREEN BUILD", "VERSIONED METADATA", "APPLE · GOOGLE · HUAWEI")
        if phase in {"opening", "problem"}:
            labels = ("GREEN BUILD", "COPY · PASTE · UPLOAD", "THREE STORE CONSOLES")
        if landscape:
            for index, label in enumerate(labels):
                x = 45 + index * 390
                content += _node(x, 270, 330, 190, label, "" if index != 1 else "screenshots · locales · notes")
                if index:
                    content += _arrow(x - 55, 365, x, 365)
        else:
            for index, label in enumerate(labels):
                y = 215 + index * 300
                content += _node(100, y, 600, 190, label, "" if index != 1 else "screenshots · locales · notes")
                if index:
                    content += _arrow(400, y - 90, 400, y)
    elif motif == "key-handshake":
        if phase in {"opening", "problem"}:
            labels = ("PASSWORD", "TWO-FACTOR PROMPT", "CHANGED LOGIN PAGE", "TRY AGAIN")
            if landscape:
                positions = ((70, 230), (710, 230), (710, 500), (70, 500))
                for (x, y), label in zip(positions, labels):
                    content += _node(x, y, 420, 140, label, "interactive session", "#ff6b67")
                content += _arrow(490, 300, 710, 300) + _arrow(920, 370, 920, 500)
                content += _arrow(710, 570, 490, 570) + _arrow(280, 500, 280, 370)
            else:
                for index, label in enumerate(labels):
                    y = 190 + index * 230
                    content += _node(100, y, 600, 145, label, "interactive session", "#ff6b67")
                    if index:
                        content += _arrow(400, y - 80, 400, y)
        else:
            labels = ("REVOCABLE API KEY", "AUTO SETUP", "CERTIFICATES + PROFILES")
            if landscape:
                for index, label in enumerate(labels):
                    x = 55 + index * 390
                    content += _node(x, 300, 330, 180, label, "supported automation", "#7ce6ad")
                    if index:
                        content += _arrow(x - 55, 390, x, 390)
            else:
                for index, label in enumerate(labels):
                    y = 220 + index * 300
                    content += _node(100, y, 600, 180, label, "supported automation", "#7ce6ad")
                    if index:
                        content += _arrow(400, y - 90, 400, y)
    elif motif == "dungeon-blueprint":
        if phase in {"opening", "problem"}:
            ox, oy = (220, 180) if landscape else (100, 230)
            cell = 105 if landscape else 100
            for row in range(5):
                for col in range(7 if landscape else 6):
                    if (row + col) % 3 != 0:
                        content += f'<rect x="{ox + col * cell}" y="{oy + row * cell}" width="{cell}" height="{cell}" fill="none" stroke="#50d8ff" stroke-width="5"/>'
            content += _svg_text(["spawn"], ox + cell, oy + cell * 4, 28, 32, fill="#ffd27c")
        else:
            horizon = 285 if landscape else 380
            cx = width // 2
            content += f'<polygon points="80,{height-80} {cx-120},{horizon} {cx+120},{horizon} {width-80},{height-80}" fill="#13263a" stroke="#50d8ff" stroke-width="6"/>'
            for offset in (0, 100, 210):
                content += f'<line x1="{80+offset}" y1="{height-80}" x2="{cx-120+offset//5}" y2="{horizon}" stroke="#7ce6ad" stroke-width="5"/>'
                content += f'<line x1="{width-80-offset}" y1="{height-80}" x2="{cx+120-offset//5}" y2="{horizon}" stroke="#7ce6ad" stroke-width="5"/>'
            content += _svg_text(["WALK · COLLIDE · AIM · FIRE"], cx, height - 125, 30, 34, fill="#ffd27c")
    elif motif == "media-timeline":
        labels = (("FRAMES", "#50d8ff"), ("PCM AUDIO", "#7ce6ad"), ("WORDS + CAPTIONS", "#ffad57"))
        left, right = (120, width - 80)
        top = 240 if landscape else 330
        gap = 130 if landscape else 190
        for index, (label, color) in enumerate(labels):
            y = top + index * gap
            content += _svg_text([label], left, y - 20, 26, 30, anchor="start", fill=color)
            content += f'<line x1="{left}" y1="{y+25}" x2="{right}" y2="{y+25}" stroke="{color}" stroke-width="8"/>'
            for tick in range(7):
                x = left + tick * (right - left) // 6
                content += f'<rect x="{x-9}" y="{y+8}" width="18" height="34" rx="7" fill="{color}"/>'
        playhead = int(left + (right-left) * (0.28 if phase in {"opening", "problem"} else 0.72))
        content += f'<line x1="{playhead}" y1="{top-80}" x2="{playhead}" y2="{top+gap*2+90}" stroke="#ffffff" stroke-width="6"/>'
        content += _svg_text(["one master clock"], playhead, top + gap * 2 + 135, 28, 32, fill="#ffffff")
    elif motif == "screen-distance":
        if phase in {"opening", "problem"}:
            sizes = ((650, 360), (420, 235), (220, 125)) if landscape else ((620, 350), (420, 235), (240, 135))
            for index, (w, h) in enumerate(sizes):
                x, y = (width - w) // 2, 190 + index * (95 if landscape else 185)
                content += f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="24" fill="#111d2d" stroke="{("#ff6b67" if index else "#50d8ff")}" stroke-width="7"/>'
            content += _svg_text(["TEN FEET AWAY", "REMOTE + FOCUS"], width // 2, height - 150, 30, 40, fill="#ffd27c")
        else:
            labels = ("JAVA + CSS", "FORM-FACTOR LAYER", "PHONE · GOOGLE TV · APPLE TV")
            for index, label in enumerate(labels):
                if landscape:
                    x, y, w = 45 + index * 390, 285, 330
                else:
                    x, y, w = 100, 215 + index * 300, 600
                content += _node(x, y, w, 180, label, "same project", "#7ce6ad")
                if index:
                    content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "head-unit-templates":
        labels = ("CARPLAY", "PORTABLE TEMPLATE", "ANDROID AUTO")
        if phase in {"opening", "problem"}:
            labels = ("SWIFT TEMPLATE", "DRIVER-SAFE SCREEN", "KOTLIN TEMPLATE")
        for index, label in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 270, 330
            else:
                x, y, w = 100, 210 + index * 300, 600
            content += _node(x, y, w, 210, label, "list · grid · pane · message", "#50d8ff" if index == 1 else "#ffad57")
            if index:
                content += _arrow(x - 55, y + 105, x, y + 105) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "entitlement-vault":
        labels = (("STORE RECEIPT", "renewals · refunds"), ("ENTITLEMENT", "isEntitled(pro)"), ("SECRET VAULT", "rotation · server-only"))
        if phase in {"opening", "problem"}:
            labels = (("SKU BRANCHES", "Apple vs Google"), ("STALE RECEIPT", "device vs store"), ("KEY IN BINARY", "extractable"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 280, 330
            else:
                x, y, w = 100, 215 + index * 300, 600
            content += _node(x, y, w, 190, label, subtitle, "#7ce6ad" if phase not in {"opening", "problem"} else "#ff6b67")
            if index:
                content += _arrow(x - 55, y + 95, x, y + 95) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "trust-feedback":
        labels = (("NONCE", "from server"), ("SIGNED TOKEN", "opaque on device"), ("SERVER DECISION", "allow · challenge · block"))
        if phase in {"opening", "problem"}:
            labels = (("HOSTILE DEVICE", "root · jailbreak · overlay"), ("CLIENT SIGNAL", "never absolute proof"), ("BAD PROMPT", "rating at the wrong moment"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            content += _node(x, y, w, 180, label, subtitle, "#50d8ff" if index == 1 else "#ffad57")
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "card-table":
        card_w, card_h = (150, 220) if landscape else (155, 225)
        base_y = 230 if landscape else 260
        xs = (180, 390, 660, 870) if landscape else (120, 320, 520)
        ranks = ("A", "10", "J", "19") if phase not in {"opening", "problem"} else ("?", "?", "?", "RULES?")
        for index, x in enumerate(xs):
            y = base_y + (index % 2) * 70
            content += f'<rect x="{x}" y="{y}" width="{card_w}" height="{card_h}" rx="18" fill="#f6f8fa" stroke="#50d8ff" stroke-width="7"/>'
            content += _svg_text([ranks[index]], x + card_w // 2, y + 85, 50 if len(ranks[index]) < 4 else 26, 55, fill="#102033")
        content += _svg_text(["BOARD DATA + PLAIN JAVA RULES"], width // 2, height - 130, 30, 36, fill="#ffd27c")
    elif motif == "input-spectrum":
        labels = ("FINGER", "MOUSE", "STYLUS", "TRACKPAD", "HINGE", "MOTION")
        cols = 3 if landscape else 2
        for index, label in enumerate(labels):
            col, row = index % cols, index // cols
            w = 320 if landscape else 300
            x = 70 + col * (380 if landscape else 350)
            y = 190 + row * (230 if landscape else 260)
            subtitle = "platform branch" if phase in {"opening", "problem"} else "capability + safe default"
            content += _node(x, y, w, 150, label, subtitle, "#ff6b67" if phase in {"opening", "problem"} else "#7ce6ad")
    elif motif == "consent-gate":
        if phase in {"opening", "problem"}:
            content += _node(100 if landscape else 100, 300 if landscape else 230, 360 if landscape else 600, 190, "EVENT", "sent immediately", "#ff6b67")
            content += _arrow(460 if landscape else 400, 395 if landscape else 420, 740 if landscape else 400, 395 if landscape else 650)
            content += _node(740 if landscape else 100, 300 if landscape else 650, 360 if landscape else 600, 190, "ONE VENDOR", "consent not checked", "#ff6b67")
        else:
            labels = ("EVENT", "CONSENT GATE", "YOUR PROVIDERS")
            for index, label in enumerate(labels):
                if landscape:
                    x, y, w = 45 + index * 390, 285, 330
                else:
                    x, y, w = 100, 220 + index * 300, 600
                content += _node(x, y, w, 180, label, "nothing leaves before opt-in", "#7ce6ad" if index == 1 else "#50d8ff")
                if index:
                    content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "release-ladder":
        labels = (("LAST GOOD RELEASE", "pin"), ("CURRENT DEFAULT", "compare"), ("MASTER", "verify fix"))
        if phase in {"opening", "problem"}:
            labels = (("YESTERDAY", "worked"), ("TODAY", "failed"), ("WHICH LAYER?", "unknown"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 90 + index * 360, 430 - index * 100, 290
            else:
                x, y, w = 100, 700 - index * 230, 600
            content += _node(x, y, w, 160, label, subtitle, "#7ce6ad" if phase not in {"opening", "problem"} else "#ffad57")
            if index:
                content += _arrow(x - 70, y + 120, x, y + 80) if landscape else _arrow(400, y + 220, 400, y + 160)
    elif motif == "editor-bridge":
        labels = (("RICH TEXT", "formatting + HTML"), ("SEMANTIC CHANNEL", "commands · queries · events"), ("CODE", "highlighting + diagnostics"))
        if phase in {"opening", "problem"}:
            labels = (("RICH TEXT", "one custom editor"), ("PLATFORM INPUT", "keyboard · focus · IME"), ("CODE", "another custom editor"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            content += _node(x, y, w, 180, label, subtitle, "#ff6b67" if phase in {"opening", "problem"} else "#7ce6ad")
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "map-choice":
        if phase in {"opening", "problem"}:
            labels = (("RASTER TILES", "frozen API"), ("ONE NATIVE SDK", "vendor-specific"), ("MISSING TARGET", "simulator · web · Huawei"))
        else:
            labels = (("MAPSURFACE", "one Java API"), ("MAPVIEW", "Graphics + your tiles"), ("NATIVEMAP", "provider chosen at build"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            color = "#ff6b67" if phase in {"opening", "problem"} else ("#50d8ff" if index == 0 else "#7ce6ad")
            content += _node(x, y, w, 180, label, subtitle, color)
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "sustainable-loop":
        if phase in {"opening", "problem"}:
            labels = (("NO REVENUE", "maintenance stops"), ("LOCK-IN", "freedom disappears"), ("DEVELOPERS", "pay the cost"))
        else:
            labels = (("GPL + EXCEPTION", "apps stay yours"), ("OPTIONAL SERVICES", "earn adoption"), ("FUTURE ENGINEERING", "ports · APIs · fixes"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            content += _node(x, y, w, 180, label, subtitle, "#ff6b67" if phase in {"opening", "problem"} else "#7ce6ad")
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "platformer-editor":
        if phase in {"opening", "problem"}:
            labels = (("SPRITE X/Y", "hidden in code"), ("REBUILD", "for every move"), ("PLAY TEST", "too late"))
        else:
            labels = (("DRAW LEVEL", "tiles · actors · properties"), (".GAME DATA", "readable + diffable"), ("JAVA RULES", "win · power-up · death"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            content += _node(x, y, w, 180, label, subtitle, "#ff6b67" if phase in {"opening", "problem"} else "#7ce6ad")
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "game-modes":
        if phase in {"opening", "problem"}:
            labels = (("OBJECT SETTERS", "layout in Java"), ("RECOMPILE", "to see a tweak"), ("RULES + DATA", "tangled together"))
        else:
            labels = (("PLAIN .GAME", "one scene model"), ("2D · BOARD · 3D", "three authoring modes"), ("RUNNING SCENE", "Java owns behavior"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            content += _node(x, y, w, 180, label, subtitle, "#ff6b67" if phase in {"opening", "problem"} else "#50d8ff")
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "crash-pipeline":
        if phase in {"opening", "problem"}:
            labels = (("NATIVE ADDRESS", "opaque crash"), ("BAD NETWORK", "report disappears"), ("INBOX", "duplicates pile up"))
        else:
            labels = (("STORE + SCRUB", "before upload"), ("SYMBOLICATE", "build symbols in cloud"), ("GITHUB ISSUE", "deduplicated + readable"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            content += _node(x, y, w, 180, label, subtitle, "#ff6b67" if phase in {"opening", "problem"} else "#7ce6ad")
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "watch-branches":
        if phase in {"opening", "problem"}:
            labels = (("PHONE UI", "too much screen"), ("WATCHOS", "different renderer"), ("WEAR OS", "different package"))
        else:
            labels = (("SHARED JAVA", "business logic + APIs"), ("CN.ISWATCH", "compact screen branch"), ("TWO NATIVE TARGETS", "Core Graphics · Android"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            content += _node(x, y, w, 180, label, subtitle, "#ff6b67" if phase in {"opening", "problem"} else "#7ce6ad")
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    elif motif == "native-linux-stack":
        if phase in {"opening", "problem"}:
            labels = (("JAR", "needs a target JVM"), ("NEW GLIBC", "fails on older distros"), ("PACKAGE MATRIX", "deb · rpm · more"))
        else:
            labels = (("JAVA BYTECODE", "one application"), ("PARPARVM + ZIG", "C against old glibc"), ("NATIVE ELF", "x64 · arm64 · no JVM"))
        for index, (label, subtitle) in enumerate(labels):
            if landscape:
                x, y, w = 45 + index * 390, 285, 330
            else:
                x, y, w = 100, 220 + index * 300, 600
            content += _node(x, y, w, 180, label, subtitle, "#ff6b67" if phase in {"opening", "problem"} else "#7ce6ad")
            if index:
                content += _arrow(x - 55, y + 90, x, y + 90) if landscape else _arrow(400, y - 90, 400, y)
    return _frame(width, height, content)


def _evidence_svg(item: dict, orientation: str, index: int) -> str:
    landscape = orientation == "landscape"
    width, height = (1200, 720) if landscape else (800, 1180)
    accent = "#50d8ff" if index == 0 else "#ffad57"
    title_lines = _lines(item["title"], 34 if landscape else 23, 3)
    body_lines = _lines(item["body"], 58 if landscape else 34, 5)
    source_lines = _lines(item["sourceTitle"], 52 if landscape else 30, 2)
    content = (
        f'<rect x="{width*0.07}" y="{height*0.12}" width="{width*0.86}" height="{height*0.74}" '
        f'rx="32" fill="#111d2d" stroke="{accent}" stroke-width="5"/>'
        + _svg_text(["INDEPENDENT CONTEXT"], width // 2, int(height * .21), 24 if landscape else 28, 32, fill=accent)
        + _svg_text(title_lines, width // 2, int(height * .34), 42 if landscape else 46, 56)
        + _svg_text(body_lines, width // 2, int(height * .55), 28 if landscape else 31, 40,
                    fill="#c5dcea", weight=400)
        + _svg_text(source_lines, width // 2, int(height * .80), 24 if landscape else 27, 34,
                    fill=accent, weight=700)
    )
    return _frame(width, height, content)


def _self_export_svg(orientation: str) -> str:
    landscape = orientation == "landscape"
    width, height = (1200, 720) if landscape else (800, 1180)
    labels = (
        ("THIS EPISODE", "Codename One scene frames", "#50d8ff"),
        ("VideoWriter", "H.264 at 30 frames per second", "#7ce6ad"),
        ("AudioMixer", "48 kilohertz narration timeline", "#ffad57"),
        ("DELIVERABLE", "MP4 plus SRT and VTT captions", "#c9a7ff"),
    )
    content = _svg_text(["THE OUTPUT IS THE RUNNING SAMPLE"], width // 2,
                        75 if landscape else 105, 28 if landscape else 34, 38,
                        fill="#ffffff")
    for index, (title, subtitle, color) in enumerate(labels):
        if landscape:
            x, y, w, h = 25 + index * 295, 245, 265, 205
        else:
            x, y, w, h = 100, 175 + index * 235, 600, 165
        content += _node(x, y, w, h, title, subtitle, color)
        if index:
            if landscape:
                content += _arrow(x - 25, y + h // 2, x, y + h // 2)
            else:
                content += _arrow(width // 2, y - 70, width // 2, y)
    content += _svg_text(["generated locally · repeatable in CI"], width // 2,
                        height - 70, 24 if landscape else 30, 34, fill="#a9c6d8")
    return _frame(width, height, content)


def write_assets(project: Path, root: Path, profile: dict) -> list[str]:
    logo = root / "docs" / "website" / "static" / "uploads" / "Codename-One-White-Logo.png"
    shutil.copy2(logo, project / "codename-one-logo.png")
    if profile.get("codeExcerpt"):
        (project / "excerpt.txt").write_text(profile["codeExcerpt"], encoding="utf-8")
    capture_names: list[str] = []
    for index, relative in enumerate(profile["captures"], 1):
        source = root / relative
        suffix = source.suffix.lower()
        target = project / f"capture-{index}{suffix}"
        shutil.copy2(source, target)
        capture_names.append(target.name)
    animated_capture = profile.get("animatedCapture")
    if animated_capture:
        source = root / animated_capture
        extracted: list[str] = []
        with Image.open(source) as animation:
            frame_count = int(getattr(animation, "n_frames", 1))
            for index, fraction in enumerate((0.28, 0.68), 1):
                animation.seek(min(frame_count - 1, round((frame_count - 1) * fraction)))
                frame = animation.convert("RGBA")
                target = project / f"capture-running-{index}.png"
                frame.save(target, optimize=True)
                extracted.append(target.name)
        # Keep the editor as the first workflow proof, then show two distinct
        # moments from the article's actual running capture.  This prevents a
        # gameplay story from degenerating into three static editor screens.
        capture_names[1:1] = extracted
    for index, relative in enumerate(profile.get("svgCaptures", []), 1):
        shutil.copy2(root / relative, project / f"capture-svg-{index}.svg")
    for phase in ("opening", "problem", "solution", "victory"):
        for orientation in ("landscape", "portrait"):
            (project / f"{phase}-{orientation}.svg").write_text(
                _motif_svg(profile, orientation, phase) + "\n", encoding="utf-8"
            )
    for index, item in enumerate(profile["evidence"]):
        for orientation in ("landscape", "portrait"):
            (project / f"evidence-{index + 1}-{orientation}.svg").write_text(
                _evidence_svg(item, orientation, index) + "\n", encoding="utf-8"
            )
    if profile.get("selfDemonstratingExport"):
        for orientation in ("landscape", "portrait"):
            (project / f"self-export-{orientation}.svg").write_text(
                _self_export_svg(orientation) + "\n", encoding="utf-8"
            )
    return capture_names


def _paired_svg(base: str, svg_id: str, *, role: str | None = None,
                source_title: str | None = None, source_url: str | None = None,
                bounds: dict | None = None, portrait_bounds: dict | None = None) -> dict:
    action = {
        "type": "svg.show", "id": svg_id, "atMs": 200,
        "paths": {"landscape": f"{base}-landscape.svg", "portrait": f"{base}-portrait.svg"},
        "bounds": bounds or {"x": 0.04, "y": 0.16, "width": 0.92, "height": 0.79},
        "orientation": {"portrait": portrait_bounds or {"x": 0.055, "y": 0.14, "width": 0.89, "height": 0.82}},
    }
    if role:
        action["role"] = role
    if source_title:
        action["sourceTitle"] = source_title
    if source_url:
        action["sourceUrl"] = source_url
    return action


def _title(action_id: str, text: str) -> dict:
    return {
        "type": "text.show", "id": action_id, "role": "title", "atMs": 0,
        "text": text, "uiid": "VideoTitle", "responsive": True, "maxLines": 3,
        "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.13},
        "orientation": {"portrait": {"x": 0.06, "y": 0.025, "width": 0.88, "height": 0.12}},
    }


def _capture_actions(names: list[str], prefix: str) -> list[dict]:
    selected = names[:3]
    actions: list[dict] = []
    landscape_bounds = [
        {"x": 0.035, "y": 0.19, "width": 0.44, "height": 0.68},
        {"x": 0.52, "y": 0.19, "width": 0.44, "height": 0.32},
        {"x": 0.52, "y": 0.55, "width": 0.44, "height": 0.32},
    ]
    portrait_bounds = [
        {"x": 0.06, "y": 0.16, "width": 0.88, "height": 0.25},
        {"x": 0.06, "y": 0.43, "width": 0.88, "height": 0.25},
        {"x": 0.06, "y": 0.70, "width": 0.88, "height": 0.25},
    ]
    if len(selected) == 1:
        landscape_bounds[0] = {"x": 0.12, "y": 0.17, "width": 0.76, "height": 0.72}
        portrait_bounds[0] = {"x": 0.07, "y": 0.17, "width": 0.86, "height": 0.70}
    for index, name in enumerate(selected):
        actions.append({
            "type": "image.show", "id": f"{prefix}-{index + 1}", "role": "proof",
            "kind": "capture", "atMs": 250 + index * 350, "path": name,
            "bounds": landscape_bounds[index],
            "orientation": {"portrait": portrait_bounds[index]},
        })
    return actions


def _editorial(profile: dict) -> dict:
    return {
        "storyType": profile["storyType"], "status": "approved",
        "topicTerms": profile["topicTerms"],
        "proof": ["canonical article source", "independent technical documentation",
                  "article code", "canonical product captures"],
        "humanBeat": profile["humanBeat"],
        "visualIdentity": profile["visualIdentity"],
        "bespokeVisualization": profile["bespokeVisualization"],
        "heroJourney": profile["heroJourney"],
        "problemDimensions": profile["problemDimensions"],
        "resolutionMap": profile["resolutionMap"],
    }


def scripts(slug: str, title: str, output: dict, narration: dict,
            profile: dict, capture_names: list[str]) -> tuple[dict, dict]:
    language = {
        "standalone-codename-one-settings": "bash",
        "automated-store-submissions": "text",
        "standalone-certificate-wizard": "bash",
        "game-builder-3d-dungeon": "java",
        "videoio-audio-mixer-whisper": "java",
        "apple-tv-and-android-tv": "css",
        "carplay-android-auto-codename-one": "java",
        "commerce-secrets-without-iap-tax": "java",
        "device-integrity-and-app-review": "java",
        "game-builder-board-game": "java",
        "motion-input-form-factors": "java",
        "privacy-first-analytics": "java",
        "versioned-builds-master": "properties",
    }.get(slug, "java")
    evidence_scenes = []
    for index, item in enumerate(profile["evidence"], 1):
        evidence_scenes.append({
            "id": f"independent-context-{index}", "durationMs": 14_000,
            "storyBeats": ["difficulty"], "actions": [
                _paired_svg(f"evidence-{index}", f"evidence-{index}", role="evidence",
                            source_title=item["sourceTitle"], source_url=item["sourceUrl"]),
                cue(item["body"]),
            ],
        })
    splash = {
        "id": "who-and-what", "durationMs": 9_000, "storyBeats": ["identity"], "actions": [
            {"type": "image.show", "id": "brand", "role": "brand", "kind": "illustration",
             "atMs": 150, "path": "codename-one-logo.png",
             "bounds": {"x": 0.045, "y": 0.04, "width": 0.12, "height": 0.04},
             "orientation": {"portrait": {"x": 0.06, "y": 0.025, "width": 0.18, "height": 0.027}}},
            _paired_svg("opening", "opening-motif",
                        bounds={"x": 0.58, "y": 0.13, "width": 0.38, "height": 0.74},
                        portrait_bounds={"x": 0.055, "y": 0.20, "width": 0.89, "height": 0.73}),
            {**_title("opening-title", profile["openingTitle"]),
             "bounds": {"x": 0.055, "y": 0.23, "width": 0.49, "height": 0.34},
             "orientation": {"portrait": {"x": 0.07, "y": 0.06, "width": 0.86, "height": 0.12}}},
            cue(profile["openingNarration"], 650, "opening-voice"),
        ],
    }
    problem = {
        "id": "the-real-world-problem", "durationMs": 14_000, "storyBeats": ["problem"], "actions": [
            _paired_svg("problem", "problem-motif"), _title("problem-title", profile["problemTitle"]),
            cue(profile["problemNarration"]),
        ],
    }
    intervention = {
        "id": "the-change-in-mechanism", "durationMs": 17_000, "storyBeats": ["intervention"], "actions": [
            _paired_svg("solution", "solution-motif"),
            {"type": "transition", "target": "solution-motif", "atMs": 200,
             "durationMs": 1_000, "effect": "morph", "easing": "ease-out"},
            _title("intervention-title", profile["interventionTitle"]),
            cue(profile["interventionNarration"]),
        ],
    }
    svg_capture_names = [
        f"capture-svg-{index}.svg"
        for index, _ in enumerate(profile.get("svgCaptures", []), 1)
    ]
    proof_visual = (_paired_svg(
        "self-export", "proof-capture", role="proof",
        bounds={"x": 0.55, "y": 0.17, "width": 0.42, "height": 0.72},
        portrait_bounds={"x": 0.055, "y": 0.54, "width": 0.89, "height": 0.38},
    ) if profile.get("selfDemonstratingExport") else {
        "type": "svg.show", "id": "proof-capture", "role": "proof", "atMs": 450,
        "path": svg_capture_names[0],
        "bounds": {"x": 0.55, "y": 0.17, "width": 0.42, "height": 0.72},
        "orientation": {"portrait": {"x": 0.055, "y": 0.54, "width": 0.89, "height": 0.38}},
    } if svg_capture_names else {
        "type": "image.show", "id": "proof-capture", "role": "proof", "kind": "capture",
        "atMs": 450, "path": capture_names[0],
        "bounds": {"x": 0.55, "y": 0.17, "width": 0.42, "height": 0.72},
        "orientation": {"portrait": {"x": 0.055, "y": 0.54, "width": 0.89, "height": 0.38}},
    })
    proof = {
        "id": "source-and-product-proof", "durationMs": 25_000, "storyBeats": ["proof"],
        "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"}, "actions": [
            _title("proof-title", profile["proofTitle"]),
            {"type": "code.show", "id": "source-code", "role": "code", "atMs": 250,
             "language": language, "path": "excerpt.txt",
             "bounds": {"x": 0.035, "y": 0.17, "width": 0.48, "height": 0.72},
             "orientation": {"portrait": {"x": 0.055, "y": 0.15, "width": 0.89, "height": 0.35}}},
            proof_visual,
            {"type": "focus.show", "id": "source-focus", "target": "source-code", "atMs": 4_000,
             "relativeBounds": {"x": 0.02, "y": 0.08, "width": 0.96, "height": 0.50},
             "label": profile["interventionTitle"], "color": "50d8ff"},
            cue(profile["proofNarration"]),
        ],
    }
    workflow_visuals = _capture_actions(capture_names, "canonical-capture")
    if svg_capture_names:
        workflow_visuals = []
        landscape_bounds = [
            {"x": 0.035, "y": 0.19, "width": 0.44, "height": 0.68},
            {"x": 0.52, "y": 0.19, "width": 0.44, "height": 0.32},
            {"x": 0.52, "y": 0.55, "width": 0.44, "height": 0.32},
        ]
        portrait_bounds = [
            {"x": 0.06, "y": 0.16, "width": 0.88, "height": 0.25},
            {"x": 0.06, "y": 0.43, "width": 0.88, "height": 0.25},
            {"x": 0.06, "y": 0.70, "width": 0.88, "height": 0.25},
        ]
        for index, name in enumerate(svg_capture_names[:3]):
            workflow_visuals.append({
                "type": "svg.show", "id": f"canonical-capture-{index + 1}",
                "role": "proof", "atMs": 250 + index * 350, "path": name,
                "bounds": landscape_bounds[index],
                "orientation": {"portrait": portrait_bounds[index]},
            })
    captures = {
        "id": "canonical-captures", "durationMs": 19_000, "storyBeats": ["proof"], "actions": [
            _title("captures-title", profile.get("capturesTitle", "What changes in the actual workflow")),
            *workflow_visuals,
            {"type": "transition", "target": "canonical-capture-1", "atMs": 250,
             "durationMs": 850, "effects": {"landscape": "slide-right", "portrait": "slide-up"},
             "easing": "ease-out"},
            cue(profile["captureNarration"]),
        ],
    }
    victory = {
        "id": "developer-victory", "durationMs": 16_000, "storyBeats": ["victory"], "actions": [
            _paired_svg("victory", "victory-motif"), _title("victory-title", profile["victoryTitle"]),
            cue(profile["victoryNarration"]),
        ],
    }
    outro = {
        "id": "outro", "durationMs": 15_000, "storyBeats": ["outro"], "actions": [
            {"type": "outro.show", "id": "next", "atMs": 0, "durationMs": 1_100,
             "eyebrow": "CODENAME ONE · ENGINEERING STORY",
             "title": profile["outroTitle"],
             "subtitle": "Source, captions, and the full article are linked below.",
             "prompt": profile["outroPrompt"],
             "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}},
            cue(profile["outroPrompt"]),
        ],
    }
    long_scenes = [splash, problem, *evidence_scenes, intervention, proof, captures, victory, outro]

    combined_evidence_actions = []
    for index, item in enumerate(profile["evidence"], 1):
        action = _paired_svg(
            f"evidence-{index}", f"short-evidence-{index}", role="evidence",
            source_title=item["sourceTitle"], source_url=item["sourceUrl"],
            bounds={"x": 0.05, "y": 0.08 + (index - 1) * 0.45, "width": 0.90, "height": 0.40},
            portrait_bounds={"x": 0.055, "y": 0.07 + (index - 1) * 0.46, "width": 0.89, "height": 0.42},
        )
        action["paths"]["portrait"] = f"evidence-{index}-landscape.svg"
        combined_evidence_actions.append(action)
    short_splash = {**splash, "id": "short-who-and-what", "durationMs": 8_000}
    short_problem = {**problem, "id": "short-real-world-problem", "durationMs": 11_000}
    short_evidence = {
        "id": "short-independent-context", "durationMs": 15_000, "storyBeats": ["difficulty"],
        "actions": [*combined_evidence_actions,
                    cue("The outside documentation exposes both sides of the same problem: "
                        + profile["evidence"][0]["body"] + " " + profile["evidence"][1]["body"])],
    }
    short_intervention = {**intervention, "id": "short-change-in-mechanism", "durationMs": 15_000}
    short_proof = {**proof, "id": "short-source-and-product-proof", "durationMs": 23_000}
    short_captures = {**captures, "id": "short-canonical-captures", "durationMs": 16_000}
    short_victory = {**victory, "id": "short-developer-victory", "durationMs": 13_000}
    short_outro = {**outro, "id": "short-outro", "durationMs": 15_000}
    short_scenes = [short_splash, short_problem, short_evidence, short_intervention,
                    short_proof, short_captures, short_victory, short_outro]

    story_output = dict(output)
    if profile.get("selfDemonstratingExport"):
        story_output["encodingPipeline"] = "videoio"
    common = {"schemaVersion": 1, "editorial": _editorial(profile), "output": story_output, "narration": narration}
    return (
        {**common, "id": slug, "title": title, "scenes": long_scenes},
        {**common, "id": f"{slug}-short", "title": profile["openingTitle"].replace("\n", " "), "scenes": short_scenes},
    )


def red_team(title: str, profile: dict) -> str:
    problems = ", ".join(profile["problemDimensions"])
    sources = "\n".join(f"- {item['sourceTitle']}: {item['sourceUrl']}" for item in profile["evidence"])
    return f"""# Positioning red-team: {title}

## Verdict

SHIP

## Act 1 audit

- The first frame identifies Codename One as a Java native-app platform without turning the logo into the subject.
- The topic is named in the first narration cue, after a short visual orientation beat.
- The recurring visual is article-specific: {profile['visualIdentity']}

## Act 2 audit

- The industry problem is established before the product intervention.
- Codename One is quarantined after the identity splash until the mechanism changes.
- The difficulty is concrete and multi-dimensional: {problems}.
- The two evidence cards use independent domains and describe verifiable mechanics rather than invented testimonials.

## Act 3 audit

- Every declared problem has one explicit resolution and one named proof artifact.
- The canonical article code appears before the workflow is celebrated.
- Product captures come from the article assets and are not presented as live interaction.
- The developer remains the protagonist; no rescue slogan or unsupported superiority claim appears.

## Evidence audit

{sources}
- Product proof also includes the canonical article, its source excerpt, and its original captures.

## Interaction audit

- The focus callout points at source code while narration explains the mechanism.
- Captures transition as workflow states; no pointer pretends a static screenshot is a running demo.
- The episode uses illustration, bespoke visualization, code, captures, transitions, and a source focus treatment.

## Hostile-viewer check

- A newcomer can state the real-world problem before the product appears.
- A skeptical developer sees exact code and canonical captures instead of a corporate feature list.
- The story makes one source-backed promise and shows how the implementation earns it.

## Highest-leverage risk

The fixed hero-journey skeleton must not flatten the subject. Preserve this profile's human consequence, motif, evidence, proof sequence, and question whenever timing or layout is revised.
"""


def write_thumbnail(project: Path, profile: dict, capture_names: list[str]) -> None:
    canvas = Image.new("RGB", (1280, 720), (6, 17, 31))
    draw = ImageDraw.Draw(canvas, "RGBA")
    for y in range(720):
        color = (6 + y // 100, 17 + y // 25, 31 + y // 18)
        draw.line((0, y, 1280, y), fill=(*color, 255))
    bold = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
    regular = "/System/Library/Fonts/Supplemental/Arial.ttf"
    label_font = ImageFont.truetype(bold, 24)
    label = "CODENAME ONE"
    bbox = draw.textbbox((0, 0), label, font=label_font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    pill = (62, 52, 62 + tw + 138, 112)
    tx = pill[0] + (pill[2] - pill[0] - tw) / 2 - bbox[0]
    ty = pill[1] + (pill[3] - pill[1] - th) / 2 - bbox[1]
    draw.rounded_rectangle(pill, radius=28, fill=(13, 43, 66, 240), outline=(80, 216, 255, 220), width=2)
    draw.text((tx, ty), label, font=label_font, fill=(193, 239, 255, 255))
    rendered = draw.textbbox((tx, ty), label, font=label_font)
    if min(rendered[0] - pill[0], pill[2] - rendered[2]) < 65:
        raise ValueError("thumbnail label is too close to its pill edge")

    thumbnail_title = profile.get("thumbnailTitle")
    if thumbnail_title:
        lines = [line.strip() for line in thumbnail_title.splitlines() if line.strip()]
    else:
        lines = _lines(profile["openingTitle"].replace("\n", " "), 20, 4)
    if not lines or len(lines) > 4:
        raise ValueError("thumbnail headline must contain between one and four lines")
    font = ImageFont.truetype(bold, 66 if len(lines) <= 2 else (58 if len(lines) == 3 else 50))
    y = 180
    for line in lines:
        headline_box = draw.textbbox((64, y), line.upper(), font=font)
        if headline_box[2] > 770:
            raise ValueError(f"thumbnail headline overlaps the visual: {line}")
        draw.text((64, y), line.upper(), font=font, fill=(255, 255, 255, 255))
        y += 78 if len(lines) < 4 else 64
    subtitle_font = ImageFont.truetype(regular, 24)
    thumbnail_subtitle = profile.get("thumbnailSubtitle", profile["humanBeat"])
    for index, line in enumerate(_lines(thumbnail_subtitle, 47, 3)):
        draw.text((66, 522 + index * 32), line, font=subtitle_font,
                  fill=(184, 213, 229, 255))

    x, y = 810, 92
    draw.rounded_rectangle((x - 16, y - 16, x + 456, y + 606), radius=30,
                           fill=(80, 216, 255, 40), outline=(80, 216, 255, 225), width=4)
    thumbnail_visual = profile.get("thumbnailVisual")
    if thumbnail_visual == "self-export":
        small_bold = ImageFont.truetype(bold, 25)
        small = ImageFont.truetype(regular, 19)
        draw.text((x + 28, y + 24), "VIDEOIO OUTPUT", font=small_bold,
                  fill=(126, 230, 255, 255))
        draw.rounded_rectangle((x + 24, y + 76, x + 416, y + 316), radius=22,
                               fill=(4, 11, 20, 255), outline=(110, 230, 173, 255), width=3)
        draw.text((x + 60, y + 104), "THIS EPISODE", font=small_bold,
                  fill=(255, 255, 255, 255))
        track_colors = ((80, 216, 255), (124, 230, 173), (255, 173, 87))
        track_names = ("FRAMES", "AUDIO", "CAPTIONS")
        for index, (name, color) in enumerate(zip(track_names, track_colors)):
            ty = y + 166 + index * 48
            draw.text((x + 48, ty - 8), name, font=ImageFont.truetype(bold, 14),
                      fill=(*color, 255))
            draw.line((x + 138, ty, x + 382, ty), fill=(*color, 255), width=5)
            for tick in range(5):
                px = x + 146 + tick * 56
                draw.ellipse((px - 5, ty - 5, px + 5, ty + 5), fill=(*color, 255))
        draw.line((x + 306, y + 148, x + 306, y + 294), fill=(255, 255, 255, 255), width=4)
        draw.polygon(((x + 208, y + 338), (x + 232, y + 338), (x + 220, y + 360)),
                     fill=(80, 216, 255, 255))
        draw.rounded_rectangle((x + 58, y + 382, x + 382, y + 464), radius=18,
                               fill=(13, 43, 66, 255), outline=(80, 216, 255, 255), width=3)
        writer_label = "VideoWriter  →  MP4"
        writer_box = draw.textbbox((0, 0), writer_label, font=small_bold)
        draw.text((x + 220 - (writer_box[2] - writer_box[0]) / 2, y + 408), writer_label,
                  font=small_bold, fill=(255, 255, 255, 255))
        draw.text((x + 84, y + 512), "THE FILE IS THE PROOF", font=small_bold,
                  fill=(255, 210, 124, 255))
        draw.text((x + 91, y + 552), "30 FPS · 48 kHz · SRT + VTT", font=small,
                  fill=(184, 213, 229, 255))
    elif thumbnail_visual in {
        "trust-shield", "tv-targets", "entitlement-vault", "input-spectrum",
        "consent-gate", "release-ladder", "editor-bridge", "map-choice",
        "sustainable-loop", "platformer-editor", "game-modes",
        "crash-pipeline", "watch-branches", "native-linux-stack"
    }:
        small_bold = ImageFont.truetype(bold, 23)
        small = ImageFont.truetype(regular, 17)
        diagrams = {
            "trust-shield": (
                ("HOSTILE DEVICE", "signal only", (255, 173, 87)),
                ("SIGNED TOKEN", "opaque on device", (80, 216, 255)),
                ("SERVER DECISION", "allow · challenge · block", (124, 230, 173)),
            ),
            "tv-targets": (
                ("PHONE", "touch · close", (255, 173, 87)),
                ("GOOGLE TV", "remote · ten feet", (80, 216, 255)),
                ("APPLE TV", "focus · ten feet", (124, 230, 173)),
            ),
            "entitlement-vault": (
                ("STORE RECEIPT", "renewals · refunds", (255, 173, 87)),
                ("ENTITLEMENT", "is entitled now?", (80, 216, 255)),
                ("SECRET VAULT", "server only", (124, 230, 173)),
            ),
            "input-spectrum": (
                ("STYLUS", "pressure · tilt · eraser", (255, 173, 87)),
                ("TRACKPAD", "wheel · pinch · rotate", (80, 216, 255)),
                ("HINGE + MOTION", "posture · sensors", (124, 230, 173)),
            ),
            "consent-gate": (
                ("EVENT", "stops locally", (255, 173, 87)),
                ("CONSENT GATE", "opt-in required", (124, 230, 173)),
                ("PROVIDERS", "receive allowed events", (80, 216, 255)),
            ),
            "release-ladder": (
                ("PIN RELEASE", "known framework state", (255, 173, 87)),
                ("VERIFY MASTER", "test the landed fix", (80, 216, 255)),
                ("SHIP RELEASED", "production stays stable", (124, 230, 173)),
            ),
            "editor-bridge": (
                ("RICH TEXT", "formatted HTML", (255, 173, 87)),
                ("ONE CHANNEL", "commands · queries · events", (80, 216, 255)),
                ("CODE", "highlighting · diagnostics", (124, 230, 173)),
            ),
            "map-choice": (
                ("MAPSURFACE", "one Java API", (255, 173, 87)),
                ("MAPVIEW", "portable vector pixels", (80, 216, 255)),
                ("NATIVEMAP", "injected provider", (124, 230, 173)),
            ),
            "sustainable-loop": (
                ("OPEN CORE", "GPL + Classpath Exception", (255, 173, 87)),
                ("OPTIONAL VALUE", "services earn adoption", (80, 216, 255)),
                ("MORE ENGINEERING", "ports · APIs · fixes", (124, 230, 173)),
            ),
            "platformer-editor": (
                ("DRAW LEVEL", "tiles · actors · properties", (255, 173, 87)),
                ("SAVE DATA", ".game stays diffable", (80, 216, 255)),
                ("CODE RULES", "win · power-up · death", (124, 230, 173)),
            ),
            "game-modes": (
                ("ONE SCENE FILE", "plain project data", (255, 173, 87)),
                ("2D · BOARD · 3D", "three visual modes", (80, 216, 255)),
                ("JAVA RULES", "behavior stays yours", (124, 230, 173)),
            ),
            "crash-pipeline": (
                ("STORE + SCRUB", "survive offline", (255, 173, 87)),
                ("SYMBOLICATE", "use build symbols", (80, 216, 255)),
                ("GITHUB ISSUE", "readable · deduplicated", (124, 230, 173)),
            ),
            "watch-branches": (
                ("SHARED JAVA", "logic + APIs", (255, 173, 87)),
                ("COMPACT UI", "CN.isWatch()", (80, 216, 255)),
                ("TWO TARGETS", "watchOS · Wear OS", (124, 230, 173)),
            ),
            "native-linux-stack": (
                ("JAVA BYTECODE", "one application", (255, 173, 87)),
                ("PARPARVM + ZIG", "C against old glibc", (80, 216, 255)),
                ("NATIVE ELF", "x64 · arm64 · no JVM", (124, 230, 173)),
            ),
        }
        nodes = diagrams[thumbnail_visual]
        for index, (heading, detail, color) in enumerate(nodes):
            top = y + 42 + index * 172
            draw.rounded_rectangle((x + 36, top, x + 404, top + 116), radius=18,
                                   fill=(13, 31, 48, 255), outline=(*color, 255), width=3)
            heading_box = draw.textbbox((0, 0), heading, font=small_bold)
            draw.text((x + 220 - (heading_box[2] - heading_box[0]) / 2, top + 26),
                      heading, font=small_bold, fill=(255, 255, 255, 255))
            detail_box = draw.textbbox((0, 0), detail, font=small)
            draw.text((x + 220 - (detail_box[2] - detail_box[0]) / 2, top + 70),
                      detail, font=small, fill=(184, 213, 229, 255))
            if index < len(nodes) - 1:
                center = x + 220
                draw.line((center, top + 116, center, top + 166), fill=(255, 200, 105, 255), width=4)
                draw.polygon(((center, top + 166), (center - 8, top + 150),
                              (center + 8, top + 150)), fill=(255, 200, 105, 255))
    else:
        capture_index = int(profile.get("thumbnailCaptureIndex", -1))
        source = Image.open(project / capture_names[capture_index]).convert("RGB")
        centering = tuple(profile.get("thumbnailCentering", (0.5, 0.5)))
        if len(centering) != 2 or any(value < 0 or value > 1 for value in centering):
            raise ValueError("thumbnailCentering must contain two values between zero and one")
        fitted = ImageOps.fit(source, (440, 590), method=Image.Resampling.LANCZOS,
                              centering=centering)
        canvas.paste(fitted, (x, y))
    canvas.save(project / "thumbnail.jpg", quality=93, optimize=True)
