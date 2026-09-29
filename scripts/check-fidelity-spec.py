#!/usr/bin/env python3
"""Validate fidelity-tests.yaml, which three separate native reference apps must agree with.

The file is parsed on-device by FidelitySpecParser, a restricted flat-YAML reader that
silently ignores anything it does not recognise. That is what makes it robust to new keys,
and also what makes a typo invisible: `native_windows:` instead of `native_win:` is not an
error, it simply means the row never runs on Windows and nobody is told why.

Each native reference app additionally carries its own hard-coded table of widget keys,
because none of them can link the on-device parser. Nothing makes those tables agree with
this file except a check like this one.

Deliberately dependency-free: it runs on a bare runner with no pip install, and it parses the
same restricted subset the on-device reader does rather than using a real YAML library, so it
cannot accept a file the device would reject.
"""
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
SPEC = Path(__file__).resolve().parent / "fidelity-app/common/src/main/resources/fidelity-tests.yaml"

RENDERER = REPO / "scripts/fidelity-app/common/src/main/java/com/codenameone/fidelity/render/Cn1WidgetRenderer.java"

MOBILE_KEYS = {"native", "native_android"}
DESKTOP_KEYS = {"native_win", "native_mac", "native_gnome"}
KNOWN_KEYS = MOBILE_KEYS | DESKTOP_KEYS | {
    "id", "cn1_uiid", "text", "backdrop", "material", "states", "platforms", "frames",
    "golden_sets", "motion_checks", "tile_width_mm", "tile_height_mm", "tile_width_px", "tile_height_px",
}
# Use fixture runner IDs, not host names: getNativeKind("linux") resolves a key,
# but a platforms: linux filter does not match the workflow's "gnome" runner.
PLATFORM_NATIVE_KEYS = {"ios": "native", "android": "native_android", "windows": "native_win",
                        "macos": "native_mac", "gnome": "native_gnome"}
KNOWN_PLATFORMS = set(PLATFORM_NATIVE_KEYS)
KNOWN_STATES = {"normal", "pressed", "disabled", "selected", "hover", "focus"}
KNOWN_MATERIALS = {"normal", "glass", "lens"}
KNOWN_BACKDROPS = {"photo", "gradient", "grouped"}
# motion_checks selects MorphFrameValidator's property checks for a frames row.
KNOWN_MOTION_CHECKS = {"monotonic", "distinct"}
GOLDENS_DIR = REPO / "scripts/fidelity-app/goldens"
TILE_KEYS = {"tile_width_mm", "tile_height_mm", "tile_width_px", "tile_height_px"}

DEFAULT_KEYS = TILE_KEYS | {"bg", "appearances"}


def unquote(value):
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        return value[1:-1]
    return value


def check_tile_dimension(key, value, where):
    try:
        valid = re.fullmatch(r"\+?[0-9]+", value) and 0 < int(value) <= 2147483647
    except ValueError:
        valid = False
    if not valid:
        yield_error(f"{where}: '{key}' must be a positive Java integer")


def check_frames(value, where):
    seen = set()
    for token in value.split(","):
        token = token.strip()
        # The runner pads the raw token into _tNNN filenames; the comparator requires
        # exactly three digits. Signs and overlong zero-padding are not valid frames.
        if not re.fullmatch(r"[0-9]{1,3}", token) or int(token) > 100:
            yield_error(f"{where}: invalid frame '{token}'; use integers from 0 to 100")
            continue
        frame = int(token)
        if frame in seen:
            yield_error(f"{where}: duplicate frame value {frame}")
        seen.add(frame)


