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

import com.codename1.gamebuilder.art.AssetArt;
import com.codename1.gamebuilder.editor.CompanionCodeGen;
import com.codename1.gamebuilder.editor.EditorController;
import com.codename1.gamebuilder.editor.EditorModel;
import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.gamebuilder.editor.TerrainBrush;
import com.codename1.gamebuilder.editor.Tool;
import com.codename1.gamebuilder.project.ProjectBinding;
import com.codename1.gamebuilder.project.ProjectIO;
import com.codename1.gamebuilder.ui.EditorCanvas;
import com.codename1.gaming.level.AssetCatalog;
import com.codename1.gaming.level.AssetDef;
import com.codename1.gaming.level.AssetPack;
import com.codename1.gaming.level.GameElement;
import com.codename1.gaming.level.GameLevel;
import com.codename1.gaming.level.Layer;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.TooltipManager;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.components.InteractionDialog;
import com.codename1.components.ToastBar;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The Codename One game builder: a visual level / map editor for the
/// `com.codename1.gaming` engine, adapted from the "GameForge" design.
///
/// Every control is wired; the live preview plays in-place (toggle, no navigation) so
/// there is never a dead-end screen. Behavior and structure are covered by tests
/// (EditorControllerTest, GameBuilderStructureHarness).
public class GameBuilder extends Lifecycle {

    private EditorController controller;
    private EditorCanvas canvas;
    private Form form;
    private ScrollY hierarchy;
    private Container assetLib;
    private Container assetPanel;
    private int assetState = 1;   // 0=collapsed, 1=normal, 2=expanded (see all assets)
    private static final float[] ASSET_HEIGHTS = {9f, 34f, 70f};
    private String editingLayer;   // layer whose inline rename/delete editor is open
    private ScrollY inspector;
    private Container rail;
    private Label zoomLabel;
    private Button undoBtn;
    private Button redoBtn;
    private ProjectBinding binding;
    private String packageName;
    private String activePackTab;
    private final Set<String> collapsedLayers = new HashSet<>();
    private final Set<String> collapsedSections = new HashSet<>();
    private final Set<String> autoCollapsedOnce = new HashSet<>();
    private boolean playing;
    private UITimer playTimer;
    private int thumb;

    // light / dark mode (the editor ships dark; this overlays a light palette)
    private boolean darkMode = true;
    private final java.util.Map<String, String> darkColors = new java.util.HashMap<>();
    private static final String[][] LIGHT_PALETTE = {
        {"GBForm", "EEF2FA", "1B2740"}, {"GBChrome", "EEF2FA", null}, {"GBSidebar", "E6ECF6", null},
        {"GBRail", "DCE4F2", null}, {"GBTopBar", "E3E9F5", null}, {"GBPanel", "FFFFFF", null},
        {"GBPanelHeaderRow", "F4F7FC", null}, {"GBPanelHeader", null, "47526A"},
        {"GBProject", "DCE4F2", "1B2740"}, {"GBChromeBtn", "E3E9F5", "33415F"},
        {"GBField", "FFFFFF", "1B2740"}, {"GBPicker", "EEF3FB", "1B2740"},
        {"GBPropLabel", null, "5C6B86"}, {"GBPropValue", null, "1B2740"},
        {"GBInspectorTitleRow", "E3E9F5", null}, {"GBInspectorTitle", null, "1B2740"},
        {"GBSection", "EEF3FB", null}, {"GBSectionTitle", null, "1B2740"}, {"GBSectionChev", null, "5C6B86"},
        {"GBLayer", "FFFFFF", null}, {"GBLayerActive", "E3E9F5", null}, {"GBLayerName", null, "1B2740"},
        {"GBLayerNameOff", null, "A6B0C4"}, {"GBCount", null, "6B7790"}, {"GBRowIcon", null, "6B7790"},
        {"GBChevron", null, "6B7790"}, {"GBEntity", "FFFFFF", "1B2740"}, {"GBEntityActive", "E3E9F5", "1B2740"},
        {"GBHint", null, "5C6B86"}, {"GBTab", "DCE4F2", "33415F"}, {"GBAsset", "EEF3FB", "1B2740"},
        {"GBAssetCount", null, "5C6B86"}, {"GBZoom", null, "1B2740"}, {"GBCheck", null, "1B2740"},
        {"GBStepper", "E3E9F5", "33415F"}, {"GBSceneCard", "FFFFFF", "1B2740"},
        {"GBPropRow", "FFFFFF", null}, {"GBTool", "DCE4F2", "33415F"}, {"GBTabs", "FFFFFF", null},
        {"GBInspectorTitle", "EAF1FF", "1B2740"},
        {"GBSceneCardActive", "E3E9F5", "1B2740"},
        {"GBDialog", "FFFFFF", null}, {"GBDialogTitle", null, "1B2740"},
        {"GBMini", "E3E9F5", "33415F"}, {"GBLayerEditor", "EEF3FB", null},
        {"GBAssetSel", "DCE4F2", "1B2740"}, {"GBBadge", "F0E2C0", "8A5A00"},
        {"GBCode", "F4F7FC", "33415F"}, {"GBAssetCount", null, "5C6B86"},
        {"GBDivider", "C2CEE4", null}, {"GBHintBox", "EEF3FB", null}
    };

    // editor text-size toggle (S / M / L)
    private int fontStep = 1;
    private static final float[] FONT_SCALES = {0.85f, 1f, 1.25f};
    private static final String[] FONT_LABELS = {"Small", "Medium", "Large"};
    private final java.util.Map<String, Float> baseFontPx = new java.util.HashMap<>();
    private static final String[] SCALE_UIIDS = {
        "GBProject", "GBField", "GBPicker", "GBPropLabel", "GBPropValue", "GBInspectorTitle",
        "GBSectionTitle", "GBPanelHeader", "GBLayerName", "GBLayerNameOff", "GBEntity",
        "GBEntityActive", "GBHint", "GBAsset", "GBAssetSel", "GBZoom", "GBCount", "GBTab",
        "GBTabActive", "GBBadge", "GBCheck", "GBLive", "GBLiveActive", "GBBuild",
        "GBSceneCard", "GBSceneCardActive", "GBAssetCount", "GBStepper"
    };

    @Override
    public void runApp() {
        Toolbar.setGlobalToolbar(true);
        TooltipManager.enableTooltips();   // hover tooltips on every control (desktop hover is
                                           // armed by GameBuilderLauncher in the javase module)
        Display.getInstance().setDragStartPercentage(0);   // drags fire on the first pixel (move/divider/3D)
        thumb = Display.getInstance().convertToPixels(5f);

        AssetCatalog catalog = StarterPacks.loadCatalog();
        AssetArt.install(catalog);

        binding = ProjectIO.loadBinding();
        GameLevel initial = StarterPacks.demoLevel();
        String sceneName = "Level1";
        if (binding != null) {
            ProjectIO.loadCustomPack(binding, catalog);   // bring back previously imported art
            packageName = binding.packageName();
            List<String> scenes = ProjectIO.listScenes(binding);
            if (!scenes.isEmpty()) {
                sceneName = scenes.get(0);
                try {
                    initial = ProjectIO.loadScene(binding, sceneName);
                } catch (IOException e) {
                    com.codename1.io.Log.e(e);
                }
            } else {
                initial = StarterPacks.newLevel(GameLevel.MODE_2D);
            }
        }
        EditorModel model = new EditorModel(initial, catalog);
        model.setSceneName(sceneName);
        controller = new EditorController(model);
        activePackTab = initial.getAssetPack();

        form = new Form("", new BorderLayout());
        form.setUIID("GBForm");
        form.setEnableCursors(true);   // desktop hover cursors (e.g. resize over dividers)
        form.getToolbar().setUIID("GBChrome");

        canvas = new EditorCanvas(controller);
        canvas.setOnChange(this::refresh);

        hierarchy = new ScrollY();
        assetLib = new Container(BoxLayout.y());
        inspector = new ScrollY();
        rail = new Container(BoxLayout.y());
        rail.setUIID("GBRail");

        // default to painting tiles so the brush works on first click
        selectFirstTileLayer();

        buildFormLayout();
        buildMenus();
        updateWindowTitle();
        refresh();
        form.show();
    }

    /// Builds (or rebuilds) the whole form shell — sidebars, panel wrappers, dividers and the
    /// top bar — around the persistent widgets (canvas/hierarchy/inspector/assetLib/rail). It
    /// is re-run on a light/dark toggle so EVERY container + its baked FontImage icons are
    /// recreated under the current theme (refreshTheme alone leaves baked icons/outer chrome
    /// on the old palette).
    private void buildFormLayout() {
        form.removeAll();
        // form.removeAll() detaches the columns but not the persistent widgets nested inside
        // them; detach those too so they can be re-parented into the freshly-built shell.
        detach(rail);
        detach(canvas);
        detach(hierarchy);
        detach(inspector);
        detach(assetLib);

        Container leftCol = new Container(new BorderLayout());
        leftCol.setUIID("GBSidebar");
        leftCol.add(BorderLayout.WEST, rail);
        leftCol.add(BorderLayout.CENTER, hierarchyPanel());
        leftCol.setPreferredW(Display.getInstance().convertToPixels(42f));
        leftCol.add(BorderLayout.EAST, vDivider(leftCol, +1));   // drag to resize the left panel

        Button assetSmaller = iconBtn("btn.assetcollapse", FontImage.MATERIAL_KEYBOARD_ARROW_DOWN, "GBChromeBtn",
                "Shrink the asset library", () -> sizeAssetLibrary(-1));
        Button assetBigger = iconBtn("btn.assetexpand", FontImage.MATERIAL_KEYBOARD_ARROW_UP, "GBChromeBtn",
                "Expand the asset library (see all assets)", () -> sizeAssetLibrary(1));
        Container assetToggles = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        assetToggles.add(assetBigger);
        assetToggles.add(assetSmaller);
        assetPanel = panel("ASSET LIBRARY", assetLib, assetToggles);
        assetPanel.setPreferredH(Display.getInstance().convertToPixels(32f));
        Container centerCol = new Container(new BorderLayout());
        centerCol.setUIID("GBForm");
        centerCol.add(BorderLayout.CENTER, canvas);
        centerCol.add(BorderLayout.SOUTH, assetPanel);

        Container right = panel("INSPECTOR", inspector, null);
        right.setPreferredW(Display.getInstance().convertToPixels(37f));
        Container rightWrap = new Container(new BorderLayout());
        rightWrap.setUIID("GBSidebar");
        rightWrap.add(BorderLayout.CENTER, right);
        rightWrap.add(BorderLayout.WEST, vDivider(right, -1));   // drag to resize the inspector

        form.add(BorderLayout.NORTH, buildTopBar());
        form.add(BorderLayout.WEST, leftCol);
        form.add(BorderLayout.CENTER, centerCol);
        form.add(BorderLayout.EAST, rightWrap);
    }

