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

import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphChangeListener;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphGroup;
import com.codename1.graph.GraphJson;
import com.codename1.graph.GraphModel;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphNodeTemplate;
import com.codename1.graph.GraphPort;
import com.codename1.graph.arrange.ForceArrangement;
import com.codename1.graph.arrange.GraphArrangement;
import com.codename1.graph.arrange.ArrangementResult;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.ScrollSurface;
import com.codename1.ui.Image;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.io.JSONParser;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.util.UndoHistory;
import com.codename1.util.UndoableEdit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A pannable, zoomable view of a `GraphModel` where nodes are dragged,
/// connected and grouped: a system diagram, a mind map, or a node editor.
///
/// ```java
/// DefaultGraphModel model = new DefaultGraphModel();
/// model.addNode(new GraphNode("api", "API gateway"));
/// model.addNode(new GraphNode("orders", "Orders"));
/// model.addEdge("api", "orders");
/// NodeGraph graph = new NodeGraph(model);
/// graph.arrange(new LayeredArrangement(), false);
/// Form form = new Form("Services", new BorderLayout());
/// form.add(BorderLayout.CENTER, graph);
/// form.show();
/// ```
///
/// What a user can do:
///
/// - drag the background to pan, with a fling; pinch, or Ctrl or Cmd with the
///   wheel, to zoom about the pointer;
/// - tap a node or an edge to select it, and Shift-tap to add to the
///   selection; drag a node to move it, with everything selected;
/// - drag from an output port to an input port to connect them. A node
///   without ports shows a handle on its trailing edge while it is selected,
///   and a long press on it starts a connection too;
/// - drag the end of an edge off an input port to reconnect or drop it;
/// - long press the background, or Shift-drag it, to select with a box;
/// - tap a group's title to collapse or expand it, double tap a node to open
///   it (and edit its label when `setLabelEditing` is on);
/// - drag in the minimap to move the view.
///
/// Every edit is offered to the `GraphListener`s, then to the model; either
/// may refuse it. With `setEditingEnabled(false)` the graph can only be panned,
/// zoomed and selected.
///
/// The nodes and edges are painted, so the component costs what is on
/// screen: below `getDetailZoom` they are drawn without text, which keeps a
/// graph of thousands of nodes moving at frame rate. A node can still carry a
/// real component from a `NodeComponentFactory`.
public class NodeGraph extends Container {
    /// Edges with ports curve between them; other edges are straight, or
    /// follow the bends an arrangement gave them.
    public static final int EDGE_AUTO = 0;

    /// Straight segments through the bends.
    public static final int EDGE_STRAIGHT = 1;

    /// Horizontal and vertical segments only.
    public static final int EDGE_ORTHOGONAL = 2;

    /// Smooth curves through the bends.
    public static final int EDGE_CURVED = 3;

    /// The height of the title band of a node with ports, in world units.
    static final double HEADER = 30;
    private static final double COLLAPSED_W = 180;
    private static final double COLLAPSED_H = 60;
    private static final double GROUP_PAD = 24;
    private static final double GROUP_TITLE = 28;
    private static final int LAYOUT_MS = 450;

    private static final int G_NONE = 0;
    private static final int G_MOVE = 2;
    private static final int G_CONNECT = 3;
    private static final int G_MARQUEE = 4;
    private static final int G_MINIMAP = 5;
    private static final int G_RESIZE = 6;
    private static final int G_LANE = 7;

    /// The width of a lane's title band, in world units.
    private static final double LANE_HEADER = 30;
    private static final double MIN_NODE_W = 40;
    private static final double MIN_NODE_H = 24;
    private static final double MIN_LANE = 60;
    private static final double PASTE_STEP = 24;

    private GraphModel model;
    private final GraphChangeListener modelListener = new GraphChangeListener() {
        @Override
        public void graphChanged(GraphModel m) {
            modelChanged();
        }
    };
    private GraphNodeRenderer nodeRenderer = new DefaultGraphNodeRenderer();
    private GraphEdgeRenderer edgeRenderer = new DefaultGraphEdgeRenderer();
    private final List<GraphListener> listeners = new ArrayList<GraphListener>();

    private float scale = 1f;
    private float minZoom = 0.08f;
    private float maxZoom = 4f;
    private float detailZoom = 0.3f;
    private float componentZoom = 0.6f;
    private double viewX;
    private double viewY;
    private boolean viewSet;
    private int edgeStyle = EDGE_AUTO;
    private boolean editable = true;
    private boolean gridVisible = true;
    private double snap;
    private boolean minimapVisible;
    private boolean labelEditing;

    private final List<GraphNode> selection = new ArrayList<GraphNode>();
    private GraphEdge selectedEdge;

    private final Canvas canvas = new Canvas();
    private final Container layer = new Container(new NodeLayout());
    private NodeComponentFactory componentFactory;
    private final Map<String, Component> components = new HashMap<String, Component>();
    private TextField labelField;

    // the bounds of every group, minX, minY, maxX, maxY, from the last refresh
    private final Map<String, double[]> groupBounds = new HashMap<String, double[]>();
    private boolean groupsDirty = true;
    private final Map<String, Boolean> connectedPorts = new HashMap<String, Boolean>();

    // gesture
    private int gesture;
    private int pressX;
    private int pressY;
    private int lastX;
    private int lastY;
    private boolean shiftDown;
    private GraphNode pressNode;
    private GraphPort pressPort;
    private GraphGroup pressGroup;
    private GraphEdge pressEdge;
    private boolean pressHandle;
    private boolean pressMinimap;

    // moving
    private final List<GraphNode> moving = new ArrayList<GraphNode>();
    private double[] movingFrom;
    private double moveWorldX;
    private double moveWorldY;

    // connecting
    private GraphNode connectNode;
    private GraphPort connectPort;
    private boolean connectReversed;
    private GraphEdge detached;
    private GraphNode hoverNode;
    private GraphPort hoverPort;
    private boolean hoverValid;

    // marquee start, in world units: the view may scroll under it
    private double marqueeX;
    private double marqueeY;

    // animation
    private Map<String, double[]> animFrom;
    private ArrangementResult animTo;
    private long animStart;
    private ForceArrangement live;
    private boolean liveRunning;

    // editing extras
    private UndoHistory undoHistory;
    private boolean resizable;
    private GraphNodeTemplate pendingTemplate;
    private GraphNodeTemplate previewTemplate;
    private int previewX;
    private int previewY;
    private PortEditorFactory portEditorFactory;
    private final Map<String, Component> portEditors = new HashMap<String, Component>();
    private GraphJson.UserDataCodec userDataCodec;
    private int pasteCount;

    // resizing a node by a corner: 0 top leading, 1 top trailing, 2 bottom leading, 3 bottom trailing
    private int pressCorner = -1;
    private double[] resizeFrom;

    // resizing a lane by its trailing (1) or bottom (2) edge, or moving it with its members
    private GraphGroup pressLane;
    private int laneAxis;
    private GraphGroup movingLane;
    private double[] laneFrom;

    // the routes the edges of moving nodes had, for undo
    private final List<GraphEdge> unrouted = new ArrayList<GraphEdge>();
    private final List<double[]> unroutedRoutes = new ArrayList<double[]>();

    // path buffers
    private double[] pathX = new double[64];
    private double[] pathY = new double[64];
    private final double[] rectBuf = new double[4];
    private final double[] rectBuf2 = new double[4];

    /// An empty graph backed by a new `DefaultGraphModel`.
    public NodeGraph() {
        this(new DefaultGraphModel());
    }

    /// A graph showing `model`.
    public NodeGraph(GraphModel model) {
        super(new com.codename1.ui.layouts.LayeredLayout());
        setUIID("NodeGraph");
        setScrollable(false);
        layer.setUIID("Container");
        layer.getAllStyles().setBgTransparency(0);
        add(canvas);
        add(layer);
        setModel(model);
    }

    // ------------------------------------------------------------------ model

    /// The model shown.
    public GraphModel getModel() {
        return model;
    }

    /// Shows another model, and clears the selection. An edit under way ends,
    /// and the undo history is cleared: its steps belong to the model that
    /// was shown.
    public void setModel(GraphModel model) {
        if (this.model != null) {
            // what was under way belongs to the model that is leaving
            endLabelEdit();
            cancelGesture();
            this.model.removeChangeListener(modelListener);
        }
        animTo = null;
        animFrom = null;
        pendingTemplate = null;
        // its steps would edit a model that is no longer shown
        if (undoHistory != null) {
            undoHistory.clear();
        }
        this.model = model == null ? new DefaultGraphModel() : model;
        this.model.addChangeListener(modelListener);
        selection.clear();
        selectedEdge = null;
        components.clear();
        portEditors.clear();
        layer.removeAll();
        viewSet = false;
        modelChanged();
    }

    private void modelChanged() {
        groupsDirty = true;
        connectedPorts.clear();
        for (int i = selection.size() - 1; i >= 0; i--) {
            if (model.getNode(selection.get(i).getId()) != selection.get(i)) { // NOPMD CompareObjectsWithEquals - the same object is meant
                selection.remove(i);
            }
        }
        if (selectedEdge != null && !model.getEdges().contains(selectedEdge)) {
            selectedEdge = null;
        }
        if (!components.isEmpty()) {
            List<String> gone = new ArrayList<String>();
            for (String id : components.keySet()) {
                if (model.getNode(id) == null) {
                    gone.add(id);
                }
            }
            for (String id : gone) {
                Component c = components.remove(id);
                if (c != null && c.getParent() != null) {
                    layer.removeComponent(c);
                }
            }
        }
        if (!portEditors.isEmpty()) {
            List<String> gone = new ArrayList<String>();
            for (Map.Entry<String, Component> en : portEditors.entrySet()) {
                Component c = en.getValue();
                String id = c == null ? null : (String) c.getClientProperty("cn1$graphNode");
                if (id != null && model.getNode(id) == null) {
                    gone.add(en.getKey());
                }
            }
            for (String key : gone) {
                Component c = portEditors.remove(key);
                if (c != null && c.getParent() != null) {
                    layer.removeComponent(c);
                }
            }
        }
        syncComponents();
        repaint();
    }

    /// Paints nodes. Never null.
    public GraphNodeRenderer getNodeRenderer() {
        return nodeRenderer;
    }

    /// Replaces the node painter; null restores the default.
    public void setNodeRenderer(GraphNodeRenderer r) {
        nodeRenderer = r == null ? new DefaultGraphNodeRenderer() : r;
        repaint();
    }

    /// Paints edges. Never null.
    public GraphEdgeRenderer getEdgeRenderer() {
        return edgeRenderer;
    }

    /// Replaces the edge painter; null restores the default.
    public void setEdgeRenderer(GraphEdgeRenderer r) {
        edgeRenderer = r == null ? new DefaultGraphEdgeRenderer() : r;
        repaint();
    }

    /// Adds a listener.
    public void addGraphListener(GraphListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /// Removes a listener.
    public void removeGraphListener(GraphListener l) {
        listeners.remove(l);
    }

    // --------------------------------------------------------------- settings

    /// True, the default, when the user may move, connect and remove.
    public boolean isEditingEnabled() {
        return editable;
    }

    /// Allows or forbids edits; panning, zooming and selecting stay.
    public void setEditingEnabled(boolean editable) {
        this.editable = editable;
        repaint();
    }

    /// One of `EDGE_AUTO`, the default, `EDGE_STRAIGHT`, `EDGE_ORTHOGONAL`
    /// or `EDGE_CURVED`.
    public int getEdgeStyle() {
        return edgeStyle;
    }

    /// Sets how edges are drawn.
    public void setEdgeStyle(int edgeStyle) {
        this.edgeStyle = edgeStyle;
        repaint();
    }

    /// True, the default, when a dot grid is painted behind the graph.
    public boolean isGridVisible() {
        return gridVisible;
    }

    /// Shows or hides the dot grid.
    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
        repaint();
    }

    /// The step dragged nodes snap to in world units, or 0 for none.
    public double getSnap() {
        return snap;
    }

    /// Snaps dragged nodes to a step; 0 turns it off.
    public void setSnap(double snap) {
        this.snap = Math.max(0, snap);
    }

    /// True when a minimap of the whole graph is painted in a corner.
    public boolean isMinimapVisible() {
        return minimapVisible;
    }

    /// Shows or hides the minimap.
    public void setMinimapVisible(boolean minimapVisible) {
        this.minimapVisible = minimapVisible;
        repaint();
    }

    /// True when a double tap edits the node's label in place.
    public boolean isLabelEditing() {
        return labelEditing;
    }

    /// Lets a double tap edit a node's label in place.
    public void setLabelEditing(boolean labelEditing) {
        this.labelEditing = labelEditing;
    }

    /// The zoom below which nodes and edges are drawn without text.
    public float getDetailZoom() {
        return detailZoom;
    }

    /// Sets the zoom below which text is not drawn.
    public void setDetailZoom(float detailZoom) {
        this.detailZoom = detailZoom;
        repaint();
    }

    /// The zoom below which nodes show no component from the factory.
    public float getComponentZoom() {
        return componentZoom;
    }

    /// Sets the zoom below which components are hidden.
    public void setComponentZoom(float componentZoom) {
        this.componentZoom = componentZoom;
        syncComponents();
    }

    /// Makes components for node bodies; null for none.
    public void setNodeComponentFactory(NodeComponentFactory f) {
        componentFactory = f;
        components.clear();
        layer.removeAll();
        syncComponents();
    }

    /// The factory, or null.
    public NodeComponentFactory getNodeComponentFactory() {
        return componentFactory;
    }

    /// The component made for a node, or null when none was made yet.
    public Component getNodeComponent(GraphNode node) {
        return components.get(node.getId());
    }

    /// Limits the zoom.
    public void setZoomRange(float min, float max) {
        minZoom = Math.max(0.01f, Math.min(min, max));
        maxZoom = Math.max(minZoom, max);
        setZoom(getZoom());
    }

    // ------------------------------------------------------------------- view

