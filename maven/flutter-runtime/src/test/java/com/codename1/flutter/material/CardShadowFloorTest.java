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
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.RoundRectBorder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression for the gallery's Cards demo: a {@code Card} at Material's default
 * elevation (1, unstated by {@code demos/material/cards_demo.dart}'s {@code
 * Card(clipBehavior: ..., shape: ..., child: ...)}) cast no visible shadow at
 * all, where Flutter's reference shows a soft one just past the card's own
 * edge.
 *
 * <p>{@link CardRenderElement#applyStyle} computed {@code shadowSpread} as
 * {@code 0.25 + elevation * 0.25}mm, un-floored -- at elevation 1 that is
 * 0.5mm. A pixel scan of {@code
 * docs/developer-guide/img/flutter-gallery-card.jpg} shows the card's own
 * fill running flat to its edge and jumping straight to the page's white in
 * BOTH columns, with none of the darker band (down to luma 219 against a 242
 * fill and a 255 page) Flutter's copy shows for three rows just past that
 * same edge. {@link ElevationShadow#spreadMm} floors every caller -- Card
 * included -- at 1mm, the value the Shrine "NEXT" button (also in this
 * chapter) used and DOES show a visible, if faint, halo at in its own
 * screenshot. See {@link ElevationShadow#spreadMm}'s own comment for the
 * full reasoning.</p>
 */
class CardShadowFloorTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static Component mountCard(Double elevation) {
        Card card = new Card();
        if (elevation != null) {
            card.elevation(elevation);
        }
        card.child(new Text("hello"));
        Element root = FlutterUI.mount(card, new RenderHost(), new BuildOwner());
        CardRenderElement el = (CardRenderElement) root;
        el.layout(BoxConstraints.loose(1000, 1000));
        Component c = el.component();
        assertNotNull(c, "RasterDisplay gives createComponent a real Container");
        return c;
    }

    @Test
    void aDefaultCardsShadowSpreadIsFlooredAtOneMillimetre() {
        // No elevation stated: CardRenderElement falls back to Material's own
        // default (1), the same as the gallery's Card(...) calls.
        Component c = mountCard(null);
        assertTrue(c.getUnselectedStyle().getBorder() instanceof RoundRectBorder);
        RoundRectBorder border = (RoundRectBorder) c.getUnselectedStyle().getBorder();
        assertEquals(1f, border.getShadowSpread(), 0.001f,
                "elevation 1's un-floored 0.25 + 1 * 0.25 = 0.5mm never showed up in the "
                        + "shipped gallery screenshot; ElevationShadow.spreadMm's 1mm floor "
                        + "is what the Shrine NEXT button already proves visible");
        assertTrue(border.getShadowOpacity() > 0, "elevation 1 must still cast SOME shadow");
    }

    @Test
    void aHigherElevationCardsShadowIsStrongerThanTheDefault() {
        Component low = mountCard(1.0);
        Component high = mountCard(8.0);
        RoundRectBorder lowBorder = (RoundRectBorder) low.getUnselectedStyle().getBorder();
        RoundRectBorder highBorder = (RoundRectBorder) high.getUnselectedStyle().getBorder();
        assertTrue(highBorder.getShadowOpacity() > lowBorder.getShadowOpacity(),
                "a more elevated card must read as MORE lifted, not wear one fixed shadow "
                        + "at every elevation");
    }
}
