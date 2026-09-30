/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.util.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Places and draws text labels with greedy collision avoidance: a label is
/// only drawn when its bounding box does not overlap one already placed this
/// frame. Each label is rendered with a one-pixel halo for legibility over
/// busy map content. This is a lightweight stand-in for a full label engine,
/// adequate for place names on a basemap.
///
/// Road names are laid along the road itself ([#placeAlongLine]): each glyph
/// sits on the line, turned to its direction, and the name repeats along a
/// long road the way street maps show it.
final class LabelEngine {

    // Largest turn between neighbouring glyphs before a spot is judged too
    // bendy to read; a name on a hairpin is dropped rather than scrambled.
    private static final double MAX_GLYPH_TURN = Math.PI / 4;

    // Below this spread every glyph shares one angle, so the name is drawn as
    // a single rotated string and keeps the font's kerning.
    private static final double STRAIGHT_TOLERANCE = 0.035;

    private final List occupied = new ArrayList();
    private final Map placedAnchors = new HashMap();
    private final Map fontCache = new HashMap();
    private Transform scratch;

    /// Clears placements at the start of a frame.
    void reset() {
        occupied.clear();
        placedAnchors.clear();
    }

    /// Lays `text` along the polyline `pts` (interleaved screen `x,y`),
    /// starting at the middle of the line and repeating every `repeat` pixels
    /// in both directions while the line is long enough to hold it. A copy is
    /// skipped when its centre is outside `left,top..right,bottom`, when the
    /// line bends too sharply under it, when it collides with a label already
    /// placed, or when the same name was placed less than `repeat` pixels away
    /// (neighbouring tiles each carry their own piece of a long road). Text is
    /// kept upright, reading left to right.
    ///
    /// Without affine transforms the name is placed horizontally, as [#place]
    /// would, at the middle of the longest stretch of the line inside the
    /// bounds.
    ///
    /// Returns true when at least one copy was drawn.
    boolean placeAlongLine(Graphics g, String text, double sizePx, int textColor, int haloColor,
                           double[] pts, double repeat, int left, int top, int right, int bottom) {
        if (text == null || text.length() == 0 || pts == null || pts.length < 4) {
            return false;
        }
        int n = pts.length / 2;
        double[] cum = new double[n];
        for (int i = 1; i < n; i++) {
            double dx = pts[i * 2] - pts[i * 2 - 2];
            double dy = pts[i * 2 + 1] - pts[i * 2 - 1];
            cum[i] = cum[i - 1] + Math.sqrt(dx * dx + dy * dy);
        }
        double total = cum[n - 1];
        if (total <= 0) {
            return false;
        }
        double mid = total / 2;
        if (!g.isAffineSupported()) {
            // Not the whole line's midpoint: an overzoomed road carries its
            // entire parent-tile geometry, whose middle is often far off screen
            // while the part in view goes unnamed.
            double at = visibleMiddle(pts, cum, left, top, right, bottom);
            if (at < 0) {
                return false;
            }
            double[] p = pointAt(pts, cum, at);
            return place(g, text, sizePx, textColor, haloColor, round(p[0]), round(p[1]));
        }
        Font font = fontFor(sizePx);
        int h = font.getHeight();
        boolean perGlyph = drawsCharByChar(text);
        int[] widths;
        int textW;
        if (perGlyph) {
            int len = text.length();
            widths = new int[len];
            textW = 0;
            for (int i = 0; i < len; i++) {
                widths[i] = font.charWidth(text.charAt(i));
                textW += widths[i];
            }
        } else {
            // Shaped text is measured and drawn whole. The cells only sample
            // the road under it, for the bend check and the collision boxes.
            textW = font.stringWidth(text);
            int cells = Math.max(1, (textW + h - 1) / Math.max(1, h));
            widths = new int[cells];
            for (int i = 0; i < cells; i++) {
                widths[i] = textW * (i + 1) / cells - textW * i / cells;
            }
        }
        if (textW <= 0 || total < textW + h) {
            return false;
        }
        if (repeat < textW + h) {
            repeat = textW + h;
        }
        boolean placedAny = false;
        for (int k = 0; mid - k * repeat - textW / 2.0 >= 0; k++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                if (k == 0 && sign < 0) {
                    continue;
                }
                double d = mid + sign * k * repeat;
                if (d - textW / 2.0 < 0 || d + textW / 2.0 > total) {
                    continue;
                }
                if (placeCopy(g, text, font, widths, textW, h, perGlyph, textColor, haloColor, pts,
                        cum, d, repeat, left, top, right, bottom)) {
                    placedAny = true;
                }
            }
        }
        return placedAny;
    }

    private boolean placeCopy(Graphics g, String text, Font font, int[] widths, int textW, int h,
                              boolean perGlyph, int textColor, int haloColor, double[] pts,
                              double[] cum, double d, double repeat, int left, int top, int right,
                              int bottom) {
        double[] center = pointAt(pts, cum, d);
        if (center[0] < left || center[0] > right || center[1] < top || center[1] > bottom) {
            return false;
        }
        List anchors = (List) placedAnchors.get(text);
        if (anchors != null) {
            for (Object a : anchors) {
                double[] other = (double[]) a;
                double dx = other[0] - center[0];
                double dy = other[1] - center[1];
                if (dx * dx + dy * dy < repeat * repeat) {
                    return false;
                }
            }
        }
        double[] start = pointAt(pts, cum, d - textW / 2.0);
        double[] end = pointAt(pts, cum, d + textW / 2.0);
        // Read left to right: when the line runs leftwards under the text,
        // walk it backwards and turn every glyph half a revolution.
        boolean reverse = end[0] < start[0];
        int len = widths.length;
        double[] gx = new double[len];
        double[] gy = new double[len];
        double[] ga = new double[len];
        int[][] boxes = new int[len][];
        double advance = 0;
        for (int i = 0; i < len; i++) {
            double offset = advance + widths[i] / 2.0;
            advance += widths[i];
            double at = reverse ? d + textW / 2.0 - offset : d - textW / 2.0 + offset;
            double[] p = pointAt(pts, cum, at);
            double angle = p[2];
            if (reverse) {
                angle += Math.PI;
            }
            if (i > 0 && Math.abs(normalize(angle - ga[i - 1])) > MAX_GLYPH_TURN) {
                return false;
            }
            gx[i] = p[0];
            gy[i] = p[1];
            ga[i] = angle;
            double c = Math.abs(Math.cos(angle));
            double s = Math.abs(Math.sin(angle));
            int ex = (int) Math.ceil(c * widths[i] / 2.0 + s * h / 2.0) + 1;
            int ey = (int) Math.ceil(s * widths[i] / 2.0 + c * h / 2.0) + 1;
            int[] box = new int[]{round(p[0]) - ex, round(p[1]) - ey, ex * 2, ey * 2};
            for (Object occItem : occupied) {
                int[] o = (int[]) occItem;
                if (intersects(box[0], box[1], box[2], box[3], o[0], o[1], o[2], o[3])) {
                    return false;
                }
            }
            boxes[i] = box;
        }
        // Drawn whole, the text is a straight line from its first cell to its
        // last; keep it only where the road stays within half a line of that,
        // or the name would leave the road on a bend.
        double chordX = reverse ? start[0] - end[0] : end[0] - start[0];
        double chordY = reverse ? start[1] - end[1] : end[1] - start[1];
        double chordLen = Math.sqrt(chordX * chordX + chordY * chordY);
        if (!perGlyph) {
            if (chordLen <= 0) {
                return false;
            }
            for (int i = 0; i < len; i++) {
                double off = ((gx[i] - start[0]) * chordY - (gy[i] - start[1]) * chordX) / chordLen;
                if (Math.abs(off) > h / 2.0) {
                    return false;
                }
            }
        }
        for (int[] box : boxes) {
            occupied.add(box);
        }
        if (anchors == null) {
            anchors = new ArrayList();
            placedAnchors.put(text, anchors);
        }
        anchors.add(new double[]{center[0], center[1]});
        if (perGlyph) {
            drawAlong(g, text, font, widths, h, textColor, haloColor, center, gx, gy, ga);
        } else {
            drawRotated(g, text, font, h, textColor, haloColor, (start[0] + end[0]) / 2,
                    (start[1] + end[1]) / 2, MathUtil.atan2(chordY, chordX));
        }
        return true;
    }

    // Whether each char of `text` renders the same drawn on its own, which
    // laying glyphs one by one along a bend requires. That holds for Latin,
    // Greek, Cyrillic and CJK without combining marks. It does not hold for
    // scripts whose letters join or reorder (Arabic, Hebrew, the Indic
    // scripts), for combining marks, which would detach from their base, or
    // for surrogate pairs, which would split into two invalid halves -- such
    // names are drawn whole, where the platform shapes them.
    static boolean drawsCharByChar(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean safe = c < 0x0300
                    || (c >= 0x0370 && c <= 0x0482)
                    || (c >= 0x048A && c <= 0x052F)
                    || (c >= 0x1E00 && c <= 0x1FFF)
                    || (c >= 0x2010 && c <= 0x2027)
                    || (c >= 0x2030 && c <= 0x205E)
                    || (c >= 0x3000 && c <= 0x9FFF)
                    || (c >= 0xAC00 && c <= 0xD7A3)
                    || (c >= 0xFF01 && c <= 0xFFEF);
            if (!safe) {
                return false;
            }
        }
        return true;
    }

    private void drawRotated(Graphics g, String text, Font font, int h, int textColor, int haloColor,
                             double centerX, double centerY, double angle) {
        Transform saved = g.getTransform();
        if (scratch == null) {
            scratch = Transform.makeIdentity();
        }
        g.setFont(font);
        int cx = round(centerX);
        int cy = round(centerY);
        try {
            scratch.setTransform(saved);
            scratch.rotate((float) angle, cx, cy);
            g.setTransform(scratch);
            draw(g, text, font, textColor, haloColor, cx - font.stringWidth(text) / 2, cy - h / 2);
        } finally {
            g.setTransform(saved);
        }
    }

    private void drawAlong(Graphics g, String text, Font font, int[] widths, int h, int textColor,
                           int haloColor, double[] center, double[] gx, double[] gy, double[] ga) {
        boolean straight = true;
        for (int i = 1; i < ga.length && straight; i++) {
            straight = Math.abs(normalize(ga[i] - ga[0])) <= STRAIGHT_TOLERANCE;
        }
        if (straight) {
            drawRotated(g, text, font, h, textColor, haloColor, center[0], center[1], meanAngle(ga));
            return;
        }
        Transform saved = g.getTransform();
        if (scratch == null) {
            scratch = Transform.makeIdentity();
        }
        g.setFont(font);
        try {
            for (int i = 0; i < ga.length; i++) {
                int px = round(gx[i]);
                int py = round(gy[i]);
                scratch.setTransform(saved);
                scratch.rotate((float) ga[i], px, py);
                g.setTransform(scratch);
                draw(g, String.valueOf(text.charAt(i)), font, textColor, haloColor,
                        px - widths[i] / 2, py - h / 2);
            }
        } finally {
            g.setTransform(saved);
        }
    }

    // The distance along the line of the middle of its longest run inside the
    // rectangle, or -1 when no part of it is inside. Each segment is clipped to
    // the rectangle (Liang-Barsky); touching clipped pieces join into one run.
    private static double visibleMiddle(double[] pts, double[] cum, int left, int top, int right,
                                        int bottom) {
        double bestStart = -1;
        double bestEnd = -1;
        double runStart = -1;
        double runEnd = -1;
        for (int i = 1; i < cum.length; i++) {
            double x0 = pts[i * 2 - 2];
            double y0 = pts[i * 2 - 1];
            double dx = pts[i * 2] - x0;
            double dy = pts[i * 2 + 1] - y0;
            double t0 = 0;
            double t1 = 1;
            double[] p = {-dx, dx, -dy, dy};
            double[] q = {x0 - left, right - x0, y0 - top, bottom - y0};
            boolean inside = true;
            for (int k = 0; k < 4 && inside; k++) {
                if (p[k] == 0) {
                    inside = q[k] >= 0;
                } else {
                    double r = q[k] / p[k];
                    if (p[k] < 0) {
                        t0 = Math.max(t0, r);
                    } else {
                        t1 = Math.min(t1, r);
                    }
                    inside = t0 <= t1;
                }
            }
            if (!inside) {
                continue;
            }
            double length = cum[i] - cum[i - 1];
            double a = cum[i - 1] + t0 * length;
            double b = cum[i - 1] + t1 * length;
            if (runStart >= 0 && a <= runEnd + 1e-6) {
                runEnd = b;
            } else {
                runStart = a;
                runEnd = b;
            }
            if (runEnd - runStart > bestEnd - bestStart) {
                bestStart = runStart;
                bestEnd = runEnd;
            }
        }
        return bestStart < 0 ? -1 : (bestStart + bestEnd) / 2;
    }

    // The point at distance `d` along the line, and the direction of the
    // segment it falls on: {x, y, angle}.
    private static double[] pointAt(double[] pts, double[] cum, double d) {
        int n = cum.length;
        int seg = 1;
        while (seg < n - 1 && cum[seg] < d) {
            seg++;
        }
        double x0 = pts[seg * 2 - 2];
        double y0 = pts[seg * 2 - 1];
        double x1 = pts[seg * 2];
        double y1 = pts[seg * 2 + 1];
        double length = cum[seg] - cum[seg - 1];
        double t = length <= 0 ? 0 : (d - cum[seg - 1]) / length;
        if (t < 0) {
            t = 0;
        } else if (t > 1) {
            t = 1;
        }
        double angle;
        if (length > 0) {
            angle = MathUtil.atan2(y1 - y0, x1 - x0);
        } else {
            angle = directionNear(pts, cum, seg);
        }
        return new double[]{x0 + t * (x1 - x0), y0 + t * (y1 - y0), angle};
    }

    // A zero-length segment (a repeated vertex) has no direction of its own;
    // borrow the nearest segment that does.
    private static double directionNear(double[] pts, double[] cum, int seg) {
        int n = cum.length;
        for (int delta = 1; delta < n; delta++) {
            int[] candidates = {seg + delta, seg - delta};
            for (int c : candidates) {
                if (c >= 1 && c < n && cum[c] - cum[c - 1] > 0) {
                    return MathUtil.atan2(pts[c * 2 + 1] - pts[c * 2 - 1], pts[c * 2] - pts[c * 2 - 2]);
                }
            }
        }
        return 0;
    }

    private static double meanAngle(double[] angles) {
        double sum = 0;
        for (double a : angles) {
            sum += normalize(a - angles[0]);
        }
        return angles[0] + sum / angles.length;
    }

    // Wraps an angle into -PI..PI.
    private static double normalize(double angle) {
        return angle - 2 * Math.PI * Math.floor((angle + Math.PI) / (2 * Math.PI));
    }

    private static int round(double v) {
        return (int) Math.floor(v + 0.5);
    }

    /// Attempts to draw `text` centered at `cx,cy`. Returns false (drawing
    /// nothing) when it would collide with an already placed label.
    boolean place(Graphics g, String text, double sizePx, int textColor, int haloColor, int cx, int cy) {
        if (text == null || text.length() == 0) {
            return false;
        }
        Font font = fontFor(sizePx);
        int w = font.stringWidth(text);
        int h = font.getHeight();
        int x = cx - w / 2;
        int y = cy - h / 2;
        int bx = x - 2;
        int by = y - 2;
        int bw = w + 4;
        int bh = h + 4;
        for (Object occItem : occupied) {
            int[] o = (int[]) occItem;
            if (intersects(bx, by, bw, bh, o[0], o[1], o[2], o[3])) {
                return false;
            }
        }
        occupied.add(new int[]{bx, by, bw, bh});
        draw(g, text, font, textColor, haloColor, x, y);
        return true;
    }

    private void draw(Graphics g, String text, Font font, int textColor, int haloColor, int x, int y) {
        g.setFont(font);
        int prevAlpha = g.getAlpha();
        int haloA = (haloColor >>> 24) & 0xff;
        if (haloA > 0) {
            g.setAlpha(haloA);
            g.setColor(haloColor & 0xffffff);
            g.drawString(text, x - 1, y);
            g.drawString(text, x + 1, y);
            g.drawString(text, x, y - 1);
            g.drawString(text, x, y + 1);
        }
        int textA = (textColor >>> 24) & 0xff;
        if (textA == 0) {
            textA = 255;
        }
        g.setAlpha(textA);
        g.setColor(textColor & 0xffffff);
        g.drawString(text, x, y);
        g.setAlpha(prevAlpha);
    }

    private Font fontFor(double sizePx) {
        int bucket;
        int sizeConst;
        if (sizePx <= 12) {
            bucket = 0;
            sizeConst = Font.SIZE_SMALL;
        } else if (sizePx <= 17) {
            bucket = 1;
            sizeConst = Font.SIZE_MEDIUM;
        } else {
            bucket = 2;
            sizeConst = Font.SIZE_LARGE;
        }
        Integer k = Integer.valueOf(bucket);
        Font f = (Font) fontCache.get(k);
        if (f == null) {
            f = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, sizeConst);
            fontCache.put(k, f);
        }
        return f;
    }

    private static boolean intersects(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}
