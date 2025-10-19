package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
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
}
