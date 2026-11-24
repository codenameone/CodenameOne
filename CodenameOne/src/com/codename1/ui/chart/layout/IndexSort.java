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

/// A stable merge sort of indices by a key per index, shared by the layouts. Stable so equal
/// keys keep their input order, exactly as the insertion sorts it replaced did, in O(n log n).
final class IndexSort {
    private IndexSort() {
    }

    /// Sorts the first `n` entries of `idx` by `key[idx[k]]`.
    ///
    /// #### Parameters
    ///
    /// - `idx`: the indices to sort, in place
    ///
    /// - `n`: how many leading entries of `idx` to sort
    ///
    /// - `key`: the key of each index
    ///
    /// - `descending`: true for the largest key first
    static void sort(int[] idx, int n, double[] key, boolean descending) {
        if (n < 2) {
            return;
        }
        int[] src = idx;
        int[] dst = new int[n];
        for (int width = 1; width < n; width *= 2) {
            for (int lo = 0; lo < n; lo += 2 * width) {
                int mid = Math.min(lo + width, n);
                int hi = Math.min(lo + 2 * width, n);
                int a = lo;
                int b = mid;
                int o = lo;
                while (a < mid && b < hi) {
                    double ka = key[src[a]];
                    double kb = key[src[b]];
                    // the right run wins only when strictly ahead, so equal keys keep their order
                    boolean takeRight = descending ? kb > ka : kb < ka;
                    dst[o++] = takeRight ? src[b++] : src[a++];
                }
                while (a < mid) {
                    dst[o++] = src[a++];
                }
                while (b < hi) {
                    dst[o++] = src[b++];
                }
            }
            int[] t = src;
            src = dst;
            dst = t;
        }
        if (src != idx) { //NOPMD CompareObjectsWithEquals - which buffer holds the result is an identity question
            System.arraycopy(src, 0, idx, 0, n);
        }
    }
}
