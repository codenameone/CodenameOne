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
package com.codename1.unitycompat.unityengine;

/// The solid pixels of a tilemap as one picture, and the two ways of
/// turning that picture into collision shapes: the closed outlines around
/// each solid region, and the rectangles that fill them.
///
/// Tiles side by side are one region here, because they are neighbouring
/// pixels of one picture. That is the whole of what a composite collider
/// is for: a floor of forty tiles is one straight edge, with no seam at
/// each tile for a body sliding along it to catch on.
final class TileOutline {
    /// Solid pixels: a row of bits for each row from the bottom, the
    /// leftmost pixel in the lowest bit.
    private int[] bits = new int[0];
    private int stride;
    int width;
    int height;
    /// For each corner between pixels, which of the four edges leaving it
    /// are part of an outline still to be walked.
    private byte[] leaving = new byte[0];
    /// The corners of the outline being walked, x then y.
    int[] loop = new int[64];
    int loopLength;

    /// What takes the shapes: corners are in pixels from the bottom left.
    interface Sink {
        void outline(int[] corners, int count);

        void box(int left, int bottom, int right, int top);
    }

    void clear(int pixelsAcross, int pixelsUp) {
        width = pixelsAcross;
        height = pixelsUp;
        stride = (pixelsAcross + 31) >> 5;
        int n = stride * pixelsUp;
        if (bits.length < n) {
            bits = new int[n];
        } else {
            for (int i = 0; i < n; i++) {
                bits[i] = 0;
            }
        }
    }

    void set(int x, int y) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            bits[y * stride + (x >> 5)] |= 1 << (x & 31);
        }
    }

    void fill(int left, int bottom, int right, int top) {
        for (int y = bottom; y < top; y++) {
            for (int x = left; x < right; x++) {
                set(x, y);
            }
        }
    }

    boolean solid(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height
                && (bits[y * stride + (x >> 5)] & (1 << (x & 31))) != 0;
    }

    private void corner(int x, int y) {
        if (loopLength + 2 > loop.length) {
            int[] grown = new int[loop.length * 2];
            System.arraycopy(loop, 0, grown, 0, loopLength);
            loop = grown;
        }
        loop[loopLength++] = x;
        loop[loopLength++] = y;
    }

    /// Walks every outline, solid on the left, and hands each to the sink
    /// with no corner on a straight run.
    ///
    /// An edge of the picture between a solid pixel and an empty one is a
    /// step of an outline, and leaves a corner in one of four directions:
    /// east under a solid pixel, north on its right, west over it, south
    /// on its left. Where two solid pixels touch only at a corner, the
    /// walk turns right, which keeps each of them in an outline of its
    /// own instead of pinching one outline through a point.
    void outlines(Sink sink) {
        int across = width + 1;
        int n = across * (height + 1);
        if (leaving.length < n) {
            leaving = new byte[n];
        } else {
            for (int i = 0; i < n; i++) {
                leaving[i] = 0;
            }
        }
        for (int y = 0; y < height; y++) {
            int row = y * stride;
            for (int word = 0; word < stride; word++) {
                if (bits[row + word] == 0) {
                    continue;
                }
                int from = word << 5;
                int to = from + 32 < width ? from + 32 : width;
                for (int x = from; x < to; x++) {
                    if (!solid(x, y)) {
                        continue;
                    }
                    if (!solid(x, y - 1)) {
                        leaving[y * across + x] |= 1;
                    }
                    if (!solid(x + 1, y)) {
                        leaving[y * across + x + 1] |= 2;
                    }
                    if (!solid(x, y + 1)) {
                        leaving[(y + 1) * across + x + 1] |= 4;
                    }
                    if (!solid(x - 1, y)) {
                        leaving[(y + 1) * across + x] |= 8;
                    }
                }
            }
        }
        for (int start = 0; start < n; start++) {
            while (leaving[start] != 0) {
                int first = 0;
                while ((leaving[start] & (1 << first)) == 0) {
                    first++;
                }
                loopLength = 0;
                int at = start;
                int heading = first;
                corner(start % across, start / across);
                boolean closed = false;
                while (!closed) {
                    leaving[at] &= (byte) ~(1 << heading);
                    at += heading == 0 ? 1 : heading == 1 ? across : heading == 2 ? -1 : -across;
                    if (at == start) {
                        closed = true;
                        // A walk that comes back heading the way it left
                        // started in the middle of a straight run.
                        if (heading == first) {
                            System.arraycopy(loop, 2, loop, 0, loopLength - 2);
                            loopLength -= 2;
                        }
                        break;
                    }
                    int right = (heading + 3) & 3;
                    int left = (heading + 1) & 3;
                    int turn;
                    if ((leaving[at] & (1 << right)) != 0) {
                        turn = right;
                    } else if ((leaving[at] & (1 << heading)) != 0) {
                        turn = heading;
                    } else if ((leaving[at] & (1 << left)) != 0) {
                        turn = left;
                    } else {
                        // Cannot happen in a picture of whole pixels; an
                        // open run is dropped rather than walked forever.
                        loopLength = 0;
                        break;
                    }
                    if (turn != heading) {
                        corner(at % across, at / across);
                        heading = turn;
                    }
                }
                if (closed && loopLength >= 6) {
                    sink.outline(loop, loopLength / 2);
                }
            }
        }
    }

    /// Covers the solid pixels with rectangles: each run of a row, joined
    /// to the same run of the rows above it for as long as they match.
    void boxes(Sink sink) {
        // The run of each column's start that is still open, by its left
        // edge: where it ends and which row it began on.
        int[] openRight = new int[width + 1];
        int[] openBottom = new int[width + 1];
        for (int x = 0; x <= width; x++) {
            openRight[x] = -1;
        }
        for (int y = 0; y <= height; y++) {
            int x = 0;
            // Close every open run this row does not repeat exactly.
            for (int left = 0; left < width; left++) {
                int right = openRight[left];
                if (right < 0) {
                    continue;
                }
                boolean same = y < height && !solid(left - 1, y) && !solid(right, y);
                for (int k = left; same && k < right; k++) {
                    same = solid(k, y);
                }
                if (!same) {
                    sink.box(left, openBottom[left], right, y);
                    openRight[left] = -1;
                }
            }
            if (y == height) {
                break;
            }
            while (x < width) {
                if (!solid(x, y)) {
                    x++;
                    continue;
                }
                int left = x;
                while (x < width && solid(x, y)) {
                    x++;
                }
                if (openRight[left] != x) {
                    openRight[left] = x;
                    openBottom[left] = y;
                }
            }
        }
    }
}
