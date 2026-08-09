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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.plaf.UIManager;

public class BackgroundThreadUiAccessTest extends BaseTest {

    /// Not safe for the runner's silent-timeout retry: it drives UI access from a background thread it starts itself,
    /// and that worker outlives runTest(). A retry resets the shared
    /// completion state, so a late done() from the first attempt's worker
    /// would complete the second attempt and advance the suite early.
    @Override
    public boolean isRetrySafe() {
        return false;
    }
    @Override
    public boolean runTest() {
        Thread worker = new Thread(() -> {
            try {
                Display display = Display.getInstance();
                int width = display.getDisplayWidth();
                int pixels = display.convertToPixels(10, true);
                UIManager manager = UIManager.getInstance();
                if (width <= 0 || pixels <= 0 || manager == null) {
                    fail("Unexpected display metrics: width=" + width + " pixels=" + pixels);
                    return;
                }
                done();
            } catch (Throwable t) {
                fail("Background UI access test failed: " + t);
            }
        }, "cn1-ui-access-bg");
        worker.start();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
