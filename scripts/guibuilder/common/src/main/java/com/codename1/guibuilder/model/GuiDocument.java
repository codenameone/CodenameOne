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

import com.codename1.util.regex.StringReader;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;
import com.codename1.xml.XMLWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GuiDocument {
    /** Single-name Guided Layout relationships. Mirrors the attributes GuidedLayoutSupport reads. */
    private static final String[] REFERENCE_NAME_ATTRIBUTES = {
            "guidedMatchWidth", "guidedMatchHeight", "guidedReferenceTarget"};
    private static final String SIZE_MATCH = "match";
    private static final String SIZE_PREFERRED = "preferred";

    private final String path;
    private Element root;
    private Element selected;
    private boolean modified;
    /** The XML as last written to disk; null until this document has been saved. */
    private String savedXml;
    private final List<State> undo = new ArrayList<>();
    private final List<State> redo = new ArrayList<>();
    private int transactionDepth;
    private State transactionStart;

    private GuiDocument(String path, Element root) {
        this.path = path;
        this.root = root;
        this.selected = root;
    }

    public static GuiDocument parse(String path, String xml) {
        Element root = new XMLParser().parse(new StringReader(xml));
        if (root == null || !"component".equals(root.getTagName())) {
            throw new IllegalArgumentException("GUI file does not contain a component root");
        }
        return new GuiDocument(path, root);
    }

    public String path() { return path; }
    public Element root() { return root; }
    public Element selected() { return selected; }
    /**
     * True when the document differs from what is on disk. This is derived from the saved XML
     * rather than reported by a flag the undo stack restores: a snapshot taken before a save
     * carries "not modified", so undoing across a save used to report a document that no longer
     * matched the file as clean, and the next form switch then discarded it without a prompt.
     */
    public boolean isModified() {
        if (savedXml == null) return modified;
        return !savedXml.equals(xmlOnly());
    }

    public void markSaved() {
        modified = false;
        savedXml = xmlOnly();
    }

    public void select(Element element) {
        if (element != null && containsElement(element)) selected = element;
    }

    /** Identity-based ownership check. Elements parsed for another form must never be accepted. */
    public boolean containsElement(Element element) {
        return element != null && containsIdentity(root, element);
    }

    public String attribute(String name, String fallback) {
        String value = selected.getAttribute(name);
        return value == null ? fallback : value;
    }

    public void setAttribute(String name, String value) {
        beforeMutation();
        setNormalizedAttribute(selected, name, value);
        modified = true;
    }

    /**
     * Sets an attribute on a specific element rather than the selection.
     *
     * @param element the element to change
     * @param name the attribute name
     * @param value the new value, or null to remove it
     */
    public void setAttribute(Element element, String name, String value) {
        if (element == null) return;
        beforeMutation();
        setNormalizedAttribute(element, name, value);
        modified = true;
    }

    /**
     * The UIID that is actually in force, which is what the CSS selector field must show.
     *
     * <p>The XML type is not it. A SpanLabel and an Accordion are Containers that never call
     * setUIID, so both the preview and the generated application style them as "Container" while
     * this field claimed "SpanLabel" -- a developer could write a rule against the selector the
     * designer told them was live and watch it apply to nothing. A Form or Dialog root reports its
     * own UIID rather than the content pane's, because that is the one setUIID() sets at runtime.
     *
     * @param element the selected element
     * @return the UIID the runtime will resolve styles from
     */
    public String effectiveUiid(Element element) {
        String explicit = element == null ? null : element.getAttribute("uiid");
        if (explicit != null && explicit.length() > 0) return explicit;
        String type = element == null ? null : element.getAttribute("type");
        if (type == null || type.length() == 0) return "Component";
        String constructorDefault = CONSTRUCTOR_UIID.get(type);
        return constructorDefault == null ? type : constructorDefault;
    }

    /** Types whose constructor leaves a UIID that is not the type's own name. */
    private static final Map<String, String> CONSTRUCTOR_UIID = new HashMap<String, String>();
    static {
        CONSTRUCTOR_UIID.put("SpanLabel", "Container");
        CONSTRUCTOR_UIID.put("Accordion", "Container");
    }

    public Element parentOf(Element element) {
        return element == root ? null : findParent(root, element);
    }

    public String parentLayout(Element element) {
        Element parent = parentOf(element);
        if (parent == null) return "";
        String layout = parent.getAttribute("layout");
        return layout == null || layout.length() == 0 ? "BoxLayout" : layout;
    }

    /** Returns a valid, deterministic constraint even for malformed legacy BorderLayouts. */
    public static String effectiveBorderConstraint(Element parent, Element child) {
        if (parent == null || child == null) return "Center";
        List<String> used = new ArrayList<>();
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object value = parent.getChildAt(i);
            if (!(value instanceof Element) || !"component".equals(((Element) value).getTagName())) continue;
            String constraint = normalizeBorderConstraint(((Element) value).getAttribute("layoutConstraint"));
            if (constraint == null || used.contains(constraint)) constraint = firstFreeBorderConstraint(used);
            if (((Element) value) == child) return constraint;
            used.add(constraint);
        }
        return "Center";
    }

    public static Element childAtBorderConstraint(Element parent, String constraint, Element excluding) {
        String wanted = normalizeBorderConstraint(constraint);
        if (parent == null || wanted == null) return null;
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object value = parent.getChildAt(i);
            if (value instanceof Element && "component".equals(((Element) value).getTagName()) && ((Element) value) != excluding
                    && wanted.equals(effectiveBorderConstraint(parent, ((Element) value)))) return ((Element) value);
        }
        return null;
    }

    public static String normalizeBorderConstraint(String constraint) {
        if (constraint == null) return null;
        if ("north".equalsIgnoreCase(constraint)) return "North";
        if ("south".equalsIgnoreCase(constraint)) return "South";
        if ("east".equalsIgnoreCase(constraint)) return "East";
        if ("west".equalsIgnoreCase(constraint)) return "West";
        if ("center".equalsIgnoreCase(constraint)) return "Center";
        return null;
    }

    private static String firstFreeBorderConstraint(List<String> used) {
        String[] order = {"Center", "North", "South", "West", "East"};
        for (String candidate : order) if (!used.contains(candidate)) return candidate;
        return "Center";
    }

    public boolean moveSelectedBy(int delta) {
        if (selected == root || delta == 0) return false;
        Element parent = findParent(root, selected);
        if (parent == null) return false;
        int index = childIndex(parent, selected);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= parent.getNumChildren()) return false;
        beforeMutation();
        parent.removeChildAt(index);
        parent.insertChildAt(selected, target);
        modified = true;
        return true;
    }

    public List<Element> commands() {
        List<Element> result = new ArrayList<>();
        for (int i = 0; i < root.getNumChildren(); i++) {
            Object child = root.getChildAt(i);
            if (child instanceof Element && "command".equals(((Element) child).getTagName())) result.add(((Element) child));
        }
        return result;
    }

    public Element addCommand() {
        beforeMutation();
        Element command = new Element("command");
        command.setAttribute("name", "Command " + (commands().size() + 1));
        command.setAttribute("placement", "right");
        command.setAttribute("actionEvent", "onCommand" + (commands().size() + 1));
        root.addChild(command);
        modified = true;
        return command;
    }

    public boolean removeCommand(Element command) {
        if (command == null || !"command".equals(command.getTagName())) return false;
        int index = childIndex(root, command);
        if (index < 0) return false;
        beforeMutation();
        root.removeChildAt(index);
        modified = true;
        return true;
    }

    public void setCommandAttribute(Element command, String name, String value) {
        if (command == null || !"command".equals(command.getTagName())) return;
        beforeMutation();
        setNormalizedAttribute(command, name, value);
        modified = true;
    }

    /**
     * True when the parent is a BorderLayout whose five regions are all taken. Core
     * {@code BorderLayout.addLayoutComponent()} removes whatever already occupies a region, so a
     * sixth child does not stack up -- it silently evicts an existing one from the preview and the
     * generated container while remaining in the .gui hierarchy.
     *
     * @param parent the container an addition is aimed at
     * @return true when there is no free region left
     */
    public static boolean borderLayoutIsFull(Element parent) {
        if (parent == null || !"BorderLayout".equals(parent.getAttribute("layout"))) return false;
        return componentsIn(parent).size() >= BORDER_LAYOUT_REGIONS;
    }

    /**
     * The text the preview shows for a freshly added component, so the document carries it too.
     *
     * @param type the component type
     * @return the default text, or null for a type that has none
     */
    public static String defaultTextFor(String type) {
        if ("Button".equals(type)) return "Button";
        if ("Label".equals(type)) return "Label";
        if ("SpanLabel".equals(type)) return "Wrapped label text";
        if ("TextArea".equals(type)) return "Text area";
        if ("CheckBox".equals(type)) return "Check box";
        if ("RadioButton".equals(type)) return "Radio button";
        return null;
    }

    /**
     * The hint the canvas shows for a component with none of its own.
     *
     * @param type the component type
     * @return the default hint, or null for a type that has none
     */
    public static String defaultHintFor(String type) {
        return "TextField".equals(type) ? "Text field" : null;
    }

    /** North, South, East, West and Center: everything a BorderLayout can hold. */
    private static final int BORDER_LAYOUT_REGIONS = 5;

    /**
     * @param type the component type to add
     * @return the new element, or null when the selected parent cannot take another child
     */
    public Element addComponent(String type) {
        Element target = acceptsChildren(selected) ? selected : findParent(root, selected);
        if (borderLayoutIsFull(target == null ? root : target)) return null;
        beforeMutation();
        Element parent = selected;
        if (!acceptsChildren(parent)) parent = findParent(root, selected);
        if (parent == null) parent = root;
        Element child = new Element("component");
        child.setAttribute("type", type);
        child.setAttribute("name", uniqueName(type));
        // The preview substitutes a visible default for a missing text attribute while the
        // generator emits an empty string, so a control added and saved without being edited went
        // from labelled on the canvas to blank at runtime. Store what the canvas shows.
        String defaultText = defaultTextFor(type);
        if (defaultText != null) child.setAttribute("text", defaultText);
        // The preview shows a hint for a bare TextField while the generator emits an empty one, so
        // saving an untouched field removed the only prompt the canvas had shown.
        String defaultHint = defaultHintFor(type);
        if (defaultHint != null) child.setAttribute("hint", defaultHint);
        if (acceptsChildren(child)) child.setAttribute("layout", "LayeredLayout");
        parent.addChild(child);
        assignFreeTableCell(parent, child);
        selected = child;
        modified = true;
        return child;
    }

    /**
     * Gives a new child of a table an explicit free cell.
     *
     * <p>Without this a component added from the palette or the menu carries no cell at all, and
     * every consumer has to invent one. They did not agree: the preview fell back to sibling order
     * while the generated source fell back to cell (0, 0), so a table that looked right in the
     * designer compiled to every component stacked in one corner.
     */
    private static void assignFreeTableCell(Element parent, Element child) {
        if (!"TableLayout".equals(parent.getAttribute("layout"))) return;
        int columns = tableColumns(parent);
        Set<String> taken = new LinkedHashSet<>();
        for (Element sibling : componentsIn(parent)) {
            if (sibling == child) continue;
            markOccupied(taken, parent, sibling);
        }
        // The child brings its own span when it is a paste, so the whole rectangle it would cover
        // has to be free and inside the table -- checking only the anchor let a two-column
        // component land beside an occupied cell and overlap on the next rebuild.
        int childRowSpan = Math.max(1, parseInt(child.getAttribute("tableVerticalSpan"), 1));
        // Clamped to the destination: a component copied from a wider table can carry a span larger
        // than this table has columns, and the search below would then reject every candidate and
        // spin forever on the EDT.
        int childColumnSpan = Math.min(columns,
                Math.max(1, parseInt(child.getAttribute("tableHorizontalSpan"), 1)));
        if (childColumnSpan != parseInt(child.getAttribute("tableHorizontalSpan"), 1)) {
            setNormalizedAttribute(child, "tableHorizontalSpan", String.valueOf(childColumnSpan));
        }
        for (int cursor = 0; ; cursor++) {
            int row = cursor / columns;
            int column = cursor % columns;
            if (column + childColumnSpan > columns) continue;
            if (spanOverlaps(taken, row, column, childRowSpan, childColumnSpan)) continue;
            setNormalizedAttribute(child, "tableRow", String.valueOf(row));
            setNormalizedAttribute(child, "tableColumn", String.valueOf(column));
            int declaredRows = parseInt(parent.getAttribute("tableLayoutRows"), 2);
            // Grown by the child's full height: a spanning child placed on the last row needs every
            // row it covers to exist, or the constraint reaches past the declared table again.
            int needed = row + childRowSpan;
            if (declaredRows < needed) {
                setNormalizedAttribute(parent, "tableLayoutRows", String.valueOf(needed));
            }
            return;
        }
    }

    /**
     * Marks every cell a sibling covers, not just the one it starts in. A component with a span
     * greater than one occupies a rectangle; treating it as a single cell handed the next component
     * a slot underneath it, and TableLayout then placed two children in the same space.
     *
     * @param taken the cells already claimed
     * @param row the candidate anchor row
     * @param column the candidate anchor column
     * @param rowSpan how many rows the child covers
     * @param columnSpan how many columns the child covers
     * @return true when any cell of that rectangle is already claimed
     */
    private static boolean spanOverlaps(Set<String> taken, int row, int column, int rowSpan, int columnSpan) {
        for (int r = row; r < row + rowSpan; r++) {
            for (int c = column; c < column + columnSpan; c++) {
                if (taken.contains(r + ":" + c)) return true;
            }
        }
        return false;
    }

    private static void markOccupied(Set<String> taken, Element parent, Element sibling) {
        int row = effectiveTableRow(parent, sibling);
        int column = effectiveTableColumn(parent, sibling);
        int rowSpan = Math.max(1, parseInt(sibling.getAttribute("tableVerticalSpan"), 1));
        int columnSpan = Math.max(1, parseInt(sibling.getAttribute("tableHorizontalSpan"), 1));
        for (int r = row; r < row + rowSpan; r++) {
            for (int c = column; c < column + columnSpan; c++) {
                taken.add(r + ":" + c);
            }
        }
    }

    /** The declared column count of a table, never below one. */
    public static int tableColumns(Element parent) {
        return Math.max(1, parseInt(parent == null ? null : parent.getAttribute("tableLayoutColumns"), 2));
    }

    /**
     * The row a table child occupies. Components loaded from a hand written .gui file may carry no
     * cell; they fall back to sibling order so that the preview, the generated source and the
     * inspector all place them identically.
     */
    public static int effectiveTableRow(Element parent, Element child) {
        Integer declared = parseIntOrNull(child == null ? null : child.getAttribute("tableRow"));
        return declared != null ? Math.max(0, declared.intValue())
                : componentsIn(parent).indexOf(child) / tableColumns(parent);
    }

    /** The column a table child occupies; see {@link #effectiveTableRow}. */
    public static int effectiveTableColumn(Element parent, Element child) {
        Integer declared = parseIntOrNull(child == null ? null : child.getAttribute("tableColumn"));
        int columns = tableColumns(parent);
        return declared != null ? Math.max(0, Math.min(columns - 1, declared.intValue()))
                : componentsIn(parent).indexOf(child) % columns;
    }

    private static int parseInt(String value, int fallback) {
        Integer parsed = parseIntOrNull(value);
        return parsed == null ? fallback : parsed.intValue();
    }

    private static Integer parseIntOrNull(String value) {
        if (value == null || value.trim().length() == 0) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public boolean deleteSelected() {
        if (selected == root) return false;
        Element parent = findParent(root, selected);
        if (parent == null) return false;
        int index = childIndex(parent, selected);
        if (index < 0) return false;
        Element removed = selected;
        beginTransaction();
        try {
            parent.removeChildAt(index);
            selected = parent;
            // Guided Layout relationships are stored by name. Leaving a reference to a deleted
            // component behind makes its dependents jump to an inset measured from nothing.
            clearReferencesTo(root, namesIn(removed, new LinkedHashSet<String>()));
            modified = true;
        } finally {
            endTransaction();
        }
        return true;
    }

    /**
     * Renames the selected component, guaranteeing a unique name and repointing every Guided
     * Layout relationship that referenced the old name, as one undo step. Returns the name that
     * was actually applied, which differs from the request when the request was already taken.
     */
    public String renameSelected(String requestedName) {
        Element target = selected;
        if (target == null || !"component".equals(target.getTagName())) return null;
        String previous = target.getAttribute("name");
        String requested = sanitizeName(requestedName == null ? "" : requestedName.trim());
        if (requested.length() == 0 || requested.equals(previous)) return previous;
        Set<Element> exclude = new LinkedHashSet<>();
        exclude.add(target);
        String unique = availableName(requested, exclude, null);
        beginTransaction();
        try {
            setNormalizedAttribute(target, "name", unique);
            if (previous != null && previous.length() > 0) {
                Map<String, String> renames = new LinkedHashMap<>();
                renames.put(previous, unique);
                remapReferences(root, renames);
            }
            modified = true;
        } finally {
            endTransaction();
        }
        return unique;
    }

    /**
     * Strips the characters the delimited attributes reserve.
     *
     * <p>guidedReferences is pipe separated and remapReferences() writes a renamed anchor straight
     * into it, so a name containing a pipe split into several sides on the next rebuild -- the
     * anchor stopped resolving and its dependant jumped. Whitespace goes for the same reason:
     * guidedReferencePositions is space separated.
     *
     * @param name the requested name
     * @return the name with reserved characters removed
     */
    static String sanitizeName(String name) {
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '|' || Character.isWhitespace(c)) continue;
            out.append(c);
        }
        return out.toString();
    }

    public String copySelectedXml() {
        return new XMLWriter(true).toXML(selected);
    }

    /**
     * True for the types that can only be a document root.
     *
     * @param type the {@code type} attribute
     * @return true when the type is a screen rather than a component
     */
    public static boolean isRootOnlyType(String type) {
        return "Form".equals(type) || "Dialog".equals(type);
    }

    /**
     * @param xml the component XML to paste
     * @return the pasted element, or null when there is nothing to paste or the target parent is a
     *     BorderLayout with no free region
     */
    public Element pasteXml(String xml) {
        if (xml == null || xml.length() == 0) return null;
        Element pasted = new XMLParser().parse(new StringReader(xml));
        if (pasted == null || !"component".equals(pasted.getTagName())) return null;
        // A Form or Dialog is a screen, not a child. Copy on the root and Paste put a cloned screen
        // underneath itself: the canvas substitutes a content pane container for the nested
        // element while the generated source emits new Form(...), so the two hierarchies stop matching -- and
        // the palette cannot legitimately add one either.
        if (isRootOnlyType(pasted.getAttribute("type"))) return null;
        // Same rule as addComponent(): a sixth child does not stack up in a BorderLayout, it evicts
        // one of the five while the XML keeps both.
        Element target = acceptsChildren(selected) ? selected : findParent(root, selected);
        if (borderLayoutIsFull(target == null ? root : target)) return null;
        beginTransaction();
        try {
            if (pasted.getParent() != null) removeChild(pasted.getParent(), pasted);
            Element parent = acceptsChildren(selected) ? selected : findParent(root, selected);
            if (parent == null) parent = root;
            uniquifyPastedNames(pasted);
            parent.addChild(pasted);
            // Pasted XML keeps the cell it was copied from, so pasting a table child back into its
            // own table put two components in one cell. The cell it carries is dropped and a free
            // one assigned, exactly as for a component added from the palette.
            if ("TableLayout".equals(parent.getAttribute("layout"))) {
                setNormalizedAttribute(pasted, "tableRow", null);
                setNormalizedAttribute(pasted, "tableColumn", null);
                assignFreeTableCell(parent, pasted);
            }
            selected = pasted;
            modified = true;
        } finally {
            endTransaction();
        }
        return pasted;
    }

    /**
     * A pasted subtree keeps its internal relationships but must not collide with names that are
     * already used: every component in it is renamed where necessary and the references inside the
     * subtree follow. Names outside the subtree keep pointing at the original components.
     */
    private void uniquifyPastedNames(Element pasted) {
        List<Element> elements = new ArrayList<>();
        collect(pasted, elements);
        Set<Element> exclude = new LinkedHashSet<>(elements);
        Set<String> assigned = new LinkedHashSet<>();
        Map<String, String> renames = new LinkedHashMap<>();
        for (Element element : elements) {
            String current = element.getAttribute("name");
            String type = element.getAttribute("type");
            String base = current == null || current.length() == 0
                    ? lowerFirst(type == null || type.length() == 0 ? "component" : type) : current;
            String unique = availableName(base, exclude, assigned);
            assigned.add(unique);
            if (unique.equals(current)) continue;
            setNormalizedAttribute(element, "name", unique);
            if (current != null && current.length() > 0) renames.put(current, unique);
        }
        if (!renames.isEmpty()) remapReferences(pasted, renames);
    }

    /** Returns `requested` when it is free, otherwise the same base with the lowest free suffix. */
    private String availableName(String requested, Set<Element> exclude, Set<String> reserved) {
        if (isNameAvailable(requested, exclude, reserved)) return requested;
        String base = requested;
        while (base.length() > 1 && base.charAt(base.length() - 1) >= '0'
                && base.charAt(base.length() - 1) <= '9') {
            base = base.substring(0, base.length() - 1);
        }
        for (int index = 1; index < 10000; index++) {
            String candidate = base + index;
            if (isNameAvailable(candidate, exclude, reserved)) return candidate;
        }
        return requested;
    }

    private boolean isNameAvailable(String name, Set<Element> exclude, Set<String> reserved) {
        if (reserved != null && reserved.contains(name)) return false;
        return !containsName(root, name, exclude);
    }

    private static void remapReferences(Element element, Map<String, String> renames) {
        String[] references = splitReferences(element);
        if (references != null) {
            boolean changed = false;
            for (int i = 0; i < references.length; i++) {
                String replacement = renames.get(references[i].trim());
                if (replacement == null) continue;
                references[i] = replacement;
                changed = true;
            }
            if (changed) setNormalizedAttribute(element, "guidedReferences", joinReferences(references));
        }
        for (String attribute : REFERENCE_NAME_ATTRIBUTES) {
            String value = element.getAttribute(attribute);
            String replacement = value == null ? null : renames.get(value.trim());
            if (replacement != null) setNormalizedAttribute(element, attribute, replacement);
        }
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element) remapReferences(((Element) child), renames);
        }
    }

    /**
     * Clears the guided-layout relationships a container's remaining children hold to a component
     * that has just left it. {@code GuidedLayoutSupport.namedChild()} resolves a reference only
     * among the current parent's children, so a reference to a moved-away sibling silently becomes
     * null on the next refresh and the dependent component jumps or resizes.
     *
     * @param parent the container the component was moved out of
     * @param movedAway the component that left
     */
    public void detachReferencesWithin(Element parent, Element movedAway) {
        if (parent == null || movedAway == null) return;
        String name = movedAway.getAttribute("name");
        if (name == null || name.length() == 0) return;
        Set<String> gone = new LinkedHashSet<>();
        gone.add(name);
        for (Element sibling : componentsIn(parent)) {
            if (sibling == movedAway) continue;
            clearReferencesTo(sibling, gone);
        }
    }

    private static void clearReferencesTo(Element element, Set<String> removedNames) {
        String[] references = splitReferences(element);
        if (references != null) {
            boolean changed = false;
            for (int i = 0; i < references.length; i++) {
                if (!removedNames.contains(references[i].trim())) continue;
                references[i] = "-";
                changed = true;
            }
            if (changed) setNormalizedAttribute(element, "guidedReferences", joinReferences(references));
        }
        clearMatchReference(element, "guidedMatchWidth", "guidedHorizontalSize", removedNames);
        clearMatchReference(element, "guidedMatchHeight", "guidedVerticalSize", removedNames);
        String target = element.getAttribute("guidedReferenceTarget");
        if (target != null && removedNames.contains(target.trim())) {
            setNormalizedAttribute(element, "guidedReferenceTarget", null);
        }
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element) clearReferencesTo(((Element) child), removedNames);
        }
    }

    /** A match policy whose target is gone falls back to the component's own preferred size. */
    private static void clearMatchReference(Element element, String matchAttribute,
            String policyAttribute, Set<String> removedNames) {
        String value = element.getAttribute(matchAttribute);
        if (value == null || !removedNames.contains(value.trim())) return;
        setNormalizedAttribute(element, matchAttribute, null);
        if (SIZE_MATCH.equals(element.getAttribute(policyAttribute))) {
            setNormalizedAttribute(element, policyAttribute, SIZE_PREFERRED);
        }
    }

    private static String[] splitReferences(Element element) {
        String raw = element.getAttribute("guidedReferences");
        return raw == null || raw.length() == 0 ? null : raw.split("\\|", -1);
    }

    private static String joinReferences(String[] references) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < references.length; i++) {
            if (i > 0) out.append('|');
            out.append(references[i] == null || references[i].trim().length() == 0
                    ? "-" : references[i].trim());
        }
        return out.toString();
    }

    private static Set<String> namesIn(Element element, Set<String> out) {
        String name = element.getAttribute("name");
        if (name != null && name.length() > 0) out.add(name);
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) {
                namesIn(((Element) child), out);
            }
        }
        return out;
    }

    public boolean moveSelectedTo(Element target) {
        return moveSelectedTo(target, false);
    }

    public boolean moveSelectedTo(Element target, boolean afterTarget) {
        if (selected == root || target == null || target == selected) return false;
        Element oldParent = findParent(root, selected);
        Element targetParent = findParent(root, target);
        if (oldParent == null) return false;
        Element destination = acceptsChildren(target) ? target : targetParent;
        if (destination == null || isDescendant(selected, destination)) return false;
        beforeMutation();
        int targetIndex = destination.getNumChildren();
        if (destination == targetParent) {
            for (int i = 0; i < destination.getNumChildren(); i++) {
                if (destination.getChildAt(i) == target) {
                    targetIndex = i + (afterTarget ? 1 : 0);
                    break;
                }
            }
        }
        int oldIndex = destination == oldParent ? childIndex(oldParent, selected) : -1;
        removeChild(oldParent, selected);
        if (oldIndex >= 0 && oldIndex < targetIndex) targetIndex--;
        if (destination == oldParent && targetIndex > destination.getNumChildren()) targetIndex = destination.getNumChildren();
        destination.insertChildAt(selected, targetIndex);
        modified = true;
        return true;
    }

    public int componentIndex(Element parent, Element child) {
        if (parent == null || child == null) return -1;
        int componentIndex = 0;
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object value = parent.getChildAt(i);
            if (!(value instanceof Element) || !"component".equals(((Element) value).getTagName())) continue;
            if (((Element) value) == child) return componentIndex;
            componentIndex++;
        }
        return -1;
    }

    public boolean moveSelectedToParent(Element destination, int componentIndex) {
        if (selected == root || destination == null || !acceptsChildren(destination)
                || selected == destination || isDescendant(selected, destination)) return false;
        Element oldParent = findParent(root, selected);
        if (oldParent == null) return false;
        beforeMutation();
        removeChild(oldParent, selected);
        int xmlIndex = destination.getNumChildren();
        int seen = 0;
        for (int i = 0; i < destination.getNumChildren(); i++) {
            Object child = destination.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) {
                if (seen >= componentIndex) { xmlIndex = i; break; }
                seen++;
            }
        }
        destination.insertChildAt(selected, Math.max(0, Math.min(xmlIndex, destination.getNumChildren())));
        modified = true;
        return true;
    }

    public List<Element> components() {
        List<Element> result = new ArrayList<>();
        collect(root, result);
        return result;
    }

    /** The direct component children of {@code parent}, in document order, skipping non-component tags. */
    public static List<Element> componentsIn(Element parent) {
        List<Element> result = new ArrayList<>();
        if (parent == null) return result;
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object child = parent.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) result.add(((Element) child));
        }
        return result;
    }

    public String toXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" + xmlOnly();
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    public void beginTransaction() {
        if (transactionDepth++ == 0) transactionStart = capture();
    }

    public void endTransaction() {
        if (transactionDepth == 0) return;
        if (--transactionDepth == 0) {
            if (transactionStart != null && !transactionStart.xml.equals(xmlOnly())) {
                undo.add(transactionStart);
                redo.clear();
            }
            transactionStart = null;
        }
    }

    /**
     * Abandons the current transaction, putting the document back exactly as it was when the
     * transaction opened and recording nothing on the undo stack.
     *
     * <p>A rejected drag used to be cleaned up with ordinary edits, so the insertion and its
     * deletion both landed in the history and the next Undo resurrected a component the user had
     * been told was discarded -- and left a freshly opened document marked modified even though its
     * XML was unchanged.
     */
    public void abortTransaction() {
        if (transactionDepth == 0) return;
        transactionDepth = 0;
        State start = transactionStart;
        transactionStart = null;
        if (start != null) restore(start);
    }

    public boolean undo() {
        if (!canUndo()) return false;
        redo.add(capture());
        restore(undo.remove(undo.size() - 1));
        return true;
    }

    public boolean redo() {
        if (!canRedo()) return false;
        undo.add(capture());
        restore(redo.remove(redo.size() - 1));
        return true;
    }

    public static boolean acceptsChildren(Element element) {
        String type = element == null ? null : element.getAttribute("type");
        return "Form".equals(type) || "Container".equals(type) || "Dialog".equals(type)
                || "Tabs".equals(type) || "Accordion".equals(type);
    }

    private String uniqueName(String type) {
        int index = 1;
        while (containsName(root, lowerFirst(type) + index, null)) index++;
        return lowerFirst(type) + index;
    }

    private static String lowerFirst(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }

    private static boolean containsName(Element element, String name, Set<Element> exclude) {
        if ((exclude == null || !exclude.contains(element)) && name.equals(element.getAttribute("name"))) {
            return true;
        }
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element && containsName(((Element) child), name, exclude)) return true;
        }
        return false;
    }

    private static Element findParent(Element parent, Element target) {
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object child = parent.getChildAt(i);
            if (child == target) return parent;
            if (child instanceof Element) {
                Element found = findParent(((Element) child), target);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void removeChild(Element parent, Element child) {
        for (int i = 0; i < parent.getNumChildren(); i++) {
            if (parent.getChildAt(i) == child) {
                parent.removeChildAt(i);
                return;
            }
        }
    }

    private static int childIndex(Element parent, Element child) {
        for (int i = 0; i < parent.getNumChildren(); i++) {
            if (parent.getChildAt(i) == child) return i;
        }
        return -1;
    }

    private static boolean isDescendant(Element ancestor, Element candidate) {
        if (ancestor == candidate) return true;
        for (int i = 0; i < ancestor.getNumChildren(); i++) {
            Object child = ancestor.getChildAt(i);
            if (child instanceof Element && isDescendant(((Element) child), candidate)) return true;
        }
        return false;
    }

    private static boolean containsIdentity(Element parent, Element target) {
        if (parent == target) return true;
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object child = parent.getChildAt(i);
            if (child instanceof Element && containsIdentity(((Element) child), target)) return true;
        }
        return false;
    }

    private static void collect(Element element, List<Element> result) {
        result.add(element);
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) collect(((Element) child), result);
        }
    }

    /** Attributes whose value is the user's own text. Trimming these, or dropping them when they
     * are set to the empty string, changes what the form says: the leading spaces in a label
     * disappear, and clearing a text field brings the sample placeholder back instead of leaving it
     * empty. Structural attributes stay normalized because a stray space in a cell index or a
     * layout name is never intentional. */
    private static final String TEXTUAL_ATTRIBUTES = "|text|hint|title|";

    /** XMLParser stores case-insensitive attribute keys in lowercase. Always mutate that canonical
     * key and remove a possible camel-case duplicate created by older GUI Builder versions. */
    private static void setNormalizedAttribute(Element element, String name, String value) {
        String canonical = name.toLowerCase();
        element.removeAttribute(canonical);
        if (!canonical.equals(name)) element.removeAttribute(name);
        if (value == null) return;
        if (TEXTUAL_ATTRIBUTES.indexOf("|" + canonical + "|") >= 0) {
            element.setAttribute(canonical, value);
            return;
        }
        if (value.trim().length() > 0) element.setAttribute(canonical, value.trim());
    }

    private void beforeMutation() {
        if (transactionDepth > 0) return;
        undo.add(capture());
        redo.clear();
    }

    private State capture() {
        return new State(xmlOnly(), selected == null ? null : selected.getAttribute("name"), modified);
    }

    /**
     * Serializes the tree with attributes in a stable order.
     *
     * <p>Element stores attributes in a Hashtable, so XMLWriter emits them in whatever order that
     * table happens to iterate. Two trees describing the same form then produce different text,
     * which made undo comparison unreliable -- an edit-then-undo round trip reported a change that
     * had not happened -- and made every save churn unrelated lines in the .gui file. Ordering is
     * type, name, layout, then the rest alphabetically: the identifying attributes first, so the
     * files stay readable.
     */
    private String xmlOnly() {
        StringBuilder out = new StringBuilder();
        writeElement(root, 0, out);
        return out.toString();
    }

    private static final String[] LEADING_ATTRIBUTES = {"type", "name", "layout"};

    private static void writeElement(Element element, int depth, StringBuilder out) {
        // String.repeat is outside the Codename One runtime API the compliance check enforces.
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < depth; i++) indentBuilder.append('\t');
        String indent = indentBuilder.toString();
        out.append(indent).append('<').append(element.getTagName());
        for (String key : orderedAttributeNames(element)) {
            out.append(' ').append(key).append("=\"").append(escape(element.getAttribute(key))).append('"');
        }
        // Every tag, not just components: a form also carries <command> children for its toolbar,
        // and dropping them here would delete the toolbar on the next save.
        List<Element> children = new ArrayList<>();
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element && ((Element) child).getTagName() != null) children.add(((Element) child));
        }
        if (children.isEmpty()) {
            out.append(" />\n");
            return;
        }
        out.append(">\n");
        for (Element child : children) writeElement(child, depth + 1, out);
        out.append(indent).append("</").append(element.getTagName()).append(">\n");
    }

    private static List<String> orderedAttributeNames(Element element) {
        java.util.Hashtable attributes = element.getAttributes();
        List<String> rest = new ArrayList<>();
        if (attributes != null) {
            for (java.util.Enumeration keys = attributes.keys(); keys.hasMoreElements();) {
                rest.add(String.valueOf(keys.nextElement()));
            }
        }
        java.util.Collections.sort(rest);
        List<String> ordered = new ArrayList<>();
        for (String leading : LEADING_ATTRIBUTES) {
            if (rest.remove(leading)) ordered.add(leading);
        }
        ordered.addAll(rest);
        return ordered;
    }

    private static String escape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                // XML attribute normalization turns a literal newline, carriage return or tab into
                // a space when the file is read back, so a multi line TextArea value quietly
                // collapsed onto one line the first time the form was saved and reopened.
                case '\n':
                    out.append("&#10;");
                    break;
                case '\r':
                    out.append("&#13;");
                    break;
                case '\t':
                    out.append("&#9;");
                    break;
                default:
                    out.append(c);
                    break;
            }
        }
        return out.toString();
    }

    private void restore(State state) {
        root = new XMLParser().parse(new StringReader(state.xml));
        selected = state.selectedName == null ? root : findByName(root, state.selectedName);
        if (selected == null) selected = root;
        modified = state.modified;
    }

    private static Element findByName(Element element, String name) {
        if (name.equals(element.getAttribute("name"))) return element;
        for (int i = 0; i < element.getNumChildren(); i++) {
            Object child = element.getChildAt(i);
            if (child instanceof Element) {
                Element found = findByName((Element) child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static final class State {
        final String xml;
        final String selectedName;
        final boolean modified;
        State(String xml, String selectedName, boolean modified) {
            this.xml = xml;
            this.selectedName = selectedName;
            this.modified = modified;
        }
    }
}
