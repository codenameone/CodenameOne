#!/usr/bin/env python3
"""Fail on any Apple symbol we reference that no public SDK header declares.

Why this exists
---------------
App Store Connect rejected customer uploads with

    Validation failed (409) The app references non-public symbols in
    Payload/<app>.app/<app>: _CCCryptorGCMAddAAD, _CCCryptorGCMAddIV, _CCCryptorGCMFinal

and every check in this tree was green. Nothing here could have failed:

* The symbols LINK. libcommonCrypto exports them and the SDK's own text stubs
  list them, so the compiler, the linker, the framework-link gate and every
  simulator and device build accept them. "Private" is not a property the
  toolchain enforces; it only means no public header declares the name.
* They were behind a feature gate. CN1Crypto.m declared them only under
  CN1_INCLUDE_CRYPTO_GCM, which the sample application never turns on, so no
  CI build ever compiled that code at all.
* The gate was turned on by accident in customer builds. The builder enabled
  CN1_INCLUDE_CRYPTO with a plain String.replace, which also matched the
  CN1_INCLUDE_CRYPTO_GCM line below it. Any application using any crypto API got
  the private GCM calls, whatever its hints said.

What it checks
--------------
One question, asked of every symbol our code imports from the SDK:

    is this name declared anywhere in the SDK's PUBLIC headers?

The SDK's `.tbd` stubs say which names the system libraries export, and the
headers say which of those Apple has published. An exported name that no public
header mentions is what App Store review calls a non-public symbol. The header
text is scanned with comments removed, so a name that appears only in a comment
does not count as declared.

It runs in two modes, and CI uses both, because they answer different halves:

* **Source mode** (`--project-dir`) compiles every native in
  `Ports/iOSPort/nativeSources` with EVERY feature gate turned on, harvested the
  same way `check-ios-sdk-deltas.py` harvests them. A private call behind a
  gate the sample app never enables is exactly what shipped, so a sweep of only
  what one sample builds would have missed it again.
* **Binary mode** (`--binary`) reads a linked `.app` -- the real Release device
  build -- so everything that actually reaches the link is covered too: the
  translated code, cn1libs, CocoaPods and the Swift parts. `nm -m` names the
  library each import binds to, so no index is needed to attribute it.

Both prove they can fail before trusting a clean result: a probe that imports
the three CommonCrypto GCM symbols must be reported, and public symbols beside
it must not be.

Scope: C and Objective-C symbols. Swift and C++ names are mangled and declared
in .swiftinterface files and C++ headers that a header scan cannot match, so they
are skipped rather than all reported; so are names the compiler and Apple's Swift
toolchain emit (COMPILER_EMITTED) and the Swift runtime Xcode embeds. Private
Objective-C SELECTORS are not checked: App Store Connect reports those as a
warning, and a selector list would be dominated by our own methods.
"""

import argparse
import importlib.util
import os
import plistlib
import re
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NATIVE_SOURCES = os.path.join(REPO_ROOT, "Ports", "iOSPort", "nativeSources")
TRANSLATOR_SOURCES = os.path.join(REPO_ROOT, "vm", "ByteCodeTranslator", "src")

# Optional engines the translator emits into a project only for applications that use them,
# which natives detect with __has_include. The sample application configures an encrypted
# database, so its project carries the bundled SQLite engine AND its cipher marker, and the
# natives compile their cipher branch -- where the engine DEFINES sqlite3_key inside the
# application. The branches every other customer compiles are never seen unless those files
# are hidden, and that is where the private import lived. So each configuration the
# translator can emit (ByteCodeTranslator.emitBundledSqlite) is reproduced by hiding the files
# it would not have written, and every native that names one of them is compiled again.
ENGINE_VARIANTS = [
    ("without-sqlite-cipher", ["cn1_sqlite3_cipher.h"]),
    ("without-bundled-sqlite", ["cn1_sqlite3.c", "cn1_sqlite3.h", "cn1_sqlite3_amalgamation.h",
                                "cn1_sqlite3_cipher.h"]),
]


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, os.path.join(REPO_ROOT, "scripts", filename))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


# The gate list, the configurations and the translated-header stubs are shared with the SDK
# delta check on purpose: two harvesters could disagree about what "every gate" means.
deltas = _load("cn1_ios_sdk_deltas", "check-ios-sdk-deltas.py")

