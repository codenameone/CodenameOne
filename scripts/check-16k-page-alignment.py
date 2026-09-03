#!/usr/bin/env python3
"""Reject 64-bit native libraries that are not 16 KB page aligned.

Android 15 introduced devices with a 16 KB memory page size, and Google Play
requires every 64-bit shared library in an upload to be laid out for them:
new apps and updates targeting API 35+ have needed it since 2025-11-01, and
Wear OS uploads containing native code need it (alongside 64-bit slices) from
2026-09-15. See https://developer.android.com/guide/practices/page-sizes.

"Laid out for them" means each `PT_LOAD` program header carries `p_align` of
at least 0x4000. Alignment is baked in at *link* time, so no packaging step
can repair it after the fact -- AGP aligns the ZIP entries, and bundletool
reports the result, but neither one can move a segment inside a prebuilt
`.so`. A library linked with a 4 KB assumption stays broken until it is
rebuilt, which is why this is a gate on the checked-in artifact rather than a
check on the generated app.

The failure it prevents is quiet in exactly the way that matters: the app
builds, uploads, and runs on every 4 KB device the developer owns, and only
Play review or a 16 KB device says otherwise. And because the artifact is
Codename One's rather than the developer's, they cannot fix it themselves.

The alignment rule is scoped twice. It covers 64-bit ELF only, because 16 KB
pages are a 64-bit feature and `armeabi-v7a`/`x86` never run on such a
device. And it covers only libraries Android packaging actually ships -- a
desktop Linux `.so` is built for 4 KB pages and is correct at 0x1000, and a
cn1lib can carry one (Cn1libMojo packages `linux`/`javase` resources into
`nativelinux`/`nativese`). Everything skipped is still reported under
--verbose, and the integrity checks below are NOT scoped: a `.so` that is not
an ELF, or one that is truncated, is a bug on any platform.

Usage:
  scripts/check-16k-page-alignment.py             # every tracked artifact
  scripts/check-16k-page-alignment.py PATH ...    # only these files
  scripts/check-16k-page-alignment.py --verbose   # list everything scanned
"""

import io
import os
import re
import struct
import subprocess
import sys
import zipfile

# Google's required minimum `p_align` for a PT_LOAD segment. A library built
# by NDK r28+ gets this by default; r26/r27 need both
# -Wl,-z,max-page-size=16384 and -Wl,-z,common-page-size=16384.
REQUIRED_ALIGNMENT = 0x4000

# Containers and libraries are recognised by content, never by file
# extension. An extension list is a curated allow-list, and this one was
# already wrong: it knew `.aar`/`.apk`/`.aab`/`.jar`/`.zip` and so walked
# straight past every `.cn1lib` in the tree -- Codename One's own library
# format, a ZIP, and the standard packaging path by which a cn1lib ships
# Android natives. `tests/core/lib/QRScanner.cn1lib` reaches real `.so`
# files through `nativeand.zip` and then `ZBarScannerLibrary.aar`, and none
# of them were being inspected.
#
# Sniffing the magic bytes ends that class of bug instead of moving the
# list forward one entry: any container, whatever it is called, and any
# ELF, whatever it is called, is found.
ZIP_MAGIC = (b"PK\x03\x04", b"PK\x05\x06", b"PK\x07\x08")

# Content decides what gets *scanned*; these names decide what gets
# *challenged*. They are not the old allow-list returning -- nothing is found
# by being on them, and a container with a name nobody anticipated is still
# picked up by its magic. They encode a second rule: a file whose name
# declares a format must actually be that format. Without it, a zeroed or
# truncated `libfoo.so` matches no magic, is classified as an ordinary file,
# and is skipped in silence -- the exact "could not look reads as clean" hole
# this gate exists to avoid, reachable by corrupting a library rather than
# misaligning it.
ARCHIVE_NAME_SUFFIXES = (".aar", ".apk", ".aab", ".jar", ".zip", ".cn1lib")

