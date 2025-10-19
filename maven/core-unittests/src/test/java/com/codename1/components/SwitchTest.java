package com.codename1.components;

import com.codename1.test.UITestBase;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class SwitchTest extends UITestBase {
    @BeforeEach
    void configureDisplay() {
        when(implementation.isBuiltinSoundsEnabled()).thenReturn(false);
        doNothing().when(implementation).playBuiltinSound(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void testSetValueFiresChangeOnlyOnStateChange() {
        Switch sw = new Switch();
        AtomicInteger changeCount = new AtomicInteger();
        sw.addChangeListener(evt -> changeCount.incrementAndGet());

        sw.setValue(true);
        sw.setValue(true);
        sw.setValue(false);

        assertEquals(2, changeCount.get());
    }

    @Test
    void testSetValueWithFireEventTriggersAction() throws Exception {
        Switch sw = new Switch();
        AtomicInteger changeCount = new AtomicInteger();
        AtomicInteger actionCount = new AtomicInteger();

        ActionListener changeListener = evt -> changeCount.incrementAndGet();
        ActionListener actionListener = evt -> actionCount.incrementAndGet();
        sw.addChangeListener(changeListener);
        sw.addActionListener(actionListener);

        Method setValue = Switch.class.getDeclaredMethod("setValue", boolean.class, boolean.class);
        setValue.setAccessible(true);
        setValue.invoke(sw, true, true);
        setValue.invoke(sw, true, true);
        setValue.invoke(sw, false, true);

        assertEquals(2, changeCount.get());
        assertEquals(2, actionCount.get());
    }

    @Test
    void testPropertyInterface() {
        Switch sw = new Switch();
        assertArrayEquals(new String[]{"value"}, sw.getPropertyNames());
        assertEquals(Boolean.FALSE, sw.getPropertyValue("value"));

        sw.setPropertyValue("value", Boolean.TRUE);
        assertTrue(sw.isValue());
    }

    @Test
    void testToggleHelpersUpdateState() {
        Switch sw = new Switch();
        sw.setOn();
        assertTrue(sw.isOn());
        assertFalse(sw.isOff());

        sw.setOff();
        assertFalse(sw.isOn());
        assertTrue(sw.isOff());
    }

    @Test
    void testReleasableComponentDefaults() {
        Switch sw = new Switch();
        assertFalse(sw.isAutoRelease());
        assertEquals(0, sw.getReleaseRadius());
        sw.setAutoRelease(true);
        sw.setReleaseRadius(5);
        sw.setReleased();
        assertFalse(sw.isAutoRelease());
        assertEquals(0, sw.getReleaseRadius());
    }

    @Test
    void testListenersCanBeRemoved() {
        Switch sw = new Switch();
        AtomicInteger actionCount = new AtomicInteger();
        ActionListener listener = evt -> actionCount.incrementAndGet();
        sw.addActionListener(listener);
        sw.removeActionListener(listener);

        Method fire = getFireActionMethod();
        try {
            fire.invoke(sw);
        } catch (Exception e) {
            fail(e);
        }
        assertEquals(0, actionCount.get());
    }

    private Method getFireActionMethod() {
        try {
            Method fire = Switch.class.getDeclaredMethod("fireActionEvent");
            fire.setAccessible(true);
            return fire;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
