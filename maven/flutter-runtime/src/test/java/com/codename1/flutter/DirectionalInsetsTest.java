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
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Container;
import com.codename1.flutter.widgets.Directionality;
import com.codename1.flutter.widgets.Padding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EdgeInsetsDirectional is an EdgeInsets, so a layout that read left()/right() took
 * start as left whatever the ambient direction: under RTL, {@code only(start: 16)}
 * padded the left edge instead of the right.
 */
class DirectionalInsetsTest {

    private static RenderElement layoutUnder(TextDirection d, Widget child) {
        Directionality dir = new Directionality();
        dir.textDirection(d);
        dir.child(child);
        RenderHost host = new RenderHost();
        FlutterUI.mount(dir, host, new BuildOwner());
        RenderElement r = host.rootRenderElement();
        r.layout(BoxConstraints.loose(400, 600));
        r.position(0, 0);
        return r;
    }

    private static Padding padded() {
        Padding p = new Padding();
        p.padding(EdgeInsetsDirectional.only(16, 0, 0, 0));
        p.child(new ProbeBox(50, 20));
        return p;
    }

    private static Container margined() {
        Container c = new Container();
        c.margin(EdgeInsetsDirectional.only(16, 0, 0, 0));
        c.child(new ProbeBox(50, 20));
        return c;
    }

    @Test
    void paddingStartIsTheRightEdgeUnderRtl() {
        int ltr = layoutUnder(TextDirection.ltr, padded()).renderChildren().get(0).x();
        int rtl = layoutUnder(TextDirection.rtl, padded()).renderChildren().get(0).x();
        assertTrue(ltr > 0, "LTR: start is the left edge");
        assertEquals(0, rtl, "RTL: start is the right edge, so nothing pads the left");
    }

    @Test
    void containerMarginStartIsTheRightEdgeUnderRtl() {
        int ltr = layoutUnder(TextDirection.ltr, margined()).renderChildren().get(0).x();
        int rtl = layoutUnder(TextDirection.rtl, margined()).renderChildren().get(0).x();
        assertTrue(ltr > 0, "LTR: start is the left edge");
        assertEquals(0, rtl, "RTL: start is the right edge");
    }

    @Test
    void resolveSwapsStartAndEndOnlyUnderRtl() {
        EdgeInsetsDirectional d = EdgeInsetsDirectional.only(1, 2, 3, 4);
        EdgeInsets rtl = d.resolve(TextDirection.rtl);
        assertEquals(3, rtl.left());
        assertEquals(1, rtl.right());
        assertEquals(2, rtl.top());
        assertEquals(4, rtl.bottom());
        assertEquals(1, d.resolve(TextDirection.ltr).left());
    }

    /// Two directional insets add to a directional one (Flutter's EdgeInsetsGeometry.add);
    /// the inherited absolute add turned start into left, which stayed left under RTL.
    @Test
    void addingDirectionalInsetsStaysDirectional() {
        EdgeInsets sum = EdgeInsetsDirectional.only(10, 0, 0, 0).add(EdgeInsetsDirectional.only(5, 1, 2, 0));
        assertTrue(sum instanceof EdgeInsetsDirectional);
        EdgeInsets rtl = sum.resolve(TextDirection.rtl);
        assertEquals(15, rtl.right());
        assertEquals(2, rtl.left());
        assertEquals(1, rtl.top());
    }

    /// A BorderRadiusDirectional resolves against the direction; ClipRRect and the
    /// uniform-radius readers treated it as no radius at all and drew square corners.
    @Test
    void aDirectionalBorderRadiusResolves() {
        BorderRadiusDirectional d = BorderRadiusDirectional.only(Radius.circular(8), null, null, null);
        assertEquals(8, BorderRadius.of(d, TextDirection.ltr).topLeft().x());
        assertEquals(8, BorderRadius.of(d, TextDirection.rtl).topRight().x());
        assertEquals(8, BorderRadius.of(d, null).topLeft().x(), "null direction reads LTR");
    }
}
