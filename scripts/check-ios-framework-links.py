#!/usr/bin/env python3
"""Prove the iOS port's native sources link against the frameworks we declare.

Codename One issue: `_OBJC_CLASS_$_UTType` was undefined in every customer
archive while CI stayed green for days. The port referenced the UTType class,
`ByteCodeTranslator` linked `UniformTypeIdentifiers.framework` only on the macOS
branch, and nothing noticed -- because the sample application CI builds happens
to contain Swift in its app target, and a single `import SwiftUI` makes swiftc
stamp an autolink hint for that framework into its object file. The linker
honours the hint and quietly supplies what the project never declared.

That masking is the whole problem, so this check never links a Swift object, a
CocoaPods library or an app extension. It asks one question about the port
alone:

    every symbol our native sources reference, that an SDK framework defines --
    is it defined by a framework THIS PROJECT DECLARES?

A symbol the SDK can supply but the project does not ask for is a link failure
in any application whose own sources do not happen to drag the framework in.

The port's natives include translated headers (`java_lang_String.h` and
friends), which exist only inside a generated project -- so this runs against
the output of `cn1:build`/`build-ios-app.sh` rather than against the raw tree.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile

# A symbol index built from the SDK's text-based stubs is only as good as the
# format it parses, and a format change would empty it and report success. The
# floor and the probes below turn that into a failure instead. Both are far
# below what any real SDK carries: iPhoneOS26.2 indexes over 400,000 symbols.
MIN_INDEXED_SYMBOLS = 50000
SELF_TEST_SYMBOLS = {
    "_OBJC_CLASS_$_NSString": "Foundation",
    "_OBJC_CLASS_$_UIView": "UIKit",
}


def run(cmd, **kw):
    return subprocess.run(cmd, capture_output=True, text=True, **kw)


def fail(message):
    sys.stderr.write("check-ios-framework-links: %s\n" % message)
    sys.exit(2)


def sdk_path(sdk):
    out = run(["xcrun", "--sdk", sdk, "--show-sdk-path"])
    if out.returncode != 0:
        fail("cannot locate the %s SDK: %s" % (sdk, out.stderr.strip()))
    return out.stdout.strip()


def load_pbxproj(project_dir):
    """Return the parsed project, and the directory holding the generated sources."""
    # Pick the project whose generated sources are beside it. A CocoaPods build
    # puts a second .xcodeproj in the tree, and taking whichever sorted first is
    # how this would end up analysing Pods instead of the application.
    candidates = [e for e in sorted(os.listdir(project_dir)) if e.endswith(".xcodeproj")]
    matched = [e for e in candidates
               if os.path.isdir(os.path.join(project_dir, e[: -len(".xcodeproj")] + "-src"))]
    if not matched:
        fail("no .xcodeproj with generated sources beside it in %s -- point this at a "
             "generated ios-source project (found: %s)"
             % (project_dir, ", ".join(candidates) or "nothing"))
    if len(matched) > 1:
        fail("ambiguous: %s each have generated sources in %s" % (", ".join(matched), project_dir))
    xcodeproj = os.path.join(project_dir, matched[0])
    pbx = os.path.join(xcodeproj, "project.pbxproj")
    out = run(["plutil", "-convert", "json", "-o", "-", pbx])
    if out.returncode != 0:
        fail("cannot read %s: %s" % (pbx, out.stderr.strip()))
    app = os.path.basename(xcodeproj)[: -len(".xcodeproj")]
    src = os.path.join(project_dir, app + "-src")
    return json.loads(out.stdout), app, src


def app_target(objects, app):
    """The application target, not an extension: extensions are what carry the Swift."""
    for value in objects.values():
        if value.get("isa") != "PBXNativeTarget":
            continue
        if value.get("productType") != "com.apple.product-type.application":
            continue
        if value.get("name") == app:
            return value
    fail("no application target named %s in the project" % app)


def declared_frameworks(objects, target):
    """Framework names in the target's own link phase -- what the app really asks for."""
    names = set()
    for phase_id in target.get("buildPhases", []):
        phase = objects.get(phase_id, {})
        if phase.get("isa") != "PBXFrameworksBuildPhase":
            continue
        for build_file_id in phase.get("files", []):
            build_file = objects.get(build_file_id, {})
            ref = objects.get(build_file.get("fileRef", ""), {})
            name = ref.get("name") or ref.get("path") or ""
            name = os.path.basename(name)
            if name.endswith(".framework"):
                names.add(name[: -len(".framework")])
    return names


