package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CNTest extends UITestBase {

    @AfterEach
    void restoreFlags() {
        Component.revalidateOnStyleChange = true;
    }

    @Test
    void testSetPropertyUpdatesComponentFlags() {
        CN.setProperty("Component.revalidateOnStyleChange", "false");
        assertFalse(Component.revalidateOnStyleChange);

        CN.setProperty("Component.revalidateOnStyleChange", "true");
        assertTrue(Component.revalidateOnStyleChange);
    }

    @FormTest
    void testRequestFullScreenDelegatesToImplementation() {
        if(CN.isFullScreenSupported()) {
            assertTrue(CN.requestFullScreen());
            assertTrue(CN.isInFullScreenMode());
        }
    }

    @FormTest
    void testCanExecuteDelegatesToImplementation() {
        assertTrue(CN.canExecute("scheme:test"));
    }

    @FormTest
    void testPropertyAndDisplayAccessors() {
        implementation.putProperty("key", "value");
        assertEquals("value", CN.getProperty("key", "default"));
        assertEquals(implementation.getDisplayWidth(), CN.getDisplayWidth());
        assertEquals(implementation.getDisplayHeight(), CN.getDisplayHeight());
    }
}
