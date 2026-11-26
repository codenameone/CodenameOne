/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.ops;

import java.util.ArrayList;
import java.util.List;

/// The warehouse board's stages and the moves between them, shared by the
/// server (which enforces them) and the app (which labels its board with them).
///
/// An inbound task -- a purchase order arriving at a location -- goes
/// RECEIVING -> PUTAWAY -> DONE. An outbound task -- an order leaving it --
/// goes PICK -> PACK -> DISPATCH -> DONE. Either may step back one stage
/// (a miscount, a parcel reopened); nothing jumps a stage and DONE is final.
///
/// [#pathOrder] is the walk through the bins for a pick list: aisle by aisle,
/// up one aisle and down the next (a serpentine), so the picker never doubles back.
public final class WarehouseFlow {
    public static final String INBOUND = "INBOUND";
    public static final String OUTBOUND = "OUTBOUND";
    public static final String RECEIVING = "RECEIVING";
    public static final String PUTAWAY = "PUTAWAY";
    public static final String PICK = "PICK";
    public static final String PACK = "PACK";
    public static final String DISPATCH = "DISPATCH";
    public static final String DONE = "DONE";
    /// The board's columns, left to right.
    public static final String[] COLUMNS = {RECEIVING, PUTAWAY, PICK, PACK, DISPATCH, DONE};
    private static final String[] IN = {RECEIVING, PUTAWAY, DONE};
    private static final String[] OUT = {PICK, PACK, DISPATCH, DONE};

    private WarehouseFlow() {
    }

    private static int at(String[] stages, String s) {
        for (int iter = 0; iter < stages.length; iter++) {
            if (stages[iter].equals(s)) {
                return iter;
            }
        }
        return -1;
    }

    private static String[] stages(String kind) {
        return INBOUND.equals(kind) ? IN : OUTBOUND.equals(kind) ? OUT : null;
    }

    /// The stage a new task of `kind` starts in.
    public static String first(String kind) {
        return INBOUND.equals(kind) ? RECEIVING : PICK;
    }

    /// Whether a task of `kind` may move from `from` to `to`.
    public static boolean legal(String kind, String from, String to) {
        String[] s = stages(kind);
        if (s == null || from == null || to == null) {
            return false;
        }
        int a = at(s, from);
        int b = at(s, to);
        if (a < 0 || b < 0 || DONE.equals(from)) {
            return false;
        }
        return b == a + 1 || b == a - 1;
    }

    /// Where a task of `kind` in `from` may go, in column order.
    public static List<String> targets(String kind, String from) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            if (legal(kind, from, COLUMNS[iter])) {
                out.add(COLUMNS[iter]);
            }
        }
        return out;
    }

    /// The forward move from `from`, or null at the end.
    public static String next(String kind, String from) {
        String[] s = stages(kind);
        int a = s == null ? -1 : at(s, from);
        return a < 0 || a + 1 >= s.length ? null : s[a + 1];
    }

    /// A sort key for a bin on the serpentine walk: aisles ascending, and
    /// within an aisle the position ascending on odd aisles and descending on
    /// even ones; the shelf breaks ties low to high.
    public static long pathKey(int aisle, int shelf, int position) {
        int p = Math.max(0, Math.min(9999, position));
        int walk = aisle % 2 == 0 ? 9999 - p : p;
        return (long) Math.max(0, aisle) * 100000000L + walk * 10000L + Math.max(0, Math.min(9999, shelf));
    }

    /// The indexes of `bins` (each `{aisle, shelf, position}`) in walking order.
    public static int[] pathOrder(int[][] bins) {
        int n = bins == null ? 0 : bins.length;
        int[] order = new int[n];
        long[] keys = new long[n];
        for (int iter = 0; iter < n; iter++) {
            order[iter] = iter;
            keys[iter] = pathKey(bins[iter][0], bins[iter][1], bins[iter][2]);
        }
        // Insertion sort: pick lists are short and the sort must be stable.
        for (int i = 1; i < n; i++) {
            int o = order[i];
            int j = i - 1;
            while (j >= 0 && keys[order[j]] > keys[o]) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = o;
        }
        return order;
    }
}
