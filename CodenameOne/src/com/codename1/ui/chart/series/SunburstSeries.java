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
import com.codename1.ui.chart.ChartPoint;
import com.codename1.ui.chart.TreeNode;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.chart.layout.Shapes;

/// A sunburst: the hierarchy as concentric rings, the root in the centre and each level one
/// ring further out, every node's angle proportional to its value. Tap a branch to make it the
/// centre; tap the centre to go back up.
///
/// ```java
/// Chart chart = new Chart();
/// TreeNode root = new TreeNode("All");
/// root.add(new TreeNode("Photos", 40));
/// TreeNode apps = root.add(new TreeNode("Apps"));
/// apps.add(new TreeNode("Games", 15));
/// apps.add(new TreeNode("Tools", 10));
/// root.add(new TreeNode("Music", 20));
/// chart.addSeries(new SunburstSeries("Disk usage", root).setMaxDepth(3));
/// ```
public class SunburstSeries extends HierarchySeries {
    private final com.codename1.ui.geom.GeneralPath sectorPath = new com.codename1.ui.geom.GeneralPath();
    private float cx;
    private float cy;
    private float hole;
    private float outer;

    /// Creates a sunburst.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    ///
    /// - `root`: the tree's root
    public SunburstSeries(String name, TreeNode root) {
        super(name, root);
        setMaxDepth(3);
    }

    @Override
    public void paint(ChartContext ctx) {
        nodes.clear();
        cells.clear();
        getCurrent();
        crumb = null;
        if (current == null) {
            return;
        }
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        float t = growth(ctx);
        cx = ctx.getPlotX() + ctx.getPlotWidth() / 2f;
        cy = ctx.getPlotY() + ctx.getPlotHeight() / 2f;
        float r = Math.min(ctx.getPlotWidth(), ctx.getPlotHeight()) / 2f - ctx.mm(1);
        int levels = Math.max(1, Math.min(getMaxDepth(), depthBelow(current)));
        float ring = r / (levels + 0.6f);
        hole = ring * 0.6f;
        outer = r;
        int alpha = g.getAlpha();
        g.setAlpha((int) (alpha * getVisibilityFraction()));
        paintRing(ctx, current, 0, 360 * t, 1, levels, ring, t >= 1);
        String label = current.getLabel();
        int fh = f.getHeight();
        if (label != null && hole > fh) {
            g.setColor(ctx.getTheme().getTextColor());
            g.setFont(f);
            String a = LabelLayout.ellipsize(f, label, (int) (hole * 1.8f));
            g.drawString(a, Math.round(cx - f.stringWidth(a) / 2f), Math.round(cy - fh));
            String b = LabelLayout.ellipsize(f, formatValue(ctx, current.getValue()), (int) (hole * 1.8f));
            g.setColor(ctx.getTheme().getSecondaryTextColor());
            g.drawString(b, Math.round(cx - f.stringWidth(b) / 2f), Math.round(cy));
        }
        // the hole goes back up even when it is too small for the label; hitTest also takes a
        // tap in a narrow band just outside the rings, so a sunburst too small to have a usable hole can still drill out
        if (current != root) { //NOPMD CompareObjectsWithEquals - identity of the same instance is intended
            crumb = crumbBuf;
            crumb[0] = Math.round(cx - hole);
            crumb[1] = Math.round(cy - hole);
            crumb[2] = Math.round(hole * 2);
            crumb[3] = Math.round(hole * 2);
        }
        g.setAlpha(alpha);
    }

    @Override
    protected boolean crumbContains(float ax, float ay) {
        // the hole is a circle: the corners of its bounding square are on the first ring
        if (!super.crumbContains(ax, ay)) {
            return false;
        }
        float r = crumb[2] / 2f;
        float dx = ax - (crumb[0] + r);
        float dy = ay - (crumb[1] + r);
        return dx * dx + dy * dy <= r * r;
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        ChartPoint p = super.hitTest(ctx, x, y, radius);
        if (p != null || crumb == null) {
            return p;
        }
        float dx = x + ctx.getOriginX() - cx;
        float dy = y + ctx.getOriginY() - cy;
        // only a narrow band just past the outer ring goes up: farther out is the title, the
        // legend or another row, and a tap or hover there must not drill out or show a tooltip
        float d2 = dx * dx + dy * dy;
        float band = outer + Math.max(radius, ctx.mm(4));
        if (d2 > outer * outer && d2 <= band * band) {
            TreeNode up = current.getParent();
            return new ChartPoint(this, -1, 0, current.getValue(), x, y, up == null ? null : up.getLabel());
        }
        return null;
    }

