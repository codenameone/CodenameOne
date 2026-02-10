#!/usr/bin/env python3
import re
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Optional, Tuple

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "content"
COURSES = [
    CONTENT / "course-01-java-for-mobile-devices.md",
    CONTENT / "course-02-deep-dive-mobile-development-with-codename-one.md",
    CONTENT / "course-03-build-real-world-full-stack-mobile-apps-java.md",
]
OUT_BASE = CONTENT / "courses"


def slugify(s: str) -> str:
    s = s.lower().strip()
    s = re.sub(r"[^a-z0-9]+", "-", s)
    s = re.sub(r"-+", "-", s).strip("-")
    return s or "lesson"


def split_frontmatter(text: str) -> Tuple[str, str]:
    if text.startswith("---\n"):
        end = text.find("\n---\n", 4)
        if end != -1:
            return text[4:end], text[end + 5 :]
    return "", text


def parse_meta(frontmatter: str) -> dict:
    meta = {}
    for line in frontmatter.splitlines():
        line = line.strip()
        if not line or ":" not in line:
            continue
        k, v = line.split(":", 1)
        meta[k.strip()] = v.strip().strip('"')
    return meta


@dataclass
class Lesson:
    module_title: str
    module_order: int
    title: str
    body_lines: List[str] = field(default_factory=list)
    lesson_order_in_module: int = 0
    global_order: int = 0

    def summary(self) -> str:
        for line in self.body_lines:
            t = line.strip()
            if not t:
                continue
            if t.startswith("{{<") or t.startswith("!["):
                continue
            if t.startswith("*") or t.startswith("-"):
                continue
            return t[:190]
        return "Watch the lesson and follow the accompanying resources."


@dataclass
class Module:
    title: str
    order: int
    intro_lines: List[str] = field(default_factory=list)
    lessons: List[Lesson] = field(default_factory=list)


def parse_course(body: str, treat_h2_as_lessons: bool) -> Tuple[List[str], List[Module], Optional[str]]:
    lines = body.splitlines()
    intro_lines: List[str] = []
    modules: List[Module] = []
    hero_line: Optional[str] = None

    current_module: Optional[Module] = None
    current_lesson: Optional[Lesson] = None

    h2_re = re.compile(r"^##\s+(.+?)\s*$")
    h3_re = re.compile(r"^###\s+(.+?)\s*$")

    def flush_lesson():
        nonlocal current_lesson
        if current_lesson and current_module:
            while current_lesson.body_lines and not current_lesson.body_lines[-1].strip():
                current_lesson.body_lines.pop()
            current_module.lessons.append(current_lesson)
        current_lesson = None

    for raw in lines:
        line = raw.rstrip("\n")
        if hero_line is None and line.strip().startswith("![Hero image]"):
            hero_line = line.strip()
            continue

        m2 = h2_re.match(line)
        m3 = h3_re.match(line)

        if m2:
            flush_lesson()
            title = m2.group(1).strip()
            if treat_h2_as_lessons:
                if current_module is None:
                    current_module = Module(title="Course Lessons", order=1)
                    modules.append(current_module)
                current_lesson = Lesson(
                    module_title=current_module.title,
                    module_order=current_module.order,
                    title=title,
                )
            else:
                current_module = Module(title=title, order=len(modules) + 1)
                modules.append(current_module)
            continue

        if m3 and not treat_h2_as_lessons:
            flush_lesson()
            if current_module is None:
                current_module = Module(title="Course Lessons", order=len(modules) + 1)
                modules.append(current_module)
            current_lesson = Lesson(
                module_title=current_module.title,
                module_order=current_module.order,
                title=m3.group(1).strip(),
            )
            continue

        if current_lesson is not None:
            current_lesson.body_lines.append(line)
        elif current_module is not None:
            current_module.intro_lines.append(line)
        else:
            intro_lines.append(line)

    flush_lesson()

    # If a module has intro content but no explicit lessons, make it a lesson
    for module in modules:
        if not module.lessons:
            lesson = Lesson(
                module_title=module.title,
                module_order=module.order,
                title=module.title,
                body_lines=module.intro_lines.copy(),
            )
            module.lessons.append(lesson)
            module.intro_lines = []

    # Assign orders
    global_order = 1
    for module in modules:
        for idx, lesson in enumerate(module.lessons, start=1):
            lesson.lesson_order_in_module = idx
            lesson.global_order = global_order
            global_order += 1

    while intro_lines and not intro_lines[-1].strip():
        intro_lines.pop()

    return intro_lines, modules, hero_line


def write_hub(source: Path, meta: dict, intro: List[str], modules: List[Module], hero_line: Optional[str]):
    title = meta.get("title", source.stem)
    date = meta.get("date", "2024-12-13")
    slug = meta.get("slug", source.stem)
    course_id = slug

    lines: List[str] = [
        "---",
        f'title: "{title}"',
        f"date: {date}",
        f'slug: "{slug}"',
        'layout: "course-hub"',
        f'course_id: "{course_id}"',
        "---",
        "",
    ]
    if hero_line:
        lines += [hero_line, ""]
    lines.extend(intro)
    if lines and lines[-1].strip() != "":
        lines.append("")

    source.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def write_lessons(course_slug: str, title: str, lessons: List[Lesson]):
    course_dir = OUT_BASE / course_slug
    course_dir.mkdir(parents=True, exist_ok=True)

    # clear existing generated lessons
    for old in course_dir.glob("*.md"):
        old.unlink()

    for lesson in lessons:
        fname = f"{lesson.global_order:03d}-{slugify(lesson.title)}.md"
        path = course_dir / fname
        module_key = f"{lesson.module_order:02d}-{slugify(lesson.module_title)}"

        body = [
            "---",
            f'title: "{lesson.title}"',
            'layout: "course-lesson"',
            f'course_id: "{course_slug}"',
            f'course_title: "{title}"',
            f'module_title: "{lesson.module_title}"',
            f'module_key: "{module_key}"',
            f"module_order: {lesson.module_order}",
            f"lesson_order: {lesson.lesson_order_in_module}",
            f"weight: {lesson.global_order}",
            "is_course_lesson: true",
            f'description: "{lesson.summary().replace("\"", "\\\"")}"',
            "---",
            "",
            f"> Module {lesson.module_order}: {lesson.module_title}",
            "",
        ]
        body.extend(lesson.body_lines)
        path.write_text("\n".join(body).rstrip() + "\n", encoding="utf-8")


def process(source: Path):
    text = source.read_text(encoding="utf-8")
    front, body = split_frontmatter(text)
    meta = parse_meta(front)
    slug = meta.get("slug", source.stem)
    title = meta.get("title", source.stem)
    treat_h2_as_lessons = slug == "course-01-java-for-mobile-devices"

    intro, modules, hero_line = parse_course(body, treat_h2_as_lessons)
    lessons = [lesson for module in modules for lesson in module.lessons]

    write_lessons(slug, title, lessons)
    write_hub(source, meta, intro, modules, hero_line)

    print(f"{source.name}: generated {len(lessons)} lesson pages in courses/{slug}/")


def main():
    OUT_BASE.mkdir(parents=True, exist_ok=True)
    for src in COURSES:
        process(src)


if __name__ == "__main__":
    main()
