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
import platform as _host
import re
import shutil
import queue
import subprocess
import threading
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

ANDROID_WARMUP_LAUNCHES = int(os.environ.get("BENCH_ANDROID_WARMUP", "3"))
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

    def _time_stream(self, side, reader, started):
        """Times from `started` to the side's markers, reading from `reader`.

        The clock is started by the caller immediately before the process is
        spawned, so it spans process creation too -- which is part of what a
        user waits for and is not something either runtime can measure about
        itself.

        Returns (upper_ms, lower_ms). `lower_ms` is None for a side that emits
        only one marker, and the report then treats the bracket as a point.

        Both limits are enforced by the CLOCK, never by the child's output.
        `reader` is a _LineReader, whose get() gives up after a timeout, so a
        launch that hangs before its marker is abandoned at LAUNCH_TIMEOUT_S,
        and an application that goes quiet after its last marker -- which is
        what a healthy one does -- is let go SETTLE_S after its marker. With a
        blocking readline both waited for one more line that might never come,
        and the job sat until the workflow's own two-hour timeout.

        The idle period is counted from the MARKER, not from the launch: the
        caller samples memory as soon as this returns, so a deadline measured
        from launch gave a side that started in 10 s only 2 s to settle and one
        that started in 13 s none at all -- the two sides were sampled at
        different points in their lifecycle. The Android adapter sleeps SETTLE_S
        after its launch returns for the same reason. LAUNCH_TIMEOUT_S bounds
        only the wait for the marker, so it cannot cut a settle short either.
        """
        marker = MARKERS[side]
        lower_marker = LOWER_BOUND_MARKERS.get(side)
        upper = None
        lower = None
        deadline = started + LAUNCH_TIMEOUT_S
        settle_until = None
        while True:
            now = time.time()
            limit = deadline if settle_until is None else settle_until
            if now >= limit:
                break
            line = reader.get(max(0.0, limit - now))
            if line is _LineReader.EOF:
                break
            if line is None:
                continue  # timed out; the checks above decide whether to stop
            if lower is None and lower_marker and lower_marker.search(line):
                lower = (time.time() - started) * 1000.0
            if upper is None and marker.search(line):
                seen = time.time()
                upper = (seen - started) * 1000.0
                settle_until = seen + SETTLE_S
        return upper, lower