def per_file_flags(objects, target):
    """Per-file COMPILER_FLAGS from the target's own sources phase.

    Three natives open with `#error ... requires ARC`, and the project compiles
    them with a per-file `-fobjc-arc` because the port as a whole is MRR. Reading
    the flags out of the project rather than guessing keeps this check honest
    about what it compiled.
    """
    flags = {}
    for phase_id in target.get("buildPhases", []):
        phase = objects.get(phase_id, {})
        if phase.get("isa") != "PBXSourcesBuildPhase":
            continue
        for build_file_id in phase.get("files", []):
            build_file = objects.get(build_file_id, {})
            extra = (build_file.get("settings") or {}).get("COMPILER_FLAGS")
            if not extra:
                continue
            ref = objects.get(build_file.get("fileRef", ""), {})
            name = os.path.basename(ref.get("name") or ref.get("path") or "")
            if name:
                flags[name] = str(extra).split()
    return flags


def build_settings(objects, target, configuration):
    """Target settings layered over project settings, the way Xcode resolves them."""
    def settings_for(list_id):
        config_list = objects.get(list_id, {})
        chosen = None
        for config_id in config_list.get("buildConfigurations", []):
            config = objects.get(config_id, {})
            if config.get("name") == configuration:
                chosen = config
                break
            if chosen is None:
                chosen = config
        return (chosen or {}).get("buildSettings", {})

    project = None
    for value in objects.values():
        if value.get("isa") == "PBXProject":
            project = value
            break
    merged = dict(settings_for((project or {}).get("buildConfigurationList", "")))
    merged.update(settings_for(target.get("buildConfigurationList", "")))
    return merged


def preprocessor_flags(settings):
    defs = settings.get("GCC_PREPROCESSOR_DEFINITIONS", [])
    if isinstance(defs, str):
        defs = [defs]
    flags = []
    for item in defs:
        item = item.strip()
        # $(inherited) and anything Xcode would expand is not ours to resolve.
        if not item or item.startswith("$"):
            continue
        flags.append("-D" + item)
    return flags


def port_native_sources(repo_root, src_dir):
    """The port's own natives, as they were copied into the generated project.

    Scoped to our sources deliberately. Translated C only calls into natives, and
    a cn1lib's natives are its author's to declare -- neither is this gate's to
    police.
    """
    native_dir = os.path.join(repo_root, "Ports", "iOSPort", "nativeSources")
    if not os.path.isdir(native_dir):
        fail("cannot find %s" % native_dir)
    names = sorted(n for n in os.listdir(native_dir) if n.endswith(".m"))
    present = [n for n in names if os.path.isfile(os.path.join(src_dir, n))]
    missing = [n for n in names if n not in present]
    return present, missing


def compile_environment_flags(settings, project_dir):
    """The prefix header and language standard the target really compiles with.

    Without the prefix header the port does not see `cn1_globals.h`, so `JAVA_INT`
    is an unknown type and ten natives fail to compile -- and a native that fails
    to compile is one whose framework references are never examined.

    The three -Wno-error entries cover diagnostics clang promoted to errors by
    default after this code was written (implicit declarations, int/pointer
    conversions). The generated project pins `-std=c99` and builds fine; this
    check is not the place to relitigate that, and turning them back into
    warnings keeps it reading the same sources Xcode does.
    """
    flags = []
    standard = settings.get("GCC_C_LANGUAGE_STANDARD")
    if standard:
        flags.append("-std=" + str(standard))
    prefix = settings.get("GCC_PREFIX_HEADER")
    if prefix:
        path = os.path.join(project_dir, str(prefix))
        if os.path.isfile(path):
            flags += ["-include", path]
    flags += [
        "-Wno-error=implicit-function-declaration",
        "-Wno-error=incompatible-pointer-types",
        "-Wno-error=int-conversion",
    ]
    return flags


def module_flags(settings, work_dir):
    """Modules only where the project asks for them, and never as a source of truth.

    `IPhoneBuilder` turns CLANG_ENABLE_MODULES on for the Metal build (and for
    pods, the watch and the VPN extension), and `GLUIImage.h` needs it: under
    CN1_USE_METAL it says `@import Metal;`, which does not compile with modules
    off. So the flag is mirrored purely to make the sources parse.

    It does not weaken the check. Modules make clang stamp an autolink hint into
    the object, which is precisely how this class of bug hides -- but a hint is a
    load command, not a definition, so `nm` still reports the symbol undefined,
    and the probe linked below carries no hints at all. What the project would
    have got for free is exactly what this check refuses to credit.
    """
    if str(settings.get("CLANG_ENABLE_MODULES", "NO")).upper() != "YES":
        return []
    cache = os.path.join(work_dir, "modulecache")
    return ["-fmodules", "-fmodules-cache-path=" + cache]


