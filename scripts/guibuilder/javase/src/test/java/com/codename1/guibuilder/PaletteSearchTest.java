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

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Filters the component palette through its search field, in the application the builder really
 * shows. Typing a single character used to throw "Component is already contained in Container":
 * each change rebuilt the palette heading and re-added the search field to a new container while
 * the discarded one still held it, which left the palette empty and the field detached.
 */
class PaletteSearchTest {
    private static final String SEARCH_ID = "guibuilder.palette.search";
    private static final String ITEM_PREFIX = "guibuilder.palette.";

    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!Display.isInitialized()) Display.init(new JPanel());
    }

    @Test
    void typingInTheSearchFieldFiltersThePaletteAndKeepsTheField() throws Exception {
        workspace();
        Form form = Display.getInstance().getCurrent();
        assertNotNull(form, "the builder showed no form");
        TextField search = (TextField) findById(form, SEARCH_ID);
        assertNotNull(search, "the palette has no search field");
        Container palette = search.getParent().getParent();
        assertEquals(10, paletteItems(palette).size(), "the unfiltered palette lists every type");

        setSearchText(search, "b");
        assertSame(form, search.getComponentForm(),
                "filtering detached the search field from the form, so it cannot be typed into");
        assertSame(search, findById(form, SEARCH_ID), "filtering replaced the search field");
        assertEquals(Arrays.asList("Button", "Label", "SpanLabel", "CheckBox", "RadioButton", "Tabs"), paletteItems(palette),
                "the palette must list the types whose name contains the filter");

        // A second change is where the original failure would have struck again.
        setSearchText(search, "bu");
        assertEquals(Arrays.asList("Button", "RadioButton"), paletteItems(palette));

        setSearchText(search, "");
        assertEquals(10, paletteItems(palette).size(), "clearing the filter must bring every type back");
        assertSame(form, search.getComponentForm());
    }

    // ---- harness ----------------------------------------------------------------------------

    /**
     * Changes the text on the event dispatch thread, where the field's data change listener runs,
     * and fails with whatever that listener threw rather than letting it vanish into the EDT.
     */
    private static void setSearchText(TextField search, String text) {
        final Throwable[] thrown = new Throwable[1];
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                search.setText(text);
            } catch (Throwable t) {
                thrown[0] = t;
            }
        });
        settle();
        if (thrown[0] != null) fail("filtering the palette by \"" + text + "\" threw", thrown[0]);
    }

    private static List<String> paletteItems(Container root) {
        List<String> out = new ArrayList<>();
        collectItems(root, out);
        return out;
    }

    private static void collectItems(Container root, List<String> out) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            String id = component.getSemantics().getIdentifier();
            if (component instanceof Button && id != null && id.startsWith(ITEM_PREFIX)) {
                out.add(((Button) component).getText());
            }
            if (component instanceof Container) collectItems((Container) component, out);
        }
    }

    private static Component findById(Container root, String id) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            if (id.equals(component.getSemantics().getIdentifier())) return component;
            if (component instanceof Container) {
                Component nested = findById((Container) component, id);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static CodenameOneGUIBuilder workspace() throws Exception {
        Path demo = new java.io.File("../demo-project").getCanonicalFile().toPath();
        Path input = Files.createTempFile("guibuilder", ".input");
        Files.write(input, ("projectDir=" + demo + "\nguiDir=" + demo.resolve("src/main/guibuilder")
                + "\nsourceDir=" + demo.resolve("src/main/java")
                + "\ncssFile=" + demo.resolve("src/main/css/theme.css")
                + "\ninitialForm=com.example.NestedLayoutsForm\n").getBytes(StandardCharsets.UTF_8));
        input.toFile().deleteOnExit();
        System.setProperty("guibuilder.input", input.toString());
        System.setProperty("guibuilder.canvasMode", "desktop");
        final CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        // On the EDT, as Codename One runs an application's lifecycle; see LiveTypingTest.
        Display.getInstance().callSeriallyAndWait(() -> {
            builder.init(null);
            builder.runApp();
        });
        settle();
        return builder;
    }

    private static void settle() {
        for (int i = 0; i < 4; i++) {
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Display.getInstance().callSeriallyAndWait(() -> { });
        }
    }
}
