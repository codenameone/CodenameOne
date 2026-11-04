/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.StyleEngine;
import com.codename1.fxcompat.runtime.Units;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/// A label with `wrapText` on breaks its text into lines at its width.
public class WrapTextTest {

    private static final String TEXT = "The quick brown fox jumps over the lazy dog and keeps on running "
            + "far past the end of a narrow card";

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
        StyleEngine.setInstance(null);
    }

    private static Label label(boolean wrap) {
        Label l = new Label(TEXT);
        l.setWrapText(wrap);
        return l;
    }

    @Test
    public void wrappingLabelGrowsAtANarrowWidth() {
        Label l = label(true);
        new Scene(new VBox(l), 400, 400);
        assertSame(Orientation.HORIZONTAL, l.getContentBias());
        double oneLine = l.prefHeight(-1);
        double narrow = l.prefHeight(60);
        assertTrue("narrow " + narrow + " one line " + oneLine, narrow >= oneLine * 3);
        assertEquals(oneLine, l.prefHeight(l.prefWidth(-1) + 10), 0.01);
        assertTrue(l.minWidth(-1) < 60);
    }

    @Test
    public void wrapTextOffIsUnchanged() {
        Label l = label(false);
        new Scene(new VBox(l), 400, 400);
        assertNull(l.getContentBias());
        assertEquals(l.prefHeight(-1), l.prefHeight(60), 0.01);
    }

    @Test
    public void columnOfWrappingLabelsIsTallerWhenNarrow() {
        VBox box = new VBox(label(true), label(true), label(true));
        new Scene(box, 400, 400);
        assertSame(Orientation.HORIZONTAL, box.getContentBias());
        double wide = box.prefHeight(2000);
        double narrow = box.prefHeight(80);
        assertTrue("narrow " + narrow + " wide " + wide, narrow > wide * 2);
    }

    @Test
    public void scrollPaneFitToWidthGivesWrappedHeight() {
        Label l = label(true);
        VBox box = new VBox(l);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        new Scene(sp, 90, 400);
        sp.layout();
        box.layout();
        double one = l.prefHeight(-1);
        assertTrue("label " + l.getHeight() + " one line " + one, l.getHeight() >= one * 3);
        assertTrue(box.getHeight() >= l.getHeight());
    }
}
