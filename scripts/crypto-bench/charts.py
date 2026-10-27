#!/usr/bin/env python3
"""Draws the developer guide's crypto benchmark charts from measured runs.

    scripts/crypto-bench/charts.py [results-dir] [out-dir]

results-dir (default scripts/crypto-bench/results) holds the raw output of each run, as the
tools print it:

  jvm.txt         scripts/crypto-bench/jvm-run.sh
  native.txt      scripts/crypto-bench/native/run.sh
  android.txt     scripts/crypto-bench/android-run.sh
  gpu-metal.txt   scripts/crypto-gpu/batch-check.sh 16384
  js-webgpu.txt   scripts/crypto-gpu/js-batch-check.sh

and out-dir (default docs/developer-guide/img) receives crypto-bench-ratios.svg, the core
against its reference library on each target, and crypto-bench-batch.svg, batch verification
throughput from the browser to the native CPU and GPU. Nothing is typed in by hand: a new run
replaces a file here and the charts follow it.
"""
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ROW = re.compile(r"^(\S+)\s+core=\s*([\d.]+)\s+ref=\s*([\d.]+)\s+ratio=\s*([\d.]+)")
FONT = 'font-family="Arial, sans-serif"'
COLORS = ["#2f6fb5", "#e08a1e", "#3a9b5c", "#8a5cc2", "#c4473d"]


def rows(path, section=None):
    """name -> (core, ref, ratio); for android.txt, the rows under one '## ' heading."""
    out = {}
    current = None
    with open(path) as f:
        for line in f:
            if line.startswith("## "):
                current = line[3:].strip()
                continue
            m = ROW.match(line)
            if m and (section is None or current == section):
                out[m.group(1)] = (float(m.group(2)), float(m.group(3)), float(m.group(4)))
    return out


def gpu_rows(path):
    """kernel -> dict of the batch-check gpu-row numbers."""
    out = {}
    pat = re.compile(r"^gpu-row (\S+)\s+gpu/cpu\s+([\d.]+)\s+speedup\s+([\d.]+)\s+crossover (\d+) (\w+)"
                     r"\s+cpu (\d+)/s\s+gpu (\d+)/s")
    with open(path) as f:
        for line in f:
            m = pat.match(line)
            if m:
                out[m.group(1)] = {"ratio": float(m.group(2)), "speedup": float(m.group(3)),
                                   "crossover": int(m.group(4)), "unit": m.group(5),
                                   "cpu": float(m.group(6)), "gpu": float(m.group(7))}
    return out


