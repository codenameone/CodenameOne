package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BrowserWindowTest extends UITestBase {

    @FormTest
    void nativeWindowDelegatesToImplementation() {
        Object nativeWindow = new Object();
        implementation.setNativeBrowserWindow(nativeWindow);

        BrowserWindow window = new BrowserWindow("https://start");

        ActionListener loadListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        };
        window.addLoadListener(loadListener);
        assertTrue(implementation.getNativeBrowserWindowOnLoadListener().contains(loadListener));

        window.removeLoadListener(loadListener);
        assertFalse(implementation.getNativeBrowserWindowOnLoadListener().contains(loadListener));

        window.setTitle("Docs");
        assertEquals("Docs", implementation.getNativeBrowserWindowTitle());

        window.setSize(640, 480);
        assertEquals(new Dimension(640, 480), implementation.getNativeBrowserWindowSize());

        ActionListener closeListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        };
        window.addCloseListener(closeListener);
        assertTrue(implementation.getNativeBrowserWindowCloseListener().contains(closeListener));

        assertFalse(implementation.isNativeBrowserWindowShowInvoked());
        window.show();
        assertTrue(implementation.isNativeBrowserWindowShowInvoked());

        assertFalse(implementation.isNativeBrowserWindowHideInvoked());
        assertFalse(implementation.isNativeBrowserWindowCleanupInvoked());
        window.close();
        assertTrue(implementation.isNativeBrowserWindowHideInvoked());
        assertTrue(implementation.isNativeBrowserWindowCleanupInvoked());

        window.removeCloseListener(closeListener);
        assertFalse(implementation.getNativeBrowserWindowCloseListener().contains(closeListener));
    }
}
