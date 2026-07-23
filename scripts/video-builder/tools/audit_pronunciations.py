#!/usr/bin/env python3
"""Validate and optionally synthesize the shared Kokoro technical lexicon."""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INDEX = ROOT / "pronunciations" / "technical-en-us.json"
DEFAULT_MODEL = ROOT / "examples" / "release-feature" / ".video-tools" / "kokoro" / "kokoro-v1.0.onnx"
AUDITION = (
    "Codename One uses ARKit and ARCore.\n"
    "The API runs on iOS.\n"
    "JIT and AOT compilation use clang.\n"
    "WidgetKit and ActivityKit render widgets and Live Activities.\n"
    "RemoteViews powers Android widgets.\n"
    "A Dynamic Island update carries the ETA from JavaSE without waking the EDT."
)


def kokoro_executable() -> Path:
    found = shutil.which("kokoro-tts")
    if found:
        return Path(found)
    candidate = Path.home() / ".local" / "bin" / "kokoro-tts"
    if candidate.is_file():
        return candidate
    raise RuntimeError("kokoro-tts is not installed")


def tool_python(executable: Path) -> Path:
    first_line = executable.read_text(encoding="utf-8").splitlines()[0]
    if not first_line.startswith("#!"):
        raise RuntimeError(f"cannot resolve Kokoro Python from {executable}")
    candidate = Path(first_line[2:])
    if not candidate.is_file():
        raise RuntimeError(f"Kokoro Python does not exist: {candidate}")
    return candidate


def apply_entries(text: str, entries: dict[str, str]) -> str:
    result = text
    for source in sorted(entries, key=len, reverse=True):
        prefix = r"(?<![A-Za-z0-9_])" if source[0].isalnum() or source[0] == "_" else ""
        suffix = r"(?![A-Za-z0-9_])" if source[-1].isalnum() or source[-1] == "_" else ""
        result = re.sub(prefix + re.escape(source) + suffix, entries[source], result)
    return result


def phonemize(python: Path, terms: dict[str, str], language: str) -> dict[str, str]:
    program = """
import json, sys
from kokoro_onnx import Tokenizer
payload = json.loads(sys.stdin.read())
tokenizer = Tokenizer()
print(json.dumps({key: tokenizer.phonemize(value, payload['language'])
                  for key, value in payload['terms'].items()}, ensure_ascii=False))
"""
    result = subprocess.run(
        [str(python), "-c", program], input=json.dumps({"terms": terms, "language": language}),
        text=True, stdout=subprocess.PIPE, check=True
    )
    return json.loads(result.stdout)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--index", type=Path, default=DEFAULT_INDEX)
    parser.add_argument("--audition", type=Path)
    parser.add_argument("--model", type=Path, default=DEFAULT_MODEL)
    parser.add_argument("--voice", default="af_heart:60,af_bella:40")
    args = parser.parse_args()

    index = json.loads(args.index.read_text(encoding="utf-8"))
    entries = {str(key): str(value) for key, value in index["entries"].items()}
    expected = {str(key): str(value) for key, value in index["expectedPhonemes"].items()}
    dotted = {key: value for key, value in entries.items()
              if re.search(r"(?:\b[A-Z]\.\s*){2,}", value)}
    if dotted:
        raise RuntimeError(f"punctuation-driven acronym pronunciations are forbidden: {dotted}")

    executable = kokoro_executable()
    python = tool_python(executable)
    actual = phonemize(python, {key: entries[key] for key in expected}, index["language"])
    mismatches = {key: {"expected": expected[key], "actual": actual.get(key)}
                  for key in expected if actual.get(key) != expected[key]}
    if mismatches:
        raise RuntimeError("phoneme regression: " + json.dumps(mismatches, ensure_ascii=False))

    audition_path = None
    spoken = apply_entries(AUDITION, entries)
    if args.audition:
        audition_path = args.audition.resolve()
        audition_path.parent.mkdir(parents=True, exist_ok=True)
        voices = args.model.resolve().with_name("voices-v1.0.bin")
        with tempfile.TemporaryDirectory(prefix="cn1-pronunciation-") as temporary:
            source = Path(temporary) / "audition.txt"
            source.write_text(spoken, encoding="utf-8")
            subprocess.run([
                str(executable), str(source), str(audition_path), "--format", "wav",
                "--speed", "0.97", "--lang", index["language"], "--voice", args.voice,
                "--model", str(args.model.resolve()), "--voices", str(voices),
            ], check=True)
        if not audition_path.is_file() or audition_path.stat().st_size < 44:
            raise RuntimeError("Kokoro reported success but did not create a valid audition WAV")

    print(json.dumps({
        "ok": True,
        "index": str(args.index.resolve()),
        "spoken": spoken,
        "phonemes": actual,
        "audition": str(audition_path) if audition_path else None,
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
