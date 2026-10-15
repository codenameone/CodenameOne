#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
"""Local-only native title-area harnesses. No CI configuration or application host."""
import argparse
import os
import pathlib
import shutil
import subprocess
import tempfile

root = pathlib.Path(__file__).resolve().parents[2]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument("platform", choices=["linux", "windows", "macos"])
p.add_argument("--output", type=pathlib.Path, help="New directory for binaries/screenshots")
p.add_argument("--build-only", action="store_true", help="Build the Windows executable without Wine")
a = p.parse_args()
if os.environ.get("CI"):
    p.error("Native title-area tests are local only")
if a.platform == "macos":
    subprocess.run(["python3", str(root / "scripts/desktop-titlebar-tests/macos.py")], check=True)
    raise SystemExit(0)
workspace = None
if a.output:
    a.output.mkdir(parents=True, exist_ok=False)
    out = a.output.resolve()
else:
    workspace = tempfile.TemporaryDirectory(prefix="cn1-titlebar-test-")
    out = pathlib.Path(workspace.name)
# These harnesses never translate/run Java. The runtime header only needs the
# generated index declaration, not an application-specific generated source tree.
(out / "cn1_class_method_index.h").write_text("extern int classNameLookup[];\n")
common = ["-ffunction-sections", "-fdata-sections", "-I" + str(root / "vm/ByteCodeTranslator/src"), "-I" + str(out)]
if a.platform == "linux":
    flags = subprocess.check_output(["pkg-config", "--cflags", "--libs", "gtk+-3.0", "pangocairo"], text=True).split()
    binary = out / "linux"
    subprocess.run(["gcc", "-std=c11"] + common + ["-I" + str(root / "Ports/LinuxPort/nativeSources"),
                    str(root / "scripts/desktop-titlebar-tests/linux.c"), "-Wl,--gc-sections"] + flags +
                   ["-lpthread", "-lm", "-o", str(binary)], check=True)
    env = dict(os.environ, G_DEBUG="fatal-warnings")
    subprocess.run(["xvfb-run", "-a", "sh", "-c",
                    'openbox >"$1/openbox.log" 2>&1 & wm=$!; trap "kill $wm 2>/dev/null || true" EXIT; '
                    '"$1/linux" "$1/right.png" "$1/left.png"', "sh", str(out)], check=True, env=env)
else:
    # Clang accepts the C11 atomics in the production ParparVM headers under C++.
    # The MinGW linker supplies Windows import libraries; no headers are mocked.
    linker = "x86_64-w64-mingw32-g++"
    sysroot = pathlib.Path(subprocess.check_output([linker, "-print-sysroot"], text=True).strip())
    compiler = shutil.which("clang++")
    if not compiler:
        p.error("clang++ is required for the Windows cross-compile")
    flags = [compiler, "--target=x86_64-w64-windows-gnu", "--sysroot=" + str(sysroot),
             "-isystem", str(sysroot / "x86_64-w64-mingw32/include"), "-std=c++17"] + common + [
             "-I" + str(root / "Ports/WindowsPort/nativeSources")]
    for source in ["cn1_windows_chrome.cpp", "cn1_windows_window.cpp", "cn1_windows_menu.cpp"]:
        subprocess.run(flags + ["-fsyntax-only", str(root / "Ports/WindowsPort/nativeSources" / source)], check=True)
    subprocess.run(flags + ["-c", str(root / "scripts/desktop-titlebar-tests/windows.cpp"),
                            "-o", str(out / "windows.o")], check=True)
    subprocess.run([linker, str(out / "windows.o"), "-Wl,--gc-sections", "-ldwmapi", "-luser32",
                    "-ladvapi32", "-o", str(out / "windows.exe")], check=True)
    if not a.build_only:
        subprocess.run(["wine", str(out / "windows.exe")], check=True)
if a.output:
    print("Artifacts:", out)
if workspace:
    workspace.cleanup()
