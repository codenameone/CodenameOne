package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class URLTest {
    private CodenameOneImplementation implementation;
    private Map<String, byte[]> files;

    @BeforeEach
    void setUp() {
        implementation = TestImplementationProvider.installImplementation(true);
        files = new ConcurrentHashMap<String, byte[]>();

        when(implementation.getAppHomePath()).thenReturn("file://app/");
        when(implementation.exists(anyString())).thenAnswer(invocation -> files.containsKey(invocation.getArgument(0)));
        when(implementation.getFileLength(anyString())).thenAnswer(invocation -> {
            byte[] data = files.get(invocation.getArgument(0));
            return data == null ? 0L : data.length;
        });

        try {
            when(implementation.openFileOutputStream(anyString())).thenAnswer(invocation -> {
                final String key = invocation.getArgument(0);
                return new ByteArrayOutputStream() {
                    @Override
                    public void close() throws IOException {
                        files.put(key, toByteArray());
                        super.close();
                    }
                };
            });

            when(implementation.openFileInputStream(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                byte[] data = files.get(key);
                if (data == null) {
                    throw new IOException("Missing file " + key);
                }
                return new ByteArrayInputStream(data);
            });
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void parsesUrlComponents() throws URISyntaxException {
        URL url = new URL("https://user:secret@example.com:8443/path/resource?x=1");

        assertEquals("x=1", url.getQuery());
        assertEquals("/path/resource", url.getPath());
        assertEquals("user:secret", url.getUserInfo());
        assertEquals("example.com:8443", url.getAuthority());
        assertEquals(8443, url.getPort());
        assertEquals(443, url.getDefaultPort());
        assertEquals("https", url.getProtocol());
        assertEquals("example.com", url.getHost());
        assertTrue(url.toString().startsWith("https://"));
        assertEquals(url.hashCode(), url.hashCode());
        assertFalse(url.equals(new URL("https://example.com")));
        assertFalse(url.sameFile(new URL("https://example.com")));
    }

    @Test
    void fileConnectionReadsAndWritesUsingFileSystemStorage() throws Exception {
        URL url = new URL("file:/virtual/test.txt");
        String key = "file:/" + url.toURI().getPath();
        byte[] initial = "seed".getBytes(StandardCharsets.UTF_8);
        files.put(key, initial);

        URL.URLConnection connection = url.openConnection();
        connection.connect();

        assertEquals(initial.length, connection.getContentLength());
        InputStream input = connection.getInputStream();
        byte[] buffer = new byte[initial.length];
        assertEquals(initial.length, input.read(buffer));
        assertArrayEquals(initial, buffer);
        input.close();

        OutputStream output = connection.getOutputStream();
        byte[] updated = "updated".getBytes(StandardCharsets.UTF_8);
        output.write(updated);
        output.close();

        assertArrayEquals(updated, files.get(key));
    }

    @Test
    void httpConnectionDelegatesToImplementation() throws Exception {
        final Object nativeConnection = new Object();
        when(implementation.connect(anyString(), anyBoolean(), anyBoolean())).thenReturn(nativeConnection);
        when(implementation.openInputStream(nativeConnection)).thenReturn(new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8)));
        when(implementation.openOutputStream(nativeConnection)).thenAnswer(invocation -> new ByteArrayOutputStream());
        when(implementation.getHeaderField(anyString(), any())).thenReturn(null);
        when(implementation.getHeaderFieldNames(nativeConnection)).thenReturn(new String[]{"Content-Type"});
        when(implementation.getHeaderFields("Content-Type", nativeConnection)).thenReturn(new String[]{"text/plain"});
        when(implementation.getContentLength(nativeConnection)).thenReturn(4);

        URL url = new URL("http://example.com/service");
        URL.HttpURLConnection connection = (URL.HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "text/plain");
        connection.connect();

        verify(implementation).connect("http://example.com/service", true, true);
        verify(implementation).setHttpMethod(nativeConnection, "GET");
        verify(implementation).setHeader(nativeConnection, "Accept", "text/plain");

        connection.setRequestMethod("POST");
        verify(implementation).setHttpMethod(nativeConnection, "POST");

        InputStream input = connection.getInputStream();
        byte[] data = new byte[4];
        assertEquals(4, input.read(data));
        assertEquals("body", new String(data, StandardCharsets.UTF_8));

        assertEquals(4, connection.getContentLength());
        Map<String, List<String>> headers = connection.getHeaderFields();
        assertEquals(Collections.singletonList("text/plain"), headers.get("Content-Type"));
    }

    @Test
    void setRequestMethodThrowsOnFailureAfterConnect() throws Exception {
        final Object nativeConnection = new Object();
        when(implementation.connect(anyString(), anyBoolean(), anyBoolean())).thenReturn(nativeConnection);
        doAnswer(invocation -> {
            throw new IOException("unsupported");
        }).when(implementation).setHttpMethod(nativeConnection, "DELETE");

        URL url = new URL("http://example.com/delete");
        URL.HttpURLConnection connection = (URL.HttpURLConnection) url.openConnection();
        connection.connect();

        assertThrows(IllegalArgumentException.class, () -> connection.setRequestMethod("DELETE"));
    }
}
