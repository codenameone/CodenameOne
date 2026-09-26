#!/usr/bin/env python3
"""Turns a native tab-motion probe capture into Codename One's model of it.

The capture comes from scripts/probe-ios-tab-motion.sh (an instrumented
UITabBarController on the simulator, see
scripts/fidelity-app/ios-native-ref/motion-probe/README.md). Subcommands:

  fit LOG... -o templates.json
      Aligns every clean tap-driven selection in the logs and averages the motion
      channels at 60 Hz, the deformation as E(t) + distance * S(t).
  java templates.json TabGlassMotion.java
      Rewrites the tables TabGlassMotion still keeps (the deformation and the
      whole-bar pulse); the other channels are closed forms (MotionForms).
  fixture LOG... -o native-ios27.csv
      Exports the per-frame native channels TabGlassMotionTest checks the Java
      model against.
  check templates.json LOG...
      Replays the model -- closed forms plus the kept tables -- against every tap
      on the vsync clock, prints the worst errors, and fails if a closed form is
      off by more than TabGlassMotionTest allows.
  material SHOT_DIR
      Fits the bar's glass material (GlassRecipe) from the lossless screenshots.

Gestures (holds and drags, see TabGlassGesture):

  gesture --hold HOLD.log --drags LOG... --settle LOG... -o TabGlassGesture.java
      Fits the release wobble (from the quick tap in the hold log) and the scrub
      deformation kernels (from every drag log) and rewrites the tables between
      the "generated tables (tabmotion.py gesture)" markers, then replays the
      settle after a scrub (the tap's travel spring, a closed form) over the
      episodes of the --settle logs -- the ones gesture-fixture exports, whose
      rest positions are the tabs -- and fails if it is off by more than its
      tolerance.
  gesture-fixture LOG... -o native-gestures.csv
      Exports the per-frame native episodes TabGlassGestureTest replays.
  springs --press LOG... --drag LOG
      Fits the bar press spring and the scrub follow spring (the hand-written
      constants at the top of TabGlassGesture).

Vibrancy (the colour matrix of vibrant content, see VibrancyMatrix):

  vibrancy --dark LOG... --light LOG...
      Holds every matrix in PROBE_TINTSWEEP logs to the closed forms VibrancyMatrix
      implements, and fails if any is off by more than the log's precision.
  vibrancy-fixture --dark LOG --light LOG --grey-dark LOG --grey-light LOG -o native-vibrancy.csv
      Exports the measured matrices VibrancyMatrixTest checks the Java against.

Needs numpy and scipy (and Pillow for `material`).
"""
import argparse
import json
import re
import sys

import numpy as np

FPS = 60.0
N_SAMPLES = 100
LIFT_PT = 16.0
REST_LENS_H = 54.0
RELEASE_PT = 3.5
LIFT_SNAP = 0.975

def read_text(path, newline=None):
    with open(path, newline=newline) as f:
        return f.read()


def write_text(path, text, newline=None):
    with open(path, "w", newline=newline) as f:
        f.write(text)


def read_json(path):
    with open(path) as f:
        return json.load(f)


def write_json(obj, path):
    with open(path, "w") as f:
        json.dump(obj, f)


# --------------------------------------------------------------------------- log

LRE = re.compile(r"L (\d+) d=(\d+) (\S+) win=\(([^)]*)\) b=\(([^)]*)\) pos=\(([^)]*)\) "
                 r"T=\(([^)]*)\) op=(\S+) hid=(\d) cr=(\S+) bg=(\S+) f=(.*)")


def load_log(path):
    frames, touches, cur = [], [], None
    for line in read_text(path).splitlines(True):
        if line.startswith("FRAME"):
            cur = {"t": float(line.split()[1][2:]), "L": {}}
            frames.append(cur)
        elif line.startswith("L ") and cur is not None:
            m = LRE.match(line.strip())
            if not m:
                continue
            f = lambda s: [float(x) for x in s.split(",")]
            i = int(m.group(1))
            cur["L"][i] = dict(id=i, d=int(m.group(2)), cls=m.group(3), win=f(m.group(4)), b=f(m.group(5)),
                               pos=f(m.group(6)), T=f(m.group(7)), op=float(m.group(8)), bg=m.group(11))
        elif line.startswith("TOUCH"):
            touches.append({k: float(v) for k, v in (kv.split("=") for kv in line.split()[1:])})
    return frames, touches


def by_class(layers, cls):
    return [l for l in sorted(layers.values(), key=lambda l: l["id"]) if l["cls"] == cls]


def extract_taps(path, dur=2.0):
    """One record per tap: per-frame channels, aligned so t=0 is the last frame at rest."""
    frames, touches = load_log(path)
    downs = [t for t in touches if t["phase"] == 0]
    ups = [t for t in touches if t["phase"] == 3]
    taps = []
    for k, d in enumerate(downs):
        rows = []
        for f in frames:
            if not (d["t"] - 0.02 <= f["t"] < d["t"] + dur):
                continue
            L = f["L"]
            lens = (by_class(L, "_UILiquidLensView") or [None])[0]
            plat = (by_class(L, "_UITabSelectionView") or [None])[0]
            btns = by_class(L, "_UITabButton")
            btn = max(btns, key=lambda l: abs(l["T"][0] - 1)) if btns else None
            bars = by_class(L, "_UITabBarItemPlatterView")
            bar = max(bars, key=lambda l: abs(l["T"][0] - 1)) if bars else None
            glow = (by_class(L, "LittleGlowView") or [None])[0]
            gc = [l for l in L.values() if l["cls"] == "UIView" and l["d"] == 3 and l["bg"] == "(1.000,1.000)"]
            if lens is None:
                continue
            r = dict(t=f["t"], lx=lens["win"][0] + lens["win"][2] / 2, lw=lens["b"][0], lh=lens["b"][1],
                     sx=lens["T"][0], sy=lens["T"][3], tx=lens["T"][4],
                     platOp=plat["op"] if plat else np.nan, cs=btn["T"][0] if btn else np.nan,
                     bs=bar["T"][0] if bar else 1.0, bw=bar["b"][0] if bar else np.nan,
                     gs=glow["T"][0] if glow else np.nan, gop=glow["op"] if glow else 0.0,
                     gcop=gc[0]["op"] if gc else 0.0)
            # Travel from the lens layer's position in its parent: the window frame
            # also moves with the transform's tx, and on the first frame after a
            # re-parent it is briefly reported at the window origin.
            r["px"] = lens["pos"][0]
            rows.append(r)
        if not rows:
            continue
        cols = {key: np.array([r[key] for r in rows], dtype=float) for key in rows[0]}
        a, b = cols["px"][0], cols["px"][-1]
        if abs(b - a) < 1:
            continue
        p = (cols["px"] - a) / (b - a)
        i0 = int(np.argmax(p > 0.001))
        t0 = cols["t"][max(i0 - 1, 0)]
        up = ups[k]["t"] - d["t"] if k < len(ups) else None
        taps.append(dict(src=path, k=k, D=b - a, t=cols["t"] - t0, p=p, c=cols, up=up,
                         dt=np.diff(cols["t"][:12]) * 1000))
    return taps


