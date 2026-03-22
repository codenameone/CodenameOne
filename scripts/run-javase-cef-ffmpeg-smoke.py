#!/usr/bin/env python3
import os
import platform
import shutil
import subprocess
import sys
import time
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
ARTIFACTS_DIR = Path(os.environ.get("ARTIFACTS_DIR", REPO_ROOT / "artifacts" / "javase-cef-ffmpeg-smoke"))
ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
M2_REPO = Path(os.environ.get("MAVEN_REPO_LOCAL", REPO_ROOT / ".m2" / "repository"))
M2_REPO.mkdir(parents=True, exist_ok=True)


def log(message: str) -> None:
    print(f"[javase-cef-ffmpeg-smoke] {message}", flush=True)


def run(cmd, cwd=None, env=None, log_name=None, timeout=None):
    cwd = cwd or REPO_ROOT
    env = env or os.environ.copy()
    if log_name:
        log_path = ARTIFACTS_DIR / log_name
        with log_path.open("wb") as out:
            out.write(("CMD: " + " ".join(str(c) for c in cmd) + "\n").encode("utf-8"))
            out.flush()
            proc = subprocess.Popen(cmd, cwd=str(cwd), env=env, stdout=out, stderr=subprocess.STDOUT)
            try:
                rc = proc.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait()
                raise RuntimeError(f"Timed out running {' '.join(str(c) for c in cmd)}")
        if rc != 0:
            raise RuntimeError(f"Command failed with exit code {rc}: {' '.join(str(c) for c in cmd)}")
        return log_path
    rc = subprocess.call(cmd, cwd=str(cwd), env=env)
    if rc != 0:
        raise RuntimeError(f"Command failed with exit code {rc}: {' '.join(str(c) for c in cmd)}")


def tail_text(path: Path, max_lines: int = 60) -> str:
    if not path.exists():
        return ""
    text = path.read_text("utf-8", errors="replace").splitlines()
    return "\n".join(text[-max_lines:])


def find_recent_file(name: str, started_at: float):
    newest = None
    newest_mtime = started_at - 5
    try:
        for candidate in Path.home().rglob(name):
            try:
                mtime = candidate.stat().st_mtime
            except OSError:
                continue
            if mtime >= newest_mtime:
                newest = candidate
                newest_mtime = mtime
    except OSError:
        return None
    return newest


def mvn_cmd():
    return "mvn.cmd" if platform.system() == "Windows" else "mvn"


def javac_cmd():
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / ("javac.exe" if platform.system() == "Windows" else "javac")
        if candidate.exists():
            return str(candidate)
    return "javac"


def java_cmd():
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / ("java.exe" if platform.system() == "Windows" else "java")
        if candidate.exists():
            return str(candidate)
    return "java"


def ffmpeg_cmd():
    if platform.system() == "Windows":
        return shutil.which("ffmpeg.exe") or "ffmpeg.exe"
    return shutil.which("ffmpeg") or "ffmpeg"


def build_video(video_file: Path):
    cmd = [
        ffmpeg_cmd(),
        "-y",
        "-f",
        "lavfi",
        "-i",
        "color=c=red:s=320x240:d=3",
        "-f",
        "lavfi",
        "-i",
        "sine=frequency=440:duration=3",
        "-c:v",
        "libx264",
        "-pix_fmt",
        "yuv420p",
        "-c:a",
        "aac",
        "-shortest",
        str(video_file),
    ]
    run(cmd, log_name="ffmpeg-generate.log")


def install_runtime():
    cmd = [
        mvn_cmd(),
        "-B",
        f"-Dmaven.repo.local={M2_REPO}",
        "-f",
        str(REPO_ROOT / "maven" / "pom.xml"),
        "-pl",
        "javase,cn1-binaries,codenameone-maven-plugin",
        "-am",
        "-DskipTests",
        "-Dmaven.javadoc.skip=true",
        "-Plocal-dev-javase",
        "install",
    ]
    run(cmd, log_name="build-runtime.log", timeout=1800)


