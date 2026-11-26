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
package com.codenameone.developerguide.screenshots;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.MasonryLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/// Tiles of different heights packed into two columns, for the developer guide.
class ComponentsMasonryFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-masonry-layout";
    }

    @Override
    public Form build() {
        Form hi = new Form("Gallery", BoxLayout.y());
        MasonryLayout m = new MasonryLayout(2);
        m.setGap(1.5f);
        Container grid = new Container(m);
        int[] heights = {24, 14, 18, 30, 12, 20, 16, 22};
        int[] colors = {0x90caf9, 0xa5d6a7, 0xffcc80, 0xce93d8, 0xef9a9a, 0x80cbc4, 0xfff59d, 0xb0bec5};
        for (int i = 0; i < heights.length; i++) {
            Label tile = new Label("Tile " + (i + 1));
            tile.getAllStyles().setBgColor(colors[i]);
            tile.getAllStyles().setBgTransparency(255);
            tile.getAllStyles().setFgColor(0x222222);
            tile.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
            tile.getAllStyles().setPadding(heights[i] / 2, heights[i] / 2, 2, 2);
            grid.add(tile);
        }
        hi.add(grid);
        hi.show();
        return hi;
    }
}
