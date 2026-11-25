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
package com.codename1.art;

import com.codename1.ui.geom.GeneralPath;
import com.codename1.util.MathUtil;

/// Parses SVG path data (the `d` attribute) into a `GeneralPath`.
///
/// Supports every command of SVG 1.1 in absolute and relative form, implicit repeats,
/// and elliptical arcs, which are converted to cubic curves. Figma's `fillGeometry` uses the
/// same syntax. Malformed data ends the path at the last complete command, as browsers do,
/// rather than throwing.
public final class PathData {
    private final String s;
    private int pos;

    private PathData(String s) {
        this.s = s;
    }

    /// Parses path data.
    ///
    /// #### Parameters
    ///
    /// - `d`: the path data
    ///
    /// #### Returns
    ///
    /// the path; empty when `d` is null or has no valid command
    public static GeneralPath parse(String d) {
        GeneralPath p = new GeneralPath();
        if (d != null) {
            new PathData(d).run(p);
        }
        return p;
    }

    private void run(GeneralPath p) {
        double cx = 0;
        double cy = 0;
        double sx = 0;
        double sy = 0;
        double lcx = 0;
        double lcy = 0;
        char last = 0;
        char cmd = 0;
        boolean open = false;
        while (true) {
            skipSep();
            if (pos >= s.length()) {
                return;
            }
            char c = s.charAt(pos);
            if (isCommand(c)) {
                cmd = c;
                pos++;
            } else if (cmd == 0 || cmd == 'Z' || cmd == 'z') {
                return;
            }
            boolean rel = cmd >= 'a';
            char u = rel ? (char) (cmd - 32) : cmd;
            double ox = rel ? cx : 0;
            double oy = rel ? cy : 0;
            try {
                switch (u) {
                    case 'M':
                        cx = ox + num();
                        cy = oy + num();
                        p.moveTo(cx, cy);
                        open = true;
                        sx = cx;
                        sy = cy;
                        cmd = rel ? 'l' : 'L';
                        break;
                    case 'L':
                        cx = ox + num();
                        cy = oy + num();
                        p.lineTo(cx, cy);
                        break;
                    case 'H':
                        cx = ox + num();
                        p.lineTo(cx, cy);
                        break;
                    case 'V':
                        cy = oy + num();
                        p.lineTo(cx, cy);
                        break;
                    case 'C': {
                        double x1 = ox + num();
                        double y1 = oy + num();
                        lcx = ox + num();
                        lcy = oy + num();
                        cx = ox + num();
                        cy = oy + num();
                        p.curveTo(x1, y1, lcx, lcy, cx, cy);
                        break;
                    }
                    case 'S': {
                        double x1 = cx;
                        double y1 = cy;
                        if (last == 'C' || last == 'S') {
                            x1 = 2 * cx - lcx;
                            y1 = 2 * cy - lcy;
                        }
                        lcx = ox + num();
                        lcy = oy + num();
                        cx = ox + num();
                        cy = oy + num();
                        p.curveTo(x1, y1, lcx, lcy, cx, cy);
                        break;
                    }
                    case 'Q':
                        lcx = ox + num();
                        lcy = oy + num();
                        cx = ox + num();
                        cy = oy + num();
                        p.quadTo(lcx, lcy, cx, cy);
                        break;
                    case 'T':
                        if (last == 'Q' || last == 'T') {
                            lcx = 2 * cx - lcx;
                            lcy = 2 * cy - lcy;
                        } else {
                            lcx = cx;
                            lcy = cy;
                        }
                        cx = ox + num();
                        cy = oy + num();
                        p.quadTo(lcx, lcy, cx, cy);
                        break;
                    case 'A': {
                        double rx = Math.abs(num());
                        double ry = Math.abs(num());
                        double rot = num();
                        boolean large = flag();
                        boolean sweep = flag();
                        double x = ox + num();
                        double y = oy + num();
                        arc(p, cx, cy, rx, ry, rot, large, sweep, x, y);
                        cx = x;
                        cy = y;
                        break;
                    }
                    case 'Z':
                        if (open) {
                            p.closePath();
                        }
                        cx = sx;
                        cy = sy;
                        break;
                    default:
                        return;
                }
            } catch (NumberFormatException e) {
                return;
            }
            last = u;
            if (!open && u != 'M') {
                return;
            }
        }
    }

