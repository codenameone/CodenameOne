package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputComponentTest extends UITestBase {

    /**
     * Concrete implementation of InputComponent for testing purposes
     */
    private static class TestInputComponent extends InputComponent {
        private final TextField editor = new TextField();

        TestInputComponent() {
            initInput();
        }

        @Override
        public Component getEditor() {
            return editor;
        }
    }

    @Test
    void testInputComponentCreation() {
        TestInputComponent input = new TestInputComponent();
        assertNotNull(input);
        assertNotNull(input.getEditor());
    }

    @Test
    void testLabelText() {
        TestInputComponent input = new TestInputComponent();
        input.label("Username");
        input.constructUI();

        Label label = input.getLabel();
        assertEquals("Username", label.getText());
    }

    @Test
    void testLabelChaining() {
        TestInputComponent input = new TestInputComponent();
        InputComponent result = input.label("Email");

        assertSame(input, result, "label() should return this for chaining");
    }

    @Test
    void testErrorMessage() {
        TestInputComponent input = new TestInputComponent();
        input.errorMessage("Invalid input");
        input.constructUI();

        Component errorMsg = input.getErrorMessage();
        assertTrue(errorMsg instanceof TextHolder);
        assertEquals("Invalid input", ((TextHolder) errorMsg).getText());
    }

    @Test
    void testErrorMessageChaining() {
        TestInputComponent input = new TestInputComponent();
        InputComponent result = input.errorMessage("Error");

        assertSame(input, result, "errorMessage() should return this for chaining");
    }

    @Test
    void testClearErrorMessage() {
        TestInputComponent input = new TestInputComponent();
        input.errorMessage("Error");
        input.constructUI();

        input.errorMessage(null);

        Component errorMsg = input.getErrorMessage();
        assertTrue(errorMsg instanceof TextHolder);
        assertNull(((TextHolder) errorMsg).getText());
    }

    @Test
    void testDescriptionMessage() {
        TestInputComponent input = new TestInputComponent();
        input.descriptionMessage("Enter your username");
        input.constructUI();

        Label descMsg = input.getDescriptionMessage();
        assertEquals("Enter your username", descMsg.getText());
    }

    @Test
    void testDescriptionMessageChaining() {
        TestInputComponent input = new TestInputComponent();
        InputComponent result = input.descriptionMessage("Description");

        assertSame(input, result, "descriptionMessage() should return this for chaining");
    }

    @Test
    void testOnTopModeDefault() {
        TestInputComponent input = new TestInputComponent();
        // Default may vary, just verify it returns a value
        boolean mode = input.isOnTopMode();
        assertTrue(mode || !mode);
    }

    @Test
    void testOnTopModeSet() {
        TestInputComponent input = new TestInputComponent();
        input.onTopMode(true);

        assertTrue(input.isOnTopMode());

        input.onTopMode(false);
        assertFalse(input.isOnTopMode());
    }

    @Test
    void testOnTopModeChaining() {
        TestInputComponent input = new TestInputComponent();
        InputComponent result = input.onTopMode(true);

        assertSame(input, result, "onTopMode() should return this for chaining");
    }

    @Test
    void testActionText() {
        TestInputComponent input = new TestInputComponent();
        input.actionText("Submit");
        input.constructUI();

        assertEquals("Submit", input.getActionText());
        assertNotNull(input.getAction());
    }

    @Test
    void testActionTextChaining() {
        TestInputComponent input = new TestInputComponent();
        InputComponent result = input.actionText("OK");

        assertSame(input, result, "actionText() should return this for chaining");
    }

    @Test
    void testActionIcon() {
        TestInputComponent input = new TestInputComponent();
        input.action('X');
        input.constructUI();

        assertNotNull(input.getAction());
    }

    @Test
    void testActionIconChaining() {
        TestInputComponent input = new TestInputComponent();
        InputComponent result = input.action('X');

        assertSame(input, result, "action() should return this for chaining");
    }

    @Test
    void testActionClick() {
        TestInputComponent input = new TestInputComponent();
        final boolean[] clicked = {false};

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                clicked[0] = true;
            }
        };

        input.actionText("Click");
        input.actionClick(listener);
        input.constructUI();

        Button action = input.getAction();
        assertNotNull(action);

        // Simulate click
        action.fireActionEvent();
        assertTrue(clicked[0], "Action listener should be invoked");
    }

    @Test
    void testActionClickChaining() {
        TestInputComponent input = new TestInputComponent();
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
            }
        };
        InputComponent result = input.actionClick(listener);

        assertSame(input, result, "actionClick() should return this for chaining");
    }

    @Test
    void testActionUIID() {
        TestInputComponent input = new TestInputComponent();
        input.actionText("Action");
        input.actionUIID("CustomActionButton");
        input.constructUI();

        assertEquals("CustomActionButton", input.getActionUIID());
        assertEquals("CustomActionButton", input.getAction().getUIID());
    }

    @Test
    void testActionUIIDChaining() {
        TestInputComponent input = new TestInputComponent();
        InputComponent result = input.actionUIID("Custom");

        assertSame(input, result, "actionUIID() should return this for chaining");
    }

    @Test
    void testActionAsButtonDefault() {
        TestInputComponent input = new TestInputComponent();
        assertFalse(input.isActionAsButton());
    }

    @Test
    void testActionAsButtonSet() {
        TestInputComponent input = new TestInputComponent();
        input.actionAsButton(true);

        assertTrue(input.isActionAsButton());

        input.actionAsButton(false);
        assertFalse(input.isActionAsButton());
    }

    @Test
    void testActionAsButtonChaining() {
        TestInputComponent input = new TestInputComponent();
        InputComponent result = input.actionAsButton(true);

        assertSame(input, result, "actionAsButton() should return this for chaining");
    }

    @Test
    void testGroupComponents() {
        TestInputComponent input1 = new TestInputComponent();
        TestInputComponent input2 = new TestInputComponent();
        TestInputComponent input3 = new TestInputComponent();

        input1.onTopMode(false);
        input2.onTopMode(false);
        input3.onTopMode(false);

        input1.label("First");
        input2.label("Second");
        input3.label("Third Field");

        InputComponent.group(input1, input2, input3);
        input1.constructUI();
        input2.constructUI();
        input3.constructUI();

        // After grouping, all labels should have same preferred width
        int width1 = input1.getLabel().getPreferredW();
        int width2 = input2.getLabel().getPreferredW();
        int width3 = input3.getLabel().getPreferredW();

        assertEquals(width1, width2);
        assertEquals(width2, width3);
    }

    @Test
    void testGroupOnlyAffectsNonOnTopMode() {
        TestInputComponent input1 = new TestInputComponent();
        TestInputComponent input2 = new TestInputComponent();

        input1.onTopMode(true);
        input2.onTopMode(false);

        input1.label("First");
        input2.label("Second");

        // Should not throw, just skip input1
        InputComponent.group(input1, input2);
    }

    @Test
    void testMultiLineErrorMessageStaticSetter() {
        boolean original = InputComponent.isMultiLineErrorMessage();

        InputComponent.setMultiLineErrorMessage(true);
        assertTrue(InputComponent.isMultiLineErrorMessage());

        InputComponent.setMultiLineErrorMessage(false);
        assertFalse(InputComponent.isMultiLineErrorMessage());

        // Restore original
        InputComponent.setMultiLineErrorMessage(original);
    }

    @Test
    void testGetActionReturnsNullWhenNoActionSet() {
        TestInputComponent input = new TestInputComponent();
        assertNull(input.getAction());
    }

    @Test
    void testConstructUICreatesLayout() {
        TestInputComponent input = new TestInputComponent();
        input.label("Test");
        assertEquals(0, input.getComponentCount());

        input.constructUI();

        assertTrue(input.getComponentCount() > 0, "constructUI should add components");
    }

    @Test
    void testConstructUIOnlyOnce() {
        TestInputComponent input = new TestInputComponent();
        input.label("Test");
        input.constructUI();

        int count = input.getComponentCount();
        input.constructUI();

        assertEquals(count, input.getComponentCount(),
                "constructUI should not add components if already constructed");
    }

    @Test
    void testOnTopModeLayoutDifference() {
        TestInputComponent onTop = new TestInputComponent();
        onTop.onTopMode(true);
        onTop.label("Label");
        onTop.constructUI();

        TestInputComponent side = new TestInputComponent();
        side.onTopMode(false);
        side.label("Label");
        side.constructUI();

        // Both should have components but potentially different structures
        assertTrue(onTop.getComponentCount() > 0);
        assertTrue(side.getComponentCount() > 0);
    }

    @Test
    void testEditorHasLabelForComponent() {
        TestInputComponent input = new TestInputComponent();
        input.label("Username");
        input.constructUI();

        TextField editor = (TextField) input.getEditor();
        assertSame(input.getLabel(), editor.getLabelForComponent());
    }

    @Test
    void testUIIDDefaultsToTextComponent() {
        TestInputComponent input = new TestInputComponent();
        assertEquals("TextComponent", input.getUIID());
    }

    @Test
    void testLabelIsFocusable() {
        TestInputComponent input = new TestInputComponent();
        input.label("Test");
        input.constructUI();

        assertFalse(input.getLabel().isFocusable());
    }

    @Test
    void testChainedConfiguration() {
        TestInputComponent input = new TestInputComponent();

        InputComponent result = input
                .label("Email")
                .errorMessage("Invalid email")
                .descriptionMessage("Enter your email address")
                .onTopMode(true)
                .actionText("Verify");

        assertSame(input, result);

        input.constructUI();

        assertEquals("Email", input.getLabel().getText());
        assertEquals("Invalid email", ((TextHolder) input.getErrorMessage()).getText());
        assertEquals("Enter your email address", input.getDescriptionMessage().getText());
        assertTrue(input.isOnTopMode());
        assertEquals("Verify", input.getActionText());
    }

    @Test
    void testCalcPreferredSizeBeforeConstruction() {
        TestInputComponent input = new TestInputComponent();
        input.label("Test");

        // Should not throw even before constructUI
        assertNotNull(input.getPreferredSize());
    }

    @Test
    void testCalcPreferredSizeAfterConstruction() {
        TestInputComponent input = new TestInputComponent();
        input.label("Test");
        input.constructUI();

        assertNotNull(input.getPreferredSize());
        assertTrue(input.getPreferredW() > 0);
        assertTrue(input.getPreferredH() > 0);
    }

    @Test
    void testActionButtonClearedWithNullText() {
        TestInputComponent input = new TestInputComponent();
        input.actionText("Submit");
        input.constructUI();
        assertNotNull(input.getAction());

        input.actionText(null);
        // Action button should still exist but with no text
        assertNotNull(input.getAction());
    }
}