def run_smoke_app(video_file: Path, screenshot_path: Path, status_path: Path):
    screenshot_path.unlink(missing_ok=True)
    status_path.unlink(missing_ok=True)
    started_at = time.time()

    base_cmd = [
        mvn_cmd(),
        "-B",
        f"-Dmaven.repo.local={M2_REPO}",
        "-f",
        str(REPO_ROOT / "maven" / "tests" / "javase-cef-ffmpeg-smoke" / "pom.xml"),
        "-pl",
        "javase",
        "-am",
        "-DskipTests",
        "-Dmaven.javadoc.skip=true",
        f"-Dcn1.test.video={video_file}",
        "-Dcn1.javase.implementation=cef",
        "-Dcn1.javase.mediaImplementation=ffmpeg",
        "-Psimulator",
        "verify",
    ]
    cmd = base_cmd
    if platform.system() == "Linux" and shutil.which("xvfb-run"):
        cmd = ["xvfb-run", "-a"] + base_cmd
    timeout = 900 if platform.system() == "Windows" else 600
    timed_out = False
    try:
        run(cmd, log_name="smoke-app.log", timeout=timeout)
    except RuntimeError as exc:
        if "Timed out running" not in str(exc):
            raise
        timed_out = True

    if not screenshot_path.exists():
        discovered = find_recent_file(screenshot_path.name, started_at)
        if discovered is not None:
            screenshot_path = discovered
    if not status_path.exists():
        discovered_status = find_recent_file(status_path.name, started_at)
        if discovered_status is not None:
            status_path = discovered_status

    if timed_out and screenshot_path.exists() and status_path.exists():
        log("Simulator command timed out after producing artifacts; continuing with verification")
        return screenshot_path, status_path

    if not screenshot_path.exists():
        status_text = status_path.read_text("utf-8", errors="replace") if status_path.exists() else ""
        log_tail = tail_text(ARTIFACTS_DIR / "smoke-app.log")
        raise RuntimeError(
            f"Screenshot not found at {screenshot_path}. Status: {status_text}\n"
            f"Smoke log tail:\n{log_tail}"
        )

    return screenshot_path, status_path


def verify_screenshot(screenshot_path: Path):
    classes_dir = ARTIFACTS_DIR / "verifier-classes"
    classes_dir.mkdir(parents=True, exist_ok=True)
    source = REPO_ROOT / "scripts" / "javase-cef-ffmpeg-smoke" / "ScreenshotVerifier.java"
    run([javac_cmd(), "-d", str(classes_dir), str(source)], log_name="verifier-javac.log")
    try:
        run([java_cmd(), "-cp", str(classes_dir), "ScreenshotVerifier", str(screenshot_path)], log_name="verify-screenshot.log")
    except RuntimeError as exc:
        log_tail = tail_text(ARTIFACTS_DIR / "verify-screenshot.log")
        raise RuntimeError(f"{exc}\nVerifier log tail:\n{log_tail}")


def verify_status(status_path: Path):
    if not status_path.exists():
        raise RuntimeError(f"Status file not found at {status_path}")
    data = {}
    for line in status_path.read_text("utf-8", errors="replace").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            data[key.strip()] = value.strip()
    if data.get("cefLoaded") != "true":
        raise RuntimeError(f"CEF classes were not loaded: {data}")
    impl_class = data.get("displayImplementationClass", "")
    if "JavaCEFSEPort" not in impl_class:
        raise RuntimeError(f"JavaSE runtime did not resolve to the CEF implementation: {data}")
    peer_class = data.get("browserPeerClass", "")
    if "CEFBrowserComponent" not in peer_class:
        raise RuntimeError(f"Browser peer was not created using the CEF backend: {data}")
    if data.get("videoFrameRendered") != "true":
        raise RuntimeError(f"FFmpeg video frame was not rendered before capture: {data}")
    if platform.system() == "Windows":
        avg = data.get("videoAverageColor", "")
        try:
            r, g, b = [int(part.strip()) for part in avg.split(",", 2)]
        except Exception:
            raise RuntimeError(f"Unable to parse Windows FFmpeg videoAverageColor from status: {data}")
        if r < 120 or r <= g + 25 or r <= b + 25:
            raise RuntimeError(f"Windows FFmpeg frame is not red enough according to backend status: {data}")


def main():
    log(f"Artifacts: {ARTIFACTS_DIR}")
    video_file = ARTIFACTS_DIR / "red-video.mp4"
    build_video(video_file)
    install_runtime()

    app_home = Path.home() / "com.codenameone.tests.javase.CefFfmpegSmoke"
    app_home.mkdir(parents=True, exist_ok=True)
    screenshot_path = app_home / "cef-ffmpeg-smoke.png"
    status_path = app_home / "cef-ffmpeg-smoke-status.txt"

    screenshot_path, status_path = run_smoke_app(video_file, screenshot_path, status_path)
    shutil.copy2(screenshot_path, ARTIFACTS_DIR / screenshot_path.name)
    if status_path.exists():
        shutil.copy2(status_path, ARTIFACTS_DIR / status_path.name)
    verify_status(status_path)
    if platform.system() != "Windows":
        verify_screenshot(screenshot_path)
    log("Smoke test completed successfully")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        log(f"FAILED: {exc}")
        raise
