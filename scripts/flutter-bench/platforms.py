#!/usr/bin/env python3
"""Per-platform halves of the Flutter-vs-Codename One benchmark.

Everything that differs between platforms is here, behind one small interface:
how an artifact is located and sized, how the application is launched, and how
its memory is read while it sits idle. The orchestration, statistics and
reporting are platform independent and live in `benchlib.py`.

An adapter that cannot run reports WHY through `available()`, and the harness
records that reason instead of silently producing a partial comparison. A
benchmark that quietly drops a platform reads exactly like one that measured it
and found no difference.

Which adapters have actually been exercised is recorded per class in
`EXERCISED`, and printed by `run_bench.py --list`. That distinction is worth
keeping honest: an unexercised adapter is a proposal, not a measurement.
"""

import glob
import os
import re
import shutil
import subprocess
import time

import benchlib

# Which line ends the start-up clock on each side.
#
# Getting this pairing right is the whole difficulty of the start-up metric,
# and it is easy to get wrong in our own favour.
#
# Flutter's FIRST frame is a warm-up frame: runApp schedules it before the root
# widget is attached, so it paints a couple of dozen elements in ~10ms. Timing
# to that measures nothing. The gallery therefore also prints FIRSTCONTENT, the
# first frame after its element tree stops growing.
#
# But FIRSTCONTENT is still not the event to compare against. It is a UI-thread
# post-frame callback that runs BEFORE that frame is rasterised, whereas
# Codename One's FIRSTFRAME fires once the form is on screen. Comparing the two
# charges one runtime for rasterising its first screen and not the other --
# main_bench.dart says so in as many words, and the harness this replaces
# compared exactly that pair, which flattered Codename One.
#
# So the comparison is BRACKETED. RASTERDONE is printed when the content
# frame's timings arrive, which is after its raster completed and is therefore
# an upper bound; FIRSTCONTENT is the lower bound. Flutter's true
# present-complete lies between them, and both ends are reported rather than
# one being picked.
#
# BENCH:FIRSTFRAME-INVALID is deliberately NOT matched: the application prints
# it instead of the marker when its first frame reported errors, so a run whose
# widget tree failed half way through layout times out here rather than
# recording the much faster time it took to paint a broken screen.
MARKERS = {
    "codenameone": re.compile(r"BENCH:FIRSTFRAME(?!-INVALID)"),
    "flutter": re.compile(r"BENCH:RASTERDONE"),
}

# The optimistic end of the bracket, for the side that has two.
LOWER_BOUND_MARKERS = {
    "flutter": re.compile(r"BENCH:FIRSTCONTENT"),
}

LAUNCH_TIMEOUT_S = float(os.environ.get("BENCH_LAUNCH_TIMEOUT", "90"))
SETTLE_S = float(os.environ.get("BENCH_SETTLE", "12"))


class Unavailable(Exception):
    """Raised by an adapter that cannot measure on this host."""


class Adapter(object):
    """One platform's half of the benchmark."""

    id = None
    label = None
    # Whether this adapter has been run end to end against real builds.
    exercised = False

    def available(self):
        """(ok, reason). `reason` is shown in the report when ok is False."""
        raise NotImplementedError

    def artifact(self, side):
        """The built application for `side`, as a path."""
        raise NotImplementedError

    def sizes(self, side, workdir):
        """install/code/wire bytes for `side`."""
        path = self.artifact(side)
        return {
            "install_bytes": benchlib.tree_size(path),
            "code_bytes": self.code_size(side),
            "wire_bytes": benchlib.wire_size(path, workdir),
        }

    def code_size(self, side):
        """Executable bytes only, or None where the platform cannot say.

        Reported separately from installed size because the two answer
        different questions: installed size includes assets, which are the
        same file on both sides and therefore dilute the comparison, while
        executable size is the runtime and the compiled application.
        """
        return None

    def launch_and_time(self, side):
        """(upper_ms, lower_ms, idle_memory_bytes) for one run."""
        raise NotImplementedError

    def notes(self):
        """Caveats that belong beside this platform's numbers.

        Carried into the report rather than left in this source file: a reader
        looking at a table has no way to know that one of its rows means
        something different from the others unless the table says so.
        """
        return []

    # -- shared helpers -------------------------------------------------

    def _time_stream(self, side, read_line, started):
        """Times from `started` to the side's markers, reading `read_line`.

        The clock is started by the caller immediately before the process is
        spawned, so it spans process creation too -- which is part of what a
        user waits for and is not something either runtime can measure about
        itself.

        Returns (upper_ms, lower_ms). `lower_ms` is None for a side that emits
        only one marker, and the report then treats the bracket as a point.
        """
        marker = MARKERS[side]
        lower_marker = LOWER_BOUND_MARKERS.get(side)
        upper = None
        lower = None
        deadline = started + LAUNCH_TIMEOUT_S
        while time.time() < deadline:
            line = read_line()
            if line is None:
                break
            if lower is None and lower_marker and lower_marker.search(line):
                lower = (time.time() - started) * 1000.0
            if upper is None and marker.search(line):
                upper = (time.time() - started) * 1000.0
            if upper is not None and time.time() - started > SETTLE_S:
                break
        return upper, lower


