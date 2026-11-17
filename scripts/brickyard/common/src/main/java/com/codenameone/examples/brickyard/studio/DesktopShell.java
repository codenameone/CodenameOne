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
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// The studio's docked layout, as a desktop engine editor lays one out: the
/// Explorer docked left, Properties right, Output (or the script) along the
/// bottom and the 3D viewport in the middle. On a desktop a strip of tool
/// icons runs along the top, under the operating system's menu bar; a tablet
/// gets the same docks with the touch bar instead. It only arranges; every
/// action is the [Studio]'s own.
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
    private Container header;
    private Button saveButton;
    private CodeEditor source;
    private TextArea output;
    private Form form;
    private int activeTool = 1;

    /// True on a desktop, where the tool strip of small icons runs under the
    /// OS menu bar; a tablet's touch bar is the [Studio]'s own.
    private final boolean pointer;

    DesktopShell(Studio studio, boolean pointer) {
        this.studio = studio;
        this.pointer = pointer;
    }

    void install(Form f, Container viewport, Container explorer, Container properties, CodeEditor src,
            TextArea out) {
        form = f;
        source = src;
        output = out;
        if (pointer) {
            // The menus are the operating system's (see StudioActions); the
            // window keeps only the icon strip an editor has under them.
            f.add(BorderLayout.NORTH, toolStrip());
        }

        left = pane("EXPLORER", FontImage.MATERIAL_ACCOUNT_TREE, scroll(explorer), "StudioDockLeft");
        // Docks take a share of the window, as an editor's do, so the
        // viewport stays the largest thing on screen at any window size.
        left.setPreferredW(Math.max(CN.convertToPixels(pointer ? 40 : 48), CN.getDisplayWidth() * 21 / 100));
        right = pane("PROPERTIES", FontImage.MATERIAL_TUNE, scroll(properties), "StudioDockRight");
        right.setPreferredW(Math.max(CN.convertToPixels(44), CN.getDisplayWidth() * 23 / 100));

        bottomBody = new Container(new BorderLayout());
        bottomOutputTab = headerTab("Output", "dockOutput", ev -> studio.showTab(3));
        bottomScriptTab = headerTab("Script", "dockScript", ev -> studio.showTab(2));
        header = new Container(new BorderLayout());
        header.setUIID("StudioPaneHeader");
        saveButton = new Button("Save", "StudioMiniButton");
        saveButton.setName("saveScript");
        FontImage.setMaterialIcon(saveButton, FontImage.MATERIAL_SAVE, 2.6f);
        saveButton.addActionListener(ev -> studio.saveScript());
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
            // Code wants room: the dock grows to half the window while a
            // script is open, and Save sits in the header like an IDE's.
            bottom.setPreferredH(CN.getDisplayHeight() * 45 / 100);
            swapHeaderEnd(saveButton);
        } else {
            bottomBody.add(BorderLayout.CENTER, scroll(output));
            bottom.setPreferredH(CN.convertToPixels(34));
            swapHeaderEnd(bottomTitle);
            bottomTitle.setText("");
        }
        if (bottom.isHidden()) {
            bottom.setHidden(false);
        }
    }

    /// Puts `c` at the dock header's right end in place of what is there.
    private void swapHeaderEnd(Component c) {
        if (c.getParent() == header) { // NOPMD CompareObjectsWithEquals - tree identity
            return;
        }
        Component end = ((BorderLayout) header.getLayout()).getEast();
        if (end != null) {
            header.removeComponent(end);
        }
        header.add(BorderLayout.EAST, c);
    }

    private void toggle(Container pane) {
        pane.setHidden(!pane.isHidden());
        form.revalidate();
    }

    /// Shows or hides a dock: 0 the Explorer, 1 Properties.
    void toggleDock(int which) {
        toggle(which == 0 ? left : right);
    }

    // ---- tool strip ----

    private Container toolStrip() {
        Container strip = new Container(new BorderLayout());
        strip.setUIID("StudioToolStrip");
        toolButtons.setUIID("Container");
        rebuildTools();
        Container actions = BoxLayout.encloseX(
                icon(FontImage.MATERIAL_VIEW_IN_AR, "Insert part", "studioPart", studio::addPart),
                icon(FontImage.MATERIAL_NOTE_ADD, "Insert script", "studioScript", studio::addScript),
                separator(),
                icon(FontImage.MATERIAL_SMART_TOY, "Connect AI agent", "studioAgent", studio::agentMode),
                separator(),
                icon(FontImage.MATERIAL_CENTER_FOCUS_STRONG, "Focus selection", "studioFocus", studio::frameSelection),
                separator(),
                icon(FontImage.MATERIAL_UNDO, "Undo", "studioUndo", studio::undo),
                icon(FontImage.MATERIAL_REDO, "Redo", "studioRedo", studio::redo),
                separator(),
                icon(FontImage.MATERIAL_PLAY_ARROW, "Play test", "studioPlay", studio::playtest),
                icon(FontImage.MATERIAL_STOP, "Stop", "studioStop", studio::stopPlaytest),
                separator(),
                icon(FontImage.MATERIAL_ROCKET_LAUNCH, "Publish", "studioPublish", studio::askPublish));
        strip.add(BorderLayout.WEST, BoxLayout.encloseX(toolButtons, separator(), actions));
        return strip;
    }

    /// The transform tools: each is a mode of the viewport's gizmo (see
    /// `com.codenameone.examples.brickyard.rbx.Gizmo`), in the same order.
    private void rebuildTools() {
        toolButtons.removeAll();
        for (int i = 0; i < TOOLS.length; i++) {
            final int k = i;
            Button b = new Button("", i == activeTool ? "StudioIconSelected" : "StudioIcon");
            FontImage.setMaterialIcon(b, TOOL_ICONS[i], 3.2f);
            b.setName("tool" + TOOLS[i]);
            b.setAccessibilityText(TOOLS[i] + " tool");
            b.setTooltip(TOOLS[i]);
            b.addActionListener(ev -> {
                activeTool = k;
                rebuildTools();
                studio.setTool(k);
                toolButtons.revalidate();
            });
            toolButtons.add(b);
        }
    }

    private static Button icon(char glyph, String label, String name, final Runnable run) {
        Button b = new Button("", "StudioIcon");
        FontImage.setMaterialIcon(b, glyph, 3.2f);
        b.setName(name);
        // An icon-only button still says what it does, to a screen reader
        // and as the tool tip a desktop shows.
        b.setAccessibilityText(label);
        b.setTooltip(label);
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
