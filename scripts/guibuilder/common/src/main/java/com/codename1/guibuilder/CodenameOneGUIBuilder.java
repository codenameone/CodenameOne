/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.guibuilder;

import com.codename1.components.SpanLabel;
import com.codename1.components.SplitPane;
import com.codename1.components.ToastBar;
import com.codename1.guibuilder.model.GuiDocument;
import com.codename1.guibuilder.project.ProjectBinding;
import com.codename1.guibuilder.project.ProjectIO;
import com.codename1.guibuilder.ui.ComponentPreviewFactory;
import com.codename1.guibuilder.ui.DragGuideOverlay;
import com.codename1.guibuilder.ui.GuidedLayoutSupport;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.mcp.MCP;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Font;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextField;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.TooltipManager;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.accessibility.AccessibilityGrouping;
import com.codename1.ui.accessibility.AccessibilityLiveRegion;
import com.codename1.ui.accessibility.AccessibilityRole;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.MutableResource;
import com.codename1.ui.util.Resources;
import com.codename1.xml.Element;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodenameOneGUIBuilder extends Lifecycle {
    private static CodenameOneGUIBuilder active;
    private ProjectBinding binding;
    private List<String> guiFiles = new ArrayList<>();
    private GuiDocument document;
    private Form workspace;
    private Container formsPanel;
    private Container hierarchyPanel;
    private Container canvasHost;
    private Container canvasOverlayHost;
    private Container inspectorHost;
    private Label status;
    private String paletteFilter = "";
    private Map<Element, String> javaNames = new LinkedHashMap<>();
    private String clipboardXml;
    private boolean darkMode;
    private String canvasMode = "phonePortrait";
    private Element dropGuideTarget;
    private Hashtable projectTheme;
    /// The look and feel of the design canvas. The preview hierarchy owns it so the project theme
    /// and the builder's own chrome can coexist; they cannot share the global UIManager.
    private UIManager previewUIManager = UIManager.createInstance();
    /// The device surface the canvas UIManager is attached to, so a new one can replace it.
    private Container deviceSurface;
    private int themeApplyCount;
    private Hashtable builderTheme;
    private Component previewRoot;
    private Component formToolbarPreview;
    private Label formTitlePreview;
    private DragGuideOverlay dragGuideOverlay;
    private Container selectionActions;
    private final LinkedHashSet<Element> selectedElements = new LinkedHashSet<>();
    private Component boxDropSpacer;
    private Element boxDropTargetElement;
    private Container boxDropParent;
    private int boxDropIndex = -1;
    private int inspectorTabIndex;
    private boolean refreshPending;
    private TextField inlineEditor;
    private Component inlineEditorSource;
    private Container inlineEditorParent;
    private Element inlineEditorTarget;
    private String inlineEditorAttribute;
    private boolean finishingInlineEditor;
    private DropPlan activeDropPlan;
    private CodeEditor activeCodeEditor;
    /** Reopens whichever editor was on screen after the canvas is rebuilt; null when none is. */
    private Runnable activeEditorReopen;
    private boolean reopeningEditor;
    /**
     * Live mirror of the open editor's text, and the text it last agreed with on disk. A canvas
     * rebuild destroys the editor component, so without a mirror the reopen re-read the file and
     * every unsaved keystroke vanished the moment the user dropped, deleted or undid anything.
     * The pair also answers whether the buffer is dirty, which is what Close has to know.
     */
    private String editorBuffer;
    private String editorBufferOnDisk;
    private String lastObservedCss;
    private com.codename1.ui.util.UITimer cssLiveTimer;
    private int cssEditRevision;
    private Element designerDraggedElement;
    private GuiDocument designerDragDocument;
    private String designerPaletteType;
    private Component designerDragSource;
    private Runnable designerSuppressAction;
    private int designerPressX;
    private int designerPressY;
    private int designerGrabOffsetX;
    private int designerGrabOffsetY;
    private boolean designerDragArmed;
    private boolean designerDragActive;
    private Element guidedResizeElement;
    private Component guidedResizeSource;
    private int guidedResizeEdges;
    private int guidedResizePressX;
    private int guidedResizePressY;
    private int guidedResizeStartX;
    private int guidedResizeStartY;
    private int guidedResizeStartW;
    private int guidedResizeStartH;
    private boolean guidedResizeArmed;
    private boolean guidedResizeActive;
    private Component designerCursorComponent;
    private ResizePlan activeResizePlan;
    private GuiBuilderMcpController mcpController;
    private String lastDragJournalSignature;

    @Override
    public void init(Object context) {
        super.init(context);
        active = this;
        Resources global = Resources.getGlobalResources();
        String[] themeNames = global == null ? new String[0] : global.getThemeResourceNames();
        builderTheme = themeNames.length == 0 ? new Hashtable() : global.getTheme(themeNames[0]);
        Display.getInstance().setProperty("GUIBuilderDesignMode", "true");
        Display.getInstance().setDragStartPercentage(1);
        binding = ProjectIO.loadBinding();
        String requestedDarkMode = System.getProperty("guibuilder.darkMode");
        darkMode = requestedDarkMode == null
                ? Preferences.get("guibuilder.darkMode", Boolean.TRUE.equals(Display.getInstance().isDarkMode()))
                : Boolean.parseBoolean(requestedDarkMode);
        Display.getInstance().setDarkMode(Boolean.valueOf(darkMode));
        Log.bindCrashProtection(false);
    }

    @Override
    public void runApp() {
        Toolbar.setGlobalToolbar(true);
        TooltipManager.enableTooltips();
        if (binding == null) {
            showUnboundState();
            return;
        }
        guiFiles = ProjectIO.findGuiFiles(binding.guiDir());
        String requestedCanvasMode = System.getProperty("guibuilder.canvasMode");
        if (requestedCanvasMode != null && requestedCanvasMode.length() > 0) canvasMode = requestedCanvasMode;
        workspace = new Form("Codename One GUI Builder", new BorderLayout()) {
            @Override
            public void pointerHover(int[] x, int[] y) {
                if (x != null && y != null && x.length > 0 && y.length > 0) {
                    updateDesignerHoverCursor(x[0], y[0]);
                }
                super.pointerHover(x, y);
            }
        };
        workspace.setName("GUIBuilderWorkspace");
        workspace.setEnableCursors(true);
        workspace.getSemantics().setIdentifier("guibuilder.workspace")
                .setPaneTitle("Codename One GUI Builder").setGrouping(AccessibilityGrouping.GROUP);
        workspace.getToolbar().setTitleCentered(false);
        installToolbar(workspace.getToolbar());
        workspace.add(BorderLayout.CENTER, buildWorkspace());
        status = new Label("Ready", "BuilderStatus");
        status.getSemantics().setIdentifier("guibuilder.status").setLabel("GUI Builder status")
                .setLiveRegion(AccessibilityLiveRegion.POLITE);
        workspace.add(BorderLayout.SOUTH, status);
        workspace.show();
        installDragGuideOverlay();
        if (!guiFiles.isEmpty()) {
            String initial = resolveInitialForm();
            openForm(initial == null ? guiFiles.get(0) : initial);
            String initialSelection = System.getProperty("guibuilder.initialSelection");
            if (initialSelection != null && initialSelection.length() > 0) {
                String[] names = initialSelection.split(",");
                for (int i = 0; i < names.length; i++) {
                    Element element = findElementNamed(document, names[i].trim());
                    if (element != null) selectElement(element, i > 0, "startup");
                }
            }
            if ("css".equals(System.getProperty("guibuilder.openEditor"))) {
                Display.getInstance().callSerially(this::openCss);
            } else if ("java".equals(System.getProperty("guibuilder.openEditor"))) {
                Display.getInstance().callSerially(this::openCompanionSource);
            }
        } else {
            showEmptyProject();
        }
        installWorkspacePointerRouting();
        configureMcp();
    }

    private void configureMcp() {
        mcpController = new GuiBuilderMcpController(this);
        mcpController.register();
        recordAction("mcp_ready", "socketSupported", Boolean.valueOf(MCP.isSocketSupported()));
        String requestedPort = System.getProperty("guibuilder.mcp.port");
        if (requestedPort == null || requestedPort.trim().length() == 0 || MCP.isRunning()) return;
        try {
            int port = Integer.parseInt(requestedPort.trim());
            if (port < 1 || port > 65535) throw new NumberFormatException("port out of range");
            MCP.startSocketServer(port);
            recordAction("mcp_starting", "port", Integer.valueOf(port));
            setStatus("MCP starting on 127.0.0.1:" + port);
            final int checkedPort = port;
            new com.codename1.ui.util.UITimer(() -> {
                if (MCP.isRunning()) {
                    recordAction("mcp_started", "port", Integer.valueOf(checkedPort));
                    setStatus("Ready • MCP listening on 127.0.0.1:" + checkedPort);
                } else {
                    recordAction("mcp_error", "message", "Unable to bind 127.0.0.1:" + checkedPort);
                    setStatus("MCP unavailable • port " + checkedPort + " is already in use");
                }
            }).schedule(700, false, workspace);
        } catch (Throwable ex) {
            Log.e(ex);
            recordAction("mcp_error", "message", ex.getMessage());
            setStatus("MCP unavailable • " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    /** Form initialization may transfer pointer listeners to an owning form. Install the
     * designer's capture routing only after show(), when this is definitively the active form. */
    private void installWorkspacePointerRouting() {
        workspace.addPointerPressedListener(e -> handleDesignerPointerPressed(e.getX(), e.getY(), additiveSelection(e)));
        workspace.addPointerDraggedListener(e -> {
            if (guidedResizeArmed) updateGuidedResize(e.getX(), e.getY());
            else updateDesignerDrag(e.getX(), e.getY());
        });
        workspace.addPointerReleasedListener(e -> {
            if (guidedResizeArmed) finishGuidedResize(e.getX(), e.getY());
            else finishDesignerDrag(e.getX(), e.getY());
        });
    }

    void handleDesignerPointerPressed(int x, int y) {
        handleDesignerPointerPressed(x, y, false);
    }

    void handleDesignerPointerPressed(int x, int y, boolean additive) {
        if (selectionActions != null && selectionActions.isVisible()
                && x >= selectionActions.getAbsoluteX() && x <= selectionActions.getAbsoluteX() + selectionActions.getWidth()
                && y >= selectionActions.getAbsoluteY() && y <= selectionActions.getAbsoluteY() + selectionActions.getHeight()) return;
        TextField editor = inlineEditor;
        if (editor != null && (x < editor.getAbsoluteX() || x > editor.getAbsoluteX() + editor.getWidth()
                || y < editor.getAbsoluteY() || y > editor.getAbsoluteY() + editor.getHeight())) {
            finishInlineEditor();
        }
        Element hit = null;
        if (inlineEditor == null && canvasHost != null
                && x >= canvasHost.getAbsoluteX() && x <= canvasHost.getAbsoluteX() + canvasHost.getWidth()
                && y >= canvasHost.getAbsoluteY() && y <= canvasHost.getAbsoluteY() + canvasHost.getHeight()) {
            hit = elementAt(canvasHost, x, y);
        }
        boolean duplicateComponentPress = designerDragArmed && hit == designerDraggedElement
                && Math.abs(x - designerPressX) + Math.abs(y - designerPressY) <= 2;
        boolean duplicateResizePress = guidedResizeArmed && hit == guidedResizeElement
                && Math.abs(x - guidedResizePressX) + Math.abs(y - guidedResizePressY) <= 2;
        if (duplicateComponentPress || duplicateResizePress) return;
        if (designerDragArmed || guidedResizeArmed) cancelDesignerDrag();
        if (hit != null && document != null && hit != document.root()
                && (additive || !selectedElements.contains(hit)
                        || (selectedElements.size() > 1 && hit != document.selected()))) {
            selectElement(hit, additive, "canvas");
        }
        // Resize hit-testing must use the component under this press, never the previously
        // selected component. Otherwise an ordinary click on an overlapping neighbor is stolen.
        if (!additive && hit != null && document != null && selectedElements.contains(hit)
                && beginGuidedResize(x, y)) return;
        if (hit != null && document != null && hit != document.root() && selectedElements.contains(hit)) {
            armDesignerDrag(hit, null, componentForElement(canvasHost, hit), x, y, null);
        }
    }

    private static boolean additiveSelection(ActionEvent event) {
        PointerEvent pointer = event == null ? null : event.getPointerEvent();
        return pointer != null && (pointer.isShiftDown() || pointer.isControlDown() || pointer.isMetaDown());
    }

    private void showUnboundState() {
        Form form = new Form("Codename One GUI Builder", new BorderLayout());
        Container empty = new Container(BoxLayout.y());
        empty.setUIID("BuilderWelcome");
        empty.add(new Label("GUI Builder", "BuilderWelcomeTitle"));
        empty.add(new SpanLabel("Open the builder from a Codename One Maven project with mvn cn1:guibuilder.", "BuilderWelcomeCopy"));
        form.add(BorderLayout.CENTER, BorderLayout.centerAbsolute(empty));
        form.show();
    }

    private Component buildWorkspace() {
        formsPanel = new Container(BoxLayout.y());
        formsPanel.setUIID("BuilderSidebar");
        formsPanel.setScrollableY(true);
        formsPanel.getSemantics().setIdentifier("guibuilder.forms").setLabel("Project forms")
                .setRole(AccessibilityRole.LIST).setGrouping(AccessibilityGrouping.GROUP);
        hierarchyPanel = new Container(BoxLayout.y());
        hierarchyPanel.setUIID("BuilderSidebar");
        hierarchyPanel.setScrollableY(true);
        hierarchyPanel.getSemantics().setIdentifier("guibuilder.hierarchy").setLabel("Component hierarchy")
                .setRole(AccessibilityRole.TREE).setGrouping(AccessibilityGrouping.GROUP);
        canvasHost = new Container(new BorderLayout());
        canvasHost.setUIID("BuilderCanvasArea");
        canvasHost.getSemantics().setIdentifier("guibuilder.canvas").setLabel("Design canvas")
                .setGrouping(AccessibilityGrouping.GROUP);
        canvasOverlayHost = new Container(new LayeredLayout());
        canvasOverlayHost.setUIID("BuilderCanvasArea");
        canvasOverlayHost.add(canvasHost);
        ((LayeredLayout) canvasOverlayHost.getLayout()).setInsets(canvasHost, "0 0 0 0");
        inspectorHost = new Container(new BorderLayout());
        inspectorHost.setUIID("BuilderInspector");
        inspectorHost.getSemantics().setIdentifier("guibuilder.inspector").setLabel("Inspector")
                .setGrouping(AccessibilityGrouping.GROUP);

        Container left = new Container(new BorderLayout());
        left.setUIID("BuilderSidebar");
        left.add(BorderLayout.NORTH, sectionTitle("PROJECT FORMS"));
        Container leftBody = new Container(new GridLayout(2, 1));
        leftBody.setUIID("BuilderSidebar");
        Tabs projectTabs = new Tabs();
        projectTabs.getSemantics().setIdentifier("guibuilder.projectTabs").setLabel("Project navigation");
        projectTabs.setSwipeActivated(false);
        projectTabs.addTab("Forms", formsPanel);
        projectTabs.addTab("Hierarchy", hierarchyPanel);
        leftBody.add(projectTabs);
        leftBody.add(buildPalette());
        left.add(BorderLayout.CENTER, leftBody);
        refreshForms();

        Container center = new Container(new BorderLayout());
        center.setUIID("BuilderCanvasArea");
        center.setScrollableX(true);
        center.add(BorderLayout.NORTH, buildCanvasToolbar());
        center.add(BorderLayout.CENTER, canvasOverlayHost);

        Container right = new Container(new BorderLayout());
        right.setUIID("BuilderInspector");
        right.add(BorderLayout.NORTH, sectionTitle("INSPECTOR"));
        right.add(BorderLayout.CENTER, inspectorHost);

        SplitPane centerAndInspector = new SplitPane(SplitPane.HORIZONTAL_SPLIT, center, right,
                "45%", "72%", "90%");
        SplitPane workspaceSplit = new SplitPane(SplitPane.HORIZONTAL_SPLIT, left, centerAndInspector,
                "10%", "20%", "40%");
        return workspaceSplit;
    }

    private void installDragGuideOverlay() {
        dragGuideOverlay = new DragGuideOverlay();
        canvasOverlayHost.add(dragGuideOverlay);
        ((LayeredLayout) canvasOverlayHost.getLayout()).setInsets(dragGuideOverlay, "0 0 0 0");
        selectionActions = buildSelectionActions();
        selectionActions.setVisible(false);
        canvasOverlayHost.add(selectionActions);
        ((LayeredLayout) canvasOverlayHost.getLayout()).setInsets(selectionActions, "0 auto auto 0");
        canvasOverlayHost.revalidate();
    }

    private Container buildSelectionActions() {
        Container palette = new Container(new GridLayout(1, 9));
        palette.setUIID("BuilderSelectionActions");
        palette.getSemantics().setIdentifier("guibuilder.selectionActions")
                .setLabel("Selected component layout actions").setGrouping(AccessibilityGrouping.GROUP);
        palette.add(selectionAction(FontImage.MATERIAL_ALIGN_HORIZONTAL_LEFT,
                "Align left edges to the filled-handle reference component", "alignLeft"));
        palette.add(selectionAction(FontImage.MATERIAL_ALIGN_HORIZONTAL_CENTER,
                "Align horizontal centers to the filled-handle reference component", "alignHCenter"));
        palette.add(selectionAction(FontImage.MATERIAL_ALIGN_HORIZONTAL_RIGHT,
                "Align right edges to the filled-handle reference component", "alignRight"));
        palette.add(selectionAction(FontImage.MATERIAL_ALIGN_VERTICAL_TOP,
                "Align top edges to the filled-handle reference component", "alignTop"));
        palette.add(selectionAction(FontImage.MATERIAL_TEXT_FIELDS,
                "Align text baselines to the filled-handle reference component", "alignBaseline"));
        palette.add(selectionAction(FontImage.MATERIAL_ALIGN_VERTICAL_BOTTOM,
                "Align bottom edges to the filled-handle reference component", "alignBottom"));
        palette.add(selectionAction(FontImage.MATERIAL_WIDTH_FULL,
                "Make every selected component as wide as the filled-handle reference component", "matchWidth"));
        palette.add(selectionAction(FontImage.MATERIAL_HEIGHT,
                "Make every selected component as tall as the filled-handle reference component", "matchHeight"));
        palette.add(selectionAction(FontImage.MATERIAL_LINK_OFF,
                "Disconnect selected components from alignment and size relationships", "disconnect"));
        return palette;
    }

    private Button selectionAction(char icon, String tooltip, String action) {
        Button button = new Button("");
        button.setUIID("BuilderSelectionAction");
        button.setTooltip(tooltip);
        button.setCursor(Component.HAND_CURSOR);
        button.setAlignment(Component.CENTER);
        button.getAllStyles().setAlignment(Component.CENTER);
        FontImage.setMaterialIcon(button, icon, 3.2f);
        button.getSemantics().setIdentifier("guibuilder.selectionAction." + action).setLabel(tooltip);
        button.addActionListener(e -> applySelectionAction(action));
        return button;
    }

    private void installToolbar(Toolbar toolbar) {
        toolbar.addMaterialCommandToLeftBar("Save", FontImage.MATERIAL_SAVE, e -> save());
        toolbar.addMaterialCommandToLeftBar("Undo", FontImage.MATERIAL_UNDO, e -> undo());
        toolbar.addMaterialCommandToLeftBar("Redo", FontImage.MATERIAL_REDO, e -> redo());
        toolbar.addMaterialCommandToLeftBar("Refresh", FontImage.MATERIAL_REFRESH, e -> refreshProject());
        toolbar.addMaterialCommandToRightBar("CSS", FontImage.MATERIAL_COLOR_LENS, e -> openCss());
        toolbar.addMaterialCommandToRightBar("Code", FontImage.MATERIAL_CODE, e -> openCompanionSource());
    }

    private Component buildCanvasToolbar() {
        Container bar = new Container(new FlowLayout(Component.CENTER));
        bar.setUIID("BuilderCanvasToolbar");
        bar.add(iconButton(FontImage.MATERIAL_STAY_CURRENT_PORTRAIT, "Phone portrait", () -> setCanvasMode("phonePortrait")));
        bar.add(iconButton(FontImage.MATERIAL_STAY_CURRENT_LANDSCAPE, "Phone landscape", () -> setCanvasMode("phoneLandscape")));
        bar.add(iconButton(FontImage.MATERIAL_TABLET, "Tablet portrait", () -> setCanvasMode("tabletPortrait")));
        bar.add(iconButton(FontImage.MATERIAL_DESKTOP_MAC, "Desktop — full canvas", () -> setCanvasMode("desktop")));
        return bar;
    }

    private Component buildPalette() {
        Container palette = new Container(new BorderLayout());
        palette.setUIID("BuilderPalette");
        palette.getSemantics().setIdentifier("guibuilder.palette").setLabel("Component palette")
                .setRole(AccessibilityRole.LIST).setGrouping(AccessibilityGrouping.GROUP);
        TextField search = new TextField("", "Search components");
        search.setUIID("BuilderSearch");
        search.getSemantics().setIdentifier("guibuilder.palette.search").setLabel("Search components")
                .setRole(AccessibilityRole.SEARCH_FIELD);
        search.addDataChangedListener((type, index) -> {
            paletteFilter = search.getText().toLowerCase();
            rebuildPalette(palette);
        });
        palette.putClientProperty("search", search);
        rebuildPalette(palette);
        return palette;
    }

    private void rebuildPalette(Container palette) {
        TextField search = (TextField) palette.getClientProperty("search");
        palette.removeAll();
        Container heading = new Container(BoxLayout.y());
        heading.add(sectionTitle("COMPONENTS"));
        heading.add(search);
        palette.add(BorderLayout.NORTH, heading);
        Container items = new Container(new GridLayout(5, 2));
        items.setScrollableY(true);
        String[] types = {"Button", "Label", "SpanLabel", "TextField", "TextArea", "CheckBox", "RadioButton", "Slider", "Container", "Tabs"};
        char[] icons = {FontImage.MATERIAL_SMART_BUTTON, FontImage.MATERIAL_TEXT_FIELDS, FontImage.MATERIAL_SUBJECT,
                FontImage.MATERIAL_EDIT, FontImage.MATERIAL_NOTES, FontImage.MATERIAL_CHECK_BOX,
                FontImage.MATERIAL_RADIO_BUTTON_CHECKED, FontImage.MATERIAL_TUNE, FontImage.MATERIAL_VIEW_AGENDA,
                FontImage.MATERIAL_TAB};
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            if (paletteFilter.length() > 0 && !type.toLowerCase().contains(paletteFilter)) continue;
            Button item = new Button(type, material(icons[i], "BuilderPaletteIcon"));
            item.setUIID("BuilderPaletteItem");
            item.getSemantics().setIdentifier("guibuilder.palette." + type.toLowerCase())
                    .setLabel("Add " + type).setHint("Activate to add, or drag onto the design canvas");
            final boolean[] suppressAction = new boolean[1];
            item.addActionListener(e -> {
                if (suppressAction[0]) {
                    suppressAction[0] = false;
                    return;
                }
                addComponent(type);
            });
            item.addPointerPressedListener(e -> armDesignerDrag(null, type, item, e.getX(), e.getY(),
                    () -> suppressAction[0] = true));
            items.add(item);
        }
        palette.add(BorderLayout.CENTER, items);
        palette.revalidate();
    }

    private void refreshForms() {
        formsPanel.removeAll();
        for (String path : guiFiles) {
            String relative = relativeFormName(path);
            String simple = relative.substring(relative.lastIndexOf('.') + 1);
            Button form = new Button(simple, material(FontImage.MATERIAL_INSERT_DRIVE_FILE, "BuilderFormIcon"));
            form.setTooltip(relative);
            form.getSemantics().setIdentifier("guibuilder.form." + relative).setLabel(simple)
                    .setDescription(relative).setRole(AccessibilityRole.LIST_ITEM)
                    .setSelected(Boolean.valueOf(document != null && path.equals(document.path())));
            form.setUIID(document != null && path.equals(document.path()) ? "BuilderFormItemSelected" : "BuilderFormItem");
            form.addActionListener(e -> switchForm(path));
            formsPanel.add(form);
        }
        formsPanel.revalidate();
    }

    private void switchForm(String path) {
        cancelDesignerDrag();
        // Opening another form tears the editor pane down, and its buffer is the only copy of an
        // unsaved edit. The document itself can be perfectly clean while the source, CSS or model
        // pane is not, so this has to be asked separately.
        if (editorBufferIsDirty()
                && !com.codename1.ui.Dialog.show("Unsaved changes",
                        "The open editor has changes you have not saved. Opening another form"
                        + " discards them.", "Discard", "Keep editing")) {
            setStatus("Kept your unsaved editor changes");
            return;
        }
        if (document != null && document.isModified()) {
            if (com.codename1.ui.Dialog.show("Unsaved changes", "Save changes before switching forms?", "Save", "Discard")) {
                // Staying on a form whose save failed is the only way to keep the work: opening the
                // next form replaces the document and the edits are gone.
                if (!save()) {
                    setStatus("Still editing " + relativeFormName(document.path()) + "; the save failed");
                    return;
                }
            }
        }
        openForm(path);
    }

    /**
     * @return true when the form was parsed and is now the document being edited. Callers that
     *     report success to a client have to know: a malformed file left the previous form on
     *     screen while the caller announced the new one.
     */
    private boolean openForm(String path) {
        try {
            cancelDesignerDrag();
            document = GuiDocument.parse(path, ProjectIO.read(path));
            activeEditorReopen = null;
            selectedElements.clear();
            recordAction("form_opened", "form", relativeFormName(path));
            loadProjectTheme();
            refreshForms();
            refreshEditor();
            setStatus("Editing " + relativeFormName(path));
            return true;
        } catch (Exception ex) {
            ToastBar.showErrorMessage("Unable to open GUI file: " + ex.getMessage());
            return false;
        }
    }

    private void refreshEditor() {
        if (document == null) return;
        TooltipManager.hideTooltip();
        normalizeSelection();
        cancelDesignerDrag();
        activeCodeEditor = null;
        finishInlineEditor();
        canvasHost.removeAll();
        boolean desktop = "desktop".equals(canvasMode);
        Container stage = new Container(desktop ? new BorderLayout() : new FlowLayout(Component.CENTER));
        stage.setUIID("BuilderStage");
        final int surfaceWidth = desktop ? -1 : "phoneLandscape".equals(canvasMode) ? 1200
                : "tabletPortrait".equals(canvasMode) ? 960 : 720;
        final int surfaceHeight = desktop ? -1 : "phoneLandscape".equals(canvasMode) ? 720
                : "tabletPortrait".equals(canvasMode) ? 1280 : 1200;
        Container device = new Container(new BorderLayout()) {
            @Override protected Dimension calcPreferredSize() {
                Dimension natural = super.calcPreferredSize();
                if (surfaceWidth > 0) natural.setWidth(surfaceWidth);
                if (surfaceHeight > 0) natural.setHeight(surfaceHeight);
                return natural;
            }
        };
        device.setUIID(desktop ? "BuilderDesktopSurface" : "BuilderDevice");
        formToolbarPreview = buildFormToolbarPreview();
        device.add(BorderLayout.NORTH, formToolbarPreview);
        Component preview = ComponentPreviewFactory.create(document.root(), document.selected(), new ComponentPreviewFactory.SelectionHandler() {
            @Override public void selected(Element element) {
                selectElement(element, false, "preview");
            }
            @Override public void selected(Element element, boolean additive) {
                selectElement(element, additive, "preview");
            }
            @Override public void dragPressed(Element element, Component source, int x, int y) {
                if (!selectedElements.contains(element)) return;
                if (selectedElements.size() <= 1 && !beginGuidedResize(x, y)) {
                    armDesignerDrag(element, null, source, x, y, null);
                } else if (selectedElements.size() > 1) {
                    armDesignerDrag(element, null, source, x, y, null);
                }
            }
            @Override public boolean isDragActive() { return designerDragActive; }
            @Override public void dragMoved(int x, int y) {
                if (guidedResizeArmed) updateGuidedResize(x, y); else updateDesignerDrag(x, y);
            }
            @Override public void dragReleased(int x, int y) {
                if (guidedResizeArmed) finishGuidedResize(x, y); else finishDesignerDrag(x, y);
            }
            @Override public void editContent(Element element) {
                selectElement(element, false, "inline-edit");
                editSelectedContent();
            }
        });
        previewRoot = preview;
        // The whole device surface resolves its look and feel from the canvas UIManager, so the
        // project CSS styles the preview and the builder's own theme never leaks into it.
        deviceSurface = device;
        device.setUIManager(previewUIManager);
        if (preview instanceof Container) ((Container) preview).setUIManager(previewUIManager);
        refreshProjectThemeOnPreview();
        device.add(BorderLayout.CENTER, preview);
        if (desktop) stage.add(BorderLayout.CENTER, device); else stage.add(device);
        canvasHost.add(BorderLayout.CENTER, stage);
        refreshInspector();
        refreshHierarchy();
        canvasHost.revalidate();
        Display.getInstance().callSerially(this::refreshGuidedSelectionOverlay);
        // Rebuilding the canvas replaces everything inside it, including the split pane an open
        // editor lives in -- so any drop, delete or undo silently closed the editor mid-edit.
        // Put it back. The guard stops the reopen from recursing through this method.
        Runnable reopen = activeEditorReopen;
        if (reopen != null && !reopeningEditor) {
            reopeningEditor = true;
            try {
                reopen.run();
            } finally {
                reopeningEditor = false;
            }
        }
    }

    /**
     * Starts mirroring an editor's text so a canvas rebuild can put the buffer back and Close can
     * tell whether there is anything to lose.
     *
     * @param editor the editor being shown
     * @param content the text it was opened with
     */
    private void trackEditorBuffer(final CodeEditor editor, String content) {
        editorBuffer = content;
        // Only the reopen path carries a buffer forward; a fresh open agrees with disk by
        // definition, and after a save the caller resets this to the text it wrote.
        if (!reopeningEditor) editorBufferOnDisk = content;
        editor.addChangeListener(e -> editor.getText(text -> editorBuffer = text));
    }

    /** True when the open editor holds text that is not on disk. */
    private boolean editorBufferIsDirty() {
        return editorBuffer != null && !editorBuffer.equals(editorBufferOnDisk);
    }

    /**
     * Closes an open editor pane for good, rather than letting the next refresh restore it. The
     * buffer is the only copy of an unsaved edit -- reopening reads the file -- so closing without
     * asking discarded the user's work outright.
     */
    private void closeEditorPane() {
        if (editorBufferIsDirty()
                && !com.codename1.ui.Dialog.show("Unsaved changes",
                        "Closing this editor discards the changes you have not saved.",
                        "Discard", "Keep editing")) {
            return;
        }
        activeEditorReopen = null;
        editorBuffer = null;
        editorBufferOnDisk = null;
        refreshEditor();
    }

    private Component buildFormToolbarPreview() {
        Container bar = new Container(new BorderLayout());
        bar.setUIID("BuilderFormToolbar");
        formTitlePreview = new Label(document.root().getAttribute("title") == null ? "Untitled Form" : document.root().getAttribute("title"), "Title");
        formTitlePreview.getSemantics().setIdentifier("guibuilder.preview.formTitle")
                .setLabel("Form title").setHint("Double click to edit the form title");
        formTitlePreview.addLongPressListener(e -> { document.select(document.root()); editSelectedContent(); });
        formTitlePreview.addPointerReleasedListener(e -> {
            long now = System.currentTimeMillis();
            Object previous = formTitlePreview.getClientProperty("gui.lastClick");
            formTitlePreview.putClientProperty("gui.lastClick", Long.valueOf(now));
            if (previous instanceof Long && now - ((Long) previous).longValue() < 450) {
                document.select(document.root());
                editSelectedContent();
            }
        });
        bar.add(BorderLayout.CENTER, formTitlePreview);
        Container left = new Container(BoxLayout.x());
        Container right = new Container(BoxLayout.x());
        for (Element command : document.commands()) {
            Button button = new Button(value(command, "name", "Command"));
            button.setUIID("TitleCommand");
            // The command editor offers overflow and side placements and the generated source
            // honours them. Falling through to the right bar here showed a toolbar the running
            // application would never produce, so placement could not be checked before saving.
            String placement = value(command, "placement", "right");
            if ("side".equals(placement)) {
                // A side command reaches the user through the side menu and an overflow command
                // through the overflow menu; neither sits on the bar as its own button. Showing
                // where it actually lives beats showing it in the wrong corner.
                button.setText("[side] " + button.getText());
            } else if ("overflow".equals(placement)) {
                button.setText("[overflow] " + button.getText());
            }
            if ("left".equals(placement) || "side".equals(placement)) left.add(button); else right.add(button);
        }
        if (left.getComponentCount() > 0) bar.add(BorderLayout.WEST, left);
        if (right.getComponentCount() > 0) bar.add(BorderLayout.EAST, right);
        return bar;
    }

    private void refreshHierarchy() {
        hierarchyPanel.removeAll();
        if (document != null) addHierarchyRow(document.root(), 0);
        hierarchyPanel.revalidate();
    }

    private void addHierarchyRow(Element element, int depth) {
        String type = element.getAttribute("type");
        String name = element.getAttribute("name");
        Button row = new Button((name == null ? type : name) + "  ·  " + type,
                material(GuiDocument.acceptsChildren(element) ? FontImage.MATERIAL_FOLDER_OPEN : FontImage.MATERIAL_DRAG_HANDLE,
                        "BuilderFormIcon"));
        row.setUIID(selectedElements.contains(element) ? "BuilderHierarchySelected" : "BuilderHierarchyItem");
        row.getSemantics().setIdentifier("guibuilder.hierarchy." + value(element, "name", type))
                .setLabel((name == null ? type : name) + ", " + type)
                .setRole(AccessibilityRole.TREE_ITEM)
                .setSelected(Boolean.valueOf(selectedElements.contains(element)));
        row.getAllStyles().setPaddingLeft(depth * 3 + 1);
        final boolean[] suppressAction = new boolean[1];
        row.addActionListener(e -> {
            if (suppressAction[0]) {
                suppressAction[0] = false;
                return;
            }
            selectElement(element, false, "hierarchy");
            refreshEditor();
        });
        if (element != document.root()) {
            row.addPointerPressedListener(e -> armHierarchyDrag(element,
                    componentForElement(canvasHost, element), e.getX(), e.getY(), () -> suppressAction[0] = true));
        }
        row.putClientProperty("gui.element", element);
        hierarchyPanel.add(row);
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) {
                addHierarchyRow(((Element) child), depth + 1);
            }
        }
    }

    private boolean dragThresholdReached(int startX, int startY, int x, int y) {
        return Math.abs(x - startX) + Math.abs(y - startY) >= 6;
    }

    private void armDesignerDrag(Element element, String paletteType, Component source, int x, int y,
            Runnable suppressAction) {
        if (document == null || (element != null && !document.containsElement(element))) {
            cancelDesignerDrag();
            return;
        }
        designerDraggedElement = element;
        designerDragDocument = document;
        designerPaletteType = paletteType;
        designerDragSource = source;
        designerSuppressAction = suppressAction;
        designerPressX = x;
        designerPressY = y;
        designerGrabOffsetX = element == null || source == null ? 0 : Math.max(0, x - source.getAbsoluteX());
        designerGrabOffsetY = element == null || source == null ? 0 : Math.max(0, y - source.getAbsoluteY());
        designerDragArmed = true;
        designerDragActive = false;
        lastDragJournalSignature = null;
        recordAction("drag_armed", "component", element == null ? null : value(element, "name", "component"),
                "paletteType", paletteType, "x", Integer.valueOf(x), "y", Integer.valueOf(y));
    }

    private void armHierarchyDrag(Element element, Component preview, int x, int y, Runnable suppressAction) {
        armDesignerDrag(element, null, preview, x, y, suppressAction);
        // The press occurs in the tree row, not inside the preview component.  Using the tree's
        // coordinates as a grab offset can place the guide hundreds of pixels away.
        designerGrabOffsetX = 0;
        designerGrabOffsetY = 0;
    }

    private boolean beginGuidedResize(int x, int y) {
        ResizeHit hit = resizeHitAt(x, y);
        if (hit == null) return false;
        Element element = hit.element;
        Component source = hit.component;
        int edges = hit.edges;
        if (element != document.selected()) {
            document.select(element);
            refreshGuidedSelectionOverlay();
        }
        cancelDesignerDrag();
        guidedResizeElement = element;
        guidedResizeSource = source;
        guidedResizeEdges = edges;
        guidedResizePressX = x;
        guidedResizePressY = y;
        guidedResizeStartX = source.getAbsoluteX();
        guidedResizeStartY = source.getAbsoluteY();
        guidedResizeStartW = source.getWidth();
        guidedResizeStartH = source.getHeight();
        guidedResizeArmed = true;
        guidedResizeActive = false;
        return true;
    }

    private ResizeHit resizeHitAt(int x, int y) {
        if (document == null || canvasHost == null || selectedElements.isEmpty()) return null;
        int slop = Math.max(10, Display.getInstance().convertToPixels(1.6f));
        Element primary = document.selected();
        ResizeHit primaryHit = resizeHit(primary, x, y, slop);
        if (primaryHit != null) return primaryHit;
        for (Element element : selectedElements) {
            if (element == primary) continue;
            ResizeHit hit = resizeHit(element, x, y, slop);
            if (hit != null) return hit;
        }
        return null;
    }

    private ResizeHit resizeHit(Element element, int x, int y, int slop) {
        if (element == null || element == document.root() || !selectedElements.contains(element)
                || !"LayeredLayout".equals(document.parentLayout(element))) return null;
        Component source = componentForElement(canvasHost, element);
        if (source == null || source.getWidth() < 1 || source.getHeight() < 1) return null;
        int leftDistance = Math.abs(x - source.getAbsoluteX());
        int rightDistance = Math.abs(x - source.getAbsoluteX() - source.getWidth());
        int topDistance = Math.abs(y - source.getAbsoluteY());
        int bottomDistance = Math.abs(y - source.getAbsoluteY() - source.getHeight());
        boolean withinX = x >= source.getAbsoluteX() - slop && x <= source.getAbsoluteX() + source.getWidth() + slop;
        boolean withinY = y >= source.getAbsoluteY() - slop && y <= source.getAbsoluteY() + source.getHeight() + slop;
        int edges = 0;
        if (withinY && leftDistance <= slop && leftDistance + 1 < rightDistance) edges |= 1;
        else if (withinY && rightDistance <= slop && rightDistance + 1 < leftDistance) edges |= 2;
        if (withinX && topDistance <= slop && topDistance + 1 < bottomDistance) edges |= 4;
        else if (withinX && bottomDistance <= slop && bottomDistance + 1 < topDistance) edges |= 8;
        return edges == 0 ? null : new ResizeHit(element, source, edges);
    }

    private void updateDesignerHoverCursor(int x, int y) {
        if (workspace == null) return;
        Component hovered = workspace.getComponentAt(x, y);
        int cursor = designerResizeCursorAt(x, y);
        if (designerCursorComponent != null && designerCursorComponent != hovered) {
            designerCursorComponent.setCursor(Component.DEFAULT_CURSOR);
        }
        if (hovered != null) hovered.setCursor(cursor);
        designerCursorComponent = cursor == Component.DEFAULT_CURSOR ? null : hovered;
    }

    int designerResizeCursorAt(int x, int y) {
        ResizeHit hit = resizeHitAt(x, y);
        return hit == null ? Component.DEFAULT_CURSOR : resizeCursor(hit.edges);
    }

    private int resizeCursor(int edges) {
        if ((edges & 4) != 0 && (edges & 1) != 0) return Component.NW_RESIZE_CURSOR;
        if ((edges & 4) != 0 && (edges & 2) != 0) return Component.NE_RESIZE_CURSOR;
        if ((edges & 8) != 0 && (edges & 1) != 0) return Component.SW_RESIZE_CURSOR;
        if ((edges & 8) != 0 && (edges & 2) != 0) return Component.SE_RESIZE_CURSOR;
        if ((edges & 1) != 0) return Component.W_RESIZE_CURSOR;
        if ((edges & 2) != 0) return Component.E_RESIZE_CURSOR;
        if ((edges & 4) != 0) return Component.N_RESIZE_CURSOR;
        if ((edges & 8) != 0) return Component.S_RESIZE_CURSOR;
        return Component.DEFAULT_CURSOR;
    }

    private void updateGuidedResize(int x, int y) {
        if (!guidedResizeArmed) return;
        if (!guidedResizeActive && !dragThresholdReached(guidedResizePressX, guidedResizePressY, x, y)) return;
        guidedResizeActive = true;
        activeResizePlan = planGuidedResize(x, y);
        if (activeResizePlan != null && dragGuideOverlay != null) {
            dragGuideOverlay.showResize((Container) guidedResizeSource.getParent(), guidedResizeSource,
                    activeResizePlan.x, activeResizePlan.y, activeResizePlan.width, activeResizePlan.height,
                    activeResizePlan.description);
            showGuidedResizeSimulation(guidedResizeElement, (Container) guidedResizeSource.getParent(),
                    guidedResizeSource, activeResizePlan, guidedResizeEdges);
            setStatus(activeResizePlan.description);
        }
    }

    private ResizePlan planGuidedResize(int pointerX, int pointerY) {
        if (guidedResizeSource == null || guidedResizeSource.getParent() == null) return null;
        Container parent = guidedResizeSource.getParent();
        int dx = pointerX - guidedResizePressX;
        int dy = pointerY - guidedResizePressY;
        int left = guidedResizeStartX;
        int right = guidedResizeStartX + guidedResizeStartW;
        int top = guidedResizeStartY;
        int bottom = guidedResizeStartY + guidedResizeStartH;
        if ((guidedResizeEdges & 1) != 0) left += dx;
        if ((guidedResizeEdges & 2) != 0) right += dx;
        if ((guidedResizeEdges & 4) != 0) top += dy;
        if ((guidedResizeEdges & 8) != 0) bottom += dy;
        int minWidth = Math.max(16, Display.getInstance().convertToPixels(2));
        int minHeight = Math.max(16, Display.getInstance().convertToPixels(2));
        if (right - left < minWidth) {
            if ((guidedResizeEdges & 1) != 0) left = right - minWidth; else right = left + minWidth;
        }
        if (bottom - top < minHeight) {
            if ((guidedResizeEdges & 4) != 0) top = bottom - minHeight; else bottom = top + minHeight;
        }
        ResizePlan plan = new ResizePlan(left, top, right - left, bottom - top);
        int threshold = Math.max(8, Display.getInstance().convertToPixels(1.5f));
        int bestHorizontal = threshold + 1;
        int bestVertical = threshold + 1;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component sibling = parent.getComponentAt(i);
            if (sibling == guidedResizeSource || sibling.getClientProperty("gui.element") == null) continue;
            Element siblingElement = (Element) sibling.getClientProperty("gui.element");
            if ((guidedResizeEdges & 2) != 0) {
                int distance = Math.abs((plan.x + plan.width) - (plan.x + sibling.getWidth()));
                if (distance < bestHorizontal) {
                    plan.width = sibling.getWidth(); plan.matchWidth = siblingElement; bestHorizontal = distance;
                    plan.horizontalDescription = "same width as " + value(siblingElement, "name", "component");
                }
            } else if ((guidedResizeEdges & 1) != 0) {
                int candidate = plan.x + plan.width - sibling.getWidth();
                int distance = Math.abs(plan.x - candidate);
                if (distance < bestHorizontal) {
                    plan.x = candidate; plan.width = sibling.getWidth(); plan.matchWidth = siblingElement; bestHorizontal = distance;
                    plan.horizontalDescription = "same width as " + value(siblingElement, "name", "component");
                }
            }
            if ((guidedResizeEdges & 8) != 0) {
                int distance = Math.abs((plan.y + plan.height) - (plan.y + sibling.getHeight()));
                if (distance < bestVertical) {
                    plan.height = sibling.getHeight(); plan.matchHeight = siblingElement; bestVertical = distance;
                    plan.verticalDescription = "same height as " + value(siblingElement, "name", "component");
                }
            } else if ((guidedResizeEdges & 4) != 0) {
                int candidate = plan.y + plan.height - sibling.getHeight();
                int distance = Math.abs(plan.y - candidate);
                if (distance < bestVertical) {
                    plan.y = candidate; plan.height = sibling.getHeight(); plan.matchHeight = siblingElement; bestVertical = distance;
                    plan.verticalDescription = "same height as " + value(siblingElement, "name", "component");
                }
            }
        }
        int parentLeft = contentX(parent);
        int parentTop = contentY(parent);
        int parentRight = parentLeft + contentWidth(parent);
        int parentBottom = parentTop + contentHeight(parent);
        if ((guidedResizeEdges & 1) != 0 && Math.abs(plan.x - parentLeft) <= threshold) {
            plan.width += plan.x - parentLeft; plan.x = parentLeft; plan.matchWidth = null; plan.horizontalDescription = "dock left";
        }
        if ((guidedResizeEdges & 2) != 0 && Math.abs(plan.x + plan.width - parentRight) <= threshold) {
            plan.width = parentRight - plan.x; plan.matchWidth = null; plan.horizontalDescription = "dock right";
        }
        if ((guidedResizeEdges & 4) != 0 && Math.abs(plan.y - parentTop) <= threshold) {
            plan.height += plan.y - parentTop; plan.y = parentTop; plan.matchHeight = null; plan.verticalDescription = "dock top";
        }
        if ((guidedResizeEdges & 8) != 0 && Math.abs(plan.y + plan.height - parentBottom) <= threshold) {
            plan.height = parentBottom - plan.y; plan.matchHeight = null; plan.verticalDescription = "dock bottom";
        }
        plan.description = plan.horizontalDescription == null ? plan.verticalDescription
                : plan.verticalDescription == null ? plan.horizontalDescription
                : plan.horizontalDescription + " • " + plan.verticalDescription;
        if (plan.description == null) plan.description = plan.width + " × " + plan.height;
        return plan;
    }

    private void finishGuidedResize(int x, int y) {
        if (!guidedResizeArmed) return;
        boolean active = guidedResizeActive;
        ResizePlan plan = active ? planGuidedResize(x, y) : null;
        Element element = guidedResizeElement;
        Component source = guidedResizeSource;
        int edges = guidedResizeEdges;
        guidedResizeArmed = guidedResizeActive = false;
        guidedResizeElement = null;
        guidedResizeSource = null;
        activeResizePlan = null;
        if (!active || plan == null || element == null || !document.containsElement(element)
                || source == null || source.getParent() == null) {
            refreshGuidedSelectionOverlay();
            return;
        }
        Container parent = source.getParent();
        commitGuidedSelectionResize(element, parent, plan, edges);
        refreshEditor();
        setStatus(selectedElements.size() > 1
                ? "Resized " + selectedElements.size() + " components using reference "
                        + value(element, "name", "component") + " at " + plan.width + " × " + plan.height
                : "Resized " + value(element, "name", "component") + " to " + plan.width + " × " + plan.height);
    }

    void commitGuidedResize(Element element, Container parent, ResizePlan plan, int edges) {
        Component preview = componentForElement(canvasHost, element);
        commitGuidedResize(document, element, parent, preview, plan, edges);
    }

    void commitGuidedSelectionResize(Element element, Container parent, ResizePlan plan, int edges) {
        boolean group = selectedElements.size() > 1 && selectedElements.contains(element)
                && selectedElementsShareGuidedParent();
        ResizePlan referencePlan = group ? fixedReferenceResizePlan(plan, edges) : plan;
        document.beginTransaction();
        try {
            commitGuidedResize(document, element, parent, componentForElement(canvasHost, element), referencePlan, edges);
            if (group) {
                for (Element selected : new ArrayList<Element>(selectedElements)) {
                    if (selected == element) continue;
                    Component preview = componentForElement(canvasHost, selected);
                    if ((edges & 3) != 0) {
                        applyLayeredRelationship(document, selected, element, "matchWidth", preview);
                    }
                    if ((edges & 12) != 0) {
                        applyLayeredRelationship(document, selected, element, "matchHeight", preview);
                    }
                }
            }
            document.select(element);
        } finally {
            document.endTransaction();
        }
    }

    private ResizePlan fixedReferenceResizePlan(ResizePlan plan, int edges) {
        ResizePlan fixed = new ResizePlan(plan.x, plan.y, plan.width, plan.height);
        fixed.horizontalDescription = plan.horizontalDescription;
        fixed.verticalDescription = plan.verticalDescription;
        fixed.description = plan.description;
        fixed.matchWidth = (edges & 3) == 0 ? plan.matchWidth : null;
        fixed.matchHeight = (edges & 12) == 0 ? plan.matchHeight : null;
        return fixed;
    }

    private void commitGuidedResize(GuiDocument targetDocument, Element element, Container parent,
            Component preview, ResizePlan plan, int edges) {
        targetDocument.select(element);
        int marginLeft = preview == null ? 0 : preview.getStyle().getMarginLeftNoRTL();
        int marginTop = preview == null ? 0 : preview.getStyle().getMarginTop();
        String[] insets = GuidedLayoutSupport.insetValues(element);
        String[] refs = GuidedLayoutSupport.referenceNames(element);
        String[] positions = GuidedLayoutSupport.referencePositions(element);
        targetDocument.beginTransaction();
        try {
            if ((edges & 3) != 0) {
                String explicitMatch = value(element, "guidedMatchWidth", "");
                boolean keepExplicitMatch = GuidedLayoutSupport.MATCH.equals(GuidedLayoutSupport.horizontalPolicy(element))
                        && plan.matchWidth != null && explicitMatch.equals(value(plan.matchWidth, "name", ""));
                if (keepExplicitMatch) {
                    applyMatchedWidth(plan.matchWidth, insets, refs, positions, plan.x, plan.width);
                    targetDocument.setAttribute("guidedMatchWidth", explicitMatch);
                    targetDocument.setAttribute("guidedHorizontalSize", GuidedLayoutSupport.MATCH);
                } else {
                    insets[3] = Math.max(0, plan.x - contentX(parent) - marginLeft) + "px";
                    insets[1] = "auto"; refs[3] = refs[1] = "-"; positions[3] = positions[1] = "0";
                    targetDocument.setAttribute("guidedPreferredWidth", String.valueOf(plan.width));
                    targetDocument.setAttribute("guidedHorizontalSize", GuidedLayoutSupport.FIXED);
                    targetDocument.setAttribute("guidedMatchWidth", null);
                }
                targetDocument.setAttribute("guidedHorizontalAnchor", null);
            }
            if ((edges & 12) != 0) {
                String explicitMatch = value(element, "guidedMatchHeight", "");
                boolean keepExplicitMatch = GuidedLayoutSupport.MATCH.equals(GuidedLayoutSupport.verticalPolicy(element))
                        && plan.matchHeight != null && explicitMatch.equals(value(plan.matchHeight, "name", ""));
                if (keepExplicitMatch) {
                    applyMatchedHeight(plan.matchHeight, insets, refs, positions, plan.y, plan.height);
                    targetDocument.setAttribute("guidedMatchHeight", explicitMatch);
                    targetDocument.setAttribute("guidedVerticalSize", GuidedLayoutSupport.MATCH);
                } else {
                    insets[0] = Math.max(0, plan.y - contentY(parent) - marginTop) + "px";
                    insets[2] = "auto"; refs[0] = refs[2] = "-"; positions[0] = positions[2] = "0";
                    targetDocument.setAttribute("guidedPreferredHeight", String.valueOf(plan.height));
                    targetDocument.setAttribute("guidedVerticalSize", GuidedLayoutSupport.FIXED);
                    targetDocument.setAttribute("guidedMatchHeight", null);
                }
                targetDocument.setAttribute("guidedVerticalAnchor", null);
            }
            targetDocument.setAttribute("layeredInsets", GuidedLayoutSupport.joinInsets(insets[0], insets[1], insets[2], insets[3]));
            targetDocument.setAttribute("guidedReferences", GuidedLayoutSupport.joinReferences(refs[0], refs[1], refs[2], refs[3]));
            targetDocument.setAttribute("guidedReferencePositions", GuidedLayoutSupport.joinPositions(positions[0], positions[1], positions[2], positions[3]));
        } finally {
            targetDocument.endTransaction();
        }
    }

    private void updateDesignerDrag(int x, int y) {
        if (!designerDragArmed) return;
        if (!designerDragActive && dragThresholdReached(designerPressX, designerPressY, x, y)) {
            designerDragActive = true;
            recordAction("drag_started", "component", designerDraggedElement == null ? null
                    : value(designerDraggedElement, "name", "component"), "paletteType", designerPaletteType);
            if (designerSuppressAction != null) designerSuppressAction.run();
            finishInlineEditor();
            hideDropGuide();
        }
        if (designerDragActive) {
            autoScrollDuringDrag(x, y);
            showDropGuide(designerDraggedElement, activePreviewElementAt(x, y), designerDragSource, x, y);
        }
    }

    private void finishDesignerDrag(int x, int y) {
        if (!designerDragArmed) {
            if (activeDropPlan != null || boxDropSpacer != null) hideDropGuide();
            return;
        }
        Element element = designerDraggedElement;
        GuiDocument dragDocument = designerDragDocument;
        String paletteType = designerPaletteType;
        boolean activeDrag = designerDragActive;
        Component dragSource = designerDragSource;
        DropPlan releasePlan = activeDrag && element != null && dragDocument == document
                ? planDrop(element, designerDropTargetAt(element, x, y), dragSource, x, y) : null;
        designerDraggedElement = null;
        designerDragDocument = null;
        designerPaletteType = null;
        designerDragSource = null;
        designerSuppressAction = null;
        designerDragArmed = false;
        designerDragActive = false;
        if (!activeDrag) {
            recordAction("drag_cancelled", "reason", "released_before_threshold");
            hideDropGuide();
            return;
        }
        recordAction("drag_released", "component", element == null ? null : value(element, "name", "component"),
                "x", Integer.valueOf(x), "y", Integer.valueOf(y));
        if (dragDocument != document || (element != null && !document.containsElement(element))) {
            hideDropGuide();
            setStatus("Drop cancelled because the active form changed");
            return;
        }
        if (paletteType != null) addComponentAt(paletteType, x, y);
        else if (element != null) handleComponentDrop(element, releasePlan, x, y);
    }

    private void handleComponentDrop(Element dragged, DropPlan releasePlan, int x, int y) {
        try {
            if (!document.containsElement(dragged)) {
                setStatus("Drop cancelled — component is not part of the active form");
                return;
            }
            DropPlan plan = releasePlan;
            if (plan == null || !plan.valid) {
                recordAction("drop_rejected", "component", value(dragged, "name", "component"),
                        "reason", plan == null ? "outside_form" : plan.message);
                setStatus(plan == null ? "Drop outside the form — no changes made" : plan.message);
                return;
            }
            boolean grouped = selectedElements.size() > 1 && selectedElements.contains(dragged)
                    && applyGroupedGuidedDrop(dragged, plan);
            if (grouped || applyDropPlan(dragged, plan, x, y)) {
                recordAction("drop_committed", "component", value(dragged, "name", "component"),
                        "groupCount", Integer.valueOf(grouped ? selectedElements.size() : 1),
                        "layout", plan.layout, "constraint", plan.constraint, "target",
                        value(plan.target, "name", value(plan.target, "type", "component")),
                        "snap", plan.snapDescription,
                        "horizontalKind", plan.horizontalSnap == null ? null : plan.horizontalSnap.kind,
                        "horizontalReference", snapReferenceName(plan.horizontalSnap),
                        "verticalKind", plan.verticalSnap == null ? null : plan.verticalSnap.kind,
                        "verticalReference", snapReferenceName(plan.verticalSnap));
                setStatus(grouped ? "Moved " + selectedElements.size() + " components as a group"
                        : "Moved " + value(dragged, "name", value(dragged, "type", "component"))
                                + (plan.constraint == null ? "" : " to " + plan.constraint));
                scheduleDesignerRefresh();
            }
        } catch (Throwable ex) {
            Log.e(ex);
            recordAction("drop_error", "component", value(dragged, "name", "component"), "message", ex.getMessage());
            setStatus("Drop failed safely — " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
            ToastBar.showErrorMessage("Unable to complete drop; the form was left editable");
        } finally {
            hideDropGuide();
            Component preview = componentForElement(canvasHost, dragged);
            if (preview != null) preview.setVisible(true);
            if (workspace != null) workspace.revalidate();
        }
    }

    boolean applyGroupedGuidedDrop(Element dragged, DropPlan plan) {
        if (document == null || plan == null || !plan.valid || !"LayeredLayout".equals(plan.layout)
                || !selectedElementsShareGuidedParent()) return false;
        GroupMoveGeometry geometry = groupedMoveGeometry(dragged, plan);
        if (geometry == null) return false;
        Element primary = document.selected();
        Set<String> selectedNames = new LinkedHashSet<>();
        for (Element element : selectedElements) selectedNames.add(value(element, "name", ""));
        document.beginTransaction();
        try {
            // Components outside the group must not be pulled along by a selected anchor. Rebase
            // them at their current rectangle before moving the group, while retaining every
            // relationship whose two endpoints are both selected.
            for (Element selected : selectedElements) {
                String selectedName = value(selected, "name", "");
                for (Element candidate : document.components()) {
                    String candidateName = value(candidate, "name", "");
                    if (selectedNames.contains(candidateName)) continue;
                    if (incomingReferenceNames(document, candidateName).contains(selectedName)) {
                        freezeDependencyAtCurrentBounds(document, candidateName, selectedName);
                    }
                }
            }
            for (Map.Entry<Element, Component> entry : geometry.previews.entrySet()) {
                Component preview = entry.getValue();
                translateGuidedGroupElement(entry.getKey(), geometry.parent, preview,
                        geometry.dx, geometry.dy, selectedNames);
            }
            document.select(primary);
        } finally {
            document.endTransaction();
        }
        return true;
    }

    private GroupMoveGeometry groupedMoveGeometry(Element dragged, DropPlan plan) {
        if (document == null || canvasHost == null || dragged == null || plan == null
                || !plan.valid || !"LayeredLayout".equals(plan.layout)
                || !selectedElements.contains(dragged) || !selectedElementsShareGuidedParent()) return null;
        Element parentElement = document.parentOf(dragged);
        if (parentElement != plan.parent) return null;
        Component parentPreview = componentForElement(canvasHost, parentElement);
        Component draggedPreview = componentForElement(canvasHost, dragged);
        if (!(parentPreview instanceof Container) || draggedPreview == null) return null;
        Map<Element, Component> previews = new LinkedHashMap<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Element element : selectedElements) {
            if (document.parentOf(element) != parentElement) return null;
            Component preview = componentForElement(canvasHost, element);
            if (preview == null) return null;
            previews.put(element, preview);
            minX = Math.min(minX, preview.getAbsoluteX());
            minY = Math.min(minY, preview.getAbsoluteY());
            maxX = Math.max(maxX, preview.getAbsoluteX() + preview.getWidth());
            maxY = Math.max(maxY, preview.getAbsoluteY() + preview.getHeight());
        }
        int dx = plan.snapX - draggedPreview.getAbsoluteX();
        int dy = plan.snapY - draggedPreview.getAbsoluteY();
        int contentLeft = contentX(((Container) parentPreview));
        int contentTop = contentY(((Container) parentPreview));
        int contentRight = contentLeft + contentWidth(((Container) parentPreview));
        int contentBottom = contentTop + contentHeight(((Container) parentPreview));
        dx = Math.max(contentLeft - minX, Math.min(dx, contentRight - maxX));
        dy = Math.max(contentTop - minY, Math.min(dy, contentBottom - maxY));
        return new GroupMoveGeometry(((Container) parentPreview), previews, dx, dy);
    }

    GuidedSimulation simulateGroupedGuidedDrop(Element dragged, DropPlan plan) {
        GroupMoveGeometry geometry = groupedMoveGeometry(dragged, plan);
        if (geometry == null || geometry.previews.size() < 2) return null;
        List<DragGuideOverlay.GlassItem> items = new ArrayList<>();
        Set<String> changed = new LinkedHashSet<>();
        String activeName = value(dragged, "name", "component");
        for (Map.Entry<Element, Component> entry : geometry.previews.entrySet()) {
            Component preview = entry.getValue();
            String name = value(entry.getKey(), "name", value(entry.getKey(), "type", "component"));
            changed.add(name);
            items.add(new DragGuideOverlay.GlassItem(name,
                    preview.getAbsoluteX(), preview.getAbsoluteY(), preview.getWidth(), preview.getHeight(),
                    preview.getAbsoluteX() + geometry.dx, preview.getAbsoluteY() + geometry.dy,
                    preview.getWidth(), preview.getHeight(), name.equals(activeName)));
        }
        return new GuidedSimulation(document, items, new ArrayList<>(),
                "Preview: move " + items.size() + " components as a group", changed);
    }

    private void translateGuidedGroupElement(Element element, Container parent, Component component,
            int dx, int dy, Set<String> selectedNames) {
        String[] insets = GuidedLayoutSupport.insetValues(element);
        String[] refs = GuidedLayoutSupport.referenceNames(element);
        String[] positions = GuidedLayoutSupport.referencePositions(element);
        boolean leftInternal = selectedNames.contains(refs[3]);
        boolean rightInternal = selectedNames.contains(refs[1]);
        boolean topInternal = selectedNames.contains(refs[0]);
        boolean bottomInternal = selectedNames.contains(refs[2]);
        document.select(element);

        if (!leftInternal && !rightInternal) {
            insets[3] = Math.max(0, component.getAbsoluteX() + dx - contentX(parent)
                    - component.getStyle().getMarginLeftNoRTL()) + "px";
            insets[1] = "auto";
            refs[3] = refs[1] = "-";
            positions[3] = positions[1] = "0";
            document.setAttribute("guidedHorizontalAnchor", null);
        } else {
            if (!leftInternal) { insets[3] = "auto"; refs[3] = "-"; positions[3] = "0"; }
            if (!rightInternal) { insets[1] = "auto"; refs[1] = "-"; positions[1] = "0"; }
        }
        if (!topInternal && !bottomInternal) {
            insets[0] = Math.max(0, component.getAbsoluteY() + dy - contentY(parent)
                    - component.getStyle().getMarginTop()) + "px";
            insets[2] = "auto";
            refs[0] = refs[2] = "-";
            positions[0] = positions[2] = "0";
            document.setAttribute("guidedVerticalAnchor", null);
        } else {
            if (!topInternal) { insets[0] = "auto"; refs[0] = "-"; positions[0] = "0"; }
            if (!bottomInternal) { insets[2] = "auto"; refs[2] = "-"; positions[2] = "0"; }
        }

        // Fill needs opposing constraints. If a group move removes one external edge, preserve
        // the rendered dimension instead of allowing the component to resize unexpectedly.
        if (GuidedLayoutSupport.FILL.equals(GuidedLayoutSupport.horizontalPolicy(element))
                && !(leftInternal && rightInternal)) {
            document.setAttribute("guidedHorizontalSize", GuidedLayoutSupport.FIXED);
            document.setAttribute("guidedPreferredWidth", String.valueOf(Math.max(1, component.getWidth())));
        }
        if (GuidedLayoutSupport.FILL.equals(GuidedLayoutSupport.verticalPolicy(element))
                && !(topInternal && bottomInternal)) {
            document.setAttribute("guidedVerticalSize", GuidedLayoutSupport.FIXED);
            document.setAttribute("guidedPreferredHeight", String.valueOf(Math.max(1, component.getHeight())));
        }
        document.setAttribute("layeredInsets", GuidedLayoutSupport.joinInsets(insets[0], insets[1], insets[2], insets[3]));
        document.setAttribute("guidedReferences", GuidedLayoutSupport.joinReferences(refs[0], refs[1], refs[2], refs[3]));
        document.setAttribute("guidedReferencePositions", GuidedLayoutSupport.joinPositions(
                positions[0], positions[1], positions[2], positions[3]));
    }

    private void addComponentAt(String type, int x, int y) {
        Element previous = document.selected();
        Element added = null;
        // A rejected drop has to leave the document exactly as it was. The candidate is inserted
        // under the current selection before the plan is known, and for a TableLayout parent that
        // insertion assigns a cell and can grow tableLayoutRows -- undoing only the child left the
        // table enlarged and the document dirty for a drop that never happened.
        String tableRowsBefore = null;
        Element candidateParent = GuiDocument.acceptsChildren(previous) ? previous : document.parentOf(previous);
        if (candidateParent != null && "TableLayout".equals(candidateParent.getAttribute("layout"))) {
            tableRowsBefore = candidateParent.getAttribute("tableLayoutRows");
        }
        try {
            added = document.addComponent(type);
            if (added == null) {
                ToastBar.showErrorMessage("That container is a BorderLayout and all five regions are taken");
                setStatus("Nothing added: the drop target is a full BorderLayout");
                return;
            }
            Element target = activePreviewElementAt(x, y);
            Component palettePreview = ComponentPreviewFactory.create(added, null, new ComponentPreviewFactory.SelectionHandler() {
                @Override public void selected(Element element) { }
                @Override public void dragPressed(Element element, Component source, int px, int py) { }
                @Override public boolean isDragActive() { return false; }
                @Override public void editContent(Element element) { }
            });
            DropPlan plan = planDrop(added, target, palettePreview, x, y);
            if (plan == null || !plan.valid) {
                document.select(added);
                document.deleteSelected();
                restoreTableRows(candidateParent, tableRowsBefore);
                document.select(previous);
                setStatus(plan == null ? "Drop inside the form to add " + type : plan.message);
                return;
            }
            if (applyDropPlan(added, plan, x, y)) {
                setStatus("Added " + type + (plan.constraint == null ? "" : " to " + plan.constraint));
                scheduleDesignerRefresh();
            }
        } catch (Throwable ex) {
            Log.e(ex);
            if (added != null && document.parentOf(added) != null) {
                document.select(added);
                document.deleteSelected();
                restoreTableRows(candidateParent, tableRowsBefore);
                document.select(previous);
            }
            setStatus("Drop failed safely — " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        } finally {
            hideDropGuide();
            if (workspace != null) workspace.revalidate();
        }
    }

    /**
     * Puts a table's declared row count back after a rejected drop.
     *
     * @param parent the table the candidate was inserted into, or null when it was not a table
     * @param rows the value the attribute held before, possibly null
     */
    private void restoreTableRows(Element parent, String rows) {
        if (parent == null || !"TableLayout".equals(parent.getAttribute("layout"))) return;
        document.setAttribute(parent, "tableLayoutRows", rows);
    }

    private boolean dropAfter(Element target, String layout, int x, int y) {
        if (target == null || GuiDocument.acceptsChildren(target)) return false;
        Component preview = componentForElement(canvasHost, target);
        if (preview == null) return false;
        Element parent = document.parentOf(target);
        return "FlowLayout".equals(layout) || "GridLayout".equals(layout)
                ? x >= preview.getAbsoluteX() + preview.getWidth() / 2
                : "X".equals(value(parent, "boxLayoutAxis", "Y"))
                ? x >= preview.getAbsoluteX() + preview.getWidth() / 2
                : y >= preview.getAbsoluteY() + preview.getHeight() / 2;
    }

    private void showDropGuide(Element dragged, Element target, Component source, int x, int y) {
        target = normalizeDesignerDropTarget(dragged, target);
        dropGuideTarget = target;
        DropPlan plan = planDrop(dragged, target, source, x, y);
        if (plan == null) {
            hideDropGuide();
            return;
        }
        activeDropPlan = plan;
        String journalSignature = value(dragged, "name", designerPaletteType == null ? "component" : designerPaletteType)
                + "|" + value(plan.target, "name", value(plan.target, "type", "component"))
                + "|" + plan.layout + "|" + plan.constraint + "|" + plan.valid + "|" + plan.snapDescription;
        if (!journalSignature.equals(lastDragJournalSignature)) {
            lastDragJournalSignature = journalSignature;
            recordAction("drag_preview", "component", value(dragged, "name", designerPaletteType),
                    "target", value(plan.target, "name", value(plan.target, "type", "component")),
                    "layout", plan.layout, "constraint", plan.constraint, "valid", Boolean.valueOf(plan.valid),
                    "description", plan.snapDescription,
                    "horizontalKind", plan.horizontalSnap == null ? null : plan.horizontalSnap.kind,
                    "horizontalReference", snapReferenceName(plan.horizontalSnap),
                    "verticalKind", plan.verticalSnap == null ? null : plan.verticalSnap.kind,
                    "verticalReference", snapReferenceName(plan.verticalSnap));
        }
        Component parentPreview = componentForElement(canvasHost, plan.parent);
        Element guideElement = plan.occupied == null ? target : plan.occupied;
        Component targetPreview = componentForElement(canvasHost, guideElement);
        if (dragGuideOverlay != null && parentPreview != null) {
            dragGuideOverlay.showGuide(plan.layout, value(plan.parent, "boxLayoutAxis", "Y"), plan.constraint,
                    plan.valid, parentPreview, targetPreview, source, x, y,
                    plan.snapX, plan.snapY, plan.snapW, plan.snapH, plan.snapDescription);
            if (dragged != null && plan.valid && "LayeredLayout".equals(plan.layout)) {
                GuidedSimulation grouped = simulateGroupedGuidedDrop(dragged, plan);
                if (grouped != null) showSimulation(grouped);
                else showGuidedDropSimulation(dragged, plan, (Container) parentPreview, source);
            } else {
                dragGuideOverlay.clearSimulation();
            }
        }
        if ("BoxLayout".equals(plan.layout) && plan.valid && parentPreview instanceof Container) {
            showBoxDropSpacer(((Container) parentPreview), targetPreview, plan.after,
                    "X".equals(value(plan.parent, "boxLayoutAxis", "Y")));
        } else {
            clearBoxDropSpacer();
        }
        setStatus(plan.message);
    }

    private void hideDropGuide() {
        dropGuideTarget = null;
        activeDropPlan = null;
        if (dragGuideOverlay != null) dragGuideOverlay.hideGuide();
        clearBoxDropSpacer();
    }

    private void scheduleDesignerRefresh() {
        if (refreshPending) return;
        refreshPending = true;
        Display.getInstance().callSerially(() -> {
            refreshPending = false;
            refreshEditor();
        });
    }

    private void showBoxDropSpacer(Container parent, Component target, boolean after, boolean horizontal) {
        int index = target != null && target.getParent() == parent ? parent.getComponentIndex(target) : parent.getComponentCount();
        if (after && index < parent.getComponentCount()) index++;
        if (boxDropParent == parent && boxDropIndex == index && boxDropSpacer != null) return;
        if (boxDropParent == parent && boxDropSpacer != null && boxDropSpacer.getParent() == parent) {
            boxDropIndex = Math.max(0, Math.min(index, parent.getComponentCount() - 1));
            parent.removeComponent(boxDropSpacer);
            parent.addComponent(Math.min(boxDropIndex, parent.getComponentCount()), boxDropSpacer);
            parent.revalidate();
            return;
        }
        clearBoxDropSpacer();
        final int spacerWidth = horizontal ? Math.max(12, Display.getInstance().convertToPixels(2)) : 1;
        final int spacerHeight = horizontal ? 1 : Math.max(12, Display.getInstance().convertToPixels(2));
        Label spacer = new Label(" ", "BuilderDropSpacer") {
            @Override protected Dimension calcPreferredSize() {
                return new Dimension(spacerWidth, spacerHeight);
            }
        };
        spacer.setShowEvenIfBlank(true);
        boxDropSpacer = spacer;
        boxDropTargetElement = target == null ? null : (Element) target.getClientProperty("gui.element");
        spacer.putClientProperty("gui.dropTargetElement", boxDropTargetElement);
        boxDropParent = parent;
        boxDropIndex = Math.max(0, Math.min(index, parent.getComponentCount()));
        parent.addComponent(boxDropIndex, spacer);
        // revalidate, never animateLayout: a layout animation captures the preview tree and
        // re-applies that captured state when it finishes. The drop commits and rebuilds the
        // canvas well inside the animation's window, so the animation then restored the
        // pre-drop preview over the new one -- the model moved, the canvas did not, and the
        // spacer stayed behind. Nested containers made it worse because each level animated.
        parent.revalidate();
    }

    private void clearBoxDropSpacer() {
        if (boxDropSpacer != null && boxDropSpacer.getParent() != null) {
            Container parent = boxDropSpacer.getParent();
            parent.removeComponent(boxDropSpacer);
            parent.revalidate();
        }
        boxDropSpacer = null;
        boxDropTargetElement = null;
        boxDropParent = null;
        boxDropIndex = -1;
    }

    DropPlan planDrop(Element dragged, Element target, Component source, int x, int y) {
        if (document == null || target == null || target == dragged
                || !document.containsElement(target)
                || (dragged != null && !document.containsElement(dragged))) return null;
        Element parent = GuiDocument.acceptsChildren(target) ? target : document.parentOf(target);
        if (parent == null || parent == dragged) return null;
        String layout = value(parent, "layout", "BoxLayout");
        DropPlan plan = new DropPlan();
        plan.document = document;
        plan.target = target;
        plan.parent = parent;
        plan.layout = layout;
        plan.snapX = x - (dragged == null ? 0 : designerGrabOffsetX);
        plan.snapY = y - (dragged == null ? 0 : designerGrabOffsetY);
        plan.after = dropAfter(target, layout, x, y);
        plan.valid = true;
        plan.message = "Drop into " + value(parent, "name", value(parent, "type", "container"));
        placementAdapter(layout).plan(plan, dragged, target, source, x, y);
        return plan;
    }

    private PlacementAdapter placementAdapter(String layout) {
        if ("BorderLayout".equals(layout)) return new BorderPlacementAdapter();
        if ("LayeredLayout".equals(layout)) return new LayeredPlacementAdapter();
        if ("TableLayout".equals(layout)) return new TablePlacementAdapter();
        if ("GridLayout".equals(layout)) return new GridPlacementAdapter();
        if ("FlowLayout".equals(layout)) return new FlowPlacementAdapter();
        return new BoxPlacementAdapter();
    }

    String placementAdapterName(String layout) { return placementAdapter(layout).getClass().getSimpleName(); }

    private interface PlacementAdapter {
        void plan(DropPlan plan, Element dragged, Element target, Component source, int x, int y);
    }

    private final class BorderPlacementAdapter implements PlacementAdapter {
        public void plan(DropPlan plan, Element dragged, Element target, Component source, int x, int y) {
            Component parentPreview = componentForElement(canvasHost, plan.parent);
            plan.constraint = borderRegionAt(plan.parent, dragged, parentPreview, source, x, y);
            plan.occupied = GuiDocument.childAtBorderConstraint(plan.parent, plan.constraint, dragged);
            if (plan.occupied != null && (dragged == null || source == null)) {
                plan.valid = false;
                plan.message = plan.constraint + " is occupied by "
                        + value(plan.occupied, "name", value(plan.occupied, "type", "component"));
            } else if (plan.occupied != null) {
                plan.message = "Swap " + value(dragged, "name", "component") + " with "
                        + value(plan.occupied, "name", "component") + " in " + plan.constraint;
            } else plan.message = "Drop into the " + plan.constraint + " region";
        }
    }

    private final class LayeredPlacementAdapter implements PlacementAdapter {
        public void plan(DropPlan plan, Element dragged, Element target, Component source, int x, int y) {
            resolveLayeredSnap(plan, source, plan.snapX, plan.snapY);
            plan.message = plan.snapDescription == null ? "Place freely in LayeredLayout" : "Snap: " + plan.snapDescription;
        }
    }

    private class BoxPlacementAdapter implements PlacementAdapter {
        public void plan(DropPlan plan, Element dragged, Element target, Component source, int x, int y) {
            plan.message = "Insert " + (plan.after ? "after " : "before ") + value(target, "name", "component");
        }
    }

    private final class FlowPlacementAdapter extends BoxPlacementAdapter { }
    private final class GridPlacementAdapter extends BoxPlacementAdapter { }

    /**
     * A table drop is a placement into one addressed cell, not an insertion into a sequence. It
     * therefore behaves like BorderLayout rather than BoxLayout: the pointer picks the cell, and a
     * cell that is already taken swaps rather than pushing everything along.
     */
    private final class TablePlacementAdapter implements PlacementAdapter {
        public void plan(DropPlan plan, Element dragged, Element target, Component source, int x, int y) {
            Component parentPreview = componentForElement(canvasHost, plan.parent);
            plan.tableCell = tableCellAt(plan.parent, parentPreview, x, y);
            plan.occupied = childAtTableCell(plan.parent, plan.tableCell[0], plan.tableCell[1], dragged);
            String cell = "row " + plan.tableCell[0] + ", column " + plan.tableCell[1];
            if (plan.occupied == null) {
                plan.message = "Place in " + cell;
            } else if (dragged == null) {
                plan.valid = false;
                plan.message = cell + " is occupied by "
                        + value(plan.occupied, "name", value(plan.occupied, "type", "component"));
            } else {
                plan.message = "Swap with " + value(plan.occupied, "name", "component") + " in " + cell;
            }
        }
    }

    private String borderRegionAt(Element parent, Element dragged, Component parentPreview,
            Component source, int x, int y) {
        if (parentPreview == null || parentPreview.getWidth() < 1 || parentPreview.getHeight() < 1
                || x < parentPreview.getAbsoluteX() || x > parentPreview.getAbsoluteX() + parentPreview.getWidth()
                || y < parentPreview.getAbsoluteY() || y > parentPreview.getAbsoluteY() + parentPreview.getHeight()) {
            return firstAvailableBorderConstraint(parent, dragged);
        }
        int px = parentPreview.getAbsoluteX();
        int py = parentPreview.getAbsoluteY();
        int pw = parentPreview.getWidth();
        int ph = parentPreview.getHeight();
        int sourceW = source == null || source.getWidth() < 1 ? Math.max(48, pw / 5) : source.getWidth();
        int sourceH = source == null || source.getHeight() < 1 ? Math.max(32, ph / 8) : source.getHeight();

        // CENTER normally covers every pixel left over by BorderLayout.  If component hit-testing
        // runs first, that makes empty EAST/WEST (and often NORTH/SOUTH) impossible to reach.
        // Resolve intentional edge bands first, then use the component under the pointer for the
        // central area.  The bands scale with the dragged component but remain generous enough to
        // use with a mouse.
        String edgeRegion = borderEdgeRegion(px, py, pw, ph, sourceW, sourceH, x, y);
        if (edgeRegion != null) return edgeRegion;

        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object childValue = parent.getChildAt(i);
            if (!(childValue instanceof Element) || ((Element) childValue) == dragged || !"component".equals(((Element) childValue).getTagName())) continue;
            Component childPreview = componentForElement(canvasHost, ((Element) childValue));
            if (childPreview != null && x >= childPreview.getAbsoluteX() && x <= childPreview.getAbsoluteX() + childPreview.getWidth()
                    && y >= childPreview.getAbsoluteY() && y <= childPreview.getAbsoluteY() + childPreview.getHeight()) {
                return GuiDocument.effectiveBorderConstraint(parent, ((Element) childValue));
            }
        }
        return "Center";
    }

    static String borderEdgeRegion(int px, int py, int pw, int ph, int sourceW, int sourceH, int x, int y) {
        int horizontalBand = Math.min(Math.max(Math.max(64, sourceW), pw * 30 / 100), Math.max(1, pw * 40 / 100));
        int verticalBand = Math.min(Math.max(Math.max(44, sourceH), ph * 22 / 100), Math.max(1, ph * 35 / 100));
        int north = y - py;
        int south = py + ph - y;
        int west = x - px;
        int east = px + pw - x;
        String region = null;
        double score = Double.MAX_VALUE;
        if (north >= 0 && north <= verticalBand) { region = "North"; score = (double) north / verticalBand; }
        if (south >= 0 && south <= verticalBand && (double) south / verticalBand < score) {
            region = "South"; score = (double) south / verticalBand;
        }
        if (west >= 0 && west <= horizontalBand && (double) west / horizontalBand < score) {
            region = "West"; score = (double) west / horizontalBand;
        }
        if (east >= 0 && east <= horizontalBand && (double) east / horizontalBand < score) region = "East";
        return region;
    }

    private void resolveLayeredSnap(DropPlan plan, Component source, int x, int y) {
        Component parentComponent = componentForElement(canvasHost, plan.parent);
        if (!(parentComponent instanceof Container)) return;
        int contentW = contentWidth(((Container) parentComponent));
        int contentH = contentHeight(((Container) parentComponent));
        int width = source == null ? Math.max(80, contentW / 6)
                : source.getWidth() > 0 ? source.getWidth() : Math.max(1, source.getPreferredW());
        int height = source == null ? Math.max(36, contentH / 12)
                : source.getHeight() > 0 ? source.getHeight() : Math.max(1, source.getPreferredH());
        int desiredX = Math.max(contentX(((Container) parentComponent)), Math.min(x, contentX(((Container) parentComponent)) + contentW - width));
        int desiredY = Math.max(contentY(((Container) parentComponent)), Math.min(y, contentY(((Container) parentComponent)) + contentH - height));
        SnapResult horizontal = snapAxis(((Container) parentComponent), source, desiredX, width, true);
        SnapResult vertical = snapAxis(((Container) parentComponent), source, desiredY, height, false);
        plan.snapX = horizontal.position;
        plan.snapY = vertical.position;
        plan.snapW = width;
        plan.snapH = height;
        plan.horizontalSnap = horizontal;
        plan.verticalSnap = vertical;
        if (horizontal.description != null && vertical.description != null) {
            plan.snapDescription = horizontal.description + " • " + vertical.description;
        } else {
            plan.snapDescription = horizontal.description == null ? vertical.description : horizontal.description;
        }
    }

    private SnapResult snapAxis(Container parent, Component source, int desired, int size, boolean horizontal) {
        List<SnapCandidate> candidates = new ArrayList<>();
        int start = horizontal ? contentX(parent) : contentY(parent);
        int length = horizontal ? contentWidth(parent) : contentHeight(parent);
        candidates.add(new SnapCandidate(start, horizontal ? "dock left" : "dock top", null, "parentStart"));
        candidates.add(new SnapCandidate(start + length - size, horizontal ? "dock right" : "dock bottom", null, "parentEnd"));
        candidates.add(new SnapCandidate(start + (length - size) / 2,
                horizontal ? "center horizontally" : "center vertically", null, "parentCenter"));
        int gap = Math.max(6, Display.getInstance().convertToPixels(1));
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component sibling = parent.getComponentAt(i);
            if (sibling == source || sibling == boxDropSpacer || sibling.getClientProperty("gui.element") == null) continue;
            Element siblingElement = (Element) sibling.getClientProperty("gui.element");
            int siblingStart = horizontal ? sibling.getAbsoluteX() : sibling.getAbsoluteY();
            int siblingSize = horizontal ? sibling.getWidth() : sibling.getHeight();
            String name = sibling.getName() == null ? "component" : sibling.getName().replace("preview.", "");
            candidates.add(new SnapCandidate(siblingStart, "align start with " + name, siblingElement, "alignStart"));
            candidates.add(new SnapCandidate(siblingStart + siblingSize - size, "align end with " + name, siblingElement, "alignEnd"));
            candidates.add(new SnapCandidate(siblingStart + (siblingSize - size) / 2,
                    "align center with " + name, siblingElement, "alignCenter"));
            candidates.add(new SnapCandidate(siblingStart + siblingSize + gap, "space after " + name, siblingElement, "after"));
            candidates.add(new SnapCandidate(siblingStart - size - gap, "space before " + name, siblingElement, "before"));
            if (!horizontal) {
                int sourceBaseline = source == null ? -1 : source.getBaseline(
                        source.getWidth() > 0 ? source.getWidth() : source.getPreferredW(),
                        source.getHeight() > 0 ? source.getHeight() : source.getPreferredH());
                int siblingBaseline = sibling.getBaseline(sibling.getWidth(), sibling.getHeight());
                if (sourceBaseline >= 0 && siblingBaseline >= 0) {
                    candidates.add(new SnapCandidate(sibling.getAbsoluteY() + siblingBaseline - sourceBaseline,
                            "align baseline with " + name, siblingElement, "baseline"));
                }
            }
        }
        int threshold = Math.max(8, Display.getInstance().convertToPixels(1.5f));
        int best = desired;
        int bestDistance = threshold + 1;
        SnapCandidate selected = null;
        for (int i = 0; i < candidates.size(); i++) {
            SnapCandidate candidate = candidates.get(i);
            int distance = Math.abs(candidate.position - desired);
            if (distance < bestDistance) {
                best = candidate.position;
                bestDistance = distance;
                selected = candidate;
            }
        }
        best = Math.max(start, Math.min(best, start + length - size));
        if (bestDistance > threshold) selected = null;
        return new SnapResult(best, selected == null ? null : selected.description,
                selected == null ? null : selected.reference, selected == null ? null : selected.kind);
    }

    private String firstAvailableBorderConstraint(Element parent, Element excluding) {
        String[] constraints = {"Center", "North", "South", "West", "East"};
        for (String candidate : constraints) {
            if (GuiDocument.childAtBorderConstraint(parent, candidate, excluding) == null) return candidate;
        }
        return excluding != null && document.parentOf(excluding) == parent
                ? GuiDocument.effectiveBorderConstraint(parent, excluding) : "Center";
    }

    boolean applyDropPlan(Element dragged, DropPlan plan, int x, int y) {
        if (document == null || plan == null || plan.document != document
                || !document.containsElement(dragged)
                || !document.containsElement(plan.parent)
                || !document.containsElement(plan.target)
                || (plan.occupied != null && !document.containsElement(plan.occupied))) return false;
        document.beginTransaction();
        try {
            return applyDropPlanImpl(dragged, plan, x, y);
        } finally {
            document.endTransaction();
        }
    }

    private boolean applyDropPlanImpl(Element dragged, DropPlan plan, int x, int y) {
        Element oldParent = document.parentOf(dragged);
        if ("TableLayout".equals(plan.layout) && plan.tableCell != null) {
            return applyTableDrop(dragged, plan, oldParent);
        }
        int oldIndex = document.componentIndex(oldParent, dragged);
        String oldLayout = oldParent == null ? "" : value(oldParent, "layout", "BoxLayout");
        String oldLayeredInsets = dragged.getAttribute("layeredInsets");
        String oldConstraint = oldParent == null || !"BorderLayout".equals(value(oldParent, "layout", "BoxLayout"))
                ? null : GuiDocument.effectiveBorderConstraint(oldParent, dragged);
        if (plan.occupied != null && oldParent == plan.parent) {
            document.select(plan.occupied);
            document.setAttribute("layoutConstraint", oldConstraint);
            document.select(dragged);
            document.setAttribute("layoutConstraint", plan.constraint);
            return true;
        }
        if (plan.occupied != null) {
            int destinationIndex = document.componentIndex(plan.parent, plan.occupied);
            document.select(plan.occupied);
            if (!document.moveSelectedToParent(oldParent, oldIndex)) return false;
            if ("BorderLayout".equals(oldLayout)) {
                document.setAttribute("layoutConstraint", oldConstraint);
                document.setAttribute("layeredInsets", null);
            } else if ("LayeredLayout".equals(oldLayout)) {
                document.setAttribute("layoutConstraint", null);
                document.setAttribute("layeredInsets", oldLayeredInsets);
            } else {
                document.setAttribute("layoutConstraint", null);
                document.setAttribute("layeredInsets", null);
            }
            document.select(dragged);
            if (!document.moveSelectedToParent(plan.parent, destinationIndex)) return false;
        }
        document.select(dragged);
        // A positional change inside a LayeredLayout must not also change stacking/XML order.
        // Apart from being surprising, that made commit differ from the glass simulation and
        // could cause a referenced sibling chain to be laid out in a different pass. Moving to a
        // different parent still needs a structural move; reordering within the same guided
        // parent belongs to the explicit tree/z-order commands.
        boolean sameGuidedParent = "LayeredLayout".equals(plan.layout) && oldParent == plan.parent;
        boolean moved = plan.occupied != null || sameGuidedParent || document.moveSelectedTo(plan.target, plan.after);
        if (!moved && document.parentOf(dragged) != plan.parent) return false;
        document.select(dragged);
        if ("BorderLayout".equals(plan.layout)) {
            document.setAttribute("layoutConstraint", plan.constraint);
            document.setAttribute("layeredInsets", null);
        } else if ("LayeredLayout".equals(plan.layout)) {
            document.setAttribute("layoutConstraint", null);
            Component parentPreview = componentForElement(canvasHost, plan.parent);
            if (parentPreview != null && parentPreview.getWidth() > 0 && parentPreview.getHeight() > 0) {
                Component source = componentForElement(canvasHost, dragged);
                int width = plan.snapW > 0 ? plan.snapW
                        : source == null || source.getWidth() < 1 ? Math.max(1, parentPreview.getWidth() / 5) : source.getWidth();
                int height = plan.snapH > 0 ? plan.snapH
                        : source == null || source.getHeight() < 1 ? Math.max(1, parentPreview.getHeight() / 10) : source.getHeight();
                if (plan.horizontalSnap != null || plan.verticalSnap != null) {
                    prepareGuidedCycleBreak(document, dragged, plan);
                    persistGuidedConstraints(document, dragged, plan, (Container) parentPreview, source, width, height);
                    return true;
                }
                int contentW = contentWidth(parentPreview);
                int contentH = contentHeight(parentPreview);
                int marginLeft = source == null ? 0 : source.getStyle().getMarginLeftNoRTL();
                int marginRight = source == null ? 0 : source.getStyle().getMarginRightNoRTL();
                int marginTop = source == null ? 0 : source.getStyle().getMarginTop();
                int marginBottom = source == null ? 0 : source.getStyle().getMarginBottom();
                int leftPx = Math.max(0, plan.snapX - contentX(parentPreview) - marginLeft);
                int topPx = Math.max(0, plan.snapY - contentY(parentPreview) - marginTop);
                int rightPx = Math.max(0, contentW - leftPx - width - marginLeft - marginRight);
                int bottomPx = Math.max(0, contentH - topPx - height - marginTop - marginBottom);
                document.setAttribute("layeredInsets", topPx + "px " + rightPx + "px "
                        + bottomPx + "px " + leftPx + "px");
            }
        } else if ("TableLayout".equals(plan.layout)) {
            document.setAttribute("layoutConstraint", null);
            document.setAttribute("layeredInsets", null);
            normalizeTableCells(plan.parent);
            document.select(dragged);
        } else {
            document.setAttribute("layoutConstraint", null);
            document.setAttribute("layeredInsets", null);
        }
        return true;
    }

    /**
     * Commits a table drop as an explicit cell assignment. Only the dragged component and, when the
     * aimed cell was taken, its previous occupant change cells; every other child keeps the cell it
     * had. XML order is left alone as well, because in a table it carries no layout meaning and
     * reordering it would churn the generated source for no visible reason.
     */
    private boolean applyTableDrop(Element dragged, DropPlan plan, Element oldParent) {
        int row = plan.tableCell[0];
        int column = plan.tableCell[1];
        Integer vacatedRow = parseInteger(dragged.getAttribute("tableRow"));
        Integer vacatedColumn = parseInteger(dragged.getAttribute("tableColumn"));
        if (oldParent != plan.parent) {
            document.select(dragged);
            if (!document.moveSelectedToParent(plan.parent, componentChildren(plan.parent).size())) return false;
        }
        if (plan.occupied != null) {
            document.select(plan.occupied);
            boolean canSwap = oldParent == plan.parent && vacatedRow != null && vacatedColumn != null;
            int[] destination = canSwap ? new int[]{vacatedRow.intValue(), vacatedColumn.intValue()}
                    : firstFreeTableCell(plan.parent, dragged, plan.occupied);
            document.setAttribute("tableRow", String.valueOf(destination[0]));
            document.setAttribute("tableColumn", String.valueOf(destination[1]));
        }
        document.select(dragged);
        document.setAttribute("layoutConstraint", null);
        document.setAttribute("layeredInsets", null);
        document.setAttribute("tableRow", String.valueOf(row));
        document.setAttribute("tableColumn", String.valueOf(column));
        normalizeTableCells(plan.parent);
        document.select(dragged);
        return true;
    }

    /** The first cell in row-major order that no sibling claims, growing past the last row if needed. */
    private int[] firstFreeTableCell(Element parent, Element... ignored) {
        int columns = Math.max(1, integer(value(parent, "tableLayoutColumns", "2"), 2));
        Set<String> taken = new LinkedHashSet<>();
        for (Element child : componentChildren(parent)) {
            boolean skip = false;
            for (Element candidate : ignored) skip = skip || child == candidate;
            if (skip) continue;
            Integer row = parseInteger(child.getAttribute("tableRow"));
            Integer column = parseInteger(child.getAttribute("tableColumn"));
            if (row != null && column != null) taken.add(row + ":" + column);
        }
        for (int cursor = 0; ; cursor++) {
            int row = cursor / columns;
            int column = cursor % columns;
            if (!taken.contains(row + ":" + column)) return new int[]{row, column};
        }
    }

    private void persistGuidedConstraints(GuiDocument targetDocument, Element element, DropPlan plan, Container parent,
            Component source, int width, int height) {
        String[] insets = GuidedLayoutSupport.insetValues(element);
        String[] references = GuidedLayoutSupport.referenceNames(element);
        String[] positions = GuidedLayoutSupport.referencePositions(element);
        String horizontalPolicy = GuidedLayoutSupport.horizontalPolicy(element);
        String verticalPolicy = GuidedLayoutSupport.verticalPolicy(element);
        String matchWidthName = value(element, "guidedMatchWidth", "");
        String matchHeightName = value(element, "guidedMatchHeight", "");
        Set<String> retainedReferences = new LinkedHashSet<>();
        if (plan.horizontalSnap != null && plan.horizontalSnap.reference != null) {
            retainedReferences.add(value(plan.horizontalSnap.reference, "name", ""));
        }
        if (plan.verticalSnap != null && plan.verticalSnap.reference != null) {
            retainedReferences.add(value(plan.verticalSnap.reference, "name", ""));
        }
        int tearDistance = Math.max(6, Display.getInstance().convertToPixels(1));
        boolean pulledAway = source == null || Math.abs(plan.snapX - source.getAbsoluteX())
                + Math.abs(plan.snapY - source.getAbsoluteY()) > tearDistance;
        int marginLeft = source == null ? 0 : source.getStyle().getMarginLeftNoRTL();
        int marginRight = source == null ? 0 : source.getStyle().getMarginRightNoRTL();
        int marginTop = source == null ? 0 : source.getStyle().getMarginTop();
        int marginBottom = source == null ? 0 : source.getStyle().getMarginBottom();
        int left = plan.snapX - contentX(parent) - marginLeft;
        int top = plan.snapY - contentY(parent) - marginTop;
        int right = contentWidth(parent) - left - width - marginLeft - marginRight;
        int bottom = contentHeight(parent) - top - height - marginTop - marginBottom;

        clearHorizontalReferences(references, positions);
        clearVerticalReferences(references, positions);
        targetDocument.select(element);
        targetDocument.setAttribute("guidedHorizontalAnchor", null);
        targetDocument.setAttribute("guidedVerticalAnchor", null);

        applyHorizontalPosition(targetDocument, plan.horizontalSnap, insets, references, positions, left, right, width, source);
        applyVerticalPosition(targetDocument, plan.verticalSnap, insets, references, positions, top, bottom, height, source);

        if (GuidedLayoutSupport.FILL.equals(horizontalPolicy) && pulledAway && retainedReferences.isEmpty()) {
            horizontalPolicy = GuidedLayoutSupport.FIXED;
            targetDocument.setAttribute("guidedMatchWidth", null);
        } else if (GuidedLayoutSupport.FILL.equals(horizontalPolicy)) {
            insets[3] = Math.max(0, left) + "px";
            insets[1] = Math.max(0, right) + "px";
            references[3] = references[1] = "-";
        } else if (GuidedLayoutSupport.MATCH.equals(horizontalPolicy)) {
            Element match = namedSibling(plan.parent, matchWidthName);
            if (pulledAway && !retainedReferences.contains(matchWidthName)) {
                horizontalPolicy = GuidedLayoutSupport.FIXED;
                targetDocument.setAttribute("guidedMatchWidth", null);
            } else if (match != null) {
                applyMatchedWidth(match, insets, references, positions, plan.snapX, width);
            } else {
                horizontalPolicy = GuidedLayoutSupport.FIXED;
                targetDocument.setAttribute("guidedMatchWidth", null);
            }
        }
        if (GuidedLayoutSupport.FIXED.equals(horizontalPolicy)) {
            stabilizeFixedHorizontalSpan(plan.horizontalSnap, insets, references, positions, width, source);
            targetDocument.setAttribute("guidedPreferredWidth", String.valueOf(Math.max(1, width)));
        } else if (GuidedLayoutSupport.PREFERRED.equals(horizontalPolicy)) {
            targetDocument.setAttribute("guidedPreferredWidth", null);
        }

        if ("baseline".equals(plan.verticalSnap == null ? null : plan.verticalSnap.kind)) {
            verticalPolicy = GuidedLayoutSupport.PREFERRED;
            targetDocument.setAttribute("guidedPreferredHeight", null);
        } else if (GuidedLayoutSupport.FILL.equals(verticalPolicy) && pulledAway && retainedReferences.isEmpty()) {
            verticalPolicy = GuidedLayoutSupport.FIXED;
            targetDocument.setAttribute("guidedMatchHeight", null);
        } else if (GuidedLayoutSupport.FILL.equals(verticalPolicy)) {
            insets[0] = Math.max(0, top) + "px";
            insets[2] = Math.max(0, bottom) + "px";
            references[0] = references[2] = "-";
        } else if (GuidedLayoutSupport.MATCH.equals(verticalPolicy)) {
            Element match = namedSibling(plan.parent, matchHeightName);
            if (pulledAway && !retainedReferences.contains(matchHeightName)) {
                verticalPolicy = GuidedLayoutSupport.FIXED;
                targetDocument.setAttribute("guidedMatchHeight", null);
            } else if (match != null) {
                applyMatchedHeight(match, insets, references, positions, plan.snapY, height);
            } else {
                verticalPolicy = GuidedLayoutSupport.FIXED;
                targetDocument.setAttribute("guidedMatchHeight", null);
            }
        }
        if (GuidedLayoutSupport.FIXED.equals(verticalPolicy)) {
            stabilizeFixedVerticalSpan(plan.verticalSnap, insets, references, positions, height, source);
            targetDocument.setAttribute("guidedPreferredHeight", String.valueOf(Math.max(1, height)));
        } else if (GuidedLayoutSupport.PREFERRED.equals(verticalPolicy)) {
            targetDocument.setAttribute("guidedPreferredHeight", null);
        }

        targetDocument.setAttribute("guidedHorizontalSize", horizontalPolicy);
        targetDocument.setAttribute("guidedVerticalSize", verticalPolicy);
        targetDocument.setAttribute("layeredInsets", GuidedLayoutSupport.joinInsets(insets[0], insets[1], insets[2], insets[3]));
        targetDocument.setAttribute("guidedReferences", GuidedLayoutSupport.joinReferences(
                references[0], references[1], references[2], references[3]));
        targetDocument.setAttribute("guidedReferencePositions", GuidedLayoutSupport.joinPositions(
                positions[0], positions[1], positions[2], positions[3]));
    }

    /**
     * An auto opposite inset normally supplies preferred size. Near a parent edge LayeredLayout
     * clamps that auto inset to zero and silently shrinks the component. Pair both sides with the
     * same reference anchor instead: the relationship remains responsive, while the distance
     * between the two constraints is the component's complete outer width.
     */
    private void stabilizeFixedHorizontalSpan(SnapResult snap, String[] insets, String[] refs,
            String[] positions, int width, Component source) {
        if (snap == null || snap.reference == null) return;
        String ref = value(snap.reference, "name", "-");
        int outer = Math.max(1, width + (source == null ? 0
                : source.getStyle().getMarginLeftNoRTL() + source.getStyle().getMarginRightNoRTL()));
        int gap = standardGap();
        if ("alignStart".equals(snap.kind)) {
            refs[1] = ref; positions[1] = "1"; insets[1] = (-outer) + "px";
        } else if ("alignEnd".equals(snap.kind)) {
            refs[1] = ref; positions[1] = "0"; insets[1] = "0px";
        } else if ("after".equals(snap.kind)) {
            refs[1] = ref; positions[1] = "0"; insets[1] = (-(outer + gap)) + "px";
        } else if ("before".equals(snap.kind)) {
            refs[3] = ref; positions[3] = "0"; insets[3] = (-(outer + gap)) + "px";
        }
    }

    private void stabilizeFixedVerticalSpan(SnapResult snap, String[] insets, String[] refs,
            String[] positions, int height, Component source) {
        if (snap == null || snap.reference == null || "baseline".equals(snap.kind)) return;
        String ref = value(snap.reference, "name", "-");
        int outer = Math.max(1, height + (source == null ? 0
                : source.getStyle().getMarginTop() + source.getStyle().getMarginBottom()));
        int gap = standardGap();
        if ("alignStart".equals(snap.kind)) {
            refs[2] = ref; positions[2] = "1"; insets[2] = (-outer) + "px";
        } else if ("alignEnd".equals(snap.kind)) {
            refs[2] = ref; positions[2] = "0"; insets[2] = "0px";
        } else if ("after".equals(snap.kind)) {
            refs[2] = ref; positions[2] = "0"; insets[2] = (-(outer + gap)) + "px";
        } else if ("before".equals(snap.kind)) {
            refs[0] = ref; positions[0] = "0"; insets[0] = (-(outer + gap)) + "px";
        }
    }

    private void applyHorizontalPosition(GuiDocument targetDocument, SnapResult snap, String[] insets, String[] refs,
            String[] positions, int left, int right, int width, Component source) {
        String kind = snap == null ? null : snap.kind;
        String ref = snap == null || snap.reference == null ? "-" : value(snap.reference, "name", "-");
        insets[1] = "auto";
        if ("parentEnd".equals(kind)) {
            insets[3] = "auto"; insets[1] = "0px";
        } else if ("parentCenter".equals(kind)) {
            insets[3] = "50%"; targetDocument.setAttribute("guidedHorizontalAnchor", "0.5");
        } else if ("alignStart".equals(kind)) {
            insets[3] = "0px"; refs[3] = ref; positions[3] = "0";
        } else if ("alignEnd".equals(kind)) {
            // A right inset referenced to a component can clip preferred width to the reference
            // box. Express equal right edges as a left edge measured backwards from the
            // reference's right edge so a position-only drag cannot resize the component.
            int outerWidth = Math.max(1, width + (source == null ? 0
                    : source.getStyle().getMarginLeftNoRTL() + source.getStyle().getMarginRightNoRTL()));
            insets[3] = (-outerWidth) + "px"; insets[1] = "auto";
            refs[3] = ref; positions[3] = "1";
        } else if ("alignCenter".equals(kind)) {
            insets[3] = "0%"; refs[3] = ref; positions[3] = "0.5";
            targetDocument.setAttribute("guidedHorizontalAnchor", "0.5");
        } else if ("after".equals(kind)) {
            insets[3] = standardGap() + "px"; refs[3] = ref; positions[3] = "1";
        } else if ("before".equals(kind)) {
            insets[3] = "auto"; insets[1] = standardGap() + "px"; refs[1] = ref; positions[1] = "1";
        } else {
            insets[3] = Math.max(0, left) + "px";
        }
    }

    private void applyVerticalPosition(GuiDocument targetDocument, SnapResult snap, String[] insets, String[] refs,
            String[] positions, int top, int bottom, int height, Component source) {
        String kind = snap == null ? null : snap.kind;
        String ref = snap == null || snap.reference == null ? "-" : value(snap.reference, "name", "-");
        insets[2] = "auto";
        if ("parentEnd".equals(kind)) {
            insets[0] = "auto"; insets[2] = "0px";
        } else if ("parentCenter".equals(kind)) {
            insets[0] = "50%"; targetDocument.setAttribute("guidedVerticalAnchor", "0.5");
        } else if ("alignStart".equals(kind)) {
            insets[0] = "0px"; refs[0] = ref; positions[0] = "0";
        } else if ("alignEnd".equals(kind)) {
            int outerHeight = Math.max(1, height + (source == null ? 0
                    : source.getStyle().getMarginTop() + source.getStyle().getMarginBottom()));
            insets[0] = (-outerHeight) + "px"; insets[2] = "auto";
            refs[0] = ref; positions[0] = "1";
        } else if ("alignCenter".equals(kind)) {
            insets[0] = "0%"; refs[0] = ref; positions[0] = "0.5";
            targetDocument.setAttribute("guidedVerticalAnchor", "0.5");
        } else if ("after".equals(kind)) {
            insets[0] = standardGap() + "px"; refs[0] = ref; positions[0] = "1";
        } else if ("before".equals(kind)) {
            insets[0] = "auto"; insets[2] = standardGap() + "px"; refs[2] = ref; positions[2] = "1";
        } else if ("baseline".equals(kind)) {
            insets[0] = "baseline"; insets[2] = "auto"; refs[0] = ref; positions[0] = "0";
        } else {
            insets[0] = Math.max(0, top) + "px";
        }
    }

    private void applyMatchedWidth(Element match, String[] insets, String[] refs, String[] positions,
            int absoluteX, int width) {
        applyMatchedWidth(match, componentForElement(canvasHost, match), insets, refs, positions, absoluteX, width);
    }

    private void applyMatchedWidth(Element match, Component target, String[] insets, String[] refs,
            String[] positions, int absoluteX, int width) {
        if (target == null) return;
        int delta = absoluteX - target.getAbsoluteX();
        String name = value(match, "name", "-");
        insets[3] = delta + "px";
        insets[1] = (-delta) + "px";
        refs[3] = refs[1] = name;
        positions[3] = positions[1] = "0";
    }

    private void applyMatchedHeight(Element match, String[] insets, String[] refs, String[] positions,
            int absoluteY, int height) {
        applyMatchedHeight(match, componentForElement(canvasHost, match), insets, refs, positions, absoluteY, height);
    }

    private void applyMatchedHeight(Element match, Component target, String[] insets, String[] refs,
            String[] positions, int absoluteY, int height) {
        if (target == null) return;
        int delta = absoluteY - target.getAbsoluteY();
        String name = value(match, "name", "-");
        insets[0] = delta + "px";
        insets[2] = (-delta) + "px";
        refs[0] = refs[2] = name;
        positions[0] = positions[2] = "0";
    }

    private Element namedSibling(Element parent, String name) {
        if (parent == null || name == null || name.length() == 0) return null;
        for (Element child : componentChildren(parent)) if (name.equals(child.getAttribute("name"))) return child;
        return null;
    }

    /** A component cannot be anchored to one of its own transitive dependents. Before installing
     * such a relationship, rebase the first dependent in each path onto the parent at its current
     * pixels. This preserves the downstream UI exactly and makes the requested target a stable
     * anchor instead of relying on LayeredLayout's circular-reference fallback. */
    private void prepareGuidedCycleBreak(GuiDocument targetDocument, Element targetDragged, DropPlan plan) {
        String draggedName = value(targetDragged, "name", "");
        if (draggedName.length() == 0) return;
        Set<String> requestedReferences = new LinkedHashSet<>();
        if (plan.horizontalSnap != null && plan.horizontalSnap.reference != null) {
            requestedReferences.add(value(plan.horizontalSnap.reference, "name", ""));
        }
        if (plan.verticalSnap != null && plan.verticalSnap.reference != null) {
            requestedReferences.add(value(plan.verticalSnap.reference, "name", ""));
        }
        Set<String> rebase = new LinkedHashSet<>();
        for (String requested : requestedReferences) {
            if (requested.length() == 0 || !dependencyReachable(document, draggedName, requested)) continue;
            for (Element candidate : document.components()) {
                String candidateName = value(candidate, "name", "");
                if (!incomingReferenceNames(document, candidateName).contains(draggedName)) continue;
                if (requested.equals(candidateName) || dependencyReachable(document, candidateName, requested)) {
                    rebase.add(candidateName);
                }
            }
        }
        for (String dependentName : rebase) {
            freezeDependencyAtCurrentBounds(targetDocument, dependentName, draggedName);
        }
    }

    private boolean dependencyReachable(GuiDocument sourceDocument, String from, String to) {
        if (from.equals(to)) return true;
        List<String> pending = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        pending.add(from);
        while (!pending.isEmpty()) {
            String current = pending.remove(0);
            if (!visited.add(current)) continue;
            for (Element candidate : sourceDocument.components()) {
                String name = value(candidate, "name", "");
                if (name.length() == 0 || !incomingReferenceNames(sourceDocument, name).contains(current)) continue;
                if (to.equals(name)) return true;
                pending.add(name);
            }
        }
        return false;
    }

    private void freezeDependencyAtCurrentBounds(GuiDocument targetDocument, String dependentName,
            String removedReference) {
        Element original = findElementNamed(document, dependentName);
        Element target = findElementNamed(targetDocument, dependentName);
        Component preview = original == null ? null : componentForElement(canvasHost, original);
        if (target == null || preview == null || preview.getParent() == null) return;
        Container parent = preview.getParent();
        String[] insets = GuidedLayoutSupport.insetValues(target);
        String[] refs = GuidedLayoutSupport.referenceNames(target);
        String[] positions = GuidedLayoutSupport.referencePositions(target);
        boolean horizontal = removedReference.equals(refs[1]) || removedReference.equals(refs[3])
                || removedReference.equals(value(target, "guidedMatchWidth", ""));
        boolean vertical = removedReference.equals(refs[0]) || removedReference.equals(refs[2])
                || removedReference.equals(value(target, "guidedMatchHeight", ""));
        targetDocument.select(target);
        if (horizontal) {
            insets[3] = Math.max(0, preview.getAbsoluteX() - contentX(parent)
                    - preview.getStyle().getMarginLeftNoRTL()) + "px";
            insets[1] = "auto";
            clearHorizontalReferences(refs, positions);
            targetDocument.setAttribute("guidedHorizontalAnchor", null);
            targetDocument.setAttribute("guidedMatchWidth", null);
            // Detaching an axis must freeze the complete rendered rectangle. A component
            // can currently render larger than its nominal preferred size because of theme
            // metrics, margins, or its old relationship. Falling back to preferred size here
            // resized it and pulled every downstream dependent along with it.
            targetDocument.setAttribute("guidedHorizontalSize", GuidedLayoutSupport.FIXED);
            targetDocument.setAttribute("guidedPreferredWidth", String.valueOf(Math.max(1, preview.getWidth())));
        }
        if (vertical) {
            insets[0] = Math.max(0, preview.getAbsoluteY() - contentY(parent)
                    - preview.getStyle().getMarginTop()) + "px";
            insets[2] = "auto";
            clearVerticalReferences(refs, positions);
            targetDocument.setAttribute("guidedVerticalAnchor", null);
            targetDocument.setAttribute("guidedMatchHeight", null);
            targetDocument.setAttribute("guidedVerticalSize", GuidedLayoutSupport.FIXED);
            targetDocument.setAttribute("guidedPreferredHeight", String.valueOf(Math.max(1, preview.getHeight())));
        }
        targetDocument.setAttribute("layeredInsets", GuidedLayoutSupport.joinInsets(
                insets[0], insets[1], insets[2], insets[3]));
        targetDocument.setAttribute("guidedReferences", GuidedLayoutSupport.joinReferences(
                refs[0], refs[1], refs[2], refs[3]));
        targetDocument.setAttribute("guidedReferencePositions", GuidedLayoutSupport.joinPositions(
                positions[0], positions[1], positions[2], positions[3]));
    }

    private void showGuidedDropSimulation(Element dragged, DropPlan plan, Container parent, Component source) {
        GuidedSimulation simulation = simulateGuidedDrop(dragged, plan, parent, source);
        showSimulation(simulation);
    }

    private void showGuidedResizeSimulation(Element element, Container parent, Component source,
            ResizePlan plan, int edges) {
        GuidedSimulation simulation = simulateGuidedResize(element, parent, source, plan, edges);
        showSimulation(simulation);
    }

    private void showSimulation(GuidedSimulation simulation) {
        if (dragGuideOverlay == null) return;
        if (simulation == null) dragGuideOverlay.clearSimulation();
        else dragGuideOverlay.showSimulation(simulation.items, simulation.links, simulation.summary);
    }

    /** Runs a proposed guided drop against an isolated XML clone. The active document and undo
     * history remain untouched until pointer release. */
    GuidedSimulation simulateGuidedDrop(Element dragged, DropPlan plan, Container parent, Component source) {
        if (document == null || previewRoot == null || dragged == null || plan == null || !plan.valid
                || !"LayeredLayout".equals(plan.layout)) return null;
        GuiDocument simulated = GuiDocument.parse(document.path(), document.toXml());
        Element simulatedDragged = equivalentElement(simulated, dragged);
        Element simulatedParent = equivalentElement(simulated, plan.parent);
        if (simulatedDragged == null || simulatedParent == null) return null;
        if (simulated.parentOf(simulatedDragged) != simulatedParent) {
            Element simulatedTarget = equivalentElement(simulated, plan.target);
            simulated.select(simulatedDragged);
            if (simulatedTarget == null || !simulated.moveSelectedTo(simulatedTarget, plan.after)) return null;
        }
        int width = plan.snapW > 0 ? plan.snapW : source == null || source.getWidth() < 1
                ? Math.max(1, parent.getWidth() / 5) : source.getWidth();
        int height = plan.snapH > 0 ? plan.snapH : source == null || source.getHeight() < 1
                ? Math.max(1, parent.getHeight() / 10) : source.getHeight();
        prepareGuidedCycleBreak(simulated, simulatedDragged, plan);
        persistGuidedConstraints(simulated, simulatedDragged, plan, parent, source, width, height);
        return buildGuidedSimulation(simulated, value(dragged, "name", "component"));
    }

    /** Runs a proposed resize against an isolated XML clone, including all existing match and
     * positional references so its full cascade can be painted before commit. */
    GuidedSimulation simulateGuidedResize(Element element, Container parent, Component source,
            ResizePlan plan, int edges) {
        if (document == null || previewRoot == null || element == null || plan == null) return null;
        GuiDocument simulated = GuiDocument.parse(document.path(), document.toXml());
        Element simulatedElement = equivalentElement(simulated, element);
        if (simulatedElement == null) return null;
        boolean group = selectedElements.size() > 1 && selectedElements.contains(element)
                && selectedElementsShareGuidedParent();
        commitGuidedResize(simulated, simulatedElement, parent, source,
                group ? fixedReferenceResizePlan(plan, edges) : plan, edges);
        if (group) {
            for (Element selected : selectedElements) {
                if (selected == element) continue;
                Element simulatedSelected = equivalentElement(simulated, selected);
                Component preview = componentForElement(canvasHost, selected);
                if (simulatedSelected == null || preview == null) continue;
                if ((edges & 3) != 0) {
                    applyLayeredRelationship(simulated, simulatedSelected, simulatedElement, "matchWidth", preview);
                }
                if ((edges & 12) != 0) {
                    applyLayeredRelationship(simulated, simulatedSelected, simulatedElement, "matchHeight", preview);
                }
            }
            simulated.select(simulatedElement);
        }
        return buildGuidedSimulation(simulated, value(element, "name", "component"));
    }

    private GuidedSimulation buildGuidedSimulation(GuiDocument simulated, String activeName) {
        Component originalRoot = previewRoot;
        if (!(originalRoot instanceof Container) || originalRoot.getWidth() < 1 || originalRoot.getHeight() < 1) return null;
        Map<String, PreviewRect> before = previewBounds(document, (Container) originalRoot);
        Component rendered;
        try {
            if (projectTheme != null) UIManager.getInstance().setThemeProps(projectTheme);
            rendered = ComponentPreviewFactory.create(simulated.root(), null, simulationSelectionHandler());
            if (projectTheme != null) rendered.refreshTheme();
        } finally {
            if (builderTheme != null) UIManager.getInstance().setThemeProps(builderTheme);
        }
        ComponentPreviewFactory.stabilizeDesignStyles(rendered);
        rendered.setX(originalRoot.getAbsoluteX());
        rendered.setY(originalRoot.getAbsoluteY());
        rendered.setWidth(originalRoot.getWidth());
        rendered.setHeight(originalRoot.getHeight());
        layoutSimulation((Container) rendered);
        Map<String, PreviewRect> after = previewBounds(simulated, (Container) rendered);
        Set<String> changed = new LinkedHashSet<>();
        List<DragGuideOverlay.GlassItem> items = new ArrayList<>();
        for (Map.Entry<String, PreviewRect> entry : after.entrySet()) {
            String name = entry.getKey();
            PreviewRect oldRect = before.get(name);
            PreviewRect newRect = entry.getValue();
            if (oldRect == null) continue;
            boolean activeItem = name.equals(activeName);
            if (!activeItem && !oldRect.differs(newRect)) continue;
            if (oldRect.differs(newRect)) changed.add(name);
            items.add(new DragGuideOverlay.GlassItem(name,
                    oldRect.x, oldRect.y, oldRect.width, oldRect.height,
                    newRect.x, newRect.y, newRect.width, newRect.height, activeItem));
        }
        List<DragGuideOverlay.DependencyLink> links = dependencyLinks(simulated, after, changed, activeName);
        Map<String, String[]> originalPairs = dependencyPairs(document);
        Map<String, String[]> simulatedPairs = dependencyPairs(simulated);
        List<String> rebased = new ArrayList<>();
        Set<String> detachedFromActive = new LinkedHashSet<>();
        for (Map.Entry<String, String[]> entry : originalPairs.entrySet()) {
            if (simulatedPairs.containsKey(entry.getKey())) continue;
            String reference = entry.getValue()[0];
            String dependent = entry.getValue()[1];
            PreviewRect referenceBefore = before.get(reference);
            PreviewRect dependentAfter = after.get(dependent);
            if (referenceBefore != null && dependentAfter != null) {
                links.add(new DragGuideOverlay.DependencyLink(reference, dependent,
                        referenceBefore.x + referenceBefore.width / 2,
                        referenceBefore.y + referenceBefore.height / 2,
                        dependentAfter.x + dependentAfter.width / 2,
                        dependentAfter.y + dependentAfter.height / 2, true));
            }
            if (activeName.equals(dependent)) detachedFromActive.add(reference);
            else rebased.add(dependent + " from " + reference);
        }
        List<String> affected = new ArrayList<>();
        for (String name : changed) if (!name.equals(activeName)) affected.add(name);
        String summary = "Preview: " + activeName;
        if (changed.contains(activeName)) summary += " changes";
        if (!affected.isEmpty()) summary += " • also affects " + joinNames(affected);
        else summary += " • no dependent components change";
        if (!detachedFromActive.isEmpty()) summary += " • detaches from " + joinNames(new ArrayList<>(detachedFromActive));
        if (!rebased.isEmpty()) summary += " • keeps in place " + joinNames(rebased);
        return new GuidedSimulation(simulated, items, links, summary, changed);
    }

    private Map<String, String[]> dependencyPairs(GuiDocument targetDocument) {
        Map<String, String[]> result = new LinkedHashMap<>();
        for (Element dependent : targetDocument.components()) {
            String dependentName = value(dependent, "name", "");
            if (dependentName.length() == 0) continue;
            for (String reference : incomingReferenceNames(targetDocument, dependentName)) {
                String key = reference + "\n" + dependentName;
                result.put(key, new String[]{reference, dependentName});
            }
        }
        return result;
    }

    private Set<String> incomingReferenceNames(GuiDocument targetDocument, String componentName) {
        Set<String> result = new LinkedHashSet<>();
        Element element = findElementNamed(targetDocument, componentName);
        if (element == null) return result;
        for (String reference : GuidedLayoutSupport.referenceNames(element)) {
            if (reference != null && reference.length() > 0 && !"-".equals(reference)) result.add(reference);
        }
        String matchWidth = value(element, "guidedMatchWidth", "");
        String matchHeight = value(element, "guidedMatchHeight", "");
        if (matchWidth.length() > 0) result.add(matchWidth);
        if (matchHeight.length() > 0) result.add(matchHeight);
        return result;
    }

    private String joinNames(List<String> names) {
        StringBuilder out = new StringBuilder();
        for (String name : names) {
            if (out.length() > 0) out.append(", ");
            out.append(name);
        }
        return out.toString();
    }

    private List<DragGuideOverlay.DependencyLink> dependencyLinks(GuiDocument simulated,
            Map<String, PreviewRect> bounds, Set<String> changed, String activeName) {
        List<DragGuideOverlay.DependencyLink> result = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (Element dependent : simulated.components()) {
            String dependentName = value(dependent, "name", "");
            if (dependentName.length() == 0) continue;
            Set<String> references = new LinkedHashSet<>();
            for (String reference : GuidedLayoutSupport.referenceNames(dependent)) {
                if (reference != null && reference.length() > 0 && !"-".equals(reference)) references.add(reference);
            }
            String matchWidth = value(dependent, "guidedMatchWidth", "");
            String matchHeight = value(dependent, "guidedMatchHeight", "");
            if (matchWidth.length() > 0) references.add(matchWidth);
            if (matchHeight.length() > 0) references.add(matchHeight);
            for (String reference : references) {
                if (!changed.contains(dependentName) && !changed.contains(reference)
                        && !activeName.equals(dependentName) && !activeName.equals(reference)) continue;
                PreviewRect from = bounds.get(reference);
                PreviewRect to = bounds.get(dependentName);
                String key = reference + "\n" + dependentName;
                if (from == null || to == null || !unique.add(key)) continue;
                result.add(new DragGuideOverlay.DependencyLink(reference, dependentName,
                        from.x + from.width / 2, from.y + from.height / 2,
                        to.x + to.width / 2, to.y + to.height / 2));
            }
        }
        return result;
    }

    private Map<String, PreviewRect> previewBounds(GuiDocument targetDocument, Container root) {
        Map<String, PreviewRect> result = new LinkedHashMap<>();
        for (Element element : targetDocument.components()) {
            if (element == targetDocument.root()) continue;
            String name = value(element, "name", "");
            Component component = componentForElement(root, element);
            if (name.length() == 0 || component == null) continue;
            result.put(name, new PreviewRect(component.getAbsoluteX(), component.getAbsoluteY(),
                    component.getWidth(), component.getHeight()));
        }
        return result;
    }

    private void layoutSimulation(Container container) {
        container.layoutContainer();
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component child = container.getComponentAt(i);
            if (child instanceof Container) layoutSimulation(((Container) child));
        }
    }

    private ComponentPreviewFactory.SelectionHandler simulationSelectionHandler() {
        return new ComponentPreviewFactory.SelectionHandler() {
            @Override public void selected(Element element) { }
            @Override public void dragPressed(Element element, Component source, int x, int y) { }
            @Override public boolean isDragActive() { return false; }
            @Override public void editContent(Element element) { }
        };
    }

    private Element equivalentElement(GuiDocument targetDocument, Element original) {
        if (original == null) return null;
        if (document != null && original == document.root()) return targetDocument.root();
        String name = value(original, "name", "");
        if (name.length() == 0) return null;
        return findElementNamed(targetDocument, name);
    }

    private Element findElementNamed(GuiDocument targetDocument, String name) {
        if (targetDocument == null || name == null || name.length() == 0) return null;
        for (Element candidate : targetDocument.components()) {
            if (name.equals(candidate.getAttribute("name"))) return candidate;
        }
        return null;
    }

    private void clearHorizontalReferences(String[] refs, String[] positions) {
        refs[1] = refs[3] = "-"; positions[1] = positions[3] = "0";
    }

    private void clearVerticalReferences(String[] refs, String[] positions) {
        refs[0] = refs[2] = "-"; positions[0] = positions[2] = "0";
    }

    private int standardGap() { return Math.max(6, Display.getInstance().convertToPixels(1)); }

    /**
     * Gives every child of a table a cell, without ever moving a child that already has one.
     *
     * <p>Reassigning cells from sibling order is what an implicit, code-first TableLayout does, and
     * it is wrong for a designer: dropping one component then renumbers every other cell, so the
     * whole table jumps to a layout nobody asked for. Cells are the user's placement decision, so
     * only components that have no cell yet, or whose cell collides with an earlier sibling, are
     * assigned one, and they take the first free cell in row-major order.
     */
    private void normalizeTableCells(Element parent) {
        int columns = Math.max(1, integer(value(parent, "tableLayoutColumns", "2"), 2));
        List<Element> children = componentChildren(parent);
        Set<String> taken = new LinkedHashSet<>();
        List<Element> unplaced = new ArrayList<>();
        for (Element child : children) {
            Integer row = parseInteger(child.getAttribute("tableRow"));
            Integer column = parseInteger(child.getAttribute("tableColumn"));
            if (row == null || column == null || row.intValue() < 0 || column.intValue() < 0
                    || column.intValue() >= columns || !taken.add(row + ":" + column)) {
                unplaced.add(child);
            }
        }
        int cursor = 0;
        for (Element child : unplaced) {
            while (taken.contains((cursor / columns) + ":" + (cursor % columns))) cursor++;
            taken.add((cursor / columns) + ":" + (cursor % columns));
            document.select(child);
            document.setAttribute("tableRow", String.valueOf(cursor / columns));
            document.setAttribute("tableColumn", String.valueOf(cursor % columns));
        }
        growTableToFit(parent, taken);
    }

    /** Widens the declared row count so no assigned cell falls outside the table and disappears. */
    private void growTableToFit(Element parent, Set<String> occupiedCells) {
        int requiredRows = 1;
        for (String cell : occupiedCells) {
            Integer row = parseInteger(cell.substring(0, cell.indexOf(':')));
            if (row != null) requiredRows = Math.max(requiredRows, row.intValue() + 1);
        }
        if (integer(value(parent, "tableLayoutRows", "2"), 2) < requiredRows) {
            document.select(parent);
            document.setAttribute("tableLayoutRows", String.valueOf(requiredRows));
        }
    }

    /** The child occupying {@code row}/{@code column}, or null. */
    /**
     * The child occupying a cell, counting the rectangle a span covers rather than only the cell a
     * child starts in. Testing the anchor alone treated a covered cell as empty, so a drop there
     * was assigned a cell the span already owned and the rebuild overlapped or failed.
     *
     * @param parent the table
     * @param row the row being dropped on
     * @param column the column being dropped on
     * @param ignored a child to skip, typically the one being dragged
     * @return the child covering that cell, or null when it is genuinely free
     */
    private Element childAtTableCell(Element parent, int row, int column, Element ignored) {
        for (Element child : componentChildren(parent)) {
            if (child == ignored) continue;
            Integer childRow = parseInteger(child.getAttribute("tableRow"));
            Integer childColumn = parseInteger(child.getAttribute("tableColumn"));
            if (childRow == null || childColumn == null) continue;
            int rowSpan = Math.max(1, integer(child.getAttribute("tableVerticalSpan"), 1));
            int columnSpan = Math.max(1, integer(child.getAttribute("tableHorizontalSpan"), 1));
            if (row >= childRow.intValue() && row < childRow.intValue() + rowSpan
                    && column >= childColumn.intValue() && column < childColumn.intValue() + columnSpan) {
                return child;
            }
        }
        return null;
    }

    /**
     * The cell under the pointer, derived from the parent preview's own geometry rather than from
     * sibling order, so a drop lands where it was aimed.
     */
    int[] tableCellAt(Element parent, Component parentPreview, int x, int y) {
        int columns = Math.max(1, integer(value(parent, "tableLayoutColumns", "2"), 2));
        int rows = Math.max(1, integer(value(parent, "tableLayoutRows", "2"), 2));
        if (parentPreview == null || parentPreview.getWidth() < 1 || parentPreview.getHeight() < 1) {
            return new int[]{0, 0};
        }
        int width = Math.max(1, contentWidth(parentPreview));
        int height = Math.max(1, contentHeight(parentPreview));
        int column = ((x - contentX(parentPreview)) * columns) / width;
        int row = ((y - contentY(parentPreview)) * rows) / height;
        return new int[]{Math.max(0, Math.min(rows - 1, row)), Math.max(0, Math.min(columns - 1, column))};
    }

    private List<Element> componentChildren(Element parent) {
        List<Element> children = new ArrayList<>();
        if (parent == null) return children;
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object child = parent.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) children.add(((Element) child));
        }
        return children;
    }

    static final class DropPlan {
        GuiDocument document;
        Element target;
        Element parent;
        Element occupied;
        String layout;
        String constraint;
        String message;
        boolean after;
        boolean valid;
        int snapX;
        int snapY;
        int snapW;
        int snapH;
        String snapDescription;
        SnapResult horizontalSnap;
        SnapResult verticalSnap;
        /** {row, column} for a TableLayout drop; null for every other layout. */
        int[] tableCell;
    }

    static final class GuidedSimulation {
        final GuiDocument document;
        final List<DragGuideOverlay.GlassItem> items;
        final List<DragGuideOverlay.DependencyLink> links;
        final String summary;
        final Set<String> changedNames;

        GuidedSimulation(GuiDocument document, List<DragGuideOverlay.GlassItem> items,
                List<DragGuideOverlay.DependencyLink> links, String summary, Set<String> changedNames) {
            this.document = document;
            this.items = items;
            this.links = links;
            this.summary = summary;
            this.changedNames = changedNames;
        }
    }

    private static final class GroupMoveGeometry {
        final Container parent;
        final Map<Element, Component> previews;
        final int dx;
        final int dy;

        GroupMoveGeometry(Container parent, Map<Element, Component> previews, int dx, int dy) {
            this.parent = parent;
            this.previews = previews;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private static final class PreviewRect {
        final int x, y, width, height;
        PreviewRect(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
        boolean differs(PreviewRect other) {
            return other == null || Math.abs(x - other.x) > 1 || Math.abs(y - other.y) > 1
                    || Math.abs(width - other.width) > 1 || Math.abs(height - other.height) > 1;
        }
    }

    private static final class ResizeHit {
        final Element element;
        final Component component;
        final int edges;

        ResizeHit(Element element, Component component, int edges) {
            this.element = element;
            this.component = component;
            this.edges = edges;
        }
    }

    static final class ResizePlan {
        int x;
        int y;
        int width;
        int height;
        Element matchWidth;
        Element matchHeight;
        String horizontalDescription;
        String verticalDescription;
        String description;
        ResizePlan(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    static final class SnapResult {
        final int position;
        final String description;
        final Element reference;
        final String kind;
        SnapResult(int position, String description, Element reference, String kind) {
            this.position = position;
            this.description = description;
            this.reference = reference;
            this.kind = kind;
        }
    }

    private static final class SnapCandidate {
        final int position;
        final String description;
        final Element reference;
        final String kind;
        SnapCandidate(int position, String description, Element reference, String kind) {
            this.position = position;
            this.description = description;
            this.reference = reference;
            this.kind = kind;
        }
    }

    private static int contentX(Component component) {
        return component.getAbsoluteX() + component.getStyle().getPaddingLeftNoRTL();
    }

    private static int contentY(Component component) {
        return component.getAbsoluteY() + component.getStyle().getPaddingTop();
    }

    private static int contentWidth(Component component) {
        return Math.max(1, component.getWidth() - component.getStyle().getPaddingLeftNoRTL()
                - component.getStyle().getPaddingRightNoRTL());
    }

    private static int contentHeight(Component component) {
        return Math.max(1, component.getHeight() - component.getStyle().getPaddingTop()
                - component.getStyle().getPaddingBottom());
    }

    private Component componentForElement(Container root, Element element) {
        if (element == null) return null;
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            if (component.getClientProperty("gui.element") == element) return component;
            if (component instanceof Container) {
                Component nested = componentForElement(((Container) component), element);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    Element elementAt(Container root, int x, int y) {
        for (int i = root.getComponentCount() - 1; i >= 0; i--) {
            Component component = root.getComponentAt(i);
            int ax = component.getAbsoluteX();
            int ay = component.getAbsoluteY();
            if (x < ax || x > ax + component.getWidth() || y < ay || y > ay + component.getHeight()) continue;
            if (component instanceof Container) {
                Element nested = elementAt(((Container) component), x, y);
                if (nested != null) return nested;
            }
            Object element = component.getClientProperty("gui.element");
            if (element instanceof Element) return ((Element) element);
            Object dropTarget = component.getClientProperty("gui.dropTargetElement");
            if (dropTarget instanceof Element) return ((Element) dropTarget);
        }
        return null;
    }

    private Element activePreviewElementAt(int x, int y) {
        if (document == null || canvasHost == null) return null;
        Element hit = elementAt(canvasHost, x, y);
        return document.containsElement(hit) ? hit : null;
    }

    private Element designerDropTargetAt(Element dragged, int x, int y) {
        return normalizeDesignerDropTarget(dragged, activePreviewElementAt(x, y));
    }

    Element normalizeDesignerDropTarget(Element dragged, Element target) {
        if (dragged == null || target == null) return target;
        if (target == dragged || containsElementIdentity(dragged, target)) return document.parentOf(dragged);
        return target;
    }

    private boolean containsElementIdentity(Element ancestor, Element candidate) {
        if (ancestor == null || candidate == null) return false;
        for (int i = 0; i < ancestor.getNumChildren(); i++) {
            Object child = ancestor.getChildAt(i);
            if (child == candidate) return true;
            if (child instanceof Element && containsElementIdentity(((Element) child), candidate)) return true;
        }
        return false;
    }

    boolean isActiveDocumentElement(Element element) {
        return document != null && document.containsElement(element);
    }

    List<Element> selectedElementsSnapshot() {
        return new ArrayList<>(selectedElements);
    }

    Container canvasHostForTest() { return canvasHost; }

    CodeEditor activeEditorForTest() { return activeCodeEditor; }

    /** Recompiles theme.css from disk and pushes it at the preview, as a live CSS edit does. */
    boolean reloadProjectCssForTest() {
        try {
            return applyProjectCss(ProjectIO.read(binding.cssFile()), new CodeEditor("css", ""));
        } catch (IOException ex) {
            return false;
        }
    }

    GuiDocument documentForTest() { return document; }

    private void cancelDesignerDrag() {
        designerDraggedElement = null;
        designerDragDocument = null;
        designerPaletteType = null;
        designerDragSource = null;
        designerSuppressAction = null;
        designerDragArmed = false;
        designerDragActive = false;
        guidedResizeElement = null;
        guidedResizeSource = null;
        guidedResizeArmed = false;
        guidedResizeActive = false;
        activeResizePlan = null;
        hideDropGuide();
    }

    private void refreshGuidedSelectionOverlay() {
        if (dragGuideOverlay == null || document == null || canvasHost == null) return;
        normalizeSelection();
        List<Component> components = new ArrayList<>();
        for (Element element : selectedElements) {
            Component component = componentForElement(canvasHost, element);
            if (component != null && "LayeredLayout".equals(document.parentLayout(element))) components.add(component);
        }
        Component primary = document.selected() == null ? null : componentForElement(canvasHost, document.selected());
        if (!components.isEmpty()) {
            dragGuideOverlay.showSelections(components, primary);
        } else {
            dragGuideOverlay.clearSelection();
        }
        refreshSelectionActions(components);
    }

    private void normalizeSelection() {
        if (document == null) {
            selectedElements.clear();
            return;
        }
        rebindSelectionToDocument();
        selectedElements.removeIf(element -> !document.containsElement(element));
        Element primary = document.selected();
        if (selectedElements.isEmpty() && primary != null && primary != document.root()) selectedElements.add(primary);
        if (!selectedElements.isEmpty() && !selectedElements.contains(primary)) {
            document.select(selectedElements.iterator().next());
        }
    }

    /**
     * Re-resolves the selection against the live tree by component name.
     *
     * <p>Undo and redo restore by reparsing, so every Element identity changes even though the
     * form is unchanged. Everything the editor holds -- the multi-selection here, and the drag
     * state cleared alongside it -- then refers to a tree the document has thrown away. Dropping
     * those entries would silently deselect after every undo; names are unique in a document, so
     * they survive the reparse and are the right key to re-resolve against.
     */
    private void rebindSelectionToDocument() {
        if (selectedElements.isEmpty()) return;
        List<Element> rebound = new ArrayList<>();
        for (Element element : selectedElements) {
            if (document.containsElement(element)) {
                rebound.add(element);
                continue;
            }
            String name = element.getAttribute("name");
            Element live = name == null ? null : findElementNamed(document, name);
            if (live != null) rebound.add(live);
        }
        if (rebound.equals(new ArrayList<>(selectedElements))) return;
        selectedElements.clear();
        selectedElements.addAll(rebound);
    }

    private void selectElement(Element element, boolean additive, String source) {
        if (document == null || element == null || !document.containsElement(element)) return;
        if (element == document.root()) {
            selectedElements.clear();
            document.select(element);
        } else if (additive) {
            if (selectedElements.contains(element)) {
                boolean removedReference = element == document.selected();
                selectedElements.remove(element);
                if (selectedElements.isEmpty()) document.select(document.root());
                else if (removedReference) document.select(selectedElements.iterator().next());
            } else {
                Element reference = selectedElements.contains(document.selected()) ? document.selected() : null;
                selectedElements.add(element);
                // Modifier-click grows the group without silently changing the component that
                // alignment and size actions use as their reference.
                document.select(reference == null ? element : reference);
            }
        } else if (selectedElements.size() > 1 && selectedElements.contains(element)) {
            // Keep the group intact when a selected member becomes the drag handle. Modifier
            // clicks still toggle membership; clicking an unselected component starts a new group.
            document.select(element);
        } else {
            selectedElements.clear();
            selectedElements.add(element);
            document.select(element);
        }
        recordAction("selection_changed", "component", value(element, "name", value(element, "type", "component")),
                "source", source, "additive", Boolean.valueOf(additive), "count", Integer.valueOf(selectedElements.size()));
        if (inspectorHost != null) refreshInspector();
        if (hierarchyPanel != null) refreshHierarchy();
        refreshGuidedSelectionOverlay();
        setStatus(selectedElements.size() > 1 ? selectedElements.size() + " components selected • reference: "
                + value(document.selected(), "name", "component")
                + " • click a selected component to change the reference"
                : selectedElements.isEmpty() ? "Selection cleared"
                : "Selected " + value(document.selected(), "name", value(document.selected(), "type", "component")) + " • drag to reposition");
    }

    private void refreshSelectionActions(List<Component> selectedComponents) {
        if (selectionActions == null || canvasOverlayHost == null) return;
        boolean show = selectedComponents != null && selectedComponents.size() > 1
                && selectedElementsShareGuidedParent();
        selectionActions.setVisible(show);
        if (!show) return;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (Component component : selectedComponents) {
            minX = Math.min(minX, component.getAbsoluteX());
            minY = Math.min(minY, component.getAbsoluteY());
            maxX = Math.max(maxX, component.getAbsoluteX() + component.getWidth());
        }
        int paletteWidth = Math.max(1, selectionActions.getPreferredW());
        int paletteHeight = Math.max(1, selectionActions.getPreferredH());
        int hostX = canvasOverlayHost.getAbsoluteX();
        int hostY = canvasOverlayHost.getAbsoluteY();
        int beside = maxX - hostX + 8;
        boolean fitsBeside = beside + paletteWidth <= canvasOverlayHost.getWidth() - 4;
        int left = fitsBeside ? beside : Math.max(4, Math.min(canvasOverlayHost.getWidth() - paletteWidth - 4,
                (minX + maxX - paletteWidth) / 2 - hostX));
        int top = fitsBeside ? Math.max(4, minY - hostY)
                : Math.max(4, minY - hostY - paletteHeight - 8);
        ((LayeredLayout) canvasOverlayHost.getLayout()).setInsets(selectionActions,
                top + "px auto auto " + left + "px");
        selectionActions.revalidate();
    }

    private boolean selectedElementsShareGuidedParent() {
        Element parent = null;
        for (Element element : selectedElements) {
            Element candidate = document.parentOf(element);
            if (candidate == null || !"LayeredLayout".equals(value(candidate, "layout", "BoxLayout"))) return false;
            if (parent == null) parent = candidate;
            else if (parent != candidate) return false;
        }
        return parent != null;
    }

    void autoScrollDuringDrag(int x, int y) {
        if (canvasHost == null) return;
        Component hit = deepestComponentAt(canvasHost, x, y);
        Container scrollable = hit instanceof Container ? (Container) hit : hit == null ? null : hit.getParent();
        while (scrollable != null && scrollable != canvasHost
                && !scrollable.isScrollableX() && !scrollable.isScrollableY()) scrollable = scrollable.getParent();
        if (scrollable == null || scrollable == canvasHost) return;
        int edge = Math.max(24, Display.getInstance().convertToPixels(3));
        int stepX = Math.max(18, scrollable.getWidth() / 12);
        int stepY = Math.max(18, scrollable.getHeight() / 12);
        if (scrollable.isScrollableX()) {
            if (x < scrollable.getAbsoluteX() + edge) {
                scrollable.scrollRectToVisible(Math.max(0, scrollable.getScrollX() - stepX),
                        scrollable.getScrollY(), 1, 1, null);
            } else if (x > scrollable.getAbsoluteX() + scrollable.getWidth() - edge) {
                scrollable.scrollRectToVisible(scrollable.getScrollX() + scrollable.getWidth() + stepX,
                        scrollable.getScrollY(), 1, 1, null);
            }
        }
        if (scrollable.isScrollableY()) {
            if (y < scrollable.getAbsoluteY() + edge) {
                scrollable.scrollRectToVisible(scrollable.getScrollX(),
                        Math.max(0, scrollable.getScrollY() - stepY), 1, 1, null);
            } else if (y > scrollable.getAbsoluteY() + scrollable.getHeight() - edge) {
                scrollable.scrollRectToVisible(scrollable.getScrollX(),
                        scrollable.getScrollY() + scrollable.getHeight() + stepY, 1, 1, null);
            }
        }
    }

    private Component deepestComponentAt(Container root, int x, int y) {
        for (int i = root.getComponentCount() - 1; i >= 0; i--) {
            Component component = root.getComponentAt(i);
            if (x < component.getAbsoluteX() || x > component.getAbsoluteX() + component.getWidth()
                    || y < component.getAbsoluteY() || y > component.getAbsoluteY() + component.getHeight()) continue;
            if (component instanceof Container) {
                Component nested = deepestComponentAt(((Container) component), x, y);
                if (nested != null) return nested;
            }
            return component;
        }
        return null;
    }

    private void refreshInspector() {
        inspectorHost.removeAll();
        Element selected = document.selected();
        Tabs tabs = new Tabs();
        tabs.setSwipeActivated(false);
        tabs.setUIID("BuilderInspectorTabs");
        tabs.addTab("Properties", propertiesTab(selected));
        tabs.addTab("Layout", layoutTab(selected));
        tabs.addTab("Events", eventsTab(selected));
        tabs.setSelectedIndex(Math.max(0, Math.min(2, inspectorTabIndex)), false);
        tabs.addSelectionListener((oldIndex, newIndex) -> inspectorTabIndex = newIndex);
        inspectorHost.add(BorderLayout.CENTER, tabs);
        inspectorHost.revalidate();
    }

    private Component propertiesTab(Element element) {
        Container fields = inspectorFields();
        String type = document.attribute("type", "Component");
        fields.add(new Label(type, "BuilderInspectorComponent"));
        fields.add(nameField());
        if (isFormLike(type)) {
            fields.add(propertyField("Title", "title"));
            fields.add(bindingStrategyPicker());
        }
        else if (hasText(type)) fields.add(propertyField("Text", "text"));
        if ("TextField".equals(type) || "TextArea".equals(type)) fields.add(propertyField("Hint", "hint"));
        fields.add(propertyField("UIID (CSS selector)", "uiid", document.effectiveUiid(element)));
        fields.add(booleanProperty("Enabled", "enabled", true));
        fields.add(booleanProperty("Visible", "visible", true));
        fields.add(booleanProperty("Right-to-left", "rtl", false));
        if (hasText(type)) {
            fields.add(propertyField("Icon/text gap", "gap", "2"));
            fields.add(pickerProperty("Alignment", "alignment", new String[]{"left", "center", "right"}, "left"));
            fields.add(booleanProperty("Ticker when clipped", "tickerEnabled", false));
        }
        if ("Button".equals(type)) fields.add(booleanProperty("Toggle button", "toggle", false));
        if ("CheckBox".equals(type) || "RadioButton".equals(type)) fields.add(booleanProperty("Selected", "selected", false));
        if ("TextField".equals(type) || "TextArea".equals(type)) {
            fields.add(propertyField("Columns", "columns", "12"));
            fields.add(propertyField("Maximum length", "maxSize", "0"));
            fields.add(booleanProperty("Editable", "editable", true));
            fields.add(booleanProperty("Grow by content", "growByContent", true));
            fields.add(pickerProperty("Input constraint", "constraint",
                    new String[]{"ANY", "EMAILADDR", "PASSWORD", "NUMERIC", "URL"}, "ANY"));
        }
        if ("TextArea".equals(type)) fields.add(propertyField("Rows", "rows", "3"));
        if ("Slider".equals(type)) {
            fields.add(propertyField("Minimum", "minValue", "0"));
            fields.add(propertyField("Maximum", "maxValue", "100"));
            fields.add(propertyField("Progress", "progress", "50"));
            fields.add(booleanProperty("Editable", "editable", false));
            fields.add(booleanProperty("Infinite progress", "infinite", false));
        }
        if ("Tabs".equals(type)) {
            fields.add(propertyField("Selected tab index", "selectedIndex", "0"));
            fields.add(pickerProperty("Tab placement", "tabPlacement",
                    new String[]{"top", "bottom", "left", "right"}, "top"));
        }
        if (GuiDocument.acceptsChildren(element)) {
            fields.add(booleanProperty("Scroll horizontally", "scrollableX", false));
            fields.add(booleanProperty("Scroll vertically", "scrollableY", false));
        }
        if (hasText(type) || isFormLike(type)) {
            Button inline = new Button("Edit content in place", material(FontImage.MATERIAL_EDIT, "BuilderInlineIcon"));
            inline.setUIID("BuilderSecondaryAction");
            inline.addActionListener(e -> editSelectedContent());
            fields.add(inline);
        }
        if (isFormLike(type)) {
            fields.add(fieldLabel("Toolbar commands"));
            fields.add(toolbarCommandsEditor());
        }
        Button css = new Button("Edit theme.css", material(FontImage.MATERIAL_COLOR_LENS, "BuilderInlineIcon"));
        css.setUIID("BuilderSecondaryAction");
        css.addActionListener(e -> openCss());
        fields.add(css);
        Button delete = new Button("Delete component", material(FontImage.MATERIAL_DELETE, "BuilderDangerIcon"));
        delete.setUIID("BuilderDangerAction");
        delete.setEnabled(element != document.root());
        delete.addActionListener(e -> { if (document.deleteSelected()) refreshEditor(); });
        fields.add(delete);
        return fields;
    }

    private Component bindingStrategyPicker() {
        Container field = new Container(BoxLayout.y());
        field.add(fieldLabel("Data binding"));
        String strategy = value(document.root(), "bindingStrategy", "properties");
        String selected = "bindable".equals(strategy) ? "@Bindable POJO"
                : "none".equals(strategy) ? "None" : "PropertyBusinessObject";
        Picker picker = stringPicker(new String[]{"None", "PropertyBusinessObject", "@Bindable POJO"}, selected);
        picker.addActionListener(e -> {
            String choice = picker.getSelectedString();
            document.select(document.root());
            document.setAttribute("bindingStrategy", "None".equals(choice) ? "none"
                    : "@Bindable POJO".equals(choice) ? "bindable" : "properties");
            setStatus("Binding strategy: " + choice + " • Java preview regenerated on open");
        });
        field.add(picker);
        return field;
    }

    private Component toolbarCommandsEditor() {
        Container commands = new Container(BoxLayout.y());
        for (Element command : document.commands()) {
            Container row = new Container(BoxLayout.y());
            row.setUIID("BuilderCommandCard");
            TextField name = new TextField(value(command, "name", "Command"));
            name.setHint("Command label");
            name.addDataChangedListener((t, i) -> document.setCommandAttribute(command, "name", name.getText()));
            Picker placement = stringPicker(new String[]{"left", "right", "overflow", "side"}, value(command, "placement", "right"));
            placement.addActionListener(e -> {
                document.setCommandAttribute(command, "placement", placement.getSelectedString());
                refreshEditor();
            });
            TextField event = new TextField(value(command, "actionEvent", "onCommand"));
            event.setHint("Event handler");
            event.addDataChangedListener((t, i) -> document.setCommandAttribute(command, "actionEvent", event.getText()));
            Button remove = new Button("Remove command");
            remove.setUIID("BuilderDangerAction");
            remove.addActionListener(e -> { document.removeCommand(command); refreshEditor(); });
            row.add(name).add(placement).add(event).add(remove);
            commands.add(row);
        }
        Button add = new Button("Add toolbar command", material(FontImage.MATERIAL_ADD, "BuilderInlineIcon"));
        add.setUIID("BuilderSecondaryAction");
        add.addActionListener(e -> { document.addCommand(); refreshEditor(); });
        commands.add(add);
        return commands;
    }

    private Component layoutTab(Element element) {
        Container fields = inspectorFields();
        if (element != document.root()) {
            Container order = new Container(new GridLayout(1, 2));
            Button earlier = new Button("Move earlier", material(FontImage.MATERIAL_ARROW_UPWARD, "BuilderInlineIcon"));
            Button later = new Button("Move later", material(FontImage.MATERIAL_ARROW_DOWNWARD, "BuilderInlineIcon"));
            earlier.setUIID("BuilderSecondaryAction");
            later.setUIID("BuilderSecondaryAction");
            earlier.addActionListener(e -> moveSelectedInParent(-1));
            later.addActionListener(e -> moveSelectedInParent(1));
            order.add(earlier).add(later);
            fields.add(fieldLabel("Order in parent"));
            fields.add(order);
        }
        if (GuiDocument.acceptsChildren(element)) {
            Picker layout = stringPicker(new String[]{"BoxLayout", "BorderLayout", "FlowLayout", "GridLayout", "TableLayout", "LayeredLayout"}, document.attribute("layout", "BoxLayout"));
            layout.addActionListener(e -> update("layout", String.valueOf(layout.getSelectedString())));
            fields.add(fieldLabel("Container layout"));
            fields.add(layout);
            if ("BoxLayout".equals(document.attribute("layout", "BoxLayout"))) {
                Picker axis = stringPicker(new String[]{"Y", "X"}, document.attribute("boxLayoutAxis", "Y"));
                axis.addActionListener(e -> update("boxLayoutAxis", axis.getSelectedString()));
                fields.add(fieldLabel("Axis"));
                fields.add(axis);
            } else if ("GridLayout".equals(document.attribute("layout", "BoxLayout"))) {
                fields.add(numericPropertyField("Grid rows", "gridLayoutRows", "1", 1, 100));
                fields.add(numericPropertyField("Grid columns", "gridLayoutColumns", "2", 1, 100));
            } else if ("TableLayout".equals(document.attribute("layout", "BoxLayout"))) {
                fields.add(numericPropertyField("Table rows", "tableLayoutRows", "2", 1, 100));
                fields.add(numericPropertyField("Table columns", "tableLayoutColumns", "2", 1, 100));
            }
        }
        Picker constraint = stringPicker(new String[]{"", "North", "South", "East", "West", "Center"}, document.attribute("layoutConstraint", ""));
        constraint.addActionListener(e -> update("layoutConstraint", constraint.getSelectedString()));
        fields.add(fieldLabel("Parent constraint"));
        fields.add(constraint);
        if ("LayeredLayout".equals(document.parentLayout(element))) {
            Element referenceElement = guidedReferenceElement(element);
            fields.add(fieldLabel("Alignment reference"));
            Picker reference = stringPicker(guidedReferenceNames(element), referenceElement == null
                    ? "Nearest component" : value(referenceElement, "name", "Nearest component"));
            reference.addActionListener(e -> {
                String selected = reference.getSelectedString();
                document.setAttribute("guidedReferenceTarget", "Nearest component".equals(selected) ? null : selected);
            });
            fields.add(reference);

            fields.add(fieldLabel("Horizontal size policy"));
            Picker horizontalPolicy = stringPicker(new String[]{"Preferred", "Fixed", "Fill parent", "Match reference"},
                    policyLabel(GuidedLayoutSupport.horizontalPolicy(element)));
            horizontalPolicy.addActionListener(e -> applyGuidedSizePolicy(true, policyValue(horizontalPolicy.getSelectedString())));
            fields.add(horizontalPolicy);
            fields.add(fieldLabel("Vertical size policy"));
            Picker verticalPolicy = stringPicker(new String[]{"Preferred", "Fixed", "Fill parent", "Match reference"},
                    policyLabel(GuidedLayoutSupport.verticalPolicy(element)));
            verticalPolicy.addActionListener(e -> applyGuidedSizePolicy(false, policyValue(verticalPolicy.getSelectedString())));
            fields.add(verticalPolicy);

            fields.add(fieldLabel("Align to reference"));
            Container align = new Container(new GridLayout(2, 3));
            align.add(layeredAction("Left", "alignLeft"));
            align.add(layeredAction("H center", "alignHCenter"));
            align.add(layeredAction("Right", "alignRight"));
            align.add(layeredAction("Top", "alignTop"));
            align.add(layeredAction("Baseline", "alignBaseline"));
            align.add(layeredAction("Bottom", "alignBottom"));
            fields.add(align);
            Container actions = new Container(new GridLayout(2, 2));
            actions.add(layeredAction("Same width", "matchWidth"));
            actions.add(layeredAction("Same height", "matchHeight"));
            actions.add(layeredAction("Fill width", "fillWidth"));
            actions.add(layeredAction("Fill height", "fillHeight"));
            fields.add(actions);
            fields.add(propertyField("Advanced insets (top right bottom left)", "layeredInsets", "auto auto auto auto"));
        } else if ("TableLayout".equals(document.parentLayout(element))) {
            fields.add(numericPropertyField("Table row", "tableRow", "0", 0, 99));
            fields.add(numericPropertyField("Table column", "tableColumn", "0", 0, 99));
            fields.add(numericPropertyField("Horizontal span", "tableHorizontalSpan", "1", 1, 100));
            fields.add(numericPropertyField("Vertical span", "tableVerticalSpan", "1", 1, 100));
            fields.add(numericPropertyField("Column width %", "tableWidth", "-1", -1, 100));
            fields.add(numericPropertyField("Row height %", "tableHeight", "-1", -1, 100));
        }
        fields.add(new SpanLabel("Guided Layout is the default free-form designer. Drag edges or corners to resize. Blue guides align edges and centers; the baseline guide keeps text aligned. Size policies remain responsive when the form changes size.", "BuilderHelp"));
        return fields;
    }

    private Button layeredAction(String label, String action) {
        Button button = new Button(label);
        button.setUIID("BuilderSecondaryAction");
        button.addActionListener(e -> applyLayeredAction(action));
        return button;
    }

    void applySelectionAction(String action) {
        normalizeSelection();
        if (selectedElements.isEmpty() || !selectedElementsShareGuidedParent()) {
            setStatus("Layout actions require components in the same Guided Layout container");
            return;
        }
        Element anchor = document.selected();
        if (anchor == null || !selectedElements.contains(anchor)) return;
        if (!"disconnect".equals(action) && selectedElements.size() < 2) return;
        document.beginTransaction();
        try {
            if ("disconnect".equals(action)) {
                for (Element element : new ArrayList<Element>(selectedElements)) disconnectGuidedElement(element);
            } else {
                for (Element element : new ArrayList<Element>(selectedElements)) {
                    if (element != anchor) applyLayeredRelationship(element, anchor, action);
                }
            }
            document.select(anchor);
        } finally {
            document.endTransaction();
        }
        recordAction("multi_selection_action", "action", action, "count", Integer.valueOf(selectedElements.size()),
                "anchor", value(anchor, "name", "component"));
        setStatus("disconnect".equals(action) ? "Disconnected " + selectedElements.size() + " components from layout relationships"
                : labelForLayeredAction(action) + " using reference " + value(anchor, "name", "component")
                        + " across " + selectedElements.size() + " components");
        if (workspace != null) refreshEditor();
    }

    private void applyLayeredRelationship(Element element, Element referenceElement, String action) {
        Component component = componentForElement(canvasHost, element);
        applyLayeredRelationship(document, element, referenceElement, action, component);
    }

    private void applyLayeredRelationship(GuiDocument targetDocument, Element element,
            Element referenceElement, String action, Component component) {
        if (component == null || referenceElement == null) return;
        String[] insets = GuidedLayoutSupport.insetValues(element);
        String[] refs = GuidedLayoutSupport.referenceNames(element);
        String[] positions = GuidedLayoutSupport.referencePositions(element);
        String referenceName = value(referenceElement, "name", "-");
        targetDocument.select(element);
        if ("alignLeft".equals(action)) {
            insets[3] = "0px"; insets[1] = "auto"; refs[3] = referenceName; refs[1] = "-"; positions[3] = "0";
            targetDocument.setAttribute("guidedHorizontalAnchor", null);
        } else if ("alignHCenter".equals(action)) {
            insets[3] = "0%"; insets[1] = "auto"; refs[3] = referenceName; refs[1] = "-"; positions[3] = "0.5";
            targetDocument.setAttribute("guidedHorizontalAnchor", "0.5");
        } else if ("alignRight".equals(action)) {
            insets[3] = "auto"; insets[1] = "0px"; refs[3] = "-"; refs[1] = referenceName; positions[1] = "0";
            targetDocument.setAttribute("guidedHorizontalAnchor", null);
        } else if ("alignTop".equals(action)) {
            insets[0] = "0px"; insets[2] = "auto"; refs[0] = referenceName; refs[2] = "-"; positions[0] = "0";
            targetDocument.setAttribute("guidedVerticalAnchor", null);
        } else if ("alignBaseline".equals(action)) {
            insets[0] = "baseline"; insets[2] = "auto"; refs[0] = referenceName; refs[2] = "-"; positions[0] = "0";
            targetDocument.setAttribute("guidedVerticalSize", GuidedLayoutSupport.PREFERRED);
            targetDocument.setAttribute("guidedPreferredHeight", null);
        } else if ("alignBottom".equals(action)) {
            insets[0] = "auto"; insets[2] = "0px"; refs[0] = "-"; refs[2] = referenceName; positions[2] = "0";
            targetDocument.setAttribute("guidedVerticalAnchor", null);
        } else if ("matchWidth".equals(action)) {
            applyMatchedWidth(referenceElement, relationshipReferencePreview(targetDocument, referenceElement),
                    insets, refs, positions, component.getAbsoluteX(), component.getWidth());
            targetDocument.setAttribute("guidedMatchWidth", referenceName);
            targetDocument.setAttribute("guidedHorizontalSize", GuidedLayoutSupport.MATCH);
        } else if ("matchHeight".equals(action)) {
            applyMatchedHeight(referenceElement, relationshipReferencePreview(targetDocument, referenceElement),
                    insets, refs, positions, component.getAbsoluteY(), component.getHeight());
            targetDocument.setAttribute("guidedMatchHeight", referenceName);
            targetDocument.setAttribute("guidedVerticalSize", GuidedLayoutSupport.MATCH);
        }
        targetDocument.setAttribute("layeredInsets", GuidedLayoutSupport.joinInsets(insets[0], insets[1], insets[2], insets[3]));
        targetDocument.setAttribute("guidedReferences", GuidedLayoutSupport.joinReferences(refs[0], refs[1], refs[2], refs[3]));
        targetDocument.setAttribute("guidedReferencePositions", GuidedLayoutSupport.joinPositions(
                positions[0], positions[1], positions[2], positions[3]));
    }

    private Component relationshipReferencePreview(GuiDocument targetDocument, Element referenceElement) {
        Element liveReference = targetDocument == document ? referenceElement
                : findElementNamed(document, value(referenceElement, "name", ""));
        return componentForElement(canvasHost, liveReference);
    }

    private void disconnectGuidedElement(Element element) {
        Component component = componentForElement(canvasHost, element);
        Element parentElement = document.parentOf(element);
        Component parentComponent = componentForElement(canvasHost, parentElement);
        if (component == null || !(parentComponent instanceof Container)) return;
        freezeGuidedElement(element, ((Container) parentComponent), component, component.getAbsoluteX(), component.getAbsoluteY());
    }

    private void freezeGuidedElement(Element element, Container parent, Component component, int absoluteX, int absoluteY) {
        int left = Math.max(0, absoluteX - contentX(parent) - component.getStyle().getMarginLeftNoRTL());
        int top = Math.max(0, absoluteY - contentY(parent) - component.getStyle().getMarginTop());
        document.select(element);
        document.setAttribute("layeredInsets", top + "px auto auto " + left + "px");
        document.setAttribute("guidedReferences", "- - - -");
        document.setAttribute("guidedReferencePositions", "0 0 0 0");
        document.setAttribute("guidedHorizontalAnchor", null);
        document.setAttribute("guidedVerticalAnchor", null);
        document.setAttribute("guidedMatchWidth", null);
        document.setAttribute("guidedMatchHeight", null);
        document.setAttribute("guidedHorizontalSize", GuidedLayoutSupport.FIXED);
        document.setAttribute("guidedVerticalSize", GuidedLayoutSupport.FIXED);
        document.setAttribute("guidedPreferredWidth", String.valueOf(Math.max(1, component.getWidth())));
        document.setAttribute("guidedPreferredHeight", String.valueOf(Math.max(1, component.getHeight())));
    }

    private void moveSelectedInParent(int delta) {
        if (reorderSelectedInParent(delta)) refreshEditor();
    }

    boolean reorderSelectedInParent(int delta) {
        Element selected = document.selected();
        Element parent = document.parentOf(selected);
        boolean moved = false;
        document.beginTransaction();
        try {
            // In a table the neighbour must be identified before the move, because "the component
            // one step earlier" is the one whose cell the user wants; afterwards it is the
            // selection itself that sits at that index.
            Element neighbour = parent != null && "TableLayout".equals(value(parent, "layout", "BoxLayout"))
                    ? siblingBy(parent, selected, delta) : null;
            if (!document.moveSelectedBy(delta)) return false;
            moved = true;
            if (parent != null && "TableLayout".equals(value(parent, "layout", "BoxLayout"))) {
                // Swap the two cells rather than renumbering the table from sibling order: the
                // user asked these two components to trade places, not the rest to shuffle.
                if (neighbour != null) swapTableCells(selected, neighbour);
                normalizeTableCells(parent);
                document.select(selected);
            }
        } finally {
            document.endTransaction();
        }
        return moved;
    }

    private Element siblingBy(Element parent, Element element, int delta) {
        List<Element> children = componentChildren(parent);
        int index = children.indexOf(element) + delta;
        return index < 0 || index >= children.size() ? null : children.get(index);
    }

    private void swapTableCells(Element first, Element second) {
        String firstRow = first.getAttribute("tableRow");
        String firstColumn = first.getAttribute("tableColumn");
        document.select(first);
        document.setAttribute("tableRow", second.getAttribute("tableRow"));
        document.setAttribute("tableColumn", second.getAttribute("tableColumn"));
        document.select(second);
        document.setAttribute("tableRow", firstRow);
        document.setAttribute("tableColumn", firstColumn);
    }

    private void applyLayeredAction(String action) {
        Element element = document.selected();
        Element parentElement = document.parentOf(element);
        Component component = componentForElement(canvasHost, element);
        Component parentComponent = componentForElement(canvasHost, parentElement);
        if (component == null || !(parentComponent instanceof Container)) return;
        int leftPx = component.getAbsoluteX() - ((Container) parentComponent).getAbsoluteX();
        int topPx = component.getAbsoluteY() - ((Container) parentComponent).getAbsoluteY();
        int width = component.getWidth();
        int height = component.getHeight();
        Component reference = guidedReferenceComponent(element, ((Container) parentComponent), component);
        Element referenceElement = reference == null ? null : (Element) reference.getClientProperty("gui.element");
        if ((action.startsWith("align") || action.startsWith("match")) && referenceElement == null) {
            setStatus("Add another component before creating an alignment relationship");
            return;
        }
        String[] insets = GuidedLayoutSupport.insetValues(element);
        String[] refs = GuidedLayoutSupport.referenceNames(element);
        String[] positions = GuidedLayoutSupport.referencePositions(element);
        String referenceName = referenceElement == null ? "-" : value(referenceElement, "name", "-");
        document.beginTransaction();
        try {
            if ("alignLeft".equals(action)) {
                insets[3] = "0px"; insets[1] = "auto"; refs[3] = referenceName; refs[1] = "-"; positions[3] = "0";
                document.setAttribute("guidedHorizontalAnchor", null);
            } else if ("alignHCenter".equals(action)) {
                insets[3] = "0%"; insets[1] = "auto"; refs[3] = referenceName; refs[1] = "-"; positions[3] = "0.5";
                document.setAttribute("guidedHorizontalAnchor", "0.5");
            } else if ("alignRight".equals(action)) {
                insets[3] = "auto"; insets[1] = "0px"; refs[3] = "-"; refs[1] = referenceName; positions[1] = "0";
                document.setAttribute("guidedHorizontalAnchor", null);
            } else if ("alignTop".equals(action)) {
                insets[0] = "0px"; insets[2] = "auto"; refs[0] = referenceName; refs[2] = "-"; positions[0] = "0";
                document.setAttribute("guidedVerticalAnchor", null);
            } else if ("alignBaseline".equals(action)) {
                insets[0] = "baseline"; insets[2] = "auto"; refs[0] = referenceName; refs[2] = "-"; positions[0] = "0";
                document.setAttribute("guidedVerticalSize", GuidedLayoutSupport.PREFERRED);
                document.setAttribute("guidedPreferredHeight", null);
            } else if ("alignBottom".equals(action)) {
                insets[0] = "auto"; insets[2] = "0px"; refs[0] = "-"; refs[2] = referenceName; positions[2] = "0";
                document.setAttribute("guidedVerticalAnchor", null);
            } else if ("matchWidth".equals(action)) {
                applyMatchedWidth(referenceElement, insets, refs, positions, component.getAbsoluteX(), component.getWidth());
                document.setAttribute("guidedMatchWidth", referenceName);
                document.setAttribute("guidedHorizontalSize", GuidedLayoutSupport.MATCH);
            } else if ("matchHeight".equals(action)) {
                applyMatchedHeight(referenceElement, insets, refs, positions, component.getAbsoluteY(), component.getHeight());
                document.setAttribute("guidedMatchHeight", referenceName);
                document.setAttribute("guidedVerticalSize", GuidedLayoutSupport.MATCH);
            } else if ("fillWidth".equals(action)) {
                insets[3] = Math.max(0, component.getAbsoluteX() - contentX(((Container) parentComponent))) + "px";
                insets[1] = Math.max(0, contentX(((Container) parentComponent)) + contentWidth(((Container) parentComponent)) - component.getAbsoluteX() - component.getWidth()) + "px";
                refs[3] = refs[1] = "-";
                document.setAttribute("guidedHorizontalSize", GuidedLayoutSupport.FILL);
            } else if ("fillHeight".equals(action)) {
                insets[0] = Math.max(0, component.getAbsoluteY() - contentY(((Container) parentComponent))) + "px";
                insets[2] = Math.max(0, contentY(((Container) parentComponent)) + contentHeight(((Container) parentComponent)) - component.getAbsoluteY() - component.getHeight()) + "px";
                refs[0] = refs[2] = "-";
                document.setAttribute("guidedVerticalSize", GuidedLayoutSupport.FILL);
            }
            document.setAttribute("layeredInsets", GuidedLayoutSupport.joinInsets(insets[0], insets[1], insets[2], insets[3]));
            document.setAttribute("guidedReferences", GuidedLayoutSupport.joinReferences(refs[0], refs[1], refs[2], refs[3]));
            document.setAttribute("guidedReferencePositions", GuidedLayoutSupport.joinPositions(positions[0], positions[1], positions[2], positions[3]));
        } finally {
            document.endTransaction();
        }
        setStatus(labelForLayeredAction(action) + (reference == null ? "" : " using " + reference.getName().replace("preview.", "")));
        refreshEditor();
    }

    private void applyGuidedSizePolicy(boolean horizontal, String policy) {
        Element element = document.selected();
        Element parentElement = document.parentOf(element);
        Component component = componentForElement(canvasHost, element);
        Component parentComponent = componentForElement(canvasHost, parentElement);
        if (component == null || !(parentComponent instanceof Container)) return;
        Component reference = guidedReferenceComponent(element, ((Container) parentComponent), component);
        Element referenceElement = reference == null ? null : (Element) reference.getClientProperty("gui.element");
        if (GuidedLayoutSupport.MATCH.equals(policy) && referenceElement == null) {
            setStatus("Match size needs another component as its reference");
            return;
        }
        String[] insets = GuidedLayoutSupport.insetValues(element);
        String[] refs = GuidedLayoutSupport.referenceNames(element);
        String[] positions = GuidedLayoutSupport.referencePositions(element);
        document.beginTransaction();
        try {
            if (horizontal) {
                if (GuidedLayoutSupport.PREFERRED.equals(policy) || GuidedLayoutSupport.FIXED.equals(policy)) {
                    insets[3] = Math.max(0, component.getAbsoluteX() - contentX(((Container) parentComponent))) + "px";
                    insets[1] = "auto"; refs[3] = refs[1] = "-";
                    document.setAttribute("guidedPreferredWidth", GuidedLayoutSupport.FIXED.equals(policy)
                            ? String.valueOf(component.getWidth()) : null);
                } else if (GuidedLayoutSupport.FILL.equals(policy)) {
                    insets[3] = Math.max(0, component.getAbsoluteX() - contentX(((Container) parentComponent))) + "px";
                    insets[1] = Math.max(0, contentX(((Container) parentComponent)) + contentWidth(((Container) parentComponent)) - component.getAbsoluteX() - component.getWidth()) + "px";
                    refs[3] = refs[1] = "-";
                } else {
                    applyMatchedWidth(referenceElement, insets, refs, positions, component.getAbsoluteX(), component.getWidth());
                    document.setAttribute("guidedMatchWidth", value(referenceElement, "name", ""));
                }
                document.setAttribute("guidedHorizontalSize", policy);
            } else {
                if (GuidedLayoutSupport.PREFERRED.equals(policy) || GuidedLayoutSupport.FIXED.equals(policy)) {
                    insets[0] = Math.max(0, component.getAbsoluteY() - contentY(((Container) parentComponent))) + "px";
                    insets[2] = "auto"; refs[0] = refs[2] = "-";
                    document.setAttribute("guidedPreferredHeight", GuidedLayoutSupport.FIXED.equals(policy)
                            ? String.valueOf(component.getHeight()) : null);
                } else if (GuidedLayoutSupport.FILL.equals(policy)) {
                    insets[0] = Math.max(0, component.getAbsoluteY() - contentY(((Container) parentComponent))) + "px";
                    insets[2] = Math.max(0, contentY(((Container) parentComponent)) + contentHeight(((Container) parentComponent)) - component.getAbsoluteY() - component.getHeight()) + "px";
                    refs[0] = refs[2] = "-";
                } else {
                    applyMatchedHeight(referenceElement, insets, refs, positions, component.getAbsoluteY(), component.getHeight());
                    document.setAttribute("guidedMatchHeight", value(referenceElement, "name", ""));
                }
                document.setAttribute("guidedVerticalSize", policy);
            }
            document.setAttribute("layeredInsets", GuidedLayoutSupport.joinInsets(insets[0], insets[1], insets[2], insets[3]));
            document.setAttribute("guidedReferences", GuidedLayoutSupport.joinReferences(refs[0], refs[1], refs[2], refs[3]));
            document.setAttribute("guidedReferencePositions", GuidedLayoutSupport.joinPositions(positions[0], positions[1], positions[2], positions[3]));
        } finally {
            document.endTransaction();
        }
        refreshEditor();
        setStatus((horizontal ? "Horizontal" : "Vertical") + " size policy: " + policyLabel(policy));
    }

    private String[] guidedReferenceNames(Element element) {
        Element parent = document.parentOf(element);
        List<String> names = new ArrayList<>();
        names.add("Nearest component");
        for (Element child : componentChildren(parent)) if (child != element) names.add(value(child, "name", value(child, "type", "component")));
        return names.toArray(new String[names.size()]);
    }

    private Element guidedReferenceElement(Element element) {
        Element parent = document.parentOf(element);
        return namedSibling(parent, value(element, "guidedReferenceTarget", ""));
    }

    private Component guidedReferenceComponent(Element element, Container parent, Component component) {
        Element explicit = guidedReferenceElement(element);
        Component result = explicit == null ? null : componentForElement(canvasHost, explicit);
        return result == null ? nearestSibling(parent, component) : result;
    }

    private String policyLabel(String policy) {
        if (GuidedLayoutSupport.FIXED.equals(policy)) return "Fixed";
        if (GuidedLayoutSupport.FILL.equals(policy)) return "Fill parent";
        if (GuidedLayoutSupport.MATCH.equals(policy)) return "Match reference";
        return "Preferred";
    }

    private String policyValue(String label) {
        if ("Fixed".equals(label)) return GuidedLayoutSupport.FIXED;
        if ("Fill parent".equals(label)) return GuidedLayoutSupport.FILL;
        if ("Match reference".equals(label)) return GuidedLayoutSupport.MATCH;
        return GuidedLayoutSupport.PREFERRED;
    }

    private Component nearestSibling(Container parent, Component source) {
        Component nearest = null;
        long best = Long.MAX_VALUE;
        int cx = source.getAbsoluteX() + source.getWidth() / 2;
        int cy = source.getAbsoluteY() + source.getHeight() / 2;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component candidate = parent.getComponentAt(i);
            if (candidate == source || candidate.getClientProperty("gui.element") == null) continue;
            long dx = candidate.getAbsoluteX() + candidate.getWidth() / 2 - cx;
            long dy = candidate.getAbsoluteY() + candidate.getHeight() / 2 - cy;
            long distance = dx * dx + dy * dy;
            if (distance < best) { best = distance; nearest = candidate; }
        }
        return nearest;
    }

    private String percent(int pixels, int total) {
        return Math.max(0, Math.min(100, Math.round(pixels * 100f / Math.max(1, total)))) + "%";
    }

    private String labelForLayeredAction(String action) {
        if ("alignLeft".equals(action)) return "Aligned left edges";
        if ("alignHCenter".equals(action)) return "Aligned horizontal centers";
        if ("alignRight".equals(action)) return "Aligned right edges";
        if ("alignTop".equals(action)) return "Aligned top edges";
        if ("alignBaseline".equals(action)) return "Aligned text baselines";
        if ("alignBottom".equals(action)) return "Aligned bottom edges";
        if ("fillWidth".equals(action)) return "Filled available width";
        if ("fillHeight".equals(action)) return "Filled available height";
        if ("matchWidth".equals(action)) return "Matched nearest width";
        return "Matched nearest height";
    }

    private Component eventsTab(Element element) {
        Container fields = inspectorFields();
        fields.add(propertyField("Action handler", "actionEvent"));
        fields.add(new SpanLabel("Create or edit the selected handler here. The embedded Java editor keeps generated UI code separate and adds a handler stub only when it is missing.", "BuilderHelp"));
        Button code = new Button("Edit event handler", material(FontImage.MATERIAL_CODE, "BuilderInlineIcon"));
        code.setUIID("BuilderPrimaryAction");
        code.addActionListener(e -> openEventHandler());
        fields.add(code);
        return fields;
    }

    private Container inspectorFields() {
        Container fields = new Container(BoxLayout.y());
        fields.setUIID("BuilderInspectorFields");
        fields.setScrollableY(true);
        return fields;
    }

    private Component propertyField(String label, String attribute) {
        return propertyField(label, attribute, document.attribute(attribute, ""));
    }

    /**
     * The component name is the identity that Guided Layout relationships and generated Java
     * fields are built from. It is committed on Enter or focus loss rather than on every
     * keystroke, is forced to stay unique, and repoints every relationship that referenced the
     * previous name in one undo step.
     */
    private Component nameField() {
        Element target = document.selected();
        TextField field = new TextField(value(target, "name", ""));
        field.setUIID("BuilderField");
        field.getSemantics().setIdentifier("guibuilder.property.name").setLabel("Name");
        Runnable commit = () -> {
            if (document == null || !document.containsElement(target)) return;
            String previous = value(target, "name", "");
            String requested = field.getText() == null ? "" : field.getText().trim();
            if (requested.equals(previous)) return;
            if (requested.length() == 0) {
                field.setText(previous);
                setStatus("A component name cannot be empty");
                return;
            }
            document.select(target);
            String applied = document.renameSelected(requested);
            recordAction("property_changed", "component", previous, "property", "name", "value", applied);
            if (applied != null && !applied.equals(requested)) {
                field.setText(applied);
                setStatus("Renamed to " + applied + " because " + requested + " is already used");
            } else {
                setStatus("Renamed " + previous + " to " + applied + " • relationships updated");
            }
            scheduleDesignerRefresh();
        };
        field.addActionListener(e -> commit.run());
        field.addFocusListener(new FocusListener() {
            @Override public void focusGained(Component component) { }
            @Override public void focusLost(Component component) { commit.run(); }
        });
        return BoxLayout.encloseY(fieldLabel("Name"), field);
    }

    private Component propertyField(String label, String attribute, String fallback) {
        Element target = document.selected();
        TextField field = new TextField(document.attribute(attribute, fallback));
        field.setUIID("BuilderField");
        field.getSemantics().setIdentifier("guibuilder.property." + attribute).setLabel(label);
        field.addDataChangedListener((type, index) -> update(target, attribute, field.getText()));
        return BoxLayout.encloseY(fieldLabel(label), field);
    }

    private Component numericPropertyField(String label, String attribute, String fallback, int minimum, int maximum) {
        Element target = document.selected();
        TextField field = new TextField(document.attribute(attribute, fallback));
        field.setUIID("BuilderField");
        field.getSemantics().setIdentifier("guibuilder.property." + attribute).setLabel(label);
        Runnable validate = () -> {
            Integer parsed = parseInteger(field.getText());
            boolean valid = parsed != null && parsed >= minimum && parsed <= maximum;
            field.setUIID(valid ? "BuilderField" : "BuilderFieldError");
            field.repaint();
        };
        Runnable commit = () -> {
            Integer parsed = parseInteger(field.getText());
            if (parsed == null || parsed < minimum || parsed > maximum) {
                field.setText(document.attribute(attribute, fallback));
                field.setUIID("BuilderField");
                setStatus(label + " must be a number from " + minimum + " to " + maximum);
                return;
            }
            String normalized = String.valueOf(parsed);
            if (!normalized.equals(document.attribute(attribute, fallback))) update(target, attribute, normalized);
        };
        field.addDataChangedListener((type, index) -> validate.run());
        field.addActionListener(e -> commit.run());
        field.addFocusListener(new com.codename1.ui.events.FocusListener() {
            public void focusGained(Component component) { }
            public void focusLost(Component component) { commit.run(); }
        });
        return BoxLayout.encloseY(fieldLabel(label), field);
    }

    static Integer parseInteger(String value) {
        try {
            if (value == null || value.length() == 0) return null;
            int start = value.charAt(0) == '-' ? 1 : 0;
            if (start == value.length()) return null;
            for (int i = start; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (ch < '0' || ch > '9') return null;
            }
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Component booleanProperty(String label, String attribute, boolean fallback) {
        Element target = document.selected();
        CheckBox field = new CheckBox(label);
        field.setUIID("BuilderCheck");
        field.getSemantics().setIdentifier("guibuilder.property." + attribute).setLabel(label);
        field.setSelected("true".equals(document.attribute(attribute, String.valueOf(fallback))));
        field.addActionListener(e -> update(target, attribute, String.valueOf(field.isSelected())));
        return field;
    }

    private Component pickerProperty(String label, String attribute, String[] values, String fallback) {
        Element target = document.selected();
        Picker picker = stringPicker(values, document.attribute(attribute, fallback));
        picker.getSemantics().setIdentifier("guibuilder.property." + attribute).setLabel(label);
        picker.addActionListener(e -> update(target, attribute, picker.getSelectedString()));
        return BoxLayout.encloseY(fieldLabel(label), picker);
    }

    private Label fieldLabel(String text) { return new Label(text, "BuilderFieldLabel"); }

    private Picker stringPicker(String[] values, String selected) {
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings(values);
        picker.setSelectedString(selected);
        picker.setUIID("BuilderPicker");
        return picker;
    }

    private void update(String attribute, String value) {
        update(document.selected(), attribute, value);
    }

    private void update(Element element, String attribute, String value) {
        document.select(element);
        document.setAttribute(attribute, value);
        recordAction("property_changed", "component", value(element, "name", value(element, "type", "component")),
                "property", attribute, "value", value);
        if ("layout".equals(attribute) || "boxLayoutAxis".equals(attribute)
                || "gridLayoutRows".equals(attribute) || "gridLayoutColumns".equals(attribute)
                || "tableLayoutRows".equals(attribute) || "tableLayoutColumns".equals(attribute)
                || "tableRow".equals(attribute) || "tableColumn".equals(attribute)
                || "tableHorizontalSpan".equals(attribute) || "tableVerticalSpan".equals(attribute)
                || "tableWidth".equals(attribute) || "tableHeight".equals(attribute)
                || "layoutConstraint".equals(attribute)) {
            refreshEditor();
        } else {
            updatePreviewAttribute(element, attribute, value);
        }
        setStatus("Modified • " + relativeFormName(document.path()));
    }

    private void updatePreviewAttribute(Element element, String attribute, String value) {
        if (element == document.root() && "title".equals(attribute) && formTitlePreview != null) {
            formTitlePreview.setText(value == null || value.length() == 0 ? "Untitled Form" : value);
            formTitlePreview.getParent().revalidate();
            return;
        }
        Component component = componentForElement(canvasHost, element);
        if (component == null) return;
        if ("text".equals(attribute)) {
            if (component instanceof SpanLabel) ((SpanLabel) component).setText(value);
            else if (component instanceof Label) ((Label) component).setText(value);
            else if (component instanceof TextArea) ((TextArea) component).setText(value);
        } else if ("hint".equals(attribute) && component instanceof TextArea) {
            ((TextArea) component).setHint(value);
        } else if ("enabled".equals(attribute)) {
            component.setEnabled(!"false".equals(value));
        } else if ("visible".equals(attribute)) {
            component.setVisible(!"false".equals(value));
        } else if ("rtl".equals(attribute)) {
            component.setRTL("true".equals(value));
        } else if ("name".equals(attribute)) {
            component.setName("preview." + value);
        } else if ("gap".equals(attribute) && component instanceof Label) {
            ((Label) component).setGap(integer(value, ((Label) component).getGap()));
        } else if ("alignment".equals(attribute)) {
            int alignment = "center".equals(value) ? Component.CENTER : "right".equals(value) ? Component.RIGHT : Component.LEFT;
            if (component instanceof Label) ((Label) component).setAlignment(alignment);
            else if (component instanceof TextArea) ((TextArea) component).setAlignment(alignment);
        } else if ("tickerEnabled".equals(attribute) && component instanceof Label) {
            ((Label) component).setTickerEnabled("true".equals(value));
        } else if ("toggle".equals(attribute) && component instanceof Button) {
            ((Button) component).setToggle("true".equals(value));
        } else if ("selected".equals(attribute) && component instanceof CheckBox) {
            ((CheckBox) component).setSelected("true".equals(value));
        } else if ("columns".equals(attribute) && component instanceof TextArea) {
            ((TextArea) component).setColumns(integer(value, ((TextArea) component).getColumns()));
        } else if ("rows".equals(attribute) && component instanceof TextArea) {
            ((TextArea) component).setRows(integer(value, ((TextArea) component).getRows()));
        } else if ("maxSize".equals(attribute) && component instanceof TextArea) {
            ((TextArea) component).setMaxSize(integer(value, ((TextArea) component).getMaxSize()));
        } else if ("editable".equals(attribute) && component instanceof TextArea) {
            ((TextArea) component).setEditable(!"false".equals(value));
        } else if ("growByContent".equals(attribute) && component instanceof TextArea) {
            ((TextArea) component).setGrowByContent(!"false".equals(value));
        } else if ("scrollableX".equals(attribute) && component instanceof Container) {
            ((Container) component).setScrollableX("true".equals(value));
        } else if ("scrollableY".equals(attribute) && component instanceof Container) {
            ((Container) component).setScrollableY("true".equals(value));
        } else if ("minValue".equals(attribute) && component instanceof com.codename1.ui.Slider) {
            ((com.codename1.ui.Slider) component).setMinValue(integer(value, ((com.codename1.ui.Slider) component).getMinValue()));
        } else if ("maxValue".equals(attribute) && component instanceof com.codename1.ui.Slider) {
            ((com.codename1.ui.Slider) component).setMaxValue(integer(value, ((com.codename1.ui.Slider) component).getMaxValue()));
        } else if ("progress".equals(attribute) && component instanceof com.codename1.ui.Slider) {
            ((com.codename1.ui.Slider) component).setProgress(integer(value, ((com.codename1.ui.Slider) component).getProgress()));
        } else if ("editable".equals(attribute) && component instanceof com.codename1.ui.Slider) {
            ((com.codename1.ui.Slider) component).setEditable("true".equals(value));
        } else if ("infinite".equals(attribute) && component instanceof com.codename1.ui.Slider) {
            ((com.codename1.ui.Slider) component).setInfinite("true".equals(value));
        } else if ("selectedIndex".equals(attribute) && component instanceof Tabs && ((Tabs) component).getTabCount() > 0) {
            ((Tabs) component).setSelectedIndex(Math.max(0, Math.min(((Tabs) component).getTabCount() - 1, integer(value, 0))), false);
        } else if ("tabPlacement".equals(attribute) && component instanceof Tabs) {
            ((Tabs) component).setTabPlacement("bottom".equals(value) ? Component.BOTTOM : "left".equals(value) ? Component.LEFT
                    : "right".equals(value) ? Component.RIGHT : Component.TOP);
        } else if ("layeredInsets".equals(attribute) && component.getParent() != null
                && component.getParent().getLayout() instanceof LayeredLayout) {
            try { ((LayeredLayout) component.getParent().getLayout()).setInsets(component, value); } catch (RuntimeException ignored) { }
        } else if ("uiid".equals(attribute)) {
            component.setUIID(value == null || value.length() == 0 ? document.effectiveUiid(element) : value);
            refreshSinglePreviewTheme(component);
        }
        if (component.getParent() != null) component.getParent().revalidate();
        component.repaint();
    }

    private void refreshSinglePreviewTheme(Component component) {
        try {
            if (projectTheme != null) {
                UIManager.getInstance().setThemeProps(projectTheme);
                component.refreshTheme();
            }
        } finally {
            if (builderTheme != null) UIManager.getInstance().setThemeProps(builderTheme);
        }
        ComponentPreviewFactory.stabilizeDesignStyles(component);
    }

    private int integer(String value, int fallback) {
        try { return Integer.parseInt(value == null || value.length() == 0 ? String.valueOf(fallback) : value); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private void addComponent(String type) {
        if (document == null) return;
        if (document.addComponent(type) == null) {
            // Core BorderLayout evicts whatever holds the region it is handed, so a sixth child
            // would remove one of the five already placed rather than joining them.
            ToastBar.showErrorMessage("That container is a BorderLayout and all five regions are taken");
            setStatus("Nothing added: the selected BorderLayout is full");
            return;
        }
        refreshEditor();
        setStatus("Added " + type);
    }

    private void editSelectedContent() {
        if (document == null) return;
        finishInlineEditor();
        Element target = document.selected();
        String type = value(target, "type", "Component");
        String attribute = isFormLike(type) ? "title" : "text";
        Component source = target == document.root() ? formTitlePreview : componentForElement(canvasHost, target);
        if (source == null || workspace == null) return;
        Container parent = source.getParent();
        if (parent == null) return;
        final int editorWidth = Math.max(1, source.getWidth());
        final int editorHeight = Math.max(1, source.getHeight());
        TextField editor = new TextField(value(target, attribute, "")) {
            @Override protected Dimension calcPreferredSize() {
                Dimension natural = super.calcPreferredSize();
                natural.setWidth(Math.max(editorWidth, natural.getWidth()));
                natural.setHeight(Math.max(editorHeight, natural.getHeight()));
                return natural;
            }
        };
        editor.setUIID("BuilderInlineEditor");
        editor.setSingleLineTextArea(true);
        editor.addDataChangedListener((changeType, index) -> {
            document.select(target);
            document.setAttribute(attribute, editor.getText());
            updatePreviewAttribute(target, attribute, editor.getText());
            setStatus("Editing " + attribute + " in place • Enter or click away to finish");
        });
        editor.setDoneListener(e -> finishInlineEditor());
        editor.addFocusListener(new FocusListener() {
            @Override public void focusGained(Component cmp) { }
            @Override public void focusLost(Component cmp) {
                Display.getInstance().callSerially(() -> finishInlineEditor());
            }
        });
        inlineEditor = editor;
        inlineEditorSource = source;
        inlineEditorParent = parent;
        inlineEditorTarget = target;
        inlineEditorAttribute = attribute;
        parent.replace(source, editor, null);
        parent.revalidate();
        Display.getInstance().callSerially(() -> {
            if (inlineEditor == editor) {
                editor.requestFocus();
                editor.startEditingAsync();
            }
        });
    }

    private void finishInlineEditor() {
        if (inlineEditor == null || finishingInlineEditor) return;
        finishingInlineEditor = true;
        TextField editor = inlineEditor;
        inlineEditor = null;
        Component source = inlineEditorSource;
        Container parent = inlineEditorParent;
        Element target = inlineEditorTarget;
        String attribute = inlineEditorAttribute;
        inlineEditorSource = null;
        inlineEditorParent = null;
        inlineEditorTarget = null;
        inlineEditorAttribute = null;
        Runnable complete = () -> completeInlineEditor(editor, source, parent, target, attribute);
        if (editor.isEditing()) editor.stopEditing(() -> Display.getInstance().callSerially(complete));
        else complete.run();
    }

    private void completeInlineEditor(TextField editor, Component source, Container parent,
            Element target, String attribute) {
        if (parent != null && source != null && editor.getParent() == parent) {
            parent.replace(editor, source, null);
        } else if (editor.getParent() != null) {
            Container actualParent = editor.getParent();
            actualParent.removeComponent(editor);
        }
        commitInlineValue(target, attribute, editor.getText());
        if (parent != null) parent.revalidate();
        finishingInlineEditor = false;
        if (document != null && inspectorHost != null) refreshInspector();
    }

    void commitInlineValue(Element target, String attribute, String value) {
        if (document == null || target == null || attribute == null) return;
        document.select(target);
        document.setAttribute(attribute, value);
        updatePreviewAttribute(target, attribute, value);
    }

    private void copy() {
        if (document != null) clipboardXml = document.copySelectedXml();
    }

    private void undo() {
        finishInlineEditor();
        if (document != null && document.undo()) {
            refreshEditor();
            recordAction("undo", "form", relativeFormName(document.path()));
            setStatus("Undo");
        }
    }

    private void redo() {
        finishInlineEditor();
        if (document != null && document.redo()) {
            refreshEditor();
            recordAction("redo", "form", relativeFormName(document.path()));
            setStatus("Redo");
        }
    }

    private void cut() {
        if (document == null || document.selected() == document.root()) return;
        copy();
        document.deleteSelected();
        refreshEditor();
    }

    private void paste() {
        if (document != null && document.pasteXml(clipboardXml) != null) refreshEditor();
    }

    private void deleteSelection() {
        if (document != null && document.deleteSelected()) refreshEditor();
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        Preferences.set("guibuilder.darkMode", darkMode);
        Display.getInstance().setDarkMode(Boolean.valueOf(darkMode));
        UIManager.getInstance().setThemeProps(builderTheme);
        workspace.refreshTheme();
        refreshEditor();
    }

    /**
     * @return true when the form reached disk. Callers that are about to replace the in-memory
     *     document have to know: a read only file or a full disk otherwise discarded the edits the
     *     user had just asked to keep.
     */
    private boolean save() {
        if (document == null) return true;
        try {
            ProjectIO.write(document.path(), document.toXml());
            // The .gui file alone changes nothing at runtime. Without regenerating the companion
            // here, a component added in the designer and saved was simply absent from the running
            // application until the user happened to open the Code pane and save that too, and a
            // freshly scaffolded form stayed an empty screen despite a successful save.
            String sourcePath = companionSourcePath();
            String existing = ProjectIO.exists(sourcePath) ? ProjectIO.read(sourcePath) : null;
            if (!writeCompanionSource(sourcePath, existing)) return false;
            // The generated companion references <Form>Model whenever a binding strategy is set,
            // and cn1:create-gui-form does not write bindingStrategy, so a scaffolded form defaults
            // to "properties". Creating the model only when the Code pane is saved left an ordinary
            // Save producing source that referenced a class which did not exist.
            ensureBindingModel(sourcePath);
            // Only now: marking the document clean after the .gui write but before the companion
            // one meant a failed companion write left isModified() false, so the next form switch
            // went ahead without retrying and the runtime source stayed stale.
            document.markSaved();
            recordAction("saved", "form", relativeFormName(document.path()));
            setStatus("Saved " + relativeFormName(document.path()) + " and its companion source");
            ToastBar.showMessage("GUI form saved", FontImage.MATERIAL_CHECK);
            return true;
        } catch (IOException ex) {
            ToastBar.showErrorMessage("Save failed: " + ex.getMessage());
            return false;
        }
    }

    private void refreshProject() {
        guiFiles = ProjectIO.findGuiFiles(binding.guiDir());
        refreshForms();
        if (document == null) return;
        // Re-reading the form from disk throws away everything edited since the last save. Refresh
        // is about picking up files added outside the editor, so it must not cost the user work.
        if (document.isModified()
                && !com.codename1.ui.Dialog.show("Unsaved changes",
                        "Reloading " + relativeFormName(document.path())
                        + " from disk discards the changes you have not saved.", "Reload", "Keep editing")) {
            setStatus("Project refreshed; kept your unsaved changes");
            return;
        }
        openForm(document.path());
    }

    private void showEmptyProject() {
        canvasHost.removeAll();
        canvasHost.add(BorderLayout.CENTER, new SpanLabel("No GUI Builder files were found under src/main/guibuilder. Create one with mvn cn1:create-gui-form -DclassName=com.example.MyForm.", "BuilderEmptyProject"));
        canvasHost.revalidate();
    }

    private void setCanvasMode(String mode) {
        canvasMode = mode;
        refreshEditor();
        setStatus("Preview: " + mode.replace("phone", "phone ").replace("tablet", "tablet "));
    }

    private void openCss() {
        if (binding == null || binding.cssFile() == null) return;
        try {
            refreshEditor();
            String css = ProjectIO.read(binding.cssFile());
            if (reopeningEditor && editorBuffer != null) css = editorBuffer;
            CodeEditor editor = new CodeEditor("css", css);
            activeCodeEditor = editor;
            lastObservedCss = css;
            // The CSS editor already observes its own text to recompile live, so it mirrors the
            // buffer from that path instead of registering a second change listener here.
            editorBuffer = css;
            if (!reopeningEditor) editorBufferOnDisk = css;
            editor.setTheme(darkMode ? "dark" : "light");
            editor.setEditable(true);
            editor.setShowLineNumbers(true);
            Component stage = canvasHost.getComponentAt(0);
            // Name the file being edited. "theme.css" alone left it ambiguous which stylesheet the
            // canvas was actually showing when the two did not appear to agree.
            Container editorPane = editorPane(binding.cssFile(), editor,
                    () -> editor.getText(value -> saveCss(value, editor)), this::closeEditorPane);
            activeEditorReopen = this::openCss;
            editor.addChangeListener(event -> scheduleLiveCss(editor));
            if (cssLiveTimer != null) cssLiveTimer.cancel();
            cssLiveTimer = com.codename1.ui.util.UITimer.timer(250, true, workspace, () -> {
                if (activeCodeEditor != editor || editor.getComponentForm() != workspace) {
                    if (cssLiveTimer != null) cssLiveTimer.cancel();
                    return;
                }
                editor.getText(value -> {
                    if (value != null && !value.equals(lastObservedCss)) {
                        scheduleLiveCss(editor);
                    }
                });
            });
            canvasHost.removeComponent(stage);
            SplitPane split = new SplitPane(SplitPane.HORIZONTAL_SPLIT, editorPane, stage, "20%", "50%", "80%");
            canvasHost.removeAll();
            canvasHost.add(BorderLayout.CENTER, split);
            canvasHost.revalidate();
            // Focus only once the editor is in the form: requestFocus does nothing before that.
            editor.onReady(editor::focusEditor);
            setStatus("Editing CSS • changes compile into the live preview");
        } catch (IOException ex) {
            ToastBar.showErrorMessage("Unable to read theme.css: " + ex.getMessage());
        }
    }

    private void scheduleLiveCss(CodeEditor editor) {
        final int revision = ++cssEditRevision;
        com.codename1.ui.util.UITimer.timer(120, false, workspace, () -> {
            if (revision != cssEditRevision || activeCodeEditor != editor) return;
            editor.getText(value -> {
                if (value == null) return;
                editorBuffer = value;
                if (value.equals(lastObservedCss)) return;
                lastObservedCss = value;
                applyProjectCss(value, editor);
            });
        });
    }

    private void openCompanionSource() {
        openSourceEditor(null);
    }

    private void openBindingModel() {
        if (document == null || binding == null || binding.sourceDir() == null) return;
        String formPath = companionSourcePath();
        String modelPath = formPath.substring(0, formPath.length() - 5) + "Model.java";
        try {
            // Rebuild the canvas first: opening an editor over an already open one would nest
            // split panes and push the design surface out of reach.
            refreshEditor();
            String source = ProjectIO.exists(modelPath) ? ProjectIO.read(modelPath) : generatedModelSource();
            if (reopeningEditor && editorBuffer != null) source = editorBuffer;
            CodeEditor editor = new CodeEditor("java", source);
            activeCodeEditor = editor;
            trackEditorBuffer(editor, source);
            editor.setTheme(darkMode ? "dark" : "light");
            editor.setEditable(true);
            editor.onReady(editor::focusEditor);
            Component stage = canvasHost.getComponentCount() == 0 ? new Label() : canvasHost.getComponentAt(0);
            Container editorPane = editorPane("Binding model: " + relativeFormName(document.path()) + "Model",
                    editor, () -> editor.getText(value -> saveSource(modelPath, value)), this::closeEditorPane);
            activeEditorReopen = this::openBindingModel;
            canvasHost.removeComponent(stage);
            canvasHost.removeAll();
            canvasHost.add(BorderLayout.CENTER,
                    new SplitPane(SplitPane.HORIZONTAL_SPLIT, editorPane, stage, "20%", "58%", "85%"));
            canvasHost.revalidate();
            setStatus("Editing generated PropertyBusinessObject binding model");
        } catch (IOException ex) {
            ToastBar.showErrorMessage("Unable to open binding model: " + ex.getMessage());
        }
    }

    private void openEventHandler() {
        String handler = document == null ? "" : document.attribute("actionEvent", "").trim();
        if (handler.length() == 0) {
            ToastBar.showErrorMessage("Enter an action handler name first");
            return;
        }
        openSourceEditor(handler);
    }

    private void openSourceEditor(String handler) {
        if (document == null || binding == null || binding.sourceDir() == null) return;
        String sourcePath = companionSourcePath();
        try {
            // Rebuild the canvas first: opening an editor over an already open one would nest
            // split panes and push the design surface out of reach.
            refreshEditor();
            String generated = defaultCompanionSource();
            String source = ProjectIO.exists(sourcePath) ? mergeGeneratedSource(ProjectIO.read(sourcePath), generated) : generated;
            // A rebuild must not cost the user their unsaved text; the mirror is what they were
            // actually looking at, the file is merely what was last written.
            if (reopeningEditor && editorBuffer != null) source = editorBuffer;
            for (String generatedHandler : generatedHandlers()) source = ensureHandler(source, generatedHandler);
            // Normalized, because the stub and the listener are both generated from the normalized
            // name. Passing the raw Events value here appended a second, invalid declaration for
            // anything that is not already an identifier, so "on-click" opened source that could
            // not compile and could then be saved in that state.
            if (handler != null) source = ensureHandler(source, javaIdentifier(handler));
            CodeEditor editor = new CodeEditor("java", source);
            activeCodeEditor = editor;
            trackEditorBuffer(editor, source);
            editor.setTheme(darkMode ? "dark" : "light");
            editor.setEditable(true);
            // No protected regions here. Marking the generated blocks read-only meant the caret
            // landing anywhere in them -- which is most of the file, and where it lands after a
            // click -- refused every keystroke, so the editor read as completely dead. It bought
            // nothing either: saveSourceAndModel keeps only the user region and regenerates the
            // rest from the model, so edits to generated code are discarded on save regardless.
            int userMarker = source.indexOf("// <gui-builder-user-code>");
            final int editableOffset = userMarker < 0 ? 0
                    : userMarker + "// <gui-builder-user-code>".length() + 1;
            Component stage = canvasHost.getComponentCount() == 0 ? new Label() : canvasHost.getComponentAt(0);
            final String reopenHandler = handler;
            Container editorPane = editorPane(handler == null ? "Companion Java source • edit inside USER CODE markers" : "Handler: " + handler, editor,
                    () -> editor.getText(value -> saveSourceAndModel(sourcePath, value)), this::closeEditorPane);
            activeEditorReopen = () -> openSourceEditor(reopenHandler);
            canvasHost.removeComponent(stage);
            SplitPane split = new SplitPane(SplitPane.HORIZONTAL_SPLIT, editorPane, stage, "20%", "58%", "85%");
            canvasHost.removeAll();
            canvasHost.add(BorderLayout.CENTER, split);
            canvasHost.revalidate();
            // Only now: requestFocus does nothing for a component that is not yet in the form.
            editor.onReady(() -> {
                editor.setCursorPosition(editableOffset);
                editor.focusEditor();
            });
            setStatus("Editing " + relativeFormName(document.path()) + " Java behavior"
                    + " - only the USER CODE region is kept on save");
        } catch (IOException ex) {
            ToastBar.showErrorMessage("Unable to open source: " + ex.getMessage());
        }
    }

    private Container editorPane(String title, CodeEditor editor, Runnable saveAction, Runnable closeAction) {
        Container pane = new Container(new BorderLayout());
        pane.setUIID("BuilderEditorPane");
        Container actions = new Container(new BorderLayout());
        actions.setUIID("BuilderEditorToolbar");
        actions.add(BorderLayout.CENTER, new Label(title, "BuilderEditorTitle"));
        Button close = new Button("Close");
        close.setUIID("BuilderSecondaryAction");
        close.addActionListener(e -> closeAction.run());
        Button save = new Button("Save");
        save.setUIID("BuilderPrimaryAction");
        save.addActionListener(e -> saveAction.run());
        actions.add(BorderLayout.WEST, close);
        actions.add(BorderLayout.EAST, save);
        pane.add(BorderLayout.NORTH, actions);
        pane.add(BorderLayout.CENTER, editor);
        return pane;
    }

    private void saveCss(String css, CodeEditor editor) {
        if (!applyProjectCss(css, editor)) return;
        try {
            ProjectIO.write(binding.cssFile(), css);
            editorBufferOnDisk = css;
            setStatus("Saved theme.css • preview updated");
        } catch (IOException ex) {
            ToastBar.showErrorMessage("CSS save failed: " + ex.getMessage());
        }
    }

    private boolean applyProjectCss(String css, CodeEditor editor) {
        try {
            MutableResource resources = new MutableResource();
            new CSSThemeCompiler().compile(css, resources, "ProjectTheme");
            projectTheme = resources.getTheme("ProjectTheme");
            normalizeCompiledTheme(projectTheme);
            editor.setDiagnostics(new ArrayList<CodeDiagnostic>());
            refreshProjectThemeOnPreview();
            setStatus("CSS compiled • live preview updated");
            return true;
        } catch (RuntimeException ex) {
            List<CodeDiagnostic> diagnostics = new ArrayList<>();
            diagnostics.add(new CodeDiagnostic(1, 1, ex.getMessage() == null ? "CSS compile error" : ex.getMessage()));
            editor.setDiagnostics(diagnostics);
            setStatus("CSS error • fix the highlighted problem before saving");
            return false;
        }
    }

    private void refreshProjectThemeOnPreview() {
        if (projectTheme == null) {
            setStatus("Project CSS is not loaded - the canvas is showing the builder's theme");
            return;
        }
        // Install the project theme into the canvas's own UIManager and leave the global one alone.
        // Swapping the global theme, refreshing, then swapping the builder's theme back meant the
        // preview only held the project styling for the instant of that call: every later
        // re-resolution -- a repaint, a revalidate, any refreshTheme cascade -- resolved against
        // the builder chrome again, so CSS edits appeared to do nothing.
        // A brand new manager each time rather than reinstalling props into the existing one.
        // Installing into a live manager left components resolving styles it had already cached,
        // so the canvas kept the previous look and the stylesheet appeared to have no effect.
        themeApplyCount++;
        previewUIManager = UIManager.createInstance();
        previewUIManager.setThemeProps(projectTheme);
        if (deviceSurface != null) deviceSurface.setUIManager(previewUIManager);
        if (previewRoot instanceof Container) {
            ((Container) previewRoot).setUIManager(previewUIManager);
        }
        if (previewRoot != null) {
            previewRoot.refreshTheme(false);
            ComponentPreviewFactory.stabilizeDesignStyles(previewRoot);
        }
        if (formToolbarPreview != null) {
            formToolbarPreview.refreshTheme(false);
            ComponentPreviewFactory.stabilizeDesignStyles(formToolbarPreview);
        }
        if (canvasHost != null) {
            canvasHost.revalidate();
            canvasHost.repaint();
        }
        // Repaint the form as well. A component only repaints itself when it is already painting
        // its own region, and the canvas sits inside a split pane whose parent decides the frame,
        // so restyling without asking the form for a new frame left the old pixels on screen.
        Form current = Display.getInstance().getCurrent();
        if (current != null) current.repaint();
    }

    private void loadProjectTheme() {
        if (binding == null || binding.cssFile() == null) return;
        try {
            MutableResource resources = new MutableResource();
            new CSSThemeCompiler().compile(ProjectIO.read(binding.cssFile()), resources, "ProjectTheme");
            projectTheme = resources.getTheme("ProjectTheme");
            normalizeCompiledTheme(projectTheme);
        } catch (Exception ex) {
            projectTheme = null;
            setStatus("Project CSS has errors: " + ex.getMessage());
        }
    }

    /**
     * Writes the companion Java source for the current document, regenerating everything outside
     * the user-code markers from the .gui model and keeping only what the developer wrote between
     * them.
     *
     * <p>The editor deliberately leaves the generated regions editable, because marking them read
     * only refused every keystroke wherever the caret happened to land. That makes regenerating
     * here the thing that keeps the promise the editor makes: edits to imports, buildUI or the
     * class braces cannot survive to diverge from the document or stop the file compiling.
     *
     * @param path the companion source path
     * @param userSource the text to carry the user-code region from -- the editor buffer when the
     *     code pane is saving, the file on disk when the designer is saving
     * @return true when the file reached disk
     */
    private boolean writeCompanionSource(String path, String userSource) {
        try {
            String generated = defaultCompanionSource();
            ProjectIO.write(path, userSource == null ? generated : mergeGeneratedSource(userSource, generated));
            return true;
        } catch (IOException ex) {
            ToastBar.showErrorMessage("Source save failed: " + ex.getMessage());
            return false;
        }
    }

    private void saveSource(String path, String source) {
        if (writeCompanionSource(path, source)) {
            // The file now matches what was written, not the raw buffer: the generated regions were
            // regenerated on the way out, so Close must compare against that.
            editorBufferOnDisk = editorBuffer;
            setStatus("Saved companion Java source");
        }
    }

    private void saveSourceAndModel(String path, String source) {
        saveSource(path, source);
        setStatus(ensureBindingModel(path)
                ? "Saved form source and created its binding model"
                : "Saved form source • existing model left unchanged");
    }

    /**
     * Writes the binding model the generated companion refers to, when the form uses a binding
     * strategy and the model is not already there.
     *
     * @param sourcePath the companion source path
     * @return true when a model was created by this call
     */
    private boolean ensureBindingModel(String sourcePath) {
        if ("none".equals(value(document.root(), "bindingStrategy", "properties"))) return false;
        String modelPath = sourcePath.substring(0, sourcePath.length() - 5) + "Model.java";
        if (ProjectIO.exists(modelPath)) return false;
        try {
            ProjectIO.write(modelPath, generatedModelSource());
            return true;
        } catch (IOException ex) {
            ToastBar.showErrorMessage("Model save failed: " + ex.getMessage());
            return false;
        }
    }

    private void normalizeCompiledTheme(Hashtable theme) {
        if (theme == null) return;
        List<Object> keys = new ArrayList<>();
        for (Object key : theme.keySet()) keys.add(key);
        for (Object keyObject : keys) {
            String key = String.valueOf(keyObject);
            Object raw = theme.get(keyObject);
            if (key.endsWith("." + Style.FONT) && raw instanceof String) {
                String font = (String) raw;
                if (font.length() > 1 && font.charAt(0) == '"' && font.charAt(font.length() - 1) == '"') {
                    font = font.substring(1, font.length() - 1);
                }
                try {
                    int style = font.indexOf("Bold") >= 0 ? Font.STYLE_BOLD : Font.STYLE_PLAIN;
                    if (font.indexOf("Italic") >= 0) style |= Font.STYLE_ITALIC;
                    theme.put(keyObject, Font.createTrueTypeFont(font, font).derive(Font.getDefaultFont().getHeight(), style));
                } catch (RuntimeException ex) {
                    theme.put(keyObject, Font.getDefaultFont());
                }
                continue;
            }
            boolean padding = key.endsWith("." + Style.PADDING);
            boolean margin = key.endsWith("." + Style.MARGIN);
            if ((!padding && !margin) || !(raw instanceof String)) continue;
            String[] values = ((String) raw).split(",");
            if (values.length != 4) continue;
            byte[] units = new byte[4];
            StringBuilder normalized = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                String item = values[i].trim();
                byte unit = Style.UNIT_TYPE_PIXELS;
                if (item.endsWith("mm")) { unit = Style.UNIT_TYPE_DIPS; item = item.substring(0, item.length() - 2); }
                else if (item.endsWith("px")) item = item.substring(0, item.length() - 2);
                else if (item.endsWith("%")) { unit = Style.UNIT_TYPE_SCREEN_PERCENTAGE; item = item.substring(0, item.length() - 1); }
                units[i] = unit;
                if (i > 0) normalized.append(',');
                normalized.append(item.length() == 0 ? "0" : item);
            }
            theme.put(keyObject, normalized.toString());
            String unitKey = key.substring(0, key.lastIndexOf('.') + 1) + (padding ? Style.PADDING_UNIT : Style.MARGIN_UNIT);
            theme.put(unitKey, units);
        }
    }

    private String companionSourcePath() {
        String relative = document.path().substring(ProjectIO.fsUrl(binding.guiDir()).length());
        if (relative.startsWith("/")) relative = relative.substring(1);
        return binding.sourceDir() + "/" + relative.substring(0, relative.length() - 4) + ".java";
    }

    private String defaultCompanionSource() {
        assignJavaNames();
        String form = relativeFormName(document.path());
        int dot = form.lastIndexOf('.');
        String packageName = dot < 0 ? "" : form.substring(0, dot);
        String className = dot < 0 ? form : form.substring(dot + 1);
        String modelName = className + "Model";
        String strategy = value(document.root(), "bindingStrategy", "properties");
        boolean bindingEnabled = !"none".equals(strategy);
        boolean annotationBinding = "bindable".equals(strategy);
        StringBuilder out = new StringBuilder();
        out.append("// <gui-builder-generated>\n");
        if (packageName.length() > 0) out.append("package ").append(packageName).append(";\n\n");
        out.append("import com.codename1.components.Accordion;\n")
                .append("import com.codename1.components.SpanLabel;\n")
                .append("import com.codename1.ui.*;\n")
                .append("import com.codename1.ui.events.ActionEvent;\n")
                .append("import com.codename1.ui.geom.Dimension;\n")
                .append("import com.codename1.ui.layouts.*;\n")
                .append("import com.codename1.ui.table.TableLayout;\n");
        if (bindingEnabled) out.append(annotationBinding
                ? "import com.codename1.binding.Binding;\nimport com.codename1.binding.Binders;\n"
                : "import com.codename1.properties.UiBinding;\n");
        // A .gui whose root is a Container is a reusable piece of UI, not a screen, and a Dialog is
        // neither. Generating "extends Form" for all three produced a class the project could not
        // use as the type it was designed as. Dialog takes the same (title, layout) constructor Form
        // does; Container takes the layout alone.
        String rootType = value(document.root(), "type", "Form");
        boolean containerRoot = "Container".equals(rootType);
        String superClass = containerRoot ? "Container" : "Dialog".equals(rootType) ? "Dialog" : "Form";
        String superCall = containerRoot
                ? "super(" + layoutSource(document.root()) + ");\n"
                : "super(\"" + javaEscape(value(document.root(), "title", className)) + "\", "
                        + layoutSource(document.root()) + ");\n";
        out.append("\n// Generated live from ").append(relativeFormName(document.path())).append(".gui.\n")
                .append("public class ").append(className).append(" extends ").append(superClass).append(" {\n");
        if (bindingEnabled) {
            out.append("    private final ").append(modelName).append(" model;\n")
                    .append(annotationBinding ? "    private Binding binding;\n" : "    private UiBinding.Binding binding;\n");
        }
        for (Element element : document.components()) {
            if (element != document.root()) out.append("    private ").append(javaType(element)).append(" ")
                    .append(javaName(element)).append(";\n");
        }
        if (bindingEnabled) {
            out.append("\n    public ").append(className).append("() { this(new ").append(modelName).append("()); }\n\n")
                    .append("    public ").append(className).append("(").append(modelName).append(" model) {\n")
                    .append("        ").append(superCall)
                    .append("        this.model = model;\n        buildUI();\n")
                    .append(annotationBinding ? "        binding = Binders.bind(model, this);\n    }\n\n"
                            : "        binding = new UiBinding().bind(model, this);\n    }\n\n");
        } else {
            out.append("\n    public ").append(className).append("() {\n")
                    .append("        ").append(superCall).append("        buildUI();\n    }\n\n");
        }
        out.append("    private void buildUI() {\n");
        // The root's own inspector properties belong here. Emitting only the descendants meant a
        // reusable Container root could have its UIID, RTL, visibility or scrolling set in the
        // designer and shown in the preview while the runtime instance kept the defaults.
        if (document.root().getAttribute("uiid") != null) {
            out.append("        setUIID(\"").append(javaEscape(document.root().getAttribute("uiid"))).append("\");\n");
        }
        appendGeneratedProperties(out, document.root(), "this", rootType, "        ");
        appendGeneratedChildren(out, document.root(), "this", "        ");
        for (Element command : document.commands()) {
            String placement = value(command, "placement", "right");
            String method = "left".equals(placement) ? "addCommandToLeftBar" : "overflow".equals(placement)
                    ? "addCommandToOverflowMenu" : "side".equals(placement) ? "addCommandToSideMenu" : "addCommandToRightBar";
            out.append("        getToolbar().").append(method).append("(\"")
                    .append(javaEscape(value(command, "name", "Command"))).append("\", null, this::")
                    .append(javaIdentifier(value(command, "actionEvent", "onCommand"))).append(");\n");
        }
        out.append("    }\n\n");
        if (bindingEnabled) out.append("    public ").append(modelName).append(" getModel() { return model; }\n\n");
        out.append("// </gui-builder-generated>\n")
                .append("// <gui-builder-user-code>\n");
        for (String handler : generatedHandlers()) {
            out.append(handlerStub(handler));
        }
        return out.append("// </gui-builder-user-code>\n")
                .append("// <gui-builder-generated>\n}\n// </gui-builder-generated>\n").toString();
    }

    private void appendGeneratedChildren(StringBuilder out, Element parent, String parentName, String indent) {
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object value = parent.getChildAt(i);
            if (!(value instanceof Element) || !"component".equals(((Element) value).getTagName())) continue;
            String name = javaName(((Element) value));
            String type = value(((Element) value), "type", "Container");
            out.append(indent).append(name).append(" = ").append(componentSource(((Element) value))).append(";\n")
                    .append(indent).append(name).append(".setName(\"").append(javaEscape(value(((Element) value), "name", name))).append("\");\n");
            if (((Element) value).getAttribute("uiid") != null) out.append(indent).append(name).append(".setUIID(\"")
                    .append(javaEscape(((Element) value).getAttribute("uiid"))).append("\");\n");
            appendHint(out, ((Element) value), name, type, indent);
            appendGeneratedProperties(out, ((Element) value), name, type, indent);
            String handler = ((Element) value).getAttribute("actionEvent");
            if (handler != null && handler.length() > 0 && firesActionEvents(type)) {
                out.append(indent).append(name).append(".addActionListener(this::").append(javaIdentifier(handler)).append(");\n");
            }
            if ("Tabs".equals(value(parent, "type", ""))) {
                out.append(indent).append(parentName).append(".addTab(\"").append(javaEscape(value(((Element) value), "name", "Tab")))
                        .append("\", ").append(name).append(");\n");
            } else if ("Accordion".equals(value(parent, "type", ""))) {
                out.append(indent).append(parentName).append(".addContent(\"")
                        .append(javaEscape(value(((Element) value), "name", "Section"))).append("\", ").append(name).append(");\n");
            } else if ("BorderLayout".equals(value(parent, "layout", "BoxLayout"))) {
                out.append(indent).append(parentName).append(".add(BorderLayout.")
                        .append(GuiDocument.effectiveBorderConstraint(parent, ((Element) value)).toUpperCase()).append(", ").append(name).append(");\n");
            } else if ("TableLayout".equals(value(parent, "layout", "BoxLayout"))) {
                out.append(indent).append(parentName).append(".add(((TableLayout) ").append(parentName)
                        .append(".getLayout()).createConstraint(")
                        .append(GuiDocument.effectiveTableRow(parent, ((Element) value)))
                        .append(", ").append(GuiDocument.effectiveTableColumn(parent, ((Element) value))).append(")")
                        .append(".horizontalSpan(").append(value(((Element) value), "tableHorizontalSpan", "1")).append(")")
                        .append(".verticalSpan(").append(value(((Element) value), "tableVerticalSpan", "1")).append(")")
                        // The preview honours these percentages, so a form designed against them
                        // collapses to preferred-size columns at runtime when they are dropped here.
                        .append(percentageConstraint(((Element) value), "tableWidth", "widthPercentage"))
                        .append(percentageConstraint(((Element) value), "tableHeight", "heightPercentage"))
                        .append(", ").append(name).append(");\n");
            } else out.append(indent).append(parentName).append(".add(").append(name).append(");\n");
            if (GuiDocument.acceptsChildren(((Element) value))) appendGeneratedChildren(out, ((Element) value), name, indent);
            appendGeneratedTabState(out, ((Element) value), name, type, indent);
        }
        if ("LayeredLayout".equals(value(parent, "layout", "BoxLayout"))) {
            for (Element child : componentChildren(parent)) {
                String name = javaName(child);
                String[] refs = GuidedLayoutSupport.referenceNames(child);
                out.append(indent).append("((LayeredLayout) ").append(parentName).append(".getLayout())")
                        .append(".setInsets(").append(name).append(", \"")
                        .append(javaEscape(value(child, "layeredInsets", "auto auto auto auto"))).append("\")")
                        .append(".setReferenceComponents(").append(name);
                for (int side = 0; side < 4; side++) {
                    Element reference = namedSibling(parent, refs[side]);
                    out.append(", ").append(reference == null ? "null" : javaName(reference));
                }
                out.append(")")
                        .append(".setReferencePositions(").append(name).append(", \"")
                        .append(javaEscape(value(child, "guidedReferencePositions", "0 0 0 0"))).append("\")")
                        .append(".setPercentInsetAnchorHorizontal(").append(name).append(", ")
                        .append(value(child, "guidedHorizontalAnchor", "0")).append("f)")
                        .append(".setPercentInsetAnchorVertical(").append(name).append(", ")
                        .append(value(child, "guidedVerticalAnchor", "0")).append("f);\n");
            }
        }
    }

    /**
     * Emits the inspector properties that the preview applies through
     * {@code ComponentPreviewFactory.applyAttributes}. Without these the designer showed a component
     * as configured while the generated form fell back to every default, so the running app did not
     * match the design. Only attributes the document actually carries are emitted, and only for the
     * types that really own the setter, so the generated source stays readable and always compiles.
     */
    private void appendGeneratedProperties(StringBuilder out, Element element, String name, String type,
            String indent) {
        appendBooleanSetter(out, element, name, indent, "enabled", "setEnabled");
        appendBooleanSetter(out, element, name, indent, "visible", "setVisible");
        appendBooleanSetter(out, element, name, indent, "rtl", "setRTL");
        if (isLabelType(type) || "SpanLabel".equals(type)) {
            appendIntSetter(out, element, name, indent, "gap", "setGap");
        }
        if (isLabelType(type) || "TextArea".equals(type) || "TextField".equals(type)) {
            String alignment = element.getAttribute("alignment");
            if (alignment != null) out.append(indent).append(name).append(".setAlignment(Component.")
                    .append(alignmentConstant(alignment)).append(");\n");
        }
        if (isLabelType(type)) {
            appendBooleanSetter(out, element, name, indent, "tickerEnabled", "setTickerEnabled");
        }
        if ("Button".equals(type)) appendBooleanSetter(out, element, name, indent, "toggle", "setToggle");
        if ("CheckBox".equals(type) || "RadioButton".equals(type)) {
            appendBooleanSetter(out, element, name, indent, "selected", "setSelected");
        }
        if ("TextField".equals(type) || "TextArea".equals(type)) {
            appendIntSetter(out, element, name, indent, "columns", "setColumns");
            appendIntSetter(out, element, name, indent, "maxSize", "setMaxSize");
            appendBooleanSetter(out, element, name, indent, "editable", "setEditable");
            appendBooleanSetter(out, element, name, indent, "growByContent", "setGrowByContent");
            String constraint = element.getAttribute("constraint");
            if (constraint != null) out.append(indent).append(name).append(".setConstraint(TextArea.")
                    .append(constraintConstant(constraint)).append(");\n");
        }
        if ("TextArea".equals(type)) appendIntSetter(out, element, name, indent, "rows", "setRows");
        if ("Slider".equals(type)) {
            appendIntSetter(out, element, name, indent, "minValue", "setMinValue");
            appendIntSetter(out, element, name, indent, "maxValue", "setMaxValue");
            appendIntSetter(out, element, name, indent, "progress", "setProgress");
            appendBooleanSetter(out, element, name, indent, "editable", "setEditable");
            appendBooleanSetter(out, element, name, indent, "infinite", "setInfinite");
        }
        if (GuiDocument.acceptsChildren(element)) {
            appendBooleanSetter(out, element, name, indent, "scrollableX", "setScrollableX");
            appendBooleanSetter(out, element, name, indent, "scrollableY", "setScrollableY");
        }
    }

    /**
     * Tab placement and selection are emitted after the tabs themselves, because selecting an index
     * on an empty Tabs throws.
     */
    private void appendGeneratedTabState(StringBuilder out, Element element, String name, String type,
            String indent) {
        if (!"Tabs".equals(type)) return;
        String placement = element.getAttribute("tabPlacement");
        if (placement != null) out.append(indent).append(name).append(".setTabPlacement(Component.")
                .append(tabPlacementConstant(placement)).append(");\n");
        Integer selected = parseInteger(element.getAttribute("selectedIndex"));
        int tabs = componentChildren(element).size();
        if (selected != null && selected.intValue() >= 0 && selected.intValue() < tabs) {
            out.append(indent).append(name).append(".setSelectedIndex(").append(selected.intValue()).append(", false);\n");
        }
    }

    private void appendBooleanSetter(StringBuilder out, Element element, String name, String indent,
            String attribute, String setter) {
        String raw = element.getAttribute(attribute);
        if (raw == null) return;
        out.append(indent).append(name).append('.').append(setter).append('(')
                .append("true".equals(raw) ? "true" : "false").append(");\n");
    }

    private void appendIntSetter(StringBuilder out, Element element, String name, String indent,
            String attribute, String setter) {
        Integer parsed = parseInteger(element.getAttribute(attribute));
        if (parsed == null) return;
        out.append(indent).append(name).append('.').append(setter).append('(').append(parsed.intValue()).append(");\n");
    }

    private String percentageConstraint(Element element, String attribute, String method) {
        Integer parsed = parseInteger(element.getAttribute(attribute));
        return parsed == null || parsed.intValue() < 0 ? "" : "." + method + "(" + parsed.intValue() + ")";
    }

    private boolean isLabelType(String type) {
        return "Label".equals(type) || "Button".equals(type) || "CheckBox".equals(type)
                || "RadioButton".equals(type);
    }

    private static String alignmentConstant(String value) {
        if ("center".equalsIgnoreCase(value)) return "CENTER";
        if ("right".equalsIgnoreCase(value)) return "RIGHT";
        return "LEFT";
    }

    private static String tabPlacementConstant(String value) {
        if ("bottom".equalsIgnoreCase(value)) return "BOTTOM";
        if ("left".equalsIgnoreCase(value)) return "LEFT";
        if ("right".equalsIgnoreCase(value)) return "RIGHT";
        return "TOP";
    }

    private static String constraintConstant(String value) {
        if ("EMAILADDR".equals(value) || "PASSWORD".equals(value) || "NUMERIC".equals(value)
                || "URL".equals(value)) {
            return value;
        }
        return "ANY";
    }

    private String handlerStub(String handler) {
        return "    protected void " + handler + "(ActionEvent event) {\n"
                + "        // Add behavior here.\n    }\n\n";
    }

    private String generatedModelSource() {
        assignJavaNames();
        String form = relativeFormName(document.path());
        int dot = form.lastIndexOf('.');
        String packageName = dot < 0 ? "" : form.substring(0, dot);
        String className = (dot < 0 ? form : form.substring(dot + 1)) + "Model";
        String strategy = value(document.root(), "bindingStrategy", "properties");
        List<Element> bindable = new ArrayList<>();
        for (Element element : document.components()) {
            String type = value(element, "type", "");
            if ("TextField".equals(type) || "TextArea".equals(type) || "CheckBox".equals(type) || "RadioButton".equals(type)) bindable.add(element);
        }
        StringBuilder out = new StringBuilder();
        if (packageName.length() > 0) out.append("package ").append(packageName).append(";\n\n");
        if ("none".equals(strategy)) {
            return out.append("// Data binding is disabled for this form.\n")
                    .append("public class ").append(className).append(" {\n}\n").toString();
        }
        if ("bindable".equals(strategy)) {
            out.append("import com.codename1.annotations.Bind;\n")
                    .append("import com.codename1.annotations.Bindable;\n")
                    .append("import com.codename1.binding.BindAttr;\n\n")
                    .append("@Bindable\npublic class ").append(className).append(" {\n");
            for (Element element : bindable) {
                String type = value(element, "type", "");
                boolean selected = "CheckBox".equals(type) || "RadioButton".equals(type);
                String name = javaName(element);
                String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                out.append("    @Bind(name = \"").append(javaEscape(value(element, "name", name)))
                        .append("\", attr = BindAttr.").append(selected ? "SELECTED" : "TEXT").append(")\n")
                        .append("    private ").append(selected ? "boolean" : "String").append(" ").append(name).append(" = ")
                        .append(selected ? String.valueOf("true".equals(value(element, "selected", "false")))
                                : "\"" + javaEscape(value(element, "text", "")) + "\"").append(";\n\n")
                        .append("    public ").append(selected ? "boolean is" : "String get").append(cap).append("() { return ")
                        .append(name).append("; }\n")
                        .append("    public void set").append(cap).append("(").append(selected ? "boolean" : "String")
                        .append(" value) { this.").append(name).append(" = value; }\n\n");
            }
            return out.append("}\n").toString();
        }
        out.append("import com.codename1.properties.*;\n\npublic class ").append(className)
                .append(" implements PropertyBusinessObject {\n");
        for (Element element : bindable) {
            String type = value(element, "type", "");
            boolean selected = "CheckBox".equals(type) || "RadioButton".equals(type);
            out.append("    public final Property<").append(selected ? "Boolean" : "String").append(", ").append(className).append("> ")
                    .append(javaName(element)).append(" = new Property<>(\"").append(javaEscape(value(element, "name", javaName(element))))
                    .append("\", ").append(selected ? String.valueOf("true".equals(value(element, "selected", "false")))
                            : "\"" + javaEscape(value(element, "text", "")) + "\"").append(");\n");
        }
        out.append("    private final PropertyIndex index = new PropertyIndex(this, \"").append(className).append("\"");
        for (Element element : bindable) out.append(", ").append(javaName(element));
        return out.append(");\n    @Override public PropertyIndex getPropertyIndex() { return index; }\n}\n").toString();
    }

    /**
     * True for the root types that carry a title and a toolbar. {@code cn1:create-gui-form
     * -DguiType=Dialog} is explicitly supported and the generator already keeps Dialog as the
     * superclass, so a Form-only test left those forms unable to edit the very attributes the
     * generated source consumes.
     *
     * @param type the {@code type} attribute of the element
     * @return true when the element behaves like a form
     */
    /**
     * True for the component types that expose {@code addActionListener}. The Events tab offers a
     * handler for all of them and a stub is generated either way, so leaving text inputs and
     * sliders out here produced a form that compiled while the handler was never invoked.
     *
     * @param type the {@code type} attribute of the element
     * @return true when a listener can be registered on this type
     */
    private static boolean firesActionEvents(String type) {
        return "Button".equals(type) || "CheckBox".equals(type) || "RadioButton".equals(type)
                || "TextField".equals(type) || "TextArea".equals(type) || "Slider".equals(type);
    }

    private static boolean isFormLike(String type) {
        return "Form".equals(type) || "Dialog".equals(type);
    }

    private String javaType(Element element) {
        String type = value(element, "type", "Container");
        return isFormLike(type) ? "Container" : type;
    }

    /**
     * Appends the hint for a text component. The inspector offers Hint for TextArea and applies it
     * live, so omitting it from the generated source made it vanish on the next canvas rebuild and
     * never reach the compiled form.
     *
     * @param out the source being built
     * @param element the component element
     * @param name the generated field name
     * @param type the component type
     * @param indent the current indent
     */
    private void appendHint(StringBuilder out, Element element, String name, String type, String indent) {
        if (!"TextArea".equals(type)) return;
        String hint = element.getAttribute("hint");
        if (hint == null || hint.length() == 0) return;
        out.append(indent).append(name).append(".setHint(\"").append(javaEscape(hint)).append("\");\n");
    }

    private String componentSource(Element element) {
        String type = value(element, "type", "Container");
        String text = "\"" + javaEscape(value(element, "text", "")) + "\"";
        String source;
        if ("Container".equals(type)) source = "new Container(" + layoutSource(element) + ")";
        else if ("Tabs".equals(type)) source = "new Tabs()";
        // Accordion is a child-accepting type the document model already supports, and it lives in
        // com.codename1.components with no String constructor, so the generic "new Type(text)"
        // fallback produced source that does not compile.
        else if ("Accordion".equals(type)) source = "new Accordion()";
        else if ("Slider".equals(type)) source = "new Slider()";
        else if ("TextField".equals(type)) source = "new TextField(" + text + ", \"" + javaEscape(value(element, "hint", "")) + "\")";
        else if ("TextArea".equals(type)) source = "new TextArea(" + text + ")";
        else source = "new " + type + "(" + text + ")";
        String fixedWidth = element.getAttribute("guidedPreferredWidth");
        String fixedHeight = element.getAttribute("guidedPreferredHeight");
        if (fixedWidth == null && fixedHeight == null) return source;
        return source + " { @Override protected Dimension calcPreferredSize() { "
                + "Dimension size = super.calcPreferredSize(); "
                + (fixedWidth == null ? "" : "size.setWidth(" + fixedWidth + "); ")
                + (fixedHeight == null ? "" : "size.setHeight(" + fixedHeight + "); ")
                + "return size; } }";
    }

    private String layoutSource(Element element) {
        String layout = value(element, "layout", "BoxLayout");
        if ("BorderLayout".equals(layout)) return "new BorderLayout()";
        if ("FlowLayout".equals(layout)) return "new FlowLayout()";
        if ("GridLayout".equals(layout)) return "new GridLayout(" + value(element, "gridLayoutRows", "1") + ", " + value(element, "gridLayoutColumns", "2") + ")";
        if ("TableLayout".equals(layout)) return "new TableLayout(" + value(element, "tableLayoutRows", "2") + ", " + value(element, "tableLayoutColumns", "2") + ")";
        if ("LayeredLayout".equals(layout)) return "new LayeredLayout()";
        return "X".equals(value(element, "boxLayoutAxis", "Y")) ? "BoxLayout.x()" : "BoxLayout.y()";
    }

    private List<String> generatedHandlers() {
        List<String> handlers = new ArrayList<>();
        for (Element element : document.components()) {
            String handler = element.getAttribute("actionEvent");
            if (handler != null && handler.length() > 0 && !handlers.contains(javaIdentifier(handler))) handlers.add(javaIdentifier(handler));
        }
        for (Element command : document.commands()) {
            String handler = javaIdentifier(value(command, "actionEvent", "onCommand"));
            if (!handlers.contains(handler)) handlers.add(handler);
        }
        return handlers;
    }

    /**
     * The Java name of a component, unique across the document. A .gui may legitimately name two
     * components "foo-bar" and "foo_bar", or name one "class"; both produce companion source that
     * does not compile unless the names are disambiguated here.
     */
    private String javaName(Element element) {
        String assigned = javaNames.get(element);
        return assigned != null ? assigned : javaIdentifier(value(element, "name", value(element, "type", "component")));
    }

    /** Recomputed for every generation because a rename changes every name after it. */
    private void assignJavaNames() {
        javaNames = new LinkedHashMap<>();
        Set<String> used = new LinkedHashSet<>();
        // the names the generated class itself declares
        used.add("model");
        used.add("binding");
        used.add("buildUI");
        // The PropertyBusinessObject model declares its own PropertyIndex called index, so a
        // bindable control named "index" produced a model with two fields of that name.
        used.add("index");
        for (Element element : document.components()) {
            if (element == document.root()) continue;
            String base = javaIdentifier(value(element, "name", value(element, "type", "component")));
            String candidate = base;
            int suffix = 2;
            while (!used.add(candidate)) {
                candidate = base + suffix;
                suffix++;
            }
            javaNames.put(element, candidate);
        }
    }

    private String javaIdentifier(String value) {
        String cleaned = value == null ? "component" : value.replaceAll("[^A-Za-z0-9_$]", "_");
        if (cleaned.length() == 0) return "component";
        char first = cleaned.charAt(0);
        boolean validStart = first == '_' || first == '$' || first >= 'A' && first <= 'Z' || first >= 'a' && first <= 'z';
        String identifier = validStart ? cleaned : "_" + cleaned;
        // A component may legitimately be called "class" or "new"; the field or method generated
        // from it may not.
        return isJavaReserved(identifier) ? identifier + "_" : identifier;
    }

    private static final String JAVA_RESERVED = "|abstract|assert|boolean|break|byte|case|catch|"
            + "char|class|const|continue|default|do|double|else|enum|extends|false|final|finally|"
            + "float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|"
            + "package|private|protected|public|return|short|static|strictfp|super|switch|"
            + "synchronized|this|throw|throws|transient|true|try|void|volatile|while|_|var|record|"
            + "sealed|permits|yield|";

    private static boolean isJavaReserved(String identifier) {
        return JAVA_RESERVED.indexOf("|" + identifier + "|") >= 0;
    }
    private String javaEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String ensureHandler(String source, String handler) {
        if (source.indexOf(handler + "(") >= 0) return source;
        int close = source.indexOf("// </gui-builder-user-code>");
        if (close < 0) close = source.lastIndexOf('}');
        String method = "\n    private void " + handler + "(ActionEvent event) {\n"
                + "        // Add event behavior here.\n"
                + "    }\n";
        return close < 0 ? source + method : source.substring(0, close) + method + source.substring(close);
    }

    private String mergeGeneratedSource(String existing, String generated) {
        String startMarker = "// <gui-builder-user-code>";
        String endMarker = "// </gui-builder-user-code>";
        int oldStart = existing.indexOf(startMarker);
        int oldEnd = existing.indexOf(endMarker);
        if (oldStart < 0 || oldEnd < oldStart) {
            if (existing.indexOf("// Generated live from ") >= 0) return generated;
            String migrated = migrateLegacySource(existing, generated);
            return migrated != null ? migrated : existing;
        }
        int newStart = generated.indexOf(startMarker);
        int newEnd = generated.indexOf(endMarker);
        if (newStart < 0 || newEnd < newStart) return generated;
        String userCode = existing.substring(oldStart + startMarker.length(), oldEnd);
        return generated.substring(0, newStart + startMarker.length()) + userCode + generated.substring(newEnd);
    }

    static final String LEGACY_GENERATED_START = "//-- DON'T EDIT BELOW THIS LINE!!!";
    static final String LEGACY_GENERATED_END = "//-- DON'T EDIT ABOVE THIS LINE!!!";

    /**
     * Rewrites a companion source scaffolded by an older {@code cn1:create-gui-form} into this
     * editor's format. Those files carry {@code //-- DON'T EDIT} markers and an empty
     * {@code initGuiBuilderComponents}, which this editor does not write to, so without this a form
     * designed in a project created by the old scaffolder saved its .gui and produced an empty
     * screen at runtime. Anything the developer added to the class is carried into the user-code
     * region; the old constructors and the old generated block are dropped because the new
     * generated region replaces both.
     *
     * @return the migrated source, or null when this is not a legacy scaffolded file
     */
    String migrateLegacySource(String existing, String generated) {
        int legacyStart = existing.indexOf(LEGACY_GENERATED_START);
        int legacyEnd = existing.indexOf(LEGACY_GENERATED_END);
        if (legacyStart < 0 || legacyEnd < legacyStart) return null;
        String carried = legacyUserMembers(existing, legacyStart, legacyEnd);
        String marker = "// <gui-builder-user-code>";
        if (carried.length() == 0) return carriedImports(existing, generated);
        String merged = carriedImports(existing, dropStubsAlreadyWritten(generated, carried));
        int insert = merged.indexOf(marker);
        if (insert < 0) return merged;
        insert += marker.length();
        return merged.substring(0, insert) + "\n" + carried + "\n" + merged.substring(insert);
    }

    /**
     * Adds the legacy file's own imports to the generated header. Migration carries the class body
     * across but the header is regenerated, so a user method that depended on a custom import
     * compiled before the migration and not after it -- an existing project would stop building on
     * the first save in this editor.
     *
     * @param existing the legacy companion source
     * @param generated the freshly generated source to add the imports to
     * @return the generated source with any imports it was missing
     */
    private String carriedImports(String existing, String generated) {
        int classAt = classDeclaration(existing);
        String header = classAt < 0 ? existing : existing.substring(0, classAt);
        StringBuilder missing = new StringBuilder();
        for (String line : header.split("\n")) {
            String statement = line.trim();
            if (!statement.startsWith("import ") || !statement.endsWith(";")) continue;
            if (generated.indexOf(statement) >= 0) continue;
            missing.append(statement).append('\n');
        }
        if (missing.length() == 0) return generated;
        int anchor = generated.lastIndexOf("\nimport ");
        if (anchor < 0) return generated;
        int endOfImports = generated.indexOf('\n', anchor + 1);
        if (endOfImports < 0) return generated;
        return generated.substring(0, endOfImports + 1) + missing + generated.substring(endOfImports + 1);
    }

    /**
     * Removes the empty handler stubs for events the migrated code already implements. The old
     * scaffold kept the developer's handlers outside its generated block, so carrying them over
     * without this produced a class declaring the same method twice.
     */
    private String dropStubsAlreadyWritten(String generated, String carried) {
        String out = generated;
        for (String handler : generatedHandlers()) {
            if (carried.indexOf(handler + "(") < 0) continue;
            out = out.replace(handlerStub(handler), "");
        }
        return out;
    }

    private String legacyUserMembers(String existing, int legacyStart, int legacyEnd) {
        int bodyStart = existing.indexOf('{', classDeclaration(existing));
        int bodyEnd = existing.lastIndexOf('}');
        if (bodyStart < 0 || bodyEnd <= bodyStart) return "";
        String body = existing.substring(bodyStart + 1, bodyEnd);
        int offset = bodyStart + 1;
        if (legacyStart > offset && legacyEnd > legacyStart) {
            int endOfLine = existing.indexOf('\n', legacyEnd);
            int cut = (endOfLine < 0 ? existing.length() : endOfLine + 1) - offset;
            body = body.substring(0, legacyStart - offset) + body.substring(Math.min(cut, body.length()));
        }
        return removeConstructors(body, className(existing)).trim();
    }

    private int classDeclaration(String source) {
        int index = source.indexOf("class ");
        return index < 0 ? 0 : index;
    }

    private String className(String source) {
        int index = classDeclaration(source);
        if (index == 0 && !source.startsWith("class ")) return "";
        int start = index + "class ".length();
        int end = start;
        while (end < source.length() && isIdentifierChar(source.charAt(end))) end++;
        return source.substring(start, end);
    }

    private static boolean isIdentifierChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '$';
    }

    /**
     * Drops every constructor of the scaffolded class. The generated region declares its own, and
     * the scaffolded ones call an {@code initGuiBuilderComponents} that no longer exists.
     */
    private String removeConstructors(String body, String className) {
        if (className.length() == 0) return body;
        String out = body;
        int from = 0;
        while (true) {
            int index = out.indexOf(className + "(", from);
            if (index < 0) return out;
            char before = index == 0 ? ' ' : out.charAt(index - 1);
            int close = matchingBrace(out, out.indexOf(")", index));
            boolean instantiation = index >= 4 && "new ".equals(out.substring(index - 4, index));
            if (isIdentifierChar(before) || before == '.' || instantiation || close < 0) {
                from = index + className.length();
                continue;
            }
            int lineStart = out.lastIndexOf("\n", index);
            int cut = lineStart < 0 ? 0 : lineStart;
            out = out.substring(0, cut) + out.substring(close + 1);
            from = cut;
        }
    }

    /**
     * Finds the closing brace of the block that starts after {@code afterIndex}, ignoring braces
     * inside string and character literals so a constructor that builds text is not truncated.
     */
    private int matchingBrace(String source, int afterIndex) {
        if (afterIndex < 0) return -1;
        int depth = 0;
        boolean started = false;
        char quote = 0;
        for (int i = afterIndex; i < source.length(); i++) {
            char c = source.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
                continue;
            }
            if (c == '"' || c == '\'') { quote = c; continue; }
            if (c == '{') { depth++; started = true; }
            else if (c == '}') {
                depth--;
                if (started && depth == 0) return i;
                if (depth < 0) return -1;
            } else if (c == ';' && !started) {
                return -1;
            }
        }
        return -1;
    }

    private String resolveInitialForm() {
        String requested = binding.initialForm();
        if (requested == null || requested.length() == 0) return null;
        for (String file : guiFiles) {
            if (file.endsWith(requested.replace('.', '/') + ".gui") || file.endsWith(requested + ".gui")) return file;
        }
        return null;
    }

    private String relativeFormName(String path) {
        String base = binding == null || binding.guiDir() == null ? "" : ProjectIO.fsUrl(binding.guiDir());
        String relative = path.startsWith(base) ? path.substring(base.length()) : path;
        if (relative.startsWith("/")) relative = relative.substring(1);
        return relative.endsWith(".gui") ? relative.substring(0, relative.length() - 4).replace('/', '.') : relative;
    }

    private Button iconButton(char icon, String tooltip, Runnable action) {
        Button button = new Button(material(icon, "BuilderCanvasIcon"));
        button.setUIID("BuilderCanvasButton");
        button.setName(tooltip);
        button.setAccessibilityText(tooltip);
        button.getSemantics().setIdentifier("guibuilder.canvasMode." + semanticKey(tooltip)).setLabel(tooltip);
        button.addActionListener(e -> action.run());
        return button;
    }

    private Label sectionTitle(String title) {
        Label label = new Label(title, "BuilderSectionTitle");
        label.getSemantics().setRole(AccessibilityRole.HEADING).setHeadingLevel(2).setLabel(title);
        return label;
    }
    private boolean hasText(String type) {
        return "Label".equals(type) || "SpanLabel".equals(type) || "Button".equals(type)
                || "CheckBox".equals(type) || "RadioButton".equals(type)
                || "TextField".equals(type) || "TextArea".equals(type);
    }
    /**
     * The fallback applies to an attribute that is absent, not to one the user deliberately
     * emptied: clearing a label's text must leave it empty rather than bringing the sample text
     * back. Structural attributes are removed rather than blanked, so they are unaffected.
     */
    private static String value(Element element, String attribute, String fallback) {
        String result = element == null ? null : element.getAttribute(attribute);
        return result == null ? fallback : result;
    }
    private FontImage material(char icon, String uiid) { return FontImage.createMaterial(icon, UIManager.getInstance().getComponentStyle(uiid)); }
    private void setStatus(String message) {
        String previous = status == null ? null : status.getText();
        if (status != null) status.setText(message);
        if (previous == null || !previous.equals(message)) recordAction("status", "message", message);
    }

    private static String snapReferenceName(SnapResult snap) {
        return snap == null || snap.reference == null ? null
                : value(snap.reference, "name", value(snap.reference, "type", "component"));
    }

    private void recordAction(String kind, Object... values) {
        if (mcpController == null) return;
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        for (int i = 0; values != null && i + 1 < values.length; i += 2) {
            if (values[i] != null && values[i + 1] != null) details.put(String.valueOf(values[i]), values[i + 1]);
        }
        if (document != null) {
            details.put("form", relativeFormName(document.path()));
            Element selected = document.selected();
            if (selected != null) details.put("selected", value(selected, "name", value(selected, "type", "component")));
        }
        mcpController.record(kind, details);
    }

    Map<String, Object> mcpState(long latestSequence) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("latestSequence", Long.valueOf(latestSequence));
        out.put("canvasMode", canvasMode);
        out.put("darkMode", Boolean.valueOf(darkMode));
        out.put("status", status == null ? null : status.getText());
        List<Object> forms = new ArrayList<Object>();
        for (String path : guiFiles) forms.add(relativeFormName(path));
        out.put("forms", forms);
        if (canvasHost != null) out.put("canvasBounds", componentBounds(canvasHost));
        out.put("previewUIManager", String.valueOf(System.identityHashCode(previewUIManager)));
        out.put("globalUIManager", String.valueOf(System.identityHashCode(UIManager.getInstance())));
        out.put("projectThemeLoaded", Boolean.valueOf(projectTheme != null));
        out.put("themeApplyCount", Integer.valueOf(themeApplyCount));
        // What each manager resolves for a plain Label, so the stylesheet on disk, the compiled
        // theme and what the canvas resolves can be compared from outside the process.
        out.put("previewLabelFg", String.format("%06x",
                Integer.valueOf(previewUIManager.getComponentStyle("Label").getFgColor())));
        out.put("globalLabelFg", String.format("%06x",
                Integer.valueOf(UIManager.getInstance().getComponentStyle("Label").getFgColor())));
        if (projectTheme != null) {
            out.put("projectThemeLabelFg", String.valueOf(projectTheme.get("Label.fgColor")));
            out.put("projectThemeKeys", Integer.valueOf(projectTheme.size()));
        }
        if (binding != null) out.put("cssFile", binding.cssFile());
        if (document == null) return out;
        normalizeSelection();
        out.put("activeForm", relativeFormName(document.path()));
        out.put("path", document.path());
        out.put("modified", Boolean.valueOf(document.isModified()));
        out.put("canUndo", Boolean.valueOf(document.canUndo()));
        out.put("canRedo", Boolean.valueOf(document.canRedo()));
        Element selected = document.selected();
        out.put("selected", selected == null ? null : value(selected, "name", value(selected, "type", "component")));
        List<Object> selection = new ArrayList<Object>();
        for (Element element : selectedElements) selection.add(value(element, "name", value(element, "type", "component")));
        out.put("selectedComponents", selection);
        int[] selectionPaintBounds = dragGuideOverlay == null ? null : dragGuideOverlay.selectionPaintBounds();
        if (selectionPaintBounds != null) out.put("selectionPaintBounds", rect(selectionPaintBounds[0],
                selectionPaintBounds[1], selectionPaintBounds[2], selectionPaintBounds[3]));
        if (dragGuideOverlay != null) {
            List<Object> selectionBounds = new ArrayList<Object>();
            for (int[] bounds : dragGuideOverlay.selectionPaintBoundsList()) {
                selectionBounds.add(rect(bounds[0], bounds[1], bounds[2], bounds[3]));
            }
            out.put("selectionPaintBoundsList", selectionBounds);
        }
        List<Object> components = new ArrayList<Object>();
        for (Element element : document.components()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            String name = value(element, "name", value(element, "type", "component"));
            item.put("name", name);
            item.put("type", value(element, "type", "Component"));
            Element parent = document.parentOf(element);
            item.put("parent", parent == null ? null : value(parent, "name", value(parent, "type", "component")));
            if (GuiDocument.acceptsChildren(element)) item.put("layout", value(element, "layout", "BoxLayout"));
            Map<String, Object> attributes = new LinkedHashMap<String, Object>();
            Hashtable raw = element.getAttributes();
            if (raw != null) {
                Enumeration keys = raw.keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    attributes.put(String.valueOf(key), raw.get(key));
                }
            }
            item.put("attributes", attributes);
            Component preview = componentForElement(canvasHost, element);
            if (preview != null) {
                // The resolved style, not the stylesheet's text. This is what the canvas is actually
                // drawing with, so it is the only way to tell from outside whether a CSS edit
                // reached the preview or stopped somewhere on the way.
                Map<String, Object> style = new LinkedHashMap<String, Object>();
                style.put("uiid", preview.getUIID());
                style.put("fgColor", String.format("%06x", Integer.valueOf(preview.getUnselectedStyle().getFgColor())));
                style.put("bgColor", String.format("%06x", Integer.valueOf(preview.getUnselectedStyle().getBgColor())));
                style.put("bgTransparency", Integer.valueOf(preview.getUnselectedStyle().getBgTransparency() & 0xff));
                style.put("uiManager", String.valueOf(System.identityHashCode(preview.getUIManager())));
                item.put("style", style);
                item.put("bounds", componentBounds(preview));
                item.put("visible", Boolean.valueOf(preview.isVisible() && preview.getWidth() > 0 && preview.getHeight() > 0));
                item.put("accessibilityIdentifier", preview.getSemantics().getIdentifier());
            }
            components.add(item);
        }
        out.put("components", components);
        if (activeDropPlan != null) {
            Map<String, Object> guide = new LinkedHashMap<String, Object>();
            guide.put("layout", activeDropPlan.layout);
            guide.put("valid", Boolean.valueOf(activeDropPlan.valid));
            guide.put("constraint", activeDropPlan.constraint);
            guide.put("target", value(activeDropPlan.target, "name", value(activeDropPlan.target, "type", "component")));
            guide.put("message", activeDropPlan.message);
            guide.put("snapDescription", activeDropPlan.snapDescription);
            guide.put("snapBounds", rect(activeDropPlan.snapX, activeDropPlan.snapY,
                    activeDropPlan.snapW, activeDropPlan.snapH));
            out.put("dropGuide", guide);
        }
        return out;
    }

    boolean mcpSelectComponent(String componentName) {
        return mcpSelectComponent(componentName, false);
    }

    boolean mcpSelectComponent(String componentName, boolean additive) {
        if (document == null || componentName == null) return false;
        Element element = findElementNamed(document, componentName);
        if (element == null) return false;
        selectElement(element, additive, "mcp");
        setStatus("Selected " + componentName + " through MCP");
        return true;
    }

    String mcpOpenForm(String formName) {
        if (formName == null || formName.length() == 0) return "form is required";
        if (document != null && document.isModified()) return "Active form has unsaved changes";
        for (String path : guiFiles) {
            String relative = relativeFormName(path);
            String simple = relative.substring(relative.lastIndexOf('.') + 1);
            if (formName.equals(relative) || formName.equals(simple) || formName.equals(path)) {
                if (!openForm(path)) return "Could not open " + relative;
                recordAction("form_opened", "form", relative, "source", "mcp");
                return null;
            }
        }
        return "No form named " + formName;
    }

    String mcpDragComponent(String componentName, String targetName, String placement,
            Integer requestedX, Integer requestedY) {
        if (document == null || canvasHost == null) return "No active GUI document";
        Element element = findElementNamed(document, componentName);
        if (element == null || element == document.root()) return "No draggable component named " + componentName;
        Component source = componentForElement(canvasHost, element);
        if (source == null || source.getWidth() < 1 || source.getHeight() < 1) return componentName + " is not visible";
        if ((requestedX == null) != (requestedY == null)) return "x and y must be supplied together";
        int startX = source.getAbsoluteX() + source.getWidth() / 2;
        int startY = source.getAbsoluteY() + source.getHeight() / 2;
        int releaseX;
        int releaseY;
        if (requestedX != null) {
            releaseX = requestedX.intValue();
            releaseY = requestedY.intValue();
        } else {
            Element target = targetName == null || targetName.length() == 0
                    ? document.root() : findElementNamed(document, targetName);
            if (target == null) return "No target component named " + targetName;
            Component targetPreview = componentForElement(canvasHost, target);
            if (targetPreview == null) return "Target " + targetName + " is not visible";
            String where = placement == null || placement.length() == 0 ? "center" : placement;
            int gap = standardGap();
            // Aim the POINTER, not the dragged component's top-left corner. A drop is resolved from
            // the pointer position, so deriving the release point from the component's own box sent
            // the pointer outside the target whenever the component was larger than it -- dropping
            // a full width button "into" a narrow column landed it in the next column instead.
            int centreX = targetPreview.getAbsoluteX() + targetPreview.getWidth() / 2;
            int centreY = targetPreview.getAbsoluteY() + targetPreview.getHeight() / 2;
            releaseX = centreX;
            releaseY = centreY;
            if ("above".equals(where)) releaseY = targetPreview.getAbsoluteY() - gap;
            else if ("below".equals(where)) releaseY = targetPreview.getAbsoluteY() + targetPreview.getHeight() + gap;
            else if ("leftOf".equals(where)) releaseX = targetPreview.getAbsoluteX() - gap;
            else if ("rightOf".equals(where)) releaseX = targetPreview.getAbsoluteX() + targetPreview.getWidth() + gap;
            else if ("before".equals(where) || "after".equals(where)) {
                // Stay inside the target: before/after is an insertion next to it in its own
                // parent, and dropAfter() decides the side from which half of it the pointer is in.
                Element parent = document.parentOf(target);
                boolean horizontal = parent != null && ("X".equals(value(parent, "boxLayoutAxis", "Y"))
                        || "FlowLayout".equals(value(parent, "layout", "BoxLayout"))
                        || "GridLayout".equals(value(parent, "layout", "BoxLayout")));
                if (horizontal) {
                    releaseX = "after".equals(where) ? targetPreview.getAbsoluteX() + targetPreview.getWidth() - 1
                            : targetPreview.getAbsoluteX() + 1;
                } else {
                    releaseY = "after".equals(where) ? targetPreview.getAbsoluteY() + targetPreview.getHeight() - 1
                            : targetPreview.getAbsoluteY() + 1;
                }
            } else if (!"center".equals(where)) {
                return "Unknown placement " + where;
            }
        }
        String before = document.toXml();
        handleDesignerPointerPressed(startX, startY);
        updateDesignerDrag(releaseX, releaseY);
        finishDesignerDrag(releaseX, releaseY);
        if (before.equals(document.toXml())) return "Drag completed without changing the document: "
                + (status == null ? "no valid drop" : status.getText());
        recordAction("mcp_drag_completed", "component", componentName,
                "x", Integer.valueOf(releaseX), "y", Integer.valueOf(releaseY));
        return null;
    }

    String mcpCommand(String command) {
        if (command == null) return "command is required";
        // A client that is told the save succeeded will happily go on to close the editor.
        if ("save".equals(command)) {
            if (!save()) return "Save failed; the form is still only in the editor";
        }
        else if ("undo".equals(command)) undo();
        else if ("redo".equals(command)) redo();
        else if ("refresh".equals(command)) refreshProject();
        else if ("toggleDarkMode".equals(command)) toggleDarkMode();
        else if ("phonePortrait".equals(command) || "phoneLandscape".equals(command)
                || "tabletPortrait".equals(command) || "desktop".equals(command)) setCanvasMode(command);
        else return "Unknown command " + command;
        recordAction("mcp_command", "command", command);
        return null;
    }

    private static List<Object> componentBounds(Component component) {
        return rect(component.getAbsoluteX(), component.getAbsoluteY(), component.getWidth(), component.getHeight());
    }

    private static List<Object> rect(int x, int y, int width, int height) {
        List<Object> bounds = new ArrayList<Object>();
        bounds.add(Integer.valueOf(x));
        bounds.add(Integer.valueOf(y));
        bounds.add(Integer.valueOf(width));
        bounds.add(Integer.valueOf(height));
        return bounds;
    }

    private static String semanticKey(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; value != null && i < value.length(); i++) {
            char ch = Character.toLowerCase(value.charAt(i));
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') out.append(ch);
            else if (out.length() > 0 && out.charAt(out.length() - 1) != '.') out.append('.');
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '.') out.deleteCharAt(out.length() - 1);
        return out.toString();
    }

    public static void saveActiveDocument() { if (active != null) active.save(); }
    public static void refreshActiveProject() { if (active != null) active.refreshProject(); }
    public static void openActiveCss() { if (active != null) active.openCss(); }
    public static void openActiveSource() { if (active != null) active.openCompanionSource(); }
    public static void openActiveModel() { if (active != null) active.openBindingModel(); }
    static void openActiveCssForTest() { if (active != null) active.openCss(); }
    static CodeEditor activeCodeEditorForTest() { return active == null ? null : active.activeCodeEditor; }
    static int activePreviewForegroundForTest(String componentName) {
        if (active == null || active.document == null || !(active.previewRoot instanceof Container)) return -1;
        for (Element element : active.document.components()) {
            if ((componentName == null && "Button".equals(element.getAttribute("type")))
                    || (componentName != null && componentName.equals(element.getAttribute("name")))) {
                Component component = active.componentForElement((Container) active.previewRoot, element);
                return component == null ? -1 : component.getStyle().getFgColor();
            }
        }
        return -1;
    }
    static int[] activePreviewBoundsForTest(String componentName) {
        if (active == null || active.document == null || !(active.previewRoot instanceof Container)) return null;
        for (Element element : active.document.components()) {
            if (componentName.equals(element.getAttribute("name"))) {
                Component component = active.componentForElement((Container) active.previewRoot, element);
                return component == null ? null : new int[]{component.getAbsoluteX(), component.getAbsoluteY(),
                        component.getWidth(), component.getHeight()};
            }
        }
        return null;
    }
    static String activeDocumentAttributeForTest(String componentName, String attribute) {
        if (active == null || active.document == null) return null;
        for (Element element : active.document.components()) {
            if (componentName.equals(element.getAttribute("name"))) return element.getAttribute(attribute);
        }
        return null;
    }
    static int activePreviewBaselineForTest(String componentName) {
        int[] bounds = activePreviewBoundsForTest(componentName);
        if (bounds == null || active == null) return -1;
        for (Element element : active.document.components()) {
            if (componentName.equals(element.getAttribute("name"))) {
                Component component = active.componentForElement((Container) active.previewRoot, element);
                int baseline = component == null ? -1 : component.getBaseline(component.getWidth(), component.getHeight());
                return baseline < 0 ? -1 : component.getAbsoluteY() + baseline;
            }
        }
        return -1;
    }
    static String activeDesignerStateForTest() {
        return active == null ? "inactive" : "armed=" + active.designerDragArmed
                + ", active=" + active.designerDragActive
                + ", resizeArmed=" + active.guidedResizeArmed
                + ", resizeActive=" + active.guidedResizeActive
                + ", press=" + active.designerPressX + "," + active.designerPressY
                + ", status=" + (active.status == null ? "" : active.status.getText());
    }
    static String activeSelectedNameForTest() {
        return active == null || active.document == null || active.document.selected() == null ? null
                : active.document.selected().getAttribute("name");
    }
    static String activeCanvasModeForTest() { return active == null ? null : active.canvasMode; }
    static int[] activeNamedUiBoundsForTest(String componentName) {
        if (active == null || active.workspace == null) return null;
        Component component = active.findNamedUiComponent(active.workspace, componentName);
        return component == null ? null : new int[]{component.getAbsoluteX(), component.getAbsoluteY(),
                component.getWidth(), component.getHeight()};
    }
    private Component findNamedUiComponent(Component component, String componentName) {
        if (componentName.equals(component.getName())) return component;
        if (component instanceof Container) {
            for (int i = 0; i < ((Container) component).getComponentCount(); i++) {
                Component found = findNamedUiComponent(((Container) component).getComponentAt(i), componentName);
                if (found != null) return found;
            }
        }
        return null;
    }
    static void activeDesktopPointerDragged(int x, int y) {
        if (active == null || !active.designerDragArmed && !active.guidedResizeArmed) return;
        if (active.guidedResizeArmed) active.updateGuidedResize(x, y); else active.updateDesignerDrag(x, y);
    }
    static void activeDesktopPointerReleased(int x, int y) {
        if (active == null || !active.designerDragArmed && !active.guidedResizeArmed) return;
        if (active.guidedResizeArmed) active.finishGuidedResize(x, y); else active.finishDesignerDrag(x, y);
    }
    public static void cutActiveSelection() { if (active != null) active.cut(); }
    public static void copyActiveSelection() { if (active != null) active.copy(); }
    public static void pasteActiveSelection() { if (active != null) active.paste(); }
    public static void deleteActiveSelection() { if (active != null) active.deleteSelection(); }
    public static void undoActiveEdit() { if (active != null) active.undo(); }
    public static void redoActiveEdit() { if (active != null) active.redo(); }
    public static void toggleActiveDarkMode() { if (active != null) active.toggleDarkMode(); }
    public static boolean isActiveDarkMode() { return active != null && active.darkMode; }
    public static String[] activeFormNames() {
        if (active == null) return new String[0];
        String[] names = new String[active.guiFiles.size()];
        for (int i = 0; i < names.length; i++) names[i] = active.relativeFormName(active.guiFiles.get(i));
        return names;
    }
    public static void openActiveForm(int index) {
        if (active != null && index >= 0 && index < active.guiFiles.size()) active.switchForm(active.guiFiles.get(index));
    }
}
