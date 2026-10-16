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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.codename1.flutter.Color;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Widget;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import dart.core.DartList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// Opacity fades its subtree as ONE layer, as Flutter's does: where two opaque
/// children overlap, the result is the same half-tone as where they do not.
/// Applying the alpha per draw blended the overlap twice, so it came out darker.
class OpacityLayerTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static Widget box(double left, double width) {
        ColoredBox c = new ColoredBox();
        c.color(new Color(0xffff0000L));
        Positioned p = new Positioned();
        p.left(left);
        p.top(0);
        p.width(width);
        p.height(40);
        p.child(c);
        return p;
    }

    private static Opacity fade(double opacity, Widget child) {
        Opacity o = new Opacity();
        o.opacity(opacity);
        o.child(child);
        return o;
    }

    private static Widget twoOverlapping() {
        Stack s = new Stack();
        s.children(DartList.of(box(0, 60), box(30, 60)));
        return s;
    }

    /// Lays {@code w} out in a 200x100 box and paints it over white.
    private static Image paint(Container root) {
        root.setX(0);
        root.setY(0);
        root.setWidth(200);
        root.setHeight(100);
        root.layoutContainer();
        Image img = Image.createImage(200, 100, 0xffffffff);
        Graphics g = img.getGraphics();
        root.paintComponent(g);
        return img;
    }

    private static int pixel(Image img, int x, int y) {
        return ((RasterDisplay.Surface) img.getImage()).at(x, y);
    }

    /// Half red over white: 0.5 of 255 rounds either way, so 0x7f and 0x80 both are.
    private static void assertHalfRed(int argb) {
        String h = hex(argb);
        if (!"ffff7f7f".equals(h) && !"ffff8080".equals(h)) {
            assertEquals("ffff8080", h, "half red over white");
        }
    }

    private static String hex(int argb) {
        return Integer.toHexString(argb);
    }

    @Test
    void overlappingChildrenFadeAsOneLayer() {
        Image img = paint(FlutterUI.wrap(fade(0.5, twoOverlapping())));
        int alone = pixel(img, 10, 20);
        int overlap = pixel(img, 45, 20);
        assertHalfRed(alone);
        assertEquals(hex(alone), hex(overlap), "the overlap must not blend twice");
        assertEquals("ffffffff", hex(pixel(img, 120, 20)), "outside both boxes stays white");
    }

    @Test
    void nestedOpacityMultiplies() {
        Image img = paint(FlutterUI.wrap(fade(0.5, fade(0.5, twoOverlapping()))));
        // 0.25 red over white: 255 - 0.25 * 255 ~ 0xbf
        assertEquals(hex(pixel(img, 10, 20)), hex(pixel(img, 45, 20)));
        assertEquals("ffffbfbf", hex(pixel(img, 10, 20)));
    }

    @Test
    void zeroAndOnePaintNothingAndEverything() {
        Image none = paint(FlutterUI.wrap(fade(0, twoOverlapping())));
        assertEquals("ffffffff", hex(pixel(none, 45, 20)));
        Image all = paint(FlutterUI.wrap(fade(1, twoOverlapping())));
        assertEquals("ffff0000", hex(pixel(all, 45, 20)));
    }

    /// A lone box still needs the layer: its fill is a style background, which Codename
    /// One paints at the style's own alpha, not through the current one.
    @Test
    void aLoneColoredBoxFades() {
        ColoredBox leaf = new ColoredBox();
        leaf.color(new Color(0xffff0000L));
        Image img = paint(FlutterUI.wrap(fade(0.5, leaf)));
        assertHalfRed(pixel(img, 10, 20));
    }

    /// A lone Text draws its glyphs once each, through the current alpha: it keeps the
    /// direct path and needs no offscreen.
    @Test
    void aLoneTextNeedsNoLayer() {
        Container root = FlutterUI.wrap(fade(0.5, new Text("hello")));
        // The app theme derives FlutterText from Label, which has no background; this
        // display has no theme, whose default style is opaque.
        Container pane = (Container) find(root).component();
        pane.getComponentAt(0).getAllStyles().setBgTransparency(0);
        paint(root);
        assertEquals(0, find(root).layersRendered(), "a single draw needs no layer");
        // A background behind the text is a second draw under the first: layer.
        pane.getComponentAt(0).getAllStyles().setBgTransparency(255);
        paint(root);
        assertEquals(1, find(root).layersRendered());
    }

    /// An animated fade repaints every frame at the same size: the offscreen is reused,
    /// not reallocated.
    @Test
    void theLayerIsReusedAcrossFrames() {
        Container root = FlutterUI.wrap(fade(0.5, twoOverlapping()));
        paint(root);
        OpacityRenderElement e = find(root);
        Image first = e.lastLayer();
        paint(root);
        assertSame(first, e.lastLayer());
        assertEquals(2, e.layersRendered());
    }

    private static OpacityRenderElement find(Container root) {
        final OpacityRenderElement[] out = new OpacityRenderElement[1];
        com.codename1.flutter.Element top =
                ((com.codename1.flutter.rendering.FlutterRootLayout) root.getLayout()).host().rootElement();
        visit(top, out);
        return out[0];
    }

    private static void visit(com.codename1.flutter.Element e, final OpacityRenderElement[] out) {
        if (e instanceof OpacityRenderElement && out[0] == null) {
            out[0] = (OpacityRenderElement) e;
        }
        e.visitChildren(new dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.Element>() {
            @Override
            public void call(com.codename1.flutter.Element c) {
                visit(c, out);
            }
        });
    }
}
