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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

/// The studio's desktop layout, as Roblox Studio or Unity lay out an editor:
/// a menu bar, a strip of tool icons, the Explorer docked left, Properties
/// right, Output (or the script) along the bottom and the 3D viewport in
/// the middle. It only arranges; every action is the [Studio]'s own.
final class DesktopShell {
    private static final String[] TOOLS = {"Select", "Move", "Rotate", "Scale"};
    private static final char[] TOOL_ICONS = {FontImage.MATERIAL_NEAR_ME, FontImage.MATERIAL_OPEN_WITH,
        FontImage.MATERIAL_ROTATE_RIGHT, FontImage.MATERIAL_ASPECT_RATIO};

    private final Studio studio;
    private final Container toolButtons = new Container(BoxLayout.x());
    private Container left;
    private Container right;
    private Container bottom;
    private Container bottomBody;
    private Label bottomTitle;
    private Button bottomOutputTab;
    private Button bottomScriptTab;
    private TextArea source;
    private TextArea output;
    private Form form;
    private int activeTool = 1;

    DesktopShell(Studio studio) {
        this.studio = studio;
    }

    void install(Form f, Container viewport, Container explorer, Container properties, TextArea src,
            TextArea out) {
        form = f;
        source = src;
        output = out;
        Container north = BoxLayout.encloseY(menuBar(), toolStrip());
        f.add(BorderLayout.NORTH, north);

        left = pane("EXPLORER", FontImage.MATERIAL_ACCOUNT_TREE, scroll(explorer), "StudioDockLeft");
        // Docks take a share of the window, as an editor's do, so the
        // viewport stays the largest thing on screen at any window size.
        left.setPreferredW(Math.max(CN.convertToPixels(40), CN.getDisplayWidth() * 21 / 100));
        right = pane("PROPERTIES", FontImage.MATERIAL_TUNE, scroll(properties), "StudioDockRight");
        right.setPreferredW(Math.max(CN.convertToPixels(44), CN.getDisplayWidth() * 23 / 100));

        bottomBody = new Container(new BorderLayout());
        bottomOutputTab = headerTab("Output", "dockOutput", ev -> studio.showTab(3));
        bottomScriptTab = headerTab("Script", "dockScript", ev -> studio.showTab(2));
        Container header = new Container(new BorderLayout());
        header.setUIID("StudioPaneHeader");
        header.add(BorderLayout.WEST, BoxLayout.encloseX(bottomOutputTab, bottomScriptTab));
        bottomTitle = new Label("", "StudioPaneHeaderHint");
        header.add(BorderLayout.EAST, bottomTitle);
        bottom = new Container(new BorderLayout());
        bottom.setUIID("StudioDockBottom");
        bottom.add(BorderLayout.NORTH, header);
        bottom.add(BorderLayout.CENTER, bottomBody);
        bottom.setPreferredH(CN.convertToPixels(34));

        viewport.setUIID("StudioViewport");
        // The GPU view is sized to the viewport, which here is what is left
        // between the bars and the bottom dock.
        // No preferred height: the centre of the border layout is whatever
        // the bars and the bottom dock leave, and the GPU view must be drawn
        // at exactly that size or the frame is stretched and soft.
        Container centre = new Container(new BorderLayout());
        centre.setUIID("StudioCentre");
        centre.add(BorderLayout.CENTER, viewport);
        centre.add(BorderLayout.SOUTH, bottom);

        Container body = new Container(new BorderLayout());
        body.setUIID("StudioDesktop");
        body.add(BorderLayout.WEST, left);
        body.add(BorderLayout.EAST, right);
        body.add(BorderLayout.CENTER, centre);
        f.add(BorderLayout.CENTER, body);
    }

    /// Shows the script editor (true) or the output in the bottom dock.
    void showBottom(boolean script) {
        bottomBody.removeAll();
        bottomOutputTab.setUIID(script ? "StudioPaneTab" : "StudioPaneTabSelected");
        bottomScriptTab.setUIID(script ? "StudioPaneTabSelected" : "StudioPaneTab");
        if (script) {
            if (source.getParent() != null) {
                source.remove();
            }
            bottomBody.add(BorderLayout.CENTER, source);
            Button save = new Button("Save script", "StudioMiniButton");
            save.setName("saveScript");
            save.addActionListener(ev -> studio.saveScript());
            bottomBody.add(BorderLayout.EAST, save);
            bottomTitle.setText("Script");
        } else {
            bottomBody.add(BorderLayout.CENTER, scroll(output));
            bottomTitle.setText("");
        }
        if (bottom.isHidden()) {
            bottom.setHidden(false);
        }
    }

    // ---- menu bar ----

