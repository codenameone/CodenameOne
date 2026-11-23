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

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChartReviewRound31LayoutTest {

    @Test
    public void placementWorkIsLinear() {
        LabelLayout ll = new LabelLayout(0, 0, 4000, 4000);
        Random r = new Random(7);
        int n = 10000;
        int placed = 0;
        for (int i = 0; i < n; i++) {
            if (ll.placeAt(r.nextInt(3960), r.nextInt(3990), 30, 8)) {
                placed++;
            }
        }
        assertTrue(placed > 1000, "positive control: many labels placed: " + placed);
        assertTrue(ll.comparisons <= 20L * n, "comparisons " + ll.comparisons + " for " + n + " placements");
        // a second frame reuses the grid and starts empty
        ll.setBounds(0, 0, 4000, 4000);
        assertTrue(ll.placeAt(10, 10, 30, 8));
    }
}
