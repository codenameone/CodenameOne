/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.animations.CupertinoPageTransition;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CupertinoPageTransition}'s back-mode paint order.
 *
 * <p>Lives in this package rather than alongside its subject
 * ({@code com.codename1.ui.animations}) because verifying draw order needs to override
 * {@link Image#drawImage(Graphics, Object, int, int)}, which is {@code protected} and
 * only reachable from {@code com.codename1.ui} itself or a subclass -- the same reason
 * {@link OffscreenFormPaintTest} lives here instead of beside the screenshot suite it
 * documents.</p>
 */
public class CupertinoPageTransitionTest extends UITestBase {

    /// In back (pop) mode the departing page -- {@code sourceBuffer} -- is the one on
    /// top, leaving across the whole screen; the page underneath it, coming home, is
    /// {@code destBuffer}. paint() must therefore draw destBuffer first and sourceBuffer
    /// last in back mode, or the revealed page covers the one that is supposed to be
    /// leaving on top of it.
    ///
    /// Pixel-level verification is not possible in this harness:
    /// {@code TestCodenameOneImplementation.drawImage(Object, Object, int, int)} is a
    /// documented no-op, so nothing a {@code Graphics.drawImage} call paints is ever
    /// readable back from the destination buffer. Recording which of the two page
    /// buffers draws first is the narrowest substitute that still exercises the real
    /// {@code paint()} method rather than re-deriving its arithmetic. A small Image
    /// subclass records it: a Mockito spy did the same, but building the spies took
    /// seconds on a cold JVM and pushed the test past FormTest's 5s EDT timeout.
    @FormTest
    public void backModePaintsTheDepartingSourcePageOnTop() throws Exception {
        int w = 300;
        int h = 200;

        Form source = solidPage(w, h);
        Form destination = solidPage(w, h);

        CupertinoPageTransition forward = CupertinoPageTransition.create(500);
        CupertinoPageTransition back = (CupertinoPageTransition) forward.copy(true);
        assertTrue(back.isBack(), "copy(true) must produce a back-mode instance");

        back.init(source, destination);
        back.initTransition();

        // Freeze both curves at t=0, where every cubic-bezier-style motion this class
        // uses starts exactly at its source value: deterministic, so the test does not
        // depend on real elapsed time between initTransition() and paint().
        freezeMotion(back, "arriving", 0);
        freezeMotion(back, "departing", 0);

        Field sourceBufferField = field(back, "sourceBuffer");
        Field destBufferField = field(back, "destBuffer");
        List<String> drawn = new ArrayList<String>();
        sourceBufferField.set(back, new RecordingImage((Image) sourceBufferField.get(back), "source", drawn));
        destBufferField.set(back, new RecordingImage((Image) destBufferField.get(back), "dest", drawn));

        Image frame = Image.createImage(w, h, 0xffffffff);
        Graphics g = frame.getGraphics();
        back.paint(g);

        assertTrue(drawn.contains("source") && drawn.contains("dest"), "both pages must be painted: " + drawn);
        assertEquals("source", drawn.get(drawn.size() - 1),
                "the departing source page must be painted last, on top: " + drawn);
        assertTrue(drawn.indexOf("dest") < drawn.lastIndexOf("source"), "dest before source: " + drawn);
    }

    /// Wraps a page buffer's native image and records each draw of it, in order.
    private static final class RecordingImage extends Image {
        private final String name;
        private final List<String> log;

        RecordingImage(Image wrapped, String name, List<String> log) {
            super(wrapped.getImage());
            this.name = name;
            this.log = log;
        }

        @Override
        protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
            log.add(name);
            super.drawImage(g, nativeGraphics, x, y);
        }
    }

    /// A Form whose content pane is entirely covered by one full-bleed child, matching
    /// the real shape of a transition's source/destination (a page, not a bare
    /// top-level component). revalidate() (not a manual layoutContainer() call) is what
    /// gets the child its bounds here -- see {@link Image#createImage(int, int)}'s own
    /// documented off-screen-paint recipe.
    private static Form solidPage(final int w, final int h) {
        Form form = new Form();
        form.setWidth(w);
        form.setHeight(h);
        form.getContentPane().setWidth(w);
        form.getContentPane().setHeight(h);
        form.getContentPane().setLayout(new LayeredLayout());

        Container fill = new Container();
        fill.setPreferredSize(new Dimension(w, h));
        Style style = fill.getAllStyles();
        style.setBgColor(0x123456);
        style.setBgTransparency(255);
        style.setPadding(0, 0, 0, 0);
        style.setMargin(0, 0, 0, 0);
        style.setBorder(Border.createEmpty());

        form.add(fill);
        form.revalidate();
        form.setVisible(true);
        return form;
    }

    private static void freezeMotion(CupertinoPageTransition t, String fieldName, long time) throws Exception {
        Field f = field(t, fieldName);
        Motion m = (Motion) f.get(t);
        m.setCurrentMotionTime(time);
    }

    private static Field field(Object t, String fieldName) throws Exception {
        Field f = t.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f;
    }
}
