#!/usr/bin/env python3
"""Size accounting and result assembly for the desktop-compat benchmarks.

Subcommands, each printing or writing JSON:

  size PATH...          bytes on disk and bytes as one deflated zip
  layers JAR            what the compatibility runtime contributes to a jar
  translated DIR        which classes survived into the translated C project
  environment           the machine the numbers were taken on
  assemble DIR OUT      merge the per-step JSON files of one application
  collect OUT RESULT... several applications' results in one document
  diagnose LOG DEFAULT  why a build log failed: first telling line and a class

Python 3.9 compatible, standard library only.
"""

import argparse
import json
import os
import platform
import re
import subprocess
import sys
import tempfile
import zipfile

# Class-name prefixes, as they appear in a staged (relocated) jar, of each part
# of the runtime an imported desktop application ships.
LAYERS = [
    ("swing-compat", ("com/codename1/desktopcompat/",)),
    ("javafx-compat", ("com/codename1/fxcompat/",)),
    ("compat-jdk", ("com/codename1/compat/jdk/",)),
    ("generated", ("com/codename1/generated/",)),
    ("codenameone-core", ("com/codename1/",)),
    ("java-api", ("java/", "javax/", "sun/", "kotlin/")),
]


def layer_of(class_path):
    for name, prefixes in LAYERS:
        for prefix in prefixes:
            if class_path.startswith(prefix):
                return name
    return "application"


def walk_files(path):
    if os.path.isfile(path) or os.path.islink(path):
        yield path, os.path.basename(path)
        return
    base = os.path.dirname(os.path.abspath(path))
    for folder, dirs, files in os.walk(path):
        dirs.sort()
        # A symlinked directory is reported by os.walk under dirs and not
        # descended into; record the link itself, as a bundle ships it.
        for name in sorted(files) + [d for d in dirs if os.path.islink(os.path.join(folder, d))]:
            full = os.path.join(folder, name)
            yield full, os.path.relpath(full, base)


def measure_size(paths, scratch):
    """Uncompressed bytes and the size of a single deflate -9 zip of the paths."""
    total = 0
    count = 0
    handle, archive = tempfile.mkstemp(suffix=".zip", dir=scratch)
    os.close(handle)
    try:
        with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as bundle:
            for path in paths:
                for full, relative in walk_files(path):
                    count += 1
                    if os.path.islink(full):
                        target = os.readlink(full)
                        total += len(target)
                        info = zipfile.ZipInfo(relative)
                        info.external_attr = 0o120777 << 16
                        bundle.writestr(info, target)
                    else:
                        total += os.path.getsize(full)
                        bundle.write(full, relative)
        zipped = os.path.getsize(archive)
    finally:
        os.remove(archive)
    return {"files": count, "bytes": total, "zip_bytes": zipped}


def jar_layers(jar):
    """Per-layer class count and bytes of a jar."""
    layers = {}
    resources = {"files": 0, "bytes": 0, "compressed_bytes": 0}
    with zipfile.ZipFile(jar) as bundle:
        for entry in bundle.infolist():
            if entry.filename.endswith("/"):
                continue
            if entry.filename.endswith(".class"):
                bucket = layers.setdefault(layer_of(entry.filename),
                                           {"classes": 0, "bytes": 0, "compressed_bytes": 0})
                bucket["classes"] += 1
            else:
                bucket = resources
                bucket["files"] += 1
            bucket["bytes"] += entry.file_size
            bucket["compressed_bytes"] += entry.compress_size
    return {"jar": os.path.basename(jar), "jar_bytes": os.path.getsize(jar),
            "classes": layers, "resources": resources}


CLASS_DEFINITION = re.compile(r"^struct clazz class__([A-Za-z0-9_]+) = \{", re.M)


