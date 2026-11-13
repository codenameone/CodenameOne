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

package com.codename1.guibuilder.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The component types the GUI Builder can place and the properties each one accepts.
 *
 * <p>This is what the inspector's Properties tab offers, written down once so that a caller who
 * cannot see the inspector -- an MCP client -- is held to exactly the same set and the same
 * ranges. The inspector builds its fields by hand;
 * {@code McpEditingToolsTest.theCatalogOffersExactlyWhatTheInspectorOffers} (in the javase
 * module) opens it for every palette type and the Form root and fails when the two disagree, so a
 * field added there without an entry here (or the reverse) cannot ship.
 *
 * <p>Layout, constraint, name, event and binding attributes are deliberately absent: each has a
 * dedicated operation with its own validation (border regions swap, table cells must stay free,
 * renames repoint relationships, the binding model may need regenerating).
 */
public final class ComponentCatalog {
    /** The types the palette adds, in palette order. */
    public static final String[] PALETTE_TYPES = {
        "Button", "Label", "SpanLabel", "TextField", "TextArea", "CheckBox", "RadioButton",
        "Slider", "Container", "Tabs"
    };

    /** The types a .gui document may be rooted at. */
    public static final String[] ROOT_TYPES = {"Form", "Dialog", "Container"};

    /** The layouts the inspector offers for a container. */
    public static final String[] LAYOUTS = {
        "BoxLayout", "BorderLayout", "FlowLayout", "GridLayout", "TableLayout", "LayeredLayout"
    };

    /** BorderLayout regions, in the order the inspector lists them. */
    public static final String[] BORDER_REGIONS = {"North", "South", "East", "West", "Center"};

    /** The guided (LayeredLayout) alignment and sizing actions, as the inspector's Layout tab offers them. */
    public static final String[] GUIDED_ALIGN_ACTIONS = {
        "alignLeft", "alignHCenter", "alignRight", "alignTop", "alignBaseline", "alignBottom",
        "matchWidth", "matchHeight", "fillWidth", "fillHeight"
    };

    /** Toolbar command placements. */
    public static final String[] COMMAND_PLACEMENTS = {"left", "right", "overflow", "side"};

    /** What a property holds. */
    public static final String KIND_TEXT = "text";
    public static final String KIND_BOOLEAN = "boolean";
    public static final String KIND_INTEGER = "integer";
    public static final String KIND_CHOICE = "choice";

    /** One inspector property. */
    public static final class Property {
        private final String name;
        private final String label;
        private final String kind;
        private final String defaultValue;
        private final int minimum;
        private final int maximum;
        private final String[] choices;

        Property(String name, String label, String kind, String defaultValue, int minimum, int maximum,
                String[] choices) {
            this.name = name;
            this.label = label;
            this.kind = kind;
            this.defaultValue = defaultValue;
            this.minimum = minimum;
            this.maximum = maximum;
            this.choices = choices;
        }

        /** @return the .gui attribute */
        public String name() { return name; }

        /** @return the inspector label */
        public String label() { return label; }

        /** @return one of the {@code KIND_*} constants */
        public String kind() { return kind; }

        /** @return the value in force when the attribute is absent, or null when it depends on the component */
        public String defaultValue() { return defaultValue; }

        /** @return the smallest accepted integer */
        public int minimum() { return minimum; }

        /** @return the largest accepted integer */
        public int maximum() { return maximum; }

        /** @return the accepted values of a choice */
        public String[] choices() { return choices == null ? new String[0] : choices.clone(); }

