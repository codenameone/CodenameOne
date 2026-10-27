#!/usr/bin/env python3
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
#
"""Renders vm/ByteCodeTranslator/src/cn1_crypto_gpu_kernels.h for the two GPU dialects.

Writes vm/ByteCodeTranslator/src/cn1_crypto_gpu_shaders.h, holding
  - the Metal Shading Language source, which the Metal host compiles at run time
    (an MSL library compiled ahead of time is tied to an SDK, and the source is
    what every Apple target can take), and
  - one SPIR-V module per kernel, compiled from GLSL by glslc, for Vulkan.

The header records a digest of its inputs. `--check` regenerates nothing and
fails when the committed header was produced from different sources, which is
what CI runs: glslc is only needed by whoever changes the kernels.

    scripts/crypto-gpu/gen.py [--glslc PATH]
    scripts/crypto-gpu/gen.py --check
"""
import argparse
import glob
import hashlib
import os
import re
import subprocess
import sys
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "vm", "ByteCodeTranslator", "src")
INC = os.path.join(SRC, "cn1_crypto_gpu_kernels.h")
OUT = os.path.join(SRC, "cn1_crypto_gpu_shaders.h")

KERNELS = ["SHA256", "KECCAK256", "ECDSA", "SCHNORR", "ED25519"]

MSL_PRELUDE = """#include <metal_stdlib>
using namespace metal;
#define U(x) uint(x)
#define MULHI(a, b) mulhi(uint(a), uint(b))
#define FN inline
#define STRUCT(n) struct n
#define CARR(t, n, c) constant t n[c] = {
#define CARR_END };
#define TBL_PARAM , device const uint *tbl
#define TBL_ARG , tbl
#define TBL_W(i) tbl[i]
#define U2 uint2
#define u2(a, b) uint2(a, b)
#define CN1G_KERNELS 1
#define CN1G_ALL 1
#define KERNEL(name) kernel void name(device const uint *inb [[buffer(0)]], device uint *outb [[buffer(1)]], \\
    device const uint *tbl [[buffer(2)]], constant uint4 &pc [[buffer(3)]], uint gid_ [[thread_position_in_grid]])
#define IN_W(i) inb[i]
#define OUT_W(i, v) outb[i] = (v)
#define P_LEN pc.x
#define P_COUNT pc.y
#define P_BASE pc.z
#define GID gid_
"""

GLSL_PRELUDE = """#version 450
layout(local_size_x = 64) in;
layout(std430, binding = 0) readonly buffer B0 { uint d[]; } inb;
layout(std430, binding = 1) writeonly buffer B1 { uint d[]; } outb;
layout(std430, binding = 2) readonly buffer B2 { uint d[]; } tblb;
layout(push_constant) uniform PC { uint len; uint count; uint a; uint b; } pc;
uint cn1g_mulhi(uint a, uint b) { uint h, l; umulExtended(a, b, h, l); return h; }
#define U(x) uint(x)
#define MULHI(a, b) cn1g_mulhi(a, b)
#define FN
#define STRUCT(n) struct n
#define CARR(t, n, c) const t n[c] = t[c](
#define CARR_END );
#define TBL_PARAM
#define TBL_ARG
#define TBL_W(i) tblb.d[i]
#define U2 uvec2
#define u2(a, b) uvec2(a, b)
#define CN1G_KERNELS 1
#define CN1G_ALL 0
#define KERNEL(name) void main()
#define IN_W(i) inb.d[i]
#define OUT_W(i, v) outb.d[i] = (v)
#define P_LEN pc.len
#define P_COUNT pc.count
#define P_BASE pc.a
#define GID gl_GlobalInvocationID.x
"""

HEADER = """/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
 * GENERATED by scripts/crypto-gpu/gen.py from cn1_crypto_gpu_kernels.h -- do not edit.
 * Regenerate after changing the kernels; CI fails when this is stale.
 */
"""


def digest():
    h = hashlib.sha256()
    for path in (INC, os.path.abspath(__file__)):
        with open(path, "rb") as f:
            h.update(f.read().replace(b"\r\n", b"\n"))
    return h.hexdigest()


def find_glslc():
    for pattern in (os.path.expanduser("~/Library/Android/sdk/ndk/*/shader-tools/*/glslc"),
                    os.path.join(os.environ.get("ANDROID_HOME", "/nonexistent"), "ndk/*/shader-tools/*/glslc"),
                    os.path.join(os.environ.get("ANDROID_NDK_HOME", "/nonexistent"), "shader-tools/*/glslc")):
        found = sorted(glob.glob(pattern))
        if found:
            return found[-1]
    return "glslc"


def c_string(text):
    lines = []
    for line in text.split("\n"):
        line = line.replace("\\", "\\\\").replace('"', '\\"')
        lines.append('    "%s\\n"' % line)
    return "\n".join(lines)


def spirv(glslc, source, kernel):
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "k.comp")
        out = os.path.join(tmp, "k.spv")
        with open(src, "w") as f:
            f.write(source)
        subprocess.check_call([glslc, "-O", "--target-env=vulkan1.0", "-DCN1G_K_" + kernel, src, "-o", out])
        with open(out, "rb") as f:
            data = f.read()
    words = [int.from_bytes(data[i:i + 4], "little") for i in range(0, len(data), 4)]
    rows = []
    for i in range(0, len(words), 8):
        rows.append("    " + ", ".join("0x%08xu" % w for w in words[i:i + 8]) + ",")
    return words, "\n".join(rows)


def committed_digest():
    if not os.path.exists(OUT):
        return None
    with open(OUT) as f:
        m = re.search(r'#define CN1G_SOURCES_SHA256 "([0-9a-f]+)"', f.read())
    return m.group(1) if m else None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--glslc")
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    want = digest()
    if args.check:
        have = committed_digest()
        if have != want:
            print("cn1_crypto_gpu_shaders.h is stale: it was generated from sources with digest %s, "
                  "the tree has %s. Run scripts/crypto-gpu/gen.py." % (have, want))
            return 1
        print("cn1_crypto_gpu_shaders.h matches its sources")
        return 0
    with open(INC) as f:
        inc = f.read()
    glslc = args.glslc or find_glslc()
    parts = [HEADER, "#ifndef CN1_CRYPTO_GPU_SHADERS_H\n#define CN1_CRYPTO_GPU_SHADERS_H\n",
             '#define CN1G_SOURCES_SHA256 "%s"\n' % want,
             "static const char cn1g_msl_source[] =\n%s;\n" % c_string(MSL_PRELUDE + inc)]
    for k in KERNELS:
        words, rows = spirv(glslc, GLSL_PRELUDE + inc, k)
        parts.append("static const uint32_t cn1g_spv_%s[%d] = {\n%s\n};\n" % (k.lower(), len(words), rows))
    parts.append("#endif\n")
    with open(OUT, "w", newline="\n") as f:
        f.write("\n".join(parts))
    print("wrote %s" % os.path.relpath(OUT, ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
