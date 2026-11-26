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
import com.codename1.components.Switch;
import com.codename1.graph.DefaultGraphModel;
import com.codename1.graph.GraphNode;
import com.codename1.graph.GraphNodeTemplate;
import com.codename1.graph.arrange.LayeredArrangement;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.graph.DefaultGraphNodeRenderer;
import com.codename1.ui.graph.GraphAdapter;
import com.codename1.ui.graph.GraphPalette;
import com.codename1.ui.graph.NodeGraph;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.util.UndoHistory;
import com.codenameone.examples.bazaar.api.AutomationDto;
import com.codenameone.examples.bazaar.api.NodeParamDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// One automation on a `NodeGraph`: a palette of the catalog's steps,
/// grouped and coloured by kind, the graph to drag them onto and join, and an
/// inspector for the selected step's parameters.
///
/// The graph does Delete, Cmd+Z / Cmd+Shift+Z, copy and paste, wheel and
/// pinch zoom by itself; this adds a menu on a step, validation against the
/// server with the faulty steps outlined, auto-arrange, and saving. On a
/// phone the palette is an Add step menu and the inspector a sheet.
final class AutomationEditor extends Container {
    private static final float SIDE_MM = 62f;

    private final AutomationsPane owner;
    private final List<NodeTypeDto> catalog;
    private final DefaultGraphModel model;
    private final NodeGraph graph;
    private final Marks renderer = new Marks();
    private final Container inspector = new Container(BoxLayout.y());
    private final Container problems = new Container(BoxLayout.y());
    private final TextField name;
    private final boolean wide = Layouts.wide();
    private AutomationDto automation;
    private boolean active;
    private List<String> errors = new ArrayList<String>();

    AutomationEditor(AutomationsPane owner, AutomationDto automation, List<NodeTypeDto> catalog) {
        super(new BorderLayout());
        setName("automation-editor");
        setUIID("MeAutoRoot");
        this.owner = owner;
        this.automation = automation;
        this.catalog = catalog;
        this.active = automation.active();
        model = AutomationCodec.fromJson(automation.graphJson());
        for (GraphNode n : model.getNodes()) {
            AutomationCodec.own(n, AutomationCodec.find(catalog, AutomationCodec.type(n)));
        }
        for (int iter = 0; iter < catalog.size(); iter++) {
            renderer.setAccent(catalog.get(iter).type(),
                    AutomationCodec.kindColor(catalog.get(iter).kind()));
        }
        graph = new NodeGraph(model);
        graph.setName("automation-graph");
        graph.setNodeRenderer(renderer);
        graph.setUserDataCodec(AutomationCodec.INSTANCE);
        graph.setUndoHistory(new UndoHistory());
        graph.setGridVisible(true);
        graph.setSnap(10);
        graph.setEdgeStyle(NodeGraph.EDGE_CURVED);
        graph.setMinimapVisible(wide);
        graph.addGraphListener(new GraphAdapter() {
            @Override
            public void nodeCreated(NodeGraph g, GraphNode node) {
                AutomationCodec.own(node, AutomationCodec.find(AutomationEditor.this.catalog,
                        node.getStyleKey()));
                inspect();
            }

            @Override
            public void selectionChanged(NodeGraph g) {
                inspect();
            }

            @Override
            public void nodeOpened(NodeGraph g, GraphNode node) {
                if (!wide) {
                    sheet(node);
                }
            }
        });
        RowMenus.attach(graph, this::menu);

        name = new TextField(automation.name() == null ? "" : automation.name(),
                Lang.tr("Automation name"), 20, TextField.ANY);
        name.setUIID("SvField");
        name.setName("automation-name");

        add(BorderLayout.NORTH, bar());
        Container centre = new Container(new BorderLayout());
        centre.add(BorderLayout.CENTER, graph);
        problems.setUIID("MeAutoProblems");
        problems.setName("automation-problems");
        centre.add(BorderLayout.SOUTH, problems);
        add(BorderLayout.CENTER, centre);
        if (wide) {
            add(BorderLayout.WEST, side(palettes()));
            inspector.setUIID("MeAutoInspector");
            inspector.setScrollableY(true);
            inspector.setName("automation-inspector");
            add(BorderLayout.EAST, side(inspector));
        }
        inspect();
        if (!model.getNodes().isEmpty()) {
            CN.callSerially(graph::fitToView);
        }
    }

    // --------------------------------------------------------------- the bar