def is_clean(tap):
    """The taps the model is fitted on: not the first of a run (it lags a frame),
    a quick ~66 ms press (a longer one grows the bar pulse), no frame hitch at the
    start, and the fast lift every 3-tab tap plays (see TabGlassMotion)."""
    if tap["k"] == 0 or tap["up"] is None or tap["up"] > 0.075:
        return False
    if tap["dt"].max() > 25:
        return False
    return tap["c"]["lh"][:18].max() >= REST_LENS_H + LIFT_PT - 0.1


# --------------------------------------------------------------------------- fit

def grid():
    return np.arange(N_SAMPLES) / FPS


def rs(t, v, g):
    return np.interp(g, t, v)


def fit(taps):
    g = grid()
    A = np.array([abs(tp["D"]) for tp in taps])
    X = np.vstack([np.ones_like(A), A]).T

    def lin(f):
        Y = np.array([rs(tp["t"], f(tp), g) for tp in taps])
        c = np.linalg.lstsq(X, Y, rcond=None)[0]
        return c[0], c[1]

    out = {"P": np.mean([rs(tp["t"], tp["p"], g) for tp in taps], 0)}
    out["SXE"], out["SXS"] = lin(lambda tp: tp["c"]["sx"] - 1)
    out["SYE"], out["SYS"] = lin(lambda tp: tp["c"]["sy"] - 1)
    out["TXE"], out["TXS"] = lin(lambda tp: tp["c"]["tx"] * np.sign(tp["D"]))
    pre, fall, gop = [], [], []
    for tp in taps:
        t, L, op = tp["t"], (tp["c"]["lh"] - REST_LENS_H) / LIFT_PT, tp["c"]["platOp"]
        ipk = int(np.argmax(L >= L.max() - 1e-6))
        irel = ipk + int(np.argmax(L[ipk:] < L.max() - 0.01)) - 1
        pre.append(rs(t[:irel + 1], 1 - op[:irel + 1], g[g <= t[irel]]))
        s = t[irel:] - t[irel]
        fall.append(rs(s, L[irel:], g))
        gop.append(rs(s, op[irel:], g))
    m = min(len(x) for x in pre)
    U = np.mean([x[:m] for x in pre], 0)
    while len(U) < N_SAMPLES:
        r = (1 - U[-1]) / (1 - U[-2])
        U = np.append(U, 1 - (1 - U[-1]) * r)
    out["U"] = U
    # a tap whose first falling frame was dropped would shift the curve by a frame
    keep = [i for i, f in enumerate(fall) if abs(f[1] - 0.933) < 0.02]
    out["LF"] = np.mean([fall[i] for i in keep], 0)
    out["GOP"] = np.mean([gop[i] for i in keep], 0)
    out["BAR"] = np.mean([rs(tp["t"], (tp["c"]["bs"] - 1) * tp["c"]["bw"][-1], g) for tp in taps], 0)
    def held(v):
        # The glow layer is removed once it has faded; hold its last scale.
        v = np.array(v, dtype=float)
        last = 1.0
        for i in range(len(v)):
            if np.isnan(v[i]):
                v[i] = last
            else:
                last = v[i]
        return v

    out["GLOWS"] = np.mean([rs(tp["t"], held(tp["c"]["gs"]), g) for tp in taps], 0)
    out["GLOWOP"] = np.mean([rs(tp["t"], tp["c"]["gop"], g) for tp in taps], 0)
    out["GLOWC"] = np.mean([rs(tp["t"], tp["c"]["gcop"], g) for tp in taps], 0)
    return {k: [float(x) for x in v] for k, v in out.items()}


# --------------------------------------------------------------------------- closed forms

def spring_displacement(omega, zeta, x0, v0, t):
    """DampedSpring.displacement: free response of a damped spring resting at 0."""
    if t <= 0:
        return x0
    decay = np.exp(-zeta * omega * t)
    if zeta < 1:
        wd = omega * np.sqrt(1 - zeta * zeta)
        b = (v0 + zeta * omega * x0) / wd
        return decay * (x0 * np.cos(wd * t) + b * np.sin(wd * t))
    if zeta == 1:
        return decay * (x0 + (v0 + omega * x0) * t)
    s = omega * np.sqrt(zeta * zeta - 1)
    r1, r2 = -zeta * omega + s, -zeta * omega - s
    c2 = (v0 - r1 * x0) / (r2 - r1)
    return (x0 - c2) * np.exp(r1 * t) + c2 * np.exp(r2 * t)


def spring_velocity(omega, zeta, x0, v0, t, h=1e-7):
    return (spring_displacement(omega, zeta, x0, v0, t + h) - spring_displacement(omega, zeta, x0, v0, t - h)) / (2 * h)


class MotionForms:
    """The closed forms TabGlassMotion implements (same constants, same names). They
    were found in these captures on the VSYNC clock: every frame renders at exactly
    start + n/60 s, while the logged frame times carry the main thread's jitter."""
    START_S = 0.00226
    POSITION_OMEGA, POSITION_ZETA, SETTLE_PT = 2 * np.pi / 0.4, 0.85, 0.195
    LIFT_OMEGA, LIFT_FALL_S = 2 * np.pi / 0.25, 13.5 / FPS
    PLATTER_OMEGA, PLATTER_RETURN_S = 2 * np.pi / 0.4, 28.5 / FPS
    GLOW_RISE_OMEGA, GLOW_FALL_OMEGA, GLOW_PEAK, GLOW_UP_S = 2 * np.pi / 0.1, 2 * np.pi / 0.5, 0.8445, 4 / FPS
    GLOW_MASK, GLOW_HIDE, GLOW_GROWTH = 0.33675, 0.005, 3.0

    def position(self, t):
        return 1 + spring_displacement(self.POSITION_OMEGA, self.POSITION_ZETA, -1, 0, t - self.START_S)

    def settled_position(self, t, d):
        p = self.position(t)
        peak = self.START_S + np.pi / (self.POSITION_OMEGA * np.sqrt(1 - self.POSITION_ZETA ** 2))
        return 1.0 if t >= peak and d * abs(p - 1) < self.SETTLE_PT else p

    def lift_rise(self, t):
        return 1 + spring_displacement(self.LIFT_OMEGA, 1, -1, 0, t - self.START_S)

    def lift_fall(self, s):
        return 0.0 if s >= self.LIFT_FALL_S else spring_displacement(self.LIFT_OMEGA, 1, 1, 0, s)

    def platter_return(self, s, release):
        if s >= self.PLATTER_RETURN_S:
            return 1.0
        v0 = -spring_velocity(self.LIFT_OMEGA, 1, -1, 0, release - self.START_S)
        return 1 + spring_displacement(self.PLATTER_OMEGA, 1, -self.lift_rise(release), v0, s)

    def glow_layer(self, t):
        if t < self.GLOW_UP_S:
            return self.GLOW_PEAK + spring_displacement(self.GLOW_RISE_OMEGA, 1, -self.GLOW_PEAK, 0, t - self.START_S)
        up = self.GLOW_UP_S - self.START_S
        x0 = self.GLOW_PEAK + spring_displacement(self.GLOW_RISE_OMEGA, 1, -self.GLOW_PEAK, 0, up)
        v0 = spring_velocity(self.GLOW_RISE_OMEGA, 1, -self.GLOW_PEAK, 0, up)
        v = spring_displacement(self.GLOW_FALL_OMEGA, 1, x0, v0, t - self.GLOW_UP_S)
        return 0.0 if v < self.GLOW_HIDE else v

    def glow_opacity(self, t):
        layer = self.glow_layer(t)
        mask = self.GLOW_MASK * layer
        return 0.0 if t >= self.GLOW_UP_S and mask < self.GLOW_HIDE else mask * layer

    def glow_scale(self, t):
        return 1 + self.GLOW_GROWTH * (1 + spring_displacement(self.GLOW_FALL_OMEGA, 1, -1, 0, t - self.GLOW_UP_S))