# Names the COMPILER emits calls to, which no header declares because no source spells them.
# ARC, blocks, stack protection and Objective-C literals lower to these; Apple's own toolchain
# generates them in every application, so they are not what review means by non-public.
# Exact names only: a prefix such as "objc_" would also admit a private runtime function
# called by hand.
COMPILER_EMITTED = {
    "__objc_empty_cache",
    "___CFConstantStringClassReference",
    "___objc_personality_v0",
    "___stack_chk_fail",
    "___stack_chk_guard",
    "___chkstk_darwin",
    # Objective-C literals. clang lowers @[...], @{...}, @1, @1.5 and @YES to constant objects
    # of these classes, and the empty collections to the shared singletons.
    "_OBJC_CLASS_$_NSConstantArray",
    "_OBJC_CLASS_$_NSConstantDictionary",
    "_OBJC_CLASS_$_NSConstantDoubleNumber",
    "_OBJC_CLASS_$_NSConstantFloatNumber",
    "_OBJC_CLASS_$_NSConstantIntegerNumber",
    "___NSArray0__struct",
    "___NSDictionary0__struct",
    # The same singletons as the tvOS runtime exports them, without the __struct suffix.
    "___NSArray0__",
    "___NSDictionary0__",
    "___kCFBooleanFalse",
    "___kCFBooleanTrue",
    # _FORTIFY_SOURCE: memcpy, strcpy and friends with a known destination size compile to
    # their checked twins.
    "___memcpy_chk",
    "___memmove_chk",
    "___memset_chk",
    "___strcpy_chk",
    "___strncpy_chk",
    "___strcat_chk",
    "___strncat_chk",
    "___stpcpy_chk",
    "___stpncpy_chk",
    "___strlcpy_chk",
    "___strlcat_chk",
    "___sprintf_chk",
    "___snprintf_chk",
    "___vsprintf_chk",
    "___vsnprintf_chk",
    # Synthesized property accessors.
    "_objc_getProperty",
    "_objc_setProperty",
    "_objc_setProperty_atomic",
    "_objc_setProperty_atomic_copy",
    "_objc_setProperty_nonatomic",
    "_objc_setProperty_nonatomic_copy",
    "_objc_copyStruct",
    "_objc_copyCppObjectAtomic",
    "__NSConcreteGlobalBlock",
    "__NSConcreteStackBlock",
    "__Block_object_assign",
    "__Block_object_dispose",
    "_objc_alloc",
    "_objc_alloc_init",
    "_objc_autorelease",
    "_objc_autoreleasePoolPop",
    "_objc_autoreleasePoolPush",
    "_objc_autoreleaseReturnValue",
    "_objc_claimAutoreleasedReturnValue",
    "_objc_copyWeak",
    "_objc_destroyWeak",
    "_objc_initWeak",
    "_objc_loadWeakRetained",
    "_objc_moveWeak",
    "_objc_msgSendSuper2",
    "_objc_opt_class",
    "_objc_opt_isKindOfClass",
    "_objc_opt_new",
    "_objc_opt_respondsToSelector",
    "_objc_opt_self",
    "_objc_release",
    "_objc_release_x0",
    "_objc_release_x1",
    "_objc_release_x2",
    "_objc_release_x3",
    "_objc_release_x4",
    "_objc_release_x5",
    "_objc_release_x6",
    "_objc_release_x7",
    "_objc_release_x8",
    "_objc_release_x9",
    "_objc_release_x10",
    "_objc_release_x11",
    "_objc_release_x12",
    "_objc_release_x13",
    "_objc_release_x14",
    "_objc_release_x15",
    "_objc_release_x19",
    "_objc_release_x20",
    "_objc_release_x21",
    "_objc_release_x22",
    "_objc_release_x23",
    "_objc_release_x24",
    "_objc_release_x25",
    "_objc_release_x26",
    "_objc_release_x27",
    "_objc_release_x28",
    "_objc_retain",
    "_objc_retainAutorelease",
    "_objc_retainAutoreleaseReturnValue",
    "_objc_retainAutoreleasedReturnValue",
    "_objc_retainBlock",
    "_objc_retain_x0",
    "_objc_retain_x1",
    "_objc_retain_x2",
    "_objc_retain_x3",
    "_objc_retain_x4",
    "_objc_retain_x5",
    "_objc_retain_x6",
    "_objc_retain_x7",
    "_objc_retain_x8",
    "_objc_retain_x9",
    "_objc_retain_x10",
    "_objc_retain_x11",
    "_objc_retain_x12",
    "_objc_retain_x13",
    "_objc_retain_x14",
    "_objc_retain_x15",
    "_objc_retain_x19",
    "_objc_retain_x20",
    "_objc_retain_x21",
    "_objc_retain_x22",
    "_objc_retain_x23",
    "_objc_retain_x24",
    "_objc_retain_x25",
    "_objc_retain_x26",
    "_objc_retain_x27",
    "_objc_retain_x28",
    "_objc_storeStrong",
    "_objc_storeWeak",
    "_objc_unsafeClaimAutoreleasedReturnValue",
    "dyld_stub_binder",
    "_objc_allocWithZone",
    # @available / #available lower to a runtime OS version check.
    "__availability_version_check",
    # 128-bit integer arithmetic, from compiler-rt.
    "___divti3",
    "___modti3",
    "___udivti3",
    "___umodti3",
    # The C++ ABI: static destructors and exception unwinding.
    "___cxa_atexit",
    "___gxx_personality_v0",
    # The entry point Xcode's linker sets for every app extension.
    "_NSExtensionMain",
    # Apple's Swift toolchain. libswiftCompatibility56.a, which the toolchain links into Swift
    # targets that deploy below iOS 15, references the voucher and dispatch internals; Swift's
    # Dispatch types are the OS_dispatch_* classes. Neither is spelled in our sources.
    "_voucher_adopt",
    "_voucher_copy",
    "_dispatch_queue_set_width",
    "__objc_rootAutorelease",
    "_OBJC_CLASS_$_OS_dispatch_data",
    "_OBJC_CLASS_$_OS_dispatch_group",
    "_OBJC_CLASS_$_OS_dispatch_io",
    "_OBJC_CLASS_$_OS_dispatch_object",
    "_OBJC_CLASS_$_OS_dispatch_queue",
    "_OBJC_CLASS_$_OS_dispatch_queue_attr",
    "_OBJC_CLASS_$_OS_dispatch_semaphore",
    "_OBJC_CLASS_$_OS_dispatch_source",
    "_OBJC_CLASS_$_OS_dispatch_workloop",
    "_OBJC_CLASS_$_OS_object",
}

