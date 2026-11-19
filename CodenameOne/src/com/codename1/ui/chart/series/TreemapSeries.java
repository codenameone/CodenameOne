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
package com.codename1.ui.chart.series;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.chart.ChartContext;
import com.codename1.ui.chart.TreeNode;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.chart.layout.Squarify;

/// A squarified treemap: every node is a rectangle whose area is proportional to its value,
/// nested inside its parent. Branches large enough get a header with their name; leaves show
/// their name and value when the text fits, and the tooltip always does.
///
/// ```java
/// chart.addSeries(new TreemapSeries("Portfolio", root));
/// ```
public class TreemapSeries extends HierarchySeries {
    /// Creates a treemap.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `root`: the tree's root
    public TreemapSeries(String name, TreeNode root) {
        super(name, root);
    }

    @Override
    public void paint(ChartContext ctx) {
        nodes.clear();
        cells.clear();
        getCurrent();
        if (current == null) {
            return;
        }
        int top = paintBreadcrumb(ctx);
        float t = growth(ctx);
        float x = ctx.getPlotX();
        float y = ctx.getPlotY() + top;
        float w = ctx.getPlotWidth();
        float h = ctx.getPlotHeight() - top;
        // grow from the centre while entering or zooming
        x += w * (1 - t) / 2;
        y += h * (1 - t) / 2;
        w *= t;
        h *= t;
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        paintChildren(ctx, current, x, y, w, h, 1, t >= 1);
        g.setAlpha(alpha);
    }

    private void paintChildren(ChartContext ctx, TreeNode parent, float x, float y, float w, float h, int depth, boolean labels) {
        int n = parent.getChildCount();
        if (n == 0) {
            return;
        }
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            // a branch whose children overflowed the sum is infinite: it counts as the largest
            // finite value so it keeps its place, where Squarify would count it as nothing
            double cv = parent.getChild(i).getValue();
            v[i] = cv == Double.POSITIVE_INFINITY ? Double.MAX_VALUE : cv;
        }
        float[][] rects = Squarify.layout(v, x, y, w, h);
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        int gap = Math.max(1, ctx.mm(0.3f));
        int pad = ctx.mm(0.8f);
        for (int i = 0; i < n; i++) {
            TreeNode c = parent.getChild(i);
            float[] r = rects[i];
            float rx = r[0] + gap / 2f;
            float ry = r[1] + gap / 2f;
            float rw = r[2] - gap;
            float rh = r[3] - gap;
            if (rw < 1 || rh < 1) {
                continue;
            }
            int color = colorOf(ctx, c);
            g.setColor(color);
            g.fillRect(Math.round(rx), Math.round(ry), Math.round(rw), Math.round(rh));
            record(c, new float[]{rx, ry, rw, rh});
            boolean nest = c.getChildCount() > 0 && depth < getMaxDepth() && rw > fh * 4 && rh > fh * 3;
            if (labels && rw > pad * 2 + fh && rh > fh + pad) {
                g.setColor(BarSeries.contrast(color));
                g.setFont(f);
                String name = LabelLayout.ellipsize(f, c.getLabel(), (int) (rw - pad * 2));
                if (name.length() > 0 && !"...".equals(name)) {
                    g.drawString(name, Math.round(rx + pad), Math.round(ry + pad / 2f));
                    if (!nest && rh > fh * 2 + pad) {
                        String val = LabelLayout.ellipsize(f, formatValue(ctx, c.getValue()), (int) (rw - pad * 2));
                        g.drawString(val, Math.round(rx + pad), Math.round(ry + pad / 2f + fh));
                    }
                }
            }
            if (nest) {
                float head = fh + pad;
                paintChildren(ctx, c, rx + pad / 2f, ry + head, rw - pad, rh - head - pad / 2f, depth + 1, labels);
            }
        }
    }

    @Override
    protected boolean contains(float[] r, float x, float y) {
        return x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3];
    }

    @Override
    protected float[] anchor(float[] r) {
        return new float[]{r[0] + r[2] / 2, r[1] + r[3] / 2};
    }
}