def js_rows(path):
    out = {}
    with open(path) as f:
        for line in f:
            if line.startswith("CN1-CRYPTO ") and "PerSec" in line:
                f2 = dict(kv.split("=") for kv in line.split()[2:])
                out[line.split()[1]] = {"gpu": float(f2["gpuPerSec"]), "cpu": float(f2["cpuPerSec"])}
    return out


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def svg(width, height, body, title):
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">\n'
            '  <rect x="0" y="0" width="%d" height="%d" fill="#ffffff"/>\n'
            '  <text x="%d" y="24" %s font-size="15" font-weight="bold" text-anchor="middle" fill="#222222">%s</text>\n'
            '%s</svg>\n') % (width, height, width, height, width, height, width // 2, FONT, esc(title), "".join(body))


def legend(names, x, y):
    body = []
    for i, n in enumerate(names):
        body.append('  <rect x="%d" y="%d" width="12" height="12" fill="%s"/>\n' % (x, y + 18 * i, COLORS[i]))
        body.append('  <text x="%d" y="%d" %s font-size="12" fill="#333333">%s</text>\n'
                    % (x + 18, y + 18 * i + 10, FONT, esc(n)))
    return body


def grouped_bars(title, groups, series, values, axis, log=False, ref_line=None, fmt=lambda v: "%.2f" % v, width=760):
    """Horizontal grouped bars: groups down the page, one bar per series in each."""
    left, right, top, bar, gap = 190, 70, 50 + 18 * len(series), 13, 14
    height = top + len(groups) * (bar * len(series) + gap) + 46
    hi = max(v for g in values for v in g if v is not None)
    lo = min(v for g in values for v in g if v is not None)
    plot = width - left - right

    def scale(v):
        if log:
            a, b = math.floor(math.log10(lo)), math.ceil(math.log10(hi))
            return plot * (math.log10(v) - a) / (b - a)
        return plot * v / (hi * 1.08)

    body = legend(series, left, 40)
    y = top
    for gi, g in enumerate(groups):
        body.append('  <text x="%d" y="%d" %s font-size="12" text-anchor="end" fill="#222222">%s</text>\n'
                    % (left - 8, y + bar * len(series) / 2 + 4, FONT, esc(g)))
        for si in range(len(series)):
            v = values[gi][si]
            if v is not None:
                w = max(1, scale(v))
                body.append('  <rect x="%d" y="%d" width="%.1f" height="%d" fill="%s"/>\n'
                            % (left, y, w, bar - 2, COLORS[si]))
                body.append('  <text x="%.1f" y="%d" %s font-size="10" fill="#333333">%s</text>\n'
                            % (left + w + 4, y + bar - 4, FONT, fmt(v)))
            y += bar
        y += gap
    base = y - gap + 6
    body.append('  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#888888"/>\n' % (left, top - 4, left, base))
    if log:
        a, b = math.floor(math.log10(lo)), math.ceil(math.log10(hi))
        for e in range(a, b + 1):
            x = left + scale(10 ** e)
            body.append('  <line x1="%.1f" y1="%d" x2="%.1f" y2="%d" stroke="#dddddd"/>\n' % (x, top - 4, x, base))
            body.append('  <text x="%.1f" y="%d" %s font-size="10" text-anchor="middle" fill="#555555">%s</text>\n'
                        % (x, base + 14, FONT, "{:,}".format(10 ** e)))
    if ref_line is not None:
        x = left + scale(ref_line)
        body.append('  <line x1="%.1f" y1="%d" x2="%.1f" y2="%d" stroke="#c4473d" stroke-dasharray="4 3"/>\n'
                    % (x, top - 4, x, base))
        body.append('  <text x="%.1f" y="%d" %s font-size="10" text-anchor="middle" fill="#c4473d">%s</text>\n'
                    % (x, base + 14, FONT, "parity"))
    body.append('  <text x="%d" y="%d" %s font-size="11" text-anchor="middle" fill="#555555">%s</text>\n'
                % (left + plot // 2, base + 32, FONT, esc(axis)))
    return svg(width, height, body, title)


def main(argv):
    results = argv[1] if len(argv) > 1 else os.path.join(HERE, "results")
    out = argv[2] if len(argv) > 2 else os.path.join(ROOT, "docs", "developer-guide", "img")
    jvm = rows(os.path.join(results, "jvm.txt"))
    native = rows(os.path.join(results, "native.txt"))
    android = rows(os.path.join(results, "android.txt"), "native kernels")
    metal = gpu_rows(os.path.join(results, "gpu-metal.txt"))
    js = js_rows(os.path.join(results, "js-webgpu.txt"))

    ops = [("ECDSA sign", "secp256k1.sign", "secp256k1.sign", "secp256k1.sign"),
           ("ECDSA verify", "secp256k1.verify", "secp256k1.verify", "secp256k1.verify"),
           ("Schnorr verify", "schnorr.verify", "schnorr.verify", "schnorr.verify"),
           ("Ed25519 verify", "ed25519.verify", "ed25519.verify.openssl", "ed25519.verify"),
           ("Keccak-256, 1 KB", "keccak256.1k", "keccak256.1k.openssl", "keccak256.1k"),
           ("SHA-256, 1 KB", "sha256.1k", "sha256.1k.openssl", "sha256.1k"),
           ("BIP-39 seed", "pbkdf2.sha512.bip39", "pbkdf2.sha512.bip39.openssl", "pbkdf2.sha512.bip39")]
    groups, values = [], []
    for label, j, n, a in ops:
        groups.append(label)
        values.append([jvm[j][2] if j in jvm else None, native[n][2] if n in native else None,
                       android[a][2] if a in android else None])
    ratios = grouped_bars("Codename One against the reference library, same machine", groups,
                          ["JVM, against BouncyCastle", "Native, against libsecp256k1 or OpenSSL",
                           "Android native, against BouncyCastle on ART"],
                          values, "times the reference library's throughput", ref_line=1.0, fmt=lambda v: "%.2fx" % v)

    kernels = [("ECDSA", "ecdsa", "secp256k1.verify", "secp256k1.verify"),
               ("Schnorr", "schnorr", "schnorr.verify", "schnorr.verify"),
               ("Ed25519", "ed25519", "ed25519.verify", "ed25519.verify.openssl")]
    groups, values = [], []
    for label, k, j, n in kernels:
        g = metal.get(k)
        groups.append(label)
        values.append([js[k]["cpu"] if k in js else None, js[k]["gpu"] if k in js else None,
                       jvm[j][0] if j in jvm else None, native[n][0] if n in native else None,
                       g["cpu"] * g["speedup"] if g else None])
    batch = grouped_bars("Signatures verified per second, by platform", groups,
                         ["Browser, portable Java", "Browser, WebGPU",
                          "JVM, one core", "Native, one core", "Native, every core and the GPU"],
                         values, "signatures verified per second (log scale)", log=True, fmt=lambda v: "{:,.0f}".format(v),
                         width=780)
    groups, values = [], []
    for label, k in (("ECDSA verify", "ecdsa"), ("Schnorr verify", "schnorr"), ("Ed25519 verify", "ed25519")):
        g = metal.get(k)
        if g:
            groups.append(label)
            values.append([g["cpu"], g["gpu"], g["cpu"] * g["speedup"]])
    gpu = grouped_bars("Splitting a batch between the CPU and the GPU (Metal)", groups,
                       ["CPU, every core", "GPU alone", "Both together"],
                       values, "signatures verified per second", fmt=lambda v: "{:,.0f}".format(v))

    java = rows(os.path.join(results, "android.txt"), "Java engine")
    groups, values = [], []
    for label, k in (("ECDSA sign", "secp256k1.sign"), ("ECDSA verify", "secp256k1.verify"),
                     ("Schnorr verify", "schnorr.verify"), ("Ed25519 sign", "ed25519.sign"),
                     ("Ed25519 verify", "ed25519.verify"), ("Keccak-256, 1 KB", "keccak256.1k"),
                     ("RIPEMD-160, 1 KB", "ripemd160.1k"), ("BIP-39 seed", "pbkdf2.sha512.bip39")):
        groups.append(label)
        values.append([android[k][2] if k in android else None, java[k][2] if k in java else None])
    droid = grouped_bars("Android: both engines against BouncyCastle on ART", groups,
                         ["Native kernels", "Portable Java"],
                         values, "times BouncyCastle's throughput", ref_line=1.0, fmt=lambda v: "%.2fx" % v)

    groups, values = [], []
    for label, k in (("ECDSA verify", "batch.secp256k1.verify"), ("ECDSA verify, packed", "batch.packed.secp256k1.verify"),
                     ("Ed25519 verify", "batch.ed25519.verify.openssl"), ("SHA-256 Merkle level", "batch.sha256.merkle.openssl"),
                     ("Keccak-256 Merkle level", "batch.keccak256.merkle.openssl")):
        if k in native:
            groups.append(label)
            values.append([native[k][2]])
    multi = grouped_bars("A native batch on every core, against one call at a time", groups,
                         ["CryptoBatch, against the reference library in a loop"],
                         values, "times the reference library's throughput", ref_line=1.0, fmt=lambda v: "%.2fx" % v)

    for name, text in (("crypto-bench-ratios.svg", ratios), ("crypto-bench-batch.svg", batch),
                       ("crypto-bench-gpu.svg", gpu), ("crypto-bench-android.svg", droid),
                       ("crypto-bench-multicore.svg", multi)):
        with open(os.path.join(out, name), "w", newline="\n") as f:
            f.write(text)
        print("wrote " + os.path.relpath(os.path.join(out, name), ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
