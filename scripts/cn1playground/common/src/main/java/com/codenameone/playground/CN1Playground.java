package com.codenameone.playground;

import com.codename1.components.SplitPane;
import com.codename1.components.MultiButton;
import com.codename1.system.Lifecycle;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.UITimer;
import com.codename1.ui.util.Resources;

import java.util.List;

public class CN1Playground extends Lifecycle {
    private static final String THEME_ROLE = "playgroundThemeRole";

    private final PlaygroundRunner runner = new PlaygroundRunner();
    private Form appForm;
    private TextArea editor;
    private TextArea output;
    private Container previewRoot;
    private Container historyMenu;
    private Resources theme;
    private boolean websiteDarkMode;
    private int editSequence;

    @Override
    public void runApp() {
        com.codename1.ui.CN.setProperty("platformHint.javascript.beforeUnloadMessage", null);
        theme = Resources.getGlobalResources();
        Form form = new Form("CN1 Playground", new BorderLayout());
        appForm = form;
        Toolbar toolbar = form.getToolbar();
        toolbar.setTitleCentered(false);
        setThemeRole(form.getContentPane(), "form");

        editor = createEditor(PlaygroundStateStore.loadCurrentScript());
        output = createOutput(PlaygroundStateStore.loadCurrentOutput());
        previewRoot = createPreviewRoot();
        historyMenu = new Container(BoxLayout.y());

        Container editorPanel = new Container(new BorderLayout());
        setThemeRole(editorPanel, "panel");
        editorPanel.add(BorderLayout.CENTER, editor);
        editorPanel.add(BorderLayout.SOUTH, output);

        Container previewPanel = new Container(new BorderLayout());
        setThemeRole(previewPanel, "panel");
        previewPanel.add(BorderLayout.NORTH, createPreviewHeader());
        previewPanel.add(BorderLayout.CENTER, previewRoot);

        Component content = createMainContent(editorPanel, previewPanel);
        setThemeRole(content, "content");
        form.add(BorderLayout.CENTER, content);

        Command runCommand = Command.create("Run", null, e -> runScript(form));
        Command resetCommand = Command.create("Reset", null, e -> resetEditor());
        Command exampleCommand = Command.create("Load Example", null, e -> loadBuildMethodExample());
        toolbar.addCommandToRightBar(runCommand);
        toolbar.addCommandToRightBar(resetCommand);
        toolbar.addCommandToOverflowMenu(exampleCommand);
        installSideMenu(toolbar, form);
        editor.addDataChangedListener((type, index) -> {
            persistCurrentState();
            scheduleHistorySnapshot();
        });

        runScript(form);
        initWebsiteThemeSync(form);
        form.show();
        notifyWebsiteUiReady();
    }

    private TextArea createEditor(String initialText) {
        TextArea area = new TextArea(initialText, 16, 80);
        area.setHint("Write BeanShell playground code here");
        area.setGrowByContent(false);
        area.setRows(18);
        area.setMaxSize(100000);
        setThemeRole(area, "editor");
        return area;
    }

    private TextArea createOutput(String initialText) {
        TextArea area = new TextArea(initialText, 5, 80);
        area.setConstraint(TextArea.ANY | TextArea.UNEDITABLE);
        area.setFocusable(true);
        area.setActAsLabel(false);
        area.setTextSelectionEnabled(true);
        area.setSingleLineTextArea(false);
        area.setGrowByContent(false);
        area.setRows(6);
        area.getAllStyles().setBgColor(0xf3f4f6);
        area.getAllStyles().setBgTransparency(255);
        area.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        area.getAllStyles().setPadding(2, 2, 2, 2);
        setThemeRole(area, "output");
        return area;
    }

    private Container createPreviewRoot() {
        Container root = new Container(new BorderLayout());
        root.setScrollableY(true);
        root.getAllStyles().setBgColor(0xffffff);
        root.getAllStyles().setBgTransparency(255);
        root.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        root.getAllStyles().setPadding(2, 2, 2, 2);
        setThemeRole(root, "preview");
        return root;
    }

