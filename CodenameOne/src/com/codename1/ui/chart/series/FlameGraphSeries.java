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

/// A flame graph: each node is a bar as wide as its share of its parent, and its children sit
/// on top of it, so the widest stacks are where the time (or memory, or any other total) goes.
/// The root is the bottom row. With `setInverted(true)` the root is the top row and the
/// children hang below it, which is usually called an icicle chart.
///
/// Tapping a bar zooms into it: that node becomes the bottom row and spans the full width.
/// Tapping the breadcrumb goes back up. A branch is as wide as the sum of its children, so
/// time a function spends in its own code goes in a leaf child of its own, conventionally
/// named "self". Bars too narrow for their text show none, and the tooltip always shows the
/// name and value.
///
/// Bars take the node's own color when it has one. Otherwise they get warm colors, picked from
/// the node's name so the same function keeps its color between captures; `setWarmColors(false)`
/// uses the theme's palette by top level branch instead, as a treemap does.
///
/// ```java
/// TreeNode main = new TreeNode("main");
/// TreeNode load = main.add(new TreeNode("load"));
/// load.add(new TreeNode("parse", 30));
/// load.add(new TreeNode("decode", 20));
/// TreeNode render = main.add(new TreeNode("render"));
/// render.add(new TreeNode("layout", 25));
/// render.add(new TreeNode("paint", 15));
/// Chart chart = new Chart();
/// chart.addSeries(new FlameGraphSeries("CPU", main));
/// ```
///
/// ![A flame graph](https://www.codenameone.com/developer-guide/img/generated/chart-flame-graph.png)
/// ![Inverted, as an icicle chart](https://www.codenameone.com/developer-guide/img/generated/chart-icicle.png)
public class FlameGraphSeries extends HierarchySeries {
    private boolean inverted;
    private boolean warmColors = true;

    /// Creates a flame graph.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `root`: the tree's root, drawn as the bottom row
    public FlameGraphSeries(String name, TreeNode root) {
        super(name, root);
        setMaxDepth(64);
    }

    /// Draws the root at the top and the children below it (an icicle chart) instead of the
    /// root at the bottom.
    ///
    /// #### Parameters
    ///
    /// - `inverted`: true for the root at the top
    ///
    /// #### Returns
    ///
    /// this series
    public FlameGraphSeries setInverted(boolean inverted) {
        this.inverted = inverted;
        return this;
    }

    /// True when the root is drawn at the top.
    ///
    /// #### Returns
    ///
    /// true for an icicle chart
    public boolean isInverted() {
        return inverted;
    }

    /// Chooses between warm colors picked from each node's name, the default, and the theme's
    /// palette by top level branch.
    ///
    /// #### Parameters
    ///
    /// - `warm`: false for the theme's palette
    ///
    /// #### Returns
    ///
    /// this series
    public FlameGraphSeries setWarmColors(boolean warm) {
        this.warmColors = warm;
        return this;
    }

    /// True when bars get warm colors picked from their names.
    ///
    /// #### Returns
    ///
    /// true by default
    public boolean isWarmColors() {
        return warmColors;
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
        Font f = ctx.getFont();
        int fh = f.getHeight();
        int pad = ctx.mm(0.6f);
        float rowH = fh + pad * 2;
        float x = ctx.getPlotX();
        float w = ctx.getPlotWidth();
        float areaTop = ctx.getPlotY() + top;
        float areaH = ctx.getPlotHeight() - top;
        int rows = Math.max(1, (int) (areaH / rowH));
        if (rows * rowH > areaH) {
            rowH = areaH / rows;
        }
        // a shallow tree would leave most of a tall plot empty above it: its rows grow to fill
        // the area, up to twice the height their text needs, so the bars stay bars
        int depth = 1 + maxDepthUnder(current, Math.min(rows, getMaxDepth()) - 1);
        if (depth < rows) {
            rowH = Math.min(areaH / depth, rowH * 2);
            rows = Math.max(1, (int) (areaH / rowH));
        }
        Graphics g = ctx.getGraphics();
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        int[] clip = g.getClip();
        g.clipRect(Math.round(x), Math.round(areaTop), Math.round(w), Math.round(areaH));
        // entering, the stacks rise row by row from the root: each row's bars grow in height
        // once the rows under it are up
        int stack = t >= 1 ? rows : Math.min(rows, 1 + maxDepthUnder(current, Math.min(rows, getMaxDepth()) - 1));
        paintNode(ctx, current, x, w, 0, rows, stack, rowH, areaTop, areaH, t, t >= 1);
        g.setClip(clip);
        g.setAlpha(alpha);
    }

