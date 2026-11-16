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
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import java.util.ArrayList;
import java.util.List;

/// Every studio action, declared once and presented the way each form factor
/// expects:
///
/// - **Desktop**: the operating system's own menu bar. Each action is a
///   [Command] with a menu path ([Command#setDesktopMenu(String)]) and a
///   shortcut; with `desktop.titleBar=native` Codename One hands them to the
///   real menu bar (the macOS screen menu, a Windows or Linux window menu).
/// - **Phone and tablet**: no menu bar. The everyday tools sit in a
///   thumb-height bar, the rest of the catalog is in the side menu, what
///   acts on the selection is in the overflow menu, and inserting is a
///   sheet of large tiles.
final class StudioActions {
    /// One entry of the catalog.
    static final class Action {
        final String menu;
        final String label;
        final char icon;
        final char key;
        final int modifiers;
        final String role;
        final Runnable run;

        Action(String menu, String label, char icon, char key, int modifiers, String role, Runnable run) {
            this.menu = menu;
            this.label = label;
            this.icon = icon;
            this.key = key;
            this.modifiers = modifiers;
            this.role = role;
            this.run = run;
        }
    }

    private static final int PRIMARY = Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY;
    private static final int SHIFT = Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY
            | Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT;

    private final Studio studio;
    private final List<Action> all = new ArrayList<Action>();

    StudioActions(Studio studio) {
        this.studio = studio;
        build();
    }

    private void add(String menu, String label, char icon, Runnable run) {
        all.add(new Action(menu, label, icon, (char) 0, 0, null, run));
    }

    private void add(String menu, String label, char icon, char key, int mods, String role, Runnable run) {
        all.add(new Action(menu, label, icon, key, mods, role, run));
    }

    private void build() {
        final Studio s = studio;
        add("File", "New from Template...", FontImage.MATERIAL_DASHBOARD, 'N', PRIMARY,
                Command.DESKTOP_ROLE_NEW, s::pickTemplate);
        add("File", "Import .rbxlx...", FontImage.MATERIAL_FILE_OPEN, 'O', PRIMARY,
                Command.DESKTOP_ROLE_OPEN, s::importPlace);
        add("File", "Export .rbxlx", FontImage.MATERIAL_SAVE_ALT, 'E', SHIFT, null, s::exportPlace);
        add("File", "Capture Thumbnail", FontImage.MATERIAL_PHOTO_CAMERA, s::captureThumbnail);
        add("File", "Publish...", FontImage.MATERIAL_ROCKET_LAUNCH, 'P', SHIFT, null, s::askPublish);
        add("File", "Close Studio", FontImage.MATERIAL_EXIT_TO_APP, 'W', PRIMARY,
                Command.DESKTOP_ROLE_CLOSE, s::exit);

        add("Edit", "Undo", FontImage.MATERIAL_UNDO, 'Z', PRIMARY, Command.DESKTOP_ROLE_UNDO, s::undo);
        add("Edit", "Redo", FontImage.MATERIAL_REDO, 'Z', SHIFT, Command.DESKTOP_ROLE_REDO, s::redo);
        add("Edit", "Duplicate", FontImage.MATERIAL_CONTENT_COPY, 'D', PRIMARY, null, s::duplicateSelected);
        add("Edit", "Delete", FontImage.MATERIAL_DELETE, (char) 8, 0, null, s::deleteSelected);

        add("View", "Explorer", FontImage.MATERIAL_ACCOUNT_TREE, '1', PRIMARY, null, () -> s.view(0));
        add("View", "Properties", FontImage.MATERIAL_TUNE, '2', PRIMARY, null, () -> s.view(1));
        add("View", "Script Editor", FontImage.MATERIAL_CODE, '3', PRIMARY, null, () -> s.view(2));
        add("View", "Output", FontImage.MATERIAL_TERMINAL, '4', PRIMARY, null, () -> s.view(3));
        add("View", "Focus Selection", FontImage.MATERIAL_CENTER_FOCUS_STRONG, 'F', PRIMARY, null,
                s::frameSelection);

        add("Insert", "Part", FontImage.MATERIAL_VIEW_IN_AR, 'I', PRIMARY, null, s::addPart);
        add("Insert", "Ball", FontImage.MATERIAL_SPORTS_BASEBALL, () -> s.addPart("Ball"));
        add("Insert", "Cylinder", FontImage.MATERIAL_STORAGE, () -> s.addPart("Cylinder"));
        add("Insert", "Script", FontImage.MATERIAL_NOTE_ADD, 'I', SHIFT, null, s::addScript);
        for (int i = 0; i < Models.count(); i++) {
            final int m = i;
            add("Insert/Models", Models.name(i), Models.icon(i), () -> s.addModel(m));
        }
        for (int i = 0; i < Models.snippetCount(); i++) {
            final int m = i;
            add("Insert/Scripts", Models.snippetName(i), FontImage.MATERIAL_DESCRIPTION, () -> s.addSnippet(m));
        }
        add("Insert/Effects", "Fire", FontImage.MATERIAL_LOCAL_FIRE_DEPARTMENT, () -> s.addEffect("fire"));
        add("Insert/Effects", "Sparkles", FontImage.MATERIAL_AUTO_AWESOME, () -> s.addEffect("sparkles"));
        add("Insert/Effects", "Smoke", FontImage.MATERIAL_CLOUD, () -> s.addEffect("smoke"));
        add("Insert/Effects", "Point Light", FontImage.MATERIAL_LIGHTBULB, () -> s.addEffect("light"));
        add("Insert/Lighting", "Noon", FontImage.MATERIAL_WB_SUNNY, () -> s.lighting("noon"));
        add("Insert/Lighting", "Dusk", FontImage.MATERIAL_WB_TWILIGHT, () -> s.lighting("dusk"));
        add("Insert/Lighting", "Night", FontImage.MATERIAL_NIGHTLIGHT, () -> s.lighting("night"));

        add("Test", "Play", FontImage.MATERIAL_PLAY_ARROW, 'R', PRIMARY, null, s::playtest);
        add("Test", "Stop", FontImage.MATERIAL_STOP, '.', PRIMARY, null, s::stopPlaytest);

        add("AI", "Connect AI Agent...", FontImage.MATERIAL_SMART_TOY, s::agentMode);
        add("AI", "Compatibility Report", FontImage.MATERIAL_FACT_CHECK, s::compatReport);
    }

