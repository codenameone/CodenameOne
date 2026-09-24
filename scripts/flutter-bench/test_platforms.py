#!/usr/bin/env python3
"""Tests for the adapter mechanics that do not need a real build or device.

    python3 scripts/flutter-bench/test_platforms.py

Marker timing against real child processes, and how a desktop artifact
directory is resolved to the binary to launch and the code to size. The
measurements themselves still need real builds; see test_benchlib.py.
"""

import os
import stat
import subprocess
import sys
import tempfile
import time
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import benchlib  # noqa: E402
import platforms  # noqa: E402


def _child(code):
    return subprocess.Popen([sys.executable, "-u", "-c", code],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, bufsize=1)


class TimeStreamDeadlines(unittest.TestCase):
    """The limits hold whatever the child does, because the clock enforces them."""

    def setUp(self):
        self._saved = (platforms.LAUNCH_TIMEOUT_S, platforms.SETTLE_S)
        platforms.LAUNCH_TIMEOUT_S = 2.0
        platforms.SETTLE_S = 1.0
        self.adapter = platforms.Adapter()

    def tearDown(self):
        platforms.LAUNCH_TIMEOUT_S, platforms.SETTLE_S = self._saved

    def _time(self, side, code):
        proc = _child(code)
        try:
            started = time.time()
            result = self.adapter._time_stream(side, platforms._LineReader(proc.stdout), started)
            return result, time.time() - started
        finally:
            proc.kill()
            proc.wait()

    def test_a_silent_hang_is_abandoned_at_the_launch_timeout(self):
        (upper, lower), elapsed = self._time("codenameone", "import time; time.sleep(60)")
        self.assertIsNone(upper)
        self.assertLess(elapsed, 5.0, "a blocking read would have waited out the child")

    def test_a_quiet_app_after_its_marker_is_let_go_at_settle(self):
        # What a HEALTHY app does: print the marker, then nothing. The loop used
        # to wait for another line before it would look at the settle time.
        (upper, _), elapsed = self._time(
            "codenameone", "import time; print('BENCH:FIRSTFRAME'); time.sleep(60)")
        self.assertIsNotNone(upper)
        self.assertLess(elapsed, 2.5)
        self.assertGreaterEqual(elapsed, 0.9, "the idle period is still waited for")

    def test_the_idle_period_is_counted_from_the_marker_not_the_launch(self):
        # A side that reaches its marker late still gets the full SETTLE_S
        # before memory is sampled; a deadline from launch cut it to nothing.
        # The marker also lands close to LAUNCH_TIMEOUT_S, which bounds only
        # the wait FOR the marker.
        (upper, _), elapsed = self._time(
            "codenameone", "import time; time.sleep(1.5); print('BENCH:FIRSTFRAME');"
                           "time.sleep(60)")
        self.assertIsNotNone(upper)
        self.assertGreaterEqual(elapsed, upper / 1000.0 + 0.9,
                                "the settle must start at the marker")
        self.assertLess(elapsed, 4.0)

    def test_the_flutter_bracket_is_still_read(self):
        (upper, lower), _ = self._time(
            "flutter", "import time; print('BENCH:FIRSTCONTENT'); time.sleep(0.2);"
                       "print('BENCH:RASTERDONE'); time.sleep(60)")
        self.assertIsNotNone(lower)
        self.assertIsNotNone(upper)
        self.assertLessEqual(lower, upper)

    def test_an_exit_ends_the_wait(self):
        (upper, _), elapsed = self._time("codenameone", "print('unrelated')")
        self.assertIsNone(upper)
        self.assertLess(elapsed, 1.5)


def _elf(text, rodata):
    """A minimal ELF64 with an executable .text and a non-executable .rodata."""
    import struct
    header = bytearray(64)
    header[:4] = b"\x7fELF"
    header[4], header[5] = 2, 1                        # 64-bit, little-endian
    struct.pack_into("<Q", header, 0x28, 64)           # e_shoff
    struct.pack_into("<HH", header, 0x3A, 64, 3)       # e_shentsize, e_shnum
    sections = bytearray(64 * 3)                       # [0] is the null section
    struct.pack_into("<IIQ", sections, 64 + 0, 0, 1, 0x6)   # .text: PROGBITS, ALLOC|EXECINSTR
    struct.pack_into("<Q", sections, 64 + 0x20, text)
    struct.pack_into("<IIQ", sections, 128 + 0, 0, 1, 0x2)  # .rodata: PROGBITS, ALLOC
    struct.pack_into("<Q", sections, 128 + 0x20, rodata)
    return bytes(header + sections) + b"\0" * (text + rodata)


