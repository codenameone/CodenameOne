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
import com.codename1.graph.GraphNodeTemplate;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.List;

/// The kinds of node a `NodeGraph` can be given, as a strip of items to drag
/// onto it: the shapes of a flow chart, or the node types of a node editor.
///
/// ```java
/// GraphPalette palette = new GraphPalette(graph);
/// palette.add(new GraphNodeTemplate("Step").setShape(GraphShapes.RECT));
/// palette.add(new GraphNodeTemplate("Decision").setShape(GraphShapes.DECISION));
/// form.add(BorderLayout.WEST, palette);
/// ```
///
/// Dragging an item over the graph and letting go adds a node there. Tapping
/// an item arms it instead, and the next tap on the graph's background adds
/// the node, which is the way without a drag. Either way the node is offered
/// to the graph's listeners and its model, as `NodeGraph.createNode` does.
///
/// The palette does not scroll; give it the items that fit, or several
/// palettes.
public class GraphPalette extends Component {
    private final NodeGraph graph;
    private final List<GraphNodeTemplate> templates = new ArrayList<GraphNodeTemplate>();
    private boolean horizontal;
    private int pressed = -1;
    private int pressX;
    private int pressY;
    private boolean dragging;
    private final GeneralPath path = new GeneralPath();

    /// A vertical palette for a graph.
    public GraphPalette(NodeGraph graph) {
        this.graph = graph;
        setUIID("NodeGraphPalette");
        setFocusable(false);
    }

    // the armed item was used: listened to only while the palette is shown,
    // so a palette that is thrown away is not kept alive by the graph
    private final GraphAdapter used = new GraphAdapter() {
        @Override
        public void nodeCreated(NodeGraph g, GraphNode node) {
            repaint();
        }
    };

    @Override
    protected void initComponent() {
        super.initComponent();
        graph.addGraphListener(used);
    }

    @Override
    protected void deinitialize() {
        graph.removeGraphListener(used);
        // a drag the form took away mid-gesture is let go of
        if (pressed >= 0) {
            pressed = -1;
            dragging = false;
            graph.clearDropPreview();
        }
        super.deinitialize();
    }

    /// The graph the items are added to.
    public NodeGraph getGraph() {
        return graph;
    }

    /// Adds an item.
    public GraphPalette add(GraphNodeTemplate template) {
        if (template != null) {
            templates.add(template);
            setShouldCalcPreferredSize(true);
            repaint();
        }
        return this;
    }

    /// Removes an item.
    public void remove(GraphNodeTemplate template) {
        if (templates.remove(template)) {
            // an item removed while held is let go of
            pressed = -1;
            dragging = false;
            graph.clearDropPreview();
            if (graph.getPendingTemplate() == template) { // NOPMD CompareObjectsWithEquals - the armed item itself
                graph.setPendingTemplate(null);
            }
            setShouldCalcPreferredSize(true);
            repaint();
        }
    }

    /// The items, in order. Do not modify the list.
    public List<GraphNodeTemplate> getTemplates() {
        return templates;
    }

    /// True when the items are laid in a row.
    public boolean isHorizontal() {
        return horizontal;
    }

