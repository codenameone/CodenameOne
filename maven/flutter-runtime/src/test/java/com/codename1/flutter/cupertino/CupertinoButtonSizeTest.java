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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.flutter.widgets.Text;
import com.codename1.flutter.material.ButtonRenderElement;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.Style;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End to end: {@link CupertinoButton}'s Flutter-sized {@link
 * com.codename1.flutter.material.ButtonStyle} (see its own class comment)
 * actually reaches the mounted CN1 {@code Button} through
 * {@code ButtonRenderElement.resolvedPadding}/{@code applyLabelFont}/{@code
 * performLayout} -- not just held on the composed widget, which {@link
 * CupertinoButtonThemeTest} already covers.
 */
class CupertinoButtonSizeTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static ButtonRenderElement mount(CupertinoButton b) {
        Element root = FlutterUI.mount(b, new RenderHost(), new BuildOwner());
        final ButtonRenderElement[] found = new ButtonRenderElement[1];
        visit(root, found);
        assertNotNull(found[0], "expected CupertinoButton to compose onto a material button");
        return found[0];
    }

    private static void visit(Element e, ButtonRenderElement[] found) {
        if (e == null || found[0] != null) {
            return;
        }
        if (e instanceof ButtonRenderElement) {
            found[0] = (ButtonRenderElement) e;
            return;
        }
        e.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element child) {
                visit(child, found);
            }
        });
    }

    @Test
    void unfilledGetsTwentyBySixteenPaddingAndSeventeenPointLabel() {
        CupertinoButton b = new CupertinoButton();
        b.onPressed(() -> { });
        b.child(new Text("Button"));

        ButtonRenderElement el = mount(b);
        Component c = el.component();
        assertNotNull(c, "RasterDisplay gives createComponent a real Button");
        Style s = c.getUnselectedStyle();
        // kCupertinoButtonPadding[CupertinoButtonSize.large] (Flutter 3.35):
        // vertical 16, horizontal 20.
        int expectedV = (int) Math.round(Dp.px(16));
        int expectedH = (int) Math.round(Dp.px(20));
        assertEquals(expectedV, s.getPaddingTop());
        assertEquals(expectedV, s.getPaddingBottom());
        assertEquals(expectedH, s.getPaddingLeftNoRTL());
        assertEquals(expectedH, s.getPaddingRightNoRTL());
        assertEquals((float) Dp.px(17), s.getFont().getPixelSize(), 0.01f);
    }

    @Test
    void filledGetsTwentyBySixteenPaddingAndFortyFourMinimum() {
        CupertinoButton b = CupertinoButton.filled(null, () -> { }, new Text("With Background"));

        ButtonRenderElement el = mount(b);
        Component c = el.component();
        assertNotNull(c);
        Style s = c.getUnselectedStyle();
        // Same kCupertinoButtonPadding[CupertinoButtonSize.large] map the unfilled
        // form reads -- Flutter 3.35 no longer gives the filled form its own, much
        // wider default.
        assertEquals((int) Math.round(Dp.px(16)), s.getPaddingTop());
        assertEquals((int) Math.round(Dp.px(16)), s.getPaddingBottom());
        assertEquals((int) Math.round(Dp.px(20)), s.getPaddingLeftNoRTL());
        assertEquals((int) Math.round(Dp.px(20)), s.getPaddingRightNoRTL());

        // kCupertinoButtonMinSize[CupertinoButtonSize.large] (44, SQUARE) -- not
        // Material 3's 64x40 -- read from performLayout, not from the style, since
        // it is a layout floor.
        com.codename1.flutter.rendering.Size laidOut = el.layout(
                com.codename1.flutter.rendering.BoxConstraints.loose(1000, 1000));
        assertTrue(laidOut.height() >= Dp.px(44) - 0.5,
                "a filled Cupertino button is never shorter than the 44lp floor");
    }
}
