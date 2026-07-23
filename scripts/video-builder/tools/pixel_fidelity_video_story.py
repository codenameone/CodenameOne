#!/usr/bin/env python3
"""Article-specific story package for native-reference UI fidelity testing."""

from __future__ import annotations

import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


SLUG = "pixel-perfect-is-a-test"
DEMO_CLASS = "com.codename1.ui.FidelityMorphDemoScene"


def cue(text: str, at: int = 650, cue_id: str = "voice") -> dict:
    return {
        "type": "narration.cue",
        "id": cue_id,
        "atMs": at,
        "text": text,
        "caption": text,
    }


def svg_document(width: int, height: int, body: str) -> str:
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}">\n'
        "<style>"
        ".title{font:700 48px sans-serif;fill:#f5f8fb}"
        ".label{font:700 29px sans-serif;fill:#f5f8fb}"
        ".body{font:25px sans-serif;fill:#c8d4e3}"
        ".small{font:21px sans-serif;fill:#8ea4ba}"
        ".card{fill:#111a27;stroke:#26384c;stroke-width:3}"
        "</style>\n"
        f"{body}\n</svg>"
    )


def story_svgs() -> dict[str, str]:
    intro_l = svg_document(1200, 700, """
<defs><linearGradient id="lens" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#62e6ff"/><stop offset="1" stop-color="#9f7aea"/></linearGradient></defs>
<rect x="75" y="100" width="1050" height="500" rx="46" fill="#0b1420" stroke="#2b4057" stroke-width="4"/>
<rect x="145" y="185" width="910" height="265" rx="64" fill="#e9eef5"/>
<circle cx="260" cy="317" r="55" fill="#f8fafc" stroke="#8798aa" stroke-width="4"/><circle cx="600" cy="317" r="55" fill="#f8fafc" stroke="#8798aa" stroke-width="4"/><circle cx="940" cy="317" r="55" fill="#f8fafc" stroke="#8798aa" stroke-width="4"/>
<path d="M260 317 C380 215 465 225 600 317 C725 410 825 405 940 317" fill="none" stroke="#51677f" stroke-width="8" stroke-dasharray="16 14"/>
<ellipse cx="600" cy="317" rx="122" ry="82" fill="url(#lens)" opacity=".68" stroke="#ffffff" stroke-width="6"/>
<path d="M540 286 Q600 242 660 286 M540 348 Q600 392 660 348" fill="none" stroke="#ffffff" stroke-width="6" opacity=".9"/>
<g fill="#26384c"><rect x="170" y="505" width="72" height="72" rx="8"/><rect x="252" y="505" width="72" height="72" rx="8"/><rect x="334" y="505" width="72" height="72" rx="8"/></g>
<g fill="#4fd1c5"><rect x="416" y="505" width="72" height="72" rx="8"/><rect x="498" y="505" width="72" height="72" rx="8"/></g>
<path d="M790 520 l36 36 l86 -95" fill="none" stroke="#68d391" stroke-width="18" stroke-linecap="round" stroke-linejoin="round"/>
<text x="600" y="660" class="body" text-anchor="middle">appearance becomes versioned input · motion becomes measurable output</text>
""")
    intro_p = svg_document(800, 1100, """
<defs><linearGradient id="lens" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#62e6ff"/><stop offset="1" stop-color="#9f7aea"/></linearGradient></defs>
<rect x="65" y="115" width="670" height="800" rx="48" fill="#0b1420" stroke="#2b4057" stroke-width="4"/>
<rect x="115" y="230" width="570" height="300" rx="70" fill="#e9eef5"/>
<circle cx="205" cy="380" r="48" fill="#f8fafc" stroke="#8798aa" stroke-width="4"/><circle cx="400" cy="380" r="48" fill="#f8fafc" stroke="#8798aa" stroke-width="4"/><circle cx="595" cy="380" r="48" fill="#f8fafc" stroke="#8798aa" stroke-width="4"/>
<path d="M205 380 C285 300 325 300 400 380 C480 465 525 455 595 380" fill="none" stroke="#51677f" stroke-width="7" stroke-dasharray="14 12"/>
<ellipse cx="400" cy="380" rx="104" ry="78" fill="url(#lens)" opacity=".7" stroke="#ffffff" stroke-width="6"/>
<g fill="#26384c"><rect x="145" y="635" width="88" height="88" rx="10"/><rect x="249" y="635" width="88" height="88" rx="10"/><rect x="353" y="635" width="88" height="88" rx="10"/></g>
<g fill="#4fd1c5"><rect x="457" y="635" width="88" height="88" rx="10"/><rect x="561" y="635" width="88" height="88" rx="10"/></g>
<path d="M272 817 l44 44 l104 -118" fill="none" stroke="#68d391" stroke-width="20" stroke-linecap="round" stroke-linejoin="round"/>
<text x="400" y="1000" class="body" text-anchor="middle">version the look</text><text x="400" y="1038" class="body" text-anchor="middle">measure the motion</text>
""")
    states_l = svg_document(1200, 700, """
<text x="600" y="58" class="title" text-anchor="middle">A component is a matrix of states</text>
<g transform="translate(75 112)">
<rect width="1050" height="480" rx="28" class="card"/>
<g class="label"><text x="260" y="56" text-anchor="middle">Normal</text><text x="525" y="56" text-anchor="middle">Pressed</text><text x="790" y="56" text-anchor="middle">Selected</text></g>
<g class="body"><text x="80" y="165">Light</text><text x="80" y="345">Dark</text></g>
<g fill="#e8edf4"><rect x="175" y="105" width="170" height="88" rx="44"/><rect x="440" y="105" width="170" height="88" rx="44"/><rect x="705" y="105" width="170" height="88" rx="44"/></g>
<g fill="#1c2938"><rect x="175" y="285" width="170" height="88" rx="44"/><rect x="440" y="285" width="170" height="88" rx="44"/><rect x="705" y="285" width="170" height="88" rx="44"/></g>
<g fill="none" stroke-width="7"><rect x="440" y="105" width="170" height="88" rx="44" stroke="#7c8da1"/><rect x="705" y="105" width="170" height="88" rx="44" stroke="#4fd1c5"/><rect x="440" y="285" width="170" height="88" rx="44" stroke="#9f7aea"/><rect x="705" y="285" width="170" height="88" rx="44" stroke="#62e6ff"/></g>
<path d="M925 120 v255" stroke="#3d5269" stroke-width="3"/><text x="965" y="180" class="small">geometry</text><text x="965" y="225" class="small">color</text><text x="965" y="270" class="small">contrast</text><text x="965" y="315" class="small">motion</text>
</g>
<text x="600" y="650" class="body" text-anchor="middle">one screenshot can pass while the interaction is still wrong</text>
""")
    states_p = svg_document(800, 1100, """
<text x="400" y="58" class="title" text-anchor="middle">Every state counts</text>
<g transform="translate(65 115)"><rect width="670" height="850" rx="30" class="card"/>
<g class="label"><text x="130" y="60">Normal</text><text x="130" y="315">Pressed</text><text x="130" y="570">Selected</text></g>
<g fill="#e8edf4"><rect x="105" y="95" width="205" height="88" rx="44"/><rect x="105" y="350" width="205" height="88" rx="44"/><rect x="105" y="605" width="205" height="88" rx="44"/></g>
<g fill="#1c2938"><rect x="365" y="95" width="205" height="88" rx="44"/><rect x="365" y="350" width="205" height="88" rx="44"/><rect x="365" y="605" width="205" height="88" rx="44"/></g>
<text x="207" y="225" class="small" text-anchor="middle">LIGHT</text><text x="467" y="225" class="small" text-anchor="middle">DARK</text>
<g fill="none" stroke-width="7"><rect x="105" y="350" width="205" height="88" rx="44" stroke="#7c8da1"/><rect x="365" y="350" width="205" height="88" rx="44" stroke="#9f7aea"/><rect x="105" y="605" width="205" height="88" rx="44" stroke="#4fd1c5"/><rect x="365" y="605" width="205" height="88" rx="44" stroke="#62e6ff"/></g>
<text x="335" y="790" class="body" text-anchor="middle">geometry · color · contrast · motion</text></g>
<text x="400" y="1040" class="body" text-anchor="middle">a screenshot is only one cell</text>
""")
    pipeline_l = svg_document(1200, 700, """
<text x="600" y="62" class="title" text-anchor="middle">Turn the platform into an answer sheet</text>
<g transform="translate(55 170)">
<rect x="0" y="0" width="235" height="260" rx="28" class="card"/><rect x="285" y="0" width="235" height="260" rx="28" class="card"/><rect x="570" y="0" width="235" height="260" rx="28" class="card"/><rect x="855" y="0" width="235" height="260" rx="28" class="card"/>
<g class="label" text-anchor="middle"><text x="117" y="65">Native app</text><text x="402" y="65">Versioned goldens</text><text x="687" y="65">Codename One</text><text x="972" y="65">One-way gate</text></g>
<g class="body" text-anchor="middle"><text x="117" y="120">UIKit + Material 3</text><text x="117" y="158">real controls</text><text x="402" y="120">light + dark</text><text x="402" y="158">pressed + selected</text><text x="687" y="120">same component</text><text x="687" y="158">same pinned state</text><text x="972" y="120">visual score</text><text x="972" y="158">geometry metrics</text></g>
<g fill="none" stroke="#62e6ff" stroke-width="7"><path d="M235 130 H278"/><path d="M520 130 H563"/><path d="M805 130 H848"/></g>
<g fill="#62e6ff"><path d="M278 130 l-18 -12 v24z"/><path d="M563 130 l-18 -12 v24z"/><path d="M848 130 l-18 -12 v24z"/></g>
<path d="M910 210 l35 35 l75 -90" fill="none" stroke="#68d391" stroke-width="15" stroke-linecap="round" stroke-linejoin="round"/>
</g>
<text x="600" y="565" class="body" text-anchor="middle">a score may improve · a known baseline may never silently get worse</text>
""")
    pipeline_p = svg_document(800, 1100, """
<text x="400" y="55" class="title" text-anchor="middle">Build an answer sheet</text>
<g transform="translate(110 105)">
<g class="card"><rect x="0" y="0" width="580" height="170" rx="28"/><rect x="0" y="220" width="580" height="170" rx="28"/><rect x="0" y="440" width="580" height="170" rx="28"/><rect x="0" y="660" width="580" height="170" rx="28"/></g>
<g class="label"><text x="45" y="62">1 · Native reference apps</text><text x="45" y="282">2 · Versioned golden images</text><text x="45" y="502">3 · Matching Codename One render</text><text x="45" y="722">4 · One-way regression gate</text></g>
<g class="body"><text x="45" y="112">UIKit and Material 3 controls</text><text x="45" y="332">light, dark, normal, pressed, selected</text><text x="45" y="552">same component, theme, and state</text><text x="45" y="772">visual score plus geometry metrics</text></g>
<g fill="none" stroke="#62e6ff" stroke-width="7"><path d="M290 170 V213"/><path d="M290 390 V433"/><path d="M290 610 V653"/></g>
<g fill="#62e6ff"><path d="M290 213 l-12 -18 h24z"/><path d="M290 433 l-12 -18 h24z"/><path d="M290 653 l-12 -18 h24z"/></g>
</g><text x="400" y="1015" class="body" text-anchor="middle">version the look · ratchet the result</text>
""")
    metrics_l = svg_document(1200, 700, """
<text x="600" y="60" class="title" text-anchor="middle">A percentage is a gate, not a verdict</text>
<g transform="translate(90 145)"><rect width="1020" height="390" rx="32" class="card"/>
<text x="250" y="75" class="label" text-anchor="middle">Android · 54 pairs</text><text x="770" y="75" class="label" text-anchor="middle">iOS · 68 pairs</text>
<text x="250" y="205" font-family="sans-serif" font-size="96" font-weight="700" fill="#4fd1c5" text-anchor="middle">95.5%</text><text x="770" y="205" font-family="sans-serif" font-size="96" font-weight="700" fill="#62e6ff" text-anchor="middle">94.4%</text>
<text x="250" y="255" class="body" text-anchor="middle">median tolerant score</text><text x="770" y="255" class="body" text-anchor="middle">median tolerant score</text>
<path d="M510 45 V335" stroke="#2b4057" stroke-width="4"/>
<text x="510" y="330" class="small" text-anchor="middle">also measure bounds · width · center · radius</text></g>
<text x="600" y="610" class="body" text-anchor="middle">human review still judges motion, translucency, and whether the right detail matched</text>
""")
    metrics_p = svg_document(800, 1100, """
<text x="400" y="55" class="title" text-anchor="middle">Measure, then look</text>
<g transform="translate(80 120)"><rect width="640" height="760" rx="34" class="card"/>
<text x="320" y="85" class="label" text-anchor="middle">Android · 54 pairs</text><text x="320" y="205" font-family="sans-serif" font-size="92" font-weight="700" fill="#4fd1c5" text-anchor="middle">95.5%</text><text x="320" y="250" class="body" text-anchor="middle">median tolerant score</text>
<path d="M75 315 H565" stroke="#2b4057" stroke-width="4"/>
<text x="320" y="395" class="label" text-anchor="middle">iOS · 68 pairs</text><text x="320" y="515" font-family="sans-serif" font-size="92" font-weight="700" fill="#62e6ff" text-anchor="middle">94.4%</text><text x="320" y="560" class="body" text-anchor="middle">median tolerant score</text>
<path d="M75 625 H565" stroke="#2b4057" stroke-width="4"/>
<text x="320" y="695" class="body" text-anchor="middle">bounds · width · center · radius</text></g>
<text x="400" y="970" class="body" text-anchor="middle">the gate catches regression</text><text x="400" y="1010" class="body" text-anchor="middle">eyes judge the experience</text>
""")
    return {
        "fidelity-intro-landscape.svg": intro_l,
        "fidelity-intro-portrait.svg": intro_p,
        "state-matrix-landscape.svg": states_l,
        "state-matrix-portrait.svg": states_p,
        "answer-sheet-landscape.svg": pipeline_l,
        "answer-sheet-portrait.svg": pipeline_p,
        "fidelity-metrics-landscape.svg": metrics_l,
        "fidelity-metrics-portrait.svg": metrics_p,
    }