    private static void detach(Component c) {
        if (c != null && c.getParent() != null) {
            c.getParent().removeComponent(c);
        }
    }

    private void updateWindowTitle() {
        form.setTitle(controller.model().getSceneName() + " — Game Builder");
    }

    // ---- native desktop menu bar (File / Edit / View / …) --------------------

    private void buildMenus() {
        int primary = Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY;
        int shift = Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT;

        menu("File", "New 2D Platformer", (char) 0, 0, () -> newScene(GameLevel.MODE_2D));
        menu("File", "New 3D Map", (char) 0, 0, () -> newScene(GameLevel.MODE_3D));
        menu("File", "New Board Game", (char) 0, 0, () -> newScene(GameLevel.MODE_BOARD));
        menu("File", "Open Scene…", 'O', primary, this::openProjectDialog);
        menu("File", "Save", 'S', primary, this::save);
        menu("File", "Open Java in IDE", (char) 0, 0, this::openInIde);

        menu("Edit", "Undo", 'Z', primary, () -> { controller.undo(); refresh(); });
        menu("Edit", "Redo", 'Z', primary | shift, () -> { controller.redo(); refresh(); });
        menu("Edit", "Delete Selection", (char) 0, 0, () -> { controller.deleteSelection(); refresh(); });

        menu("View", "Toggle Grid", '\'', primary, () -> {
            canvas.setGridVisible(!canvas.isGridVisible());
            canvas.repaint();
            rebuildTopBar();
        });
        menu("View", "Toggle Snap to Grid", (char) 0, 0, () -> {
            controller.model().setSnap(!controller.model().isSnap());
            rebuildTopBar();
        });
        menu("View", "Zoom In", '=', primary, () -> { canvas.zoomBy(1.25); afterZoom(); });
        menu("View", "Zoom Out", '-', primary, () -> { canvas.zoomBy(0.8); afterZoom(); });
        menu("View", "3D: Perspective / Top-down", (char) 0, 0, () -> {
            canvas.setTopDown3D(!canvas.isTopDown3D());
            ToastBar.showMessage(canvas.isTopDown3D() ? "3D: top-down view" : "3D: perspective view",
                    FontImage.MATERIAL_3D_ROTATION);
            canvas.repaint();
        });
        menu("View", "Toggle Light / Dark", (char) 0, 0, this::toggleLightDark);
        menu("View", "Cycle Text Size", (char) 0, 0, this::cycleFontSize);
        menu("View", "Show Generated Code", (char) 0, 0, this::showCode);

        for (Tool t : Tool.values()) {
            menu("Tools", t.label(), (char) 0, 0, () -> { controller.model().setTool(t); refresh(); });
        }

        menu("Layer", "Add Layer", (char) 0, 0, () -> {
            controller.addEntityLayer("Layer " + (controller.model().level().layers().size() + 1));
            refresh();
        });

        menu("Game", "Play / Stop", 'R', primary, this::toggleLive);
    }

    private static String toolTip(Tool t) {
        return switch (t) {
            case SELECT -> "Select — click an object to edit it";
            case MOVE -> "Move — drag the selected object";
            case BRUSH -> "Brush — paint tiles, or stamp the selected actor";
            case FILL -> "Fill — flood-fill the active tile layer";
            case TERRAIN -> "Terrain — sculpt ground height, holes & walls (3D)";
            case ERASE -> "Erase — remove a tile or delete an object";
            case PAN -> "Pan — drag to scroll the canvas";
        };
    }

    private void menu(String group, String name, char shortcut, int modifiers, Runnable action) {
        Command c = Command.create(name, null, e -> action.run());
        c.setDesktopMenu(group);
        if (shortcut != 0) {
            c.setDesktopShortcut(shortcut, modifiers == 0 ? Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY : modifiers);
        }
        form.getToolbar().addCommandToOverflowMenu(c);
    }

    private void newScene(int mode) {
        controller.newLevel(mode);
        activePackTab = controller.model().level().getAssetPack();
        collapsedLayers.clear();
        selectFirstTileLayer();
        updateWindowTitle();
        refresh();
    }

    /// Creates a streaming/region "large world" scene (a GameWorld of linked regions). The
    /// active region's streaming terrain is edited and previewed; regions are managed from the
    /// inspector's World section.
    private void newLargeWorldScene() {
        controller.loadLevel(StarterPacks.newLargeWorld(), "World1");
        activePackTab = controller.model().level().getAssetPack();
        collapsedLayers.clear();
        controller.model().setSelection(null);
        updateWindowTitle();
        refresh();
    }

    // ---- test/introspection hooks -------------------------------------------

    public EditorController getController() {
        return controller;
    }

    public EditorCanvas getCanvas() {
        return canvas;
    }

    public Form getForm() {
        return form;
    }

    public boolean isPlaying() {
        return playing;
    }

    // ---- top bar -------------------------------------------------------------

    private Container buildTopBar() {
        Container bar = new Container(new BorderLayout());
        bar.setUIID("GBTopBar");

        Label logo = new Label("", "GBLogoMark");
        FontImage.setMaterialIcon(logo, FontImage.MATERIAL_VIDEOGAME_ASSET, 5.5f);
        Button project = new Button(controller.model().getSceneName(), "GBProject");
        project.setName("btn.project");
        FontImage.setMaterialIcon(project, FontImage.MATERIAL_ARROW_DROP_DOWN, 5f);
        project.setTextPosition(Component.LEFT);
        project.setTooltip("Project / scene — open, rename, or start a new game type");
        project.addActionListener(e -> openProjectDialog());
        undoBtn = iconBtn("btn.undo", FontImage.MATERIAL_UNDO, "GBChromeBtn", "Undo (Cmd+Z)", () -> { controller.undo(); refresh(); });
        undoBtn.setEnabled(controller.canUndo());
        redoBtn = iconBtn("btn.redo", FontImage.MATERIAL_REDO, "GBChromeBtn", "Redo (Cmd+Shift+Z)", () -> { controller.redo(); refresh(); });
        redoBtn.setEnabled(controller.canRedo());
        Button grid = iconBtn("btn.grid", FontImage.MATERIAL_GRID_ON, canvas.isGridVisible() ? "GBToggleOn" : "GBChromeBtn", "Show / hide the grid", () -> {
            canvas.setGridVisible(!canvas.isGridVisible());
            canvas.repaint();
            rebuildTopBar();
        });
        Button snap = iconBtn("btn.snap", FontImage.MATERIAL_FILTER_CENTER_FOCUS, controller.model().isSnap() ? "GBToggleOn" : "GBChromeBtn", "Snap objects to grid cells (on/off)", () -> {
            controller.model().setSnap(!controller.model().isSnap());
            rebuildTopBar();
        });
        zoomLabel = new Label(Math.round(canvas.getZoom() * 100) + "%", "GBZoom");
        Button zout = iconBtn("btn.zoomout", FontImage.MATERIAL_REMOVE, "GBChromeBtn", "Zoom out (Cmd+-)", () -> { canvas.zoomBy(0.8); afterZoom(); });
        Button zin = iconBtn("btn.zoomin", FontImage.MATERIAL_ADD, "GBChromeBtn", "Zoom in (Cmd+=)", () -> { canvas.zoomBy(1.25); afterZoom(); });
        Button theme = iconBtn("btn.theme", darkMode ? FontImage.MATERIAL_DARK_MODE : FontImage.MATERIAL_LIGHT_MODE,
                "GBChromeBtn", "Toggle light / dark mode", this::toggleLightDark);
        Button fontSize = iconBtn("btn.fontsize", FontImage.MATERIAL_FORMAT_SIZE, "GBChromeBtn",
                "Cycle editor text size (Small / Medium / Large)", this::cycleFontSize);

        // Tools and zoom are left-aligned in one non-wrapping row (conventional desktop
        // toolbar); only the Live / Build actions are pinned to the right. This avoids a
        // squeezed center region where the zoom controls would wrap or clip behind the
        // actions when a long scene name widens the left group.
        Container left = new Container(BoxLayout.x());
        left.getStyle().setBgTransparency(0);
        left.add(logo);
        left.add(project);
        left.add(undoBtn);
        left.add(redoBtn);
        left.add(grid);
        left.add(snap);
        left.add(zout);
        left.add(zoomLabel);
        left.add(zin);
        left.add(theme);
        left.add(fontSize);
        // FlowLayout wrapper keeps the row at its natural height, vertically centered.
        Container leftWrap = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        leftWrap.add(left);
        bar.add(BorderLayout.WEST, leftWrap);

        Button live = new Button(playing ? "Stop" : "Live", playing ? "GBLiveActive" : "GBLive");
        live.setName("btn.live");
        FontImage.setMaterialIcon(live, playing ? FontImage.MATERIAL_STOP : FontImage.MATERIAL_FIBER_MANUAL_RECORD, 4.5f);
        live.setTextPosition(Component.RIGHT);
        live.setGap(Display.getInstance().convertToPixels(1.6f));
        live.setTooltip("Play the level in place (Stop to return) — Cmd+R");
        live.addActionListener(e -> toggleLive());
        Button save = new Button("Save", "GBBuild");
        save.setName("btn.save");
        FontImage.setMaterialIcon(save, FontImage.MATERIAL_SAVE, 4.5f);
        save.setGap(Display.getInstance().convertToPixels(1.6f));
        save.setTooltip("Save the level to the project (Cmd+S)");
        save.addActionListener(e -> save());
        Container right = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        right.add(live);
        right.add(save);
        bar.add(BorderLayout.EAST, right);
        return bar;
    }

