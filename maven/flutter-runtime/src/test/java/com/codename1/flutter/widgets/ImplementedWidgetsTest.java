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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Offset;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Widgets that used to declare themselves unimplemented and render the child (or nothing)
 * now do the thing they describe.
 *
 * <p>Each of these was a silent hole: the app drew a plausible screen with a piece missing
 * and reported it only to a diagnostic channel nobody reads during normal use.</p>
 */
class ImplementedWidgetsTest {

    private static RenderElement mount(Widget w) {
        return (RenderElement) FlutterUI.mount(w, new RenderHost(), new BuildOwner());
    }

    // ---------------------------------------------------------------- RotatedBox

    @Test
    @DisplayName("an odd quarter turn swaps the box's width and height")
    void rotatedBoxSwapsAxes() {
        RotatedBox box = new RotatedBox();
        box.quarterTurns(1);
        box.child(new ProbeBox(40, 10));

        RenderElement e = mount(box);
        e.layout(BoxConstraints.loose(200, 200));

        assertEquals(10, e.size().width(), 1e-9, "a quarter turn stands the box on end");
        assertEquals(40, e.size().height(), 1e-9);
    }

    @Test
    @DisplayName("an even quarter turn leaves the footprint alone")
    void rotatedBoxKeepsAxesOnHalfTurn() {
        RotatedBox box = new RotatedBox();
        box.quarterTurns(2);
        box.child(new ProbeBox(40, 10));

        RenderElement e = mount(box);
        e.layout(BoxConstraints.loose(200, 200));

        assertEquals(40, e.size().width(), 1e-9);
        assertEquals(10, e.size().height(), 1e-9);
    }

    @Test
    @DisplayName("negative turns normalise rather than misbehaving")
    void rotatedBoxNormalisesNegativeTurns() {
        RotatedBox box = new RotatedBox();
        box.quarterTurns(-1);
        box.child(new ProbeBox(40, 10));

        RenderElement e = mount(box);
        e.layout(BoxConstraints.loose(200, 200));

        assertEquals(10, e.size().width(), 1e-9);
    }

    // ------------------------------------------------- FractionalTranslation

    @Test
    @DisplayName("a fractional translation does not move the layout")
    void fractionalTranslationIsPaintOnly() {
        FractionalTranslation t = new FractionalTranslation();
        t.translation(new Offset(0.5, 0.5));
        t.child(new ProbeBox(40, 20));

        RenderElement e = mount(t);
        e.layout(BoxConstraints.loose(200, 200));

        assertEquals(40, e.size().width(), 1e-9);
        assertEquals(20, e.size().height(), 1e-9);
        assertEquals(0.5, t.fraction().dx(), 1e-9);
    }

    // ------------------------------------------------------------------ GridTile

    @Test
    @DisplayName("a tile with a footer stacks it over the child")
    void gridTileStacksItsBands() {
        GridTile tile = new GridTile();
        tile.child(new ProbeBox(50, 50));
        tile.footer(new ProbeBox(50, 10));

        Widget built = tile.build(null);
        assertTrue(built instanceof Stack, "header/footer need a Stack, got " + built);
    }

    @Test
    @DisplayName("a bare tile is still just its child")
    void gridTileWithoutBandsIsTheChild() {
        GridTile tile = new GridTile();
        ProbeBox child = new ProbeBox(50, 50);
        tile.child(child);

        assertEquals(child, tile.build(null), "no bands means no wrapper");
    }

    // ------------------------------------------------------------------ ClipRect

    @Test
    @DisplayName("Clip.none really does not clip")
    void clipNoneDoesNotClip() {
        ClipRect c = new ClipRect();
        c.clipBehavior(com.codename1.flutter.Clip.none);
        c.child(new ProbeBox(10, 10));

        Element e = c.createElement();
        assertFalse(e instanceof ClipRectRenderElement,
                "Clip.none must not get the clipping element - its pane clips regardless");
    }

    @Test
    @DisplayName("the default behaviour still clips")
    void clipHardEdgeStillClips() {
        ClipRect c = new ClipRect();
        c.child(new ProbeBox(10, 10));

        assertTrue(c.createElement() instanceof ClipRectRenderElement);
    }

    // ---------------------------------------------------------------- FlutterLogo

    @Test
    @DisplayName("the logo paints something, at the size asked for")
    void flutterLogoRenders() {
        FlutterLogo logo = new FlutterLogo();
        logo.size(48);

        Widget built = logo.build(null);
        assertTrue(built instanceof CustomPaint, "the logo is drawn, got " + built);
        CustomPaint paint = (CustomPaint) built;
        assertNotNull(paint.getPainter(), "with a painter that actually draws");
        assertEquals(48, paint.getSize().width(), 1e-9);
    }
}
