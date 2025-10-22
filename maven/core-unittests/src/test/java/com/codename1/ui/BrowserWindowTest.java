package com.codename1.ui;

import com.codename1.components.ComponentTestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrowserWindowTest extends ComponentTestBase {

    @Test
    void nativeWindowDelegatesToImplementation() {
        Object nativeWindow = new Object();
        when(implementation.createNativeBrowserWindow("https://start"))
                .thenReturn(nativeWindow);

        BrowserWindow window = new BrowserWindow("https://start");

        ActionListener loadListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        };
        window.addLoadListener(loadListener);
        verify(implementation).addNativeBrowserWindowOnLoadListener(nativeWindow, loadListener);

        window.removeLoadListener(loadListener);
        verify(implementation).removeNativeBrowserWindowOnLoadListener(nativeWindow, loadListener);

        window.setTitle("Docs");
        verify(implementation).nativeBrowserWindowSetTitle(nativeWindow, "Docs");

        window.setSize(640, 480);
        verify(implementation).nativeBrowserWindowSetSize(nativeWindow, 640, 480);

        ActionListener closeListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        };
        window.addCloseListener(closeListener);
        verify(implementation).nativeBrowserWindowAddCloseListener(nativeWindow, closeListener);

        window.show();
        verify(implementation).nativeBrowserWindowShow(nativeWindow);

        window.close();
        verify(implementation).nativeBrowserWindowHide(nativeWindow);
        verify(implementation).nativeBrowserWindowCleanup(nativeWindow);

        window.removeCloseListener(closeListener);
        verify(implementation).nativeBrowserWindowRemoveCloseListener(nativeWindow, closeListener);
    }

    @Test
    void fallbackUsesEmbeddedBrowserWhenNativeUnavailable() throws Exception {
        when(implementation.createNativeBrowserWindow(anyString())).thenReturn(null);
        when(implementation.createBrowserComponent(any())).thenReturn(new DummyPeerComponent());
        doNothing().when(implementation).setBrowserURL(any(PeerComponent.class), anyString());
        doNothing().when(implementation).browserExecute(any(PeerComponent.class), anyString());

        TrackingForm previous = new TrackingForm();
        when(implementation.getCurrentForm()).thenReturn(previous);

        BrowserWindow window = new BrowserWindow("https://fallback");
        flushSerialCalls();

        Form form = getPrivateField(window, "form", Form.class);
        assertNotNull(form, "Fallback form should be created when native window is unavailable");
        assertEquals("", form.getTitle());

        BrowserComponent webview = getPrivateField(window, "webview", BrowserComponent.class);
        assertNotNull(webview, "Embedded BrowserComponent should exist");
        assertEquals("https://fallback", webview.getURL());

        ActionListener loadListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        };
        window.addLoadListener(loadListener);
        Hashtable listeners = getPrivateField(webview, "listeners", Hashtable.class);
        assertTrue(listeners.containsKey(BrowserComponent.onLoad));

        window.removeLoadListener(loadListener);
        assertFalse(listeners.containsKey(BrowserComponent.onLoad));

        window.setTitle("Support");
        assertEquals("Support", form.getTitle());

        final AtomicInteger closeCount = new AtomicInteger();
        window.addCloseListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeCount.incrementAndGet();
            }
        });
        window.close();
        assertEquals(1, closeCount.get());
        assertTrue(previous.showBackInvoked, "Fallback close should navigate back to the previous form");

        window.close();
        assertEquals(1, closeCount.get(), "Closing twice should not trigger listeners again");
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static class TrackingForm extends Form {
        boolean showBackInvoked;

        @Override
        public void showBack() {
            showBackInvoked = true;
        }
    }

    private static class DummyPeerComponent extends PeerComponent {
        DummyPeerComponent() {
            super(new Object());
            Style style = getUnselectedStyle();
            style.setMargin(0, 0, 0, 0);
        }
    }
}
