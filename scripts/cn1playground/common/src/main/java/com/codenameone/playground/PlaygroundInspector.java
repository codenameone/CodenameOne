package com.codenameone.playground;

import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.UITimer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/// Component inspector panel.
///
/// Laid out per the redesign spec: tree at the top with chevrons + outlined
/// type icons indented by nesting depth, then a single-column form split
/// into IDENTITY / CONTENT / APPEARANCE / LAYOUT sections. Each section opens
/// with an uppercase divider label; fields are label-on-left (22 mm fixed
/// width) + input-on-right. Multi-value numeric fields (bounds / padding /
/// margin) render as 4 sub-inputs plus a micro-label row directly beneath.
/// Units pick from a horizontal segmented control. Colour fields pair a hex
/// input with a live 9 mm swatch.
final class PlaygroundInspector {
    interface Listener {
        void onComponentPropertyChanged(Component component, String property, Object value);
    }

    private static final String EMPTY_TREE_LABEL = "Run code to see component tree";
    private static final String[] UNIT_NAMES = {"Dips", "Pixels"};
    private static final byte[] UNIT_VALUES = {Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_PIXELS};

    private final Container component;
    private final Container treeContainer;
    private final Container propertiesContainer;
    private final Listener listener;
    private Component selectedComponent;
    private Component previewRoot;
    private Form glassPaneForm;
    private Painter originalGlassPane;
    private boolean darkMode;
    private UITimer highlightTimer;
    private final java.util.Set<Component> expanded = new java.util.HashSet<Component>();

    private Container unifiedScroll;

    PlaygroundInspector(boolean darkMode, Listener listener) {
        this.darkMode = darkMode;
        this.listener = listener;

        treeContainer = new Container(BoxLayout.y());
        propertiesContainer = new Container(BoxLayout.y());

        // A single scrollable Y container holds the tree, the divider, and the
        // property form, so the user scrolls the inspector panel as one unit
        // rather than two independent nested scrolls (which produced a bouncy
        // dual-scroll effect).
        unifiedScroll = new Container(BoxLayout.y());
        unifiedScroll.setScrollableY(true);
        unifiedScroll.add(treeContainer);
        unifiedScroll.add(buildTreeDivider());
        unifiedScroll.add(propertiesContainer);

        component = new Container(new BorderLayout());
        component.setUIID(darkMode ? "PlaygroundInspectorRootDark" : "PlaygroundInspectorRoot");
        component.add(BorderLayout.CENTER, unifiedScroll);

        applyTheme(darkMode);
        rebuildTree();
        updatePropertyPanel(null);
    }

    private Container buildTreeDivider() {
        Container divider = new Container(new BorderLayout());
        divider.setUIID(uiidDark("PlaygroundInspectorTreeDivider"));
        divider.setPreferredH(Math.max(1, Display.getInstance().convertToPixels(0.3f)));
        return divider;
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
        component.setUIID(darkMode ? "PlaygroundInspectorRootDark" : "PlaygroundInspectorRoot");
        int panelBg = darkMode ? 0x0A1D3A : 0xFFFFFF;
        treeContainer.getAllStyles().setBgColor(panelBg);
        treeContainer.getAllStyles().setBgTransparency(255);
        propertiesContainer.getAllStyles().setBgColor(panelBg);
        propertiesContainer.getAllStyles().setBgTransparency(255);

        // Breathing-room padding is applied inline on the tree and property
        // containers (not the root) so the divider between them can span the
        // full panel width without being cropped.
        treeContainer.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        treeContainer.getAllStyles().setPadding(0, 0, 2, 2);
        propertiesContainer.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        propertiesContainer.getAllStyles().setPadding(0, 2, 2, 2);

        rebuildTree();
        updatePropertyPanel(selectedComponent);
    }

    private String uiidDark(String base) {
        return darkMode ? base + "Dark" : base;
    }

    // ============================================================
    // Tree
    // ============================================================

