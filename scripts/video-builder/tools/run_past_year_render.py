#!/usr/bin/env python3
"""Measure, fit, render, and verify one past-year YouTube video pair."""

from __future__ import annotations

import argparse
import fcntl
import hashlib
import json
import math
import os
import pty
import re
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[3]
VIDEO_ROOT = ROOT / "scripts" / "video-builder"
PROJECT_ROOT = VIDEO_ROOT / "projects" / "past-year"
GENERATOR = VIDEO_ROOT / "tools" / "generate_past_year_packages.py"
DEFAULT_JAVA = (
    Path(os.environ["JAVA17_HOME"]) / "bin" / "java"
    if os.environ.get("JAVA17_HOME")
    else Path(shutil.which("java") or "java")
)
DEFAULT_JAR = VIDEO_ROOT / "javase" / "target" / "codenameone-video-builder-8.0-SNAPSHOT.jar"
DEFAULT_OUTPUT = Path(tempfile.gettempdir()) / "cn1-youtube-past-year"
PACKAGE_VALIDATOR = Path(os.environ.get(
    "CN1_YOUTUBE_PACKAGE_VALIDATOR",
    Path.home() / ".codex" / "skills" / "cn1-syndication-strategist"
    / "scripts" / "validate_youtube_package.py",
))
QUALITY_GATE = VIDEO_ROOT / "tools" / "quality_gate.py"


RUNTIME_FAILURE = re.compile(
    r"(?im)(?:Exception in thread|Caused by:\s+\S+(?:Exception|Error)|"
    r"\b(?:java|javax|com\.codename1)\.[\w.$]+(?:Exception|Error):|^\s*FATAL\b)"
)


def run(args: list[str], capture: bool = False, log_path: Path | None = None,
        reject_runtime_errors: bool = False) -> str:
    print("+", " ".join(args), flush=True)
    if not capture and log_path is None and not reject_runtime_errors:
        subprocess.run(args, cwd=ROOT, check=True)
        return ""
    if log_path is None:
        process = subprocess.run(
            args, cwd=ROOT, check=True, text=True, stdout=subprocess.PIPE,
            stderr=None,
        )
        return process.stdout or ""

    # The JavaSE port expects a terminal on macOS. A plain stdout/stderr pipe can
    # make the native startup abort before Java emits an exception. Keep a PTY,
    # but capture every byte so a nominally successful renderer cannot hide an
    # EDT or native exception in its log.
    master, slave = pty.openpty()
    process = subprocess.Popen(args, cwd=ROOT, stdout=slave, stderr=slave)
    os.close(slave)
    chunks: list[bytes] = []
    try:
        while True:
            try:
                block = os.read(master, 64 * 1024)
            except OSError:
                break
            if not block:
                break
            chunks.append(block)
            sys.stdout.buffer.write(block)
            sys.stdout.buffer.flush()
    finally:
        os.close(master)
    return_code = process.wait()
    output = b"".join(chunks).decode("utf-8", errors="replace")
    if log_path is not None:
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text(output, encoding="utf-8")
    if return_code:
        raise subprocess.CalledProcessError(return_code, args, output=output)
    if reject_runtime_errors and RUNTIME_FAILURE.search(output):
        raise RuntimeError(f"runtime failure found in successful command log: {log_path}")
    return output


def json_result(output: str) -> dict:
    for line in reversed(output.splitlines()):
        line = line.strip()
        if line.startswith("{"):
            return json.loads(line)
    raise RuntimeError("video builder did not emit a JSON result")