# Mangled names are C++ and Swift API. Headers spell `operator new` and Swift declares its API
# in .swiftinterface files, so a header scan cannot judge either, and every one would read as
# undeclared. They are out of this check's scope, which is C and Objective-C -- where our
# natives live and where the rejection that prompted it came from.
MANGLED_PREFIXES = ("__Z", "_$s", "_$S", "__swift_")
SWIFT_MANGLED_CLASS = "_Tt"

# Apple's Swift runtime, copied into Frameworks by Xcode for back deployment. Apple builds and
# signs these; the App Store accepts them as they are and nothing in them is ours to change.
APPLE_EMBEDDED = re.compile(r"^libswift[A-Za-z0-9_]*\.dylib$")

# The Swift runtime's C entry points (swift_retain, swift_allocObject, ...). swiftc emits calls
# to them and no header declares them. Credited only when the import really binds to a libswift
# library: a whole-library exemption would also hide an UNMANGLED private C symbol that a Swift
# library happens to re-export, and binary mode is the only coverage dependencies get.
SWIFT_RUNTIME_PREFIX = "_swift_"
SWIFT_RUNTIME_LIBRARY = re.compile(r"^libswift")

# The self-test. These must be reported: the exact three App Store Connect rejected.
PROBE_PRIVATE = ["_CCCryptorGCMAddIV", "_CCCryptorGCMAddAAD", "_CCCryptorGCMFinal"]
# And these must not: one public C function from the same library, one Objective-C class.
PROBE_PUBLIC = ["_CCCryptorCreateWithMode", "_OBJC_CLASS_$_NSString"]
PROBE_SOURCE = """
#import <Foundation/Foundation.h>
#import <CommonCrypto/CommonCryptor.h>
extern CCCryptorStatus CCCryptorGCMAddIV(CCCryptorRef, const void*, size_t);
extern CCCryptorStatus CCCryptorGCMAddAAD(CCCryptorRef, const void*, size_t);
extern CCCryptorStatus CCCryptorGCMFinal(CCCryptorRef, void*, size_t*);
int cn1_private_api_probe(void) {
    CCCryptorRef r = NULL;
    size_t n = 0;
    CCCryptorCreateWithMode(0, 0, 0, 0, NULL, NULL, 0, NULL, 0, 0, 0, &r);
    return (int) [[NSString string] length] + CCCryptorGCMAddIV(r, NULL, 0)
        + CCCryptorGCMAddAAD(r, NULL, 0) + CCCryptorGCMFinal(r, NULL, &n);
}
"""

# Floors below which the indexes are broken rather than small. The iOS 27 SDK carries about
# 750 stubs and several hundred thousand exported names, and about 7,000 headers.
MIN_EXPORTED = 50000
MIN_HEADER_NAMES = 50000

# The clang target OS for each SDK the source sweep can compile against. Set once in main().
#
# tvOS is swept against both its SDKs, with every gate on like iOS: a tvOS slice compiles the
# same sources with the switches the iOS app turned on, so a gate whose code tvOS cannot compile
# is a TV build that fails. The device SDK differs from the simulator's -- it has no
# LocalAuthentication, for one -- and only the simulator is ever built by a UI job.
SDK_TRIPLES = {
    "iphoneos": ("arm64-apple-ios", ""),
    "appletvos": ("arm64-apple-tvos", ""),
    "appletvsimulator": ("arm64-apple-tvos", "-simulator"),
}
TRIPLE_PREFIX = "arm64-apple-ios"
# TvNativeBuilder's default tvNative.minDeploymentTarget, before it raises to the SDK floor.
TVOS_DEFAULT_DEPLOYMENT_TARGET = "13.0"
TRIPLE_SUFFIX = ""

# Directories whose headers are not public API. No SDK in Xcode 26 or 27 ships either, but if one
# did, a private header would put its names into the "declared" index and a private import would
# read as public -- the exact failure this check exists to catch.
PRIVATE_HEADER_DIRS = {"PrivateHeaders", "PrivateFrameworks"}

IDENT = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")
COMMENTS = re.compile(r"/\*.*?\*/|//[^\n]*", re.S)
TBD_LIST = re.compile(r"\b(objc-classes|objc-eh-types|objc-ivars|symbols):\s*\[(.*?)\]", re.S)


def run(cmd, **kw):
    return subprocess.run(cmd, capture_output=True, text=True, **kw)


def fail(message):
    sys.stderr.write("check-ios-private-api: %s\n" % message)
    sys.exit(2)


def sdk_root(sdk, developer_dir):
    env = dict(os.environ)
    if developer_dir:
        env["DEVELOPER_DIR"] = developer_dir
    out = run(["xcrun", "--sdk", sdk, "--show-sdk-path"], env=env)
    if out.returncode != 0 or not out.stdout.strip():
        fail("cannot locate the %s SDK: %s" % (sdk, out.stderr.strip()))
    return os.path.realpath(out.stdout.strip())


