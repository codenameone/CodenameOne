#!/usr/bin/env python3
"""Run one desktop application under a private X server and measure it.

This is the Linux half of the desktop-compat benchmarks. It is handed a command
line and treats it as a black box, so the JVM baseline and the Codename One
native binary are launched, watched and driven by exactly the same code:

* a private ``Xvfb`` (and ``openbox``, so both toolkits get focus and a frame
  the way they would on a desktop) is started for every single launch;
* **startup** is the time from ``fork`` to the first moment the application's
  window holds something other than one flat colour. It is read off the X
  server, never out of the application, so neither variant is instrumented;
* **memory** is summed over the application's whole process tree from
  ``/proc/<pid>/smaps_rollup`` (RSS, PSS, USS) and the peak comes from
  ``/usr/bin/time -v``;
* **idle CPU** is the user + system time the tree accumulates over the idle
  window, as a percentage of one core;
* **scripted input** is injected with XTEST, in window-relative coordinates.

Python 3.9 compatible. Needs python-xlib (``python3-xlib``).
"""

import argparse
import json
import os
import re
import shutil
import signal
import statistics
import struct
import subprocess
import sys
import time
import zlib

try:
    from Xlib import X, XK, display
    from Xlib.ext import xtest
except ImportError:  # pragma: no cover - reported at run time
    display = None

CLK_TCK = os.sysconf("SC_CLK_TCK")
PAGE_POLL_SECONDS = 0.010
# A window counts as painted once this many distinct colours are visible in it.
# One is an unpainted window, two is a flat background and a border.
PAINTED_COLOURS = 3
STRIP_PITCH = 4


def log(message):
    sys.stderr.write("[x11bench] %s\n" % message)
    sys.stderr.flush()


# --------------------------------------------------------------------------
# /proc readers


def process_table():
    """pid -> parent pid for every process visible in /proc."""
    table = {}
    for name in os.listdir("/proc"):
        if not name.isdigit():
            continue
        try:
            with open("/proc/%s/stat" % name) as handle:
                data = handle.read()
        except OSError:
            continue
        # The command is parenthesised and may itself contain spaces or ')'.
        tail = data[data.rfind(")") + 2:].split()
        table[int(name)] = int(tail[1])
    return table


def descendants(root_pid, include_root):
    table = process_table()
    found = []
    frontier = [root_pid]
    while frontier:
        parent = frontier.pop()
        for pid, ppid in table.items():
            if ppid == parent and pid not in found:
                found.append(pid)
                frontier.append(pid)
    if include_root and root_pid in table:
        found.append(root_pid)
    return found


def cpu_ticks(pids):
    total = 0
    for pid in pids:
        try:
            with open("/proc/%d/stat" % pid) as handle:
                data = handle.read()
        except OSError:
            continue
        tail = data[data.rfind(")") + 2:].split()
        total += int(tail[11]) + int(tail[12])
    return total


def memory_kb(pids):
    """Summed RSS / PSS / USS of the given processes, in kB."""
    totals = {"rss_kb": 0, "pss_kb": 0, "uss_kb": 0}
    readable = 0
    for pid in pids:
        try:
            with open("/proc/%d/smaps_rollup" % pid) as handle:
                text = handle.read()
        except OSError:
            continue
        readable += 1
        fields = {}
        for line in text.splitlines():
            match = re.match(r"^(\w+):\s+(\d+) kB", line)
            if match:
                fields[match.group(1)] = int(match.group(2))
        totals["rss_kb"] += fields.get("Rss", 0)
        totals["pss_kb"] += fields.get("Pss", 0)
        totals["uss_kb"] += fields.get("Private_Clean", 0) + fields.get("Private_Dirty", 0)
    totals["processes"] = readable
    return totals


def thread_count(pids):
    total = 0
    for pid in pids:
        try:
            total += len(os.listdir("/proc/%d/task" % pid))
        except OSError:
            continue
    return total