    private Container createPreviewHeader() {
        Container header = new Container(BoxLayout.x());
        setThemeRole(header, "header");
        Label title = new Label("Live Preview");
        setThemeRole(title, "headerTitle");
        Button reload = new Button("Run");
        setThemeRole(reload, "headerButton");
        reload.addActionListener(e -> {
            Form current = reload.getComponentForm();
            if (current != null) {
                runScript(current);
            }
        });
        header.addAll(title, reload);
        return header;
    }

    private Component createMainContent(Container editorPanel, Container previewPanel) {
        if (com.codename1.ui.CN.getDisplayWidth() >= 900) {
            return new SplitPane(SplitPane.HORIZONTAL_SPLIT, editorPanel, previewPanel, "45%", "25%", "75%");
        }
        Container stacked = new Container(new GridLayout(2, 1));
        stacked.addAll(editorPanel, previewPanel);
        return stacked;
    }

    private void runScript(Form form) {
        List<PlaygroundStateStore.HistoryEntry> history = PlaygroundStateStore.pushHistory(editor.getText());
        refreshHistoryMenu(form.getToolbar(), history);
        previewRoot.removeAll();
        output.setText("");
        appendOutput("Running script...");
        PlaygroundContext context = new PlaygroundContext(form, previewRoot, theme, this::appendOutput);
        PlaygroundRunner.RunResult result = runner.run(editor.getText(), context);
        replacePreview(result.getComponent());
        appendOutput(result.getMessage());
        persistCurrentState();
    }

    private void replacePreview(Component component) {
        previewRoot.removeAll();
        if (component == null) {
            previewRoot.revalidate();
            return;
        }
        detachForPreview(component);
        previewRoot.add(BorderLayout.CENTER, component);
        previewRoot.revalidate();
    }

    private void detachForPreview(Component component) {
        Container parent = component.getParent();
        if (parent != null) {
            parent.removeComponent(component);
        }
    }

    private void resetEditor() {
        editor.setText(PlaygroundExamples.DEFAULT_SCRIPT);
        output.setText("");
        previewRoot.removeAll();
        previewRoot.revalidate();
        persistCurrentState();
    }

    private void loadBuildMethodExample() {
        editor.setText(PlaygroundExamples.BUILD_METHOD_SCRIPT);
        output.setText("");
        persistCurrentState();
    }

    private void appendOutput(String message) {
        String current = output.getText();
        if (current == null || current.length() == 0) {
            output.setText(message);
        } else {
            output.setText(current + "\n" + message);
        }
        persistCurrentState();
    }

    private void installSideMenu(Toolbar toolbar, Form form) {
        toolbar.addComponentToSideMenu(new PlaygroundMenuSection("Samples"));
        for (PlaygroundExamples.Sample sample : PlaygroundExamples.SAMPLES) {
            toolbar.addComponentToSideMenu(createSideMenuButton(sample.title, () -> {
                editor.setText(sample.script);
                persistCurrentState();
                runScript(form);
                toolbar.closeSideMenu();
            }));
        }
        toolbar.addComponentToSideMenu(new PlaygroundMenuSection("History"));
        toolbar.addComponentToSideMenu(historyMenu);
        refreshHistoryMenu(toolbar, PlaygroundStateStore.loadHistory());
    }

    private void refreshHistoryMenu(Toolbar toolbar, List<PlaygroundStateStore.HistoryEntry> history) {
        historyMenu.removeAll();
        if (history.isEmpty()) {
            Label empty = new Label("No saved runs yet");
            empty.setUIID("PlaygroundMenuEmpty");
            historyMenu.add(empty);
        } else {
            for (int i = 0; i < history.size(); i++) {
                PlaygroundStateStore.HistoryEntry entry = history.get(i);
                historyMenu.add(createHistoryButton(entry, history, toolbar));
            }
        }
        historyMenu.revalidate();
    }

