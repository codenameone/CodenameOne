package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.BrowserNavigationCallback;
import com.codename1.ui.plaf.Style;
import com.codename1.util.AsyncResource;
import com.codename1.util.Callback;
import com.codename1.util.SuccessCallback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrowserComponentTest extends UITestBase {

    private PeerComponent peer;

    @BeforeEach
    void preparePeer() {
        peer = mock(PeerComponent.class);
        when(peer.getUnselectedStyle()).thenReturn(new Style());
        when(peer.getStyle()).thenReturn(new Style());
        when(peer.toImage()).thenReturn(Image.createImage(1, 1));
        when(implementation.createBrowserComponent(any(BrowserComponent.class))).thenReturn(peer);
    }

    @Test
    void constructorReplacesPlaceholderWithPeerComponent() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        assertEquals(1, browser.getComponentCount());
        assertSame(peer, browser.getComponentAt(0));
    }

    @Test
    void addWebEventListenerFiresOnEvent() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        final AtomicInteger counter = new AtomicInteger();
        browser.addWebEventListener(BrowserComponent.onStart, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                counter.incrementAndGet();
            }
        });
        browser.fireWebEvent(BrowserComponent.onStart, new ActionEvent("start"));
        assertEquals(1, counter.get());
    }

    @Test
    void readyCallbackInvokedWhenStartEventFired() {
        BrowserComponent browser = new BrowserComponent();
        final AtomicBoolean invoked = new AtomicBoolean();
        browser.ready(new SuccessCallback<BrowserComponent>() {
            @Override
            public void onSucess(BrowserComponent value) {
                invoked.set(true);
                assertSame(browser, value);
            }
        });
        browser.fireWebEvent(BrowserComponent.onStart, new ActionEvent("start"));
        assertTrue(invoked.get());
    }

    @Test
    void readyPromiseCompletesAfterStartEvent() {
        BrowserComponent browser = new BrowserComponent();
        AsyncResource<BrowserComponent> promise = browser.ready(0);
        assertFalse(promise.isDone());
        browser.fireWebEvent(BrowserComponent.onStart, new ActionEvent("start"));
        assertTrue(promise.isDone());
        assertSame(browser, promise.get());
    }

    @Test
    void executeWithParametersInjectsValues() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        browser.execute("window.call(${0}, ${1});", new Object[]{"value", Integer.valueOf(3)});
        ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
        verify(implementation).browserExecute(any(PeerComponent.class), script.capture());
        String js = script.getValue();
        assertTrue(js.contains("\"value\""));
        assertTrue(js.contains("3"));
    }

    @Test
    void injectParametersSupportsProxiesAndReferences() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        BrowserComponent.JSProxy proxy = browser.createJSProxy("window");
        BrowserComponent.JSRef ref = new BrowserComponent.JSRef("5", "number");
        String expression = BrowserComponent.injectParameters("call(${0}, ${1}, ${2})", "text", proxy, ref);
        assertEquals("call(\"text\", window, 5)", expression);
    }

    @Test
    void fireBrowserNavigationCallbacksReturnsFalseWhenCallbackRejects() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        browser.addBrowserNavigationCallback(new BrowserNavigationCallback() {
            @Override
            public boolean shouldNavigate(String url) {
                return !url.contains("blocked");
            }
        });
        assertFalse(browser.fireBrowserNavigationCallbacks("https://example.com/blocked"));
        assertTrue(browser.fireBrowserNavigationCallbacks("https://example.com/allowed"));
    }

    @Test
    void fireBrowserNavigationCallbacksDeliversReturnValuesOnEdt() throws Exception {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        final AtomicReference<BrowserComponent.JSRef> callbackValue = new AtomicReference<BrowserComponent.JSRef>();
        SuccessCallback<BrowserComponent.JSRef> callback = new SuccessCallback<BrowserComponent.JSRef>() {
            @Override
            public void onSucess(BrowserComponent.JSRef value) {
                callbackValue.set(value);
            }
        };
        int id = registerReturnValueCallback(browser, callback);
        String payload = "{\"callbackId\":" + id + ",\"value\":\"42\",\"type\":\"number\"}";
        String encoded = URLEncoder.encode(payload, "UTF-8");
        boolean result = browser.fireBrowserNavigationCallbacks("https://example.com/!cn1return/" + encoded);
        assertFalse(result);
        flushSerialCalls();
        BrowserComponent.JSRef value = callbackValue.get();
        assertNotNull(value);
        assertEquals("42", value.getValue());
        assertEquals(BrowserComponent.JSType.NUMBER, value.getJSType());
    }

    @Test
    void fireBrowserNavigationCallbacksRunsSynchronouslyWhenNotOnEdt() throws Exception {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        browser.setFireCallbacksOnEdt(false);
        final AtomicReference<BrowserComponent.JSRef> callbackValue = new AtomicReference<BrowserComponent.JSRef>();
        SuccessCallback<BrowserComponent.JSRef> callback = new SuccessCallback<BrowserComponent.JSRef>() {
            @Override
            public void onSucess(BrowserComponent.JSRef value) {
                callbackValue.set(value);
            }
        };
        int id = registerReturnValueCallback(browser, callback);
        String payload = "{\"callbackId\":" + id + ",\"value\":\"true\",\"type\":\"boolean\"}";
        String encoded = URLEncoder.encode(payload, "UTF-8");
        boolean result = browser.fireBrowserNavigationCallbacks("https://example.com/!cn1return/" + encoded);
        assertFalse(result);
        assertNotNull(callbackValue.get());
        assertTrue(callbackValue.get().getBoolean());
    }

    @Test
    void captureScreenshotUsesImplementationResult() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        AsyncResource<Image> resource = new AsyncResource<Image>();
        resource.complete(Image.createImage(5, 5));
        when(implementation.captureBrowserScreenshot(peer)).thenReturn(resource);
        AsyncResource<Image> result = browser.captureScreenshot();
        assertSame(resource, result);
    }

    @Test
    void createDataUriEncodesBytes() {
        byte[] data = new byte[]{0x01, 0x02, 0x03};
        String uri = BrowserComponent.createDataURI(data, "image/png");
        assertTrue(uri.startsWith("data:image/png;base64,"));
        assertTrue(uri.length() > "data:image/png;base64,".length());
    }

    @Test
    void setPropertyQueuedUntilPeerReady() {
        BrowserComponent browser = new BrowserComponent();
        browser.setProperty("foo", "bar");
        verify(implementation, never()).setBrowserProperty(any(PeerComponent.class), anyString(), any());
        flushSerialCalls();
        verify(implementation).setBrowserProperty(peer, "foo", "bar");
    }

    @Test
    void putClientPropertyPropagatesToPeer() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        browser.putClientProperty("HTML5Peer.removeOnDeinitialize", Boolean.TRUE);
        verify(peer).putClientProperty("HTML5Peer.removeOnDeinitialize", Boolean.TRUE);
    }

    @Test
    void setDebugModeTogglesClientProperties() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        browser.setDebugMode(true);
        assertTrue(browser.isDebugMode());
        browser.setDebugMode(false);
        assertFalse(browser.isDebugMode());
    }

    @Test
    void captureScreenshotFallsBackToComponentImage() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        when(implementation.captureBrowserScreenshot(peer)).thenReturn(null);
        AsyncResource<Image> resource = browser.captureScreenshot();
        assertNotNull(resource.get());
    }

    @Test
    void executeWithCallbackBuildsJavascriptWrapper() {
        BrowserComponent browser = new BrowserComponent();
        flushSerialCalls();
        doNothing().when(implementation).browserExecute(any(PeerComponent.class), anyString());
        SuccessCallback<BrowserComponent.JSRef> callback = new Callback<BrowserComponent.JSRef>() {
            @Override
            public void onSucess(BrowserComponent.JSRef value) {
            }
        };
        browser.execute("callback.onSuccess('done')", callback);
        ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
        verify(implementation).browserExecute(any(PeerComponent.class), script.capture());
        assertTrue(script.getValue().contains("callbackId"));
    }

    @Test
    void setUrlDelegatesOncePeerReady() {
        BrowserComponent browser = new BrowserComponent();
        browser.setURL("https://codenameone.com");
        flushSerialCalls();
        verify(implementation).setBrowserURL(peer, "https://codenameone.com");
    }

    @Test
    void getUrlReturnsCachedValueWhenPeerMissing() {
        BrowserComponent browser = new BrowserComponent();
        browser.setURL("https://codenameone.com");
        assertEquals("https://codenameone.com", browser.getURL());
    }

    private int registerReturnValueCallback(BrowserComponent browser, SuccessCallback<BrowserComponent.JSRef> callback) throws Exception {
        Method m = BrowserComponent.class.getDeclaredMethod("addReturnValueCallback", SuccessCallback.class);
        m.setAccessible(true);
        Integer id = (Integer) m.invoke(browser, callback);
        return id.intValue();
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
