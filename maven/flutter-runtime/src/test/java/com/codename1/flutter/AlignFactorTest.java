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
import com.codename1.flutter.widgets.Align;
import com.codename1.flutter.widgets.ClipRect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Align's widthFactor/heightFactor - Flutter's RenderPositionedBox sizing. This is the
 * geometry an expand/collapse animation runs on: the gallery's category and settings lists
 * animate {@code ClipRect(child: Align(heightFactor: t, child: ...))} with t from 0 to 1,
 * so the box has to be a FRACTION of the child while the child keeps its full size.
 */
class AlignFactorTest {

    private RenderElement mountAndLayout(Widget root, BoxConstraints constraints) {
        BuildOwner owner = new BuildOwner();
        RenderHost host = new RenderHost();
        FlutterUI.mount(root, host, owner);
        RenderElement r = host.rootRenderElement();
        r.layout(constraints);
        r.position(0, 0);
        return r;
    }

    private Align align(Double heightFactor, Alignment alignment, Widget child) {
        Align a = new Align();
        a.heightFactor(heightFactor);
        a.alignment(alignment);
        a.child(child);
        return a;
    }

    @Test
    void heightFactorTakesAFractionOfTheChildHeight() {
        RenderElement root = mountAndLayout(
                align(0.25, Alignment.topCenter, new ProbeBox(100, 200)),
                BoxConstraints.loose(300, 300));
        assertEquals(new Size(300, 50), root.size());
    }

    @Test
    void heightFactorZeroCollapsesTheBoxWhileTheChildKeepsItsSize() {
        RenderElement root = mountAndLayout(
                align(0.0, Alignment.topCenter, new ProbeBox(100, 200)),
                BoxConstraints.loose(300, 300));
        assertEquals(0.0, root.size().height());
        assertEquals(new Size(100, 200), root.renderChildren().get(0).size(),
                "the child measures itself in full; only the box collapses");
    }

    @Test
    void heightFactorOneIsTheFullyExpandedEnd() {
        RenderElement root = mountAndLayout(
                align(1.0, Alignment.topCenter, new ProbeBox(100, 200)),
                BoxConstraints.loose(300, 300));
        assertEquals(200.0, root.size().height());
    }

    @Test
    void topCenterHoldsTheChildAtTheTopSoTheCollapseRevealsFromTheTop() {
        RenderElement root = mountAndLayout(
                align(0.25, Alignment.topCenter, new ProbeBox(100, 200)),
                BoxConstraints.loose(300, 300));
        RenderElement child = root.renderChildren().get(0);
        assertEquals(0, child.y(), "no vertical shift: the visible slice is the child's top");
        assertEquals(100, child.x(), "centred horizontally within 300");
    }

    @Test
    void widthFactorShrinkWrapsTheHorizontalAxisToo() {
        Align a = new Align();
        a.widthFactor(0.5);
        a.alignment(Alignment.topCenter);
        a.child(new ProbeBox(100, 200));
        RenderElement root = mountAndLayout(a, BoxConstraints.loose(300, 300));
        assertEquals(50.0, root.size().width());
        assertEquals(300.0, root.size().height(), "no heightFactor: that axis still fills");
    }

    @Test
    void noFactorsKeepsTheFillBehaviour() {
        RenderElement root = mountAndLayout(
                align(null, Alignment.center, new ProbeBox(100, 200)),
                BoxConstraints.loose(300, 300));
        assertEquals(new Size(300, 300), root.size());
    }

    @Test
    void anUnboundedAxisShrinkWrapsEvenWithoutAFactor() {
        RenderElement root = mountAndLayout(
                align(null, Alignment.center, new ProbeBox(100, 200)),
                new BoxConstraints(0, 300, 0, Double.POSITIVE_INFINITY));
        assertEquals(new Size(300, 200), root.size());
    }

    @Test
    void clipRectReportsTheCollapsedBoxNotTheChild() {
        // The ClipRect is what hides the overflow; it must take the Align's reduced size
        // rather than growing to the child, or there would be nothing to clip against.
        ClipRect clip = new ClipRect();
        clip.child(align(0.25, Alignment.topCenter, new ProbeBox(100, 200)));

        RenderElement root = mountAndLayout(clip, BoxConstraints.loose(300, 300));
        assertEquals(50.0, root.size().height());
    }
}
