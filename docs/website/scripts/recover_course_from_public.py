#!/usr/bin/env python3
import re
import html
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "content"
PUBLIC = ROOT / "public"

TARGETS = [
    "course-01-java-for-mobile-devices",
    "course-02-deep-dive-mobile-development-with-codename-one",
    "course-03-build-real-world-full-stack-mobile-apps-java",
]

TOKEN_RE = re.compile(
    r'(?P<h2><h2\s+id="[^"]*">(?P<h2t>.*?)<a\s+hidden)'
    r'|(?P<h3><h3\s+id="[^"]*">(?P<h3t>.*?)<a\s+hidden)'
    r'|(?P<p><p>(?P<pt>.*?)</p>)'
    r'|(?P<li><li>(?P<lit>.*?)</li>)'
    r'|(?P<yt><iframe[^>]*src="https://www\.youtube\.com/embed/(?P<ytid>[A-Za-z0-9_-]+)[^"]*"[^>]*></iframe>)'
    r'|(?P<ss><iframe[^>]*src="https://www\.slideshare\.net/slideshow/embed_code/key/(?P<sskey>[A-Za-z0-9]+)[^"]*"[^>]*></iframe>)',
    flags=re.S,
)


def strip_tags(s: str) -> str:
    s = re.sub(r"<[^>]+>", "", s)
    s = html.unescape(s)
    s = s.replace("\xa0", " ")
    s = re.sub(r"\s+", " ", s).strip()
    return s


def split_frontmatter(text: str):
    if text.startswith("---\n"):
        end = text.find("\n---\n", 4)
        if end != -1:
            return text[: end + 5], text[end + 5 :]
    return "", text


def recover(slug: str):
    hub_path = CONTENT / f"{slug}.md"
    html_path = PUBLIC / slug / "index.html"
    if not hub_path.exists() or not html_path.exists():
        return

    front, hub_body = split_frontmatter(hub_path.read_text(encoding="utf-8"))
    hero_line = ""
    intro_from_hub = []
    for ln in hub_body.splitlines():
        t = ln.strip()
        if not t:
            if intro_from_hub and intro_from_hub[-1] != "":
                intro_from_hub.append("")
            continue
        if t.startswith("![Hero image]"):
            hero_line = t
            continue
        intro_from_hub.append(t)

    raw = html_path.read_text(encoding="utf-8")
    start = raw.find('<div class="post-content">')
    end = raw.find('<footer class="post-footer">')
    if start == -1 or end == -1 or end <= start:
        raise RuntimeError(f"Couldn't parse {html_path}")
    block = raw[start:end]

    out = [front.strip(), ""]
    if hero_line:
        out += [hero_line, ""]

    # Use hub intro if present; fallback to paragraphs before first heading
    html_intro = []
    seen_heading = False
    body_tokens = []

    for m in TOKEN_RE.finditer(block):
        if m.group("h2"):
            seen_heading = True
            body_tokens.append(("h2", strip_tags(m.group("h2t"))))
        elif m.group("h3"):
            seen_heading = True
            body_tokens.append(("h3", strip_tags(m.group("h3t"))))
        elif m.group("p"):
            text = strip_tags(m.group("pt"))
            if not text:
                continue
            if not seen_heading:
                html_intro.append(text)
            else:
                body_tokens.append(("p", text))
        elif m.group("li"):
            text = strip_tags(m.group("lit"))
            if text:
                body_tokens.append(("li", text))
        elif m.group("yt"):
            body_tokens.append(("yt", m.group("ytid")))
        elif m.group("ss"):
            body_tokens.append(("ss", m.group("sskey")))

    intro = intro_from_hub if any(x.strip() for x in intro_from_hub) else html_intro
    if intro:
        out.extend(intro)
        out.append("")

    for typ, val in body_tokens:
        if typ == "h2":
            out += [f"## {val}", ""]
        elif typ == "h3":
            out += [f"### {val}", ""]
        elif typ == "p":
            out += [val, ""]
        elif typ == "li":
            out += [f"* {val}", ""]
        elif typ == "yt":
            out += [f"{{{{< youtube {val} >}}}}", ""]
        elif typ == "ss":
            out += [f"{{{{< slideshare key=\"{val}\" >}}}}", ""]

    text = "\n".join(out).rstrip() + "\n"
    # Collapse overly large blank runs
    text = re.sub(r"\n{3,}", "\n\n", text)
    hub_path.write_text(text, encoding="utf-8")
    print(f"Recovered markdown from public: {hub_path.name}")


if __name__ == "__main__":
    for slug in TARGETS:
        recover(slug)
