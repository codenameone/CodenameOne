#!/usr/bin/env python3
"""Pins the classification boundaries of check-16k-page-alignment.py.

Every case here is one a review caught on PR #5688, and each was a variant of
the same mistake: something the gate could not read, or was not asked about,
came out looking identical to something it had read and found compliant. The
opposite failure showed up too -- a valid artifact rejected because the rule
was applied where it does not apply.

So the three verdicts are pinned deliberately and separately:

  enforced   a 64-bit library under Android packaging
  exempt     32-bit, or not Android packaging -- reported, never failed
  reported   unreadable, truncated, or not an ELF at all -- always a failure

ELF payloads are synthesised rather than copied from a build, so the test is
self-contained and does not need a toolchain or a checked-in fixture.
"""

import io
import os
import struct
import types
import unittest
import zipfile

GATE_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "check-16k-page-alignment.py")


def load_gate():
    """Load the gate from source, never from `__pycache__`.

    importlib would happily serve a stale `.pyc` -- `python3 -m py_compile`
    on the gate leaves one behind, and it really did shadow an edit here,
    reporting a behaviour the source no longer had. A pinning test that can
    pass against code that no longer exists is worse than no test, so the
    source is compiled every run.
    """
    with io.open(GATE_PATH, encoding="utf-8") as handle:
        source = handle.read()
    module = types.ModuleType("check_16k_page_alignment")
    module.__file__ = GATE_PATH
    exec(compile(source, GATE_PATH, "exec"), module.__dict__)
    return module


gate = load_gate()

ALIGNED_DEFAULT = 0x4000

ELF64_HEADER = 64
ELF64_PHENTSIZE = 56
ELF32_HEADER = 52
ELF32_PHENTSIZE = 32


def elf64(p_align):
    """A minimal little-endian 64-bit ELF with one PT_LOAD segment."""
    total = ELF64_HEADER + ELF64_PHENTSIZE
    header = struct.pack(
        "<4sBBBB8xHHIQQQIHHHHHH",
        b"\x7fELF", 2, 1, 1, 0,          # magic, ELFCLASS64, LSB, version
        3, 0xB7, 1,                      # ET_DYN, AArch64, version
        0, ELF64_HEADER, 0, 0,           # entry, phoff, shoff, flags
        ELF64_HEADER, ELF64_PHENTSIZE, 1, 0, 0, 0)
    phdr = struct.pack(
        "<IIQQQQQQ",
        1, 5,                            # PT_LOAD, R+X
        0, 0, 0,                         # offset, vaddr, paddr
        total, total, p_align)           # filesz, memsz, align
    return header + phdr


def elf32(p_align):
    """A minimal little-endian 32-bit ELF with one PT_LOAD segment."""
    total = ELF32_HEADER + ELF32_PHENTSIZE
    header = struct.pack(
        "<4sBBBB8xHHIIIIIHHHHHH",
        b"\x7fELF", 1, 1, 1, 0,          # magic, ELFCLASS32, LSB, version
        3, 0x28, 1,                      # ET_DYN, ARM, version
        0, ELF32_HEADER, 0, 0,           # entry, phoff, shoff, flags
        ELF32_HEADER, ELF32_PHENTSIZE, 1, 0, 0, 0)
    phdr = struct.pack(
        "<IIIIIIII",
        1, 0, 0, 0,                      # PT_LOAD, offset, vaddr, paddr
        total, total, 5, p_align)        # filesz, memsz, flags, align
    return header + phdr


def patched_elf64(**fields):
    """A 64-bit ELF with named header fields overwritten, to reach the
    malformed-ELF guards that a well-formed payload never exercises."""
    payload = bytearray(elf64(ALIGNED_DEFAULT))
    layout = {
        "ei_class": (4, "<B"),
        "e_phoff": (0x20, "<Q"),
        "e_phentsize": (0x36, "<H"),
        "e_phnum": (0x38, "<H"),
        "p_type": (ELF64_HEADER + 0x00, "<I"),
        "p_align": (ELF64_HEADER + 0x30, "<Q"),
    }
    for field, value in fields.items():
        offset, code = layout[field]
        struct.pack_into(code, payload, offset, value)
    return bytes(payload)