# 16 KB pages are an Android/Google Play requirement, not a property of ELF.
# An ordinary desktop Linux x86_64 `.so` is built for 4 KB pages and is
# perfectly correct at 0x1000, so the alignment rule must apply only where
# Android packaging says the library ships to a device. Codename One really
# can carry such a file: Cn1libMojo.buildLinux() packages a cn1lib's
# `linux/src/main/resources` into `nativelinux`, and buildJavase() does the
# same for `nativese`. Without this scoping the workflow -- which reads every
# tracked file -- would block every PR over a valid artifact.
#
# Only the alignment rule is scoped. The integrity checks (a `.so` that is
# not an ELF, a truncated one) stay universal, because those are bugs on any
# platform.
ANDROID_CONTAINER_SUFFIXES = (".aar", ".apk", ".aab")

# The Android payload a cn1lib carries, written by Cn1libMojo.buildAndroid().
ANDROID_CN1LIB_PAYLOAD = "nativeand.zip"

# Directory names Android packaging uses for native libraries, and the ABI
# directory that must sit directly inside one. Together they are what marks a
# path as Android even outside an AAR -- a loose `jniLibs/arm64-v8a/libfoo.so`
# in a port is still an Android artifact.
ANDROID_LIB_DIRS = frozenset(["jni", "lib", "jnilibs"])
ANDROID_ABIS = frozenset([
    "armeabi", "armeabi-v7a", "arm64-v8a", "x86", "x86_64",
    "mips", "mips64", "riscv64",
])

# An archive inside an archive is legitimate and, as the QRScanner cn1lib
# shows, three deep happens in practice. Bounded so a malformed or hostile
# ZIP cannot spin this gate forever, but with headroom: exceeding the limit
# is reported as a failure, so a limit set too close to real usage would
# raise false alarms rather than silently skip.
MAX_ARCHIVE_DEPTH = 8

# A whole-tree run that inspected nothing must not report success: a wrong
# working directory, or a `git ls-files` that came back empty, would
# otherwise read exactly like a clean tree. This repository ships one native
# artifact (the cn1-ai-whisper AAR) holding six 64-bit libraries. If that
# artifact is ever legitimately removed, lower this floor in the same commit
# rather than deleting the check.
MIN_LIBRARIES_SCANNED = 1

ELF_MAGIC = b"\x7fELF"
# Longest magic this gate sniffs, so callers know how many bytes to read.
MAGIC_LENGTH = 4
ELFCLASS32 = 1
ELFCLASS64 = 2
ELFDATA2LSB = 1
ELFDATA2MSB = 2
PT_LOAD = 1

# Per ELF class: header size, the offsets of e_phoff / e_phentsize / e_phnum,
# the struct code for an address-sized word, the smallest legal program header
# entry, and the offsets of p_offset / p_filesz / p_align inside one. The two
# program header layouts differ in field order, not just in width, so the
# offsets cannot be derived from the word size.
ELF_LAYOUT = {
    ELFCLASS64: (64, 0x20, 0x36, 0x38, "Q", 56, 0x08, 0x20, 0x30),
    ELFCLASS32: (52, 0x1C, 0x2A, 0x2C, "I", 32, 0x04, 0x10, 0x1C),
}


class MalformedElf(Exception):
    """A `.so` payload that cannot be read as a well-formed ELF."""


