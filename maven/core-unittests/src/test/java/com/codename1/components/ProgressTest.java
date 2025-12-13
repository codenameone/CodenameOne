package com.codename1.components;

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class ProgressTest extends UITestBase {

    @FormTest
    void testConstructorInitializesWithRequest() {
        ConnectionRequest request = new ConnectionRequest();
        Progress progress = new Progress("Loading", request);
        assertEquals("Loading", progress.getTitle());
        assertNotNull(progress);
    }

    @FormTest
    void testConstructorWithPercentageFlag() {
        ConnectionRequest request = new ConnectionRequest();
        Progress progress = new Progress("Loading", request, true);
        assertEquals("Loading", progress.getTitle());
        assertNotNull(progress);
    }

    @FormTest
    void testDisposeOnCompletionGetterAndSetter() {
        ConnectionRequest request = new ConnectionRequest();
        Progress progress = new Progress("Loading", request);

        assertFalse(progress.isDisposeOnCompletion());

        progress.setDisposeOnCompletion(true);
        assertTrue(progress.isDisposeOnCompletion());

        progress.setDisposeOnCompletion(false);
        assertFalse(progress.isDisposeOnCompletion());
    }

    @FormTest
    void testAutoShowGetterAndSetter() {
        ConnectionRequest request = new ConnectionRequest();
        Progress progress = new Progress("Loading", request);

        assertFalse(progress.isAutoShow());

        progress.setAutoShow(true);
        assertTrue(progress.isAutoShow());

        progress.setAutoShow(false);
        assertFalse(progress.isAutoShow());
    }

    @FormTest
    void testDispose() {
        ConnectionRequest request = new ConnectionRequest();
        Progress progress = new Progress("Loading", request);

        progress.dispose();
        // Verify disposal doesn't throw exception
        assertNotNull(progress);
    }
}
