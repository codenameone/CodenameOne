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
import com.codename1.flutter.widgets.ConstrainedBox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ConstrainedBox constraint intersection (BoxConstraints.enforce), headless
 * (logical pixels == device pixels).
 */
class ConstrainedBoxTest {

    private RenderElement mountAndLayout(Widget root, BoxConstraints constraints) {
        BuildOwner owner = new BuildOwner();
        RenderHost host = new RenderHost();
        FlutterUI.mount(root, host, owner);
        RenderElement r = host.rootRenderElement();
        r.layout(constraints);
        r.position(0, 0);
        return r;
    }

    @Test
    void enforceClampsAdditionalIntoIncomingBounds() {
        BoxConstraints additional = new BoxConstraints(100, 200, 50, 80);
        BoxConstraints incoming = BoxConstraints.loose(150, 60);
        BoxConstraints enforced = additional.enforce(incoming);
        assertEquals(100.0, enforced.minWidth());
        assertEquals(150.0, enforced.maxWidth());
        assertEquals(50.0, enforced.minHeight());
        assertEquals(60.0, enforced.maxHeight());
    }

    @Test
    void enforceWithTightIncomingWins() {
        BoxConstraints additional = new BoxConstraints(100, 200, 0, 80);
        BoxConstraints enforced = additional.enforce(BoxConstraints.tight(50, 40));
        assertEquals(50.0, enforced.minWidth());
        assertEquals(50.0, enforced.maxWidth());
        assertEquals(40.0, enforced.minHeight());
        assertEquals(40.0, enforced.maxHeight());
    }

    @Test
    void dartConstructedBoxConstraintsDefaultsToUnconstrained() {
        BoxConstraints c = new BoxConstraints();
        assertEquals(0.0, c.minWidth());
        assertEquals(Double.POSITIVE_INFINITY, c.maxWidth());
        assertEquals(0.0, c.minHeight());
        assertEquals(Double.POSITIVE_INFINITY, c.maxHeight());
        c.minWidth(10.0);
        c.maxWidth(20.0);
        c.minHeight(5.0);
        c.maxHeight(15.0);
        assertEquals(new BoxConstraints(10, 20, 5, 15), c);
    }

    @Test
    void constrainedBoxImposesMinimumsOnASmallChild() {
        BoxConstraints additional = new BoxConstraints();
        additional.minWidth(100.0);
        additional.minHeight(40.0);
        ConstrainedBox box = new ConstrainedBox();
        box.constraints(additional);
        box.child(new ProbeBox(10, 10));

        RenderElement root = mountAndLayout(box, BoxConstraints.loose(300, 300));
        assertEquals(new Size(100, 40), root.size());
        assertEquals(new Size(100, 40), root.renderChildren().get(0).size());
    }

    @Test
    void constrainedBoxMaximumsCapTheChild() {
        BoxConstraints additional = new BoxConstraints();
        additional.maxWidth(50.0);
        additional.maxHeight(20.0);
        ConstrainedBox box = new ConstrainedBox();
        box.constraints(additional);
        box.child(new ProbeBox(500, 500));

        RenderElement root = mountAndLayout(box, BoxConstraints.loose(300, 300));
        assertEquals(new Size(50, 20), root.size());
    }

    @Test
    void incomingTightConstraintsOverrideTheAdditionalOnes() {
        BoxConstraints additional = new BoxConstraints();
        additional.minWidth(100.0);
        additional.maxWidth(200.0);
        ConstrainedBox box = new ConstrainedBox();
        box.constraints(additional);
        box.child(new ProbeBox(10, 10));

        RenderElement root = mountAndLayout(box, BoxConstraints.tight(60, 60));
        assertEquals(new Size(60, 60), root.size());
    }

    @Test
    void constrainedBoxWithoutChildSizesToTheSmallestEnforcedSize() {
        BoxConstraints additional = new BoxConstraints();
        additional.minWidth(80.0);
        additional.minHeight(30.0);
        ConstrainedBox box = new ConstrainedBox();
        box.constraints(additional);

        RenderElement root = mountAndLayout(box, BoxConstraints.loose(300, 300));
        assertEquals(new Size(80, 30), root.size());
    }
}