def compile_natives(sources, src_dir, sdk, target_triple, flags, out_dir, file_flags):
    objects = []
    failures = []
    for name in sources:
        obj = os.path.join(out_dir, name[:-2] + ".o")
        cmd = [
            "clang", "-c", "-arch", "arm64", "-target", target_triple,
            "-isysroot", sdk, "-fno-objc-arc", "-w",
            "-I", src_dir,
        ] + flags + file_flags.get(name, []) + [os.path.join(src_dir, name), "-o", obj]
        result = run(cmd)
        if result.returncode != 0:
            failures.append((name, result.stderr.strip().splitlines()[:4]))
        else:
            objects.append(obj)
    return objects, failures


def symbol_sets(objects):
    """Undefined and defined symbols across every compiled native."""
    undefined, defined = set(), set()
    for obj in objects:
        out = run(["nm", "-g", obj])
        if out.returncode != 0:
            continue
        for line in out.stdout.splitlines():
            parts = line.split()
            if len(parts) < 2:
                continue
            kind, name = parts[-2], parts[-1]
            if kind == "U":
                undefined.add(name)
            else:
                defined.add(name)
    return undefined, defined


TBD_OBJC = re.compile(r"objc-classes:\s*\[(.*?)\]", re.S)
TBD_SYMS = re.compile(r"\bsymbols:\s*\[(.*?)\]", re.S)


def index_sdk_symbols(sdk):
    """symbol -> framework, read from the SDK's own text stubs.

    Both spellings matter: `objc-classes: [ UTType ]` is the class, and the
    linker looks for it as `_OBJC_CLASS_$_UTType`.
    """
    index = {}
    frameworks_root = os.path.join(sdk, "System", "Library", "Frameworks")
    for entry in sorted(os.listdir(frameworks_root)):
        if not entry.endswith(".framework"):
            continue
        framework = entry[: -len(".framework")]
        tbd = os.path.join(frameworks_root, entry, framework + ".tbd")
        if not os.path.isfile(tbd):
            continue
        try:
            with open(tbd, "r", errors="replace") as handle:
                text = handle.read()
        except OSError:
            continue
        for match in TBD_OBJC.finditer(text):
            for cls in match.group(1).replace("\n", " ").split(","):
                cls = cls.strip().strip("'\"")
                if cls:
                    index.setdefault("_OBJC_CLASS_$_" + cls, framework)
                    index.setdefault("_OBJC_METACLASS_$_" + cls, framework)
        for match in TBD_SYMS.finditer(text):
            for sym in match.group(1).replace("\n", " ").split(","):
                sym = sym.strip().strip("'\"")
                if sym:
                    index.setdefault(sym, framework)
    return index


