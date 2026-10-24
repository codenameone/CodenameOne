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
package com.codename1.desktopcompat.javax.swing.border;

import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import org.junit.Test;

/// Every side of an etched or bevelled border is painted, whatever the
/// background: on a white panel there is nothing brighter than the
/// background, and the lit sides must not disappear into it.
public class BorderSidesTest extends KernelTestBase {
    private static final int W = 200;
    private static final int H = 120;

    private static int luma(int rgb) {
        return ((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff);
    }

    /// The outermost painted pixel of the left, top, right and bottom side.
    private int[] sides(Border b, Color background) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(true);
        p.setBackground(background);
        p.setBorder(b);
        JFrame f = new JFrame();
        f.getContentPane().add(p, BorderLayout.CENTER);
        f.setSize(W, H);
        show(f);
        int[][] rows = raster(f);
        int w = p.getWidth();
        int h = p.getHeight();
        assertTrue(w > 40 && h > 40);
        int[] out = {pixel(rows, p, 0, h / 2), pixel(rows, p, w / 2, 0), pixel(rows, p, w - 1, h / 2),
            pixel(rows, p, w / 2, h - 1), pixel(rows, p, w / 2, h / 2)};
        f.dispose();
        return out;
    }

    private void allFourShow(String what, Border b, Color background) {
        int[] s = sides(b, background);
        String[] names = {"left", "top", "right", "bottom"};
        for (int i = 0; i < 4; i++) {
            assertTrue(what + ": the " + names[i] + " side is the background",
                    Math.abs(luma(s[i]) - luma(s[4])) >= 24);
        }
    }

    @Test
    public void anEtchedBorderShowsAllFourSides() {
        Color[] grounds = {Color.WHITE, new Color(0xf2f2f7), Color.LIGHT_GRAY, new Color(0x202020)};
        for (int i = 0; i < grounds.length; i++) {
            allFourShow("raised on " + grounds[i], new EtchedBorder(EtchedBorder.RAISED), grounds[i]);
            allFourShow("lowered on " + grounds[i], new EtchedBorder(EtchedBorder.LOWERED), grounds[i]);
        }
    }

    @Test
    public void aBevelShowsAllFourSides() {
        Color[] grounds = {Color.WHITE, new Color(0xf2f2f7), Color.LIGHT_GRAY, new Color(0x202020)};
        for (int i = 0; i < grounds.length; i++) {
            allFourShow("raised on " + grounds[i], new BevelBorder(BevelBorder.RAISED), grounds[i]);
            allFourShow("lowered on " + grounds[i], new BevelBorder(BevelBorder.LOWERED), grounds[i]);
        }
    }

    @Test
    public void aSoftBevelShowsAllFourSides() {
        Color[] grounds = {Color.WHITE, Color.LIGHT_GRAY};
        for (int i = 0; i < grounds.length; i++) {
            allFourShow("raised on " + grounds[i], new SoftBevelBorder(BevelBorder.RAISED), grounds[i]);
            allFourShow("lowered on " + grounds[i], new SoftBevelBorder(BevelBorder.LOWERED), grounds[i]);
        }
    }

    /// Raised is lit from the top left and lowered from the bottom right,
    /// also where the lit sides had to be drawn darker than the background.
    @Test
    public void theLitSidesAreTheLighterOnes() {
        int[] up = sides(new BevelBorder(BevelBorder.RAISED), Color.WHITE);
        assertTrue(luma(up[0]) > luma(up[2]));
        assertTrue(luma(up[1]) > luma(up[3]));
        int[] down = sides(new BevelBorder(BevelBorder.LOWERED), Color.WHITE);
        assertTrue(luma(down[0]) < luma(down[2]));
        assertTrue(luma(down[1]) < luma(down[3]));
    }

    /// Where the background leaves room, the highlight is the brighter
    /// shade the JDK derives.
    @Test
    public void aMidToneBackgroundKeepsTheBrighterHighlight() {
        JPanel p = new JPanel();
        p.setBackground(new Color(120, 120, 120));
        assertTrue(new EtchedBorder().getHighlightColor(p).equals(new Color(120, 120, 120).brighter()));
        assertTrue(new BevelBorder(BevelBorder.RAISED).getHighlightInnerColor(p)
                .equals(new Color(120, 120, 120).brighter()));
    }
}