def _pe(code, data):
    """A minimal PE with one executable and one data section."""
    import struct
    image = bytearray(0x40 + 24 + 80)
    image[:2] = b"MZ"
    struct.pack_into("<I", image, 0x3C, 0x40)
    image[0x40:0x44] = b"PE\0\0"
    struct.pack_into("<H", image, 0x40 + 6, 2)         # NumberOfSections
    struct.pack_into("<H", image, 0x40 + 20, 0)        # SizeOfOptionalHeader
    table = 0x40 + 24
    struct.pack_into("<I", image, table + 16, code)
    struct.pack_into("<I", image, table + 36, 0x60000020)   # CODE|EXECUTE|READ
    struct.pack_into("<I", image, table + 40 + 16, data)
    struct.pack_into("<I", image, table + 40 + 36, 0x40000040)  # INITIALIZED_DATA|READ
    return bytes(image) + b"\0" * (code + data)


class DesktopArtifacts(unittest.TestCase):
    """A recipe hands over a DIRECTORY; the adapter finds the binary in it."""

    def setUp(self):
        self.dir = tempfile.mkdtemp()

    def _write(self, rel, content, executable=False):
        path = os.path.join(self.dir, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as handle:
            handle.write(content)
        if executable:
            os.chmod(path, os.stat(path).st_mode | stat.S_IXUSR)
        return path

    def test_linux_bundle_launches_the_executable_and_sizes_every_elf(self):
        exe = self._write("gallery", _elf(100, 50), executable=True)
        self._write("lib/libapp.so", _elf(5000, 3000), executable=True)
        self._write("lib/libflutter_linux_gtk.so", _elf(7000, 1000))
        self._write("data/flutter_assets/AssetManifest.json", b"{}")
        adapter = platforms.LinuxAdapter(self.dir, self.dir)
        self.assertEqual(exe, adapter._executable("flutter"),
                         "a shared library with an executable bit is not the app")
        self.assertEqual(12100, adapter.code_size("flutter"),
                         "the Dart image and the engine are code too, not just the launcher")

    def test_resources_linked_into_the_executable_are_not_code(self):
        # Codename One's native Linux port .incbin's the application's resources
        # into .rodata; counting whole files reported the artwork as code.
        self._write("Bench", _elf(4000, 90000), executable=True)
        self.assertEqual(4000, platforms.LinuxAdapter(self.dir, self.dir).code_size("codenameone"))

    def test_linux_ambiguity_fails_loudly(self):
        self._write("one", _elf(10, 0), executable=True)
        self._write("two", _elf(10, 0), executable=True)
        with self.assertRaises(platforms.Unavailable):
            platforms.LinuxAdapter(self.dir, self.dir)._executable("flutter")

    def test_windows_release_directory_sizes_every_pe_image(self):
        exe = self._write("gallery.exe", _pe(100, 400))
        self._write("flutter_windows.dll", _pe(9000, 2000))
        self._write("data/app.so", _elf(4000, 0))
        adapter = platforms.WindowsAdapter(self.dir, self.dir)
        self.assertEqual(exe, adapter._executable("flutter"))
        self.assertEqual(9100, benchlib.pe_code_size(self.dir))

    def test_a_file_artifact_is_used_as_is(self):
        exe = self._write("Bench", _elf(10, 0), executable=True)
        self.assertEqual(exe, platforms.LinuxAdapter(exe, exe)._executable("codenameone"))


class AndroidInstall(unittest.TestCase):
    """The APK that was sized is the one launched, installed fresh."""

    def setUp(self):
        self.bin = tempfile.mkdtemp()
        self.log = os.path.join(self.bin, "calls.log")
        self.result = os.path.join(self.bin, "install_result")
        with open(self.result, "w") as handle:
            handle.write("Success")
        adb = os.path.join(self.bin, "adb")
        with open(adb, "w") as handle:
            handle.write("#!/bin/sh\n"
                         "echo \"$*\" >> '%s'\n"
                         "case \"$*\" in *\\ install\\ *|install\\ *) cat '%s';; "
                         "*'package compile'*) echo Success;; "
                         "*'am start'*) echo 'TotalTime: 123';; esac\n"
                         % (self.log, self.result))
        os.chmod(adb, 0o755)
        self._path = os.environ["PATH"]
        os.environ["PATH"] = self.bin + os.pathsep + self._path
        self._settle = platforms.SETTLE_S
        platforms.SETTLE_S = 0
        apk = os.path.join(self.bin, "app.apk")
        open(apk, "wb").close()
        self.adapter = platforms.AndroidAdapter(
            None, {"codenameone": "com.example.bench", "flutter": "com.x.gallery"},
            {"codenameone": ".Main", "flutter": ".MainActivity"},
            {"codenameone": apk, "flutter": apk})

    def tearDown(self):
        os.environ["PATH"] = self._path
        platforms.SETTLE_S = self._settle

    def _calls(self):
        with open(self.log) as handle:
            return [line.strip() for line in handle]

    def test_uninstalls_then_installs_before_the_first_launch_only(self):
        cold, _, _ = self.adapter.launch_and_time("codenameone")
        self.adapter.launch_and_time("codenameone")
        calls = self._calls()
        installs = [c for c in calls if c.startswith("install")]
        self.assertEqual(1, len(installs), "once per run, not per launch: %s" % calls)
        self.assertLess(calls.index("uninstall com.example.bench"), calls.index(installs[0]))
        first_start = next(i for i, c in enumerate(calls) if "am start" in c)
        self.assertLess(calls.index(installs[0]), first_start, "installed before it is launched")
        compile_at = calls.index("shell cmd package compile -m speed -f com.example.bench")
        self.assertLess(compile_at, first_start, "compiled to steady state before any launch")
        starts = [c for c in calls if "am start" in c]
        self.assertEqual(platforms.ANDROID_WARMUP_LAUNCHES + 2, len(starts),
                         "the untimed warm-up launches, then the two timed ones")
        self.assertEqual(123.0, cold)

    def test_a_failed_install_is_reported_not_measured(self):
        with open(self.result, "w") as handle:
            handle.write("Failure [INSTALL_PARSE_FAILED_NO_CERTIFICATES]")
        with self.assertRaises(platforms.Unavailable) as caught:
            self.adapter.launch_and_time("flutter")
        self.assertIn("NO_CERTIFICATES", str(caught.exception))
        self.assertFalse(any("am start" in c for c in self._calls()),
                         "nothing may be timed when the install failed")


class AndroidSizing(unittest.TestCase):
    """Sizes are for the one ABI a phone is delivered, on both sides."""

    def _apk(self, entries):
        import zipfile
        path = os.path.join(tempfile.mkdtemp(), "app.apk")
        with zipfile.ZipFile(path, "w", zipfile.ZIP_STORED) as archive:
            for name, size in entries:
                archive.writestr(name, b"x" * size)
        return path

    def _adapter(self, apk):
        return platforms.AndroidAdapter(None, {"codenameone": "a", "flutter": "b"},
                                        {"codenameone": ".A", "flutter": ".B"},
                                        {"codenameone": apk, "flutter": apk})

    def test_a_fat_apk_counts_one_abi(self):
        apk = self._apk([("classes.dex", 1000), ("lib/arm64-v8a/libapp.so", 5000),
                         ("lib/x86_64/libapp.so", 5000), ("lib/armeabi-v7a/libapp.so", 5000),
                         ("assets/flutter_assets/a.png", 2000)])
        adapter = self._adapter(apk)
        self.assertEqual(6000, adapter.code_size("flutter"),
                         "the dex and the arm64 libraries, not all three ABIs")
        delivered = adapter.delivered_size("flutter")
        self.assertLess(delivered, os.path.getsize(apk) - 9900)
        self.assertGreater(delivered, 8000, "the arm64 slice, the dex and the assets stay")

    def test_a_single_abi_apk_is_its_own_size(self):
        apk = self._apk([("classes.dex", 4000), ("lib/arm64-v8a/libcn1.so", 700),
                         ("assets/a.png", 2000)])
        adapter = self._adapter(apk)
        self.assertEqual(os.path.getsize(apk), adapter.delivered_size("codenameone"))
        self.assertEqual(4700, adapter.code_size("codenameone"))


if __name__ == "__main__":
    unittest.main()