    private static int depthBelow(TreeNode n) {
        int d = 0;
        for (int i = 0; i < n.getChildCount(); i++) {
            d = Math.max(d, depthBelow(n.getChild(i)));
        }
        return n.getChildCount() == 0 ? 0 : d + 1;
    }

    // a branch whose children overflowed the sum counts as the largest finite value
    private static double finiteShare(double v) {
        if (!(v > 0)) { //NOPMD LogicInversion - also true for NaN
            return 0;
        }
        return Double.isInfinite(v) ? Double.MAX_VALUE : v;
    }

    private void paintRing(ChartContext ctx, TreeNode parent, double start, double sweep, int level, int levels, float ring, boolean labels) {
        if (level > levels) {
            return;
        }
        // the children's shares are scaled by the largest, as a pie scales its sweeps: values
        // near Double.MAX_VALUE sum to infinity, which would leave every share 0
        int count = parent.getChildCount();
        double max = 0;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, finiteShare(parent.getChild(i).getValue()));
        }
        if (!(max > 0)) { //NOPMD LogicInversion - also true for NaN
            return;
        }
        double total = 0;
        for (int i = 0; i < count; i++) {
            total += finiteShare(parent.getChild(i).getValue()) / max;
        }
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        float inner = hole + ring * (level - 1);
        float outer = inner + ring - Math.max(1, ctx.mm(0.3f));
        double a = start;
        for (int i = 0; i < count; i++) {
            TreeNode c = parent.getChild(i);
            // divide first: sweep * value overflows for values near Double.MAX_VALUE, and an
            // infinite or NaN sweep would hand the sector shape a NaN angle
            double s = sweep * (finiteShare(c.getValue()) / max / total);
            if (!(s > 0) || Double.isInfinite(s)) { //NOPMD LogicInversion - also rejects NaN
                continue;
            }
            int color = colorOf(ctx, c);
            g.setColor(color);
            double pad = s > 1 ? 0.4 : 0;
            g.fillShape(Shapes.sector(sectorPath, cx, cy, outer, inner, a + pad, s - pad * 2));
            float[] cell = nextCell(4);
            cell[0] = inner;
            cell[1] = outer;
            cell[2] = (float) a;
            cell[3] = (float) s;
            record(c, cell);
            if (labels) {
                double mid = a + s / 2;
                float mr = (inner + outer) / 2;
                double arc = Math.toRadians(s) * mr;
                String txt = LabelLayout.ellipsize(f, c.getLabel(), (int) Math.min(arc - ctx.mm(1), ring * 1.6f));
                int w = f.stringWidth(txt);
                if (txt.length() > 0 && !"...".equals(txt) && arc > w + ctx.mm(1) && ring > fh) {
                    int x = Math.round(Shapes.px(cx, mr, mid) - w / 2f);
                    int y = Math.round(Shapes.py(cy, mr, mid) - fh / 2f);
                    if (ctx.getLabels().placeAt(x, y, w, fh)) {
                        g.setColor(BarSeries.contrast(color));
                        g.drawString(txt, x, y);
                    }
                }
            }
            paintRing(ctx, c, a, s, level + 1, levels, ring, labels);
            a += s;
        }
    }

    @Override
    protected boolean contains(float[] geo, float x, float y) {
        float dx = x - cx;
        float dy = y - cy;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < geo[0] || d > geo[1]) {
            return false;
        }
        double deg = Math.toDegrees(com.codename1.util.MathUtil.atan2(dx, -dy));
        double rel = deg - geo[2];
        rel = rel - 360 * Math.floor(rel / 360);
        return rel <= geo[3];
    }

    @Override
    protected float[] anchor(float[] geo) {
        double mid = geo[2] + geo[3] / 2;
        float mr = (geo[0] + geo[1]) / 2;
        return new float[]{Shapes.px(cx, mr, mid), Shapes.py(cy, mr, mid)};
    }
}