# ----------------------------------------------------------------------
# Desktop: a plain child process whose stdout the harness can read.
# ----------------------------------------------------------------------

class _ProcessAdapter(Adapter):
    """Common shape for platforms where the app is a child process."""

    def _executable(self, side):
        raise NotImplementedError

    def _memory(self, pid):
        raise NotImplementedError

    def launch_and_time(self, side):
        exe = self._executable(side)
        started = time.time()
        proc = subprocess.Popen(
            [exe], stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, bufsize=1, env=dict(os.environ, BENCH_MARKERS="1"))
        try:
            upper, lower = self._time_stream(
                side, lambda: proc.stdout.readline() or None, started)
            memory = self._memory(proc.pid) if upper is not None else None
            return upper, lower, memory
        finally:
            proc.terminate()
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()


class MacOSAdapter(_ProcessAdapter):
    """macOS / Mac Catalyst, both sides as .app bundles.

    This is the shape `benchcn1/tools/bench_compare.py` measured by hand, and
    the only pairing whose numbers have been quoted so far.
    """

    id = "macos"
    label = "macOS (Mac Catalyst)"
    exercised = False

    def __init__(self, cn1_app, flutter_app):
        self.apps = {"codenameone": cn1_app, "flutter": flutter_app}

    def available(self):
        if os.uname().sysname != "Darwin":
            return False, "not a macOS host"
        for side, path in self.apps.items():
            if not os.path.isdir(path):
                return False, "missing %s build at %s" % (side, path)
        return True, None

    def artifact(self, side):
        return self.apps[side]

    def _executable(self, side):
        app = self.apps[side]
        macos = os.path.join(app, "Contents", "MacOS")
        entries = [e for e in glob.glob(os.path.join(macos, "*"))
                   if os.access(e, os.X_OK)]
        if not entries:
            raise Unavailable("no executable in %s" % macos)
        return entries[0]

    def code_size(self, side):
        return benchlib.macho_code_size(self.apps[side])

    def _memory(self, pid):
        """Physical footprint, not RSS.

        `ps -o rss` counts every resident page including clean, file-backed
        ones the kernel is free to evict, so it moves with whatever else the
        machine is doing. Measured on one unchanged binary in one state it
        reported 151MB, 207MB and 219MB within a single afternoon, while
        phys_footprint for that state was stable -- and phys_footprint is the
        number Apple's own memory limits are enforced against.
        """
        out = benchlib.run(["vmmap", "--summary", str(pid)])
        for line in out.stdout.splitlines():
            if line.startswith("Physical footprint:"):
                return _parse_bytes(line.split(":", 1)[1].strip())
        return None