FORMS = MotionForms()
# What TabGlassMotionTest allows the closed forms (pt, pt, opacity); the glow is
# checked against the raw frames here since the fixture does not carry it.
FORM_TOLERANCE = dict(pos=0.9, lift=0.15, plat=0.01, glow_scale=0.001, glow_op=0.005)


# --------------------------------------------------------------------------- model

class Model:
    def __init__(self, tpl):
        self.T = tpl

    def samp(self, name, t):
        a = self.T[name]
        x = t * FPS
        if x <= 0:
            return a[0]
        i = int(x)
        if i >= len(a) - 1:
            return a[-1]
        f = x - i
        return a[i] * (1 - f) + a[i + 1] * f

    def release(self, D):
        # The first falling frame is the first one within RELEASE_PT of the target,
        # but never before the frame after the lift has snapped to full.
        snap = next(i for i in range(1000) if FORMS.lift_rise(i / FPS) >= LIFT_SNAP)
        for i in range(1, 2 * int(FPS)):
            if abs(D) * (1 - FORMS.position(i / FPS)) < RELEASE_PT:
                return (max(i, snap + 1) - 1) / FPS
        return 2.0

    def frame(self, t, D):
        ad, sg = abs(D), (1 if D >= 0 else -1)
        tr = self.release(D)
        if t < tr:
            u = FORMS.lift_rise(t)
            L, op = (1.0 if u >= LIFT_SNAP else u), 1 - u
        else:
            L, op = FORMS.lift_fall(t - tr), FORMS.platter_return(t - tr, tr)
        return dict(p=FORMS.settled_position(t, ad), L=L, platOp=op, cs=1 + 0.16 * L,
                    sx=1 + self.samp("SXE", t) + ad * self.samp("SXS", t),
                    sy=1 + self.samp("SYE", t) + ad * self.samp("SYS", t),
                    tx=sg * (self.samp("TXE", t) + ad * self.samp("TXS", t)),
                    bar=self.samp("BAR", t))


def check(tpl, taps):
    """Every tap on the vsync clock (round(t * 60) / 60, see MotionForms). The closed
    forms are held to FORM_TOLERANCE on the clean taps -- the ones with the fast
    lift the model describes -- and the run fails if one is exceeded."""
    m = Model(tpl)
    worst = {}
    for tp in taps:
        c, D, err = tp["c"], tp["D"], {}
        rest_w = c["lw"][-1]
        glow_scale = np.array(c["gs"], dtype=float)
        last = 1.0
        for i in range(len(glow_scale)):
            if np.isnan(glow_scale[i]):
                glow_scale[i] = last  # the glow layer is removed once it has faded
            else:
                last = glow_scale[i]
        for i, t in enumerate(tp["t"]):
            if t < 0:
                continue
            t = round(t * FPS) / FPS
            f = m.frame(t, D)
            e = dict(pos=abs(f["p"] - tp["p"][i]) * abs(D),
                     lift=abs(LIFT_PT * f["L"] - (c["lh"][i] - REST_LENS_H)),
                     w=abs((rest_w + LIFT_PT * f["L"]) * f["sx"] - c["lw"][i] * c["sx"][i]),
                     h=abs((REST_LENS_H + LIFT_PT * f["L"]) * f["sy"] - c["lh"][i] * c["sy"][i]),
                     lead=abs(f["tx"] - c["tx"][i]), plat=abs(f["platOp"] - c["platOp"][i]),
                     glow_scale=abs(FORMS.glow_scale(t) - glow_scale[i]),
                     glow_op=abs(FORMS.glow_opacity(t) - c["gop"][i] * c["gcop"][i]))
            for key, v in e.items():
                err[key] = max(err.get(key, 0), v)
        print("%-40s tap %d D=%7.1f clean=%s " % (tp["src"][-40:], tp["k"], D, is_clean(tp))
              + " ".join("%s=%.3f" % kv for kv in err.items()))
        if is_clean(tp):
            for key in FORM_TOLERANCE:
                worst[key] = max(worst.get(key, 0), err[key])
    print("closed forms, worst over the clean taps: "
          + " ".join("%s=%.4f (<=%g)" % (k, worst.get(k, 0), FORM_TOLERANCE[k]) for k in FORM_TOLERANCE))
    bad = [k for k in FORM_TOLERANCE if worst.get(k, 0) > FORM_TOLERANCE[k]]
    if bad:
        sys.exit("closed forms no longer match the capture: " + ", ".join(bad))


# --------------------------------------------------------------------------- java

# The channels TabGlassMotion still keeps as tables; the others are MotionForms.
TABLES = [("SXE", "STRETCH_X", "Lens scaleX - 1, distance-independent part."),
          ("SXS", "STRETCH_X_PER_PT", "Lens scaleX - 1 per point of travel."),
          ("SYE", "STRETCH_Y", "Lens scaleY - 1, distance-independent part."),
          ("SYS", "STRETCH_Y_PER_PT", "Lens scaleY - 1 per point of travel."),
          ("TXE", "LEAD_PT", "Lens centre lead in points along the travel direction, distance-independent part."),
          ("TXS", "LEAD_PER_PT", "Lens centre lead per point of travel."),
          ("BAR", "BAR_GROW_PT", "Whole-bar width growth in points; the bar scales uniformly by (w + grow) / w.")]
REST = {"SXE": 0, "SXS": 0, "SYE": 0, "SYS": 0, "TXE": 0, "TXS": 0, "BAR": 0}
PER_PT = ("SXS", "SYS", "TXS")


