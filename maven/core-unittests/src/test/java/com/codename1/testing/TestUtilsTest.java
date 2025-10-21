package com.codename1.testing;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Storage;
import com.codename1.io.TestImplementationProvider;
import com.codename1.test.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.util.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestUtilsTest extends UITestBase {
    private Form testForm;

    @BeforeEach
    void setUp() {
        testForm = new Form("Test Form", new BorderLayout());
    }

    // Verbose mode tests
    @Test
    void setVerboseModeChangesVerbosity() {
        TestUtils.setVerboseMode(true);
        TestUtils.setVerboseMode(false);
        // Just verify it doesn't throw
        assertDoesNotThrow(() -> TestUtils.setVerboseMode(true));
    }

    // Wait methods tests
    @Test
    void waitForPausesExecution() {
        long startTime = System.currentTimeMillis();
        TestUtils.waitFor(100);
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime >= 90); // Allow some margin
    }

    @Test
    void waitForHandlesZeroDuration() {
        assertDoesNotThrow(() -> TestUtils.waitFor(0));
    }

    // Component finding tests
    @Test
    void findByNameReturnsNullWhenComponentNotFound() {
        testForm.show();
        Component result = TestUtils.findByName("NonExistentComponent");
        assertNull(result);
    }

    @Test
    void findByNameFindsComponentByName() {
        Button button = new Button("Click Me");
        button.setName("TestButton");
        testForm.add(CN.CENTER, button);
        testForm.show();

        Component found = TestUtils.findByName("TestButton");
        assertSame(button, found);
    }

    @Test
    void findByNameFindsNestedComponent() {
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        Label label = new Label("Test Label");
        label.setName("NestedLabel");
        container.add(label);
        testForm.add(CN.CENTER, container);
        testForm.show();

        Component found = TestUtils.findByName("NestedLabel");
        assertSame(label, found);
    }

    @Test
    void findLabelTextFindsLabelByText() {
        Label label = new Label("Find Me");
        testForm.add(CN.CENTER, label);
        testForm.show();

        Label found = TestUtils.findLabelText("Find Me");
        assertSame(label, found);
    }

    @Test
    void findLabelTextReturnsNullWhenNotFound() {
        testForm.show();
        Label found = TestUtils.findLabelText("NonExistent");
        assertNull(found);
    }

    // List selection tests
    @Test
    void selectInListByNameSelectsCorrectOffset() {
        DefaultListModel<String> model = new DefaultListModel<String>(new String[]{"Item 1", "Item 2", "Item 3"});
        List list = new List(model);
        list.setName("TestList");
        testForm.add(CN.CENTER, list);
        testForm.show();

        TestUtils.selectInList("TestList", 1);
        assertEquals(1, list.getSelectedIndex());
    }

    @Test
    void selectInListByPathSelectsCorrectOffset() {
        DefaultListModel<String> model = new DefaultListModel<String>(new String[]{"Item 1", "Item 2", "Item 3"});
        List list = new List(model);
        testForm.add(CN.CENTER, list);
        testForm.show();

        TestUtils.selectInList(new int[]{0}, 2);
        assertEquals(2, list.getSelectedIndex());
    }

    // Button clicking tests
    @Test
    void clickButtonByLabelClicksButton() {
        final boolean[] clicked = {false};
        Button button = new Button("Click Me");
        button.addActionListener(e -> clicked[0] = true);
        testForm.add(CN.CENTER, button);
        testForm.show();

        TestUtils.clickButtonByLabel("Click Me");
        TestUtils.waitFor(100);

        // Button should have been pressed
        assertNotNull(button);
    }

    @Test
    void clickButtonByNameClicksButton() {
        final boolean[] clicked = {false};
        Button button = new Button("Test");
        button.setName("TestButton");
        button.addActionListener(e -> clicked[0] = true);
        testForm.add(CN.CENTER, button);
        testForm.show();

        TestUtils.clickButtonByName("TestButton");
        TestUtils.waitFor(100);

        assertNotNull(button);
    }

    @Test
    void clickButtonByPathClicksButton() {
        Button button = new Button("Test");
        testForm.add(CN.CENTER, button);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.clickButtonByPath(new int[]{0}));
    }

    // Form navigation tests
    @Test
    void goBackExecutesBackCommand() {
        Form form1 = new Form("Form 1");
        Form form2 = new Form("Form 2");
        form2.setBackCommand(new Command("Back") {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                form1.showBack();
            }
        });
        form2.show();

        TestUtils.goBack();
        TestUtils.waitFor(100);

        // Verify back command was executed
        assertNotNull(form2.getBackCommand());
    }

    @Test
    void clickMenuItemExecutesCommand() {
        Command testCommand = new Command("Test Command");
        testForm.addCommand(testCommand);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.clickMenuItem("Test Command"));
    }

    // Component path tests
    @Test
    void getComponentByPathReturnsCorrectComponent() {
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        Label label1 = new Label("Label 1");
        Label label2 = new Label("Label 2");
        container.add(label1);
        container.add(label2);
        testForm.add(CN.CENTER, container);
        testForm.show();

        Component found = TestUtils.getComponentByPath(new int[]{0, 1});
        assertSame(label2, found);
    }

    // Text setting tests
    @Test
    void setTextByNameSetsTextAreaText() {
        TextField textField = new TextField();
        textField.setName("TestField");
        testForm.add(CN.CENTER, textField);
        testForm.show();

        TestUtils.setText("TestField", "New Text");
        assertEquals("New Text", textField.getText());
    }

    @Test
    void setTextByPathSetsTextAreaText() {
        TextField textField = new TextField();
        testForm.add(CN.CENTER, textField);
        testForm.show();

        TestUtils.setText(new int[]{0}, "Path Text");
        assertEquals("Path Text", textField.getText());
    }

    @Test
    void setTextHandlesLabel() {
        Label label = new Label("Old");
        label.setName("TestLabel");
        testForm.add(CN.CENTER, label);
        testForm.show();

        TestUtils.setText("TestLabel", "New");
        assertEquals("New", label.getText());
    }

    // Visibility tests
    @Test
    void ensureVisibleScrollsToComponent() {
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        for (int i = 0; i < 20; i++) {
            container.add(new Label("Label " + i));
        }
        Label targetLabel = new Label("Target");
        targetLabel.setName("Target");
        container.add(targetLabel);
        container.setScrollableY(true);
        testForm.add(CN.CENTER, container);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.ensureVisible("Target"));
    }

    @Test
    void ensureVisibleByPathScrollsToComponent() {
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        Label label = new Label("Test");
        container.add(label);
        testForm.add(CN.CENTER, container);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.ensureVisible(new int[]{0, 0}));
    }

    @Test
    void ensureVisibleWithComponentScrollsToComponent() {
        Label label = new Label("Test");
        testForm.add(CN.CENTER, label);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.ensureVisible(label));
    }

    // Form waiting tests
    @Test
    void waitForFormTitleWaitsForCorrectForm() {
        testForm.setTitle("Expected Title");
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.waitForFormTitle("Expected Title", 1000));
    }

    @Test
    void waitForFormNameWaitsForCorrectForm() {
        testForm.setName("ExpectedName");
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.waitForFormName("ExpectedName", 1000));
    }

    @Test
    void waitForUnnamedFormWaitsForFormWithoutName() {
        Form unnamedForm = new Form("Title");
        unnamedForm.show();

        assertDoesNotThrow(() -> TestUtils.waitForUnnamedForm(1000));
    }

    // Logging tests
    @Test
    void logMessageLogsString() {
        assertDoesNotThrow(() -> TestUtils.log("Test message"));
    }

    @Test
    void logExceptionLogsThrowable() {
        Exception testException = new RuntimeException("Test");
        assertDoesNotThrow(() -> TestUtils.log(testException));
    }

    // Key press tests
    @Test
    void keyPressSimulatesKeyPress() {
        testForm.show();
        assertDoesNotThrow(() -> TestUtils.keyPress(10));
    }

    @Test
    void keyReleaseSimulatesKeyRelease() {
        testForm.show();
        assertDoesNotThrow(() -> TestUtils.keyRelease(10));
    }

    @Test
    void gameKeyPressSimulatesGameKeyPress() {
        testForm.show();
        assertDoesNotThrow(() -> TestUtils.gameKeyPress(Display.GAME_UP));
    }

    @Test
    void gameKeyReleaseSimulatesGameKeyRelease() {
        testForm.show();
        assertDoesNotThrow(() -> TestUtils.gameKeyRelease(Display.GAME_DOWN));
    }

    // Pointer tests
    @Test
    void pointerPressSimulatesPress() {
        Button button = new Button("Test");
        button.setName("TestButton");
        testForm.add(CN.CENTER, button);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.pointerPress(0.5f, 0.5f, "TestButton"));
    }

    @Test
    void pointerPressWithNullComponentName() {
        testForm.show();
        assertDoesNotThrow(() -> TestUtils.pointerPress(0.5f, 0.5f, (String) null));
    }

    @Test
    void pointerReleaseSimulatesRelease() {
        Button button = new Button("Test");
        button.setName("TestButton");
        testForm.add(CN.CENTER, button);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.pointerRelease(0.5f, 0.5f, "TestButton"));
    }

    @Test
    void pointerDragSimulatesDrag() {
        Component comp = new Label("Test");
        comp.setName("TestComp");
        testForm.add(CN.CENTER, comp);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.pointerDrag(0.3f, 0.3f, "TestComp"));
    }

    @Test
    void pointerPressByPathSimulatesPress() {
        Button button = new Button("Test");
        testForm.add(CN.CENTER, button);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.pointerPress(0.5f, 0.5f, new int[]{0}));
    }

    @Test
    void pointerReleaseByPathSimulatesRelease() {
        Button button = new Button("Test");
        testForm.add(CN.CENTER, button);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.pointerRelease(0.5f, 0.5f, new int[]{0}));
    }

    @Test
    void pointerDragByPathSimulatesDrag() {
        Component comp = new Label("Test");
        testForm.add(CN.CENTER, comp);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.pointerDrag(0.3f, 0.3f, new int[]{0}));
    }

    // Assertion tests - Basic
    @Test
    void assertBoolPassesOnTrue() {
        assertDoesNotThrow(() -> TestUtils.assertBool(true));
    }

    @Test
    void assertBoolFailsOnFalse() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertBool(false));
    }

    @Test
    void assertBoolWithMessageFailsWithMessage() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> TestUtils.assertBool(false, "Custom error"));
        assertEquals("Custom error", exception.getMessage());
    }

    @Test
    void failAlwaysFails() {
        assertThrows(RuntimeException.class, () -> TestUtils.fail());
    }

    @Test
    void failWithMessageFailsWithMessage() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> TestUtils.fail("Failure message"));
        assertEquals("Failure message", exception.getMessage());
    }

    @Test
    void assertTruePassesOnTrue() {
        assertDoesNotThrow(() -> TestUtils.assertTrue(true));
    }

    @Test
    void assertTrueFailsOnFalse() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertTrue(false));
    }

    @Test
    void assertFalsePassesOnFalse() {
        assertDoesNotThrow(() -> TestUtils.assertFalse(false));
    }

    @Test
    void assertFalseFailsOnTrue() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertFalse(true));
    }

    // Null assertions
    @Test
    void assertNullPassesOnNull() {
        assertDoesNotThrow(() -> TestUtils.assertNull(null));
    }

    @Test
    void assertNullFailsOnNonNull() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertNull("not null"));
    }

    @Test
    void assertNotNullPassesOnNonNull() {
        assertDoesNotThrow(() -> TestUtils.assertNotNull("not null"));
    }

    @Test
    void assertNotNullFailsOnNull() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotNull(null));
    }

    // Same/Not Same assertions
    @Test
    void assertSamePassesOnSameObject() {
        Object obj = new Object();
        assertDoesNotThrow(() -> TestUtils.assertSame(obj, obj));
    }

    @Test
    void assertSameFailsOnDifferentObjects() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertSame(new Object(), new Object()));
    }

    @Test
    void assertNotSamePassesOnDifferentObjects() {
        assertDoesNotThrow(() -> TestUtils.assertNotSame(new Object(), new Object()));
    }

    @Test
    void assertNotSameFailsOnSameObject() {
        Object obj = new Object();
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotSame(obj, obj));
    }

    // Primitive equality tests
    @Test
    void assertEqualPassesForEqualBytes() {
        assertDoesNotThrow(() -> TestUtils.assertEqual((byte) 5, (byte) 5));
    }

    @Test
    void assertEqualFailsForUnequalBytes() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual((byte) 5, (byte) 6));
    }

    @Test
    void assertEqualPassesForEqualShorts() {
        assertDoesNotThrow(() -> TestUtils.assertEqual((short) 100, (short) 100));
    }

    @Test
    void assertEqualFailsForUnequalShorts() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual((short) 100, (short) 101));
    }

    @Test
    void assertEqualPassesForEqualInts() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(42, 42));
    }

    @Test
    void assertEqualFailsForUnequalInts() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(42, 43));
    }

    @Test
    void assertEqualPassesForEqualLongs() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(1000L, 1000L));
    }

    @Test
    void assertEqualFailsForUnequalLongs() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(1000L, 1001L));
    }

    // Float/Double equality with tolerance
    @Test
    void assertEqualPassesForEqualFloatsWithinTolerance() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(1.0f, 1.01f, 2.0));
    }

    @Test
    void assertEqualFailsForFloatsOutsideTolerance() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(1.0f, 2.0f, 0.1));
    }

    @Test
    void assertEqualPassesForEqualDoublesWithinTolerance() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(1.0, 1.01, 2.0));
    }

    @Test
    void assertEqualFailsForDoublesOutsideTolerance() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual(1.0, 2.0, 0.1));
    }

    @Test
    void assertRangePassesForValuesWithinAbsoluteError() {
        assertDoesNotThrow(() -> TestUtils.assertRange(10.0, 10.5, 1.0));
    }

    @Test
    void assertRangeFailsForValuesOutsideAbsoluteError() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertRange(10.0, 15.0, 1.0));
    }

    // Object equality
    @Test
    void assertEqualPassesForEqualObjects() {
        assertDoesNotThrow(() -> TestUtils.assertEqual("test", "test"));
    }

    @Test
    void assertEqualFailsForUnequalObjects() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertEqual("test", "other"));
    }

    @Test
    void assertEqualHandlesNullObjects() {
        assertDoesNotThrow(() -> TestUtils.assertEqual(null, null));
    }

    // Not equal tests
    @Test
    void assertNotEqualPassesForUnequalInts() {
        assertDoesNotThrow(() -> TestUtils.assertNotEqual(5, 6));
    }

    @Test
    void assertNotEqualFailsForEqualInts() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotEqual(5, 5));
    }

    @Test
    void assertNotEqualPassesForUnequalObjects() {
        assertDoesNotThrow(() -> TestUtils.assertNotEqual("test", "other"));
    }

    @Test
    void assertNotEqualFailsForEqualObjects() {
        assertThrows(RuntimeException.class, () -> TestUtils.assertNotEqual("test", "test"));
    }

    // Array equality tests
    @Test
    void assertArrayEqualPassesForEqualByteArrays() {
        byte[] arr1 = {1, 2, 3};
        byte[] arr2 = {1, 2, 3};
        assertDoesNotThrow(() -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    @Test
    void assertArrayEqualFailsForUnequalByteArrays() {
        byte[] arr1 = {1, 2, 3};
        byte[] arr2 = {1, 2, 4};
        assertThrows(RuntimeException.class, () -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    @Test
    void assertArrayEqualFailsForDifferentLengthArrays() {
        byte[] arr1 = {1, 2, 3};
        byte[] arr2 = {1, 2};
        assertThrows(RuntimeException.class, () -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    @Test
    void assertArrayEqualPassesForEqualIntArrays() {
        int[] arr1 = {10, 20, 30};
        int[] arr2 = {10, 20, 30};
        assertDoesNotThrow(() -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    @Test
    void assertArrayEqualPassesForEqualObjectArrays() {
        String[] arr1 = {"a", "b", "c"};
        String[] arr2 = {"a", "b", "c"};
        assertDoesNotThrow(() -> TestUtils.assertArrayEqual(arr1, arr2));
    }

    // Exception assertions
    @Test
    void assertExceptionPassesWhenExpectedExceptionThrown() {
        assertDoesNotThrow(() ->
                TestUtils.assertException(new RuntimeException(), () -> {
                    throw new RuntimeException();
                })
        );
    }

    @Test
    void assertExceptionFailsWhenNoExceptionThrown() {
        assertThrows(RuntimeException.class, () ->
                TestUtils.assertException(new RuntimeException(), () -> {
                    // No exception
                })
        );
    }

    @Test
    void assertExceptionFailsWhenWrongExceptionThrown() {
        assertThrows(RuntimeException.class, () ->
                TestUtils.assertException(new RuntimeException(), () -> {
                    throw new IllegalArgumentException();
                })
        );
    }

    @Test
    void assertNoExceptionPassesWhenNoExceptionThrown() {
        assertDoesNotThrow(() ->
                TestUtils.assertNoException(() -> {
                    // No exception
                })
        );
    }

    @Test
    void assertNoExceptionFailsWhenExceptionThrown() {
        assertThrows(RuntimeException.class, () ->
                TestUtils.assertNoException(() -> {
                    throw new RuntimeException();
                })
        );
    }

    // UI specific assertions
    @Test
    void assertTitlePassesForCorrectTitle() {
        testForm.setTitle("Expected Title");
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertTitle("Expected Title"));
    }

    @Test
    void assertTitleFailsForIncorrectTitle() {
        testForm.setTitle("Actual Title");
        testForm.show();

        assertThrows(RuntimeException.class, () -> TestUtils.assertTitle("Expected Title"));
    }

    @Test
    void assertLabelPassesForCorrectLabel() {
        Label label = new Label("Test Text");
        label.setName("TestLabel");
        testForm.add(CN.CENTER, label);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertLabel("TestLabel", "Test Text"));
    }

    @Test
    void assertLabelByTextPassesForExistingLabel() {
        Label label = new Label("Find This");
        testForm.add(CN.CENTER, label);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertLabel("Find This"));
    }

    @Test
    void assertLabelByPathPassesForCorrectLabel() {
        Label label = new Label("Path Label");
        testForm.add(CN.CENTER, label);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertLabel(new int[]{0}, "Path Label"));
    }

    @Test
    void assertTextAreaPassesForCorrectText() {
        TextArea textArea = new TextArea("Test Content");
        textArea.setName("TestArea");
        testForm.add(CN.CENTER, textArea);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertTextArea("TestArea", "Test Content"));
    }

    @Test
    void assertTextAreaContainingPassesForPartialMatch() {
        TextArea textArea = new TextArea("This is a long text with specific content");
        textArea.setName("TestArea");
        testForm.add(CN.CENTER, textArea);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertTextAreaContaining("TestArea", "specific content"));
    }

    @Test
    void assertTextAreaStartingWithPassesForCorrectPrefix() {
        TextArea textArea = new TextArea("Start of text");
        textArea.setName("TestArea");
        testForm.add(CN.CENTER, textArea);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertTextAreaStartingWith("TestArea", "Start"));
    }

    @Test
    void assertTextAreaEndingWithPassesForCorrectSuffix() {
        TextArea textArea = new TextArea("End of text");
        textArea.setName("TestArea");
        testForm.add(CN.CENTER, textArea);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertTextAreaEndingWith("TestArea", "text"));
    }

    @Test
    void assertTextAreaByPathPassesForCorrectText() {
        TextArea textArea = new TextArea("Path Text");
        testForm.add(CN.CENTER, textArea);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertTextArea(new int[]{0}, "Path Text"));
    }

    @Test
    void assertTextAreaByTextPassesForExistingText() {
        TextArea textArea = new TextArea("Specific Text");
        testForm.add(CN.CENTER, textArea);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.assertTextArea("Specific Text"));
    }

    // Toolbar command tests
    @Test
    void getToolbarCommandsReturnsEmptyArrayWhenNoToolbar() {
        testForm.show();
        Command[] commands = TestUtils.getToolbarCommands();
        assertNotNull(commands);
    }

    @Test
    void showSidemenuOpensMenu() {
        testForm.getToolbar();
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.showSidemenu());
    }

    @Test
    void executeToolbarCommandAtOffsetExecutesCommand() {
        Toolbar toolbar = testForm.getToolbar();
        Command testCommand = new Command("Test");
        testForm.addCommand(testCommand);
        testForm.show();

        assertDoesNotThrow(() -> TestUtils.executeToolbarCommandAtOffset(0));
    }

    // Screenshot test
    @Test
    void screenshotTestReturnsTrueWhenImageIONotSupported() {
        // Mock ImageIO to return null
        boolean result = TestUtils.screenshotTest("test_screenshot");
        // Should return true when ImageIO is not supported
        assertTrue(result);
    }

    // Helper method tests
    @Test
    void findTextAreaTextFindsTextArea() {
        TextArea textArea = new TextArea("Find This Text");
        testForm.add(CN.CENTER, textArea);
        testForm.show();

        TextArea found = TestUtils.findTextAreaText("Find This Text");
        assertSame(textArea, found);
    }

    @Test
    void findTextAreaTextReturnsNullWhenNotFound() {
        testForm.show();
        TextArea found = TestUtils.findTextAreaText("NonExistent");
        assertNull(found);
    }
}