    private Container menuBar() {
        Container bar = new Container(new FlowLayout(Component.LEFT));
        bar.setUIID("StudioMenuBar");
        bar.add(menu("File", new Object[] {
            "New from template...", (Runnable) studio::pickTemplate,
            "Open template...", (Runnable) () -> TemplateGallery.show(CN.getCurrentForm()),
            "Import .rbxlx", (Runnable) studio::importPlace,
            "Export .rbxlx", (Runnable) studio::exportPlace,
            "Publish...", (Runnable) studio::askPublish,
            "Capture thumbnail", (Runnable) studio::captureThumbnail,
            "Compatibility report", (Runnable) studio::compatReport,
            "AI agent mode", (Runnable) studio::agentMode,
            "Exit", (Runnable) studio::exit}));
        bar.add(menu("Edit", new Object[] {
            "Undo", (Runnable) studio::undo,
            "Redo", (Runnable) studio::redo,
            "Duplicate", (Runnable) studio::duplicateSelected,
            "Delete", (Runnable) studio::deleteSelected}));
        bar.add(menu("View", new Object[] {
            "Explorer", (Runnable) () -> toggle(left),
            "Properties", (Runnable) () -> toggle(right),
            "Output", (Runnable) () -> toggle(bottom)}));
        bar.add(menu("Insert", new Object[] {
            "Part", (Runnable) studio::addPart,
            "Script", (Runnable) studio::addScript}));
        bar.add(menu("Test", new Object[] {
            "Play", (Runnable) studio::playtest,
            "Stop", (Runnable) studio::stopPlaytest}));
        return bar;
    }

    /// A menu title that drops its items, given as label, action pairs.
    private Button menu(final String name, final Object[] items) {
        final Button title = new Button(name, "StudioMenu");
        title.setName("menu" + name);
        title.addActionListener(ev -> {
            final Dialog d = new Dialog(new BorderLayout());
            d.setDialogUIID("StudioMenuPopup");
            d.getContentPane().setUIID("StudioMenuPopupBody");
            d.setDisposeWhenPointerOutOfBounds(true);
            Container list = new Container(BoxLayout.y());
            for (int i = 0; i + 1 < items.length; i += 2) {
                final Object action = items[i + 1];
                Button item = new Button(String.valueOf(items[i]), "StudioMenuItem");
                item.setName("menuItem-" + items[i]);
                item.addActionListener(e -> {
                    d.dispose();
                    if (action instanceof Runnable) {
                        CN.callSerially((Runnable) action);
                    }
                });
                list.add(item);
            }
            d.add(BorderLayout.CENTER, list);
            d.showPopupDialog(title);
        });
        return title;
    }

    private void toggle(Container pane) {
        pane.setHidden(!pane.isHidden());
        form.revalidate();
    }

    // ---- tool strip ----

    private Container toolStrip() {
        Container strip = new Container(new BorderLayout());
        strip.setUIID("StudioToolStrip");
        toolButtons.setUIID("Container");
        rebuildTools();
        Container actions = BoxLayout.encloseX(
                icon(FontImage.MATERIAL_VIEW_IN_AR, "studioPart", studio::addPart),
                icon(FontImage.MATERIAL_NOTE_ADD, "studioScript", studio::addScript),
                separator(),
                icon(FontImage.MATERIAL_UNDO, "studioUndo", studio::undo),
                icon(FontImage.MATERIAL_REDO, "studioRedo", studio::redo),
                separator(),
                icon(FontImage.MATERIAL_PLAY_ARROW, "studioPlay", studio::playtest),
                icon(FontImage.MATERIAL_STOP, "studioStop", studio::stopPlaytest),
                separator(),
                icon(FontImage.MATERIAL_ROCKET_LAUNCH, "studioPublish", studio::askPublish));
        strip.add(BorderLayout.WEST, BoxLayout.encloseX(toolButtons, separator(), actions));
        return strip;
    }

    /// The transform tools: each is a mode of the viewport's gizmo (see
    /// `com.codename1.rbx.Gizmo`), in the same order.
    private void rebuildTools() {
        toolButtons.removeAll();
        for (int i = 0; i < TOOLS.length; i++) {
            final int k = i;
            Button b = new Button("", i == activeTool ? "StudioIconSelected" : "StudioIcon");
            FontImage.setMaterialIcon(b, TOOL_ICONS[i], 3.2f);
            b.setName("tool" + TOOLS[i]);
            b.addActionListener(ev -> {
                activeTool = k;
                rebuildTools();
                studio.setTool(k);
                toolButtons.revalidate();
            });
            toolButtons.add(b);
        }
    }

    private static Button icon(char glyph, String name, final Runnable run) {
        Button b = new Button("", "StudioIcon");
        FontImage.setMaterialIcon(b, glyph, 3.2f);
        b.setName(name);
        b.addActionListener(ev -> run.run());
        return b;
    }

    private static Label separator() {
        return new Label("", "StudioSeparator");
    }

    // ---- panes ----

    private static Container pane(String name, char glyph, Component content, String uiid) {
        Label header = new Label(name, "StudioPaneHeader");
        FontImage.setMaterialIcon(header, glyph, 2.6f);
        Container p = new Container(new BorderLayout());
        p.setUIID(uiid);
        p.add(BorderLayout.NORTH, header);
        p.add(BorderLayout.CENTER, content);
        return p;
    }

    private static Button headerTab(String text, String name,
            com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent> l) {
        Button b = new Button(text, "StudioPaneTab");
        b.setName(name);
        b.addActionListener(l);
        return b;
    }

    private static Container scroll(Component c) {
        if (c.getParent() != null) {
            c.remove();
        }
        Container w = new Container(new BorderLayout());
        w.setScrollableY(true);
        w.add(BorderLayout.CENTER, c);
        return w;
    }
}