def java_tables(tpl):
    out = []
    for key, name, doc in TABLES:
        v, r = list(tpl[key]), REST[key]
        # per-point tables are multiplied by up to ~260 pt of travel
        thr = 1e-6 if key in PER_PT else 2e-4
        last = max([i for i, x in enumerate(v) if abs(x - r) > thr] + [0]) + 1
        v = v[:last + 1]
        v[-1] = r
        dec = 7 if key in PER_PT else 4
        vals = [("%." + str(dec) + "f") % x + "f" for x in v]
        vals = [s.replace("-0." + "0" * dec + "f", "0." + "0" * dec + "f") for s in vals]
        lines, cur = [], "           "
        for s in vals:
            if len(cur) + len(s) + 2 > 100:
                lines.append(cur.rstrip())
                cur = "           "
            cur += " " + s + ","
        lines.append(cur.rstrip().rstrip(","))
        out.append("    /// %s\n    private static final float[] %s = {\n%s\n    };\n" % (doc, name, "\n".join(lines)))
    return "\n".join(out) + "\n"


def write_java(tpl, path):
    s = read_text(path)
    a = s.index("    /// " + TABLES[0][2])
    b = s.index("    // ---- outputs ----")
    write_text(path, s[:a] + java_tables(tpl) + s[b:])


# --------------------------------------------------------------------------- fixture

def fixture(taps, path):
    rows = ["# Native iOS 27 UITabBarController selection motion, one row per display frame.",
            "# Captured by scripts/probe-ios-tab-motion.sh and exported by",
            "# scripts/fidelity-app/tools/tab-motion/tabmotion.py fixture.",
            "# set,tap,travelPt,liftRegime,tSec,position,liftPt,scaleX,scaleY,leadPt,platterOpacity,contentScale,barGrowPt",
            "# Every tap is a quick ~66 ms touch: a longer press grows the bar pulse further.",
            "# liftRegime: fast = the lift curve every 3-tab tap uses; slow = the slower rise UIKit",
            "# sometimes plays on the 5-tab bar (position/deformation still hold, lift is not asserted)."]
    for tp in taps:
        c = tp["c"]
        regime = "fast" if c["lh"][:18].max() >= REST_LENS_H + LIFT_PT - 0.1 else "slow"
        name = re.sub(r"\.log$", "", tp["src"].split("/")[-1])
        for i, t in enumerate(tp["t"]):
            if t < 0 or t > 1.7:
                continue
            rows.append("%s,%d,%.2f,%s,%.4f,%.5f,%.3f,%.4f,%.4f,%.3f,%.4f,%.4f,%.3f" % (
                name, tp["k"], tp["D"], regime, t, tp["p"][i], c["lh"][i] - REST_LENS_H, c["sx"][i], c["sy"][i],
                c["tx"][i], c["platOp"][i], c["cs"][i], (c["bs"][i] - 1) * c["bw"][-1]))
    write_text(path, "\n".join(rows) + "\n")


# --------------------------------------------------------------------------- material

