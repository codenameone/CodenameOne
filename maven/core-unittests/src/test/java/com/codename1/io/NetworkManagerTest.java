package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NetworkManagerTest {
    private NetworkManager manager;
    private CodenameOneImplementation implementation;

    @BeforeEach
    void setUp() throws Exception {
        implementation = TestImplementationProvider.installImplementation(true);
        manager = NetworkManager.getInstance();
        resetManagerState();
        bootstrapDisplayThread();
    }

    @Test
    void autoDetectUrlMutators() {
        NetworkManager.setAutoDetectURL("https://example.com/ping");
        assertEquals("https://example.com/ping", NetworkManager.getAutoDetectURL());
    }

    @Test
    void addDefaultHeaderStoresValue() throws Exception {
        manager.addDefaultHeader("X-Test", "value");
        Field headersField = NetworkManager.class.getDeclaredField("userHeaders");
        headersField.setAccessible(true);
        Object headers = headersField.get(manager);
        assertNotNull(headers);
        assertEquals("value", ((java.util.Hashtable) headers).get("X-Test"));
    }

    @Test
    void errorListenersReceiveEvents() {
        AtomicInteger invocations = new AtomicInteger();
        ActionListener<NetworkEvent> listener = evt -> {
            invocations.incrementAndGet();
            evt.consume();
        };
        manager.addErrorListener(listener);
        boolean consumed = manager.handleErrorCode(new ConnectionRequest(), 404, "Not Found");
        assertTrue(consumed);
        assertEquals(1, invocations.get());
    }

    @Test
    void progressListenersTrackUpdates() {
        AtomicInteger lengths = new AtomicInteger();
        ActionListener<NetworkEvent> listener = evt -> lengths.addAndGet(evt.getLength());
        manager.addProgressListener(listener);
        manager.fireProgressEvent(new ConnectionRequest(), NetworkEvent.PROGRESS_TYPE_OUTPUT, 50, 10);
        assertEquals(50, lengths.get());
        assertTrue(manager.hasProgressListeners());
        manager.removeProgressListener(listener);
        assertFalse(manager.hasProgressListeners());
    }

    @Test
    void removeProgressListenerClearsDispatcher() {
        ActionListener<NetworkEvent> listener = evt -> {};
        manager.addProgressListener(listener);
        assertTrue(manager.hasProgressListeners());
        manager.removeProgressListener(listener);
        assertFalse(manager.hasProgressListeners());
    }

    @Test
    void setTimeoutDelegatesWhenSupported() {
        manager.setTimeout(1234);
        verify(implementation).setTimeout(1234);
    }

    @Test
    void setTimeoutStoresValueWhenUnsupported() throws Exception {
        reset(implementation);
        implementation = TestImplementationProvider.installImplementation(false);
        manager = NetworkManager.getInstance();
        resetManagerState();

        manager.setTimeout(4321);
        Field timeoutField = NetworkManager.class.getDeclaredField("timeout");
        timeoutField.setAccessible(true);
        assertEquals(4321, timeoutField.getInt(manager));
    }

    @Test
    void assignToThreadRecordsMapping() throws Exception {
        manager.assignToThread(MockConnectionRequest.class, 2);
        Field assignField = NetworkManager.class.getDeclaredField("threadAssignements");
        assignField.setAccessible(true);
        java.util.Hashtable table = (java.util.Hashtable) assignField.get(manager);
        assertEquals(2, table.get(MockConnectionRequest.class.getName()));
    }

    @Test
    void enumerateQueueReturnsSnapshot() throws Exception {
        Vector pending = getPendingQueue();
        ConnectionRequest first = new ConnectionRequest();
        ConnectionRequest second = new ConnectionRequest();
        pending.addElement(first);
        pending.addElement(second);

        Enumeration enumeration = manager.enumurateQueue();
        assertTrue(enumeration.hasMoreElements());
        assertSame(first, enumeration.nextElement());
        assertSame(second, enumeration.nextElement());
        assertFalse(enumeration.hasMoreElements());
    }

    @Test
    void isQueueIdleReflectsCurrentRequest() throws Exception {
        Vector pending = getPendingQueue();
        pending.clear();
        assertTrue(manager.isQueueIdle());

        NetworkManager.NetworkThread thread = manager.new NetworkThread();
        Object array = Array.newInstance(thread.getClass(), 1);
        Array.set(array, 0, thread);
        Field threadsField = NetworkManager.class.getDeclaredField("networkThreads");
        threadsField.setAccessible(true);
        threadsField.set(manager, array);
        assertTrue(manager.isQueueIdle());

        Field currentRequestField = thread.getClass().getDeclaredField("currentRequest");
        currentRequestField.setAccessible(true);
        currentRequestField.set(thread, new ConnectionRequest());
        assertFalse(manager.isQueueIdle());
    }

    @Test
    void apDelegatesToImplementation() {
        when(implementation.isAPSupported()).thenReturn(true);
        when(implementation.getAPIds()).thenReturn(new String[]{"wifi"});
        when(implementation.getAPType("wifi")).thenReturn(NetworkManager.ACCESS_POINT_TYPE_WLAN);
        when(implementation.getAPName("wifi")).thenReturn("WiFi");
        manager.setCurrentAccessPoint("wifi");
        assertTrue(manager.isAPSupported());
        assertArrayEquals(new String[]{"wifi"}, manager.getAPIds());
        assertEquals(NetworkManager.ACCESS_POINT_TYPE_WLAN, manager.getAPType("wifi"));
        assertEquals("WiFi", manager.getAPName("wifi"));
    }

    private Vector getPendingQueue() throws Exception {
        Field pendingField = NetworkManager.class.getDeclaredField("pending");
        pendingField.setAccessible(true);
        return (Vector) pendingField.get(manager);
    }

    private void resetManagerState() throws Exception {
        Field runningField = NetworkManager.class.getDeclaredField("running");
        runningField.setAccessible(true);
        runningField.setBoolean(manager, false);

        Field networkThreadsField = NetworkManager.class.getDeclaredField("networkThreads");
        networkThreadsField.setAccessible(true);
        networkThreadsField.set(manager, null);

        Field errorField = NetworkManager.class.getDeclaredField("errorListeners");
        errorField.setAccessible(true);
        errorField.set(manager, null);

        Field progressField = NetworkManager.class.getDeclaredField("progressListeners");
        progressField.setAccessible(true);
        progressField.set(manager, null);

        Field autoDetectedField = NetworkManager.class.getDeclaredField("autoDetected");
        autoDetectedField.setAccessible(true);
        autoDetectedField.setBoolean(manager, false);

        Field threadAssignments = NetworkManager.class.getDeclaredField("threadAssignements");
        threadAssignments.setAccessible(true);
        ((java.util.Hashtable) threadAssignments.get(manager)).clear();

        Field timeoutField = NetworkManager.class.getDeclaredField("timeout");
        timeoutField.setAccessible(true);
        timeoutField.setInt(manager, 300000);

        getPendingQueue().clear();
    }

    private void bootstrapDisplayThread() throws Exception {
        Display display = Display.getInstance();
        Field edtField = Display.class.getDeclaredField("edt");
        edtField.setAccessible(true);
        edtField.set(display, Thread.currentThread());
    }

    private static class MockConnectionRequest extends ConnectionRequest {
    }
}
