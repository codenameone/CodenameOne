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
package com.codename1.ui.chart.layout;

import com.codename1.ui.Font;

/// Places text so no two labels overlap. Every label is offered a list of candidate
/// positions around its anchor; the first that stays inside the bounds and clear of every
/// label already placed wins, and a label with no free position is dropped rather than
/// drawn over another. Callers place the most important labels first.
public final class LabelLayout {
    /// Candidate: centred above the anchor.
    public static final int ABOVE = 1;
    /// Candidate: centred below the anchor.
    public static final int BELOW = 2;
    /// Candidate: right of the anchor, vertically centred.
    public static final int RIGHT = 4;
    /// Candidate: left of the anchor, vertically centred.
    public static final int LEFT = 8;
    /// Candidate: centred on the anchor.
    public static final int CENTER = 16;

    private int[] rects = new int[64];
    private int count;
    private int boundsX;
    private int boundsY;
    private int boundsW;
    private int boundsH;
    private int gap = 2;

    // A uniform grid over the bounds: each cell lists the boxes that touch it, so `fits`
    // compares a box only with its neighbours instead of every box placed so far. The arrays
    // grow once and are reused by every later frame; a new frame invalidates every cell by
    // bumping `generation` rather than clearing them.
    private int cellSize = 1;
    private int cols;
    private int rows;
    private int[] cellHead = new int[0];
    private int[] cellGen = new int[0];
    private int generation;
    private int[] entryRect = new int[64];
    private int[] entryNext = new int[64];
    private int entries;
    // per box, the query that last compared it, so a box in several cells is compared once
    private int[] seenBy = new int[16];
    private int query;
    // box comparisons made by fits, for tests that hold the work linear
    int comparisons;

    /// Creates a layout confined to a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `x`: bounds left
    ///
    /// - `y`: bounds top
    ///
    /// - `w`: bounds width
    ///
    /// - `h`: bounds height
    public LabelLayout(int x, int y, int w, int h) {
        setBounds(x, y, w, h);
    }

    /// Changes the bounds and forgets every placed label.
    public void setBounds(int x, int y, int w, int h) {
        boundsX = x;
        boundsY = y;
        boundsW = w;
        boundsH = h;
        count = 0;
        entries = 0;
        generation++;
        cellSize = Math.max(16, Math.max(w, h) / 64 + 1);
        cols = Math.max(1, w / cellSize + 1);
        rows = Math.max(1, h / cellSize + 1);
        if (cols * rows > cellHead.length) {
            cellHead = new int[cols * rows];
            cellGen = new int[cols * rows];
        }
    }

    private int colOf(int x) {
        return Math.max(0, Math.min(cols - 1, (x - boundsX) / cellSize));
    }

    private int rowOf(int y) {
        return Math.max(0, Math.min(rows - 1, (y - boundsY) / cellSize));
    }

    /// The minimum clearance kept between labels, in pixels.
    public void setGap(int gap) {
        this.gap = gap;
    }

    /// The number of labels placed so far.
    public int size() {
        return count;
    }

    /// The bounds of the `i`th placed or reserved box, as `{x, y, w, h}`.
    ///
    /// #### Parameters
    ///
    /// - `i`: an index below `size()`
    ///
    /// #### Returns
    ///
    /// a new array holding the box
    public int[] getRect(int i) {
        int o = i * 4;
        return new int[]{rects[o], rects[o + 1], rects[o + 2], rects[o + 3]};
    }

    /// Reserves an area (a legend, a marker) that labels must avoid.
    public void reserve(int x, int y, int w, int h) {
        add(x, y, w, h);
    }