def material(shot_dir):
    from PIL import Image
    from scipy.ndimage import gaussian_filter
    from scipy.optimize import least_squares
    import os
    backdrop_png = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources",
                                "glass-backdrop.png")

    def backdrop(kind, F):
        H, W, _ = F.shape
        s = W / 393.0
        if kind == "photo":
            with Image.open(backdrop_png) as im:
                return np.array(im.convert("RGB").resize((W, H), Image.BICUBIC)).astype(float)
        if kind == "grey":
            return np.full_like(F, 128.0)
        B = np.zeros_like(F)
        for k in range(0, int(852 / 23) + 1):
            y0, y1 = int(round(23 * k * s)), int(round(23 * (k + 1) * s))
            yc = min(H - 1, (y0 + y1) // 2)
            B[y0:y1] = np.median(F[yc - 5:yc + 5, int(6 * s):int(18 * s)].reshape(-1, 3), 0)
        for x in np.arange(0, 393, 20):
            B[:, int(round(x * s)):int(round((x + 1) * s))] = F[int(705 * s), int(round(x * s)) + 1]
        return B

    for appearance in ("light", "dark"):
        data = []
        # The flat grey pins the transfer's absolute level exactly (the native bar
        # and the committed golden agree there to the level); the bands pin the
        # blur and the photo the colour response.
        for kind in ("stripes", "photo", "grey"):
            with Image.open(os.path.join(shot_dir, "live-%s-%s.png" % (appearance, kind))) as im:
                F = np.array(im.convert("RGB")).astype(float)
            s = F.shape[1] / 393.0
            regs = [(slice(int(781 * s), int(823 * s)), slice(int(x0 * s), int(x1 * s)))
                    for x0, x1 in ((236, 254), (298, 324))]
            data.append((F, backdrop(kind, F), regs))
        best = None
        for sig in (16, 24, 32, 40, 48):
            M, Y = [], []
            for F, B, regs in data:
                Bb = np.stack([gaussian_filter(B[..., c], sig) for c in range(3)], -1)
                M += [Bb[r].reshape(-1, 3) for r in regs]
                Y += [F[r].reshape(-1, 3) for r in regs]
            M, Y = np.concatenate(M), np.concatenate(Y)
            lum = (M @ [0.2126, 0.7152, 0.0722])[:, None]
            res = least_squares(lambda p: (np.clip((lum + (M - lum) * p[0]) * p[1] + p[2], 0, 255) - Y).ravel(),
                                [1.8, 0.5, 60])
            rms = float(np.sqrt(np.mean(res.fun ** 2)))
            if best is None or rms < best[0]:
                best = (rms, sig, res.x)
        # Re-anchor the offset so flat grey lands exactly on the native level: that
        # one level is measured directly, the rest of the fit is a compromise.
        F = data[2][0]
        s = F.shape[1] / 393.0
        grey_level = F[int(795 * s):int(800 * s), int(240 * s):int(250 * s)].mean()
        anchored = grey_level - 128.0 * best[2][1]
        print("%s: blur %d px, sat %.3f scale %.3f offset %.1f (rms %.2f/255); grey-anchored offset %.1f"
              % (appearance, best[1], best[2][0], best[2][1], best[2][2], best[0], anchored))


# --------------------------------------------------------------------------- gestures

# The springs TabGlassGesture runs (see `springs`): the lens follows the finger on
# FOLLOW, the whole bar grows on the press spring, both TOUCH_LAG behind the touch.
FOLLOW_OMEGA = 32.0
FOLLOW_ZETA = 0.903
TOUCH_LAG = 0.02
# The release wobble starts WOBBLE_START after touch-up; WOBBLE_N frames are fitted.
WOBBLE_START = 0.90
WOBBLE_N = 45
# Scrub deformation kernels: FIR_TAPS frames of delay, ridge-regularized.
FIR_TAPS = 60
FIR_RIDGE = 1e-2
# The settle after a scrub is a closed form (the tap's travel spring, FORMS, started
# from the scrub's state); `gesture` replays it exactly as TabGlassGesture does over
# the fixture's scrubs. The tolerance is what it reaches on the committed captures,
# plus 10%.
SETTLE_TOLERANCE = {"rms": 0.38, "max": 3.8}
STEP = 1 / 240.
GESTURE_BEGIN = "    // ---- generated tables (tabmotion.py gesture) ----\n"
TABLES_END = "    // ---- end of generated tables ----\n"


def lens_series(path):
    """The whole log as one per-frame array (every frame with a lens), and its touches.

    Columns: t, lens x (position in its parent), lift pt, lens width, scaleX, scaleY,
    tx, platter opacity, bar scale, content scale."""
    frames, touches = load_log(path)
    rows = []
    for f in frames:
        L = f["L"]
        lens = (by_class(L, "_UILiquidLensView") or [None])[0]
        plat = (by_class(L, "_UITabSelectionView") or [None])[0]
        bars = by_class(L, "_UITabBarItemPlatterView")
        bar = max(bars, key=lambda l: abs(l["T"][0] - 1)) if bars else None
        btns = by_class(L, "_UITabButton")
        btn = max(btns, key=lambda l: abs(l["T"][0] - 1)) if btns else None
        if not lens:
            continue
        rows.append((f["t"], lens["pos"][0], lens["b"][1] - REST_LENS_H, lens["b"][0], lens["T"][0], lens["T"][3],
                     lens["T"][4], plat["op"] if plat else np.nan, bar["T"][0] if bar else 1,
                     btn["T"][0] if btn else 1))
    return np.array(rows), touches


def presses(touches):
    """(down, up) touch records of every press, in order."""
    downs = [t for t in touches if t["phase"] == 0]
    ups = [t for t in touches if t["phase"] == 3]
    return list(zip(downs, ups))


def is_scrub(touches, d, u):
    return any(t["phase"] == 1 and d["t"] < t["t"] < u["t"] for t in touches)


class Finger:
    """The finger's x at any time: the last touch event at or before it."""

    def __init__(self, touches):
        self.t = np.array([q["t"] for q in touches])
        self.x = np.array([q["x"] for q in touches])

    def __call__(self, t):
        i = int(np.searchsorted(self.t, t, side="right")) - 1
        return None if i < 0 else self.x[i]


def spring(times, target, omega, zeta, x0=0.0, v0=0.0, dt=1 / 600.):
    """x'' = omega^2 (target(t) - x) - 2 zeta omega x', stepped at dt, sampled at times."""
    tt = np.arange(times[0], times[-1] + dt, dt)
    xs = np.empty(len(tt))
    x, v = x0, v0
    for i, t in enumerate(tt):
        v += (omega * omega * (target(t) - x) - 2 * zeta * omega * v) * dt
        x += v * dt
        xs[i] = x
    return np.interp(times, tt, xs)


def tab_centres(t, x):
    """Lens positions held for 0.3 s (the tabs it rested on), near-duplicates merged."""
    rest = []
    for i in range(len(t)):
        j = np.searchsorted(t, t[i] + 0.3)
        if j < len(t) and np.ptp(x[i:j]) < 0.05:
            rest.append(x[i])
    cc = []
    for c in sorted(rest):
        if not cc or c - cc[-1] > 5:
            cc.append(c)
    return cc


def finger_offset(path):
    """Finger (window x) minus lens target (lens-parent x), fitted with the follow
    spring over every press of a drag log, and rounded to 0.01 pt. None for a log
    with no drag."""
    from scipy.optimize import least_squares
    R, T = lens_series(path)
    t, x = R[:, 0], R[:, 1]
    finger = Finger(T)
    ps = [(d["t"], u["t"]) for d, u in presses(T) if is_scrub(T, d, u)]
    if not ps:
        return None

    def resid(p):
        off, out = p[0], []
        for d, u in ps:
            m = (t >= d) & (t <= u + 0.02)
            if m.sum() < 5:
                continue
            x0 = x[m][0]
            out.append(spring(t[m], lambda tt: (finger(tt - TOUCH_LAG) - off) if tt - TOUCH_LAG >= d else x0,
                              FOLLOW_OMEGA, FOLLOW_ZETA, x0=x0) - x[m])
        return np.concatenate(out)

    # start from the finger-to-lens gap once each press has settled on the finger
    guess = np.mean([finger(u) - np.interp(u, t, x) for d, u in ps])
    r = least_squares(resid, [guess])
    off = round(float(r.x[0]), 2)
    print("%s: finger offset %.2f pt (follow rms %.2f, max %.2f pt)"
          % (path, off, np.sqrt(np.mean(r.fun ** 2)), np.abs(r.fun).max()), file=sys.stderr)
    return off


def wobble(hold_log):
    """The release wobble, read off the quickest press of the hold log (a tap on the
    already selected tab, so nothing else moves), from WOBBLE_START after touch-up."""
    R, T = lens_series(hold_log)
    t = R[:, 0]
    ps = presses(T)
    k = int(np.argmin([u["t"] - d["t"] for d, u in ps]))
    G = WOBBLE_START + np.arange(WOBBLE_N) / FPS

    def at(u):
        return {n: np.round(np.interp(u + G, t, R[:, j] - (0 if n == "tx" else 1)), 5)
                for j, n in ((4, "sx"), (5, "sy"), (6, "tx"))}

    W = at(ps[k][1]["t"])
    for i, (d, u) in enumerate(ps):
        if i != k:
            o = at(u["t"])
            print("wobble after the %.0f ms press vs the %.0f ms tap: max diff sx %.4f sy %.4f tx %.3f pt"
                  % ((u["t"] - d["t"]) * 1000, (ps[k][1]["t"] - ps[k][0]["t"]) * 1000,
                     *(np.abs(o[n] - W[n]).max() for n in ("sx", "sy", "tx"))), file=sys.stderr)
    return W


def fixture_episodes(lines):
    """The E/T/F rows of a gesture fixture as episodes (what TabGlassGestureTest loads)."""
    eps = {}
    for line in lines:
        p = line.split(",")
        i = int(p[1])
        if p[0] == "E":
            eps[i] = dict(tag=p[2], fromPt=float(p[3]), toPt=float(p[4]), minPt=float(p[5]), maxPt=float(p[6]),
                          upS=float(p[7]), scrubS=float(p[8]), settlePt=float(p[9]), T=[], F=[])
        else:
            eps[i]["T" if p[0] == "T" else "F"].append([float(x) for x in p[2:]])
    return eps


def scrub_centre(e, end):
    """TabGlassGesture.scrubTrack: the lens centre at 60 Hz from the scrub start -- the
    follow spring (stepped like the Java) until the release reaches the lens, then
    the settle spring from that state, in closed form."""
    touches = e["T"]

    def target(s):
        f = None
        for q in touches:
            if q[0] <= s - TOUCH_LAG:
                f = q[1]
            else:
                break
        return e["fromPt"] if f is None else min(max(f, e["minPt"]), e["maxPt"])

    w, z = FOLLOW_OMEGA, FOLLOW_ZETA
    handoff = e["upS"] + TOUCH_LAG
    frames = max(1, int(np.ceil((end - e["scrubS"]) * FPS)) + 2)
    out = np.empty(frames)
    x, v, s, settling = e["fromPt"], 0.0, e["scrubS"], False
    for i in range(frames):
        fs = e["scrubS"] + i / FPS
        if fs >= handoff:
            if not settling:
                while s < handoff:
                    v += (w * w * (target(s) - x) - 2 * z * w * v) * STEP
                    x += v * STEP
                    s += STEP
                settling = True
            out[i] = e["settlePt"] + spring_displacement(FORMS.POSITION_OMEGA, FORMS.POSITION_ZETA,
                                                         x - e["settlePt"], v, fs - handoff)
            continue
        for k in range(int(round(1 / (FPS * STEP)))):
            if s >= fs:
                break
            v += (w * w * (target(s) - x) - 2 * z * w * v) * STEP
            x += v * STEP
            s += STEP
        out[i] = x
    return out


def settle_check(lines):
    """The lens centre of every scrub in the fixture rows, replayed like
    TabGlassGesture.at, against the captured frames."""
    errs = []
    for e in fixture_episodes(lines).values():
        if e["scrubS"] < 0:
            continue
        F = np.array([f for f in e["F"] if f[0] >= 0])
        tr = scrub_centre(e, F[-1, 0] + 2 / FPS)
        for f in F:
            if f[0] < e["scrubS"]:
                centre = e["fromPt"]
            else:
                fx = (f[0] - e["scrubS"]) * FPS
                n = min(int(fx), len(tr) - 2)
                centre = tr[n] + (tr[n + 1] - tr[n]) * (fx - n)
            errs.append(centre - f[1])
    errs = np.array(errs)
    rms, worst = np.sqrt(np.mean(errs ** 2)), np.abs(errs).max()
    print("settle (tap spring %.3f rad/s, zeta %.3f): %d frames, rms %.3f pt, max %.2f pt (tolerance %.2f / %.1f)"
          % (FORMS.POSITION_OMEGA, FORMS.POSITION_ZETA, len(errs), rms, worst, SETTLE_TOLERANCE["rms"],
             SETTLE_TOLERANCE["max"]), file=sys.stderr)
    return rms <= SETTLE_TOLERANCE["rms"] and worst <= SETTLE_TOLERANCE["max"]


def scrub_kernels(drag_logs, W):
    """The lens deformation while scrubbing, as FIR kernels over its own speed and
    acceleration (the release wobble W taken out first). Every press of the drag logs
    is used except one that starts on another tab (it plays a tap first)."""
    WG = WOBBLE_START + np.arange(WOBBLE_N) / FPS
    eps = []
    for path in drag_logs:
        R, T = lens_series(path)
        t = R[:, 0]
        for d, u in presses(T):
            d, u = d["t"], u["t"]
            end = u + 0.85
            g = np.arange(d - 0.03, end, 1 / 240.)
            m = (t >= d - 0.2) & (t <= end + 0.3)
            X = np.interp(g, t[m], R[m, 1])
            early = (g >= d) & (g <= d + 0.25)
            if np.ptp(X[early]) > 3:
                print("scrub: skip %s %.0f ms press (tap phase first)" % (path, (u - d) * 1000), file=sys.stderr)
                continue
            G = np.arange(g[0], g[-1], 1 / FPS)
            ys = {}
            for j, n in ((4, "sx"), (5, "sy"), (6, "tx")):
                y = np.interp(G, g, np.interp(g, t[m], R[m, j])) - (0 if n == "tx" else 1)
                ys[n] = y - np.interp(G - u, WG, W[n], left=0, right=0)
            eps.append((path, np.interp(G, g, X), ys))

    def design(x, names):
        V = np.gradient(x, 1 / FPS)
        A = np.gradient(V, 1 / FPS)
        F = {"|v|": np.abs(V) / 1000, "a*s": A * np.sign(V) / 10000, "v": V / 1000, "a": A / 10000}
        cols = []
        for n in names:
            f = F[n]
            for k in range(FIR_TAPS):
                cols.append(np.r_[np.zeros(min(k, len(f))), f[:max(len(f) - k, 0)]])
        return np.column_stack(cols)

    def ridge(D, Y):
        X, Y = np.vstack(D), np.concatenate(Y)
        return np.linalg.solve(X.T @ X + FIR_RIDGE * np.eye(X.shape[1]), X.T @ Y)

    out = {}
    for n, names in (("sx", ["|v|", "a*s"]), ("sy", ["|v|", "a*s"]), ("tx", ["v", "a"])):
        Ds = [design(x, names) for p, x, ys in eps]
        Ys = [ys[n] for p, x, ys in eps]
        worst = []
        for log in drag_logs:
            tr = [i for i, e in enumerate(eps) if e[0] != log]
            te = [i for i, e in enumerate(eps) if e[0] == log]
            if not te:
                continue
            h = ridge([Ds[i] for i in tr], [Ys[i] for i in tr])
            worst.append(max(np.abs(Ds[i] @ h - Ys[i]).max() for i in te))
        h = ridge(Ds, Ys)
        print("scrub %s: %d episodes, leave-one-log-out max error per log %s (peak %.3f)"
              % (n, len(eps), np.round(worst, 3), max(np.abs(y).max() for y in Ys)), file=sys.stderr)
        out[n] = (h[:FIR_TAPS], h[FIR_TAPS:])
    return out


def java_array(name, doc, vals, dec, private=False):
    """A float[] table, 7 values per line, trailing values that print as zero dropped."""
    s = [("%." + str(dec) + "f") % v for v in vals]
    s = [("0." + "0" * dec) if v == "-0." + "0" * dec else v for v in s]
    while len(s) > 1 and float(s[-1]) == 0:
        s.pop()
    s = [v + "f" for v in s]
    lines = ["            " + ", ".join(s[i:i + 7]) + "," for i in range(0, len(s), 7)]
    lines[-1] = lines[-1].rstrip(",")
    return "    /// %s\n    %sstatic final float[] %s = {\n%s\n    };\n" % (
        doc, "private " if private else "", name, "\n".join(lines))


FIR_NOTE = """    // The deformation kernels below are free 60-tap fits. Folded onto velocity
    // alone each channel is one smooth kernel, scaleY's that of scaleX inverted and
    // about 1.5 times larger. Two damped modes shared by all channels (omega_n 6.0
    // and 15.5 rad/s, zeta 0.38 and 0.42) match their RMS but not their worst frame.
"""


def gesture_tables(W, K):
    w = [("WOBBLE_X", "Release wobble, lens scaleX - 1, from WOBBLE_START_S after touch-up.", W["sx"], 5),
         ("WOBBLE_Y", "Release wobble, lens scaleY - 1.", W["sy"], 5),
         ("WOBBLE_TX", "Release wobble, lens centre offset in points (always towards +x).", W["tx"], 4)]
    k = [("FIR_SX_SPEED", "Scrub deformation kernels: scaleX - 1 per |v|/1000 pt/s, per frame of delay.",
          K["sx"][0], 6),
         ("FIR_SX_ACCEL", "scaleX - 1 per (a * sign(v))/10000 pt/s^2.", K["sx"][1], 6),
         ("FIR_SY_SPEED", "scaleY - 1 per |v|/1000 pt/s.", K["sy"][0], 6),
         ("FIR_SY_ACCEL", "scaleY - 1 per (a * sign(v))/10000 pt/s^2.", K["sy"][1], 6),
         ("FIR_TX_VEL", "Centre offset (pt) per v/1000 pt/s.", K["tx"][0], 5),
         ("FIR_TX_ACCEL", "Centre offset (pt) per a/10000 pt/s^2.", K["tx"][1], 5)]
    return ("\n".join(java_array(*x) for x in w) + "\n" + FIR_NOTE
            + "\n".join(java_array(*x) for x in k) + "\n")


def splice(path, begin, block):
    """Replaces what lies between the begin marker line and the end marker line."""
    s = read_text(path, newline="")
    if begin not in s:
        sys.exit("%s has no line %r: add the markers around the generated tables first" % (path, begin.strip()))
    a = s.index(begin) + len(begin)
    b = s.index(TABLES_END, a)
    write_text(path, s[:a] + block + s[b:], newline="")


def gesture_rows(logs):
    """One E(pisode) row per press, its T(ouch) rows (scrubs only) and F(rame) rows."""
    out, eid = [], 0
    for log in logs:
        off = finger_offset(log)
        tag = re.sub(r"-(light|dark)$", "", re.sub(r"\.log$", "", log.split("/")[-1]))
        fr, T = load_log(log)
        rows = []
        for f in fr:
            L = f["L"]
            lens = (by_class(L, "_UILiquidLensView") or [None])[0]
            bars = by_class(L, "_UITabBarItemPlatterView")
            bar = max(bars, key=lambda l: abs(l["T"][0] - 1)) if bars else None
            if lens:
                rows.append((f["t"], lens["pos"][0], (lens["b"][1] - REST_LENS_H) / LIFT_PT, lens["T"][0] - 1,
                             lens["T"][3] - 1, lens["T"][4], (bar["T"][0] - 1) * bar["b"][0] if bar else 0))
        R = np.array(rows)
        cc = tab_centres(R[:, 0], R[:, 1])
        for d, u in presses(T):
            t0 = d["t"]
            seg = R[(R[:, 0] >= t0 - 0.02) & (R[:, 0] <= u["t"] + 1.7)]
            x0 = np.interp(t0, R[:, 0], R[:, 1])
            moves = [x for x in T if x["phase"] == 1 and t0 < x["t"] < u["t"]]
            if moves:
                early = (seg[:, 0] >= t0) & (seg[:, 0] <= t0 + 0.25)
                if np.ptp(seg[early, 1]) > 3:
                    continue  # started on another tab: it plays a tap first
                from_pt = to_pt = x0
                fing = [x for x in T if t0 <= x["t"] <= u["t"]]
                fx = u["x"] - off
                settle_pt = min(cc, key=lambda c: abs(c - fx))
                scrub_start = moves[0]["t"] - t0
            else:
                from_pt, to_pt, fing, scrub_start = x0, seg[-1, 1], [], -1
                settle_pt = to_pt
            out.append("E,%d,%s,%.3f,%.3f,%.3f,%.3f,%.4f,%.4f,%.3f" % (
                eid, tag, from_pt, to_pt, min(cc), max(cc), u["t"] - t0, scrub_start, settle_pt))
            for q in fing:
                out.append("T,%d,%.4f,%.3f" % (eid, q["t"] - t0, q["x"] - off))
            for r in seg:
                out.append("F,%d,%.4f,%.3f,%.4f,%.5f,%.5f,%.4f,%.3f" % (eid, r[0] - t0, *r[1:]))
            eid += 1
    return out


def gesture_fixture(logs, path):
    out = gesture_rows(logs)
    write_text(path, "# kind,episode,... see TabGlassGestureTest\n" + "\n".join(out) + "\n")
    print("%d episodes, %d lines" % (sum(1 for r in out if r.startswith("E,")), len(out)), file=sys.stderr)


def springs(press_logs, drag_log):
    from scipy.optimize import least_squares
    E = []
    for path in press_logs:
        fr, T = load_log(path)
        rows = []
        for f in fr:
            bars = by_class(f["L"], "_UITabBarItemPlatterView")
            if bars:
                b = max(bars, key=lambda l: abs(l["T"][0] - 1))
                rows.append((f["t"], (b["T"][0] - 1) * b["b"][0]))
        R = np.array(rows)
        for d, u in presses(T):
            d, u = d["t"], u["t"]
            m = (R[:, 0] >= d - 0.05) & (R[:, 0] < u + 0.8)
            if m.sum() > 10 and (u - d) < 3.5:
                E.append((R[m, 0], R[m, 1], d, u))

    def bar_resid(p):
        w, z, amp, lag = p
        return np.concatenate([spring(t, lambda tt: amp if d + lag <= tt < u + lag else 0.0, w, z) - g
                               for t, g, d, u in E])

    r = least_squares(bar_resid, [18, 0.6, 14, 0.02], bounds=([1, 0.05, 1, -0.1], [200, 3, 40, 0.2]))
    print("bar press spring (%d presses): omega %.2f zeta %.3f press growth %.2f pt lag %.3f s, rms %.3f pt"
          % (len(E), *r.x, np.sqrt(np.mean(r.fun ** 2))))
    R, T = lens_series(drag_log)
    t, x = R[:, 0], R[:, 1]
    finger = Finger(T)
    eps = []
    for d, u in presses(T):
        m = (t >= d["t"] - 0.05) & (t <= u["t"] + 0.02)
        eps.append((t[m], x[m], d["t"]))

    def follow_resid(p):
        w, z, lag, off = p
        out = []
        for tm, xm, d in eps:
            def tf(tt, x0=xm[0], d=d):
                v = finger(tt - lag)
                return x0 if v is None or tt - lag < d else v - off
            out.append(spring(tm, tf, w, z, x0=xm[0]) - xm)
        return np.concatenate(out)

    r = least_squares(follow_resid, [25, 0.8, 0.02, 59.67], bounds=([2, 0.1, -0.05, 40], [300, 3, 0.2, 80]))
    print("scrub follow spring: omega %.2f zeta %.3f lag %.3f s finger offset %.2f pt, rms %.2f max %.2f pt"
          % (*r.x, np.sqrt(np.mean(r.fun ** 2)), np.abs(r.fun).max()))


# --------------------------------------------------------------------------- vibrancy

# The closed forms VibrancyMatrix implements; `vibrancy` holds every logged matrix to
# them. Structure and constants were found from these captures (see VibrancyMatrix).
VIB_TOLERANCE = 2e-5


def dark_matrix(t):
    mx, mn = t.max(), t.min()
    e = 0.5 * mn / mx
    return (0.5 - e) * np.outer(np.ones(3), t) / (t @ t) + e * np.eye(3), t.copy()


def light_matrix(t):
    mx, mn = t.max(), t.min()
    e = 0.5 * mn / mx
    d = 1 - t
    c = 0.3 * (0.5 - e) / (d @ d)
    alpha = 1 - e - c * d.sum()
    return c * np.outer(1 + t, d) + e * np.eye(3), alpha * (1 + t) - 1 + e * t


def grey_matrix(g):
    q = 1 + 0.05 * g * (1 - g)
    return 5.0 / 16 / q * np.eye(3), np.full(3, (19.0 / 16 * g - 0.25) / q)


def load_tints(path):
    """(hex, tint 0..1, distinct 4x5 matrices) per TINT line of a PROBE_TINTSWEEP log.
    The first matrix is the selected item's."""
    out = []
    for line in read_text(path).splitlines(True):
        if not line.startswith("TINT "):
            continue
        parts = line.split()
        h = parts[1]
        t = np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)]) / 255.
        mats = []
        for p in parts[2:]:
            m = re.match(r"(\w+):M\[([^\]]*)\]", p)
            if m:
                M = np.array([float(x) for x in m.group(2).split(",")]).reshape(4, 5)
                if not any(np.allclose(M, U) for U in mats):
                    mats.append(M)
        out.append((h, t, mats))
    return out


