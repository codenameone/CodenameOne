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
import com.codename1.xml.Element;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The demo project is the surface anyone evaluating the editor actually opens, so a form that
 * parses but renders nothing is a broken first impression. Every form is checked for the same
 * things the designer relies on: unique names, a coherent tree, and every component visible.
 */
class DemoFormsTest {
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    @Test
    void everyDemoFormLoadsRendersAndKeepsEveryComponentVisible() throws Exception {
        List<File> forms = demoForms();
        assertTrue(forms.size() >= 12, "expected the demo project to cover more cases: " + forms);
        for (File file : forms) {
            GuiDocument document = GuiDocument.parse(file.getPath(),
                    new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));

            Set<String> names = new LinkedHashSet<>();
            for (Element element : document.components()) {
                String name = element.getAttribute("name");
                assertNotNull(name, file.getName() + " has a component with no name");
                assertTrue(names.add(name), file.getName() + " reuses the name " + name
                        + ", which collides in the generated source");
                if (element == document.root()) continue;
                assertNotNull(document.parentOf(element),
                        file.getName() + ": " + name + " is detached from the tree");
            }

            Container rendered = (Container) render(document);
            for (Element element : document.components()) {
                if (element == document.root()) continue;
                Component preview = findPreview(rendered, element);
                assertNotNull(preview, file.getName() + ": " + element.getAttribute("name")
                        + " does not render at all");
                assertTrue(preview.getWidth() > 0 && preview.getHeight() > 0,
                        file.getName() + ": " + element.getAttribute("name") + " renders at "
                                + preview.getWidth() + "x" + preview.getHeight() + "; it is invisible");
            }
        }
    }

    @Test
    void everyDemoFormSurvivesASaveAndReload() throws Exception {
        for (File file : demoForms()) {
            GuiDocument document = GuiDocument.parse(file.getPath(),
                    new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            GuiDocument reloaded = GuiDocument.parse(file.getPath(), document.toXml());
            assertEquals(structure(document), structure(reloaded),
                    file.getName() + " does not survive a save/load round trip");
            assertEquals(reloaded.toXml(), GuiDocument.parse(file.getPath(), reloaded.toXml()).toXml(),
                    file.getName() + " keeps changing every time it is written");
        }
    }

    private static List<File> demoForms() {
        File directory = new File("../demo-project/src/main/guibuilder/com/example");
        assertTrue(directory.isDirectory(), "demo forms are missing at " + directory.getAbsolutePath());
        List<File> forms = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.getName().endsWith(".gui")) forms.add(file);
        }
        java.util.Collections.sort(forms);
        return forms;
    }

    private static List<String> structure(GuiDocument document) {
        List<String> rows = new ArrayList<>();
        for (Element element : document.components()) {
            Element parent = document.parentOf(element);
            rows.add((parent == null ? "-" : parent.getAttribute("name")) + "/" + element.getAttribute("name"));
        }
        return rows;
    }

    private static Component render(GuiDocument document) {
        Component rendered = ComponentPreviewFactory.create(document.root(), null,
                new ComponentPreviewFactory.SelectionHandler() {
                    public void selected(Element element) { }
                    public void dragPressed(Element element, Component source, int x, int y) { }
                    public boolean isDragActive() { return false; }
                    public void editContent(Element element) { }
                });
        rendered.setWidth(720);
        rendered.setHeight(1200);
        layoutNested((Container) rendered);
        return rendered;
    }

    private static void layoutNested(Container container) {
        container.layoutContainer();
        for (int i = 0; i < container.getComponentCount(); i++) {
            if (container.getComponentAt(i) instanceof Container nested) layoutNested(nested);
        }
    }

    private static Component findPreview(Container root, Element element) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            if (component.getClientProperty("gui.element") == element) return component;
            if (component instanceof Container container) {
                Component nested = findPreview(container, element);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
