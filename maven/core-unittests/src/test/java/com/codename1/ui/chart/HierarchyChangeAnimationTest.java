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
package com.codename1.ui.chart;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.chart.series.FlameGraphSeries;
import com.codename1.ui.chart.series.TreemapSeries;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// A change to the values of a tree moves a treemap's cells and a flame graph's bars from
/// where they were to where they belong, as every other series animates its data changes.
/// They used to jump straight to the new layout. Each case paints the start, middle and end
/// of the change and requires the middle frame to differ from both. (Only series drawn with
/// rectangles are checked here: the unit test port does not rasterize arcs or lines, so a
/// sunburst's frames read alike whatever it draws. The device filmstrips cover those.)
public class HierarchyChangeAnimationTest extends UITestBase {
    private static final int W = 480;
    private static final int H = 360;
    private static final long T0 = 40000;
    private static final int DURATION = 600;

    /// One chart type: how to build it and how to change its data.
    private abstract static class Case {
        final String name;

        Case(String name) {
            this.name = name;
        }

        abstract void build(Chart c);

        abstract void change();
    }

    private static TreeNode[] leaves = new TreeNode[0];

    private static TreeNode tree() {
        TreeNode root = new TreeNode("All");
        TreeNode a = root.add(new TreeNode("A"));
        TreeNode a1 = a.add(new TreeNode("A1", 40));
        TreeNode a2 = a.add(new TreeNode("A2", 20));
        TreeNode b = root.add(new TreeNode("B"));
        TreeNode b1 = b.add(new TreeNode("B1", 25));
        TreeNode c = root.add(new TreeNode("C", 15));
        leaves = new TreeNode[]{a1, a2, b1, c};
        return root;
    }

    private static void changeTree() {
        leaves[0].setValue(10);
        leaves[3].setValue(60);
    }

    private static List<Case> cases() {
        List<Case> l = new ArrayList<Case>();
        l.add(new Case("treemap") {
            void build(Chart c) {
                c.addSeries(new TreemapSeries("t", tree()));
            }
            void change() {
                changeTree();
            }
        });
        l.add(new Case("flame graph") {
            void build(Chart c) {
                c.addSeries(new FlameGraphSeries("f", tree()));
            }
            void change() {
                changeTree();
            }
        });
        return l;
    }

    private static long frame(Chart c, long t) {
        AnimationTime.setTime(t);
        c.animate();
        Image img = Image.createImage(W, H, 0xffffffff);
        c.paint(img.getGraphics());
        int[] rgb = img.getRGB();
        long h = 1125899906842597L;
        for (int i = 0; i < rgb.length; i++) {
            h = 31 * h + rgb[i];
        }
        return h;
    }

    private static Chart show(Case k) {
        Chart c = new Chart();
        c.setAnimationDuration(DURATION);
        c.setLegendPosition(Chart.LEGEND_NONE);
        c.setTooltipMode(Chart.TOOLTIP_NONE);
        k.build(c);
        Form f = new Form(new BorderLayout());
        f.add(BorderLayout.CENTER, c);
        f.show();
        c.setX(0);
        c.setY(0);
        c.setWidth(W);
        c.setHeight(H);
        return c;
    }

    @FormTest
    public void aChangeToTheTreeAnimates() {
        StringBuilder still = new StringBuilder();
        try {
            for (Case k : cases()) {
                AnimationTime.setTime(T0);
                Chart c = show(k);
                frame(c, T0);
                long settled = frame(c, T0 + DURATION * 3);
                long t1 = T0 + DURATION * 4;
                AnimationTime.setTime(t1);
                k.change();
                long start = frame(c, t1);
                long mid = frame(c, t1 + DURATION / 2);
                long end = frame(c, t1 + DURATION * 3);
                if (end == settled) {
                    still.append(k.name).append(" (the change drew nothing), ");
                } else if (mid == end || mid == start) {
                    still.append(k.name).append(", ");
                }
            }
        } finally {
            AnimationTime.reset();
        }
        assertTrue(still.length() == 0, "these types jump to new data instead of animating: " + still);
    }
}
