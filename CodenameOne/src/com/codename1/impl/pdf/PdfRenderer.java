/*
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
package com.codename1.impl.pdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Interprets page content streams onto a [PdfDevice].
///
/// Covered: paths, clipping, every text operator with embedded TrueType,
/// CFF, OpenType and Type 1 fonts and Type 3 fonts, images (with soft
/// masks, stencil and color-key masks), form XObjects, axial and radial
/// shadings, tiling patterns, constant alpha, and annotation appearance
/// streams, blend modes, soft masks, knockout groups, line dashes, mesh and
/// function shadings, and vertical writing. Not
/// covered: fonts that are neither embedded nor nameable, drawn with a
/// platform font of the same family.
public final class PdfRenderer {
    private static final int MAX_DEPTH = 12;
    private static final int MAX_TILES = 400;
    private static final int MAX_SHADING_PIXELS = 1024 * 1024;

    private final PdfFile file;
    private final PdfDevice dev;
    private final Map<Object, FontRes> fonts = new HashMap<Object, FontRes>();
    private double[] pageClip;
    private int budget;

    /// A renderer for pages of `file` drawing on `dev`.
    public PdfRenderer(PdfFile file, PdfDevice dev) {
        this.file = file;
        this.dev = dev;
    }

    /// The matrix from the user space of `page` to device pixels at
    /// `scale` pixels per point, honouring the crop box and rotation.
    public static double[] pageMatrix(PdfDict page, double scale) {
        double[] b = PdfContent.box(page);
        double bx = b[0];
        double by = b[1];
        double bw = b[2];
        double bh = b[3];
        double s = scale;
        switch (PdfContent.rotation(page)) {
            case 90:
                return new double[] {0, s, s, 0, -by * s, -bx * s};
            case 180:
                return new double[] {-s, 0, 0, s, (bx + bw) * s, -by * s};
            case 270:
                return new double[] {0, -s, -s, 0, (by + bh) * s, (bx + bw) * s};
            default:
                return new double[] {s, 0, 0, -s, -bx * s, (by + bh) * s};
        }
    }

    /// Draws `page` at `scale` pixels per point; with `annotations`, the
    /// appearance of visible annotations too.
    public void render(PdfDict page, double scale, boolean annotations) {
        double[] base = pageMatrix(page, scale);
        double[] b = PdfContent.box(page);
        boolean swap = PdfContent.rotation(page) % 180 != 0;
        pageClip = new double[] {0, 0, (swap ? b[3] : b[2]) * scale, (swap ? b[2] : b[3]) * scale};
        budget = 20 * 1000 * 1000;
        State gs = new State();
        gs.ctm = base;
        gs.patternBase = base;
        gs.clip = pageClip;
        byte[] content = PdfContent.concat(file, page.get("Contents"));
        dev.save();
        try {
            run(content, page.getDict("Resources"), gs, 0);
        } catch (RuntimeException e) {
            // A malformed page draws what it could.
        }
        dev.restore();
        if (annotations) {
            drawAnnotations(page, base);
        }
    }

    private void drawAnnotations(PdfDict page, double[] base) {
        List<Object> annots = page.getArray("Annots");
        if (annots == null) {
            return;
        }
        for (int i = 0; i < annots.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object o = file.resolve(annots.get(i));
            if (!(o instanceof PdfDict)) {
                continue;
            }
            PdfDict a = (PdfDict) o;
            int flags = a.getInt("F", 0);
            if ((flags & 2) != 0 || (flags & 32) != 0 || "Popup".equals(a.getName("Subtype"))) {
                continue;
            }
            PdfDict ap = a.getDict("AP");
            Object n = ap == null ? null : ap.get("N");
            if (n instanceof PdfDict && !(n instanceof PdfStream)) {
                String state = a.getName("AS");
                n = state == null ? null : ((PdfDict) n).get(state);
            }
            if (!(n instanceof PdfStream)) {
                continue;
            }
            PdfStream form = (PdfStream) n;
            double[] rect = arr(a.getArray("Rect"), 4);
            double[] bbox = arr(form.getArray("BBox"), 4);
            if (rect == null || bbox == null) {
                continue;
            }
            double[] m = matrix(form.getArray("Matrix"));
            double[] tb = transformBox(bbox, m);
            double rx0 = Math.min(rect[0], rect[2]);
            double ry0 = Math.min(rect[1], rect[3]);
            double rx1 = Math.max(rect[0], rect[2]);
            double ry1 = Math.max(rect[1], rect[3]);
            double sx = tb[2] - tb[0] == 0 ? 1 : (rx1 - rx0) / (tb[2] - tb[0]);
            double sy = tb[3] - tb[1] == 0 ? 1 : (ry1 - ry0) / (tb[3] - tb[1]);
            double[] fit = {sx, 0, 0, sy, rx0 - tb[0] * sx, ry0 - tb[1] * sy};
            State gs = new State();
            gs.ctm = mul(mul(m, fit), base);
            gs.patternBase = gs.ctm;
            gs.clip = pageClip;
            dev.save();
            try {
                PdfPath clip = new PdfPath();
                rectPath(clip, bbox[0], bbox[1], bbox[2] - bbox[0], bbox[3] - bbox[1], gs.ctm);
                dev.clip(clip, false);
                PdfDict res = form.getDict("Resources");
                run(form.getData(), res, gs, 1);
            } catch (java.io.IOException e) {
                // skip
            } catch (RuntimeException e) {
                // skip
            }
            dev.restore();
        }
    }

    private static double[] transformBox(double[] b, double[] m) {
        double[] xs = new double[4];
        double[] ys = new double[4];
        double[][] pts = {{b[0], b[1]}, {b[2], b[1]}, {b[0], b[3]}, {b[2], b[3]}};
        for (int i = 0; i < 4; i++) {
            xs[i] = m[0] * pts[i][0] + m[2] * pts[i][1] + m[4];
            ys[i] = m[1] * pts[i][0] + m[3] * pts[i][1] + m[5];
        }
        return new double[] {min(xs), min(ys), max(xs), max(ys)};
    }

    private static double min(double[] v) {
        double m = v[0];
        for (int i = 1; i < v.length; i++) {
            m = Math.min(m, v[i]);
        }
        return m;
    }

    private static double max(double[] v) {
        double m = v[0];
        for (int i = 1; i < v.length; i++) {
            m = Math.max(m, v[i]);
        }
        return m;
    }

    /// The graphics state.
    private static final class State {
        double[] ctm;
        double[] clip;
        PdfColorSpace fillCs = PdfColorSpace.DEVICE_GRAY;
        PdfColorSpace strokeCs = PdfColorSpace.DEVICE_GRAY;
        int fillRgb;
        int strokeRgb;
        Object fillPattern;
        Object strokePattern;
        double fillAlpha = 1;
        double strokeAlpha = 1;
        double lineWidth = 1;
        int cap;
        int join;
        double miter = 10;
        double[] dash;
        double dashPhase;
        int blend;
        PdfDict softMask;
        double[] softMaskCtm;
        /// The knockout group being drawn, shared by the copies made inside it.
        Knockout knockout;
        /// The default space of the page or form being drawn, which
        /// patterns are positioned in.
        double[] patternBase;
        FontRes font;
        double fontSize = 12;
        double charSpace;
        double wordSpace;
        double hscale = 1;
        double leading;
        double rise;
        int renderMode;
        PdfDict resources;

        State copy() {
            State s = new State();
            s.ctm = ctm;
            s.clip = clip;
            s.fillCs = fillCs;
            s.strokeCs = strokeCs;
            s.fillRgb = fillRgb;
            s.strokeRgb = strokeRgb;
            s.fillPattern = fillPattern;
            s.strokePattern = strokePattern;
            s.fillAlpha = fillAlpha;
            s.strokeAlpha = strokeAlpha;
            s.lineWidth = lineWidth;
            s.cap = cap;
            s.join = join;
            s.miter = miter;
            s.dash = dash;
            s.dashPhase = dashPhase;
            s.blend = blend;
            s.softMask = softMask;
            s.softMaskCtm = softMaskCtm;
            s.knockout = knockout;
            s.patternBase = patternBase;
            s.font = font;
            s.fontSize = fontSize;
            s.charSpace = charSpace;
            s.wordSpace = wordSpace;
            s.hscale = hscale;
            s.leading = leading;
            s.rise = rise;
            s.renderMode = renderMode;
            s.resources = resources;
            return s;
        }
    }

    private static int argb(int rgb, double alpha) {
        int a = (int) (alpha * 255 + 0.5);
        a = a < 0 ? 0 : a > 255 ? 255 : a;
        return (a << 24) | (rgb & 0xffffff);
    }

    // Patterns and soft masks start their content at a fixed depth, so the
    // depth argument alone cannot stop a pattern that paints with itself;
    // this counts every content stream actually on the stack.
    private int active;

    private void run(byte[] data, PdfDict resources, State initial, int depth) {
        if (depth > MAX_DEPTH || active >= MAX_DEPTH * 2) {
            return;
        }
        active++;
        try {
            runContent(data, resources, initial, depth);
        } finally {
            active--;
        }
    }

    private void runContent(byte[] data, PdfDict resources, State initial, int depth) {
        PdfLexer lx = new PdfLexer(data, 0, data.length, false);
        List<Object> ops = new ArrayList<Object>();
        List<State> stack = new ArrayList<State>();
        State gs = initial;
        gs.resources = resources;
        PdfPath path = new PdfPath();
        int pendingClip = 0;
        double cx = 0;
        double cy = 0;
        double sx = 0;
        double sy = 0;
        double[] tm = {1, 0, 0, 1, 0, 0};
        double[] tlm = {1, 0, 0, 1, 0, 0};
        int saves = 0;
        while (budget-- > 0) {
            Object o = lx.next();
            if (o == PdfLexer.EOF) { // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
                break;
            }
            if (!(o instanceof PdfLexer.Op)) {
                ops.add(o);
                continue;
            }
            String op = ((PdfLexer.Op) o).name;
            int n = ops.size();
            char c0 = op.length() > 0 ? op.charAt(0) : ' ';
            switch (c0) {
                case 'q':
                    if ("q".equals(op)) {
                        stack.add(gs);
                        gs = gs.copy();
                        dev.save();
                        saves++;
                    }
                    break;
                case 'Q':
                    if ("Q".equals(op) && !stack.isEmpty()) {
                        gs = stack.remove(stack.size() - 1);
                        dev.restore();
                        saves--;
                    }
                    break;
                case 'c':
                    if ("cm".equals(op) && n >= 6) {
                        gs.ctm = mul(nums(ops, n - 6), gs.ctm);
                    } else if ("c".equals(op) && n >= 6) {
                        double[] a = nums(ops, n - 6);
                        curve(path, gs.ctm, a[0], a[1], a[2], a[3], a[4], a[5]);
                        cx = a[4];
                        cy = a[5];
                    } else if ("cs".equals(op) && n >= 1) {
                        gs.fillCs = PdfColorSpace.resolve(file, ops.get(n - 1), gs.resources);
                        gs.fillRgb = gs.fillCs.getKind() == PdfColorSpace.PATTERN ? 0 : gs.fillCs.toRgb(gs.fillCs.initial());
                        gs.fillPattern = null;
                    }
                    break;
                case 'm':
                    if ("m".equals(op) && n >= 2) {
                        cx = num(ops.get(n - 2));
                        cy = num(ops.get(n - 1));
                        sx = cx;
                        sy = cy;
                        path.moveTo(tx(gs.ctm, cx, cy), ty(gs.ctm, cx, cy));
                    }
                    break;
                case 'l':
                    if ("l".equals(op) && n >= 2) {
                        cx = num(ops.get(n - 2));
                        cy = num(ops.get(n - 1));
                        path.lineTo(tx(gs.ctm, cx, cy), ty(gs.ctm, cx, cy));
                    }
                    break;
                case 'v':
                    if ("v".equals(op) && n >= 4) {
                        double[] a = nums4(ops, n - 4);
                        curve(path, gs.ctm, cx, cy, a[0], a[1], a[2], a[3]);
                        cx = a[2];
                        cy = a[3];
                    }
                    break;
                case 'y':
                    if ("y".equals(op) && n >= 4) {
                        double[] a = nums4(ops, n - 4);
                        curve(path, gs.ctm, a[0], a[1], a[2], a[3], a[2], a[3]);
                        cx = a[2];
                        cy = a[3];
                    }
                    break;
                case 'h':
                    if ("h".equals(op)) {
                        path.closePath();
                        cx = sx;
                        cy = sy;
                    }
                    break;
                case 'r':
                    if ("re".equals(op) && n >= 4) {
                        double[] a = nums4(ops, n - 4);
                        rectPath(path, a[0], a[1], a[2], a[3], gs.ctm);
                        cx = a[0];
                        cy = a[1];
                        sx = cx;
                        sy = cy;
                    } else if ("rg".equals(op) && n >= 3) {
                        gs.fillCs = PdfColorSpace.DEVICE_RGB;
                        gs.fillRgb = gs.fillCs.toRgb(new double[] {num(ops.get(n - 3)), num(ops.get(n - 2)),
                            num(ops.get(n - 1))});
                        gs.fillPattern = null;
                    } else if ("ri".equals(op)) {
                        break;
                    }
                    break;
                case 'R':
                    if ("RG".equals(op) && n >= 3) {
                        gs.strokeCs = PdfColorSpace.DEVICE_RGB;
                        gs.strokeRgb = gs.strokeCs.toRgb(new double[] {num(ops.get(n - 3)), num(ops.get(n - 2)),
                            num(ops.get(n - 1))});
                        gs.strokePattern = null;
                    }
                    break;
                case 'g':
                    if ("g".equals(op) && n >= 1) {
                        gs.fillCs = PdfColorSpace.DEVICE_GRAY;
                        gs.fillRgb = gs.fillCs.toRgb(new double[] {num(ops.get(n - 1))});
                        gs.fillPattern = null;
                    } else if ("gs".equals(op) && n >= 1) {
                        extGState(gs, ops.get(n - 1));
                    }
                    break;
                case 'G':
                    if ("G".equals(op) && n >= 1) {
                        gs.strokeCs = PdfColorSpace.DEVICE_GRAY;
                        gs.strokeRgb = gs.strokeCs.toRgb(new double[] {num(ops.get(n - 1))});
                        gs.strokePattern = null;
                    }
                    break;
                case 'k':
                    if ("k".equals(op) && n >= 4) {
                        double[] a = nums4(ops, n - 4);
                        gs.fillCs = PdfColorSpace.DEVICE_CMYK;
                        gs.fillRgb = PdfColorSpace.cmyk(a[0], a[1], a[2], a[3]);
                        gs.fillPattern = null;
                    }
                    break;
                case 'K':
                    if ("K".equals(op) && n >= 4) {
                        double[] a = nums4(ops, n - 4);
                        gs.strokeCs = PdfColorSpace.DEVICE_CMYK;
                        gs.strokeRgb = PdfColorSpace.cmyk(a[0], a[1], a[2], a[3]);
                        gs.strokePattern = null;
                    }
                    break;
                case 's':
                    if ("sc".equals(op) || "scn".equals(op)) {
                        setColor(gs, ops, true);
                    } else if ("s".equals(op)) {
                        path.closePath();
                        paint(path, gs, false, true, false, pendingClip);
                        path = new PdfPath();
                        pendingClip = 0;
                    } else if ("sh".equals(op) && n >= 1) {
                        PdfDict sh = gs.resources == null ? null : gs.resources.getDict("Shading");
                        Object shading = sh != null && ops.get(n - 1) instanceof PdfName
                                ? sh.get(((PdfName) ops.get(n - 1)).value) : null;
                        if (shading instanceof PdfDict) {
                            shade((PdfDict) shading, gs.ctm, gs.clip, gs.fillAlpha);
                        }
                    }
                    break;
                case 'S':
                    if ("SC".equals(op) || "SCN".equals(op)) {
                        setColor(gs, ops, false);
                    } else if ("S".equals(op)) {
                        paint(path, gs, false, true, false, pendingClip);
                        path = new PdfPath();
                        pendingClip = 0;
                    }
                    break;
                case 'C':
                    if ("CS".equals(op) && n >= 1) {
                        gs.strokeCs = PdfColorSpace.resolve(file, ops.get(n - 1), gs.resources);
                        gs.strokeRgb = gs.strokeCs.getKind() == PdfColorSpace.PATTERN ? 0
                                : gs.strokeCs.toRgb(gs.strokeCs.initial());
                        gs.strokePattern = null;
                    }
                    break;
                case 'f':
                case 'F':
                    if ("f".equals(op) || "F".equals(op) || "f*".equals(op)) {
                        paint(path, gs, true, false, "f*".equals(op), pendingClip);
                        path = new PdfPath();
                        pendingClip = 0;
                    }
                    break;
                case 'B':
                    if ("B".equals(op) || "B*".equals(op)) {
                        paint(path, gs, true, true, "B*".equals(op), pendingClip);
                        path = new PdfPath();
                        pendingClip = 0;
                    } else if ("BT".equals(op)) {
                        tm = new double[] {1, 0, 0, 1, 0, 0};
                        tlm = tm;
                    } else if ("BI".equals(op)) {
                        PdfStream img = lx.inlineImage();
                        img.file = file;
                        drawImage(img, gs);
                    } else if ("BDC".equals(op) || "BMC".equals(op) || "BX".equals(op)) {
                        break;
                    }
                    break;
                case 'b':
                    if ("b".equals(op) || "b*".equals(op)) {
                        path.closePath();
                        paint(path, gs, true, true, "b*".equals(op), pendingClip);
                        path = new PdfPath();
                        pendingClip = 0;
                    }
                    break;
                case 'n':
                    if ("n".equals(op)) {
                        paint(path, gs, false, false, false, pendingClip);
                        path = new PdfPath();
                        pendingClip = 0;
                    }
                    break;
                case 'W':
                    if ("W".equals(op)) {
                        pendingClip = 1;
                    } else if ("W*".equals(op)) {
                        pendingClip = 2;
                    }
                    break;
                case 'w':
                    if ("w".equals(op) && n >= 1) {
                        gs.lineWidth = num(ops.get(n - 1));
                    }
                    break;
                case 'J':
                    if ("J".equals(op) && n >= 1) {
                        gs.cap = (int) num(ops.get(n - 1));
                    }
                    break;
                case 'd':
                    // d0 and d1 (Type 3 glyph metrics) need nothing here.
                    if ("d".equals(op) && n >= 2) {
                        setDash(gs, ops.get(n - 2), ops.get(n - 1));
                    }
                    break;
                case 'j':
                    if ("j".equals(op) && n >= 1) {
                        gs.join = (int) num(ops.get(n - 1));
                    }
                    break;
                case 'M':
                    if ("M".equals(op) && n >= 1) {
                        gs.miter = num(ops.get(n - 1));
                    }
                    break;
                case 'D':
                    if ("Do".equals(op) && n >= 1) {
                        doXObject(ops.get(n - 1), gs, depth);
                    }
                    break;
                case 'T':
                    if ("Tf".equals(op) && n >= 2) {
                        gs.fontSize = num(ops.get(n - 1));
                        gs.font = font(gs.resources, ops.get(n - 2));
                    } else if ("Tc".equals(op) && n >= 1) {
                        gs.charSpace = num(ops.get(n - 1));
                    } else if ("Tw".equals(op) && n >= 1) {
                        gs.wordSpace = num(ops.get(n - 1));
                    } else if ("Tz".equals(op) && n >= 1) {
                        gs.hscale = num(ops.get(n - 1)) / 100;
                    } else if ("TL".equals(op) && n >= 1) {
                        gs.leading = num(ops.get(n - 1));
                    } else if ("Ts".equals(op) && n >= 1) {
                        gs.rise = num(ops.get(n - 1));
                    } else if ("Tr".equals(op) && n >= 1) {
                        gs.renderMode = (int) num(ops.get(n - 1));
                    } else if (("Td".equals(op) || "TD".equals(op)) && n >= 2) {
                        double ttx = num(ops.get(n - 2));
                        double tty = num(ops.get(n - 1));
                        if ("TD".equals(op)) {
                            gs.leading = -tty;
                        }
                        tlm = mul(new double[] {1, 0, 0, 1, ttx, tty}, tlm);
                        tm = tlm;
                    } else if ("Tm".equals(op) && n >= 6) {
                        tlm = nums(ops, n - 6);
                        tm = tlm;
                    } else if ("T*".equals(op)) {
                        tlm = mul(new double[] {1, 0, 0, 1, 0, -gs.leading}, tlm);
                        tm = tlm;
                    } else if (("Tj".equals(op) || "TJ".equals(op)) && n >= 1) {
                        tm = show(gs, tm, ops.get(n - 1), depth);
                    }
                    break;
                case '\'':
                case '"':
                    if (n >= 1) {
                        if ("\"".equals(op) && n >= 3) {
                            gs.wordSpace = num(ops.get(n - 3));
                            gs.charSpace = num(ops.get(n - 2));
                        }
                        tlm = mul(new double[] {1, 0, 0, 1, 0, -gs.leading}, tlm);
                        tm = show(gs, tlm, ops.get(n - 1), depth);
                    }
                    break;
                case 'i':
                    break;
                default:
                    break;
            }
            ops.clear();
        }
        while (saves-- > 0) {
            dev.restore();
        }
    }

    private void setColor(State gs, List<Object> ops, boolean fill) {
        PdfColorSpace cs = fill ? gs.fillCs : gs.strokeCs;
        int n = ops.size();
        if (cs.getKind() == PdfColorSpace.PATTERN) {
            Object name = n > 0 ? ops.get(n - 1) : null;
            PdfDict pats = gs.resources == null ? null : gs.resources.getDict("Pattern");
            Object pat = pats != null && name instanceof PdfName ? pats.get(((PdfName) name).value) : null;
            int rgb = 0x808080;
            if (cs.getBase() != null && n > 1) {
                double[] c = new double[n - 1];
                for (int i = 0; i < n - 1; i++) {
                    c[i] = num(ops.get(i));
                }
                rgb = cs.getBase().toRgb(c);
            }
            if (fill) {
                gs.fillPattern = pat;
                gs.fillRgb = rgb;
            } else {
                gs.strokePattern = pat;
                gs.strokeRgb = rgb;
            }
            return;
        }
        int count = Math.min(n, Math.max(1, cs.getComponents()));
        double[] c = new double[count];
        for (int i = 0; i < count; i++) {
            c[i] = num(ops.get(n - count + i));
        }
        int rgb = cs.toRgb(c);
        if (fill) {
            gs.fillRgb = rgb;
            gs.fillPattern = null;
        } else {
            gs.strokeRgb = rgb;
            gs.strokePattern = null;
        }
    }

    private void extGState(State gs, Object name) {
        PdfDict all = gs.resources == null ? null : gs.resources.getDict("ExtGState");
        Object o = all != null && name instanceof PdfName ? all.get(((PdfName) name).value) : null;
        if (!(o instanceof PdfDict)) {
            return;
        }
        PdfDict e = (PdfDict) o;
        if (e.has("ca")) {
            gs.fillAlpha = e.getNumber("ca", 1);
        }
        if (e.has("CA")) {
            gs.strokeAlpha = e.getNumber("CA", 1);
        }
        if (e.has("LW")) {
            gs.lineWidth = e.getNumber("LW", 1);
        }
        if (e.has("LC")) {
            gs.cap = e.getInt("LC", 0);
        }
        if (e.has("LJ")) {
            gs.join = e.getInt("LJ", 0);
        }
        if (e.has("BM")) {
            gs.blend = Math.max(0, PdfBlend.parse(e.get("BM")));
        }
        if (e.has("SMask")) {
            Object sm = e.get("SMask");
            gs.softMask = sm instanceof PdfDict ? (PdfDict) sm : null;
            gs.softMaskCtm = gs.ctm;
        }
        List<Object> dl = e.getArray("D");
        if (dl != null && dl.size() >= 2) {
            setDash(gs, file.resolve(dl.get(0)), file.resolve(dl.get(1)));
        }
        if (e.has("ML")) {
            gs.miter = e.getNumber("ML", 10);
        }
        List<Object> f = e.getArray("Font");
        if (f != null && f.size() >= 2) {
            gs.font = fontFromDict(file.resolve(f.get(0)));
            gs.fontSize = num(f.get(1));
        }
    }

    private void paintNow(PdfPath path, State gs, boolean fill, boolean stroke, boolean evenOdd) {
        if (fill) {
            if (gs.fillPattern != null) {
                fillPattern(path, gs, evenOdd);
            } else {
                dev.fill(path, evenOdd, argb(gs.fillRgb, gs.fillAlpha));
            }
        }
        if (stroke) {
            double w = gs.lineWidth * Math.sqrt(Math.abs(gs.ctm[0] * gs.ctm[3] - gs.ctm[1] * gs.ctm[2]));
            if (w < 1) {
                w = 1;
            }
            PdfPath p = path;
            if (gs.dash != null) {
                double sc = Math.sqrt(Math.abs(gs.ctm[0] * gs.ctm[3] - gs.ctm[1] * gs.ctm[2]));
                float[] dd = new float[gs.dash.length];
                for (int i = 0; i < dd.length; i++) {
                    dd[i] = (float) (gs.dash[i] * sc);
                }
                p = path.dash(dd, (float) (gs.dashPhase * sc));
            }
            if (!p.isEmpty()) {
                dev.stroke(p, (float) w, gs.cap, gs.join, (float) gs.miter, argb(gs.strokeRgb, gs.strokeAlpha));
            }
        }
    }

    private static double scaleOf(double[] m) {
        return Math.sqrt(Math.abs(m[0] * m[3] - m[1] * m[2]));
    }

    /// True when drawing in `gs` needs compositing: a blend mode other
    /// than Normal, or a soft mask.
    private static boolean composited(State gs) {
        return gs.blend != PdfBlend.NORMAL || gs.softMask != null;
    }

    /// The integer device rectangle `{x, y, w, h}` of `box` (`{x0, y0,
    /// x1, y1}`) inside the clip, or null when empty or too large.
    private int[] layerRect(State gs, double[] box) {
        double[] c = intersect(gs.clip, box);
        int x0 = (int) Math.floor(Math.max(0, c[0]));
        int y0 = (int) Math.floor(Math.max(0, c[1]));
        int x1 = (int) Math.ceil(Math.min(pageClip[2], c[2]));
        int y1 = (int) Math.ceil(Math.min(pageClip[3], c[3]));
        if (x1 <= x0 || y1 <= y0 || (long) (x1 - x0) * (y1 - y0) > MAX_LAYER) {
            return null;
        }
        return new int[] {x0, y0, x1 - x0, y1 - y0};
    }

    private static final int MAX_LAYER = 16 * 1000 * 1000;
    private int layerDepth;

    /// Draws `draw` into a layer over `r`, then composites the layer onto
    /// the surface with the blend mode and soft mask of `gs` and constant
    /// alpha `alpha`. Inside, the state draws plainly.
    private void withLayer(State gs, int[] r, double alpha, Runnable draw) {
        int mode = gs.blend;
        PdfDict mask = gs.softMask;
        double[] maskCtm = gs.softMaskCtm;
        if (layerDepth > 8) {
            draw.run();
            return;
        }
        layerDepth++;
        gs.blend = PdfBlend.NORMAL;
        gs.softMask = null;
        int[] layer;
        dev.beginLayer(r[0], r[1], r[2], r[3]);
        try {
            draw.run();
        } finally {
            layer = dev.endLayer();
            gs.blend = mode;
            gs.softMask = mask;
        }
        int[] m = mask == null ? null : softMaskPixels(mask, maskCtm, gs, r);
        int[] back = mode == PdfBlend.NORMAL ? null : dev.pixels(r[0], r[1], r[2], r[3]);
        layerDepth--;
        int[] out = PdfBlend.composite(back, layer, m, mode, alpha, r[2], r[3]);
        dev.image(out, r[2], r[3], new double[] {1, 0, 0, 1, r[0], r[1]});
    }

    private static boolean isKnockout(PdfStream form) {
        PdfDict g = form.getDict("Group");
        return g != null && Boolean.TRUE.equals(g.get("K"));
    }

    /// The pixels of a knockout group: each object inside replaces what
    /// the group's earlier objects drew where it covers them, instead of
    /// compositing over them.
    private static final class Knockout {
        final int[] rect;
        final int[] pixels;

        Knockout(int[] rect) {
            this.rect = rect;
            pixels = new int[rect[2] * rect[3]];
        }
    }

    /// Draws one object of a knockout group: alone into a layer, then
    /// into the group's pixels, replacing them in proportion to its
    /// coverage. `objAlpha` is the constant alpha the object is drawn
    /// with, which its coverage is measured against.
    private void knock(State gs, double objAlpha, Runnable draw) {
        Knockout k = gs.knockout;
        int[] r = k.rect;
        int[] layer;
        dev.beginLayer(r[0], r[1], r[2], r[3]);
        try {
            draw.run();
        } finally {
            layer = dev.endLayer();
        }
        if (layer == null) {
            return;
        }
        // Coverage is the layer's alpha relative to the object's own
        // constant alpha, so a translucent object still knocks out fully.
        double full = objAlpha <= 0 ? 255 : 255 * objAlpha;
        int[] out = k.pixels;
        int n = Math.min(out.length, layer.length);
        for (int i = 0; i < n; i++) {
            int src = layer[i];
            int sa = src >>> 24;
            if (sa == 0) {
                continue;
            }
            double f = Math.min(1, sa / full);
            int dst = out[i];
            out[i] = lerpPremultiplied(dst, src, f);
        }
    }

    /// `(1 - f) * a + f * b`, computed on premultiplied colors.
    private static int lerpPremultiplied(int a, int b, double f) {
        double aa = (a >>> 24) / 255.0;
        double ba = (b >>> 24) / 255.0;
        double oa = aa * (1 - f) + ba * f;
        if (oa <= 0) {
            return 0;
        }
        int out = clamp255((int) (oa * 255 + 0.5)) << 24;
        for (int sft = 16; sft >= 0; sft -= 8) {
            double ac = ((a >> sft) & 0xff) * aa;
            double bc = ((b >> sft) & 0xff) * ba;
            double c = (ac * (1 - f) + bc * f) / oa;
            out |= clamp255((int) (c + 0.5)) << sft;
        }
        return out;
    }

    /// The soft mask `sm` over `r`, one value 0-255 per pixel: the
    /// luminosity or the alpha of its group, rendered with the matrix in
    /// effect when the mask was set.
    private int[] softMaskPixels(PdfDict sm, double[] ctm, State gs, int[] r) {
        Object go = sm.get("G");
        int n = r[2] * r[3];
        int[] out = new int[n];
        if (!(go instanceof PdfStream)) {
            return out;
        }
        PdfStream g = (PdfStream) go;
        boolean luminosity = !"Alpha".equals(sm.getName("S"));
        layerDepth++;
        dev.beginLayer(r[0], r[1], r[2], r[3]);
        int[] px;
        try {
            if (luminosity) {
                PdfPath bg = new PdfPath();
                bg.moveTo(r[0], r[1]);
                bg.lineTo(r[0] + r[2], r[1]);
                bg.lineTo(r[0] + r[2], r[1] + r[3]);
                bg.lineTo(r[0], r[1] + r[3]);
                bg.closePath();
                dev.fill(bg, false, 0xff000000 | backdropRgb(sm.getArray("BC")));
            }
            State m = new State();
            m.ctm = mul(matrix(g.getArray("Matrix")), ctm);
            m.patternBase = m.ctm;
            m.clip = new double[] {r[0], r[1], r[0] + r[2], r[1] + r[3]};
            PdfDict res = g.getDict("Resources");
            m.resources = res != null ? res : gs.resources;
            run(g.getData(), m.resources, m, MAX_DEPTH - 4);
        } catch (java.io.IOException e) {
            // an unreadable mask hides everything
        } catch (RuntimeException e) {
            // a malformed mask draws what it could
        } finally {
            px = dev.endLayer();
            layerDepth--;
        }
        PdfFunction tr = sm.has("TR") && !(sm.get("TR") instanceof PdfName) ? PdfFunction.parse(sm.get("TR")) : null;
        double[] in = new double[1];
        for (int i = 0; i < n && i < px.length; i++) {
            int p = px[i];
            int v;
            if (luminosity) {
                v = (((p >> 16) & 0xff) * 77 + ((p >> 8) & 0xff) * 151 + (p & 0xff) * 28) >> 8;
            } else {
                v = p >>> 24;
            }
            if (tr != null) {
                in[0] = v / 255.0;
                double[] o = tr.eval(in);
                v = (int) (Math.max(0, Math.min(1, o.length > 0 ? o[0] : 0)) * 255 + 0.5);
            }
            out[i] = v;
        }
        return out;
    }

    /// The RGB of a soft mask's backdrop color `BC`, black by default.
    private static int backdropRgb(List<Object> bc) {
        if (bc == null || bc.isEmpty()) {
            return 0;
        }
        double[] c = new double[bc.size()];
        for (int i = 0; i < c.length; i++) {
            Object v = bc.get(i);
            c[i] = v instanceof Number ? ((Number) v).doubleValue() : 0;
        }
        int r;
        int g;
        int b;
        if (c.length >= 4) {
            r = (int) (255 * (1 - Math.min(1, c[0] + c[3])));
            g = (int) (255 * (1 - Math.min(1, c[1] + c[3])));
            b = (int) (255 * (1 - Math.min(1, c[2] + c[3])));
        } else if (c.length == 3) {
            r = (int) (255 * c[0]);
            g = (int) (255 * c[1]);
            b = (int) (255 * c[2]);
        } else {
            r = (int) (255 * c[0]);
            g = r;
            b = r;
        }
        return (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    /// Sets the dash pattern from a `d` operator's operands; an empty or
    /// all-zero array means solid lines.
    private static void setDash(State gs, Object array, Object phase) {
        gs.dash = null;
        if (!(array instanceof List)) {
            return;
        }
        List<?> l = (List<?>) array;
        double[] d = new double[l.size()];
        double total = 0;
        for (int i = 0; i < d.length; i++) {
            Object v = l.get(i);
            d[i] = v instanceof Number ? Math.max(0, ((Number) v).doubleValue()) : 0;
            total += d[i];
        }
        if (total > 0) {
            gs.dash = d;
            gs.dashPhase = phase instanceof Number ? ((Number) phase).doubleValue() : 0;
        }
    }

    private void paint(final PdfPath path, final State gs, final boolean fill, final boolean stroke,
            final boolean evenOdd, int clip) {
        if (path.isEmpty()) {
            return;
        }
        if ((fill || stroke) && gs.knockout != null) {
            double objAlpha = fill && stroke ? Math.max(gs.fillAlpha, gs.strokeAlpha)
                    : fill ? gs.fillAlpha : gs.strokeAlpha;
            knock(gs, objAlpha, new Runnable() {
                @Override
                public void run() {
                    State plain = gs.copy();
                    plain.knockout = null;
                    paint(path, plain, fill, stroke, evenOdd, 0);
                }
            });
        } else if ((fill || stroke) && composited(gs)) {
            float[] b = path.getBounds();
            double pad = stroke ? gs.lineWidth * scaleOf(gs.ctm) + gs.miter + 2 : 1;
            int[] r = layerRect(gs, new double[] {b[0] - pad, b[1] - pad, b[2] + pad, b[3] + pad});
            if (r != null) {
                withLayer(gs, r, 1, new Runnable() {
                    @Override
                    public void run() {
                        paintNow(path, gs, fill, stroke, evenOdd);
                    }
                });
            }
        } else {
            paintNow(path, gs, fill, stroke, evenOdd);
        }
        if (clip != 0) {
            dev.clip(path, clip == 2);
            float[] b = path.getBounds();
            if (b != null) {
                gs.clip = intersect(gs.clip, new double[] {b[0], b[1], b[2], b[3]});
            }
        }
    }

    private static double[] intersect(double[] a, double[] b) {
        if (a == null) {
            return b;
        }
        return new double[] {Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.min(a[2], b[2]),
            Math.min(a[3], b[3])};
    }

    private void fillPattern(PdfPath path, State gs, boolean evenOdd) {
        Object p = gs.fillPattern;
        if (!(p instanceof PdfDict)) {
            return;
        }
        PdfDict pat = (PdfDict) p;
        float[] b = path.getBounds();
        double[] region = intersect(gs.clip, new double[] {b[0], b[1], b[2], b[3]});
        double[] pm = mul(matrix(pat.getArray("Matrix")), pageBase(gs));
        dev.save();
        dev.clip(path, evenOdd);
        if (pat.getInt("PatternType", 1) == 2) {
            Object sh = pat.get("Shading");
            if (sh instanceof PdfDict) {
                shade((PdfDict) sh, pm, region, gs.fillAlpha);
            }
        } else if (pat instanceof PdfStream) {
            tile((PdfStream) pat, pm, region, gs);
        }
        dev.restore();
    }

    /// Patterns live in the default space of the page or form they are
    /// used in, not the current one.
    private static double[] pageBase(State gs) {
        return gs.patternBase != null ? gs.patternBase : gs.ctm;
    }

    private void tile(PdfStream pat, double[] pm, double[] region, State gs) {
        double xstep = pat.getNumber("XStep", 0);
        double ystep = pat.getNumber("YStep", 0);
        double[] bbox = arr(pat.getArray("BBox"), 4);
        if (xstep == 0 || ystep == 0 || bbox == null || region == null) {
            return;
        }
        double[] inv = invert(pm);
        if (inv == null) {
            return;
        }
        double[] r = transformBox(new double[] {region[0], region[1], region[2], region[3]}, inv);
        int i0 = (int) Math.floor((r[0] - bbox[2]) / Math.abs(xstep));
        int i1 = (int) Math.ceil((r[2] - bbox[0]) / Math.abs(xstep));
        int j0 = (int) Math.floor((r[1] - bbox[3]) / Math.abs(ystep));
        int j1 = (int) Math.ceil((r[3] - bbox[1]) / Math.abs(ystep));
        if ((long) (i1 - i0 + 1) * (j1 - j0 + 1) > MAX_TILES) {
            // Too fine to draw tile by tile: approximate with the color of
            // an uncolored pattern, or mid-gray.
            PdfPath all = new PdfPath();
            all.moveTo(region[0], region[1]);
            all.lineTo(region[2], region[1]);
            all.lineTo(region[2], region[3]);
            all.lineTo(region[0], region[3]);
            all.closePath();
            dev.fill(all, false, argb(pat.getInt("PaintType", 1) == 2 ? gs.fillRgb : 0xc0c0c0, gs.fillAlpha * 0.5));
            return;
        }
        byte[] content;
        try {
            content = pat.getData();
        } catch (java.io.IOException e) {
            return;
        }
        PdfDict res = pat.getDict("Resources");
        for (int j = j0; j <= j1; j++) {
            for (int i = i0; i <= i1; i++) {
                State t = new State();
                t.ctm = mul(new double[] {1, 0, 0, 1, i * xstep, j * ystep}, pm);
                t.patternBase = t.ctm;
                t.clip = region;
                t.fillRgb = gs.fillRgb;
                t.strokeRgb = gs.fillRgb;
                t.fillAlpha = gs.fillAlpha;
                dev.save();
                PdfPath c = new PdfPath();
                rectPath(c, bbox[0], bbox[1], bbox[2] - bbox[0], bbox[3] - bbox[1], t.ctm);
                dev.clip(c, false);
                run(content, res != null ? res : gs.resources, t, MAX_DEPTH - 2);
                dev.restore();
            }
        }
    }

    // ---- shadings ----

    private void shade(PdfDict sh, double[] m, double[] region, double alpha) {
        if (region == null) {
            return;
        }
        int type = sh.getInt("ShadingType", 0);
        PdfColorSpace cs = PdfColorSpace.resolve(file, sh.getRaw("ColorSpace"), null);
        PdfFunction[] fn = sh.has("Function") ? PdfFunction.parseAll(file, sh.getRaw("Function")) : null;
        double[] r = region;
        double[] bbox = arr(sh.getArray("BBox"), 4);
        if (bbox != null) {
            double[] db = transformBox(bbox, m);
            r = intersect(r, db);
        }
        int x0 = (int) Math.floor(r[0]);
        int y0 = (int) Math.floor(r[1]);
        int x1 = (int) Math.ceil(r[2]);
        int y1 = (int) Math.ceil(r[3]);
        int w = x1 - x0;
        int h = y1 - y0;
        if (w <= 0 || h <= 0) {
            return;
        }
        if (type >= 4 && type <= 7 && sh instanceof PdfStream) {
            double mstep = 1;
            while ((long) (w / mstep) * (long) (h / mstep) > MAX_SHADING_PIXELS) {
                mstep *= 2;
            }
            int mw = Math.max(1, (int) Math.ceil(w / mstep));
            int mh = Math.max(1, (int) Math.ceil(h / mstep));
            int[] mpx = new int[mw * mh];
            PdfMesh.draw((PdfStream) sh, type, cs, fn, m, mpx, mw, mh, x0, y0, mstep, (int) (alpha * 255 + 0.5));
            dev.image(mpx, mw, mh, new double[] {mstep, 0, 0, mstep, x0, y0});
            return;
        }
        if (type == 1 && fn != null) {
            functionShading(sh, cs, fn, m, x0, y0, w, h, alpha);
            return;
        }
        if (type != 2 && type != 3 || fn == null) {
            // Malformed shadings: a flat fill with the
            // shading's middle color or its background.
            int rgb = 0x808080;
            List<Object> bg = sh.getArray("Background");
            if (bg != null) {
                double[] c = arr(bg, bg.size());
                rgb = cs.toRgb(c);
            } else if (fn != null) {
                rgb = color(cs, fn, 0.5);
            }
            PdfPath all = new PdfPath();
            all.moveTo(x0, y0);
            all.lineTo(x1, y0);
            all.lineTo(x1, y1);
            all.lineTo(x0, y1);
            all.closePath();
            dev.fill(all, false, argb(rgb, alpha));
            return;
        }
        double step = 1;
        while ((long) (w / step) * (long) (h / step) > MAX_SHADING_PIXELS) {
            step *= 2;
        }
        int sw = Math.max(1, (int) Math.ceil(w / step));
        int shh = Math.max(1, (int) Math.ceil(h / step));
        double[] inv = invert(m);
        if (inv == null) {
            return;
        }
        double[] coords = arr(sh.getArray("Coords"), type == 2 ? 4 : 6);
        if (coords == null) {
            return;
        }
        double[] domain = arr(sh.getArray("Domain"), 2);
        double t0 = domain == null ? 0 : domain[0];
        double t1 = domain == null ? 1 : domain[1];
        List<Object> ext = sh.getArray("Extend");
        boolean e0 = ext != null && !ext.isEmpty() && Boolean.TRUE.equals(ext.get(0));
        boolean e1 = ext != null && ext.size() > 1 && Boolean.TRUE.equals(ext.get(1));
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = color(cs, fn, t0 + (t1 - t0) * i / 255.0);
        }
        int a = (int) (alpha * 255 + 0.5) << 24;
        int[] px = new int[sw * shh];
        for (int j = 0; j < shh; j++) {
            double dy = y0 + (j + 0.5) * step;
            for (int i = 0; i < sw; i++) {
                double dx = x0 + (i + 0.5) * step;
                double ux = inv[0] * dx + inv[2] * dy + inv[4];
                double uy = inv[1] * dx + inv[3] * dy + inv[5];
                double s = type == 2 ? axial(coords, ux, uy) : radial(coords, ux, uy);
                if (Double.isNaN(s)) {
                    continue;
                }
                if (s < 0) {
                    if (!e0) {
                        continue;
                    }
                    s = 0;
                } else if (s > 1) {
                    if (!e1) {
                        continue;
                    }
                    s = 1;
                }
                px[j * sw + i] = a | lut[(int) (s * 255 + 0.5)];
            }
        }
        dev.image(px, sw, shh, new double[] {step, 0, 0, step, x0, y0});
    }

    /// A function-based shading: the color at each point is the function
    /// of its coordinates in the shading's domain.
    private void functionShading(PdfDict sh, PdfColorSpace cs, PdfFunction[] fn, double[] m, int x0, int y0, int w,
            int h, double alpha) {
        double[] mat = arr(sh.getArray("Matrix"), 6);
        double[] full = mat == null ? m : mul(mat, m);
        double[] inv = invert(full);
        if (inv == null) {
            return;
        }
        double[] dom = arr(sh.getArray("Domain"), 4);
        if (dom == null) {
            dom = new double[] {0, 1, 0, 1};
        }
        double step = 1;
        while ((long) (w / step) * (long) (h / step) > MAX_SHADING_PIXELS / 4) {
            step *= 2;
        }
        int sw = Math.max(1, (int) Math.ceil(w / step));
        int shh = Math.max(1, (int) Math.ceil(h / step));
        int a = (int) (alpha * 255 + 0.5) << 24;
        int[] px = new int[sw * shh];
        double[] in = new double[2];
        for (int j = 0; j < shh; j++) {
            double dy = y0 + (j + 0.5) * step;
            for (int i = 0; i < sw; i++) {
                double dx = x0 + (i + 0.5) * step;
                double ux = inv[0] * dx + inv[2] * dy + inv[4];
                double uy = inv[1] * dx + inv[3] * dy + inv[5];
                if (ux < dom[0] || ux > dom[1] || uy < dom[2] || uy > dom[3]) {
                    continue;
                }
                in[0] = ux;
                in[1] = uy;
                double[] c;
                if (fn.length == 1) {
                    c = fn[0].eval(in);
                } else {
                    c = new double[fn.length];
                    for (int k = 0; k < fn.length; k++) {
                        c[k] = fn[k].eval(in)[0];
                    }
                }
                px[j * sw + i] = a | cs.toRgb(c);
            }
        }
        dev.image(px, sw, shh, new double[] {step, 0, 0, step, x0, y0});
    }

    private static int color(PdfColorSpace cs, PdfFunction[] fn, double t) {
        double[] in = {t};
        double[] c;
        if (fn.length == 1) {
            c = fn[0].eval(in);
        } else {
            c = new double[fn.length];
            for (int i = 0; i < fn.length; i++) {
                c[i] = fn[i].eval(in)[0];
            }
        }
        return cs.toRgb(c);
    }

    private static double axial(double[] c, double x, double y) {
        double dx = c[2] - c[0];
        double dy = c[3] - c[1];
        double len = dx * dx + dy * dy;
        if (len == 0) {
            return 0;
        }
        return ((x - c[0]) * dx + (y - c[1]) * dy) / len;
    }

    private static double radial(double[] c, double x, double y) {
        double cdx = c[3] - c[0];
        double cdy = c[4] - c[1];
        double dr = c[5] - c[2];
        double pdx = x - c[0];
        double pdy = y - c[1];
        double a = cdx * cdx + cdy * cdy - dr * dr;
        double b = pdx * cdx + pdy * cdy + c[2] * dr;
        double cc = pdx * pdx + pdy * pdy - c[2] * c[2];
        if (a == 0) {
            if (b == 0) {
                return Double.NaN;
            }
            double s = cc / (2 * b);
            return c[2] + s * dr >= 0 ? s : Double.NaN;
        }
        double disc = b * b - a * cc;
        if (disc < 0) {
            return Double.NaN;
        }
        double sq = Math.sqrt(disc);
        double s1 = (b + sq) / a;
        double s2 = (b - sq) / a;
        double hi = Math.max(s1, s2);
        double lo = Math.min(s1, s2);
        if (c[2] + hi * dr >= 0) {
            return hi;
        }
        if (c[2] + lo * dr >= 0) {
            return lo;
        }
        return Double.NaN;
    }

    // ---- XObjects and images ----

    private void doXObject(final Object name, final State gs, final int depth) {
        if (gs.knockout != null) {
            // An XObject is one object of the knockout group around it.
            knock(gs, gs.fillAlpha, new Runnable() {
                @Override
                public void run() {
                    State plain = gs.copy();
                    plain.knockout = null;
                    doXObject(name, plain, depth);
                }
            });
            return;
        }
        PdfDict xo = gs.resources == null ? null : gs.resources.getDict("XObject");
        Object x = xo != null && name instanceof PdfName ? xo.get(((PdfName) name).value) : null;
        if (!(x instanceof PdfStream)) {
            return;
        }
        PdfStream s = (PdfStream) x;
        String sub = s.getName("Subtype");
        if ("Image".equals(sub)) {
            if (composited(gs)) {
                final PdfStream img = s;
                final State st = gs;
                int[] r = layerRect(gs, transformBox(new double[] {0, 0, 1, 1}, gs.ctm));
                if (r != null) {
                    withLayer(gs, r, 1, new Runnable() {
                        @Override
                        public void run() {
                            drawImage(img, st);
                        }
                    });
                }
            } else {
                drawImage(s, gs);
            }
        } else if ("Form".equals(sub) && depth < MAX_DEPTH && s.getDict("Group") != null
                && (composited(gs) || gs.fillAlpha < 1 || isKnockout(s))) {
            // A transparency group: drawn opaque into a layer, then
            // composited as one with the state's blend mode, soft mask
            // and fill alpha.
            final PdfStream form = s;
            final State inner = gs.copy();
            inner.fillAlpha = 1;
            inner.strokeAlpha = 1;
            inner.blend = PdfBlend.NORMAL;
            inner.softMask = null;
            final int d = depth;
            double[] bbox = arr(s.getArray("BBox"), 4);
            double[] box = bbox == null ? gs.clip
                    : transformBox(bbox, mul(matrix(s.getArray("Matrix")), gs.ctm));
            final int[] r = layerRect(gs, box);
            if (r != null) {
                final Knockout k = isKnockout(s) ? new Knockout(r) : null;
                inner.knockout = k;
                withLayer(gs, r, gs.fillAlpha, new Runnable() {
                    @Override
                    public void run() {
                        drawForm(form, inner, d);
                        if (k != null) {
                            dev.image(k.pixels, r[2], r[3], new double[] {1, 0, 0, 1, r[0], r[1]});
                        }
                    }
                });
            }
        } else if ("Form".equals(sub) && depth < MAX_DEPTH) {
            drawForm(s, gs, depth);
        }
    }

    private void drawForm(PdfStream s, State gs, int depth) {
        State f = gs.copy();
        f.ctm = mul(matrix(s.getArray("Matrix")), gs.ctm);
        f.patternBase = f.ctm;
        double[] bbox = arr(s.getArray("BBox"), 4);
        dev.save();
        if (bbox != null) {
            PdfPath c = new PdfPath();
            rectPath(c, bbox[0], bbox[1], bbox[2] - bbox[0], bbox[3] - bbox[1], f.ctm);
            dev.clip(c, false);
            float[] b = c.getBounds();
            f.clip = intersect(f.clip, new double[] {b[0], b[1], b[2], b[3]});
        }
        PdfDict res = s.getDict("Resources");
        try {
            run(s.getData(), res != null ? res : gs.resources, f, depth + 1);
        } catch (java.io.IOException e) {
            // an unreadable form draws nothing
        }
        dev.restore();
    }

    private void drawImage(PdfStream s, State gs) {
        PdfImageDecoder img = PdfImageDecoder.decode(s, gs.resources, dev, gs.fillRgb);
        if (img == null) {
            return;
        }
        if (gs.fillAlpha < 1) {
            int a = (int) (gs.fillAlpha * 255 + 0.5);
            int[] px = img.argb;
            for (int i = 0; i < px.length; i++) {
                px[i] = ((((px[i] >>> 24) * a) / 255) << 24) | (px[i] & 0xffffff);
            }
        }
        double[] m = mul(new double[] {1.0 / img.width, 0, 0, -1.0 / img.height, 0, 1}, gs.ctm);
        dev.image(img.argb, img.width, img.height, m);
    }

    // ---- text ----

    private double[] show(final State gs, final double[] tmIn, final Object arg, final int depth) {
        if (gs.knockout != null && gs.renderMode != 3 && gs.renderMode != 7) {
            final double[][] res = new double[1][];
            int rm = gs.renderMode;
            boolean textFill = rm == 0 || rm == 2 || rm == 4 || rm == 6;
            knock(gs, textFill ? gs.fillAlpha : gs.strokeAlpha, new Runnable() {
                @Override
                public void run() {
                    State plain = gs.copy();
                    plain.knockout = null;
                    res[0] = show(plain, tmIn, arg, depth);
                }
            });
            return res[0] != null ? res[0] : tmIn;
        }
        if (!composited(gs) || gs.renderMode == 3 || gs.renderMode == 7) {
            return showNow(gs, tmIn, arg, depth);
        }
        final double[][] result = new double[1][];
        int[] r = layerRect(gs, gs.clip);
        if (r == null) {
            return showNow(gs, tmIn, arg, depth);
        }
        withLayer(gs, r, 1, new Runnable() {
            @Override
            public void run() {
                result[0] = showNow(gs, tmIn, arg, depth);
            }
        });
        return result[0] != null ? result[0] : tmIn;
    }

    private double[] showNow(State gs, double[] tmIn, Object arg, int depth) {
        double[] tm = tmIn;
        FontRes f = gs.font;
        if (f == null) {
            f = fontFromDict(null);
            gs.font = f;
        }
        List<?> items;
        if (arg instanceof List) {
            items = (List<?>) arg;
        } else {
            List<Object> one = new ArrayList<Object>(1);
            one.add(arg);
            items = one;
        }
        int mode = gs.renderMode;
        boolean fill = mode == 0 || mode == 2 || mode == 4 || mode == 6;
        boolean stroke = mode == 1 || mode == 2 || mode == 5 || mode == 6;
        PdfPath path = new PdfPath();
        double size = gs.fontSize;
        boolean vertical = f.type3 == null && f.decoder.isVertical();
        for (int i = 0; i < items.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object it = items.get(i);
            if (it instanceof Number) {
                if (vertical) {
                    double adj = -((Number) it).doubleValue() / 1000 * size;
                    tm = mul(new double[] {1, 0, 0, 1, 0, adj}, tm);
                } else {
                    double adj = -((Number) it).doubleValue() / 1000 * size * gs.hscale;
                    tm = mul(new double[] {1, 0, 0, 1, adj, 0}, tm);
                }
                continue;
            }
            if (!(it instanceof PdfString)) {
                continue;
            }
            byte[] s = ((PdfString) it).bytes;
            int p = 0;
            while (p < s.length) {
                int len = Math.max(1, f.decoder.codeLength(s, p));
                int code = PdfFontDecoder.read(s, p, len);
                p += len;
                double[] vm = vertical ? f.decoder.verticalMetrics(code) : null;
                double ox = vm == null ? 0 : -vm[1] / 1000 * size;
                double oy = vm == null ? gs.rise : -vm[2] / 1000 * size;
                double[] trm = mul(mul(new double[] {size * gs.hscale, 0, 0, size, ox, oy}, tm), gs.ctm);
                double adv;
                if (f.type3 != null) {
                    adv = type3Glyph(f, code, trm, gs, depth);
                } else {
                    adv = f.decoder.width(code) / 1000;
                    if (fill || stroke) {
                        if (!f.glyph(code, trm, path)) {
                            String t = f.decoder.text(code);
                            if (t.length() > 0 && !" ".equals(t)) {
                                dev.text(t, f.family, f.bold, f.italic, trm, argb(gs.fillRgb, gs.fillAlpha));
                            }
                        }
                    }
                }
                if (vm != null) {
                    double ty = vm[0] / 1000 * size + gs.charSpace;
                    if (len == 1 && code == 32) {
                        ty += gs.wordSpace;
                    }
                    tm = mul(new double[] {1, 0, 0, 1, 0, ty}, tm);
                    continue;
                }
                double w = adv * size + gs.charSpace;
                if (len == 1 && code == 32) {
                    w += gs.wordSpace;
                }
                tm = mul(new double[] {1, 0, 0, 1, w * gs.hscale, 0}, tm);
            }
        }
        if (!path.isEmpty()) {
            if (fill) {
                if (gs.fillPattern != null) {
                    fillPattern(path, gs, false);
                } else {
                    dev.fill(path, false, argb(gs.fillRgb, gs.fillAlpha));
                }
            }
            if (stroke) {
                double w = gs.lineWidth * Math.sqrt(Math.abs(gs.ctm[0] * gs.ctm[3] - gs.ctm[1] * gs.ctm[2]));
                dev.stroke(path, (float) Math.max(1, w), gs.cap, gs.join, (float) gs.miter,
                        argb(gs.strokeRgb, gs.strokeAlpha));
            }
        }
        return tm;
    }

    private double type3Glyph(FontRes f, int code, double[] trm, State gs, int depth) {
        String name = f.decoder.glyphName(code);
        Object proc = name == null || f.type3 == null ? null : f.type3.get(name);
        if (proc instanceof PdfStream && depth < MAX_DEPTH) {
            State g = gs.copy();
            g.ctm = mul(f.fontMatrix, trm);
            try {
                run(((PdfStream) proc).getData(), f.type3Resources != null ? f.type3Resources : gs.resources, g,
                        depth + 1);
            } catch (java.io.IOException e) {
                // an unreadable glyph draws nothing
            }
        }
        double w = f.decoder.width(code);
        return w * f.fontMatrix[0];
    }

    private FontRes font(PdfDict resources, Object name) {
        PdfDict fd = resources == null ? null : resources.getDict("Font");
        Object o = fd != null && name instanceof PdfName ? fd.get(((PdfName) name).value) : null;
        return fontFromDict(o);
    }

    private FontRes fontFromDict(Object o) {
        Object key = o == null ? "default" : o;
        FontRes f = fonts.get(key);
        if (f == null) {
            f = new FontRes(file, o instanceof PdfDict ? (PdfDict) o : null);
            fonts.put(key, f);
        }
        return f;
    }

    /// A font resource prepared for drawing.
    private static final class FontRes {
        final PdfFontDecoder decoder;
        TrueTypeOutlines tt;
        CffFont cff;
        Type1Font t1;
        double[] fontMatrix = {0.001, 0, 0, 0.001, 0, 0};
        int[] cidToGid;
        boolean symbolic;
        boolean cidFont;
        PdfDict type3;
        PdfDict type3Resources;
        int family;
        boolean bold;
        boolean italic;
        final Map<Integer, PdfPath> cache = new HashMap<Integer, PdfPath>();

        FontRes(PdfFile file, PdfDict font) {
            decoder = new PdfFontDecoder(font);
            if (font == null) {
                return;
            }
            String subtype = font.getName("Subtype");
            PdfDict desc = font;
            if ("Type0".equals(subtype)) {
                List<Object> df = font.getArray("DescendantFonts");
                Object d = df == null || df.isEmpty() ? null : file.resolve(df.get(0));
                if (d instanceof PdfDict) {
                    desc = (PdfDict) d;
                    cidFont = true;
                    Object map = desc.get("CIDToGIDMap");
                    if (map instanceof PdfStream) {
                        try {
                            byte[] m = ((PdfStream) map).getData();
                            cidToGid = new int[m.length / 2];
                            for (int i = 0; i < cidToGid.length; i++) {
                                cidToGid[i] = ((m[i * 2] & 0xff) << 8) | (m[i * 2 + 1] & 0xff);
                            }
                        } catch (java.io.IOException e) {
                            cidToGid = null;
                        }
                    }
                }
            }
            if ("Type3".equals(subtype)) {
                type3 = font.getDict("CharProcs");
                type3Resources = font.getDict("Resources");
                double[] m = matrix(font.getArray("FontMatrix"));
                fontMatrix = m;
                return;
            }
            String base = font.getName("BaseFont");
            PdfDict fdesc = desc.getDict("FontDescriptor");
            int flags = fdesc == null ? 0 : fdesc.getInt("Flags", 0);
            symbolic = (flags & 4) != 0;
            family = (flags & 1) != 0 ? 2 : (flags & 2) != 0 ? 1 : 0;
            if (base != null) {
                if (base.indexOf("Courier") >= 0 || base.indexOf("Mono") >= 0) {
                    family = 2;
                } else if (base.indexOf("Times") >= 0 || base.indexOf("Serif") >= 0 && base.indexOf("Sans") < 0
                        || base.indexOf("Georgia") >= 0 || base.indexOf("Garamond") >= 0
                        || base.indexOf("Roman") >= 0 || base.indexOf("Minion") >= 0) {
                    family = 1;
                }
                bold = base.indexOf("Bold") >= 0 || base.indexOf("Black") >= 0 || base.indexOf("Heavy") >= 0
                        || base.indexOf("Semibold") >= 0;
                italic = base.indexOf("Italic") >= 0 || base.indexOf("Oblique") >= 0;
            }
            if ((flags & 64) != 0) {
                italic = true;
            }
            if (fdesc != null && fdesc.getNumber("FontWeight", 400) >= 600) {
                bold = true;
            }
            if (fdesc == null) {
                return;
            }
            try {
                Object ff2 = fdesc.get("FontFile2");
                Object ff3 = fdesc.get("FontFile3");
                Object ff = fdesc.get("FontFile");
                if (ff2 instanceof PdfStream) {
                    tt = new TrueTypeOutlines(((PdfStream) ff2).getData());
                } else if (ff3 instanceof PdfStream) {
                    byte[] d = ((PdfStream) ff3).getData();
                    String st = ((PdfStream) ff3).getName("Subtype");
                    if ("OpenType".equals(st) && !hasCffTable(d)) {
                        tt = new TrueTypeOutlines(d);
                    } else {
                        cff = new CffFont(d);
                        fontMatrix = cff.getFontMatrix();
                    }
                } else if (ff instanceof PdfStream) {
                    t1 = new Type1Font(((PdfStream) ff).getData());
                    fontMatrix = t1.getFontMatrix();
                }
            } catch (java.io.IOException e) {
                tt = null;
                cff = null;
                t1 = null;
            } catch (RuntimeException e) {
                tt = null;
                cff = null;
                t1 = null;
            }
        }

        private boolean hasCffTable(byte[] d) {
            if (d.length < 12) {
                return false;
            }
            int n = ((d[4] & 0xff) << 8) | (d[5] & 0xff);
            for (int i = 0; i < n; i++) {
                int rec = 12 + i * 16;
                if (rec + 4 <= d.length && d[rec] == 'C' && d[rec + 1] == 'F' && d[rec + 2] == 'F') {
                    return true;
                }
            }
            return false;
        }

        /// Appends the outline of `code` under `trm` to `path`; false
        /// when the font has no outline for it (or none at all).
        boolean glyph(int code, double[] trm, PdfPath path) {
            if (tt == null && cff == null && t1 == null) {
                return false;
            }
            Integer key = Integer.valueOf(code);
            PdfPath em = cache.get(key);
            if (em == null) {
                em = new PdfPath();
                outline(code, em);
                cache.put(key, em);
            }
            if (em.isEmpty()) {
                // A space, or a code the font does not have: nothing to draw
                // either way, and no fallback for an embedded font.
                return true;
            }
            append(em, trm, path);
            return true;
        }

        private void outline(final int code, final PdfPath em) {
            if (tt != null) {
                int gid = ttGlyph(code);
                if (gid > 0 || cidFont) {
                    tt.outline(gid, sink(em, new double[] {1, 0, 0, 1, 0, 0}));
                }
                return;
            }
            if (cff != null) {
                int gid;
                if (cidFont) {
                    gid = cff.isCid() ? cff.glyphForCid(code) : code;
                } else {
                    String name = decoder.glyphName(code);
                    gid = symbolic || name == null ? cff.glyphForCode(code) : cff.glyphForName(name);
                    if (gid < 0) {
                        gid = name != null ? cff.glyphForName(name) : -1;
                    }
                    if (gid < 0) {
                        gid = cff.glyphForCode(code);
                    }
                }
                if (gid >= 0) {
                    cff.outline(gid, sink(em, fontMatrix));
                }
                return;
            }
            String name = decoder.glyphName(code);
            if (name == null || !t1.hasGlyph(name)) {
                name = t1.nameForCode(code);
            }
            if (name != null) {
                t1.outline(name, sink(em, fontMatrix));
            }
        }

        private int ttGlyph(int code) {
            if (cidFont) {
                if (cidToGid != null) {
                    return code < cidToGid.length ? cidToGid[code] : 0;
                }
                return code;
            }
            int g = 0;
            if (symbolic && tt.hasSymbolCmap()) {
                g = tt.glyphForSymbol(code);
            }
            if (g == 0 && tt.hasUnicodeCmap()) {
                String name = decoder.glyphName(code);
                String u = name == null ? "" : PdfFontDecoder.unicodeOf(name);
                if (u.length() > 0) {
                    g = tt.glyphForUnicode(u.charAt(0));
                }
            }
            if (g == 0) {
                g = tt.glyphForMac(code);
            }
            if (g == 0) {
                g = tt.glyphForSymbol(code);
            }
            if (g == 0 && !tt.hasUnicodeCmap() && !tt.hasSymbolCmap()) {
                g = code;
            }
            return g;
        }
    }

    private static GlyphSink sink(final PdfPath p, final double[] m) {
        return new GlyphSink() {
            @Override
            public void moveTo(double x, double y) {
                p.moveTo(tx(m, x, y), ty(m, x, y));
            }

            @Override
            public void lineTo(double x, double y) {
                p.lineTo(tx(m, x, y), ty(m, x, y));
            }

            @Override
            public void quadTo(double cx, double cy, double x, double y) {
                p.quadTo(tx(m, cx, cy), ty(m, cx, cy), tx(m, x, y), ty(m, x, y));
            }

            @Override
            public void curveTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
                p.curveTo(tx(m, c1x, c1y), ty(m, c1x, c1y), tx(m, c2x, c2y), ty(m, c2x, c2y), tx(m, x, y),
                        ty(m, x, y));
            }

            @Override
            public void closePath() {
                p.closePath();
            }
        };
    }

    private static void append(PdfPath em, double[] m, PdfPath out) {
        float[] pts = em.getPoints();
        int pi = 0;
        int n = em.getOpCount();
        for (int i = 0; i < n; i++) {
            switch (em.getOp(i)) {
                case PdfPath.MOVE:
                    out.moveTo(tx(m, pts[pi], pts[pi + 1]), ty(m, pts[pi], pts[pi + 1]));
                    pi += 2;
                    break;
                case PdfPath.LINE:
                    out.lineTo(tx(m, pts[pi], pts[pi + 1]), ty(m, pts[pi], pts[pi + 1]));
                    pi += 2;
                    break;
                case PdfPath.QUAD:
                    out.quadTo(tx(m, pts[pi], pts[pi + 1]), ty(m, pts[pi], pts[pi + 1]),
                            tx(m, pts[pi + 2], pts[pi + 3]), ty(m, pts[pi + 2], pts[pi + 3]));
                    pi += 4;
                    break;
                case PdfPath.CUBIC:
                    out.curveTo(tx(m, pts[pi], pts[pi + 1]), ty(m, pts[pi], pts[pi + 1]),
                            tx(m, pts[pi + 2], pts[pi + 3]), ty(m, pts[pi + 2], pts[pi + 3]),
                            tx(m, pts[pi + 4], pts[pi + 5]), ty(m, pts[pi + 4], pts[pi + 5]));
                    pi += 6;
                    break;
                default:
                    out.closePath();
                    break;
            }
        }
    }

    // ---- geometry helpers ----

    private static void curve(PdfPath p, double[] m, double x1, double y1, double x2, double y2, double x3,
            double y3) {
        p.curveTo(tx(m, x1, y1), ty(m, x1, y1), tx(m, x2, y2), ty(m, x2, y2), tx(m, x3, y3), ty(m, x3, y3));
    }

    private static void rectPath(PdfPath p, double x, double y, double w, double h, double[] m) {
        p.moveTo(tx(m, x, y), ty(m, x, y));
        p.lineTo(tx(m, x + w, y), ty(m, x + w, y));
        p.lineTo(tx(m, x + w, y + h), ty(m, x + w, y + h));
        p.lineTo(tx(m, x, y + h), ty(m, x, y + h));
        p.closePath();
    }

    private static double tx(double[] m, double x, double y) {
        return m[0] * x + m[2] * y + m[4];
    }

    private static double ty(double[] m, double x, double y) {
        return m[1] * x + m[3] * y + m[5];
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    private static double[] nums(List<?> l, int from) {
        double[] v = new double[6];
        for (int i = 0; i < 6; i++) {
            v[i] = from + i < l.size() ? num(l.get(from + i)) : 0;
        }
        return v;
    }

    private static double[] nums4(List<?> l, int from) {
        double[] v = new double[4];
        for (int i = 0; i < 4; i++) {
            v[i] = num(l.get(from + i));
        }
        return v;
    }

    /// The first `count` numbers of array `l`, or null when it has fewer.
    private static double[] arr(List<Object> l, int count) {
        if (l == null || l.size() < count) {
            return null;
        }
        double[] v = new double[count];
        for (int i = 0; i < count; i++) {
            v[i] = num(l.get(i));
        }
        return v;
    }

    private static double[] matrix(List<Object> l) {
        double[] m = arr(l, 6);
        return m == null ? new double[] {1, 0, 0, 1, 0, 0} : m;
    }

    static double[] mul(double[] a, double[] b) {
        return PdfTextExtractor.mul(a, b);
    }

    private static double[] invert(double[] m) {
        double det = m[0] * m[3] - m[1] * m[2];
        if (det == 0) {
            return null;
        }
        return new double[] {m[3] / det, -m[1] / det, -m[2] / det, m[0] / det,
            (m[2] * m[5] - m[3] * m[4]) / det, (m[1] * m[4] - m[0] * m[5]) / det};
    }
}
