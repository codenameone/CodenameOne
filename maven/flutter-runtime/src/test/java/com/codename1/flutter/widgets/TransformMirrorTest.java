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

/// Transform.scale with a negative scaleX/scaleY mirrors its subtree -- Flutter's
/// `Transform.scale(scaleX: -1)` flips a card rather than hiding it. paintWithEffect's
/// collapsed-scale check in {@link TransformRenderElement} used to build a signed
/// destination width/height from `w * sx` and treat a negative result exactly like a
/// scaled-to-zero transform (`dw <= 0`), so a mirror painted nothing -- even though
/// hit testing, which is pure algebra and invertible for any nonzero factor, kept
/// reporting the mirrored subtree as live where it was invisible.
///
/// {@link RasterDisplay} reports no 2D transform support (the default every port
/// starts from), so these tests exercise the image-flip fallback rather than the
/// `Graphics#setTransform` path.
class TransformMirrorTest {

    private static final int SIZE = 40;
    private static final long RED = 0xffff0000L;
    private static final long BLUE = 0xff0000ffL;

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static Widget coloredBox(long argb) {
        ColoredBox c = new ColoredBox();
        c.color(new Color(argb));
        return c;
    }

    /// A left/right half split, red on the left and blue on the right -- asymmetric,
    /// so a horizontal mirror shows up as the colours swapping sides rather than a
    /// uniform box redrawn identically.
    private static Widget leftRightHalves() {
        Positioned left = new Positioned();
        left.left(0);
        left.top(0);
        left.width(SIZE / 2.0);
        left.height(SIZE);
        left.child(coloredBox(RED));

        Positioned right = new Positioned();
        right.left(SIZE / 2.0);
        right.top(0);
        right.width(SIZE / 2.0);
        right.height(SIZE);
        right.child(coloredBox(BLUE));

        Stack s = new Stack();
        s.children(DartList.of(left, right));
        return s;
    }

    /// A top/bottom half split, red on top and blue on the bottom, for the vertical
    /// mirror.
    private static Widget topBottomHalves() {
        Positioned top = new Positioned();
        top.left(0);
        top.top(0);
        top.width(SIZE);
        top.height(SIZE / 2.0);
        top.child(coloredBox(RED));

        Positioned bottom = new Positioned();
        bottom.left(0);
        bottom.top(SIZE / 2.0);
        bottom.width(SIZE);
        bottom.height(SIZE / 2.0);
        bottom.child(coloredBox(BLUE));

        Stack s = new Stack();
        s.children(DartList.of(top, bottom));
        return s;
    }

    private static Widget sized(Widget child) {
        SizedBox box = new SizedBox();
        box.width(SIZE);
        box.height(SIZE);
        box.child(child);
        return box;
    }

    private static Image paint(Widget w) {
        Container root = FlutterUI.wrap(w);
        root.setX(0);
        root.setY(0);
        root.setWidth(SIZE);
        root.setHeight(SIZE);
        root.layoutContainer();
        Image img = Image.createImage(SIZE, SIZE, 0xffffffff);
        Graphics g = img.getGraphics();
        root.paintComponent(g);
        return img;
    }

    private static int pixel(Image img, int x, int y) {
        return ((RasterDisplay.Surface) img.getImage()).at(x, y);
    }

    private static String hex(int argb) {
        return Integer.toHexString(argb);
    }

    @Test
    void mirroredScaleXPaintsTheFlippedSubtreeInstead() {
        Transform t = Transform.scale(null, null, -1.0, null, null, null, null, null, sized(leftRightHalves()));
        Image img = paint(t);
        int leftPixel = pixel(img, 10, 20);
        int rightPixel = pixel(img, 30, 20);
        // Flipped about the centre: the blue right half lands on the left and the red
        // left half lands on the right. Before the fix both pixels stayed the white
        // background -- the subtree was never painted at all.
        assertEquals(hex(0xff0000ff), hex(leftPixel), "the right (blue) half is mirrored to the left");
        assertEquals(hex(0xffff0000), hex(rightPixel), "the left (red) half is mirrored to the right");
    }

    @Test
    void mirroredScaleYPaintsTheFlippedSubtreeInstead() {
        Transform t = Transform.scale(null, null, null, -1.0, null, null, null, null, sized(topBottomHalves()));
        Image img = paint(t);
        int topPixel = pixel(img, 20, 10);
        int bottomPixel = pixel(img, 20, 30);
        assertEquals(hex(0xff0000ff), hex(topPixel), "the bottom (blue) half is mirrored to the top");
        assertEquals(hex(0xffff0000), hex(bottomPixel), "the top (red) half is mirrored to the bottom");
    }
}
