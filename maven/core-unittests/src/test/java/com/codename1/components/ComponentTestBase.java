package com.codename1.components;

import com.codename1.test.UITestBase;
import com.codename1.ui.Display;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Base class for component tests that provides utilities for working with the mocked display.
 */
abstract class ComponentTestBase extends UITestBase {
    /**
     * Processes any pending serial calls that were queued via {@link Display#callSerially(Runnable)}.
     */
    protected void flushSerialCalls() {
        try {
            Method m = Display.class.getDeclaredMethod("processSerialCalls");
            m.setAccessible(true);
            m.invoke(Display.getInstance());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to access Display.processSerialCalls", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke Display.processSerialCalls", e.getCause());
        }
    }
}
