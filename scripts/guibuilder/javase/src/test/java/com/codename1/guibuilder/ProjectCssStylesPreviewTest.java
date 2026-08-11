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
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The design canvas has to be styled by the project's own stylesheet. Everything else about live CSS
 * is pointless if what the canvas shows is unrelated to the file being edited, and a test that
 * writes its own one-rule stylesheet cannot tell the difference -- it proves a rule can reach the
 * preview, not that the project's rules do.
 */
class ProjectCssStylesPreviewTest {
    private static final int PREVIEW_WIDTH = 900;

    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!Display.isInitialized()) Display.init(new JPanel());
    }

    @Test
    void theCanvasIsStyledByTheProjectStylesheet() throws Exception {
        Path project = copyOfDemoProject();
        Path css = project.resolve("src/main/css/theme.css");
        Files.write(css, ("Form { background-color: #ffffff; color: #172033; }\n"
                + "Label, SpanLabel { color: #123456; }\n"
                + "Title { color: #654321; }\n"
                + "Button { color: #abcdef; }\n").getBytes(StandardCharsets.UTF_8));

        CodenameOneGUIBuilder builder = workspace(project);
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        settle();

        assertEquals(0x123456, foreground(builder, "nestedDescription"),
                "a Label on the canvas must take its colour from the project stylesheet");
        assertEquals(0xabcdef, foreground(builder, "nestedAction"),
                "a Button on the canvas must take its colour from the project stylesheet");
    }

    @Test
    void editingTheProjectStylesheetRestylesTheCanvas() throws Exception {
        Path project = copyOfDemoProject();
        Path css = project.resolve("src/main/css/theme.css");
        Files.write(css, "Label, SpanLabel { color: #123456; }\n".getBytes(StandardCharsets.UTF_8));

        CodenameOneGUIBuilder builder = workspace(project);
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.NestedLayoutsForm")));
        settle();
        assertEquals(0x123456, foreground(builder, "nestedDescription"), "the starting colour must apply");

        // pink is what a person actually types, and it exercises the named-colour path.
        Files.write(css, "Label, SpanLabel { color: pink; }\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("true", onEdt(() -> String.valueOf(builder.reloadProjectCssForTest())),
                "recompiling the edited stylesheet failed");
        settle();

        assertEquals(0xffc0cb, foreground(builder, "nestedDescription"),
                "editing the stylesheet must restyle the canvas");
    }

    // ---- harness ----------------------------------------------------------------------------

    private static int foreground(CodenameOneGUIBuilder builder, String name) {
        Component preview = find(builder.canvasHostForTest(), name);
        assertNotNull(preview, name + " does not render on the canvas");
        return preview.getUnselectedStyle().getFgColor();
    }

    private static Component find(Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            Object element = component.getClientProperty("gui.element");
            if (element instanceof Element && name.equals(((Element) element).getAttribute("name"))) return component;
            if (component instanceof Container) {
                Component nested = find(((Container) component), name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    /** Works on a copy so a test never edits the checked-in demo project. */
    private static Path copyOfDemoProject() throws Exception {
        Path source = new File("../demo-project").getCanonicalFile().toPath();
        Path target = Files.createTempDirectory("guibuilder-demo");
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.toArray(Path[]::new)) {
                if (path.toString().contains("/target/")) continue;
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return target;
    }

    private static CodenameOneGUIBuilder workspace(Path project) throws Exception {
        Path input = Files.createTempFile("guibuilder", ".input");
        Files.write(input, ("projectDir=" + project + "\n"
                + "guiDir=" + project.resolve("src/main/guibuilder") + "\n"
                + "sourceDir=" + project.resolve("src/main/java") + "\n"
                + "cssFile=" + project.resolve("src/main/css/theme.css") + "\n"
                + "initialForm=com.example.NestedLayoutsForm\n").getBytes(StandardCharsets.UTF_8));
        input.toFile().deleteOnExit();
        System.setProperty("guibuilder.input", input.toString());
        System.setProperty("guibuilder.canvasMode", "desktop");
        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        builder.init(null);
        builder.runApp();
        settle();
        return builder;
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