def evict_from_page_cache(paths):
    """Make the next launch read its own files from disk again.

    Returns the method that was used. ``drop_caches`` is the real thing and
    needs a privileged container; ``fadvise`` only evicts the files named, so
    the system libraries both variants share stay warm.
    """
    try:
        os.sync()
        with open("/proc/sys/vm/drop_caches", "w") as handle:
            handle.write("3\n")
        return "drop_caches"
    except OSError:
        pass
    evicted = 0
    for root in paths:
        candidates = []
        if os.path.isdir(root):
            for folder, _dirs, files in os.walk(root):
                candidates.extend(os.path.join(folder, name) for name in files)
        elif os.path.isfile(root):
            candidates.append(root)
        for path in candidates:
            try:
                fd = os.open(path, os.O_RDONLY)
            except OSError:
                continue
            try:
                os.fsync(fd)
            except OSError:
                pass
            try:
                os.posix_fadvise(fd, 0, 0, os.POSIX_FADV_DONTNEED)
                evicted += 1
            except OSError:
                pass
            finally:
                os.close(fd)
    return "fadvise(%d files)" % evicted


# --------------------------------------------------------------------------
# X server


class Screen(object):
    """A private Xvfb + openbox pair, and the queries the benchmark needs."""

    def __init__(self, size, log_dir):
        self.size = size
        self.log_dir = log_dir
        self.xvfb = None
        self.wm = None
        self.name = None
        self.dpy = None
        self.gtk_session = False

    def start(self):
        read_fd, write_fd = os.pipe()
        xvfb_log = open(os.path.join(self.log_dir, "xvfb.log"), "ab")
        self.xvfb = subprocess.Popen(
            ["Xvfb", "-displayfd", str(write_fd), "-screen", "0", self.size + "x24",
             "-nolisten", "tcp", "-noreset"],
            pass_fds=[write_fd], stdout=xvfb_log, stderr=xvfb_log)
        os.close(write_fd)
        number = b""
        deadline = time.monotonic() + 20
        while not number.endswith(b"\n"):
            if time.monotonic() > deadline:
                raise RuntimeError("Xvfb did not report a display")
            chunk = os.read(read_fd, 16)
            if not chunk:
                raise RuntimeError("Xvfb exited before reporting a display")
            number += chunk
        os.close(read_fd)
        self.name = ":" + number.decode().strip()
        self.dpy = display.Display(self.name)
        if shutil.which("openbox"):
            wm_log = open(os.path.join(self.log_dir, "openbox.log"), "ab")
            env = dict(os.environ, DISPLAY=self.name)
            self.wm = subprocess.Popen(["openbox", "--sm-disable"], env=env,
                                       stdout=wm_log, stderr=wm_log)
            # Wait until it owns the root window's substructure redirect.
            atom = self.dpy.intern_atom("_NET_SUPPORTING_WM_CHECK")
            deadline = time.monotonic() + 10
            while time.monotonic() < deadline:
                if self.dpy.screen().root.get_full_property(atom, X.AnyPropertyType):
                    break
                time.sleep(0.05)
        self.gtk_session = self.start_gtk_session()
        return self.name

    def start_gtk_session(self):
        """Makes the new X server one a GTK program has already run on.

        The first GTK 3 program on an X server initialises GL to choose its
        visuals and then records the choice on the root window (GDK_VISUALS),
        "to avoid having to initialize GL each time, as it may not be used
        later" in GDK's words. Every later program reads the property and never
        loads a GL driver. A desktop session has had its first GTK program long
        before an application starts; a private Xvfb has not, so each launch
        measured here WAS the first one, and carried Mesa's llvmpipe with it:
        53 MB of the resident set of a native Notepad (108 MB, against 56 MB
        launched second on the same server), none of it the application's.
        One bare gtk_init, as a process of its own that exits, puts the server
        in the state a session leaves it in. Returns whether it did.
        """
        code = ("import ctypes, sys; "
                "sys.exit(0 if ctypes.CDLL('libgtk-3.so.0').gtk_init_check(None, None) else 1)")
        env = dict(os.environ, DISPLAY=self.name, NO_AT_BRIDGE="1")
        env.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")
        try:
            with open(os.path.join(self.log_dir, "gtk-session.log"), "ab") as out:
                return subprocess.call([sys.executable, "-c", code], env=env,
                                       stdout=out, stderr=out, timeout=30) == 0
        except (OSError, subprocess.SubprocessError):
            return False

    def stop(self):
        for process in (self.wm, self.xvfb):
            if process is None:
                continue
            try:
                process.terminate()
                process.wait(timeout=5)
            except Exception:  # noqa: BLE001 - best effort teardown
                try:
                    process.kill()
                except Exception:  # noqa: BLE001
                    pass
        try:
            if self.dpy is not None:
                self.dpy.close()
        except Exception:  # noqa: BLE001
            pass

    def application_window(self):
        """Geometry (x, y, w, h) of the largest viewable top-level window."""
        root = self.dpy.screen().root
        best = None
        try:
            children = root.query_tree().children
        except Exception:  # noqa: BLE001 - a window vanished mid-query
            return None
        for child in children:
            try:
                attributes = child.get_attributes()
                if attributes.map_state != X.IsViewable or attributes.win_class != X.InputOutput:
                    continue
                geometry = child.get_geometry()
            except Exception:  # noqa: BLE001
                continue
            if geometry.width < 100 or geometry.height < 100:
                continue
            area = geometry.width * geometry.height
            if best is None or area > best[0]:
                best = (area, (geometry.x, geometry.y, geometry.width, geometry.height))
        return best[1] if best else None

    def colours(self, geometry):
        """(distinct colours, fingerprint) of horizontal strips through a window.

        Strips, not the whole window: a full-screen GetImage every poll would
        make the measuring process the busiest thing on the machine. The top is
        skipped so a window manager's title bar cannot count as paint.
        """
        root = self.dpy.screen().root
        screen = self.dpy.screen()
        x, y, width, height = geometry
        left = max(0, x + width // 20)
        right = min(screen.width_in_pixels, x + width - width // 20)
        top = max(0, y + max(40, height // 10))
        bottom = min(screen.height_in_pixels, y + height - height // 20)
        if right - left < 20 or bottom - top < 8:
            return 0, 0
        seen = set()
        fingerprint = 0
        row = top
        while row < bottom:
            try:
                image = root.get_image(left, row, right - left, 1, X.ZPixmap, 0xFFFFFFFF)
            except Exception:  # noqa: BLE001
                return 0, 0
            data = image.data
            if isinstance(data, str):
                data = data.encode("latin-1")
            usable = len(data) - (len(data) % 4)
            seen.update(memoryview(data[:usable]).cast("I"))
            fingerprint = hash((fingerprint, data))
            row += STRIP_PITCH
        return len(seen), fingerprint

    def screenshot(self, geometry, path):
        """Write the window as a PNG: the evidence that what was timed is the
        application's own first screen and not an error dialog."""
        root = self.dpy.screen().root
        screen = self.dpy.screen()
        x, y, width, height = geometry
        x = max(0, x)
        y = max(0, y)
        width = min(width, screen.width_in_pixels - x)
        height = min(height, screen.height_in_pixels - y)
        rows = []
        try:
            for row in range(height):
                data = root.get_image(x, y + row, width, 1, X.ZPixmap, 0xFFFFFFFF).data
                if isinstance(data, str):
                    data = data.encode("latin-1")
                pixels = bytearray(width * 3)
                pixels[0::3] = data[2:width * 4:4]
                pixels[1::3] = data[1:width * 4:4]
                pixels[2::3] = data[0:width * 4:4]
                rows.append(b"\x00" + bytes(pixels))
        except Exception:  # noqa: BLE001
            return False

        def chunk(kind, payload):
            body = kind + payload
            return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body))

        with open(path, "wb") as handle:
            handle.write(b"\x89PNG\r\n\x1a\n")
            handle.write(chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)))
            handle.write(chunk(b"IDAT", zlib.compress(b"".join(rows), 6)))
            handle.write(chunk(b"IEND", b""))
        return True

    # -- input ---------------------------------------------------------

    def pointer(self, px, py):
        xtest.fake_input(self.dpy, X.MotionNotify, x=int(px), y=int(py))
        self.dpy.sync()

    def click(self, px, py, button=1):
        self.pointer(px, py)
        time.sleep(0.03)
        xtest.fake_input(self.dpy, X.ButtonPress, button)
        self.dpy.sync()
        time.sleep(0.03)
        xtest.fake_input(self.dpy, X.ButtonRelease, button)
        self.dpy.sync()

    def key(self, name):
        keysym = XK.string_to_keysym(name)
        if keysym == 0:
            raise ValueError("unknown key name: %s" % name)
        keycode = self.dpy.keysym_to_keycode(keysym)
        if keycode == 0:
            raise ValueError("no keycode for key: %s" % name)
        xtest.fake_input(self.dpy, X.KeyPress, keycode)
        self.dpy.sync()
        time.sleep(0.02)
        xtest.fake_input(self.dpy, X.KeyRelease, keycode)
        self.dpy.sync()


def parse_script(path):
    """Read an input script: one action per line, '#' starts a comment.

    sleep SECONDS | move FX FY | click FX FY | key NAME | type TEXT
    FX and FY are fractions of the application window, so one script drives
    both variants even though their windows are not the same size.
    """
    actions = []
    with open(path) as handle:
        for number, raw in enumerate(handle, 1):
            line = raw.split("#", 1)[0].strip()
            if not line:
                continue
            parts = line.split(None, 1)
            verb = parts[0]
            argument = parts[1] if len(parts) > 1 else ""
            if verb == "sleep":
                actions.append(("sleep", float(argument)))
            elif verb in ("move", "click"):
                fx, fy = argument.split()
                actions.append((verb, float(fx), float(fy)))
            elif verb == "key":
                actions.append(("key", argument.strip()))
            elif verb == "type":
                actions.append(("type", argument))
            else:
                raise ValueError("%s:%d: unknown action %r" % (path, number, verb))
    return actions


KEY_NAMES = {" ": "space", ".": "period", ",": "comma", "-": "minus"}


def play_script(screen, actions, alive):
    for action in actions:
        if not alive():
            return False
        verb = action[0]
        if verb == "sleep":
            time.sleep(action[1])
            continue
        if verb in ("move", "click"):
            geometry = screen.application_window()
            if geometry is None:
                continue
            x, y, width, height = geometry
            px = x + action[1] * width
            py = y + action[2] * height
            if verb == "move":
                screen.pointer(px, py)
            else:
                screen.click(px, py)
        elif verb == "key":
            screen.key(action[1])
        elif verb == "type":
            for character in action[1]:
                screen.key(KEY_NAMES.get(character, character))
                time.sleep(0.03)
        time.sleep(0.12)
    return alive()


# --------------------------------------------------------------------------
# one launch


def parse_time_report(path):
    try:
        with open(path) as handle:
            text = handle.read()
    except OSError:
        return None
    match = re.search(r"Maximum resident set size \(kbytes\): (\d+)", text)
    return int(match.group(1)) if match else None


def launch(command, cwd, env, size, log_dir, tag, idle_seconds, script, settle_seconds,
           startup_timeout, screenshots=False):
    """Start the application once and measure it. Returns a dict."""
    os.makedirs(log_dir, exist_ok=True)
    screen = Screen(size, log_dir)
    result = {"tag": tag, "ok": False}
    time_report = os.path.join(log_dir, tag + ".time")
    if os.path.exists(time_report):
        os.remove(time_report)
    app_log = open(os.path.join(log_dir, tag + ".log"), "wb")
    process = None
    try:
        name = screen.start()
        result["gtk_session"] = screen.gtk_session
        child_env = dict(os.environ)
        child_env.update(env)
        child_env["DISPLAY"] = name
        # No accessibility bus: it is a second process the JVM and GTK would
        # each start, and it is not part of either application.
        child_env["NO_AT_BRIDGE"] = "1"
        child_env.setdefault("LIBGL_ALWAYS_SOFTWARE", "1")
        started = time.monotonic()
        process = subprocess.Popen(
            ["/usr/bin/time", "-v", "-o", time_report] + list(command),
            cwd=cwd, env=child_env, stdout=app_log, stderr=subprocess.STDOUT,
            start_new_session=True)

        def alive():
            return process.poll() is None

        def tree():
            return descendants(process.pid, False)

        window_at = None
        painted_at = None
        geometry = None
        fingerprint = None
        changed_at = None
        deadline = started + startup_timeout
        while time.monotonic() < deadline and alive():
            now = time.monotonic()
            geometry = screen.application_window()
            if geometry is not None:
                if window_at is None:
                    window_at = now
                colours, current = screen.colours(geometry)
                if colours >= PAINTED_COLOURS:
                    painted_at = time.monotonic()
                    fingerprint = current
                    changed_at = painted_at
                    break
            time.sleep(PAGE_POLL_SECONDS)
        if painted_at is None:
            result["error"] = ("exited before painting (status %s)" % process.returncode
                               if not alive() else "no painted window within %ds" % startup_timeout)
            return result
        result["window_ms"] = round((window_at - started) * 1000.0, 1)
        result["first_paint_ms"] = round((painted_at - started) * 1000.0, 1)

        # When did the window stop changing? One quiet second ends the search;
        # an application that animates forever never settles and reports null.
        settled_at = None
        search_until = painted_at + 8.0
        while time.monotonic() < search_until and alive():
            geometry = screen.application_window() or geometry
            _colours, current = screen.colours(geometry)
            now = time.monotonic()
            if current != fingerprint:
                fingerprint = current
                changed_at = now
            elif now - changed_at >= 1.0:
                settled_at = changed_at
                break
            time.sleep(0.02)
        result["settled_ms"] = (round((settled_at - started) * 1000.0, 1)
                                if settled_at is not None else None)
        result["window_size"] = [geometry[2], geometry[3]]
        result["ok"] = True

        if idle_seconds > 0:
            # Idle: RAM at the end of the window, CPU across the second half of
            # it so the tail of startup work is not billed as idling.
            time.sleep(idle_seconds / 2.0)
            ticks_before = cpu_ticks(tree())
            wall_before = time.monotonic()
            fingerprint_before = screen.colours(screen.application_window() or geometry)[1]
            repaints = 0
            while time.monotonic() - wall_before < idle_seconds / 2.0:
                time.sleep(0.25)
                current = screen.colours(screen.application_window() or geometry)[1]
                if current != fingerprint_before:
                    repaints += 1
                    fingerprint_before = current
            wall = time.monotonic() - wall_before
            pids = tree()
            ticks = cpu_ticks(pids) - ticks_before
            idle = memory_kb(pids)
            idle["threads"] = thread_count(pids)
            idle["cpu_percent"] = round(100.0 * ticks / CLK_TCK / wall, 2)
            idle["cpu_window_seconds"] = round(wall, 2)
            idle["visible_changes"] = repaints
            result["idle"] = idle
            if screenshots:
                screen.screenshot(screen.application_window() or geometry,
                                  os.path.join(log_dir, tag + "-idle.png"))
            if not alive():
                result["ok"] = False
                result["error"] = "exited while idle (status %s)" % process.returncode
                return result

        if script:
            survived = play_script(screen, script, alive)
            time.sleep(settle_seconds)
            if not survived or not alive():
                result["ok"] = False
                result["error"] = "exited during the input script (status %s)" % process.returncode
                return result
            pids = tree()
            after = memory_kb(pids)
            after["threads"] = thread_count(pids)
            result["after_script"] = after
            if screenshots:
                screen.screenshot(screen.application_window() or geometry,
                                  os.path.join(log_dir, tag + "-after.png"))
        return result
    finally:
        if process is not None:
            if process.poll() is None:
                # The application, not /usr/bin/time: time then reports and exits.
                for pid in descendants(process.pid, False):
                    try:
                        os.kill(pid, signal.SIGTERM)
                    except OSError:
                        pass
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    try:
                        os.killpg(process.pid, signal.SIGKILL)
                    except OSError:
                        pass
                    process.wait()
            peak = parse_time_report(time_report)
            if peak is not None:
                result["peak_rss_kb"] = peak
        screen.stop()
        app_log.close()


def summary(values):
    if not values:
        return None
    return {"median": round(statistics.median(values), 1), "min": round(min(values), 1),
            "max": round(max(values), 1), "runs": len(values)}


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--label", required=True, help="name of this variant in the output")
    parser.add_argument("--out", required=True, help="JSON file to write")
    parser.add_argument("--log-dir", required=True, help="directory for logs and scratch files")
    parser.add_argument("--cwd", default=None, help="working directory of the application")
    parser.add_argument("--env", action="append", default=[], metavar="NAME=VALUE")
    parser.add_argument("--screen", default="1280x800", help="Xvfb screen size")
    parser.add_argument("--startup-runs", type=int, default=7)
    parser.add_argument("--session-runs", type=int, default=3)
    parser.add_argument("--idle-seconds", type=float, default=10.0)
    parser.add_argument("--settle-seconds", type=float, default=3.0)
    parser.add_argument("--startup-timeout", type=float, default=90.0)
    parser.add_argument("--script", default=None, help="input script (see parse_script)")
    parser.add_argument("--evict", action="append", default=[], metavar="PATH",
                        help="file or directory to evict before each cold start")
    parser.add_argument("--check-only", action="store_true",
                        help="one session, exit 0 only if it survives the script")
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()

    if display is None:
        sys.exit("python-xlib is required (apt install python3-xlib)")
    command = args.command
    if command and command[0] == "--":
        command = command[1:]
    if not command:
        parser.error("no command given")
    env = {}
    for item in args.env:
        name, _sep, value = item.partition("=")
        env[name] = value
    script = parse_script(args.script) if args.script else None
    os.makedirs(args.log_dir, exist_ok=True)

    def one(tag, idle, with_script):
        return launch(command, args.cwd, env, args.screen, args.log_dir, tag, idle,
                      script if with_script else None, args.settle_seconds, args.startup_timeout,
                      screenshots=tag in ("check", "session-0"))

    report = {"label": args.label, "command": command, "screen": args.screen,
              "idle_seconds": args.idle_seconds, "errors": []}

    if args.check_only:
        outcome = one("check", 2.0, True)
        report["check"] = outcome
        with open(args.out, "w") as handle:
            json.dump(report, handle, indent=2, sort_keys=True)
        sys.exit(0 if outcome.get("ok") else 1)

    # One throwaway launch so first-run side effects (font caches, JavaFX
    # extracting natives, a settings directory being created) are in no sample.
    primer = one("primer", 0, False)
    if not primer.get("ok"):
        report["errors"].append("primer: " + primer.get("error", "failed"))
        report["ok"] = False
        with open(args.out, "w") as handle:
            json.dump(report, handle, indent=2, sort_keys=True)
        log("%s: %s" % (args.label, report["errors"][0]))
        sys.exit(1)

    for mode in ("cold", "warm"):
        samples = []
        for index in range(args.startup_runs):
            method = None
            if mode == "cold":
                method = evict_from_page_cache(args.evict)
                report["cold_method"] = method
            outcome = one("%s-%d" % (mode, index), 0, False)
            if outcome.get("ok"):
                samples.append(outcome)
            else:
                report["errors"].append("%s-%d: %s" % (mode, index, outcome.get("error")))
        report["startup_" + mode] = {
            "first_paint_ms": summary([s["first_paint_ms"] for s in samples]),
            "window_ms": summary([s["window_ms"] for s in samples]),
            "settled_ms": summary([s["settled_ms"] for s in samples
                                   if s.get("settled_ms") is not None]),
            "samples": samples,
        }
        log("%s %s first paint: %s" % (args.label, mode, report["startup_" + mode]["first_paint_ms"]))

    sessions = []
    for index in range(args.session_runs):
        outcome = one("session-%d" % index, args.idle_seconds, True)
        sessions.append(outcome)
        if not outcome.get("ok"):
            report["errors"].append("session-%d: %s" % (index, outcome.get("error")))
    good = [s for s in sessions if s.get("ok")]

    def middle(section, field):
        values = [s[section][field] for s in good if section in s and field in s[section]]
        return round(statistics.median(values), 2) if values else None

    report["sessions"] = sessions
    report["idle"] = {field: middle("idle", field) for field in
                      ("rss_kb", "pss_kb", "uss_kb", "threads", "cpu_percent", "visible_changes")}
    report["after_script"] = {field: middle("after_script", field) for field in
                              ("rss_kb", "pss_kb", "uss_kb", "threads")}
    peaks = [s["peak_rss_kb"] for s in good if "peak_rss_kb" in s]
    report["peak_rss_kb"] = round(statistics.median(peaks)) if peaks else None
    report["ok"] = bool(good)
    with open(args.out, "w") as handle:
        json.dump(report, handle, indent=2, sort_keys=True)
    log("%s idle: %s" % (args.label, report["idle"]))
    sys.exit(0 if report["ok"] else 1)


if __name__ == "__main__":
    main()