def read_load_alignments(data):
    """Return `(is_64_bit, [p_align of every PT_LOAD segment])`.

    Raises MalformedElf for anything that does not parse -- a truncated
    library, a corrupted one, or a file that merely happens to be named
    `.so`. Nothing is skipped: an entry this function could not read is an
    entry whose alignment nobody checked, and that must not come out reading
    the same as one checked and found compliant. Returning "exempt" for an
    unreadable payload would also make the exemption reachable by corruption
    -- a truncated 64-bit library starts with the ELF magic and would be
    waved through as if it were a 32-bit slice.

    32-bit libraries are parsed rather than waved past on the class byte
    alone, for the same reason: `is_64_bit` is False only once the file has
    been shown to really be a 32-bit ELF. The caller exempts it from the
    alignment rule, which does not apply to 32-bit ABIs.
    """
    if len(data) < 5 or data[:4] != ELF_MAGIC:
        raise MalformedElf("not an ELF file")

    ei_class = data[4]
    if ei_class not in ELF_LAYOUT:
        raise MalformedElf("unknown ELF class 0x%02x" % ei_class)
    (header_size, phoff_off, phentsize_off, phnum_off, word, min_phentsize,
     poffset_off, pfilesz_off, palign_off) = ELF_LAYOUT[ei_class]

    if len(data) < header_size:
        raise MalformedElf(
            "truncated: %d bytes, shorter than the %d-byte ELF header"
            % (len(data), header_size))

    endian = data[5]
    if endian == ELFDATA2LSB:
        prefix = "<"
    elif endian == ELFDATA2MSB:
        prefix = ">"
    else:
        raise MalformedElf("unknown ELF data encoding 0x%02x" % endian)

    e_phoff = struct.unpack_from(prefix + word, data, phoff_off)[0]
    e_phentsize = struct.unpack_from(prefix + "H", data, phentsize_off)[0]
    e_phnum = struct.unpack_from(prefix + "H", data, phnum_off)[0]

    if e_phentsize < min_phentsize:
        raise MalformedElf(
            "program header entry size %d is too small" % e_phentsize)
    if e_phoff == 0 or e_phnum == 0:
        raise MalformedElf("no program headers")
    if e_phoff + e_phnum * e_phentsize > len(data):
        raise MalformedElf("program header table runs past end of file")

    alignments = []
    for index in range(e_phnum):
        offset = e_phoff + index * e_phentsize
        # p_type is the first word of a program header in both classes.
        p_type = struct.unpack_from(prefix + "I", data, offset)[0]
        if p_type != PT_LOAD:
            continue
        # A segment that claims to run past the end of the file is a
        # truncated artifact. Its program headers can still parse -- they sit
        # near the front -- so without this the alignment reads as correct on
        # a library whose contents are simply not all there.
        p_offset = struct.unpack_from(prefix + word, data, offset + poffset_off)[0]
        p_filesz = struct.unpack_from(prefix + word, data, offset + pfilesz_off)[0]
        if p_offset + p_filesz > len(data):
            raise MalformedElf(
                "truncated: PT_LOAD segment ends at %d, past the %d-byte file"
                % (p_offset + p_filesz, len(data)))
        alignments.append(
            struct.unpack_from(prefix + word, data, offset + palign_off)[0])
    if not alignments:
        raise MalformedElf("no PT_LOAD segments")
    return ei_class == ELFCLASS64, alignments


def is_android_payload(label):
    """True when the accumulated label shows Android packaging.

    `label` is the scan path, with `!` separating each archive from what was
    read out of it, so this sees the whole chain -- an `.so` reached through
    `QRScanner.cn1lib!nativeand.zip!ZBarScannerLibrary.aar!jni/...` is Android
    because of containers three levels up.
    """
    for part in label.split("!"):
        base = os.path.basename(part).lower()
        if base.endswith(ANDROID_CONTAINER_SUFFIXES):
            return True
        if base == ANDROID_CN1LIB_PAYLOAD:
            return True
        segments = [segment.lower() for segment in part.replace("\\", "/").split("/")]
        for index in range(len(segments) - 1):
            if (segments[index] in ANDROID_LIB_DIRS
                    and segments[index + 1] in ANDROID_ABIS):
                return True
    return False


# A shared-library soname version is numeric: `libfoo.so.1`, `libfoo.so.1.2`.
SONAME_VERSION = re.compile(r"[0-9]+(\.[0-9]+)*\Z")