def vibrancy_check(dark_logs, light_logs):
    """Worst matrix-entry error of the closed forms against every logged tint. A
    tint's item layers carry several matrices (selected and unselected items), so
    each tint is scored against the one that matches best -- the selected item's."""
    worst = 0.0
    for appearance, logs, colour in (("dark", dark_logs, dark_matrix), ("light", light_logs, light_matrix)):
        n = 0
        for log in logs:
            for h, t, mats in load_tints(log):
                if not mats:
                    continue
                W, off = grey_matrix(t[0]) if t.max() == t.min() else colour(t)
                err = min(max(np.abs(M[:3, :3] - W).max(), np.abs(M[:3, 4] - off).max()) for M in mats)
                if err > VIB_TOLERANCE:
                    print("%s %s %s: off by %.2e" % (log, appearance, h, err), file=sys.stderr)
                worst = max(worst, err)
                n += 1
        print("%s: %d tints" % (appearance, n), file=sys.stderr)
    print("worst entry error %.2e (tolerance %.0e)" % (worst, VIB_TOLERANCE), file=sys.stderr)
    if worst > VIB_TOLERANCE:
        sys.exit("the vibrancy closed forms no longer match the capture")


def vibrancy_fixture(dark, light, grey_dark, grey_light, path, count=300):
    rows = ["# Native iOS 27 vibrancy matrices read from UIKit (vibrantColorMatrix inputColorMatrix)",
            "# for the selected tab-bar item, by tint. Captured with the motion probe (PROBE_TINTSWEEP).",
            "# appearance,tint,m00,m01,m02,off0,m10,m11,m12,off1,m20,m21,m22,off2"]

    def row(app, h, M):
        return "%s,%s," % (app, h) + ",".join("%.5f,%.5f,%.5f,%.5f" % (M[r, 0], M[r, 1], M[r, 2], M[r, 4])
                                               for r in range(3))
    for app, log in (("dark", dark), ("light", light)):
        rows += [row(app, h, mats[0]) for h, t, mats in load_tints(log)[:count]]
    for app, log in (("dark", grey_dark), ("light", grey_light)):
        rows += [row(app, h, mats[0]) for h, t, mats in load_tints(log) if t.max() == t.min()]
    write_text(path, "\n".join(rows) + "\n")


