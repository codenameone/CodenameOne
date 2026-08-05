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
}