    private void rebuildTree() {
        treeContainer.removeAll();
        if (previewRoot == null) {
            Label empty = new Label(EMPTY_TREE_LABEL);
            empty.setUIID(uiidDark("PlaygroundPropEmpty"));
            treeContainer.add(empty);
            if (treeContainer.getComponentForm() != null) {
                treeContainer.revalidate();
            }
            return;
        }
        buildTreeRows(previewRoot, 0);
        if (treeContainer.getComponentForm() != null) {
            treeContainer.revalidate();
        }
    }

    private void buildTreeRows(Component c, int depth) {
        if (c == null) {
            return;
        }
        treeContainer.add(createTreeRow(c, depth));
        if (c instanceof Container && isExpanded(c)) {
            Container cnt = (Container) c;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                buildTreeRows(cnt.getComponentAt(i), depth + 1);
            }
        }
    }

    private boolean isExpanded(Component c) {
        // Default: root is expanded, everything else collapsed.
        return expanded.contains(c) || c == previewRoot;
    }

    private Container createTreeRow(Component c, int depth) {
        final boolean isContainer = c instanceof Container;
        boolean selected = c == selectedComponent;
        String rowUiid = uiidDark(selected ? "PlaygroundTreeRowActive" : "PlaygroundTreeRow");

        final Button chevron = new Button();
        chevron.setUIID(uiidDark("PlaygroundTreeChevron"));
        if (isContainer) {
            char arrow = isExpanded(c) ? FontImage.MATERIAL_EXPAND_MORE : FontImage.MATERIAL_CHEVRON_RIGHT;
            FontImage.setMaterialIcon(chevron, arrow, 2.6f);
            chevron.addActionListener(e -> {
                if (isExpanded(c) && c != previewRoot) {
                    expanded.remove(c);
                } else {
                    expanded.add(c);
                }
                rebuildTree();
            });
        } else {
            chevron.setVisible(false);
            chevron.setHidden(true);
        }

        Label typeIcon = new Label();
        typeIcon.setUIID(uiidDark("PlaygroundTreeTypeIcon"));
        char typeChar = isContainer ? FontImage.MATERIAL_FOLDER_OPEN : FontImage.MATERIAL_ARTICLE;
        FontImage.setMaterialIcon(typeIcon, typeChar, 3f);

        Label textLabel = new Label(c.getClass().getSimpleName() + extractBracket(c));
        textLabel.setUIID(uiidDark(selected ? "PlaygroundTreeTypeActive" : "PlaygroundTreeType"));

        Container body = new Container(BoxLayout.x());
        body.getAllStyles().setBgTransparency(0);
        body.add(typeIcon);
        body.add(textLabel);

        Container row = new Container(new BorderLayout());
        row.setUIID(rowUiid);
        row.add(BorderLayout.WEST, chevron);
        row.add(BorderLayout.CENTER, body);

        // Depth indent: spec values (3 mm base + 5 mm per level) rendered at
        // ~75% so they sit correctly at CN1 physical-mm scale.
        float indentMm = 2f + depth * 3.8f;
        row.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        row.getAllStyles().setPaddingLeft((int) indentMm);
        row.getAllStyles().setPaddingRight(1);
        row.getAllStyles().setPaddingTop(0);
        row.getAllStyles().setPaddingBottom(0);
        int rowH = Display.getInstance().convertToPixels(6f);
        row.setPreferredH(rowH);

        // Selection fires on a pointer release anywhere on the row, except when
        // the release lands inside the chevron button (which handles its own
        // expand/collapse). addPointerReleasedListener fires in addition to the
        // chevron's own ActionListener, so gate on absolute X coords.
        row.addPointerReleasedListener(e -> {
            if (isContainer && chevron.isVisible()) {
                int x = e.getX();
                int cx = chevron.getAbsoluteX();
                if (x >= cx && x < cx + chevron.getWidth()) {
                    return;
                }
            }
            handleComponentSelected(c);
        });
        return row;
    }

    private static String extractBracket(Component c) {
        String uiid = c.getUIID();
        String typeName = c.getClass().getSimpleName();
        if (uiid != null && uiid.length() > 0 && !uiid.equals(typeName)) {
            return " [" + uiid + "]";
        }
        return "";
    }

    private void handleComponentSelected(Component c) {
        selectedComponent = c;
        highlightComponent(c);
        updatePropertyPanel(c);
        rebuildTree();
    }

    private void highlightComponent(Component c) {
        if (c == null) {
            clearHighlight();
            return;
        }
        Form form = c.getComponentForm();
        if (form == null) {
            clearHighlight();
            return;
        }
        clearHighlight();
        glassPaneForm = form;
        originalGlassPane = form.getGlassPane();
        form.setGlassPane(new HighlightPainter(c, darkMode));
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

    // ============================================================
    // Property panel
    // ============================================================

    private void updatePropertyPanel(Component comp) {
        propertiesContainer.removeAll();

        if (comp == null) {
            Label empty = new Label("Select a component");
            empty.setUIID(uiidDark("PlaygroundPropEmpty"));
            propertiesContainer.add(empty);
            if (propertiesContainer.getComponentForm() != null) {
                propertiesContainer.revalidate();
            }
            return;
        }

        addSectionHeader("IDENTITY");
        addTextRow("Type", comp.getClass().getSimpleName(), false, null);
        addTextRow("UIID", comp.getUIID(), true, v -> {
            comp.setUIID(v);
            notifyChange(comp, "uiid");
        });
        addTextRow("Name", comp.getName() == null ? "" : comp.getName(), true, v -> {
            comp.setName(v);
            notifyChange(comp, "name");
        });

        // CONTENT only appears when the selected component actually has editable
        // text, so we never render an empty "Text: -" placeholder.
        String textValue = null;
        Consumer<String> textSetter = null;
        if (comp instanceof Label) {
            textValue = ((Label) comp).getText();
            textSetter = v -> { ((Label) comp).setText(v); notifyChange(comp, "text"); };
        } else if (comp instanceof TextField) {
            textValue = ((TextField) comp).getText();
            textSetter = v -> { ((TextField) comp).setText(v); notifyChange(comp, "text"); };
        } else if (comp instanceof TextArea) {
            textValue = ((TextArea) comp).getText();
            textSetter = v -> { ((TextArea) comp).setText(v); notifyChange(comp, "text"); };
        }
        if (textSetter != null) {
            addSectionHeader("CONTENT");
            addTextRow("Text", textValue == null ? "" : textValue, true, textSetter);
        }

        addSectionHeader("APPEARANCE");
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

        addSectionHeader("LAYOUT");
        addMultiValueRow("Bounds",
                new int[]{comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight()},
                new String[]{"X", "Y", "Width", "Height"},
                vals -> {
                    comp.setX(vals[0]);
                    comp.setY(vals[1]);
                    comp.setWidth(vals[2]);
                    comp.setHeight(vals[3]);
                    notifyChange(comp, "bounds");
                });
        addPaddingMarginRow("Padding", s, true);
        addUnitSegmented("Padding units", getUnit(s.getPaddingUnit()), unit -> {
            s.setPaddingUnit(unit);
            notifyChange(comp, "padding");
        });
        addPaddingMarginRow("Margin", s, false);
        addUnitSegmented("Margin units", getUnit(s.getMarginUnit()), unit -> {
            s.setMarginUnit(unit);
            notifyChange(comp, "margin");
        });
        addBooleanRow("Visible", comp.isVisible(), v -> {
            comp.setVisible(v);
            notifyChange(comp, "visible");
        });

        if (propertiesContainer.getComponentForm() != null) {
            propertiesContainer.revalidate();
        }
    }

    // ============================================================
    // Field builders
    // ============================================================

    private int mm(float v) {
        return Display.getInstance().convertToPixels(v);
    }

    private void addSectionHeader(String title) {
        Label header = new Label(title);
        header.setUIID(uiidDark("PlaygroundInspectorSection"));
        propertiesContainer.add(header);
    }

    private Container baseFieldRow(String labelText) {
        Container row = new Container(new BorderLayout());
        row.setUIID(uiidDark("PlaygroundFieldRow"));
        row.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
        row.getAllStyles().setMargin(1, 0, 0, 0);

        Label label = new Label(labelText);
        label.setUIID(uiidDark("PlaygroundFieldLabel"));
        // Spec's 22 mm label-column rendered as CSS-px-sized 12 mm -- spec values
        // were in design-doc mm which at CN1's physical-mm are roughly 2x too big.
        label.setPreferredW(mm(12));

        row.add(BorderLayout.WEST, label);
        return row;
    }

    private void addTextRow(String label, String value, boolean editable, Consumer<String> callback) {
        Container row = baseFieldRow(label);
        if (editable && callback != null) {
            TextField field = new TextField(value == null ? "" : value);
            field.setUIID(uiidDark("PlaygroundFieldInput"));
            field.setSingleLineTextArea(true);
            field.addDataChangedListener((t, i) -> callback.accept(field.getText()));
            row.add(BorderLayout.CENTER, field);
        } else {
            Label v = new Label(value == null ? "" : value);
            v.setUIID(uiidDark("PlaygroundFieldReadOnly"));
            row.add(BorderLayout.CENTER, v);
        }
        propertiesContainer.add(row);
    }

    private void addColorRow(String labelText, int color, int alpha, ColorCallback callback) {
        int safeAlpha = clampAlpha(alpha);
        Container row = baseFieldRow(labelText);

        TextField hexField = new TextField(formatColor(color));
        hexField.setUIID(uiidDark("PlaygroundFieldInput"));
        hexField.setSingleLineTextArea(true);

        Label swatch = new Label(" ");
        swatch.setUIID(uiidDark("PlaygroundInspectorSwatch"));
        int swatchSize = mm(5);
        swatch.setPreferredW(swatchSize);
        swatch.setPreferredH(swatchSize);
        updateColorPreview(swatch, color, safeAlpha);

        Runnable updater = () -> {
            Integer c = parseColor(hexField.getText());
            if (c != null) {
                callback.update(c, safeAlpha);
                updateColorPreview(swatch, c, safeAlpha);
            }
        };
        hexField.addDataChangedListener((t, i) -> updater.run());

        Container rightWrap = new Container(new BorderLayout());
        rightWrap.getAllStyles().setBgTransparency(0);
        rightWrap.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
        rightWrap.getAllStyles().setMargin(0, 0, 0, 2);
        rightWrap.add(BorderLayout.CENTER, hexField);
        rightWrap.add(BorderLayout.EAST, swatch);
        row.add(BorderLayout.CENTER, rightWrap);
        propertiesContainer.add(row);
    }

    private void addMultiValueRow(String labelText, int[] values, String[] tooltips,
                                  Consumer<int[]> commit) {
        Container row = baseFieldRow(labelText);

        Container fields = new Container(new GridLayout(1, 4));
        fields.getAllStyles().setBgTransparency(0);

        // Spec's micro labels (X Y W H / T R B L) are represented as tooltips so
        // the row stays compact while each input is still discoverable.
        TextField[] tfs = new TextField[4];
        for (int i = 0; i < 4; i++) {
            TextField f = new TextField(String.valueOf(values[i]));
            f.setUIID(uiidDark("PlaygroundFieldInput"));
            f.setSingleLineTextArea(true);
            f.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
            // Symmetric horizontal margin on every cell so fields never touch;
            // the GridLayout distributes the four cells at equal width and each
            // cell's internal input has the same gap from its left and right
            // neighbour (the GridLayout's surrounding padding absorbs the outer
            // half-gap).
            f.getAllStyles().setMargin(0, 0, 1, 1);
            if (tooltips != null && i < tooltips.length && tooltips[i] != null) {
                f.setTooltip(tooltips[i]);
            }
            tfs[i] = f;
            fields.add(f);
        }
        Runnable commitRunner = () -> {
            int[] v = new int[4];
            for (int i = 0; i < 4; i++) {
                v[i] = parseInt(tfs[i].getText(), values[i]);
            }
            commit.accept(v);
        };
        for (TextField f : tfs) {
            f.addDataChangedListener((t, i) -> commitRunner.run());
        }

        // The grid itself gets negative horizontal margin equal to the per-cell
        // margin so the outer edges align back with the CENTER slot.
        fields.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
        fields.getAllStyles().setMargin(0, 0, -1, -1);

        row.add(BorderLayout.CENTER, fields);
        propertiesContainer.add(row);
    }

    private void addPaddingMarginRow(String labelText, Style s, boolean isPadding) {
        int[] v = new int[4];
        if (isPadding) {
            v[0] = s.getPaddingTop();
            v[1] = s.getPaddingRight(false);
            v[2] = s.getPaddingBottom();
            v[3] = s.getPaddingLeft(false);
        } else {
            v[0] = s.getMarginTop();
            v[1] = s.getMarginRight(false);
            v[2] = s.getMarginBottom();
            v[3] = s.getMarginLeft(false);
        }
        addMultiValueRow(labelText, v, new String[]{"Top", "Right", "Bottom", "Left"}, vals -> {
            if (isPadding) {
                s.setPadding(vals[0], vals[2], vals[3], vals[1]);
            } else {
                s.setMargin(vals[0], vals[2], vals[3], vals[1]);
            }
            notifyChange(selectedComponent, isPadding ? "padding" : "margin");
        });
    }

    private void addUnitSegmented(String labelText, int activeIndex, Consumer<Byte> onChange) {
        Container row = baseFieldRow(labelText);

        Container seg = new Container(new GridLayout(1, UNIT_NAMES.length));
        seg.setUIID(uiidDark("PlaygroundInspectorSegment"));

        Button[] buttons = new Button[UNIT_NAMES.length];
        for (int i = 0; i < UNIT_NAMES.length; i++) {
            final int idx = i;
            Button b = new Button(UNIT_NAMES[i]);
            b.setUIID(uiidDark(idx == activeIndex
                    ? "PlaygroundInspectorSegmentActive"
                    : "PlaygroundInspectorSegmentInactive"));
            b.addActionListener(e -> {
                for (int j = 0; j < buttons.length; j++) {
                    buttons[j].setUIID(uiidDark(j == idx
                            ? "PlaygroundInspectorSegmentActive"
                            : "PlaygroundInspectorSegmentInactive"));
                }
                onChange.accept(UNIT_VALUES[idx]);
            });
            buttons[i] = b;
            seg.add(b);
        }
        row.add(BorderLayout.CENTER, seg);
        propertiesContainer.add(row);
    }

    private void addBooleanRow(String labelText, boolean value, Consumer<Boolean> callback) {
        Container row = baseFieldRow(labelText);
        CheckBox cb = new CheckBox();
        cb.setSelected(value);
        cb.setUIID(uiidDark("PlaygroundInspectorCheckbox"));
        cb.addActionListener(e -> callback.accept(cb.isSelected()));
        Container wrap = new Container(new FlowLayout(Component.LEFT, Component.CENTER));
        wrap.getAllStyles().setBgTransparency(0);
        wrap.add(cb);
        row.add(BorderLayout.CENTER, wrap);
        propertiesContainer.add(row);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void updateColorPreview(Label preview, int color, int alpha) {
        preview.getAllStyles().setBgTransparency(alpha);
        preview.getAllStyles().setBgColor(color & 0xFFFFFF);
    }

    private int clampAlpha(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private int getUnit(byte[] units) {
        byte unit = units != null && units.length > 0 ? units[0] : Style.UNIT_TYPE_DIPS;
        for (int i = 0; i < UNIT_VALUES.length; i++) {
            if (UNIT_VALUES[i] == unit) {
                return i;
            }
        }
        return 0;
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
        return "#" + hex.toUpperCase();
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
