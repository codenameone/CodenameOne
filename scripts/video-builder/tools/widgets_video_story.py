#!/usr/bin/env python3
"""Article-specific video package for widgets, Live Activities, and Dynamic Island."""

from __future__ import annotations

import html
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


SLUG = "widgets-live-activities-dynamic-island"
DEMO_CLASS = "com.codename1.videobuilder.demos.SurfacesDemoScene"


def cue(text: str, at: int = 0, cue_id: str = "voice") -> dict:
    return {"type": "narration.cue", "id": cue_id, "atMs": at, "text": text,
            "overflow": "extend"}


def svg_document(width: int, height: int, body: str) -> str:
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#081321"/><stop offset="1" stop-color="#102b42"/></linearGradient>
  <filter id="glow"><feGaussianBlur stdDeviation="9" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
</defs>
<rect width="100%" height="100%" rx="36" fill="url(#bg)"/>
{body}
</svg>"""


def text(x: int, y: int, value: str, size: int = 38, color: str = "#f5f8fb",
         weight: int = 700, anchor: str = "start") -> str:
    return (f'<text x="{x}" y="{y}" fill="{color}" font-family="Arial, sans-serif" '
            f'font-size="{size}" font-weight="{weight}" text-anchor="{anchor}">'
            f'{html.escape(value)}</text>')


def card(x: int, y: int, w: int, h: int, title: str, subtitle: str,
         accent: str = "#ff8a34") -> str:
    return f"""<g>
<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="28" fill="#142438" stroke="#425b72" stroke-width="3"/>
<rect x="{x + 24}" y="{y + 24}" width="12" height="{h - 48}" rx="6" fill="{accent}"/>
{text(x + 62, y + 66, title, 32)}
{text(x + 62, y + 112, subtitle, 23, '#9fb5c8', 400)}
</g>"""


def story_svgs() -> dict[str, str]:
    assets: dict[str, str] = {}
    for portrait in (False, True):
        suffix = "portrait" if portrait else "landscape"
        w, h = ((900, 1200) if portrait else (1400, 760))

        if portrait:
            intro_body = f"""
{text(450, 92, 'APP WINDOW', 24, '#7890a4', 700, 'middle')}
<rect x="170" y="135" width="560" height="650" rx="62" fill="#101f31" stroke="#4b657d" stroke-width="5"/>
<rect x="215" y="210" width="470" height="330" rx="36" fill="#172a40"/>
<rect x="270" y="275" width="92" height="92" rx="22" fill="#ff8a34"/>
{text(395, 320, 'Out for delivery', 34)}
{text(395, 380, '03:42', 54, '#50d8ff')}
<path d="M450 560 C450 700 450 770 450 900" fill="none" stroke="#ff8a34" stroke-width="11" stroke-linecap="round" filter="url(#glow)"/>
<rect x="120" y="850" width="660" height="210" rx="70" fill="#02070c" stroke="#ff8a34" stroke-width="4"/>
{text(205, 935, 'LIVE ACTIVITY', 28, '#ff8a34')}
{text(205, 1000, 'ETA keeps moving', 42)}
"""
        else:
            intro_body = f"""
{text(340, 92, 'RUNNING APP', 24, '#7890a4', 700, 'middle')}
<rect x="105" y="125" width="470" height="545" rx="55" fill="#101f31" stroke="#4b657d" stroke-width="5"/>
<rect x="150" y="205" width="380" height="280" rx="34" fill="#172a40"/>
<rect x="195" y="258" width="82" height="82" rx="21" fill="#ff8a34"/>
{text(308, 302, 'Out for delivery', 30)}
{text(308, 365, '03:42', 52, '#50d8ff')}
<path d="M575 390 C740 390 785 390 905 390" fill="none" stroke="#ff8a34" stroke-width="11" stroke-linecap="round" filter="url(#glow)"/>
<rect x="855" y="255" width="460" height="270" rx="95" fill="#02070c" stroke="#ff8a34" stroke-width="4"/>
{text(925, 350, 'LIVE ACTIVITY', 27, '#ff8a34')}
{text(925, 425, 'ETA keeps moving', 40)}
{text(340, 630, 'PROCESS STOPS', 25, '#ff6d78', 700, 'middle')}
"""
        assets[f"surfaces-intro-{suffix}.svg"] = svg_document(w, h, intro_body)

        if portrait:
            desire_body = f"""
