/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package com.codename1.io;

import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest extends UITestBase {

    @BeforeEach
    void setUp() {
        implementation.resetCleanupCalls();
    }

    @EdtTest
    void copyClosesStreamsAndInvokesCleanup() throws IOException {
        byte[] source = "payload".getBytes(StandardCharsets.UTF_8);
        InputStream input = new ByteArrayInputStream(source);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Util.copy(input, output);

        assertArrayEquals(source, output.toByteArray());
        List<Object> cleanup = implementation.getCleanupCalls();
        assertTrue(cleanup.contains(input));
        assertTrue(cleanup.contains(output));
    }

    @EdtTest
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

    @EdtTest
    void cleanupHandlesNullValues() {
        Util.cleanup(null);
        List<Object> cleanup = implementation.getCleanupCalls();
        assertTrue(cleanup.contains(null));
    }

    @EdtTest
    void ignoreCharsWhileEncodingCanBeConfigured() {
        Util.setIgnorCharsWhileEncoding("abc");
        assertEquals("abc", Util.getIgnorCharsWhileEncoding());
        Util.setIgnorCharsWhileEncoding("");
    }

    @EdtTest
    void splitBreaksStringIntoComponents() {
        String[] parts = Util.split("one,two,three", ",");
        assertArrayEquals(new String[]{"one", "two", "three"}, parts);
    }

    @EdtTest
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

    @EdtTest
    void readFullyReadsExactNumberOfBytes() throws IOException {
        byte[] source = new byte[]{1, 2, 3, 4};
        byte[] target = new byte[4];
        Util.readFully(new ByteArrayInputStream(source), target);
        assertArrayEquals(source, target);
    }

    @EdtTest
    void readFullyThrowsOnShortStream() {
        byte[] source = new byte[]{1, 2};
        byte[] target = new byte[4];
        assertThrows(EOFException.class, () -> Util.readFully(new ByteArrayInputStream(source), target));
    }

    @EdtTest
    void readAllReturnsCountUntilStreamEnds() throws IOException {
        byte[] source = new byte[]{1, 2, 3};
        byte[] target = new byte[5];
        int read = Util.readAll(new ByteArrayInputStream(source), target);
        assertEquals(3, read);
        assertEquals(1, target[0]);
        assertEquals(3, target[2]);
    }

    @EdtTest
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

    @EdtTest
    void readToStringUsesProvidedCharset() throws IOException {
        byte[] data = "héllo".getBytes("UTF-16BE");
        String value = Util.readToString(new ByteArrayInputStream(data), "UTF-16BE");
        assertEquals("héllo", value);
    }

    @EdtTest
    void mapWritesAsManyEntriesAsItsHeaderPromises() throws IOException {
        // a map that reports more entries than it hands out stands in for one that
        // another thread shrank between the size being read and the walk that
        // follows it. The header and the payload have to agree either way, since a
        // reader that trusts the header reads straight off the end of the entries.
        Map<String, Object> shrunk = new LyingSizeMap(3);
        shrunk.put("kept", "a");
        shrunk.put("alsoKept", "b");

        Map<String, Object> result = roundTripMap(shrunk);

        assertEquals(2, result.size());
        assertEquals("a", result.get("kept"));
        assertEquals("b", result.get("alsoKept"));
    }

    @EdtTest
    void mapNeverWritesMoreEntriesThanItsHeaderPromises() throws IOException {
        // the other direction: a map that grew. The extra entries must not be
        // written, or everything after this object in the stream reads as garbage.
        Map<String, Object> grown = new LyingSizeMap(1);
        grown.put("first", "a");
        grown.put("second", "b");
        grown.put("third", "c");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(output);
        Util.writeObject(grown, dataOutput);
        Util.writeObject("sentinel", dataOutput);
        dataOutput.close();

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(output.toByteArray()));
        Object map = Util.readObject(input);
        Object sentinel = Util.readObject(input);
        input.close();

        assertTrue(map instanceof Map);
        assertEquals(1, ((Map<?, ?>) map).size());
        // the stream is still aligned for whatever was written after the map
        assertEquals("sentinel", sentinel);
    }

    @EdtTest
    void hashtableWritesAsManyEntriesAsItsHeaderPromises() throws IOException {
        Hashtable<String, Object> shrunk = new LyingSizeHashtable(4);
        shrunk.put("one", "1");
        shrunk.put("two", "2");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(output);
        Util.writeObject(shrunk, dataOutput);
        dataOutput.close();

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(output.toByteArray()));
        Object result = Util.readObject(input);
        input.close();

        assertTrue(result instanceof Hashtable);
        Hashtable<?, ?> roundTrip = (Hashtable<?, ?>) result;
        assertEquals(2, roundTrip.size());
        assertEquals("1", roundTrip.get("one"));
        assertEquals("2", roundTrip.get("two"));
    }

    private Map<String, Object> roundTripMap(Map<String, Object> value) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(output);
        Util.writeObject(value, dataOutput);
        dataOutput.close();

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(output.toByteArray()));
        Object result = Util.readObject(input);
        input.close();

        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) result;
        return typed;
    }

    /// A Map whose reported size does not match the entries it iterates, which is
    /// what a map being changed on another thread looks like from the serializer.
    private static final class LyingSizeMap extends AbstractMap<String, Object> {
        private final Map<String, Object> delegate = new LinkedHashMap<String, Object>();
        private final int reportedSize;

        LyingSizeMap(int reportedSize) {
            this.reportedSize = reportedSize;
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Object put(String key, Object value) {
            return delegate.put(key, value);
        }

        @Override
        public int size() {
            return reportedSize;
        }
    }

    /// The Hashtable equivalent of {@link LyingSizeMap}; Util serializes Hashtable
    /// through its own branch.
    private static final class LyingSizeHashtable extends Hashtable<String, Object> {
        private final int reportedSize;

        LyingSizeHashtable(int reportedSize) {
            this.reportedSize = reportedSize;
        }

        @Override
        public synchronized int size() {
            return reportedSize;
        }
    }
}
