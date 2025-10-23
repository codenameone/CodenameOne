package com.codename1.ui;

import com.codename1.test.UITestBase;
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

    @Test
    void testRequestFullScreenDelegatesToImplementation() {
        when(implementation.requestFullScreen()).thenReturn(true);

        assertTrue(CN.requestFullScreen());
        verify(implementation).requestFullScreen();
    }

    @Test
    void testCanExecuteDelegatesToImplementation() {
        when(implementation.canExecute("scheme:test")).thenReturn(Boolean.TRUE);

        assertTrue(CN.canExecute("scheme:test"));
        verify(implementation).canExecute("scheme:test");
    }

    @Test
    void testPropertyAndDisplayAccessors() {
        when(implementation.getProperty(eq("key"), anyString())).thenReturn("value");

        assertEquals("value", CN.getProperty("key", "default"));
        assertEquals(implementation.getDisplayWidth(), CN.getDisplayWidth());
        assertEquals(implementation.getDisplayHeight(), CN.getDisplayHeight());
    }
}