class LinuxAdapter(_ProcessAdapter):
    """Linux desktop: Flutter's `linux` target against Codename One's JavaSE.

    NOT YET EXERCISED. The mechanics are straightforward -- both sides are
    ELF executables that print to stdout, and /proc reports memory exactly --
    but no run has produced numbers, so treat this adapter as a proposal
    until CI has published one.
    """

    id = "linux"
    label = "Linux"
    exercised = False

    def __init__(self, cn1_app, flutter_app):
        self.apps = {"codenameone": cn1_app, "flutter": flutter_app}

    def available(self):
        if os.uname().sysname != "Linux":
            return False, "not a Linux host"
        if not os.environ.get("DISPLAY") and not os.environ.get("WAYLAND_DISPLAY"):
            # Measured on this project: the JavaSE simulator needs a real
            # display and hangs in Display.init(null) without one, so a
            # headless runner has to bring up Xvfb rather than pass a
            # headless flag.
            return False, "no display; start Xvfb before measuring"
        for side, path in self.apps.items():
            if not os.path.exists(path):
                return False, "missing %s build at %s" % (side, path)
        return True, None

    def artifact(self, side):
        return self.apps[side]

    def _executable(self, side):
        path = self.apps[side]
        if os.path.isfile(path) and os.access(path, os.X_OK):
            return path
        raise Unavailable("no executable at %s" % path)

    def code_size(self, side):
        try:
            return os.path.getsize(self._executable(side))
        except (OSError, Unavailable):
            return None

    def _memory(self, pid):
        """VmRSS from /proc, which is what Linux memory limits act on."""
        try:
            with open("/proc/%d/status" % pid) as handle:
                for line in handle:
                    if line.startswith("VmRSS:"):
                        return _parse_bytes(line.split(":", 1)[1].strip())
        except OSError:
            return None
        return None


class WindowsAdapter(_ProcessAdapter):
    """Windows desktop.

    NOT YET EXERCISED.
    """

    id = "windows"
    label = "Windows"
    exercised = False

    def __init__(self, cn1_app, flutter_app):
        self.apps = {"codenameone": cn1_app, "flutter": flutter_app}

    def available(self):
        if os.name != "nt":
            return False, "not a Windows host"
        for side, path in self.apps.items():
            if not os.path.exists(path):
                return False, "missing %s build at %s" % (side, path)
        return True, None

    def artifact(self, side):
        return self.apps[side]

    def _executable(self, side):
        return self.apps[side]

    def code_size(self, side):
        try:
            return os.path.getsize(self._executable(side))
        except OSError:
            return None

    def _memory(self, pid):
        out = benchlib.run([
            "powershell", "-NoProfile", "-Command",
            "(Get-Process -Id %d).WorkingSet64" % pid])
        try:
            return int(out.stdout.strip())
        except (TypeError, ValueError):
            return None


# ----------------------------------------------------------------------
# iOS simulator: launched through simctl, which hands back a host pid.
# ----------------------------------------------------------------------