def rounded(milliseconds: int) -> int:
    return ((milliseconds + 499) // 500) * 500


def fitted_timings(script_path: Path, prepared: dict, tail_ms: int,
                   minimum_ms: int = 6_000,
                   measurement_script_path: Path | None = None) -> dict[str, int]:
    script = json.loads(script_path.read_text(encoding="utf-8"))
    measured_script = json.loads(
        (measurement_script_path or script_path).read_text(encoding="utf-8")
    )
    scene_starts: dict[str, int] = {}
    cursor = 0
    for scene in measured_script["scenes"]:
        scene_starts[scene["id"]] = cursor
        cursor += int(scene["durationMs"])
    cue_ends: dict[str, int] = {}
    for cue in prepared["cues"]:
        scene_id = cue["id"].split(":", 1)[0]
        relative_end = int(cue["atMs"]) + int(cue["durationMs"]) - scene_starts[scene_id]
        cue_ends[scene_id] = max(cue_ends.get(scene_id, 0), relative_end)
    fitted: dict[str, int] = {}
    for scene in script["scenes"]:
        scene_id = scene["id"]
        if scene_id not in cue_ends:
            raise RuntimeError(f"{script_path.name}: no prepared narration for scene {scene_id}")
        visual_ends = []
        for action in scene.get("actions", []):
            end = int(action.get("atMs", 0)) + int(action.get("durationMs", 0))
            if action.get("type") == "replay":
                replay_ms = int(action["toMs"]) - int(action["fromMs"])
                rate = float(action.get("playbackRate", 1.0))
                end = int(action["atMs"]) + int(action.get("rewindDurationMs", 650)) \
                    + math.ceil(replay_ms / rate)
            visual_ends.append(end)
        visual_end = max(visual_ends, default=0)
        duration = max(minimum_ms, visual_end + 500, cue_ends[scene_id] + tail_ms)
        if scene_id.endswith("outro") or scene_id == "outro":
            duration = max(duration, 15_000)
        fitted[scene_id] = rounded(duration)
    return fitted


def measurement_script(source: Path, target: Path) -> Path:
    """Create a timeline whose budgets cannot reject narration before it is measured."""
    script = json.loads(source.read_text(encoding="utf-8"))
    for scene in script["scenes"]:
        latest_cue = max(
            (int(action.get("atMs", 0)) for action in scene.get("actions", [])
             if action.get("type") == "narration.cue"),
            default=0,
        )
        scene["durationMs"] = max(int(scene["durationMs"]), latest_cue + 90_000)
    write_json(target, script)
    return target


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    temporary.replace(path)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while True:
            block = stream.read(1024 * 1024)
            if not block:
                break
            digest.update(block)
    return digest.hexdigest()


def verify_media(path: Path, width: int, height: int) -> dict:
    output = run([
        "ffprobe", "-v", "error", "-show_entries",
        "format=duration,size:stream=codec_name,codec_type,width,height,r_frame_rate,sample_rate,channels,duration",
        "-of", "json", str(path),
    ], capture=True)
    probe = json.loads(output)
    video = next(stream for stream in probe["streams"] if stream["codec_type"] == "video")
    audio = next(stream for stream in probe["streams"] if stream["codec_type"] == "audio")
    if (video.get("width"), video.get("height"), video.get("r_frame_rate")) != (width, height, "30/1"):
        raise RuntimeError(f"unexpected video stream in {path}: {video}")
    if video.get("codec_name") != "h264":
        raise RuntimeError(f"expected H.264 video in {path}: {video}")
    if (audio.get("sample_rate"), audio.get("channels")) != ("48000", 2):
        raise RuntimeError(f"unexpected audio stream in {path}: {audio}")
    if audio.get("codec_name") != "aac":
        raise RuntimeError(f"expected AAC audio in {path}: {audio}")
    video_duration = float(video["duration"])
    audio_duration = float(audio["duration"])
    if abs(video_duration - audio_duration) > 0.125:
        raise RuntimeError(
            f"audio/video duration drift in {path}: {audio_duration:.3f}s vs {video_duration:.3f}s"
        )
    return {
        "path": str(path),
        "sha256": sha256(path),
        "size": int(probe["format"]["size"]),
        "durationSeconds": float(probe["format"]["duration"]),
        "video": video,
        "audio": audio,
    }


def detect_body_silence(path: Path, duration_seconds: float,
                        outro_seconds: float = 15.0) -> list[dict]:
    process = subprocess.run([
        "ffmpeg", "-hide_banner", "-i", str(path), "-af",
        "silencedetect=noise=-42dB:d=1.5", "-f", "null", "-",
    ], cwd=ROOT, text=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    if process.returncode:
        raise RuntimeError(f"silence analysis failed for {path}: {process.stderr[-2000:]}")
    starts = [float(value) for value in re.findall(r"silence_start:\s*([0-9.]+)", process.stderr)]
    ends = [
        (float(end), float(length))
        for end, length in re.findall(
            r"silence_end:\s*([0-9.]+)\s*\|\s*silence_duration:\s*([0-9.]+)",
            process.stderr,
        )
    ]
    intervals: list[dict] = []
    for index, start in enumerate(starts):
        if index >= len(ends):
            continue
        end, length = ends[index]
        interval = {"start": start, "end": end, "duration": length}
        intervals.append(interval)
        # The final reserved end-screen window intentionally lets narration
        # finish early. Silence inside the story body is always a pacing defect.
        if start < duration_seconds - outro_seconds and end > 1.0:
            raise RuntimeError(
                f"unexpected {length:.2f}s silence in story body at {start:.2f}s: {path}"
            )
    return intervals


def keyframe_times(script_path: Path) -> list[tuple[str, float]]:
    script = json.loads(script_path.read_text(encoding="utf-8"))
    result: list[tuple[str, float]] = []
    cursor = 0
    for scene in script["scenes"]:
        duration = int(scene["durationMs"])
        sample = cursor + min(max(1_200, duration // 3), max(1_200, duration - 750))
        result.append((scene["id"], sample / 1000.0))
        for action in scene.get("actions", []):
            if action.get("type") == "replay":
                replay_sample = (
                    cursor + int(action.get("atMs", 0))
                    + int(action.get("rewindDurationMs", 650)) + 1_000
                )
                result.append((f"{scene['id']}-replay", replay_sample / 1000.0))
        cursor += duration
    return result


def contact_sheet(video: Path, script_path: Path, qa_dir: Path,
                  orientation: str) -> dict:
    frames_dir = qa_dir / f"{orientation}-keyframes"
    frames_dir.mkdir(parents=True, exist_ok=True)
    frames: list[dict] = []
    for index, (label, at_seconds) in enumerate(keyframe_times(script_path)):
        path = frames_dir / f"{index:02d}-{label}.png"
        run([
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-ss",
            f"{at_seconds:.3f}", "-i", str(video), "-frames:v", "1", "-y", str(path),
        ])
        frames.append({"label": label, "atSeconds": at_seconds,
                       "path": str(path), "sha256": sha256(path)})

    columns = 4
    thumb_width, thumb_height = ((440, 248) if orientation == "landscape" else (248, 440))
    caption_height = 42
    rows = math.ceil(len(frames) / columns)
    sheet = Image.new("RGB", (columns * thumb_width, rows * (thumb_height + caption_height)), "#07111d")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for index, frame in enumerate(frames):
        with Image.open(frame["path"]) as image:
            image = image.convert("RGB")
            image.thumbnail((thumb_width, thumb_height), Image.Resampling.LANCZOS)
            left = (index % columns) * thumb_width + (thumb_width - image.width) // 2
            top = (index // columns) * (thumb_height + caption_height)
            sheet.paste(image, (left, top))
        caption = f"{frame['atSeconds']:.1f}s  {frame['label']}"
        draw.text(((index % columns) * thumb_width + 8, top + thumb_height + 10),
                  caption, fill="#f5f8fb", font=font)
    sheet_path = qa_dir / f"{orientation}-contact-sheet.jpg"
    sheet.save(sheet_path, quality=90, optimize=True)
    return {"contactSheet": str(sheet_path), "contactSheetSha256": sha256(sheet_path),
            "frames": frames}


def update_state(slug: str, output: Path, qa_report: dict) -> None:
    state_path = PROJECT_ROOT / "state.json"
    state = json.loads(state_path.read_text(encoding="utf-8"))
    reports = {}
    for name in (slug, f"{slug}-short"):
        report = output / f"{name}-report.json"
        reports[name] = json.loads(report.read_text(encoding="utf-8"))
    for item in state["items"]:
        if item["slug"] != slug:
            continue
        item["status"] = "rendered-qa-pending"
        item["renders"] = {
            "landscape": reports[slug]["outputs"][0],
            "portrait": reports[f"{slug}-short"]["outputs"][0],
        }
        workflow = item.setdefault("workflow", {"schemaVersion": 1})
        workflow["local"] = {
            "status": "qa-passed",
            "completedAt": datetime.now(timezone.utc).isoformat(),
            "output": str(output),
            "qaReport": str(output / "qa-report.json"),
            "scriptHashes": qa_report["scriptHashes"],
            "mediaHashes": {
                key: value["sha256"] for key, value in qa_report["media"].items()
            },
        }
        if item.get("landscapeId") or item.get("shortId"):
            workflow["remote"] = {
                "status": "stale-against-local-render",
                "previousLandscapeId": item.get("landscapeId"),
                "previousShortId": item.get("shortId"),
                "reason": "local media hashes changed after the last remote verification",
            }
        break
    write_json(state_path, state)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("slug")
    parser.add_argument("--java", type=Path, default=Path(os.environ.get("VIDEO_BUILDER_JAVA", DEFAULT_JAVA)))
    parser.add_argument("--jar", type=Path, default=Path(os.environ.get("VIDEO_BUILDER_JAR", DEFAULT_JAR)))
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--tail-ms", type=int, default=0)
    parser.add_argument("--measure-only", action="store_true")
    parser.add_argument("--qa-only", action="store_true")
    args = parser.parse_args()

    project = PROJECT_ROOT / args.slug
    output = args.output.resolve() / args.slug
    output.mkdir(parents=True, exist_ok=True)
    lock_handle = (output / ".render.lock").open("a+", encoding="utf-8")
    try:
        fcntl.flock(lock_handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError as error:
        lock_handle.seek(0)
        owner = lock_handle.read().strip() or "another process"
        raise RuntimeError(f"render already active for {args.slug}: {owner}") from error
    lock_handle.seek(0)
    lock_handle.truncate()
    lock_handle.write(f"pid={os.getpid()} started={datetime.now(timezone.utc).isoformat()}\n")
    lock_handle.flush()
    qa_dir = output / "qa"
    logs_dir = output / "logs"
    if args.qa_only:
        timings = json.loads((project / "timings.json").read_text(encoding="utf-8"))
    else:
        timings = None
        run([sys.executable, str(GENERATOR), "--slug", args.slug, "--output", str(args.output),
             "--ignore-timings", "--force"])
        run([sys.executable, str(QUALITY_GATE), str(project / "video.json"), str(project / "short.json")])

    if not args.qa_only:
        for pass_number in range(1, 5):
            prepared = {}
            measurement_paths = {}
            for key, filename in (("landscape", "video.json"), ("portrait", "short.json")):
                measurement_paths[key] = measurement_script(
                    project / filename, project / f".{key}-measurement.json"
                )
                # `prepare` initializes the JavaSE port too.  On macOS that can
                # abort before Java starts when stdout is a plain pipe, just as
                # the render command can.  Keep the PTY-backed capture for both
                # phases so timing measurement is reliable in local/CI runs.
                stdout = run([str(args.java), "-jar", str(args.jar), "prepare",
                              str(measurement_paths[key])],
                             log_path=logs_dir / f"{key}-prepare.log",
                             reject_runtime_errors=True)
                prepared[key] = json_result(stdout)
            fitted = {
                "schemaVersion": 1,
                "tailMs": args.tail_ms,
                "landscape": fitted_timings(project / "video.json", prepared["landscape"],
                                              args.tail_ms, 6_000, measurement_paths["landscape"]),
                "portrait": fitted_timings(project / "short.json", prepared["portrait"],
                                             args.tail_ms, measurement_script_path=measurement_paths["portrait"]),
            }
            if fitted == timings:
                print(f"timing fit converged after {pass_number} passes", flush=True)
                break
            timings = fitted
            write_json(project / "timings.json", timings)
            run([sys.executable, str(GENERATOR), "--slug", args.slug,
                 "--output", str(args.output), "--force"])
        else:
            raise RuntimeError("narration timing fit did not converge after four passes")

    assert timings is not None
    run([sys.executable, str(QUALITY_GATE), str(project / "video.json"), str(project / "short.json")])

    for filename in ("video.json", "short.json"):
        run([str(args.java), "-jar", str(args.jar), "validate", str(project / filename)])
    if args.measure_only:
        print(json.dumps({"ok": True, "slug": args.slug, "timings": timings}))
        return 0

    if not args.qa_only:
        run([str(args.java), "-jar", str(args.jar), "render", str(project / "video.json"),
             "--orientation", "landscape", "--output", str(output)],
            log_path=logs_dir / "landscape-render.log", reject_runtime_errors=True)
        run([str(args.java), "-jar", str(args.jar), "render", str(project / "short.json"),
             "--orientation", "portrait", "--output", str(output)],
            log_path=logs_dir / "portrait-render.log", reject_runtime_errors=True)
    if PACKAGE_VALIDATOR.is_file():
        run([sys.executable, str(PACKAGE_VALIDATOR), str(output / "youtube.json")])
    landscape_video = output / f"{args.slug}-landscape.mp4"
    portrait_video = output / f"{args.slug}-short-portrait.mp4"
    media = {
        "landscape": verify_media(landscape_video, 1920, 1080),
        "portrait": verify_media(portrait_video, 1080, 1920),
    }
    silence = {
        key: detect_body_silence(Path(value["path"]), value["durationSeconds"])
        for key, value in media.items()
    }
    sheets = {
        "landscape": contact_sheet(landscape_video, project / "video.json", qa_dir, "landscape"),
        "portrait": contact_sheet(portrait_video, project / "short.json", qa_dir, "portrait"),
    }
    qa_report = {
        "schemaVersion": 1,
        "slug": args.slug,
        "completedAt": datetime.now(timezone.utc).isoformat(),
        "status": "passed",
        "scriptHashes": {
            "landscape": sha256(project / "video.json"),
            "portrait": sha256(project / "short.json"),
        },
        "media": media,
        "silenceIntervals": silence,
        "visualReview": sheets,
        "renderLogs": {
            "landscape": str(logs_dir / "landscape-render.log"),
            "portrait": str(logs_dir / "portrait-render.log"),
        },
    }
    write_json(output / "qa-report.json", qa_report)
    update_state(args.slug, output, qa_report)
    print(json.dumps({"ok": True, "slug": args.slug, "output": str(output), "timings": timings}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
