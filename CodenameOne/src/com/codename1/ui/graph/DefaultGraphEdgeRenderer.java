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
package com.codename1.ui.graph;

import com.codename1.graph.GraphEdge;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;

import java.util.HashMap;
import java.util.Map;

/// Paints an edge as a line with an arrowhead on a directed edge and its
/// label in a pill at the middle.
///
/// Colours come from `NodeGraphEdge` (the line) and `NodeGraphEdgeLabel`
/// (the pill and its text); a selected edge takes the colour of
/// `NodeGraphNodeSelected`. An edge's style key picks a colour from
/// `setColor`.
public class DefaultGraphEdgeRenderer implements GraphEdgeRenderer {
    private final Map<String, Integer> colors = new HashMap<String, Integer>();

    /// Maps an edge style key to a line colour.
    public void setColor(String styleKey, int rgb) {
        colors.put(styleKey, Integer.valueOf(rgb));
    }

    @Override
    public void paintEdge(Graphics g, NodeGraph graph, GraphEdge edge, double[] xs, double[] ys, int count, boolean selected) {
        if (count < 2) {
            return;
        }
        Style s = GraphLook.style("NodeGraphEdge");
        int color = s.getFgColor();
        Integer custom = edge.getStyleKey() == null ? null : colors.get(edge.getStyleKey());
        if (custom != null) {
            color = custom.intValue();
        }
        if (selected) {
            color = GraphLook.style("NodeGraphNodeSelected").getFgColor();
        }
        float zoom = graph.getZoom();
        float width = Math.max(1f, GraphLook.mm(0.3f) * Math.min(zoom, 2f) * (selected ? 1.8f : 1f));
        g.setColor(color);
        double arrow = edge.isDirected() ? Math.max(4, GraphLook.mm(1.5f) * Math.min(zoom, 1.5f)) : 0;
        // stop the line short of the target so the arrow's tip is sharp
        double ex = xs[count - 1];
        double ey = ys[count - 1];
        double bx = xs[count - 2];
        double by = ys[count - 2];
        double dx = ex - bx;
        double dy = ey - by;
        double len = Math.sqrt(dx * dx + dy * dy);
        for (int i = 0; i < count - 1; i++) {
            double x2 = xs[i + 1];
            double y2 = ys[i + 1];
            if (i == count - 2 && arrow > 0 && len > arrow) {
                x2 = ex - dx / len * arrow * 0.8;
                y2 = ey - dy / len * arrow * 0.8;
            }
            GraphLook.line(g, xs[i], ys[i], x2, y2, width);
        }
        if (arrow > 0 && len > 0.01) {
            double ux = dx / len;
            double uy = dy / len;
            double half = arrow * 0.55;
            int[] ax = {(int) Math.round(ex), (int) Math.round(ex - ux * arrow - uy * half),
                (int) Math.round(ex - ux * arrow + uy * half)};
            int[] ay = {(int) Math.round(ey), (int) Math.round(ey - uy * arrow + ux * half),
                (int) Math.round(ey - uy * arrow - ux * half)};
            g.fillPolygon(ax, ay, 3);
        }
        if (edge.getLabel() != null && edge.getLabel().length() > 0 && zoom >= graph.getDetailZoom()) {
            paintLabel(g, graph.getZoom(), edge.getLabel(), xs, ys, count);
        }
    }

    private void paintLabel(Graphics g, float zoom, String text, double[] xs, double[] ys, int count) {
        double total = 0;
        for (int i = 0; i < count - 1; i++) {
            total += dist(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }
        double half = total / 2;
        double mx = xs[0];
        double my = ys[0];
        for (int i = 0; i < count - 1; i++) {
            double d = dist(xs[i], ys[i], xs[i + 1], ys[i + 1]);
            if (half <= d && d > 0) {
                mx = xs[i] + (xs[i + 1] - xs[i]) * half / d;
                my = ys[i] + (ys[i + 1] - ys[i]) * half / d;
                break;
            }
            half -= d;
        }
        Style ls = GraphLook.style("NodeGraphEdgeLabel");
        Font f = GraphLook.font("NodeGraphEdgeLabel", zoom);
        int pad = GraphLook.mm(1f, zoom);
        int tw = f.stringWidth(text);
        int th = f.getHeight();
        int x = (int) Math.round(mx) - tw / 2 - pad;
        int y = (int) Math.round(my) - th / 2 - pad / 2;
        int w = tw + pad * 2;
        int h = th + pad;
        g.setColor(GraphLook.bg(ls, 0xffffff));
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(ls.getFgColor());
        g.setFont(f);
        g.drawString(text, x + pad, y + pad / 2);
    }

    private static double dist(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
