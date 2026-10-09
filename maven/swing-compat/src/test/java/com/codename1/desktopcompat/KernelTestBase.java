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
package com.codename1.desktopcompat;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.rt.EventBridge;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.ui.Image;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

/// What the tests that need a display share: the headless Codename One
/// implementation, the event dispatch thread, a scale of two device pixels
/// per logical pixel, and helpers that paint a frame and send it input.
public abstract class KernelTestBase {

    @Rule
    public final MainThreadRule edt = new MainThreadRule();

    private final List<JFrame> shown = new ArrayList<JFrame>();

    @BeforeClass
    public static void boot() {
        HeadlessImplementation.install();
    }

    @Before
    public void scale() {
        Units.setScale(2f);
    }

    @After
    public void cleanUp() {
        for (int i = shown.size() - 1; i >= 0; i--) {
            shown.get(i).dispose();
        }
        HeadlessImplementation.recordText = false;
        HeadlessImplementation.drawnText.clear();
        Units.setScale(0);
        EventBridge.resetClickCount();
    }

    /// Shows the frame and remembers to dispose of it.
    protected JFrame show(JFrame f) {
        shown.add(f);
        f.setVisible(true);
        return f;
    }

    /// Paints the frame's form and answers the strings drawn, each as
    /// `{text, x, y}` in device pixels.
    protected List<Object[]> paint(JFrame f) {
        HeadlessImplementation.drawnText.clear();
        HeadlessImplementation.recordText = true;
        try {
            Image target = Image.createImage(HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
            f.cn1Form().paintComponent(target.getGraphics());
            return new ArrayList<Object[]>(HeadlessImplementation.drawnText);
        } finally {
            HeadlessImplementation.recordText = false;
            HeadlessImplementation.drawnText.clear();
        }
    }

    /// Paints the frame's form into pixels and answers them as rows of
    /// ARGB, in device pixels. Shapes are filled in their color inside the
    /// clip; a string is a solid box of the size it measures.
    protected int[][] raster(JFrame f) {
        boolean clip = HeadlessImplementation.trackClip;
        HeadlessImplementation.rasterImages = true;
        HeadlessImplementation.trackClip = true;
        try {
            Image target = Image.createImage(HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT, 0xffffffff);
            com.codename1.ui.Graphics g = target.getGraphics();
            g.setClip(0, 0, HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
            f.cn1Form().paintComponent(g);
            Object rows = target.getImage();
            return rows instanceof int[][] ? (int[][]) rows : new int[0][0];
        } finally {
            HeadlessImplementation.rasterImages = false;
            HeadlessImplementation.trackClip = clip;
            HeadlessImplementation.resetRaster();
        }
    }

    /// The pixel painted at a logical point of `c`, as RGB.
    protected static int pixel(int[][] rows, Component c, int x, int y) {
        int[] p = onDisplay(c, x, y);
        return rows[p[1]][p[0]] & 0xffffff;
    }

    /// How many pixels inside the logical rectangle of `c` have the color.
    protected static int count(int[][] rows, Component c, int x, int y, int w, int h, int rgb) {
        int[] a = onDisplay(c, x, y);
        int[] b = onDisplay(c, x + w, y + h);
        int n = 0;
        for (int row = Math.max(0, a[1]); row < Math.min(rows.length, b[1]); row++) {
            for (int col = Math.max(0, a[0]); col < Math.min(rows[row].length, b[0]); col++) {
                if ((rows[row][col] & 0xffffff) == (rgb & 0xffffff)) {
                    n++;
                }
            }
        }
        return n;
    }

    protected static Object[] find(List<Object[]> text, String s) {
        for (int i = 0; i < text.size(); i++) {
            if (s.equals(text.get(i)[0])) {
                return text.get(i);
            }
        }
        return null;
    }

    /// The display position of a logical point of `c`.
    protected static int[] onDisplay(Component c, int x, int y) {
        com.codename1.ui.Component p = c.cn1Peer();
        return new int[]{p.getAbsoluteX() + Units.toDevice(x), p.getAbsoluteY() + Units.toDevice(y)};
    }

    protected static void press(JFrame f, Component c, int x, int y) {
        int[] p = onDisplay(c, x, y);
        f.cn1Form().pointerPressed(p[0], p[1]);
    }

    /// Drags the pointer the way the display does: through the overload
    /// of a form that takes arrays, which is the one a real drag arrives
    /// at.
    protected static void drag(JFrame f, Component c, int x, int y) {
        int[] p = onDisplay(c, x, y);
        f.cn1Form().pointerDragged(new int[]{p[0]}, new int[]{p[1]});
    }

    protected static void release(JFrame f, Component c, int x, int y) {
        int[] p = onDisplay(c, x, y);
        f.cn1Form().pointerReleased(p[0], p[1]);
    }
}
