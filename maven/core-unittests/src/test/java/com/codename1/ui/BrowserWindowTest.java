package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import org.junit.jupiter.api.Test;

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

    @FormTest
    public void testEvalRequest() {
        BrowserWindow.EvalRequest request = new BrowserWindow.EvalRequest();
        request.setJS("alert('Hello');");
        assertEquals("alert('Hello');", request.getJS());
    }

    @FormTest
    void fallbackWindowUsesBrowserForm() {
        implementation.setNativeBrowserWindow(null);

        Form backForm = Display.getInstance().getCurrent();
        assertNotNull(backForm);

        BrowserWindow window = new BrowserWindow("https://example.com/start");
        AtomicInteger closeCount = new AtomicInteger();
        window.addCloseListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeCount.incrementAndGet();
            }
        });

        window.show();
        Form browserForm = Display.getInstance().getCurrent();
        assertNotSame(backForm, browserForm);

        window.setTitle("Docs");
        assertEquals("Docs", browserForm.getTitle());

        window.close();
        assertEquals(1, closeCount.get());
        assertSame(backForm, Display.getInstance().getCurrent());

        window.close();
        assertEquals(1, closeCount.get());
    }
}
