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

import com.codename1.components.InteractionDialog;
import com.codename1.components.SpanLabel;
import com.codename1.mcp.MCPClientDescriptor;
import com.codename1.mcp.MCPClientRegistrar;
import com.codename1.rbx.AgentMcp;
import com.codename1.rbx.AgentTools;
import com.codename1.rbx.Engine;
import com.codename1.rbx.Gizmo;
import com.codename1.rbx.CFrame;
import com.codename1.rbx.Instance;
import com.codename1.rbx.PlayView;
import com.codename1.rbx.Vector3;
import com.codename1.rbx.Scheduler;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.PlaceLoader;
import com.codename1.rbx.place.PlaceSaver;
import com.codename1.rbx.place.RbxlxWriter;
import com.codename1.rbx.place.Templates;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.brickyard.api.ExperienceSaveDto;
import com.codenameone.examples.brickyard.api.PlaceUploadDto;
import com.codenameone.examples.brickyard.api.ThumbnailDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.play.PlaySession;
import com.codenameone.examples.brickyard.ui.Ui;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private String genre = "Obby";
    /// What a new experience says about itself until its creator edits it;
    /// the backend refuses one without a description.
    private String description = "Made in the Brickyard studio.";
    private final Container viewport = new Container(new BorderLayout());
    private final Container panel = new Container(new BorderLayout());
    private final Container tabBar = new Container(new GridLayout(1, 4));
    private int tab;
    private PlayView view;
    private Engine engine;
    private final AgentTools tools = new AgentTools(this);
    private final List<byte[]> undo = new ArrayList<byte[]>();
    /// The transform tool, a [Gizmo] mode.
    private int tool = Gizmo.MOVE;
    /// The place before the gizmo drag under way, for its undo step.
    private byte[] dragBefore;
    private Instance selected;
    private final Form form;
    private final Container explorer = new Container(BoxLayout.y());
    private final Container properties = new Container(BoxLayout.y());
    private final CodeEditor source = LuauLanguage.editor();
    private final TextArea output = new TextArea(12, 40);
    private final List<byte[]> redo = new ArrayList<byte[]>();
    /// Folders and models the creator opened in the Explorer; the rest stay
    /// folded, as an engine editor starts them, so a backdrop of fifty trees
    /// is one row.
    private int inserted;
    private static final String[] PART_PROPS = {"Shape", "Size", "CFrame", "Color", "Material", "Anchored",
        "CanCollide", "Transparency"};
    private final Set<Instance> expanded = new HashSet<Instance>();
    /// A desktop editor's layout (menu bar, tool strip, docked panes) on a
    /// desktop, a tablet or any window wide enough; the phone keeps tabs.
    private final boolean desktop;
    private DesktopShell shell;
    private Runnable exit;

    private Studio(String experienceId, String title) {
        this.experienceId = experienceId;
        this.title = title;
        form = Ui.form(title, "studio");
        form.setLayout(new BorderLayout());
        form.setScrollableY(false);
        source.setName("scriptSource");
        output.setEditable(false);
        output.setName("studioOutput");
        desktop = CN.isDesktop() || CN.isTablet() || CN.getDisplayWidth() >= CN.convertToPixels(180);
        replace(newEngine());
    }

    /// Opens `experienceId` for editing, or a new experience when it is null.
    public static void open(final String experienceId) {
        final Form previous = CN.getCurrentForm();
        if (experienceId == null) {
            TemplateGallery.show(previous);
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

    /// A new experience called `title`, started from the template `id`.
    static void create(String id, String title, Form previous) {
        Studio s = new Studio(null, title);
        s.genre = TemplateGallery.genre(id);
        s.description = TemplateGallery.blurb(id);
        s.applyTemplate(id);
        s.show(previous);
    }

    private static Engine newEngine() {
        Engine e = new Engine(Engine.SOLO);
        e.setStudio(true);
        return e;
    }

    private static final String[] TABS = {"Explorer", "Properties", "Script", "Output"};
    private static final char[] TAB_ICONS = {FontImage.MATERIAL_ACCOUNT_TREE, FontImage.MATERIAL_TUNE,
        FontImage.MATERIAL_CODE, FontImage.MATERIAL_TERMINAL};

    private void show(final Form previous) {
        current = this;
        exit = () -> {
            if (current == this) {
                current = null;
            }
            if (previous != null) {
                previous.showBack();
            }
        };
        if (desktop) {
            shell = new DesktopShell(this);
            shell.install(form, viewport, explorer, properties, source, output);
            showTab(3);
            // A desktop editor has a menu bar, not a phone's title bar: the
            // title and the way out live there (File > Exit, the close button).
            form.getToolbar().setHidden(true);
            form.show();
            rebuildViewport();
            return;
        }
        // The place in 3D on top, as in any engine's editor: drag to orbit,
        // pinch or wheel to zoom; picking a part in the Explorer flies to it.
        viewport.setUIID("StudioViewport");
        int vh = Math.max(CN.convertToPixels(36), CN.getDisplayHeight() / 3);
        viewport.setPreferredH(vh);
        Container body = new Container(new BorderLayout());
        body.add(BorderLayout.NORTH, BoxLayout.encloseY(viewport, tabBar));
        panel.setUIID("StudioPanel");
        body.add(BorderLayout.CENTER, panel);
        form.add(BorderLayout.CENTER, body);
        tabBar.setUIID("StudioTabs");
        showTab(0);
        Container bar = new Container(new GridLayout(1, 5));
        bar.setUIID("GameToolbar");
        bar.setSafeArea(true);
        bar.add(tool("Play", FontImage.MATERIAL_PLAY_ARROW, "StudioPlay", "studioPlay", ev -> playtest()));
        bar.add(tool("Part", FontImage.MATERIAL_VIEW_IN_AR, "StudioTool", "studioPart", ev -> addPart()));
        bar.add(tool("Script", FontImage.MATERIAL_NOTE_ADD, "StudioTool", "studioScript", ev -> addScript()));
        bar.add(tool("Undo", FontImage.MATERIAL_UNDO, "StudioTool", "studioUndo", ev -> undo()));
        bar.add(tool("Publish", FontImage.MATERIAL_ROCKET_LAUNCH, "StudioPublish", "studioPublish",
                ev -> askPublish()));
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
                deleteSelected();
            }
        });
        form.getToolbar().addCommandToOverflowMenu(new Command("Compatibility report") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                compatReport();
            }
        });
        form.getToolbar().addCommandToOverflowMenu(new Command("Export .rbxlx") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                exportPlace();
            }
        });
        form.getToolbar().addCommandToOverflowMenu(new Command("Connect AI agent") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                agentMode();
            }
        });
        Ui.back(form, exit);
        form.show();
        rebuildViewport();
    }

    private static Button tool(String text, char icon, String uiid, String name,
            com.codename1.ui.events.ActionListener<ActionEvent> onClick) {
        Button b = new Button(text, uiid);
        FontImage.setMaterialIcon(b, icon, 4);
        b.setTextPosition(com.codename1.ui.Component.BOTTOM);
        b.setGap(CN.convertToPixels(0.6f));
        b.setName(name);
        b.addActionListener(onClick);
        return b;
    }

    void showTab(int index) {
        tab = index;
        output.setUIID("StudioCode");
        source.setUIID("StudioCode");
        if (shell != null) {
            // Explorer and Properties are always docked; only the bottom
            // pane switches, between the script and the output.
            if (index >= 2) {
                shell.showBottom(index == 2);
            }
            form.revalidate();
            return;
        }
        tabBar.removeAll();
        for (int i = 0; i < TABS.length; i++) {
            final int k = i;
            Button b = new Button(TABS[i], i == index ? "StudioTabSelected" : "StudioTab");
            FontImage.setMaterialIcon(b, TAB_ICONS[i], 3);
            b.setGap(CN.convertToPixels(0.8f));
            b.setName("tab" + TABS[i]);
            b.addActionListener(ev -> showTab(k));
            tabBar.add(b);
        }
        panel.removeAll();
        if (index == 2) {
            Container script = new Container(new BorderLayout());
            source.setUIID("StudioCode");
            if (source.getParent() != null) {
                source.remove();
            }
            script.add(BorderLayout.CENTER, source);
            script.add(BorderLayout.SOUTH, Ui.primary("Save script", "saveScript", ev -> saveScript()));
            panel.add(BorderLayout.CENTER, script);
        } else {
            panel.add(BorderLayout.CENTER, wrap(index == 0 ? explorer : index == 1 ? properties : output));
        }
        output.setUIID("StudioCode");
        form.revalidate();
    }

    /// A new viewport over the current engine; called when the engine is
    /// replaced (a template, an undo), since a view belongs to one engine.
    private void rebuildViewport() {
        if (!CN.isEdt()) {
            CN.callSerially(this::rebuildViewport);
            return;
        }
        viewport.removeAll();
        view = new PlayView(engine);
        view.setEditMode(true);
        view.setPickListener(this::picked);
        view.getGizmo().setMode(tool);
        view.getGizmo().setSelection(selected);
        view.setTransformListener(new PlayView.TransformListener() {
            @Override
            public void transformStarted() {
                dragBefore = RbxlxWriter.writeBytes(PlaceSaver.save(engine));
            }

            @Override
            public void transformFinished() {
                if (dragBefore != null) {
                    pushUndo(dragBefore);
                    dragBefore = null;
                    showProperties();
                    form.revalidate();
                }
            }
        });
        // A GPU peer paints its whole bounds over lightweight components;
        // sized exactly to the viewport, it cannot spill onto the tabs.
        if (!desktop) {
            view.setPreferredH(viewport.getPreferredH());
        }
        frame(null);
        if (view.isSupported()) {
            viewport.add(BorderLayout.CENTER, view);
        } else {
            viewport.add(BorderLayout.CENTER, Ui.label("3D is not available on this device", "Muted"));
        }
        viewport.revalidate();
    }

    /// Points the viewport at `focus`, or at the whole place when null.
    private void frame(Instance focus) {
        if (view == null) {
            return;
        }
        double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
            -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        bounds(focus == null ? engine.getWorkspace() : focus, b, focus == null);
        if (b[0] > b[3]) {
            view.getCamera().setPivot(new Vector3(0, 0, 0), 60);
            return;
        }
        double dx = b[3] - b[0];
        double dy = b[4] - b[1];
        double dz = b[5] - b[2];
        double r = Math.sqrt(dx * dx + dy * dy + dz * dz) / 2;
        view.getCamera().setPivot(new Vector3((b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2),
                Math.max(8, r * (focus == null ? 0.85 : 2.4)));
    }

    /// Grows `b` (min xyz, max xyz) by every part under `i`. The whole place
    /// skips parts larger than 400 studs a side -- a sea or a baseplate
    /// would otherwise frame the course as a speck -- and parts tagged
    /// Scenery.
    private static void bounds(Instance i, double[] b, boolean skipHuge) {
        Object cf = i.prop("CFrame");
        Object size = i.prop("Size");
        if (i.isA("BasePart") && cf instanceof CFrame && size instanceof Vector3) {
            CFrame f = (CFrame) cf;
            Vector3 s = (Vector3) size;
            // A template tags its backdrop (hills, a tree belt, the far
            // ground) Scenery, so the place frames as its play area.
            if (!skipHuge || Math.max(s.x, s.z) < 400 && !i.hasTag("Scenery")) {
                b[0] = Math.min(b[0], f.x - s.x / 2);
                b[1] = Math.min(b[1], f.y - s.y / 2);
                b[2] = Math.min(b[2], f.z - s.z / 2);
                b[3] = Math.max(b[3], f.x + s.x / 2);
                b[4] = Math.max(b[4], f.y + s.y / 2);
                b[5] = Math.max(b[5], f.z + s.z / 2);
            }
        }
        for (Instance c : i.getChildren()) {
            bounds(c, b, skipHuge);
        }
    }

    /// A bright block beside the spawn (or at the viewport's pivot), which
    /// the viewport then frames, so the creator sees what was added.
    void addPart() {
        addPart("Block");
    }

    /// Where the next insert goes: beside the spawn, each one a step along
    /// from the last so they do not stack, or at the viewport's pivot.
    private Vector3 insertPoint(double lift) {
        inserted++;
        Vector3 at = new Vector3(0, 6, 0);
        Instance spawn = firstOf(engine.getWorkspace(), "SpawnLocation");
        Object scf = spawn == null ? null : spawn.prop("CFrame");
        if (scf instanceof CFrame) {
            CFrame f = (CFrame) scf;
            at = new Vector3(f.x + 6 * (inserted % 5) - 12, f.y + lift, f.z - 10 - 6 * (inserted / 5));
        } else if (engine.getWorkspace().prop("CurrentCamera") instanceof Instance) {
            Object focus = ((Instance) engine.getWorkspace().prop("CurrentCamera")).prop("Focus");
            if (focus instanceof CFrame) {
                at = new Vector3(((CFrame) focus).x, ((CFrame) focus).y, ((CFrame) focus).z);
            }
        }
        return at;
    }

    /// Inserts toolbox model `i`, built by its Luau, and selects it.
    void addModel(int i) {
        Vector3 at = insertPoint(-0.5);
        String code = Models.luau(i, Math.round(at.x), Math.round(at.y * 10) / 10.0, Math.round(at.z));
        edit("run_luau", "{\"source\":" + q(code) + "}");
        log(code, false);
        List<Instance> kids = engine.getWorkspace().getChildren();
        if (!kids.isEmpty()) {
            select(kids.get(kids.size() - 1));
            frameSelection();
        }
    }

    /// Adds snippet `i` as a new script: inside the selected part when one
    /// is selected (a kill brick, a moving platform act on their parent),
    /// otherwise in ServerScriptService, and opens it in the editor.
    void addSnippet(int i) {
        Instance parent = selected != null && selected.isA("BasePart") ? selected
                : engine.service("ServerScriptService");
        if (parent == null) {
            return;
        }
        String base = Models.snippetName(i).replace(" ", "");
        String name = base;
        for (int n = 2; parent.findFirstChild(name) != null; n++) {
            name = base + n;
        }
        edit("write_script", "{\"parent\":" + q(parent.fullName()) + ",\"name\":" + q(name)
                + ",\"source\":" + q(Models.snippet(i)) + "}");
        Instance created = parent.findFirstChild(name);
        if (created != null) {
            select(created, false);
            showTab(2);
        }
    }

    /// Inserts an anchored part of `shape` (Block, Ball or Cylinder) beside
    /// the spawn, each one a step along from the last so they do not stack.
    void addPart(String shape) {
        Vector3 at = insertPoint(1.9);
        edit("create_instance", "{\"class\":\"Part\",\"parent\":\"Workspace\",\"properties\":{\"Anchored\":true,"
                + "\"Shape\":" + q(shape) + ",\"Size\":[4,4,4],\"Color\":\"#ffc43c\",\"CFrame\":[" + at.x + ","
                + at.y + "," + at.z + "]}}");
        List<Instance> kids = engine.getWorkspace().getChildren();
        if (!kids.isEmpty()) {
            select(kids.get(kids.size() - 1));
        }
    }

    private static Instance firstOf(Instance root, String cls) {
        for (Instance c : root.getChildren()) {
            if (cls.equals(c.getClassName())) {
                return c;
            }
            Instance deeper = firstOf(c, cls);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    void askPublish() {
        final TextField notes = new TextField("", "What changed", 30, TextField.ANY);
        notes.setUIID("StudioField");
        notes.setName("publishNotes");
        // An in-form dialog, not a modal one: a modal Dialog is a form of
        // its own, and the 3D viewport behind it stops rendering and shows
        // as a grey slab for as long as the prompt is open.
        final InteractionDialog d = new InteractionDialog("Publish", new BorderLayout());
        d.setUIID("StudioDialog");
        d.getTitleComponent().setUIID("StudioDialogTitle");
        d.getContentPane().setUIID("StudioDialogBody");
        d.setAnimateShow(false);
        Container buttons = new Container(new GridLayout(1, 2));
        Button cancel = new Button("Cancel", "StudioDialogButton");
        cancel.addActionListener(ev -> d.dispose());
        Button go = new Button("Publish", "StudioPublish");
        go.setName("confirmPublish");
        go.addActionListener(ev -> {
            d.dispose();
            log(publish(notes.getText()));
            showTab(3);
        });
        buttons.add(cancel).add(go);
        d.add(BorderLayout.CENTER, BoxLayout.encloseY(Ui.label("Release notes", "StudioPropName"), notes));
        d.add(BorderLayout.SOUTH, buttons);
        int fw = CN.getDisplayWidth();
        int fh = CN.getDisplayHeight();
        int w = Math.min(fw - CN.convertToPixels(8), CN.convertToPixels(90));
        int h = d.getPreferredH();
        int side = (fw - w) / 2;
        int top = Math.max(0, (fh - h) / 2);
        d.show(top, Math.max(0, fh - h - top), side, side);
        notes.startEditingAsync();
    }

    /// A dialog in the editor's dark palette rather than the native theme's.
    static Dialog darkDialog(String title) {
        Dialog d = new Dialog(title, new BorderLayout());
        d.setDialogUIID("StudioDialog");
        d.getTitleComponent().setUIID("StudioDialogTitle");
        d.getContentPane().setUIID("StudioDialogBody");
        return d;
    }

    private static Container wrap(com.codename1.ui.Component c) {
        // A tab's view moves between wrappers as tabs are switched.
        if (c.getParent() != null) {
            c.remove();
        }
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
        if (view != null) {
            rebuildViewport();
        }
    }

    @Override
    public byte[] screenshot() {
        // The agent asks from its own thread; the capture is the EDT's.
        final byte[][] out = new byte[1][];
        CN.callSeriallyAndWait(() -> {
            Image shot = viewportImage();
            out[0] = shot == null ? null : encode(shot, ImageIO.FORMAT_PNG);
        });
        return out[0];
    }

    private void published(com.codenameone.examples.brickyard.api.VersionDto v) {
        log("Published version " + v.number());
        captureThumbnail();
    }

    /// Captures the viewport as it stands -- the real renderer, lit and post
    /// processed -- as the experience's tile, which is what players see in
    /// Discover. A creator frames it by orbiting before publishing.
    void captureThumbnail() {
        if (experienceId == null) {
            log("Publish first: a thumbnail belongs to a published experience");
            return;
        }
        Image shot = viewportImage();
        byte[] jpeg = shot == null ? null : encode(tile(shot), ImageIO.FORMAT_JPEG);
        if (jpeg == null) {
            log("This device cannot capture the viewport; players see a drawn tile");
            return;
        }
        Api.experiences().setThumbnail(experienceId, new ThumbnailDto(Base64.encodeNoNewline(jpeg)),
                Net.to(t -> log("Thumbnail updated"), (status, message) -> log("Thumbnail not saved: " + message)));
    }

    /// The viewport's pixels: the GPU view paints its last frame when drawn
    /// into an image, which is how the simulator's own screenshots see it.
    private Image viewportImage() {
        int w = viewport.getWidth();
        int h = viewport.getHeight();
        if (view == null || w <= 0 || h <= 0) {
            return null;
        }
        Image img = Image.createImage(w, h, 0xff000000);
        Graphics g = img.getGraphics();
        g.translate(-viewport.getX(), -viewport.getY());
        viewport.paintComponent(g, true);
        return img;
    }

    /// The centre 16:9 of `shot`, scaled to the tile size Discover shows.
    static Image tile(Image shot) {
        int w = shot.getWidth();
        int h = shot.getHeight();
        int cw = w;
        int ch = w * 9 / 16;
        if (ch > h) {
            ch = h;
            cw = h * 16 / 9;
        }
        Image crop = shot.subImage((w - cw) / 2, (h - ch) / 2, cw, ch, false);
        return crop.scaled(TILE_W, TILE_W * 9 / 16);
    }

    static final int TILE_W = 640;

    private static byte[] encode(Image img, String format) {
        ImageIO io = ImageIO.getImageIO();
        if (io == null || !io.isFormatSupported(format)) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            io.save(img, out, format, 0.85f);
            return out.toByteArray();
        } catch (IOException err) {
            return null;
        }
    }

    @Override
    public String publish(String notes) {
        final PlaceUploadDto upload = new PlaceUploadDto(Base64.encodeNoNewline(
                RbxlxWriter.writeBytes(PlaceSaver.save(engine))));
        if (experienceId == null) {
            Api.experiences().create(new ExperienceSaveDto(title, description, genre, 12, "PUBLIC", "ALL"),
                    Net.to(created -> {
                        experienceId = created.id();
                        Api.experiences().publish(experienceId, upload,
                                Net.to(this::published, Ui::failed));
                    }, Ui::failed));
            return "Creating the experience and publishing its first version";
        }
        Api.experiences().publish(experienceId, upload,
                Net.to(this::published, Ui::failed));
        return "Publishing a new version of " + experienceId;
    }

    // ---- editing ----

    /// Runs one tool as an undoable edit and refreshes the views.
    private void edit(String tool, String args) {
        byte[] before = RbxlxWriter.writeBytes(PlaceSaver.save(engine));
        try {
            String r = tools.call(tool, args);
            if (r != null && r.startsWith("{\"error\":")) {
                // Luau that failed reports it as a result, not a throw.
                log("Error: " + field(r, "error"), true);
            } else {
                log(describe(tool, args));
            }
            pushUndo(before);
        } catch (RuntimeException err) {
            Ui.toast(String.valueOf(err.getMessage()));
        }
        refresh();
    }

    /// One readable Output line for an edit, as an editor's history reads.
    static String describe(String tool, String args) {
        String what = field(args, "path");
        if (what == null) {
            what = field(args, "name");
        }
        if (what == null) {
            what = field(args, "class");
        }
        String name = what == null ? "" : what.substring(what.lastIndexOf('.') + 1);
        if ("create_instance".equals(tool)) {
            return "Inserted " + name;
        }
        if ("set_properties".equals(tool)) {
            int at = args.indexOf("\"properties\":{\"");
            String key = at < 0 ? null : field("{\"k\":" + args.substring(at + 14), "k");
            return "Set " + (key == null ? "properties" : key) + " of " + name;
        }
        if ("write_script".equals(tool)) {
            return "Saved script " + name;
        }
        if ("delete_instance".equals(tool)) {
            return "Deleted " + name;
        }
        if ("run_luau".equals(tool)) {
            int t = args.indexOf("-- Toolbox: ");
            return t < 0 ? "Ran Luau" : "Inserted model " + args.substring(t + 12, args.indexOf('\\', t));
        }
        return tool.replace('_', ' ') + " " + name;
    }

    /// The string value of `key` at the top of a flat JSON object, or null.
    private static String field(String json, String key) {
        String k = "\"" + key + "\":\"";
        int s = json.indexOf(k);
        if (s < 0) {
            return null;
        }
        s += k.length();
        int e = json.indexOf('"', s);
        return e < 0 ? null : json.substring(s, e);
    }

    private void pushUndo(byte[] before) {
        undo.add(before);
        redo.clear();
        if (undo.size() > UNDO_DEPTH) {
            undo.remove(0);
        }
    }

    /// Picks the viewport's transform tool, a [Gizmo] mode.
    void setTool(int t) {
        tool = t;
        if (view != null) {
            view.getGizmo().setMode(t);
        }
    }

    void undo() {
        if (undo.isEmpty()) {
            Ui.toast("Nothing to undo");
            return;
        }
        redo.add(RbxlxWriter.writeBytes(PlaceSaver.save(engine)));
        load(undo.remove(undo.size() - 1));
    }

    void redo() {
        if (redo.isEmpty()) {
            Ui.toast("Nothing to redo");
            return;
        }
        undo.add(RbxlxWriter.writeBytes(PlaceSaver.save(engine)));
        load(redo.remove(redo.size() - 1));
    }

    private void load(byte[] place) {
        Engine e = newEngine();
        PlaceLoader.loadPlace(e, PlaceFile.read(place));
        replace(e);
    }

    /// A copy of the selection beside it: same class, same properties.
    /// Shallow -- children are not copied.
    void duplicateSelected() {
        if (selected == null || selected.getParent() == null) {
            Ui.toast("Select an instance first");
            return;
        }
        StringBuilder props = new StringBuilder("{");
        for (Map.Entry<String, Object> p : selected.storedProps().entrySet()) {
            if (props.length() > 1) {
                props.append(',');
            }
            props.append(q(p.getKey())).append(':').append(AgentTools.json(p.getValue()));
        }
        props.append('}');
        edit("create_instance", "{\"class\":" + q(selected.getClassName()) + ",\"parent\":"
                + q(selected.getParent().fullName()) + ",\"name\":" + q(selected.getName())
                + ",\"properties\":" + props + "}");
    }

    /// Points the viewport at the selection, sized to fit it.
    void frameSelection() {
        if (selected != null && (selected.isA("BasePart") || "Model".equals(selected.getClassName()))) {
            frame(selected);
        }
    }

    void deleteSelected() {
        if (selected != null) {
            edit("delete_instance", "{\"path\":" + q(selected.fullName()) + "}");
        }
    }

    void addScript() {
        Instance sss = engine.service("ServerScriptService");
        String name = "Script";
        for (int i = 2; sss != null && sss.findFirstChild(name) != null; i++) {
            name = "Script" + i;
        }
        edit("write_script", "{\"parent\":\"ServerScriptService\",\"name\":" + q(name)
                + ",\"source\":" + q("-- " + name + ": runs on the server when the experience starts\n") + "}");
        Instance created = sss == null ? null : sss.findFirstChild(name);
        if (created != null) {
            select(created, false);
            showTab(2);
        }
    }

    void compatReport() {
        log(tools.call("compat_report", "{}"));
        showTab(3);
    }

    /// Opens `brickyard-import.rbxlx` (or the last export) from app storage.
    void importPlace() {
        String name = CN.existsInStorage("brickyard-import.rbxlx") ? "brickyard-import.rbxlx"
                : "brickyard-export.rbxlx";
        if (!CN.existsInStorage(name)) {
            log("Import: put an .rbxlx in app storage as brickyard-import.rbxlx (" + CN.getAppHomePath() + ")");
            showTab(3);
            return;
        }
        try (java.io.InputStream in = CN.createStorageInputStream(name)) {
            byte[] bytes = com.codename1.io.Util.readInputStream(in);
            undo.add(RbxlxWriter.writeBytes(PlaceSaver.save(engine)));
            load(bytes);
            log("Imported " + name);
        } catch (java.io.IOException err) {
            Ui.toast(String.valueOf(err.getMessage()));
        }
    }

    /// Returns to the editor from a running playtest.
    void stopPlaytest() {
        if (CN.getCurrentForm() != form) { // NOPMD CompareObjectsWithEquals - form identity
            form.showBack();
        } else {
            log("No playtest is running", true);
        }
    }

    void exit() {
        if (exit != null) {
            exit.run();
        }
    }

    boolean hasSelection() {
        return selected != null;
    }

    void log(String line, boolean show) {
        log(line);
        if (show) {
            showTab(3);
        }
    }

    private void applyTemplate(String id) {
        try {
            tools.call("apply_template", "{\"id\":" + q(id) + "}");
        } catch (RuntimeException err) {
            Ui.toast(String.valueOf(err.getMessage()));
        }
        refresh();
    }

    void pickTemplate() {
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

    void saveScript() {
        if (selected == null || !(selected.storedProps().containsKey("Source"))) {
            Ui.toast("Select a script first");
            return;
        }
        final Instance script = selected;
        source.getText(text -> {
            Instance parent = script.getParent();
            edit("write_script", "{\"parent\":" + q(parent == null ? "game" : parent.fullName()) + ",\"name\":"
                    + q(script.getName()) + ",\"source\":" + q(text == null ? "" : text) + "}");
            log("Saved " + script.getName());
        });
    }

    void playtest() {
        byte[] bytes = RbxlxWriter.writeBytes(PlaceSaver.save(engine));
        PlaySession.start(experienceId == null ? "draft" : experienceId, title + " (test)", bytes);
    }

    void exportPlace() {
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
    void agentMode() {
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
        // Every call an agent makes lands in Output, the same way the
        // studio's own buttons do, so the creator watches the AI build.
        AgentMcp.setActivity((tool, args, result, failed) -> {
            Studio s = current;
            if (s == null) {
                return;
            }
            s.log((failed ? "AI  failed: " : "AI  ") + agentLine(tool, args)
                    + (failed ? " -- " + result : ""), true);
            s.refresh();
        });
        showConnectAgent();
    }

    /// What an agent's call did, in the words of Output's own history.
    static String agentLine(String tool, String args) {
        if ("run_luau".equals(tool) || "playtest".equals(tool) || "screenshot".equals(tool)
                || "place_tree".equals(tool) || "compat_report".equals(tool)) {
            return tool.replace('_', ' ');
        }
        return describe(tool, args);
    }

    /// The MCP port this app's server listens on: the simulator sets
    /// `cn1.mcp.port`, and the default is what the bridge attaches to.
    static String mcpPort() {
        String p = System.getProperty("cn1.mcp.port");
        return p == null || p.length() == 0 ? "8765" : p;
    }

    /// How a desktop AI host reaches this app's MCP server: a stdio bridge
    /// that attaches to the running simulator, as the simulator's own MCP
    /// menu writes it.
    static MCPClientDescriptor bridgeDescriptor() {
        List<String> args = new ArrayList<String>();
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        args.add("com.codename1.impl.javase.MCPStdioLauncher");
        args.add("--attach");
        args.add(mcpPort());
        return new MCPClientDescriptor("brickyard-studio",
                System.getProperty("java.home") + "/bin/java", args);
    }

    /// The connect sheet: the server, every AI host found on this machine
    /// with a button that adds the studio to it, and what an agent can do.
    private void showConnectAgent() {
        Dialog d = darkDialog("Connect an AI agent");
        d.setName("agentDialog");
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        Label status = new Label("MCP server running on localhost:" + mcpPort(), "StudioAgentOn");
        FontImage.setMaterialIcon(status, FontImage.MATERIAL_HUB, 3);
        body.add(status);
        body.add(new SpanLabel("Any MCP host -- Claude, Codex, Cursor -- can drive this studio. It reads "
                + "the place, inserts parts and models, writes Luau, adds effects, playtests and publishes, "
                + "and every edit shows in Output and can be undone.", "StudioDialogText"));
        body.add(new Label("AI hosts on this computer", "StudioDialogHeading"));
        MCPClientRegistrar reg = MCPClientRegistrar.getInstance();
        List<MCPClientRegistrar.MCPClient> hosts = reg.isSupported() ? reg.detectClients()
                : new ArrayList<MCPClientRegistrar.MCPClient>();
        if (hosts.isEmpty()) {
            body.add(new SpanLabel("No AI host found. Add this server to yours by hand:\n"
                    + "java -cp <app class path> com.codename1.impl.javase.MCPStdioLauncher --attach "
                    + mcpPort(), "StudioDialogText"));
        }
        for (final MCPClientRegistrar.MCPClient h : hosts) {
            final Button connect = new Button("Connect", "StudioDialogButton");
            connect.setName("connect-" + h.getId());
            connect.setEnabled(h.isWritable());
            connect.addActionListener(ev -> {
                List<MCPClientRegistrar.MCPClient> one = new ArrayList<MCPClientRegistrar.MCPClient>();
                one.add(h);
                reg.register(bridgeDescriptor(), one);
                connect.setText("Connected");
                connect.setEnabled(false);
                log("Connected " + h.getDisplayName() + " to the studio; restart it to load the tools", true);
            });
            Label name = new Label(h.getDisplayName(), "StudioDialogText");
            FontImage.setMaterialIcon(name, FontImage.MATERIAL_SMART_TOY, 3);
            body.add(BorderLayout.centerEastWest(name, connect, null));
        }
        body.add(new Label(AgentTools.toolCount() + " studio tools", "StudioDialogHeading"));
        Container grid = new Container(new GridLayout(2));
        for (int i = 0; i < AgentTools.toolCount(); i++) {
            grid.add(new Label("brickyard_" + AgentTools.tool(i)[0], "StudioDialogMono"));
        }
        body.add(grid);
        body.add(new Label("Try asking", "StudioDialogHeading"));
        body.add(new SpanLabel("\"Add a ring of six torches around the spawn, make it night, and write "
                + "a script that gives a coin every time a player touches the gold block.\"",
                "StudioDialogQuote"));
        d.add(BorderLayout.CENTER, body);
        Button done = new Button("Done", "StudioDialogButton");
        done.setName("agentDone");
        done.addActionListener(ev -> d.dispose());
        d.add(BorderLayout.SOUTH, done);
        d.showPacked(BorderLayout.CENTER, true);
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
        if (view != null) {
            view.getGizmo().setSelection(selected);
        }
        explorer.removeAll();
        addTree(engine.getGame(), 0);
        showProperties();
        form.revalidate();
    }

    private void addTree(Instance i, int depth) {
        addTree(i, depth, 1);
    }

    /// One Explorer row for `i`; `copies` above one folds that many
    /// identical siblings (twenty rocks) into a single row.
    private void addTree(Instance i, int depth, int copies) {
        if (depth > 0) {
            final Instance target = i;
            Button b = new Button(copies > 1 ? i.getName() + "  x" + copies : i.getName(),
                    i == selected ? "StudioNodeSelected" : "StudioNode"); // NOPMD CompareObjectsWithEquals - tree identity
            b.setName("node-" + i.getName());
            final boolean folds = folds(i);
            b.addActionListener(ev -> {
                if (folds && !expanded.remove(target)) {
                    expanded.add(target);
                }
                select(target);
            });
            FontImage.setMaterialIcon(b, icon(i), 3);
            b.setGap(CN.convertToPixels(1));
            b.getAllStyles().setPaddingLeft(CN.convertToPixels(2 + 3 * (depth - 1)));
            b.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            if (folds) {
                b.setText((expanded.contains(i) ? "- " : "+ ") + b.getText());
            }
            // A service is named after its class; saying it twice only
            // truncates the name.
            Label cls = new Label(i.getName().equals(i.getClassName()) ? "" : i.getClassName(),
                    "StudioNodeClass");
            Container row = BorderLayout.centerEastWest(null, cls, null);
            row.add(BorderLayout.CENTER, b);
            row.setLeadComponent(b);
            explorer.add(row);
        }
        if (depth < 6 && copies == 1 && (depth < 2 || !folds(i) || expanded.contains(i))) {
            List<Instance> kids = i.getChildren();
            int k = 0;
            while (k < kids.size()) {
                Instance c = kids.get(k);
                int run = 1;
                while (k + run < kids.size() && kids.get(k + run).getName().equals(c.getName())
                        && kids.get(k + run).getClassName().equals(c.getClassName())
                        && kids.get(k + run).getChildren().isEmpty() && c.getChildren().isEmpty()) {
                    run++;
                }
                addTree(c, depth + 1, run > 2 ? run : 1);
                if (run <= 2) {
                    run = 1;
                }
                k += run;
            }
        }
    }

    /// True for a container the Explorer folds: a folder or a model with
    /// something in it, below the services.
    private static boolean folds(Instance i) {
        String c = i.getClassName();
        return ("Folder".equals(c) || "Model".equals(c)) && !i.getChildren().isEmpty();
    }

    /// The Explorer's glyph for a class, as an engine editor draws one.
    static char icon(Instance i) {
        String c = i.getClassName();
        if (c.endsWith("Script")) {
            return FontImage.MATERIAL_DESCRIPTION;
        }
        if ("SpawnLocation".equals(c)) {
            return FontImage.MATERIAL_FLAG;
        }
        if ("Model".equals(c)) {
            return FontImage.MATERIAL_CATEGORY;
        }
        if ("Camera".equals(c)) {
            return FontImage.MATERIAL_VIDEOCAM;
        }
        if (i.isA("BasePart")) {
            return FontImage.MATERIAL_VIEW_IN_AR;
        }
        if (c.endsWith("Light")) {
            return FontImage.MATERIAL_LIGHTBULB;
        }
        if (c.endsWith("Gui") || c.startsWith("Text") || "Frame".equals(c)) {
            return FontImage.MATERIAL_WEB_ASSET;
        }
        return FontImage.MATERIAL_FOLDER;
    }

    private void select(Instance i) {
        select(i, true);
    }

    /// A click in the viewport: the part's model when it is in one (a tree,
    /// a checkpoint), as an editor selects, and the camera stays where the
    /// creator put it.
    private void picked(Instance part) {
        if (part == null) {
            selected = null;
            refresh();
            return;
        }
        Instance top = part;
        Instance ws = engine.getWorkspace();
        for (Instance p = part.getParent(); p != null && p != ws; p = p.getParent()) { // NOPMD CompareObjectsWithEquals - tree identity
            if ("Model".equals(p.getClassName())) {
                top = p;
            }
        }
        select(top, false);
    }

    private void select(Instance i, boolean frameIt) {
        selected = i;
        Object src = i.storedProps().get("Source");
        source.setText(src == null ? "" : String.valueOf(src));
        refresh();
        if (!frameIt) {
            return;
        }
        if (i.isA("BasePart") || "Model".equals(i.getClassName())) {
            frame(i);
        } else if (src != null) {
            showTab(2);
        }
    }

    private void showProperties() {
        properties.removeAll();
        if (selected == null) {
            SpanLabel hint = new SpanLabel("Select an instance in the Explorer, or click a part in the viewport, to edit its properties.");
            hint.setTextUIID("Muted");
            properties.add(hint);
            return;
        }
        properties.add(Ui.label(selected.getName(), "RowTitle"));
        properties.add(Ui.label(selected.getClassName(), "StudioPropName"));
        // The properties a creator reaches for first, always, as an editor
        // lists them -- a new part has stored only its size and colour --
        // and then whatever else this instance has set.
        List<String> keys = new ArrayList<String>();
        keys.add("Name");
        if (selected.isA("BasePart")) {
            for (String k : PART_PROPS) {
                keys.add(k);
            }
        }
        for (String k : selected.storedProps().keySet()) {
            if (!"Source".equals(k) && !keys.contains(k)) {
                keys.add(k);
            }
        }
        for (final String key : keys) {
            Object value = "Name".equals(key) ? selected.getName() : selected.prop(key);
            String text = AgentTools.json(value);
            if (text.length() > 1 && text.charAt(0) == '"') {
                // A name or an enum reads as itself; literal() quotes it back.
                text = text.substring(1, text.length() - 1);
            }
            final TextField f = new TextField(text, key, 20, TextField.ANY);
            f.setUIID("StudioField");
            f.setName("prop-" + key);
            final Instance target = selected;
            final String shown = f.getText();
            final Runnable commit = () -> {
                String now = f.getText();
                if (!now.equals(shown) && target.getParent() != null) {
                    edit("set_properties", "{\"path\":" + q(target.fullName())
                            + ",\"properties\":{" + q(key) + ":" + literal(now) + "}}");
                }
            };
            f.addActionListener(ev -> commit.run());
            // Live, as an inspector is: a value applies once the typing
            // pauses, without hunting for a return key on a touch screen.
            final UITimer[] pause = new UITimer[1];
            f.addDataChangedListener((type, index) -> {
                if (pause[0] != null) {
                    pause[0].cancel();
                }
                pause[0] = UITimer.timer(700, false, form, commit);
            });
            Container row = new Container(new BorderLayout());
            Label name = Ui.label(key, "StudioPropName");
            // One column for every name, so the values line up.
            name.setPreferredW(desktop ? CN.convertToPixels(22) : CN.convertToPixels(26));
            row.add(BorderLayout.WEST, name);
            row.add(BorderLayout.CENTER, f);
            properties.add(row);
        }
    }

    void log(String line) {
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