    private Container bar() {
        Container bar = new Container(new BorderLayout());
        bar.setUIID("MeAutoBar");
        Container start = new Container(new BorderLayout());
        start.add(BorderLayout.CENTER, name);
        Switch on = Ui.toggle("automation-active", active, wanted -> active = wanted);
        Container onBox = new Container(BoxLayout.x());
        onBox.add(Ui.plain(Lang.tr("Active"), "SvLabel"));
        onBox.add(on);
        start.add(BorderLayout.EAST, onBox);
        bar.add(BorderLayout.NORTH, start);
        Container tools = new Container(new FlowLayout(Component.LEFT));
        tools.setUIID("Container");
        if (!wide) {
            tools.add(icon(FontImage.MATERIAL_ADD_BOX, "Add step", "automation-add", this::addMenu));
            tools.add(icon(FontImage.MATERIAL_TUNE, "Edit step", "automation-inspect", () -> {
                if (graph.getSelectedNodes().size() == 1) {
                    sheet(graph.getSelectedNodes().get(0));
                } else {
                    Ui.say("Select a step first");
                }
            }));
        }
        tools.add(icon(FontImage.MATERIAL_UNDO, "Undo", "automation-undo", graph::undo));
        tools.add(icon(FontImage.MATERIAL_REDO, "Redo", "automation-redo", graph::redo));
        tools.add(icon(FontImage.MATERIAL_DELETE, "Delete step", "automation-delete",
                graph::deleteSelection));
        tools.add(icon(FontImage.MATERIAL_ACCOUNT_TREE, "Arrange", "automation-arrange",
                this::arrange));
        tools.add(icon(FontImage.MATERIAL_ZOOM_IN, "Zoom in", "automation-zoom-in",
                () -> zoom(1.25f)));
        tools.add(icon(FontImage.MATERIAL_ZOOM_OUT, "Zoom out", "automation-zoom-out",
                () -> zoom(0.8f)));
        tools.add(icon(FontImage.MATERIAL_FIT_SCREEN, "Fit", "automation-fit", graph::fitToView));
        tools.add(icon(FontImage.MATERIAL_HISTORY, "Activity", "automation-activity",
                () -> owner.activity(automation)));
        tools.add(icon(FontImage.MATERIAL_RULE, "Check", "automation-validate",
                () -> validate(null)));
        Button save = Ui.primary(Lang.tr("Save"), "automation-save", e -> save());
        bar.add(BorderLayout.CENTER, tools);
        bar.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(save));
        return bar;
    }

    private Button icon(char icon, String text, String name, Runnable run) {
        Button b = new Button(wide ? Lang.tr(text) : "", "MeTool");
        b.setName(name);
        Ui.icon(b, icon, 3.2f);
        b.addActionListener(e -> run.run());
        return b;
    }

    private void zoom(float by) {
        graph.zoomAt(by, graph.getWidth() / 2, graph.getHeight() / 2);
        graph.repaint();
    }

    private void arrange() {
        LayeredArrangement layered = new LayeredArrangement();
        layered.setDirection(LayeredArrangement.LEFT_TO_RIGHT);
        layered.setLayerGap(70);
        layered.setNodeGap(30);
        graph.arrange(layered, true);
    }

    private Container side(Component content) {
        Container side = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension d = super.calcPreferredSize();
                d.setWidth(CN.convertToPixels(SIDE_MM));
                return d;
            }
        };
        side.setUIID("MeAutoSide");
        side.add(BorderLayout.CENTER, content);
        return side;
    }

    // ------------------------------------------------------------ the palette

    private Container palettes() {
        Container column = new Container(BoxLayout.y());
        column.setScrollableY(true);
        column.setName("automation-palette");
        String[] titles = {"Triggers", "Conditions", "Actions"};
        for (int k = 0; k < AutomationCodec.KINDS.length; k++) {
            List<NodeTypeDto> types = AutomationCodec.ofKind(catalog, AutomationCodec.KINDS[k]);
            if (types.isEmpty()) {
                continue;
            }
            Label heading = Ui.plain(Lang.tr(titles[k]), "MeAutoKind");
            heading.getAllStyles().setFgColor(AutomationCodec.kindColor(AutomationCodec.KINDS[k]));
            column.add(heading);
            GraphPalette palette = new GraphPalette(graph);
            palette.setName("palette-" + AutomationCodec.KINDS[k]);
            for (int iter = 0; iter < types.size(); iter++) {
                GraphNodeTemplate tpl = AutomationCodec.template(types.get(iter));
                tpl.setLabel(Lang.tr(tpl.getLabel()));
                palette.add(tpl);
            }
            column.add(palette);
        }
        column.add(Ui.text(Lang.tr("Drag a step onto the canvas, or tap it and then tap the "
                + "canvas. Join an output to an input to connect steps."), "SvMuted"));
        return column;
    }

    private void addMenu() {
        Command[] items = new Command[catalog.size()];
        for (int iter = 0; iter < catalog.size(); iter++) {
            final NodeTypeDto t = catalog.get(iter);
            items[iter] = Command.create(Lang.tr(t.label()) + Ui.DOT + Lang.tr(kindWord(t.kind())),
                    null, e -> {
                        GraphNodeTemplate tpl = AutomationCodec.template(t);
                        tpl.setLabel(Lang.tr(tpl.getLabel()));
                        double wx = graph.screenToWorldX(graph.getWidth() / 2)
                                - AutomationCodec.WIDTH / 2;
                        double wy = graph.screenToWorldY(graph.getHeight() / 2)
                                - AutomationCodec.HEIGHT / 2;
                        GraphNode made = graph.createNode(tpl, wx, wy);
                        if (made != null) {
                            graph.select(made);
                        }
                    });
        }
        com.codename1.ui.ContextMenu.show(graph, graph.getWidth() / 2, 0, items);
    }

    static String kindWord(String kind) {
        if (AutomationCodec.TRIGGER.equalsIgnoreCase(kind)) {
            return "Trigger";
        }
        if (AutomationCodec.CONDITION.equalsIgnoreCase(kind)) {
            return "Condition";
        }
        return "Action";
    }

    // --------------------------------------------------------- the step menu

    private Command[] menu() {
        final List<GraphNode> picked = graph.getSelectedNodes();
        List<Command> items = new ArrayList<Command>();
        if (picked.size() == 1) {
            final GraphNode n = picked.get(0);
            items.add(Command.create(Lang.tr("Edit step"), null, e -> {
                if (wide) {
                    inspect();
                } else {
                    sheet(n);
                }
            }));
            items.add(Command.create(Lang.tr("Centre"), null, e -> graph.centerOn(n)));
        }
        if (!picked.isEmpty()) {
            items.add(Command.create(Lang.tr("Duplicate"), null, e -> graph.duplicateSelection()));
            items.add(Command.create(Lang.tr("Delete step"), null, e -> graph.deleteSelection()));
        }
        items.add(Command.create(Lang.tr("Arrange"), null, e -> arrange()));
        items.add(Command.create(Lang.tr("Fit"), null, e -> graph.fitToView()));
        return items.toArray(new Command[items.size()]);
    }

    // ------------------------------------------------------------ inspector

    private void inspect() {
        if (!wide) {
            return;
        }
        inspector.removeAll();
        List<GraphNode> picked = graph.getSelectedNodes();
        if (picked.size() != 1) {
            inspector.add(Ui.plain(Lang.tr("Step"), "MeAutoKind"));
            inspector.add(Ui.text(Lang.tr("Select a step to change what it does."), "SvMuted"));
        } else {
            fill(inspector, picked.get(0));
        }
        Ui.relayout(inspector);
    }

    private void sheet(GraphNode node) {
        Sheet sheet = new Sheet(null, Lang.tr("Step"));
        Container content = sheet.getContentPane();
        content.setLayout(BoxLayout.y());
        content.setScrollableY(true);
        fill(content, node);
        sheet.show();
    }

    private void fill(Container to, final GraphNode node) {
        final NodeTypeDto t = AutomationCodec.find(catalog, AutomationCodec.type(node));
        Label kind = Ui.plain(Lang.tr(kindWord(t == null ? null : t.kind())), "MeAutoKind");
        kind.getAllStyles().setFgColor(AutomationCodec.kindColor(t == null ? null : t.kind()));
        to.add(kind);
        to.add(Ui.plain(t == null ? AutomationCodec.type(node) : Lang.tr(t.label()), "SvRowTitle"));
        if (t != null && t.description() != null && t.description().length() > 0) {
            to.add(Ui.text(Lang.tr(t.description()), "SvMuted"));
        }
        final TextField label = MUi.text(to, "Label", "step-label", node.getLabel());
        label.addActionListener(e -> {
            model.renameNode(node, label.getText());
            graph.repaint();
        });
        label.addDataChangedListener((type, index) -> {
            node.setLabel(label.getText());
            graph.repaint();
        });
        for (int iter = 0; t != null && t.params() != null && iter < t.params().size(); iter++) {
            param(to, node, t.params().get(iter));
        }
        MessageVariants.step(to, node, AutomationCodec.type(node), this::changed);
        List<String> mine = errorsOf(node);
        for (int iter = 0; iter < mine.size(); iter++) {
            to.add(Ui.text(mine.get(iter), "SvError"));
        }
        Button remove = Ui.danger(Lang.tr("Delete step"), "step-delete", e -> {
            graph.select(node);
            graph.deleteSelection();
            Sheet open = Sheet.getCurrentSheet();
            if (open != null) {
                open.back();
            }
        });
        to.add(remove);
    }

    private void param(Container to, final GraphNode node, final NodeParamDto p) {
        String caption = Lang.tr(p.label() == null ? p.name() : p.label());
        String value = AutomationCodec.param(node, p.name());
        String kind = p.kind() == null ? "TEXT" : p.kind();
        if ("CHOICE".equalsIgnoreCase(kind) && p.choices() != null && !p.choices().isEmpty()) {
            final String[] codes = p.choices().toArray(new String[p.choices().size()]);
            final Picker picker = MUi.picker(to, caption, "param-" + p.name(), codes, value);
            picker.addActionListener(e -> {
                AutomationCodec.setParam(node, p.name(), MUi.code(picker, codes));
                changed();
            });
            return;
        }
        if ("MONEY".equalsIgnoreCase(kind)) {
            final TextField f = MUi.money(to, caption, "param-" + p.name(), cents(value));
            f.addDataChangedListener((type, index) -> {
                AutomationCodec.setParam(node, p.name(), Long.toString(MUi.cents(f)));
                changed();
            });
            return;
        }
        if ("NUMBER".equalsIgnoreCase(kind)) {
            final TextField f = MUi.number(to, caption, "param-" + p.name(), cents(value));
            f.addDataChangedListener((type, index) -> {
                AutomationCodec.setParam(node, p.name(), Integer.toString(MUi.integer(f)));
                changed();
            });
            return;
        }
        final TextField f = MUi.text(to, caption, "param-" + p.name(), value);
        f.addDataChangedListener((type, index) -> {
            AutomationCodec.setParam(node, p.name(), f.getText());
            changed();
        });
    }

    private static long cents(String value) {
        try {
            return value == null || value.length() == 0 ? 0 : Long.parseLong(value.trim());
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    private void changed() {
        if (!errors.isEmpty()) {
            errors = new ArrayList<String>();
            renderer.marked.clear();
            problems.removeAll();
            Ui.relayout(this);
        }
    }

    private List<String> errorsOf(GraphNode node) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < errors.size(); iter++) {
            if (AutomationCodec.word(errors.get(iter), node.getId())) {
                out.add(errors.get(iter));
            }
        }
        return out;
    }

    // ------------------------------------------------------ validate and save

    private AutomationDto current() {
        String n = name.getText().trim();
        return new AutomationDto(automation.id(), n.length() == 0 ? Lang.tr("Automation") : n,
                AutomationCodec.triggerKind(model), AutomationCodec.toJson(model), active,
                automation.runs(), automation.lastRunAt(), automation.updatedAt());
    }

    private void validate(final Runnable whenValid) {
        Api.automations().validate(MerchantState.storeId(), current(), Net.to(v -> {
            show(v.errors());
            if (v.valid()) {
                if (whenValid != null) {
                    whenValid.run();
                } else {
                    Ui.say("The automation is ready to run");
                }
            }
        }, Ui::fail));
    }

    private void show(List<String> found) {
        errors = found == null ? new ArrayList<String>() : new ArrayList<String>(found);
        renderer.marked.clear();
        renderer.marked.addAll(AutomationCodec.nodesNamed(model, errors));
        problems.removeAll();
        for (int iter = 0; iter < errors.size(); iter++) {
            Label l = Ui.plain(errors.get(iter), "MeAutoProblem");
            FontImage.setMaterialIcon(l, FontImage.MATERIAL_ERROR_OUTLINE, 2.8f);
            problems.add(l);
        }
        graph.repaint();
        inspect();
        Ui.relayout(this);
    }

    private void save() {
        if (active) {
            validate(this::store);
        } else {
            store();
        }
    }

    private void store() {
        AutomationDto wanted = current();
        Net.Ok<AutomationDto> done = saved -> {
            automation = saved;
            Ui.say("Saved");
            owner.saved();
        };
        if (automation.id() == null) {
            Api.automations().create(MerchantState.storeId(), wanted, Net.to(done, Ui::fail));
        } else {
            Api.automations().save(MerchantState.storeId(), automation.id(), wanted,
                    Net.to(done, Ui::fail));
        }
    }

    /// The default node look, with a red outline and a dot on the steps the
    /// last check found fault with.
    static final class Marks extends DefaultGraphNodeRenderer {
        final Set<String> marked = new HashSet<String>();

        @Override
        public void paintNode(Graphics g, NodeGraph graph, GraphNode node, int x, int y, int w,
                int h, boolean selected) {
            super.paintNode(g, graph, node, x, y, w, h, selected);
            if (!marked.contains(node.getId())) {
                return;
            }
            int color = g.getColor();
            int alpha = g.getAlpha();
            g.setColor(0xdc2626);
            g.setAlpha(255);
            int stroke = Math.max(1, CN.convertToPixels(0.4f));
            for (int iter = 0; iter < stroke; iter++) {
                g.drawRect(x - iter - 1, y - iter - 1, w + 2 * iter + 1, h + 2 * iter + 1);
            }
            int r = Math.max(4, Math.min(w, h) / 6);
            g.fillArc(x + w - r, y - r, r * 2, r * 2, 0, 360);
            g.setColor(color);
            g.setAlpha(alpha);
        }
    }
}
