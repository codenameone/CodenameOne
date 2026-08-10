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

import com.codename1.xml.Element;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiDocumentTest {
    @Test
    void undoRestoresTheSavedStateAndRedoRestoresTheModifiedState() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\"><component type=\"Button\" name=\"action\" text=\"Before\"/></component>");
        document.select(document.components().get(1));
        document.setAttribute("text", "After");
        assertTrue(document.isModified());

        assertTrue(document.undo());
        assertFalse(document.isModified(), "undoing the first edit must return to the saved state");
        assertEquals("Before", document.components().get(1).getAttribute("text"));

        assertTrue(document.redo());
        assertTrue(document.isModified());
        assertEquals("After", document.components().get(1).getAttribute("text"));
    }

    @Test
    void preservesExistingXmlAndSupportsEditing() {
        GuiDocument document = GuiDocument.parse("Login.gui",
                "<component type=\"Form\" name=\"Login\" layout=\"BoxLayout\">"
                + "<component type=\"Label\" name=\"title\" text=\"Welcome\" custom=\"keep-me\"/>"
                + "</component>");

        assertEquals(2, document.components().size());
        document.select(document.components().get(1));
        document.setAttribute("uiid", "LoginTitle");
        document.addComponent("Button");

        String saved = document.toXml();
        assertTrue(saved.contains("custom=\"keep-me\""));
        assertTrue(saved.contains("uiid=\"LoginTitle\""));
        assertTrue(saved.contains("type=\"Button\""));
        assertTrue(document.isModified());
    }

    @Test
    void createsUniqueComponentNames() {
        GuiDocument document = GuiDocument.parse("Form.gui", "<component type=\"Form\" name=\"Form\"/>");
        assertEquals("button1", document.addComponent("Button").getAttribute("name"));
        document.select(document.root());
        assertEquals("button2", document.addComponent("Button").getAttribute("name"));
    }

    @Test
    void newContainersUseGuidedLayoutByDefault() {
        GuiDocument document = GuiDocument.parse("Form.gui", "<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\"/>");
        Element container = document.addComponent("Container");
        assertEquals("LayeredLayout", container.getAttribute("layout"));
    }

    @Test
    void doesNotDeleteRoot() {
        GuiDocument document = GuiDocument.parse("Form.gui", "<component type=\"Form\" name=\"Form\"/>");
        assertFalse(document.deleteSelected());
    }

    @Test
    void copiesAndPastesWithAUniqueName() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\">"
                + "<component type=\"Button\" name=\"button1\" text=\"Save\"/>"
                + "</component>");

        document.select(document.components().get(1));
        String clipboard = document.copySelectedXml();
        document.select(document.root());
        Element pasted = document.pasteXml(clipboard);

        assertNotNull(pasted);
        assertEquals("button2", pasted.getAttribute("name"));
        assertEquals("Save", pasted.getAttribute("text"));
        assertEquals(3, document.components().size());
    }

    /**
     * Pasted XML carries the cell it was copied from, so pasting a table child back into its own
     * table used to stack the copy on top of the original in the preview and the generated source.
     */
    @Test
    void aPastedTableChildTakesAFreeCellInsteadOfTheOneItWasCopiedFrom() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\">"
                + "<component type=\"Container\" name=\"grid\" layout=\"TableLayout\""
                + " tableLayoutRows=\"2\" tableLayoutColumns=\"2\">"
                + "<component type=\"Button\" name=\"cell\" tableRow=\"0\" tableColumn=\"0\"/>"
                + "</component></component>");

        Element original = named(document, "cell");
        document.select(original);
        String clipboard = document.copySelectedXml();
        document.select(named(document, "grid"));
        Element pasted = document.pasteXml(clipboard);

        assertNotNull(pasted);
        assertNotEquals(
                original.getAttribute("tableRow") + ":" + original.getAttribute("tableColumn"),
                pasted.getAttribute("tableRow") + ":" + pasted.getAttribute("tableColumn"),
                "the copy landed in the cell it was copied from");
    }

    @Test
    void pastedContainerChildrenAlsoGetUniqueNamesAndKeepInternalRelationships() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Container\" name=\"card\" layout=\"LayeredLayout\">"
                + "<component type=\"Label\" name=\"caption\"/>"
                + "<component type=\"Button\" name=\"action\" guidedReferences=\"caption|-|-|caption\""
                + " guidedMatchWidth=\"caption\" guidedHorizontalSize=\"match\"/>"
                + "</component></component>");

        document.select(document.components().get(1));
        String clipboard = document.copySelectedXml();
        document.select(document.root());
        Element pasted = document.pasteXml(clipboard);

        assertNotNull(pasted);
        assertEquals("card1", pasted.getAttribute("name"));
        Element pastedAction = named(document, "action1");
        assertNotNull(named(document, "caption1"));
        assertNotNull(pastedAction);
        assertEquals("caption1|-|-|caption1", pastedAction.getAttribute("guidedReferences"),
                "a pasted relationship must follow the pasted copy, not the original");
        assertEquals("caption1", pastedAction.getAttribute("guidedMatchWidth"));
        assertEquals("caption|-|-|caption", named(document, "action").getAttribute("guidedReferences"),
                "the original relationship must be untouched");
    }

    @Test
    void renamingAComponentKeepsNamesUniqueAndRepointsEveryRelationship() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Label\" name=\"caption\"/>"
                + "<component type=\"Button\" name=\"action\" guidedReferences=\"caption|-|-|caption\""
                + " guidedMatchWidth=\"caption\" guidedReferenceTarget=\"caption\"/>"
                + "</component>");
        Element caption = document.components().get(1);
        Element action = document.components().get(2);

        document.select(caption);
        assertEquals("title", document.renameSelected("title"));
        assertEquals("title|-|-|title", action.getAttribute("guidedReferences"));
        assertEquals("title", action.getAttribute("guidedMatchWidth"));
        assertEquals("title", action.getAttribute("guidedReferenceTarget"));

        document.select(action);
        assertEquals("title1", document.renameSelected("title"),
                "a duplicate name would break name-based references and generated fields");

        assertTrue(document.undo());
        assertEquals("action", document.components().get(2).getAttribute("name"));
        assertTrue(document.undo());
        assertEquals("caption", document.components().get(1).getAttribute("name"));
        assertEquals("caption|-|-|caption", document.components().get(2).getAttribute("guidedReferences"),
                "a rename and its reference updates must undo together");
    }

    @Test
    void deletingAReferencedComponentLeavesNoDanglingRelationship() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Label\" name=\"caption\"/>"
                + "<component type=\"Button\" name=\"action\" guidedReferences=\"caption|-|-|caption\""
                + " guidedMatchWidth=\"caption\" guidedHorizontalSize=\"match\""
                + " guidedReferenceTarget=\"caption\"/>"
                + "</component>");

        document.select(document.components().get(1));
        assertTrue(document.deleteSelected());
        Element action = document.components().get(1);
        assertEquals("-|-|-|-", action.getAttribute("guidedReferences"));
        assertNull(action.getAttribute("guidedMatchWidth"));
        assertNull(action.getAttribute("guidedReferenceTarget"));
        assertEquals("preferred", action.getAttribute("guidedHorizontalSize"),
                "a match policy without a target must fall back to the component's own size");

        assertTrue(document.undo());
        assertEquals("caption|-|-|caption", document.components().get(2).getAttribute("guidedReferences"));
    }

    @Test
    void reordersSiblingsAndMovesComponentsIntoContainers() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\">"
                + "<component type=\"Label\" name=\"first\"/>"
                + "<component type=\"Button\" name=\"second\"/>"
                + "<component type=\"Container\" name=\"group\"/>"
                + "</component>");
        Element first = document.components().get(1);
        Element second = document.components().get(2);
        Element group = document.components().get(3);

        document.select(second);
        assertTrue(document.moveSelectedTo(first));
        assertSame(second, document.root().getChildAt(0));

        document.select(first);
        assertTrue(document.moveSelectedTo(group));
        assertSame(first, group.getChildAt(0));
        assertEquals(2, document.root().getNumChildren());
    }

    @Test
    void dropsSiblingsBeforeOrAfterTheIndicatedComponent() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\">"
                + "<component type=\"Label\" name=\"first\"/>"
                + "<component type=\"Label\" name=\"second\"/>"
                + "<component type=\"Label\" name=\"third\"/>"
                + "</component>");
        Element first = document.components().get(1);
        Element second = document.components().get(2);
        Element third = document.components().get(3);

        document.select(first);
        assertTrue(document.moveSelectedTo(second, true));
        assertSame(second, document.root().getChildAt(0));
        assertSame(first, document.root().getChildAt(1));
        assertSame(third, document.root().getChildAt(2));

        document.select(third);
        assertTrue(document.moveSelectedTo(second, false));
        assertSame(third, document.root().getChildAt(0));
        assertSame(second, document.root().getChildAt(1));
        assertSame(first, document.root().getChildAt(2));
    }

    @Test
    void movesComponentsToAnExactParentSlotForCrossContainerSwaps() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\"><component type=\"Container\" name=\"left\">"
                + "<component type=\"Label\" name=\"a\"/><component type=\"Label\" name=\"b\"/>"
                + "</component><component type=\"Container\" name=\"right\">"
                + "<component type=\"Button\" name=\"c\"/></component></component>");
        Element left = document.components().get(1);
        Element a = document.components().get(2);
        Element b = document.components().get(3);
        Element right = document.components().get(4);
        Element c = document.components().get(5);

        document.select(c);
        assertTrue(document.moveSelectedToParent(left, 1));
        assertSame(a, left.getChildAt(0));
        assertSame(c, left.getChildAt(1));
        assertSame(b, left.getChildAt(2));
        assertEquals(1, document.componentIndex(left, c));
        assertEquals(0, right.getNumChildren());
    }

    @Test
    void preventsMovingAContainerIntoItsOwnDescendant() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\">"
                + "<component type=\"Container\" name=\"outer\">"
                + "<component type=\"Container\" name=\"inner\"/>"
                + "</component></component>");
        Element outer = document.components().get(1);
        Element inner = document.components().get(2);

        document.select(outer);
        assertFalse(document.moveSelectedTo(inner));
    }

    @Test
    void exposesDefaultUiidsAndTypeAwareParentLayout() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Button\" name=\"save\"/>"
                + "</component>");
        Element button = document.components().get(1);

        assertEquals("Button", document.effectiveUiid(button));
        assertEquals("LayeredLayout", document.parentLayout(button));
        button.setAttribute("uiid", "PrimaryAction");
        assertEquals("PrimaryAction", document.effectiveUiid(button));
    }

    @Test
    void normalizesMissingAndDuplicateBorderLayoutConstraints() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" name=\"Form\" layout=\"BorderLayout\">"
                + "<component type=\"Label\" name=\"first\"/>"
                + "<component type=\"Label\" name=\"second\" layoutConstraint=\"center\"/>"
                + "<component type=\"Label\" name=\"third\" layoutConstraint=\"WEST\"/>"
                + "</component>");
        Element first = document.components().get(1);
        Element second = document.components().get(2);
        Element third = document.components().get(3);

        assertEquals("Center", GuiDocument.effectiveBorderConstraint(document.root(), first));
        assertEquals("North", GuiDocument.effectiveBorderConstraint(document.root(), second));
        assertEquals("West", GuiDocument.effectiveBorderConstraint(document.root(), third));
        assertSame(second, GuiDocument.childAtBorderConstraint(document.root(), "North", null));
        assertNull(GuiDocument.childAtBorderConstraint(document.root(), "South", null));
    }

    @Test
    void editsToolbarCommandsWithoutMixingThemIntoComponents() {
        GuiDocument document = GuiDocument.parse("Form.gui", "<component type=\"Form\" name=\"Form\"/>");
        Element command = document.addCommand();
        document.setCommandAttribute(command, "name", "Save");
        document.setCommandAttribute(command, "placement", "left");

        assertEquals(1, document.commands().size());
        assertEquals("Save", document.commands().get(0).getAttribute("name"));
        assertEquals(1, document.components().size());
        assertTrue(document.toXml().contains("<command"));
        assertTrue(document.removeCommand(command));
        assertTrue(document.commands().isEmpty());
    }

    @Test
    void updatesParsedCaseInsensitiveAttributesWithoutCreatingStaleDuplicates() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\" layout=\"LayeredLayout\">"
                + "<component type=\"Label\" name=\"card\" layeredInsets=\"0% 80% 80% 0%\" layoutConstraint=\"North\"/>"
                + "</component>");
        Element card = document.components().get(1);
        document.select(card);
        document.setAttribute("layeredInsets", "20% 60% 60% 20%");
        document.setAttribute("layoutConstraint", "Center");

        assertEquals("20% 60% 60% 20%", card.getAttribute("layeredInsets"));
        assertEquals("Center", card.getAttribute("layoutConstraint"));
        String xml = document.toXml();
        assertEquals(1, occurrences(xml.toLowerCase(), "layeredinsets="));
        assertEquals(1, occurrences(xml.toLowerCase(), "layoutconstraint="));
        GuiDocument reparsed = GuiDocument.parse("Form.gui", xml);
        assertEquals("20% 60% 60% 20%", reparsed.components().get(1).getAttribute("layeredInsets"));
        assertEquals("Center", reparsed.components().get(1).getAttribute("layoutConstraint"));
    }

    @Test
    void undoRedoTreatsACompoundPlacementAsOneEditAndRestoresNestedHierarchy() {
        GuiDocument document = GuiDocument.parse("Form.gui",
                "<component type=\"Form\"><component type=\"Container\" name=\"outer\">"
                + "<component type=\"Container\" name=\"inner\"><component type=\"Button\" name=\"moveMe\"/>"
                + "</component></component><component type=\"Container\" name=\"destination\"/></component>");
        Element button = document.components().get(3);
        Element destination = document.components().get(4);
        document.select(button);
        document.beginTransaction();
        assertTrue(document.moveSelectedToParent(destination, 0));
        document.setAttribute("layoutConstraint", "Center");
        document.endTransaction();
        assertSame(destination, document.parentOf(document.selected()));
        assertTrue(document.canUndo());

        assertTrue(document.undo());
        Element restoredButton = document.selected();
        assertEquals("moveMe", restoredButton.getAttribute("name"));
        assertEquals("inner", document.parentOf(restoredButton).getAttribute("name"));
        assertFalse(document.canUndo(), "compound placement should create one undo entry");
        assertTrue(document.canRedo());

        assertTrue(document.redo());
        assertEquals("destination", document.parentOf(document.selected()).getAttribute("name"));
        assertEquals("Center", document.selected().getAttribute("layoutConstraint"));
    }

    @Test
    void undoingAcrossASaveLeavesTheDocumentDirty() {
        GuiDocument document = GuiDocument.parse("path.gui",
                "<component type=\"Form\" name=\"Form\" layout=\"LayeredLayout\"></component>");
        document.addComponent("Button");
        document.markSaved();
        assertFalse(document.isModified(), "a document that was just written matches the file");

        // Undo takes the form back to a state that predates the save, so it no longer matches what
        // is on disk even though the snapshot it came from was captured while the form was clean.
        assertTrue(document.undo());
        assertTrue(document.isModified(),
                "undoing past the save point must leave the document dirty, or the next form switch"
                        + " discards the undone state without a prompt");

        assertTrue(document.redo());
        assertFalse(document.isModified(), "redoing back to the saved content is clean again");
    }

    private static Element named(GuiDocument document, String name) {
        for (Element element : document.components()) {
            if (name.equals(element.getAttribute("name"))) return element;
        }
        return null;
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0; at += needle.length()) count++;
        return count;
    }
    @Test
    void pastingAScreenTypeIsRejected() {
        // Copy on the root and Paste put a cloned Form underneath itself. The canvas substitutes a
        // content pane for the nested element while the generated source emits new Form(...), so
        // the preview and the saved application stopped describing the same tree.
        GuiDocument document = GuiDocument.parse("/tmp/p/gui/com/example/F.gui",
                "<component type=\"Form\" name=\"F\" layout=\"BoxLayout\">"
                + "<component type=\"Button\" name=\"ok\"/></component>");
        document.select(document.root());
        String copied = document.copySelectedXml();

        assertNull(document.pasteXml(copied), "a Form is a screen, not a child");
        assertEquals(2, document.components().size(), "and nothing was added: " + document.toXml());

        // An ordinary component still pastes.
        document.select(document.components().get(1));
        assertNotNull(document.pasteXml(document.copySelectedXml()));
        assertEquals(3, document.components().size());
    }

}
