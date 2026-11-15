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

import com.codename1.rbx.AgentMcp;
import com.codename1.rbx.AgentTools;
import com.codename1.rbx.Engine;
import com.codename1.rbx.Instance;
import com.codename1.rbx.Scheduler;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceLoader;
import com.codename1.rbx.place.PlaceSaver;
import com.codename1.rbx.place.RbxlxWriter;
import com.codename1.rbx.place.Templates;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.brickyard.api.ExperienceSaveDto;
import com.codenameone.examples.brickyard.api.PlaceUploadDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.play.PlaySession;
import com.codenameone.examples.brickyard.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The creator's editor. Every edit -- from a button here or from an AI
/// agent through the app's MCP server -- goes through the same
/// [AgentTools], so what an agent can do and what a creator can do are one
/// list. The tabs are Explorer (the instance tree), Properties (of the
/// selection), Script (the selected script's source) and Output.
public final class Studio implements AgentTools.Studio {
    private static final int UNDO_DEPTH = 30;
    private static Studio current;
    private static boolean agentInstalled;

    private String experienceId;
    private String title;
    private Engine engine;
    private final AgentTools tools = new AgentTools(this);
    private final List<byte[]> undo = new ArrayList<byte[]>();
    private Instance selected;
    private final Form form;
    private final Container explorer = new Container(BoxLayout.y());
    private final Container properties = new Container(BoxLayout.y());
    private final TextArea source = new TextArea(12, 40);
    private final TextArea output = new TextArea(12, 40);

    private Studio(String experienceId, String title) {
        this.experienceId = experienceId;
        this.title = title;
        form = new Form(title, new BorderLayout());
        form.setName("studio");
        source.setName("scriptSource");
        output.setEditable(false);
        output.setName("studioOutput");
        replace(newEngine());
    }

    /// Opens `experienceId` for editing, or a new experience when it is null.
    public static void open(final String experienceId) {
        final Form previous = CN.getCurrentForm();
        if (experienceId == null) {
            Studio s = new Studio(null, "New experience");
            s.applyTemplate("obby");
            s.show(previous);
            return;
        }
        Api.experiences().place(experienceId, Net.to(place -> {
            Studio s = new Studio(experienceId, "Studio");
            byte[] bytes = place == null || place.data() == null ? null : Base64.decode(utf8(place.data()));
            if (bytes == null) {
                s.applyTemplate("obby");
            } else {
                Engine e = newEngine();
                PlaceLoader.loadPlace(e, PlaceFile.read(bytes));
                s.replace(e);
            }
            s.show(previous);
        }, Ui::failed));
    }

    private static Engine newEngine() {
        Engine e = new Engine(Engine.SOLO);
        e.setStudio(true);
        return e;
    }

