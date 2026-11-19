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
import com.codename1.ui.chart.Series;
import com.codename1.ui.chart.layout.LabelLayout;
import com.codename1.ui.chart.layout.SankeyLayout;
import com.codename1.ui.chart.layout.Shapes;

import java.util.ArrayList;

/// A sankey diagram: flows between stages drawn as bands whose thickness is the flow's value.
///
/// Nodes are created by name as links mention them.
///
/// ```java
/// SankeySeries s = new SankeySeries("Energy");
/// s.addLink("Solar", "Grid", 40).addLink("Gas", "Grid", 60).addLink("Grid", "Homes", 55).addLink("Grid", "Industry", 45);
/// ```
public class SankeySeries extends Series {
    private final ArrayList<String> nodeNames = new ArrayList<String>();
    private final ArrayList<int[]> links = new ArrayList<int[]>();
    private final ArrayList<double[]> linkValues = new ArrayList<double[]>();
    private float linkAlpha = 0.35f;
    private SankeyLayout last;
    private float nodeWidth;
    private boolean entering;
    // the cached layout and the inputs it was built from
    private boolean layoutDirty = true;
    private int[] cacheSrc;
    private int[] cacheDst;
    private int cacheX;
    private int cacheY;
    private int cacheW;
    private int cacheH;
    private int cacheNodeW;
    private int cacheFh;
    private int cacheMaxLabel;

    /// Creates an empty diagram.
    ///
    /// #### Parameters
    ///
    /// - `name`: the series name
    public SankeySeries(String name) {
        super(name);
    }

    @Override
    public boolean isCartesian() {
        return false;
    }

    /// Adds a flow, creating its nodes if needed.
    ///
    /// #### Parameters
    ///
    /// - `source`: the source node's name
    ///
    /// - `target`: the target node's name
    ///
    /// - `value`: the amount that flows
    ///
    /// #### Returns
    ///
    /// this series
    public SankeySeries addLink(String source, String target, double value) {
        if (getChart() != null) {
            getChart().beginChange();
        }
        links.add(new int[]{node(source), node(target)});
        linkValues.add(new double[]{value});
        dataChanged();
        return this;
    }

    /// Removes all nodes and links.
    ///
    /// #### Returns
    ///
    /// this series
    public SankeySeries clear() {
        if (getChart() != null) {
            getChart().beginChange();
        }
        nodeNames.clear();
        links.clear();
        linkValues.clear();
        dataChanged();
        return this;
    }

    /// The opacity of the link bands.
    ///
    /// #### Parameters
    ///
    /// - `a`: 0 to 1
    ///
    /// #### Returns
    ///
    /// this series
    public SankeySeries setLinkAlpha(float a) {
        linkAlpha = a;
        repaintChart();
        return this;
    }

    private int node(String name) {
        int i = nodeNames.indexOf(name);
        if (i < 0) {
            nodeNames.add(name);
            i = nodeNames.size() - 1;
        }
        return i;
    }

    /// The number of nodes.
    public int getNodeCount() {
        return nodeNames.size();
    }

    /// The number of links.
    public int getLinkCount() {
        return links.size();
    }

    @Override
    protected void captureEntryStart() {
        entering = true;
    }

    private int colorOf(ChartContext ctx, int node) {
        return ctx.getTheme().getSeriesColor(node);
    }

    @Override
    protected void dataChanged() {
        layoutDirty = true;
        super.dataChanged();
    }