def nested_zips(depth, innermost):
    """`depth` archives, one inside the next, innermost holding a library."""
    payload = zipped({"jni/arm64-v8a/libfoo.so": innermost})
    for level in range(depth - 1):
        payload = zipped({"level%d.zip" % level: payload})
    return payload


def zipped(entries):
    """A ZIP archive built in memory from {name: bytes}."""
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        for name, payload in entries.items():
            archive.writestr(name, payload)
    return buffer.getvalue()


def scan(label, payload):
    """Run the scanner over one payload and return (failures, scanner)."""
    scanner = gate.Scanner(verbose=False)
    kind = gate.classify(label, payload[:gate.MAGIC_LENGTH])
    if kind == "elf":
        scanner.check_library(label, payload)
    elif kind == "zip":
        scanner.scan_archive(label, payload, 1)
    return scanner.failures, scanner


ALIGNED = 0x4000
MISALIGNED = 0x1000
ANDROID_64 = "app.aar!jni/arm64-v8a/libfoo.so"


class SharedLibraryNames(unittest.TestCase):
    """A `.so` name routes to the ELF reader, so its shape decides a lot."""

    def test_plain_and_numeric_soname_versions_are_libraries(self):
        for name in ["libfoo.so", "libfoo.so.1", "libfoo.so.1.2",
                     "libfoo.so.1.2.3", "jni/arm64-v8a/libfoo.so"]:
            self.assertTrue(gate.looks_like_shared_library(name), name)

    def test_metadata_beside_a_library_is_not_a_library(self):
        # A checksum or signature sidecar is a text file. Treating it as a
        # library failed the whole run with "not an ELF file".
        for name in ["libfoo.so.sha256", "libfoo.so.asc", "libfoo.so.md5",
                     "libfoo.so.sig", "libfoo.so.txt", "libfoo.so.debug",
                     "libfoo.so.", "notes.txt"]:
            self.assertFalse(gate.looks_like_shared_library(name), name)


class AndroidPackagingDetection(unittest.TestCase):
    def test_android_containers_and_abi_directories(self):
        for label in ["app.aar!jni/arm64-v8a/libfoo.so",
                      "app.apk!lib/x86_64/libfoo.so",
                      "app.aab!jni/arm64-v8a/libfoo.so",
                      "L.cn1lib!nativeand.zip!inner.aar!jni/arm64-v8a/libfoo.so",
                      "Ports/Android/jniLibs/arm64-v8a/libfoo.so"]:
            self.assertTrue(gate.is_android_payload(label), label)

    def test_desktop_payloads_are_not_android(self):
        # Cn1libMojo packages linux/javase resources into these.
        for label in ["L.cn1lib!nativelinux.zip!libfoo.so",
                      "L.cn1lib!nativese.zip!libfoo.so",
                      "maven/x/linux/src/main/resources/libfoo.so",
                      "libfoo.so"]:
            self.assertFalse(gate.is_android_payload(label), label)


class AlignmentIsEnforcedOnAndroid(unittest.TestCase):
    def test_misaligned_android_64_bit_library_fails(self):
        failures, _ = scan(ANDROID_64, elf64(MISALIGNED))
        self.assertEqual(1, len(failures), failures)
        self.assertIn("p_align 0x1000", failures[0])

    def test_aligned_android_64_bit_library_passes(self):
        failures, scanner = scan(ANDROID_64, elf64(ALIGNED))
        self.assertEqual([], failures)
        self.assertEqual(1, scanner.libraries_scanned)

    def test_misaligned_library_inside_a_nested_cn1lib_fails(self):
        # cn1lib -> nativeand.zip -> aar -> .so, the depth the tree really
        # uses, reached through a container the extension list did not know.
        inner = zipped({"jni/arm64-v8a/libfoo.so": elf64(MISALIGNED)})
        payload = zipped({"nativeand.zip": zipped({"inner.aar": inner})})
        failures, _ = scan("QRScanner.cn1lib", payload)
        self.assertEqual(1, len(failures), failures)
        self.assertIn("p_align 0x1000", failures[0])