    /// The current zoom. At zoom 1 a node is drawn at its world size scaled
    /// to the theme font, so labels fit on dense screens as they do on a
    /// desktop; `fitToView` never goes past it.
    public float getZoom() {
        return scale / unit();
    }

    /// Zooms about the centre of the view.
    public void setZoom(float z) {
        zoomAt(z * unit() / scale, canvas.getWidth() / 2, canvas.getHeight() / 2);
    }

    /// The world x at the left edge of the view.
    public double getViewX() {
        return viewX;
    }

    /// The world y at the top edge of the view.
    public double getViewY() {
        return viewY;
    }

    /// Scrolls so the world point is at the top left corner of the view.
    public void setView(double worldX, double worldY) {
        viewX = worldX;
        viewY = worldY;
        viewSet = true;
        syncComponents();
        repaint();
    }

    /// Scrolls so the node is in the middle of the view.
    public void centerOn(GraphNode node) {
        double[] r = visibleRect(node, rectBuf);
        if (r == null) {
            return;
        }
        setView(r[0] + r[2] / 2 - canvas.getWidth() / 2.0 / scale, r[1] + r[3] / 2 - canvas.getHeight() / 2.0 / scale);
    }

    /// Zooms and scrolls so the whole graph fits the view, never past zoom 1.
    public void fitToView() {
        double[] b = contentBounds();
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        if (b == null || w <= 0 || h <= 0) {
            viewSet = false;
            return;
        }
        int margin = GraphLook.mm(4);
        double bw = Math.max(1, b[2] - b[0]);
        double bh = Math.max(1, b[3] - b[1]);
        float z = (float) Math.min((w - margin * 2) / bw, (h - margin * 2) / bh);
        scale = clampZoom(Math.min(unit(), z));
        viewX = (b[0] + b[2]) / 2 - w / 2.0 / scale;
        viewY = (b[1] + b[3]) / 2 - h / 2.0 / scale;
        viewSet = true;
        syncComponents();
        repaint();
    }

    private float clampZoom(float z) {
        float u = unit();
        return Math.max(minZoom * u, Math.min(maxZoom * u, z));
    }

    // Screen pixels per world unit at zoom 1. World units are sized for a
    // theme font about 20 pixels high; on a dense screen the font is two or
    // three times that, and a node that stayed 160 pixels wide would cut its
    // label to a letter or two. Never below 1, so a desktop draws as it did.
    private static float unit() {
        return GraphLook.unit();
    }

    /// Multiplies the zoom by `factor` keeping the world point under the
    /// screen point where it is.
    ///
    /// #### Returns
    ///
    /// false when the zoom was already at its limit
    public boolean zoomAt(float factor, int screenX, int screenY) {
        float nz = clampZoom(scale * factor);
        if (Math.abs(nz - scale) < 1e-6f) {
            return false;
        }
        double wx = screenToWorldX(screenX);
        double wy = screenToWorldY(screenY);
        scale = nz;
        viewX = wx - screenX / (double) scale;
        viewY = wy - screenY / (double) scale;
        syncComponents();
        repaint();
        return true;
    }

    /// A world x as a pixel from the left of the view.
    public int worldToScreenX(double wx) {
        return (int) Math.round((wx - viewX) * scale);
    }

    /// A world y as a pixel from the top of the view.
    public int worldToScreenY(double wy) {
        return (int) Math.round((wy - viewY) * scale);
    }

    /// A world length in pixels.
    public int worldToScreenLength(double len) {
        return (int) Math.round(len * scale);
    }

    /// A pixel from the left of the view as a world x.
    public double screenToWorldX(int sx) {
        return viewX + sx / (double) scale;
    }

    /// A pixel from the top of the view as a world y.
    public double screenToWorldY(int sy) {
        return viewY + sy / (double) scale;
    }

    int headerHeightOnScreen() {
        return worldToScreenLength(HEADER);
    }

    int portRadiusOnScreen() {
        return Math.max(3, Math.round(GraphLook.mm(1.1f) * Math.min(1.4f, Math.max(0.5f, getZoom()))));
    }

    // -------------------------------------------------------------- selection

    /// The selected nodes, in the order they were selected. A copy.
    public List<GraphNode> getSelectedNodes() {
        return new ArrayList<GraphNode>(selection);
    }

    /// The selected edge, or null.
    public GraphEdge getSelectedEdge() {
        return selectedEdge;
    }

    /// True when the node is selected.
    public boolean isSelected(GraphNode node) {
        return selection.contains(node);
    }

    /// Selects one node alone, or clears the selection with null.
    public void select(GraphNode node) {
        selection.clear();
        selectedEdge = null;
        if (node != null) {
            selection.add(node);
        }
        fireSelection();
    }

    /// Selects one edge alone.
    public void selectEdge(GraphEdge edge) {
        selection.clear();
        selectedEdge = edge;
        fireSelection();
    }

    /// Adds a node to the selection, or takes it out when it is in.
    public void toggleSelection(GraphNode node) {
        selectedEdge = null;
        if (!selection.remove(node)) {
            selection.add(node);
        }
        fireSelection();
    }