def check_default(key, value, line):
    where = f"defaults (line {line})"
    if key not in DEFAULT_KEYS:
        yield_error(f"{where}: unknown default key '{key}'; the on-device parser ignores it")
    elif key.startswith("tile_"):
        check_tile_dimension(key, value, where)
    elif key == "bg" and not re.fullmatch(r"[0-9a-fA-F]{6}", value):
        yield_error(f"{where}: bg must be a six-digit hexadecimal color")
    elif key == "appearances":
        appearances = [unquote(item.strip()) for item in value.split(",")]
        if not appearances or any(item not in {"light", "dark"} for item in appearances):
            yield_error(f"{where}: appearances must contain light and/or dark")
        elif len(appearances) != len(set(appearances)):
            yield_error(f"{where}: duplicate appearance would overwrite captures")


def parse(path):
    """Validate the documented flat subset before the permissive device parser sees it."""
    rows, cur, section = [], None, None
    sections, defaults = set(), set()
    for n, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        if "\t" in raw:
            yield_error(f"line {n}: tab character; use the documented space indentation")
        if line == line.lstrip():
            match = re.fullmatch(r"(defaults|components):", line)
            if not match:
                yield_error(f"line {n}: expected a defaults: or components: section")
                section = None
                continue
            section = match.group(1)
            if section in sections:
                yield_error(f"line {n}: duplicate {section}: section")
            sections.add(section)
            cur = None
            continue
        if section == "defaults":
            match = re.fullmatch(r"  (\w+):\s*(.*)", line)
            if not match:
                yield_error(f"line {n}: defaults need two-space indentation and key: value")
                continue
            key, value = match.group(1), unquote(match.group(2).strip())
            if key in defaults:
                yield_error(f"line {n}: duplicate default key '{key}'")
            defaults.add(key)
            check_default(key, value, n)
            continue
        if section == "components":
            match = re.fullmatch(r"  - (\w+):\s*(.*)", line)
            if match:
                cur = {"__line": n, match.group(1): unquote(match.group(2).strip())}
                rows.append(cur)
                continue
            match = re.fullmatch(r"    (\w+):\s*(.*)", line)
            if match and cur is not None:
                key = match.group(1)
                if key in cur:
                    yield_error(f"line {n}: duplicate component key '{key}'")
                cur[key] = unquote(match.group(2).strip())
                continue
        yield_error(f"line {n}: malformed or misplaced field in the flat fidelity spec")
    if not rows:
        yield_error("components: must contain at least one component")
    return rows

ERRORS = []
def yield_error(msg):
    ERRORS.append(msg)

def renderer_support():
    # Read the renderer's own literal IDs/prefixes rather than maintain a second list.
    # The device runner skips unknown IDs, so validate them before any capture starts.
    source = RENDERER.read_text()
    method = re.search(r"public static boolean isSupported\(String id\)\s*\{(.*?)^    \}", source, re.S | re.M)
    if not method:
        yield_error("cannot locate Cn1WidgetRenderer.isSupported")
        return set(), ()
    body = re.sub(r"//[^\n]*|/\*.*?\*/", "", method.group(1), flags=re.S)
    ids = set(re.findall(r'"([^"\n]+)"\.equals\(id\)', body))
    prefixes = tuple(re.findall(r'id\.startsWith\("([^"\n]+)"\)', body))
    if not ids:
        yield_error("cannot read supported CN1 renderer IDs")
    return ids, prefixes