    @Override
    public void paint(ChartContext ctx) {
        int n = nodeNames.size();
        int m = links.size();
        if (n == 0) {
            last = null;
            return;
        }
        Graphics g = ctx.getGraphics();
        Font f = ctx.getFont();
        int fh = f.getHeight();
        nodeWidth = ctx.mm(2.5f);
        float x = ctx.getPlotX();
        float w = ctx.getPlotWidth();
        // The layout depends only on the data and the plot geometry, so it is built once per
        // change of either and reused by every animation frame in between.
        boolean stale = layoutDirty || last == null || cacheX != ctx.getPlotX() || cacheY != ctx.getPlotY() || cacheW != ctx.getPlotWidth()
                || cacheH != ctx.getPlotHeight() || cacheNodeW != (int) nodeWidth || cacheFh != fh;
        if (stale) {
            cacheSrc = new int[m];
            cacheDst = new int[m];
            double[] val = new double[m];
            for (int l = 0; l < m; l++) {
                cacheSrc[l] = links.get(l)[0];
                cacheDst[l] = links.get(l)[1];
                val[l] = linkValues.get(l)[0];
            }
            // labels of the first and last columns sit outside: leave room for them
            int maxLabel = 0;
            for (int i = 0; i < n; i++) {
                maxLabel = Math.max(maxLabel, f.stringWidth(nodeNames.get(i)));
            }
            maxLabel = Math.min(maxLabel, ctx.getPlotWidth() / 5);
            cacheMaxLabel = maxLabel;
            SankeyLayout built = new SankeyLayout(n, cacheSrc, cacheDst, val, x, ctx.getPlotY(), w, ctx.getPlotHeight(), nodeWidth, Math.max(fh / 2f, ctx.mm(2)));
            if (built.getColumns() > 1) {
                float side = maxLabel + ctx.mm(1.5f);
                built = new SankeyLayout(n, cacheSrc, cacheDst, val, x, ctx.getPlotY(), w - side, ctx.getPlotHeight(), nodeWidth, Math.max(fh / 2f, ctx.mm(2)));
            }
            last = built;
            layoutDirty = false;
            cacheX = ctx.getPlotX();
            cacheY = ctx.getPlotY();
            cacheW = ctx.getPlotWidth();
            cacheH = ctx.getPlotHeight();
            cacheNodeW = (int) nodeWidth; // ctx.mm() is whole pixels
            cacheFh = fh;
        }
        int maxLabel = cacheMaxLabel;
        int[] src = cacheSrc;
        int[] dst = cacheDst;
        SankeyLayout lay = last;
        float t = 1;
        if (entering) {
            t = ctx.getProgress();
            if (t >= 1) {
                entering = false;
            }
        }
        int alpha = g.getAlpha();
        float vis = getVisibilityFraction();
        g.setAlpha((int) (alpha * vis * linkAlpha));
        for (int l = 0; l < m; l++) {
            float lw = lay.getLinkWidth(l);
            if (lw <= 0) {
                continue;
            }
            float x0 = lay.getNodeX(src[l]) + nodeWidth;
            float x1 = lay.getNodeX(dst[l]);
            // draw the ribbon growing from left to right while entering
            float xe = x0 + (x1 - x0) * t;
            float ye = lay.getLinkSourceY(l) + (lay.getLinkTargetY(l) - lay.getLinkSourceY(l)) * t;
            g.setColor(colorOf(ctx, src[l]));
            // a back link closes a cycle and runs right to left across the others: draw it faint
            // so the forward flow stays readable
            g.setAlpha((int) (alpha * vis * linkAlpha * (lay.isBackLink(l) ? 0.35f : 1f)));
            g.fillShape(Shapes.ribbon(x0, lay.getLinkSourceY(l), lay.getLinkSourceY(l) + lw, xe, ye, ye + lw));
        }
        g.setAlpha((int) (alpha * vis));
        for (int i = 0; i < n; i++) {
            g.setColor(colorOf(ctx, i));
            g.fillRect(Math.round(lay.getNodeX(i)), Math.round(lay.getNodeY(i)), Math.round(nodeWidth), Math.max(1, Math.round(lay.getNodeHeight(i))));
        }
        if (t >= 1) {
            g.setFont(f);
            for (int i = 0; i < n; i++) {
                String s = LabelLayout.ellipsize(f, nodeNames.get(i), maxLabel);
                int sw = f.stringWidth(s);
                // every label sits right of its node; the layout left room after the last column
                int lx = Math.round(lay.getNodeX(i) + nodeWidth + ctx.mm(1));
                int ly = Math.round(lay.getNodeY(i) + lay.getNodeHeight(i) / 2 - fh / 2f);
                if (ctx.getLabels().placeAt(lx, ly, sw, fh)) {
                    g.setColor(ctx.getTheme().getTextColor());
                    g.drawString(s, lx, ly);
                }
            }
        }
        g.setAlpha(alpha);
    }

    @Override
    public int getPointCount() {
        return nodeNames.size() + links.size();
    }

