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
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphPort;
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
    private boolean colorByPortType;

    /// True when an edge takes the colour of the port it leaves.
    public boolean isColorByPortType() {
        return colorByPortType;
    }

    /// Draws an edge that has no colour of its own in the colour its source
    /// port's type has in the graph's `DefaultGraphNodeRenderer`, so the
    /// wires of a node editor read by type. Off by default.
    public void setColorByPortType(boolean colorByPortType) {
        this.colorByPortType = colorByPortType;
    }

    private int typeColor(NodeGraph graph, GraphEdge edge, int fallback) {
        GraphNodeRenderer r = graph.getNodeRenderer();
        GraphNode n = edge.getSourcePortId() == null ? null : graph.getModel().getNode(edge.getSourceId());
        GraphPort p = n == null ? null : n.getPort(edge.getSourcePortId());
        if (p == null || p.getType() == null || !(r instanceof DefaultGraphNodeRenderer)) {
            return fallback;
        }
        return ((DefaultGraphNodeRenderer) r).getAccent(p.getType(), fallback);
    }

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
        } else if (colorByPortType) {
            color = typeColor(graph, edge, color);
        }
        if (selected) {
            color = GraphLook.style("NodeGraphNodeSelected").getFgColor();
        }
        float zoom = graph.getZoom();
        float width = Math.max(1f, GraphLook.mm(0.3f) * Math.min(zoom, 2f) * (selected ? 1.8f : 1f));
        g.setColor(color);
        double size = Math.max(4, GraphLook.mm(1.5f) * Math.min(zoom, 1.5f));
        int head = edge.getTargetArrow();
        int tail = edge.getSourceArrow();
        int style = edge.getLineStyle();
        // stop the line short of a filled head so its tip is sharp
        double ex = xs[count - 1];
        double ey = ys[count - 1];
        double dx = ex - xs[count - 2];
        double dy = ey - ys[count - 2];
        double len = Math.sqrt(dx * dx + dy * dy);
        double sx = xs[0];
        double sy = ys[0];
        double tdx = sx - xs[1];
        double tdy = sy - ys[1];
        double tlen = Math.sqrt(tdx * tdx + tdy * tdy);
        double headCut = cut(head, size);
        double tailCut = cut(tail, size);
        double phase = 0;
        for (int i = 0; i < count - 1; i++) {
            double x1 = xs[i];
            double y1 = ys[i];
            double x2 = xs[i + 1];
            double y2 = ys[i + 1];
            if (i == 0 && tailCut > 0 && tlen > size) {
                x1 = sx - tdx / tlen * tailCut;
                y1 = sy - tdy / tlen * tailCut;
            }
            if (i == count - 2 && headCut > 0 && len > size) {
                x2 = ex - dx / len * headCut;
                y2 = ey - dy / len * headCut;
            }
            if (style == GraphEdge.LINE_SOLID) {
                GraphLook.line(g, x1, y1, x2, y2, width);
            } else {
                phase = dashes(g, x1, y1, x2, y2, width, style == GraphEdge.LINE_DOTTED, phase);
            }
        }
        if (len > 0.01) {
            paintHead(g, head, ex, ey, dx / len, dy / len, size, width);
        }
        if (tlen > 0.01) {
            paintHead(g, tail, sx, sy, tdx / tlen, tdy / tlen, size, width);
        }
        if (edge.getLabel() != null && edge.getLabel().length() > 0 && zoom >= graph.getDetailZoom()) {
            paintLabel(g, graph.getZoom(), edge.getLabel(), xs, ys, count);
        }
    }

    // How far from its tip the line under a head stops.
    private static double cut(int kind, double size) {
        switch (kind) {
            case GraphEdge.ARROW_FILLED:
                return size * 0.8;
            case GraphEdge.ARROW_DIAMOND:
                return size * 1.4;
            case GraphEdge.ARROW_CIRCLE:
                return size * 0.8;
            default:
                return 0;
        }
    }

    // Draws one segment as dashes or dots; a stroke has no dash pattern, so
    // the segment is walked. The phase carries the pattern round a bend.
    private static double dashes(Graphics g, double x1, double y1, double x2, double y2,
            float width, boolean dotted, double phase) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01) {
            return phase;
        }
        double on = dotted ? Math.max(1.5, width) : Math.max(4, width * 4);
        double off = dotted ? Math.max(3, width * 2.5) : Math.max(3, width * 3);
        double period = on + off;
        double at = -phase;
        while (at < len) {
            double a = Math.max(0, at);
            double b = Math.min(len, at + on);
            if (b > a) {
                GraphLook.line(g, x1 + dx * a / len, y1 + dy * a / len, x1 + dx * b / len, y1 + dy * b / len, width);
            }
            at += period;
        }
        double rest = (phase + len) % period;
        return rest;
    }

    // Paints the mark at one end of an edge; the unit vector points at the tip.
    private static void paintHead(Graphics g, int kind, double ex, double ey, double ux, double uy,
            double size, float width) {
        double half = size * 0.55;
        switch (kind) {
            case GraphEdge.ARROW_FILLED: {
                int[] ax = {(int) Math.round(ex), (int) Math.round(ex - ux * size - uy * half),
                    (int) Math.round(ex - ux * size + uy * half)};
                int[] ay = {(int) Math.round(ey), (int) Math.round(ey - uy * size + ux * half),
                    (int) Math.round(ey - uy * size - ux * half)};
                g.fillPolygon(ax, ay, 3);
                break;
            }
            case GraphEdge.ARROW_OPEN:
                GraphLook.line(g, ex, ey, ex - ux * size - uy * half, ey - uy * size + ux * half, width);
                GraphLook.line(g, ex, ey, ex - ux * size + uy * half, ey - uy * size - ux * half, width);
                break;
            case GraphEdge.ARROW_DIAMOND: {
                double mid = size * 0.75;
                double side = size * 0.42;
                int[] ax = {(int) Math.round(ex), (int) Math.round(ex - ux * mid - uy * side),
                    (int) Math.round(ex - ux * size * 1.5), (int) Math.round(ex - ux * mid + uy * side)};
                int[] ay = {(int) Math.round(ey), (int) Math.round(ey - uy * mid + ux * side),
                    (int) Math.round(ey - uy * size * 1.5), (int) Math.round(ey - uy * mid - ux * side)};
                g.fillPolygon(ax, ay, 4);
                break;
            }
            case GraphEdge.ARROW_CIRCLE: {
                int r = (int) Math.max(2, Math.round(size * 0.45));
                int cx = (int) Math.round(ex - ux * r);
                int cy = (int) Math.round(ey - uy * r);
                g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
                break;
            }
            default:
                break;
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
