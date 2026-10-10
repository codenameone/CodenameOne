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
package com.codename1.flutter;

import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Container;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The gallery's study card, reduced to the geometry that decides its size:
 *
 * <pre>
 * Container(
 *   padding: EdgeInsets.symmetric(horizontal: 4),
 *   margin: EdgeInsets.symmetric(vertical: 16),
 *   height: 240, width: 296,
 *   child: Material(...))
 * </pre>
 *
 * laid out inside a carousel viewport 240 logical pixels tall. Flutter's answer, measured
 * from the native app: the card's surface is 288 x 208 - 296 minus the horizontal padding,
 * and 240 minus BOTH vertical margins, because the height passes through a ConstrainedBox
 * that the viewport's own maximum clamps.
 */
class CarouselCardGeometryTest {

    private RenderElement mountAndLayout(Widget root, BoxConstraints constraints) {
        BuildOwner owner = new BuildOwner();
        RenderHost host = new RenderHost();
        FlutterUI.mount(root, host, owner);
        RenderElement r = host.rootRenderElement();
        r.layout(constraints);
        r.position(0, 0);
        return r;
    }

    private Container carouselCard(Widget child) {
        Container c = new Container();
        c.padding(EdgeInsets.symmetric(4, 0));
        c.margin(EdgeInsets.symmetric(0, 16));
        c.height(Double.valueOf(240));
        c.width(Double.valueOf(296));
        c.child(child);
        return c;
    }

    /** The viewport the carousel gives a page: as wide as the page slot, 240 tall. */
    private BoxConstraints viewport() {
        return new BoxConstraints(0, 304, 0, 240);
    }

    @Test
    void theCardOccupiesTheWholeViewportHeight() {
        RenderElement root = mountAndLayout(carouselCard(new ProbeBox(10, 10)), viewport());
        assertEquals(240.0, root.size().height(),
                "the Container plus its margins fills the viewport");
    }

    @Test
    void bothVerticalMarginsComeOffTheSurface() {
        RenderElement root = mountAndLayout(carouselCard(new ProbeBox(10, 10)), viewport());
        RenderElement surface = root.renderChildren().get(0);
        // 240 - 16 - 16. The height of 240 is a ConstrainedBox inside the margin, so the
        // viewport's own 240 maximum clamps it rather than the two adding up.
        assertEquals(208.0, surface.size().height());
    }

    @Test
    void horizontalPaddingComesOffTheSurfaceWidth() {
        RenderElement root = mountAndLayout(carouselCard(new ProbeBox(10, 10)), viewport());
        RenderElement surface = root.renderChildren().get(0);
        assertEquals(288.0, surface.size().width(), "296 wide, less 4 of padding each side");
    }

    @Test
    void theSurfaceSitsBelowTheTopMargin() {
        RenderElement root = mountAndLayout(carouselCard(new ProbeBox(10, 10)), viewport());
        RenderElement surface = root.renderChildren().get(0);
        assertEquals(16, surface.y());
        assertEquals(4, surface.x());
    }
}