def find_clang(developer_dir):
    env = dict(os.environ)
    if developer_dir:
        env["DEVELOPER_DIR"] = developer_dir
    out = run(["xcrun", "--find", "clang"], env=env)
    if out.returncode != 0 or not out.stdout.strip():
        fail("could not locate clang through xcrun")
    return out.stdout.strip()


def walk(root, suffixes, skip_dirs=()):
    for base, dirs, files in os.walk(root, followlinks=False):
        dirs[:] = [d for d in dirs if d not in skip_dirs]
        for name in files:
            if name.endswith(suffixes):
                yield os.path.join(base, name)


def exported_names(sdk):
    """Every name a library in the SDK exports, in linker spelling."""
    names = set()
    for tbd in walk(sdk, (".tbd",)):
        try:
            with open(tbd, "r", errors="replace") as handle:
                text = handle.read()
        except OSError:
            continue
        for kind, body in TBD_LIST.findall(text):
            for item in body.replace("\n", " ").split(","):
                item = item.strip().strip("'\"")
                if not item:
                    continue
                if kind == "objc-classes":
                    names.add("_OBJC_CLASS_$_" + item)
                    names.add("_OBJC_METACLASS_$_" + item)
                elif kind == "objc-eh-types":
                    names.add("_OBJC_EHTYPE_$_" + item)
                elif kind == "objc-ivars":
                    names.add("_OBJC_IVAR_$_" + item)
                else:
                    names.add(item)
    return names


def public_header_names(sdk):
    """Every identifier the SDK's PUBLIC headers spell outside a comment."""
    names = set()
    for header in walk(sdk, (".h",), skip_dirs=PRIVATE_HEADER_DIRS):
        try:
            with open(header, "r", errors="replace") as handle:
                text = handle.read()
        except OSError:
            continue
        names.update(IDENT.findall(COMMENTS.sub(" ", text)))
    return names


def source_name(symbol):
    """The identifier a header would have to spell for this linker symbol."""
    for prefix in ("_OBJC_CLASS_$_", "_OBJC_METACLASS_$_", "_OBJC_EHTYPE_$_"):
        if symbol.startswith(prefix):
            return symbol[len(prefix):]
    if symbol.startswith("_OBJC_IVAR_$_"):
        # _OBJC_IVAR_$_Class._ivar: accessing another class's ivar directly is exactly the
        # kind of reach into a framework review rejects, so demand the ivar itself is public.
        return symbol.rsplit(".", 1)[-1]
    name = symbol[1:] if symbol.startswith("_") else symbol
    # A symbol VARIANT -- realpath$DARWIN_EXTSN, fopen$UNIX2003, stat$INODE64 -- is the same
    # declared function selected by a header macro, and the header spells the plain name.
    return name.split("$", 1)[0]


def classify(symbols, exported, headers):
    """The subset of `symbols` that the SDK exports and no public header declares."""
    private = set()
    for symbol in symbols:
        if symbol in COMPILER_EMITTED or symbol not in exported:
            continue
        if symbol.startswith(MANGLED_PREFIXES):
            continue
        if source_name(symbol).startswith(SWIFT_MANGLED_CLASS):
            # A Swift class as the Objective-C runtime sees it (_TtCs12_SwiftObject): its
            # name is mangled, so no header can spell it either.
            continue
        if source_name(symbol) not in headers:
            private.add(symbol)
    return private


def undefined_in_objects(objects):
    undefined, defined = set(), set()
    for obj in objects:
        out = run(["nm", "-g", obj])
        for line in out.stdout.splitlines():
            parts = line.split()
            if len(parts) < 2:
                continue
            (undefined if parts[-2] == "U" else defined).add(parts[-1])
    return undefined - defined


NM_IMPORT = re.compile(r"\(undefined\)\s+(?:weak\s+)?external\s+(\S+)(?:\s+\(from\s+([^)]+)\))?")


def imports_of_binary(path):
    """(symbol, library) for every import of one Mach-O, per `nm -m`."""
    # Every slice: `-arch arm64` succeeds on a universal binary and silently skips an arm64e
    # (or any other) slice, where an architecture-conditional private import would hide.
    out = run(["nm", "-m", "-u", "-arch", "all", path])
    if out.returncode != 0:
        fail("nm could not read %s: %s" % (path, out.stderr.strip()))
    result = set()
    for line in out.stdout.splitlines():
        match = NM_IMPORT.search(line)
        if match:
            result.add((match.group(1), match.group(2) or ""))
    return result


# Mach-O magics, both byte orders, thin and universal. The universal magic is also a Java class
# file's, told apart by the word after it: an architecture count for Mach-O (a handful), the
# class-file version for Java (45 and up).
MACHO_THIN = {b"\xfe\xed\xfa\xce", b"\xce\xfa\xed\xfe", b"\xfe\xed\xfa\xcf", b"\xcf\xfa\xed\xfe"}
# Universal headers, in both byte orders: FAT_MAGIC / FAT_MAGIC_64 as Apple's tools write them,
# and FAT_CIGAM / FAT_CIGAM_64, the same headers stored little-endian, which is equally valid and
# which a dependency's own tooling can produce. The architecture count that follows is read in
# the byte order the magic announces.
MACHO_FAT = {
    b"\xca\xfe\xba\xbe": "big", b"\xca\xfe\xba\xbf": "big",
    b"\xbe\xba\xfe\xca": "little", b"\xbf\xba\xfe\xca": "little",
}