    private MultiButton createHistoryButton(PlaygroundStateStore.HistoryEntry entry,
            List<PlaygroundStateStore.HistoryEntry> history, Toolbar toolbar) {
        MultiButton button = new MultiButton(entry.title());
        button.setTextLine2(entry.detail(history));
        button.setUIID("SideCommand");
        button.addActionListener(e -> {
            editor.setText(entry.script);
            persistCurrentState();
            Form current = editor.getComponentForm();
            if (current != null) {
                runScript(current);
            }
            toolbar.closeSideMenu();
        });
        return button;
    }

    private Button createSideMenuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setUIID("SideCommand");
        button.addActionListener(e -> action.run());
        return button;
    }

    private void persistCurrentState() {
        if (editor == null || output == null) {
            return;
        }
        PlaygroundStateStore.saveCurrentState(editor.getText(), output.getText());
    }

    private void scheduleHistorySnapshot() {
        if (appForm == null) {
            return;
        }
        final int snapshot = ++editSequence;
        UITimer.timer(1200, false, appForm, () -> {
            if (snapshot != editSequence || editor == null) {
                return;
            }
            refreshHistoryMenu(appForm.getToolbar(), PlaygroundStateStore.pushHistory(editor.getText()));
        });
    }

    private void initWebsiteThemeSync(Form form) {
        WebsiteThemeNative websiteThemeNative = NativeLookup.create(WebsiteThemeNative.class);
        if (websiteThemeNative == null || !websiteThemeNative.isSupported()) {
            return;
        }
        websiteDarkMode = websiteThemeNative.isDarkMode();
        applyWebsiteTheme(form, websiteDarkMode);
        form.refreshTheme();
        UITimer.timer(900, true, form, () -> {
            boolean dark = websiteThemeNative.isDarkMode();
            if (dark != websiteDarkMode) {
                websiteDarkMode = dark;
                applyWebsiteTheme(form, dark);
                form.refreshTheme();
            }
        });
    }

    private void notifyWebsiteUiReady() {
        WebsiteThemeNative nativeBridge = NativeLookup.create(WebsiteThemeNative.class);
        if (nativeBridge != null && nativeBridge.isSupported()) {
            nativeBridge.notifyUiReady();
        }
    }

    private void applyWebsiteTheme(Component component, boolean dark) {
        applyRoleStyle(component, getThemeRole(component), dark);
        if (component instanceof Container) {
            Container cnt = (Container) component;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                applyWebsiteTheme(cnt.getComponentAt(i), dark);
            }
        }
    }

    private void applyRoleStyle(Component component, String role, boolean dark) {
        if (role == null) {
            return;
        }
        Style style = component.getAllStyles();
        style.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        switch (role) {
            case "form":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x111827 : 0xf5f7fb);
                break;
            case "content":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x111827 : 0xf5f7fb);
                break;
            case "panel":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x1f2937 : 0xffffff);
                style.setMarginUnit(Style.UNIT_TYPE_DIPS);
                style.setMargin(1, 1, 1, 1);
                style.setPadding(1, 1, 1, 1);
                break;
            case "editor":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x0f172a : 0xffffff);
                style.setFgColor(dark ? 0xe5e7eb : 0x111827);
                style.setPadding(2, 2, 2, 2);
                break;
            case "output":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x111827 : 0xf3f4f6);
                style.setFgColor(dark ? 0xcbd5e1 : 0x1f2937);
                style.setPadding(2, 2, 2, 2);
                break;
            case "preview":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x0f172a : 0xffffff);
                style.setPadding(2, 2, 2, 2);
                break;
            case "header":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x1f2937 : 0xe5e7eb);
                style.setPadding(1, 1, 2, 2);
                break;
            case "headerTitle":
                style.setFgColor(dark ? 0xf8fafc : 0x111827);
                break;
            case "headerButton":
                style.setBgTransparency(255);
                style.setBgColor(dark ? 0x2563eb : 0x1d4ed8);
                style.setFgColor(0xffffff);
                style.setPadding(1, 1, 2, 2);
                break;
            default:
                break;
        }
    }

    private void setThemeRole(Component component, String role) {
        component.putClientProperty(THEME_ROLE, role);
    }

    private String getThemeRole(Component component) {
        Object role = component.getClientProperty(THEME_ROLE);
        return role instanceof String ? (String) role : null;
    }
}
