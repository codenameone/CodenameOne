/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.ui.plaf;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the cost of constructing a UIManager, which sits on every
 * application's cold start.
 *
 * <p>{@link com.codename1.ui.Display#mainEDTLoop()} calls
 * {@code UIManager.getInstance()} in its preamble, before it reaches the loop
 * that drains {@code pendingIdleSerialCalls} -- so the application start, queued
 * by then, waits for the constructor to return. Instrumenting a native build put
 * ~94ms of a 266ms launch inside it.</p>
 *
 * <p>Nearly all of that is first-touch cost -- class initialisers and the first
 * call into the platform font stack -- not the theme table. This test pins the
 * table down: {@link UIManager#resetThemeProps} fills a {@code HashMap} with 448
 * constants and resolves three fonts, and once those one-off costs are paid it
 * is sub-millisecond work. Measured here at ~0.07ms compiled and ~0.33ms
 * interpreted ({@code -Xint}), flat across repeats.</p>
 *
 * <p>The bound below is deliberately three orders of magnitude above that. It is
 * not a performance target -- it is a tripwire for someone moving real work into
 * the constructor (I/O, resource parsing, style resolution), which would land on
 * the cold start of every Codename One application. Keep it loose enough that a
 * loaded CI machine can never trip it.</p>
 */
public class UIManagerStartupCostTest extends UITestBase {

    /// Three orders of magnitude of headroom over the measured ~0.07ms.
    private static final long MAX_WARM_MILLIS = 20;

    @Test
    public void constructionStaysCheapOnceWarm() {
        // Pay the one-off costs first: this JVM may not have touched the font
        // stack yet, and the constructor is large enough to be worth compiling.
        for (int i = 0; i < 3; i++) {
            UIManager.createInstance();
        }

        int runs = 20;
        long[] each = new long[runs];
        for (int i = 0; i < runs; i++) {
            long t0 = System.nanoTime();
            UIManager.createInstance();
            each[i] = System.nanoTime() - t0;
        }
        java.util.Arrays.sort(each);
        long medianMicros = each[runs / 2] / 1000;
        System.out.println("UIManager construction: median=" + medianMicros
                + "us min=" + (each[0] / 1000) + "us max=" + (each[runs - 1] / 1000) + "us");

        assertTrue(medianMicros < MAX_WARM_MILLIS * 1000,
                "Constructing a UIManager took " + medianMicros
                + "us, over the " + MAX_WARM_MILLIS + "ms budget. This runs on the EDT"
                + " before any application code, so whatever was added here delays every"
                + " Codename One application's first frame.");
    }
}