def is_macho(path):
    try:
        with open(path, "rb") as handle:
            head = handle.read(8)
    except OSError:
        return False
    if head[:4] in MACHO_THIN:
        return True
    order = MACHO_FAT.get(head[:4])
    return order is not None and len(head) == 8 and 0 < int.from_bytes(head[4:], order) < 30


def binaries_in(app):
    """Every Mach-O an .app ships, wherever it sits.

    Found by content rather than by bundle suffix or Info.plist: the sample nests code several
    levels deep (extensions, frameworks, a companion watch app with extensions of its own), and
    a dependency can put an extensionless executable inside a .bundle or anywhere else. App
    Store Connect scans every one of them, so a suffix allowlist is a way to miss one.
    """
    if os.path.isfile(app):
        return [app]
    found = []
    for base, _dirs, files in os.walk(app):
        for name in sorted(files):
            path = os.path.join(base, name)
            if os.path.islink(path) or APPLE_EMBEDDED.match(name):
                continue
            if is_macho(path):
                found.append(path)
    if not found:
        fail("%s contains no Mach-O at all -- not a built application" % app)
    return found


# LC_BUILD_VERSION platforms (mach-o/loader.h) and the SDK each one is built against. otool prints
# the number on some versions and the symbolic name on others (WatchNativeBuilder copes with the
# same), so both spellings are keys -- lowercased, as platform_of() compares them.
PLATFORM_SDKS = {
    "1": "macosx", "macos": "macosx",
    "2": "iphoneos", "ios": "iphoneos",
    "3": "appletvos", "tvos": "appletvos",
    "4": "watchos", "watchos": "watchos",
    "6": "macosx", "maccatalyst": "macosx",
    "7": "iphonesimulator", "iossimulator": "iphonesimulator",
    "8": "appletvsimulator", "tvossimulator": "appletvsimulator",
    "9": "watchsimulator", "watchossimulator": "watchsimulator",
    "11": "xros", "xros": "xros", "visionos": "xros",
    "12": "xrsimulator", "xrossimulator": "xrsimulator", "visionossimulator": "xrsimulator",
}
LEGACY_VERSION_MIN = {
    "LC_VERSION_MIN_IPHONEOS": "iphoneos", "LC_VERSION_MIN_WATCHOS": "watchos",
    "LC_VERSION_MIN_TVOS": "appletvos", "LC_VERSION_MIN_MACOSX": "macosx",
}
BUILD_VERSION_PLATFORM = re.compile(r"cmd LC_BUILD_VERSION\s.*?\n\s*platform\s+(\S+)", re.S)


def platform_of(otool_text):
    """The SDK name for `otool -l` output, or None when it names no platform this maps."""
    match = BUILD_VERSION_PLATFORM.search(otool_text)
    if match:
        return PLATFORM_SDKS.get(match.group(1).strip().lower())
    for command, sdk in LEGACY_VERSION_MIN.items():
        if command in otool_text:
            return sdk
    return None


def platform_sdk(path):
    """The SDK a binary was built for, read from its own load commands.

    A companion watch app and its extensions are watchOS binaries inside an iOS app. Judged
    against the iPhoneOS index, a watchOS-only private symbol is simply absent from the exports
    and passes, so each binary is checked against its own platform's SDK.
    """
    # No -arch: a watch binary is a thin arm64_32, where `-arch arm64` prints nothing and still
    # exits 0. Every slice of a universal binary names the same platform.
    text = run(["otool", "-l", path]).stdout
    sdk = platform_of(text)
    if sdk:
        return sdk
    match = BUILD_VERSION_PLATFORM.search(text)
    if match:
        fail("%s declares platform %s, which this check does not map to an SDK"
             % (path, match.group(1)))
    fail("%s carries no platform load command; cannot tell which SDK to judge it against" % path)


def self_test(clang, sdk, triple, exported, headers, work_dir):
    missing = [s for s in PROBE_PRIVATE + PROBE_PUBLIC if s not in exported]
    if missing:
        fail("self-test: the SDK index lacks %s -- the .tbd format changed and every symbol "
             "would now read as not exported, which reports success" % ", ".join(missing))
    probe_m = os.path.join(work_dir, "probe.m")
    with open(probe_m, "w") as handle:
        handle.write(PROBE_SOURCE)
    probe_o = os.path.join(work_dir, "probe.o")
    res = run([clang, "-c", "-target", triple, "-isysroot", sdk, "-w", probe_m, "-o", probe_o])
    if res.returncode != 0:
        fail("self-test probe did not compile:\n%s" % res.stderr)
    found = classify(undefined_in_objects([probe_o]), exported, headers)
    if found != set(PROBE_PRIVATE):
        fail("self-test: the probe should report exactly %s, got %s"
             % (", ".join(PROBE_PRIVATE), ", ".join(sorted(found)) or "nothing"))
    return probe_o


