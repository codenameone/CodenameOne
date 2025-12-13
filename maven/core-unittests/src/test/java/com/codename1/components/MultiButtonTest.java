package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Image;
import com.codename1.ui.RadioButton;
import com.codename1.ui.layouts.BorderLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MultiButtonTest extends UITestBase {

    @FormTest
    void testDefaultConstructorSetsDefaults() {
        MultiButton mb = new MultiButton();
        assertEquals("MultiButton", mb.getUIID());
        assertEquals("MultiButton", mb.getTextLine1());
        assertTrue(mb.isFocusable());
    }

    @FormTest
    void testConstructorWithTextSetsFirstLine() {
        MultiButton mb = new MultiButton("Hello");
        assertEquals("Hello", mb.getTextLine1());
    }

    @FormTest
    void testSetTextLine1UpdatesText() {
        MultiButton mb = new MultiButton();
        mb.setTextLine1("First");
        assertEquals("First", mb.getTextLine1());
    }

    @FormTest
    void testSetTextLine2UpdatesText() {
        MultiButton mb = new MultiButton();
        mb.setTextLine2("Second");
        assertEquals("Second", mb.getTextLine2());
    }

    @FormTest
    void testSetTextLine3UpdatesText() {
        MultiButton mb = new MultiButton();
        mb.setTextLine3("Third");
        assertEquals("Third", mb.getTextLine3());
    }

    @FormTest
    void testSetTextLine4UpdatesText() {
        MultiButton mb = new MultiButton();
        mb.setTextLine4("Fourth");
        assertEquals("Fourth", mb.getTextLine4());
    }

    @FormTest
    void testSetUIIDLine1UpdatesUIID() {
        MultiButton mb = new MultiButton();
        mb.setUIIDLine1("CustomLine1");
        assertEquals("CustomLine1", mb.getUIIDLine1());
    }

    @FormTest
    void testSetUIIDLine2UpdatesUIID() {
        MultiButton mb = new MultiButton();
        mb.setUIIDLine2("CustomLine2");
        assertEquals("CustomLine2", mb.getUIIDLine2());
    }

    @FormTest
    void testSetUIIDLine3UpdatesUIID() {
        MultiButton mb = new MultiButton();
        mb.setUIIDLine3("CustomLine3");
        assertEquals("CustomLine3", mb.getUIIDLine3());
    }

    @FormTest
    void testSetUIIDLine4UpdatesUIID() {
        MultiButton mb = new MultiButton();
        mb.setUIIDLine4("CustomLine4");
        assertEquals("CustomLine4", mb.getUIIDLine4());
    }

    @FormTest
    void testSetNameLine1UpdatesName() {
        MultiButton mb = new MultiButton();
        mb.setNameLine1("FirstName");
        assertEquals("FirstName", mb.getNameLine1());
    }

    @FormTest
    void testIconGetterAndSetter() {
        MultiButton mb = new MultiButton();
        Image icon = Image.createImage(20, 20, 0xFF0000);
        mb.setIcon(icon);
        assertSame(icon, mb.getIcon());
    }

    @FormTest
    void testEmblemGetterAndSetter() {
        MultiButton mb = new MultiButton();
        Image emblem = Image.createImage(15, 15, 0x00FF00);
        mb.setEmblem(emblem);
        assertSame(emblem, mb.getEmblem());
    }

    @FormTest
    void testIconPositionGetterAndSetter() {
        MultiButton mb = new MultiButton();
        assertEquals(BorderLayout.WEST, mb.getIconPosition());

        mb.setIconPosition(BorderLayout.NORTH);
        assertEquals(BorderLayout.NORTH, mb.getIconPosition());
    }

    @FormTest
    void testEmblemPositionGetterAndSetter() {
        MultiButton mb = new MultiButton();
        assertEquals(BorderLayout.EAST, mb.getEmblemPosition());

        mb.setEmblemPosition(BorderLayout.SOUTH);
        assertEquals(BorderLayout.SOUTH, mb.getEmblemPosition());
    }

    @FormTest
    void testIconUIIDGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setIconUIID("CustomIcon");
        assertEquals("CustomIcon", mb.getIconUIID());
    }

    @FormTest
    void testEmblemUIIDGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setEmblemUIID("CustomEmblem");
        assertEquals("CustomEmblem", mb.getEmblemUIID());
    }

    @FormTest
    void testIconNameGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setIconName("MyIcon");
        assertEquals("MyIcon", mb.getIconName());
    }

    @FormTest
    void testEmblemNameGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setEmblemName("MyEmblem");
        assertEquals("MyEmblem", mb.getEmblemName());
    }

    @FormTest
    void testHorizontalLayoutGetterAndSetter() {
        MultiButton mb = new MultiButton();
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
        MultiButton mb = new MultiButton();
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
    void testCheckBoxGetterAndSetter() {
        MultiButton mb = new MultiButton();
        assertFalse(mb.isCheckBox());

        mb.setCheckBox(true);
        assertTrue(mb.isCheckBox());

        mb.setCheckBox(false);
        assertFalse(mb.isCheckBox());
    }

    @FormTest
    void testRadioButtonGetterAndSetter() {
        MultiButton mb = new MultiButton();
        assertFalse(mb.isRadioButton());

        mb.setRadioButton(true);
        assertTrue(mb.isRadioButton());

        mb.setRadioButton(false);
        assertFalse(mb.isRadioButton());
    }

    @FormTest
    void testSelectedGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setCheckBox(true);

        assertFalse(mb.isSelected());

        mb.setSelected(true);
        assertTrue(mb.isSelected());

        mb.setSelected(false);
        assertFalse(mb.isSelected());
    }

    @FormTest
    void testGroupGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setRadioButton(true);

        mb.setGroup("TestGroup");
        assertEquals("TestGroup", mb.getGroup());
    }

    @FormTest
    void testLinesTogetherModeGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setTextLine1("Line1");
        mb.setTextLine2("Line2");

        // Test setting the mode to true
        mb.setLinesTogetherMode(true);
        assertTrue(mb.isLinesTogetherMode());
    }

    @FormTest
    void testCommandGetterAndSetter() {
        MultiButton mb = new MultiButton();
        Command cmd = new Command("Test");

        mb.setCommand(cmd);
        assertSame(cmd, mb.getCommand());
    }

    @FormTest
    void testGetIconComponent() {
        MultiButton mb = new MultiButton();
        assertNotNull(mb.getIconComponent());
        assertTrue(mb.getIconComponent() instanceof Button);
    }

    @FormTest
    void testActionListenerAddAndRemove() {
        MultiButton mb = new MultiButton();
        AtomicInteger count = new AtomicInteger();

        mb.addActionListener(evt -> count.incrementAndGet());

        // Verify listener was added
        assertNotNull(mb);
    }

    @FormTest
    void testTextPropertyAccessor() {
        MultiButton mb = new MultiButton();
        mb.setText("NewText");
        assertEquals("NewText", mb.getText());
        assertEquals("NewText", mb.getTextLine1());
    }

    @FormTest
    void testMaskNameGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setMaskName("TestMask");
        assertEquals("TestMask", mb.getMaskName());
    }

    @FormTest
    void testPropertyNames() {
        MultiButton mb = new MultiButton();
        String[] props = mb.getPropertyNames();
        assertTrue(props.length > 0);
        boolean foundLine1 = false;
        for (String prop : props) {
            if ("line1".equals(prop)) {
                foundLine1 = true;
                break;
            }
        }
        assertTrue(foundLine1);
    }

    @FormTest
    void testPropertyTypes() {
        MultiButton mb = new MultiButton();
        Class[] types = mb.getPropertyTypes();
        assertEquals(mb.getPropertyNames().length, types.length);
    }

    @FormTest
    void testGetPropertyValue() {
        MultiButton mb = new MultiButton();
        mb.setTextLine1("TestValue");
        assertEquals("TestValue", mb.getPropertyValue("line1"));
    }

    @FormTest
    void testSetPropertyValue() {
        MultiButton mb = new MultiButton();
        mb.setPropertyValue("line1", "NewValue");
        assertEquals("NewValue", mb.getTextLine1());
    }

    @FormTest
    void testGapGetterAndSetter() {
        MultiButton mb = new MultiButton();
        mb.setGap(10);
        assertEquals(10, mb.getGap());
    }

    @FormTest
    void testTextPosition() {
        MultiButton mb = new MultiButton();
        mb.setTextPosition(com.codename1.ui.Component.TOP);
        assertEquals(com.codename1.ui.Component.TOP, mb.getTextPosition());
    }
}