def translated_layers(directory):
    """Classes that reached the translated C project, by layer.

    ParparVM emits one `struct clazz class__<mangled name> = {` definition per
    class that survives its dead-code pass. The definitions are counted rather
    than the files, because the Apple builds concatenate many classes into one
    file. Counting them against the classes in the staged jar is the measure of
    what was stripped.
    """
    prefixes = [(name, tuple(p.replace("/", "_") for p in group)) for name, group in LAYERS]
    layers = {}
    seen = set()
    for folder, dirs, files in os.walk(directory):
        # Build products: objects and indexes, not translator output.
        dirs[:] = [d for d in dirs if d not in ("build", "DerivedData")]
        for name in files:
            if not name.endswith((".c", ".m")):
                continue
            with open(os.path.join(folder, name), errors="replace") as handle:
                text = handle.read()
            for mangled in CLASS_DEFINITION.findall(text):
                if mangled in seen:
                    continue
                seen.add(mangled)
                layer = "application"
                for candidate, group in prefixes:
                    if any(mangled.startswith(p) for p in group):
                        layer = candidate
                        break
                bucket = layers.setdefault(layer, {"classes": 0})
                bucket["classes"] += 1
    return layers


NOISE = re.compile(r"Prohibited package name|is not a native interface|Parsing: |Building C(XX)? object|"
                   r"^\s+at |warning: ")
DIAGNOSES = [
    ("builder time limit", re.compile(r"Process timed out|Timeout reached")),
    ("unsupported API", re.compile(r"bytecode-compliance.*(ERROR|FAIL)|\[ERROR\].*is not supported on")),
    ("ParparVM translation", re.compile(r"Multiple main classes|tools\.translator\.|ByteCodeTranslator.*(Exception|rror)")),
    ("native compile or link", re.compile(r": error: |ld(\.lld|64)?: error|undefined symbol|Undefined symbols|"
                                          r"ninja: build stopped|\*\* BUILD FAILED \*\*|fatal error")),
    ("javac", re.compile(r"COMPILATION ERROR|\.java:\[?\d+.*error")),
]
FALLBACK = re.compile(r"\[ERROR\] Failed to execute goal|Exception in thread|Exception: |\[ERROR\] \S|error:")


def diagnose(log, default):
    """The first line of a build log that says why it failed, and what kind of failure it is.

    A heuristic, and labelled as one: the classification is where to start
    reading, not a verdict. The full log is kept beside the result.
    """
    with open(log, errors="replace") as handle:
        lines = [line.rstrip() for line in handle if not NOISE.search(line)]
    for classification, pattern in DIAGNOSES:
        for line in lines:
            if pattern.search(line):
                return {"classification": classification, "first_error": line.strip()[:600]}
    for line in lines:
        if FALLBACK.search(line):
            return {"classification": default, "first_error": line.strip()[:600]}
    return {"classification": default, "first_error": ""}


def run(command):
    try:
        return subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              universal_newlines=True, timeout=60).stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return None


def first_line(text):
    return text.splitlines()[0] if text else None


