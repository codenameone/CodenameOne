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

import com.codename1.guibuilder.model.GuiDocument;
import com.codename1.guibuilder.ui.ComponentPreviewFactory;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.xml.Element;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nested containers are where the designer is weakest: a move is a remove from one parent and an
 * add to another, and anything that fails between the two loses the component from the document
 * entirely. These tests hold the whole tree to account after every single gesture rather than
 * checking only the component that moved, because the symptom of the interesting bugs is a
 * component that is in neither its old parent nor its new one.
 */
class NestedHierarchyTest {
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    // ---- draining a container completely --------------------------------------------------

    @Test
    void movingEveryChildOutOfAContainerLosesNone() throws Exception {
        GuiDocument document = threeLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);

        for (String child : Arrays.asList("nestedA", "nestedB", "nestedC", "nestedD")) {
            Element moved = named(document, child);
            Element destination = named(document, "rightActions");
            assertTrue(builder.applyDropPlan(moved, plan(document, destination, destination, "BoxLayout", true), 0, 0),
                    "moving " + child + " out of the grid was rejected");
            assertHierarchyIsSound(document, everything, "after moving " + child);
            assertSame(destination, document.parentOf(moved),
                    child + " did not land in the destination: " + document.toXml());
        }
        assertEquals(0, GuiDocument.componentsIn(named(document, "leftGrid")).size(),
                "the source container should now be empty: " + document.toXml());
        assertEquals(6, GuiDocument.componentsIn(named(document, "rightActions")).size(),
                "every moved component must be in the destination: " + document.toXml());
    }

    @Test
    void anEmptiedContainerStillAcceptsComponentsBack() throws Exception {
        GuiDocument document = threeLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);
        Element grid = named(document, "leftGrid");
        Element rightActions = named(document, "rightActions");

        for (String child : Arrays.asList("nestedA", "nestedB", "nestedC", "nestedD")) {
            assertTrue(builder.applyDropPlan(named(document, child),
                    plan(document, rightActions, rightActions, "BoxLayout", true), 0, 0));
        }
        assertEquals(0, GuiDocument.componentsIn(grid).size());

        // Dropping onto a container that renders only its empty-state hint must still target the
        // container, not fall through to whatever is behind it.
        for (String child : Arrays.asList("nestedA", "nestedB")) {
            assertTrue(builder.applyDropPlan(named(document, child),
                    plan(document, grid, grid, "GridLayout", true), 0, 0),
                    "the emptied container refused " + child);
            assertHierarchyIsSound(document, everything, "after returning " + child);
        }
        assertEquals(Arrays.asList("nestedA", "nestedB"), childNames(grid), document.toXml());
    }

    @Test
    void drainingAContainerThenItsParentKeepsEveryComponent() throws Exception {
        GuiDocument document = threeLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);
        Element root = document.root();

        for (String child : Arrays.asList("nestedA", "nestedB", "nestedC", "nestedD")) {
            assertTrue(builder.applyDropPlan(named(document, child),
                    plan(document, named(document, "rightActions"), named(document, "rightActions"), "BoxLayout", true), 0, 0));
            assertHierarchyIsSound(document, everything, "after draining " + child);
        }
        // Now move the emptied container itself up to the form.
        assertTrue(builder.applyDropPlan(named(document, "leftGrid"),
                plan(document, root, root, "BorderLayout", true), 0, 0));
        assertHierarchyIsSound(document, everything, "after moving the emptied container to the form");
        assertSame(root, document.parentOf(named(document, "leftGrid")), document.toXml());
    }

    // ---- moving containers, not just leaves ------------------------------------------------

    @Test
    void movingAContainerCarriesItsWholeSubtree() throws Exception {
        GuiDocument document = threeLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);
        Element grid = named(document, "leftGrid");
        Element rightActions = named(document, "rightActions");

        assertTrue(builder.applyDropPlan(grid, plan(document, rightActions, rightActions, "BoxLayout", true), 0, 0));

        assertHierarchyIsSound(document, everything, "after moving a populated container");
        assertSame(rightActions, document.parentOf(grid), document.toXml());
        assertEquals(Arrays.asList("nestedA", "nestedB", "nestedC", "nestedD"), childNames(grid),
                "the subtree must travel intact: " + document.toXml());
    }

    @Test
    void aContainerCannotBeDroppedIntoItsOwnDescendant() throws Exception {
        GuiDocument document = threeLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);
        Element columns = named(document, "contentColumns");
        Element grid = named(document, "leftGrid");
        String before = document.toXml();

        builder.applyDropPlan(columns, plan(document, grid, grid, "GridLayout", true), 0, 0);

        assertEquals(before, document.toXml(), "a cycle must be refused without touching the document");
        assertHierarchyIsSound(document, everything, "after a refused cyclic drop");
    }

    @Test
    void deeplyNestedComponentsSurviveAMoveToTheOutermostForm() throws Exception {
        GuiDocument document = fourLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);
        Element root = document.root();

        assertTrue(builder.applyDropPlan(named(document, "deepLeaf"),
                plan(document, root, root, "BorderLayout", true), 0, 0));

        assertHierarchyIsSound(document, everything, "after hoisting a leaf from four levels down");
        assertSame(root, document.parentOf(named(document, "deepLeaf")), document.toXml());
    }

    @Test
    void aComponentCanTravelDownIntoTheDeepestContainer() throws Exception {
        GuiDocument document = fourLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);
        Element deepest = named(document, "levelThree");

        assertTrue(builder.applyDropPlan(named(document, "topLevelButton"),
                plan(document, deepest, deepest, "BoxLayout", true), 0, 0));

        assertHierarchyIsSound(document, everything, "after pushing a component four levels down");
        assertSame(deepest, document.parentOf(named(document, "topLevelButton")), document.toXml());
    }

    // ---- every parent layout ----------------------------------------------------------------

    @Test
    void everyParentLayoutAcceptsAComponentFromAnotherContainer() throws Exception {
        for (String layout : Arrays.asList("BoxLayout", "BorderLayout", "GridLayout", "FlowLayout", "TableLayout", "LayeredLayout")) {
            GuiDocument document = documentWithDestinationLayout(layout);
            CodenameOneGUIBuilder builder = builder(document);
            Set<String> everything = names(document);
            Element destination = named(document, "destination");
            Element traveller = named(document, "traveller");

            assertTrue(builder.applyDropPlan(traveller, plan(document, destination, destination, layout, true), 0, 0),
                    layout + " refused an incoming component");
            assertHierarchyIsSound(document, everything, "after a drop into " + layout);
            assertSame(destination, document.parentOf(traveller),
                    layout + " did not adopt the component: " + document.toXml());
            assertRendersFaithfully(document, "after a drop into " + layout);
        }
    }

    @Test
    void everySourceLayoutReleasesItsLastChild() throws Exception {
        for (String layout : Arrays.asList("BoxLayout", "BorderLayout", "GridLayout", "FlowLayout", "TableLayout", "LayeredLayout")) {
            GuiDocument document = documentWithSourceLayout(layout);
            CodenameOneGUIBuilder builder = builder(document);
            Set<String> everything = names(document);
            Element destination = named(document, "destination");

            assertTrue(builder.applyDropPlan(named(document, "traveller"),
                    plan(document, destination, destination, "BoxLayout", true), 0, 0),
                    layout + " would not release its only child");
            assertHierarchyIsSound(document, everything, "after emptying a " + layout);
            assertEquals(0, GuiDocument.componentsIn(named(document, "source")).size(),
                    layout + " kept a copy of the component it released: " + document.toXml());
        }
    }

    // ---- filling a container past what its layout can show --------------------------------------

    @Test
    void componentsPiledIntoABorderLayoutNeverShareARegion() throws Exception {
        GuiDocument document = document("<component type=\"Form\" layout=\"BoxLayout\" name=\"Form\">\n"
                + "    <component type=\"Container\" name=\"source\" layout=\"BoxLayout\">\n"
                + "        <component type=\"Button\" name=\"one\" text=\"One\" />\n"
                + "        <component type=\"Button\" name=\"two\" text=\"Two\" />\n"
                + "        <component type=\"Button\" name=\"three\" text=\"Three\" />\n"
                + "    </component>\n"
                + "    <component type=\"Container\" name=\"destination\" layout=\"BorderLayout\">\n"
                + "        <component type=\"Label\" name=\"resident\" text=\"Resident\" layoutConstraint=\"Center\" />\n"
                + "    </component>\n"
                + "</component>");
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);
        Element destination = named(document, "destination");

        for (String child : Arrays.asList("one", "two", "three")) {
            CodenameOneGUIBuilder.DropPlan plan = plan(document, destination, destination, "BorderLayout", true);
            plan.constraint = "Center";
            plan.occupied = GuiDocument.childAtBorderConstraint(destination, "Center", named(document, child));
            builder.applyDropPlan(named(document, child), plan, 0, 0);
            assertHierarchyIsSound(document, everything, "after piling " + child + " into Center");
        }
        assertNoSharedBorderRegion(destination, document);
        assertRendersFaithfully(document, "after piling components into one BorderLayout region");
    }

    @Test
    void aGridFilledPastItsDeclaredCellsStillShowsEveryChild() throws Exception {
        GuiDocument document = documentWithDestinationLayout("GridLayout");
        CodenameOneGUIBuilder builder = builder(document);
        Element destination = named(document, "destination");
        Set<String> everything = names(document);

        for (int i = 0; i < 6; i++) {
            document.select(destination);
            Element added = document.addComponent("Button");
            assertNotNull(added, "the grid refused an extra child");
            everything.add(added.getAttribute("name"));
        }
        assertHierarchyIsSound(document, everything, "after overfilling a 2x2 grid");
        assertRendersFaithfully(document, "after overfilling a 2x2 grid");
    }

    @Test
    void aTableFilledPastItsDeclaredCellsStillShowsEveryChild() throws Exception {
        GuiDocument document = documentWithDestinationLayout("TableLayout");
        CodenameOneGUIBuilder builder = builder(document);
        Element destination = named(document, "destination");
        Set<String> everything = names(document);

        for (int i = 0; i < 6; i++) {
            document.select(destination);
            Element added = document.addComponent("Button");
            assertNotNull(added, "the table refused an extra child");
            everything.add(added.getAttribute("name"));
        }
        assertHierarchyIsSound(document, everything, "after overfilling a 2x2 table");
        assertNoSharedTableCell(destination, document);
        assertRendersFaithfully(document, "after overfilling a 2x2 table");
    }

    private static void assertNoSharedBorderRegion(Element parent, GuiDocument document) {
        Set<String> used = new LinkedHashSet<>();
        for (Element child : GuiDocument.componentsIn(parent)) {
            String constraint = GuiDocument.effectiveBorderConstraint(parent, child);
            assertTrue(used.add(constraint),
                    "two components share the " + constraint + " region, so only one of them is ever"
                            + " visible:\n" + document.toXml());
        }
    }

    private static void assertNoSharedTableCell(Element parent, GuiDocument document) {
        Set<String> used = new LinkedHashSet<>();
        for (Element child : GuiDocument.componentsIn(parent)) {
            String cell = GuiDocument.effectiveTableRow(parent, child) + ":"
                    + GuiDocument.effectiveTableColumn(parent, child);
            assertTrue(used.add(cell),
                    "two components share cell " + cell + ", so only one of them is ever visible:\n"
                            + document.toXml());
        }
    }

    @Test
    void emptyingAColumnDoesNotPushTheOtherColumnOffTheDevice() {
        // Exactly the shape of NestedLayoutsForm once its grid column has been drained.
        GuiDocument document = document("<component type=\"Form\" layout=\"BorderLayout\" name=\"Form\">\n"
                + "    <component type=\"Label\" name=\"header\" text=\"Header\" layoutConstraint=\"North\" />\n"
                + "    <component type=\"Container\" name=\"columns\" layout=\"BoxLayout\" boxLayoutAxis=\"X\" layoutConstraint=\"Center\">\n"
                + "        <component type=\"Container\" name=\"emptied\" layout=\"GridLayout\" gridLayoutRows=\"2\" gridLayoutColumns=\"2\" />\n"
                + "        <component type=\"Container\" name=\"filled\" layout=\"BoxLayout\" boxLayoutAxis=\"Y\">\n"
                + "            <component type=\"Label\" name=\"description\" text=\"Drag components between both nested containers.\" />\n"
                + "            <component type=\"Button\" name=\"action\" text=\"Action\" />\n"
                + "            <component type=\"Button\" name=\"movedA\" text=\"A\" />\n"
                + "            <component type=\"Button\" name=\"movedB\" text=\"B\" />\n"
                + "            <component type=\"Button\" name=\"movedC\" text=\"C\" />\n"
                + "            <component type=\"Button\" name=\"movedD\" text=\"D\" />\n"
                + "        </component>\n"
                + "    </component>\n"
                + "</component>");

        // Phone portrait is the narrowest canvas mode the editor offers, and the one the empty
        // container hint used to overflow: it asked for its full text width, which left no room
        // for the neighbouring column and pushed every component past the right edge.
        Container rendered = (Container) render(document, 720, 1200);
        Component form = findPreview(rendered, document.root());
        int rightEdge = (form == null ? rendered : form).getAbsoluteX()
                + (form == null ? rendered : form).getWidth();
        for (Element element : document.components()) {
            if (element == document.root()) continue;
            Component preview = findPreview(rendered, element);
            assertNotNull(preview, element.getAttribute("name") + " does not render");
            assertTrue(preview.getAbsoluteX() < rightEdge,
                    element.getAttribute("name") + " starts at x=" + preview.getAbsoluteX()
                            + ", past the device's right edge at " + rightEdge
                            + "; emptying a column must not push its neighbour off the canvas");
        }
    }

    // ---- undo across nesting ------------------------------------------------------------------

    @Test
    void undoRestoresEveryStepOfADrainInReverse() throws Exception {
        GuiDocument document = threeLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);
        List<String> snapshots = new ArrayList<>();

        for (String child : Arrays.asList("nestedA", "nestedB", "nestedC", "nestedD")) {
            snapshots.add(document.toXml());
            assertTrue(builder.applyDropPlan(named(document, child),
                    plan(document, named(document, "rightActions"), named(document, "rightActions"), "BoxLayout", true), 0, 0));
        }
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            assertTrue(document.undo(), "undo ran out of history at step " + i);
            assertEquals(snapshots.get(i), document.toXml(), "undo did not restore step " + i);
            assertHierarchyIsSound(document, everything, "after undoing step " + i);
        }
    }

    @Test
    void redoReplaysADrainExactly() throws Exception {
        GuiDocument document = threeLevelDocument();
        CodenameOneGUIBuilder builder = builder(document);
        Set<String> everything = names(document);

        for (String child : Arrays.asList("nestedA", "nestedB")) {
            assertTrue(builder.applyDropPlan(named(document, child),
                    plan(document, named(document, "rightActions"), named(document, "rightActions"), "BoxLayout", true), 0, 0));
        }
        String drained = document.toXml();
        assertTrue(document.undo());
        assertTrue(document.undo());
        assertTrue(document.redo());
        assertTrue(document.redo());

        assertEquals(drained, document.toXml(), "redo must reproduce the drained tree exactly");
        assertHierarchyIsSound(document, everything, "after redoing a drain");
    }

    // ---- invariants ---------------------------------------------------------------------------

    /**
     * A move is only correct if the whole document is still coherent afterwards. Checks that no
     * component was lost or duplicated, that parent links agree with the child lists, and that the
     * tree survives a serialize/parse round trip -- the form is written to disk in exactly that way.
     */
    private static void assertHierarchyIsSound(GuiDocument document, Set<String> expected, String when) {
        List<Element> all = document.components();
        List<String> found = new ArrayList<>();
        for (Element element : all) found.add(element.getAttribute("name"));

        Set<String> unique = new LinkedHashSet<>(found);
        assertEquals(found.size(), unique.size(),
                "duplicate component names " + when + ": " + found + "\n" + document.toXml());
        assertEquals(expected, unique,
                "the set of components changed " + when + "; something was lost or invented\n" + document.toXml());

        for (Element element : all) {
            if (element == document.root()) continue;
            Element parent = document.parentOf(element);
            assertNotNull(parent, element.getAttribute("name") + " has no parent " + when
                    + "; it is detached from the tree\n" + document.toXml());
            assertTrue(GuiDocument.componentsIn(parent).contains(element),
                    element.getAttribute("name") + " is not listed by the parent that claims it " + when
                            + "\n" + document.toXml());
        }

        GuiDocument reparsed = GuiDocument.parse("Form.gui", document.toXml());
        assertEquals(structure(document), structure(reparsed),
                "the tree does not survive a save/load round trip " + when + "\n" + document.toXml());
    }

    /**
     * Every component must render exactly once and occupy real space. A component the model still
     * lists but that draws at zero size has disappeared as far as anyone using the editor is
     * concerned -- two components sharing one BorderLayout region behave exactly like that.
     */
    private static void assertRendersFaithfully(GuiDocument document, String when) {
        Container rendered = (Container) render(document, 600, 800);
        for (Element element : document.components()) {
            if (element == document.root()) continue;
            assertEquals(1, countPreviews(rendered, element, 0),
                    element.getAttribute("name") + " does not have exactly one preview " + when
                            + "\n" + document.toXml());
            Component preview = findPreview(rendered, element);
            assertNotNull(preview, element.getAttribute("name") + " has no preview " + when);
            assertTrue(preview.getWidth() > 0 && preview.getHeight() > 0,
                    element.getAttribute("name") + " renders at " + preview.getWidth() + "x"
                            + preview.getHeight() + " " + when + "; it is invisible in the designer\n"
                            + document.toXml());
        }
    }

    private static Component findPreview(Container root, Element element) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            if (component.getClientProperty("gui.element") == element) return component;
            if (component instanceof Container) {
                Component nested = findPreview(((Container) component), element);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static List<String> structure(GuiDocument document) {
        List<String> rows = new ArrayList<>();
        for (Element element : document.components()) {
            Element parent = document.parentOf(element);
            rows.add((parent == null ? "-" : parent.getAttribute("name")) + "/" + element.getAttribute("name"));
        }
        return rows;
    }

    // ---- fixtures -----------------------------------------------------------------------------

    private static GuiDocument threeLevelDocument() {
        return document("<component type=\"Form\" layout=\"BorderLayout\" name=\"NestedLayoutsForm\">\n"
                + "    <component type=\"Label\" name=\"nestedHeader\" text=\"Header\" layoutConstraint=\"North\" />\n"
                + "    <component type=\"Container\" name=\"contentColumns\" layout=\"BoxLayout\" boxLayoutAxis=\"X\" layoutConstraint=\"Center\">\n"
                + "        <component type=\"Container\" name=\"leftGrid\" layout=\"GridLayout\" gridLayoutRows=\"2\" gridLayoutColumns=\"2\">\n"
                + "            <component type=\"Button\" name=\"nestedA\" text=\"A\" />\n"
                + "            <component type=\"Button\" name=\"nestedB\" text=\"B\" />\n"
                + "            <component type=\"Button\" name=\"nestedC\" text=\"C\" />\n"
                + "            <component type=\"Button\" name=\"nestedD\" text=\"D\" />\n"
                + "        </component>\n"
                + "        <component type=\"Container\" name=\"rightActions\" layout=\"BoxLayout\" boxLayoutAxis=\"Y\">\n"
                + "            <component type=\"Label\" name=\"nestedDescription\" text=\"Drag between the columns.\" />\n"
                + "            <component type=\"Button\" name=\"nestedAction\" text=\"Action\" />\n"
                + "        </component>\n"
                + "    </component>\n"
                + "</component>");
    }

    private static GuiDocument fourLevelDocument() {
        return document("<component type=\"Form\" layout=\"BorderLayout\" name=\"DeepForm\">\n"
                + "    <component type=\"Button\" name=\"topLevelButton\" text=\"Top\" layoutConstraint=\"North\" />\n"
                + "    <component type=\"Container\" name=\"levelOne\" layout=\"BoxLayout\" layoutConstraint=\"Center\">\n"
                + "        <component type=\"Container\" name=\"levelTwo\" layout=\"GridLayout\" gridLayoutRows=\"1\" gridLayoutColumns=\"2\">\n"
                + "            <component type=\"Container\" name=\"levelThree\" layout=\"BoxLayout\" boxLayoutAxis=\"Y\">\n"
                + "                <component type=\"Button\" name=\"deepLeaf\" text=\"Deep\" />\n"
                + "            </component>\n"
                + "            <component type=\"Label\" name=\"levelTwoLabel\" text=\"Two\" />\n"
                + "        </component>\n"
                + "    </component>\n"
                + "</component>");
    }

    private static GuiDocument documentWithDestinationLayout(String layout) {
        return document("<component type=\"Form\" layout=\"BoxLayout\" name=\"Form\">\n"
                + "    <component type=\"Container\" name=\"source\" layout=\"BoxLayout\">\n"
                + "        <component type=\"Button\" name=\"traveller\" text=\"Travels\" />\n"
                + "    </component>\n"
                + "    <component type=\"Container\" name=\"destination\" layout=\"" + layout + "\""
                + " gridLayoutRows=\"2\" gridLayoutColumns=\"2\" tableLayoutRows=\"2\" tableLayoutColumns=\"2\">\n"
                + "        <component type=\"Label\" name=\"resident\" text=\"Resident\" />\n"
                + "    </component>\n"
                + "</component>");
    }

    private static GuiDocument documentWithSourceLayout(String layout) {
        return document("<component type=\"Form\" layout=\"BoxLayout\" name=\"Form\">\n"
                + "    <component type=\"Container\" name=\"source\" layout=\"" + layout + "\""
                + " gridLayoutRows=\"2\" gridLayoutColumns=\"2\" tableLayoutRows=\"2\" tableLayoutColumns=\"2\">\n"
                + "        <component type=\"Button\" name=\"traveller\" text=\"Travels\" />\n"
                + "    </component>\n"
                + "    <component type=\"Container\" name=\"destination\" layout=\"BoxLayout\">\n"
                + "        <component type=\"Label\" name=\"resident\" text=\"Resident\" />\n"
                + "    </component>\n"
                + "</component>");
    }

    // ---- helpers ------------------------------------------------------------------------------

    private static CodenameOneGUIBuilder.DropPlan plan(GuiDocument document, Element parent, Element target,
            String layout, boolean after) {
        CodenameOneGUIBuilder.DropPlan plan = new CodenameOneGUIBuilder.DropPlan();
        plan.document = document;
        plan.parent = parent;
        plan.target = target;
        plan.layout = layout;
        plan.after = after;
        plan.valid = true;
        if ("TableLayout".equals(layout)) plan.tableCell = new int[]{0, 0};
        return plan;
    }

    private static Set<String> names(GuiDocument document) {
        Set<String> out = new LinkedHashSet<>();
        for (Element element : document.components()) out.add(element.getAttribute("name"));
        return out;
    }

    private static List<String> childNames(Element parent) {
        List<String> out = new ArrayList<>();
        for (Element child : GuiDocument.componentsIn(parent)) out.add(child.getAttribute("name"));
        return out;
    }

    private static Element named(GuiDocument document, String name) {
        for (Element element : document.components()) {
            if (name.equals(element.getAttribute("name"))) return element;
        }
        return null;
    }

    private static int countPreviews(Container root, Element element, int found) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            if (component.getClientProperty("gui.element") == element) found++;
            if (component instanceof Container) found = countPreviews(((Container) component), element, found);
        }
        return found;
    }

    private static Component render(GuiDocument document, int width, int height) {
        Component rendered = ComponentPreviewFactory.create(document.root(), null,
                new ComponentPreviewFactory.SelectionHandler() {
                    public void selected(Element element) { }
                    public void dragPressed(Element element, Component source, int x, int y) { }
                    public boolean isDragActive() { return false; }
                    public void editContent(Element element) { }
                });
        rendered.setWidth(width);
        rendered.setHeight(height);
        layoutNested((Container) rendered);
        return rendered;
    }

    private static void layoutNested(Container container) {
        container.layoutContainer();
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component child = container.getComponentAt(i);
            if (child instanceof Container) layoutNested((Container) child);
        }
    }

    private static GuiDocument document(String xml) {
        return GuiDocument.parse("Form.gui", xml);
    }

    private static CodenameOneGUIBuilder builder(GuiDocument document) throws Exception {
        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        Field field = CodenameOneGUIBuilder.class.getDeclaredField("document");
        field.setAccessible(true);
        field.set(builder, document);
        Container canvas = new Container(new LayeredLayout());
        canvas.setWidth(600);
        canvas.setHeight(800);
        canvas.add(render(document, 600, 800));
        layoutNested(canvas);
        Field canvasField = CodenameOneGUIBuilder.class.getDeclaredField("canvasHost");
        canvasField.setAccessible(true);
        canvasField.set(builder, canvas);
        return builder;
    }
}
