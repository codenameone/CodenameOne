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

package com.codename1.guibuilder.ui;

import com.codename1.components.Accordion;
import com.codename1.components.SpanLabel;
import com.codename1.guibuilder.model.GuiDocument;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.FontImage;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.accessibility.AccessibilityGrouping;
import com.codename1.ui.accessibility.AccessibilityRole;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.PointerEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.table.TableLayout;
import com.codename1.xml.Element;

public final class ComponentPreviewFactory {
    private static final String MAX_DESIGN_WIDTH = "gui.maxDesignWidth";
    private static final String MAX_DESIGN_HEIGHT = "gui.maxDesignHeight";
    public interface SelectionHandler {
        void selected(Element element);
        default void selected(Element element, boolean additive) { selected(element); }
        void dragPressed(Element element, Component source, int x, int y);
        boolean isDragActive();
        void editContent(Element element);
        default void dragMoved(int x, int y) { }
        default void dragReleased(int x, int y) { }
    }

    private ComponentPreviewFactory() { }

    public static Component create(Element element, Element selected, SelectionHandler handler) {
        String type = value(element, "type", "Container");
        int guidedWidth = integer(element, "guidedPreferredWidth", -1);
        int guidedHeight = integer(element, "guidedPreferredHeight", -1);
        Component component;
        switch (type) {
            case "Button":
                component = new Button(value(element, "text", "Button")) {
                @Override protected Dimension calcPreferredSize() {
                    return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
                }
            };
                break;
            case "Label":
                component = new Label(value(element, "text", "Label")) {
                @Override protected Dimension calcPreferredSize() {
                    return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
                }
            };
                break;
            case "SpanLabel":
                component = new SpanLabel(value(element, "text", "Wrapped label text")) {
                @Override protected Dimension calcPreferredSize() {
                    return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
                }
            };
                break;
            case "TextField":
                component = new TextField(value(element, "text", ""), value(element, "hint", "Text field")) {
                @Override protected Dimension calcPreferredSize() {
                    return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
                }
            };
                break;
            case "TextArea":
                component = new TextArea(value(element, "text", "Text area")) {
                @Override protected Dimension calcPreferredSize() {
                    return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
                }
            };
                break;
            case "CheckBox":
                component = new CheckBox(value(element, "text", "Check box")) {
                @Override protected Dimension calcPreferredSize() {
                    return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
                }
            };
                break;
            case "RadioButton":
                component = new RadioButton(value(element, "text", "Radio button")) {
                @Override protected Dimension calcPreferredSize() {
                    return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
                }
            };
                break;
            case "Slider":
                component = new Slider() {
                @Override protected Dimension calcPreferredSize() {
                    return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
                }
            };
                break;
            case "Tabs":
                component = sizedTabs(element, selected, handler, guidedWidth, guidedHeight);
                break;
            case "Accordion":
                component = sizedAccordion(element, selected, handler, guidedWidth, guidedHeight);
                break;
            default:
                component = sizedContainer(element, selected, handler, guidedWidth, guidedHeight);
                break;
        }
        // Only an explicit uiid is applied, matching the generated source, which emits setUIID()
        // for that attribute alone. Forcing the XML type here styled a SpanLabel preview as
        // "SpanLabel" while new SpanLabel(...) runs with its constructor default of "Container"
        // (Accordion the same), so backgrounds, borders and padding differed between the canvas
        // and the saved application.
        String explicitUiid = element.getAttribute("uiid");
        if (explicitUiid != null && explicitUiid.length() > 0) component.setUIID(explicitUiid);
        applyAttributes(component, element);
        if (component instanceof CheckBox) {
            ((CheckBox) component).setIcon(FontImage.createMaterial(((CheckBox) component).isSelected()
                    ? FontImage.MATERIAL_CHECK_BOX : FontImage.MATERIAL_CHECK_BOX_OUTLINE_BLANK,
                    ((CheckBox) component).getUnselectedStyle()));
        } else if (component instanceof RadioButton) {
            ((RadioButton) component).setIcon(FontImage.createMaterial(((RadioButton) component).isSelected()
                    ? FontImage.MATERIAL_RADIO_BUTTON_CHECKED : FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED,
                    ((RadioButton) component).getUnselectedStyle()));
        }
        component.putClientProperty("gui.originalBorder", component.getUnselectedStyle().getBorder());
        component.putClientProperty("gui.element", element);
        String componentName = value(element, "name", type);
        component.setName("preview." + componentName);
        component.getSemantics().setIdentifier("guibuilder.preview." + componentName)
                .setLabel(componentName + ", " + type)
                .setDescription("Component on the GUI Builder design canvas")
                .setGrouping(component instanceof Container ? AccessibilityGrouping.GROUP : AccessibilityGrouping.AUTO);
        if (component instanceof Container) component.getSemantics().setRole(AccessibilityRole.GENERIC);
        component.setDropTarget(false);
        component.addPointerPressedListener(e -> {
            handler.selected(element, additiveSelection(e));
            if (!"Form".equals(type)) handler.dragPressed(element, component, e.getX(), e.getY());
        });
        if (supportsInlineContent(type)) component.addLongPressListener(e -> handler.editContent(element));
        component.addPointerDraggedListener(e -> handler.dragMoved(e.getX(), e.getY()));
        component.addPointerReleasedListener(e -> {
            boolean completedDrag = handler.isDragActive();
            handler.dragReleased(e.getX(), e.getY());
            if (completedDrag) return;
            if (!supportsInlineContent(type)) return;
            long now = System.currentTimeMillis();
            Object previous = component.getClientProperty("gui.lastClick");
            component.putClientProperty("gui.lastClick", Long.valueOf(now));
            if (previous instanceof Long && now - ((Long) previous).longValue() < 450) handler.editContent(element);
        });
        return component;
    }

