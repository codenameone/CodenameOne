package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.Style;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrowserWindowTest extends UITestBase {

    @Test
    void nativeWindowDelegatesToImplementation() {
        Object nativeWindow = new Object();
        when(implementation.createNativeBrowserWindow(any(String.class))).thenReturn(nativeWindow);
        BrowserWindow window = new BrowserWindow("http://start");
        verify(implementation).createNativeBrowserWindow("http://start");

        ActionListener loadListener = mock(ActionListener.class);
        window.addLoadListener(loadListener);
        verify(implementation).addNativeBrowserWindowOnLoadListener(nativeWindow, loadListener);
        window.removeLoadListener(loadListener);
        verify(implementation).removeNativeBrowserWindowOnLoadListener(nativeWindow, loadListener);

        window.setTitle("Docs");
        verify(implementation).nativeBrowserWindowSetTitle(nativeWindow, "Docs");
        window.setSize(320, 480);
        verify(implementation).nativeBrowserWindowSetSize(nativeWindow, 320, 480);

        ActionListener closeListener = mock(ActionListener.class);
        window.addCloseListener(closeListener);
        verify(implementation).nativeBrowserWindowAddCloseListener(nativeWindow, closeListener);
        window.removeCloseListener(closeListener);
        verify(implementation).nativeBrowserWindowRemoveCloseListener(nativeWindow, closeListener);

        window.show();
        verify(implementation).nativeBrowserWindowShow(nativeWindow);
        window.close();
        verify(implementation).nativeBrowserWindowHide(nativeWindow);
        verify(implementation).nativeBrowserWindowCleanup(nativeWindow);
    }

    @Test
    void fallbackModeUsesFormAndBrowserComponent() throws Exception {
        PeerComponent peer = mock(PeerComponent.class);
        when(peer.getUnselectedStyle()).thenReturn(new Style());
        when(peer.getStyle()).thenReturn(new Style());
        when(peer.toImage()).thenReturn(Image.createImage(1, 1));
        when(implementation.createNativeBrowserWindow(any(String.class))).thenReturn(null);
        when(implementation.createBrowserComponent(any(BrowserComponent.class))).thenReturn(peer);
        when(implementation.installMessageListener(peer)).thenReturn(true);

        final AtomicInteger backCalls = new AtomicInteger();
        Form previous = new Form() {
            @Override
            public void showBack() {
                backCalls.incrementAndGet();
            }
        };
        when(implementation.getCurrentForm()).thenReturn(previous);
        doNothing().when(implementation).onShow(any(Form.class));

        BrowserWindow window = new BrowserWindow("http://start");
        flushSerialCalls();

        Field webviewField = BrowserWindow.class.getDeclaredField("webview");
        webviewField.setAccessible(true);
        BrowserComponent webview = (BrowserComponent) webviewField.get(window);
        assertNotNull(webview);
        assertEquals("http://start", webview.getURL());

        final AtomicInteger loadEvents = new AtomicInteger();
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                loadEvents.incrementAndGet();
            }
        };
        window.addLoadListener(listener);
        webview.fireWebEvent("onLoad", new ActionEvent("http://start"));
        assertEquals(1, loadEvents.get());
        window.removeLoadListener(listener);
        webview.fireWebEvent("onLoad", new ActionEvent("http://start"));
        assertEquals(1, loadEvents.get());

        Field formField = BrowserWindow.class.getDeclaredField("form");
        formField.setAccessible(true);
        Form form = (Form) formField.get(window);
        assertNotNull(form);
        window.setTitle("Browser");
        assertEquals("Browser", form.getTitle());

        final AtomicInteger closeEvents = new AtomicInteger();
        window.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                closeEvents.incrementAndGet();
            }
        });
        window.close();
        assertEquals(1, closeEvents.get());
        assertEquals(1, backCalls.get());
    }

    @Test
    void evalRequestStoresJavascript() {
        BrowserWindow.EvalRequest request = new BrowserWindow.EvalRequest();
        request.setJS("let x = 1;");
        assertEquals("let x = 1;", request.getJS());
    }

    private void flushSerialCalls() {
        try {
            Display display = Display.getInstance();
            Field pendingField = Display.class.getDeclaredField("pendingSerialCalls");
            pendingField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Runnable> pending = (List<Runnable>) pendingField.get(display);

            Field runningField = Display.class.getDeclaredField("runningSerialCallsQueue");
            runningField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Deque<Runnable> running = (Deque<Runnable>) runningField.get(display);

            if ((pending == null || pending.isEmpty()) && (running == null || running.isEmpty())) {
                return;
            }

            Deque<Runnable> workQueue = new ArrayDeque<Runnable>();
            if (running != null && !running.isEmpty()) {
                workQueue.addAll(running);
                running.clear();
            }
            if (pending != null && !pending.isEmpty()) {
                workQueue.addAll(new ArrayList<Runnable>(pending));
                pending.clear();
            }

            while (!workQueue.isEmpty()) {
                Runnable job = workQueue.removeFirst();
                job.run();

                if (running != null && !running.isEmpty()) {
                    workQueue.addAll(running);
                    running.clear();
                }
                if (pending != null && !pending.isEmpty()) {
                    workQueue.addAll(new ArrayList<Runnable>(pending));
                    pending.clear();
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to drain Display serial calls", e);
        }
    }
}
