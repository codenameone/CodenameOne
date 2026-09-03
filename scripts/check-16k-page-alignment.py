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

Scope is 64-bit ELF only (`arm64-v8a`, `x86_64`). 16 KB pages are a 64-bit
feature; `armeabi-v7a` and `x86` never run on such a device, so holding their
slices to the same rule would fail builds for no benefit. Both are still
reported under --verbose so a mixed AAR is legible.

Usage:
  scripts/check-16k-page-alignment.py             # every tracked artifact
  scripts/check-16k-page-alignment.py PATH ...    # only these files
  scripts/check-16k-page-alignment.py --verbose   # list everything scanned
"""

import io
import os
import struct
import subprocess
import sys
import zipfile

# Google's required minimum `p_align` for a PT_LOAD segment. A library built
# by NDK r28+ gets this by default; r26/r27 need both
# -Wl,-z,max-page-size=16384 and -Wl,-z,common-page-size=16384.
REQUIRED_ALIGNMENT = 0x4000

# Containers that can carry a JNI payload. `.aar` is the one this repository
# actually checks in, but an `.apk`/`.aab`/`.jar` reaching the tree would
# smuggle the same problem past a gate that only knew about AARs.
ARCHIVE_EXTENSIONS = frozenset([".aar", ".apk", ".aab", ".jar", ".zip"])

# An archive inside an archive is legitimate (an AAR embeds `classes.jar`),
# so recurse -- but bound it, because a malformed or hostile ZIP should not
# be able to spin this gate forever.
MAX_ARCHIVE_DEPTH = 3

# A whole-tree run that inspected nothing must not report success: a wrong
# working directory, or a `git ls-files` that came back empty, would
# otherwise read exactly like a clean tree. This repository ships one native
# artifact (the cn1-ai-whisper AAR) holding six 64-bit libraries. If that
# artifact is ever legitimately removed, lower this floor in the same commit
# rather than deleting the check.
MIN_LIBRARIES_SCANNED = 1

ELF_MAGIC = b"\x7fELF"
ELFCLASS64 = 2
ELFDATA2LSB = 1
ELFDATA2MSB = 2
PT_LOAD = 1


class MalformedElf(Exception):
    """The bytes start with the ELF magic but cannot be read as an ELF."""


def load_segment_alignments(data):
    """Return the `p_align` of every PT_LOAD segment of a 64-bit ELF.

    Returns None when the bytes are not a 64-bit ELF -- a 32-bit library, or
    something that merely happens to be named `.so`. Raises MalformedElf when
    it claims to be one and then does not parse, because silently skipping
    that would let a truncated artifact through.
    """
    if len(data) < 64 or data[:4] != ELF_MAGIC:
        return None
    if data[4] != ELFCLASS64:
        return None

    endian = data[5]
    if endian == ELFDATA2LSB:
        prefix = "<"
    elif endian == ELFDATA2MSB:
        prefix = ">"
    else:
        raise MalformedElf("unknown ELF data encoding 0x%02x" % endian)

    # 64-bit ELF header: e_phoff at 0x20 (8 bytes), e_phentsize at 0x36 and
    # e_phnum at 0x38 (2 bytes each).
    e_phoff = struct.unpack_from(prefix + "Q", data, 0x20)[0]
    e_phentsize = struct.unpack_from(prefix + "H", data, 0x36)[0]
    e_phnum = struct.unpack_from(prefix + "H", data, 0x38)[0]

    if e_phentsize < 56:
        raise MalformedElf("program header entry size %d is too small" % e_phentsize)
    if e_phoff == 0 or e_phnum == 0:
        raise MalformedElf("no program headers")
    if e_phoff + e_phnum * e_phentsize > len(data):
        raise MalformedElf("program header table runs past end of file")

    alignments = []
    for index in range(e_phnum):
        offset = e_phoff + index * e_phentsize
        # 64-bit program header: p_type at 0x00, p_align at 0x30.
        p_type = struct.unpack_from(prefix + "I", data, offset)[0]
        if p_type != PT_LOAD:
            continue
        alignments.append(struct.unpack_from(prefix + "Q", data, offset + 0x30)[0])
    if not alignments:
        raise MalformedElf("no PT_LOAD segments")
    return alignments


class Scanner(object):
    def __init__(self, verbose):
        self.verbose = verbose
        self.failures = []
        self.libraries_scanned = 0
        self.skipped_32bit = 0

    def check_library(self, label, data):
        try:
            alignments = load_segment_alignments(data)
        except MalformedElf as error:
            self.failures.append("%s: %s" % (label, error))
            return
        if alignments is None:
            if data[:4] == ELF_MAGIC:
                self.skipped_32bit += 1
                if self.verbose:
                    print("  32-bit (not subject to 16 KB pages): %s" % label)
            return

        self.libraries_scanned += 1
        worst = min(alignments)
        if worst < REQUIRED_ALIGNMENT:
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
                name = entry.filename
                extension = os.path.splitext(name)[1].lower()
                if extension == ".so":
                    self.check_library("%s!%s" % (label, name),
                                       archive.read(entry))
                elif extension in ARCHIVE_EXTENSIONS:
                    self.scan_archive("%s!%s" % (label, name),
                                      archive.read(entry), depth + 1)

    def scan_path(self, path):
        extension = os.path.splitext(path)[1].lower()
        if extension == ".so":
            with open(path, "rb") as handle:
                self.check_library(path, handle.read())
        elif extension in ARCHIVE_EXTENSIONS:
            with open(path, "rb") as handle:
                self.scan_archive(path, handle.read(), 1)


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
        paths = [name for name in tracked_files()
                 if os.path.splitext(name)[1].lower() in
                 ARCHIVE_EXTENSIONS | frozenset([".so"])]

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
        print("", file=sys.stderr)
        print("Rebuild them with NDK r28 or newer, which emits 16 KB aligned "
              "libraries by default. On NDK r26/r27 pass both "
              "-Wl,-z,max-page-size=16384 and -Wl,-z,common-page-size=16384, "
              "and note that the NDK's own prebuilt libc++_shared.so and "
              "libomp.so are 4 KB aligned there and cannot be fixed by a "
              "linker flag.", file=sys.stderr)
        print("See https://developer.android.com/guide/practices/page-sizes",
              file=sys.stderr)
        return 1

    print("check-16k-page-alignment: %d 64-bit native libraries aligned to "
          "0x%x or more (%d 32-bit libraries not subject to the rule)"
          % (scanner.libraries_scanned, REQUIRED_ALIGNMENT,
             scanner.skipped_32bit))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