    private void afterZoom() {
        zoomLabel.setText(Math.round(canvas.getZoom() * 100) + "%");
        canvas.repaint();
        zoomLabel.getParent().revalidate();
    }

    private void rebuildTopBar() {
        Container content = form.getContentPane();
        Component north = ((BorderLayout) content.getLayout()).getNorth();
        if (north != null) {
            content.removeComponent(north);
        }
        content.add(BorderLayout.NORTH, buildTopBar());
        content.revalidate();
    }

    // ---- live preview (in place, no navigation) ------------------------------

    private void toggleLive() {
        if (playing) {
            stopLive();
        } else {
            playing = true;
            canvas.setPlayMode(true);
            playTimer = new UITimer(() -> { canvas.tick(0.033); canvas.repaint(); });
            playTimer.schedule(33, true, form);
        }
        rebuildTopBar();
        // grab focus AFTER the top bar rebuild so arrow keys reach the canvas in play mode
        if (playing) {
            form.setFocused(canvas);
            canvas.requestFocus();
        }
        canvas.repaint();
    }

    private void stopLive() {
        playing = false;
        if (playTimer != null) {
            playTimer.cancel();
            playTimer = null;
        }
        canvas.setPlayMode(false);
    }

    // ---- tool rail -----------------------------------------------------------

    private void populateRail() {
        rail.removeAll();
        boolean is3d = controller.model().level().getMode() == GameLevel.MODE_3D;
        for (Tool t : Tool.values()) {
            if (t == Tool.TERRAIN && !is3d) {
                continue;   // terrain sculpting is a 3D-only concept
            }
            Button b = new Button("", controller.model().getTool() == t ? "GBToolActive" : "GBTool");
            b.setName("tool." + t.name());
            FontImage.setMaterialIcon(b, t.icon(), 5f);
            b.setTooltip(toolTip(t));
            b.addActionListener(e -> { controller.model().setTool(t); refresh(); });
            rail.add(b);
        }
    }

    // ---- panels --------------------------------------------------------------

    private Container hierarchyPanel() {
        Button add = iconBtn("btn.addlayer", FontImage.MATERIAL_ADD, "GBChromeBtn", "Add a layer", () -> {
            controller.addEntityLayer("Layer " + (controller.model().level().layers().size() + 1));
            refresh();
        });
        return panel("HIERARCHY", hierarchy, add);
    }

    private Container panel(String title, Container body, Component headerAction) {
        Container c = new Container(new BorderLayout());
        c.setUIID("GBPanel");
        Container header = new Container(new BorderLayout());
        header.setUIID("GBPanelHeaderRow");
        header.add(BorderLayout.CENTER, new Label(title, "GBPanelHeader"));
        if (headerAction != null) {
            header.add(BorderLayout.EAST, headerAction);
        }
        c.add(BorderLayout.NORTH, header);
        c.add(BorderLayout.CENTER, body);
        return c;
    }

    private Button iconBtn(String name, char icon, String uiid, String tooltip, Runnable action) {
        Button b = new Button("", uiid);
        if (name != null) {
            b.setName(name);
        }
        FontImage.setMaterialIcon(b, icon, 4.5f);
        if (tooltip != null) {
            b.setTooltip(tooltip);
        }
        b.addActionListener(e -> action.run());
        return b;
    }

    // ---- dynamic refresh -----------------------------------------------------

    /// Public hook so tests can force a UI refresh after driving the controller directly.
    public void refreshUI() {
        refresh();
    }

    private void refresh() {
        // dispose any tooltip first: rebuilding the panels/toolbar below detaches the
        // component a tooltip may be anchored to, which would strand its full-size popup
        // layer on top of the form and swallow all pointer events (the "frozen white UI").
        com.codename1.ui.TooltipManager.hideTooltip();
        // preserve panel scroll so editing a value (e.g. a stepper) doesn't jump to the top
        int inspScroll = inspector == null ? 0 : inspector.getScrollY();
        int hierScroll = hierarchy == null ? 0 : hierarchy.getScrollY();
        rebuildTopBar();
        updateWindowTitle();
        populateRail();
        rebuildHierarchy();
        rebuildAssetLibrary();
        rebuildInspector();
        if (undoBtn != null) {
            undoBtn.setEnabled(controller.canUndo());
            redoBtn.setEnabled(controller.canRedo());
        }
        canvas.repaint();
        form.revalidate();
        restoreScroll(inspector, inspScroll);
        restoreScroll(hierarchy, hierScroll);
    }

    /// Restores a scrollable panel's vertical position after a rebuild (clamped).
    private void restoreScroll(ScrollY c, int y) {
        if (c == null || y <= 0) {
            return;
        }
        Display.getInstance().callSerially(() -> {
            int max = Math.max(0, c.getScrollDimension().getHeight() - c.getHeight());
            c.toY(Math.min(y, max));
            c.repaint();
        });
    }

    /// A vertically-scrollable container that exposes the (protected) scroll setter so the
    /// inspector/hierarchy can keep their position across a rebuild.
    private static final class ScrollY extends Container {
        ScrollY() {
            super(BoxLayout.y());
            setScrollableY(true);
        }

        void toY(int y) {
            setScrollY(y);
        }
    }

