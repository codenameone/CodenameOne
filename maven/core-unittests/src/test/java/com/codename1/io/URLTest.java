package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class URLTest {
    private TestCodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = new TestCodenameOneImplementation();
        Util.setImplementation(implementation);
        implementation.clearConnections();
        implementation.clearFileSystem();
        implementation.setAppHomePath("file://app/");
    }

    @Test
    void parsesUrlComponents() throws URISyntaxException {
        URL url = new URL("https://user:secret@example.com:8443/path/resource?x=1");

        assertEquals("x=1", url.getQuery());
        assertEquals("/path/resource", url.getPath());
        assertEquals("user:secret", url.getUserInfo());
        assertEquals("user:secret@example.com:8443", url.getAuthority());
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
        implementation.putFile(key, initial);

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

        assertArrayEquals(updated, implementation.getFileContent(key));
    }

    @Test
    void httpConnectionDelegatesToImplementation() throws Exception {
        URL url = new URL("http://example.com/service");
        TestConnection connection = implementation.createConnection("http://example.com/service");
        connection.setInputData("body".getBytes(StandardCharsets.UTF_8));
        connection.setContentLength(4);
        connection.setHeaderValues("Content-Type", Collections.singletonList("text/plain"));

        URL.HttpURLConnection http = (URL.HttpURLConnection) url.openConnection();
        http.setDoOutput(true);
        http.setRequestProperty("Accept", "text/plain");
        http.connect();

        assertTrue(connection.isReadRequested());
        assertTrue(connection.isWriteRequested());
        assertEquals("GET", connection.getHttpMethod());
        assertEquals("text/plain", connection.getHeaders().get("Accept"));

        http.setRequestMethod("POST");
        assertEquals("POST", connection.getHttpMethod());

        InputStream input = http.getInputStream();
        byte[] data = new byte[4];
        assertEquals(4, input.read(data));
        assertEquals("body", new String(data, StandardCharsets.UTF_8));

        assertEquals(4, http.getContentLength());
        Map<String, List<String>> headers = http.getHeaderFields();
        assertEquals(Collections.singletonList("text/plain"), headers.get("Content-Type"));
    }

    @Test
    void setRequestMethodThrowsOnFailureAfterConnect() throws Exception {
        URL url = new URL("http://example.com/delete");
        TestConnection connection = implementation.createConnection("http://example.com/delete");
        URL.HttpURLConnection http = (URL.HttpURLConnection) url.openConnection();
        http.connect();
        connection.failOnNextHttpMethod(new IOException("unsupported"));

        assertThrows(IllegalArgumentException.class, () -> http.setRequestMethod("DELETE"));
    }
}
