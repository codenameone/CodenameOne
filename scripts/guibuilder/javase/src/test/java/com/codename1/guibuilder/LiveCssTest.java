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

import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.xml.Element;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Editing theme.css has to change what the canvas draws; that is the whole point of live CSS. */
class LiveCssTest {
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!Display.isInitialized()) Display.init(new JPanel());
    }

    @Test
    void editingCssRestylesTheLivePreview() throws Exception {
        Path project = Files.createTempDirectory("guibuilder-css");
        Path gui = project.resolve("src/main/guibuilder/com/example");
        Files.createDirectories(gui);
        Files.write(gui.resolve("StyledForm.gui"), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<component type=\"Form\" layout=\"BoxLayout\" name=\"StyledForm\">\n"
                + "    <component type=\"Label\" name=\"styled\" text=\"Styled\" />\n"
                + "</component>\n").getBytes(StandardCharsets.UTF_8));
        Path css = project.resolve("src/main/css/theme.css");
        Files.createDirectories(css.getParent());
        Files.write(css, "Label { color: #ff0000; }\n".getBytes(StandardCharsets.UTF_8));

        Path input = Files.createTempFile("guibuilder", ".input");
        Files.write(input, ("projectDir=" + project + "\nguiDir=" + gui.getParent().getParent() + "\n"
                + "sourceDir=" + project.resolve("src/main/java") + "\ncssFile=" + css + "\n"
                + "initialForm=com.example.StyledForm\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("guibuilder.input", input.toString());
        System.setProperty("guibuilder.canvasMode", "desktop");

        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        builder.init(null);
        builder.runApp();
        settle();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.StyledForm")));
        settle();

        // Open the CSS editor first: that is how anyone edits CSS, and it rebuilds the canvas into
        // a split pane, which is the one thing the direct path never exercises.
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                java.lang.reflect.Method open = CodenameOneGUIBuilder.class.getDeclaredMethod("openCss");
                open.setAccessible(true);
                open.invoke(builder);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        settle();

        int before = foreground(builder, "styled");
        assertEquals(0xff0000, before, "the starting CSS colour must reach the preview");

        Files.write(css, "Label { color: #00ff00; }\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(onEdt(() -> String.valueOf(builder.reloadProjectCssForTest())).equals("true"),
                "recompiling the project CSS failed");
        settle();

        assertEquals(0x00ff00, foreground(builder, "styled"),
                "editing theme.css must restyle the canvas, not just the file");
    }

    @Test
    void contentPaneCssStylesTheLayerTheChildrenLiveIn() throws Exception {
        // At runtime Form.add() puts children in a content pane whose UIID is "ContentPane", and
        // the Form UIID styles the surface around it. The canvas has to draw both layers or a form
        // laid out against ContentPane padding moves the moment it is saved and run.
        CodenameOneGUIBuilder builder = builderFor("ContentPane { background-color: #0000ff; }\n"
                + "Form { background-color: #00ff00; }\n");

        Component styled = find(builder.canvasHostForTest(), builder, "styled");
        assertNotNull(styled, "the label does not render");
        Container pane = styled.getParent();
        assertEquals("ContentPane", pane.getUIID(),
                "children must sit in the content pane the generated Form adds them to");
        assertEquals(0x0000ff, pane.getUnselectedStyle().getBgColor(),
                "ContentPane CSS must reach the layer holding the children");

        Container surface = builder.formSurfaceForTest();
        assertNotNull(surface, "a Form root must have a Form-UIID surface on the canvas");
        assertEquals("Form", surface.getUIID());
        assertEquals(0x00ff00, surface.getUnselectedStyle().getBgColor(),
                "Form CSS must reach the surface, not the content pane");
        assertNotSame(surface, pane, "the Form and its content pane are separate layers");
    }

    @Test
    void editedFormCssReachesTheSurfaceAndNotOnlyTheOpenedStylesheet() throws Exception {
        // The surface is built once per canvas rebuild while a CSS edit installs a brand new
        // UIManager, so it has to be re-pointed at that manager along with everything else.
        CodenameOneGUIBuilder builder = builderFor("Form { background-color: #00ff00; }\n");
        assertEquals(0x00ff00, builder.formSurfaceForTest().getUnselectedStyle().getBgColor());

        Files.write(cssFile, "Form { background-color: #123456; }\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("true", onEdt(() -> String.valueOf(builder.reloadProjectCssForTest())),
                "recompiling the project CSS failed");
        settle();

        assertEquals(0x123456, builder.formSurfaceForTest().getUnselectedStyle().getBgColor(),
                "a CSS edit must restyle the Form surface, not just the stylesheet on disk");
    }

    @Test
    void mcpSaveRefusesWhileAnEditorPaneHoldsUnsavedText() throws Exception {
        // save() writes the document, the companion and the model; it never writes the open pane,
        // and for CSS it does not touch the stylesheet at all. A client told the save succeeded
        // will close the builder, and that text has nowhere else to live.
        CodenameOneGUIBuilder builder = builderFor("Label { color: #ff0000; }\n");
        Display.getInstance().callSeriallyAndWait(CodenameOneGUIBuilder::openActiveCssForTest);
        settle();
        CodeEditor editor = CodenameOneGUIBuilder.activeCodeEditorForTest();
        assertNotNull(editor, "the CSS editor was never created");

        Display.getInstance().callSeriallyAndWait(() -> editor.setText("Label { color: #abcdef; }\n"));
        settle();

        String saved = onEdt(() -> builder.mcpCommand("save"));
        assertNotNull(saved, "save reported success while the CSS pane held the only copy of the edit");
        assertTrue(saved.contains("unsaved"), saved);
        assertEquals("Label { color: #ff0000; }\n", new String(Files.readAllBytes(cssFile), StandardCharsets.UTF_8),
                "the refused save must not have written anything either");

        String refreshed = onEdt(() -> builder.mcpCommand("refresh"));
        assertNotNull(refreshed, "refresh reloads the project over the buffer, so it must refuse too");
    }

    /** Builds a one-label project on the given stylesheet and opens it, as the tests all need. */
    private static CodenameOneGUIBuilder builderFor(String css) throws Exception {
        Path project = Files.createTempDirectory("guibuilder-css");
        Path gui = project.resolve("src/main/guibuilder/com/example");
        Files.createDirectories(gui);
        Files.write(gui.resolve("StyledForm.gui"), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<component type=\"Form\" layout=\"BoxLayout\" name=\"StyledForm\">\n"
                + "    <component type=\"Label\" name=\"styled\" text=\"Styled\" />\n"
                + "</component>\n").getBytes(StandardCharsets.UTF_8));
        cssFile = project.resolve("src/main/css/theme.css");
        Files.createDirectories(cssFile.getParent());
        Files.write(cssFile, css.getBytes(StandardCharsets.UTF_8));

        Path input = Files.createTempFile("guibuilder", ".input");
        Files.write(input, ("projectDir=" + project + "\nguiDir=" + gui.getParent().getParent() + "\n"
                + "sourceDir=" + project.resolve("src/main/java") + "\ncssFile=" + cssFile + "\n"
                + "initialForm=com.example.StyledForm\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("guibuilder.input", input.toString());
        System.setProperty("guibuilder.canvasMode", "desktop");

        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        builder.init(null);
        builder.runApp();
        settle();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.StyledForm")));
        settle();
        return builder;
    }

    /** The stylesheet the most recent builderFor() call wrote. */
    private static Path cssFile;

    private static int foreground(CodenameOneGUIBuilder builder, String name) {
        Component preview = find(builder.canvasHostForTest(), builder, name);
        assertNotNull(preview, name + " does not render");
        return preview.getUnselectedStyle().getFgColor();
    }

    private static Component find(Container root, CodenameOneGUIBuilder builder, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            Object element = component.getClientProperty("gui.element");
            if (element instanceof Element && name.equals(((Element) element).getAttribute("name"))) return component;
            if (component instanceof Container) {
                Component nested = find(((Container) component), builder, name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static String onEdt(java.util.function.Supplier<String> work) {
        final String[] out = new String[1];
        Display.getInstance().callSeriallyAndWait(() -> out[0] = work.get());
        return out[0];
    }

    private static void settle() {
        for (int i = 0; i < 5; i++) {
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Display.getInstance().callSeriallyAndWait(() -> { });
        }
    }
}
