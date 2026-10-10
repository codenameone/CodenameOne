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
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Opacity;
import com.codename1.flutter.widgets.Padding;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.widgets.SizedBox;
import com.codename1.flutter.widgets.Wrap;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The shape of the Flutter gallery's category header, layer by layer, so an
 * inflated card height is attributed to the node that caused it rather than
 * eyeballed off a screenshot.
 *
 * <p>The header is a 64lp image in 8lp padding beside a title, inside
 * Row &gt; Expanded &gt; Wrap, inside Material inside a Container — so every
 * layer must report exactly 80lp (64 + 8 + 8). Headless, so Dp scale is 1 and
 * logical pixels are pixels.</p>
 */
class CategoryHeaderShapeTest {

    private static final double IMAGE = 64;
    private static final double PAD = 8;
    private static final double EXPECTED = IMAGE + PAD + PAD;
    private static final double WIDTH = 400;

    private static DartList<Widget> list(Widget... items) {
        DartList<Widget> l = new DartList<Widget>();
        for (Widget w : items) {
            l.add(w);
        }
        return l;
    }

    private static Widget paddedImage() {
        Padding p = new Padding();
        p.padding(EdgeInsets.all(PAD));
        p.child(new ProbeBox(IMAGE, IMAGE));
        return p;
    }

    private static Widget paddedTitle() {
        Padding p = new Padding();
        p.padding(EdgeInsets.only(PAD, 0, 0, 0));
        p.child(new ProbeBox(120, 32));
        return p;
    }

    private static Wrap headerWrap() {
        Wrap w = new Wrap();
        w.crossAxisAlignment(com.codename1.flutter.WrapCrossAlignment.center);
        w.children(list(paddedImage(), paddedTitle()));
        return w;
    }

    private static RenderElement layout(Widget w) {
        RenderHost host = new RenderHost();
        RenderElement e = (RenderElement) FlutterUI.mount(w, host, new BuildOwner());
        e.layout(BoxConstraints.loose(WIDTH, Double.POSITIVE_INFINITY));
        e.position(0, 0);
        return e;
    }

    @Test
    void wrapIsImagePlusPadding() {
        assertEquals(EXPECTED, layout(headerWrap()).size().height(), 0.001,
                "Wrap run height is the tallest child: the padded 64lp image");
    }

    @Test
    void rowOverExpandedWrapDoesNotInflate() {
        Expanded ex = new Expanded();
        ex.child(headerWrap());

        // the collapsed chevron: opacity 0 with no child, contributing nothing
        Opacity chevron = new Opacity();
        chevron.opacity(0);

        Row row = new Row();
        row.children(list(ex, chevron));

        assertEquals(EXPECTED, layout(row).size().height(), 0.001,
                "Expanded stretches on the main axis only; the row is as tall as the wrap");
    }

    @Test
    void sizedBoxWidthOnlyLeavesHeightToTheChild() {
        Expanded ex = new Expanded();
        ex.child(headerWrap());
        Row row = new Row();
        row.children(list(ex));

        SizedBox box = new SizedBox();
        box.width(WIDTH);
        box.child(row);

        assertEquals(EXPECTED, layout(box).size().height(), 0.001,
                "a width-only SizedBox must not tighten or inflate the height");
    }

    @Test
    void materialWrapsTightlyAroundItsChild() {
        Expanded ex = new Expanded();
        ex.child(headerWrap());
        Row row = new Row();
        row.children(list(ex));
        SizedBox box = new SizedBox();
        box.width(WIDTH);
        box.child(row);

        Material m = new Material();
        m.color(new com.codename1.flutter.Color(0xFFFFFFFFL));
        m.child(box);

        assertEquals(EXPECTED, layout(m).size().height(), 0.001,
                "Material is a surface, not padding — it takes its child's size");
    }

    @Test
    void containerWithMarginAddsOnlyTheMargin() {
        Expanded ex = new Expanded();
        ex.child(headerWrap());
        Row row = new Row();
        row.children(list(ex));
        SizedBox box = new SizedBox();
        box.width(WIDTH);
        box.child(row);
        Material m = new Material();
        m.child(box);

        com.codename1.flutter.widgets.Container c = new com.codename1.flutter.widgets.Container();
        c.margin(EdgeInsets.symmetric(32, PAD));
        c.child(m);

        assertEquals(EXPECTED + PAD + PAD, layout(c).size().height(), 0.001,
                "8lp margin above and below the 80lp card");
    }
}
