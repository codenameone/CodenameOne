package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.TestLogger;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.BrowserNavigationCallback;
import com.codename1.ui.plaf.Style;
import com.codename1.util.SuccessCallback;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BrowserComponentTest extends UITestBase {

    @FormTest
    void constructorCreatesPeerComponentWhenImplementationProvidesOne() throws Exception {
        DummyPeerComponent peer = new DummyPeerComponent();
        implementation.setBrowserComponent(peer);

        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();

        PeerComponent internal = getPrivateField(browser, "internal", PeerComponent.class);
        assertSame(peer, internal);
        assertEquals(1, browser.getComponentCount());
        assertSame(peer, browser.getComponentAt(0));
        assertEquals("BrowserComponent", browser.getUIID());
    }

    @FormTest
    void constructorKeepsPlaceholderWhenPeerUnavailable() throws Exception {
        TestLogger.install();
        try {
            implementation.setBrowserComponent(null);

            BrowserComponent browser = new BrowserComponent();
            flushSerialCalls();

            PeerComponent internal = getPrivateField(browser, "internal", PeerComponent.class);
            assertNull(internal, "Internal peer should remain null when implementation cannot create it");
            Component placeholder = getPrivateField(browser, "placeholder", Component.class);
            assertEquals(1, browser.getComponentCount());
            assertSame(placeholder, browser.getComponentAt(0));
            assertEquals(1, TestLogger.getPrinted().size());
            assertTrue(TestLogger.getPrinted().get(0).contains("Failed to create browser component."));
        } finally {
            TestLogger.remove();
        }
    }

    @FormTest
    void webEventListenersUpdateReadyState() throws Exception {
        DummyPeerComponent peer = new DummyPeerComponent();
        implementation.setBrowserComponent(peer);

        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();

        final List<ActionEvent> fired = new ArrayList<ActionEvent>();
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.add(evt);
            }
        };
        browser.addWebEventListener(BrowserComponent.onLoad, listener);
        browser.fireWebEvent(BrowserComponent.onLoad, new ActionEvent("https://example.com"));

        assertEquals(1, fired.size(), "Listener should be invoked once");
        Field readyField = BrowserComponent.class.getDeclaredField("ready");
        readyField.setAccessible(true);
        assertTrue(readyField.getBoolean(browser), "Ready flag should be set after onLoad");

        browser.removeWebEventListener(BrowserComponent.onLoad, listener);
        Hashtable listeners = getPrivateField(browser, "listeners", Hashtable.class);
        assertFalse(listeners.containsKey(BrowserComponent.onLoad), "Listener map should be cleaned when last listener removed");
    }

    @FormTest
    void readyCallbackInvokedOnStartAndImmediatelyAfterReady() throws Exception {
        DummyPeerComponent peer = new DummyPeerComponent();
        implementation.setBrowserComponent(peer);

        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();

        final AtomicInteger callbacks = new AtomicInteger();
        browser.ready(new SuccessCallback<BrowserComponent>() {
            public void onSucess(BrowserComponent value) {
                callbacks.incrementAndGet();
            }
        });

        Hashtable listeners = getPrivateField(browser, "listeners", Hashtable.class);
        assertTrue(listeners.containsKey(BrowserComponent.onStart), "ready(callback) should register an onStart listener");

        browser.fireWebEvent(BrowserComponent.onStart, new ActionEvent("start"));
        assertEquals(1, callbacks.get(), "Callback should be invoked when onStart fires");

        browser.fireWebEvent(BrowserComponent.onLoad, new ActionEvent("load"));
        browser.ready(new SuccessCallback<BrowserComponent>() {
            public void onSucess(BrowserComponent value) {
                callbacks.incrementAndGet();
            }
        });
        assertEquals(2, callbacks.get(), "Callback registered after ready should run immediately");
    }

    @FormTest
    void postMessageFallsBackToJavaScriptWhenNativePostFails() throws Exception {
        DummyPeerComponent peer = new DummyPeerComponent();
        implementation.setBrowserComponent(peer);

        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();

        browser.postMessage("payload", "*");

        assertFalse(implementation.getBrowserExecuted().isEmpty(), "Fallback should execute JavaScript when native post fails");
        boolean found = false;
        for (String value : implementation.getBrowserExecuted()) {
            if (value.contains("window.postMessage")) {
                found = true;
                assertTrue(value.contains("payload"));
                assertTrue(value.contains("*"));
                break;
            }
        }
        assertTrue(found, "Generated script should invoke window.postMessage");
    }

    @FormTest
    void fireBrowserNavigationCallbacksDispatchesReturnValues() throws Exception {
        DummyPeerComponent peer = new DummyPeerComponent();
        implementation.setBrowserComponent(peer);

        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();

        final List<BrowserComponent.JSRef> results = new ArrayList<BrowserComponent.JSRef>();
        SuccessCallback<BrowserComponent.JSRef> callback = new SuccessCallback<BrowserComponent.JSRef>() {
            public void onSucess(BrowserComponent.JSRef value) {
                results.add(value);
            }
        };
        browser.addJSCallback("var ignore = callback;", callback);

        Hashtable returnValueCallbacks = getPrivateField(browser, "returnValueCallbacks", Hashtable.class);
        Enumeration keys = returnValueCallbacks.keys();
        assertTrue(keys.hasMoreElements(), "Callback should be registered with an identifier");
        Integer id = (Integer) keys.nextElement();

        browser.addBrowserNavigationCallback(new BrowserNavigationCallback() {
            public boolean shouldNavigate(String url) {
                return url.indexOf("block") == -1;
            }
        });

        String json = "{\"callbackId\":" + id.intValue() + ",\"value\":\"message\",\"type\":\"string\"}";
        String encoded = URLEncoder.encode(json, "UTF-8");
        boolean shouldNavigate = browser.fireBrowserNavigationCallbacks("https://example.com/block/!cn1return/" + encoded);
        flushSerialCalls();

        assertFalse(shouldNavigate, "Navigation should be blocked when a callback vetoes the URL");
        assertEquals(1, results.size(), "Callback should receive one result");
        BrowserComponent.JSRef value = results.get(0);
        assertEquals("message", value.getValue());
        assertEquals(BrowserComponent.JSType.STRING, value.getJSType());
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object instance, String name, Class<T> type) throws Exception {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(instance);
    }

    private static class DummyPeerComponent extends PeerComponent {
        DummyPeerComponent() {
            super(new Object());
            Style s = getUnselectedStyle();
            s.setMargin(0, 0, 0, 0);
        }
    }
}