def self_test_binary(clang, sdk, triple, exported, headers, probe_o, work_dir):
    """The same probe, LINKED, read back through the binary path.

    Binary mode reads `nm -m` text, which the object self-test above never exercises. If its
    format moved, every import would stop parsing and a real app would read as clean.
    """
    dylib = os.path.join(work_dir, "probe.dylib")
    res = run([clang, "-dynamiclib", "-target", triple, "-isysroot", sdk, probe_o,
               "-framework", "Foundation", "-o", dylib])
    if res.returncode != 0:
        fail("self-test probe did not link:\n%s" % res.stderr)
    imports = imports_of_binary(dylib)
    found = classify({s for s, _lib in imports}, exported, headers)
    if found != set(PROBE_PRIVATE):
        fail("self-test: the linked probe should report exactly %s, got %s -- the `nm -m` "
             "output format changed" % (", ".join(PROBE_PRIVATE), ", ".join(sorted(found)) or "nothing"))


def compile_port(clang, sdk, project_dir, target, work_dir, jobs, verbose):
    """Every port native, every gate on, every configuration -> object files."""
    gates = deltas.harvest_gates()
    prefix_header = None
    for name in os.listdir(project_dir):
        if name.endswith("-Prefix.pch"):
            prefix_header = os.path.join(project_dir, name)
    if not prefix_header:
        fail("no *-Prefix.pch in %s" % project_dir)
    sources = sorted(n for n in os.listdir(NATIVE_SOURCES) if n.endswith((".m", ".c")))
    arc = set()
    for name in sources:
        with open(os.path.join(NATIVE_SOURCES, name), "r", errors="replace") as handle:
            if "requires ARC" in handle.read():
                arc.add(name)
    stub_dir = os.path.join(work_dir, "stubs")
    os.makedirs(stub_dir)
    all_defines = deltas.defines_for(gates, [])
    stubs = deltas.synthesize_generated_stubs(clang, sdk, project_dir, prefix_header, sources,
                                              all_defines, arc, target, stub_dir, jobs)
    print("gates turned on   : %d harvested from Ports/iOSPort/nativeSources" % len(gates))
    print("natives           : %d (%d built with ARC), %d generated-header stubs"
          % (len(sources), len(arc), len(stubs)))
    cache = os.path.join(work_dir, "modules")

    def compile_one(job):
        config, extra, name, tree = job
        obj = os.path.join(work_dir, "obj", config, name + ".o")
        cmd = [clang, "-c", "-arch", "arm64", "-target", TRIPLE_PREFIX + target + TRIPLE_SUFFIX,
               "-isysroot", sdk, "-fmodules", "-fmodules-cache-path=" + cache,
               "-fobjc-arc" if name in arc else "-fno-objc-arc", "-w",
               "-Wno-error=implicit-function-declaration", "-Wno-error=int-conversion",
               "-Wno-error=incompatible-pointer-types",
               "-I", NATIVE_SOURCES, "-I", tree, "-I", stub_dir,
               "-include", os.path.join(tree, os.path.basename(prefix_header))]
        for define in deltas.defines_for(gates, extra):
            cmd += ["-D", define]
        cmd += [os.path.join(NATIVE_SOURCES, name), "-o", obj]
        res = run(cmd)
        return job, obj, res

    jobs_list = []
    for config, extra, _witness in deltas.CONFIGURATIONS:
        jobs_list += [(config, extra, name, project_dir) for name in sources]

    for variant, hide in ENGINE_VARIANTS:
        present = [f for f in hide if os.path.isfile(os.path.join(project_dir, f))]
        if not present:
            continue
        shadow = shadow_without(project_dir, present, os.path.join(work_dir, variant))
        mentions = []
        for name in sources:
            with open(os.path.join(NATIVE_SOURCES, name), "r", errors="replace") as handle:
                text = handle.read()
            if any(f in text for f in present):
                mentions.append(name)
        jobs_list += [(variant, [], name, shadow) for name in mentions]
        print("engine variant    : %s -- %d native(s) recompiled with %s hidden"
              % (variant, len(mentions), ", ".join(present)))
    for config in set(j[0] for j in jobs_list):
        os.makedirs(os.path.join(work_dir, "obj", config))
    objects, failures = [], []
    with ThreadPoolExecutor(max_workers=jobs) as pool:
        for job, obj, res in pool.map(compile_one, jobs_list):
            if res.returncode == 0:
                objects.append((job[2], obj, job[3]))
            else:
                failures.append((job, res.stderr.strip().splitlines()[:4]))
    if failures:
        # A native that does not compile is one whose imports were never read. Reporting
        # success over a partial set is the failure this check exists to prevent.
        for (config, _extra, name, _tree), lines in failures:
            sys.stderr.write("  %s [%s]\n" % (name, config))
            for line in lines:
                sys.stderr.write("      %s\n" % line)
        fail("%d native compilations failed, so their imports were never checked" % len(failures))
    if len(objects) < deltas.MIN_CLEAN_FILES:
        fail("only %d natives compiled -- the harness is broken, not the port" % len(objects))
    print("objects compiled  : %d across %d configuration(s)"
          % (len(objects), len(set(j[0] for j in jobs_list))))
    report_unreachable_branches(clang, sdk, project_dir, target, sources, stub_dir, work_dir)
    return objects


HAS_INCLUDE_ANGLE = re.compile(r"__has_include\(\s*<([^>]+)>\s*\)")