    // ---- desktop: the OS menu bar ----

    /// Puts the whole catalog on the form's native menu bar.
    void installDesktopMenus(Form f) {
        Toolbar tb = f.getToolbar();
        for (final Action a : all) {
            Command c = Command.create(a.label, null, ev -> a.run.run());
            c.setDesktopMenu(a.menu);
            if (a.role != null) {
                c.setDesktopRole(a.role);
            }
            if (a.key != 0) {
                c.setDesktopShortcut(a.key, a.modifiers);
            }
            tb.addCommandToOverflowMenu(c);
        }
    }

    // ---- touch: side menu, overflow and the insert sheet ----

    /// The side menu: the catalog's File, Edit, View, Test and Tools
    /// sections, as tall rows a thumb can hit. Insert has a sheet of its own.
    void installSideMenu(Form f, String title) {
        Toolbar tb = f.getToolbar();
        Label head = new Label(title, "StudioSideTitle");
        FontImage.setMaterialIcon(head, FontImage.MATERIAL_CONSTRUCTION, 4);
        tb.addComponentToSideMenu(head);
        String section = null;
        for (final Action a : all) {
            if (a.menu.startsWith("Insert")) {
                continue;
            }
            if (!a.menu.equals(section)) {
                section = a.menu;
                tb.addComponentToSideMenu(new Label(section.toUpperCase(), "StudioSideSection"));
            }
            tb.addCommandToSideMenu(Command.createMaterial(a.label, a.icon, ev -> a.run.run()));
        }
    }

