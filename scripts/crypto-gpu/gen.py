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
"""Renders vm/ByteCodeTranslator/src/cn1_crypto_gpu_kernels.h for the three GPU dialects.

Writes vm/ByteCodeTranslator/src/cn1_crypto_gpu_shaders.h, holding
  - the Metal Shading Language source, which the Metal host compiles at run time
    (an MSL library compiled ahead of time is tied to an SDK, and the source is
    what every Apple target can take), and
  - one SPIR-V module per kernel, compiled from GLSL by glslc, for Vulkan;
and Ports/JavaScriptPort/src/main/webapp/js/cn1_crypto_gpu.js, holding
  - one WGSL module per kernel for WebGPU: the same GLSL with a uniform block in
    place of the push constants WebGPU lacks, compiled by glslc and translated
    by naga (cargo install naga-cli). Those are unoptimized SPIR-V, which keeps
    the field operations as functions: optimized, each curve kernel is ~700KB
    of WGSL that the browser parses on every launch.

Both outputs record a digest of their inputs. `--check` regenerates nothing and
fails when the committed header was produced from different sources, which is
what CI runs: glslc is only needed by whoever changes the kernels.

    scripts/crypto-gpu/gen.py [--glslc PATH] [--naga PATH]
    scripts/crypto-gpu/gen.py --check
"""
import argparse
import base64
import glob
import hashlib
import json
import os
import re
import struct
import subprocess
import sys
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "vm", "ByteCodeTranslator", "src")
INC = os.path.join(SRC, "cn1_crypto_gpu_kernels.h")
OUT = os.path.join(SRC, "cn1_crypto_gpu_shaders.h")
WGSL_OUT = os.path.join(ROOT, "Ports", "JavaScriptPort", "src", "main", "webapp", "js", "cn1_crypto_gpu.js")

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


def wgsl_glsl_prelude():
    """The GLSL prelude for the WGSL translation: WebGPU has no push constants, no write-only
    storage and no umulExtended that naga takes, so a uniform block, a plain buffer and a
    portable high multiply stand in."""
    p = GLSL_PRELUDE
    p = p.replace("layout(push_constant) uniform PC", "layout(std140, binding = 3) uniform PC")
    p = p.replace("writeonly buffer", "buffer")
    p = p.replace("uint cn1g_mulhi(uint a, uint b) { uint h, l; umulExtended(a, b, h, l); return h; }",
                  "uint cn1g_mulhi(uint a, uint b) {\n"
                  "    uint al = a & 0xFFFFu, ah = a >> 16, bl = b & 0xFFFFu, bh = b >> 16;\n"
                  "    uint lh = al * bh, hl = ah * bl;\n"
                  "    uint mid = ((al * bl) >> 16) + (lh & 0xFFFFu) + (hl & 0xFFFFu);\n"
                  "    return ah * bh + (lh >> 16) + (hl >> 16) + (mid >> 16);\n"
                  "}")
    assert "push_constant" not in p and "writeonly" not in p and "umulExtended" not in p
    return p


def wgsl(glslc, naga, source, kernel):
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "k.comp")
        spv = os.path.join(tmp, "k.spv")
        out = os.path.join(tmp, "k.wgsl")
        with open(src, "w") as f:
            f.write(source)
        subprocess.check_call([glslc, "-O0", "--target-env=vulkan1.0", "-DCN1G_K_" + kernel, src, "-o", spv])
        subprocess.check_call([naga, spv, out])
        with open(out) as f:
            return f.read()


# The fixed-base tables the curve kernels read, computed here rather than on a device: the C
# hosts build theirs at first use (cn1g_build_table_g/_b), a browser downloads these words.
P_K1 = 2**256 - 2**32 - 977
P_ED = 2**255 - 19
D_ED = (-121665 * pow(121666, -1, P_ED)) % P_ED


