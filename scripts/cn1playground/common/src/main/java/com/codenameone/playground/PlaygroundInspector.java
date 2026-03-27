package com.codenameone.playground;

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.Painter;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.tree.Tree;
import com.codename1.ui.tree.TreeModel;
import com.codename1.ui.util.UITimer;

import java.util.Vector;

final class PlaygroundInspector {
    interface Listener {
        void onComponentPropertyChanged(Component component, String property, Object value);
    }

    private static final String EMPTY_TREE_LABEL = "Run code to see component tree";
    private static final String[] UNIT_NAMES = {"Pixels", "Dips", "% Screen", "VW", "VH"};
    private static final byte[] UNIT_VALUES = {Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_SCREEN_PERCENTAGE, Style.UNIT_TYPE_VW, Style.UNIT_TYPE_VH};

    private final Container component;
    private final Tree tree;
    private final Container propertiesContainer;
    private final Listener listener;
    private Component selectedComponent;
    private Component previewRoot;
    private Form glassPaneForm;
    private Painter originalGlassPane;
    private boolean darkMode;
    private UITimer highlightTimer;

    PlaygroundInspector(boolean darkMode, Listener listener) {
        this.darkMode = darkMode;
        this.listener = listener;
        tree = createTree();
        propertiesContainer = createPropertiesContainer();
        component = createLayout();
        applyTheme(darkMode);
        updatePropertyPanel(null);
    }

    Component getComponent() {
        return component;
    }

    void setPreviewRoot(Component root) {
        previewRoot = root;
        rebuildTree();
    }

    void applyTheme(boolean darkMode) {
        this.darkMode = darkMode;
        applyThemeToInspectorComponent(component, darkMode);
        rebuildTree();
        updatePropertyPanel(selectedComponent);
    }