        Map<String, Object> describe() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("name", name);
            out.put("label", label);
            out.put("kind", kind);
            if (defaultValue != null) out.put("default", defaultValue);
            if (KIND_INTEGER.equals(kind)) {
                out.put("minimum", Integer.valueOf(minimum));
                out.put("maximum", Integer.valueOf(maximum));
            }
            if (choices != null) out.put("choices", list(choices));
            return out;
        }
    }

    private ComponentCatalog() { }

    /**
     * @param type a component type
     * @return true for a type that can be added to a form
     */
    public static boolean isPaletteType(String type) {
        return contains(PALETTE_TYPES, type);
    }

    /**
     * @param type a component type
     * @return true for a type a .gui file may hold anywhere: the palette types, plus Accordion,
     *     which older forms contain and the canvas and generator still understand
     */
    public static boolean isKnownChildType(String type) {
        return isPaletteType(type) || "Accordion".equals(type);
    }

    /**
     * @param type a component type
     * @return true when the type registers an action listener, so an event handler can be attached
     */
    public static boolean firesActionEvents(String type) {
        return "Button".equals(type) || "CheckBox".equals(type) || "RadioButton".equals(type)
                || "TextField".equals(type) || "TextArea".equals(type) || "Slider".equals(type);
    }

    /**
     * @param type a component type
     * @return true for a Form or Dialog, which carry a title, a toolbar and a binding strategy
     */
    public static boolean isFormLike(String type) {
        return "Form".equals(type) || "Dialog".equals(type);
    }

    /**
     * @param type a component type
     * @return true when the layout of a container of this type can be chosen. Tabs and Accordion
     *     lay themselves out.
     */
    public static boolean hasSelectableLayout(String type) {
        return GuiDocument.acceptsChildren(typeElement(type)) && !"Tabs".equals(type) && !"Accordion".equals(type);
    }

    /**
     * Every property the inspector offers for a type, name excluded.
     *
     * @param type the component type
     * @return the properties, in inspector order
     */
    public static List<Property> propertiesFor(String type) {
        List<Property> out = new ArrayList<Property>();
        if (isFormLike(type)) {
            out.add(text("title", "Title"));
        } else if (hasText(type)) {
            // What the canvas and the generated source show when the attribute is absent -- which is
            // where a reset (null) leaves it -- not an empty string.
            String sample = GuiDocument.defaultTextFor(type);
            out.add(new Property("text", "Text", KIND_TEXT, sample == null ? "" : sample, 0, 0, null));
        }
        if ("TextField".equals(type) || "TextArea".equals(type)) {
            String hint = GuiDocument.defaultHintFor(type);
            out.add(new Property("hint", "Hint", KIND_TEXT, hint == null ? "" : hint, 0, 0, null));
        }
        out.add(new Property("uiid", "UIID (CSS selector)", KIND_TEXT, null, 0, 0, null));
        out.add(bool("enabled", "Enabled", true));
        out.add(bool("visible", "Visible", true));
        out.add(bool("rtl", "Right-to-left", false));
        if (isLabelType(type) || "SpanLabel".equals(type)) out.add(integer("gap", "Icon/text gap", "2", 0, 999));
        if (isLabelType(type) || "TextField".equals(type) || "TextArea".equals(type)) {
            out.add(choice("alignment", "Alignment", "left", new String[]{"left", "center", "right"}));
        }
        if (isLabelType(type)) out.add(bool("tickerEnabled", "Ticker when clipped", false));
        if ("Button".equals(type)) out.add(bool("toggle", "Toggle button", false));
        if ("CheckBox".equals(type) || "RadioButton".equals(type)) out.add(bool("selected", "Selected", false));
        if ("TextField".equals(type) || "TextArea".equals(type)) {
            out.add(integer("columns", "Columns", null, 1, 999));
            out.add(integer("maxSize", "Maximum length", null, 0, 99999));
            out.add(bool("editable", "Editable", true));
            out.add(bool("growByContent", "Grow by content", true));
            out.add(choice("constraint", "Input constraint", "ANY",
                    new String[]{"ANY", "EMAILADDR", "PASSWORD", "NUMERIC", "URL"}));
        }
        if ("TextArea".equals(type)) out.add(integer("rows", "Rows", null, 1, 999));
        if ("Slider".equals(type)) {
            out.add(integer("minValue", "Minimum", "0", -99999, 99999));
            out.add(integer("maxValue", "Maximum", "100", -99999, 99999));
            out.add(integer("progress", "Progress", null, -99999, 99999));
            out.add(bool("editable", "Editable", false));
            out.add(bool("infinite", "Infinite progress", false));
        }
        if ("Tabs".equals(type)) {
            // The upper bound is the last tab that exists; the caller checks it against the document.
            out.add(integer("selectedIndex", "Selected tab index", "0", 0, 99999));
            out.add(choice("tabPlacement", "Tab placement", "top", new String[]{"top", "bottom", "left", "right"}));
        }
        if (GuiDocument.acceptsChildren(typeElement(type))) {
            out.add(bool("scrollableX", "Scroll horizontally", false));
            out.add(bool("scrollableY", "Scroll vertically", GuiDocument.defaultScrollableY(type)));
        }
        return out;
    }

    /**
     * @param type the component type
     * @param name the attribute, in any letter case
     * @return the property, or null when the type does not offer it
     */
    public static Property property(String type, String name) {
        if (name == null) return null;
        for (Property candidate : propertiesFor(type)) {
            if (candidate.name().equalsIgnoreCase(name)) return candidate;
        }
        return null;
    }

    /**
     * The whole catalog, as data an MCP client can read.
     *
     * @return types, layouts, constraints and events
     */
    public static Map<String, Object> describe() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        List<Object> types = new ArrayList<Object>();
        List<String> all = new ArrayList<String>();
        for (String type : PALETTE_TYPES) all.add(type);
        all.add("Form");
        all.add("Dialog");
        for (String type : all) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("type", type);
            entry.put("palette", Boolean.valueOf(isPaletteType(type)));
            entry.put("root", Boolean.valueOf(contains(ROOT_TYPES, type)));
            entry.put("container", Boolean.valueOf(GuiDocument.acceptsChildren(typeElement(type))));
            entry.put("selectableLayout", Boolean.valueOf(hasSelectableLayout(type)));
            entry.put("actionEvents", Boolean.valueOf(firesActionEvents(type)));
            List<Object> properties = new ArrayList<Object>();
            for (Property property : propertiesFor(type)) properties.add(property.describe());
            entry.put("properties", properties);
            types.add(entry);
        }
        out.put("types", types);
        Map<String, Object> layouts = new LinkedHashMap<String, Object>();
        layouts.put("BoxLayout", "axis: Y (vertical, default) or X. Children stack in document order.");
        layouts.put("BorderLayout", "At most five children, one per region: " + join(BORDER_REGIONS)
                + ". Set a child's region with the region constraint.");
        layouts.put("FlowLayout", "Children flow left to right and wrap, in document order.");
        layouts.put("GridLayout", "rows (default 1) and columns (default 2), 1 to 100 each; equal cells in document order.");
        layouts.put("TableLayout", "rows and columns (default 2 each, at least what the children occupy). Each child has"
                + " row, column, rowSpan, columnSpan and optional columnWidth/rowHeight percentages.");
        layouts.put("LayeredLayout", "Guided (free-form) layout, the default for a new container. Children are"
                + " positioned with insets and relationships: reference, horizontalSize/verticalSize"
                + " (preferred, fixed, fill, match) and align actions.");
        out.put("layouts", layouts);
        out.put("borderRegions", list(BORDER_REGIONS));
        out.put("guidedSizePolicies", list(new String[]{"preferred", "fixed", "fill", "match"}));
        out.put("guidedAlignActions", list(GUIDED_ALIGN_ACTIONS));
        out.put("commandPlacements", list(COMMAND_PLACEMENTS));
        out.put("bindingStrategies", list(new String[]{"none", "properties", "bindable"}));
        out.put("notes", list(new String[]{
            "A new Container defaults to LayeredLayout; pass layout to change it.",
            "Every change is one undo step and stays in memory until save.",
            "Components are addressed by their unique name."}));
        return out;
    }

    private static boolean hasText(String type) {
        return "Label".equals(type) || "SpanLabel".equals(type) || "Button".equals(type)
                || "CheckBox".equals(type) || "RadioButton".equals(type)
                || "TextField".equals(type) || "TextArea".equals(type);
    }

    private static boolean isLabelType(String type) {
        return "Label".equals(type) || "Button".equals(type) || "CheckBox".equals(type)
                || "RadioButton".equals(type);
    }

    private static com.codename1.xml.Element typeElement(String type) {
        com.codename1.xml.Element element = new com.codename1.xml.Element("component");
        if (type != null) element.setAttribute("type", type);
        return element;
    }

    private static Property text(String name, String label) {
        return new Property(name, label, KIND_TEXT, "", 0, 0, null);
    }

    private static Property bool(String name, String label, boolean fallback) {
        return new Property(name, label, KIND_BOOLEAN, String.valueOf(fallback), 0, 0, null);
    }

    private static Property integer(String name, String label, String fallback, int minimum, int maximum) {
        return new Property(name, label, KIND_INTEGER, fallback, minimum, maximum, null);
    }

    private static Property choice(String name, String label, String fallback, String[] choices) {
        return new Property(name, label, KIND_CHOICE, fallback, 0, 0, choices);
    }

    private static boolean contains(String[] values, String value) {
        if (value == null) return false;
        for (String candidate : values) if (candidate.equals(value)) return true;
        return false;
    }

    private static List<Object> list(String[] values) {
        List<Object> out = new ArrayList<Object>();
        for (String value : values) out.add(value);
        return out;
    }

    private static String join(String[] values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(", ");
            out.append(values[i]);
        }
        return out.toString();
    }
}
