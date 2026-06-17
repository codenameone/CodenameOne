/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gamebuilder;

import com.codename1.gamebuilder.editor.EditorController;
import com.codename1.gamebuilder.editor.Tool;
import com.codename1.gamebuilder.ui.EditorCanvas;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.Resources;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/// Headless structure + behavior validation for the game builder UI: boots the form,
/// asserts every key control is present, fires the buttons, and checks the resulting
/// state (grid/snap/zoom/tool, the Live play toggle round-trip, undo via the button,
/// add-layer). Exits non-zero on any failure. Run with the harness idiom:
///
/// ```
/// mvn -pl common -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
///     -Dexec.classpathScope=test -Dexec.mainClass=com.codename1.gamebuilder.GameBuilderStructureHarness
/// ```
public final class GameBuilderStructureHarness {
    private static final List<String> fail = new ArrayList<>();

    private static void check(boolean cond, String msg) {
        if (!cond) {
            fail.add(msg);
        }
    }

    public static void main(String[] args) throws Exception {
        Display.init(null);
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                Resources r = Resources.openLayered("/theme");
                String[] n = r.getThemeResourceNames();
                if (n != null && n.length > 0) {
                    UIManager.getInstance().setThemeProps(r.getTheme(n[0]));
                }
            } catch (Exception ignore) {
            }
        });

        final GameBuilder[] gbRef = new GameBuilder[1];
        Display.getInstance().callSeriallyAndWait(() -> {
            gbRef[0] = new GameBuilder();
            gbRef[0].runApp();
        });
        Display.getInstance().callSeriallyAndWait(() -> runChecks(gbRef[0]));

        System.out.println("[Structure] failures=" + fail.size());
        for (String f : fail) {
            System.out.println("  FAIL: " + f);
        }
        System.out.println("[Structure] RESULT " + (fail.isEmpty() ? "OK" : "FAIL"));
        System.exit(fail.isEmpty() ? 0 : 1);
    }

    private static void runChecks(GameBuilder gb) {
        Form form = gb.getForm();
        EditorController c = gb.getController();
        EditorCanvas canvas = gb.getCanvas();

        // ---- structure present ----
        check(find(form, "btn.save") != null, "Save button present");
        check(find(form, "btn.live") != null, "Live button present");
        check(find(form, "btn.project") != null, "Project button present");
        check(find(form, "btn.undo") != null && find(form, "btn.redo") != null, "undo/redo present");
        check(find(form, "btn.grid") != null && find(form, "btn.snap") != null, "grid/snap present");
        check(find(form, "btn.addlayer") != null, "add-layer button present");
        check(find(form, "btn.zoomin") != null && find(form, "btn.zoomout") != null, "zoom buttons present");
        for (Tool t : Tool.values()) {
            if (t == Tool.TERRAIN) {
                continue;   // 3D-only tool; absent on the default 2D demo (checked below)
            }
            check(find(form, "tool." + t.name()) != null, "tool " + t + " present");
        }
        check(find(form, "tool.TERRAIN") == null, "TERRAIN tool hidden in 2D mode");
        check(find(form, "scene.card") != null, "scene card present");
        check(find(form, "layer.Actors") != null, "Actors layer present");
        check(find(form, "layer.Terrain") != null, "Terrain layer present");
        check(find(form, "tab.platformer") != null, "platformer tab present");
        check(find(form, "asset.ground") != null, "ground asset present");
        check(find(form, "asset.player") != null, "player asset present");

        // ---- behavior: toggles ----
        boolean g0 = canvas.isGridVisible();
        fire(form, "btn.grid");
        check(canvas.isGridVisible() != g0, "grid toggles");
        fire(form, "btn.grid");
        check(canvas.isGridVisible() == g0, "grid toggles back");

        boolean s0 = c.model().isSnap();
        fire(form, "btn.snap");
        check(c.model().isSnap() != s0, "snap toggles");

        double z0 = canvas.getZoom();
        fire(form, "btn.zoomin");
        check(canvas.getZoom() > z0, "zoom-in increases zoom");
        fire(form, "btn.zoomout");

        fire(form, "tool.FILL");
        check(c.model().getTool() == Tool.FILL, "tool FILL selects");
        fire(form, "tool.BRUSH");
        check(c.model().getTool() == Tool.BRUSH, "tool BRUSH selects");

        // asset cells are clickable Buttons and switch to a compatible layer
        fire(form, "asset.ground");
        check("ground".equals(c.model().getSelectedAssetId()), "ground asset selects");
        check(c.model().level().getLayer(c.model().getActiveLayer()).getKind()
                == com.codename1.gaming.level.Layer.KIND_TILE, "tile asset activates a tile layer");
        fire(form, "asset.player");
        check("player".equals(c.model().getSelectedAssetId()), "player asset selects");
        check(c.model().level().getLayer(c.model().getActiveLayer()).getKind()
                != com.codename1.gaming.level.Layer.KIND_TILE, "actor asset activates an entity layer");

        // ---- the critical one: Live play toggles in-place and returns ----
        check(!gb.isPlaying(), "not playing initially");
        fire(form, "btn.live");
        check(gb.isPlaying() && canvas.isPlayMode(), "Live starts play in-place");
        fire(form, "btn.live");
        check(!gb.isPlaying() && !canvas.isPlayMode(), "Live stops play (no dead-end / back works)");

        // ---- undo via the toolbar button ----
        c.model().setActiveLayer("Terrain");
        c.model().setSelectedAssetId("brick");
        check(c.paintTile(2, 2), "paintTile applied");
        gb.refreshUI();
        Component undo = find(form, "btn.undo");
        check(undo != null && undo.isEnabled(), "undo button enabled after an edit");
        fire(form, "btn.undo");
        check(c.model().level().getLayer("Terrain").getTile(2, 2) == null, "undo button reverts the edit");

        // ---- add layer button ----
        int before = c.model().level().layers().size();
        fire(form, "btn.addlayer");
        check(c.model().level().layers().size() == before + 1, "add-layer button adds a layer");

        // ---- layer membership clarity + asset hint (#10) ----
        check(find(form, "asset.hint") != null, "asset placement hint present");
        com.codename1.gaming.level.GameElement actor =
                c.model().level().elements().isEmpty() ? null : c.model().level().elements().get(0);
        check(actor != null, "an actor element exists to inspect");
        if (actor != null) {
            c.model().setSelection(actor);
            gb.refreshUI();
            // layer selector is now a styled dropdown button (dd.layer) showing the current layer
            Component picker = find(form, "dd.layer");
            check(picker instanceof com.codename1.ui.Button, "element layer field is a dropdown");
            if (picker instanceof com.codename1.ui.Button) {
                check(actor.getLayer() != null && actor.getLayer().equals(((com.codename1.ui.Button) picker).getText()),
                        "layer dropdown shows the element's current layer");
            }
            com.codename1.gaming.level.Layer assigned = c.model().level().getLayer(actor.getLayer());
            check(assigned != null && assigned.getKind() != com.codename1.gaming.level.Layer.KIND_TILE,
                    "element sits on a valid entity layer");
            // metadata controls (#17)  (the Asset section is collapsed by default)
            check(find(form, "btn.addprop") != null, "Add-property control present");
            check(find(form, "cb.player") != null, "mark-as-player toggle present");
            // custom metadata is settable + readable (what the game reads for scoring etc.)
            actor.setProperty("hitPoints", "42");
            check(actor.getInt("hitPoints", 0) == 42, "custom metadata is readable as a typed value");
            actor.setProperty("player", true);
            check(actor.getBoolean("player", false), "mark-as-player sets a readable flag");
            actor.setProperty("player", false);
            c.model().setSelection(null);
            gb.refreshUI();
        }

        // ---- tooltips on every control (#7) ----
        String[] tipped = {
            "btn.save", "btn.live", "btn.project", "btn.undo", "btn.redo",
            "btn.grid", "btn.snap", "btn.addlayer", "btn.zoomin", "btn.zoomout",
            "scene.card", "tool.SELECT", "tool.MOVE", "tool.BRUSH", "tool.FILL",
            "tool.ERASE", "tool.PAN", "tab.platformer", "asset.ground", "asset.player"
        };
        for (String name : tipped) {
            Component cmp = find(form, name);
            String tip = cmp == null ? null : cmp.getTooltip();
            check(tip != null && !tip.isEmpty(), "tooltip present on " + name);
        }

        // ---- native desktop menus mapped to standard groups (#6) ----
        java.util.Vector menuCmds = form.getToolbar().getAllNativeMenuCommands();
        java.util.Set<String> groups = new java.util.HashSet<>();
        int withShortcut = 0;
        for (int i = 0; i < menuCmds.size(); i++) {
            com.codename1.ui.Command cmd = (com.codename1.ui.Command) menuCmds.get(i);
            String grp = cmd.getDesktopMenu();
            if (grp != null) {
                groups.add(grp);
            }
            if (cmd.getDesktopShortcutKeyChar() != 0) {
                withShortcut++;
            }
        }
        for (String grp : new String[]{"File", "Edit", "View", "Tools", "Layer", "Game"}) {
            check(groups.contains(grp), "native menu group present: " + grp);
        }
        check(withShortcut >= 5, "menu commands carry keyboard shortcuts (got " + withShortcut + ")");

        // ---- layer eye / lock / rename / delete (#15) ----
        com.codename1.gaming.level.GameElement la =
                c.model().level().elements().isEmpty() ? null : c.model().level().elements().get(0);
        if (la != null) {
            String ln = la.getLayer();
            check(find(form, "eye." + ln) != null, "layer visibility (eye) control present");
            check(find(form, "lock." + ln) != null, "layer lock control present");
            check(find(form, "edit." + ln) != null, "layer edit (rename/delete) control present");
            com.codename1.gaming.level.Layer lyr = c.model().level().getLayer(ln);
            // lock prevents moving elements on that layer
            lyr.setLocked(true);
            c.model().setSelection(la);
            double ox = la.getX();
            c.moveSelectionBy(50, 0);
            check(la.getX() == ox, "locked layer prevents moving its elements");
            lyr.setLocked(false);
            // hidden layer elements are not pickable
            lyr.setVisible(false);
            check(c.elementAt(la.getX(), la.getY()) == null, "hidden layer elements are not selectable");
            lyr.setVisible(true);
            check(c.elementAt(la.getX(), la.getY()) == la, "shown layer elements are selectable");
            // rename repoints the element's layer
            check(c.renameLayer(ln, ln + " X"), "renameLayer succeeds");
            check((ln + " X").equals(la.getLayer()), "rename repoints elements to the new layer name");
            // delete removes a layer and keeps the data alive (reassigned)
            int beforeLayers = c.model().level().layers().size();
            check(c.deleteLayer(la.getLayer()), "deleteLayer succeeds");
            check(c.model().level().layers().size() == beforeLayers - 1, "deleteLayer removes one layer");
            c.model().setSelection(null);
            gb.refreshUI();
        }

        // ---- font-size toggle + resizable dividers (#18) ----
        check(find(form, "btn.fontsize") != null, "text-size toggle present");
        check(find(form, "divider.left") != null, "left panel resize divider present");
        check(find(form, "divider.right") != null, "inspector resize divider present");
        com.codename1.ui.Font beforeFont =
                com.codename1.ui.plaf.UIManager.getInstance().getComponentStyle("GBField").getFont();
        try {
            fire(form, "btn.fontsize");   // Medium -> Large : must not throw
            // re-fetch (getComponentStyle returns a fresh copy each call)
            com.codename1.ui.Font afterFont =
                    com.codename1.ui.plaf.UIManager.getInstance().getComponentStyle("GBField").getFont();
            check(afterFont != null, "text-size toggle leaves a valid font set");
            check(beforeFont == null || afterFont.getPixelSize() != beforeFont.getPixelSize(),
                    "text-size toggle changes the font size (" + (beforeFont == null ? "?" : beforeFont.getPixelSize())
                            + "->" + afterFont.getPixelSize() + ")");
        } catch (RuntimeException ex) {
            fail.add("text-size toggle threw: " + ex);
        }

        // ---- light/dark toggle + element move preserves data (#21) ----
        check(find(form, "btn.theme") != null, "light/dark toggle present");
        String darkBg = com.codename1.gamebuilder.GameBuilderStructureHarness.bg("GBForm");
        fire(form, "btn.theme");   // -> light
        String lightBg = bg("GBForm");
        check(!darkBg.equals(lightBg), "light/dark toggle changes the app background (" + darkBg + "->" + lightBg + ")");
        fire(form, "btn.theme");   // -> dark again
        // moving an element between layers / reordering must NOT lose it
        if (!c.model().level().elements().isEmpty()) {
            com.codename1.gaming.level.GameElement mv = c.model().level().elements().get(0);
            int total = c.model().level().elements().size();
            // pick a different entity layer if one exists
            String dest = null;
            for (com.codename1.gaming.level.Layer ly : c.model().level().layers()) {
                if (ly.getKind() != com.codename1.gaming.level.Layer.KIND_TILE && !ly.getName().equals(mv.getLayer())) {
                    dest = ly.getName();
                    break;
                }
            }
            if (dest != null) {
                check(c.moveElementToLayer(mv, dest), "moveElementToLayer succeeds");
                check(mv.getLayer().equals(dest) && c.model().level().elements().contains(mv)
                        && c.model().level().elements().size() == total, "moved element is kept (not lost)");
            }
            c.reorderElement(mv, 0);
            check(c.model().level().elements().contains(mv) && c.model().level().elements().size() == total,
                    "reordered element is kept (not lost)");
        }

        // ---- custom asset import (#12) ---- (last: it switches the active pack to Custom)
        check(find(form, "btn.import") != null, "import button present in asset library");
        try {
            String newId = gb.registerImportedAsset(makePng(), "My Sprite");
            check(newId != null, "registerImportedAsset returns an id");
            check(c.model().catalog().def(newId) != null, "imported asset registered in catalog");
            check(c.model().catalog().image(newId) != null, "imported asset image resolves");
            check(c.model().catalog().getPack("custom") != null, "custom pack created");
            check(find(form, "asset." + newId) != null, "imported asset appears as a palette cell");
        } catch (Exception ex) {
            fail.add("custom import threw: " + ex);
        }
    }

    // ---- helpers -------------------------------------------------------------

    /// A tiny synthetic PNG (stand-in for a user-picked image) so the import path can be
    /// exercised headlessly without a file dialog.
    private static byte[] makePng() throws Exception {
        Image img = Image.createImage(24, 24, 0xff33cc66);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.getImageIO().save(img, bos, ImageIO.FORMAT_PNG, 1f);
        return bos.toByteArray();
    }

    /// Hex bg color of a UIID (fresh style read), for verifying the light/dark toggle.
    private static String bg(String uiid) {
        int c = com.codename1.ui.plaf.UIManager.getInstance().getComponentStyle(uiid).getBgColor() & 0xffffff;
        return Integer.toHexString(c);
    }

    private static Component find(Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component f = find((Container) c, name);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private static void fire(Form form, String name) {
        Component c = find(form, name);
        if (c instanceof Button) {
            ((Button) c).released();
        } else {
            fail.add("cannot fire (not a Button or missing): " + name);
        }
    }
}