def report_unreachable_branches(clang, sdk, project_dir, target, sources, stub_dir, work_dir):
    """Name the native branches no sweep here compiles, so the gap is visible, not silent.

    Turning every //#define gate on does not reach code behind __has_include(<Pod/Header.h>):
    MLKit and LiteRT headers exist only in a CocoaPods install the builder adds for apps that
    ask for them (and a few SDK headers only in a newer SDK), and without the header the branch
    is preprocessed away -- and it could not
    compile anyway, since it uses the pod's types. A private Apple call added there is only
    caught by binary mode on an application that links the pod, which the CI sample does not.
    So these are listed on every run rather than passed over. Whether a header resolves is
    asked of clang itself, with the include paths the sweep uses.
    """
    wanted = {}
    for name in sources:
        with open(os.path.join(NATIVE_SOURCES, name), "r", errors="replace") as handle:
            for header in HAS_INCLUDE_ANGLE.findall(handle.read()):
                wanted.setdefault(header, set()).add(name)
    if not wanted:
        return
    probe = os.path.join(work_dir, "has_include_probe.c")
    with open(probe, "w") as handle:
        for i, header in enumerate(sorted(wanted)):
            handle.write("#if __has_include(<%s>)\ncn1_found_%d\n#endif\n" % (header, i))
    out = run([clang, "-E", "-P", "-target", TRIPLE_PREFIX + target + TRIPLE_SUFFIX, "-isysroot", sdk,
               "-I", NATIVE_SOURCES, "-I", project_dir, "-I", stub_dir, probe]).stdout
    missing = [h for i, h in enumerate(sorted(wanted)) if "cn1_found_%d" % i not in out]
    if not missing:
        return
    files = sorted(set(f for h in missing for f in wanted[h]))
    print("not compiled here : %d __has_include(<...>) branch(es) whose header neither this SDK "
          "nor the project supplies (a pod, or a newer SDK), in %s" % (len(missing), ", ".join(files)))
    for header in missing:
        print("                    <%s>" % header)


def shadow_without(project_dir, hidden, shadow):
    """A symlink mirror of the generated sources with the given files left out."""
    os.makedirs(shadow)
    for name in os.listdir(project_dir):
        if name not in hidden:
            os.symlink(os.path.join(project_dir, name), os.path.join(shadow, name))
    return shadow


def project_definitions(clang, sdk, project_dir, target, work_dir, jobs):
    """Symbols the application defines itself, from the translator's runtime sources.

    A port native may call a name the SDK also exports that the project supplies -- the
    bundled SQLite engine defines sqlite3_key -- and that binds inside the application, so
    it is not an import. Only the translator's own runtime files are compiled for this, not
    the thousands of translated classes, which define nothing the SDK exports.
    """
    names = sorted(n for n in os.listdir(TRANSLATOR_SOURCES)
                   if n.endswith((".c", ".m")) and os.path.isfile(os.path.join(project_dir, n)))
    prefix = [os.path.join(project_dir, n) for n in os.listdir(project_dir)
              if n.endswith("-Prefix.pch")]
    out_dir = tempfile.mkdtemp(prefix="runtime-", dir=work_dir)

    def compile_one(name):
        obj = os.path.join(out_dir, name + ".o")
        cmd = [clang, "-c", "-arch", "arm64", "-target", TRIPLE_PREFIX + target + TRIPLE_SUFFIX,
               "-isysroot", sdk, "-fno-objc-arc", "-w", "-I", project_dir,
               "-Wno-error=implicit-function-declaration", "-Wno-error=int-conversion",
               "-Wno-error=incompatible-pointer-types"]
        if prefix and name.endswith(".m"):
            cmd += ["-include", prefix[0]]
        cmd += [os.path.join(project_dir, name), "-o", obj]
        return name, obj, run(cmd)

    defined = set()
    with ThreadPoolExecutor(max_workers=jobs) as pool:
        for name, obj, res in pool.map(compile_one, names):
            if res.returncode != 0:
                # Treated as defining nothing, which can only ADD findings, never hide one.
                print("note              : translator runtime %s did not compile here; "
                      "its definitions are not credited" % name)
                continue
            out = run(["nm", "-g", "-U", obj])
            for line in out.stdout.splitlines():
                parts = line.split()
                if len(parts) >= 3:
                    defined.add(parts[-1])
    print("project runtime   : %d translator sources, %d symbols defined in the application%s"
          % (len(names), len(defined),
             " (%s)" % os.path.basename(project_dir) if project_dir.startswith(work_dir) else ""))
    return defined


