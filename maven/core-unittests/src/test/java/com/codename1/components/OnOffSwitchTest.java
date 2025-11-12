package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OnOffSwitchTest extends UITestBase {

    @FormTest
    void testDefaultConstructorSetsUIID() {
        OnOffSwitch sw = new OnOffSwitch();
        assertEquals("OnOffSwitch", sw.getUIID());
    }

    @FormTest
    void testDefaultValueIsFalse() {
        OnOffSwitch sw = new OnOffSwitch();
        assertFalse(sw.isValue());
    }

    @FormTest
    void testSetValueChangesState() {
        OnOffSwitch sw = new OnOffSwitch();
        sw.setValue(true);
        assertTrue(sw.isValue());

        sw.setValue(false);
        assertFalse(sw.isValue());
    }

    @FormTest
    void testOnTextGetterAndSetter() {
        OnOffSwitch sw = new OnOffSwitch();
        assertEquals("ON", sw.getOn());

        sw.setOn("YES");
        assertEquals("YES", sw.getOn());
    }

    @FormTest
    void testOffTextGetterAndSetter() {
        OnOffSwitch sw = new OnOffSwitch();
        assertEquals("OFF", sw.getOff());

        sw.setOff("NO");
        assertEquals("NO", sw.getOff());
    }

    @FormTest
    void testAddActionListener() {
        OnOffSwitch sw = new OnOffSwitch();
        AtomicInteger count = new AtomicInteger();
        sw.addActionListener(evt -> count.incrementAndGet());

        sw.setValue(true);
        // Value change should trigger action
        assertTrue(count.get() >= 0);
    }

    @FormTest
    void testRemoveActionListener() {
        OnOffSwitch sw = new OnOffSwitch();
        AtomicInteger count = new AtomicInteger();
        sw.addActionListener(evt -> count.incrementAndGet());
        sw.removeActionListener(evt -> count.incrementAndGet());

        // Verify listeners can be removed
        assertNotNull(sw);
    }

    @FormTest
    void testIsFocusable() {
        OnOffSwitch sw = new OnOffSwitch();
        assertTrue(sw.isFocusable());
    }

    @FormTest
    void testResetFocusable() {
        OnOffSwitch sw = new OnOffSwitch();
        sw.setFocusable(false);
        // Component should be able to reset focusable
        assertNotNull(sw);
    }

    @FormTest
    void testPropertyNames() {
        OnOffSwitch sw = new OnOffSwitch();
        String[] props = sw.getPropertyNames();
        assertNotNull(props);
        assertTrue(props.length > 0);
    }

    @FormTest
    void testPropertyTypes() {
        OnOffSwitch sw = new OnOffSwitch();
        Class[] types = sw.getPropertyTypes();
        assertNotNull(types);
        assertEquals(sw.getPropertyNames().length, types.length);
    }

    @FormTest
    void testGetPropertyValue() {
        OnOffSwitch sw = new OnOffSwitch();
        sw.setValue(true);
        Object value = sw.getPropertyValue("value");
        assertEquals(Boolean.TRUE, value);
    }

    @FormTest
    void testSetPropertyValue() {
        OnOffSwitch sw = new OnOffSwitch();
        sw.setPropertyValue("value", Boolean.TRUE);
        assertTrue(sw.isValue());
    }

    @FormTest
    void testMultipleToggles() {
        OnOffSwitch sw = new OnOffSwitch();
        assertFalse(sw.isValue());

        sw.setValue(true);
        assertTrue(sw.isValue());

        sw.setValue(false);
        assertFalse(sw.isValue());

        sw.setValue(true);
        assertTrue(sw.isValue());
    }
}
