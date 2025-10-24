package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Preferences;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Installs a mocked {@link CodenameOneImplementation} that provides in-memory storage
 * and no-op networking hooks so that tests can execute without native services.
 */
public final class TestImplementationProvider {
    private TestImplementationProvider() {
    }

    private static final String DEFAULT_AUTO_DETECT_URL = NetworkManager.getAutoDetectURL();
    private static final String DEFAULT_PREFERENCES_LOCATION = Preferences.getPreferencesLocation();

    public static CodenameOneImplementation installImplementation(boolean timeoutSupported) {
        Storage.setStorageInstance(null);
        Map<String, byte[]> storage = new ConcurrentHashMap<>();
        CodenameOneImplementation impl = mock(CodenameOneImplementation.class);

        when(impl.shouldAutoDetectAccessPoint()).thenReturn(false);
        when(impl.isTimeoutSupported()).thenReturn(timeoutSupported);
        when(impl.isAPSupported()).thenReturn(false);
        when(impl.getAPIds()).thenReturn(new String[0]);
        when(impl.getAPType(anyString())).thenReturn(NetworkManager.ACCESS_POINT_TYPE_UNKNOWN);
        when(impl.getAPName(anyString())).thenReturn(null);
        when(impl.getCurrentAccessPoint()).thenReturn(null);

        doAnswer(invocation -> {
            storage.clear();
            return null;
        }).when(impl).clearStorage();

        doAnswer(invocation -> {
            Object key = invocation.getArgument(0);
            storage.remove(String.valueOf(key));
            return null;
        }).when(impl).deleteStorageFile(anyString());

        doAnswer(invocation -> {
            Object output = invocation.getArgument(0);
            if (output instanceof OutputStream) {
                ((OutputStream) output).close();
            }
            return null;
        }).when(impl).closingOutput(any());

        when(impl.storageFileExists(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return storage.containsKey(key);
        });

        try {
            when(impl.createStorageOutputStream(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                return new ByteArrayOutputStream() {
                    @Override
                    public void close() throws IOException {
                        storage.put(key, toByteArray());
                        super.close();
                    }
                };
            });

            when(impl.createStorageInputStream(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                byte[] data = storage.get(key);
                if (data == null) {
                    throw new IOException("Missing storage entry " + key);
                }
                return new ByteArrayInputStream(data);
            });
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        when(impl.listStorageEntries()).thenAnswer(invocation -> storage.keySet().toArray(new String[0]));
        when(impl.getStorageEntrySize(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            byte[] data = storage.get(key);
            return data == null ? 0 : data.length;
        });

        doAnswer(invocation -> null).when(impl).flushStorageCache();
        doAnswer(invocation -> null).when(impl).setStorageData(any());
        doAnswer(invocation -> null).when(impl).cleanup(any());
        doAnswer(invocation -> null).when(impl).addConnectionToQueue(any());
        doAnswer(invocation -> null).when(impl).startThread(anyString(), any());
        doAnswer(invocation -> null).when(impl).setCurrentAccessPoint(anyString());
        doAnswer(invocation -> null).when(impl).setTimeout(anyInt());

        Util.setImplementation(impl);
        return impl;
    }

    public static void resetImplementation() {
        Util.setImplementation(null);
        Storage.setStorageInstance(null);
        resetNetworkManager();
        resetPreferences();
    }

    @SuppressWarnings("unchecked")
    private static void resetNetworkManager() {
        NetworkManager manager = NetworkManager.getInstance();
        try {
            Field runningField = NetworkManager.class.getDeclaredField("running");
            runningField.setAccessible(true);
            runningField.setBoolean(manager, false);

            Field threadCountField = NetworkManager.class.getDeclaredField("threadCount");
            threadCountField.setAccessible(true);
            threadCountField.setInt(manager, 1);

            Field networkThreadsField = NetworkManager.class.getDeclaredField("networkThreads");
            networkThreadsField.setAccessible(true);
            networkThreadsField.set(manager, null);

            Field errorField = NetworkManager.class.getDeclaredField("errorListeners");
            errorField.setAccessible(true);
            errorField.set(manager, null);

            Field progressField = NetworkManager.class.getDeclaredField("progressListeners");
            progressField.setAccessible(true);
            progressField.set(manager, null);

            Field pendingField = NetworkManager.class.getDeclaredField("pending");
            pendingField.setAccessible(true);
            ((Vector) pendingField.get(manager)).clear();

            Field threadAssignmentsField = NetworkManager.class.getDeclaredField("threadAssignements");
            threadAssignmentsField.setAccessible(true);
            ((Hashtable) threadAssignmentsField.get(manager)).clear();

            Field userHeadersField = NetworkManager.class.getDeclaredField("userHeaders");
            userHeadersField.setAccessible(true);
            userHeadersField.set(manager, null);

            Field timeoutField = NetworkManager.class.getDeclaredField("timeout");
            timeoutField.setAccessible(true);
            timeoutField.setInt(manager, 300000);

            Field autoDetectedField = NetworkManager.class.getDeclaredField("autoDetected");
            autoDetectedField.setAccessible(true);
            autoDetectedField.setBoolean(manager, false);

            Field nextConnectionIdField = NetworkManager.class.getDeclaredField("nextConnectionId");
            nextConnectionIdField.setAccessible(true);
            nextConnectionIdField.setInt(manager, 1);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to reset NetworkManager state", e);
        }

        NetworkManager.setAutoDetectURL(DEFAULT_AUTO_DETECT_URL);
    }

    @SuppressWarnings("unchecked")
    private static void resetPreferences() {
        try {
            Field pField = Preferences.class.getDeclaredField("p");
            pField.setAccessible(true);
            pField.set(null, null);

            Field listenerField = Preferences.class.getDeclaredField("listenerMap");
            listenerField.setAccessible(true);
            ((Map<?, ?>) listenerField.get(null)).clear();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to reset Preferences state", e);
        }

        Preferences.setPreferencesLocation(DEFAULT_PREFERENCES_LOCATION);
    }
}