    public static void stabilizeDesignStyles(Component component) {
        component.setEnabled(true);
        component.setFocusable(false);
        component.setRippleEffect(false);
        component.setPressedStyle(new Style(component.getUnselectedStyle()));
        component.setSelectedStyle(new Style(component.getUnselectedStyle()));
        if (component instanceof Container) {
            for (int i = 0; i < ((Container) component).getComponentCount(); i++) stabilizeDesignStyles(((Container) component).getComponentAt(i));
        }
    }

    private static Component sizedContainer(Element element, Element selected, SelectionHandler handler,
            int guidedWidth, int guidedHeight) {
        Container out = new Container(layout(element)) {
            @Override protected Dimension calcPreferredSize() {
                return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
            }
        };
        // The UIID is applied by create() from the element's own uiid/type so the preview is
        // styled by the project CSS exactly as the generated form will be at runtime.
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) {
                Component rendered = create(((Element) child), selected, handler);
                if (out.getLayout() instanceof BorderLayout) {
                    out.add(GuiDocument.effectiveBorderConstraint(element, ((Element) child)), rendered);
                } else if (out.getLayout() instanceof TableLayout) {
                    // GuiDocument owns the cell rule so the preview and the generated source cannot
                    // disagree about where a component with no explicit cell belongs.
                    int row = GuiDocument.effectiveTableRow(element, ((Element) child));
                    int column = GuiDocument.effectiveTableColumn(element, ((Element) child));
                    TableLayout.Constraint constraint = ((TableLayout) out.getLayout()).createConstraint(row, column)
                            .horizontalSpan(integer(((Element) child), "tableHorizontalSpan", 1))
                            .verticalSpan(integer(((Element) child), "tableVerticalSpan", 1));
                    int width = integer(((Element) child), "tableWidth", -1);
                    int height = integer(((Element) child), "tableHeight", -1);
                    if (width != -1) constraint.widthPercentage(width);
                    if (height != -1) constraint.heightPercentage(height);
                    out.add(constraint, rendered);
                } else {
                    out.add(rendered);
                }
            }
        }
        if (out.getLayout() instanceof LayeredLayout) GuidedLayoutSupport.apply(element, out);
        if (out.getComponentCount() == 0) out.add(emptyContainerHint());
        return out;
    }

    /**
     * The marker shown for a container with no children.
     *
     * <p>Its preferred size is deliberately small and fixed. As an ordinary label it asked for
     * whatever width its text needed, which is wider than a real empty container would ever be --
     * enough that emptying one column of a horizontal box pushed the next column past the edge of
     * the device, so draining a container appeared to delete every component on the form. The hint
     * exists to mark a drop target, so it must never be the thing that decides a layout.
     */
    private static Component emptyContainerHint() {
        Label hint = new Label("Drop here", "BuilderEmptyHint") {
            @Override protected Dimension calcPreferredSize() {
                int width = Display.getInstance().convertToPixels(8);
                int height = Display.getInstance().convertToPixels(4);
                return new Dimension(width, height);
            }
        };
        hint.setEndsWith3Points(true);
        hint.setShowEvenIfBlank(true);
        return hint;
    }

    private static boolean additiveSelection(ActionEvent event) {
        PointerEvent pointer = event == null ? null : event.getPointerEvent();
        return pointer != null && (pointer.isShiftDown() || pointer.isControlDown() || pointer.isMetaDown());
    }

    private static Component sizedTabs(Element element, Element selected, SelectionHandler handler,
            int guidedWidth, int guidedHeight) {
        Tabs tabs = new Tabs() {
            @Override protected Dimension calcPreferredSize() {
                return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
            }
        };
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) {
                tabs.addTab(value(((Element) child), "name", "Tab " + (i + 1)), create(((Element) child), selected, handler));
            }
        }
        if (tabs.getTabCount() == 0) tabs.addTab("Tab", new Label("Drop content here"));
        return tabs;
    }

    /**
     * Previews an Accordion as one, rather than letting it fall through to a plain Container. The
     * document model accepts Accordion children and the generator emits {@code new Accordion()}
     * with {@code addContent()}, so rendering its sections as ordinary layout children showed a
     * canvas the running form would not match.
     *
     * @param element the accordion element
     * @param selected the currently selected element
     * @param handler the designer selection handler
     * @param guidedWidth the guided width, or -1
     * @param guidedHeight the guided height, or -1
     * @return the preview component
     */
    private static Component sizedAccordion(Element element, Element selected, SelectionHandler handler,
            int guidedWidth, int guidedHeight) {
        Accordion accordion = new Accordion() {
            @Override protected Dimension calcPreferredSize() {
                return guidedSize(this, super.calcPreferredSize(), guidedWidth, guidedHeight);
            }
        };
        int sections = 0;
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) {
                accordion.addContent(value(((Element) child), "name", "Section " + (i + 1)),
                        create(((Element) child), selected, handler));
                sections++;
            }
        }
        if (sections == 0) accordion.addContent("Section", new Label("Drop content here"));
        return accordion;
    }

    private static Layout layout(Element element) {
        String layout = value(element, "layout", "BoxLayout");
        switch (layout) {
            case "BorderLayout":
                return new DesignerBorderLayout();
            case "FlowLayout":
                return new FlowLayout();
            case "GridLayout":
                return new GridLayout(integer(element, "gridLayoutRows", 1), integer(element, "gridLayoutColumns", 2));
            case "TableLayout":
                return new TableLayout(integer(element, "tableLayoutRows", 2), integer(element, "tableLayoutColumns", 2));
            case "LayeredLayout":
                return new LayeredLayout();
            default:
                return "X".equals(element.getAttribute("boxLayoutAxis")) ? BoxLayout.x() : BoxLayout.y();
        }
    }

    /** Keeps a newly displaced edge component from consuming the entire designer surface. */
    private static final class DesignerBorderLayout extends BorderLayout {
        @Override public void layoutContainer(Container parent) {
            int contentWidth = Math.max(1, parent.getWidth() - parent.getStyle().getHorizontalPadding());
            int contentHeight = Math.max(1, parent.getHeight() - parent.getStyle().getVerticalPadding());
            capWidth(getWest(), Math.max(48, contentWidth * 30 / 100));
            capWidth(getEast(), Math.max(48, contentWidth * 30 / 100));
            capHeight(getNorth(), Math.max(36, contentHeight * 30 / 100));
            capHeight(getSouth(), Math.max(36, contentHeight * 30 / 100));
            super.layoutContainer(parent);
        }

        private static void capWidth(Component component, int maximum) {
            if (component == null) return;
            component.putClientProperty(MAX_DESIGN_WIDTH, Integer.valueOf(maximum));
            component.setShouldCalcPreferredSize(true);
        }

        private static void capHeight(Component component, int maximum) {
            if (component == null) return;
            component.putClientProperty(MAX_DESIGN_HEIGHT, Integer.valueOf(maximum));
            component.setShouldCalcPreferredSize(true);
        }
    }

    private static int integer(Element element, String name, int fallback) {
        try { return Integer.parseInt(value(element, name, String.valueOf(fallback))); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private static Dimension guidedSize(Component component, Dimension natural, int guidedWidth, int guidedHeight) {
        if (guidedWidth > 0) natural.setWidth(guidedWidth);
        if (guidedHeight > 0) natural.setHeight(guidedHeight);
        Object maximumWidth = component.getClientProperty(MAX_DESIGN_WIDTH);
        Object maximumHeight = component.getClientProperty(MAX_DESIGN_HEIGHT);
        if (maximumWidth instanceof Integer) natural.setWidth(Math.min(natural.getWidth(), ((Integer) maximumWidth).intValue()));
        if (maximumHeight instanceof Integer) natural.setHeight(Math.min(natural.getHeight(), ((Integer) maximumHeight).intValue()));
        return natural;
    }

    /** See CodenameOneGUIBuilder#value: an emptied attribute is a value, not a missing one. */
    private static String value(Element element, String name, String fallback) {
        String value = element.getAttribute(name);
        return value == null ? fallback : value;
    }

    private static boolean supportsInlineContent(String type) {
        return "Form".equals(type) || "Label".equals(type) || "SpanLabel".equals(type)
                || "Button".equals(type) || "CheckBox".equals(type) || "RadioButton".equals(type)
                || "TextField".equals(type) || "TextArea".equals(type);
    }

    private static void applyAttributes(Component component, Element element) {
        component.setEnabled(!"false".equals(value(element, "enabled", "true")));
        component.setVisible(!"false".equals(value(element, "visible", "true")));
        component.setRTL("true".equals(value(element, "rtl", "false")));
        if (component instanceof Label) {
            ((Label) component).setGap(integer(element, "gap", ((Label) component).getGap()));
            ((Label) component).setTickerEnabled("true".equals(value(element, "tickerEnabled", "false")));
        } else if (component instanceof SpanLabel) {
            // SpanLabel is a Container rather than a Label, so the inspector's gap silently did
            // nothing here while the generated source was free to set it.
            ((SpanLabel) component).setGap(integer(element, "gap", ((SpanLabel) component).getGap()));
        }
        // Applied only when the document carries it: the generated source emits nothing for an
        // absent attribute, and forcing LEFT here would make the preview disagree with a themed
        // alignment. Labels and text inputs are the components that own the setter.
        if (element.getAttribute("alignment") != null) {
            if (component instanceof Label) ((Label) component).setAlignment(alignment(element.getAttribute("alignment")));
            else if (component instanceof TextArea) ((TextArea) component).setAlignment(alignment(element.getAttribute("alignment")));
        }
        if (component instanceof Button) ((Button) component).setToggle("true".equals(value(element, "toggle", "false")));
        if (component instanceof CheckBox) ((CheckBox) component).setSelected("true".equals(value(element, "selected", "false")));
        else if (component instanceof RadioButton) ((RadioButton) component).setSelected("true".equals(value(element, "selected", "false")));
        if (component instanceof TextArea) {
            ((TextArea) component).setColumns(integer(element, "columns", ((TextArea) component).getColumns()));
            ((TextArea) component).setRows(integer(element, "rows", ((TextArea) component).getRows()));
            ((TextArea) component).setMaxSize(integer(element, "maxSize", ((TextArea) component).getMaxSize()));
            ((TextArea) component).setEditable(!"false".equals(value(element, "editable", "true")));
            ((TextArea) component).setGrowByContent(!"false".equals(value(element, "growByContent", "true")));
            ((TextArea) component).setConstraint(constraint(value(element, "constraint", "ANY")));
        }
        if (component instanceof Container) {
            ((Container) component).setScrollableX("true".equals(value(element, "scrollableX", "false")));
            ((Container) component).setScrollableY("true".equals(value(element, "scrollableY", "false")));
            ((Container) component).setTensileDragEnabled(false);
            ((Container) component).setAlwaysTensile(false);
            ((Container) component).setSmoothScrolling(false);
            ((Container) component).setScrollVisible(true);
        }
        if (component instanceof Slider) {
            ((Slider) component).setMinValue(integer(element, "minValue", ((Slider) component).getMinValue()));
            ((Slider) component).setMaxValue(integer(element, "maxValue", ((Slider) component).getMaxValue()));
            ((Slider) component).setProgress(integer(element, "progress", ((Slider) component).getProgress()));
            ((Slider) component).setEditable("true".equals(value(element, "editable", "false")));
            ((Slider) component).setInfinite("true".equals(value(element, "infinite", "false")));
        }
        if (component instanceof Tabs && ((Tabs) component).getTabCount() > 0) {
            int selected = Math.max(0, Math.min(((Tabs) component).getTabCount() - 1, integer(element, "selectedIndex", 0)));
            ((Tabs) component).setSelectedIndex(selected, false);
            ((Tabs) component).setTabPlacement(tabPlacement(value(element, "tabPlacement", "top")));
        }
    }

    private static int alignment(String value) {
        if ("center".equalsIgnoreCase(value)) return Component.CENTER;
        if ("right".equalsIgnoreCase(value)) return Component.RIGHT;
        return Component.LEFT;
    }

    private static int constraint(String value) {
        if ("EMAILADDR".equals(value)) return TextArea.EMAILADDR;
        if ("PASSWORD".equals(value)) return TextArea.PASSWORD;
        if ("NUMERIC".equals(value)) return TextArea.NUMERIC;
        if ("URL".equals(value)) return TextArea.URL;
        return TextArea.ANY;
    }

    private static int tabPlacement(String value) {
        if ("bottom".equalsIgnoreCase(value)) return Component.BOTTOM;
        if ("left".equalsIgnoreCase(value)) return Component.LEFT;
        if ("right".equalsIgnoreCase(value)) return Component.RIGHT;
        return Component.TOP;
    }
}