    private void rebuildHierarchy() {
        hierarchy.removeAll();
        EditorModel m = controller.model();
        GameLevel level = m.level();

        Button scene = new Button(m.getSceneName() + " Scene   Form " + level.getCols() + "×" + level.getRows(),
                m.getSelection() == null ? "GBSceneCardActive" : "GBSceneCard");
        scene.setName("scene.card");
        FontImage.setMaterialIcon(scene, FontImage.MATERIAL_DASHBOARD, 5.5f);
        scene.setTextPosition(Component.RIGHT);
        scene.setTooltip("Scene settings (grid, gravity, background)");
        scene.addActionListener(e -> { m.setSelection(null); refresh(); });
        hierarchy.add(scene);

        List<Layer> layers = level.layers();
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            boolean open = !collapsedLayers.contains(layer.getName());
            hierarchy.add(layerRow(layer, open));
            if (layer.getName().equals(editingLayer)) {
                hierarchy.add(layerEditorRow(layer));
            }
            if (open) {
                List<GameElement> els = level.elements();
                for (int j = 0; j < els.size(); j++) {
                    GameElement el = els.get(j);
                    if (layer.getName().equals(el.getLayer())) {
                        hierarchy.add(entityRow(el));
                    }
                }
            }
        }
    }

    private Container layerRow(Layer layer, boolean open) {
        EditorModel m = controller.model();
        Container row = new Container(new BorderLayout());
        row.setUIID(layer.getName().equals(m.getActiveLayer()) ? "GBLayerActive" : "GBLayer");
        row.setName("layer." + layer.getName());

        Button chev = new Button("", "GBChevron");
        FontImage.setMaterialIcon(chev, open ? FontImage.MATERIAL_KEYBOARD_ARROW_DOWN : FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT, 4.5f);
        chev.addActionListener(e -> {
            if (open) {
                collapsedLayers.add(layer.getName());
            } else {
                collapsedLayers.remove(layer.getName());
            }
            refresh();
        });
        Button name = new Button(layer.getName() + (layer.isLocked() ? "  (locked)" : ""),
                layer.isVisible() ? "GBLayerName" : "GBLayerNameOff");
        name.setTooltip("Make this the active layer for painting / stamping");
        name.addActionListener(e -> { m.setActiveLayer(layer.getName()); refresh(); });
        Container nameWrap = new Container(new BorderLayout());
        nameWrap.add(BorderLayout.WEST, chev);
        nameWrap.add(BorderLayout.CENTER, name);
        row.add(BorderLayout.CENTER, nameWrap);

        Label count = new Label(String.valueOf(layerCount(layer)), "GBCount");
        Button eye = iconBtn(null, layer.isVisible() ? FontImage.MATERIAL_VISIBILITY : FontImage.MATERIAL_VISIBILITY_OFF,
                "GBRowIcon", layer.isVisible() ? "Hide this layer" : "Show this layer",
                () -> { layer.setVisible(!layer.isVisible()); refresh(); });
        eye.setName("eye." + layer.getName());
        Button lock = iconBtn(null, layer.isLocked() ? FontImage.MATERIAL_LOCK : FontImage.MATERIAL_LOCK_OPEN,
                "GBRowIcon", layer.isLocked() ? "Unlock this layer" : "Lock this layer (prevent edits)",
                () -> { layer.setLocked(!layer.isLocked()); refresh(); });
        lock.setName("lock." + layer.getName());
        Button edit = iconBtn(null, FontImage.MATERIAL_MORE_VERT, "GBRowIcon",
                "Rename or delete this layer", () -> {
                    editingLayer = layer.getName().equals(editingLayer) ? null : layer.getName();
                    refresh();
                });
        edit.setName("edit." + layer.getName());
        Container actions = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        actions.add(count);
        actions.add(eye);
        actions.add(lock);
        actions.add(edit);
        row.add(BorderLayout.EAST, actions);
        return row;
    }

    /// Inline rename / delete editor shown under a layer row (no dialog). Compact icon
    /// buttons so it fits the narrow hierarchy panel.
    private Container layerEditorRow(Layer layer) {
        Container box = new Container(BoxLayout.y());
        box.setUIID("GBLayerEditor");
        TextField nameField = new TextField(layer.getName());
        nameField.setUIID("GBField");
        nameField.setName("layeredit.name");
        box.add(nameField);
        Button rename = iconBtn("layeredit.ok", FontImage.MATERIAL_CHECK, "GBMiniPrimary", "Rename", () -> {
            controller.renameLayer(layer.getName(), nameField.getText());
            editingLayer = null;
            refresh();
        });
        Button delete = iconBtn("layeredit.del", FontImage.MATERIAL_DELETE, "GBMiniDanger", "Delete layer", () -> {
            if (!controller.deleteLayer(layer.getName())) {
                ToastBar.showMessage("Can't delete the last layer", FontImage.MATERIAL_INFO);
            }
            editingLayer = null;
            refresh();
        });
        Button cancel = iconBtn("layeredit.cancel", FontImage.MATERIAL_CLOSE, "GBMini", "Cancel", () -> {
            editingLayer = null;
            refresh();
        });
        Container buttons = new Container(new GridLayout(1, 3));
        buttons.add(rename);
        buttons.add(delete);
        buttons.add(cancel);
        box.add(buttons);
        return box;
    }

    private int layerCount(Layer layer) {
        if (layer.getKind() == Layer.KIND_TILE) {
            return layer.tiles().size();
        }
        int n = 0;
        List<GameElement> els = controller.model().level().elements();
        for (int i = 0; i < els.size(); i++) {
            if (layer.getName().equals(els.get(i).getLayer())) {
                n++;
            }
        }
        return n;
    }

    private Container entityRow(GameElement el) {
        EditorModel m = controller.model();
        Container row = new Container(new BorderLayout());
        Button name = new Button(el.getName() == null ? el.getAssetId() : el.getName(),
                el == m.getSelection() ? "GBEntityActive" : "GBEntity");
        name.setName("entity." + el.getId());
        Image img = m.catalog() == null ? null : m.catalog().image(el.getAssetId());
        if (img != null) {
            name.setIcon(img.scaled(thumb * 3 / 4, thumb * 3 / 4));
        }
        name.setTextPosition(Component.RIGHT);
        name.setTooltip("Drag onto a layer to move it there, or onto another item to reorder");
        name.addActionListener(e -> { m.setSelection(el); refresh(); });
        name.setDraggable(true);                       // drag to reorder / move between layers
        // We do NOT use setDropTarget on the rows — CN1's default drop physically reparents
        // the dragged component (the bug that duplicated layers / lost the row). Instead we
        // hit-test the drop point ourselves on drag-finish, then rebuild from the model.
        name.addDragFinishedListener(e -> {
            final int dx = e.getX();
            final int dy = e.getY();
            Display.getInstance().callSerially(() -> handleHierarchyDrop(el, dx, dy));
        });
        row.add(BorderLayout.CENTER, name);
        AssetDef def = m.catalog() == null ? null : m.catalog().def(el.getAssetId());
        if (def != null && def.isUnique()) {
            row.add(BorderLayout.EAST, new Label("UNIQUE", "GBBadge"));
        }
        return row;
    }

    /// Applies a hierarchy drag: drop onto a layer row moves the element to that layer;
    /// drop onto another item moves it to that item's layer and reorders next to it.
    private void handleHierarchyDrop(GameElement dragged, int x, int y) {
        if (dragged == null) {
            refresh();
            return;
        }
        String target = rowNameAt(hierarchy, x, y);
        if (target != null && target.startsWith("layer.")) {
            controller.moveElementToLayer(dragged, target.substring("layer.".length()));
        } else if (target != null && target.startsWith("entity.")) {
            String id = target.substring("entity.".length());
            List<GameElement> els = controller.model().level().elements();
            for (GameElement t : els) {
                if (t.getId().equals(id) && t != dragged) {
                    controller.moveElementToLayer(dragged, t.getLayer());
                    controller.reorderElement(dragged, controller.model().level().elements().indexOf(t));
                    break;
                }
            }
        }
        refresh();   // always rebuild from the model, so nothing is ever "lost"
    }

    /// First component with the given name anywhere under root, or null.
    private Component findByName(Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component f = findByName((Container) c, name);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    /// The name of the deepest {@code layer.*} / {@code entity.*} row under an absolute point.
    private String rowNameAt(Container root, int x, int y) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            int ax = c.getAbsoluteX();
            int ay = c.getAbsoluteY();
            if (x < ax || x > ax + c.getWidth() || y < ay || y > ay + c.getHeight()) {
                continue;
            }
            if (c instanceof Container) {
                String inner = rowNameAt((Container) c, x, y);
                if (inner != null) {
                    return inner;
                }
            }
            String n = c.getName();
            if (n != null && (n.startsWith("layer.") || n.startsWith("entity."))) {
                return n;
            }
        }
        return null;
    }

    // ---- asset library -------------------------------------------------------

    /// Grows / shrinks the asset library by one step (collapsed → normal → expanded):
    /// expanded shows all assets in a scrollable grid; collapsed reclaims the canvas.
    private void sizeAssetLibrary(int delta) {
        assetState = Math.max(0, Math.min(ASSET_HEIGHTS.length - 1, assetState + delta));
        applyAssetPanelHeight();
        refresh();
        form.revalidate();
    }

    private void applyAssetPanelHeight() {
        if (assetPanel != null) {
            assetPanel.setPreferredH(Display.getInstance().convertToPixels(ASSET_HEIGHTS[assetState]));
        }
    }

    private void rebuildAssetLibrary() {
        assetLib.removeAll();
        EditorModel m = controller.model();
        if (m.catalog() == null || assetState == 0) {
            return;   // collapsed: header only, canvas gets the space
        }
        // Default to the level's own pack when the active tab is unset or not in the
        // catalog (e.g. just after loading a 3D level), so the relevant assets show first.
        if (activePackTab == null || m.catalog().getPack(activePackTab) == null) {
            activePackTab = m.catalog().getPack(m.level().getAssetPack()) != null
                    ? m.level().getAssetPack()
                    : (m.catalog().packs().isEmpty() ? null : m.catalog().packs().get(0).getId());
        }
        Container tabs = new Container(BoxLayout.x());
        tabs.setUIID("GBTabs");
        tabs.setScrollableX(true);
        tabs.setScrollVisible(false);
        List<AssetPack> packs = m.catalog().packs();
        int assetCount = 0;
        for (int i = 0; i < packs.size(); i++) {
            AssetPack p = packs.get(i);
            if (p.getId().equals(activePackTab)) {
                assetCount = p.size();
            }
            Button tab = new Button(p.getName(), p.getId().equals(activePackTab) ? "GBTabActive" : "GBTab");
            tab.setName("tab." + p.getId());
            tab.setTooltip("Show the " + p.getName() + " asset pack (" + p.size() + " assets)");
            tab.addActionListener(e -> { activePackTab = p.getId(); rebuildAssetLibrary(); form.revalidate(); });
            tabs.add(tab);
        }
        Container header = new Container(new BorderLayout());
        header.add(BorderLayout.CENTER, tabs);
        Container east = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        east.add(new Label(assetCount + " assets", "GBAssetCount"));
        Button importBtn = iconBtn("btn.import", FontImage.MATERIAL_ADD_PHOTO_ALTERNATE, "GBChromeBtn",
                "Import your own image as a new asset", this::importImage);
        importBtn.setTextPosition(Component.RIGHT);
        importBtn.setText("Import");
        importBtn.setGap(Display.getInstance().convertToPixels(1.4f));
        east.add(importBtn);
        header.add(BorderLayout.EAST, east);
        assetLib.add(header);

        Label hint = new Label("Click an asset then click the stage, or drag it onto the stage. "
                + "Use the ⌄ button to expand and see every asset.", "GBHint");
        hint.setName("asset.hint");
        assetLib.add(hint);

        AssetPack pack = m.catalog().getPack(activePackTab);
        if (pack == null) {
            return;
        }
        Container strip = new Container(new FlowLayout(Component.LEFT, Component.TOP));
        strip.setScrollableY(true);
        for (AssetDef def : pack.assets()) {
            Button cell = new Button(def.getName(), def.getId().equals(m.getSelectedAssetId()) ? "GBAssetSel" : "GBAsset");
            cell.setName("asset." + def.getId());
            Image img = m.catalog().image(def.getId());
            if (img != null) {
                cell.setIcon(img.scaled(thumb * 2, thumb * 2));
            }
            cell.setTextPosition(Component.BOTTOM);
            cell.setTooltip(def.getName() + (def.isTile() ? " — tile (paint into cells)" : " — actor (stamp on the stage)"));
            cell.addActionListener(e -> selectAsset(def));
            // drag an asset straight onto the canvas to place it there
            cell.setDraggable(true);
            cell.addDragFinishedListener(e -> {
                final int dx = e.getX();
                final int dy = e.getY();
                Display.getInstance().callSerially(() -> handleAssetDrop(def, dx, dy));
            });
            strip.add(cell);
        }
        assetLib.add(strip);
    }

    /// Places a dragged asset where it was dropped on the canvas (ignored if dropped
    /// elsewhere). Selecting it first switches to a compatible layer so placement works.
    private void handleAssetDrop(AssetDef def, int x, int y) {
        int ax = canvas.getAbsoluteX();
        int ay = canvas.getAbsoluteY();
        if (x < ax || x > ax + canvas.getWidth() || y < ay || y > ay + canvas.getHeight()) {
            refresh();   // dropped outside the canvas — just rebuild so nothing looks stuck
            return;
        }
        selectAsset(def);
        canvas.placeAssetAt(x, y);
        refresh();
    }

    // ---- custom asset import -------------------------------------------------

    /// Opens the native image picker and imports the chosen file as a new asset.
    private void importImage() {
        Display.getInstance().openGallery(e -> {
            if (e == null || e.getSource() == null) {
                return;   // cancelled
            }
            String path = e.getSource().toString();
            try {
                byte[] data = readAllBytes(path);
                String name = displayNameFor(path);
                String id = registerImportedAsset(data, name);
                if (id != null) {
                    ToastBar.showMessage("Imported \"" + name + "\"", FontImage.MATERIAL_CHECK_CIRCLE);
                }
            } catch (IOException ex) {
                Log.e(ex);
                ToastBar.showErrorMessage("Import failed: " + ex.getMessage());
            }
        }, Display.GALLERY_IMAGE);
    }

    /// Registers raw image bytes as a new asset in the project's "Custom" pack: builds
    /// the `AssetDef` (kind from the active layer, size from the image), makes the art
    /// usable immediately, and -- when bound to a project -- persists the PNG and the
    /// pack so it survives reload. Returns the new asset id (factored out of the file
    /// picker so it is unit-testable). Public for the structure harness.
    public String registerImportedAsset(byte[] data, String displayName) {
        AssetCatalog cat = controller.model().catalog();
        Image img;
        try {
            img = Image.createImage(data, 0, data.length);
        } catch (RuntimeException ex) {
            Log.e(ex);
            return null;
        }
        if (img == null) {
            return null;
        }
        String base = sanitizeId(displayName);
        String id = base;
        int n = 1;
        while (cat.def(id) != null) {
            id = base + "_" + (n++);
        }
        boolean tile = isTileActiveLayer();
        int w = clampDim(img.getWidth());
        int h = clampDim(img.getHeight());
        AssetDef def = new AssetDef(id, tile ? AssetDef.KIND_TILE : AssetDef.KIND_ACTOR, 0xffcccccc, w, h)
                .setName(displayName).setSource("assets/" + id + ".png");
        AssetPack custom = cat.getPack(ProjectIO.CUSTOM_PACK_ID);
        if (custom == null) {
            custom = new AssetPack(ProjectIO.CUSTOM_PACK_ID, "Custom");
        }
        custom.add(def);
        cat.addPack(custom);            // (re)indexes the pack so cat.def(id) resolves
        cat.setImage(id, img);
        if (binding != null) {
            try {
                ProjectIO.saveCustomAsset(binding, id, data);
                ProjectIO.saveCustomPack(binding, customPackJson(custom));
            } catch (IOException ex) {
                Log.e(ex);
            }
        }
        activePackTab = ProjectIO.CUSTOM_PACK_ID;
        selectAsset(def);
        return id;
    }

    private boolean isTileActiveLayer() {
        Layer l = controller.model().level().getLayer(controller.model().getActiveLayer());
        return l != null && l.getKind() == Layer.KIND_TILE;
    }

    private static int clampDim(int d) {
        return Math.max(8, Math.min(96, d));
    }

    private static String sanitizeId(String name) {
        StringBuilder sb = new StringBuilder();
        String s = name == null ? "" : name.toLowerCase();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else if (c == ' ' || c == '-' || c == '_') {
                sb.append('_');
            }
        }
        String id = sb.toString();
        return id.isEmpty() ? "asset" : id;
    }

    /// The display name from a picked file path (strip directory + extension).
    private static String displayNameFor(String path) {
        String p = path == null ? "asset" : path.replace('\\', '/');
        int slash = p.lastIndexOf('/');
        if (slash >= 0) {
            p = p.substring(slash + 1);
        }
        int dot = p.lastIndexOf('.');
        if (dot > 0) {
            p = p.substring(0, dot);
        }
        return p.isEmpty() ? "asset" : p;
    }

    private static byte[] readAllBytes(String path) throws IOException {
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                bos.write(buf, 0, r);
            }
            return bos.toByteArray();
        } finally {
            Util.cleanup(in);
        }
    }

    /// Serializes the custom pack to a {@code {"packs":[{..}]}} document for persistence.
    private static String customPackJson(AssetPack pack) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"packs\":[{\"id\":").append(jsonStr(pack.getId()))
                .append(",\"name\":").append(jsonStr(pack.getName()))
                .append(",\"assets\":[");
        List<AssetDef> defs = pack.assets();
        for (int i = 0; i < defs.size(); i++) {
            AssetDef d = defs.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(jsonStr(d.getId()))
                    .append(",\"name\":").append(jsonStr(d.getName()))
                    .append(",\"kind\":").append(d.isTile() ? "\"tile\"" : "\"actor\"")
                    .append(",\"w\":").append(d.getWidth())
                    .append(",\"h\":").append(d.getHeight())
                    .append(",\"source\":").append(jsonStr(d.getSource()))
                    .append('}');
        }
        sb.append("]}]}");
        return sb.toString();
    }

    private static String jsonStr(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /// Selects an asset for stamping/painting and switches the active layer to a
    /// compatible one (tile asset -> tile layer, actor -> entity layer) so the brush
    /// works on the very next click.
    private void selectAsset(AssetDef def) {
        controller.model().setSelectedAssetId(def.getId());
        if (def.isTile()) {
            selectFirstTileLayer();
        } else {
            selectFirstEntityLayer();
        }
        refresh();
    }

    private void selectFirstTileLayer() {
        EditorModel m = controller.model();
        Layer active = m.level().getLayer(m.getActiveLayer());
        if (active != null && active.getKind() == Layer.KIND_TILE) {
            return;
        }
        for (Layer l : m.level().layers()) {
            if (l.getKind() == Layer.KIND_TILE) {
                m.setActiveLayer(l.getName());
                return;
            }
        }
    }

    /// The next entity/model layer after `current` (cyclic), for moving an element.
    private String nextEntityLayer(String current) {
        String[] names = entityLayerNames();
        if (names.length == 0) {
            return current;
        }
        int i = -1;
        for (int k = 0; k < names.length; k++) {
            if (names[k].equals(current)) {
                i = k;
                break;
            }
        }
        return names[(i + 1) % names.length];
    }

    /// The names of every entity/model layer (the layers an element can live on).
    private String[] entityLayerNames() {
        List<Layer> ls = controller.model().level().layers();
        List<String> names = new java.util.ArrayList<>();
        for (Layer l : ls) {
            if (l.getKind() != Layer.KIND_TILE) {
                names.add(l.getName());
            }
        }
        return names.toArray(new String[0]);
    }

    private void selectFirstEntityLayer() {
        EditorModel m = controller.model();
        Layer active = m.level().getLayer(m.getActiveLayer());
        if (active != null && active.getKind() != Layer.KIND_TILE) {
            return;
        }
        for (Layer l : m.level().layers()) {
            if (l.getKind() != Layer.KIND_TILE) {
                m.setActiveLayer(l.getName());
                return;
            }
        }
    }

    // ---- inspector -----------------------------------------------------------

    private void rebuildInspector() {
        inspector.removeAll();
        EditorModel m = controller.model();
        GameLevel level = m.level();
        GameElement sel = m.getSelection();
        if (m.getTool() == Tool.TERRAIN && level.getMode() == GameLevel.MODE_3D) {
            addTerrainBrushPanel(level);
        }
        if (sel == null) {
            com.codename1.components.SpanLabel hint = new com.codename1.components.SpanLabel(
                    "Nothing selected — pick an object in the hierarchy or canvas to edit it, or stamp from the Asset Library.");
            hint.setUIID("GBHintBox");
            hint.setTextUIID("GBHint");
            inspector.add(hint);

            if (sectionHeader("Form / Scene")) {
                inspector.add(editRow("Class name", m.getSceneName(), controller::renameScene));
                // editable numeric fields (a side-scroller can be hundreds of columns wide —
                // typing beats clicking +). Invalid / non-positive input is ignored.
                inspector.add(intField("Cols", level.getCols(),
                        v -> { controller.resizeGrid(v, level.getRows(), level.getTileSize()); refresh(); }));
                inspector.add(intField("Rows", level.getRows(),
                        v -> { controller.resizeGrid(level.getCols(), v, level.getTileSize()); refresh(); }));
            }
            if (sectionHeader("Tile grid")) {
                inspector.add(intField("Tile size", level.getTileSize(),
                        v -> { controller.resizeGrid(level.getCols(), level.getRows(), v); refresh(); }));
                inspector.add(propRow("Gravity", String.valueOf(level.getDouble("gravity", 9.8))));
                inspector.add(dropdownRow("Background", new String[]{"Sky", "Forest", "Night", "Cave"},
                        level.getString("background", "Sky"),
                        v -> { controller.setLevelProperty("background", v); refresh(); }));
                inspector.add(propRow("Mode", GameLevel.modeLabel(level.getMode())));
                if (level.getMode() == GameLevel.MODE_3D) {
                    inspector.add(dropdownRow("3D play style", new String[]{"open", "flight", "race", "dungeon"},
                            level.getString("view3d", "open"),
                            v -> { controller.setLevelProperty("view3d", v); refresh(); }));
                }
            }
            if (level.isLargeWorld()) {
                addWorldPanel(level);
            }
            return;
        }
        Container title = new Container(new BorderLayout());
        title.setUIID("GBInspectorTitleRow");
        Label tl = new Label(sel.getName() == null ? sel.getAssetId() : sel.getName(), "GBInspectorTitle");
        Image img = m.catalog() == null ? null : m.catalog().image(sel.getAssetId());
        if (img != null) {
            tl.setIcon(img.scaled(thumb, thumb));
        }
        title.add(BorderLayout.CENTER, tl);
        inspector.add(title);
        if (sectionHeader("Object")) {
            inspector.add(editRow("Name", sel.getName() == null ? "" : sel.getName(), v -> { sel.setName(v); markDirty(); }));
            inspector.add(editRow("X", trim(sel.getX()), v -> { sel.setX(parse(v, sel.getX())); markDirty(); }));
            inspector.add(editRow("Y", trim(sel.getY()), v -> { sel.setY(parse(v, sel.getY())); markDirty(); }));
            if (level.getMode() == GameLevel.MODE_3D) {
                inspector.add(editRow("Elevation (Z)", trim(sel.getZ()),
                        v -> { sel.setZ(parse(v, sel.getZ())); markDirty(); }));
            }
            // Layer selector — a styled dropdown listing every entity/model layer, so it is
            // clear which band the element lives on and how to re-assign it.
            String[] entityLayers = entityLayerNames();
            String curLayer = sel.getLayer() == null ? (entityLayers.length > 0 ? entityLayers[0] : "") : sel.getLayer();
            Container layerRow = dropdownRow("Layer", entityLayers, curLayer, chosen -> {
                if (chosen != null && !chosen.equals(sel.getLayer())) {
                    sel.setLayer(chosen);
                    markDirty();
                    refresh();
                }
            });
            inspector.add(layerRow);
            inspector.add(floatStepperRow("Size", sel.getScaleX() + "×",
                    () -> { sel.setScale(clampScale(sel.getScaleX() - 0.25f)); markDirty(); refresh(); },
                    () -> { sel.setScale(clampScale(sel.getScaleX() + 0.25f)); markDirty(); refresh(); }));
        }
        // Behavior — typed game metadata on THIS object, read at runtime (hitPoints, value…)
        if (sectionHeader("Behavior")) {
            for (String key : sel.properties().keySet()) {
                if ("player".equals(key)) {
                    continue;   // surfaced as the Player toggle below
                }
                final String k = key;
                inspector.add(editRow(k, String.valueOf(sel.getProperty(k)),
                        v -> { sel.setProperty(k, v); markDirty(); }));
            }
            inspector.add(inlineAdd("Add property", "name e.g. hitPoints", "btn.addprop",
                    name -> { sel.setProperty(name, "0"); markDirty(); refresh(); }));

            boolean isPlayer = sel.getBoolean("player", false) || isPlayerAsset(sel.getAssetId());
            Button playerBtn = new Button("This is the player", isPlayer ? "GBToggleOn" : "GBChromeBtn");
            playerBtn.setName("cb.player");
            FontImage.setMaterialIcon(playerBtn,
                    isPlayer ? FontImage.MATERIAL_CHECK_CIRCLE : FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, 4f);
            playerBtn.setTooltip("Mark this element as the player the preview and arrow keys control");
            playerBtn.addActionListener(e -> {
                sel.setProperty("player", !(sel.getBoolean("player", false) || isPlayerAsset(sel.getAssetId())));
                markDirty();
                refresh();
            });
            inspector.add(playerBtn);
        }
        // Asset — the SHARED template. Clearly separated + collapsed by default so it is
        // never confused with this object's own values. Edits here affect every instance.
        final AssetDef sdef = m.catalog() == null ? null : m.catalog().def(sel.getAssetId());
        collapseByDefault("Asset: " + (sdef == null ? "?" : sdef.getId()));
        if (sdef != null && sectionHeader("Asset: " + sdef.getId())) {
            com.codename1.components.SpanLabel note = new com.codename1.components.SpanLabel(
                    "Shared template — changes here affect EVERY object using this asset.");
            note.setUIID("GBHintBox");
            note.setTextUIID("GBHint");
            inspector.add(note);
            inspector.add(propRow("Kind", sdef.isTile() ? "Tile" : "Actor"));
            inspector.add(propRow("Unique", sdef.isUnique() ? "yes" : "no"));
            inspector.add(stepperRow("Default width", sdef.getWidth(),
                    () -> { sdef.setSize(Math.max(4, sdef.getWidth() - 4), sdef.getHeight()); markDirty(); refresh(); },
                    () -> { sdef.setSize(sdef.getWidth() + 4, sdef.getHeight()); markDirty(); refresh(); }));
            inspector.add(stepperRow("Default height", sdef.getHeight(),
                    () -> { sdef.setSize(sdef.getWidth(), Math.max(4, sdef.getHeight() - 4)); markDirty(); refresh(); },
                    () -> { sdef.setSize(sdef.getWidth(), sdef.getHeight() + 4); markDirty(); refresh(); }));
            for (String key : sdef.defaultProperties().keySet()) {
                final String k = key;
                inspector.add(editRow(k, String.valueOf(sdef.defaultProperties().get(k)),
                        v -> { sdef.putDefault(k, v); markDirty(); }));
            }
            inspector.add(inlineAdd("Add default", "name", "btn.editasset",
                    name -> { sdef.putDefault(name, "0"); markDirty(); refresh(); }));
        }
        Button del = new Button("Delete", "GBDanger");
        del.setName("btn.delete");
        FontImage.setMaterialIcon(del, FontImage.MATERIAL_DELETE, 4.5f);
        del.addActionListener(e -> { controller.deleteSelection(); refresh(); });
        inspector.add(del);
    }

    private void markDirty() {
        controller.model().setDirty(true);
        canvas.repaint();
    }

    /// A thin draggable divider that resizes a side panel (CN1 has no SplitPane). {@code sign}
    /// is +1 when the panel is to the divider's left (drag right to widen) and -1 when it
    /// is to the right (drag left to widen). Implemented as a Component overriding
    /// pointerDragged (a plain Container's drag listeners don't reliably receive drags).
    private Component vDivider(Container target, int sign) {
        return new Divider(target, sign, this::revalidateForm);
    }

    private void revalidateForm() {
        form.revalidate();
    }

    /// Draggable resize handle between panels.
    private static final class Divider extends Component {
        private final Container target;
        private final int sign;
        private final Runnable onResize;
        private final int minW;
        private final int maxW;
        private int pressX;
        private int startW;

        Divider(Container target, int sign, Runnable onResize) {
            this.target = target;
            this.sign = sign;
            this.onResize = onResize;
            this.minW = Display.getInstance().convertToPixels(26f);
            this.maxW = Display.getInstance().convertToPixels(72f);
            setUIID("GBDivider");
            setName(sign > 0 ? "divider.left" : "divider.right");
            setTooltip("Drag to resize this panel");
            setDraggable(false);
            setGrabsPointerEvents(true);
            setCursor(W_RESIZE_CURSOR);   // horizontal-resize cursor on hover (desktop)
            getAllStyles().setMargin(0, 0, 0, 0);
            setPreferredW(Display.getInstance().convertToPixels(1.6f));
        }

        /// Resize with no distance threshold so the drag starts immediately.
        @Override
        protected int getDragRegionStatus(int x, int y) {
            return DRAG_REGION_IMMEDIATELY_DRAG_XY;
        }

        /// Keep receiving drag events for the whole gesture, even when the pointer leaves
        /// the thin divider (otherwise the resize stops the moment you move off it).
        @Override
        protected boolean isStickyDrag() {
            return true;
        }

        @Override
        public void pointerPressed(int x, int y) {
            pressX = x;
            startW = target.getWidth();
        }

        @Override
        public void pointerDragged(int x, int y) {
            int w = Math.max(minW, Math.min(maxW, startW + sign * (x - pressX)));
            target.setPreferredW(w);
            onResize.run();
        }

        @Override
        protected void paintBackground(Graphics g) {
            super.paintBackground(g);
            // a little grip so it reads as a draggable handle
            g.setColor(0x9fb4dd);
            int cx = getX() + getWidth() / 2;
            int cy = getY() + getHeight() / 2;
            for (int i = -2; i <= 2; i++) {
                g.fillArc(cx - 1, cy + i * 6 - 1, 3, 3, 0, 360);
            }
        }
    }

    /// Toggles between the dark (default) and a light palette by overlaying color props.
    private void toggleLightDark() {
        darkMode = !darkMode;
        applyColorTheme(darkMode);
        buildFormLayout();   // recreate every container + baked icon under the new palette
        refresh();
        form.revalidateLater();
        ToastBar.showMessage(darkMode ? "Dark mode" : "Light mode", FontImage.MATERIAL_BRIGHTNESS_6);
    }

    private void applyColorTheme(boolean dark) {
        UIManager u = UIManager.getInstance();
        java.util.Hashtable<String, Object> props = new java.util.Hashtable<>();
        for (String[] row : LIGHT_PALETTE) {
            String id = row[0];
            Style s = u.getComponentStyle(id);
            if (!darkColors.containsKey(id + ".bg")) {
                darkColors.put(id + ".bg", hex6(s.getBgColor()));
                darkColors.put(id + ".fg", hex6(s.getFgColor()));
            }
            if (row[1] != null) {
                String v = dark ? darkColors.get(id + ".bg") : row[1];
                props.put(id + ".bgColor", v);
                props.put(id + ".sel#bgColor", v);
                props.put(id + ".press#bgColor", v);
                props.put(id + ".dis#bgColor", v);
            }
            if (row[2] != null) {
                String v = dark ? darkColors.get(id + ".fg") : row[2];
                props.put(id + ".fgColor", v);
                props.put(id + ".sel#fgColor", v);
                props.put(id + ".press#fgColor", v);
                props.put(id + ".dis#fgColor", v);
            }
        }
        u.addThemeProps(props);
        if (form != null) {
            form.refreshTheme(false);   // re-derive styles for ALL components (incl. static chrome)
        }
    }

    private static String hex6(int c) {
        String s = Integer.toHexString(c & 0xffffff);
        while (s.length() < 6) {
            s = "0" + s;
        }
        return s;
    }

    /// Cycles the editor text size (Small / Medium / Large) and re-applies it.
    private void cycleFontSize() {
        fontStep = (fontStep + 1) % FONT_SCALES.length;
        applyFontScale(FONT_SCALES[fontStep]);
        refresh();
        ToastBar.showMessage("Text size: " + FONT_LABELS[fontStep], FontImage.MATERIAL_FORMAT_SIZE);
    }

    /// Scales the fonts of the editor's text UIIDs relative to their theme baseline by
    /// merging font overrides into the theme and refreshing live components. The baseline
    /// is captured once so repeated toggles never compound.
    private void applyFontScale(float scale) {
        UIManager u = UIManager.getInstance();
        java.util.Hashtable<String, Object> props = new java.util.Hashtable<>();
        for (String id : SCALE_UIIDS) {
            Font f = scaledFont(u.getComponentStyle(id).getFont(), id, scale);
            if (f != null) {
                props.put(id + ".font", f);
                props.put(id + ".sel#font", f);
                props.put(id + ".press#font", f);
            }
        }
        u.addThemeProps(props);
        if (form != null) {
            form.refreshTheme(true);
        }
    }

    /// A version of `base` sized by `scale` (TrueType derive, or a system-font S/M/L
    /// fallback for native fonts that can't be derived), preserving the weight.
    private Font scaledFont(Font f, String key, float scale) {
        if (f == null) {
            return null;
        }
        Float base = baseFontPx.get(key);
        if (base == null) {
            float px = f.getPixelSize();
            if (px <= 0) {
                px = f.getHeight();
            }
            base = px;
            baseFontPx.put(key, base);
        }
        int weight = f.getStyle();
        try {
            return f.derive(base * scale, weight);
        } catch (RuntimeException ex) {
            int size = scale < 0.95f ? Font.SIZE_SMALL : (scale > 1.1f ? Font.SIZE_LARGE : Font.SIZE_MEDIUM);
            return Font.createSystemFont(Font.FACE_PROPORTIONAL, weight, size);
        }
    }

    private static boolean isPlayerAsset(String id) {
        return "player".equals(id) || "hero".equals(id) || "spawn".equals(id);
    }

    private String nextBackground(String cur) {
        String[] opts = {"Sky", "Forest", "Night", "Cave"};
        for (int i = 0; i < opts.length; i++) {
            if (opts[i].equals(cur)) {
                return opts[(i + 1) % opts.length];
            }
        }
        return opts[0];
    }

    /// Collapses a section the FIRST time it is shown (afterwards the user controls it).
    private void collapseByDefault(String title) {
        if (autoCollapsedOnce.add(title)) {
            collapsedSections.add(title);
        }
    }

    /// The World / Regions panel for a large-world scene: switch the active region (whose
    /// streaming terrain you edit and preview) and grow the world by adding linked neighbours.
    private void addWorldPanel(GameLevel level) {
        com.codename1.gaming.level.GameWorld world = level.getWorld();
        if (world == null) {
            return;
        }
        com.codename1.components.SpanLabel hint = new com.codename1.components.SpanLabel(
                "Large world — a graph of streaming regions. Edit/preview the active region; add "
                + "linked neighbours to grow the map. Regions page in/out around the player at runtime.");
        hint.setUIID("GBHintBox");
        hint.setTextUIID("GBHint");
        inspector.add(hint);
        if (sectionHeader("World / Regions")) {
            java.util.List<com.codename1.gaming.level.Region> regions = world.residentRegions();
            String[] ids = new String[regions.size()];
            for (int i = 0; i < regions.size(); i++) {
                ids[i] = regions.get(i).getId();
            }
            String active = world.getActiveRegion() != null ? world.getActiveRegion().getId()
                    : (ids.length > 0 ? ids[0] : "");
            inspector.add(dropdownRow("Active region", ids, active, v -> {
                world.setActiveRegion(v);
                markDirty();
                refresh();
            }));
            inspector.add(propRow("Regions", String.valueOf(regions.size())));

            Container add = new Container(new GridLayout(2, 2));
            add.setUIID("GBPropRow");
            add.add(addRegionButton(world, "north"));
            add.add(addRegionButton(world, "south"));
            add.add(addRegionButton(world, "west"));
            add.add(addRegionButton(world, "east"));
            inspector.add(add);
        }
    }

    private Button addRegionButton(com.codename1.gaming.level.GameWorld world, String dir) {
        Button b = new Button("+ " + dir, "GBMini");
        b.setName("btn.addregion." + dir);
        b.addActionListener(e -> {
            com.codename1.gaming.level.Region from = world.getActiveRegion();
            if (from == null) {
                return;
            }
            String opp = "north".equals(dir) ? "south" : "south".equals(dir) ? "north"
                    : "west".equals(dir) ? "east" : "west";
            String id = dir + "-" + (world.residentRegions().size() + 1);
            double w = from.getWidth() > 0 ? from.getWidth() : 64;
            double d = from.getDepth() > 0 ? from.getDepth() : 64;
            double ox = from.getOriginX() + ("east".equals(dir) ? w : "west".equals(dir) ? -w : 0);
            double oz = from.getOriginZ() + ("south".equals(dir) ? d : "north".equals(dir) ? -d : 0);
            com.codename1.gaming.level.Region n = new com.codename1.gaming.level.Region(id, dir + " region")
                    .setOrigin(ox, oz).setSpan(w, d).link(opp, from.getId());
            from.link(dir, id);
            world.addRegion(n);
            world.setActiveRegion(id);
            markDirty();
            refresh();
        });
        return b;
    }

    private static final String[] MATERIAL_LABELS = {"Grass", "Road", "Stone", "Sand", "Water", "Dirt"};

    /// The Terrain tool's controls: pick what a dab does (raise/lower/ground/wall/paint), the
    /// surface material to paint, and fill/clear the whole floor. Shown at the top of the
    /// inspector while the Terrain tool is active.
    private void addTerrainBrushPanel(GameLevel level) {
        com.codename1.components.SpanLabel hint = new com.codename1.components.SpanLabel(
                "Terrain tool — drag on the top-down map to sculpt. Raise/Lower for hills & ramps, "
                + "Paint for surfaces (road, grass…), Wall for rooms. Hit Live to walk it.");
        hint.setUIID("GBHintBox");
        hint.setTextUIID("GBHint");
        inspector.add(hint);
        if (sectionHeader("Terrain brush")) {
            String[] brushes = new String[TerrainBrush.values().length];
            for (int i = 0; i < brushes.length; i++) {
                brushes[i] = TerrainBrush.values()[i].label();
            }
            inspector.add(dropdownRow("Brush", brushes, controller.model().getTerrainBrush().label(), v -> {
                for (TerrainBrush b : TerrainBrush.values()) {
                    if (b.label().equals(v)) {
                        controller.model().setTerrainBrush(b);
                        break;
                    }
                }
                refresh();
            }));
            int curMat = Math.max(0, Math.min(MATERIAL_LABELS.length - 1, controller.model().getTerrainMaterial()));
            inspector.add(dropdownRow("Surface", MATERIAL_LABELS, MATERIAL_LABELS[curMat], v -> {
                for (int i = 0; i < MATERIAL_LABELS.length; i++) {
                    if (MATERIAL_LABELS[i].equals(v)) {
                        controller.model().setTerrainMaterial(i);
                        break;
                    }
                }
                refresh();
            }));

            Container row = new Container(new GridLayout(1, 2));
            row.setUIID("GBPropRow");
            Button fill = new Button("Fill surface", "GBMini");
            fill.setName("btn.terrainfill");
            fill.addActionListener(e -> { controller.fillGround(true); refresh(); });
            Button clear = new Button("Clear to sky", "GBMini");
            clear.setName("btn.terrainclear");
            clear.addActionListener(e -> { controller.fillGround(false); refresh(); });
            row.add(fill);
            row.add(clear);
            inspector.add(row);
        }
    }

    private boolean sectionHeader(String title) {
        boolean open = !collapsedSections.contains(title);
        Container row = new Container(new BorderLayout());
        row.setUIID("GBSection");
        Label chev = new Label("", "GBSectionChev");
        FontImage.setMaterialIcon(chev, open ? FontImage.MATERIAL_KEYBOARD_ARROW_DOWN : FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT, 4.5f);
        row.add(BorderLayout.WEST, chev);
        row.add(BorderLayout.CENTER, new Label(title, "GBSectionTitle"));
        row.addPointerReleasedListener(e -> {
            if (open) {
                collapsedSections.add(title);
            } else {
                collapsedSections.remove(title);
            }
            refresh();
        });
        inspector.add(row);
        return open;
    }

    /// A property row with the label STACKED ABOVE its field (matches the design and keeps
    /// every row aligned regardless of label length). The field fills the row width.
    private Container labeled(String label, Component field) {
        Container row = new Container(BoxLayout.y());
        row.setUIID("GBPropRow");
        row.add(new Label(label, "GBPropLabel"));
        row.add(field);
        return row;
    }

    private Container propRow(String label, String value) {
        Container row = new Container(BoxLayout.y());
        row.setUIID("GBPropRow");
        row.add(new Label(label, "GBPropLabel"));
        row.add(new Label(value, "GBPropValue"));
        return row;
    }

    private Container stepperRow(String label, int value, Runnable minus, Runnable plus) {
        return floatStepperRow(label, String.valueOf(value), minus, plus);
    }

    /// A label-above +/- stepper whose value is a pre-formatted string.
    private Container floatStepperRow(String label, String value, Runnable minus, Runnable plus) {
        Button dec = iconBtn(null, FontImage.MATERIAL_REMOVE, "GBStepper", "Decrease", minus);
        Button inc = iconBtn(null, FontImage.MATERIAL_ADD, "GBStepper", "Increase", plus);
        Label v = new Label(value, "GBPropValue");
        Container ctrl = new Container(new BorderLayout());
        ctrl.add(BorderLayout.WEST, dec);
        ctrl.add(BorderLayout.CENTER, v);
        ctrl.add(BorderLayout.EAST, inc);
        v.getAllStyles().setAlignment(Component.CENTER);
        return labeled(label, ctrl);
    }

    private static float clampScale(float s) {
        return Math.max(0.25f, Math.min(4f, Math.round(s * 4f) / 4f));
    }

    /// An inline "add" row (a hinted text field + Add button) used in the inspector so
    /// adding metadata/defaults never opens a dialog.
    private Container inlineAdd(String label, String hint, String name, Setter onAdd) {
        TextField tf = new TextField();
        tf.setUIID("GBField");
        tf.setHint(hint);
        Button add = iconBtn(name, FontImage.MATERIAL_ADD, "GBStepper", "Add", () -> {
            String t = tf.getText().trim();
            if (!t.isEmpty()) {
                onAdd.set(t);
            }
        });
        Container row = new Container(new BorderLayout());
        row.add(BorderLayout.CENTER, tf);
        row.add(BorderLayout.EAST, add);
        return labeled(label, row);
    }

    private interface Setter {
        void set(String value);
    }

    /// A labelled string Picker (the real CN1 spinner Picker — its popup is themed via the
    /// Picker* / Spinner3D* UIIDs in theme.css). Used for the 3D play style, background,
    /// terrain brush, surface material and the element layer.
    private Container dropdownRow(String label, String[] options, String current, Setter onPick) {
        Picker p = new Picker();
        p.setName("dd." + label.toLowerCase().replace(' ', '_'));
        p.setType(Display.PICKER_TYPE_STRINGS);
        p.setStrings(options);
        p.setSelectedString(current != null && contains(options, current) ? current
                : (options.length > 0 ? options[0] : ""));
        p.setUIID("GBPicker");
        p.addActionListener(e -> {
            String v = p.getSelectedString();
            if (v != null) {
                onPick.set(v);
            }
        });
        return labeled(label, p);
    }

    private static boolean contains(String[] arr, String v) {
        for (String s : arr) {
            if (s.equals(v)) {
                return true;
            }
        }
        return false;
    }

    private Container editRow(String label, String value, Setter onChange) {
        TextField tf = new TextField(value);
        tf.setUIID("GBField");
        tf.addActionListener(e -> onChange.set(tf.getText()));
        tf.addDataChangedListener((type, index) -> onChange.set(tf.getText()));
        return labeled(label, tf);
    }

    private interface IntSetter {
        void set(int value);
    }

    /// A validated numeric field: commits on Enter/focus-change, ignores blank/non-positive
    /// input (so you can type a big number instead of clicking + hundreds of times).
    private Container intField(String label, int value, IntSetter onChange) {
        TextField tf = new TextField(String.valueOf(value));
        tf.setUIID("GBField");
        tf.setConstraint(TextField.NUMERIC);
        tf.addActionListener(e -> commitInt(tf, onChange));
        tf.addFocusListener(new com.codename1.ui.events.FocusListener() {
            public void focusGained(Component c) {
            }

            public void focusLost(Component c) {
                commitInt(tf, onChange);
            }
        });
        return labeled(label, tf);
    }

    private void commitInt(TextField tf, IntSetter onChange) {
        try {
            int v = Integer.parseInt(tf.getText().trim());
            if (v > 0) {
                onChange.set(v);
            }
        } catch (NumberFormatException ignore) {
            // leave the field for the user to correct
        }
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? Long.toString((long) d) : Double.toString(d);
    }

    private static double parse(String v, double fallback) {
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---- dialogs / save ------------------------------------------------------

    /// Opens the scene's companion {@code .java} in the system editor/IDE, located from the
    /// bound project (writing it first if it does not exist yet). Falls back to the
    /// in-editor code view when running standalone (no bound project).
    private void openInIde() {
        String scene = controller.model().getSceneName();
        if (binding != null && binding.sourceDir() != null) {
            String pkgPath = packageName == null || packageName.isEmpty() ? "" : packageName.replace('.', '/');
            String dir = pkgPath.isEmpty() ? binding.sourceDir() : binding.sourceDir() + "/" + pkgPath;
            String fileUrl = toFileUrl(dir + "/" + scene + ".java");
            try {
                if (!FileSystemStorage.getInstance().exists(fileUrl)) {
                    save();   // generate the companion on first open
                }
            } catch (RuntimeException ignore) {
                // fall through and try to open whatever is there
            }
            Display.getInstance().execute(fileUrl);
            ToastBar.showMessage("Opening " + scene + ".java in your editor…", FontImage.MATERIAL_CODE);
            return;
        }
        ToastBar.showMessage("No bound project — showing the generated code instead", FontImage.MATERIAL_INFO);
        showCode();
    }

    private static String toFileUrl(String osPath) {
        if (osPath == null || osPath.startsWith("file:")) {
            return osPath;
        }
        String s = osPath.replace('\\', '/');
        return s.startsWith("/") ? "file://" + s : "file:///" + s;
    }

    private void showCode() {
        Dialog d = new Dialog("Generated " + controller.model().getSceneName() + ".java");
        d.setLayout(new BorderLayout());
        String java = CompanionCodeGen.companionJava(packageName, controller.model().getSceneName(),
                "/" + controller.model().getSceneName() + ".game", controller.model().level());
        TextArea ta = new TextArea(java, 26, 70);
        ta.setEditable(false);
        ta.setUIID("GBCode");
        d.add(BorderLayout.CENTER, ta);
        Button close = new Button("Close", "GBProject");
        close.addActionListener(e -> d.dispose());
        d.add(BorderLayout.SOUTH, close);
        d.show();
    }

    private void openProjectDialog() {
        InteractionDialog d = new InteractionDialog("Project / Scene");
        d.setUIID("GBDialog");
        d.getTitleComponent().setUIID("GBDialogTitle");
        d.setLayout(BoxLayout.y());
        d.addComponent(new Label("Scene name", "GBPropLabel"));
        TextField nameField = new TextField(controller.model().getSceneName());
        nameField.setUIID("GBField");
        d.addComponent(nameField);
        if (binding != null) {
            List<String> scenes = ProjectIO.listScenes(binding);
            if (scenes.size() > 1) {
                d.addComponent(new Label("Open scene", "GBPropLabel"));
                for (String s : scenes) {
                    Button b = new Button(s, "GBMini");
                    b.addActionListener(e -> {
                        try {
                            controller.loadLevel(ProjectIO.loadScene(binding, s), s);
                            activePackTab = controller.model().level().getAssetPack();
                            d.dispose();
                            refresh();
                        } catch (IOException err) {
                            ToastBar.showErrorMessage("Open failed: " + err);
                        }
                    });
                    d.addComponent(b);
                }
            }
        }
        d.addComponent(new Label("New scene (game type)", "GBPropLabel"));
        Container modes = new Container(new GridLayout(2, 2));
        modes.add(newSceneButton("2D", GameLevel.MODE_2D, d));
        modes.add(newSceneButton("3D Map", GameLevel.MODE_3D, d));
        modes.add(newSceneButton("Board", GameLevel.MODE_BOARD, d));
        Button largeWorld = new Button("Large World", "GBMini");
        largeWorld.setName("btn.newlargeworld");
        largeWorld.addActionListener(e -> { d.dispose(); newLargeWorldScene(); });
        modes.add(largeWorld);
        d.addComponent(modes);

        Button ok = new Button("Rename", "GBMiniPrimary");
        ok.addActionListener(e -> { controller.renameScene(nameField.getText()); d.dispose(); refresh(); });
        Button cancel = new Button("Cancel", "GBMini");
        cancel.addActionListener(e -> d.dispose());
        Container btns = new Container(new GridLayout(1, 2));
        btns.add(cancel);
        btns.add(ok);
        d.addComponent(btns);
        // popup anchored to the project button, sized to its content (no full-screen chrome,
        // no empty bottom gap)
        Component anchor = findByName(form, "btn.project");
        if (anchor != null) {
            d.showPopupDialog(anchor);
        } else {
            int sw = Display.getInstance().getDisplayWidth();
            int dw = Display.getInstance().convertToPixels(66f);
            int left = Math.max(0, (sw - dw) / 2);
            d.show(Display.getInstance().convertToPixels(14f),
                    Display.getInstance().convertToPixels(14f), left, left);
        }
    }

    private Button newSceneButton(String label, int mode, InteractionDialog d) {
        Button b = new Button(label, "GBMini");
        b.addActionListener(e -> { d.dispose(); newScene(mode); });
        return b;
    }

    private void save() {
        String scene = controller.model().getSceneName();
        if (binding != null) {
            try {
                ProjectIO.saveScene(binding, controller.model(), packageName);
                controller.model().setDirty(false);
                com.codename1.components.ToastBar.showInfoMessage("Saved " + scene + ".game to project");
            } catch (IOException err) {
                com.codename1.io.Log.e(err);
                com.codename1.components.ToastBar.showErrorMessage("Save failed: " + err);
            }
        } else {
            com.codename1.components.ToastBar.showInfoMessage(
                    "Standalone mode — launch via mvn cn1:gamebuilder to save into a project");
        }
    }
}
