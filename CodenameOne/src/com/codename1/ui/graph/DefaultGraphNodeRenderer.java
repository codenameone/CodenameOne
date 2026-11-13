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

import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphPort;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Paints a node as a rounded card: a coloured strip on the leading edge,
/// the label, the description under it, and the ports on the sides with
/// their labels inside the card.
///
/// Colours come from the `NodeGraphNode` UIID (background, text) and
/// `NodeGraphNodeSelected` (the outline of a selected node), ports from
/// `NodeGraphPort`. A node's style key picks its strip colour from
/// `setAccent`; a node without one uses the selection colour.
public class DefaultGraphNodeRenderer implements GraphNodeRenderer {
    private final Map<String, Integer> accents = new HashMap<String, Integer>();

    /// Maps a node style key to the colour of its strip and its ports.
    public void setAccent(String styleKey, int rgb) {
        accents.put(styleKey, Integer.valueOf(rgb));
    }

    /// The colour of a style key, or `fallback` when none was set.
    public int getAccent(String styleKey, int fallback) {
        Integer c = styleKey == null ? null : accents.get(styleKey);
        return c == null ? fallback : c.intValue();
    }

    @Override
    public void paintNode(Graphics g, NodeGraph graph, GraphNode node, int x, int y, int w, int h, boolean selected) {
        Style s = GraphLook.style("NodeGraphNode");
        Style sel = GraphLook.style("NodeGraphNodeSelected");
        int bg = GraphLook.bg(s, 0xffffff);
        int fg = s.getFgColor();
        int primary = sel.getFgColor();
        int accent = getAccent(node.getStyleKey(), primary);
        float zoom = graph.getZoom();
        int arc = Math.max(2, Math.round(GraphLook.mm(1.6f) * Math.min(zoom, 1.5f)));
        boolean detail = zoom >= graph.getDetailZoom();
        if (detail) {
            GraphLook.shadow(g, x, y, w, h, arc, selected ? 1f : 0.6f);
        }
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, arc, arc);
        int strip = Math.max(2, Math.round(GraphLook.mm(0.8f) * Math.min(zoom, 1.5f)));
        g.setColor(accent);
        g.fillRoundRect(x, y, strip * 2, h, arc, arc);
        g.setColor(bg);
        g.fillRect(x + strip, y, strip, h);
        g.setColor(selected ? primary : GraphLook.mix(bg, fg, 50));
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        if (selected) {
            g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, arc, arc);
        }
        if (detail) {
            paintText(g, graph, node, x + strip * 2, y, w - strip * 2, h, fg, bg);
        }
        if (node.hasPorts()) {
            paintPorts(g, graph, node, bg, fg, detail);
        }
    }

    private void paintText(Graphics g, NodeGraph graph, GraphNode node, int x, int y, int w, int h, int fg, int bg) {
        Font f = GraphLook.font("NodeGraphNode", graph.getZoom());
        int pad = GraphLook.mm(1.2f, graph.getZoom());
        int titleH = graph.headerHeightOnScreen();
        int ty;
        if (node.hasPorts()) {
            ty = y + (titleH - f.getHeight()) / 2;
        } else if (node.getDescription() != null) {
            ty = y + (h - f.getHeight() * 2) / 2;
        } else {
            ty = y + (h - f.getHeight()) / 2;
        }
        g.setFont(f);
        g.setColor(fg);
        g.drawString(GraphLook.fit(node.getLabel(), f, w - pad * 2), x + pad, ty);
        if (node.getDescription() != null && !node.hasPorts()) {
            g.setColor(GraphLook.mix(bg, fg, 150));
            g.drawString(GraphLook.fit(node.getDescription(), f, w - pad * 2), x + pad, ty + f.getHeight());
        }
        if (node.hasPorts()) {
            g.setColor(GraphLook.mix(bg, fg, 40));
            g.drawLine(x, y + titleH, x + w - pad, y + titleH);
        }
    }

    private void paintPorts(Graphics g, NodeGraph graph, GraphNode node, int bg, int fg, boolean detail) {
        Style ps = GraphLook.style("NodeGraphPort");
        int portColor = ps.getFgColor();
        int r = graph.portRadiusOnScreen();
        Font f = GraphLook.font("NodeGraphPort", graph.getZoom());
        int pad = GraphLook.mm(1.5f, graph.getZoom()) + r;
        List<GraphPort> ins = node.getInputs();
        List<GraphPort> outs = node.getOutputs();
        for (int pass = 0; pass < 2; pass++) {
            List<GraphPort> ports = pass == 0 ? ins : outs;
            for (int i = 0; i < ports.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                GraphPort p = ports.get(i);
                int px = graph.worldToScreenX(graph.portX(node, p));
                int py = graph.worldToScreenY(graph.portY(node, p));
                boolean connected = graph.isPortConnected(node, p);
                g.setColor(bg);
                g.fillArc(px - r - 1, py - r - 1, r * 2 + 2, r * 2 + 2, 0, 360);
                g.setColor(p.getType() == null ? portColor : getAccent(p.getType(), portColor));
                if (connected) {
                    g.fillArc(px - r, py - r, r * 2, r * 2, 0, 360);
                } else {
                    g.drawArc(px - r, py - r, r * 2 - 1, r * 2 - 1, 0, 360);
                    g.drawArc(px - r + 1, py - r + 1, r * 2 - 3, r * 2 - 3, 0, 360);
                }
                if (detail && p.getLabel() != null) {
                    g.setFont(f);
                    g.setColor(GraphLook.mix(bg, fg, 190));
                    int room = labelRoom(graph, node, p, py, f, pad, pass == 0 ? outs : ins);
                    String text = GraphLook.fit(p.getLabel(), f, room);
                    int ly = py - f.getHeight() / 2;
                    if (p.isInput()) {
                        g.drawString(text, px + pad, ly);
                    } else {
                        g.drawString(text, px - pad - f.stringWidth(text), ly);
                    }
                }
            }
        }
    }

    // The width a port label may take: the whole card when nothing faces it,
    // otherwise what the label across the same row leaves, but never less
    // than half the card, so the two share a crowded row evenly.
    private static int labelRoom(NodeGraph graph, GraphNode node, GraphPort port, int py,
            Font f, int pad, List<GraphPort> across) {
        int full = graph.worldToScreenLength(node.getWidth()) - pad * 2;
        int half = full / 2 - pad;
        int taken = 0;
        for (int i = 0; i < across.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            GraphPort o = across.get(i);
            int oy = graph.worldToScreenY(graph.portY(node, o));
            if (o.getLabel() != null && Math.abs(oy - py) < f.getHeight()) {
                taken = Math.max(taken, f.stringWidth(o.getLabel()));
            }
        }
        if (taken == 0) {
            return full;
        }
        return Math.max(half, full - taken - pad * 2);
    }
}
