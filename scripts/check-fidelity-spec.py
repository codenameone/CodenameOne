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

SPEC = Path(__file__).resolve().parent / "fidelity-app/common/src/main/resources/fidelity-tests.yaml"

MOBILE_KEYS = {"native", "native_android"}
DESKTOP_KEYS = {"native_win", "native_mac", "native_gnome"}
KNOWN_KEYS = MOBILE_KEYS | DESKTOP_KEYS | {
    "id", "cn1_uiid", "text", "backdrop", "material", "states", "platforms", "frames",
    "tile_width_mm", "tile_height_mm", "tile_width_px", "tile_height_px",
}
KNOWN_STATES = {"normal", "pressed", "disabled", "selected", "hover", "focus"}

def parse(path):
    """The same flat subset FidelitySpecParser accepts: 2-space indent, no anchors, no flow."""
    rows, cur, in_components = [], None, False
    for n, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.split("#", 1)[0].rstrip() if not raw.lstrip().startswith("#") else ""
        if not line.strip():
            continue
        if line.startswith("components:"):
            in_components = True
            continue
        if not in_components:
            continue
        if "\t" in raw:
            yield_error(f"line {n}: tab character; the on-device parser rejects tabs")
        m = re.match(r"^  - (\w+):\s*(.*)$", line)
        if m:
            cur = {"__line": n, m.group(1): m.group(2).strip()}
            rows.append(cur)
            continue
        m = re.match(r"^    (\w+):\s*(.*)$", line)
        if m and cur is not None:
            cur[m.group(1)] = m.group(2).strip()
    return rows

ERRORS = []
def yield_error(msg):
    ERRORS.append(msg)

def main():
    if not SPEC.exists():
        print(f"check-fidelity-spec: {SPEC} not found", file=sys.stderr)
        return 2
    rows = parse(SPEC)
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

        for k in r:
            if k != "__line" and k not in KNOWN_KEYS:
                yield_error(f"{where}: unknown key '{k}'. The on-device parser ignores what it "
                            f"does not recognise, so this silently does nothing")

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

        # Hover is a desktop state. A mobile row asking for it would never be captured, because
        # a touch device has no pointer to hover with.
        if has_mobile and not has_desktop and "hover" in r.get("states", ""):
            yield_error(f"{where}: hover on a mobile row; a touch device has no hover to capture")

    for e in ERRORS:
        print(f"check-fidelity-spec: {e}", file=sys.stderr)
    if ERRORS:
        print(f"check-fidelity-spec: {len(ERRORS)} problem(s) in {SPEC.name}", file=sys.stderr)
        return 1
    print(f"check-fidelity-spec: {len(rows)} components ({mobile} mobile, {desktop} desktop), "
          f"{SPEC.name} is consistent")
    return 0

if __name__ == "__main__":
    sys.exit(main())
