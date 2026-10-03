/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ListTile geometry, headless with stubbed intrinsics (Dp scale is 1 without
 * a Display, so logical pixels == pixels): 16lp horizontal padding, 16lp
 * gaps, title above subtitle, leading/trailing vertically centered, 56lp
 * minimum height, tap overlay covering the tile.
 */
class ListTileLayoutTest {

    private ListTileRenderElement mountAndLayout(ListTile tile, BoxConstraints c) {
        RenderHost host = new RenderHost();
        ListTileRenderElement e = (ListTileRenderElement) FlutterUI.mount(tile, host, new BuildOwner());
        e.layout(c);
        e.position(0, 0);
        return e;
    }

    @Test
    void fullTileGeometry() {
        ListTile tile = new ListTile();
        tile.leading(new ProbeBox(20, 20));
        tile.title(new ProbeBox(100, 20));
        tile.subtitle(new ProbeBox(80, 16));
        tile.trailing(new ProbeBox(24, 24));
        tile.onTap(() -> {
        });

        ListTileRenderElement e = mountAndLayout(tile, BoxConstraints.loose(300, Double.POSITIVE_INFINITY));

        // 72lp, not 56: Material sizes a tile by its LINE COUNT, and this one has a
        // subtitle, so it is a two-line tile. 56lp is the one-line height, asserted by
        // titleOnlyTileOmitsMissingSections below.
        assertEquals(new Size(300, 72), e.size(), "72lp two-line height, full width");

        List<RenderElement> children = e.renderChildren();
        assertEquals(5, children.size(), "leading, title, subtitle, trailing, overlay");
        RenderElement leading = children.get(0);
        RenderElement title = children.get(1);
        RenderElement subtitle = children.get(2);
        RenderElement trailing = children.get(3);
        RenderElement overlay = children.get(4);

        assertEquals(16, leading.x(), "leading at the 16lp inset");
        assertEquals(26, leading.y(), "leading vertically centered: (72-20)/2");
        assertEquals(52, title.x(), "title after leading + 16lp gap: 16+20+16");
        assertEquals(18, title.y(), "text block centered: (72-36)/2");
        assertEquals(52, subtitle.x(), "subtitle aligned with title");
        assertEquals(38, subtitle.y(), "subtitle right below the title: 18+20");
        assertEquals(260, trailing.x(), "trailing right-aligned: 300-16-24");
        assertEquals(24, trailing.y(), "trailing vertically centered: (72-24)/2");
        assertEquals(new Size(300, 72), overlay.size(), "the tap overlay covers the tile");
        assertEquals(0, overlay.x());
        assertEquals(0, overlay.y());
    }

    @Test
    void titleOnlyTileOmitsMissingSections() {
        ListTile tile = new ListTile();
        tile.title(new ProbeBox(50, 20));

        ListTileRenderElement e = mountAndLayout(tile, BoxConstraints.loose(200, Double.POSITIVE_INFINITY));

        assertEquals(new Size(200, 56), e.size());
        List<RenderElement> children = e.renderChildren();
        assertEquals(2, children.size(), "title + overlay");
        RenderElement title = children.get(0);
        assertEquals(16, title.x(), "no leading: title starts at the padding");
        assertEquals(18, title.y(), "(56-20)/2");
    }

    @Test
    void tallContentGrowsTheTileWithVerticalPadding() {
        ListTile tile = new ListTile();
        tile.leading(new ProbeBox(20, 20));
        tile.title(new ProbeBox(100, 60));

        ListTileRenderElement e = mountAndLayout(tile, BoxConstraints.loose(300, Double.POSITIVE_INFINITY));

        assertEquals(76, e.size().height(), "content 60 + 2*8lp vertical padding");
        assertTrue(e.size().height() > 56);
    }

    @Test
    void unboundedWidthShrinksToIntrinsicContent() {
        ListTile tile = new ListTile();
        tile.leading(new ProbeBox(20, 20));
        tile.title(new ProbeBox(100, 20));
        tile.trailing(new ProbeBox(24, 24));

        ListTileRenderElement e = mountAndLayout(tile,
                BoxConstraints.loose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        // 16 + 20 + 16 + 100 + 16 + 24 + 16
        assertEquals(208, e.size().width());
    }

    @Test
    void tapFiresOnTap() {
        final int[] taps = {0};
        ListTile tile = new ListTile();
        tile.title(new ProbeBox(10, 10));
        tile.onTap(() -> taps[0]++);

        ListTileRenderElement e = mountAndLayout(tile, BoxConstraints.loose(100, Double.POSITIVE_INFINITY));
        e.fireTap();
        assertEquals(1, taps[0]);
    }
}
