/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
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
package com.codename1.ui.plaf;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holds VibrancyMatrix to the matrices UIKit itself installs for a tab bar tint,
 * read from the iOS 27 simulator (tab-glass-motion/native-vibrancy.csv). The
 * comparison is on OUTPUT colours over a spread of backdrops, since that is what
 * a user sees: every channel must land within the stated levels of native.
 */
class VibrancyMatrixTest {

    private static final int[] BACKDROPS = {0x000000, 0xffffff, 0x808080, 0xc8c8c8, 0x3a3a3c, 0xff3b30, 0x34c759,
        0x007aff, 0xffcc00, 0xaf52de, 0x5ac8fa, 0xa9a9a9, 0x626262};

    @Test
    void matchesUikitForEveryMeasuredTint() throws IOException {
        InputStream in = VibrancyMatrixTest.class.getResourceAsStream("/tab-glass-motion/native-vibrancy.csv");
        assertNotNull(in);
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        int n = 0;
        int worstDark = 0;
        int worstLight = 0;
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#") || line.trim().length() == 0) {
                    continue;
                }
                String[] p = line.split(",");
                boolean dark = "dark".equals(p[0]);
                int tint = Integer.parseInt(p[1], 16);
                float[] nat = new float[12];
                for (int i = 0; i < 12; i++) {
                    nat[i] = Float.parseFloat(p[2 + i]);
                }
                float[] m = VibrancyMatrix.forTint(tint, dark);
                for (int b : BACKDROPS) {
                    int d = maxChannelDelta(VibrancyMatrix.apply(nat, b), VibrancyMatrix.apply(m, b));
                    if (dark) {
                        worstDark = Math.max(worstDark, d);
                    } else {
                        worstLight = Math.max(worstLight, d);
                    }
                }
                n++;
            }
        } finally {
            r.close();
        }
        assertTrue(n > 600, "fixture truncated: " + n);
        // Dark is a closed form: exact up to rounding.
        assertTrue(worstDark <= 1, "dark off by " + worstDark + " levels");
        // Light is a fitted table: under 2 levels even over black, the worst backdrop.
        assertTrue(worstLight <= 2, "light off by " + worstLight + " levels");
    }

    @Test
    void theSystemBlueSelectionIsVividInDark() {
        // Dark system blue over the dark glass's grey: the native bar draws a bright
        // cyan-blue, far from the plain #0A84FF a flat tint would paint.
        int out = VibrancyMatrix.apply(VibrancyMatrix.forTint(0x0a84ff, true), 0x626262);
        assertTrue(((out >> 8) & 0xff) > 180, "green channel " + ((out >> 8) & 0xff));
    }

    private static int maxChannelDelta(int a, int b) {
        int d = 0;
        for (int s = 0; s <= 16; s += 8) {
            d = Math.max(d, Math.abs(((a >> s) & 0xff) - ((b >> s) & 0xff)));
        }
        return d;
    }
}