def looks_like_shared_library(name):
    """True for `libfoo.so` and for a versioned `libfoo.so.1` or `.so.1.2`.

    The version has to be numeric. A looser "contains `.so.`" test also
    swallows the metadata that sits beside a native library -- a
    `libfoo.so.sha256` checksum, a `libfoo.so.asc` signature -- and this gate
    reads every tracked path, so it would fail the whole run with "not an ELF
    file" over a text file. Being strict here costs nothing: a file that is
    genuinely a library is still found by its magic whatever it is called.
    """
    base = os.path.basename(name).lower()
    if base.endswith(".so"):
        return True
    _, separator, version = base.partition(".so.")
    return bool(separator) and SONAME_VERSION.match(version) is not None


def looks_like_archive(name):
    return os.path.basename(name).lower().endswith(ARCHIVE_NAME_SUFFIXES)


def classify(name, head):
    """Return "elf", "zip" or None for one file, from its name and content.

    Declarations are answered before discoveries, and for both formats
    symmetrically. A name that declares a format routes to that format's
    reader whatever the bytes turn out to be, so a file that is not what it
    says it is gets reported instead of quietly reinterpreted. Checking the
    library name first but leaving the archive name as a fallback after
    content was an asymmetry with real consequences: a valid ELF left in
    place of an `.aar` was read as a library and counted as a compliant
    Android slice, and the same bytes named `.cn1lib` were waved through as
    non-Android -- either way a destroyed container kept the run green.

    Only once a name declares nothing does content decide, which is what
    finds a container or a library under a name nobody anticipated.

    A deliberately corrupt archive fixture would now be reported. None
    exists, and renaming it would be the fix; the alternative is a gate that
    cannot tell a broken container from an absent one.
    """
    if looks_like_shared_library(name):
        return "elf"
    if looks_like_archive(name):
        return "zip"
    if head[:4] == ELF_MAGIC:
        return "elf"
    if head[:4] in ZIP_MAGIC:
        return "zip"
    return None


class Scanner(object):
    def __init__(self, verbose):
        self.verbose = verbose
        self.failures = []
        self.libraries_scanned = 0
        self.skipped_32bit = 0
        self.skipped_non_android = 0
        self.has_alignment_failure = False

    def check_library(self, label, data):
        try:
            is_64_bit, alignments = read_load_alignments(data)
        except MalformedElf as error:
            self.failures.append("%s: %s" % (label, error))
            return
        if not is_64_bit:
            self.skipped_32bit += 1
            if self.verbose:
                print("  32-bit (not subject to 16 KB pages): %s" % label)
            return
        if not is_android_payload(label):
            # Parsed and sound, but not shipped to an Android device, so the
            # Play requirement does not reach it.
            self.skipped_non_android += 1
            if self.verbose:
                print("  not Android packaging (16 KB rule does not apply): %s"
                      % label)
            return

        self.libraries_scanned += 1
        worst = min(alignments)
        if worst < REQUIRED_ALIGNMENT:
            self.has_alignment_failure = True
            self.failures.append(
                "%s: PT_LOAD p_align 0x%x, needs 0x%x or more"
                % (label, worst, REQUIRED_ALIGNMENT))
        elif self.verbose:
            print("  ok 0x%x: %s" % (worst, label))

    def scan_archive(self, label, data, depth):
        if depth > MAX_ARCHIVE_DEPTH:
            # Reported for the same reason an unreadable archive is: whatever
            # is nested this deep went unexamined, and the run must not call
            # itself clean over it.
            self.failures.append(
                "%s: nested more than %d archives deep, not inspected"
                % (label, MAX_ARCHIVE_DEPTH))
            return
        try:
            archive = zipfile.ZipFile(io.BytesIO(data))
        except (zipfile.BadZipFile, OSError) as error:
            # Reported, never skipped. An archive this gate cannot open is one
            # whose libraries it did not look at, and "could not look" must not
            # come out reading the same as "looked and found nothing" -- a
            # truncated artifact would then be waved through. Every archive in
            # the tree opens today, so there is no benign case to tolerate.
            self.failures.append("%s: cannot read archive (%s)" % (label, error))
            return
        with archive:
            for entry in archive.infolist():
                if entry.is_dir():
                    continue
                # Only the magic is decompressed for entries that turn out to
                # be neither, which is nearly all of them.
                try:
                    with archive.open(entry) as stream:
                        head = stream.read(MAGIC_LENGTH)
                except (zipfile.BadZipFile, OSError, RuntimeError) as error:
                    self.failures.append(
                        "%s!%s: cannot read entry (%s)"
                        % (label, entry.filename, error))
                    continue
                kind = classify(entry.filename, head)
                if kind is None:
                    continue
                nested = "%s!%s" % (label, entry.filename)
                if kind == "elf":
                    self.check_library(nested, archive.read(entry))
                else:
                    self.scan_archive(nested, archive.read(entry), depth + 1)

    def scan_path(self, path):
        with open(path, "rb") as handle:
            head = handle.read(MAGIC_LENGTH)
        kind = classify(path, head)
        if kind is None:
            return
        with open(path, "rb") as handle:
            data = handle.read()
        if kind == "elf":
            self.check_library(path, data)
        else:
            self.scan_archive(path, data, 1)