class ExemptionsAreNotFailures(unittest.TestCase):
    def test_32_bit_library_is_exempt(self):
        failures, scanner = scan("app.aar!jni/armeabi-v7a/libfoo.so",
                                 elf32(MISALIGNED))
        self.assertEqual([], failures)
        self.assertEqual(1, scanner.skipped_32bit)
        self.assertEqual(0, scanner.libraries_scanned)

    def test_desktop_64_bit_library_is_exempt_at_4kb(self):
        # A desktop Linux .so is correct at 0x1000; failing it would block
        # every PR over a valid artifact.
        failures, scanner = scan("L.cn1lib!nativelinux.zip!libfoo.so",
                                 elf64(MISALIGNED))
        self.assertEqual([], failures)
        self.assertEqual(1, scanner.skipped_non_android)
        self.assertEqual(0, scanner.libraries_scanned)

    def test_same_bytes_enforced_or_exempt_by_packaging_alone(self):
        payload = elf64(MISALIGNED)
        android, _ = scan(ANDROID_64, payload)
        desktop, _ = scan("L.cn1lib!nativelinux.zip!libfoo.so", payload)
        self.assertEqual(1, len(android))
        self.assertEqual([], desktop)


class UnreadableIsAlwaysReported(unittest.TestCase):
    """The core rule: "could not look" must never read as "looked, clean"."""

    def test_so_that_is_not_an_elf_fails(self):
        for payload in [b"\x00" * 4096, b"this is not an elf"]:
            failures, _ = scan(ANDROID_64, payload)
            self.assertEqual(1, len(failures), failures)
            self.assertIn("not an ELF file", failures[0])

    def test_corrupt_library_is_not_mistaken_for_32_bit(self):
        # A truncated 64-bit ELF still begins with the ELF magic, so the
        # exemption for an ABI the rule does not cover was reachable by
        # corrupting a library.
        failures, scanner = scan(ANDROID_64, elf64(ALIGNED)[:40])
        self.assertEqual(1, len(failures), failures)
        self.assertIn("truncated", failures[0])
        self.assertEqual(0, scanner.skipped_32bit)

    def test_segment_running_past_end_of_file_fails(self):
        # Program headers sit near the front, so a library truncated mid-file
        # parses and reports correct alignment for contents that are absent.
        payload = bytearray(elf64(ALIGNED))
        struct.pack_into("<Q", payload, ELF64_HEADER + 0x20, 1 << 20)
        failures, _ = scan(ANDROID_64, bytes(payload))
        self.assertEqual(1, len(failures), failures)
        self.assertIn("past the", failures[0])

    def test_container_replaced_by_a_valid_elf_is_reported(self):
        # The mirror of the .so case, and the one that stayed open longest:
        # a valid ELF left in place of an .aar was read as a library and
        # counted compliant, and the same bytes named .cn1lib were waved
        # through as non-Android. Either way a destroyed container kept the
        # run green. Aligned on purpose -- the alignment is not the point.
        for name in ["libs/app.aar", "libs/L.cn1lib", "libs/x.apk",
                     "libs/x.aab", "libs/x.jar", "libs/x.zip"]:
            failures, scanner = scan(name, elf64(ALIGNED))
            self.assertEqual(1, len(failures), name)
            self.assertIn("cannot read archive", failures[0])
            self.assertEqual(0, scanner.libraries_scanned, name)
            self.assertEqual(0, scanner.skipped_non_android, name)

    def test_unreadable_archive_fails(self):
        failures, _ = scan("libs/corrupt.aar", b"\x00" * 4096)
        self.assertEqual(1, len(failures), failures)
        self.assertIn("cannot read archive", failures[0])

    def test_corrupt_library_on_a_desktop_path_still_fails(self):
        # Only the alignment rule is scoped to Android; integrity is not.
        failures, _ = scan("maven/x/linux/src/main/resources/libfoo.so",
                           b"\x00" * 4096)
        self.assertEqual(1, len(failures), failures)
        self.assertIn("not an ELF file", failures[0])