def main():
    if not SPEC.exists():
        print(f"check-fidelity-spec: {SPEC} not found", file=sys.stderr)
        return 2
    ERRORS.clear()
    rows = parse(SPEC)
    renderer_ids, renderer_prefixes = renderer_support()
    seen = set()
    desktop = mobile = 0

    for r in rows:
        rid = r.get("id")
        where = f"{rid or '(no id)'} (line {r['__line']})"
        if not rid:
            yield_error(f"{where}: every component needs an id; it names the screenshot")
        elif rid in seen:
            yield_error(f"{where}: duplicate id, so one row's tiles would overwrite the other's")
        seen.add(rid)
        if rid and rid not in renderer_ids and not rid.startswith(renderer_prefixes):
            yield_error(f"{where}: unsupported CN1 renderer id")

        for k in r:
            if k != "__line" and k not in KNOWN_KEYS:
                yield_error(f"{where}: unknown key '{k}'. The on-device parser ignores what it "
                            f"does not recognise, so this silently does nothing")

        # ProcessScreenshots falls back to normal for unknown values, which can count
        # the shared backdrop as matching widget content. Explicit intent must be valid.
        if "material" in r and r["material"] not in KNOWN_MATERIALS:
            yield_error(f"{where}: unknown material '{r['material']}' "
                        f"(known: {sorted(KNOWN_MATERIALS)})")

        if "backdrop" in r and r["backdrop"] not in KNOWN_BACKDROPS:
            if not re.fullmatch(r"[0-9a-fA-F]{6}", r["backdrop"]):
                yield_error(f"{where}: backdrop must be photo, gradient, grouped, "
                            "or a six-digit hexadecimal color")
        for key in sorted(TILE_KEYS & set(r)):
            check_tile_dimension(key, r[key], where)

        if "frames" in r:
            check_frames(r["frames"], where)
        for key in ("golden_sets", "motion_checks"):
            if key in r and "frames" not in r:
                yield_error(f"{where}: '{key}' only applies to a frames row")
        for gs in [unquote(g.strip()) for g in r.get("golden_sets", "").split(",") if g.strip()]:
            # A misspelled set would silently exempt the row from every run.
            if not (GOLDENS_DIR / gs).is_dir():
                yield_error(f"{where}: unknown golden set '{gs}' (no goldens/{gs} directory)")
        if "motion_checks" in r and unquote(r["motion_checks"]) not in KNOWN_MOTION_CHECKS:
            yield_error(f"{where}: unknown motion_checks '{r['motion_checks']}' "
                        f"(known: {sorted(KNOWN_MOTION_CHECKS)})")

        has_desktop = bool(DESKTOP_KEYS & set(r))
        has_mobile = bool(MOBILE_KEYS & set(r))
        if has_desktop:
            desktop += 1
        if has_mobile:
            mobile += 1

        # A desktop row missing one platform's key runs on two platforms and not the third,
        # which reads as a gap in the results rather than as a mistake in this file.
        if has_desktop:
            missing = DESKTOP_KEYS - set(r)
            if missing and "platforms" not in r:
                yield_error(f"{where}: declares {sorted(DESKTOP_KEYS & set(r))} but not "
                            f"{sorted(missing)}. Add the key, or a platforms: list saying the "
                            f"omission is deliberate")
        if has_desktop and has_mobile:
            yield_error(f"{where}: mixes mobile and desktop native keys. Tiles are sized in mm "
                        f"on mobile and px on desktop, so one row cannot be both")

        if has_desktop and ("tile_width_mm" in r or "tile_height_mm" in r):
            yield_error(f"{where}: a desktop row sized in mm. Desktop toolkits are specified in "
                        f"logical pixels; use tile_width_px / tile_height_px")
        if has_mobile and ("tile_width_px" in r or "tile_height_px" in r):
            yield_error(f"{where}: a mobile row sized in px; use tile_width_mm / tile_height_mm")

        for st in [s.strip() for s in r.get("states", "").split(",") if s.strip()]:
            if st not in KNOWN_STATES:
                yield_error(f"{where}: unknown state '{st}' (known: {sorted(KNOWN_STATES)})")

        platforms = [unquote(p.strip()) for p in r.get("platforms", "").split(",") if p.strip()]
        for platform in platforms:
            # ComponentSpec deliberately matches prefixes in either direction. In
            # particular, "window" is valid for "windows", as are "win" and "and".
            if not any(host.startswith(platform) or platform.startswith(host)
                       for host in KNOWN_PLATFORMS):
                yield_error(f"{where}: unknown platform '{platform}'")

        # Frame captures are CN1-only and need no native reference key. Ordinary rows
        # must reach a declared native target after applying the runtime's prefix filter.
        targets = set(PLATFORM_NATIVE_KEYS) if r.get("frames") else {
            host for host, key in PLATFORM_NATIVE_KEYS.items() if r.get(key)
        }
        if not targets or platforms and not any(
                host.startswith(platform) or platform.startswith(host)
                for host in targets for platform in platforms):
            yield_error(f"{where}: platforms allow-list excludes every declared capture target")

        # Hover is a desktop state. A mobile row asking for it would never be captured, because
        # a touch device has no pointer to hover with.
        if has_mobile and not has_desktop and "hover" in r.get("states", ""):
            yield_error(f"{where}: hover on a mobile row; a touch device has no hover to capture")

    check_desktop_labels(rows)

    for e in ERRORS:
        print(f"check-fidelity-spec: {e}", file=sys.stderr)
    if ERRORS:
        print(f"check-fidelity-spec: {len(ERRORS)} problem(s) in {SPEC.name}", file=sys.stderr)
        return 1
    print(f"check-fidelity-spec: {len(rows)} components ({mobile} mobile, {desktop} desktop), "
          f"{SPEC.name} is consistent")
    return 0


