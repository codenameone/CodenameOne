package com.codenameone.playground;

import bsh.cn1.GeneratedCN1Access;
import com.codename1.system.Lifecycle;
import com.codename1.components.SplitPane;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.Resources;

public class CN1Playground extends Lifecycle {
    private final PlaygroundRunner runner = new PlaygroundRunner();
    private TextArea editor;
    private TextArea output;
    private Container previewRoot;
    private Resources theme;

    @Override
    public void runApp() {
        theme = Resources.getGlobalResources();
        Form form = new Form("CN1 Playground", new BorderLayout());
        Toolbar toolbar = form.getToolbar();
        toolbar.setTitleCentered(false);

        editor = createEditor();
        output = createOutput();
        previewRoot = createPreviewRoot();

        Container editorPanel = new Container(new BorderLayout());
        editorPanel.add(BorderLayout.CENTER, editor);
        editorPanel.add(BorderLayout.SOUTH, output);

        Container previewPanel = new Container(new BorderLayout());
        previewPanel.add(BorderLayout.NORTH, createPreviewHeader());
        previewPanel.add(BorderLayout.CENTER, previewRoot);

        Component content = createMainContent(editorPanel, previewPanel);
        form.add(BorderLayout.CENTER, content);

        Command runCommand = Command.create("Run", null, e -> runScript(form));
        Command resetCommand = Command.create("Reset", null, e -> resetEditor());
        Command exampleCommand = Command.create("Load Example", null, e -> loadBuildMethodExample());
        toolbar.addCommandToRightBar(runCommand);
        toolbar.addCommandToRightBar(resetCommand);
        toolbar.addCommandToOverflowMenu(exampleCommand);

        runScript(form);
        form.show();
    }

    private TextArea createEditor() {
        TextArea area = new TextArea(PlaygroundExamples.DEFAULT_SCRIPT, 16, 80);
        area.setHint("Write BeanShell playground code here");
        area.setGrowByContent(false);
        area.setRows(18);
        area.setMaxSize(100000);
        return area;
    }

    private TextArea createOutput() {
        TextArea area = new TextArea("", 5, 80);
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
        return area;
    }

    private Container createPreviewRoot() {
        Container root = new Container(new BorderLayout());
        root.setScrollableY(true);
        root.getAllStyles().setBgColor(0xffffff);
        root.getAllStyles().setBgTransparency(255);
        root.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        root.getAllStyles().setPadding(2, 2, 2, 2);
        return root;
    }

    private Container createPreviewHeader() {
        Container header = new Container(BoxLayout.x());
        Label title = new Label("Live Preview");
        Button reload = new Button("Run");
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
        previewRoot.removeAll();
        output.setText("");
        appendOutput("Running script...");
        appendRuntimeSmoke();
        PlaygroundContext context = new PlaygroundContext(form, previewRoot, theme, this::appendOutput);
        PlaygroundRunner.RunResult result = runner.run(editor.getText(), context);
        replacePreview(result.getComponent());
        appendOutput(result.getMessage());
    }

    private void appendRuntimeSmoke() {
        appendOutput("Smoke platform=" + com.codename1.ui.CN.getPlatformName());
        appendOutput("Smoke registry size=" + GeneratedCN1Access.debugClassIndexSize());
        appendSmokeLiteral("Container", new ClassSupplier() {
            public Class<?> get() {
                return Container.class;
            }
        });
        appendSmokeLiteral("BoxLayout", new ClassSupplier() {
            public Class<?> get() {
                return BoxLayout.class;
            }
        });
        appendSmokeLiteral("SpanLabel", new ClassSupplier() {
            public Class<?> get() {
                return com.codename1.components.SpanLabel.class;
            }
        });
        appendSmokeContains("com.codename1.ui.Container");
        appendSmokeContains("com.codename1.ui.layouts.BoxLayout");
        appendSmokeContains("com.codename1.components.SpanLabel");
        appendSmokeLookup("com.codename1.ui.Container");
        appendSmokeLookup("com.codename1.ui.layouts.BoxLayout");
        appendSmokeLookup("com.codename1.components.SpanLabel");
    }

    private void appendSmokeLiteral(String label, ClassSupplier supplier) {
        try {
            Class<?> type = supplier.get();
            appendOutput("Smoke literal " + label + "=" + describeClass(type));
        } catch (Throwable t) {
            appendOutput("Smoke literal " + label + " failed: " + t);
        }
    }

    private void appendSmokeLookup(String name) {
        try {
            appendOutput("Smoke registry " + name + "=" + describeClass(GeneratedCN1Access.INSTANCE.findClass(name)));
        } catch (Throwable t) {
            appendOutput("Smoke registry " + name + " failed: " + t);
        }
    }

    private void appendSmokeContains(String name) {
        try {
            appendOutput("Smoke registry contains " + name + "=" + GeneratedCN1Access.debugClassIndexContains(name));
        } catch (Throwable t) {
            appendOutput("Smoke registry contains " + name + " failed: " + t);
        }
    }

    private String describeClass(Class<?> type) {
        return type == null ? "null" : type.getName();
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
    }

    private void loadBuildMethodExample() {
        editor.setText(PlaygroundExamples.BUILD_METHOD_SCRIPT);
        output.setText("");
    }

    private void appendOutput(String message) {
        String current = output.getText();
        if (current == null || current.length() == 0) {
            output.setText(message);
        } else {
            output.setText(current + "\n" + message);
        }
    }

    private interface ClassSupplier {
        Class<?> get();
    }

}