    /// Tries to place a `w` by `h` box near an anchor.
    ///
    /// #### Parameters
    ///
    /// - `ax`: anchor x
    ///
    /// - `ay`: anchor y
    ///
    /// - `w`: box width
    ///
    /// - `h`: box height
    ///
    /// - `offset`: distance kept from the anchor
    ///
    /// - `candidates`: a mask of `ABOVE`, `BELOW`, `RIGHT`, `LEFT`, `CENTER`, tried in that order
    ///
    /// - `out`: receives the chosen top-left `{x, y}`
    ///
    /// #### Returns
    ///
    /// true if placed, false if every candidate collided
    public boolean place(int ax, int ay, int w, int h, int offset, int candidates, int[] out) {
        int[] order = {ABOVE, BELOW, RIGHT, LEFT, CENTER};
        for (int c : order) {
            if ((candidates & c) == 0) {
                continue;
            }
            int x;
            int y;
            switch (c) {
                case ABOVE:
                    x = ax - w / 2;
                    y = ay - offset - h;
                    break;
                case BELOW:
                    x = ax - w / 2;
                    y = ay + offset;
                    break;
                case RIGHT:
                    x = ax + offset;
                    y = ay - h / 2;
                    break;
                case LEFT:
                    x = ax - offset - w;
                    y = ay - h / 2;
                    break;
                default:
                    x = ax - w / 2;
                    y = ay - h / 2;
                    break;
            }
            if (fits(x, y, w, h)) {
                add(x, y, w, h);
                out[0] = x;
                out[1] = y;
                return true;
            }
        }
        return false;
    }

    /// Places a box at an exact position if it is free.
    ///
    /// #### Returns
    ///
    /// true if the box was free and is now reserved
    public boolean placeAt(int x, int y, int w, int h) {
        if (fits(x, y, w, h)) {
            add(x, y, w, h);
            return true;
        }
        return false;
    }

    /// True when the box is inside the bounds and clear of every placed label.
    public boolean fits(int x, int y, int w, int h) {
        if (x < boundsX || y < boundsY || x + w > boundsX + boundsW || y + h > boundsY + boundsH) {
            return false;
        }
        query++;
        int c1 = colOf(x - gap);
        int c2 = colOf(x + w + gap);
        int r1 = rowOf(y - gap);
        int r2 = rowOf(y + h + gap);
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                int cell = r * cols + c;
                if (cellGen[cell] != generation) {
                    continue;
                }
                for (int e = cellHead[cell]; e >= 0; e = entryNext[e]) {
                    int i = entryRect[e];
                    if (seenBy[i] == query) {
                        continue;
                    }
                    seenBy[i] = query;
                    comparisons++;
                    int o = i * 4;
                    if (x < rects[o] + rects[o + 2] + gap && x + w + gap > rects[o]
                            && y < rects[o + 1] + rects[o + 3] + gap && y + h + gap > rects[o + 1]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void add(int x, int y, int w, int h) {
        if ((count + 1) * 4 > rects.length) {
            int[] n = new int[rects.length * 2];
            System.arraycopy(rects, 0, n, 0, rects.length);
            rects = n;
        }
        int o = count * 4;
        rects[o] = x;
        rects[o + 1] = y;
        rects[o + 2] = w;
        rects[o + 3] = h;
        if (count >= seenBy.length) {
            int[] n = new int[seenBy.length * 2];
            System.arraycopy(seenBy, 0, n, 0, seenBy.length);
            seenBy = n;
        }
        seenBy[count] = query;
        int c2 = colOf(x + w);
        int r2 = rowOf(y + h);
        for (int r = rowOf(y); r <= r2; r++) {
            for (int c = colOf(x); c <= c2; c++) {
                link(r * cols + c, count);
            }
        }
        count++;
    }

    private void link(int cell, int box) {
        if (entries >= entryRect.length) {
            int[] a = new int[entryRect.length * 2];
            System.arraycopy(entryRect, 0, a, 0, entries);
            entryRect = a;
            int[] b = new int[entryNext.length * 2];
            System.arraycopy(entryNext, 0, b, 0, entries);
            entryNext = b;
        }
        entryRect[entries] = box;
        entryNext[entries] = cellGen[cell] == generation ? cellHead[cell] : -1;
        cellHead[cell] = entries;
        cellGen[cell] = generation;
        entries++;
    }

    /// Shortens text with a trailing ellipsis until it fits `maxWidth`.
    ///
    /// #### Parameters
    ///
    /// - `f`: the font it is drawn in
    ///
    /// - `text`: the text
    ///
    /// - `maxWidth`: the space available
    ///
    /// #### Returns
    ///
    /// the text itself if it fits, a shortened copy, or an empty string when not even the
    /// ellipsis fits
    public static String ellipsize(Font f, String text, int maxWidth) {
        if (text == null || f.stringWidth(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String dots = "...";
        int dw = f.stringWidth(dots);
        if (dw > maxWidth) {
            return "";
        }
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (f.stringWidth(text.substring(0, mid)) + dw <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo == 0 ? "" : text.substring(0, lo) + dots;
    }
}
