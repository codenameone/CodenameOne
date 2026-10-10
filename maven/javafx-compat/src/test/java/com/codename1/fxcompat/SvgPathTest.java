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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.SvgPath;
import com.codename1.fxcompat.runtime.Units;

import javafx.geometry.Bounds;
import javafx.scene.shape.SVGPath;

/// The SVG path data parser, checked against segments worked out by hand.
public class SvgPathTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
    }

    private static String number(double v) {
        long thousandths = Math.round(v * 1000);
        if (thousandths % 1000 == 0) {
            return Long.toString(thousandths / 1000);
        }
        return Double.toString(thousandths / 1000.0);
    }

    /// Writes a path as `M x y L x y ...`, rounded to thousandths.
    static String dump(FxPath path) {
        StringBuilder out = new StringBuilder();
        double[] pts = path.points();
        int p = 0;
        for (int i = 0; i < path.commandCount(); i++) {
            int n;
            switch (path.command(i)) {
                case FxPath.MOVE:
                    out.append('M');
                    n = 2;
                    break;
                case FxPath.LINE:
                    out.append('L');
                    n = 2;
                    break;
                case FxPath.QUAD:
                    out.append('Q');
                    n = 4;
                    break;
                case FxPath.CUBIC:
                    out.append('C');
                    n = 6;
                    break;
                default:
                    out.append('Z');
                    n = 0;
                    break;
            }
            for (int k = 0; k < n; k++) {
                out.append(' ').append(number(pts[p++]));
            }
            if (i < path.commandCount() - 1) {
                out.append(' ');
            }
        }
        return out.toString();
    }

    private static String parse(String data) {
        FxPath path = new FxPath();
        assertTrue(data, SvgPath.append(data, path));
        return dump(path);
    }

    private static String prefix(String data) {
        FxPath path = new FxPath();
        assertFalse(data, SvgPath.append(data, path));
        return dump(path);
    }

    @Test
    public void absoluteLinesAndClose() {
        assertEquals("M 10 20 L 30 40 L 50 40 L 50 60 Z", parse("M10 20 L30 40 H50 V60 Z"));
        assertEquals("M 0 0 L 10 0", parse("  M 0,0\n\tL 10 , 0  "));
    }

    @Test
    public void relativeLines() {
        assertEquals("M 10 10 L 15 10 L 20 10 L 20 15 Z", parse("m10 10 l5 0 h5 v5 z"));
        // A relative command after a close starts from the start point.
        assertEquals("M 10 10 L 20 10 Z M 10 10 L 11 11", parse("M10 10 h10 z l1 1"));
    }

    @Test
    public void coordinatesRepeatTheCommand() {
        assertEquals("M 0 0 L 10 0 L 10 10", parse("M0 0 10 0 10 10"));
        assertEquals("M 1 1 L 3 3 L 6 6", parse("m1 1 2 2 3 3"));
        assertEquals("M 0 0 L 1 0 L 2 0 L 2 5 L 2 7", parse("M0 0 H1 2 V5 7"));
        assertEquals("M 0 0 Q 1 1 2 0 Q 3 1 4 0", parse("M0 0 Q1 1 2 0 3 1 4 0"));
    }

    @Test
    public void compactNumbers() {
        assertEquals("M 1 -2 L 0.5 0.5", parse("M1-2L.5.5"));
        assertEquals("M -1 -2 L 3 4", parse("M-1-2L+3+4"));
        assertEquals("M 10 0.2 L 15 0", parse("M1e1 2E-1 L1.5e+1 0"));
        assertEquals("M 0 0 L 0.5 0.25 L 0.75 2", parse("M0 0L.5.25.75 2."));
    }

    @Test
    public void cubicsAndTheirSmoothForm() {
        assertEquals("M 0 0 C 0 10 10 10 10 0 C 10 -10 20 -10 20 0", parse("M0 0 C0 10 10 10 10 0 S20 -10 20 0"));
        // Without a cubic before it the first control point is the current point.
        assertEquals("M 0 0 C 0 0 5 5 10 0", parse("M0 0 S5 5 10 0"));
        assertEquals("M 10 10 C 10 15 15 15 15 10 C 15 5 20 5 20 10", parse("M10 10 c0 5 5 5 5 0 s5 -5 5 0"));
    }

    @Test
    public void quadraticsAndTheirSmoothForm() {
        assertEquals("M 0 0 Q 5 10 10 0 Q 15 -10 20 0 Q 25 10 30 0", parse("M0 0 Q5 10 10 0 T20 0 T30 0"));
        assertEquals("M 0 0 Q 0 0 10 0", parse("M0 0 T10 0"));
        assertEquals("M 10 10 Q 15 15 20 10 Q 25 5 30 10", parse("M10 10 q5 5 10 0 t10 0"));
        // A line in between forgets the control point.
        assertEquals("M 0 0 Q 5 10 10 0 L 20 0 Q 20 0 30 0", parse("M0 0 Q5 10 10 0 L20 0 T30 0"));
    }

    private static FxPath path(String data) {
        FxPath path = new FxPath();
        assertTrue(data, SvgPath.append(data, path));
        return path;
    }

    @Test
    public void arcsEndWhereTheySayAndBendTheRightWay() {
        // Half a circle of radius 5; the flags and the x run together.
        FxPath up = path("M0 0 A5 5 0 0110 0");
        assertEquals(10, up.currentX(), 0);
        assertEquals(0, up.currentY(), 0);
        double[] b = up.bounds();
        assertEquals(0, b[0], 0.05);
        assertEquals(-5, b[1], 0.05);
        assertEquals(10, b[2], 0.05);
        assertEquals(0, b[3], 0.05);

        FxPath down = path("M0 0 A5 5 0 0010 0");
        b = down.bounds();
        assertEquals(0, b[1], 0.05);
        assertEquals(5, b[3], 0.05);
        assertEquals(FxPath.CUBIC, down.command(1));
    }

    @Test
    public void largeArcFlagPicksTheLongWayRound() {
        FxPath small = path("M10 0 A10 10 0 0 1 20 10");
        double[] b = small.bounds();
        assertEquals(10, b[2] - b[0], 0.05);
        assertEquals(10, b[3] - b[1], 0.05);
        FxPath large = path("M10 0 A10 10 0 1 1 20 10");
        b = large.bounds();
        assertEquals(20, b[2] - b[0], 0.05);
        assertEquals(20, b[3] - b[1], 0.05);
        assertEquals(20, large.currentX(), 0);
        assertEquals(10, large.currentY(), 0);
    }

    @Test
    public void relativeArcAndDegenerateArcs() {
        FxPath relative = path("M5 5 a5 5 0 0 1 10 0");
        assertEquals(15, relative.currentX(), 0);
        assertEquals(5, relative.currentY(), 0);
        // A zero radius is a line.
        assertEquals("M 0 0 L 10 0", parse("M0 0 A0 5 0 0 1 10 0"));
        // Arriving where it started draws nothing.
        assertEquals("M 3 3", parse("M3 3 A5 5 0 0 1 3 3"));
        // Radii too small to reach are scaled up until they do.
        double[] b = path("M0 0 A1 1 0 0 1 10 0").bounds();
        assertEquals(-5, b[1], 0.05);
        assertEquals(10, b[2], 0.05);
    }

    @Test
    public void rotatedEllipticalArc() {
        // An ellipse 10 by 5 turned a quarter: its long axis is vertical.
        FxPath p = path("M0 0 A10 5 90 0 1 10 0");
        assertEquals(10, p.currentX(), 0);
        assertEquals(0, p.currentY(), 0);
        double[] b = p.bounds();
        assertEquals(-10, b[1], 0.05);
        assertEquals(0, b[3], 0.05);
    }

    @Test
    public void malformedDataKeepsWhatCameBefore() {
        assertEquals("M 0 0 L 10 0", prefix("M0 0 L10 0 L10 x"));
        assertEquals("", prefix("L10 10"));
        assertEquals("", prefix("10 10"));
        assertEquals("M 0 0 L 5 5 Z", prefix("M0 0 L5 5 Z 5 5"));
        assertEquals("M 0 0", prefix("M0 0 A5 5 0 2 1 10 0"));
        assertEquals("M 0 0 L 1 1", prefix("M0 0 L1 1 X 5 5 L9 9"));
        assertEquals("M 0 0", prefix("M0 0 C1 1 2 2 3"));
        assertEquals("", parse(""));
        FxPath none = new FxPath();
        assertTrue(SvgPath.append(null, none));
        assertTrue(none.isEmpty());
    }

    @Test
    public void dataMayContinueAPathThatIsUnderWay() {
        FxPath p = new FxPath();
        p.moveTo(1, 1);
        assertTrue(SvgPath.append("l2 0 V5", p));
        assertEquals("M 1 1 L 3 1 L 3 5", dump(p));
    }

    @Test
    public void svgPathNodeUsesTheData() {
        SVGPath node = new SVGPath();
        assertTrue(node.getLayoutBounds().isEmpty());
        node.setContent("M0 0 L10 0 L10 20 Z");
        Bounds b = node.getLayoutBounds();
        assertEquals(0, b.getMinX(), 0);
        assertEquals(10, b.getWidth(), 0);
        assertEquals(20, b.getHeight(), 0);
        assertTrue(node.contains(8, 4));
        assertFalse(node.contains(2, 15));
        // Broken data shows what came before the error.
        node.setContent("M0 0 L4 0 L4 4 oops");
        assertEquals(4, node.getLayoutBounds().getWidth(), 0);
        assertEquals(4, node.getLayoutBounds().getHeight(), 0);
    }
}