    private static boolean isCommand(char c) {
        return "MmLlHhVvCcSsQqTtAaZz".indexOf(c) >= 0;
    }

    private void skipSep() {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                pos++;
            } else {
                return;
            }
        }
    }

    private boolean flag() {
        skipSep();
        if (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '0' || c == '1') {
                pos++;
                return c == '1';
            }
        }
        throw new NumberFormatException("flag");
    }

    private double num() {
        skipSep();
        int start = pos;
        int n = s.length();
        if (pos < n && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) {
            pos++;
        }
        boolean digits = false;
        while (pos < n && isDigit(s.charAt(pos))) {
            pos++;
            digits = true;
        }
        if (pos < n && s.charAt(pos) == '.') {
            pos++;
            while (pos < n && isDigit(s.charAt(pos))) {
                pos++;
                digits = true;
            }
        }
        if (!digits) {
            pos = start;
            throw new NumberFormatException("number");
        }
        if (pos < n && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
            int mark = pos;
            pos++;
            if (pos < n && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) {
                pos++;
            }
            int ds = pos;
            while (pos < n && isDigit(s.charAt(pos))) {
                pos++;
            }
            if (pos == ds) {
                pos = mark;
            }
        }
        return Double.parseDouble(s.substring(start, pos));
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /// Appends an SVG elliptical arc as cubic curves (SVG 1.1 implementation notes, F.6).
    static void arc(GeneralPath p, double x1, double y1, double rx, double ry, double angleDeg,
            boolean large, boolean sweep, double x2, double y2) {
        if (x1 == x2 && y1 == y2) {
            return;
        }
        if (rx == 0 || ry == 0) {
            p.lineTo(x2, y2);
            return;
        }
        double phi = Math.toRadians(angleDeg);
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);
        double dx = (x1 - x2) / 2;
        double dy = (y1 - y2) / 2;
        double x1p = cos * dx + sin * dy;
        double y1p = -sin * dx + cos * dy;
        double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
        if (lambda > 1) {
            double sq = Math.sqrt(lambda);
            rx *= sq;
            ry *= sq;
        }
        double num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
        double den = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
        double coef = den == 0 ? 0 : Math.sqrt(Math.max(0, num / den));
        if (large == sweep) {
            coef = -coef;
        }
        double cxp = coef * rx * y1p / ry;
        double cyp = -coef * ry * x1p / rx;
        double cx = cos * cxp - sin * cyp + (x1 + x2) / 2;
        double cy = sin * cxp + cos * cyp + (y1 + y2) / 2;
        double theta1 = angle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry);
        double dtheta = angle((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry);
        if (!sweep && dtheta > 0) {
            dtheta -= 2 * Math.PI;
        } else if (sweep && dtheta < 0) {
            dtheta += 2 * Math.PI;
        }
        int segs = (int) Math.ceil(Math.abs(dtheta) / (Math.PI / 2) - 1e-9);
        if (segs < 1) {
            segs = 1;
        }
        double delta = dtheta / segs;
        double t = 4.0 / 3.0 * Math.tan(delta / 4);
        double th = theta1;
        for (int i = 0; i < segs; i++) {
            double c1 = Math.cos(th);
            double s1 = Math.sin(th);
            double th2 = th + delta;
            double c2 = Math.cos(th2);
            double s2 = Math.sin(th2);
            double ax = c1 - t * s1;
            double ay = s1 + t * c1;
            double bx = c2 + t * s2;
            double by = s2 - t * c2;
            p.curveTo(
                    cx + rx * cos * ax - ry * sin * ay, cy + rx * sin * ax + ry * cos * ay,
                    cx + rx * cos * bx - ry * sin * by, cy + rx * sin * bx + ry * cos * by,
                    cx + rx * cos * c2 - ry * sin * s2, cy + rx * sin * c2 + ry * cos * s2);
            th = th2;
        }
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        return MathUtil.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
    }
}
