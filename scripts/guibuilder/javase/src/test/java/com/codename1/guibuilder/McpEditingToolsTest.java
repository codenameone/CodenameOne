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

import com.codename1.guibuilder.model.ComponentCatalog;
import com.codename1.guibuilder.model.GuiDocument;
import com.codename1.io.JSONParser;
import com.codename1.mcp.MCP;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Tabs;
import com.codename1.xml.Element;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The editing tools, driven the way a client drives them: JSON-RPC through the MCP server into a
 * running builder, then checked against the document, the canvas and the files on disk.
 */
class McpEditingToolsTest {
    private static final String[] EDITING_TOOLS = {
        "guibuilder_component_types", "guibuilder_get_form_xml", "guibuilder_add_component",
        "guibuilder_insert_xml", "guibuilder_set_properties", "guibuilder_rename_component",
        "guibuilder_delete_components", "guibuilder_set_layout", "guibuilder_set_constraint",
        "guibuilder_set_event_handler", "guibuilder_toolbar_commands", "guibuilder_set_binding",
        "guibuilder_create_form"};

    private static int requestId;
    private Path project;

    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!Display.isInitialized()) Display.init(new JPanel());
    }

    @Test
    void everyEditingToolIsPublished() throws Exception {
        builder();
        String tools = MCP.getServer().handleMessage("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
        for (String tool : EDITING_TOOLS) assertTrue(tools.contains("\"" + tool + "\""), tool + " is not listed");
    }

    @Test
    void theCatalogOffersExactlyWhatTheInspectorOffers() throws Exception {
        // The catalog is what an MCP client is validated against; the inspector is what a person
        // sees. A property in one and not the other is either refused for no reason or accepted and
        // ignored, so the two are compared field by field for every type.
        CodenameOneGUIBuilder builder = builder();
        for (String type : ComponentCatalog.PALETTE_TYPES) {
            Map<String, Object> added = call("guibuilder_add_component", "{\"type\":\"" + type + "\"}");
            assertEquals(Boolean.TRUE, added.get("success"), type + ": " + added.get("error"));
            assertEquals(expected(type), inspectorProperties(builder), type);
        }
        assertEquals(expected("Form"), inspectorPropertiesFor(builder, "EditForm"), "Form");
    }

    @Test
    void addingAComponentWithEverythingIsOneUndoStep() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        Map<String, Object> row = call("guibuilder_add_component", "{\"type\":\"Container\",\"name\":\"buttons\","
                + "\"layout\":{\"layout\":\"BoxLayout\",\"axis\":\"X\"}}");
        assertEquals(Boolean.TRUE, row.get("success"), String.valueOf(row.get("error")));
        assertEquals("buttons", row.get("component"));
        Map<String, Object> added = call("guibuilder_add_component", "{\"type\":\"Button\",\"parent\":\"buttons\","
                + "\"name\":\"signIn\",\"properties\":{\"text\":\"Sign in\",\"uiid\":\"PrimaryButton\",\"enabled\":false}}");
        assertEquals(Boolean.TRUE, added.get("success"), String.valueOf(added.get("error")));
        assertEquals("signIn", added.get("component"));
        assertNotNull(added.get("state"), "a mutating tool answers with fresh state");

        Element button = element(builder, "signIn");
        assertEquals("buttons", builder.documentForTest().parentOf(button).getAttribute("name"));
        assertEquals("Sign in", button.getAttribute("text"));
        assertEquals("PrimaryButton", button.getAttribute("uiid"));
        assertEquals("false", button.getAttribute("enabled"));
        assertEquals("X", element(builder, "buttons").getAttribute("boxLayoutAxis"));
        assertNotNull(preview(builder, button), "the new button is not on the canvas");

        Map<String, Object> undone = call("guibuilder_command", "{\"command\":\"undo\"}");
        assertEquals(Boolean.TRUE, undone.get("success"));
        assertNull(element(builder, "signIn"), "one undo must remove the whole add, properties included");
        assertNotNull(element(builder, "buttons"), "and nothing before it");
    }

    @Test
    void aRefusedEditChangesNothingAndRecordsNoUndoStep() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        call("guibuilder_add_component", "{\"type\":\"TextField\",\"name\":\"email\"}");
        String before = builder.documentForTest().toXml();
        int undoDepth = undoDepth(builder);

        Map<String, Object> columns = call("guibuilder_set_properties",
                "{\"component\":\"email\",\"properties\":{\"hint\":\"you@example.com\",\"columns\":0}}");
        assertEquals(Boolean.FALSE, columns.get("success"));
        assertTrue(String.valueOf(columns.get("error")).contains("columns"), String.valueOf(columns.get("error")));
        assertEquals(before, builder.documentForTest().toXml(), "the hint set before the bad value must be rolled back");
        assertEquals(undoDepth, undoDepth(builder), "a refused edit must not leave an undo step");

        Map<String, Object> unknown = call("guibuilder_set_properties",
                "{\"component\":\"email\",\"properties\":{\"layout\":\"BoxLayout\"}}");
        assertTrue(String.valueOf(unknown.get("error")).contains("guibuilder_set_layout"),
                "a layout attribute must point at the tool that validates it");
        Map<String, Object> choice = call("guibuilder_set_properties",
                "{\"component\":\"email\",\"properties\":{\"constraint\":\"email\"}}");
        assertEquals(Boolean.FALSE, choice.get("success"), "constraint takes EMAILADDR, not a guess");
    }

    @Test
    void nullResetsAPropertyToItsDefault() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"caption\",\"properties\":{\"uiid\":\"Big\"}}");
        assertEquals("Big", element(builder, "caption").getAttribute("uiid"));
        Map<String, Object> reset = call("guibuilder_set_properties", "{\"component\":\"caption\",\"properties\":{\"uiid\":null}}");
        assertEquals(Boolean.TRUE, reset.get("success"), String.valueOf(reset.get("error")));
        assertNull(element(builder, "caption").getAttribute("uiid"));
    }

    @Test
    void insertedXmlGetsUniqueNamesAndKeepsItsInternalRelationships() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        String fragment = "<component type=\"Container\" name=\"card\" layout=\"LayeredLayout\">"
                + "<component type=\"Label\" name=\"title\" text=\"Title\"/>"
                + "<component type=\"Label\" name=\"subtitle\" text=\"Sub\" guidedReferenceTarget=\"title\"/>"
                + "</component>";
        Map<String, Object> first = call("guibuilder_insert_xml", "{\"xml\":" + quote(fragment) + "}");
        assertEquals(Boolean.TRUE, first.get("success"), String.valueOf(first.get("error")));
        assertEquals(java.util.Arrays.asList("card", "title", "subtitle"), first.get("components"));
        Map<String, Object> second = call("guibuilder_insert_xml", "{\"xml\":" + quote(fragment) + "}");
        assertEquals(Boolean.TRUE, second.get("success"), String.valueOf(second.get("error")));
        List<?> renamed = (List<?>) second.get("components");
        assertFalse(renamed.contains("card"), "a second copy must not reuse taken names: " + renamed);
        Element copy = element(builder, String.valueOf(renamed.get(2)));
        assertEquals(renamed.get(1), copy.getAttribute("guidedReferenceTarget"),
                "a reference inside the fragment must follow its renamed target");

        Map<String, Object> bad = call("guibuilder_insert_xml",
                "{\"xml\":" + quote("<component type=\"WebBrowser\" name=\"web\"/>") + "}");
        assertEquals(Boolean.FALSE, bad.get("success"), "a type the canvas cannot draw must be refused");
        Map<String, Object> screen = call("guibuilder_insert_xml",
                "{\"xml\":" + quote("<component type=\"Form\" name=\"nested\"/>") + "}");
        assertEquals(Boolean.FALSE, screen.get("success"), "a form cannot be nested in a form");
    }

    @Test
    void aBorderRegionTakenByAnotherChildSwapsPlaces() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        call("guibuilder_add_component", "{\"type\":\"Container\",\"name\":\"frame\",\"layout\":{\"layout\":\"BorderLayout\"}}");
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"top\",\"parent\":\"frame\","
                + "\"constraint\":{\"region\":\"North\"}}");
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"middle\",\"parent\":\"frame\","
                + "\"constraint\":{\"region\":\"Center\"}}");
        Map<String, Object> swap = call("guibuilder_set_constraint", "{\"component\":\"middle\",\"region\":\"North\"}");
        assertEquals(Boolean.TRUE, swap.get("success"), String.valueOf(swap.get("error")));
        Element frame = element(builder, "frame");
        assertEquals("North", GuiDocument.effectiveBorderConstraint(frame, element(builder, "middle")));
        assertEquals("Center", GuiDocument.effectiveBorderConstraint(frame, element(builder, "top")));

        Map<String, Object> wrongLayout = call("guibuilder_set_constraint", "{\"component\":\"frame\",\"row\":1}");
        assertEquals(Boolean.FALSE, wrongLayout.get("success"), "a table cell means nothing in a BoxLayout parent");
    }

    @Test
    void insertedXmlCannotTakeABorderRegionThatIsAlreadyUsed() throws Exception {
        // effectiveBorderConstraint() moves a duplicate region to the first free one, so a clash has
        // to be caught on the fragment's own constraint, before it lands.
        CodenameOneGUIBuilder builder = builder();
        call("guibuilder_add_component", "{\"type\":\"Container\",\"name\":\"frame\",\"layout\":{\"layout\":\"BorderLayout\"}}");
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"body\",\"parent\":\"frame\","
                + "\"constraint\":{\"region\":\"Center\"}}");
        String before = builder.documentForTest().toXml();
        Map<String, Object> clash = call("guibuilder_insert_xml", "{\"parent\":\"frame\",\"xml\":"
                + quote("<component type=\"Label\" name=\"other\" layoutConstraint=\"center\"/>") + "}");
        assertEquals(Boolean.FALSE, clash.get("success"), "two Center children must be refused, not shuffled");
        assertTrue(String.valueOf(clash.get("error")).contains("body"), String.valueOf(clash.get("error")));
        assertEquals(before, builder.documentForTest().toXml());
        Map<String, Object> reorder = call("guibuilder_insert_xml", "{\"parent\":\"frame\",\"index\":0,\"xml\":"
                + quote("<component type=\"Label\" name=\"top\"/>") + "}");
        assertEquals(Boolean.FALSE, reorder.get("success"), "an index would push body out of Center");
        assertEquals("Center", GuiDocument.effectiveBorderConstraint(element(builder, "frame"), element(builder, "body")));

        Map<String, Object> free = call("guibuilder_insert_xml", "{\"parent\":\"frame\",\"xml\":"
                + quote("<component type=\"Label\" name=\"top\"/>") + "}");
        assertEquals(Boolean.TRUE, free.get("success"), String.valueOf(free.get("error")));
        assertEquals("North", element(builder, "top").getAttribute("layoutConstraint"),
                "a fragment without a region takes the first free one, written down so it stays there");
        assertEquals("Center", GuiDocument.effectiveBorderConstraint(element(builder, "frame"), element(builder, "body")));
    }

    @Test
    void readingDoesNotDisturbThePersonAtTheKeyboard() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        call("guibuilder_toolbar_commands", "{\"action\":\"add\",\"label\":\"Share\"}");
        int undoDepth = undoDepth(builder);
        Field status = CodenameOneGUIBuilder.class.getDeclaredField("status");
        status.setAccessible(true);
        String statusBefore = ((com.codename1.ui.Label) status.get(builder)).getText();
        Field rebuilds = CodenameOneGUIBuilder.class.getDeclaredField("canvasRebuilds");
        rebuilds.setAccessible(true);
        int rebuildsBefore = rebuilds.getInt(builder);

        Map<String, Object> listed = call("guibuilder_toolbar_commands", "{\"action\":\"list\"}");
        assertEquals(1, ((List<?>) listed.get("commands")).size());
        assertEquals(undoDepth, undoDepth(builder));
        assertEquals(statusBefore, ((com.codename1.ui.Label) status.get(builder)).getText(), "listing modifies nothing");
        assertEquals(rebuildsBefore, rebuilds.getInt(builder), "listing must not rebuild the canvas");

        // While the developer is typing in place, an edit waits rather than committing their text.
        Field inline = CodenameOneGUIBuilder.class.getDeclaredField("inlineEditor");
        inline.setAccessible(true);
        com.codename1.ui.TextField typing = new com.codename1.ui.TextField("half typ");
        inline.set(builder, typing);
        try {
            Map<String, Object> refused = call("guibuilder_add_component", "{\"type\":\"Label\"}");
            assertEquals(Boolean.FALSE, refused.get("success"));
            assertTrue(String.valueOf(refused.get("error")).contains("editing"), String.valueOf(refused.get("error")));
            assertSame(typing, inline.get(builder), "the developer's editor must be left open");
            assertEquals(1, ((List<?>) call("guibuilder_toolbar_commands", "{}").get("commands")).size(),
                    "reads still work while the developer types");
        } finally {
            inline.set(builder, null);
        }
        int rebuildsBeforeRefusal = rebuilds.getInt(builder);
        Map<String, Object> refusedEdit = call("guibuilder_set_properties", "{\"component\":\"nobody\",\"properties\":{\"text\":\"x\"}}");
        assertEquals(Boolean.FALSE, refusedEdit.get("success"));
        assertEquals(rebuildsBeforeRefusal, rebuilds.getInt(builder), "a refused edit changes nothing, so nothing is redrawn");
    }

    @Test
    void tableCellsCannotOverlap() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        call("guibuilder_add_component", "{\"type\":\"Container\",\"name\":\"grid\","
                + "\"layout\":{\"layout\":\"TableLayout\",\"rows\":2,\"columns\":2}}");
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"a\",\"parent\":\"grid\"}");
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"b\",\"parent\":\"grid\"}");
        Map<String, Object> clash = call("guibuilder_set_constraint", "{\"component\":\"b\",\"row\":0,\"column\":0}");
        assertEquals(Boolean.FALSE, clash.get("success"), "two children in one cell make TableLayout throw");
        Map<String, Object> moved = call("guibuilder_set_constraint", "{\"component\":\"b\",\"row\":1,\"column\":0,"
                + "\"columnSpan\":2}");
        assertEquals(Boolean.TRUE, moved.get("success"), String.valueOf(moved.get("error")));
        assertEquals("1", element(builder, "b").getAttribute("tableRow"));
        assertEquals("2", element(builder, "b").getAttribute("tableHorizontalSpan"));
        Map<String, Object> shrink = call("guibuilder_set_layout", "{\"container\":\"grid\",\"rows\":1}");
        assertEquals(Boolean.FALSE, shrink.get("success"), "the table cannot shrink below a placed child");
    }

    @Test
    void guidedAlignmentUsesTheNamedReference() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        call("guibuilder_add_component", "{\"type\":\"Container\",\"name\":\"free\",\"layout\":{\"layout\":\"LayeredLayout\"}}");
        call("guibuilder_add_component", "{\"type\":\"Button\",\"name\":\"anchor\",\"parent\":\"free\","
                + "\"constraint\":{\"insets\":\"20px auto auto 30px\"}}");
        Map<String, Object> aligned = call("guibuilder_add_component", "{\"type\":\"Button\",\"name\":\"follower\","
                + "\"parent\":\"free\",\"constraint\":{\"insets\":\"80px auto auto 90px\",\"reference\":\"anchor\","
                + "\"align\":[\"alignLeft\"]}}");
        assertEquals(Boolean.TRUE, aligned.get("success"), String.valueOf(aligned.get("error")));
        Element follower = element(builder, "follower");
        assertEquals("anchor", follower.getAttribute("guidedReferenceTarget"));
        assertTrue(String.valueOf(follower.getAttribute("guidedReferences")).contains("anchor"),
                "alignLeft must anchor the left edge to the reference: " + follower.getAttribute("guidedReferences"));
        Map<String, Object> badInsets = call("guibuilder_set_constraint", "{\"component\":\"follower\",\"insets\":\"10 auto\"}");
        assertEquals(Boolean.FALSE, badInsets.get("success"));
    }

    @Test
    void renameRepointsReferencesAndDeleteTakesChildrenWithIt() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        call("guibuilder_add_component", "{\"type\":\"Container\",\"name\":\"free\",\"layout\":{\"layout\":\"LayeredLayout\"}}");
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"anchor\",\"parent\":\"free\"}");
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"follower\",\"parent\":\"free\","
                + "\"constraint\":{\"reference\":\"anchor\"}}");
        Map<String, Object> renamed = call("guibuilder_rename_component", "{\"component\":\"anchor\",\"name\":\"heading\"}");
        assertEquals("heading", renamed.get("component"));
        assertEquals("heading", element(builder, "follower").getAttribute("guidedReferenceTarget"));
        Map<String, Object> taken = call("guibuilder_rename_component", "{\"component\":\"follower\",\"name\":\"heading\"}");
        assertNotEquals("heading", taken.get("component"), "a taken name is made unique, not duplicated");

        Map<String, Object> deleted = call("guibuilder_delete_components", "{\"components\":[\"free\",\"heading\"]}");
        assertEquals(Boolean.TRUE, deleted.get("success"), String.valueOf(deleted.get("error")));
        assertEquals(java.util.Collections.singletonList("free"), deleted.get("deleted"),
                "a child of a deleted container goes with it rather than failing the call");
        assertNull(element(builder, "heading"));
        Map<String, Object> root = call("guibuilder_delete_components", "{\"components\":[\"EditForm\"]}");
        assertEquals(Boolean.FALSE, root.get("success"));
    }

    @Test
    void anEventHandlerBecomesAStubInTheCompanionOnSave() throws Exception {
        builder();
        call("guibuilder_add_component", "{\"type\":\"Button\",\"name\":\"signIn\"}");
        Map<String, Object> handler = call("guibuilder_set_event_handler", "{\"component\":\"signIn\",\"handler\":\"onSignIn\"}");
        assertEquals(Boolean.TRUE, handler.get("success"), String.valueOf(handler.get("error")));
        assertEquals(Boolean.FALSE, handler.get("methodExists"));
        Map<String, Object> label = call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"caption\"}");
        assertEquals(Boolean.TRUE, label.get("success"));
        assertEquals(Boolean.FALSE, call("guibuilder_set_event_handler",
                "{\"component\":\"caption\",\"handler\":\"onTap\"}").get("success"), "a Label fires no action events");
        assertEquals(Boolean.FALSE, call("guibuilder_set_event_handler",
                "{\"component\":\"signIn\",\"handler\":\"on-click\"}").get("success"), "not a Java method name");

        assertEquals(Boolean.TRUE, call("guibuilder_command", "{\"command\":\"save\"}").get("success"));
        String source = new String(Files.readAllBytes(project.resolve("src/main/java/com/example/EditForm.java")),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("void onSignIn(ActionEvent"), "save must add the handler stub:\n" + source);
        Map<String, Object> again = call("guibuilder_set_event_handler", "{\"component\":\"signIn\",\"handler\":\"onSignIn\"}");
        assertEquals(Boolean.TRUE, again.get("methodExists"));

        Map<String, Object> removed = call("guibuilder_set_event_handler", "{\"component\":\"signIn\",\"handler\":null}");
        assertEquals(Boolean.TRUE, removed.get("success"), String.valueOf(removed.get("error")));
    }

    @Test
    void toolbarCommandsAndBinding() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        Map<String, Object> added = call("guibuilder_toolbar_commands",
                "{\"action\":\"add\",\"label\":\"Share\",\"placement\":\"right\",\"handler\":\"onShare\"}");
        assertEquals(Boolean.TRUE, added.get("success"), String.valueOf(added.get("error")));
        List<?> commands = (List<?>) added.get("commands");
        assertEquals(1, commands.size());
        assertEquals("Share", ((Map<?, ?>) commands.get(0)).get("label"));
        assertEquals(Boolean.FALSE, call("guibuilder_toolbar_commands",
                "{\"action\":\"update\",\"index\":0,\"placement\":\"middle\"}").get("success"));
        assertEquals(Boolean.TRUE, call("guibuilder_toolbar_commands", "{\"action\":\"remove\",\"index\":0}").get("success"));
        assertTrue(builder.documentForTest().commands().isEmpty());

        Map<String, Object> binding = call("guibuilder_set_binding", "{\"strategy\":\"bindable\"}");
        assertEquals(Boolean.TRUE, binding.get("success"), String.valueOf(binding.get("error")));
        assertEquals("bindable", builder.documentForTest().root().getAttribute("bindingStrategy"));
        assertEquals(Boolean.FALSE, call("guibuilder_set_binding", "{\"strategy\":\"magic\"}").get("success"));
    }

    @Test
    void createFormWritesTheFormAndACompilableCompanionAndOpensIt() throws Exception {
        CodenameOneGUIBuilder builder = builder();
        Map<String, Object> created = call("guibuilder_create_form", "{\"className\":\"com.example.LoginForm\"}");
        assertEquals(Boolean.TRUE, created.get("success"), String.valueOf(created.get("error")));
        assertTrue(Files.exists(project.resolve("src/main/guibuilder/com/example/LoginForm.gui")));
        String source = new String(Files.readAllBytes(project.resolve("src/main/java/com/example/LoginForm.java")),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("class LoginForm extends Form"), source);
        assertTrue(builder.documentForTest().path().endsWith("LoginForm.gui"), "the new form must be the active one");
        assertEquals("none", builder.documentForTest().root().getAttribute("bindingStrategy"),
                "a new form must not need a model class to compile");

        assertEquals(Boolean.FALSE, call("guibuilder_create_form", "{\"className\":\"com.example.LoginForm\"}").get("success"),
                "an existing form is never overwritten");
        assertEquals(Boolean.FALSE, call("guibuilder_create_form", "{\"className\":\"LoginForm\"}").get("success"),
                "a form needs a package");
        Map<String, Object> panel = call("guibuilder_create_form", "{\"className\":\"com.example.Panel\",\"type\":\"Container\"}");
        assertEquals(Boolean.TRUE, panel.get("success"), String.valueOf(panel.get("error")));
        assertEquals("none", builder.documentForTest().root().getAttribute("bindingStrategy"),
                "a Container root must not get a binding model either; set_binding cannot turn it off there");
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/PanelModel.java")));
        call("guibuilder_add_component", "{\"type\":\"Label\"}");
        Map<String, Object> dirty = call("guibuilder_create_form", "{\"className\":\"com.example.OtherForm\"}");
        assertEquals(Boolean.FALSE, dirty.get("success"), "unsaved edits must not be discarded by opening another form");
    }

    @Test
    void theFirstFormOfAProjectCanBeCreatedAndEdited() throws Exception {
        // A new project has no forms, so the builder starts on its empty-project screen; an agent
        // must be able to start from there.
        CodenameOneGUIBuilder builder = builder(false);
        assertNull(builder.documentForTest());
        assertEquals(Boolean.FALSE, call("guibuilder_add_component", "{\"type\":\"Label\"}").get("success"),
                "there is nothing to add to yet");
        Map<String, Object> created = call("guibuilder_create_form", "{\"className\":\"com.example.FirstForm\","
                + "\"type\":\"Dialog\"}");
        assertEquals(Boolean.TRUE, created.get("success"), String.valueOf(created.get("error")));
        assertEquals("new FirstForm().show()", created.get("usage"));
        Map<String, Object> added = call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"hello\"}");
        assertEquals(Boolean.TRUE, added.get("success"), String.valueOf(added.get("error")));
        assertNotNull(preview(builder, element(builder, "hello")));
    }

    @Test
    void getFormXmlIncludesUnsavedEdits() throws Exception {
        builder();
        call("guibuilder_add_component", "{\"type\":\"Label\",\"name\":\"fresh\"}");
        Map<String, Object> xml = call("guibuilder_get_form_xml", "{}");
        assertEquals(Boolean.TRUE, xml.get("modified"));
        assertTrue(String.valueOf(xml.get("xml")).contains("fresh"));
        Map<String, Object> one = call("guibuilder_get_form_xml", "{\"component\":\"fresh\"}");
        assertTrue(String.valueOf(one.get("xml")).startsWith("<component"), String.valueOf(one.get("xml")));
    }

    // --- helpers -------------------------------------------------------------------------------

    private static TreeSet<String> expected(String type) {
        TreeSet<String> names = new TreeSet<String>();
        names.add("name");
        for (ComponentCatalog.Property property : ComponentCatalog.propertiesFor(type)) names.add(property.name());
        return names;
    }

    private static TreeSet<String> inspectorPropertiesFor(CodenameOneGUIBuilder builder, String component) throws Exception {
        Map<String, Object> selected = call("guibuilder_select", "{\"component\":\"" + component + "\"}");
        assertEquals(Boolean.TRUE, selected.get("success"), String.valueOf(selected.get("error")));
        return inspectorProperties(builder);
    }

    /** The guibuilder.property.* fields on the inspector's Properties tab for the selection. */
    private static TreeSet<String> inspectorProperties(CodenameOneGUIBuilder builder) throws Exception {
        final TreeSet<String> names = new TreeSet<String>();
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                Method refresh = CodenameOneGUIBuilder.class.getDeclaredMethod("refreshInspector");
                refresh.setAccessible(true);
                refresh.invoke(builder);
                Field host = CodenameOneGUIBuilder.class.getDeclaredField("inspectorHost");
                host.setAccessible(true);
                Tabs tabs = findTabs((Container) host.get(builder));
                collectProperties(tabs.getTabComponentAt(0), names);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        return names;
    }

    private static Tabs findTabs(Container root) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component child = root.getComponentAt(i);
            if (child instanceof Tabs) return (Tabs) child;
            if (child instanceof Container) {
                Tabs nested = findTabs((Container) child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static void collectProperties(Component component, TreeSet<String> names) {
        String identifier = component.getSemantics().getIdentifier();
        if (identifier != null && identifier.startsWith("guibuilder.property.")) {
            names.add(identifier.substring("guibuilder.property.".length()));
        }
        if (component instanceof Container) {
            Container container = (Container) component;
            for (int i = 0; i < container.getComponentCount(); i++) collectProperties(container.getComponentAt(i), names);
        }
    }

    private CodenameOneGUIBuilder builder() throws Exception {
        return builder(true);
    }

    private CodenameOneGUIBuilder builder(boolean withForm) throws Exception {
        project = Files.createTempDirectory("guibuilder-mcp-edit");
        Path gui = project.resolve("src/main/guibuilder/com/example");
        Files.createDirectories(gui);
        if (withForm) Files.write(gui.resolve("EditForm.gui"), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<component type=\"Form\" layout=\"BoxLayout\" boxLayoutAxis=\"Y\" name=\"EditForm\""
                + " title=\"Edit\" bindingStrategy=\"none\">\n"
                + "</component>\n").getBytes(StandardCharsets.UTF_8));
        Path css = project.resolve("src/main/css/theme.css");
        Files.createDirectories(css.getParent());
        Files.write(css, "Label { color: #000000; }\n".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(project.resolve("src/main/java"));
        Path input = Files.createTempFile("guibuilder", ".input");
        Files.write(input, ("projectDir=" + project + "\nguiDir=" + project.resolve("src/main/guibuilder") + "\n"
                + "sourceDir=" + project.resolve("src/main/java") + "\ncssFile=" + css + "\n"
                + "initialForm=com.example.EditForm\n").getBytes(StandardCharsets.UTF_8));
        input.toFile().deleteOnExit();
        System.setProperty("guibuilder.input", input.toString());
        System.setProperty("guibuilder.canvasMode", "desktop");
        final CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        // On the EDT, as Codename One runs an application's lifecycle; see LiveWorkspaceDragTest.
        Display.getInstance().callSeriallyAndWait(() -> {
            builder.init(null);
            builder.runApp();
        });
        settle();
        return builder;
    }

    /** Calls a tool through the MCP server exactly as a client would and returns its JSON result. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> call(String tool, String argumentsJson) throws Exception {
        String response = MCP.getServer().handleMessage("{\"jsonrpc\":\"2.0\",\"id\":" + (++requestId)
                + ",\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool + "\",\"arguments\":" + argumentsJson + "}}");
        Map<String, Object> envelope = new JSONParser().parseJSON(new StringReader(response));
        Map<String, Object> result = (Map<String, Object>) envelope.get("result");
        assertNotNull(result, tool + " failed at the protocol level: " + response);
        List<Object> content = (List<Object>) result.get("content");
        String text = String.valueOf(((Map<String, Object>) content.get(0)).get("text"));
        JSONParser parser = new JSONParser();
        parser.setUseBooleanInstance(true);
        parser.setIncludeNullsInstance(true);
        Map<String, Object> parsed = parser.parseJSON(new StringReader(text));
        settle();
        return parsed;
    }

    private static String quote(String text) {
        return JSONParser.toJson(java.util.Collections.singletonList(text)).replaceFirst("^\\[", "").replaceFirst("\\]$", "");
    }

    private static Element element(CodenameOneGUIBuilder builder, String name) {
        for (Element candidate : builder.documentForTest().components()) {
            if (name.equals(candidate.getAttribute("name"))) return candidate;
        }
        return null;
    }

    private static Component preview(CodenameOneGUIBuilder builder, Element element) {
        List<Component> found = new ArrayList<Component>();
        findPreview(builder.canvasHostForTest(), element, found);
        return found.isEmpty() ? null : found.get(0);
    }

    private static void findPreview(Container root, Element element, List<Component> found) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component child = root.getComponentAt(i);
            if (child.getClientProperty("gui.element") == element) found.add(child);
            if (child instanceof Container) findPreview((Container) child, element, found);
        }
    }

    private static int undoDepth(CodenameOneGUIBuilder builder) throws Exception {
        Field undo = GuiDocument.class.getDeclaredField("undo");
        undo.setAccessible(true);
        return ((List<?>) undo.get(builder.documentForTest())).size();
    }

    private static void settle() {
        for (int i = 0; i < 2; i++) {
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Display.getInstance().callSeriallyAndWait(() -> { });
        }
    }
}
