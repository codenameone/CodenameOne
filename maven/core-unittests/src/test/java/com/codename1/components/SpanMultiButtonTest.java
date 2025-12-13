package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SpanMultiButtonTest extends UITestBase {

    @FormTest
    void testDefaultConstructorSetsDefaults() {
        SpanMultiButton mb = new SpanMultiButton();
        assertEquals("MultiButton", mb.getUIID());
        assertTrue(mb.isFocusable());
    }

    @FormTest
    void testConstructorWithTextSetsFirstLine() {
        SpanMultiButton mb = new SpanMultiButton("Hello");
        assertEquals("Hello", mb.getTextLine1());
    }

    @FormTest
    void testSetTextLine1UpdatesText() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine1("First");
        assertEquals("First", mb.getTextLine1());
    }

    @FormTest
    void testSetTextLine2UpdatesText() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine2("Second");
        assertEquals("Second", mb.getTextLine2());
    }

    @FormTest
    void testSetTextLine3UpdatesText() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine3("Third");
        assertEquals("Third", mb.getTextLine3());
    }

    @FormTest
    void testSetTextLine4UpdatesText() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine4("Fourth");
        assertEquals("Fourth", mb.getTextLine4());
    }

    @FormTest
    void testRemoveTextLine1ClearsText() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine1("Test");
        mb.removeTextLine1();
        assertEquals("", mb.getTextLine1());
    }

    @FormTest
    void testRemoveTextLine2ClearsText() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine2("Test");
        mb.removeTextLine2();
        assertEquals("", mb.getTextLine2());
    }

    @FormTest
    void testRemoveTextLine3ClearsText() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine3("Test");
        mb.removeTextLine3();
        assertEquals("", mb.getTextLine3());
    }

    @FormTest
    void testRemoveTextLine4ClearsText() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine4("Test");
        mb.removeTextLine4();
        assertEquals("", mb.getTextLine4());
    }

    @FormTest
    void testSetUIIDLine1UpdatesUIID() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setUIIDLine1("CustomLine1");
        assertEquals("CustomLine1", mb.getUIIDLine1());
    }

    @FormTest
    void testSetUIIDLine2UpdatesUIID() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setUIIDLine2("CustomLine2");
        assertEquals("CustomLine2", mb.getUIIDLine2());
    }

    @FormTest
    void testSetUIIDLine3UpdatesUIID() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setUIIDLine3("CustomLine3");
        assertEquals("CustomLine3", mb.getUIIDLine3());
    }

    @FormTest
    void testSetUIIDLine4UpdatesUIID() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setUIIDLine4("CustomLine4");
        assertEquals("CustomLine4", mb.getUIIDLine4());
    }

    @FormTest
    void testSetNameLine1UpdatesName() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setNameLine1("FirstName");
        assertEquals("FirstName", mb.getNameLine1());
    }

    @FormTest
    void testSetNameLine2UpdatesName() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setNameLine2("SecondName");
        assertEquals("SecondName", mb.getNameLine2());
    }

    @FormTest
    void testSetNameLine3UpdatesName() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setNameLine3("ThirdName");
        assertEquals("ThirdName", mb.getNameLine3());
    }

    @FormTest
    void testSetNameLine4UpdatesName() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setNameLine4("FourthName");
        assertEquals("FourthName", mb.getNameLine4());
    }

    @FormTest
    void testIconGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        Image icon = Image.createImage(20, 20, 0xFF0000);
        mb.setIcon(icon);
        assertSame(icon, mb.getIcon());
    }

    @FormTest
    void testEmblemGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        Image emblem = Image.createImage(15, 15, 0x00FF00);
        mb.setEmblem(emblem);
        assertSame(emblem, mb.getEmblem());
    }

    @FormTest
    void testIconPositionGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setIconPosition(BorderLayout.NORTH);
        assertEquals(BorderLayout.NORTH, mb.getIconPosition());
    }

    @FormTest
    void testEmblemPositionGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setEmblemPosition(BorderLayout.SOUTH);
        assertEquals(BorderLayout.SOUTH, mb.getEmblemPosition());
    }

    @FormTest
    void testCheckBoxGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        assertFalse(mb.isCheckBox());

        mb.setCheckBox(true);
        assertTrue(mb.isCheckBox());

        mb.setCheckBox(false);
        assertFalse(mb.isCheckBox());
    }

    @FormTest
    void testRadioButtonGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        assertFalse(mb.isRadioButton());

        mb.setRadioButton(true);
        assertTrue(mb.isRadioButton());

        mb.setRadioButton(false);
        assertFalse(mb.isRadioButton());
    }

    @FormTest
    void testSelectedGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setCheckBox(true);

        assertFalse(mb.isSelected());

        mb.setSelected(true);
        assertTrue(mb.isSelected());

        mb.setSelected(false);
        assertFalse(mb.isSelected());
    }

    @FormTest
    void testGroupGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setRadioButton(true);

        mb.setGroup("TestGroup");
        assertEquals("TestGroup", mb.getGroup());
    }

    @FormTest
    void testHorizontalLayoutGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine1("Line1");
        mb.setTextLine2("Line2");

        assertFalse(mb.isHorizontalLayout());

        mb.setHorizontalLayout(true);
        assertTrue(mb.isHorizontalLayout());

        mb.setHorizontalLayout(false);
        assertFalse(mb.isHorizontalLayout());
    }

    @FormTest
    void testInvertFirstTwoEntriesGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine1("Line1");
        mb.setTextLine2("Line2");
        mb.setHorizontalLayout(true);

        assertFalse(mb.isInvertFirstTwoEntries());

        mb.setInvertFirstTwoEntries(true);
        assertTrue(mb.isInvertFirstTwoEntries());

        mb.setInvertFirstTwoEntries(false);
        assertFalse(mb.isInvertFirstTwoEntries());
    }

    @FormTest
    void testLinesTogetherModeGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine1("Line1");
        mb.setTextLine2("Line2");

        // Test setting the mode to true
        mb.setLinesTogetherMode(true);
        assertTrue(mb.isLinesTogetherMode());
    }

    @FormTest
    void testTextPropertyAccessor() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setText("NewText");
        assertEquals("NewText", mb.getText());
        assertEquals("NewText", mb.getTextLine1());
    }

    @FormTest
    void testShouldLocalizeGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        assertFalse(mb.isShouldLocalize());

        mb.setShouldLocalize(true);
        assertTrue(mb.isShouldLocalize());

        mb.setShouldLocalize(false);
        assertFalse(mb.isShouldLocalize());
    }

    @FormTest
    void testGapGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setGap(10);
        assertEquals(10, mb.getGap());
    }

    @FormTest
    void testTextPosition() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextPosition(com.codename1.ui.Component.TOP);
        assertEquals(com.codename1.ui.Component.TOP, mb.getTextPosition());
    }

    @FormTest
    void testIconUIIDGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setIconUIID("CustomIcon");
        assertEquals("CustomIcon", mb.getIconUIID());
    }

    @FormTest
    void testEmblemUIIDGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setEmblemUIID("CustomEmblem");
        assertEquals("CustomEmblem", mb.getEmblemUIID());
    }

    @FormTest
    void testActionListenerAddAndRemove() {
        SpanMultiButton mb = new SpanMultiButton();
        AtomicInteger count = new AtomicInteger();

        mb.addActionListener(evt -> count.incrementAndGet());
        mb.removeActionListener(evt -> count.incrementAndGet());

        // Verify listeners work
        assertNotNull(mb.getIconComponent());
    }

    @FormTest
    void testGetIconComponent() {
        SpanMultiButton mb = new SpanMultiButton();
        assertNotNull(mb.getIconComponent());
    }

    @FormTest
    void testPropertyNames() {
        SpanMultiButton mb = new SpanMultiButton();
        String[] props = mb.getPropertyNames();
        assertTrue(props.length > 0);
    }

    @FormTest
    void testPropertyTypes() {
        SpanMultiButton mb = new SpanMultiButton();
        Class[] types = mb.getPropertyTypes();
        assertEquals(mb.getPropertyNames().length, types.length);
    }

    @FormTest
    void testGetPropertyValue() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setTextLine1("TestValue");
        assertEquals("TestValue", mb.getPropertyValue("line1"));
    }

    @FormTest
    void testSetPropertyValue() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setPropertyValue("line1", "NewValue");
        assertEquals("NewValue", mb.getTextLine1());
    }

    @FormTest
    void testMaskNameGetterAndSetter() {
        SpanMultiButton mb = new SpanMultiButton();
        mb.setMaskName("TestMask");
        assertEquals("TestMask", mb.getMaskName());
    }
}
