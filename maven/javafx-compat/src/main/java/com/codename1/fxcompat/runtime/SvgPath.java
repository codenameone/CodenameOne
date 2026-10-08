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
package com.codename1.fxcompat.runtime;

/// Reads SVG path data, the `d` attribute of an SVG path, into an
/// [FxPath].
///
/// Every command is understood, in its absolute and relative form: `M`,
/// `L`, `H`, `V`, `C`, `S`, `Q`, `T`, `A` and `Z`. So are the compact
/// spellings the grammar allows: coordinates after a command repeat it
/// (after a move they are lines), a sign or a second decimal point starts
/// the next number (`1-2`, `.5.5`), numbers may carry an exponent and the
/// two flags of an arc need no separator. Elliptical arcs become cubic
/// curves.
///
/// Data that stops making sense ends the path there: what was read before
/// the error is kept, which is what JavaFX and SVG renderers do.
public final class SvgPath {

    private final String s;
    private int i;
    private boolean failed;

    private SvgPath(String data) {
        this.s = data;
    }

    private void skip() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                i++;
            } else {
                break;
            }
        }
    }

    private static boolean digit(char c) {
        return c >= '0' && c <= '9';
    }

    private double number() {
        skip();
        int start = i;
        int n = s.length();
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        int digits = 0;
        while (i < n && digit(s.charAt(i))) {
            i++;
            digits++;
        }
        if (i < n && s.charAt(i) == '.') {
            i++;
            while (i < n && digit(s.charAt(i))) {
                i++;
                digits++;
            }
        }
        if (digits == 0) {
            failed = true;
            return 0;
        }
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int e = i + 1;
            if (e < n && (s.charAt(e) == '+' || s.charAt(e) == '-')) {
                e++;
            }
            if (e < n && digit(s.charAt(e))) {
                while (e < n && digit(s.charAt(e))) {
                    e++;
                }
                i = e;
            }
        }
        try {
            return Double.parseDouble(s.substring(start, i));
        } catch (NumberFormatException malformed) {
            failed = true;
            return 0;
        }
    }

    private boolean flag() {
        skip();
        if (i < s.length() && (s.charAt(i) == '0' || s.charAt(i) == '1')) {
            return s.charAt(i++) == '1';
        }
        failed = true;
        return false;
    }

    private static boolean isCommand(char c) {
        return "MmLlHhVvCcSsQqTtAaZz".indexOf(c) >= 0;
    }

    /// Appends path data to a path. Answers `false` when the data was
    /// malformed; the path then holds everything up to the error. `null`
    /// and empty data add nothing and are well formed. Data for an empty
    /// path has to start with a move; data for a path that has segments
    /// may continue from its current point.
    public static boolean append(String data, FxPath out) {
        if (data == null) {
            return true;
        }
        return new SvgPath(data).read(out);
    }

    private boolean read(FxPath out) {
        char command = 0;
        char previous = 0;
        // Data appended to a path that is already under way may draw on
        // from its current point without a move of its own.
        boolean continues = !out.isEmpty();
        double cx = out.currentX();
        double cy = out.currentY();
        double sx = out.startX();
        double sy = out.startY();
        double kx = 0;
        double ky = 0;
        boolean reopen = false;
        while (true) {
            skip();
            if (i >= s.length()) {
                return true;
            }
            char ch = s.charAt(i);
            if (isCommand(ch)) {
                if (command == 0 && ch != 'M' && ch != 'm' && !continues) {
                    return false;
                }
                command = ch;
                i++;
            } else if (command == 0 || command == 'Z' || command == 'z') {
                return false;
            }
            boolean relative = command >= 'a';
            char kind = relative ? (char) (command - ('a' - 'A')) : command;
            double ox = relative ? cx : 0;
            double oy = relative ? cy : 0;
            if (kind == 'Z') {
                out.closePath();
                cx = sx;
                cy = sy;
                reopen = true;
                previous = kind;
                continue;
            }
            double x1 = 0;
            double y1 = 0;
            double x2 = 0;
            double y2 = 0;
            double x = cx;
            double y = cy;
            double rx = 0;
            double ry = 0;
            double rotation = 0;
            boolean large = false;
            boolean sweep = false;
            switch (kind) {
                case 'H':
                    x = ox + number();
                    break;
                case 'V':
                    y = oy + number();
                    break;
                case 'C':
                    x1 = ox + number();
                    y1 = oy + number();
                    x2 = ox + number();
                    y2 = oy + number();
                    x = ox + number();
                    y = oy + number();
                    break;
                case 'S':
                    x2 = ox + number();
                    y2 = oy + number();
                    x = ox + number();
                    y = oy + number();
                    break;
                case 'Q':
                    x1 = ox + number();
                    y1 = oy + number();
                    x = ox + number();
                    y = oy + number();
                    break;
                case 'A':
                    rx = number();
                    ry = number();
                    rotation = number();
                    large = flag();
                    sweep = flag();
                    x = ox + number();
                    y = oy + number();
                    break;
                default:
                    // M, L and T take one point.
                    x = ox + number();
                    y = oy + number();
                    break;
            }
            if (failed) {
                return false;
            }
            if (kind == 'M') {
                out.moveTo(x, y);
                sx = x;
                sy = y;
                reopen = false;
                // Further points after a move are lines.
                command = relative ? 'l' : 'L';
            } else {
                if (reopen) {
                    out.moveTo(sx, sy);
                    reopen = false;
                }
                if (kind == 'C') {
                    out.curveTo(x1, y1, x2, y2, x, y);
                    kx = x2;
                    ky = y2;
                } else if (kind == 'S') {
                    boolean smooth = previous == 'C' || previous == 'S';
                    out.curveTo(smooth ? 2 * cx - kx : cx, smooth ? 2 * cy - ky : cy, x2, y2, x, y);
                    kx = x2;
                    ky = y2;
                } else if (kind == 'Q') {
                    out.quadTo(x1, y1, x, y);
                    kx = x1;
                    ky = y1;
                } else if (kind == 'T') {
                    boolean smooth = previous == 'Q' || previous == 'T';
                    kx = smooth ? 2 * cx - kx : cx;
                    ky = smooth ? 2 * cy - ky : cy;
                    out.quadTo(kx, ky, x, y);
                } else if (kind == 'A') {
                    out.arcTo(rx, ry, rotation, large, sweep, x, y);
                } else {
                    out.lineTo(x, y);
                }
            }
            cx = x;
            cy = y;
            previous = kind;
        }
    }
}