    private void applyThemeToInspectorComponent(Component cmp, boolean dark) {
        if (cmp == null) {
            return;
        }

        String uiid = cmp.getUIID();
        if (uiid != null && supportsDarkVariant(uiid)) {
            if (dark && !uiid.endsWith("Dark")) {
                cmp.setUIID(uiid + "Dark");
            } else if (!dark && uiid.endsWith("Dark")) {
                cmp.setUIID(uiid.substring(0, uiid.length() - 4));
            }
        }

        if (cmp instanceof Container) {
            Container cnt = (Container) cmp;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                applyThemeToInspectorComponent(cnt.getComponentAt(i), dark);
            }
        }
    }

    private boolean supportsDarkVariant(String uiid) {
        switch (uiid) {
            case "PlaygroundInspectorRoot":
            case "PlaygroundInspectorTree":
            case "PlaygroundInspectorProps":
            case "PlaygroundPropName":
            case "PlaygroundPropValue":
            case "PlaygroundPropSmall":
            case "PlaygroundPropUnit":
            case "PlaygroundPropEmpty":
            case "PlaygroundColorPreview":
                return true;
            default:
                return false;
        }
    }

    private Tree createTree() {
        Tree t = new Tree(new EmptyTreeModel()) {
            @Override
            protected String childToDisplayLabel(Object child) {
                if (child instanceof ComponentWrapper) {
                    return ((ComponentWrapper) child).getDisplayText();
                }
                return String.valueOf(child);
            }

            @Override
            protected Component createNode(Object node, int depth) {
                Component c = super.createNode(node, depth);
                if (c != null) {
                    Style s = c.getAllStyles();
                    s.setPaddingTop(0);
                    s.setPaddingBottom(0);
                    s.setPaddingLeft(depth * 4);
                    s.setPaddingRight(2);
                    s.setMarginTop(0);
                    s.setMarginBottom(0);

                    if (c instanceof Button) {
                        c.setUIID(darkMode ? "PlaygroundInspectorTreeNodeDark" : "PlaygroundInspectorTreeNode");
                        final Object nodeObj = node;
                        ((Button) c).addActionListener(e -> {
                            if (nodeObj instanceof ComponentWrapper) {
                                handleComponentSelected(((ComponentWrapper) nodeObj).component);
                            }
                        });
                    } else {
                        c.setUIID(darkMode ? "PlaygroundInspectorTreeNodeDark" : "PlaygroundInspectorTreeNode");
                    }
                }
                return c;
            }
        };
        t.setUIID("PlaygroundInspectorTree");
        return t;
    }

    private Container createPropertiesContainer() {
        Container c = new Container(new TableLayout(0, 4));
        c.setScrollableY(true);
        c.setUIID("PlaygroundInspectorProps");
        return c;
    }

    private Container createLayout() {
        Container root = new Container(new GridLayout(2, 1));
        root.setUIID("PlaygroundInspectorRoot");
        root.addAll(tree, propertiesContainer);
        return root;
    }

    private void rebuildTree() {
        if (previewRoot == null) {
            tree.setModel(new EmptyTreeModel());
            selectedComponent = null;
            updatePropertyPanel(null);
            return;
        }
        tree.setModel(new ComponentTreeModel(previewRoot));
    }

    private void handleComponentSelected(Component component) {
        selectedComponent = component;
        highlightComponent(component);
        updatePropertyPanel(component);
    }

    private void highlightComponent(Component component) {
        if (component == null) {
            clearHighlight();
            return;
        }
        Form form = component.getComponentForm();
        if (form == null) {
            clearHighlight();
            return;
        }
        clearHighlight();
        glassPaneForm = form;
        originalGlassPane = form.getGlassPane();
        form.setGlassPane(new HighlightPainter(component, darkMode));
        form.repaint();
        
        if (highlightTimer != null) {
            highlightTimer.cancel();
        }
        highlightTimer = UITimer.timer(2000, false, form, () -> {
            clearHighlight();
            highlightTimer = null;
        });
    }

    private void clearHighlight() {
        if (glassPaneForm != null) {
            glassPaneForm.setGlassPane(originalGlassPane);
            glassPaneForm.repaint();
            glassPaneForm = null;
        }
        originalGlassPane = null;
    }

    private void updatePropertyPanel(Component comp) {
        propertiesContainer.removeAll();

        if (comp == null) {
            Label empty = new Label("Select a component");
            empty.setUIID("PlaygroundPropEmpty");
            propertiesContainer.add(empty);
            propertiesContainer.revalidate();
            return;
        }

        addRow("Type", comp.getClass().getSimpleName(), false, null);
        addRow("UIID", comp.getUIID(), true, v -> {
            comp.setUIID(v);
            notifyChange(comp, "uiid");
        });

        if (comp instanceof Label) {
            addRow("Text", ((Label) comp).getText(), true, v -> {
                ((Label) comp).setText(v);
                notifyChange(comp, "text");
            });
        } else if (comp instanceof TextField) {
            addRow("Text", ((TextField) comp).getText(), true, v -> {
                ((TextField) comp).setText(v);
                notifyChange(comp, "text");
            });
        } else if (comp instanceof TextArea) {
            addRow("Text", ((TextArea) comp).getText(), true, v -> {
                ((TextArea) comp).setText(v);
                notifyChange(comp, "text");
            });
        }

        addBoundsRow(comp);

        Style s = comp.getUnselectedStyle();
        addColorRow("Background", s.getBgColor(), s.getBgTransparency(), (color, alpha) -> {
            Style a = comp.getAllStyles();
            a.setBgColor(color);
            a.setBgTransparency(alpha);
            notifyChange(comp, "bg");
        });
        addColorRow("Foreground", s.getFgColor(), 255, (color, alpha) -> {
            comp.getAllStyles().setFgColor(color);
            notifyChange(comp, "fg");
        });

        String layoutName = "-";
        if (comp instanceof Container cnt) {
            if (cnt.getLayout() != null) {
                layoutName = cnt.getLayout().getClass().getSimpleName();
            }
        }
        addRow("Layout", layoutName, false, null);

        addPaddingMarginRow("Padding", s, true);
        addPaddingMarginRow("Margin", s, false);

        if (comp.getParent() != null) {
            Container parent = comp.getParent();
            if (parent.getLayout() instanceof BorderLayout) {
                Object constraint = comp.getClientProperty("layoutConstraint");
                String constraintStr = constraint == null ? "Center" : constraint.toString();
                addRow("Constraint", constraintStr, true, v -> {
                    comp.putClientProperty("layoutConstraint", v);
                    parent.revalidate();
                    notifyChange(comp, "constraint");
                });
            }
        }

        addBooleanRow("Visible", comp.isVisible(), v -> {
            comp.setVisible(v);
            notifyChange(comp, "visible");
        });

        applyThemeToInspectorComponent(propertiesContainer, darkMode);
        propertiesContainer.revalidate();
    }

    private void addRow(String name, String value, boolean editable, java.util.function.Consumer<String> callback) {
        Label nameLabel = new Label(name);
        nameLabel.setUIID("PlaygroundPropName");
        addProperty(nameLabel);

        if (editable && callback != null) {
            TextField field = new TextField(value == null ? "" : value);
            field.setUIID("PlaygroundPropValue");
            field.setSingleLineTextArea(true);
            field.addDataChangedListener((type, index) -> callback.accept(field.getText()));
            addProperty(field);
        } else {
            Label valueLabel = new Label(value == null ? "" : value);
            valueLabel.setUIID("PlaygroundPropValue");
            addProperty(valueLabel);
        }
    }

    private void addBoundsRow(Component comp) {
        Label nameLabel = new Label("Bounds");
        nameLabel.setUIID("PlaygroundPropName");
        addProperty(nameLabel);

        Container fields = new Container(new GridLayout(1, 4));
        fields.setUIID("PlaygroundPropGroup");

        TextField xField = addSmallField(fields, String.valueOf(comp.getX()), true);
        TextField yField = addSmallField(fields, String.valueOf(comp.getY()), true);
        TextField wField = addSmallField(fields, String.valueOf(comp.getWidth()), true);
        TextField hField = addSmallField(fields, String.valueOf(comp.getHeight()), true);

        xField.addDataChangedListener((t, i) -> {
            comp.setX(parseInt(xField.getText(), comp.getX()));
            notifyChange(comp, "bounds");
        });
        yField.addDataChangedListener((t, i) -> {
            comp.setY(parseInt(yField.getText(), comp.getY()));
            notifyChange(comp, "bounds");
        });
        wField.addDataChangedListener((t, i) -> {
            comp.setWidth(parseInt(wField.getText(), comp.getWidth()));
            notifyChange(comp, "bounds");
        });
        hField.addDataChangedListener((t, i) -> {
            comp.setHeight(parseInt(hField.getText(), comp.getHeight()));
            notifyChange(comp, "bounds");
        });

        addProperty(fields);
    }

    private TextField addSmallField(Container parent, String value, boolean editable) {
        TextField field = new TextField(value);
        field.setUIID("PlaygroundPropSmall");
        field.setSingleLineTextArea(true);
        field.setEditable(editable);
        parent.add(field);
        return field;
    }

    private void addProperty(Component cmp) {
        propertiesContainer.add(((TableLayout)propertiesContainer.getLayout()).createConstraint().wp(25), cmp);
    }

    private void addColorRow(String name, int color, int alpha, ColorCallback callback) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));

        Label nameLabel = new Label(name);
        nameLabel.setUIID("PlaygroundPropName");
        addProperty(nameLabel);

        TextField hexField = new TextField(formatColor(color), "Color", 1, 8);
        hexField.setUIID("PlaygroundPropSmall");
        hexField.setSingleLineTextArea(true);

        TextField alphaField = new TextField(String.valueOf(safeAlpha), "Alpha", 1, 3);
        alphaField.setUIID("PlaygroundPropSmall");
        alphaField.setSingleLineTextArea(true);

        Label preview = new Label("  ");
        preview.setUIID("PlaygroundColorPreview");
        updateColorPreview(preview, color, safeAlpha);

        Runnable updater = () -> {
            Integer c = parseColor(hexField.getText());
            int a = clampAlpha(parseInt(alphaField.getText(), safeAlpha));
            if (c != null) {
                callback.update(c, a);
                updateColorPreview(preview, c, a);
            }
        };

        hexField.addDataChangedListener((t, i) -> updater.run());
        alphaField.addDataChangedListener((t, i) -> updater.run());

        Container row = BoxLayout.encloseX(hexField, alphaField, preview);
        row.setUIID("PlaygroundPropGroup");
        addProperty(row);
    }

    private void updateColorPreview(Label preview, int color, int alpha) {
        preview.getAllStyles().setBgTransparency(alpha);
        preview.getAllStyles().setBgColor(color & 0xFFFFFF);
    }

    private int clampAlpha(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private void addPaddingMarginRow(String name, Style s, boolean isPadding) {
        int t, b, l, r;
        byte unit;

        if (isPadding) {
            t = s.getPaddingTop();
            b = s.getPaddingBottom();
            l = s.getPaddingLeft(false);
            r = s.getPaddingRight(false);
            byte[] units = s.getPaddingUnit();
            unit = units != null && units.length > 0 ? units[0] : Style.UNIT_TYPE_PIXELS;
        } else {
            t = s.getMarginTop();
            b = s.getMarginBottom();
            l = s.getMarginLeft(false);
            r = s.getMarginRight(false);
            byte[] units = s.getMarginUnit();
            unit = units != null && units.length > 0 ? units[0] : Style.UNIT_TYPE_PIXELS;
        }

        Label nameLabel = new Label(name);
        nameLabel.setUIID("PlaygroundPropName");
        addProperty(nameLabel);

        Container fields = new Container(new GridLayout(1, 4));
        TextField topF = addSmallField(fields, String.valueOf(t), true);
        TextField botF = addSmallField(fields, String.valueOf(b), true);
        TextField leftF = addSmallField(fields, String.valueOf(l), true);
        TextField rightF = addSmallField(fields, String.valueOf(r), true);
        addProperty(fields);

        Label unitNameLabel = new Label(name + " unit");
        unitNameLabel.setUIID("PlaygroundPropName");
        addProperty(unitNameLabel);

        ComboBox<String> unitBox = new ComboBox<>(UNIT_NAMES);
        unitBox.setSelectedItem(unitToName(unit));
        unitBox.setUIID("PlaygroundPropUnit");
        addProperty(unitBox);

        Runnable updater = () -> {
            byte currentUnit = nameToUnit((String) unitBox.getSelectedItem());
            applyPaddingMargin(
                    s,
                    isPadding,
                    parseInt(topF.getText(), t),
                    parseInt(botF.getText(), b),
                    parseInt(leftF.getText(), l),
                    parseInt(rightF.getText(), r),
                    currentUnit
            );
            notifyChange(selectedComponent, isPadding ? "padding" : "margin");
        };

        unitBox.addActionListener(e -> updater.run());
        topF.addDataChangedListener((t1, i) -> updater.run());
        botF.addDataChangedListener((t1, i) -> updater.run());
        leftF.addDataChangedListener((t1, i) -> updater.run());
        rightF.addDataChangedListener((t1, i) -> updater.run());
    }

    private void applyPaddingMargin(Style s, boolean isPadding, int t, int b, int l, int r, byte unit) {
        if (isPadding) {
            s.setPaddingUnit(unit);
            s.setPadding(t, b, l, r);
        } else {
            s.setMarginUnit(unit);
            s.setMargin(t, b, l, r);
        }
    }

    private String unitToName(byte unit) {
        for (int i = 0; i < UNIT_VALUES.length; i++) {
            if (UNIT_VALUES[i] == unit) return UNIT_NAMES[i];
        }
        return UNIT_NAMES[1];
    }

    private byte nameToUnit(String name) {
        for (int i = 0; i < UNIT_NAMES.length; i++) {
            if (UNIT_NAMES[i].equals(name)) return UNIT_VALUES[i];
        }
        return Style.UNIT_TYPE_DIPS;
    }

    private void addBooleanRow(String name, boolean value, java.util.function.Consumer<Boolean> callback) {
        Label nameLabel = new Label(name);
        nameLabel.setUIID("PlaygroundPropName");
        addProperty(nameLabel);

        CheckBox cb = new CheckBox();
        cb.setSelected(value);
        cb.setUIID("PlaygroundPropCheckbox");
        cb.addActionListener(e -> callback.accept(cb.isSelected()));
        addProperty(FlowLayout.encloseCenter(cb));
    }

    private void notifyChange(Component comp, String property) {
        if (comp == null) {
            return;
        }
        if (comp.getParent() != null) {
            comp.getParent().revalidate();
        } else {
            comp.repaint();
        }
        listener.onComponentPropertyChanged(comp, property, null);
    }

    private String formatColor(int color) {
        int c = color & 0xFFFFFF;
        String hex = Integer.toHexString(c);
        while (hex.length() < 6) hex = "0" + hex;
        return "0x" + hex.toUpperCase();
    }

    private Integer parseColor(String value) {
        if (value == null) return null;
        String t = value.trim();
        if (t.isEmpty()) return null;
        try {
            if (t.startsWith("0x") || t.startsWith("0X")) return (int) Long.parseLong(t.substring(2), 16);
            if (t.startsWith("#")) return (int) Long.parseLong(t.substring(1), 16);
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String s, int def) {
        if (s == null || s.trim().isEmpty()) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private interface ColorCallback {
        void update(int color, int alpha);
    }

    private static final class ComponentWrapper {
        final Component component;

        ComponentWrapper(Component component) {
            this.component = component;
        }

        String getDisplayText() {
            String name = component.getClass().getSimpleName();
            String uiid = component.getUIID();
            if (uiid != null && uiid.length() > 0 && !uiid.equals(name)) {
                name = name + " [" + uiid + "]";
            }
            if (component instanceof Label) {
                String text = ((Label) component).getText();
                if (text != null && text.length() > 0) {
                    String display = text.length() > 15 ? text.substring(0, 12) + "..." : text;
                    name = name + ": " + display;
                }
            }
            return name;
        }
    }

    private static final class EmptyTreeModel implements TreeModel {
        @Override
        public Vector getChildren(Object parent) {
            Vector v = new Vector();
            if (parent == null) {
                v.addElement(EMPTY_TREE_LABEL);
            }
            return v;
        }

        @Override
        public boolean isLeaf(Object node) {
            return true;
        }
    }

    private static final class ComponentTreeModel implements TreeModel {
        private final Component root;

        ComponentTreeModel(Component root) {
            this.root = root;
        }

        @Override
        public Vector getChildren(Object parent) {
            Vector v = new Vector();
            if (parent == null) {
                v.addElement(new ComponentWrapper(root));
                return v;
            }
            if (parent instanceof ComponentWrapper) {
                Component comp = ((ComponentWrapper) parent).component;
                if (comp instanceof Container) {
                    Container container = (Container) comp;
                    for (int i = 0; i < container.getComponentCount(); i++) {
                        v.addElement(new ComponentWrapper(container.getComponentAt(i)));
                    }
                }
            }
            return v;
        }

        @Override
        public boolean isLeaf(Object node) {
            if (node instanceof ComponentWrapper) {
                Component comp = ((ComponentWrapper) node).component;
                if (comp instanceof Container) {
                    return ((Container) comp).getComponentCount() == 0;
                }
                return true;
            }
            return true;
        }
    }

    private static final class HighlightPainter implements Painter {
        private final Component component;
        private final boolean darkMode;

        HighlightPainter(Component component, boolean darkMode) {
            this.component = component;
            this.darkMode = darkMode;
        }

        @Override
        public void paint(com.codename1.ui.Graphics g, com.codename1.ui.geom.Rectangle rect) {
            if (component == null || component.getParent() == null) return;
            Form form = component.getComponentForm();
            if (form == null) return;
            int x = component.getAbsoluteX() - form.getX();
            int y = component.getAbsoluteY() - form.getY();
            int w = component.getWidth();
            int h = component.getHeight();
            g.setColor(darkMode ? 0x3b82f6 : 0x2563eb);
            g.setAlpha(80);
            g.fillRect(x, y, w, h);
            g.setAlpha(255);
            g.drawRect(x, y, w, h);
        }
    }
}