# The three native reference apps HARDCODE the label each widget shows -- they are written
# in C, Swift and C# and cannot read this YAML. The CN1 side reads it from here. When the
# two disagree the comparison is between two different strings, which is a permanently
# unreachable score rather than a visible failure: "Hello" against "Text" simply scores low
# forever and reads as a theme that needs work.
#
# It had drifted on every one of the six desktop rows that carry text.
NATIVE_REF_SOURCES = {
    "windows": REPO / "scripts/fidelity-app/windows-native-ref/Program.cs",
    "macos": REPO / "scripts/fidelity-app/macos-native-ref/NativeRef.swift",
    "gnome": REPO / "scripts/fidelity-app/gnome-native-ref/native-ref.c",
}

# id -> the literal each reference app is expected to construct its widget with.
DESKTOP_LABEL_LITERALS = {
    "DesktopButton": "Button",
    "DesktopAccentButton": "Button",
    "DesktopTextField": "Text",
    "DesktopCheckBox": "Check",
    "DesktopRadioButton": "Radio",
    "DesktopComboBox": "Option",
    # Second wave. Every text-bearing row belongs here: this table is what caught all six of
    # the first wave rendering different strings on the two sides, which capped the text
    # field at 65% until it was found.
    "DesktopGroupBox": "Group",
    "DesktopLinkButton": "Link",
    "DesktopSearchField": "Search",
    "DesktopListRow": "Row",
    "DesktopDisclosure": "Details",
    "DesktopMenuBar": "File",
    "DesktopMenuItem": "Open",
    "DesktopTooltip": "Tooltip",
}


def check_desktop_labels(rows):
    """The spec's `text` for each desktop row must be the literal the reference apps use."""
    for r in rows:
        rid = r.get("id")
        if rid not in DESKTOP_LABEL_LITERALS:
            continue
        want = DESKTOP_LABEL_LITERALS[rid]
        got = r.get("text")
        if got != want:
            yield_error(f"{rid}: text is '{got}' but the native reference apps render "
                        f"'{want}'. The two sides would compare different strings, which "
                        f"caps the score at something no theme change can reach")

    # Bind the row to its native kind, then inspect only that kind's widget
    # constructor. A literal elsewhere (e.g. the accent button) proves nothing.
    for platform, path in NATIVE_REF_SOURCES.items():
        if not path.is_file():
            yield_error(f"{path.name}: missing; cannot verify the {platform} labels")
            continue
        src = path.read_text(encoding="utf-8", errors="replace")
        # Remove comments without treating comment markers inside strings as comments.
        src = re.sub(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*.*?\*/',
                     lambda m: m.group() if m.group().startswith('"') else '', src, flags=re.S)
        for row in rows:
            rid = row.get("id")
            if rid not in DESKTOP_LABEL_LITERALS:
                continue
            key = {"windows": "native_win", "macos": "native_mac", "gnome": "native_gnome"}[platform]
            kind = row.get(key)
            if not kind:
                continue
            mapping, actual = native_label(platform, src, rid, kind)
            if mapping != kind:
                yield_error(f"{path.name}: {rid} maps to '{mapping}', but the spec uses '{kind}'")
            want = row.get("text")
            if actual != want:
                yield_error(f"{path.name}: {rid} ({kind}) renders '{actual}', expected '{want}'")


