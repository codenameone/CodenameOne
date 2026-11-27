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

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.MasonryLayout;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.Layout;

/// `MasonryLayout` packing tiles of different heights into two columns, each
/// placed under the shorter column, in light and dark.
public class MasonryGridScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=MasonryGrid status=SKIPPED reason=phone-layout-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "MasonryGrid";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        Container grid = new Container(new MasonryLayout(2));
        grid.setScrollableY(true);
        grid.setScrollVisible(false);
        int[] heights = {30, 18, 22, 34, 16, 26, 20, 28};
        int[] colors = {0x3b82f6, 0x10b981, 0xf59e0b, 0xef4444, 0x8b5cf6, 0x06b6d4, 0xec4899, 0x84cc16};
        for (int i = 0; i < heights.length; i++) {
            Label tile = new Label("Tile " + (i + 1));
            tile.getAllStyles().setBgColor(colors[i]);
            tile.getAllStyles().setBgTransparency(255);
            tile.getAllStyles().setFgColor(0xffffff);
            tile.getAllStyles().setAlignment(CN.CENTER);
            tile.setPreferredH(CN.convertToPixels(heights[i]));
            grid.add(tile);
        }
        form.add(BorderLayout.CENTER, grid);
    }
}
