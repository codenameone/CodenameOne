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

    protected static void drag(JFrame f, Component c, int x, int y) {
        int[] p = onDisplay(c, x, y);
        f.cn1Form().pointerDragged(p[0], p[1]);
    }

    protected static void release(JFrame f, Component c, int x, int y) {
        int[] p = onDisplay(c, x, y);
        f.cn1Form().pointerReleased(p[0], p[1]);
    }
}
