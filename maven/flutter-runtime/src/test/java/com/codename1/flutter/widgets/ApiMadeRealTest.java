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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import dart.core.DartList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API that used to be stored and then ignored: each case here drives the behaviour
 * the setter promises, not just the setter.
 */
class ApiMadeRealTest {

    private static RenderElement mountAndLayout(Widget root, BoxConstraints c) {
        RenderHost host = new RenderHost();
        // A host container, so text gets the label it measures and paints with.
        host.container(new Container());
        FlutterUI.mount(root, host, new BuildOwner());
        RenderElement r = host.rootRenderElement();
        r.layout(c);
        return r;
    }

    private static Form show(Widget w, int width, int height) {
        Container root = FlutterUI.wrap(w);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, root);
        f.setX(0);
        f.setY(0);
        f.setWidth(width);
        f.setHeight(height);
        f.layoutContainer();
        f.getContentPane().layoutContainer();
        root.layoutContainer();
        return f;
    }

    @Test
    void singleChildScrollViewControllerDrivesAndReadsThePane() {
        RasterDisplay.install();
        try {
            ScrollController controller = new ScrollController();
            DartList<Widget> rows = new DartList<Widget>();
            for (int i = 0; i < 30; i++) {
                rows.add(new ProbeBox(10, 40));
            }
            Column col = new Column();
            col.children(rows);
            SingleChildScrollView sv = new SingleChildScrollView();
            sv.controller(controller);
            sv.child(col);

            Form f = show(sv, 200, 300);
            Container root = (Container) f.getContentPane().getComponentAt(0);
            Container pane = (Container) root.getComponentAt(0);
            pane.layoutContainer();
            assertTrue(pane.getScrollDimension().getHeight() > pane.getHeight());

            controller.jumpTo(100);
            assertEquals(Dp.px(100), pane.getScrollY(), 1, "jumpTo moves the pane");

            ((ScrollRenderElement.ScrollPane) pane).scrollToPosition((int) Math.round(Dp.px(60)), false);
            assertEquals(60.0, controller.offset(), 1.0, "the controller reads where the pane went");
        } finally {
            RasterDisplay.uninstall();
        }
    }

    @Test
    void textStylePackagePrefixesTheFamilyTheWayFlutterDoes() {
        TextStyle s = new TextStyle();
        s.fontFamily("Brand Sans");
        s.package_("brand_kit");
        assertEquals("packages/brand_kit/Brand Sans", s.fontFamily());
        assertEquals("packages/brand_kit/Brand Sans", s.getFontFamily());
        TextStyle other = s.copyWith(null, null, null, "Plain", null, null, null, null,
                null, null, null, null, null);
        assertEquals("Plain", other.fontFamily(), "a new family without a package is unprefixed");
        TextStyle merged = new TextStyle().merge(s);
        assertEquals("packages/brand_kit/Brand Sans", merged.fontFamily());
    }

    static final class GlyphlessIcon extends Icon {
        GlyphlessIcon() {
            super(null);
        }

        @Override
        public Widget build(BuildContext context) {
            return new ProbeBox(7, 9);
        }
    }

    static final class PlainSubIcon extends Icon {
        PlainSubIcon() {
            super(null);
        }
    }

    @Test
    void iconSubclassBuildIsWhatRenders() {
        RenderElement r = mountAndLayout(new GlyphlessIcon(), BoxConstraints.loose(100, 100));
        assertTrue(r.widget() instanceof ProbeBox, "the subclass's build() output renders");
        assertEquals(7.0, r.size().width(), 0.0001);

        RenderElement plain = mountAndLayout(new PlainSubIcon(), BoxConstraints.loose(100, 100));
        assertTrue(plain instanceof IconRenderElement, "an un-overridden build is the glyph");
    }

    private static Text sized(String s, double size) {
        Text t = new Text(s);
        TextStyle st = new TextStyle();
        st.fontSize(size);
        t.style(st);
        return t;
    }

    private static Text tall(String s, double size, double height) {
        Text t = sized(s, size);
        t.getStyle().height(height);
        return t;
    }

    @Test
    void baselineAlignmentLinesUpTextBaselines() {
        RasterDisplay.install();
        try {
            Row row = new Row();
            row.crossAxisAlignment(CrossAxisAlignment.baseline);
            row.children(DartList.of((Widget) sized("small", 10), tall("BIG", 40, 2)));
            RenderElement r = mountAndLayout(row, BoxConstraints.loose(400, 400));
            RenderElement small = r.renderChildren().get(0);
            RenderElement big = r.renderChildren().get(1);
            double bs = small.distanceToBaseline();
            double bb = big.distanceToBaseline();
            assertTrue(bs > 0 && bb > bs, "both texts have a baseline, the big one lower: " + bs + " " + bb + " " + small.component() + " " + small.size() + " " + small.getClass());
            assertEquals(small.relY() + bs, big.relY() + bb, 1.0, "baselines coincide");
            assertTrue(small.relY() > 0, "the small text is pushed down, not top aligned");
        } finally {
            RasterDisplay.uninstall();
        }
    }

    /** How far right the first painted line of a wrapped Text reaches, and the box width. */
    private static int[] firstLineReach(TextAlign align) {
        Text t = sized("aa bb cc dd ee ff gg hh ii jj kk ll mm nn oo pp qq rr ss tt", 14);
        t.textAlign(align);
        RenderElement r = mountAndLayout(t, BoxConstraints.tight(123, 400));
        Component c = r.component();
        c.setX(0);
        c.setY(0);
        c.setWidth(123);
        c.setHeight(400);
        Image img = Image.createImage(123, 400, 0xffffffff);
        RasterDisplay.DRAWN_TEXT.clear();
        c.paint(img.getGraphics());
        int firstY = Integer.MAX_VALUE;
        for (int[] d : RasterDisplay.DRAWN_TEXT) {
            firstY = Math.min(firstY, d[1]);
        }
        int reach = -1;
        for (int[] d : RasterDisplay.DRAWN_TEXT) {
            if (d[1] == firstY) {
                reach = Math.max(reach, d[0] + d[2]);
            }
        }
        return new int[] {reach, c.getWidth()};
    }

    @Test
    void justifiedLinesReachTheRightEdge() {
        RasterDisplay.install();
        try {
            int[] start = firstLineReach(TextAlign.start);
            int[] justified = firstLineReach(TextAlign.justify);
            assertTrue(start[0] > 0 && start[0] < start[1] - 8, "a ragged first line: " + start[0]);
            assertEquals(justified[1], justified[0], 2, "justify stretches the first line to the edge");
        } finally {
            RasterDisplay.uninstall();
        }
    }
}
