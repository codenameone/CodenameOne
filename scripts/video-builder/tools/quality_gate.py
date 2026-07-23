#!/usr/bin/env python3
"""Fail closed when a scripted video has not earned a render or upload."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


FORBIDDEN_NARRATION = (
    "shown as evidence",
    "source excerpt",
    "article section titled",
    "not as a simulated live demo",
    "the complete file and implementation links",
    "this video explains",
    "the boundary",
    "not yet",
    "does not ship",
    "doesn't ship",
    "wait for",
)
SLIDE_ACTIONS = {"intro.show", "bullets.show", "diagram.show", "outro.show", "text.show"}
PROOF_ACTIONS = {"code.show", "demo.mount"}
MINIMUM_PROBLEM_DIMENSIONS = 2


def narration_items(script: dict) -> list[tuple[int, str, str]]:
    result: list[tuple[int, str, str]] = []
    cursor = 0
    for scene in script.get("scenes", []):
        narration = scene.get("narration", {})
        if narration.get("text", "").strip():
            result.append((cursor, scene["id"], narration["text"].strip()))
        for action in scene.get("actions", []):
            if action.get("type") == "narration.cue" and action.get("text", "").strip():
                result.append((cursor + int(action.get("atMs", 0)), scene["id"], action["text"].strip()))
        cursor += int(scene.get("durationMs", 0))
    return sorted(result)


def action_items(script: dict) -> list[tuple[int, str, dict]]:
    result: list[tuple[int, str, dict]] = []
    cursor = 0
    for scene in script.get("scenes", []):
        for action in scene.get("actions", []):
            result.append((cursor + int(action.get("atMs", 0)), scene["id"], action))
        cursor += int(scene.get("durationMs", 0))
    return result


def check(script_path: Path) -> list[str]:
    script = json.loads(script_path.read_text(encoding="utf-8"))
    errors: list[str] = []
    editorial = script.get("editorial", {})
    story_type = editorial.get("storyType", "")
    if editorial.get("status") != "approved":
        errors.append("editorial.status must be approved; generic batch templates may not render")
    if story_type not in {"live-demo", "code-and-capture", "code-deep-dive", "visual-explainer"}:
        errors.append("editorial.storyType must name a reviewed story archetype")
    for field in ("humanBeat", "visualIdentity", "bespokeVisualization"):
        if len(str(editorial.get(field, "")).strip()) < 12:
            errors.append(f"editorial.{field} must describe a topic-specific choice")
    hero = editorial.get("heroJourney", {})
    for field in ("identity", "problem", "difficulty", "intervention", "victory"):
        if len(str(hero.get(field, "")).strip()) < 12:
            errors.append(f"editorial.heroJourney.{field} must state the story beat")
    raw_dimensions = editorial.get("problemDimensions", [])
    problem_dimensions = {str(item).strip().lower() for item in raw_dimensions if str(item).strip()}
    if len(problem_dimensions) < MINIMUM_PROBLEM_DIMENSIONS:
        errors.append(
            "editorial.problemDimensions must declare at least two article-specific difficulties"
        )
    if len(problem_dimensions) != len(raw_dimensions):
        errors.append("editorial.problemDimensions must be non-empty and unique")
    resolution_map = editorial.get("resolutionMap", [])
    mapped_dimensions = {str(item.get("problem", "")).strip().lower()
                         for item in resolution_map if isinstance(item, dict)}
    missing_resolutions = sorted(problem_dimensions - mapped_dimensions)
    if missing_resolutions:
        errors.append("editorial.resolutionMap must resolve: " + ", ".join(missing_resolutions))
    unexpected_resolutions = sorted(mapped_dimensions - problem_dimensions)
    if unexpected_resolutions:
        errors.append(
            "editorial.resolutionMap contains undeclared problems: "
            + ", ".join(unexpected_resolutions)
        )
    if len(mapped_dimensions) != len(resolution_map):
        errors.append("editorial.resolutionMap must map every problem exactly once")
    for index, item in enumerate(resolution_map):
        if (not isinstance(item, dict) or not str(item.get("problem", "")).strip()
                or any(len(str(item.get(field, "")).strip()) < 8
                       for field in ("solution", "proof"))):
            errors.append(f"editorial.resolutionMap[{index}] needs a concrete problem, solution, and proof")

    narration = narration_items(script)
    if not narration:
        errors.append("at least one narration cue is required")
    else:
        first_at, _, first_text = narration[0]
        first_sentence = re.split(r"(?<=[.!?])\s+", first_text, maxsplit=1)[0]
        if first_at < 500:
            errors.append("opening narration must leave at least 500 ms for visual orientation")
        if len(first_sentence.split()) > 22:
            errors.append("opening sentence must be 22 words or fewer")
    for _, scene_id, text in narration:
        lowered = text.lower()
        for phrase in FORBIDDEN_NARRATION:
            if phrase in lowered:
                errors.append(f"{scene_id}: narration contains production-note language: {phrase!r}")
        if re.search(r"\b[A-Z]{2,}/[A-Z]{2,}\b", text):
            errors.append(f"{scene_id}: slash acronym must be rewritten in spoken narration")

    for term, spoken in script.get("narration", {}).get("pronunciations", {}).items():
        if re.search(r"(?:\b[A-Z]\.\s*){2,}", str(spoken)):
            errors.append(
                f"narration pronunciation for {term!r} uses punctuation between letters; "
                "use the shared technical lexicon and phoneme audit"
            )

    for scene in script.get("scenes", []):
        for action in scene.get("actions", []):
            if action.get("type") != "narration.cue":
                continue
            caption = str(action.get("caption", action.get("text", ""))).strip()
            if re.search(r"(?:\b[A-Z]\.\s*){2,}", caption):
                errors.append(f"{scene['id']}: captions must use normal acronym spelling, not phonetic punctuation")

    actions = action_items(script)
    types = [action.get("type") for _, _, action in actions]
    first_scene = script.get("scenes", [{}])[0]
    first_actions = first_scene.get("actions", [])
    first_beats = first_scene.get("storyBeats", [])
    if "identity" not in first_beats:
        errors.append("act one must be an identity splash: who Codename One is and what this video covers")
    brand = next((action for action in first_actions
                  if action.get("type") == "image.show" and action.get("role") == "brand"), None)
    if not brand or int(brand.get("atMs", 0)) > 2_000:
        errors.append("the opening must identify Codename One with a subtle in-scene brand mark")
    elif not (brand.get("path") or brand.get("paths")):
        errors.append("the opening brand mark must use the real Codename One logo")
    else:
        bounds = brand.get("bounds", {})
        if float(bounds.get("width", 1)) * float(bounds.get("height", 1)) > 0.08:
            errors.append("the opening logo must remain a subtle signature inside the splash")
    first_eight_seconds = " ".join(text for at, _, text in narration if at <= 8_000).lower()
    if "codename one" not in first_eight_seconds:
        errors.append("the splash narration must identify Codename One within eight seconds")
    topic_terms = tuple(
        str(term).lower() for term in editorial.get(
            "topicTerms", ("augmented reality", "virtual reality", " ar ", " vr ")
        ) if str(term).strip()
    )
    padded_opening = f" {first_eight_seconds} "
    if not any(term in padded_opening for term in topic_terms):
        errors.append("the splash narration must name the video topic within eight seconds")

    required_beats = ["identity", "problem", "difficulty", "intervention", "proof", "victory", "outro"]
    beats = [beat for scene in script.get("scenes", []) for beat in scene.get("storyBeats", [])]
    cursor = -1
    for beat in required_beats:
        try:
            cursor = beats.index(beat, cursor + 1)
        except ValueError:
            errors.append(f"hero journey is missing or misorders the {beat!r} beat")
            break
    beat_times: dict[str, int] = {}
    scene_cursor = 0
    for scene in script.get("scenes", []):
        for beat in scene.get("storyBeats", []):
            beat_times.setdefault(beat, scene_cursor)
        scene_cursor += int(scene.get("durationMs", 0))
    intervention_at = beat_times.get("intervention")
    if intervention_at is not None and intervention_at < 28_000:
        errors.append("act two is too short: establish the independent real-world difficulty before the solution")
    if intervention_at is not None:
        identity_scene_id = first_scene.get("id")
        for at, scene_id, text in narration:
            if at >= intervention_at:
                break
            if scene_id != identity_scene_id and "codename one" in text.lower():
                errors.append(f"{scene_id}: solution quarantine violation; keep Codename One out of act two")
        for at, scene_id, action in actions:
            if at >= intervention_at:
                break
            if action.get("type") in PROOF_ACTIONS:
                errors.append(f"{scene_id}: product code/demo appears before the intervention")
    evidence = [(at, scene_id, action) for at, scene_id, action in actions
                if action.get("type") in {"image.show", "svg.show"}
                and action.get("role") == "evidence"]
    early_evidence = [item for item in evidence if intervention_at is None or item[0] < intervention_at]
    evidence_hosts = {str(action.get("sourceUrl", "")).split("/")[2]
                      for _, _, action in early_evidence
                      if str(action.get("sourceUrl", "")).startswith(("http://", "https://"))}
    if len(early_evidence) < 2 or len(evidence_hosts) < 2:
        errors.append("act two requires at least two independently sourced evidence visuals before the intervention")
    modes = set()
    if "code.show" in types or "code.type" in types:
        modes.add("code")
    if "demo.mount" in types:
        modes.add("demo")
    if any(action.get("type") == "image.show" and action.get("kind") == "capture"
           for _, _, action in actions):
        modes.add("capture")
    if any(action.get("type") == "image.show" and action.get("kind") == "illustration"
           for _, _, action in actions):
        modes.add("illustration")
    if "svg.show" in types or "diagram.show" in types:
        modes.add("visualization")
    if any(action in types for action in ("pointer.show", "pointer.click", "focus.show", "replay")):
        modes.add("interaction")
    if len(modes) < 3:
        errors.append("the edit requires at least three visual modes")
    first_proof = min((at for at, _, action in actions if action.get("type") in PROOF_ACTIONS),
                      default=None)
    if first_proof is None:
        errors.append("act three requires code or a compiled running demo")
    elif intervention_at is not None and first_proof < intervention_at:
        errors.append("product proof must follow the intervention")
    elif intervention_at is not None and first_proof > intervention_at + 35_000:
        errors.append("show code or a compiled demo within 35 seconds of introducing the solution")
    if story_type == "live-demo" and not {"code.show", "demo.mount"}.issubset(types):
        errors.append("live-demo stories require both code.show and demo.mount")
    if story_type == "live-demo":
        demo_actions = [action for _, _, action in actions if action.get("type") == "demo.action"]
        commands = {str(action.get("action", action.get("command", action.get("name", "")))).strip()
                    for action in demo_actions}
        commands.discard("")
        if len(demo_actions) < 2 or len(commands) < 2:
            errors.append("live-demo stories require at least two distinct compiled demo state changes")
    has_capture = any(action.get("type") == "image.show" and action.get("kind") == "capture"
                      for _, _, action in actions)
    if story_type == "code-and-capture" and ("code.show" not in types or not has_capture):
        errors.append("code-and-capture stories require both code.show and image.show")
    if story_type == "code-deep-dive" and (types.count("code.show") < 2 or "focus.show" not in types):
        errors.append("code-deep-dive stories require two code listings and a focus callout")
    capture_ids = {action.get("id") for _, _, action in actions
                   if action.get("type") == "image.show" and action.get("kind") == "capture"}
    pointer_areas = {action.get("area") for _, _, action in actions
                     if action.get("type") in {"pointer.show", "pointer.move"}}
    if story_type == "live-demo" and capture_ids & pointer_areas:
        errors.append("moving or clicking over a capture is not live interaction; target the compiled demo")
    for at, scene_id, action in actions:
        if action.get("type") != "pointer.move":
            continue
        intent = str(action.get("semanticIntent", "")).strip()
        matching_state_change = any(
            candidate_scene == scene_id
            and candidate.get("type") == "demo.action"
            and at <= candidate_at <= at + int(action.get("durationMs", 0)) + 1_000
            for candidate_at, candidate_scene, candidate in actions
        )
        if not intent or not matching_state_change:
            errors.append(
                f"{scene_id}: pointer movement must describe a real gesture with semanticIntent "
                "and drive a compiled demo.action"
            )

    consecutive_slides = 0
    for scene in script.get("scenes", []):
        visual_types = {
            action.get("type") for action in scene.get("actions", [])
            if action.get("type") not in {"transition", "layer.hide", "narration.cue"}
        }
        if visual_types and visual_types.issubset(SLIDE_ACTIONS):
            consecutive_slides += 1
            if consecutive_slides > 2:
                errors.append("more than two consecutive slide-only scenes")
                break
        else:
            consecutive_slides = 0

    red_team = script_path.with_name("red-team.md")
    if not red_team.is_file():
        errors.append("red-team.md is required")
    else:
        text = red_team.read_text(encoding="utf-8")
        verdict = re.search(r"^## Verdict\s*\n+([^\n]+)", text, re.M)
        if not verdict or verdict.group(1).strip() != "SHIP":
            errors.append("red-team verdict must be exactly SHIP after corrections are applied")
        for heading in ("Act 1 audit", "Act 2 audit", "Act 3 audit", "Evidence audit", "Interaction audit"):
            if not re.search(rf"^## {re.escape(heading)}\s*$", text, re.M):
                errors.append(f"red-team.md is missing the {heading!r} section")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("scripts", nargs="+", type=Path)
    args = parser.parse_args()
    failures = {str(path): check(path) for path in args.scripts}
    failures = {path: errors for path, errors in failures.items() if errors}
    print(json.dumps({"ok": not failures, "failures": failures}, indent=2))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