def confirm_by_linking(symbols, declared, sdk, target_triple, work_dir):
    """Link a probe that references each finding against exactly what we declare.

    The index is a text scan and could be wrong in either direction; a real link
    cannot be. Anything the linker resolves is dropped from the report.
    """
    if not symbols:
        return set()
    probe_c = os.path.join(work_dir, "probe.c")
    with open(probe_c, "w") as handle:
        handle.write("/* generated: forces a reference to each symbol under test */\n")
        for i, sym in enumerate(sorted(symbols)):
            handle.write('extern char cn1_probe_%d __asm("%s");\n' % (i, sym))
        handle.write("void* cn1_probe_refs[] = {\n")
        for i in range(len(symbols)):
            handle.write("    &cn1_probe_%d,\n" % i)
        handle.write("};\nint main(void) { return cn1_probe_refs[0] != 0; }\n")
    probe_o = os.path.join(work_dir, "probe.o")
    result = run(["clang", "-c", "-arch", "arm64", "-target", target_triple,
                  "-isysroot", sdk, "-w", probe_c, "-o", probe_o])
    if result.returncode != 0:
        fail("could not build the confirmation probe:\n%s" % result.stderr)
    cmd = ["clang", "-arch", "arm64", "-target", target_triple, "-isysroot", sdk,
           probe_o, "-o", os.path.join(work_dir, "probe.out")]
    for framework in sorted(declared):
        cmd += ["-framework", framework]
    result = run(cmd)
    unresolved = set()
    for line in result.stderr.splitlines():
        match = re.search(r'"([^"]+)", referenced from', line)
        if match:
            unresolved.add(match.group(1))
        match = re.search(r"Undefined symbol: (\S+)", line)
        if match:
            unresolved.add(match.group(1))
    return unresolved & set(symbols)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("project_dir", help="a generated ios-source project directory")
    parser.add_argument("--sdk", default="iphoneos", help="SDK to resolve against")
    parser.add_argument("--configuration", default="Release",
                        help="build configuration whose settings to use")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()

    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    sdk = sdk_path(args.sdk)
    project, app, src_dir = load_pbxproj(args.project_dir)
    objects = project["objects"]
    target = app_target(objects, app)
    declared = declared_frameworks(objects, target)
    settings = build_settings(objects, target, args.configuration)
    deployment = str(settings.get("IPHONEOS_DEPLOYMENT_TARGET", "15.0"))
    suffix = "-simulator" if args.sdk.endswith("simulator") else ""
    triple = "arm64-apple-ios%s%s" % (deployment, suffix)

    print("project           : %s (%s)" % (app, args.project_dir))
    print("sdk               : %s" % sdk)
    print("deployment target : %s" % triple)
    print("modules           : %s" % settings.get("CLANG_ENABLE_MODULES", "NO"))
    print("frameworks declared by the app target: %d" % len(declared))

    index = index_sdk_symbols(sdk)
    if len(index) < MIN_INDEXED_SYMBOLS:
        fail("indexed only %d SDK symbols, expected at least %d -- the SDK stub format "
             "changed and this check would report success without looking"
             % (len(index), MIN_INDEXED_SYMBOLS))
    for symbol, framework in SELF_TEST_SYMBOLS.items():
        if index.get(symbol) != framework:
            fail("self-test failed: %s should come from %s, index says %s"
                 % (symbol, framework, index.get(symbol)))
    print("sdk symbols indexed: %d" % len(index))

    sources, missing = port_native_sources(repo_root, src_dir)
    if missing:
        print("note: %d port natives are not in this project (%s)"
              % (len(missing), ", ".join(missing[:6]) + ("..." if len(missing) > 6 else "")))
    if not sources:
        fail("no port natives found in %s -- nothing would be checked" % src_dir)

    work_dir = tempfile.mkdtemp(prefix="cn1-framework-links-")
    obj_dir = os.path.join(work_dir, "obj")
    os.makedirs(obj_dir)
    flags = (preprocessor_flags(settings)
             + compile_environment_flags(settings, args.project_dir)
             + module_flags(settings, work_dir))
    compiled, failures = compile_natives(sources, src_dir, sdk, triple, flags, obj_dir,
                                         per_file_flags(objects, target))
    print("port natives compiled: %d of %d" % (len(compiled), len(sources)))
    if failures:
        # A native that does not compile is a native whose references are never
        # examined. Reporting success over a partial set is the exact shape of
        # failure this check exists to prevent, so it is fatal.
        sys.stderr.write("\nthese port natives did not compile, so their references were "
                         "never checked:\n")
        for name, lines in failures:
            sys.stderr.write("  %s\n" % name)
            for line in lines:
                sys.stderr.write("      %s\n" % line)
        fail("%d port natives failed to compile" % len(failures))

    undefined, defined = symbol_sets(compiled)
    candidates = undefined - defined
    from_sdk = {s: index[s] for s in candidates if s in index}
    declared_symbols = {s for s, f in from_sdk.items() if f in declared}
    suspects = set(from_sdk) - declared_symbols
    if args.verbose:
        print("undefined symbols: %d, of which the SDK defines %d"
              % (len(candidates), len(from_sdk)))

    findings = confirm_by_linking(suspects, declared, sdk, triple, work_dir)
    dropped = suspects - findings
    if dropped and args.verbose:
        print("resolved after all (re-exported through a declared framework): %d" % len(dropped))

    if not findings:
        print("\nOK: every SDK symbol the port references is supplied by a declared framework.")
        return 0

    print("\nFAIL: the port references SDK symbols no declared framework supplies.")
    print("An application links these only if its OWN sources happen to drag the")
    print("framework in -- a Swift file's autolink hint, or a CocoaPods dependency.")
    print("A plain Objective-C application fails at the link.\n")
    by_framework = {}
    for symbol in sorted(findings):
        by_framework.setdefault(from_sdk[symbol], []).append(symbol)
    for framework in sorted(by_framework):
        print("  %s.framework is not linked, but the port uses:" % framework)
        for symbol in by_framework[framework][:10]:
            print("      %s" % symbol)
        if len(by_framework[framework]) > 10:
            print("      ... and %d more" % (len(by_framework[framework]) - 10))
    print("\nAdd each to ByteCodeTranslator.includeFrameworks (weakly, via")
    print("optionalFrameworks, when the API is newer than the deployment target),")
    print("and classify it in WatchNativeBuilder for the watch slice.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
