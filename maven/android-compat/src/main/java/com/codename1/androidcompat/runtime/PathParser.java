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
package com.codename1.androidcompat.runtime;

import android.graphics.Path;
import com.codename1.util.MathUtil;

/// Parses SVG path data -- the `android:pathData` of a vector drawable --
/// into a [Path]. Supports every command of the SVG path grammar, relative
/// and absolute, including elliptical arcs, and the compact number syntax
/// (`1.5.5`, `-1-2`, exponents).
public final class PathParser {

    private final String s;
    private int pos;

    private PathParser(String s) {
        this.s = s;
    }

    public static Path createPathFromPathData(String data) {
        Path p = new Path();
        if (data != null) {
            new PathParser(data).parse(p);
        }
        return p;
    }

    private void skipSeparators() {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == ' ' || c == ',' || c == '\n' || c == '\r' || c == '\t') {
                pos++;
            } else {
                break;
            }
        }
    }

    private boolean hasNumber() {
        skipSeparators();
        if (pos >= s.length()) {
            return false;
        }
        char c = s.charAt(pos);
        return (c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.';
    }

    private float number() {
        skipSeparators();
        int start = pos;
        if (pos < s.length() && (s.charAt(pos) == '-' || s.charAt(pos) == '+')) {
            pos++;
        }
        boolean dot = false;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' && !dot) {
                dot = true;
                pos++;
            } else if ((c == 'e' || c == 'E') && pos + 1 < s.length()) {
                pos++;
                if (s.charAt(pos) == '-' || s.charAt(pos) == '+') {
                    pos++;
                }
            } else {
                break;
            }
        }
        if (start == pos) {
            pos++;
            return 0;
        }
        return Float.parseFloat(s.substring(start, pos));
    }

    /// Arc flags may be written without separators (`a1 1 0 01 1 1`).
    private boolean flag() {
        skipSeparators();
        char c = pos < s.length() ? s.charAt(pos) : '0';
        pos++;
        return c == '1';
    }

    private void parse(Path p) {
        float cx = 0;
        float cy = 0;
        float sx = 0;
        float sy = 0;
        float lastCtrlX = 0;
        float lastCtrlY = 0;
        char lastCmd = ' ';
        while (true) {
            skipSeparators();
            if (pos >= s.length()) {
                return;
            }
            char cmd = s.charAt(pos);
            if (Character.isDigit(cmd) || cmd == '-' || cmd == '+' || cmd == '.') {
                // Implicit repetition of the previous command; a moveto
                // repeats as a lineto.
                cmd = lastCmd == 'M' ? 'L' : lastCmd == 'm' ? 'l' : lastCmd;
            } else {
                pos++;
            }
            boolean rel = Character.isLowerCase(cmd);
            char up = Character.toUpperCase(cmd);
            switch (up) {
                case 'M': {
                    float x = number();
                    float y = number();
                    if (rel) {
                        x += cx;
                        y += cy;
                    }
                    p.moveTo(x, y);
                    cx = sx = x;
                    cy = sy = y;
                    lastCmd = cmd;
                    while (hasNumber()) {
                        float lx = number();
                        float ly = number();
                        if (rel) {
                            lx += cx;
                            ly += cy;
                        }
                        p.lineTo(lx, ly);
                        cx = lx;
                        cy = ly;
                    }
                    break;
                }
                case 'L':
                    do {
                        float x = number();
                        float y = number();
                        if (rel) {
                            x += cx;
                            y += cy;
                        }
                        p.lineTo(x, y);
                        cx = x;
                        cy = y;
                    } while (hasNumber());
                    break;
                case 'H':
                    do {
                        float x = number();
                        if (rel) {
                            x += cx;
                        }
                        p.lineTo(x, cy);
                        cx = x;
                    } while (hasNumber());
                    break;
                case 'V':
                    do {
                        float y = number();
                        if (rel) {
                            y += cy;
                        }
                        p.lineTo(cx, y);
                        cy = y;
                    } while (hasNumber());
                    break;
                case 'C':
                    do {
                        float x1 = number();
                        float y1 = number();
                        float x2 = number();
                        float y2 = number();
                        float x = number();
                        float y = number();
                        if (rel) {
                            x1 += cx;
                            y1 += cy;
                            x2 += cx;
                            y2 += cy;
                            x += cx;
                            y += cy;
                        }
                        p.cubicTo(x1, y1, x2, y2, x, y);
                        lastCtrlX = x2;
                        lastCtrlY = y2;
                        cx = x;
                        cy = y;
                    } while (hasNumber());
                    break;
                case 'S':
                    do {
                        float x1 = 2 * cx - lastCtrlX;
                        float y1 = 2 * cy - lastCtrlY;
                        if (Character.toUpperCase(lastCmd) != 'C' && Character.toUpperCase(lastCmd) != 'S') {
                            x1 = cx;
                            y1 = cy;
                        }
                        float x2 = number();
                        float y2 = number();
                        float x = number();
                        float y = number();
                        if (rel) {
                            x2 += cx;
                            y2 += cy;
                            x += cx;
                            y += cy;
                        }
                        p.cubicTo(x1, y1, x2, y2, x, y);
                        lastCtrlX = x2;
                        lastCtrlY = y2;
                        cx = x;
                        cy = y;
                        lastCmd = 'S';
                    } while (hasNumber());
                    break;
                case 'Q':
                    do {
                        float x1 = number();
                        float y1 = number();
                        float x = number();
                        float y = number();
                        if (rel) {
                            x1 += cx;
                            y1 += cy;
                            x += cx;
                            y += cy;
                        }
                        p.quadTo(x1, y1, x, y);
                        lastCtrlX = x1;
                        lastCtrlY = y1;
                        cx = x;
                        cy = y;
                    } while (hasNumber());
                    break;
                case 'T':
                    do {
                        float x1 = 2 * cx - lastCtrlX;
                        float y1 = 2 * cy - lastCtrlY;
                        if (Character.toUpperCase(lastCmd) != 'Q' && Character.toUpperCase(lastCmd) != 'T') {
                            x1 = cx;
                            y1 = cy;
                        }
                        float x = number();
                        float y = number();
                        if (rel) {
                            x += cx;
                            y += cy;
                        }
                        p.quadTo(x1, y1, x, y);
                        lastCtrlX = x1;
                        lastCtrlY = y1;
                        cx = x;
                        cy = y;
                        lastCmd = 'T';
                    } while (hasNumber());
                    break;
                case 'A':
                    do {
                        float rx = number();
                        float ry = number();
                        float rot = number();
                        boolean large = flag();
                        boolean sweep = flag();
                        float x = number();
                        float y = number();
                        if (rel) {
                            x += cx;
                            y += cy;
                        }
                        arc(p, cx, cy, x, y, rx, ry, rot, large, sweep);
                        cx = x;
                        cy = y;
                    } while (hasNumber());
                    break;
                case 'Z':
                    p.close();
                    cx = sx;
                    cy = sy;
                    break;
                default:
                    // Unknown command letter: stop rather than misread the rest.
                    return;
            }
            if (up != 'S' && up != 'T') {
                lastCmd = cmd;
            }
            if (up != 'C' && up != 'S' && up != 'Q' && up != 'T') {
                lastCtrlX = cx;
                lastCtrlY = cy;
            }
        }
    }

    /// SVG endpoint arc to cubic Beziers (SVG 1.1 implementation notes F.6).
    private static void arc(Path p, float x0, float y0, float x1, float y1, float rx, float ry, float angleDeg,
                            boolean largeArc, boolean sweep) {
        if (x0 == x1 && y0 == y1) {
            return;
        }
        if (rx == 0 || ry == 0) {
            p.lineTo(x1, y1);
            return;
        }
        double phi = Math.toRadians(angleDeg % 360);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);
        double dx = (x0 - x1) / 2.0;
        double dy = (y0 - y1) / 2.0;
        double x1p = cosPhi * dx + sinPhi * dy;
        double y1p = -sinPhi * dx + cosPhi * dy;
        double arx = Math.abs(rx);
        double ary = Math.abs(ry);
        double lambda = (x1p * x1p) / (arx * arx) + (y1p * y1p) / (ary * ary);
        if (lambda > 1) {
            double sq = Math.sqrt(lambda);
            arx *= sq;
            ary *= sq;
        }
        double num = arx * arx * ary * ary - arx * arx * y1p * y1p - ary * ary * x1p * x1p;
        double den = arx * arx * y1p * y1p + ary * ary * x1p * x1p;
        double coef = den == 0 ? 0 : Math.sqrt(Math.max(0, num / den));
        if (largeArc == sweep) {
            coef = -coef;
        }
        double cxp = coef * (arx * y1p / ary);
        double cyp = coef * -(ary * x1p / arx);
        double cx = cosPhi * cxp - sinPhi * cyp + (x0 + x1) / 2.0;
        double cy = sinPhi * cxp + cosPhi * cyp + (y0 + y1) / 2.0;
        double theta1 = angle(1, 0, (x1p - cxp) / arx, (y1p - cyp) / ary);
        double dtheta = angle((x1p - cxp) / arx, (y1p - cyp) / ary, (-x1p - cxp) / arx, (-y1p - cyp) / ary);
        if (!sweep && dtheta > 0) {
            dtheta -= 2 * Math.PI;
        } else if (sweep && dtheta < 0) {
            dtheta += 2 * Math.PI;
        }
        int segments = (int) Math.ceil(Math.abs(dtheta) / (Math.PI / 2));
        double step = dtheta / segments;
        double k = 4.0 / 3.0 * Math.tan(step / 4);
        double t = theta1;
        for (int i = 0; i < segments; i++) {
            double c0 = Math.cos(t);
            double s0 = Math.sin(t);
            double c1 = Math.cos(t + step);
            double s1 = Math.sin(t + step);
            double ex0 = c0 - k * s0;
            double ey0 = s0 + k * c0;
            double ex1 = c1 + k * s1;
            double ey1 = s1 - k * c1;
            p.cubicTo(
                    (float) (cx + arx * cosPhi * ex0 - ary * sinPhi * ey0),
                    (float) (cy + arx * sinPhi * ex0 + ary * cosPhi * ey0),
                    (float) (cx + arx * cosPhi * ex1 - ary * sinPhi * ey1),
                    (float) (cy + arx * sinPhi * ex1 + ary * cosPhi * ey1),
                    (float) (cx + arx * cosPhi * c1 - ary * sinPhi * s1),
                    (float) (cy + arx * sinPhi * c1 + ary * cosPhi * s1));
            t += step;
        }
    }

    /// The signed angle from u to v, in (-pi, pi].
    private static double angle(double ux, double uy, double vx, double vy) {
        double d = MathUtil.atan2(vy, vx) - MathUtil.atan2(uy, ux);
        if (d > Math.PI || d <= -Math.PI) {
            d -= 2 * Math.PI * Math.ceil((d - Math.PI) / (2 * Math.PI));
        }
        return d;
    }
}