def tracked_files():
    """Every tracked path, as an absolute name.

    Resolved from the repository root rather than the working directory: run
    from a subdirectory, `git ls-files` lists only what is under it, and the
    whole-tree mode would quietly inspect a fraction of the tree while still
    calling itself a whole-tree run.
    """
    root = subprocess.check_output(
        ["git", "rev-parse", "--show-toplevel"],
        universal_newlines=True).strip()
    output = subprocess.check_output(
        ["git", "-C", root, "ls-files", "-z"], universal_newlines=False)
    return [os.path.join(root, name.decode("utf-8"))
            for name in output.split(b"\0") if name]


def main(argv):
    verbose = "--verbose" in argv or "-v" in argv
    paths = [arg for arg in argv if not arg.startswith("-")]
    whole_tree = not paths
    if whole_tree:
        # Every tracked file. scan_path sniffs and ignores what is neither a
        # container nor an ELF, so nothing has to be guessed from a name.
        paths = tracked_files()

    scanner = Scanner(verbose)
    for path in paths:
        if not os.path.isfile(path):
            if whole_tree:
                # Tracked but deleted in the working tree. Nothing to read,
                # and nothing wrong with the tree as committed.
                continue
            print("Missing %s" % path, file=sys.stderr)
            return 2
        scanner.scan_path(path)

    if whole_tree and scanner.libraries_scanned < MIN_LIBRARIES_SCANNED:
        print("check-16k-page-alignment: found %d 64-bit native libraries, "
              "expected at least %d -- the scan looked at nothing, which is "
              "not the same as a clean tree."
              % (scanner.libraries_scanned, MIN_LIBRARIES_SCANNED),
              file=sys.stderr)
        return 2

    if scanner.failures:
        print("16 KB page alignment check failed:", file=sys.stderr)
        for failure in scanner.failures:
            print("  %s" % failure, file=sys.stderr)
        # Only offered when something was actually misaligned. A truncated or
        # unreadable artifact is a different problem, and "rebuild with a
        # newer NDK" would send the reader off in the wrong direction.
        if scanner.has_alignment_failure:
            print("", file=sys.stderr)
            print("Rebuild the misaligned libraries with NDK r28 or newer, "
                  "which emits 16 KB aligned libraries by default. On NDK "
                  "r26/r27 pass both -Wl,-z,max-page-size=16384 and "
                  "-Wl,-z,common-page-size=16384, and note that the NDK's own "
                  "prebuilt libc++_shared.so and libomp.so are 4 KB aligned "
                  "there and cannot be fixed by a linker flag.",
                  file=sys.stderr)
            print("See "
                  "https://developer.android.com/guide/practices/page-sizes",
                  file=sys.stderr)
        return 1

    print("check-16k-page-alignment: %d Android 64-bit native libraries "
          "aligned to 0x%x or more (%d 32-bit and %d non-Android libraries "
          "are not subject to the rule)"
          % (scanner.libraries_scanned, REQUIRED_ALIGNMENT,
             scanner.skipped_32bit, scanner.skipped_non_android))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
