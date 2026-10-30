#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.
"""Generates Ports/WindowsPort/nativeSources/cn1_windows_uwp_stubs.cpp.

A UWP (Xbox Developer Mode) build compiles the desktop-only Windows natives out
-- secondary windows, menus, the tray, native widgets, file dialogs, printing,
UI Automation, the camera -- because they are written against Win32 APIs the
app container does not have. Every Java native they define is still referenced
by the translated WindowsNative class, so the build needs a definition of each
one. This script writes them, each answering 0 / null / nothing, which is what
the Java side already reads as "not supported here".

Generated rather than hand-written so a native added to one of those files
cannot be forgotten: `--check` (run in CI) fails when the committed file is not
what this script would write.

    scripts/gen-windows-uwp-stubs.py           # rewrite the stubs
    scripts/gen-windows-uwp-stubs.py --check   # fail if they are stale
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NATIVE_DIR = os.path.join(ROOT, "Ports", "WindowsPort", "nativeSources")
OUT = os.path.join(NATIVE_DIR, "cn1_windows_uwp_stubs.cpp")

# The files a UWP build compiles out. Each carries the matching
# `!defined(CN1_WINDOWS_UWP)` guard; keep the two lists together.
DESKTOP_ONLY = [
    "cn1_windows_accessibility.cpp",
    "cn1_windows_audiorec.c",
    "cn1_windows_camera.cpp",
    "cn1_windows_chrome.cpp",
    "cn1_windows_desktopwindow.cpp",
    "cn1_windows_edit.c",
    "cn1_windows_filedialog.cpp",
    "cn1_windows_menu.cpp",
    "cn1_windows_notify.c",
    "cn1_windows_peer.cpp",
    "cn1_windows_print.cpp",
    "cn1_windows_widgets.cpp",
]

# Natives the UWP host (cn1_windows_corewindow.cpp) implements for real even
# though their desktop version lives in one of the files above.
IMPLEMENTED_BY_UWP_HOST = set()

DEF_RE = re.compile(
    r"^(?:extern \"C\"\s+)?(JAVA_[A-Z]+)\s+(com_codename1_\w+)\s*\(([^)]*)\)\s*\{",
    re.M)
GUARD = "!defined(CN1_WINDOWS_UWP)"

# A file the UWP build keeps can still have desktop-only stretches (io.c's
# clipboard and file dialog). Those are fenced by exactly these two lines, and
# the natives between them are stubbed as well.
REGION_START = "#if !defined(CN1_WINDOWS_UWP) /* uwp: desktop only */"
REGION_END = "#endif /* uwp: end desktop only */"


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def desktop_only_sources():
    """(file name, source text) for every stretch a UWP build compiles out."""
    for name in sorted(os.listdir(NATIVE_DIR)):
        if not name.startswith("cn1_windows") or name == os.path.basename(OUT):
            continue
        with open(os.path.join(NATIVE_DIR, name), encoding="ascii") as f:
            src = f.read()
        if name in DESKTOP_ONLY:
            if GUARD not in src:
                sys.exit("%s is listed as desktop-only but has no %s guard" % (name, GUARD))
            yield name, src
            continue
        # A near miss on the marker would silently leave a region's natives
        # unstubbed, which only a UWP link would then report.
        for line in src.splitlines():
            if "uwp: desktop only" in line and line.strip() != REGION_START:
                sys.exit("%s: malformed desktop-only marker %r; use exactly %r"
                         % (name, line.strip(), REGION_START))
        pos = 0
        while True:
            start = src.find(REGION_START, pos)
            if start < 0:
                break
            end = src.find(REGION_END, start)
            if end < 0:
                sys.exit("%s opens a desktop-only region it never closes" % name)
            yield name, src[start:end]
            pos = end


def collect():
    natives = {}
    for name, src in desktop_only_sources():
        for m in DEF_RE.finditer(strip_comments(src)):
            ret, fn, params = m.group(1), m.group(2), " ".join(m.group(3).split())
            if fn in IMPLEMENTED_BY_UWP_HOST:
                continue
            if fn in natives:
                sys.exit("%s is defined twice (%s and %s)" % (fn, natives[fn][2], name))
            natives[fn] = (ret, params, name)
    return natives


def render(natives):
    out = []
    out.append("""/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/*
 * GENERATED by scripts/gen-windows-uwp-stubs.py -- do not edit by hand.
 *
 * The desktop-only natives, answering "unsupported" in a UWP (Xbox Developer
 * Mode) build, where their real definitions are compiled out. See the script
 * for why each one has to exist.
 */
#include "cn1_windows.h"

#if defined(_WIN32) && defined(CN1_WINDOWS_UWP)

extern "C" {
""")
    for fn in sorted(natives):
        ret, params, src = natives[fn]
        out.append("\n/* %s */\n" % src)
        out.append("%s %s(%s) {\n" % (ret, fn, params))
        if ret != "JAVA_VOID":
            out.append("    return 0;\n")
        out.append("}\n")
    out.append("""
} /* extern "C" */

#endif /* _WIN32 && CN1_WINDOWS_UWP */
""")
    return "".join(out)


def main():
    text = render(collect())
    if "--check" in sys.argv[1:]:
        try:
            with open(OUT, encoding="ascii") as f:
                current = f.read()
        except IOError:
            current = None
        if current != text:
            sys.exit("%s is stale: run scripts/gen-windows-uwp-stubs.py" % os.path.relpath(OUT, ROOT))
        print("UWP stubs are current")
        return
    with open(OUT, "w", encoding="ascii", newline="\n") as f:
        f.write(text)
    print("wrote %s" % os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