class MalformedElfHeadersAreReported(unittest.TestCase):
    """Guards a well-formed payload never reaches, each pinned so removing
    it cannot pass unnoticed. Every one of these means the same thing: the
    bytes claim to be a library and are not readable as one."""

    def assert_reported(self, payload, fragment):
        failures, scanner = scan(ANDROID_64, payload)
        self.assertEqual(1, len(failures), failures)
        self.assertIn(fragment, failures[0])
        self.assertEqual(0, scanner.libraries_scanned)

    def test_program_header_table_past_end_of_file(self):
        self.assert_reported(patched_elf64(e_phnum=4096),
                             "program header table runs past end of file")

    def test_program_header_entry_too_small(self):
        self.assert_reported(patched_elf64(e_phentsize=8),
                             "program header entry size 8 is too small")

    def test_no_program_headers(self):
        self.assert_reported(patched_elf64(e_phnum=0), "no program headers")
        self.assert_reported(patched_elf64(e_phoff=0), "no program headers")

    def test_unknown_elf_class(self):
        self.assert_reported(patched_elf64(ei_class=7), "unknown ELF class")

    def test_alignment_that_is_not_a_power_of_two(self):
        # 0x4001 clears a minimum-only comparison but is not a value ELF
        # permits, so accepting it would announce a library no loader would
        # honour as compliant.
        self.assert_reported(patched_elf64(p_align=0x4001),
                             "p_align 0x4001 is not a power of two")
        self.assert_reported(patched_elf64(p_align=0x6000),
                             "is not a power of two")

    def test_legal_alignments_above_the_minimum_are_accepted(self):
        # Bigger powers of two are valid and compliant; the rule is a floor.
        for value in [0x4000, 0x8000, 0x10000]:
            failures, scanner = scan(ANDROID_64, patched_elf64(p_align=value))
            self.assertEqual([], failures, hex(value))
            self.assertEqual(1, scanner.libraries_scanned, hex(value))

    def test_no_load_segments(self):
        # A single PT_NOTE (4) and nothing to measure alignment against.
        self.assert_reported(patched_elf64(p_type=4), "no PT_LOAD segments")


class ArchiveRecursionIsBounded(unittest.TestCase):
    def test_within_the_depth_limit_the_library_is_still_found(self):
        payload = nested_zips(gate.MAX_ARCHIVE_DEPTH, elf64(MISALIGNED))
        failures, _ = scan("app.aar", payload)
        self.assertEqual(1, len(failures), failures)
        self.assertIn("p_align 0x1000", failures[0])

    def test_beyond_the_depth_limit_is_reported_not_skipped(self):
        payload = nested_zips(gate.MAX_ARCHIVE_DEPTH + 2, elf64(MISALIGNED))
        failures, _ = scan("app.aar", payload)
        self.assertEqual(1, len(failures), failures)
        self.assertIn("nested more than", failures[0])


class ContentDecidesWhatIsScanned(unittest.TestCase):
    def test_elf_under_an_unexpected_name_is_still_checked(self):
        payload = zipped({"libs/weirdname.bin": elf64(MISALIGNED)})
        failures, _ = scan("app.aar", payload)
        self.assertEqual(1, len(failures), failures)
        self.assertIn("p_align 0x1000", failures[0])

    def test_container_under_an_unexpected_name_is_still_opened(self):
        inner = zipped({"jni/arm64-v8a/libfoo.so": elf64(MISALIGNED)})
        payload = zipped({"payload.unknown": inner})
        failures, _ = scan("app.aar", payload)
        self.assertEqual(1, len(failures), failures)
        self.assertIn("p_align 0x1000", failures[0])

    def test_ordinary_files_are_ignored(self):
        payload = zipped({"README.md": b"# notes\n",
                          "classes.dex": b"dex\n035\x00"})
        failures, scanner = scan("app.aar", payload)
        self.assertEqual([], failures)
        self.assertEqual(0, scanner.libraries_scanned)


if __name__ == "__main__":
    unittest.main(verbosity=2)
