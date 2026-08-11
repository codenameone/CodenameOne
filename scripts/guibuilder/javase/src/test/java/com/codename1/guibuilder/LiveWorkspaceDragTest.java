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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.xml.Element;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the assembled workspace -- toolbar, canvas, inspector, hierarchy -- rather than the
 * document model alone. The model-level tests pass for gestures that visibly fail in the running
 * editor, because the defects live in what the canvas holds after a commit, not in the XML.
 */
class LiveWorkspaceDragTest {
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!Display.isInitialized()) Display.init(new JPanel());
    }

    @Test
    void aCrossContainerDragLeavesExactlyOnePreviewPerComponent() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        flushEdt();
        assertOnePreviewPerComponent(builder, "after opening the form");

        assertNull(onEdt(() -> builder.mcpDragComponent("nestedB", "nestedAction", "below", null, null)));
        flushEdt();

        assertOnePreviewPerComponent(builder, "after dragging nestedB into the other column");
        assertPreviewsSitInsideTheirModelParent(builder, "after dragging nestedB into the other column");
    }

    @Test
    void repeatedCrossContainerDragsStayStable() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        flushEdt();

        String[] moved = {"nestedB", "nestedC", "nestedD", "nestedA"};
        for (String name : moved) {
            assertNull(onEdt(() -> builder.mcpDragComponent(name, "nestedAction", "below", null, null)),
                    "dragging " + name + " reported an error");
            flushEdt();
            assertOnePreviewPerComponent(builder, "after dragging " + name);
            assertPreviewsSitInsideTheirModelParent(builder, "after dragging " + name);
        }
    }

    @Test
    void drainingAContainerCompletelyKeepsEveryComponent() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        flushEdt();
        List<String> everything = componentNames(builder);
        assertEquals(11, everything.size(), everything.toString());

        for (String name : new String[]{"nestedA", "nestedB", "nestedC", "nestedD"}) {
            assertNull(onEdt(() -> builder.mcpDragComponent(name, "nestedAction", "below", null, null)),
                    "dragging " + name + " reported an error");
            flushEdt();
            assertEquals(everything, componentNames(builder),
                    name + " left the document instead of moving; it is in neither container");
            assertEquals("rightActions", parentName(builder, name), hierarchy(builder).toString());
            assertOnePreviewPerComponent(builder, "after moving " + name);
            assertPreviewsSitInsideTheirModelParent(builder, "after moving " + name);
            assertEveryComponentIsActuallyVisible(builder, "after moving " + name);
        }
        assertEquals(0, childCount(builder, "leftGrid"), hierarchy(builder).toString());
        assertEquals(6, childCount(builder, "rightActions"), hierarchy(builder).toString());
    }

    @Test
    void anEmptiedContainerCanBeFilledAgainByDragging() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        flushEdt();
        List<String> everything = componentNames(builder);

        for (String name : new String[]{"nestedA", "nestedB", "nestedC", "nestedD"}) {
            assertNull(onEdt(() -> builder.mcpDragComponent(name, "nestedAction", "below", null, null)));
            flushEdt();
        }
        assertEquals(0, childCount(builder, "leftGrid"));

        // An emptied container renders only its "drop components here" hint. Dropping onto that
        // hint has to resolve to the container, or the components have nowhere to go back to.
        for (String name : new String[]{"nestedA", "nestedB"}) {
            assertNull(onEdt(() -> builder.mcpDragComponent(name, "leftGrid", "center", null, null)),
                    "the emptied container refused " + name);
            flushEdt();
            assertEquals(everything, componentNames(builder), name + " was lost on the way back");
            assertEquals("leftGrid", parentName(builder, name), hierarchy(builder).toString());
            assertOnePreviewPerComponent(builder, "after returning " + name);
        }
    }

    @Test
    void movingAPopulatedContainerThroughTheCanvasKeepsItsChildren() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        flushEdt();
        List<String> everything = componentNames(builder);

        assertNull(onEdt(() -> builder.mcpDragComponent("leftGrid", "nestedAction", "below", null, null)),
                "dragging a populated container reported an error");
        flushEdt();

        assertEquals(everything, componentNames(builder), "moving a container lost part of its subtree");
        assertEquals("rightActions", parentName(builder, "leftGrid"), hierarchy(builder).toString());
        assertEquals(4, childCount(builder, "leftGrid"), hierarchy(builder).toString());
        assertOnePreviewPerComponent(builder, "after moving a populated container");
        assertPreviewsSitInsideTheirModelParent(builder, "after moving a populated container");
    }

    @Test
    void undoAfterACrossContainerDragRestoresBothTheModelAndTheCanvas() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        flushEdt();
        List<String> before = hierarchy(builder);

        assertNull(onEdt(() -> builder.mcpDragComponent("nestedB", "nestedAction", "below", null, null)));
        flushEdt();
        assertNotEquals(before, hierarchy(builder), "the drag must actually change the tree");

        assertNull(onEdt(() -> builder.mcpCommand("undo")));
        flushEdt();

        assertEquals(before, hierarchy(builder), "undo must restore the tree exactly");
        assertOnePreviewPerComponent(builder, "after undo");
        assertPreviewsSitInsideTheirModelParent(builder, "after undo");
        for (Element element : builder.selectedElementsSnapshot()) {
            assertTrue(builder.isActiveDocumentElement(element),
                    "the selection still points at the pre-undo tree");
        }
    }

    @Test
    void aDragImmediatelyAfterUndoStillCommits() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        flushEdt();
        assertNull(onEdt(() -> builder.mcpDragComponent("nestedB", "nestedAction", "below", null, null)));
        flushEdt();
        assertNull(onEdt(() -> builder.mcpCommand("undo")));
        flushEdt();

        assertNull(onEdt(() -> builder.mcpDragComponent("nestedC", "nestedAction", "below", null, null)),
                "the first drag after an undo must still commit");
        flushEdt();
        assertEquals("rightActions", parentName(builder, "nestedC"), hierarchy(builder).toString());
        assertOnePreviewPerComponent(builder, "after the post-undo drag");
    }

    // ---- harness ---------------------------------------------------------------------------

    /** Runs work on the EDT, exactly as GuiBuilderMcpController does for every MCP tool. */
    private static String onEdt(java.util.function.Supplier<String> work) {
        final String[] out = new String[1];
        Display.getInstance().callSeriallyAndWait(() -> out[0] = work.get());
        return out[0];
    }

    private static CodenameOneGUIBuilder workspace() throws Exception {
        System.setProperty("guibuilder.input", demoBinding().toString());
        System.setProperty("guibuilder.canvasMode", "desktop");
        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        builder.init(null);
        builder.runApp();
        flushEdt();
        return builder;
    }

    /** The binding holds absolute paths, so it is written per run rather than checked in. */
    private static Path demoBinding() throws Exception {
        Path demo = new File("../demo-project").getCanonicalFile().toPath();
        Path input = Files.createTempFile("guibuilder", ".input");
        Files.write(input, ("projectDir=" + demo + "\n"
                + "guiDir=" + demo.resolve("src/main/guibuilder") + "\n"
                + "sourceDir=" + demo.resolve("src/main/java") + "\n"
                + "cssFile=" + demo.resolve("src/main/css/theme.css") + "\n"
                + "initialForm=com.example.NestedLayoutsForm\n").getBytes(StandardCharsets.UTF_8));
        input.toFile().deleteOnExit();
        return input;
    }

    /** Runs queued EDT work to completion, including refreshes that queue further refreshes. */
    private static void flushEdt() {
        for (int i = 0; i < 6; i++) {
            if (Display.getInstance().isEdt()) return;
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Display.getInstance().callSeriallyAndWait(() -> { });
        }
    }

    private static void assertOnePreviewPerComponent(CodenameOneGUIBuilder builder, String when) {
        Container canvas = builder.canvasHostForTest();
        assertNotNull(canvas, "the workspace never built a canvas");
        for (Element element : builder.documentForTest().components()) {
            int count = countPreviews(canvas, element, 0);
            assertEquals(1, count, name(element) + " has " + count + " previews on the canvas " + when
                    + "; a stale preview shadows the live one for hit testing and geometry");
        }
    }

    private static void assertPreviewsSitInsideTheirModelParent(CodenameOneGUIBuilder builder, String when) {
        Container canvas = builder.canvasHostForTest();
        for (Element element : builder.documentForTest().components()) {
            Element parent = builder.documentForTest().parentOf(element);
            if (parent == null) continue;
            Component preview = findPreview(canvas, element);
            Component parentPreview = findPreview(canvas, parent);
            if (preview == null || parentPreview == null) continue;
            assertSame(parentPreview, enclosingPreview(preview),
                    name(element) + " renders inside " + name(elementOf(enclosingPreview(preview)))
                            + " but the model says " + name(parent) + " " + when);
        }
    }

    private static Component enclosingPreview(Component component) {
        for (Container parent = component.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getClientProperty("gui.element") instanceof Element) return parent;
        }
        return null;
    }

    private static Element elementOf(Component component) {
        Object element = component == null ? null : component.getClientProperty("gui.element");
        return element instanceof Element ? ((Element) element) : null;
    }

    private static int countPreviews(Container root, Element element, int found) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            if (component.getClientProperty("gui.element") == element) found++;
            if (component instanceof Container) found = countPreviews(((Container) component), element, found);
        }
        return found;
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

    /**
     * A component that renders at zero size, or entirely outside the form it belongs to, has
     * effectively vanished from the designer even though the model still lists it -- which is what
     * "they are gone from both containers" looks like from the canvas.
     */
    private static void assertEveryComponentIsActuallyVisible(CodenameOneGUIBuilder builder, String when) {
        Container canvas = builder.canvasHostForTest();
        Component form = findPreview(canvas, builder.documentForTest().root());
        assertNotNull(form, "the form itself does not render " + when);
        for (Element element : builder.documentForTest().components()) {
            if (element == builder.documentForTest().root()) continue;
            Component preview = findPreview(canvas, element);
            assertNotNull(preview, name(element) + " has no preview " + when);
            assertTrue(preview.getWidth() > 0 && preview.getHeight() > 0,
                    name(element) + " renders at " + preview.getWidth() + "x" + preview.getHeight()
                            + " " + when + "; it is invisible on the canvas");
            boolean insideHorizontally = preview.getAbsoluteX() + preview.getWidth() > form.getAbsoluteX()
                    && preview.getAbsoluteX() < form.getAbsoluteX() + form.getWidth();
            boolean insideVertically = preview.getAbsoluteY() + preview.getHeight() > form.getAbsoluteY()
                    && preview.getAbsoluteY() < form.getAbsoluteY() + form.getHeight();
            assertTrue(insideHorizontally && insideVertically,
                    name(element) + " renders outside the form " + when + "; component at "
                            + preview.getAbsoluteX() + "," + preview.getAbsoluteY()
                            + " " + preview.getWidth() + "x" + preview.getHeight()
                            + ", form at " + form.getAbsoluteX() + "," + form.getAbsoluteY()
                            + " " + form.getWidth() + "x" + form.getHeight());
        }
    }

    private static List<String> componentNames(CodenameOneGUIBuilder builder) {
        List<String> names = new ArrayList<>();
        for (Element element : builder.documentForTest().components()) names.add(name(element));
        java.util.Collections.sort(names);
        return names;
    }

    private static int childCount(CodenameOneGUIBuilder builder, String containerName) {
        for (Element element : builder.documentForTest().components()) {
            if (containerName.equals(name(element))) {
                return com.codename1.guibuilder.model.GuiDocument.componentsIn(element).size();
            }
        }
        return -1;
    }

    private static List<String> hierarchy(CodenameOneGUIBuilder builder) {
        List<String> rows = new ArrayList<>();
        for (Element element : builder.documentForTest().components()) {
            rows.add(parentName(builder, name(element)) + "/" + name(element));
        }
        return rows;
    }

    private static String parentName(CodenameOneGUIBuilder builder, String componentName) {
        for (Element element : builder.documentForTest().components()) {
            if (componentName.equals(name(element))) {
                Element parent = builder.documentForTest().parentOf(element);
                return parent == null ? "-" : name(parent);
            }
        }
        return null;
    }

    private static String name(Element element) {
        if (element == null) return "?";
        String value = element.getAttribute("name");
        return value == null ? element.getAttribute("type") : value;
    }
}