def matched(pattern, source, group=1):
    match = re.search(pattern, source, re.S | re.M)
    return match.group(group) if match else None


def first_nonempty(pattern, source, group=1):
    """The first NON-EMPTY capture, not simply the first.

    A composite reference builds more than one labelled thing, and the one that carries the
    text is not always first: the macOS disclosure is an empty-titled NSButton for the
    triangle followed by the label that actually says "Details". Taking match one there
    reads the empty string and reports drift that is not there.
    """
    for match in re.finditer(pattern, source, re.S | re.M):
        if match.group(group):
            return match.group(group)
    return None


def native_label(platform, src, rid, kind):
    """Read the deliberately small reference-app constructor tables, failing closed on drift."""
    rid, kind = re.escape(rid), re.escape(kind)
    if platform == "windows":
        mapping = matched(r'new\(\s*"' + rid + r'"\s*,\s*"([^"\n]+)"', src)
        body = matched(r'^\s*"' + kind + r'"\s*=>\s*(.*?)(?=^\s*(?:"\w+"|_)\s*=>)', src) or ""
        # A kind whose arm is just a factory call: follow it into that method's body, so the
        # literal is still read from the one place that builds this kind and not from a
        # neighbouring arm.
        factory = matched(r'^\s*(Make\w+)\(\),\s*$', body)
        if factory:
            body = matched(r'\b' + factory + r'\(\)\s*\{(.*?)^\s*\}', src) or ""
            label = first_nonempty(
                r'(?:\.Items\.Add\(\s*|\b(?:Content|Text|Header|Title)\s*=\s*)"([^"\n]*)"',
                body)
        else:
            label = first_nonempty(r'\b(?:Content|Text|Header|Title)\s*=\s*"([^"\n]*)"', body)
    elif platform == "macos":
        mapping = matched(r'Spec\(id:\s*"' + rid + r'",\s*kind:\s*"([^"\n]+)"', src)
        body = matched(r'case "' + kind + r'":(.*?)(?=^\s*(?:case |default:))', src) or ""
        label = first_nonempty(
            r'(?:NS(?:Button|SearchField|TextField)\((?:title|checkboxWithTitle|'
            r'radioButtonWithTitle|string|labelWithString):|\.addItem\(withTitle:|'
            r'\w+\.title\s*=)\s*"([^"\n]*)"', body)
    else:
        mapping = matched(r'\{\s*"' + rid + r'",\s*"([^"\n]+)"', src)
        # Scope to make_widget: other functions also branch on these kind strings.
        factory = src.split('static GtkWidget *make_widget(', 1)[-1]
        body = matched(r'if \(strcmp\(kind, "' + kind + r'"\) == 0\) \{(.*?)'
                       r'(?=^    if \(strcmp\(kind,|^    blocker\()', factory) or ""
        label = first_nonempty(
            r'(?:gtk_(?:button|check_button)_new_with_label\(|'
            r'gtk_editable_set_text\(GTK_EDITABLE\(\w+\),|'
            r'gtk_link_button_new_with_label\("[^"\n]*",\s*|'
            r'gtk_(?:frame|expander|label)_new\(|'
            r'g_menu_append_submenu\(\w+,\s*|'
            r'adw_window_title_new\(|'
            r'const char \*items\[\]\s*=\s*\{)\s*"([^"\n]*)"', body)
    return mapping, label


if __name__ == "__main__":
    sys.exit(main())