    private void fireSelection() {
        repaint();
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            listeners.get(i).selectionChanged(this);
        }
    }

    /// Removes the selected nodes and edge, each offered to the listeners
    /// and then to the model.
    public void deleteSelection() {
        if (!editable) {
            return;
        }
        List<GraphNode> goneNodes = new ArrayList<GraphNode>();
        List<GraphEdge> goneEdges = new ArrayList<GraphEdge>();
        List<GraphNode> nodesBefore = new ArrayList<GraphNode>(model.getNodes());
        List<GraphEdge> edgesBefore = new ArrayList<GraphEdge>(model.getEdges());
        if (selectedEdge != null) {
            GraphEdge e = selectedEdge;
            if (allowRemove(null, e) && model.removeEdge(e)) {
                goneEdges.add(e);
            }
        }
        List<GraphNode> nodes = new ArrayList<GraphNode>(selection);
        for (GraphNode n : nodes) {
            if (!allowRemove(n, null)) {
                continue;
            }
            // the model drops the edges of a removed node; undo puts them back
            List<GraphEdge> touching = new ArrayList<GraphEdge>();
            for (GraphEdge e : model.getEdges()) {
                if (e.touches(n.getId())) {
                    touching.add(e);
                }
            }
            if (model.removeNode(n)) {
                goneNodes.add(n);
                for (GraphEdge e : touching) {
                    // an edge between two removed nodes, or the selected one, once
                    if (!goneEdges.contains(e)) {
                        goneEdges.add(e);
                    }
                }
            }
        }
        if (!goneNodes.isEmpty() || !goneEdges.isEmpty()) {
            record(new GraphEdits.Presence(model, goneNodes, goneEdges, true, "Delete", nodesBefore, edgesBefore));
        }
        selection.clear();
        selectedEdge = null;
        fireSelection();
    }

    private boolean allowRemove(GraphNode n, GraphEdge e) {
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            if (!listeners.get(i).removing(this, n, e)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------ arrangement

    /// Places the nodes with an arrangement, gliding them there when `animate` is
    /// true. Arrangements are not user edits: a model that refuses moves is
    /// placed anyway, the nodes moved directly.
    public void arrange(GraphArrangement arrangement, boolean animate) {
        stopLive();
        finishAnimation();
        ArrangementResult r = arrangement.arrange(model);
        recordArrange(r);
        if (!animate || canvas.getWidth() <= 0) {
            commit(r);
            if (!viewSet || canvas.getWidth() <= 0) {
                viewSet = false;
            } else {
                fitToView();
            }
            return;
        }
        animFrom = new HashMap<String, double[]>();
        for (GraphNode n : model.getNodes()) {
            animFrom.put(n.getId(), new double[]{n.getX(), n.getY()});
        }
        animTo = r;
        animStart = System.currentTimeMillis();
        for (GraphEdge e : model.getEdges()) {
            // a route belongs to the old positions; edges go straight while moving
            e.setRoute(null);
        }
        animateNow();
    }

    private void commit(ArrangementResult r) {
        for (GraphNode n : model.getNodes()) {
            double[] p = r.getPosition(n.getId());
            if (p != null && (p[0] != n.getX() || p[1] != n.getY()) && !model.moveNode(n, p[0], p[1])) {
                n.setPosition(p[0], p[1]);
            }
        }
        for (GraphEdge e : model.getEdges()) {
            e.setRoute(r.getRoute(e.getId()));
        }
        groupsDirty = true;
        syncComponents();
        repaint();
    }

    /// Runs a force arrangement live: the graph settles a step a frame, and
    /// follows a node while it is dragged. Null stops it.
    public void setLiveArrangement(ForceArrangement arrangement) {
        stopLive();
        live = arrangement;
        if (arrangement != null) {
            for (GraphEdge e : model.getEdges()) {
                e.setRoute(null);
            }
            arrangement.start(model);
            liveRunning = true;
            animateNow();
        }
    }

    private void stopLive() {
        if (live != null && liveRunning) {
            liveRunning = false;
            commit(live.result());
        }
        live = null;
        liveRunning = false;
    }

    // ------------------------------------------------------------- label edit

    /// Edits a node's label in place, in a text field over the node; the new
    /// label goes to `GraphModel.renameNode` when editing ends.
    public void startLabelEdit(final GraphNode node) {
        endLabelEdit();
        final TextField tf = new TextField(node.getLabel());
        tf.putClientProperty("cn1$graphNode", node.getId());
        tf.putClientProperty("cn1$graphLabel", Boolean.TRUE);
        labelField = tf;
        ActionListener<ActionEvent> done = new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                endLabelEdit();
            }
        };
        tf.addActionListener(done);
        tf.setDoneListener(done);
        tf.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(Component cmp) {
            }

            @Override
            public void focusLost(Component cmp) {
                endLabelEdit();
            }
        });
        layer.add(tf);
        layer.layoutContainer();
        repaint();
        tf.startEditingAsync();
    }

    private void endLabelEdit() {
        TextField tf = labelField;
        if (tf == null) {
            return;
        }
        labelField = null;
        String id = (String) tf.getClientProperty("cn1$graphNode");
        GraphNode node = model.getNode(id);
        String text = tf.getText();
        if (tf.getParent() != null) {
            layer.removeComponent(tf);
        }
        if (node != null && text != null && !text.equals(node.getLabel())) {
            String old = node.getLabel();
            if (model.renameNode(node, text)) {
                record(new GraphEdits.Rename(model, node, old, text));
            }
        }
        repaint();
    }

    // ------------------------------------------------------------------- undo

    /// The history edits are recorded in, or null.
    public UndoHistory getUndoHistory() {
        return undoHistory;
    }

    /// Records every edit the user makes in a history, so it can be undone.
    /// One history may serve several components. Null, the default, records
    /// nothing.
    ///
    /// What is recorded is what the user did through this view: moving,
    /// resizing, connecting, removing, renaming, adding, pasting, collapsing
    /// a group and `arrange`. Changes the application makes to the model
    /// directly are not.
    public void setUndoHistory(UndoHistory undoHistory) {
        this.undoHistory = undoHistory;
    }

    /// Takes back the last edit in the history. Prefer this to calling the
    /// history yourself: it first lets a running arrangement land.
    ///
    /// #### Returns
    ///
    /// false when there is no history or nothing to undo
    public boolean undo() {
        if (undoHistory == null) {
            return false;
        }
        // what is held would be let go from where it no longer is
        cancelGesture();
        finishAnimation();
        endLabelEdit();
        return undoHistory.undo();
    }

    /// Makes the last undone edit again.
    ///
    /// #### Returns
    ///
    /// false when there is no history or nothing to redo
    public boolean redo() {
        if (undoHistory == null) {
            return false;
        }
        // what is held would be let go from where it no longer is
        cancelGesture();
        finishAnimation();
        endLabelEdit();
        return undoHistory.redo();
    }

    private void record(UndoableEdit edit) {
        if (undoHistory != null) {
            undoHistory.record(edit);
        }
    }

    private void beginStep(String name) {
        if (undoHistory != null) {
            undoHistory.beginCompound(name);
        }
    }

    private void endStep() {
        if (undoHistory != null) {
            undoHistory.endCompound();
        }
    }

    /// Lands an arrangement that is still gliding.
    private void finishAnimation() {
        if (animTo != null) {
            ArrangementResult r = animTo;
            animTo = null;
            animFrom = null;
            commit(r);
        }
    }

    private void recordArrange(ArrangementResult r) {
        if (undoHistory == null) {
            return;
        }
        List<GraphNode> nodes = new ArrayList<GraphNode>(model.getNodes());
        double[] from = new double[nodes.size() * 2];
        double[] to = new double[nodes.size() * 2];
        for (int i = 0; i < nodes.size(); i++) {
            GraphNode n = nodes.get(i);
            double[] p = r.getPosition(n.getId());
            from[i * 2] = n.getX();
            from[i * 2 + 1] = n.getY();
            to[i * 2] = p == null ? n.getX() : p[0];
            to[i * 2 + 1] = p == null ? n.getY() : p[1];
        }
        List<GraphEdge> edges = new ArrayList<GraphEdge>(model.getEdges());
        List<double[]> routesFrom = new ArrayList<double[]>();
        List<double[]> routesTo = new ArrayList<double[]>();
        for (GraphEdge ge : edges) {
            routesFrom.add(ge.getRoute());
            routesTo.add(r.getRoute(ge.getId()));
        }
        record(new GraphEdits.Arrange(model, nodes, from, to, edges, routesFrom, routesTo));
    }

    // --------------------------------------------------------------- resizing

    /// True when a selected node shows handles on its corners to resize it by.
    public boolean isResizable() {
        return resizable;
    }

    /// Lets the user resize a node by dragging a corner of it while it is the
    /// only node selected. Off by default.
    public void setResizable(boolean resizable) {
        this.resizable = resizable;
        repaint();
    }

    private double snapped(double v) {
        return snap > 0 ? Math.round(v / snap) * snap : v;
    }

    private int cornerAt(int sx, int sy) {
        if (!resizable || !editable || selection.size() != 1) {
            return -1;
        }
        GraphNode n = selection.get(0);
        if (!isNodeVisible(n)) {
            return -1;
        }
        int reach = GraphLook.mm(1.8f);
        for (int c = 0; c < 4; c++) {
            int cx = worldToScreenX(n.getX() + ((c & 1) != 0 ? n.getWidth() : 0));
            int cy = worldToScreenY(n.getY() + (c >= 2 ? n.getHeight() : 0));
            if (Math.abs(cx - sx) <= reach && Math.abs(cy - sy) <= reach) {
                return c;
            }
        }
        return -1;
    }

    private void paintResizeHandles(Graphics g, GraphNode n, int color) {
        int r = Math.max(3, GraphLook.mm(0.8f));
        int bg = GraphLook.bg(GraphLook.style("NodeGraphHandle"), 0xffffff);
        for (int c = 0; c < 4; c++) {
            int cx = worldToScreenX(n.getX() + ((c & 1) != 0 ? n.getWidth() : 0));
            int cy = worldToScreenY(n.getY() + (c >= 2 ? n.getHeight() : 0));
            g.setColor(bg);
            g.fillRect(cx - r, cy - r, r * 2, r * 2);
            g.setColor(color);
            g.drawRect(cx - r, cy - r, r * 2 - 1, r * 2 - 1);
        }
    }

    private void resizeTo(int x, int y) {
        GraphNode n = pressNode;
        if (n == null || resizeFrom == null) {
            return;
        }
        double wx = snapped(screenToWorldX(x));
        double wy = snapped(screenToWorldY(y));
        double left = resizeFrom[0];
        double top = resizeFrom[1];
        double right = left + resizeFrom[2];
        double bottom = top + resizeFrom[3];
        if (pressCorner % 2 == 0) {
            left = Math.min(wx, right - MIN_NODE_W);
        } else {
            right = Math.max(wx, left + MIN_NODE_W);
        }
        if (pressCorner < 2) {
            top = Math.min(wy, bottom - MIN_NODE_H);
        } else {
            bottom = Math.max(wy, top + MIN_NODE_H);
        }
        n.setPosition(left, top);
        n.setSize(right - left, bottom - top);
        groupsDirty = true;
        syncComponents();
    }

    private void finishResize() {
        GraphNode n = pressNode;
        double[] from = resizeFrom;
        resizeFrom = null;
        if (n == null || from == null) {
            return;
        }
        double[] to = {n.getX(), n.getY(), n.getWidth(), n.getHeight()};
        n.setPosition(from[0], from[1]);
        n.setSize(from[2], from[3]);
        groupsDirty = true;
        boolean ok = to[0] != from[0] || to[1] != from[1] || to[2] != from[2] || to[3] != from[3];
        for (int l = 0; l < listeners.size() && ok; l++) {
            ok = listeners.get(l).nodeResizing(this, n, to[2], to[3]);
        }
        if (ok && model.resizeNode(n, to[2], to[3])) {
            if (to[0] != from[0] || to[1] != from[1]) {
                // a model that pins the node still lets it grow, from where it is
                model.moveNode(n, to[0], to[1]);
            }
            double[] now = {n.getX(), n.getY(), n.getWidth(), n.getHeight()};
            record(new GraphEdits.Resize(model, n, from, now));
            for (int l = 0; l < listeners.size(); l++) { // NOPMD ForLoopCanBeForeach - indexed, as the rest of the class
                listeners.get(l).nodeResized(this, n);
            }
        }
        syncComponents();
    }

    // ------------------------------------------------------------------ lanes

    /// The lane under a world point, or null. Later lanes are over earlier
    /// ones.
    public GraphGroup laneAt(double worldX, double worldY) {
        List<GraphGroup> groups = model.getGroups();
        for (int i = groups.size() - 1; i >= 0; i--) {
            GraphGroup g = groups.get(i);
            if (g.isLane() && g.contains(worldX, worldY)) {
                return g;
            }
        }
        return null;
    }

    /// Finds the lane whose trailing or bottom edge is at a point of the view
    /// and keeps it as the pressed lane.
    ///
    /// #### Returns
    ///
    /// 1 for the trailing edge, 2 for the bottom edge, 0 for neither
    private int laneEdgeAt(int sx, int sy) {
        if (!editable) {
            return 0;
        }
        double wx = screenToWorldX(sx);
        double wy = screenToWorldY(sy);
        double tol = GraphLook.mm(1.5f) / (double) scale;
        List<GraphGroup> groups = model.getGroups();
        for (int i = groups.size() - 1; i >= 0; i--) {
            GraphGroup g = groups.get(i);
            if (!g.isLane()) {
                continue;
            }
            boolean inX = wx >= g.getX() - tol && wx <= g.getX() + g.getWidth() + tol;
            boolean inY = wy >= g.getY() - tol && wy <= g.getY() + g.getHeight() + tol;
            if (inY && Math.abs(wx - (g.getX() + g.getWidth())) <= tol) {
                pressLane = g;
                return 1;
            }
            if (inX && Math.abs(wy - (g.getY() + g.getHeight())) <= tol) {
                pressLane = g;
                return 2;
            }
        }
        return 0;
    }

    private void laneResizeTo(int x, int y) {
        GraphGroup lane = pressLane;
        if (lane == null || laneFrom == null) {
            return;
        }
        double w = laneFrom[2];
        double h = laneFrom[3];
        if (laneAxis == 1) {
            w = Math.max(MIN_LANE, snapped(screenToWorldX(x)) - laneFrom[0]);
        } else {
            h = Math.max(MIN_LANE, snapped(screenToWorldY(y)) - laneFrom[1]);
        }
        lane.setBounds(laneFrom[0], laneFrom[1], w, h);
    }

    private void finishLane() {
        GraphGroup lane = pressLane;
        double[] from = laneFrom;
        pressLane = null;
        laneFrom = null;
        if (lane == null || from == null) {
            return;
        }
        double[] to = {lane.getX(), lane.getY(), lane.getWidth(), lane.getHeight()};
        lane.setBounds(from[0], from[1], from[2], from[3]);
        if ((to[2] != from[2] || to[3] != from[3]) && model.resizeGroup(lane, to[0], to[1], to[2], to[3])) {
            record(new GraphEdits.LaneBounds(model, lane, from, to));
        }
    }

    /// A node that was dropped belongs to the lane its centre is in. A node
    /// in an ordinary group stays in it.
    private void joinLane(GraphNode n) {
        String from = n.getGroupId();
        GraphGroup current = from == null ? null : model.getGroup(from);
        if (current != null && !current.isLane()) {
            return;
        }
        GraphGroup at = laneAt(n.getCenterX(), n.getCenterY());
        String to = at == null ? null : at.getId();
        if (to == null ? from == null : to.equals(from)) {
            return;
        }
        if (model.setNodeGroup(n, to)) {
            record(new GraphEdits.Membership(model, n, from, to));
        }
    }

    private void paintLane(Graphics g, GraphGroup grp, double[] r) {
        Style s = GraphLook.style("NodeGraphLane");
        Style hs = GraphLook.style("NodeGraphLaneHeader");
        int x = worldToScreenX(r[0]);
        int y = worldToScreenY(r[1]);
        int w = Math.max(1, worldToScreenX(r[0] + r[2]) - x);
        int h = Math.max(1, worldToScreenY(r[1] + r[3]) - y);
        int a = g.getAlpha();
        int bg = GraphLook.bg(s, 0xffffff);
        g.setColor(bg);
        g.setAlpha(a * Math.max(40, s.getBgTransparency() & 0xff) / 255);
        g.fillRect(x, y, w, h);
        g.setAlpha(a);
        int band = worldToScreenLength(LANE_HEADER);
        boolean across = grp.isHorizontal();
        int bw = across ? Math.min(band, w) : w;
        int bh = across ? h : Math.min(band, h);
        g.setColor(GraphLook.bg(hs, GraphLook.mix(bg, hs.getFgColor(), 30)));
        g.fillRect(x, y, bw, bh);
        g.setColor(GraphLook.mix(bg, s.getFgColor(), 90));
        g.drawRect(x, y, w - 1, h - 1);
        if (across) {
            g.drawLine(x + bw, y, x + bw, y + h - 1);
        } else {
            g.drawLine(x, y + bh, x + w - 1, y + bh);
        }
        String label = grp.getLabel();
        if (getZoom() < detailZoom || label == null || label.length() == 0) {
            return;
        }
        Font f = GraphLook.font("NodeGraphLaneHeader", getZoom());
        g.setFont(f);
        g.setColor(hs.getFgColor());
        int pad = GraphLook.mm(1.5f, getZoom());
        if (!across) {
            String text = GraphLook.fit(label, f, w - pad * 2);
            g.drawString(text, x + (w - f.stringWidth(text)) / 2, y + (bh - f.getHeight()) / 2);
        } else if (g.isAffineSupported()) {
            // the title reads up the band, as on a printed swimlane chart
            String text = GraphLook.fit(label, f, h - pad * 2);
            int cx = x + bw / 2;
            int cy = y + h / 2;
            int px = cx + g.getTranslateX();
            int py = cy + g.getTranslateY();
            g.rotateRadians((float) (-Math.PI / 2), px, py);
            g.drawString(text, cx - f.stringWidth(text) / 2, cy - f.getHeight() / 2);
            g.rotateRadians((float) (Math.PI / 2), px, py);
        } else {
            g.drawString(GraphLook.fit(label, f, bw - 2), x + 1, y + pad);
        }
    }

    // --------------------------------------------------- adding and clipboard

    /// The template the next tap on the background makes a node from, or
    /// null.
    public GraphNodeTemplate getPendingTemplate() {
        return pendingTemplate;
    }

    /// Arms a template: the next tap on the background adds a node of it
    /// there. A `GraphPalette` does this when one of its items is tapped,
    /// which is how a node is added without a drag.
    public void setPendingTemplate(GraphNodeTemplate template) {
        pendingTemplate = template;
    }

    /// Adds a node made from a template, centred on a world point and
    /// selected. The node is offered to the listeners and then to the model,
    /// and joins the lane it lands in.
    ///
    /// #### Returns
    ///
    /// the node, or null when it was refused
    public GraphNode createNode(GraphNodeTemplate template, double worldX, double worldY) {
        if (template == null || !editable) {
            return null;
        }
        GraphNode n = template.create(freeNodeId("n", null));
        n.setPosition(snapped(worldX - n.getWidth() / 2), snapped(worldY - n.getHeight() / 2));
        GraphGroup lane = laneAt(n.getCenterX(), n.getCenterY());
        if (lane != null) {
            n.setGroupId(lane.getId());
        }
        List<GraphNode> one = new ArrayList<GraphNode>();
        one.add(n);
        return addNodes(one, new ArrayList<GraphEdge>(), "Add").isEmpty() ? null : n;
    }

    private int idCounter;

    private String freeNodeId(String prefix, Set<String> taken) {
        String id;
        do {
            idCounter++;
            id = prefix + idCounter;
        } while (model.getNode(id) != null || taken != null && taken.contains(id));
        return id;
    }

    /// Offers new nodes to the listeners and the model, adds the edges whose
    /// two ends both made it, and selects what was added.
    private List<GraphNode> addNodes(List<GraphNode> nodes, List<GraphEdge> edges, String name) {
        List<GraphNode> in = new ArrayList<GraphNode>();
        Set<String> ids = new HashSet<String>();
        for (GraphNode n : nodes) {
            boolean ok = true;
            for (int l = 0; l < listeners.size() && ok; l++) {
                ok = listeners.get(l).nodeCreating(this, n);
            }
            if (ok && model.insertNode(n, -1)) {
                in.add(n);
                ids.add(n.getId());
            }
        }
        List<GraphEdge> inEdges = new ArrayList<GraphEdge>();
        for (GraphEdge e : edges) {
            if (ids.contains(e.getSourceId()) && ids.contains(e.getTargetId()) && model.insertEdge(e, -1)) {
                inEdges.add(e);
            }
        }
        if (in.isEmpty()) {
            return in;
        }
        record(new GraphEdits.Presence(model, in, inEdges, false, name));
        for (GraphNode created : in) {
            for (int l = 0; l < listeners.size(); l++) { // NOPMD ForLoopCanBeForeach - indexed, as the rest of the class
                listeners.get(l).nodeCreated(this, created);
            }
        }
        selection.clear();
        selection.addAll(in);
        selectedEdge = null;
        fireSelection();
        return in;
    }

    /// Adds copies of nodes, and of the edges among them, under new ids and
    /// moved by an offset.
    private List<GraphNode> insertCopies(List<GraphNode> srcNodes, List<GraphEdge> srcEdges,
                                         double dx, double dy, String name) {
        Map<String, String> ids = new HashMap<String, String>();
        Set<String> taken = new HashSet<String>();
        List<GraphNode> nodes = new ArrayList<GraphNode>();
        for (GraphNode n : srcNodes) {
            String id = freeNodeId("n", taken);
            taken.add(id);
            ids.put(n.getId(), id);
            GraphNode c = n.copy(id);
            c.setPosition(n.getX() + dx, n.getY() + dy);
            if (c.getGroupId() != null && model.getGroup(c.getGroupId()) == null) {
                c.setGroupId(null);
            }
            nodes.add(c);
        }
        Set<String> edgeIds = new HashSet<String>();
        for (GraphEdge e : model.getEdges()) {
            edgeIds.add(e.getId());
        }
        List<GraphEdge> edges = new ArrayList<GraphEdge>();
        for (GraphEdge e : srcEdges) {
            String s = ids.get(e.getSourceId());
            String t = ids.get(e.getTargetId());
            if (s == null || t == null) {
                continue;
            }
            String id;
            do {
                idCounter++;
                id = "e" + idCounter;
            } while (!edgeIds.add(id));
            edges.add(e.copy(id, s, t));
        }
        return addNodes(nodes, edges, name);
    }

    /// Adds a copy of every selected node, and of the edges among them, a
    /// little down and to the trailing side, and selects the copies.
    ///
    /// #### Returns
    ///
    /// false when nothing was selected or every copy was refused
    public boolean duplicateSelection() {
        if (!editable || selection.isEmpty()) {
            return false;
        }
        List<GraphNode> nodes = new ArrayList<GraphNode>(selection);
        return !insertCopies(nodes, new ArrayList<GraphEdge>(model.getEdges()), PASTE_STEP, PASTE_STEP,
                "Duplicate").isEmpty();
    }

    /// Writes the user data of copied nodes to the clipboard and reads it
    /// back on paste; without one, pasted nodes have no user data.
    public void setUserDataCodec(GraphJson.UserDataCodec codec) {
        userDataCodec = codec;
    }

    /// Puts the selected nodes, and the edges among them, on the clipboard,
    /// as the JSON `GraphJson` writes.
    ///
    /// #### Returns
    ///
    /// false when no node is selected
    public boolean copySelection() {
        if (selection.isEmpty()) {
            return false;
        }
        Map<String, Object> m = GraphJson.toMap(model, selection, userDataCodec);
        m.put("cn1graph", Integer.valueOf(1));
        Display.getInstance().copyToClipboard(JSONParser.toJson(m));
        pasteCount = 0;
        return true;
    }

    /// Copies the selection and removes it.
    ///
    /// #### Returns
    ///
    /// false when no node is selected or editing is off
    public boolean cutSelection() {
        if (!editable || !copySelection()) {
            return false;
        }
        deleteSelection();
        return true;
    }

    /// Adds the nodes on the clipboard, each time a step further from where
    /// they were copied, and selects them.
    ///
    /// #### Returns
    ///
    /// false when the clipboard holds no nodes or every one was refused
    public boolean paste() {
        if (!editable) {
            return false;
        }
        Object o = Display.getInstance().getPasteDataFromClipboard();
        if (!(o instanceof String)) {
            return false;
        }
        String text = (String) o;
        if (text.indexOf("\"cn1graph\"") < 0) {
            return false;
        }
        DefaultGraphModel read = new DefaultGraphModel();
        try {
            GraphJson.read(text, read, userDataCodec);
        } catch (IOException notAGraph) {
            return false;
        }
        pasteCount++;
        double d = PASTE_STEP * pasteCount;
        return !insertCopies(read.getNodes(), read.getEdges(), d, d, "Paste").isEmpty();
    }

    // Called by a palette while one of its items is dragged over the view.
    void showDropPreview(GraphNodeTemplate template, int absoluteX, int absoluteY) {
        previewTemplate = template;
        previewX = absoluteX - canvas.getAbsoluteX();
        previewY = absoluteY - canvas.getAbsoluteY();
        repaint();
    }

    void clearDropPreview() {
        if (previewTemplate != null) {
            previewTemplate = null;
            repaint();
        }
    }

    // A palette item was let go at a point of the screen; null when it was
    // outside the view or refused.
    GraphNode dropTemplate(GraphNodeTemplate template, int absoluteX, int absoluteY) {
        clearDropPreview();
        int sx = absoluteX - canvas.getAbsoluteX();
        int sy = absoluteY - canvas.getAbsoluteY();
        if (sx < 0 || sy < 0 || sx >= canvas.getWidth() || sy >= canvas.getHeight()) {
            return null;
        }
        return createNode(template, screenToWorldX(sx), screenToWorldY(sy));
    }

    private void paintPreview(Graphics g, int color) {
        GraphNodeTemplate t = previewTemplate;
        int w = worldToScreenLength(t.getWidth());
        int h = worldToScreenLength(t.getHeight());
        int x = previewX - w / 2;
        int y = previewY - h / 2;
        int a = g.getAlpha();
        g.setColor(color);
        g.setAlpha(a * 70 / 255);
        GraphShape shape = GraphShapeRegistry.get(t.getShape());
        if (shape != null && g.isShapeSupported()) {
            GeneralPath p = new GeneralPath();
            shape.outline(p, x, y, w, h);
            g.fillShape(p);
        } else {
            int arc = GraphLook.mm(1.6f);
            g.fillRoundRect(x, y, w, h, arc, arc);
        }
        g.setAlpha(a);
    }

    // ----------------------------------------------------------- port values

    /// Makes the editors of unconnected input ports; null for none.
    public void setPortEditorFactory(PortEditorFactory factory) {
        for (Component c : portEditors.values()) {
            if (c != null && c.getParent() != null) {
                layer.removeComponent(c);
            }
        }
        portEditors.clear();
        portEditorFactory = factory;
        syncComponents();
    }

    /// The factory of port editors, or null.
    public PortEditorFactory getPortEditorFactory() {
        return portEditorFactory;
    }

    /// The editor made for a port, or null when none was made yet.
    public Component getPortEditor(GraphNode node, GraphPort port) {
        return portEditors.get(node.getId() + "\n" + port.getId());
    }

    /// Sets the value an input port uses while nothing is connected to it,
    /// through the model, as an edit that can be undone. A port editor calls
    /// this when the user enters a value.
    ///
    /// #### Returns
    ///
    /// false when the model refused
    public boolean setPortValue(GraphPort port, Object value) {
        Object old = port.getValue();
        if (old == null ? value == null : old.equals(value)) {
            return true;
        }
        if (!model.setPortValue(port, value)) {
            return false;
        }
        record(new GraphEdits.PortValue(model, port, old, value));
        return true;
    }

    private boolean syncPortEditors(boolean show, int w, int h) {
        boolean changed = false;
        List<GraphNode> nodes = model.getNodes();
        for (int i = 0; i < nodes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            GraphNode n = nodes.get(i);
            List<GraphPort> ins = n.getInputs();
            if (ins.isEmpty()) {
                continue;
            }
            int x = worldToScreenX(n.getX());
            int y = worldToScreenY(n.getY());
            int nw = worldToScreenLength(n.getWidth());
            int nh = worldToScreenLength(n.getHeight());
            boolean onScreen = show && isNodeVisible(n) && x < w && y < h && x + nw > 0 && y + nh > 0;
            for (int k = 0; k < ins.size(); k++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                GraphPort p = ins.get(k);
                String key = n.getId() + "\n" + p.getId();
                Component c = portEditors.get(key);
                if (onScreen && !isPortConnected(n, p)) {
                    if (c == null) {
                        // a factory that had no editor for a port is asked again:
                        // its answer may depend on the value
                        c = portEditorFactory.createEditor(this, n, p);
                        if (c != null) {
                            portEditors.put(key, c);
                            c.putClientProperty("cn1$graphNode", n.getId());
                            c.putClientProperty("cn1$graphPort", p.getId());
                        }
                    }
                    if (c != null && c.getParent() == null) {
                        layer.add(c);
                        changed = true;
                    }
                } else if (c != null && c.getParent() != null) {
                    layer.removeComponent(c);
                    changed = true;
                }
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------- keys

    // A key reached the view; true when it was a command of the graph:
    // Delete or Backspace removes the selection, and with Ctrl or Cmd, C, X,
    // V and D copy, cut, paste and duplicate, Z undoes and Shift-Z or Y redoes.
    boolean keyTyped(int keyCode) {
        if (labelField != null) {
            return false;
        }
        if (keyCode == 127 || keyCode == 8) {
            if (!editable || selection.isEmpty() && selectedEdge == null) {
                return false;
            }
            deleteSelection();
            return true;
        }
        Display d = Display.getInstance();
        if (!d.isControlKeyDown() && !d.isMetaKeyDown()) {
            return false;
        }
        switch (keyCode) {
            case 'c':
            case 'C':
                return copySelection();
            case 'x':
            case 'X':
                return cutSelection();
            case 'v':
            case 'V':
                return paste();
            case 'd':
            case 'D':
                return duplicateSelection();
            case 'z':
            case 'Z':
                return d.isShiftKeyDown() ? redo() : undo();
            case 'y':
            case 'Y':
                return redo();
            default:
                return false;
        }
    }

    // --------------------------------------------------------------- geometry

    /// The world x of a port's anchor: inputs on the leading edge, outputs on
    /// the trailing one.
    public double portX(GraphNode node, GraphPort port) {
        return port.isInput() ? node.getX() : node.getX() + node.getWidth();
    }

    /// The world y of a port's anchor: the ports of one side share the body
    /// under the title evenly.
    public double portY(GraphNode node, GraphPort port) {
        List<GraphPort> side = port.isInput() ? node.getInputs() : node.getOutputs();
        int i = side.indexOf(port);
        int n = Math.max(1, side.size());
        double body = Math.max(1, node.getHeight() - HEADER);
        return node.getY() + HEADER + body * (i + 0.5) / n;
    }

    boolean isPortConnected(GraphNode node, GraphPort port) {
        if (connectedPorts.isEmpty()) {
            List<GraphEdge> edges = model.getEdges();
            connectedPorts.put("", Boolean.TRUE);
            for (int i = 0; i < edges.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                GraphEdge e = edges.get(i);
                if (e.getSourcePortId() != null) {
                    connectedPorts.put(e.getSourceId() + "\n>" + e.getSourcePortId(), Boolean.TRUE);
                }
                if (e.getTargetPortId() != null) {
                    connectedPorts.put(e.getTargetId() + "\n<" + e.getTargetPortId(), Boolean.TRUE);
                }
            }
        }
        return connectedPorts.containsKey(node.getId() + (port.isInput() ? "\n<" : "\n>") + port.getId());
    }

    private void refreshGroups() {
        if (!groupsDirty) {
            return;
        }
        groupsDirty = false;
        groupBounds.clear();
        List<GraphNode> nodes = model.getNodes();
        for (int i = 0; i < nodes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            GraphNode n = nodes.get(i);
            String gid = n.getGroupId();
            if (gid == null) {
                continue;
            }
            double[] b = groupBounds.get(gid);
            if (b == null) {
                groupBounds.put(gid, new double[]{n.getX(), n.getY(), n.getX() + n.getWidth(), n.getY() + n.getHeight()});
            } else {
                b[0] = Math.min(b[0], n.getX());
                b[1] = Math.min(b[1], n.getY());
                b[2] = Math.max(b[2], n.getX() + n.getWidth());
                b[3] = Math.max(b[3], n.getY() + n.getHeight());
            }
        }
    }

    /// A group's frame in world units, {x, y, width, height}, or null for a
    /// group with no members. A collapsed group's frame is its node, and a
    /// lane's is its own bounds.
    public double[] getGroupRect(GraphGroup group) {
        if (group.isLane()) {
            return new double[]{group.getX(), group.getY(), group.getWidth(), group.getHeight()};
        }
        refreshGroups();
        double[] b = groupBounds.get(group.getId());
        if (b == null) {
            return null;
        }
        if (group.isCollapsed()) {
            return new double[]{b[0], b[1], COLLAPSED_W, COLLAPSED_H};
        }
        return new double[]{b[0] - GROUP_PAD, b[1] - GROUP_PAD - GROUP_TITLE,
            b[2] - b[0] + GROUP_PAD * 2, b[3] - b[1] + GROUP_PAD * 2 + GROUP_TITLE};
    }

    private GraphGroup collapsedGroupOf(GraphNode n) {
        String gid = n.getGroupId();
        if (gid == null) {
            return null;
        }
        GraphGroup g = model.getGroup(gid);
        return g != null && g.isCollapsed() && !g.isLane() ? g : null;
    }

    /// True when a node is drawn: it is not inside a collapsed group.
    public boolean isNodeVisible(GraphNode n) {
        return collapsedGroupOf(n) == null;
    }

    /// The rectangle an edge end attaches to: the node, or its collapsed
    /// group.
    private double[] visibleRect(GraphNode n, double[] out) {
        GraphGroup g = collapsedGroupOf(n);
        if (g == null) {
            out[0] = n.getX();
            out[1] = n.getY();
            out[2] = n.getWidth();
            out[3] = n.getHeight();
            return out;
        }
        refreshGroups();
        double[] b = groupBounds.get(g.getId());
        if (b == null) {
            return null;
        }
        out[0] = b[0];
        out[1] = b[1];
        out[2] = COLLAPSED_W;
        out[3] = COLLAPSED_H;
        return out;
    }

    /// The bounds of everything drawn, minX, minY, maxX, maxY, or null.
    private double[] contentBounds() {
        refreshGroups();
        double[] b = null;
        List<GraphNode> nodes = model.getNodes();
        for (int i = 0; i < nodes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            double[] r = visibleRect(nodes.get(i), rectBuf);
            if (r == null) {
                continue;
            }
            if (b == null) {
                b = new double[]{r[0], r[1], r[0] + r[2], r[1] + r[3]};
            } else {
                b[0] = Math.min(b[0], r[0]);
                b[1] = Math.min(b[1], r[1]);
                b[2] = Math.max(b[2], r[0] + r[2]);
                b[3] = Math.max(b[3], r[1] + r[3]);
            }
        }
        for (GraphGroup g : model.getGroups()) {
            // a lane has bounds with nothing in it; a group needs members
            double[] r = b != null || g.isLane() ? getGroupRect(g) : null;
            if (r == null) {
                continue;
            }
            if (b == null) {
                b = new double[]{r[0], r[1], r[0] + r[2], r[1] + r[3]};
            } else {
                b[0] = Math.min(b[0], r[0]);
                b[1] = Math.min(b[1], r[1]);
                b[2] = Math.max(b[2], r[0] + r[2]);
                b[3] = Math.max(b[3], r[1] + r[3]);
            }
        }
        return b;
    }

    private void ensurePath(int n) {
        if (pathX.length < n) {
            int len = Math.max(n, pathX.length * 2);
            pathX = new double[len];
            pathY = new double[len];
        }
    }

    /// Computes an edge's path in world units into the path buffers.
    ///
    /// #### Returns
    ///
    /// the number of points, 0 for an edge that is not drawn
    int edgePath(GraphEdge e) {
        GraphNode s = model.getNode(e.getSourceId());
        GraphNode t = model.getNode(e.getTargetId());
        if (s == null || t == null) {
            return 0;
        }
        GraphGroup sg = collapsedGroupOf(s);
        GraphGroup tg = collapsedGroupOf(t);
        if (sg != null && sg == tg) { // NOPMD CompareObjectsWithEquals - the same object is meant
            return 0;
        }
        double[] sr = visibleRect(s, rectBuf);
        double[] tr = visibleRect(t, rectBuf2);
        if (sr == null || tr == null) {
            return 0;
        }
        GraphPort sp = sg == null ? s.getPort(e.getSourcePortId()) : null;
        GraphPort tp = tg == null ? t.getPort(e.getTargetPortId()) : null;
        double[] route = sg == null && tg == null ? e.getRoute() : null;
        int bends = route == null ? 0 : route.length / 2;
        boolean ports = sp != null || tp != null;
        int style = edgeStyle;
        if (style == EDGE_AUTO) {
            style = ports ? EDGE_CURVED : EDGE_STRAIGHT;
        }
        // control points: start, bends, end
        int cn = bends + 2;
        double[] cx = new double[cn];
        double[] cy = new double[cn];
        for (int i = 0; i < bends; i++) {
            cx[i + 1] = route[i * 2];
            cy[i + 1] = route[i * 2 + 1];
        }
        double tcx = tr[0] + tr[2] / 2;
        double tcy = tr[1] + tr[3] / 2;
        // ports sit on the left and right sides, so their edges leave sideways
        boolean sourceVertical = false;
        boolean targetVertical = false;
        if (sp != null) {
            cx[0] = portX(s, sp);
            cy[0] = portY(s, sp);
        } else {
            double towardX = bends > 0 ? cx[1] : tcx;
            double towardY = bends > 0 ? cy[1] : tcy;
            if (tp != null && bends == 0) {
                towardX = portX(t, tp);
                towardY = portY(t, tp);
            }
            sourceVertical = clip(sr, towardX, towardY, cx, cy, 0, style == EDGE_ORTHOGONAL);
            if (sg == null && s.getShape() != null) {
                GraphShapeRegistry.pullToOutline(s.getShape(), sr, cx, cy, 0);
            }
        }
        if (tp != null) {
            cx[cn - 1] = portX(t, tp);
            cy[cn - 1] = portY(t, tp);
        } else {
            double fromX = bends > 0 ? cx[cn - 2] : cx[0];
            double fromY = bends > 0 ? cy[cn - 2] : cy[0];
            targetVertical = clip(tr, fromX, fromY, cx, cy, cn - 1, style == EDGE_ORTHOGONAL);
            if (tg == null && t.getShape() != null) {
                GraphShapeRegistry.pullToOutline(t.getShape(), tr, cx, cy, cn - 1);
            }
        }
        if (s == t && sp == null && tp == null) { // NOPMD CompareObjectsWithEquals - the same object is meant
            // a loop: out of the top right corner and back into the top
            double r = Math.min(sr[2], sr[3]) / 2;
            ensurePath(5);
            pathX[0] = sr[0] + sr[2];
            pathY[0] = sr[1] + sr[3] / 3;
            pathX[1] = sr[0] + sr[2] + r;
            pathY[1] = sr[1] + sr[3] / 3;
            pathX[2] = sr[0] + sr[2] + r;
            pathY[2] = sr[1] - r;
            pathX[3] = sr[0] + sr[2] * 2 / 3;
            pathY[3] = sr[1] - r;
            pathX[4] = sr[0] + sr[2] * 2 / 3;
            pathY[4] = sr[1];
            return 5;
        }
        if (style == EDGE_STRAIGHT) {
            ensurePath(cn);
            System.arraycopy(cx, 0, pathX, 0, cn);
            System.arraycopy(cy, 0, pathY, 0, cn);
            return cn;
        }
        if (style == EDGE_ORTHOGONAL) {
            ensurePath(cn * 3);
            int k = 0;
            pathX[k] = cx[0];
            pathY[k++] = cy[0];
            // bends an arrangement routed through run along the flow
            boolean flowVertical = Math.abs(cy[cn - 1] - cy[0]) > Math.abs(cx[cn - 1] - cx[0]);
            for (int i = 1; i < cn; i++) {
                double px = cx[i - 1];
                double py = cy[i - 1];
                // each span leaves and enters along the side it touches
                boolean leaveV = i == 1 ? sourceVertical : flowVertical;
                boolean enterV = i == cn - 1 ? targetVertical : flowVertical;
                if (Math.abs(px - cx[i]) > 0.5 && Math.abs(py - cy[i]) > 0.5) {
                    if (leaveV && enterV) {
                        double my = (py + cy[i]) / 2;
                        pathX[k] = px;
                        pathY[k++] = my;
                        pathX[k] = cx[i];
                        pathY[k++] = my;
                    } else if (!leaveV && !enterV) {
                        double mx = (px + cx[i]) / 2;
                        pathX[k] = mx;
                        pathY[k++] = py;
                        pathX[k] = mx;
                        pathY[k++] = cy[i];
                    } else if (leaveV) {
                        pathX[k] = px;
                        pathY[k++] = cy[i];
                    } else {
                        pathX[k] = cx[i];
                        pathY[k++] = py;
                    }
                }
                pathX[k] = cx[i];
                pathY[k++] = cy[i];
            }
            return k;
        }
        // curved: a cubic per span; tangents horizontal at ports, otherwise
        // Catmull-Rom through the bends
        int steps = Math.max(6, Math.min(24, (int) (scale * 16)));
        ensurePath((cn - 1) * steps + 1);
        int k = 0;
        pathX[k] = cx[0];
        pathY[k++] = cy[0];
        for (int i = 0; i < cn - 1; i++) {
            double x0 = cx[i];
            double y0 = cy[i];
            double x3 = cx[i + 1];
            double y3 = cy[i + 1];
            double tx0;
            double ty0;
            double tx1;
            double ty1;
            double span = Math.sqrt((x3 - x0) * (x3 - x0) + (y3 - y0) * (y3 - y0));
            if (i == 0 && sp != null) {
                tx0 = Math.max(40, Math.abs(x3 - x0) / 2);
                ty0 = 0;
            } else if (i > 0) {
                tx0 = (cx[i + 1] - cx[i - 1]) / 4;
                ty0 = (cy[i + 1] - cy[i - 1]) / 4;
            } else {
                tx0 = (x3 - x0) / 3;
                ty0 = (y3 - y0) / 3;
            }
            if (i == cn - 2 && tp != null) {
                tx1 = Math.max(40, Math.abs(x3 - x0) / 2);
                ty1 = 0;
            } else if (i < cn - 2) {
                tx1 = (cx[i + 2] - cx[i]) / 4;
                ty1 = (cy[i + 2] - cy[i]) / 4;
            } else {
                tx1 = (x3 - x0) / 3;
                ty1 = (y3 - y0) / 3;
            }
            if (span < 1) {
                tx0 = 0;
                ty0 = 0;
                tx1 = 0;
                ty1 = 0;
            }
            double x1 = x0 + tx0;
            double y1 = y0 + ty0;
            double x2 = x3 - tx1;
            double y2 = y3 - ty1;
            for (int st = 1; st <= steps; st++) {
                double u = st / (double) steps;
                double v = 1 - u;
                double a = v * v * v;
                double b = 3 * v * v * u;
                double c = 3 * v * u * u;
                double d = u * u * u;
                pathX[k] = a * x0 + b * x1 + c * x2 + d * x3;
                pathY[k++] = a * y0 + b * y1 + c * y2 + d * y3;
            }
        }
        return k;
    }

    /// Puts into `out[i]` where the line from the rectangle's centre toward
    /// a point leaves the rectangle; with `orthogonal`, the middle of the side
    /// facing the point: the top or the bottom unless the point is level with
    /// the rectangle, so a flow drawn downward leaves and enters vertically.
    ///
    /// #### Returns
    ///
    /// true when the point is on the top or the bottom side
    private static boolean clip(double[] r, double tx, double ty, double[] ox, double[] oy, int i, boolean orthogonal) {
        double cx = r[0] + r[2] / 2;
        double cy = r[1] + r[3] / 2;
        double dx = tx - cx;
        double dy = ty - cy;
        if (dx == 0 && dy == 0) {
            ox[i] = cx;
            oy[i] = cy;
            return false;
        }
        double hw = r[2] / 2;
        double hh = r[3] / 2;
        if (orthogonal) {
            if (Math.abs(dy) <= hh) {
                ox[i] = cx + (dx > 0 ? hw : -hw);
                oy[i] = cy;
                return false;
            }
            ox[i] = cx;
            oy[i] = cy + (dy > 0 ? hh : -hh);
            return true;
        }
        double sx = dx == 0 ? Double.MAX_VALUE : hw / Math.abs(dx);
        double sy = dy == 0 ? Double.MAX_VALUE : hh / Math.abs(dy);
        double s = Math.min(sx, sy);
        ox[i] = cx + dx * s;
        oy[i] = cy + dy * s;
        return sy <= sx;
    }

    // ------------------------------------------------------------------ paint

    void paintGraph(Graphics g, int w, int h) {
        refreshGroups();
        Style bgStyle = GraphLook.style("NodeGraph");
        int bg = GraphLook.bg(bgStyle, 0xf5f5f5);
        g.setColor(bg);
        g.fillRect(0, 0, w, h);
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        if (gridVisible) {
            paintGrid(g, w, h, bg);
        }
        double wx0 = screenToWorldX(0);
        double wy0 = screenToWorldY(0);
        double wx1 = screenToWorldX(w);
        double wy1 = screenToWorldY(h);
        for (GraphGroup grp : model.getGroups()) {
            double[] r = getGroupRect(grp);
            if (r != null && (grp.isLane() || !grp.isCollapsed()) && r[0] < wx1 && r[1] < wy1
                    && r[0] + r[2] > wx0 && r[1] + r[3] > wy0) {
                if (grp.isLane()) {
                    paintLane(g, grp, r);
                } else {
                    paintGroup(g, grp, r);
                }
            }
        }
        List<GraphEdge> edges = model.getEdges();
        double[] sx = new double[0];
        double[] sy = new double[0];
        for (int i = 0; i < edges.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            GraphEdge e = edges.get(i);
            if (e == detached) { // NOPMD CompareObjectsWithEquals - the same object is meant
                continue;
            }
            int n = edgePath(e);
            if (n < 2) {
                continue;
            }
            double minX = pathX[0];
            double maxX = minX;
            double minY = pathY[0];
            double maxY = minY;
            for (int k = 1; k < n; k++) {
                minX = Math.min(minX, pathX[k]);
                maxX = Math.max(maxX, pathX[k]);
                minY = Math.min(minY, pathY[k]);
                maxY = Math.max(maxY, pathY[k]);
            }
            if (maxX < wx0 || minX > wx1 || maxY < wy0 || minY > wy1) {
                continue;
            }
            if (sx.length < n) {
                sx = new double[n];
                sy = new double[n];
            }
            for (int k = 0; k < n; k++) {
                sx[k] = (pathX[k] - viewX) * scale;
                sy[k] = (pathY[k] - viewY) * scale;
            }
            edgeRenderer.paintEdge(g, this, e, sx, sy, n, e == selectedEdge); // NOPMD CompareObjectsWithEquals - the same object is meant
        }
        for (GraphGroup grp : model.getGroups()) {
            double[] r = getGroupRect(grp);
            if (r != null && grp.isCollapsed() && !grp.isLane() && r[0] < wx1 && r[1] < wy1 && r[0] + r[2] > wx0 && r[1] + r[3] > wy0) {
                paintCollapsedGroup(g, grp, r);
            }
        }
        List<GraphNode> nodes = model.getNodes();
        for (int i = 0; i < nodes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            GraphNode n = nodes.get(i);
            if (n.getX() > wx1 || n.getY() > wy1 || n.getX() + n.getWidth() < wx0 || n.getY() + n.getHeight() < wy0
                    || !isNodeVisible(n)) {
                continue;
            }
            int x = worldToScreenX(n.getX());
            int y = worldToScreenY(n.getY());
            int nw = Math.max(1, worldToScreenX(n.getX() + n.getWidth()) - x);
            int nh = Math.max(1, worldToScreenY(n.getY() + n.getHeight()) - y);
            if (nw < 3 && nh < 3) {
                // a dot: a renderer has nothing to say at this size
                g.setColor(GraphLook.style("NodeGraphNodeSelected").getFgColor());
                g.fillRect(x, y, Math.max(1, nw), Math.max(1, nh));
                continue;
            }
            nodeRenderer.paintNode(g, this, n, x, y, nw, nh, selection.contains(n));
        }
        g.setAntiAliased(aa);
    }

    void paintGraphOverlay(Graphics g, int w, int h) {
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        int primary = GraphLook.style("NodeGraphNodeSelected").getFgColor();
        if (editable && selection.size() == 1 && gesture != G_MOVE && gesture != G_CONNECT) {
            GraphNode n = selection.get(0);
            if (resizable && isNodeVisible(n)) {
                paintResizeHandles(g, n, primary);
            }
            if (!n.hasPorts() && isNodeVisible(n) && gesture != G_RESIZE) {
                int r = portRadiusOnScreen() + 1;
                int hx = worldToScreenX(n.getX() + n.getWidth());
                int hy = worldToScreenY(n.getCenterY());
                g.setColor(GraphLook.bg(GraphLook.style("NodeGraphNode"), 0xffffff));
                g.fillArc(hx - r - 2, hy - r - 2, r * 2 + 4, r * 2 + 4, 0, 360);
                g.setColor(primary);
                g.fillArc(hx - r, hy - r, r * 2, r * 2, 0, 360);
            }
        }
        if (gesture == G_CONNECT && connectNode != null) {
            double ax;
            double ay;
            if (connectPort != null) {
                ax = portX(connectNode, connectPort);
                ay = portY(connectNode, connectPort);
            } else {
                double[] r = visibleRect(connectNode, rectBuf);
                ax = r[0] + r[2];
                ay = r[1] + r[3] / 2;
            }
            int x1 = worldToScreenX(ax);
            int y1 = worldToScreenY(ay);
            int x2 = lastX;
            int y2 = lastY;
            if (hoverNode != null && hoverValid) {
                if (hoverPort != null) {
                    x2 = worldToScreenX(portX(hoverNode, hoverPort));
                    y2 = worldToScreenY(portY(hoverNode, hoverPort));
                }
                double[] r = visibleRect(hoverNode, rectBuf);
                int rx = worldToScreenX(r[0]);
                int ry = worldToScreenY(r[1]);
                int arc = GraphLook.mm(1.6f);
                g.setColor(primary);
                g.drawRoundRect(rx - 3, ry - 3, worldToScreenLength(r[2]) + 5, worldToScreenLength(r[3]) + 5, arc, arc);
            }
            g.setColor(hoverNode != null && !hoverValid ? 0xd32f2f : primary);
            float width = Math.max(1f, GraphLook.mm(0.4f));
            int dashes = 0;
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            double dash = GraphLook.mm(1.5f);
            for (double at = 0; at < len; at += dash * 2) {
                double e = Math.min(len, at + dash);
                GraphLook.line(g, x1 + dx * at / len, y1 + dy * at / len, x1 + dx * e / len, y1 + dy * e / len, width);
                dashes++;
            }
            if (dashes == 0) {
                g.fillArc(x2 - 2, y2 - 2, 4, 4, 0, 360);
            }
        }
        if (gesture == G_MARQUEE) {
            int mx = worldToScreenX(marqueeX);
            int my = worldToScreenY(marqueeY);
            int x = Math.min(mx, lastX);
            int y = Math.min(my, lastY);
            int mw = Math.abs(lastX - mx);
            int mh = Math.abs(lastY - my);
            int a = g.getAlpha();
            g.setColor(primary);
            g.setAlpha(a * 30 / 255);
            g.fillRect(x, y, mw, mh);
            g.setAlpha(a);
            g.drawRect(x, y, mw, mh);
        }
        if (previewTemplate != null) {
            paintPreview(g, primary);
        }
        if (minimapVisible) {
            paintMinimap(g, w, h);
        }
        g.setAntiAliased(aa);
    }

    private void paintGrid(Graphics g, int w, int h, int bg) {
        Style gs = GraphLook.style("NodeGraphGrid");
        double step = 24;
        while (step * scale < GraphLook.mm(2.5f)) {
            step *= 4;
        }
        g.setColor(GraphLook.mix(bg, gs.getFgColor(), 110));
        int dot = GraphLook.mm(0.25f);
        double start = Math.floor(viewX / step) * step;
        double startY = Math.floor(viewY / step) * step;
        for (double wx = start; wx < screenToWorldX(w) + step; wx += step) {
            int x = worldToScreenX(wx);
            for (double wy = startY; wy < screenToWorldY(h) + step; wy += step) {
                g.fillRect(x, worldToScreenY(wy), dot, dot);
            }
        }
    }

    private void paintGroup(Graphics g, GraphGroup grp, double[] r) {
        Style s = GraphLook.style("NodeGraphGroup");
        int x = worldToScreenX(r[0]);
        int y = worldToScreenY(r[1]);
        int w = worldToScreenLength(r[2]);
        int h = worldToScreenLength(r[3]);
        int arc = Math.max(2, Math.round(GraphLook.mm(2f) * Math.min(getZoom(), 1.5f)));
        int fg = s.getFgColor();
        int a = g.getAlpha();
        g.setColor(GraphLook.bg(s, GraphLook.mix(0xffffff, fg, 20)));
        g.setAlpha(a * Math.max(40, s.getBgTransparency() & 0xff) / 255);
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setAlpha(a);
        g.setColor(GraphLook.mix(GraphLook.bg(s, 0xffffff), fg, 90));
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        if (getZoom() >= detailZoom) {
            Font f = GraphLook.font("NodeGraphGroup", getZoom());
            g.setFont(f);
            g.setColor(fg);
            int pad = GraphLook.mm(1.5f, getZoom());
            int th = worldToScreenLength(GROUP_TITLE);
            String chevron = "- ";
            g.drawString(GraphLook.fit(chevron + grp.getLabel(), f, w - pad * 2), x + pad, y + (th - f.getHeight()) / 2 + pad / 2);
        }
    }

    private void paintCollapsedGroup(Graphics g, GraphGroup grp, double[] r) {
        Style s = GraphLook.style("NodeGraphGroup");
        int x = worldToScreenX(r[0]);
        int y = worldToScreenY(r[1]);
        int w = worldToScreenLength(r[2]);
        int h = worldToScreenLength(r[3]);
        int arc = Math.max(2, Math.round(GraphLook.mm(1.6f) * Math.min(getZoom(), 1.5f)));
        int fg = s.getFgColor();
        int bg = GraphLook.bg(GraphLook.style("NodeGraphNode"), 0xffffff);
        GraphLook.shadow(g, x, y, w, h, arc, 0.6f);
        // a second card behind says there is more than one node inside
        int off = Math.max(2, GraphLook.mm(0.8f));
        g.setColor(GraphLook.mix(bg, fg, 30));
        g.fillRoundRect(x + off, y - off, w, h, arc, arc);
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setColor(GraphLook.mix(bg, fg, 90));
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        if (getZoom() >= detailZoom) {
            Font f = GraphLook.font("NodeGraphGroup", getZoom());
            g.setFont(f);
            g.setColor(fg);
            int pad = GraphLook.mm(1.5f, getZoom());
            g.drawString(GraphLook.fit("+ " + grp.getLabel(), f, w - pad * 2), x + pad, y + (h - f.getHeight()) / 2);
        }
    }

    private int[] minimapRect(int w, int h) {
        int mw = Math.min(GraphLook.mm(30), w / 3);
        int mh = Math.min(GraphLook.mm(20), h / 3);
        int m = GraphLook.mm(2);
        return new int[]{w - mw - m, h - mh - m, mw, mh};
    }

    private double[] minimapWorld(int[] mr) {
        double[] b = contentBounds();
        if (b == null) {
            return null;
        }
        double vx0 = screenToWorldX(0);
        double vy0 = screenToWorldY(0);
        double vx1 = screenToWorldX(canvas.getWidth());
        double vy1 = screenToWorldY(canvas.getHeight());
        double x0 = Math.min(b[0], vx0);
        double y0 = Math.min(b[1], vy0);
        double x1 = Math.max(b[2], vx1);
        double y1 = Math.max(b[3], vy1);
        double s = Math.min(mr[2] / Math.max(1, x1 - x0), mr[3] / Math.max(1, y1 - y0));
        // centre the content in the box
        double ox = mr[0] + (mr[2] - (x1 - x0) * s) / 2 - x0 * s;
        double oy = mr[1] + (mr[3] - (y1 - y0) * s) / 2 - y0 * s;
        return new double[]{s, ox, oy};
    }

    private void paintMinimap(Graphics g, int w, int h) {
        int[] mr = minimapRect(w, h);
        Style s = GraphLook.style("NodeGraphMinimap");
        int bg = GraphLook.bg(s, 0xffffff);
        int arc = GraphLook.mm(1.2f);
        GraphLook.shadow(g, mr[0], mr[1], mr[2], mr[3], arc, 0.7f);
        g.setColor(bg);
        g.fillRoundRect(mr[0], mr[1], mr[2], mr[3], arc, arc);
        double[] t = minimapWorld(mr);
        if (t == null) {
            return;
        }
        int[] clip = g.getClip();
        g.clipRect(mr[0], mr[1], mr[2], mr[3]);
        g.setColor(GraphLook.mix(bg, s.getFgColor(), 120));
        List<GraphNode> nodes = model.getNodes();
        for (int i = 0; i < nodes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            double[] r = visibleRect(nodes.get(i), rectBuf);
            if (r == null) {
                continue;
            }
            int x = (int) (r[0] * t[0] + t[1]);
            int y = (int) (r[1] * t[0] + t[2]);
            g.fillRect(x, y, Math.max(1, (int) (r[2] * t[0])), Math.max(1, (int) (r[3] * t[0])));
        }
        int primary = GraphLook.style("NodeGraphNodeSelected").getFgColor();
        g.setColor(primary);
        int vx = (int) (viewX * t[0] + t[1]);
        int vy = (int) (viewY * t[0] + t[2]);
        int vw = (int) (canvas.getWidth() / (double) scale * t[0]);
        int vh = (int) (canvas.getHeight() / (double) scale * t[0]);
        g.drawRect(vx, vy, vw, vh);
        g.drawRect(vx + 1, vy + 1, vw - 2, vh - 2);
        g.setClip(clip);
        g.setColor(GraphLook.mix(bg, s.getFgColor(), 60));
        g.drawRoundRect(mr[0], mr[1], mr[2] - 1, mr[3] - 1, arc, arc);
    }

    /// Paints the whole graph into a new image of the given size, fitted
    /// with a margin, at no more than zoom 1, without the minimap or the
    /// selection. For a thumbnail, a share sheet or a printout.
    public Image toImage(int width, int height) {
        Image img = Image.createImage(width, height, 0xffffffff);
        Graphics g = img.getGraphics();
        float oz = scale;
        double ox = viewX;
        double oy = viewY;
        List<GraphNode> sel = new ArrayList<GraphNode>(selection);
        GraphEdge se = selectedEdge;
        boolean mm = minimapVisible;
        selection.clear();
        selectedEdge = null;
        minimapVisible = false;
        double[] b = contentBounds();
        if (b != null) {
            int margin = GraphLook.mm(3);
            double bw = Math.max(1, b[2] - b[0]);
            double bh = Math.max(1, b[3] - b[1]);
            scale = (float) Math.min(unit(), Math.min((width - margin * 2) / bw, (height - margin * 2) / bh));
            viewX = (b[0] + b[2]) / 2 - width / 2.0 / scale;
            viewY = (b[1] + b[3]) / 2 - height / 2.0 / scale;
        }
        paintGraph(g, width, height);
        scale = oz;
        viewX = ox;
        viewY = oy;
        selection.addAll(sel);
        selectedEdge = se;
        minimapVisible = mm;
        return img;
    }

    // --------------------------------------------------------- hit testing

    /// The topmost visible node at a point of the view, or null.
    public GraphNode nodeAt(int sx, int sy) {
        double wx = screenToWorldX(sx);
        double wy = screenToWorldY(sy);
        List<GraphNode> nodes = model.getNodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GraphNode n = nodes.get(i);
            if (isNodeVisible(n) && n.contains(wx, wy) && (n.getShape() == null || GraphShapeRegistry.contains(
                    n.getShape(), wx, wy, n.getX(), n.getY(), n.getWidth(), n.getHeight()))) {
                return n;
            }
        }
        return null;
    }

    /// The edge within a finger's reach of a point of the view, or null.
    public GraphEdge edgeAt(int sx, int sy) {
        double wx = screenToWorldX(sx);
        double wy = screenToWorldY(sy);
        double tol = GraphLook.mm(2f) / (double) scale;
        GraphEdge best = null;
        double bestD = tol * tol;
        List<GraphEdge> edges = model.getEdges();
        for (int i = edges.size() - 1; i >= 0; i--) {
            GraphEdge e = edges.get(i);
            int n = edgePath(e);
            for (int k = 0; k + 1 < n; k++) {
                double d = segDist2(wx, wy, pathX[k], pathY[k], pathX[k + 1], pathY[k + 1]);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
        }
        return best;
    }

    private static double segDist2(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double l2 = dx * dx + dy * dy;
        double t = l2 == 0 ? 0 : ((px - x1) * dx + (py - y1) * dy) / l2;
        t = Math.max(0, Math.min(1, t));
        double ex = x1 + t * dx - px;
        double ey = y1 + t * dy - py;
        return ex * ex + ey * ey;
    }

    private GraphPort portAt(GraphNode n, int sx, int sy, int reach) {
        if (n == null || !n.hasPorts() || !isNodeVisible(n)) {
            return null;
        }
        GraphPort best = null;
        long bestD = (long) reach * reach;
        for (int pass = 0; pass < 2; pass++) {
            List<GraphPort> ports = pass == 0 ? n.getInputs() : n.getOutputs();
            for (int i = 0; i < ports.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                GraphPort p = ports.get(i);
                long dx = worldToScreenX(portX(n, p)) - sx;
                long dy = worldToScreenY(portY(n, p)) - sy;
                long d = dx * dx + dy * dy;
                if (d <= bestD) {
                    bestD = d;
                    best = p;
                }
            }
        }
        return best;
    }

    /// The node whose port is within reach of a point; ports stick out of
    /// their node, so a press just outside it still finds them.
    private GraphNode portOwnerAt(int sx, int sy, int reach) {
        double wx = screenToWorldX(sx);
        double wy = screenToWorldY(sy);
        double wr = reach / (double) scale;
        List<GraphNode> nodes = model.getNodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GraphNode n = nodes.get(i);
            if (n.hasPorts() && wx >= n.getX() - wr && wx <= n.getX() + n.getWidth() + wr
                    && wy >= n.getY() && wy <= n.getY() + n.getHeight() && portAt(n, sx, sy, reach) != null) {
                return n;
            }
        }
        return null;
    }

    private GraphGroup groupTitleAt(int sx, int sy) {
        double wx = screenToWorldX(sx);
        double wy = screenToWorldY(sy);
        List<GraphGroup> groups = model.getGroups();
        for (int i = groups.size() - 1; i >= 0; i--) {
            GraphGroup grp = groups.get(i);
            double[] r = getGroupRect(grp);
            if (r == null) {
                continue;
            }
            if (grp.isLane()) {
                double bw = grp.isHorizontal() ? LANE_HEADER : r[2];
                double bh = grp.isHorizontal() ? r[3] : LANE_HEADER;
                if (wx >= r[0] && wx <= r[0] + bw && wy >= r[1] && wy <= r[1] + bh) {
                    return grp;
                }
                continue;
            }
            double titleH = grp.isCollapsed() ? r[3] : GROUP_TITLE + GROUP_PAD / 2;
            if (wx >= r[0] && wx <= r[0] + r[2] && wy >= r[1] && wy <= r[1] + titleH) {
                return grp;
            }
        }
        return null;
    }

    private boolean onHandle(int sx, int sy) {
        if (!editable || selection.size() != 1) {
            return false;
        }
        GraphNode n = selection.get(0);
        if (n.hasPorts() || !isNodeVisible(n)) {
            return false;
        }
        int r = portRadiusOnScreen() + GraphLook.mm(1.5f);
        int dx = worldToScreenX(n.getX() + n.getWidth()) - sx;
        int dy = worldToScreenY(n.getCenterY()) - sy;
        return dx * dx + dy * dy <= r * r;
    }

    private boolean onMinimap(int sx, int sy) {
        if (!minimapVisible) {
            return false;
        }
        int[] mr = minimapRect(canvas.getWidth(), canvas.getHeight());
        return sx >= mr[0] && sy >= mr[1] && sx < mr[0] + mr[2] && sy < mr[1] + mr[3];
    }

    private void minimapJump(int sx, int sy) {
        int[] mr = minimapRect(canvas.getWidth(), canvas.getHeight());
        double[] t = minimapWorld(mr);
        if (t == null) {
            return;
        }
        double wx = (sx - t[1]) / t[0];
        double wy = (sy - t[2]) / t[0];
        setView(wx - canvas.getWidth() / 2.0 / scale, wy - canvas.getHeight() / 2.0 / scale);
    }

    // ---------------------------------------------------------------- pointer

    void pressed(int x, int y) {
        endLabelEdit();
        gesture = G_NONE;
        shiftDown = Display.getInstance().isShiftKeyDown();
        pressX = x;
        pressY = y;
        lastX = x;
        lastY = y;
        pressNode = null;
        pressPort = null;
        pressGroup = null;
        pressEdge = null;
        pressHandle = false;
        pressCorner = -1;
        pressLane = null;
        laneAxis = 0;
        pressMinimap = onMinimap(x, y);
        if (pressMinimap) {
            minimapJump(x, y);
            return;
        }
        if (!canvas.hasFocus() && canvas.getComponentForm() != null) {
            // keys: Delete, and copy, paste and undo with Ctrl or Cmd
            canvas.requestFocus();
        }
        int reach = portRadiusOnScreen() + GraphLook.mm(1.5f);
        pressCorner = cornerAt(x, y);
        if (pressCorner >= 0) {
            pressNode = selection.get(0);
            return;
        }
        if (onHandle(x, y)) {
            pressHandle = true;
            pressNode = selection.get(0);
            return;
        }
        GraphNode owner = portOwnerAt(x, y, reach);
        if (owner != null) {
            pressNode = owner;
            pressPort = portAt(owner, x, y, reach);
            return;
        }
        pressNode = nodeAt(x, y);
        if (pressNode == null) {
            pressGroup = groupTitleAt(x, y);
            if (pressGroup == null) {
                laneAxis = laneEdgeAt(x, y);
                if (laneAxis == 0) {
                    pressEdge = edgeAt(x, y);
                }
            }
        }
    }

    // A drag began; true when it is an edit, false to let the surface pan.
    boolean dragStarted() {
        if (pressMinimap) {
            gesture = G_MINIMAP;
        } else {
            beginDrag();
        }
        return gesture != G_NONE;
    }

    void dragged(int x, int y) {
        lastX = x;
        lastY = y;
        switch (gesture) {
            case G_MOVE:
                moveTo(x, y);
                break;
            case G_CONNECT:
                updateHover(x, y);
                break;
            case G_MINIMAP:
                minimapJump(x, y);
                break;
            case G_RESIZE:
                resizeTo(x, y);
                break;
            case G_LANE:
                laneResizeTo(x, y);
                break;
            default:
                break;
        }
        repaint();
    }

    private void beginDrag() {
        if (pressCorner >= 0) {
            gesture = G_RESIZE;
            GraphNode n = pressNode;
            resizeFrom = new double[]{n.getX(), n.getY(), n.getWidth(), n.getHeight()};
        } else if (pressLane != null && laneAxis != 0) {
            gesture = G_LANE;
            laneFrom = new double[]{pressLane.getX(), pressLane.getY(), pressLane.getWidth(), pressLane.getHeight()};
        } else if (pressHandle) {
            beginConnect(pressNode, null);
        } else if (pressPort != null && editable) {
            beginConnect(pressNode, pressPort);
        } else if (pressNode != null && editable) {
            if (!selection.contains(pressNode)) {
                if (shiftDown) {
                    toggleSelection(pressNode);
                } else {
                    select(pressNode);
                }
            }
            beginMove(new ArrayList<GraphNode>(selection), null);
        } else if (pressGroup != null && editable && !pressGroup.isCollapsed()) {
            List<GraphNode> members = new ArrayList<GraphNode>();
            for (GraphNode n : model.getNodes()) {
                if (pressGroup.getId().equals(n.getGroupId())) {
                    members.add(n);
                }
            }
            beginMove(members, pressGroup.isLane() ? pressGroup : null);
        } else if (pressNode == null && shiftDown) {
            gesture = G_MARQUEE;
            marqueeX = screenToWorldX(pressX);
            marqueeY = screenToWorldY(pressY);
        }
    }

    private void beginMove(List<GraphNode> nodes, GraphGroup lane) {
        gesture = G_MOVE;
        moving.clear();
        moving.addAll(nodes);
        movingLane = lane;
        laneFrom = lane == null ? null : new double[]{lane.getX(), lane.getY(), lane.getWidth(), lane.getHeight()};
        unrouted.clear();
        unroutedRoutes.clear();
        movingFrom = new double[moving.size() * 2];
        for (int i = 0; i < moving.size(); i++) {
            movingFrom[i * 2] = moving.get(i).getX();
            movingFrom[i * 2 + 1] = moving.get(i).getY();
            // a route was laid for where the node was
            for (GraphEdge e : model.getEdges()) {
                if (e.getRoute() != null && e.touches(moving.get(i).getId())) {
                    unrouted.add(e);
                    unroutedRoutes.add(e.getRoute());
                    e.setRoute(null);
                }
            }
        }
        moveWorldX = screenToWorldX(pressX);
        moveWorldY = screenToWorldY(pressY);
        if (live != null) {
            live.reheat(120);
            liveRunning = true;
            animateNow();
        }
    }

    private void moveTo(int x, int y) {
        double dx = screenToWorldX(x) - moveWorldX;
        double dy = screenToWorldY(y) - moveWorldY;
        if (snap > 0 && !moving.isEmpty()) {
            double nx = movingFrom[0] + dx;
            double ny = movingFrom[1] + dy;
            dx = Math.round(nx / snap) * snap - movingFrom[0];
            dy = Math.round(ny / snap) * snap - movingFrom[1];
        }
        for (int i = 0; i < moving.size(); i++) {
            GraphNode n = moving.get(i);
            n.setPosition(movingFrom[i * 2] + dx, movingFrom[i * 2 + 1] + dy);
            if (live != null) {
                live.hold(n.getId(), n.getCenterX(), n.getCenterY());
            }
        }
        if (movingLane != null) {
            movingLane.setBounds(laneFrom[0] + dx, laneFrom[1] + dy, laneFrom[2], laneFrom[3]);
        }
        groupsDirty = true;
        syncComponents();
    }

    private void beginConnect(GraphNode node, GraphPort port) {
        gesture = G_CONNECT;
        connectReversed = false;
        detached = null;
        if (port != null && port.isInput()) {
            // dragging off an input picks up the edge that ends there
            GraphEdge e = edgeIntoPort(node, port);
            if (e != null) {
                GraphNode s = model.getNode(e.getSourceId());
                if (s != null) {
                    detached = e;
                    connectNode = s;
                    connectPort = s.getPort(e.getSourcePortId());
                    connectedPorts.clear();
                    return;
                }
            }
            connectReversed = true;
        }
        connectNode = node;
        connectPort = port;
    }

    private GraphEdge edgeIntoPort(GraphNode node, GraphPort port) {
        List<GraphEdge> edges = model.getEdges();
        for (int i = edges.size() - 1; i >= 0; i--) {
            GraphEdge e = edges.get(i);
            if (node.getId().equals(e.getTargetId()) && port.getId().equals(e.getTargetPortId())) {
                return e;
            }
        }
        return null;
    }

    private void updateHover(int x, int y) {
        int reach = portRadiusOnScreen() + GraphLook.mm(2.5f);
        GraphNode n = portOwnerAt(x, y, reach);
        if (n == null) {
            n = nodeAt(x, y);
        }
        hoverNode = n;
        hoverPort = null;
        hoverValid = false;
        if (n == null) {
            return;
        }
        if (n.hasPorts()) {
            GraphPort p = portAt(n, x, y, reach);
            if (p == null || p.isInput() == connectReversed || !check(n, p)) {
                // the nearest port that would take the edge, which is what a
                // drop anywhere on the node means
                p = nearestPort(n, !connectReversed, x, y);
            }
            hoverPort = p;
            if (p == null) {
                return;
            }
        }
        hoverValid = check(n, hoverPort);
    }

    private GraphPort nearestPort(GraphNode n, boolean inputs, int x, int y) {
        List<GraphPort> ports = inputs ? n.getInputs() : n.getOutputs();
        GraphPort best = null;
        long bestD = Long.MAX_VALUE;
        for (int i = 0; i < ports.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            GraphPort p = ports.get(i);
            if (!check(n, p)) {
                continue;
            }
            long dx = worldToScreenX(portX(n, p)) - x;
            long dy = worldToScreenY(portY(n, p)) - y;
            long d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private boolean check(GraphNode n, GraphPort p) {
        GraphNode s = connectReversed ? n : connectNode;
        GraphPort sp = connectReversed ? p : connectPort;
        GraphNode t = connectReversed ? connectNode : n;
        GraphPort tp = connectReversed ? connectPort : p;
        if (model instanceof DefaultGraphModel) {
            GraphEdge d = detached;
            if (d != null) {
                // the picked up edge must not count against its own ports
                boolean same = d.getTargetId().equals(t.getId()) && tp != null && tp.getId().equals(d.getTargetPortId());
                if (same) {
                    return true;
                }
            }
            return ((DefaultGraphModel) model).getValidator().canConnect(model, s, sp, t, tp);
        }
        return s != t; // NOPMD CompareObjectsWithEquals - the same object is meant
    }

    // A long press began; true when it starts an edit.
    boolean longPressStarted(int x, int y) {
        if (pressMinimap || gesture != G_NONE) {
            return false;
        }
        if (editable && pressNode != null && pressPort == null && !pressNode.hasPorts()) {
            select(pressNode);
            beginConnect(pressNode, null);
            return true;
        }
        if (pressNode == null && pressGroup == null) {
            gesture = G_MARQUEE;
            marqueeX = screenToWorldX(x);
            marqueeY = screenToWorldY(y);
            return true;
        }
        return false;
    }

    void ended(boolean committed) {
        if (!committed) {
            cancelGesture();
            return;
        }
        int g = gesture;
        gesture = G_NONE;
        switch (g) {
            case G_MOVE:
                finishMove();
                break;
            case G_CONNECT:
                updateHover(lastX, lastY);
                finishConnect();
                break;
            case G_MARQUEE:
                finishMarquee(lastX, lastY);
                break;
            case G_RESIZE:
                finishResize();
                break;
            case G_LANE:
                finishLane();
                break;
            default:
                break;
        }
        repaint();
    }

    private void cancelGesture() {
        if (gesture == G_MOVE) {
            for (int i = 0; i < moving.size(); i++) {
                moving.get(i).setPosition(movingFrom[i * 2], movingFrom[i * 2 + 1]);
            }
            moving.clear();
            for (int i = 0; i < unrouted.size(); i++) {
                unrouted.get(i).setRoute(unroutedRoutes.get(i));
            }
            if (movingLane != null) {
                movingLane.setBounds(laneFrom[0], laneFrom[1], laneFrom[2], laneFrom[3]);
            }
            groupsDirty = true;
        } else if (gesture == G_RESIZE && pressNode != null && resizeFrom != null) {
            pressNode.setPosition(resizeFrom[0], resizeFrom[1]);
            pressNode.setSize(resizeFrom[2], resizeFrom[3]);
            groupsDirty = true;
        } else if (gesture == G_LANE && pressLane != null && laneFrom != null) {
            pressLane.setBounds(laneFrom[0], laneFrom[1], laneFrom[2], laneFrom[3]);
        }
        movingLane = null;
        unrouted.clear();
        unroutedRoutes.clear();
        detached = null;
        connectNode = null;
        connectPort = null;
        hoverNode = null;
        hoverPort = null;
        connectedPorts.clear();
        gesture = G_NONE;
        repaint();
    }

    private void finishMove() {
        beginStep("Move");
        // a listener that throws must not leave the step open: every later
        // edit would go into it and nothing could be undone again
        try {
            finishMoveStep();
        } finally {
            endStep();
            moving.clear();
            groupsDirty = true;
        }
        syncComponents();
    }

    private void finishMoveStep() {
        boolean moved = false;
        GraphGroup lane = movingLane;
        movingLane = null;
        if (lane != null) {
            double[] to = {lane.getX(), lane.getY(), lane.getWidth(), lane.getHeight()};
            lane.setBounds(laneFrom[0], laneFrom[1], laneFrom[2], laneFrom[3]);
            if ((to[0] != laneFrom[0] || to[1] != laneFrom[1]) && model.resizeGroup(lane, to[0], to[1], to[2], to[3])) {
                record(new GraphEdits.LaneBounds(model, lane, laneFrom, to));
                moved = true;
            }
        }
        for (int i = 0; i < moving.size(); i++) {
            GraphNode n = moving.get(i);
            double nx = n.getX();
            double ny = n.getY();
            double ox = movingFrom[i * 2];
            double oy = movingFrom[i * 2 + 1];
            if (nx == ox && ny == oy) {
                continue;
            }
            n.setPosition(ox, oy);
            boolean ok = true;
            for (int l = 0; l < listeners.size() && ok; l++) {
                ok = listeners.get(l).nodeMoving(this, n, nx, ny);
            }
            if (ok && model.moveNode(n, nx, ny)) {
                n.setPosition(nx, ny);
                moved = true;
                record(new GraphEdits.Move(model, n, ox, oy, nx, ny));
                if (lane == null) {
                    joinLane(n);
                }
                for (int l = 0; l < listeners.size(); l++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                    listeners.get(l).nodeMoved(this, n);
                }
            }
        }
        if (moved) {
            if (!unrouted.isEmpty()) {
                record(new GraphEdits.Routes(this, new ArrayList<GraphEdge>(unrouted),
                        new ArrayList<double[]>(unroutedRoutes), null));
            }
        } else {
            // refused, or put back where it was: the routes still fit
            for (int i = 0; i < unrouted.size(); i++) {
                unrouted.get(i).setRoute(unroutedRoutes.get(i));
            }
        }
        unrouted.clear();
        unroutedRoutes.clear();
    }

    private void finishConnect() {
        GraphNode n = hoverNode;
        GraphPort p = hoverPort;
        boolean valid = hoverValid;
        GraphEdge d = detached;
        GraphNode cn = connectNode;
        GraphPort cp = connectPort;
        boolean rev = connectReversed;
        hoverNode = null;
        hoverPort = null;
        detached = null;
        connectNode = null;
        connectPort = null;
        connectedPorts.clear();
        if (d != null) {
            if (n != null && valid && n.getId().equals(d.getTargetId()) && p != null && p.getId().equals(d.getTargetPortId())) {
                // dropped back where it was
                return;
            }
            List<GraphEdge> edgesBefore = new ArrayList<GraphEdge>(model.getEdges());
            if (!allowRemove(null, d) || !model.removeEdge(d)) {
                return;
            }
            beginStep(n == null || !valid ? "Disconnect" : "Reconnect");
            record(new GraphEdits.Presence(model, new ArrayList<GraphNode>(), single(d), true, "Disconnect",
                    new ArrayList<GraphNode>(), edgesBefore));
        } else {
            beginStep("Connect");
        }
        try {
            connectTo(n, p, valid, cn, cp, rev);
        } finally {
            endStep();
        }
    }

    private static List<GraphEdge> single(GraphEdge e) {
        List<GraphEdge> l = new ArrayList<GraphEdge>();
        l.add(e);
        return l;
    }

    private void connectTo(GraphNode n, GraphPort p, boolean valid, GraphNode cn, GraphPort cp, boolean rev) {
        if (n == null || !valid || cn == null) {
            return;
        }
        GraphNode s = rev ? n : cn;
        GraphPort sp = rev ? p : cp;
        GraphNode t = rev ? cn : n;
        GraphPort tp = rev ? cp : p;
        for (int l = 0; l < listeners.size(); l++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            if (!listeners.get(l).edgeCreating(this, s, sp, t, tp)) {
                return;
            }
        }
        GraphEdge e = model.connect(s, sp, t, tp);
        if (e != null) {
            record(new GraphEdits.Presence(model, new ArrayList<GraphNode>(), single(e), false, "Connect"));
            for (int l = 0; l < listeners.size(); l++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                listeners.get(l).edgeCreated(this, e);
            }
        }
    }

    private void finishMarquee(int x, int y) {
        double wx = screenToWorldX(x);
        double wy = screenToWorldY(y);
        double x0 = Math.min(marqueeX, wx);
        double y0 = Math.min(marqueeY, wy);
        double x1 = Math.max(marqueeX, wx);
        double y1 = Math.max(marqueeY, wy);
        if (!shiftDown) {
            selection.clear();
        }
        selectedEdge = null;
        for (GraphNode n : model.getNodes()) {
            if (isNodeVisible(n) && n.getX() >= x0 && n.getY() >= y0 && n.getX() + n.getWidth() <= x1
                    && n.getY() + n.getHeight() <= y1 && !selection.contains(n)) {
                selection.add(n);
            }
        }
        fireSelection();
    }

    void tapped(boolean twice) {
        if (pressMinimap) {
            return;
        }
        if (pressNode != null) {
            if (shiftDown) {
                toggleSelection(pressNode);
            } else if (!(selection.size() == 1 && selection.get(0) == pressNode) || selectedEdge != null) { // NOPMD CompareObjectsWithEquals - the same object is meant
                select(pressNode);
            }
            if (twice) {
                for (int l = 0; l < listeners.size(); l++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                    listeners.get(l).nodeOpened(this, pressNode);
                }
                if (labelEditing && editable) {
                    startLabelEdit(pressNode);
                }
            }
            return;
        }
        if (pressGroup != null) {
            if (pressGroup.isLane()) {
                return;
            }
            if (model.setCollapsed(pressGroup, !pressGroup.isCollapsed())) {
                groupsDirty = true;
                record(new GraphEdits.Collapse(model, pressGroup, pressGroup.isCollapsed()));
                for (int l = 0; l < listeners.size(); l++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                    listeners.get(l).groupToggled(this, pressGroup);
                }
            }
            return;
        }
        if (pressEdge != null) {
            selectEdge(pressEdge);
            return;
        }
        if (pendingTemplate != null && editable) {
            GraphNodeTemplate t = pendingTemplate;
            pendingTemplate = null;
            createNode(t, screenToWorldX(pressX), screenToWorldY(pressY));
            return;
        }
        if (!selection.isEmpty() || selectedEdge != null) {
            select(null);
        }
    }

    // -------------------------------------------------------------- animation

    private void animateNow() {
        canvas.animateNow();
    }

    // The arrangement's own motion, every frame while it runs.
    boolean tick() {
        boolean busy = false;
        if (animTo != null) {
            float t = Math.min(1f, (System.currentTimeMillis() - animStart) / (float) LAYOUT_MS);
            float e = 1 - (1 - t) * (1 - t) * (1 - t);
            for (GraphNode n : model.getNodes()) {
                double[] a = animFrom.get(n.getId());
                double[] b = animTo.getPosition(n.getId());
                if (a != null && b != null) {
                    n.setPosition(a[0] + (b[0] - a[0]) * e, a[1] + (b[1] - a[1]) * e);
                }
            }
            groupsDirty = true;
            if (t >= 1) {
                ArrangementResult r = animTo;
                animTo = null;
                animFrom = null;
                commit(r);
            } else {
                busy = true;
                syncComponents();
            }
        }
        if (live != null && liveRunning) {
            boolean more = live.step();
            ArrangementResult r = live.result();
            for (GraphNode n : model.getNodes()) {
                if (moving.contains(n)) {
                    continue;
                }
                double[] p = r.getPosition(n.getId());
                if (p != null) {
                    n.setPosition(p[0], p[1]);
                }
            }
            groupsDirty = true;
            syncComponents();
            if (!more && gesture != G_MOVE) {
                liveRunning = false;
                commit(r);
            } else {
                busy = true;
            }
        }
        return busy;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (liveRunning || animTo != null) {
            animateNow();
        }
    }

    @Override
    public void layoutContainer() {
        super.layoutContainer();
        if (!viewSet && canvas.getWidth() > 0 && canvas.getHeight() > 0 && !model.getNodes().isEmpty()) {
            fitToView();
        }
        syncComponents();
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(GraphLook.mm(60), GraphLook.mm(60));
    }

    // ------------------------------------------------------------- components

    private void syncComponents() {
        if (componentFactory == null && labelField == null && portEditorFactory == null) {
            return;
        }
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        boolean show = getZoom() >= componentZoom && w > 0 && h > 0;
        boolean changed = false;
        if (componentFactory != null) {
            List<GraphNode> nodes = model.getNodes();
            for (int i = 0; i < nodes.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
                GraphNode n = nodes.get(i);
                Component c = components.get(n.getId());
                int x = worldToScreenX(n.getX());
                int y = worldToScreenY(n.getY());
                int nw = worldToScreenLength(n.getWidth());
                int nh = worldToScreenLength(n.getHeight());
                boolean onScreen = show && isNodeVisible(n) && x < w && y < h && x + nw > 0 && y + nh > 0;
                if (onScreen) {
                    if (c == null && !components.containsKey(n.getId())) {
                        c = componentFactory.createComponent(this, n);
                        components.put(n.getId(), c);
                        if (c != null) {
                            c.putClientProperty("cn1$graphNode", n.getId());
                        }
                    }
                    if (c != null && c.getParent() == null) {
                        layer.add(c);
                        changed = true;
                    }
                } else if (c != null && c.getParent() != null) {
                    layer.removeComponent(c);
                    changed = true;
                }
            }
        }
        if (portEditorFactory != null && syncPortEditors(show, w, h)) {
            changed = true;
        }
        layer.layoutContainer();
        if (changed) {
            layer.repaint();
        }
    }

    /// Lays the node components and the label field over their nodes.
    private final class NodeLayout extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            int count = parent.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component c = parent.getComponentAt(i);
                String id = (String) c.getClientProperty("cn1$graphNode");
                GraphNode n = id == null ? null : model.getNode(id);
                if (n == null) {
                    c.setWidth(0);
                    c.setHeight(0);
                    continue;
                }
                int x = worldToScreenX(n.getX());
                int y = worldToScreenY(n.getY());
                int w = worldToScreenLength(n.getWidth());
                int h = worldToScreenLength(n.getHeight());
                String portId = (String) c.getClientProperty("cn1$graphPort");
                GraphPort port = portId == null ? null : n.getPort(portId);
                if (port != null) {
                    // the trailing half of the port's row, clear of its label
                    int ph = Math.min(c.getPreferredH(), Math.max(1, worldToScreenLength(
                            Math.max(1, n.getHeight() - HEADER) / Math.max(1, n.getInputs().size()))));
                    int inset = portRadiusOnScreen() + GraphLook.mm(1.5f);
                    c.setX(x + w / 2);
                    c.setY(worldToScreenY(portY(n, port)) - ph / 2);
                    c.setWidth(Math.max(0, w / 2 - inset));
                    c.setHeight(ph);
                } else if (c.getClientProperty("cn1$graphLabel") != null) {
                    int ph = c.getPreferredH();
                    c.setX(x);
                    c.setY(y + (h - ph) / 2);
                    c.setWidth(Math.max(w, GraphLook.mm(25)));
                    c.setHeight(ph);
                } else {
                    int top = headerHeightOnScreen();
                    int inset = Math.max(1, GraphLook.mm(1f));
                    int pad = n.hasPorts() ? portRadiusOnScreen() + GraphLook.mm(1.5f) : inset;
                    c.setX(x + pad);
                    c.setY(y + top + inset);
                    c.setWidth(Math.max(0, w - pad * 2));
                    c.setHeight(Math.max(0, h - top - inset * 2));
                }
                if (c instanceof Container) {
                    ((Container) c).layoutContainer();
                }
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(0, 0);
        }

        @Override
        public boolean isOverlapSupported() {
            return true;
        }
    }

    /// The painted surface, which takes every pointer event; for tests.
    Component canvas() {
        return canvas;
    }

    // ----------------------------------------------------------------- canvas

    /// The painted surface. `ScrollSurface` turns pointer, wheel and pinch
    /// input into gestures; the graph is unbounded, so its scroll offsets
    /// are only a counter whose change moves the view.
    private final class Canvas extends ScrollSurface {
        private int seenX;
        private int seenY;

        Canvas() {
            setUIID("Container");
            setFocusable(true);
            // asks a desktop port for the letter of a Control or Command
            // combination, which it otherwise keeps from a component
            putClientProperty("cn1$shortcutKeys", Boolean.TRUE);
            getAllStyles().setBgTransparency(0);
            setBounded(false);
        }

        void animateNow() {
            requestAnimation();
        }

        @Override
        protected void layoutSurface(int width, int height) {
        }

        @Override
        protected int getContentWidth() {
            return getWidth();
        }

        @Override
        protected int getContentHeight() {
            return getHeight();
        }

        @Override
        protected void paintContent(Graphics g, int x, int y) {
            // the view, not the offsets, places the graph
            int ox = getX();
            int oy = getY();
            g.translate(ox, oy);
            paintGraph(g, getWidth(), getHeight());
            g.translate(-ox, -oy);
        }

        @Override
        protected void paintOverlay(Graphics g, int x, int y) {
            g.translate(x, y);
            paintGraphOverlay(g, getWidth(), getHeight());
            g.translate(-x, -y);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(GraphLook.mm(60), GraphLook.mm(60));
        }

        @Override
        protected void scrollChanged() {
            int ox = getOffsetX();
            int oy = getOffsetY();
            viewX += (ox - seenX) / (double) scale;
            viewY += (oy - seenY) / (double) scale;
            seenX = ox;
            seenY = oy;
            viewSet = true;
            syncComponents();
        }

        @Override
        protected void pressStarted(int x, int y) {
            pressed(x, y);
        }

        @Override
        protected boolean editStarted(int x, int y, EditTrigger trigger) {
            // nodes and ports are handles of their own, so a plain drag and a
            // drag of a handle are the same here
            if (trigger == EditTrigger.LONG_PRESS) {
                return longPressStarted(x, y);
            }
            return dragStarted();
        }

        @Override
        protected void editMoved(int x, int y) {
            dragged(x, y);
        }

        @Override
        protected void editEnded(boolean committed) {
            ended(committed);
        }

        @Override
        protected boolean isEditAutoScroll(int x, int y) {
            return gesture != G_MINIMAP;
        }

        @Override
        protected void tapped(int x, int y, boolean doubleTap) {
            NodeGraph.this.tapped(doubleTap);
            repaint();
        }

        @Override
        protected boolean zoomBy(float factor, int x, int y) {
            return zoomAt(factor, x, y);
        }

        @Override
        protected boolean animateSurface() {
            return tick();
        }

        @Override
        public void keyPressed(int keyCode) {
            if (!keyTyped(keyCode)) {
                super.keyPressed(keyCode);
            }
        }
    }
}