    /// Points `0..nodeCount-1` are nodes, the rest are links.
    @Override
    public ChartPoint getPoint(ChartContext ctx, int index) {
        SankeyLayout lay = last;
        if (lay == null || index < 0) {
            return null;
        }
        int n = nodeNames.size();
        if (index < n) {
            float cx = lay.getNodeX(index) + nodeWidth / 2;
            float cy = lay.getNodeY(index) + lay.getNodeHeight(index) / 2;
            return new ChartPoint(this, index, index, nodeValue(index), Math.round(cx) - ctx.getOriginX(), Math.round(cy) - ctx.getOriginY(), nodeNames.get(index));
        }
        int l = index - n;
        if (l >= links.size()) {
            return null;
        }
        int[] e = links.get(l);
        float x0 = lay.getNodeX(e[0]) + nodeWidth;
        float x1 = lay.getNodeX(e[1]);
        float cx = (x0 + x1) / 2;
        float cy = (lay.getLinkSourceY(l) + lay.getLinkTargetY(l)) / 2 + lay.getLinkWidth(l) / 2;
        return new ChartPoint(this, index, index, linkValues.get(l)[0], Math.round(cx) - ctx.getOriginX(), Math.round(cy) - ctx.getOriginY(),
                nodeNames.get(e[0]) + " > " + nodeNames.get(e[1]));
    }

    private double nodeValue(int i) {
        double in = 0;
        double out = 0;
        for (int l = 0; l < links.size(); l++) {
            int[] e = links.get(l);
            double v = linkValues.get(l)[0];
            // the layout counts NaN, infinite and negative links as nothing, so the tooltip must too
            if (!(v > 0 && v < Double.POSITIVE_INFINITY)) { //NOPMD LogicInversion - also true for NaN
                continue;
            }
            if (e[0] == i) {
                out += v;
            }
            if (e[1] == i) {
                in += v;
            }
        }
        return Math.max(in, out);
    }

    @Override
    public ChartPoint hitTest(ChartContext ctx, int x, int y, int radius) {
        SankeyLayout lay = last;
        if (lay == null) {
            return null;
        }
        float ax = x + ctx.getOriginX();
        float ay = y + ctx.getOriginY();
        int n = nodeNames.size();
        for (int i = 0; i < n; i++) {
            float nx = lay.getNodeX(i);
            if (ax >= nx - radius / 2f && ax <= nx + nodeWidth + radius / 2f && ay >= lay.getNodeY(i) && ay <= lay.getNodeY(i) + lay.getNodeHeight(i)) {
                return getPoint(ctx, i);
            }
        }
        for (int l = links.size() - 1; l >= 0; l--) {
            int[] e = links.get(l);
            float x0 = lay.getNodeX(e[0]) + nodeWidth;
            float x1 = lay.getNodeX(e[1]);
            // a back link runs right to left; its ribbon spans the same x range reversed
            // paint skips an empty link, so nothing is there to hit
            if (!(lay.getLinkWidth(l) > 0) || ax < Math.min(x0, x1) || ax > Math.max(x0, x1) || x1 == x0) { //NOPMD LogicInversion - also true for NaN
                continue;
            }
            float s = ribbonEase((ax - x0) / (x1 - x0));
            float top = lay.getLinkSourceY(l) + (lay.getLinkTargetY(l) - lay.getLinkSourceY(l)) * s;
            if (ay >= top - 2 && ay <= top + Math.max(lay.getLinkWidth(l), 4) + 2) {
                return getPoint(ctx, n + l);
            }
        }
        return null;
    }

    /// The fraction of the way down a ribbon edge (`Shapes.ribbon`) at the fraction `u` of the
    /// way across it. The edge is a cubic Bezier with both inner control points at the middle x,
    /// so across is `1.5t(1-t) + t^3` and down is `3t^2 - 2t^3` of the curve parameter t. Across
    /// is strictly increasing (its slope is `3(t-0.5)^2 + 0.75`), so bisection finds t.
    static float ribbonEase(float u) {
        float lo = 0;
        float hi = 1;
        for (int k = 0; k < 20; k++) {
            float t = (lo + hi) / 2;
            if (1.5f * t * (1 - t) + t * t * t < u) {
                lo = t;
            } else {
                hi = t;
            }
        }
        float t = (lo + hi) / 2;
        return t * t * (3 - 2 * t);
    }

    @Override
    public String formatTooltip(ChartContext ctx, ChartPoint p) {
        return (p.getLabel() == null ? "" : p.getLabel() + ": ") + formatValue(ctx, p.getY());
    }

    @Override
    public void collectTableRows(java.util.List<String[]> rows) {
        for (int l = 0; l < links.size(); l++) {
            int[] e = links.get(l);
            rows.add(new String[]{getName(), nodeNames.get(e[0]) + " > " + nodeNames.get(e[1]), formatValue(null, linkValues.get(l)[0])});
        }
    }
}
