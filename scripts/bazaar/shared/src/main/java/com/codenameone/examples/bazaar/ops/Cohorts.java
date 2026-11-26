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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The retention grid: customers grouped by the month of their first order,
/// and for each later month the share of the group that ordered again.
///
/// Months are whole indexes (`year * 12 + month - 1`), computed by the
/// caller, so nothing here needs a calendar.
public final class Cohorts {
    private Cohorts() {
    }

    /// One row: a first-order month, how many customers started then, and
    /// for offset 0..n the share (basis points) active that many months on.
    public static final class Row {
        public final int month;
        public final int size;
        public final int[] retainedBp;

        Row(int month, int size, int[] retainedBp) {
            this.month = month;
            this.size = size;
            this.retainedBp = retainedBp;
        }
    }

    /// The grid for orders `customers[i]` placed in `months[i]`, oldest
    /// cohort first, with offsets up to `maxOffset` (and never past `now`).
    public static List<Row> grid(String[] customers, int[] months, int now, int maxOffset) {
        Map<String, Integer> first = new HashMap<String, Integer>();
        int n = customers == null ? 0 : customers.length;
        for (int iter = 0; iter < n; iter++) {
            String c = customers[iter];
            if (c == null || c.length() == 0) {
                continue;
            }
            Integer was = first.get(c);
            if (was == null || months[iter] < was.intValue()) {
                first.put(c, Integer.valueOf(months[iter]));
            }
        }
        // active[cohort][offset] as a set of customers per cell.
        Map<String, Boolean> seen = new HashMap<String, Boolean>();
        Map<Integer, int[]> counts = new HashMap<Integer, int[]>();
        Map<Integer, Integer> sizes = new HashMap<Integer, Integer>();
        for (Map.Entry<String, Integer> e : first.entrySet()) {
            Integer m = e.getValue();
            Integer s = sizes.get(m);
            sizes.put(m, Integer.valueOf(s == null ? 1 : s.intValue() + 1));
        }
        for (int iter = 0; iter < n; iter++) {
            String c = customers[iter];
            Integer f = c == null ? null : first.get(c);
            if (f == null) {
                continue;
            }
            int offset = months[iter] - f.intValue();
            if (offset < 0 || offset > maxOffset) {
                continue;
            }
            String key = c + "\n" + offset;
            if (seen.containsKey(key)) {
                continue;
            }
            seen.put(key, Boolean.TRUE);
            int[] row = counts.get(f);
            if (row == null) {
                row = new int[maxOffset + 1];
                counts.put(f, row);
            }
            row[offset]++;
        }
        List<Integer> order = new ArrayList<Integer>(sizes.keySet());
        java.util.Collections.sort(order);
        List<Row> out = new ArrayList<Row>();
        for (int iter = 0; iter < order.size(); iter++) {
            Integer m = order.get(iter);
            int size = sizes.get(m).intValue();
            int width = Math.max(0, Math.min(maxOffset, now - m.intValue())) + 1;
            int[] bp = new int[width];
            int[] row = counts.get(m);
            for (int o = 0; o < width; o++) {
                bp[o] = size == 0 || row == null ? 0 : (int) (row[o] * 10000L / size);
            }
            out.add(new Row(m.intValue(), size, bp));
        }
        return out;
    }

    /// A month index as `YYYY-MM`.
    public static String label(int month) {
        int y = month / 12;
        int m = month % 12 + 1;
        return y + "-" + (m < 10 ? "0" : "") + m;
    }
}
