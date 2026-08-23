package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SwitchTest extends UITestBase {
    @BeforeEach
    void configureDisplay() {
        implementation.setBuiltinSoundsEnabled(false);
    }

    @FormTest
    void testSetValueFiresChangeOnlyOnStateChange() {
        Switch sw = new Switch();
        AtomicInteger changeCount = new AtomicInteger();
        sw.addChangeListener(evt -> changeCount.incrementAndGet());

        sw.setValue(true);
        sw.setValue(true);
        sw.setValue(false);

        assertEquals(2, changeCount.get());
    }

    @FormTest
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

    @FormTest
    void testPropertyInterface() {
        Switch sw = new Switch();
        assertArrayEquals(new String[]{"value"}, sw.getPropertyNames());
        assertEquals(Boolean.FALSE, sw.getPropertyValue("value"));

        sw.setPropertyValue("value", Boolean.TRUE);
        assertTrue(sw.isValue());
    }

    @FormTest
    void testToggleHelpersUpdateState() {
        Switch sw = new Switch();
        sw.setOn();
        assertTrue(sw.isOn());
        assertFalse(sw.isOff());

        sw.setOff();
        assertFalse(sw.isOn());
        assertTrue(sw.isOff());
    }

    @FormTest
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

    @FormTest
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

    /// The animations a form currently has registered.
    @SuppressWarnings("unchecked")
    private static java.util.List<com.codename1.ui.animations.Animation> registeredAnimations(
            com.codename1.ui.Form f) throws Exception {
        java.lang.reflect.Field fld =
                com.codename1.ui.Form.class.getDeclaredField("animatableComponents");
        fld.setAccessible(true);
        Object v = fld.get(f);
        return v == null ? new java.util.ArrayList<com.codename1.ui.animations.Animation>()
                : (java.util.List<com.codename1.ui.animations.Animation>) v;
    }

    @FormTest
    void aSwitchRemovedMidAnimationStillComesOffTheFormThatRegisteredIt() throws Exception {
        com.codename1.ui.Form f = new com.codename1.ui.Form("host",
                new com.codename1.ui.layouts.BorderLayout());
        Switch sw = new Switch();
        f.add(com.codename1.ui.layouts.BorderLayout.CENTER, sw);
        f.show();

        Method animateTo = Switch.class.getDeclaredMethod("animateTo",
                boolean.class, int.class, int.class, int.class);
        animateTo.setAccessible(true);
        animateTo.invoke(sw, true, 0, 10, 10);

        assertEquals(1, registeredAnimations(f).size(),
                "the switch registers its animation on the form hosting it");
        com.codename1.ui.animations.Animation a = registeredAnimations(f).get(0);

        // The switch goes away before the animation finishes. Resolving the top level
        // again at that point answers null, or answers a different one after a
        // reparent.
        f.removeComponent(sw);

        long deadline = System.currentTimeMillis() + 3000;
        while (!registeredAnimations(f).isEmpty() && System.currentTimeMillis() < deadline) {
            a.animate();
        }

        assertTrue(registeredAnimations(f).isEmpty(),
                "the animation has to come off the form that registered it: left on, that "
                        + "form never reports itself idle, so the event dispatch thread "
                        + "cannot sleep and the finished branch runs on every frame");
    }
}