def _copy_image(source: Path, target: Path) -> None:
    target.write_bytes(source.read_bytes())


def _native_half(source: Path, target: Path) -> None:
    with Image.open(source) as image:
        # Blog comparison cards place the native reference on the left and a thin
        # divider at the midpoint. Retain a little breathing room before it.
        crop = image.crop((0, 0, image.width // 2 - 4, image.height))
        crop.save(target, quality=94, optimize=True)


def write_assets(project: Path, root: Path) -> None:
    source = root / "docs" / "website" / "static" / "blog" / SLUG
    _copy_image(source / "ios-native-vs-cn1.jpg", project / "ios-native-vs-cn1.jpg")
    _copy_image(source / "android-native-vs-cn1.jpg", project / "android-native-vs-cn1.jpg")
    _copy_image(source / "tab-morph-fidelity.png", project / "tab-morph-fidelity.png")
    _copy_image(source / "ios-showcase-before-after.jpg", project / "ios-showcase-before-after.jpg")
    _copy_image(source / "android-fab-before-after.jpg", project / "android-fab-before-after.jpg")
    _native_half(source / "ios-native-vs-cn1.jpg", project / "ios-native-reference.jpg")
    _native_half(source / "android-native-vs-cn1.jpg", project / "android-native-reference.jpg")
    _copy_image(
        root / "docs" / "website" / "static" / "uploads" / "Codename-One-White-Logo.png",
        project / "codename-one-logo.png",
    )
    for name, contents in story_svgs().items():
        (project / name).write_text(contents + "\n", encoding="utf-8")

    source_code = (root / "CodenameOne" / "src" / "com" / "codename1" / "ui" /
                   "TabSelectionMorph.java").read_text(encoding="utf-8")
    match = re.search(
        r"static TabSelectionMorph compute\(.*?\{\n(.*?)\n\s*// travel envelopes",
        source_code,
        re.S,
    )
    if not match:
        raise ValueError("cannot locate TabSelectionMorph.compute() source")
    excerpt = (
        "TabSelectionMorph m = new TabSelectionMorph();\n"
        "float progress = clamp(t);\n\n"
        "float position = springEase(progress, tokens.spring);\n"
        "int x = fromX + (int)((toX - fromX) * position);\n"
        "int width = fromWidth\n"
        "        + (int)((toWidth - fromWidth) * position);\n\n"
        "m.flight = smooth(0f, 0.12f, progress)\n"
        "        * (1f - smooth(0.64f, 0.86f, progress));\n"
        "m.magnify = tokens.restMag\n"
        "        + (tokens.peakMag - tokens.restMag) * m.flight;\n"
        "m.aberration = tokens.peakAb * m.flight;\n"
        "return m;\n"
    )
    (project / "morph-code.txt").write_text(excerpt, encoding="utf-8")


def _font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    names = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/SFNS.ttf",
    ]
    for name in names:
        if Path(name).is_file():
            return ImageFont.truetype(name, size)
    return ImageFont.load_default()


def _fit_font(draw: ImageDraw.ImageDraw, value: str, maximum_width: int,
              start: int, minimum: int, bold: bool = True) -> ImageFont.FreeTypeFont:
    for size in range(start, minimum - 1, -1):
        font = _font(size, bold)
        if draw.textbbox((0, 0), value, font=font)[2] <= maximum_width:
            return font
    raise ValueError(f"cannot fit {value!r} into {maximum_width}px")


def write_thumbnail(project: Path) -> None:
    canvas = Image.new("RGB", (1280, 720), "#08111d")
    draw = ImageDraw.Draw(canvas)
    with Image.open(project / "tab-morph-fidelity.png") as image:
        crop = image.crop((0, 70, image.width, min(image.height, 650))).convert("RGB")
        crop.thumbnail((610, 500), Image.Resampling.LANCZOS)
        panel = Image.new("RGB", (650, 520), "#e8edf4")
        panel.paste(crop, ((650 - crop.width) // 2, (520 - crop.height) // 2))
        canvas.paste(panel, (590, 135))
    draw.rounded_rectangle((566, 111, 1264, 679), radius=34, outline="#62e6ff", width=5)
    draw.rectangle((0, 0, 720, 720), fill="#08111d")
    draw.polygon([(520, 0), (760, 0), (650, 720), (410, 720)], fill="#08111d")

    brand = "CODENAME ONE"
    brand_font = _fit_font(draw, brand, 300, 32, 24)
    brand_box = draw.textbbox((0, 0), brand, font=brand_font)
    brand_width = brand_box[2] - brand_box[0]
    pill_left, pill_top = 64, 48
    pill_right = pill_left + brand_width + 72
    if pill_right > 500:
        raise ValueError("thumbnail brand pill exceeds reserved width")
    draw.rounded_rectangle((pill_left, pill_top, pill_right, pill_top + 58), radius=29,
                           fill="#111d2c", outline="#4fd1c5", width=3)
    draw.text((pill_left + 36, pill_top + 12), brand, font=brand_font, fill="#f5f8fb")
    actual_right = pill_left + 36 + brand_width
    if pill_right - actual_right < 32:
        raise ValueError("thumbnail brand text lacks right clearance")

    title_font = _font(70, True)
    sub_font = _font(39, False)
    draw.text((64, 168), "YOUR UI", font=title_font, fill="#f5f8fb")
    draw.text((64, 248), "MOVED.", font=title_font, fill="#62e6ff")
    draw.text((64, 368), "YOUR CODE", font=title_font, fill="#f5f8fb")
    draw.text((64, 448), "DIDN'T.", font=title_font, fill="#ffbd66")
    draw.text((64, 585), "Make pixel fidelity a test", font=sub_font, fill="#c8d4e3")
    canvas.save(project / "thumbnail.jpg", quality=93, optimize=True)


def _paired_svg(name: str, image_id: str, at: int = 200) -> dict:
    return {
        "type": "svg.show",
        "id": image_id,
        "atMs": at,
        "paths": {
            "landscape": f"{name}-landscape.svg",
            "portrait": f"{name}-portrait.svg",
        },
        "bounds": {"x": 0.04, "y": 0.15, "width": 0.92, "height": 0.79},
        "orientation": {"portrait": {"x": 0.06, "y": 0.13, "width": 0.88, "height": 0.82}},
    }


def _title(text: str, title_id: str) -> dict:
    return {
        "type": "text.show",
        "id": title_id,
        "atMs": 0,
        "text": text,
        "uiid": "VideoTitle",
        "responsive": True,
        "maxLines": 3,
        "bounds": {"x": 0.04, "y": 0.02, "width": 0.92, "height": 0.14},
        "orientation": {"portrait": {"x": 0.07, "y": 0.025, "width": 0.86, "height": 0.13}},
    }


def scripts(title: str, output: dict, narration: dict) -> tuple[dict, dict]:
    identity = {
        "id": "who-and-what", "durationMs": 10_000, "storyBeats": ["identity"], "actions": [
            {"type": "image.show", "id": "brand", "role": "brand", "kind": "illustration",
             "atMs": 200, "path": "codename-one-logo.png",
             "bounds": {"x": 0.045, "y": 0.04, "width": 0.12, "height": 0.04},
             "orientation": {"portrait": {"x": 0.06, "y": 0.025, "width": 0.18, "height": 0.027}}},
            _paired_svg("fidelity-intro", "intro-art", 300),
            {"type": "text.show", "id": "opening-title", "atMs": 350,
             "text": "The pixels moved.\nThe code did not.", "uiid": "VideoTitle",
             "responsive": True, "maxLines": 3,
             "bounds": {"x": 0.055, "y": 0.10, "width": 0.48, "height": 0.15},
             "orientation": {"portrait": {"x": 0.07, "y": 0.07, "width": 0.86, "height": 0.22}}},
            cue("Codename One builds native apps in Java from one codebase. Today: how pixel fidelity becomes a test instead of an argument."),
        ],
    }
    apple_problem = {
        "id": "the-platform-owns-the-update", "durationMs": 16_000,
        "storyBeats": ["problem"], "actions": [
            _title("A platform update can redesign a shipped screen", "platform-title"),
            {"type": "image.show", "id": "apple-reference", "role": "evidence", "kind": "capture",
             "atMs": 350, "path": "ios-native-reference.jpg",
             "sourceTitle": "Apple: Adopting Liquid Glass",
             "sourceUrl": "https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass",
             "bounds": {"x": 0.10, "y": 0.20, "width": 0.80, "height": 0.69},
             "orientation": {"portrait": {"x": 0.10, "y": 0.20, "width": 0.80, "height": 0.65}}},
            {"type": "text.show", "id": "apple-source", "atMs": 700,
             "text": "Apple: standard components pick up the latest look on the latest platform release",
             "uiid": "VideoCaption", "responsive": True, "maxLines": 3,
             "bounds": {"x": 0.13, "y": 0.82, "width": 0.74, "height": 0.10},
             "orientation": {"portrait": {"x": 0.10, "y": 0.84, "width": 0.80, "height": 0.08}}},
            cue("A team can test one interface, ship it, and later discover that the platform's standard controls now look and behave differently. Apple says the latest platform release gives standard components its latest look and feel."),
        ],
    }
    android_problem = {
        "id": "the-other-platform-has-another-migration", "durationMs": 17_000,
        "storyBeats": ["difficulty"], "actions": [
            _title("Android has a different design migration", "android-title"),
            {"type": "image.show", "id": "android-reference", "role": "evidence", "kind": "capture",
             "atMs": 300, "path": "android-native-reference.jpg",
             "sourceTitle": "Android: Migrate from Material 2 to Material 3",
             "sourceUrl": "https://developer.android.com/develop/ui/compose/designsystems/material2-material3",
             "bounds": {"x": 0.09, "y": 0.19, "width": 0.82, "height": 0.68},
             "orientation": {"portrait": {"x": 0.09, "y": 0.20, "width": 0.82, "height": 0.62}}},
            {"type": "text.show", "id": "android-source", "atMs": 700,
             "text": "Android: migrate dependencies, themes, screens, colors, typography, shapes, and components",
             "uiid": "VideoCaption", "responsive": True, "maxLines": 3,
             "bounds": {"x": 0.12, "y": 0.82, "width": 0.76, "height": 0.10},
             "orientation": {"portrait": {"x": 0.10, "y": 0.84, "width": 0.80, "height": 0.08}}},
            cue("Android's Material 3 migration changes packages, themes, colors, typography, shapes, and component behavior. Now the same product has two design schedules, two reference implementations, and two sets of regressions to find."),
        ],
    }
    state_problem = {
        "id": "a-screenshot-is-one-state", "durationMs": 18_000,
        "storyBeats": ["difficulty"], "actions": [
            _paired_svg("state-matrix", "state-matrix", 250),
            cue("A single screenshot is not enough. Normal, pressed, selected, disabled, light, and dark states can all diverge. Motion adds another dimension: a tab can finish in the right place while taking the wrong path to get there."),
        ],
    }
    intervention = {
        "id": "build-an-answer-sheet", "durationMs": 18_000,
        "storyBeats": ["intervention"], "actions": [
            _paired_svg("answer-sheet", "answer-sheet", 250),
            {"type": "transition", "target": "answer-sheet", "atMs": 250,
             "durationMs": 900, "effect": "morph", "easing": "ease-out"},
            cue("Codename One turns the native platform into a versioned answer sheet. Small UIKit and Material apps render real controls on pinned toolchains. Their captures become goldens. Continuous integration renders the matching Codename One state and refuses a regression."),
        ],
    }
    progress_actions = [
        {"type": "demo.action", "atMs": 3_000 + step * 450,
         "name": "setProgress", "arguments": {"value": step / 16.0}}
        for step in range(17)
    ]
    motion_proof = {
        "id": "run-the-motion-model", "durationMs": 32_000, "storyBeats": ["proof"],
        "composition": {"landscape": "code-left-demo-right", "portrait": "code-over-demo"},
        "actions": [
            _title("The motion is code, frames, and a bounded result", "motion-title"),
            {"type": "code.show", "id": "morph-code", "role": "code", "atMs": 250,
             "language": "java", "path": "morph-code.txt"},
            {"type": "demo.mount", "id": "morph-demo", "role": "demo", "atMs": 300,
             "class": DEMO_CLASS, "animated": False},
            {"type": "pointer.show", "id": "tap-tab", "atMs": 2_700, "style": "touch",
             "area": "morph-demo", "x": 0.83, "y": 0.72},
            {"type": "pointer.click", "target": "tap-tab", "atMs": 3_000, "durationMs": 320},
            {"type": "demo.action", "atMs": 12_500, "name": "toggleAppearance", "arguments": {}},
            {"type": "focus.show", "id": "flight-focus", "target": "morph-code", "atMs": 4_300,
             "relativeBounds": {"x": 0.02, "y": 0.25, "width": 0.96, "height": 0.62},
             "label": "position, flight, magnification, aberration", "color": "62e6ff"},
            {"type": "replay", "atMs": 16_500, "fromMs": 2_500, "toMs": 10_600,
             "rewindDurationMs": 650, "rewindFps": 10, "replayFps": 10,
             "playbackRate": 0.62,
             "label": "Again: measured travel and bounded spring settle"},
            cue("This is the shipped tab-selection geometry running inside the compiled sample. The same progress value determines position, stretch, lift, magnification, color separation, and the bounded spring settle."),
            cue("Watch the transition again more slowly. The lens grows during flight, compresses at the destination, overshoots without leaving the bar, and settles on the same final frame every time.", 14_000, "replay-voice"),
            cue("Motion gets fixed reference frames and repeatable review too.", 23_500, "motion-answer-sheet"),
        ] + progress_actions,
    }
    capture_proof = {
        "id": "compare-real-output", "durationMs": 20_000, "storyBeats": ["proof"], "actions": [
            _title("Native on the left. Codename One on the right.", "capture-title"),
            {"type": "image.show", "id": "ios-pairs", "role": "proof", "kind": "capture",
             "atMs": 250, "path": "ios-native-vs-cn1.jpg",
             "bounds": {"x": 0.08, "y": 0.17, "width": 0.39, "height": 0.76},
             "orientation": {"portrait": {"x": 0.08, "y": 0.16, "width": 0.84, "height": 0.43}}},
            {"type": "image.show", "id": "android-pairs", "role": "proof", "kind": "capture",
             "atMs": 500, "path": "android-native-vs-cn1.jpg",
             "bounds": {"x": 0.53, "y": 0.20, "width": 0.39, "height": 0.69},
             "orientation": {"portrait": {"x": 0.08, "y": 0.61, "width": 0.84, "height": 0.30}}},
            {"type": "transition", "target": "android-pairs", "atMs": 500,
             "durationMs": 800, "effects": {"landscape": "slide-right", "portrait": "slide-up"},
             "easing": "ease-out"},
            cue("These are current report captures, not illustrations. Native controls are on the left and Codename One controls are on the right. The divider makes a wrong width or corner radius visible before a release changes every screen."),
        ],
    }
    metrics = {
        "id": "measure-without-worshipping-the-score", "durationMs": 18_000,
        "storyBeats": ["proof"], "actions": [
            _paired_svg("fidelity-metrics", "metrics", 250),
            cue("The Android baseline has fifty-four pairs with a ninety-five point five percent median. The iOS baseline has sixty-eight pairs with a ninety-four point four percent median. Those numbers catch movement; bounds, center offsets, size ratios, radius, and human review explain it."),
        ],
    }
    victory = {
        "id": "adopt-the-look-on-your-schedule", "durationMs": 17_000,
        "storyBeats": ["victory"], "actions": [
            _title("The developer chooses when the UI moves", "victory-title"),
            {"type": "image.show", "id": "before-after-ios", "kind": "capture", "atMs": 300,
             "path": "ios-showcase-before-after.jpg",
             "bounds": {"x": 0.08, "y": 0.20, "width": 0.84, "height": 0.53},
             "orientation": {"portrait": {"x": 0.08, "y": 0.23, "width": 0.84, "height": 0.43}}},
            {"type": "text.show", "id": "victory-copy", "atMs": 1_000,
             "text": "Version the reference\nRatchet the baseline\nShip the visual change deliberately",
             "uiid": "VideoBody", "responsive": True, "maxLines": 5,
             "bounds": {"x": 0.16, "y": 0.76, "width": 0.68, "height": 0.19},
             "orientation": {"portrait": {"x": 0.10, "y": 0.70, "width": 0.80, "height": 0.18}}},
            cue("The result is control. Codename One can adopt Liquid Glass and Material 3 deliberately, prove the change against native references, and keep the component implementation inside the shipped app. The operating system cannot silently replace that button after release."),
        ],
    }
    outro = {
        "id": "outro", "durationMs": 15_000, "storyBeats": ["outro"], "actions": [
            {"type": "outro.show", "id": "next", "atMs": 0, "durationMs": 1_100,
             "eyebrow": "CODENAME ONE · NATIVE UI ENGINEERING",
             "title": "Own the pixels. Test the promise.",
             "subtitle": "Next: how ParparVM closed the warmed-JVM performance gap",
             "prompt": "Which component should the fidelity suite test next?",
             "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}},
            cue("The next video shows how ParparVM closed a warmed Java performance gap. Which component should the fidelity suite test next?"),
        ],
    }

    editorial = {
        "storyType": "live-demo",
        "status": "approved",
        "topicTerms": ["pixel fidelity", "native fidelity", "ui fidelity"],
        "proof": [
            "official Apple and Android migration guidance",
            "versioned native-reference captures",
            "compiled TabSelectionMorph geometry",
            "current iOS and Android fidelity reports",
        ],
        "humanBeat": "A developer ships a tested screen, then discovers that a platform redesign changed the interface without an application code change.",
        "visualIdentity": "A moving glass lens crosses a fixed pixel grid; the grid becomes a versioned answer sheet and finally a green regression ratchet.",
        "bespokeVisualization": "Normal, pressed, selected, light, dark, and moving states form a matrix that collapses into one native-reference pipeline.",
        "heroJourney": {
            "identity": "Codename One is introduced subtly as a native Java app platform, and the topic is measurable UI fidelity.",
            "problem": "Platform-owned controls can adopt a new design after the application team has already tested and shipped its screen.",
            "difficulty": "Apple and Android evolve independently, while appearance spans geometry, state, contrast, translucency, and motion.",
            "intervention": "The developer versions native reference captures and compares deterministic Codename One renders in continuous integration.",
            "victory": "The developer chooses when to adopt a visual change and a one-way gate prevents known fidelity from silently regressing.",
        },
        "problemDimensions": ["platform timing", "cross-platform divergence", "state and motion", "subjective review"],
        "resolutionMap": [
            {"problem": "platform timing", "solution": "versioned native golden sets", "proof": "pinned iOS 26 and Android API 36 captures"},
            {"problem": "cross-platform divergence", "solution": "separate UIKit and Material reference applications", "proof": "authentic iOS and Android comparison reports"},
            {"problem": "state and motion", "solution": "fixed state and progress matrices", "proof": "compiled morph demo and seven committed progress frames"},
            {"problem": "subjective review", "solution": "visual scores plus geometric metrics and human review", "proof": "baselines, offsets, ratios, radius, and report captures"},
        ],
    }

    long_scenes = [identity, apple_problem, android_problem, state_problem, intervention,
                   motion_proof, capture_proof, metrics, victory, outro]
    short_scenes = [
        {**identity, "id": "short-who-and-what", "durationMs": 9_000},
        {**apple_problem, "id": "short-platform-change", "durationMs": 14_000},
        {**android_problem, "id": "short-two-platforms", "durationMs": 14_000},
        {**state_problem, "id": "short-state-matrix", "durationMs": 14_000},
        {**intervention, "id": "short-answer-sheet", "durationMs": 15_000},
        {**motion_proof, "id": "short-motion-proof", "durationMs": 30_000},
        {**metrics, "id": "short-metrics", "durationMs": 15_000},
        {**victory, "id": "short-victory", "durationMs": 15_000},
        {"id": "short-outro", "durationMs": 15_000, "storyBeats": ["outro"], "actions": [
            {"type": "outro.show", "id": "short-next", "atMs": 0, "durationMs": 1_000,
             "eyebrow": "CODENAME ONE · UI FIDELITY",
             "title": "Version the look. Test the motion.",
             "subtitle": "The related video shows the full native comparison and compiled model.",
             "prompt": "Which component should the fidelity suite test next?",
             "bounds": {"x": 0, "y": 0, "width": 1, "height": 1}},
            cue("The related video shows the complete native comparison, compiled motion model, and regression gate."),
        ]},
    ]
    common = {"schemaVersion": 1, "editorial": editorial, "output": output, "narration": narration}
    return (
        {**common, "id": SLUG, "title": title, "scenes": long_scenes},
        {**common, "id": f"{SLUG}-short", "title": "Your UI changed without a code change", "scenes": short_scenes},
    )


def red_team(title: str) -> str:
    return f"""# Positioning red-team: {title}

## Verdict

SHIP

## Thesis

Platform-owned UI can change on the platform's schedule; versioned native-reference tests let the developer choose and verify when the product's pixels move.

## Act 1 audit

- The logo is a small signature. A moving lens over a fixed pixel grid owns the frame.
- Codename One and measurable pixel fidelity are both named within eight seconds.
- The opening conflict is understandable without framework knowledge: the pixels moved while the code did not.

## Act 2 audit

- Codename One disappears after the splash until the intervention.
- Apple documentation establishes that standard controls pick up the latest platform appearance.
- Android documentation establishes a separate Material 2 to Material 3 migration across dependencies, themes, colors, typography, shapes, and components.
- Native-only captures show the independent problem without leaking the solution.
- The state matrix escalates from a static screenshot to interaction and motion.

## Act 3 audit

- The intervention names a mechanism: pinned reference applications, versioned goldens, matching renders, and a one-way gate.
- The compiled demo executes the shipped TabSelectionMorph geometry and changes state five times.
- Authentic reports show native on the left and Codename One on the right.
- Every declared problem dimension maps to a concrete mechanism and proof artifact.

## Evidence audit

- Apple and Android claims are attributed to their official documentation.
- iOS and Android images come from the repository's real native-reference and comparison suites.
- The 95.5 and 94.4 percent medians match the canonical article and committed baselines.
- The narration says percentages are regression signals, not proof of perceptual equivalence.

## Interaction audit

- The touch marker triggers a compiled demo action rather than moving a static picture.
- Progress changes position, stretch, lift, magnification, aberration, spring settle, and appearance.
- Replay covers the meaningful transition, rewinds quickly, and replays slowly.
- The code focus names the exact values visible in the running model.

## Hostile-viewer check

- “Custom rendering never looks native.” The video shows current native and Codename One pairs and states the remaining visual differences instead of claiming perfection.
- “A pixel score is meaningless.” The video adds bounds, size, center, radius, motion, and human review, and calls the percentage a gate rather than a verdict.
- “You are just freezing an old design.” The victory is scheduled, tested adoption of new designs, not permanent visual stasis.

## Boldness ruling

Defend control, consistency, and testability. Do not claim universal visual equivalence or native-widget implementation.

## Highest-leverage correction

Keep the platform-owned timing problem ahead of the Codename One mechanism; otherwise this collapses into another theme release announcement.
"""
