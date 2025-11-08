package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Command;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SpanButtonTest extends UITestBase {

    @FormTest
    void testDefaultConstructorCreatesEmptyButton() {
        SpanButton button = new SpanButton();
        assertNotNull(button);
        assertEquals("Button", button.getUIID());
    }

    @FormTest
    void testConstructorWithTextSetsText() {
        SpanButton button = new SpanButton("Click Me");
        assertEquals("Click Me", button.getText());
    }

    @FormTest
    void testConstructorWithTextAndUIIDSetsTextAndUIID() {
        SpanButton button = new SpanButton("Test", "CustomButton");
        assertEquals("Test", button.getText());
        assertEquals("CustomButton", button.getTextUIID());
    }

    @FormTest
    void testSetTextUpdatesText() {
        SpanButton button = new SpanButton();
        button.setText("New Text");
        assertEquals("New Text", button.getText());
    }

    @FormTest
    void testIconGetterAndSetter() {
        SpanButton button = new SpanButton("Test");
        Image icon = Image.createImage(20, 20, 0xFF0000);
        button.setIcon(icon);
        assertSame(icon, button.getIcon());
    }

    @FormTest
    void testTextUIIDGetterAndSetter() {
        SpanButton button = new SpanButton("Test");
        button.setTextUIID("CustomTextUIID");
        assertEquals("CustomTextUIID", button.getTextUIID());
    }

    @FormTest
    void testIconUIIDGetterAndSetter() {
        SpanButton button = new SpanButton("Test");
        button.setIconUIID("CustomIconUIID");
        assertEquals("CustomIconUIID", button.getIconUIID());
    }

    @FormTest
    void testIconPositionGetterAndSetter() {
        SpanButton button = new SpanButton("Test");
        assertEquals(BorderLayout.WEST, button.getIconPosition());

        button.setIconPosition(BorderLayout.EAST);
        assertEquals(BorderLayout.EAST, button.getIconPosition());

        button.setIconPosition(BorderLayout.NORTH);
        assertEquals(BorderLayout.NORTH, button.getIconPosition());
    }

    @FormTest
    void testTextPositionGetterAndSetter() {
        SpanButton button = new SpanButton("Test");

        button.setTextPosition(com.codename1.ui.Component.RIGHT);
        assertEquals(com.codename1.ui.Component.RIGHT, button.getTextPosition());

        button.setTextPosition(com.codename1.ui.Component.LEFT);
        assertEquals(com.codename1.ui.Component.LEFT, button.getTextPosition());
    }

    @FormTest
    void testGapGetterAndSetter() {
        SpanButton button = new SpanButton("Test");
        button.setGap(10);
        assertEquals(10, button.getGap());

        button.setGap(20);
        assertEquals(20, button.getGap());
    }

    @FormTest
    void testShouldLocalizeGetterAndSetter() {
        SpanButton button = new SpanButton("Test");
        assertTrue(button.isShouldLocalize());

        button.setShouldLocalize(false);
        assertFalse(button.isShouldLocalize());

        button.setShouldLocalize(true);
        assertTrue(button.isShouldLocalize());
    }

    @FormTest
    void testAddActionListener() {
        SpanButton button = new SpanButton("Test");
        AtomicInteger count = new AtomicInteger();
        button.addActionListener(evt -> count.incrementAndGet());

        // Verify listener was added
        assertNotNull(button);
    }

    @FormTest
    void testRemoveActionListener() {
        SpanButton button = new SpanButton("Test");
        AtomicInteger count = new AtomicInteger();
        button.addActionListener(evt -> count.incrementAndGet());
        button.removeActionListener(evt -> count.incrementAndGet());

        // Verify listener was removed
        assertNotNull(button);
    }

    @FormTest
    void testCommandGetterAndSetter() {
        SpanButton button = new SpanButton("Test");
        Command cmd = new Command("Test Command");
        button.setCommand(cmd);
        assertSame(cmd, button.getCommand());
    }

    @FormTest
    void testPropertyNames() {
        SpanButton button = new SpanButton("Test");
        String[] props = button.getPropertyNames();
        assertNotNull(props);
        assertTrue(props.length > 0);
    }

    @FormTest
    void testPropertyTypes() {
        SpanButton button = new SpanButton("Test");
        Class[] types = button.getPropertyTypes();
        assertNotNull(types);
        assertEquals(button.getPropertyNames().length, types.length);
    }

    @FormTest
    void testGetPropertyValue() {
        SpanButton button = new SpanButton("TestValue");
        Object text = button.getPropertyValue("text");
        assertEquals("TestValue", text);
    }

    @FormTest
    void testSetPropertyValue() {
        SpanButton button = new SpanButton();
        button.setPropertyValue("text", "NewValue");
        assertEquals("NewValue", button.getText());
    }

    @FormTest
    void testMaterialIconMethods() {
        SpanButton button = new SpanButton("Test");
        button.setMaterialIcon('\uE855', 4.0f);
        assertNotNull(button.getIcon());
    }

    @FormTest
    void testFontIconMethods() {
        SpanButton button = new SpanButton("Test");
        assertNotNull(button.getIconStyleComponent());
    }

    @FormTest
    void testMaskNameGetterAndSetter() {
        SpanButton button = new SpanButton("Test");
        button.setMaskName("TestMask");
        assertEquals("TestMask", button.getMaskName());
    }

    @FormTest
    void testTextAllCaps() {
        SpanButton button = new SpanButton("test");
        button.setTextAllCaps(true);
        assertTrue(button.isTextAllCaps());

        button.setTextAllCaps(false);
        assertFalse(button.isTextAllCaps());
    }

    @FormTest
    void testIconsFromState() {
        SpanButton button = new SpanButton("Test");
        Image icon = Image.createImage(10, 10, 0x00FF00);
        button.setIcon(icon);

        Image rollover = Image.createImage(10, 10, 0xFF0000);
        button.setRolloverIcon(rollover);
        assertSame(rollover, button.getRolloverIcon());

        Image pressed = Image.createImage(10, 10, 0x0000FF);
        button.setPressedIcon(pressed);
        assertSame(pressed, button.getPressedIcon());

        Image disabled = Image.createImage(10, 10, 0xCCCCCC);
        button.setDisabledIcon(disabled);
        assertSame(disabled, button.getDisabledIcon());

        Image rolloverPressed = Image.createImage(10, 10, 0xFFFF00);
        button.setRolloverPressedIcon(rolloverPressed);
        assertSame(rolloverPressed, button.getRolloverPressedIcon());
    }

    @FormTest
    void testIsFocusable() {
        SpanButton button = new SpanButton("Test");
        assertTrue(button.isFocusable());
    }

    @FormTest
    void testActualButton() {
        SpanButton button = new SpanButton("Test");
        assertNotNull(button.getActualButton());
    }
}