def k1_add(a, b):
    if a is None:
        return b
    (x1, y1), (x2, y2) = a, b
    if a == b:
        m = 3 * x1 * x1 * pow(2 * y1, -1, P_K1)
    else:
        m = (y2 - y1) * pow(x2 - x1, -1, P_K1)
    x3 = (m * m - x1 - x2) % P_K1
    return x3, (m * (x1 - x3) - y1) % P_K1


def ed_add(a, b):
    (x1, y1), (x2, y2) = a, b
    t = D_ED * x1 * x2 * y1 * y2
    return ((x1 * y2 + y1 * x2) * pow(1 + t, -1, P_ED) % P_ED,
            (y1 * y2 + x1 * x2) * pow(1 - t, -1, P_ED) % P_ED)


def le_words(v):
    return [(v >> (32 * i)) & 0xFFFFFFFF for i in range(8)]


def fixed_base_tables():
    g = (0x79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798,
         0x483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8)
    b = (0x216936D3CD6E53FEC0A4E231FDD6DC5C692CC7609525A7B2C9562D608F25D51A,
         0x6666666666666666666666666666666666666666666666666666666666666658)
    tg, tb = [0] * (64 * 16 * 16), [0] * (64 * 16 * 24)
    for w in range(64):
        acc = g
        for n in range(1, 16):
            tg[(w * 16 + n) * 16:(w * 16 + n + 1) * 16] = le_words(acc[0]) + le_words(acc[1])
            acc = k1_add(acc, g)
        for _ in range(4):
            g = k1_add(g, g)
        acc = b
        for n in range(1, 16):
            x, y = acc
            tb[(w * 16 + n) * 24:(w * 16 + n + 1) * 24] = (le_words((y - x) % P_ED) + le_words((y + x) % P_ED)
                                                            + le_words(2 * D_ED * x * y % P_ED))
            acc = ed_add(acc, b)
        for _ in range(4):
            b = ed_add(b, b)
    return tg, tb


def b64_words(words):
    return base64.b64encode(struct.pack("<%dI" % len(words), *words)).decode("ascii")


def js_string(text):
    return json.dumps(text)


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


def committed_digest(path, pattern):
    if not os.path.exists(path):
        return None
    with open(path) as f:
        m = re.search(pattern, f.read())
    return m.group(1) if m else None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--glslc")
    ap.add_argument("--naga")
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    want = digest()
    if args.check:
        stale = 0
        for path, pattern in ((OUT, r'#define CN1G_SOURCES_SHA256 "([0-9a-f]+)"'),
                              (WGSL_OUT, r'CN1_CRYPTO_GPU_SOURCES_SHA256 = "([0-9a-f]+)"')):
            have = committed_digest(path, pattern)
            if have != want:
                print("%s is stale: it was generated from sources with digest %s, the tree has %s. "
                      "Run scripts/crypto-gpu/gen.py." % (os.path.relpath(path, ROOT), have, want))
                stale = 1
        if not stale:
            print("the generated shaders match their sources")
        return stale
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
    naga = args.naga or "naga"
    js = [HEADER.replace("cn1_crypto_gpu_kernels.h --", "vm/ByteCodeTranslator/src/cn1_crypto_gpu_kernels.h\n * --"),
          "/* The WebGPU kernels; port.js loads this file the first time a batch asks for the GPU. */",
          'var CN1_CRYPTO_GPU_SOURCES_SHA256 = "%s";' % want,
          "var CN1_CRYPTO_GPU_WGSL = {"]
    for k in KERNELS:
        js.append("  %s: %s," % (k.lower(), js_string(wgsl(glslc, naga, wgsl_glsl_prelude() + inc, k))))
    js.append("};")
    tg, tb = fixed_base_tables()
    js.append("/* the fixed-base tables, little-endian words in base64: secp256k1's G and Ed25519's B */")
    js.append('var CN1_CRYPTO_GPU_TABLES = { g: "%s", b: "%s" };\n' % (b64_words(tg), b64_words(tb)))
    with open(WGSL_OUT, "w", newline="\n") as f:
        f.write("\n".join(js))
    print("wrote %s" % os.path.relpath(WGSL_OUT, ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
