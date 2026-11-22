/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.studio;

import com.codename1.components.SpanLabel;
import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphEdge;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphPort;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.graph.NodeGraph;
import com.codenameone.examples.brickyard.rbx.Instance;
import com.codenameone.examples.brickyard.ui.Ui;

/// The experimental Logic tab: the selected part's [LogicGraph] drawn as a
/// node chain, a fixed palette that edits it, and Apply, which writes the
/// compiled Luau into the part's `Logic` script through the same
/// `write_script` tool the Script tab uses.
///
/// The [LogicGraph] is the source of truth and the [NodeGraph] a view of it,
/// rebuilt after every palette edit; nodes can be dragged and zoomed, not
/// wired by hand. A script that does not read back as a graph (written or
/// edited as text, or a corrupt header) is never overwritten from here
/// without the creator asking: the tab says the graph is detached and
/// offers the Script tab.
final class LogicPanel {
    private final Studio studio;
    private final Container root = new Container(new BorderLayout());
    private Instance part;
    private LogicGraph graph;

    LogicPanel(Studio studio) {
        this.studio = studio;
        root.setUIID("StudioPanel");
        root.setName("logicPanel");
    }

    Component view() {
        return root;
    }

    /// Shows the logic of `selected`: a part, or a part's Logic script.
    void show(Instance selected) {
        root.removeAll();
        part = null;
        graph = null;
        Instance p = selected;
        if (p != null && !p.isA("BasePart") && p.getParent() != null && p.getParent().isA("BasePart")) {
            p = p.getParent();
        }
        if (p == null || !p.isA("BasePart")) {
            root.add(BorderLayout.CENTER, hint("Select a part to give it logic: a touch trigger and what it does."));
            root.revalidate();
            return;
        }
        part = p;
        Instance script = p.findFirstChild(LogicGraph.SCRIPT_NAME);
        Object src = script == null ? null : script.storedProps().get("Source");
        if (src != null) {
            graph = LogicGraph.fromSource(String.valueOf(src));
            if (graph == null) {
                detached();
                return;
            }
        } else {
            graph = LogicGraph.collectible();
        }
        render(src == null);
    }

    private void detached() {
        Container c = new Container(BoxLayout.y());
        c.add(hint("This part's Logic script was written or edited as text, so the graph is detached. "
                + "The script keeps running as written; edit it in the Script tab."));
        c.add(Ui.secondary("Open in the Script tab", "logicOpenScript", ev -> studio.showTab(2)));
        c.add(Ui.secondary("Replace with a new graph", "logicReplace", ev -> {
            graph = LogicGraph.collectible();
            render(true);
        }));
        root.add(BorderLayout.CENTER, c);
        root.revalidate();
    }

    private void render(boolean unsaved) {
        root.removeAll();
        DefaultGraphModel model = new LockedModel();
        double w = 200;
        double h = 52;
        double gap = 34;
        GraphNode prev = new GraphNode("trigger", "Touched (" + part.getName() + ")", 0, 0, w, h);
        model.addNode(prev);
        int i = 0;
        for (LogicGraph.Step s : graph.steps()) {
            i++;
            // Left to right: the pane is wider than tall on a phone and on a
            // desktop, so a column fitted to it shrank every node to a speck.
            GraphNode n = new GraphNode("step" + i, s.label(), i * (w + gap), 0, w, h);
            model.addNode(n);
            model.addEdge(prev.getId(), n.getId());
            prev = n;
        }
        NodeGraph view = new NodeGraph(model);
        view.setName("logicGraph");
        view.setMinimapVisible(false);
        root.add(BorderLayout.CENTER, view);

        // One row that scrolls sideways: wrapped, the steps took four rows of
        // a phone's pane and left the graph above them no height at all.
        Container palette = new Container(BoxLayout.x());
        palette.setScrollableX(true);
        palette.setName("logicPalette");
        palette.add(step("Is a player?", LogicGraph.IS_PLAYER));
        palette.add(step("Add to leaderstat", LogicGraph.ADD_STAT));
        palette.add(step("Flash part", LogicGraph.FLASH));
        palette.add(step("Destroy part", LogicGraph.DESTROY));
        palette.add(Ui.secondary("Remove last", "logicRemoveLast", ev -> {
            graph.removeLast();
            render(true);
        }));
        Container south = BoxLayout.encloseY(palette,
                Ui.primary(unsaved ? "Apply to " + part.getName() : "Applied - apply again", "logicApply",
                        ev -> apply()));
        root.add(BorderLayout.SOUTH, south);
        root.add(BorderLayout.NORTH, hint("Experimental: the graph compiles to the Luau in this part's "
                + LogicGraph.SCRIPT_NAME + " script."));
        root.revalidate();
        final GraphNode newest = prev;
        CN.callSerially(() -> {
            // Fitted, a long chain in a phone's short pane shrank to boxes no
            // one could read: below full size it pans instead, on the newest
            // step, the one just added.
            view.fitToView();
            if (view.getZoom() < 1f) {
                view.setZoom(1f);
                view.centerOn(newest);
            }
        });
    }

    private Button step(String label, final String kind) {
        return Ui.secondary("+ " + label, "logicAdd-" + kind, ev -> {
            if (graph.add(kind)) {
                render(true);
            } else {
                Ui.toast("A behavior holds up to eight steps");
            }
        });
    }

    /// The view's model refuses every structural edit made on the canvas --
    /// renaming, deleting, wiring, pasting -- so what is drawn is always what
    /// [LogicGraph#toLuau()] compiles. Moving a node is allowed; it is layout.
    static final class LockedModel extends DefaultGraphModel {
        @Override
        public GraphEdge connect(GraphNode source, GraphPort sourcePort, GraphNode target, GraphPort targetPort) {
            return null;
        }

        @Override
        public boolean removeEdge(GraphEdge edge) {
            return false;
        }

        @Override
        public boolean removeNode(GraphNode node) {
            return false;
        }

        @Override
        public boolean renameNode(GraphNode node, String label) {
            return false;
        }

        @Override
        public boolean insertNode(GraphNode node, int index) {
            return false;
        }

        @Override
        public boolean insertEdge(GraphEdge edge, int index) {
            return false;
        }
    }

    private void apply() {
        if (part == null || graph == null) {
            return;
        }
        studio.writeLogic(part, graph.toLuau());
        show(part);
    }

    private static Component hint(String text) {
        SpanLabel l = new SpanLabel(text);
        l.setTextUIID("Muted");
        return l;
    }
}
