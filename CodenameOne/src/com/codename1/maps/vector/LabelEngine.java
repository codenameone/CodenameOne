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

    // The side of a cell of the grid the placed boxes are filed under, as a
    // power of two: a label is a few cells wide and one high.
    private static final int CELL_SHIFT = 6;

    // A box over more cells than this is kept in a short list of its own
    // instead of being filed under every one of them.
    private static final int MAX_CELLS = 32;

    // The boxes placed this frame, four ints each: x, y, width, height.
    private int[] boxes = new int[1024];
    private int boxCount;
    // The grid. A cell hashes to a bucket; a bucket is a chain of entries,
    // each naming a box. Two cells sharing a bucket only means a box that is
    // nowhere near is compared as well, and found not to overlap.
    private final int[] buckets = new int[2048];
    private int[] entryBox = new int[2048];
    private int[] entryNext = new int[2048];
    private int entryCount;
    private int[] largeBoxes = new int[16];
    private int largeCount;

    private final Map placedAnchors = new HashMap();
    // The fonts in use, one for each size asked for -- a handful, the sizes
    // of a style times one density -- and the size each was made for.
    private static final int FONTS = 8;
    private final Font[] fonts = new Font[FONTS];
    private final int[] fontSizes = new int[FONTS];
    private int fontCount;
    // The height of each of them, asked for once a frame; -1 before.
    private final int[] fontHeights = new int[FONTS];
    // How far the outline of a label reaches past its strokes, in pixels.
    private int halo = 1;
    // The part of the screen a label has to be wholly inside, if any.
    private boolean bounded;
    private int boundLeft;
    private int boundTop;
    private int boundRight;
    private int boundBottom;
    private Transform scratch;

    LabelEngine() {
        clearBuckets();
    }

    /// Clears placements at the start of a frame.
    void reset() {
        boxCount = 0;
        entryCount = 0;
        largeCount = 0;
        for (int i = 0; i < FONTS; i++) {
            fontHeights[i] = -1;
        }
        clearBuckets();
        placedAnchors.clear();
        bounded = false;
    }

    /// As [#reset], for a frame whose labels must each fit inside the given
    /// rectangle. A name cut off by the edge of the map is not a name: "ay
    /// Street" tells the reader less than nothing.
    void reset(int left, int top, int right, int bottom) {
        reset();
        bounded = true;
        boundLeft = left;
        boundTop = top;
        boundRight = right;
        boundBottom = bottom;
    }

    /// How far the outline around a label reaches, in device pixels. One pixel
    /// is an outline on a display of one pixel to the point and nothing at all
    /// on a phone, where it is a third of the thinnest stroke of a letter.
    void setHalo(int pixels) {
        halo = Math.max(1, pixels);
    }

    private void clearBuckets() {
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = -1;
        }
    }

    private int bucketOf(int cellX, int cellY) {
        return ((cellX * 73856093) ^ (cellY * 19349663)) & (buckets.length - 1);
    }

    // Whether the box overlaps one placed this frame.
    //
    // Every label in view asks this, and most of them are turned away, so it
    // is what a frame of a busy map spends its time on. Walking every placed
    // box for each of them grew with the square of the labels in view: a
    // desktop-sized map of a city centre compared millions of boxes a frame.
    // Only the boxes filed under the cells this one touches are compared.
    private boolean collides(int x, int y, int w, int h) {
        for (int i = 0; i < largeCount; i++) {
            if (overlaps(largeBoxes[i], x, y, w, h)) {
                return true;
            }
        }
        int x0 = x >> CELL_SHIFT;
        int y0 = y >> CELL_SHIFT;
        int x1 = (x + w) >> CELL_SHIFT;
        int y1 = (y + h) >> CELL_SHIFT;
        if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) > MAX_CELLS) {
            for (int i = 0; i < boxCount; i++) {
                if (overlaps(i, x, y, w, h)) {
                    return true;
                }
            }
            return false;
        }
        for (int cy = y0; cy <= y1; cy++) {
            for (int cx = x0; cx <= x1; cx++) {
                for (int e = buckets[bucketOf(cx, cy)]; e >= 0; e = entryNext[e]) {
                    if (overlaps(entryBox[e], x, y, w, h)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean overlaps(int box, int x, int y, int w, int h) {
        int at = box * 4;
        return intersects(x, y, w, h, boxes[at], boxes[at + 1], boxes[at + 2], boxes[at + 3]);
    }

    private void occupy(int x, int y, int w, int h) {
        if (boxCount * 4 == boxes.length) {
            boxes = grow(boxes);
        }
        int box = boxCount++;
        int at = box * 4;
        boxes[at] = x;
        boxes[at + 1] = y;
        boxes[at + 2] = w;
        boxes[at + 3] = h;
        int x0 = x >> CELL_SHIFT;
        int y0 = y >> CELL_SHIFT;
        int x1 = (x + w) >> CELL_SHIFT;
        int y1 = (y + h) >> CELL_SHIFT;
        if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) > MAX_CELLS) {
            if (largeCount == largeBoxes.length) {
                largeBoxes = grow(largeBoxes);
            }
            largeBoxes[largeCount++] = box;
            return;
        }
        for (int cy = y0; cy <= y1; cy++) {
            for (int cx = x0; cx <= x1; cx++) {
                if (entryCount == entryBox.length) {
                    entryBox = grow(entryBox);
                    entryNext = grow(entryNext);
                }
                int bucket = bucketOf(cx, cy);
                entryBox[entryCount] = box;
                entryNext[entryCount] = buckets[bucket];
                buckets[bucket] = entryCount++;
            }
        }
    }

    private static int[] grow(int[] from) {
        int[] grown = new int[from.length * 2];
        System.arraycopy(from, 0, grown, 0, from.length);
        return grown;
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
        return placeAlongLine(g, null, text, sizePx, textColor, haloColor, pts, repeat, left, top,
                right, bottom);
    }

    /// [#placeAlongLine] for a label of the map, whose measurements are kept
    /// on `candidate` from one frame to the next.
    boolean placeAlongLine(Graphics g, LabelCandidate candidate, double[] pts, double sizePx,
                           double repeat, int left, int top, int right, int bottom) {
        return placeAlongLine(g, candidate, candidate.text, sizePx, candidate.textColor,
                candidate.haloColor, pts, repeat, left, top, right, bottom);
    }

    private boolean placeAlongLine(Graphics g, LabelCandidate candidate, String text, double sizePx,
                                   int textColor, int haloColor, double[] pts, double repeat,
                                   int left, int top, int right, int bottom) {
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
            return place(g, candidate, text, sizePx, textColor, haloColor, round(p[0]), round(p[1]));
        }
        Font font = fontFor(sizePx);
        int h = heightOf(font);
        boolean perGlyph;
        int[] widths;
        int textW;
        if (candidate != null && candidate.measured(font, h) && candidate.cellWidths != null) {
            perGlyph = candidate.perGlyph;
            widths = candidate.cellWidths;
            textW = candidate.textWidth;
        } else {
            perGlyph = drawsCharByChar(text);
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
            if (candidate != null) {
                candidate.remember(font, h, textW, perGlyph, widths);
            }
        }
        if (textW <= 0 || total < textW + h) {
            return false;
        }
        if (repeat < textW + h) {
            repeat = textW + h;
        }
        // The stretch of the road that is on screen, less half a line at the
        // edges: the whole name has to fit on it. The middle of the road is
        // where a name goes while it fits there, and the middle of a road
        // that runs off the screen is as often as not at the edge, which is
        // how a street came to be called "Hyde Stre".
        double[] run = visibleRun(pts, cum, left + h / 2, top + h / 2, right - h / 2, bottom - h / 2);
        if (run == null) {
            return false;
        }
        double from = Math.max(run[0], 0) + textW / 2.0;
        double to = Math.min(run[1], total) - textW / 2.0;
        if (to < from) {
            return false;
        }
        // The places a copy may go are fixed to the road -- its middle, and
        // steps of about half a name either way that include every whole
        // repeat -- and not to the screen, so a name stays where it is on the
        // ground while the map is dragged, and moves along only when the
        // place it had would cut it off.
        int parts = Math.max(1, (int) (repeat / Math.max(1.0, textW / 2.0)));
        double step = repeat / parts;
        int lo = (int) Math.ceil((from - mid) / step - 1e-9);
        int hi = (int) Math.floor((to - mid) / step + 1e-9);
        boolean placedAny = false;
        int up = lo > 0 ? lo : Math.min(hi, 0);
        int down = up - 1;
        while (up <= hi || down >= lo) {
            boolean takeUp = up <= hi && (down < lo || Math.abs(up) <= Math.abs(down));
            int j = takeUp ? up++ : down--;
            if (placeCopy(g, text, font, widths, textW, h, perGlyph, textColor, haloColor, pts,
                    cum, mid + j * step, repeat, left, top, right, bottom)) {
                placedAny = true;
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
        double[] angles = new double[cum.length];
        for (int i = 0; i < angles.length; i++) {
            angles[i] = Double.NaN;
        }
        double[] gx = new double[len];
        double[] gy = new double[len];
        double[] ga = new double[len];
        int[][] glyphBoxes = new int[len][];
        double advance = 0;
        for (int i = 0; i < len; i++) {
            double offset = advance + widths[i] / 2.0;
            advance += widths[i];
            double at = reverse ? d + textW / 2.0 - offset : d - textW / 2.0 + offset;
            double[] p = pointAt(pts, cum, at, angles);
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
            if (collides(box[0], box[1], box[2], box[3])) {
                return false;
            }
            glyphBoxes[i] = box;
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
        for (int[] box : glyphBoxes) {
            occupy(box[0], box[1], box[2], box[3]);
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
                    || (c >= 0x3000 && c <= 0x9FFF && !isCjkCombiningMark(c))
                    || (c >= 0xAC00 && c <= 0xD7A3)
                    || (c >= 0xFF01 && c <= 0xFFEF);
            if (!safe) {
                return false;
            }
        }
        return true;
    }

    // The combining marks inside the CJK block: the ideographic tone marks and
    // the kana voicing marks (dakuten/handakuten), which a decomposed name
    // such as "ha" + U+3099 attaches to its base. Drawn on their own they
    // would float as separate rotated glyphs.
    private static boolean isCjkCombiningMark(char c) {
        return (c >= 0x302A && c <= 0x302F) || c == 0x3099 || c == 0x309A;
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
            // Not a glyph at a time: a road is a few straight stretches, and
            // the glyphs on one of them share its direction, so each stretch
            // is drawn as one piece of the name. Drawn singly, a name of a
            // dozen letters on a road with one kink in it was sixty strings
            // with their outlines, on every frame -- three quarters of all the
            // text a busy map drew.
            int from = 0;
            while (from < ga.length) {
                int to = from + 1;
                while (to < ga.length && Math.abs(normalize(ga[to] - ga[from])) <= STRAIGHT_TOLERANCE) {
                    to++;
                }
                int width = 0;
                for (int i = from; i < to; i++) {
                    width += widths[i];
                }
                // The middle of the stretch: half way between the middles of
                // its first and last glyphs, put right for their widths.
                double angle = ga[from];
                double shift = (widths[from] - widths[to - 1]) / 4.0;
                int px = round((gx[from] + gx[to - 1]) / 2 - shift * Math.cos(angle));
                int py = round((gy[from] + gy[to - 1]) / 2 - shift * Math.sin(angle));
                scratch.setTransform(saved);
                scratch.rotate((float) angle, px, py);
                g.setTransform(scratch);
                draw(g, to - from == text.length() ? text : text.substring(from, to), font, textColor,
                        haloColor, px - width / 2, py - h / 2);
                from = to;
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
        double[] run = visibleRun(pts, cum, left, top, right, bottom);
        return run == null ? -1 : (run[0] + run[1]) / 2;
    }

    // The longest run of the line inside the rectangle, as the distances along
    // the line it starts and ends at, or null when no part of it is inside.
    private static double[] visibleRun(double[] pts, double[] cum, int left, int top, int right,
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
        return bestStart < 0 ? null : new double[]{bestStart, bestEnd};
    }

    // The point at distance `d` along the line, and the direction of the
    // segment it falls on: {x, y, angle}.
    private static double[] pointAt(double[] pts, double[] cum, double d) {
        return pointAt(pts, cum, d, null);
    }

    // `angles` holds the direction of each segment once it has been worked
    // out, NaN before: the glyphs of a name mostly share a segment, and an
    // arc tangent for each of them, for every road in view, was a twentieth
    // of a frame. Null leaves the direction out of the answer.
    private static double[] pointAt(double[] pts, double[] cum, double d, double[] angles) {
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
        double angle = 0;
        if (angles != null) {
            angle = angles[seg];
            if (Double.isNaN(angle)) {
                if (length > 0) {
                    angle = MathUtil.atan2(y1 - y0, x1 - x0);
                } else {
                    angle = directionNear(pts, cum, seg);
                }
                angles[seg] = angle;
            }
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
        return place(g, null, text, sizePx, textColor, haloColor, cx, cy);
    }

    /// [#place] for a label of the map, whose width is kept on `candidate`
    /// from one frame to the next.
    boolean place(Graphics g, LabelCandidate candidate, double sizePx, int cx, int cy) {
        return place(g, candidate, candidate.text, sizePx, candidate.textColor, candidate.haloColor,
                cx, cy);
    }

    // Measuring is the other thing a frame did for every label in view, drawn
    // or not: a name that loses its place to a neighbour was measured first,
    // on every frame of a drag. A label keeps its measurements for as long as
    // the font it was measured in answers the same height, which follows the
    // display's density.
    private boolean place(Graphics g, LabelCandidate candidate, String text, double sizePx,
                          int textColor, int haloColor, int cx, int cy) {
        if (text == null || text.length() == 0) {
            return false;
        }
        Font font = fontFor(sizePx);
        int h = heightOf(font);
        int w;
        // Not what a road name was measured as for laying along its road:
        // that is the sum of its glyphs, which is not the width of the string.
        if (candidate != null && candidate.measured(font, h) && candidate.cellWidths == null) {
            w = candidate.textWidth;
        } else {
            w = font.stringWidth(text);
            if (candidate != null) {
                candidate.remember(font, h, w, false, null);
            }
        }
        int x = cx - w / 2;
        int y = cy - h / 2;
        int bx = x - 2;
        int by = y - 2;
        int bw = w + 4;
        int bh = h + 4;
        if (bounded && (x < boundLeft || y < boundTop || x + w > boundRight || y + h > boundBottom)) {
            return false;
        }
        if (collides(bx, by, bw, bh)) {
            return false;
        }
        occupy(bx, by, bw, bh);
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
            if (halo > 1) {
                // Further out than a pixel the four copies go on the diagonals,
                // where each covers two sides at once: on the axes they are
                // four ghosts of the name with the corners between them bare.
                // Still four, though -- the outline is most of what a label
                // costs to draw, and it is drawn for every label every frame.
                int d = Math.max(1, (halo * 3 + 2) / 4);
                g.drawString(text, x - d, y - d);
                g.drawString(text, x + d, y - d);
                g.drawString(text, x - d, y + d);
                g.drawString(text, x + d, y + d);
            } else {
                g.drawString(text, x - 1, y);
                g.drawString(text, x + 1, y);
                g.drawString(text, x, y - 1);
                g.drawString(text, x, y + 1);
            }
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

    // Every label asks the height of its font, and a port may work it out
    // each time it is asked. It can change -- with the display's density --
    // but not during a frame.
    private int heightOf(Font font) {
        for (int i = 0; i < fontCount; i++) {
            if (fonts[i] == font) { //NOPMD CompareObjectsWithEquals
                if (fontHeights[i] < 0) {
                    fontHeights[i] = font.getHeight();
                }
                return fontHeights[i];
            }
        }
        return font.getHeight();
    }

    // The font for text of `sizePx` device pixels.
    //
    // At that size, where the platform can make one. The three sizes of the
    // system font are the platform's idea of small, medium and large text, not
    // a number of pixels: asked for thirteen logical pixels on a phone of three
    // pixels to the point, the old lookup read 39 as "large" and set every
    // street name in the largest type the device has, three times the size the
    // style asked for.
    private Font fontFor(double sizePx) {
        int size = Math.max(1, (int) Math.floor(sizePx + 0.5));
        for (int i = 0; i < fontCount; i++) {
            if (fontSizes[i] == size) {
                return fonts[i];
            }
        }
        Font f = null;
        if (Font.isNativeFontSchemeSupported()) {
            Font face = Font.createTrueTypeFont("native:MainRegular", "native:MainRegular");
            if (face != null) {
                f = face.derive(size, Font.STYLE_PLAIN);
            }
        }
        if (f == null) {
            int sizeConst = Font.SIZE_LARGE;
            if (size <= 12) {
                sizeConst = Font.SIZE_SMALL;
            } else if (size <= 17) {
                sizeConst = Font.SIZE_MEDIUM;
            }
            f = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, sizeConst);
        }
        if (fontCount == FONTS) {
            // More sizes than a style has: start over rather than grow.
            fontCount = 0;
        }
        fonts[fontCount] = f;
        fontSizes[fontCount] = size;
        fontHeights[fontCount] = -1;
        fontCount++;
        return f;
    }

    private static boolean intersects(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}
