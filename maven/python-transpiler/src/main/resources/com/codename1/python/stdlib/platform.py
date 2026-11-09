# Copyright (c) 2001 Python Software Foundation; All Rights Reserved
# Licensed under the Python Software Foundation License Version 2; the full
# text is in maven/python-transpiler/third-party/cpython/LICENSE.txt.
#
# Derived from CPython's Lib/platform.py. Changes: rewritten for the Codename One
# Python runtime, trimmed to what the transpiler supports and reorganised;
# the C accelerators and platform-specific branches are left out.
#
# Original notice:
#
#
#     Copyright (c) 1999-2000, Marc-Andre Lemburg; mailto:mal@lemburg.com
#     Copyright (c) 2000-2010, eGenix.com Software GmbH; mailto:info@egenix.com
#
#     Permission to use, copy, modify, and distribute this software and its
#     documentation for any purpose and without fee or royalty is hereby granted,
#     provided that the above copyright notice appear in all copies and that
#     both that copyright notice and this permission notice appear in
#     supporting documentation or portions thereof, including modifications,
#     that you make.
#
#     EGENIX.COM SOFTWARE GMBH DISCLAIMS ALL WARRANTIES WITH REGARD TO
#     THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
#     FITNESS, IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL,
#     INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
#     FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
#     NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
#     WITH THE USE OR PERFORMANCE OF THIS SOFTWARE !
#
#

"""platform: what is known about the device. system() is the platform
name the port reports (iOS, Android, ...), and the Python identity is the
translator's: implementation "CodenameOne" at the language version it
compiles."""

import sys as _sys


def _platform_name():
    try:
        from java.com.codename1.ui import Display
        n = Display.getInstance().getPlatformName()
        return {"ios": "iOS", "and": "Android", "win": "Windows", "mac": "Darwin", "se": "Java",
                "HTML5": "Emscripten"}.get(str(n), str(n))
    except Exception:
        return "Java"


def system():
    return _platform_name()


def node():
    return "localhost"


def release():
    return ""


def version():
    return ""


def machine():
    return ""


def processor():
    return ""


def architecture(executable=None, bits="", linkage=""):
    return ("64bit", "")


def uname():
    import collections
    r = collections.namedtuple("uname_result", "system node release version machine processor")
    return r(system(), node(), release(), version(), machine(), processor())


def platform(aliased=False, terse=False):
    return system()


def python_implementation():
    return "CodenameOne"


def python_version_tuple():
    v = _sys.version_info
    return (str(v[0]), str(v[1]), str(v[2]))


def python_version():
    return ".".join(python_version_tuple())


def python_build():
    return ("", "")


def python_compiler():
    return "Codename One Python transpiler"


def python_branch():
    return ""


def python_revision():
    return ""


def mac_ver(release="", versioninfo=("", "", ""), machine=""):
    return (release, versioninfo, machine)


def win32_ver(release="", version="", csd="", ptype=""):
    return (release, version, csd, ptype)


def libc_ver(executable=None, lib="", version="", chunksize=16384):
    return (lib, version)


def freedesktop_os_release():
    raise OSError("no os-release file")