def environment():
    facts = {
        "system": platform.system(),
        "kernel": platform.release(),
        "machine": platform.machine(),
        "python": platform.python_version(),
        "cpus": os.cpu_count(),
        "commit": os.environ.get("BENCH_COMMIT"),
        "container_image": os.environ.get("BENCH_IMAGE_NAME"),
        "in_container": os.path.exists("/run/.containerenv") or os.path.exists("/.dockerenv"),
        "screen": os.environ.get("BENCH_SCREEN"),
    }
    if platform.system() == "Linux":
        try:
            with open("/etc/os-release") as handle:
                for line in handle:
                    if line.startswith("PRETTY_NAME="):
                        facts["os"] = line.split("=", 1)[1].strip().strip('"')
        except OSError:
            pass
        try:
            with open("/proc/meminfo") as handle:
                facts["ram_kb"] = int(handle.readline().split()[1])
        except (OSError, ValueError, IndexError):
            pass
        model = None
        try:
            with open("/proc/cpuinfo") as handle:
                for line in handle:
                    if line.lower().startswith(("model name", "cpu part", "hardware")):
                        model = line.split(":", 1)[1].strip()
                        if line.lower().startswith("model name"):
                            break
        except OSError:
            pass
        facts["cpu"] = model
        facts["host_cpu"] = os.environ.get("BENCH_HOST_CPU")
        facts["gtk"] = run(["pkg-config", "--modversion", "gtk+-3.0"])
    elif platform.system() == "Darwin":
        facts["os"] = "macOS " + platform.mac_ver()[0]
        facts["cpu"] = run(["sysctl", "-n", "machdep.cpu.brand_string"])
        ram = run(["sysctl", "-n", "hw.memsize"])
        facts["ram_kb"] = int(ram) // 1024 if ram and ram.isdigit() else None
        facts["xcode"] = (run(["xcodebuild", "-version"]) or "").replace("\n", ", ")
    for key, variable in (("baseline_jdk", "BENCH_JDK21_HOME"), ("cn1_build_jdk", "JAVA17_HOME")):
        home = os.environ.get(variable)
        if home:
            facts[key] = first_line(run([os.path.join(home, "bin", "java"), "--version"]))
    jmods = os.environ.get("BENCH_JAVAFX_JMODS")
    if jmods and os.path.isfile(os.path.join(jmods, "VERSION")):
        with open(os.path.join(jmods, "VERSION")) as handle:
            facts["openjfx"] = handle.read().strip()
    compiler = os.environ.get("CN1_CC")
    if compiler:
        facts["cn1_c_compiler"] = compiler
        facts["cn1_c_compiler_version"] = first_line(run([compiler, "--version"]))
    return facts


def assemble(directory, out):
    """Merge NAME.json files into one object keyed by NAME ('a.b.json' nests)."""
    merged = {}
    for name in sorted(os.listdir(directory)):
        if not name.endswith(".json"):
            continue
        with open(os.path.join(directory, name)) as handle:
            try:
                value = json.load(handle)
            except ValueError:
                value = {"error": "unreadable " + name}
        keys = name[:-5].split(".")
        node = merged
        for key in keys[:-1]:
            node = node.setdefault(key, {})
        node[keys[-1]] = value
    with open(out, "w") as handle:
        json.dump(merged, handle, indent=2, sort_keys=True)
        handle.write("\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command")
    size = commands.add_parser("size")
    size.add_argument("--scratch", required=True, help="directory for the temporary zip")
    size.add_argument("paths", nargs="+")
    layers = commands.add_parser("layers")
    layers.add_argument("jar")
    translated = commands.add_parser("translated")
    translated.add_argument("directory")
    commands.add_parser("environment")
    merge = commands.add_parser("assemble")
    merge.add_argument("directory")
    merge.add_argument("out")
    diagnosis = commands.add_parser("diagnose")
    diagnosis.add_argument("log")
    diagnosis.add_argument("default")
    collect = commands.add_parser("collect")
    collect.add_argument("out")
    collect.add_argument("results", nargs="+")
    args = parser.parse_args()

    if args.command == "size":
        missing = [p for p in args.paths if not os.path.lexists(p)]
        if missing:
            sys.exit("no such path: " + ", ".join(missing))
        result = measure_size(args.paths, args.scratch)
    elif args.command == "layers":
        result = jar_layers(args.jar)
    elif args.command == "translated":
        result = translated_layers(args.directory)
    elif args.command == "environment":
        result = environment()
    elif args.command == "diagnose":
        result = diagnose(args.log, args.default)
        result.update({"ok": False, "log": args.log})
    elif args.command == "assemble":
        assemble(args.directory, args.out)
        return
    elif args.command == "collect":
        documents = []
        for path in args.results:
            with open(path) as handle:
                documents.append(json.load(handle))
        with open(args.out, "w") as handle:
            json.dump({"results": documents}, handle, indent=2, sort_keys=True)
            handle.write("\n")
        return
    else:
        parser.error("no command given")
        return
    json.dump(result, sys.stdout, indent=2, sort_keys=True)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