<circle cx="450" cy="220" r="105" fill="#173049" stroke="#50d8ff" stroke-width="4"/>
<path d="M410 210 l45 -28 l45 28 v58 l-45 28 l-45 -28z" fill="#ff8a34"/>
{text(450, 395, 'The useful moment', 45, '#f5f8fb', 700, 'middle')}
{text(450, 450, 'happens outside the app', 38, '#a9bed0', 400, 'middle')}
{card(105, 555, 690, 190, 'One glance', 'No reopen. No refresh ritual.')}
{card(105, 785, 690, 190, 'One changing ETA', 'Visible while life keeps moving.', '#50d8ff')}
"""
        else:
            desire_body = f"""
<circle cx="275" cy="380" r="145" fill="#173049" stroke="#50d8ff" stroke-width="4"/>
<path d="M220 365 l60 -36 l60 36 v74 l-60 36 l-60 -36z" fill="#ff8a34"/>
{text(510, 250, 'The useful moment happens', 53)}
{text(510, 316, 'outside the app', 53, '#50d8ff')}
{card(510, 385, 740, 150, 'One glance', 'No reopen. No refresh ritual.')}
{card(510, 560, 740, 150, 'One changing ETA', 'Visible while life keeps moving.', '#50d8ff')}
"""
        assets[f"surfaces-desire-{suffix}.svg"] = svg_document(w, h, desire_body)

        if portrait:
            dead_body = f"""
{text(450, 90, 'THE APP PROCESS CAN BE GONE', 30, '#ff6d78', 700, 'middle')}
{card(120, 155, 660, 155, 'Component tree', 'not running', '#ff6d78')}
{card(120, 350, 660, 155, 'Event dispatch thread', 'not running', '#ff6d78')}
{card(120, 545, 660, 155, 'Java listener', 'not waiting', '#ff6d78')}
<path d="M450 730 v105" stroke="#60758a" stroke-width="8" stroke-dasharray="15 14"/>
{card(120, 870, 660, 175, 'System surface', 'still visible and interactive', '#7ce6ad')}
"""
        else:
            dead_body = f"""
{text(700, 92, 'THE APP PROCESS CAN BE GONE', 32, '#ff6d78', 700, 'middle')}
{card(70, 165, 365, 160, 'Component tree', 'not running', '#ff6d78')}
{card(70, 365, 365, 160, 'Event thread', 'not running', '#ff6d78')}
{card(70, 565, 365, 140, 'Java listener', 'not waiting', '#ff6d78')}
<path d="M490 405 H780" stroke="#60758a" stroke-width="8" stroke-dasharray="15 14"/>
{card(830, 255, 500, 300, 'System surface', 'still visible\nand interactive', '#7ce6ad')}
"""
        assets[f"dead-process-{suffix}.svg"] = svg_document(w, h, dead_body)

        if portrait:
            split_body = f"""
{text(450, 90, 'ONE EXPERIENCE, DIFFERENT RULES', 28, '#f5f8fb', 700, 'middle')}
{card(95, 150, 710, 180, 'Apple', 'timelines · ActivityKit · regions')}
{card(95, 370, 710, 180, 'Android', 'RemoteViews · receivers · notifications', '#7ce6ad')}
{card(95, 590, 710, 180, 'Desktop + simulator', 'preview · sizing · time control', '#50d8ff')}
<path d="M450 810 v165" stroke="#ff8a34" stroke-width="8" stroke-dasharray="15 14"/>
{text(450, 1035, 'Taps must survive a cold start', 35, '#ffb073', 700, 'middle')}
"""
        else:
            split_body = f"""
{text(700, 92, 'ONE EXPERIENCE, DIFFERENT RULES', 32, '#f5f8fb', 700, 'middle')}
{card(55, 180, 390, 330, 'Apple', 'Widget timelines\nActivityKit regions')}
{card(505, 180, 390, 330, 'Android', 'RemoteViews\nreceivers + notifications', '#7ce6ad')}
{card(955, 180, 390, 330, 'Desktop', 'preview + sizing\ntime control', '#50d8ff')}
<path d="M250 565 H1150" stroke="#ff8a34" stroke-width="7" stroke-dasharray="15 14"/>
{text(700, 650, 'Taps and time changes must survive a cold start', 34, '#ffb073', 700, 'middle')}
"""
        assets[f"platform-rules-{suffix}.svg"] = svg_document(w, h, split_body)

        if portrait:
            insight_body = f"""
{text(450, 95, 'STOP SHIPPING A LIVE UI TREE', 28, '#7890a4', 700, 'middle')}
<g opacity=".55">{card(120, 150, 660, 160, 'Component + listener', 'requires a living process', '#ff6d78')}</g>
<path d="M450 350 v115" stroke="#50d8ff" stroke-width="10" marker-end="url(#none)"/>
{card(120, 500, 660, 200, 'Serializable layout', 'small node catalog + state keys', '#50d8ff')}
{card(120, 750, 660, 200, 'Dated timeline', 'Preparing → Out → Arriving → Delivered', '#ff8a34')}
{text(450, 1035, 'DATA OUTLIVES THE PROCESS', 34, '#7ce6ad', 700, 'middle')}
"""
        else:
            insight_body = f"""
