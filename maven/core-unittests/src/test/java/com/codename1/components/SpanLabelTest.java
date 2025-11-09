package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class SpanLabelTest extends UITestBase {

    @FormTest
    void testDefaultConstructorCreatesEmptyLabel() {
        SpanLabel label = new SpanLabel();
        assertNotNull(label);
        assertEquals("Container", label.getUIID());
    }

    @FormTest
    void testConstructorWithTextSetsText() {
        SpanLabel label = new SpanLabel("Hello World");
        assertEquals("Hello World", label.getText());
    }

    @FormTest
    void testConstructorWithTextAndUIIDSetsTextAndUIID() {
        SpanLabel label = new SpanLabel("Test", "CustomLabel");
        assertEquals("Test", label.getText());
        assertEquals("CustomLabel", label.getTextUIID());
    }

    @FormTest
    void testSetTextUpdatesText() {
        SpanLabel label = new SpanLabel();
        label.setText("New Text");
        assertEquals("New Text", label.getText());
    }

    @FormTest
    void testIconGetterAndSetter() {
        SpanLabel label = new SpanLabel("Test");
        Image icon = Image.createImage(20, 20, 0xFF0000);
        label.setIcon(icon);
        assertSame(icon, label.getIcon());
    }

    @FormTest
    void testTextUIIDGetterAndSetter() {
        SpanLabel label = new SpanLabel("Test");
        label.setTextUIID("CustomTextUIID");
        assertEquals("CustomTextUIID", label.getTextUIID());
    }

    @FormTest
    void testIconUIIDGetterAndSetter() {
        SpanLabel label = new SpanLabel("Test");
        label.setIconUIID("CustomIconUIID");
        assertEquals("CustomIconUIID", label.getIconUIID());
    }

    @FormTest
    void testIconPositionGetterAndSetter() {
        SpanLabel label = new SpanLabel("Test");
        assertEquals(BorderLayout.WEST, label.getIconPosition());

        label.setIconPosition(BorderLayout.EAST);
        assertEquals(BorderLayout.EAST, label.getIconPosition());

        label.setIconPosition(BorderLayout.NORTH);
        assertEquals(BorderLayout.NORTH, label.getIconPosition());
    }

    @FormTest
    void testTextPositionGetterAndSetter() {
        SpanLabel label = new SpanLabel("Test");

        label.setTextPosition(com.codename1.ui.Component.RIGHT);
        assertEquals(com.codename1.ui.Component.RIGHT, label.getTextPosition());

        label.setTextPosition(com.codename1.ui.Component.LEFT);
        assertEquals(com.codename1.ui.Component.LEFT, label.getTextPosition());
    }

    @FormTest
    void testGapGetterAndSetter() {
        SpanLabel label = new SpanLabel("Test");
        label.setGap(10);
        assertEquals(10, label.getGap());

        label.setGap(20);
        assertEquals(20, label.getGap());
    }

    @FormTest
    void testShouldLocalizeGetterAndSetter() {
        SpanLabel label = new SpanLabel("Test");
        assertTrue(label.isShouldLocalize());

        label.setShouldLocalize(false);
        assertFalse(label.isShouldLocalize());

        label.setShouldLocalize(true);
        assertTrue(label.isShouldLocalize());
    }

    @FormTest
    void testPreferredWGetterAndSetter() {
        SpanLabel label = new SpanLabel("Test");

        label.setPreferredW(200);
        assertEquals(200, label.getPreferredW());

        label.setPreferredW(-1);
        assertTrue(label.getPreferredW() != -1); // Should calculate actual preferred width
    }

    @FormTest
    void testPropertyNames() {
        SpanLabel label = new SpanLabel("Test");
        String[] props = label.getPropertyNames();
        assertNotNull(props);
        assertTrue(props.length > 0);
    }

    @FormTest
    void testPropertyTypes() {
        SpanLabel label = new SpanLabel("Test");
        Class[] types = label.getPropertyTypes();
        assertNotNull(types);
        assertEquals(label.getPropertyNames().length, types.length);
    }

    @FormTest
    void testGetPropertyValue() {
        SpanLabel label = new SpanLabel("TestValue");
        Object text = label.getPropertyValue("text");
        assertEquals("TestValue", text);
    }

    @FormTest
    void testSetPropertyValue() {
        SpanLabel label = new SpanLabel();
        label.setPropertyValue("text", "NewValue");
        assertEquals("NewValue", label.getText());
    }

    @FormTest
    void testMaterialIconMethods() {
        SpanLabel label = new SpanLabel("Test");
        label.setMaterialIcon('\uE855', 4.0f);
        assertNotNull(label.getIcon());
    }

    @FormTest
    void testFontIconMethods() {
        SpanLabel label = new SpanLabel("Test");
        assertNotNull(label.getIconStyleComponent());
    }

    @FormTest
    void testTextComponentGetter() {
        SpanLabel label = new SpanLabel("Test");
        assertNotNull(label.getTextComponent());
        assertEquals("Test", label.getTextComponent().getText());
    }

    @FormTest
    void testTextAllStylesGetter() {
        SpanLabel label = new SpanLabel("Test");
        assertNotNull(label.getTextAllStyles());
    }

    @FormTest
    void testTextSelectedAndUnselectedStyle() {
        SpanLabel label = new SpanLabel("Test");
        assertNotNull(label.getTextUnselectedStyle());
        assertNotNull(label.getTextSelectedStyle());
    }

    @FormTest
    void testTextBlockAlign() {
        SpanLabel label = new SpanLabel("Test");
        int initialAlign = label.getTextBlockAlign();

        label.setTextBlockAlign(com.codename1.ui.Component.CENTER);
        assertEquals(com.codename1.ui.Component.CENTER, label.getTextBlockAlign());
    }

    @FormTest
    void testTextSelectionEnabled() {
        SpanLabel label = new SpanLabel("Test");
        assertFalse(label.isTextSelectionEnabled());

        label.setTextSelectionEnabled(true);
        assertTrue(label.isTextSelectionEnabled());
    }

    @FormTest
    void testIconValign() {
        SpanLabel label = new SpanLabel("Test");
        label.setIcon(Image.createImage(10, 10, 0xFF0000));

        label.setIconValign(com.codename1.ui.Component.TOP);
        assertEquals(com.codename1.ui.Component.TOP, label.getIconValign());
    }

    @FormTest
    void testGetMaterialIconAndSize() {
        SpanLabel label = new SpanLabel("Test");
        label.setMaterialIcon('\uE855', 4.0f);
        assertEquals('\uE855', label.getMaterialIcon());
        assertEquals(4.0f, label.getMaterialIconSize(), 0.01f);
    }

    @FormTest
    void testGetFontIconAndSize() {
        SpanLabel label = new SpanLabel("Test");
        label.setMaterialIcon('\uE855');
        assertEquals('\uE855', label.getFontIcon());
    }
}
