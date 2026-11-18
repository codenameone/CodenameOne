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
package com.codename1.ui.table;

/// The selected view rows of a grid, as sorted, disjoint, inclusive ranges,
/// so that selecting every row of a million costs one range.
final class GridSelection {
    // start, end pairs
    private int[] ranges = new int[8];
    private int count;

    void clear() {
        count = 0;
    }

    boolean isEmpty() {
        return count == 0;
    }

    int rangeCount() {
        return count;
    }

    int rangeStart(int i) {
        return ranges[i * 2];
    }

    int rangeEnd(int i) {
        return ranges[i * 2 + 1];
    }

    boolean contains(int row) {
        int lo = 0;
        int hi = count - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (row < ranges[mid * 2]) {
                hi = mid - 1;
            } else if (row > ranges[mid * 2 + 1]) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /// How many rows are selected.
    int size() {
        int n = 0;
        for (int i = 0; i < count; i++) {
            n += ranges[i * 2 + 1] - ranges[i * 2] + 1;
        }
        return n;
    }

    int first() {
        return count == 0 ? -1 : ranges[0];
    }

    int[] toArray() {
        int[] out = new int[size()];
        int k = 0;
        for (int i = 0; i < count; i++) {
            for (int r = ranges[i * 2]; r <= ranges[i * 2 + 1]; r++) {
                out[k++] = r;
            }
        }
        return out;
    }

    void set(int from, int to) {
        count = 0;
        add(from, to);
    }

    void add(int from, int to) {
        if (from > to) {
            int t = from;
            from = to;
            to = t;
        }
        if (from < 0) {
            from = 0;
        }
        if (to < from) {
            return;
        }
        int[] out = new int[(count + 1) * 2];
        int k = 0;
        boolean placed = false;
        for (int i = 0; i < count; i++) {
            int s = ranges[i * 2];
            int e = ranges[i * 2 + 1];
            if (e < from - 1) {
                out[k++] = s;
                out[k++] = e;
            } else if (s > to + 1) {
                if (!placed) {
                    out[k++] = from;
                    out[k++] = to;
                    placed = true;
                }
                out[k++] = s;
                out[k++] = e;
            } else {
                from = Math.min(from, s);
                to = Math.max(to, e);
            }
        }
        if (!placed) {
            out[k++] = from;
            out[k++] = to;
        }
        ranges = out.length < 8 ? grow(out, 8) : out;
        count = k / 2;
    }

    void remove(int from, int to) {
        if (from > to) {
            int t = from;
            from = to;
            to = t;
        }
        int[] out = new int[(count + 1) * 2];
        int k = 0;
        for (int i = 0; i < count; i++) {
            int s = ranges[i * 2];
            int e = ranges[i * 2 + 1];
            if (e < from || s > to) {
                out[k++] = s;
                out[k++] = e;
                continue;
            }
            if (s < from) {
                out[k++] = s;
                out[k++] = from - 1;
            }
            if (e > to) {
                if (k + 2 > out.length) {
                    out = grow(out, out.length + 4);
                }
                out[k++] = to + 1;
                out[k++] = e;
            }
        }
        ranges = out.length < 8 ? grow(out, 8) : out;
        count = k / 2;
    }

    void toggle(int row) {
        if (contains(row)) {
            remove(row, row);
        } else {
            add(row, row);
        }
    }

    /// Moves the selection for rows inserted or removed at `at`: a positive
    /// `delta` rows were inserted before `at`, a negative one removed from
    /// `at` on.
    void shift(int at, int delta) {
        if (delta == 0 || count == 0) {
            return;
        }
        if (delta < 0) {
            remove(at, at - delta - 1);
        }
        for (int i = 0; i < count * 2; i++) {
            if (ranges[i] >= at) {
                ranges[i] += delta;
            }
        }
        if (delta > 0) {
            // A range that held `at` was split by the insertion; the new rows
            // are not selected.
            for (int i = 0; i < count; i++) {
                int s = ranges[i * 2];
                int e = ranges[i * 2 + 1];
                if (s < at && e >= at + delta) {
                    remove(at, at + delta - 1);
                    break;
                }
            }
        }
    }

    private static int[] grow(int[] a, int n) {
        int[] b = new int[n];
        System.arraycopy(a, 0, b, 0, Math.min(a.length, n));
        return b;
    }
}