    private void paintNode(ChartContext ctx, TreeNode n, float x, float w, int depth, int rows, int stack, float rowH,
                           float areaTop, float areaH, float t, boolean labels) {
        if (depth >= rows || depth >= getMaxDepth() || w < 1) {
            return;
        }
        float grown = rowGrowth(t, depth, stack);
        if (grown <= 0) {
            return;
        }
        float h = (rowH - 1) * grown;
        float y = inverted ? areaTop + depth * rowH : areaTop + areaH - (depth + 1) * rowH + (rowH - 1 - h);
        Graphics g = ctx.getGraphics();
        int color = flameColor(ctx, n);
        float[] cell = nextCell(4);
        cell[0] = x;
        cell[1] = y;
        cell[2] = w;
        cell[3] = h;
        // a data change slides and stretches every bar from where it was
        tween(ctx, n, cell);
        int ix = Math.round(cell[0]);
        int iw = Math.max(1, Math.round(cell[0] + cell[2]) - ix - 1);
        y = cell[1];
        h = cell[3];
        cell[0] = ix;
        cell[2] = iw;
        g.setColor(color);
        g.fillRect(ix, Math.round(y), iw, Math.max(1, Math.round(h)));
        record(n, cell);
        Font f = ctx.getFont();
        int pad = ctx.mm(0.6f);
        if (labels && iw > pad * 2 + f.getHeight()) {
            String name = LabelLayout.ellipsize(f, n.getLabel(), iw - pad * 2);
            if (name.length() > 0 && !"...".equals(name)) {
                g.setColor(BarSeries.contrast(color));
                g.setFont(f);
                g.drawString(name, ix + pad, Math.round(y + (h - f.getHeight()) / 2));
            }
        }
        int count = n.getChildCount();
        double total = n.getValue();
        if (count == 0 || !(total > 0) || Double.isInfinite(total)) { //NOPMD LogicInversion - also true for a NaN total
            return;
        }
        float cx = x;
        for (int i = 0; i < count; i++) {
            TreeNode c = n.getChild(i);
            float cw = (float) (w * usable(c.getValue()) / total);
            paintNode(ctx, c, cx, cw, depth + 1, rows, stack, rowH, areaTop, areaH, t, labels);
            cx += cw;
        }
    }

    private static double usable(double v) {
        return v > 0 && !Double.isInfinite(v) ? v : 0;
    }

    // how many rows the tallest stack above n reaches, capped at limit
    private static int maxDepthUnder(TreeNode n, int limit) {
        if (limit <= 0 || n.getChildCount() == 0) {
            return 0;
        }
        int best = 0;
        for (int i = 0; i < n.getChildCount() && best < limit; i++) {
            best = Math.max(best, 1 + maxDepthUnder(n.getChild(i), limit - 1));
        }
        return best;
    }

    // the share of row `depth` drawn at progress t when `stack` rows rise one after another
    private static float rowGrowth(float t, int depth, int stack) {
        if (t >= 1) {
            return 1;
        }
        float per = 1f / Math.max(1, stack);
        float r = (t - depth * per) / per;
        return r <= 0 ? 0 : r >= 1 ? 1 : r;
    }

    private int flameColor(ChartContext ctx, TreeNode n) {
        if (!warmColors || n.getColor() != null) {
            return colorOf(ctx, n);
        }
        // a stable hash of the name, so a node keeps its color from one capture to the next
        String s = n.getLabel();
        int h = 0;
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                h = h * 31 + s.charAt(i);
            }
        }
        h = h & 0x7fffffff;
        // red 205..255, green 70..200, blue 30..80: the classic oranges, reds and yellows
        int r = 205 + h % 51;
        int gr = 70 + (h / 51) % 131;
        int b = 30 + (h / 6681) % 51;
        return (r << 16) | (gr << 8) | b;
    }

    @Override
    protected float[] collapsed(float[] r) {
        return new float[]{r[0] + r[2] / 2, r[1], 0, r[3]};
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