class _LineReader(object):
    """A child's output as lines that can be waited for WITH A TIMEOUT.

    A pipe's readline() blocks until the child writes or exits, and nothing in
    the standard library puts a timeout on it portably -- select() does not
    work on pipes on Windows. So one daemon thread per stream does the blocking
    read and hands lines over through a queue, and the reader waits on the
    queue instead.
    """

    EOF = object()

    def __init__(self, stream, on_line=None):
        self._lines = queue.Queue()
        self._on_line = on_line
        thread = threading.Thread(target=self._pump, args=(stream,))
        thread.daemon = True
        thread.start()

    def _pump(self, stream):
        try:
            for line in iter(stream.readline, ""):
                if self._on_line is not None:
                    self._on_line(line)
                self._lines.put(line)
        except (OSError, ValueError):
            pass  # the stream was closed under us; that is an end too
        finally:
            self._lines.put(_LineReader.EOF)

    def get(self, timeout):
        """The next line, _LineReader.EOF at the end, or None on timeout."""
        try:
            return self._lines.get(timeout=timeout)
        except queue.Empty:
            return None


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
            upper, lower = self._time_stream(side, _LineReader(proc.stdout), started)
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
        if _host.system() != "Darwin":
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
    """Linux desktop: Flutter's `linux` bundle against Codename One's NATIVE
    Linux port (ParparVM to an ELF against GTK3/Cairo), not the JVM build.

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
        if _host.system() != "Linux":
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
        return _launchable(self.apps[side], _is_linux_executable, "ELF executable")

    def code_size(self, side):
        """Every ELF in the artifact: see benchlib.elf_code_size for why."""
        return benchlib.elf_code_size(self.apps[side])

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


def _launchable(artifact, is_executable, kind):
    """The one binary to launch in a desktop artifact.

    The recipes hand over what each toolchain produces, which is a DIRECTORY:
    Flutter's Linux bundle and Windows Release folder, and the result folder
    of Codename One's native builders. Popen cannot launch a directory, so
    accepting only an executable file reported every desktop side as having no
    executable at all. A file is still taken as-is.

    Top level only, deliberately: the libraries each runtime ships beside its
    launcher live in subfolders or carry library names, and more than one
    candidate is reported rather than guessed at -- launching the wrong one
    would time something that is not the application.
    """
    if os.path.isfile(artifact):
        if is_executable(artifact):
            return artifact
        raise Unavailable("%s is not an %s" % (artifact, kind))
    if not os.path.isdir(artifact):
        raise Unavailable("no artifact at %s" % artifact)
    found = sorted(os.path.join(artifact, name) for name in os.listdir(artifact)
                   if is_executable(os.path.join(artifact, name)))
    if len(found) == 1:
        return found[0]
    if not found:
        raise Unavailable("no %s in %s" % (kind, artifact))
    raise Unavailable("more than one %s in %s, refusing to guess: %s"
                      % (kind, artifact, ", ".join(os.path.basename(f) for f in found)))


def _is_linux_executable(path):
    """An ELF regular file with the execute bit that is not a shared library.

    The name decides the library case, because the ELF type cannot: a modern
    position-independent executable is ET_DYN exactly like a .so, and builds
    commonly leave the execute bit set on libraries too.
    """
    name = os.path.basename(path)
    if not os.path.isfile(path) or os.path.islink(path) or not os.access(path, os.X_OK):
        return False
    if name.endswith(".so") or ".so." in name:
        return False
    try:
        with open(path, "rb") as handle:
            return handle.read(4) == b"\x7fELF"
    except OSError:
        return False


def _is_windows_executable(path):
    return os.path.isfile(path) and path.lower().endswith(".exe")


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
        return _launchable(self.apps[side], _is_windows_executable, ".exe")

    def code_size(self, side):
        """Every PE image in the artifact: see benchlib.pe_code_size for why."""
        return benchlib.pe_code_size(self.apps[side])

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

        def note_pid(line):
            match = pid_line.match(line.strip())
            if match:
                seen["pid"] = int(match.group(1))

        try:
            upper, lower = self._time_stream(
                side, _LineReader(proc.stdout, on_line=note_pid), started)
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
        self._installed = set()

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

    # The ABI every size is reported for: what a phone is actually delivered.
    SIZE_ABI = "arm64-v8a"

    def _entries(self, side):
        import zipfile as _zip
        try:
            with _zip.ZipFile(self.apks[side]) as archive:
                return archive.infolist()
        except (OSError, _zip.BadZipFile):
            return None

    @classmethod
    def _other_abi(cls, name):
        """Whether a zip entry is a native library for an ABI other than SIZE_ABI."""
        parts = name.split("/")
        return len(parts) > 2 and parts[0] == "lib" and parts[1] != cls.SIZE_ABI

    def code_size(self, side):
        """The dex and the SIZE_ABI native libraries: the runtime plus the app.

        One ABI, not every ABI in the file. `flutter build apk` produces a FAT
        apk with the engine and the Dart image for arm64, armv7 and x86_64, and
        a phone installs one of them -- Play delivers per-ABI splits. Summing all
        three counted Flutter's code about three times over and reported a 13x
        difference that no user would see. The rule is the same for both sides.
        """
        entries = self._entries(side)
        if entries is None:
            return None
        total = sum(i.file_size for i in entries
                    if not self._other_abi(i.filename)
                    and (i.filename.endswith(".dex") or i.filename.endswith(".so")))
        return total or None

    def delivered_size(self, side):
        """The apk as delivered for SIZE_ABI: the file minus other ABIs' libraries.

        Each excluded entry costs its compressed bytes plus its local and central
        directory headers (30 and 46 bytes, each carrying the name), so an apk
        with no other ABIs comes out at exactly its file size.
        """
        entries = self._entries(side)
        if entries is None:
            return None
        excluded = sum(i.compress_size + 30 + 46 + 2 * len(i.filename.encode("utf-8"))
                       for i in entries if self._other_abi(i.filename))
        return os.path.getsize(self.apks[side]) - excluded

    def sizes(self, side, workdir):
        # An apk is already a zip, so its size as delivered IS the download size.
        delivered = self.delivered_size(side)
        return {
            "install_bytes": delivered,
            "code_bytes": self.code_size(side),
            "wire_bytes": delivered,
        }

    def notes(self):
        # Each note parenthesized: a list of bare adjacent literals is where a
        # missing comma silently merges two items, so every boundary is explicit.
        return [
            ("Android sizes are for the arm64-v8a slice a phone is delivered, on both "
             "sides; Flutter's release apk carries three ABIs and would otherwise count "
             "its engine about three times."),
            ("Android start-up and memory come from an x86_64 emulator with a software "
             "GPU. They compare the two runtimes on one machine; they are not phone "
             "timings, and a renderer that leans on the GPU is penalised more there."),
        ]

    def _install(self, side):
        """Installs the APK that was sized, once per run, replacing any other.

        Launching without installing measured nothing on a fresh emulator --
        the package was simply not there -- and on a reused device it could
        measure whatever older build happened to be installed under the same
        package, beside the size of the one that was built. Uninstalling first
        rather than `install -r` also clears a copy signed with another key,
        which `-r` refuses to replace.
        """
        if side in self._installed:
            return
        package = self.packages[side]
        self._adb("uninstall", package)  # absent is fine; that is the usual case
        out = self._adb("install", "-t", self.apks[side])
        if out.returncode != 0 or "Success" not in (out.stdout or ""):
            detail = ((out.stdout or "") + (out.stderr or "")).strip().splitlines()
            raise Unavailable("could not install the %s apk: %s"
                              % (side, detail[-1] if detail else "adb returned %d" % out.returncode))
        # Compiled to its steady state BEFORE anything is timed. A fresh install
        # is compiled in the background as it runs, so start-up fell across
        # every run -- 2853, 888, 1209, 543, 418 ms in one of them -- and
        # best-of-five picked whichever run the compiler had reached. The
        # "best" moved 282 to 418 ms between runs of unchanged code and tripped
        # a 25% gate. `speed` is the state an idle device optimizes an app to,
        # applied to both sides (Flutter's own dex is small, its code is AOT).
        compiled = self._adb("shell", "cmd", "package", "compile", "-m", "speed", "-f", package)
        if compiled.returncode != 0 or "Success" not in (compiled.stdout or ""):
            raise Unavailable("could not compile the %s app ahead of time: %s"
                              % (side, ((compiled.stdout or "") + (compiled.stderr or "")).strip()))
        # Launches that are not timed: first-run work -- creating the app's
        # data directory, first-launch caches -- belongs to installation, not
        # to start-up. Three, not one: with a single warm-up both sides still
        # fell across all five timed runs (Codename One 1065, 1362, 478, 406,
        # 367 ms; Flutter 1993 to 1130), because the emulator's own caches
        # keep warming after the app's code is compiled.
        for _ in range(ANDROID_WARMUP_LAUNCHES):
            self._adb("shell", "am", "start", "-W", "-n", "%s/%s" % (package, self.activities[side]))
            self._adb("shell", "am", "force-stop", package)
        self._installed.add(side)

    def launch_and_time(self, side):
        self._install(side)
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