class IOSAdapter(Adapter):
    """iOS, from RELEASE device bundles.

    Sizes only, and the reason is not a limitation of this harness.

    `flutter build ios --simulator --release` is refused outright by the
    Flutter tool -- "Release mode is not supported for simulators" -- because
    Dart cannot AOT-compile for the simulator, which runs a JIT debug engine.
    So a simulator comparison puts Flutter's debug build against a Codename One
    release build, and every number it produces is meaningless: the debug
    engine carries the whole JIT, is several times larger, and starts on a
    completely different path.

    What CI can do without provisioning is build BOTH sides for a device in
    release and size them. `--no-codesign` leaves an unsigned .app that is
    byte-accurate for sizing. Start-up and memory need the application to
    actually run, which needs a signed build on real hardware, so this adapter
    reports them as not measured rather than substituting a simulator figure.

    Pass `--ios-udid` for a real attached device to get timings too.
    """

    id = "ios"
    label = "iOS (release, device bundle)"
    exercised = False

    def __init__(self, udid, bundles, apps, renderer=None, device=False):
        self.udid = udid
        self.bundles = bundles
        self.apps = apps
        self.renderer = renderer
        self.device = device
        if renderer:
            self.id = "ios-%s" % renderer
            self.label = "iOS (release, %s)" % renderer

    def available(self):
        if not shutil.which("xcrun"):
            return False, "xcrun not on PATH"
        for side, path in self.apps.items():
            if not os.path.isdir(path):
                return False, "missing %s build at %s" % (side, path)
        return True, None

    def artifact(self, side):
        return self.apps[side]

    def code_size(self, side):
        return benchlib.macho_code_size(self.apps[side])

    def launch_and_time(self, side):
        if not self.device:
            raise Unavailable(
                "iOS start-up and memory are not measured here: Flutter cannot "
                "AOT-compile for the simulator, so a simulator run would time a "
                "JIT debug build against a release build. Sizes come from "
                "release device bundles; timings need signed hardware.")
        bundle = self.bundles[side]
        benchlib.run(["xcrun", "simctl", "terminate", self.udid, bundle])
        started = time.time()
        proc = subprocess.Popen(
            ["xcrun", "simctl", "launch", "--console-pty", self.udid, bundle],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
        # simctl prints "<bundle>: <pid>" on its own line. Reading the pid from
        # there beats searching the process table: the table lookup has to guess
        # at the executable's name, and both sides of this benchmark are
        # deliberately similar applications.
        pid_line = re.compile(r"^%s:\s*([0-9]+)\s*$" % re.escape(bundle))
        seen = {}

        def read_line():
            line = proc.stdout.readline()
            if not line:
                return None
            match = pid_line.match(line.strip())
            if match:
                seen["pid"] = int(match.group(1))
            return line

        try:
            upper, lower = self._time_stream(side, read_line, started)
            memory = None
            if upper is not None:
                memory = self._memory(bundle, seen.get("pid"))
            return upper, lower, memory
        finally:
            proc.terminate()
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()
            benchlib.run(["xcrun", "simctl", "terminate", self.udid, bundle])

    def _memory(self, bundle, pid=None):
        if pid is None:
            pid = self._pid_of(bundle)
        if pid is None:
            return None
        out = benchlib.run(["vmmap", "--summary", str(pid)])
        for line in out.stdout.splitlines():
            if line.startswith("Physical footprint:"):
                return _parse_bytes(line.split(":", 1)[1].strip())
        return None

    def _pid_of(self, bundle):
        """Fallback pid lookup, for a launch whose pid line was not seen.

        The executable name is the last path component of the installed
        bundle. This is a guess and the stream-parsed pid is preferred.
        """
        name = self.apps["codenameone"] if bundle == self.bundles["codenameone"] \
            else self.apps["flutter"]
        exe = os.path.splitext(os.path.basename(name))[0]
        out = benchlib.run(["pgrep", "-f", "%s.app/%s" % (exe, exe)])
        for token in out.stdout.split():
            try:
                return int(token)
            except ValueError:
                continue
        return None


# ----------------------------------------------------------------------
# Android: adb does the launching, and reports start-up itself.
# ----------------------------------------------------------------------

class AndroidAdapter(Adapter):
    """Android, both sides installed on one emulator or device.

    NOT YET EXERCISED.

    Start-up comes from `am start -W`, which reports TotalTime: the interval
    the platform itself considers "until the activity was drawn". That is a
    different clock from the desktop adapters' marker timing and the two are
    not interchangeable -- but it is the clock Android's own tooling and Play
    vitals use, and inventing a second one would make our number unreviewable.

    Memory is PSS from `dumpsys meminfo`, not RSS: on Android the runtime and
    its shared libraries are mapped into every process, and only PSS divides
    that shared cost correctly between them.
    """

    id = "android"
    label = "Android"
    exercised = False

    def __init__(self, serial, packages, activities, apks):
        self.serial = serial
        self.packages = packages
        self.activities = activities
        self.apks = apks

    def _adb(self, *args):
        cmd = ["adb"]
        if self.serial:
            cmd += ["-s", self.serial]
        return benchlib.run(cmd + list(args))

    def available(self):
        if not shutil.which("adb"):
            return False, "adb not on PATH"
        out = self._adb("get-state")
        if out.returncode != 0 or "device" not in out.stdout:
            return False, "no attached device or emulator"
        for side, path in self.apks.items():
            if not os.path.exists(path):
                return False, "missing %s apk at %s" % (side, path)
        return True, None

    def artifact(self, side):
        return self.apks[side]

    def code_size(self, side):
        """The dex and native libraries, which is the runtime plus the app.

        The rest of an apk is resources and assets, identical on both sides.
        """
        import zipfile as _zip
        total = 0
        try:
            with _zip.ZipFile(self.apks[side]) as archive:
                for info in archive.infolist():
                    if info.filename.endswith(".dex") or info.filename.endswith(".so"):
                        total += info.file_size
        except (OSError, _zip.BadZipFile):
            return None
        return total or None

    def wire_size_override(self, side):
        """An apk is already a zip, so its own size IS the download size."""
        return os.path.getsize(self.apks[side])

    def sizes(self, side, workdir):
        return {
            "install_bytes": benchlib.tree_size(self.apks[side]),
            "code_bytes": self.code_size(side),
            "wire_bytes": self.wire_size_override(side),
        }

    def launch_and_time(self, side):
        package = self.packages[side]
        self._adb("shell", "am", "force-stop", package)
        self._adb("shell", "logcat", "-c")
        out = self._adb("shell", "am", "start", "-W", "-n",
                        "%s/%s" % (package, self.activities[side]))
        cold = None
        for line in out.stdout.splitlines():
            if line.startswith("TotalTime:"):
                try:
                    cold = float(line.split(":", 1)[1].strip())
                except ValueError:
                    cold = None
        time.sleep(SETTLE_S)
        memory = self._memory(package)
        self._adb("shell", "am", "force-stop", package)
        # `am start -W` reports one number, so there is no bracket here.
        return cold, None, memory

    def _memory(self, package):
        out = self._adb("shell", "dumpsys", "meminfo", package)
        for line in out.stdout.splitlines():
            stripped = line.strip()
            if stripped.startswith("TOTAL PSS:"):
                try:
                    return int(stripped.split(":")[1].split()[0]) * 1024
                except (IndexError, ValueError):
                    return None
            if stripped.startswith("TOTAL"):
                parts = stripped.split()
                if len(parts) > 1 and parts[1].isdigit():
                    return int(parts[1]) * 1024
        return None


# ----------------------------------------------------------------------
# JavaScript: both sides are static bundles served over http.
# ----------------------------------------------------------------------

class JavaScriptAdapter(Adapter):
    """The web build of each side, measured in headless Chrome.

    NOT YET EXERCISED.

    Size is the served bundle rather than an installed tree, which is the
    honest analogue: nothing is installed, and what the user pays for is the
    transfer. Memory is the JS heap, which is the only figure a page can
    report about itself -- it excludes the renderer's own allocations, so it
    understates both sides and must not be compared against a native number.
    """

    id = "javascript"
    label = "JavaScript"
    exercised = False

    def __init__(self, bundles):
        self.bundles = bundles

    def available(self):
        chrome = _find_chrome()
        if not chrome:
            return False, "no Chrome or Chromium on PATH"
        for side, path in self.bundles.items():
            if not os.path.isdir(path):
                return False, "missing %s bundle at %s" % (side, path)
        return True, None

    def artifact(self, side):
        return self.bundles[side]

    def code_size(self, side):
        """Script bytes only: the runtime plus the compiled application."""
        total = 0
        for root, _dirs, files in os.walk(self.bundles[side]):
            for name in files:
                if name.endswith(".js") or name.endswith(".wasm"):
                    total += os.path.getsize(os.path.join(root, name))
        return total or None

    def launch_and_time(self, side):
        raise Unavailable(
            "the JavaScript adapter needs a Chrome DevTools driver; "
            "sizes are reported and timings are not")


def _find_chrome():
    for name in ("google-chrome", "chromium", "chromium-browser"):
        found = shutil.which(name)
        if found:
            return found
    mac = ("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")
    return mac if os.path.exists(mac) else None


def _parse_bytes(text):
    """Parses the human sizes vmmap and /proc print: '123.4M', '5016 kB'."""
    text = text.strip()
    match = re.match(r"([0-9]+(?:\.[0-9]+)?)\s*([KMGkmg]?)B?", text)
    if not match:
        return None
    value = float(match.group(1))
    scale = {"": 1, "k": 1024, "K": 1024,
             "m": 1024 ** 2, "M": 1024 ** 2,
             "g": 1024 ** 3, "G": 1024 ** 3}[match.group(2)]
    # /proc reports kB with no suffix letter in the number itself.
    if match.group(2) == "" and text.lower().endswith("kb"):
        scale = 1024
    return int(value * scale)