    /// Lays the items in a row, for a palette above or below the graph, or
    /// in a column, the default.
    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
        setShouldCalcPreferredSize(true);
    }

    private int itemW() {
        return GraphLook.mm(15);
    }

    private int itemH() {
        return GraphLook.mm(8) + GraphLook.style("NodeGraphPaletteItem").getFont().getHeight() + GraphLook.mm(2);
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        int n = Math.max(1, templates.size());
        int w = horizontal ? n * itemW() : itemW();
        int h = horizontal ? itemH() : n * itemH();
        return new Dimension(w + s.getHorizontalPadding(), h + s.getVerticalPadding());
    }

    private int originX() {
        return getX() + getStyle().getPaddingLeftNoRTL();
    }

    private int originY() {
        return getY() + getStyle().getPaddingTop();
    }

    /// The item at a point of the palette's parent, or -1.
    private int itemAt(int x, int y) {
        int rx = x - originX();
        int ry = y - originY();
        if (rx < 0 || ry < 0) {
            return -1;
        }
        int i;
        if (horizontal) {
            if (ry >= itemH()) {
                return -1;
            }
            i = rx / itemW();
        } else {
            if (rx >= itemW()) {
                return -1;
            }
            i = ry / itemH();
        }
        return i < templates.size() ? i : -1;
    }

    @Override
    public void paint(Graphics g) {
        Style is = GraphLook.style("NodeGraphPaletteItem");
        Font f = is.getFont();
        int iw = itemW();
        int ih = itemH();
        int gap = GraphLook.mm(1);
        int primary = GraphLook.style("NodeGraphNodeSelected").getFgColor();
        int bg = GraphLook.bg(is, 0xffffff);
        int fg = is.getFgColor();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        GraphNodeTemplate armed = graph.getPendingTemplate();
        int n = templates.size();
        for (int i = 0; i < n; i++) {
            GraphNodeTemplate t = templates.get(i);
            int x = originX() + (horizontal ? i * iw : 0);
            int y = originY() + (horizontal ? 0 : i * ih);
            boolean on = t == armed || i == pressed; // NOPMD CompareObjectsWithEquals - the armed item itself
            int arc = GraphLook.mm(1.2f);
            if (on) {
                g.setColor(GraphLook.mix(bg, primary, 50));
                g.fillRoundRect(x + gap / 2, y + gap / 2, iw - gap, ih - gap, arc, arc);
            }
            // the outline, in the proportions of the node it makes
            int boxW = iw - gap * 4;
            int boxH = ih - f.getHeight() - gap * 3;
            double ratio = t.getWidth() / Math.max(1, t.getHeight());
            int pw = boxW;
            int ph = (int) Math.round(pw / Math.max(0.2, ratio));
            if (ph > boxH) {
                ph = boxH;
                pw = (int) Math.round(ph * ratio);
            }
            int px = x + (iw - pw) / 2;
            int py = y + gap + (boxH - ph) / 2;
            GraphShape shape = GraphShapeRegistry.get(t.getShape());
            if (shape != null && g.isShapeSupported()) {
                path.reset();
                shape.outline(path, px, py, pw - 1, ph - 1);
                g.setColor(bg);
                g.fillShape(path);
                g.setColor(on ? primary : GraphLook.mix(bg, fg, 150));
                g.drawShape(path, GraphLook.stroke(Math.max(1f, GraphLook.mm(0.25f))));
                path.reset();
                shape.detail(path, px, py, pw - 1, ph - 1);
                if (path.getPointsSize() > 0) {
                    g.drawShape(path, GraphLook.stroke(1f));
                }
            } else {
                g.setColor(bg);
                g.fillRoundRect(px, py, pw, ph, arc, arc);
                g.setColor(on ? primary : GraphLook.mix(bg, fg, 150));
                g.drawRoundRect(px, py, pw - 1, ph - 1, arc, arc);
                int r = Math.max(1, GraphLook.mm(0.5f));
                if (t.hasPorts()) {
                    g.fillArc(px - r, py + ph / 2 - r, r * 2, r * 2, 0, 360);
                    g.fillArc(px + pw - r - 1, py + ph / 2 - r, r * 2, r * 2, 0, 360);
                }
            }
            String name = t.getName();
            if (name != null) {
                String text = GraphLook.fit(name, f, iw - gap * 2);
                g.setFont(f);
                g.setColor(fg);
                g.drawString(text, x + (iw - f.stringWidth(text)) / 2, y + ih - f.getHeight() - gap);
            }
        }
        g.setAntiAliased(aa);
    }

    @Override
    protected boolean isStickyDrag() {
        // the drag leaves the palette for the graph
        return true;
    }

    @Override
    public void pointerPressed(int x, int y) {
        pressX = x;
        pressY = y;
        dragging = false;
        pressed = itemAt(x - getAbsoluteX() + getX(), y - getAbsoluteY() + getY());
        repaint();
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (pressed < 0) {
            return;
        }
        if (!dragging) {
            int slop = GraphLook.mm(1.5f);
            if (Math.abs(x - pressX) < slop && Math.abs(y - pressY) < slop) {
                return;
            }
            dragging = true;
        }
        if (pressed >= templates.size()) {
            return;
        }
        graph.showDropPreview(templates.get(pressed), x, y);
    }

    @Override
    public void pointerReleased(int x, int y) {
        int item = pressed;
        pressed = -1;
        if (item >= 0 && item < templates.size()) {
            GraphNodeTemplate t = templates.get(item);
            if (dragging) {
                graph.setPendingTemplate(null);
                graph.dropTemplate(t, x, y);
            } else {
                // tapping the armed item lets go of it
                graph.setPendingTemplate(graph.getPendingTemplate() == t ? null : t); // NOPMD CompareObjectsWithEquals - the armed item itself
            }
        }
        dragging = false;
        graph.clearDropPreview();
        repaint();
    }
}
