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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Counts work rather than time: the counter is a package-private hook in the layout.
public class ChartReviewRound29LayoutTest {

    @Test
    public void longChainLaysOutInLinearWork() {
        int n = 10000;
        int m = n - 1;
        int[] src = new int[m];
        int[] dst = new int[m];
        double[] v = new double[m];
        // listed from the end of the chain: the worst order for repeated relaxation passes
        for (int l = 0; l < m; l++) {
            src[l] = m - 1 - l;
            dst[l] = m - l;
            v[l] = 1;
        }
        SankeyLayout lay = new SankeyLayout(n, src, dst, v, 0, 0, 1000, 500, 10, 2);
        assertEquals(n, lay.getColumns(), "one column per node of the chain");
        assertEquals(n - 1, lay.getColumn(n - 1));
        assertTrue(lay.columnSteps <= 2 * m, "linear: " + lay.columnSteps);
    }
}
