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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphNodeTemplate;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.graph.DefaultGraphNodeRenderer;
import com.codename1.ui.graph.GraphAdapter;
import com.codename1.ui.graph.GraphPalette;
import com.codename1.ui.graph.NodeGraph;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.bazaar.api.JourneyDto;
import com.codenameone.examples.bazaar.api.NodeParamDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Customer journeys: the list, and an editor on `NodeGraph` with the
/// journey palette (entries, steps, splits, exits), the parameters of the
/// selected node, and a live overlay of how many customers sit at each node
/// and how many have passed it, refreshed every few seconds while the editor
/// is on screen. The graph is saved as the same JSON the automations use.
final class JourneysPane extends MerchantPane {
    private List<NodeTypeDto> catalog;

    JourneysPane() {
        super("Journeys", "merchant-journeys");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "New journey", "journey-new", () -> open(
                new JourneyDto(null, Lang.tr("New journey"), "", "DRAFT", 0, null)));
        Api.journeys().catalog(MerchantState.storeId(), Net.to(found -> {
            catalog = found;
            Api.journeys().list(MerchantState.storeId(), Net.to(this::paint, Ui::fail));
        }, Ui::fail));
    }

    private void paint(List<JourneyDto> journeys) {
        Container page = MUi.page("journey-list");
        Container list = new Container(BoxLayout.y());
        list.setUIID("MeCard");
        if (journeys.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_ROUTE, "No journeys yet",
                    "Start one with New journey."));
        }
        for (int iter = 0; iter < journeys.size(); iter++) {
            final JourneyDto j = journeys.get(iter);
            list.add(Ui.row(FontImage.MATERIAL_ROUTE, j.name(), Lang.tr(Words.of(j.status())),
                    Ui.flat(FontImage.MATERIAL_EDIT, "journey-edit-" + iter, e -> open(j))));
        }
        page.add(list);
        root(page);
    }

    private void open(JourneyDto j) {
        if (catalog == null) {
            return;
        }
        push(j.name(), new Editor(j));
    }

    /// Paints each node as usual, then its live count under it.
    static final class Counts extends DefaultGraphNodeRenderer {
        Map<String, long[]> counts = new HashMap<String, long[]>();

        @Override
        public void paintNode(Graphics g, NodeGraph graph, GraphNode node, int x, int y, int w,
                int h, boolean selected) {
            super.paintNode(g, graph, node, x, y, w, h, selected);
            long[] c = counts.get(node.getId());
            if (c == null) {
                return;
            }
            String text = JourneyGraph.badge(c);
            if (text == null) {
                return;
            }
            int color = g.getColor();
            int tw = g.getFont().stringWidth(text);
            int th = g.getFont().getHeight();
            g.setColor(0x1e293b);
            g.fillRoundRect(x + (w - tw) / 2 - th / 3, y + h + 2, tw + th * 2 / 3, th, th, th);
            g.setColor(0xffffff);
            g.drawString(text, x + (w - tw) / 2, y + h + 2);
            g.setColor(color);
        }
    }

    private final class Editor extends Container {
        private final JourneyDto journey;
        private final NodeGraph graph;
        private final Counts renderer = new Counts();
        private final Container inspector = new Container(BoxLayout.y());
        private final TextField name;
        private UITimer refresh;

        Editor(JourneyDto j) {
            super(new BorderLayout());
            journey = j;
            DefaultGraphModel model = JourneyGraph.fromJson(j.graphJson(), catalog);
            graph = new NodeGraph(model);
            graph.setName("journey-graph");
            graph.setNodeRenderer(renderer);
            graph.setUserDataCodec(AutomationCodec.INSTANCE);
            graph.setGridVisible(true);
            graph.setEdgeStyle(NodeGraph.EDGE_CURVED);
            graph.addGraphListener(new GraphAdapter() {
                @Override
                public void nodeCreated(NodeGraph g, GraphNode node) {
                    AutomationCodec.own(node, AutomationCodec.find(catalog, node.getStyleKey()));
                    inspect();
                }

                @Override
                public void selectionChanged(NodeGraph g) {
                    inspect();
                }
            });
            name = new TextField(j.name() == null ? "" : j.name(), Lang.tr("Journey name"), 20,
                    TextField.ANY);
            name.setUIID("SvField");
            name.setName("journey-name");
            Container top = new Container(new BorderLayout());
            top.add(BorderLayout.CENTER, name);
            top.add(BorderLayout.EAST, Ui.primary(Lang.tr("Save"), "journey-save", e -> save()));
            add(BorderLayout.NORTH, top);
            add(BorderLayout.WEST, palette());
            add(BorderLayout.CENTER, graph);
            inspector.setName("journey-inspector");
            add(BorderLayout.EAST, inspector);
            inspect();
        }

        private Container palette() {
            Container column = new Container(BoxLayout.y());
            column.setScrollableY(true);
            column.setName("journey-palette");
            for (int k = 0; k < JourneyGraph.KINDS.length; k++) {
                GraphPalette palette = null;
                for (int iter = 0; iter < catalog.size(); iter++) {
                    NodeTypeDto t = catalog.get(iter);
                    if (!JourneyGraph.KINDS[k].equals(t.kind())) {
                        continue;
                    }
                    if (palette == null) {
                        Label heading = Ui.plain(Lang.tr(JourneyGraph.kindWord(t.kind())),
                                "MeAutoKind");
                        heading.getAllStyles().setFgColor(JourneyGraph.kindColor(t.kind()));
                        column.add(heading);
                        palette = new GraphPalette(graph);
                        palette.setName("journey-palette-" + JourneyGraph.KINDS[k]);
                        column.add(palette);
                    }
                    GraphNodeTemplate tpl = AutomationCodec.template(t);
                    tpl.setLabel(Lang.tr(tpl.getLabel()));
                    palette.add(tpl);
                }
            }
            return column;
        }

        private void inspect() {
            inspector.removeAll();
            List<GraphNode> picked = graph.getSelectedNodes();
            if (picked.size() != 1) {
                List<String> problems = JourneyGraph.problems(graph.getModel(), catalog);
                inspector.add(Ui.text(problems.isEmpty() ? Lang.tr("Ready to save")
                        : Lang.tr(problems.get(0)), "SvMuted"));
                Ui.relayout(inspector);
                return;
            }
            final GraphNode n = picked.get(0);
            NodeTypeDto t = AutomationCodec.find(catalog, AutomationCodec.type(n));
            inspector.add(Ui.plain(t == null ? AutomationCodec.type(n) : Lang.tr(t.label()),
                    "SvRowTitle"));
            for (int iter = 0; t != null && t.params() != null && iter < t.params().size(); iter++) {
                final NodeParamDto p = t.params().get(iter);
                final TextField f = MUi.text(inspector, p.label(), "journey-param-" + p.name(),
                        AutomationCodec.param(n, p.name()));
                f.addDataChangedListener((type, index) ->
                        AutomationCodec.setParam(n, p.name(), f.getText().trim()));
            }
            Ui.relayout(inspector);
        }

        private void save() {
            List<String> problems = JourneyGraph.problems(graph.getModel(), catalog);
            if (!problems.isEmpty()) {
                Ui.say(problems.get(0));
                return;
            }
            JourneyDto wanted = new JourneyDto(journey.id(), name.getText().trim(),
                    JourneyGraph.toJson(graph.getModel()), journey.status(), journey.createdAt(),
                    null);
            Net.Ok<JourneyDto> done = saved -> {
                Ui.say("Saved");
                pop();
                load();
            };
            if (journey.id() == null) {
                Api.journeys().create(MerchantState.storeId(), wanted, Net.to(done, Ui::fail));
            } else {
                Api.journeys().save(MerchantState.storeId(), journey.id(), wanted,
                        Net.to(done, Ui::fail));
            }
        }

        private void poll() {
            if (journey.id() == null) {
                return;
            }
            Api.journeys().stats(MerchantState.storeId(), journey.id(), Net.to(stats -> {
                renderer.counts = JourneyGraph.counts(stats);
                graph.repaint();
            }, (status, message) -> { }));
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            poll();
            if (refresh == null && getComponentForm() != null) {
                refresh = UITimer.timer(5000, true, getComponentForm(), this::poll);
            }
        }

        @Override
        protected void deinitialize() {
            if (refresh != null) {
                refresh.cancel();
                refresh = null;
            }
            super.deinitialize();
        }
    }
}