    /// The overflow menu: what acts on the current selection. Drawn in the
    /// form's own layer, dropping from the button: the toolbar's stock
    /// overflow is a Dialog, a form of its own, and the 3D view behind it
    /// stopped while it was open.
    void installOverflow(final Form f) {
        f.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_MORE_VERT, 4, ev -> showOverflow(f));
    }

    private void showOverflow(Form f) {
        final InteractionDialog d = new InteractionDialog(new BorderLayout());
        d.setUIID("StudioSheet");
        d.setDialogUIID("StudioPopup");
        d.getContentPane().setUIID("StudioPopup");
        d.setDisposeWhenPointerOutOfBounds(true);
        Container list = new Container(BoxLayout.y());
        String[] labels = {"Duplicate", "Delete", "Focus Selection", "Add Script Here", "Compatibility Report"};
        char[] icons = {FontImage.MATERIAL_CONTENT_COPY, FontImage.MATERIAL_DELETE,
            FontImage.MATERIAL_CENTER_FOCUS_STRONG, FontImage.MATERIAL_NOTE_ADD, FontImage.MATERIAL_FACT_CHECK};
        final Runnable[] runs = {studio::duplicateSelected, studio::deleteSelected, studio::frameSelection,
            studio::addScript, studio::compatReport};
        for (int i = 0; i < labels.length; i++) {
            final int k = i;
            Button row = new Button(labels[i], "SideCommand");
            FontImage.setMaterialIcon(row, icons[i], 3.6f);
            row.setGap(CN.convertToPixels(2));
            row.setName("more-" + labels[i]);
            row.addActionListener(ev -> {
                d.dispose();
                CN.callSerially(runs[k]);
            });
            list.add(row);
        }
        d.add(BorderLayout.CENTER, list);
        int top = f.getToolbar().getHeight();
        int w = d.getPreferredW() + CN.convertToPixels(4);
        int h = d.getPreferredH() + CN.convertToPixels(8);
        d.show(top, Math.max(0, f.getHeight() - top - h), Math.max(0, f.getWidth() - w - CN.convertToPixels(1)),
                CN.convertToPixels(1));
    }

    private static final String[] SHEET_TABS = {"Parts", "Models", "Scripts", "Effects", "Lighting"};
    private static final String[] SHEET_MENUS = {"Insert", "Insert/Models", "Insert/Scripts",
        "Insert/Effects", "Insert/Lighting"};

    /// The insert sheet: a row of category chips over a grid of big tiles,
    /// sliding up from the bottom edge, as a touch app inserts. Opened by the
    /// bar's Insert button; a tap on a tile inserts and closes it.
    void showInsertSheet(Form f) {
        final InteractionDialog d = new InteractionDialog(new BorderLayout());
        d.setUIID("StudioSheet");
        d.setDialogUIID("StudioSheet");
        d.getContentPane().setUIID("StudioSheetBody");
        d.setDisposeWhenPointerOutOfBounds(true);
        d.setAnimateShow(true);
        final Container grid = new Container();
        final Container chips = new Container(BoxLayout.x());
        chips.setScrollableX(true);
        chips.setUIID("StudioSheetChips");
        fillSheet(d, grid, chips, 0);
        Container head = new Container(new BorderLayout());
        head.setUIID("StudioSheetHead");
        head.add(BorderLayout.WEST, new Label("Insert", "StudioSheetTitle"));
        Button close = new Button("", "StudioIcon");
        FontImage.setMaterialIcon(close, FontImage.MATERIAL_CLOSE, 4);
        close.setAccessibilityText("Close");
        close.setName("insertClose");
        close.addActionListener(ev -> d.dispose());
        head.add(BorderLayout.EAST, close);
        d.add(BorderLayout.NORTH, BoxLayout.encloseY(head, chips));
        // North, not centre: tiles keep their own height instead of
        // stretching to fill a tall sheet.
        Container scroll = new Container(new BorderLayout());
        scroll.setScrollableY(true);
        scroll.add(BorderLayout.NORTH, grid);
        d.add(BorderLayout.CENTER, scroll);
        int h = Math.min(f.getHeight() * (CN.isTablet() ? 42 : 55) / 100, CN.convertToPixels(95));
        d.show(f.getHeight() - h, 0, 0, 0);
    }

    /// Picks one of `labels` from a sheet of big tiles; `chosen` gets its
    /// index. The selected one is highlighted.
    void showTileSheet(Form f, String title, String[] labels, char[] icons, int selected,
            final java.util.function.IntConsumer chosen) {
        final InteractionDialog d = new InteractionDialog(new BorderLayout());
        d.setUIID("StudioSheet");
        d.setDialogUIID("StudioSheet");
        d.getContentPane().setUIID("StudioSheetBody");
        d.setDisposeWhenPointerOutOfBounds(true);
        Container head = new Container(new BorderLayout());
        head.setUIID("StudioSheetHead");
        head.add(BorderLayout.WEST, new Label(title, "StudioSheetTitle"));
        d.add(BorderLayout.NORTH, head);
        Container grid = new Container(new GridLayout(1, labels.length));
        for (int i = 0; i < labels.length; i++) {
            final int k = i;
            Button tile = new Button(labels[i], i == selected ? "StudioToolSelected" : "StudioTile");
            FontImage.setMaterialIcon(tile, icons[i], 6);
            tile.setTextPosition(Component.BOTTOM);
            tile.setName("pick-" + labels[i]);
            tile.addActionListener(ev -> {
                d.dispose();
                chosen.accept(k);
            });
            grid.add(tile);
        }
        d.add(BorderLayout.CENTER, grid);
        int h = d.getPreferredH() + CN.convertToPixels(10);
        d.show(Math.max(0, f.getHeight() - h), 0, 0, 0);
    }

    private void fillSheet(final InteractionDialog d, final Container grid, final Container chips, final int cat) {
        chips.removeAll();
        for (int i = 0; i < SHEET_TABS.length; i++) {
            final int k = i;
            Button chip = new Button(SHEET_TABS[i], i == cat ? "StudioChipSelected" : "StudioChip");
            chip.setName("insertTab" + SHEET_TABS[i]);
            chip.addActionListener(ev -> {
                fillSheet(d, grid, chips, k);
                d.revalidate();
            });
            chips.add(chip);
        }
        grid.removeAll();
        int cols = CN.isTablet() ? 6 : 3;
        grid.setLayout(new GridLayout(Math.max(1, (count(SHEET_MENUS[cat]) + cols - 1) / cols), cols));
        for (final Action a : all) {
            if (!a.menu.equals(SHEET_MENUS[cat])) {
                continue;
            }
            Button tile = new Button(a.label, "StudioTile");
            FontImage.setMaterialIcon(tile, a.icon, 6);
            tile.setTextPosition(Component.BOTTOM);
            tile.setName("insert-" + a.label);
            tile.addActionListener(ev -> {
                d.dispose();
                CN.callSerially(a.run);
            });
            grid.add(tile);
        }
    }

    private int count(String menu) {
        int n = 0;
        for (Action a : all) {
            if (a.menu.equals(menu)) {
                n++;
            }
        }
        return n;
    }
}
