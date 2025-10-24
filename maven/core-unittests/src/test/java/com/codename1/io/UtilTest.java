package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class UtilTest {
    private CodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = TestImplementationProvider.installImplementation(true);
    }

    @AfterEach
    void tearDown() {
        TestImplementationProvider.resetImplementation();
    }

    @Test
    void copyClosesStreamsAndInvokesCleanup() throws IOException {
        byte[] source = "payload".getBytes(StandardCharsets.UTF_8);
        InputStream input = new ByteArrayInputStream(source);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Util.copy(input, output);

        assertArrayEquals(source, output.toByteArray());
        verify(implementation, atLeastOnce()).cleanup(input);
        verify(implementation, atLeastOnce()).cleanup(output);
    }

    @Test
    void copyNoCloseKeepsStreamsOpenAndReportsProgress() throws IOException {
        byte[] source = new byte[32];
        for (int i = 0; i < source.length; i++) {
            source[i] = (byte) i;
        }
        InputStream input = new ByteArrayInputStream(source);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final int[] updates = new int[1];

        Util.copyNoClose(input, output, 8, (stream, count) -> updates[0] = count);

        assertArrayEquals(source, output.toByteArray());
        assertEquals(source.length, updates[0]);
    }

    @Test
    void cleanupHandlesNullValues() {
        Util.cleanup(null);
        verify(implementation).cleanup(null);
    }

    @Test
    void ignoreCharsWhileEncodingCanBeConfigured() {
        Util.setIgnorCharsWhileEncoding("abc");
        assertEquals("abc", Util.getIgnorCharsWhileEncoding());
        Util.setIgnorCharsWhileEncoding("");
    }

    @Test
    void splitBreaksStringIntoComponents() {
        String[] parts = Util.split("one,two,three", ",");
        assertArrayEquals(new String[]{"one", "two", "three"}, parts);
    }

    @Test
    void mergeAndInsertAndRemoveArrayOperationsWork() {
        String[] first = new String[]{"a", "b"};
        String[] second = new String[]{"c"};
        String[] merged = new String[3];
        Util.mergeArrays(first, second, merged);
        assertArrayEquals(new String[]{"a", "b", "c"}, merged);

        String[] destination = new String[4];
        Util.insertObjectAtOffset(merged, destination, 1, "x");
        assertArrayEquals(new String[]{"a", "x", "b", "c"}, destination);

        String[] removed = new String[3];
        Util.removeObjectAtOffset(destination, removed, "x");
        assertArrayEquals(new String[]{"a", "b", "c"}, removed);

        Util.removeObjectAtOffset(destination, removed, 0);
        assertArrayEquals(new String[]{"x", "b", "c"}, removed);
    }

    @Test
    void readFullyReadsExactNumberOfBytes() throws IOException {
        byte[] source = new byte[]{1, 2, 3, 4};
        byte[] target = new byte[4];
        Util.readFully(new ByteArrayInputStream(source), target);
        assertArrayEquals(source, target);
    }

    @Test
    void readFullyThrowsOnShortStream() {
        byte[] source = new byte[]{1, 2};
        byte[] target = new byte[4];
        assertThrows(EOFException.class, () -> Util.readFully(new ByteArrayInputStream(source), target));
    }

    @Test
    void readAllReturnsCountUntilStreamEnds() throws IOException {
        byte[] source = new byte[]{1, 2, 3};
        byte[] target = new byte[5];
        int read = Util.readAll(new ByteArrayInputStream(source), target);
        assertEquals(3, read);
        assertEquals(1, target[0]);
        assertEquals(3, target[2]);
    }

    @Test
    void writeAndReadObjectRoundTripsSupportedTypes() throws IOException {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("name", "Alice");
        payload.put("age", Integer.valueOf(30));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(output);
        Util.writeObject(payload, dataOutput);
        dataOutput.close();

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(output.toByteArray()));
        Object result = Util.readObject(input);
        input.close();

        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> roundTrip = (Map<String, Object>) result;
        assertEquals("Alice", roundTrip.get("name"));
        assertEquals(30, ((Integer) roundTrip.get("age")).intValue());
    }

    @Test
    void readToStringUsesProvidedCharset() throws IOException {
        byte[] data = "héllo".getBytes("UTF-16BE");
        String value = Util.readToString(new ByteArrayInputStream(data), "UTF-16BE");
        assertEquals("héllo", value);
    }
}