<g opacity=".50">{card(70, 230, 390, 260, 'Live UI tree', 'component + listener\nneeds a process', '#ff6d78')}</g>
<path d="M500 360 H650" stroke="#50d8ff" stroke-width="10"/>
{card(675, 125, 650, 220, 'Serializable layout', 'surface nodes + state keys', '#50d8ff')}
{card(675, 400, 650, 220, 'Dated timeline', 'Preparing → Out → Arriving → Delivered', '#ff8a34')}
{text(700, 700, 'DATA OUTLIVES THE PROCESS', 35, '#7ce6ad', 700, 'middle')}
"""
        assets[f"data-not-ui-{suffix}.svg"] = svg_document(w, h, insight_body)

        if portrait:
            constraint_body = f"""
{text(450, 90, 'SYSTEM SURFACES HAVE RULES', 29, '#f5f8fb', 700, 'middle')}
<rect x="95" y="160" width="710" height="820" rx="55" fill="#12253a" stroke="#425b72" stroke-width="4"/>
{card(145, 220, 610, 150, 'Text + images', 'serializable')}
{card(145, 410, 610, 150, 'Rows + columns', 'portable layout', '#50d8ff')}
{card(145, 600, 610, 150, 'Progress + actions', 'native behavior', '#7ce6ad')}
<rect x="210" y="815" width="480" height="92" rx="30" fill="#06101b" stroke="#ff8a34" stroke-width="4"/>
{text(450, 875, 'COMMON SYSTEM FLOOR', 26, '#ffb073', 700, 'middle')}
"""
        else:
            constraint_body = f"""
{text(700, 95, 'SYSTEM SURFACES HAVE TIGHTER RULES THAN APP SCREENS', 31, '#f5f8fb', 700, 'middle')}
<rect x="100" y="170" width="1200" height="430" rx="55" fill="#12253a" stroke="#425b72" stroke-width="4"/>
{card(150, 235, 300, 250, 'Text + images', 'serializable')}
{card(550, 235, 300, 250, 'Rows + columns', 'portable layout', '#50d8ff')}
{card(950, 235, 300, 250, 'Progress + actions', 'native behavior', '#7ce6ad')}
<rect x="390" y="635" width="620" height="78" rx="28" fill="#06101b" stroke="#ff8a34" stroke-width="4"/>
{text(700, 687, 'COMMON SYSTEM FLOOR', 28, '#ffb073', 700, 'middle')}
"""
        assets[f"surface-constraints-{suffix}.svg"] = svg_document(w, h, constraint_body)

        if portrait:
            victory_body = f"""
{text(450, 95, 'THE APP SLEEPS', 31, '#7890a4', 700, 'middle')}
<circle cx="450" cy="270" r="115" fill="#101f31" stroke="#60758a" stroke-width="5"/>
{text(450, 285, 'z z z', 54, '#60758a', 700, 'middle')}
<path d="M450 420 v95" stroke="#ff8a34" stroke-width="9"/>
{card(105, 545, 690, 180, 'Home + lock screen', 'ETA 00:48', '#ff8a34')}
{card(105, 765, 690, 180, 'Dynamic Island + notification', 'Arriving now', '#50d8ff')}
{text(450, 1040, 'THE EXPERIENCE KEEPS MOVING', 29, '#7ce6ad', 700, 'middle')}
"""
        else:
            victory_body = f"""
{text(240, 130, 'THE APP SLEEPS', 30, '#7890a4', 700, 'middle')}
<circle cx="240" cy="355" r="135" fill="#101f31" stroke="#60758a" stroke-width="5"/>
{text(240, 372, 'z z z', 54, '#60758a', 700, 'middle')}
<path d="M405 355 H555" stroke="#ff8a34" stroke-width="9"/>
{card(600, 165, 710, 180, 'Home + lock screen', 'ETA 00:48', '#ff8a34')}
{card(600, 390, 710, 180, 'Dynamic Island + notification', 'Arriving now', '#50d8ff')}
{text(955, 665, 'THE EXPERIENCE KEEPS MOVING', 31, '#7ce6ad', 700, 'middle')}
"""
        assets[f"surfaces-victory-{suffix}.svg"] = svg_document(w, h, victory_body)
    return assets


def write_assets(project: Path, root: Path) -> None:
    source = root / "samples" / "samples" / "SurfacesSample" / "SurfacesSample.java"
    lines = source.read_text(encoding="utf-8").splitlines()

    def excerpt(first: int, last: int, name: str) -> None:
        (project / name).write_text("\n".join(lines[first - 1:last]) + "\n", encoding="utf-8")

    excerpt(222, 229, "timeline.txt")
    excerpt(297, 309, "dynamic-text.txt")
    excerpt(324, 339, "live-activity.txt")
    excerpt(115, 124, "cold-start.txt")

    asset_root = root / "docs" / "website" / "static" / "blog" / SLUG
    for name in ("widget-preview.png", "dynamic-island.png", "clock-widget.png", "sample-form.png"):
        (project / name).write_bytes((asset_root / name).read_bytes())
    (project / "codename-one-logo.png").write_bytes(
        (root / "docs" / "website" / "static" / "uploads" / "Codename-One-White-Logo.png").read_bytes()
    )
    for name, contents in story_svgs().items():
        (project / name).write_text(contents + "\n", encoding="utf-8")


def write_thumbnail(project: Path) -> None:
    canvas = Image.new("RGB", (1280, 720), (6, 16, 29))
    draw = ImageDraw.Draw(canvas, "RGBA")
    for y in range(720):
        draw.line((0, y, 1280, y), fill=(6 + y // 160, 16 + y // 28, 29 + y // 16, 255))
    bold = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
    regular = "/System/Library/Fonts/Supplemental/Arial.ttf"
    label_font = ImageFont.truetype(bold, 24)
    label = "CODENAME ONE"
    bbox = draw.textbbox((0, 0), label, font=label_font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    pill = (68, 54, 68 + tw + 136, 112)
    tx = pill[0] + (pill[2] - pill[0] - tw) / 2 - bbox[0]
    ty = pill[1] + (pill[3] - pill[1] - th) / 2 - bbox[1]
    rendered = draw.textbbox((tx, ty), label, font=label_font)
    clearance = min(rendered[0] - pill[0], pill[2] - rendered[2])
    if clearance < 64:
        raise ValueError(f"thumbnail brand clearance {clearance}px is below 64px")
    draw.rounded_rectangle(pill, radius=27, fill=(15, 43, 66, 235),
                           outline=(80, 216, 255, 200), width=2)
    draw.text((tx, ty), label, font=label_font, fill=(190, 237, 255, 255))

    draw.text((68, 172), "YOUR APP", font=ImageFont.truetype(bold, 69), fill=(255, 255, 255, 255))
    draw.text((68, 252), "IS ASLEEP.", font=ImageFont.truetype(bold, 69), fill=(255, 255, 255, 255))
    draw.text((68, 352), "THE ETA ISN'T.", font=ImageFont.truetype(bold, 57), fill=(80, 216, 255, 255))
    draw.text((71, 449), "Widgets · Live Activities · Dynamic Island",
              font=ImageFont.truetype(regular, 28), fill=(187, 213, 229, 255))
    draw.rounded_rectangle((70, 535, 610, 625), radius=35, fill=(19, 36, 55, 255),
                           outline=(255, 138, 52, 230), width=3)
    draw.text((113, 561), "OUT FOR DELIVERY    00:48",
              font=ImageFont.truetype(bold, 27), fill=(255, 255, 255, 255))

    capture = Image.open(project / "widget-preview.png").convert("RGB")
    # Use the authentic article capture, framed as a simulator window.
    crop = capture.crop((0, 0, capture.width, min(capture.height, int(capture.width * 1.08))))
    crop.thumbnail((500, 610), Image.Resampling.LANCZOS)
    px = 1240 - crop.width
    py = (720 - crop.height) // 2
    draw.rounded_rectangle((px - 20, py - 20, 1260, py + crop.height + 20), radius=32,
                           fill=(71, 213, 255, 35), outline=(71, 213, 255, 220), width=4)
    canvas.paste(crop, (px, py))
    canvas.save(project / "thumbnail.jpg", quality=93, optimize=True)


def scripts(title: str, output: dict, narration: dict) -> tuple[dict, dict]:
    def image(name: str, image_id: str, bounds: dict, at: int = 250) -> dict:
        return {"type": "image.show", "id": image_id, "role": "proof", "kind": "capture",
                "atMs": at, "path": name, "bounds": bounds}

    def paired_svg(name: str, svg_id: str, bounds: dict | None = None, role: str | None = None,
                   source_title: str | None = None, source_url: str | None = None,
                   portrait_bounds: dict | None = None) -> dict:
        action = {"type": "svg.show", "id": svg_id, "atMs": 250,
                  "paths": {"landscape": f"{name}-landscape.svg", "portrait": f"{name}-portrait.svg"},
                  "bounds": bounds or {"x": 0.04, "y": 0.17, "width": 0.92, "height": 0.77},
                  "orientation": {"portrait": portrait_bounds or
                                  {"x": 0.06, "y": 0.16, "width": 0.88, "height": 0.78}}}
        if role:
            action["role"] = role
        if source_title:
            action["sourceTitle"] = source_title
        if source_url:
            action["sourceUrl"] = source_url
        return action

    splash = {"id": "who-and-what", "durationMs": 9_000, "storyBeats": ["identity"], "actions": [
        {"type": "image.show", "id": "brand", "role": "brand", "kind": "illustration", "atMs": 200,
         "path": "codename-one-logo.png", "bounds": {"x": 0.045, "y": 0.045, "width": 0.12, "height": 0.04},
         "orientation": {"portrait": {"x": 0.06, "y": 0.025, "width": 0.18, "height": 0.027}}},
        paired_svg("surfaces-intro", "intro", {"x": 0.49, "y": 0.09, "width": 0.47, "height": 0.82}),
        {"type": "text.show", "id": "opening-title", "role": "title", "atMs": 350,
         "text": "Your app can stop.\nThe experience can keep moving.", "uiid": "VideoTitle",
         "responsive": True, "maxLines": 3, "bounds": {"x": 0.055, "y": 0.24, "width": 0.46, "height": 0.30},
         "orientation": {"portrait": {"x": 0.07, "y": 0.08, "width": 0.86, "height": 0.24}}},
        cue("Codename One builds native apps in Java from one codebase. Today: widgets, Live Activities, and Dynamic Island after the app stops.", 650),
    ]}
    desire = {"id": "the-glance", "durationMs": 14_000, "storyBeats": ["problem"], "actions": [
        paired_svg("surfaces-desire", "desire"),
        {"type": "text.show", "id": "desire-title", "atMs": 0, "text": "The useful moment is a glance", "uiid": "VideoTitle",
         "responsive": True, "maxLines": 2, "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.13}},
        cue("Imagine a delivery app. The customer wants the current ETA at a glance, without reopening the app every few seconds just to watch a number change."),
    ]}
    dead = {"id": "outside-the-process", "durationMs": 16_000, "storyBeats": ["difficulty"], "actions": [
        paired_svg("dead-process", "dead-process", role="evidence",
                   source_title="Keeping a widget up to date",
                   source_url="https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date"),
        {"type": "text.show", "id": "dead-title", "atMs": 0, "text": "A widget is not a tiny app screen", "uiid": "VideoTitle",
         "responsive": True, "maxLines": 2, "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.13}},
        cue("That sounds simple until the app process is gone. There is no live component tree, event dispatch thread, object graph, or Java listener waiting for the next tick or tap."),
    ]}
    rules = {"id": "every-platform-changes-the-rules", "durationMs": 17_000, "storyBeats": ["difficulty"], "actions": [
        paired_svg("platform-rules", "platform-rules", role="evidence",
                   source_title="Build a widget host",
                   source_url="https://developer.android.com/develop/ui/views/appwidgets/host"),
        {"type": "text.show", "id": "rules-title", "atMs": 0, "text": "Then every platform changes the rules", "uiid": "VideoTitle",
         "responsive": True, "maxLines": 2, "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.13}},
        cue("Apple uses widget timelines and ActivityKit regions. Android widgets use a constrained RemoteViews tree, receivers, and notifications. Desktop preview, sizing, updates, and cold-start taps add another development loop."),
    ]}
    insight = {"id": "publish-data-not-a-live-ui", "durationMs": 15_000, "storyBeats": ["intervention"], "actions": [
        paired_svg("data-not-ui", "data-model"),
        {"type": "transition", "target": "data-model", "atMs": 250, "durationMs": 900, "effect": "morph", "easing": "ease-out"},
        {"type": "text.show", "id": "insight-title", "atMs": 0, "text": "Publish data that can outlive the process", "uiid": "VideoTitle",
         "responsive": True, "maxLines": 2, "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.13}},
        cue("Codename One changes the unit of work. Instead of exporting a live component, the app publishes a serializable surface layout and a dated timeline of state. The operating system persists and renders both."),
    ]}
    timeline = {"id": "one-timeline-four-moments", "durationMs": 31_000, "storyBeats": ["proof"],
                "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"}, "actions": [
        {"type": "text.show", "id": "timeline-title", "role": "title", "atMs": 0,
         "text": "One timeline carries four moments", "uiid": "VideoTitle", "responsive": True, "maxLines": 2},
        {"type": "code.show", "id": "timeline-code", "role": "code", "atMs": 200, "language": "java", "path": "timeline.txt"},
        {"type": "demo.mount", "id": "timeline-demo", "role": "demo", "atMs": 250, "class": DEMO_CLASS, "animated": False},
        {"type": "demo.action", "atMs": 3_200, "name": "publish", "arguments": {}},
        {"type": "demo.action", "atMs": 7_000, "name": "nextTimeline", "arguments": {}},
        {"type": "demo.action", "atMs": 10_500, "name": "nextTimeline", "arguments": {}},
        {"type": "demo.action", "atMs": 14_000, "name": "nextTimeline", "arguments": {}},
        {"type": "demo.action", "atMs": 15_000, "name": "resize", "arguments": {}},
        {"type": "focus.show", "id": "timeline-focus", "target": "timeline-code", "atMs": 3_500,
         "relativeBounds": {"x": 0.02, "y": 0.18, "width": 0.96, "height": 0.66},
         "label": "four dated state maps", "color": "ff9c52"},
        {"type": "replay", "atMs": 20_000, "fromMs": 9_700, "toMs": 14_700,
         "rewindDurationMs": 600, "rewindFps": 10, "playbackRate": 0.58,
         "label": "Again: the timeline advances while the app sleeps"},
        cue("This is source from the compiled sample. It publishes one layout and four dated state maps: Preparing, Out for delivery, Arriving now, and Delivered. The preview advances those entries and changes size before a device build."),
        cue("Watch that change once more. The renderer snaps back to Out for delivery, adds the rewind marker, then plays the same transition more slowly. The app does not repaint the widget; time selects the next state the system already has.", 16_000, "replay-voice"),
    ]}
    native_time = {"id": "time-moves-without-java", "durationMs": 18_000, "storyBeats": ["proof"],
                   "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"}, "actions": [
        {"type": "text.show", "id": "time-title", "role": "title", "atMs": 0,
         "text": "The countdown is native timed text", "uiid": "VideoTitle", "responsive": True, "maxLines": 2},
        {"type": "code.show", "id": "timer-code", "role": "code", "atMs": 200, "language": "java", "path": "dynamic-text.txt"},
        {"type": "demo.mount", "id": "timer-demo", "role": "demo", "atMs": 250, "class": DEMO_CLASS, "animated": False},
        {"type": "demo.action", "atMs": 2_500, "name": "publish", "arguments": {}},
        {"type": "focus.show", "id": "timer-focus", "target": "timer-code", "atMs": 3_600,
         "relativeBounds": {"x": 0.02, "y": 0.36, "width": 0.96, "height": 0.32},
         "label": "STYLE_TIMER_DOWN", "color": "50d8ff"},
        cue("The ETA uses SurfaceDynamicText. WidgetKit renders timed text, and Android maps it to a chronometer, so the visible countdown can change without repeatedly waking Java."),
    ]}
    surfaces = {"id": "native-system-surfaces", "durationMs": 18_000, "storyBeats": ["proof"], "actions": [
        {"type": "text.show", "id": "surfaces-title", "atMs": 0,
         "text": "The same state reaches the places users already look", "uiid": "VideoTitle", "responsive": True,
         "maxLines": 2, "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.13}},
        image("widget-preview.png", "widget-capture", {"x": 0.055, "y": 0.18, "width": 0.47, "height": 0.72}),
        image("dynamic-island.png", "island-capture", {"x": 0.55, "y": 0.27, "width": 0.40, "height": 0.34}, 500),
        cue("That state becomes an iOS home-screen widget, a Live Activity on the lock screen and Dynamic Island, an Android widget or ongoing notification, and a floating desktop surface."),
    ]}
    live = {"id": "live-activity-running", "durationMs": 25_000, "storyBeats": ["proof"],
            "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"}, "actions": [
        {"type": "text.show", "id": "live-title", "role": "title", "atMs": 0,
         "text": "Now the Live Activity changes for real", "uiid": "VideoTitle", "responsive": True, "maxLines": 2},
        {"type": "code.show", "id": "live-code", "role": "code", "atMs": 200, "language": "java", "path": "live-activity.txt"},
        {"type": "demo.mount", "id": "live-demo", "role": "demo", "atMs": 250, "class": DEMO_CLASS, "animated": False},
        {"type": "demo.action", "atMs": 3_000, "name": "startActivity", "arguments": {}},
        {"type": "demo.action", "atMs": 7_000, "name": "advanceActivity", "arguments": {}},
        {"type": "demo.action", "atMs": 11_000, "name": "advanceActivity", "arguments": {}},
        {"type": "demo.action", "atMs": 15_000, "name": "advanceActivity", "arguments": {}},
        {"type": "focus.show", "id": "regions-focus", "target": "live-code", "atMs": 4_000,
         "relativeBounds": {"x": 0.02, "y": 0.23, "width": 0.96, "height": 0.62},
         "label": "compact + expanded Dynamic Island regions", "color": "ff9c52"},
        cue("The LiveActivityDescriptor reuses the delivery layout and adds compact and expanded Dynamic Island regions. The running demo starts the activity, then sends three state updates. Progress and text change because the API changed state, not because an image moved."),
    ]}
    cold = {"id": "tap-crosses-the-cold-start", "durationMs": 18_000, "storyBeats": ["proof"],
            "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"}, "actions": [
        {"type": "text.show", "id": "cold-title", "role": "title", "atMs": 0,
         "text": "The tap crosses a cold start", "uiid": "VideoTitle", "responsive": True, "maxLines": 2},
        {"type": "code.show", "id": "cold-code", "role": "code", "atMs": 200, "language": "java", "path": "cold-start.txt"},
        {"type": "demo.mount", "id": "cold-demo", "role": "demo", "atMs": 250, "class": DEMO_CLASS, "animated": False},
        {"type": "demo.action", "atMs": 2_800, "name": "publish", "arguments": {}},
        {"type": "demo.action", "atMs": 6_200, "name": "tapSurface", "arguments": {}},
        {"type": "focus.show", "id": "cold-focus", "target": "cold-code", "atMs": 4_500,
         "relativeBounds": {"x": 0.02, "y": 0.28, "width": 0.96, "height": 0.62},
         "label": "queued until the handler is registered", "color": "7ce6ad"},
        cue("A dead app has no listener. The surface carries the action ID and order payload. A tap launches the app, then Codename One delivers the queued event after the handler registers, marked as a cold start."),
    ]}
    constraint = {"id": "the-honest-constraint", "durationMs": 14_000, "storyBeats": ["victory"], "actions": [
        paired_svg("surface-constraints", "constraints"),
        {"type": "text.show", "id": "constraint-title", "atMs": 0,
         "text": "Portable system surfaces use a focused node set", "uiid": "VideoTitle", "responsive": True,
         "maxLines": 2, "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.13}},
        cue("System surfaces deliberately use a smaller catalog than full app screens: text, images, rows, columns, progress, vectors, and actions. That focused model fits the rules each operating system permits."),
    ]}
    victory = {"id": "experience-keeps-moving", "durationMs": 16_000, "storyBeats": ["victory"], "actions": [
        paired_svg("surfaces-victory", "victory"),
        {"type": "text.show", "id": "victory-title", "atMs": 0,
         "text": "The app sleeps. The experience keeps moving.", "uiid": "VideoTitle", "responsive": True,
         "maxLines": 2, "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.13}},
        cue("The developer writes the delivery state once, tests the hard moments at the desk, and lets each platform render the experience where users already look. The package arrives even while the app sleeps."),
    ]}
    outro = {"id": "outro", "durationMs": 15_000, "storyBeats": ["outro"], "actions": [
        {"type": "outro.show", "id": "next", "atMs": 0, "durationMs": 1_100,
         "eyebrow": "CODENAME ONE · SYSTEM SURFACES",
         "title": "What should stay useful after your app closes?",
         "subtitle": "Widgets · Live Activities · Dynamic Island · notifications · desktop",
         "prompt": "Tell us the surface your users need next.",
         "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}},
        cue("What should your app keep useful after it closes? Tell us which system surface would change the experience for your users."),
    ]}

    editorial = {
        "storyType": "live-demo", "status": "approved",
        "topicTerms": ["widgets", "live activities", "dynamic island"],
        "proof": ["Apple WidgetKit documentation", "Android widget documentation",
                  "real SurfacesSample source", "compiled Surfaces API demo"],
        "humanBeat": "A customer wants a delivery ETA at a glance after the app process has stopped.",
        "visualIdentity": "An orange package and its ETA escape the app and keep moving across native system surfaces.",
        "bespokeVisualization": "The living component tree morphs into a durable layout plus dated timeline before real code drives the compiled preview.",
        "heroJourney": {
            "identity": "Codename One is introduced subtly as the Java native-app platform, and the topic is widgets and Live Activities.",
            "problem": "The useful delivery moment happens after the customer closes the app.",
            "difficulty": "No process or listener remains, and each platform imposes a different lifecycle and surface technology.",
            "intervention": "A serializable surface layout and dated state timeline replace the assumption of a live component.",
            "victory": "The developer tests real time, resize, Live Activity, and cold-start changes from one Java model."
        },
        "problemDimensions": ["apis", "tools", "languages", "debugging", "hardware", "simulation"],
        "resolutionMap": [
            {"problem": "apis", "solution": "one com.codename1.surfaces model", "proof": "timeline and LiveActivity source"},
            {"problem": "tools", "solution": "one Codename One project pipeline", "proof": "compiled demo module"},
            {"problem": "languages", "solution": "shared Java surface declarations", "proof": "syntax-highlighted sample source"},
            {"problem": "debugging", "solution": "desktop preview with controllable time", "proof": "timeline replay and resize actions"},
            {"problem": "hardware", "solution": "native system-surface backends", "proof": "article widget and Dynamic Island captures"},
            {"problem": "simulation", "solution": "JavaSE surface instrumentation", "proof": "compiled state and cold-start actions"},
        ],
    }
    long_scenes = [splash, desire, dead, rules, insight, timeline, native_time, surfaces,
                   live, cold, constraint, victory, outro]

    short_problem = {"id": "short-problem-proof", "durationMs": 15_000, "storyBeats": ["difficulty"], "actions": [
        paired_svg("dead-process", "short-dead", {"x": 0.06, "y": 0.14, "width": 0.88, "height": 0.38},
                   "evidence", "Keeping a widget up to date",
                   "https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date",
                   {"x": 0.06, "y": 0.14, "width": 0.88, "height": 0.38}),
        paired_svg("platform-rules", "short-rules", {"x": 0.06, "y": 0.54, "width": 0.88, "height": 0.38},
                   "evidence", "Build a widget host",
                   "https://developer.android.com/develop/ui/views/appwidgets/host",
                   {"x": 0.06, "y": 0.54, "width": 0.88, "height": 0.38}),
        {"type": "text.show", "id": "short-problem-title", "atMs": 0,
         "text": "No process. Different platform rules.", "uiid": "VideoTitle", "responsive": True, "maxLines": 2,
         "bounds": {"x": 0.06, "y": 0.03, "width": 0.88, "height": 0.10}},
        cue("The problem is that widgets live outside the running UI. Apple, Android, and desktop give them different lifecycles, layouts, update rules, and cold-start behavior."),
    ]}
    short_timeline = {**timeline, "id": "short-timeline", "durationMs": 24_000}
    short_timeline["actions"] = [a for a in timeline["actions"] if a.get("type") != "replay" and a.get("id") != "replay-voice"]
    short_live = {**live, "id": "short-live-activity", "durationMs": 20_000}
    short_cold = {**cold, "id": "short-cold-start", "durationMs": 15_000}
    short_victory = {**victory, "id": "short-victory", "durationMs": 12_000}
    short_splash = {**splash, "id": "short-who-and-what", "durationMs": 8_000}
    short_desire = {**desire, "id": "short-the-glance", "durationMs": 10_000}
    short_outro = {**outro, "id": "short-outro", "durationMs": 15_000}
    short_scenes = [short_splash, short_desire, short_problem, {**insight, "id": "short-data-not-ui", "durationMs": 13_000},
                    short_timeline, short_live, short_cold, short_victory, short_outro]

    common = {"schemaVersion": 1, "editorial": editorial, "output": output, "narration": narration}
    return (
        {**common, "id": SLUG, "title": title, "scenes": long_scenes},
        {**common, "id": f"{SLUG}-short", "title": "The app stopped. Why is the ETA still moving?", "scenes": short_scenes},
    )
