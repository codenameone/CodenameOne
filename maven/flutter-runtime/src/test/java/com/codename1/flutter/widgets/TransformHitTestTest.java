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

import com.codename1.flutter.Alignment;
import com.codename1.flutter.Color;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Widget;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// A Transform moves where its child is hit, not just where it is drawn, as long as
/// transformHitTests is on (Flutter's default). The child used to stay tappable at its
/// untransformed position and dead where the user could see it.
class TransformHitTestTest {

    private int taps;

    @BeforeEach
    void install() {
        RasterDisplay.install();
        taps = 0;
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    /// A 50x50 tappable box at the top-left, under {@code transform}.
    private Widget tappable(Transform transform) {
        ColoredBox face = new ColoredBox();
        face.color(new Color(0xff0000ffL));
        SizedBox box = new SizedBox();
        box.width(50);
        box.height(50);
        box.child(face);
        GestureDetector g = new GestureDetector();
        g.onTap(() -> taps++);
        g.child(box);
        transform.child(g);
        Align a = new Align();
        a.alignment(Alignment.topLeft);
        a.child(transform);
        return a;
    }

    private static Form show(Widget w) {
        Container root = FlutterUI.wrap(w);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, root);
        f.setX(0);
        f.setY(0);
        f.setWidth(RasterDisplay.WIDTH);
        f.setHeight(RasterDisplay.HEIGHT);
        f.layoutContainer();
        f.getContentPane().layoutContainer();
        root.layoutContainer();
        return f;
    }

    private int tapsAt(Form f, int x, int y) {
        int before = taps;
        f.pointerPressed(x, y);
        f.pointerReleased(x, y);
        return taps - before;
    }

    /// The laid-out top-left of the transformed box on screen. Taken from the pane, not
    /// assumed: the form's padding depends on whatever theme the JVM has installed.
    private static int[] origin(Form f) {
        Container root = (Container) f.getContentPane().getComponentAt(0);
        com.codename1.ui.Component pane = root.getComponentAt(0);
        return new int[] {pane.getAbsoluteX(), pane.getAbsoluteY()};
    }

    @Test
    void aTranslatedChildIsHitWhereItIsDrawn() {
        Form f = show(tappable(Transform.translate(null, new Offset(100, 0), null, null, null)));
        int[] o = origin(f);
        int y = o[1] + 20;
        assertEquals(1, tapsAt(f, o[0] + 120, y), "the drawn position takes the tap");
        assertEquals(0, tapsAt(f, o[0] + 20, y), "the untransformed position is empty");
    }

    @Test
    void transformHitTestsFalseKeepsTheLayoutPosition() {
        Form f = show(tappable(Transform.translate(null, new Offset(100, 0), Boolean.FALSE, null, null)));
        int[] o = origin(f);
        int y = o[1] + 20;
        assertEquals(0, tapsAt(f, o[0] + 120, y));
        assertEquals(1, tapsAt(f, o[0] + 20, y));
    }

    /// Scaled 2x about its centre (25, 25): the box covers -25..75 on screen.
    @Test
    void aScaledChildIsHitAcrossItsScaledBounds() {
        Form f = show(tappable(Transform.scale(null, 2.0, null, null, null, null, null, null, null)));
        int[] o = origin(f);
        int y = o[1] + 25;
        assertEquals(1, tapsAt(f, o[0] + 70, y), "inside the scaled box, outside the layout box");
        assertEquals(0, tapsAt(f, o[0] + 80, y), "outside both");
    }

    /// A drag the subtree passes up to its ancestors -- how Codename One finds the
    /// scrollable to move -- leaves the transformed pane in screen pixels again. Handed
    /// up in the subtree's coordinates, a scroller outside would jump by the offset.
    @Test
    void aDragHandedUpLeavesInScreenPixels() {
        Form f = show(tappable(Transform.translate(null, new Offset(100, 0), null, null, null)));
        Container root = (Container) f.getContentPane().getComponentAt(0);
        final java.util.List<Integer> seen = new java.util.ArrayList<Integer>();
        root.addPointerDraggedListener(e -> seen.add(e.getX()));
        int[] o = origin(f);
        int y = o[1] + 20;
        f.pointerPressed(o[0] + 120, y);
        f.pointerDragged(o[0] + 130, y);
        f.pointerDragged(o[0] + 140, y);
        f.pointerReleased(o[0] + 140, y);
        org.junit.jupiter.api.Assertions.assertFalse(seen.isEmpty(), "the drag reached the ancestor");
        for (int x : seen) {
            org.junit.jupiter.api.Assertions.assertTrue(x >= o[0] + 120, "screen pixels, got " + seen);
        }
    }

    /// Turned a quarter about its centre, a box 50 wide and 10 tall stands upright.
    @Test
    void aRotatedChildIsHitWhereItIsDrawn() {
        ColoredBox face = new ColoredBox();
        face.color(new Color(0xff0000ffL));
        SizedBox bar = new SizedBox();
        bar.width(50);
        bar.height(10);
        bar.child(face);
        GestureDetector g = new GestureDetector();
        g.onTap(() -> taps++);
        g.child(bar);
        Transform t = Transform.rotate(null, Math.PI / 2, null, null, null, null, g);
        Align a = new Align();
        a.alignment(Alignment.topLeft);
        a.child(t);
        Form f = show(a);
        int[] o = origin(f);
        // Centre (25, 5); upright the bar spans x 20..30, y -20..30.
        assertEquals(1, tapsAt(f, o[0] + 25, o[1] + 25), "inside the turned bar");
        assertEquals(0, tapsAt(f, o[0] + 45, o[1] + 5), "inside the layout bar, outside the turned one");
    }
}