def sdk_index(name, developer_dir, root=None):
    """(exported names, public header identifiers) for one SDK, with the size floors applied."""
    root = root or sdk_root(name, developer_dir)
    exported = exported_names(root)
    headers = public_header_names(root)
    print("sdk %-14s: %d exports, %d header identifiers (%s)"
          % (name, len(exported), len(headers), root))
    if len(exported) < MIN_EXPORTED or len(headers) < MIN_HEADER_NAMES:
        fail("the %s indexes are implausibly small (%d exports, %d header names); the SDK "
             "layout changed and every symbol would read as fine"
             % (name, len(exported), len(headers)))
    return exported, headers


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--project-dir",
                    help="a generated *-ios-source/*-src directory; compiles the port's natives "
                         "with every feature gate on")
    ap.add_argument("--binary", action="append", default=[],
                    help="a built .app (or Mach-O) to check; repeatable")
    ap.add_argument("--sdk", default="iphoneos")
    ap.add_argument("--developer-dir", default=os.environ.get("DEVELOPER_DIR"),
                    help="the Xcode Contents/Developer the project was built with")
    ap.add_argument("--jobs", type=int, default=os.cpu_count() or 4)
    ap.add_argument("--verbose", action="store_true")
    args = ap.parse_args()
    if not args.project_dir and not args.binary:
        ap.error("nothing to check: pass --project-dir, --binary or both")
    if args.developer_dir:
        os.environ["DEVELOPER_DIR"] = args.developer_dir

    global TRIPLE_PREFIX, TRIPLE_SUFFIX
    if args.sdk not in SDK_TRIPLES:
        ap.error("--sdk must be one of %s" % ", ".join(sorted(SDK_TRIPLES)))
    TRIPLE_PREFIX, TRIPLE_SUFFIX = SDK_TRIPLES[args.sdk]
    sdk = sdk_root(args.sdk, args.developer_dir)
    clang = find_clang(args.developer_dir)
    # Start where the builders start -- IPhoneBuilder's 14.0 for iOS, TvNativeBuilder's 13.0 for
    # tvOS -- and raise to the SDK's own floor for THIS platform, as both builders do.
    target = TVOS_DEFAULT_DEPLOYMENT_TARGET if TRIPLE_PREFIX == "arm64-apple-tvos" \
        else deltas.CN1_DEFAULT_DEPLOYMENT_TARGET
    floor = deltas.sdk_minimum_deployment_target(sdk, args.sdk)
    if floor and deltas.sdk_version_macro(floor) > deltas.sdk_version_macro(target):
        target = floor
    triple = TRIPLE_PREFIX + target + TRIPLE_SUFFIX
    print("sdk               : %s" % sdk)
    print("clang             : %s" % clang)
    print("deployment target : %s" % target)

    exported, headers = sdk_index(args.sdk, args.developer_dir, sdk)
    indexes = {args.sdk: (exported, headers)}

    work_dir = tempfile.mkdtemp(prefix="cn1-private-api-")
    # The stub synthesis borrowed from check-ios-sdk-deltas.py compiles through its module cache;
    # keep that inside this run's own directory too.
    deltas.MODULE_CACHE = os.path.join(work_dir, "modules")
    findings = {}
    try:
        probe_o = self_test(clang, sdk, triple, exported, headers, work_dir)
        if args.binary:
            self_test_binary(clang, sdk, triple, exported, headers, probe_o, work_dir)
        print("self-test         : the CommonCrypto GCM probe is reported, public symbols are not")

        if args.project_dir:
            if not os.path.isdir(args.project_dir):
                fail("--project-dir is not a directory: %s" % args.project_dir)
            # Absolute, because the shadow tree below links back into it.
            args.project_dir = os.path.abspath(args.project_dir)
            objects = compile_port(clang, sdk, args.project_dir, target, work_dir, args.jobs,
                                   args.verbose)
            # Per source tree: with an optional engine hidden, the names it would have
            # defined are imports again, which is the whole point of hiding it.
            local = {}
            for tree in sorted(set(o[2] for o in objects)):
                local[tree] = project_definitions(clang, sdk, tree, target, work_dir, args.jobs)
            for name, obj, tree in objects:
                for symbol in classify(undefined_in_objects([obj]) - local[tree], exported, headers):
                    findings.setdefault(symbol, set()).add("Ports/iOSPort/nativeSources/" + name)

        for app in args.binary:
            if not os.path.exists(app):
                fail("--binary does not exist: %s" % app)
            for binary in binaries_in(app):
                platform = platform_sdk(binary)
                if platform not in indexes:
                    indexes[platform] = sdk_index(platform, args.developer_dir)
                bin_exported, bin_headers = indexes[platform]
                imports = imports_of_binary(binary)
                if not imports:
                    # Every executable imports at least dyld's and libSystem's entry points.
                    fail("read no imports from %s -- nm output did not parse" % binary)
                symbols = {sym for sym, lib in imports
                           if not (sym.startswith(SWIFT_RUNTIME_PREFIX)
                                   and SWIFT_RUNTIME_LIBRARY.match(lib))}
                label = os.path.relpath(binary, os.path.dirname(os.path.abspath(app)))
                print("binary            : %s (%s, %d imports)" % (label, platform, len(imports)))
                libraries = dict(imports)
                for symbol in classify(symbols, bin_exported, bin_headers):
                    findings.setdefault(symbol, set()).add(
                        "%s (from %s)" % (label, libraries.get(symbol) or "?"))
    finally:
        shutil.rmtree(work_dir, ignore_errors=True)

    if not findings:
        print("\nOK: every SDK symbol referenced is declared in a public SDK header.")
        return 0
    print("\nFAIL: references to SDK symbols that no public header declares.")
    print("App Store Connect rejects an upload that imports any of these (\"Validation")
    print("failed (409) The app references non-public symbols\"). They compile and link,")
    print("which is why nothing else in the build notices.\n")
    for symbol in sorted(findings):
        print("  %s" % symbol)
        for where in sorted(findings[symbol]):
            print("      %s" % where)
    print("\nReplace the call with public API. If the symbol is emitted by the compiler")
    print("rather than written in source, add it to COMPILER_EMITTED with the reason.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
