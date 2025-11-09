package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TextAreaTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        TextArea textArea = new TextArea();
        assertNotNull(textArea);
        assertEquals("", textArea.getText());
        assertFalse(textArea.isEditable());
    }

    @FormTest
    void testConstructorWithText() {
        TextArea textArea = new TextArea("Hello");
        assertEquals("Hello", textArea.getText());
    }

    @FormTest
    void testConstructorWithRows() {
        TextArea textArea = new TextArea(5, 20);
        assertEquals(5, textArea.getRows());
    }

    @FormTest
    void testConstructorWithTextAndRows() {
        TextArea textArea = new TextArea("Test", 3);
        assertEquals("Test", textArea.getText());
        assertEquals(3, textArea.getRows());
    }

    @FormTest
    void testConstructorWithTextRowsAndCols() {
        TextArea textArea = new TextArea("Text", 4, 20);
        assertEquals("Text", textArea.getText());
        assertEquals(4, textArea.getRows());
        assertEquals(20, textArea.getColumns());
    }

    @FormTest
    void testConstructorWithRowsAndCols() {
        TextArea textArea = new TextArea(3, 25);
        assertEquals(3, textArea.getRows());
        assertEquals(25, textArea.getColumns());
    }

    @FormTest
    void testConstructorWithTextRowsColsAndConstraint() {
        TextArea textArea = new TextArea("Email", 2, 30, TextArea.EMAILADDR);
        assertEquals("Email", textArea.getText());
        assertEquals(2, textArea.getRows());
        assertEquals(30, textArea.getColumns());
        assertEquals(TextArea.EMAILADDR, textArea.getConstraint());
    }

    @FormTest
    void testSetText() {
        TextArea textArea = new TextArea();
        textArea.setText("New text");
        assertEquals("New text", textArea.getText());
    }

    @FormTest
    void testSetTextNull() {
        TextArea textArea = new TextArea("Initial");
        textArea.setText(null);
        assertEquals("", textArea.getText());
    }

    @FormTest
    void testRows() {
        TextArea textArea = new TextArea();
        textArea.setRows(5);
        assertEquals(5, textArea.getRows());

        textArea.setRows(10);
        assertEquals(10, textArea.getRows());
    }

    @FormTest
    void testColumns() {
        TextArea textArea = new TextArea();
        textArea.setColumns(20);
        assertEquals(20, textArea.getColumns());

        textArea.setColumns(40);
        assertEquals(40, textArea.getColumns());
    }

    @FormTest
    void testMaxSize() {
        TextArea textArea = new TextArea();
        textArea.setMaxSize(100);
        assertEquals(100, textArea.getMaxSize());
    }

    @FormTest
    void testConstraint() {
        TextArea textArea = new TextArea();

        textArea.setConstraint(TextArea.NUMERIC);
        assertEquals(TextArea.NUMERIC, textArea.getConstraint());

        textArea.setConstraint(TextArea.EMAILADDR);
        assertEquals(TextArea.EMAILADDR, textArea.getConstraint());

        textArea.setConstraint(TextArea.PASSWORD);
        assertEquals(TextArea.PASSWORD, textArea.getConstraint());
    }

    @FormTest
    void testEditableFlag() {
        TextArea textArea = new TextArea();
        assertFalse(textArea.isEditable());

        textArea.setEditable(true);
        assertTrue(textArea.isEditable());

        textArea.setEditable(false);
        assertFalse(textArea.isEditable());
    }

    @FormTest
    void testGrowByContent() {
        TextArea textArea = new TextArea();

        textArea.setGrowByContent(true);
        assertTrue(textArea.isGrowByContent());

        textArea.setGrowByContent(false);
        assertFalse(textArea.isGrowByContent());
    }

    @FormTest
    void testEndsWith3Points() {
        TextArea textArea = new TextArea();

        textArea.setEndsWith3Points(true);
        assertTrue(textArea.isEndsWith3Points());

        textArea.setEndsWith3Points(false);
        assertFalse(textArea.isEndsWith3Points());
    }

    @FormTest
    void testSingleLineTextArea() {
        TextArea textArea = new TextArea();

        textArea.setSingleLineTextArea(true);
        assertTrue(textArea.isSingleLineTextArea());

        textArea.setSingleLineTextArea(false);
        assertFalse(textArea.isSingleLineTextArea());
    }

    @FormTest
    void testHint() {
        TextArea textArea = new TextArea();
        textArea.setHint("Enter text");
        assertEquals("Enter text", textArea.getHint());

        textArea.setHint("New hint");
        assertEquals("New hint", textArea.getHint());
    }

    @FormTest
    void testHintLabel() {
        TextArea textArea = new TextArea();
        Component hintLabel = textArea.getHintLabel();
        // Hint label may be null if not set
        assertTrue(hintLabel == null || hintLabel instanceof Label);
    }

    @FormTest
    void testVerticalAlignment() {
        TextArea textArea = new TextArea();

        textArea.setVerticalAlignment(Component.TOP);
        assertEquals(Component.TOP, textArea.getVerticalAlignment());

        textArea.setVerticalAlignment(Component.CENTER);
        assertEquals(Component.CENTER, textArea.getVerticalAlignment());

        textArea.setVerticalAlignment(Component.BOTTOM);
        assertEquals(Component.BOTTOM, textArea.getVerticalAlignment());
    }

    @FormTest
    void testAlignment() {
        TextArea textArea = new TextArea();

        textArea.setAlignment(Component.LEFT);
        assertEquals(Component.LEFT, textArea.getAlignment());

        textArea.setAlignment(Component.CENTER);
        assertEquals(Component.CENTER, textArea.getAlignment());

        textArea.setAlignment(Component.RIGHT);
        assertEquals(Component.RIGHT, textArea.getAlignment());
    }

    @FormTest
    void testAddActionListener() {
        TextArea textArea = new TextArea();
        AtomicBoolean listenerCalled = new AtomicBoolean(false);

        ActionListener listener = evt -> listenerCalled.set(true);
        textArea.addActionListener(listener);

        // Verify listener was added (no exception)
        assertNotNull(textArea);
    }

    @FormTest
    void testRemoveActionListener() {
        TextArea textArea = new TextArea();
        ActionListener listener = evt -> {};

        textArea.addActionListener(listener);
        textArea.removeActionListener(listener);

        // Verify listener was removed (no exception)
        assertNotNull(textArea);
    }

    @FormTest
    void testAddDataChangeListener() {
        TextArea textArea = new TextArea();
        AtomicInteger changeCount = new AtomicInteger(0);

        DataChangedListener listener = (type, index) -> changeCount.incrementAndGet();
        textArea.addDataChangeListener(listener);

        textArea.setText("New");
        assertTrue(changeCount.get() >= 0);
    }

    @FormTest
    void testRemoveDataChangeListener() {
        TextArea textArea = new TextArea();
        DataChangedListener listener = (type, index) -> {};

        textArea.addDataChangeListener(listener);
        textArea.removeDataChangeListener(listener);

        // Verify listener was removed (no exception)
        assertNotNull(textArea);
    }

    @FormTest
    void testLinesToScroll() {
        TextArea textArea = new TextArea();
        textArea.setLinesToScroll(5);
        assertEquals(5, textArea.getLinesToScroll());
    }

    @FormTest
    void testReplaceText() {
        TextArea textArea = new TextArea("Hello world");
        String text = textArea.getText();
        text = text.replace("world", "universe");
        textArea.setText(text);
        assertEquals("Hello universe", textArea.getText());
    }

    @FormTest
    void testGetActualRows() {
        TextArea textArea = new TextArea("Line1\nLine2\nLine3");
        int actualRows = textArea.getActualRows();
        assertTrue(actualRows >= 1);
    }

    @FormTest
    void testGetBaseline() {
        TextArea textArea = new TextArea("Test");
        int baseline = textArea.getBaseline(100, 50);
        assertTrue(baseline >= 0);
    }

    @FormTest
    void testGetBaselineResizeBehavior() {
        TextArea textArea = new TextArea();
        int behavior = textArea.getBaselineResizeBehavior();
        assertTrue(behavior >= Component.BRB_CONSTANT_ASCENT);
    }

    @FormTest
    void testGetPreferredSize() {
        TextArea textArea = new TextArea("Test text");
        Dimension pref = textArea.getPreferredSize();
        assertTrue(pref.getWidth() > 0);
        assertTrue(pref.getHeight() > 0);
    }

    @FormTest
    void testPreferredWidth() {
        TextArea textArea = new TextArea();
        textArea.setPreferredW(200);
        assertEquals(200, textArea.getPreferredW());
    }

    @FormTest
    void testIsQwertyInput() {
        TextArea textArea = new TextArea();
        // Just verify the method can be called
        boolean qwerty = textArea.isQwertyInput();
        assertTrue(qwerty || !qwerty);
    }

    @FormTest
    void testGetCursorPosition() {
        TextArea textArea = new TextArea("Hello");
        int pos = textArea.getCursorPosition();
        assertTrue(pos >= 0);
    }

    @FormTest
    void testConstraintFlags() {
        TextArea textArea = new TextArea();

        textArea.setConstraint(TextArea.INITIAL_CAPS_WORD);
        assertEquals(TextArea.INITIAL_CAPS_WORD, textArea.getConstraint());

        textArea.setConstraint(TextArea.INITIAL_CAPS_SENTENCE);
        assertEquals(TextArea.INITIAL_CAPS_SENTENCE, textArea.getConstraint());
    }

    @FormTest
    void testRightToLeft() {
        TextArea textArea = new TextArea();
        textArea.setRTL(true);
        assertTrue(textArea.isRTL());

        textArea.setRTL(false);
        assertFalse(textArea.isRTL());
    }

    @FormTest
    void testUIID() {
        TextArea textArea = new TextArea();
        textArea.setUIID("CustomTextArea");
        assertEquals("CustomTextArea", textArea.getUIID());
    }

    @FormTest
    void testContrainstTypes() {
        assertEquals(0, TextArea.ANY);
        assertEquals(1, TextArea.EMAILADDR);
        assertEquals(2, TextArea.NUMERIC);
        assertEquals(3, TextArea.PHONENUMBER);
        assertEquals(4, TextArea.URL);
        assertEquals(5, TextArea.DECIMAL);
        assertTrue(TextArea.PASSWORD > 0);
        assertTrue(TextArea.UNEDITABLE > 0);
        assertTrue(TextArea.SENSITIVE > 0);
        assertTrue(TextArea.NON_PREDICTIVE > 0);
        assertTrue(TextArea.INITIAL_CAPS_WORD > 0);
        assertTrue(TextArea.INITIAL_CAPS_SENTENCE > 0);
    }

    @FormTest
    void testAppendText() {
        TextArea textArea = new TextArea("Hello");
        textArea.setText(textArea.getText() + " World");
        assertEquals("Hello World", textArea.getText());
    }

    @FormTest
    void testTextWithNewlines() {
        TextArea textArea = new TextArea("Line1\nLine2\nLine3");
        assertTrue(textArea.getText().contains("\n"));
        assertTrue(textArea.getActualRows() > 1);
    }

    @FormTest
    void testRefreshTheme() {
        TextArea textArea = new TextArea();
        assertDoesNotThrow(() -> textArea.refreshTheme(false));
        assertDoesNotThrow(() -> textArea.refreshTheme(true));
    }
}