# --------------------------------------------------------------------------- cli

def all_taps(logs):
    taps = []
    for log in logs:
        taps += extract_taps(log)
    return taps


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("fit")
    a.add_argument("logs", nargs="+")
    a.add_argument("-o", required=True)
    a = sub.add_parser("java")
    a.add_argument("templates")
    a.add_argument("java")
    a = sub.add_parser("fixture")
    a.add_argument("logs", nargs="+")
    a.add_argument("-o", required=True)
    a = sub.add_parser("check")
    a.add_argument("templates")
    a.add_argument("logs", nargs="+")
    a = sub.add_parser("material")
    a.add_argument("shots")
    a = sub.add_parser("gesture")
    a.add_argument("--hold", required=True)
    a.add_argument("--drags", nargs="+", required=True)
    a.add_argument("--settle", nargs="+", required=True, help="the logs gesture-fixture exports")
    a.add_argument("-o", required=True)
    a = sub.add_parser("gesture-fixture")
    a.add_argument("logs", nargs="+")
    a.add_argument("-o", required=True)
    a = sub.add_parser("springs")
    a.add_argument("--press", nargs="+", required=True)
    a.add_argument("--drag", required=True)
    a = sub.add_parser("vibrancy")
    a.add_argument("--dark", nargs="+", required=True)
    a.add_argument("--light", nargs="+", required=True)
    a = sub.add_parser("vibrancy-fixture")
    for k in ("--dark", "--light", "--grey-dark", "--grey-light", "-o"):
        a.add_argument(k, required=True)
    args = ap.parse_args()
    if args.cmd == "fit":
        clean = [tp for tp in all_taps(args.logs) if is_clean(tp)]
        print("fitting %d clean taps" % len(clean), file=sys.stderr)
        write_json(fit(clean), args.o)
    elif args.cmd == "java":
        write_java(read_json(args.templates), args.java)
    elif args.cmd == "fixture":
        taps = [tp for tp in all_taps(args.logs) if tp["k"] != 0 and tp["up"] is not None and tp["up"] <= 0.075
                and tp["dt"].max() <= 25]
        fixture(taps, args.o)
    elif args.cmd == "check":
        check(read_json(args.templates), all_taps(args.logs))
    elif args.cmd == "material":
        material(args.shots)
    elif args.cmd == "gesture":
        W = wobble(args.hold)
        splice(args.o, GESTURE_BEGIN, gesture_tables(W, scrub_kernels(args.drags, W)))
        if not settle_check(gesture_rows(args.settle)):
            sys.exit("the settle after a scrub no longer matches the capture")
    elif args.cmd == "gesture-fixture":
        gesture_fixture(args.logs, args.o)
    elif args.cmd == "springs":
        springs(args.press, args.drag)
    elif args.cmd == "vibrancy":
        vibrancy_check(args.dark, args.light)
    elif args.cmd == "vibrancy-fixture":
        vibrancy_fixture(args.dark, args.light, args.grey_dark, args.grey_light, args.o)


if __name__ == "__main__":
    main()