    private void show(final Form previous) {
        current = this;
        Tabs tabs = new Tabs();
        tabs.addTab("Explorer", wrap(explorer));
        tabs.addTab("Properties", wrap(properties));
        Container script = new Container(new BorderLayout());
        script.add(BorderLayout.CENTER, source);
        script.add(BorderLayout.SOUTH, Ui.primary("Save script", "saveScript", ev -> saveScript()));
        tabs.addTab("Script", script);
        tabs.addTab("Output", wrap(output));
        form.add(BorderLayout.CENTER, tabs);
        Container bar = new Container(new GridLayout(1, 4));
        bar.add(Ui.primary("Play", "studioPlay", ev -> playtest()));
        bar.add(Ui.secondary("Part", "studioPart", ev -> edit("create_instance",
                "{\"class\":\"Part\",\"parent\":\"Workspace\",\"properties\":{\"Anchored\":true,\"CFrame\":[0,6,0]}}")));
        bar.add(Ui.secondary("Script", "studioScript", ev -> edit("write_script",
                "{\"parent\":\"ServerScriptService\",\"name\":\"Script\",\"source\":\"print('hello')\"}")));
        bar.add(Ui.secondary("Undo", "studioUndo", ev -> undo()));
        form.add(BorderLayout.SOUTH, bar);
        form.getToolbar().addCommandToOverflowMenu(new Command("Templates") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                pickTemplate();
            }
        });
        form.getToolbar().addCommandToOverflowMenu(new Command("Delete selected") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (selected != null) {
                    edit("delete_instance", "{\"path\":" + q(selected.fullName()) + "}");
                }
            }
        });
        form.getToolbar().addCommandToOverflowMenu(new Command("Compatibility report") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                log(tools.call("compat_report", "{}"));
            }
        });
        form.getToolbar().addCommandToOverflowMenu(new Command("Export .rbxlx") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                exportPlace();
            }
        });
        form.getToolbar().addCommandToOverflowMenu(new Command("AI agent mode") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                agentMode();
            }
        });
        form.getToolbar().addCommandToOverflowMenu(new Command("Publish") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                TextField notes = new TextField("", "What changed", 30, TextField.ANY);
                if (Dialog.show("Publish", notes, new Command("Cancel"), new Command("Publish")).getCommandName()
                        .equals("Publish")) {
                    log(publish(notes.getText()));
                }
            }
        });
        Ui.back(form, () -> {
            if (current == this) {
                current = null;
            }
            if (previous != null) {
                previous.showBack();
            }
        });
        form.show();
    }

    private static Container wrap(com.codename1.ui.Component c) {
        Container w = new Container(new BorderLayout());
        w.setScrollableY(true);
        w.add(BorderLayout.CENTER, c);
        return w;
    }

    // ---- AgentTools.Studio ----

    @Override
    public Engine edited() {
        return engine;
    }

    @Override
    public void replace(Engine e) {
        engine = e;
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                log(type + ": " + text);
            }
        });
        selected = null;
        refresh();
    }

    @Override
    public byte[] screenshot() {
        return null;
    }

    @Override
    public String publish(String notes) {
        final PlaceUploadDto upload = new PlaceUploadDto(Base64.encodeNoNewline(
                RbxlxWriter.writeBytes(PlaceSaver.save(engine))));
        if (experienceId == null) {
            Api.experiences().create(new ExperienceSaveDto(title, notes, "obby", 12, "public", "everyone"),
                    Net.to(created -> {
                        experienceId = created.id();
                        Api.experiences().publish(experienceId, upload,
                                Net.to(v -> log("Published version " + v.number()), Ui::failed));
                    }, Ui::failed));
            return "Creating the experience and publishing its first version";
        }
        Api.experiences().publish(experienceId, upload,
                Net.to(v -> log("Published version " + v.number()), Ui::failed));
        return "Publishing a new version of " + experienceId;
    }

    // ---- editing ----

    /// Runs one tool as an undoable edit and refreshes the views.
    private void edit(String tool, String args) {
        byte[] before = RbxlxWriter.writeBytes(PlaceSaver.save(engine));
        try {
            log(tool + " -> " + tools.call(tool, args));
            undo.add(before);
            if (undo.size() > UNDO_DEPTH) {
                undo.remove(0);
            }
        } catch (RuntimeException err) {
            Ui.toast(String.valueOf(err.getMessage()));
        }
        refresh();
    }

    private void undo() {
        if (undo.isEmpty()) {
            Ui.toast("Nothing to undo");
            return;
        }
        Engine e = newEngine();
        PlaceLoader.loadPlace(e, PlaceFile.read(undo.remove(undo.size() - 1)));
        replace(e);
    }

    private void applyTemplate(String id) {
        try {
            tools.call("apply_template", "{\"id\":" + q(id) + "}");
        } catch (RuntimeException err) {
            Ui.toast(String.valueOf(err.getMessage()));
        }
        refresh();
    }

    private void pickTemplate() {
        Container c = new Container(BoxLayout.y());
        final Dialog d = new Dialog("Start from a template");
        for (final String id : Templates.ids()) {
            c.add(Ui.secondary(id, "template-" + id, ev -> {
                d.dispose();
                byte[] before = RbxlxWriter.writeBytes(PlaceSaver.save(engine));
                undo.add(before);
                applyTemplate(id);
            }));
        }
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, c);
        d.show();
    }

    private void saveScript() {
        if (selected == null || !(selected.storedProps().containsKey("Source"))) {
            Ui.toast("Select a script first");
            return;
        }
        Instance parent = selected.getParent();
        edit("write_script", "{\"parent\":" + q(parent == null ? "game" : parent.fullName()) + ",\"name\":"
                + q(selected.getName()) + ",\"source\":" + q(source.getText()) + "}");
    }

    private void playtest() {
        byte[] bytes = RbxlxWriter.writeBytes(PlaceSaver.save(engine));
        PlaySession.start(experienceId == null ? "draft" : experienceId, title + " (test)", bytes);
    }

    private void exportPlace() {
        String path = CN.getAppHomePath() + "brickyard-export.rbxlx";
        try (java.io.OutputStream out = CN.createStorageOutputStream("brickyard-export.rbxlx")) {
            out.write(RbxlxWriter.writeBytes(PlaceSaver.save(engine)));
            log("Exported to storage as brickyard-export.rbxlx (" + path + ")");
        } catch (java.io.IOException err) {
            Ui.toast(String.valueOf(err.getMessage()));
        }
    }

    /// Registers the studio's tools with the app's built-in MCP server, so an
    /// AI agent connected to the device or simulator can build the place:
    /// `brickyard_place_tree`, `brickyard_create_instance`,
    /// `brickyard_write_script`, `brickyard_playtest` and the rest. The tools
    /// always act on the studio that is open.
    private void agentMode() {
        if (!agentInstalled) {
            agentInstalled = true;
            AgentMcp.install(new AgentTools.Studio() {
                @Override
                public Engine edited() {
                    return studio().edited();
                }

                @Override
                public void replace(Engine e) {
                    studio().replace(e);
                }

                @Override
                public byte[] screenshot() {
                    return studio().screenshot();
                }

                @Override
                public String publish(String notes) {
                    return studio().publish(notes);
                }
            });
        }
        Dialog.show("AI agent mode", "The studio's tools are registered with this app's MCP server. Connect "
                + "an agent to it and ask it to build: it can read the place, create parts and scripts, add "
                + "effects, playtest and publish. Edits show here as it works.", "OK", null);
    }

    private static Studio studio() {
        if (current == null) {
            throw new IllegalStateException("Open the studio first");
        }
        return current;
    }

    // ---- views ----

    private void refresh() {
        if (!CN.isEdt()) {
            CN.callSerially(this::refresh);
            return;
        }
        explorer.removeAll();
        addTree(engine.getGame(), 0);
        showProperties();
        form.revalidate();
    }

    private void addTree(Instance i, int depth) {
        if (depth > 0) {
            StringBuilder indent = new StringBuilder();
            for (int d = 1; d < depth; d++) {
                indent.append("    ");
            }
            final Instance target = i;
            Button b = new Button(indent + i.getName() + "  (" + i.getClassName() + ")");
            b.setUIID(i == selected ? "StudioNodeSelected" : "StudioNode");
            b.addActionListener(ev -> select(target));
            explorer.add(b);
        }
        if (depth < 6) {
            for (Instance c : i.getChildren()) {
                addTree(c, depth + 1);
            }
        }
    }

    private void select(Instance i) {
        selected = i;
        Object src = i.storedProps().get("Source");
        source.setText(src == null ? "" : String.valueOf(src));
        refresh();
    }

    private void showProperties() {
        properties.removeAll();
        if (selected == null) {
            properties.add(new Label("Select an instance in the Explorer"));
            return;
        }
        properties.add(new Label(selected.fullName()));
        for (Map.Entry<String, Object> p : selected.storedProps().entrySet()) {
            final String key = p.getKey();
            if ("Source".equals(key)) {
                continue;
            }
            final TextField f = new TextField(AgentTools.json(p.getValue()), key, 20, TextField.ANY);
            f.setName("prop-" + key);
            f.addActionListener(ev -> edit("set_properties", "{\"path\":" + q(selected.fullName())
                    + ",\"properties\":{" + q(key) + ":" + literal(f.getText()) + "}}"));
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.WEST, new Label(key));
            row.add(BorderLayout.CENTER, f);
            properties.add(row);
        }
    }

    private void log(String line) {
        if (!CN.isEdt()) {
            CN.callSerially(() -> log(line));
            return;
        }
        String t = output.getText() + line + "\n";
        output.setText(t.length() > 20000 ? t.substring(t.length() - 20000) : t);
    }

    /// The text of a property field as a JSON value: numbers, booleans,
    /// arrays and quoted strings as typed, anything else as a string.
    static String literal(String text) {
        String t = text.trim();
        if (t.startsWith("[") || t.startsWith("\"") || "true".equals(t) || "false".equals(t)) {
            return t;
        }
        try {
            Double.parseDouble(t);
            return t;
        } catch (NumberFormatException notNumber) {
            return q(t);
        }
    }

    static String q(String s) {
        return AgentTools.jsonString(s);
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return new byte[0];
        }
    }
}
