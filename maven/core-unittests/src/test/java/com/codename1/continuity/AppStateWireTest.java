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
package com.codename1.continuity;

import com.codename1.io.Util;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two wire formats an {@link AppState} has to survive, and the payload rule that makes both
 * possible.
 *
 * <p>A state is written to storage on this device, handed to an operating system that may deliver
 * it to another one, and sent through a relay to a device that may not be running the same build.
 * Every one of those is lossy for something, so what is admitted into a payload is deliberately
 * narrow -- and the point of these tests is that the narrowness is enforced where the application
 * can act on it rather than discovered as a value that stopped arriving.</p>
 */
public class AppStateWireTest {

    @Test
    public void jsonRoundTripPreservesEveryField() throws Exception {
        AppState state = sample();

        AppState back = StateCodec.fromJson(StateCodec.toJson(state));

        assertNotNull(back);
        assertEquals(Arrays.asList("/home", "/users/42"), back.getRoutes());
        assertEquals("Ada", back.getPayload().get("name"));
        assertEquals("device-a", back.getDeviceId());
        assertEquals("Editing Ada", back.getTitle());
        assertEquals(7L, back.getSequence());
        assertEquals(1700000000123L, back.getTimestamp());
    }

    /**
     * The reason the sequence and timestamp are encoded as strings.
     *
     * <p>JSON has one number type and {@code JSONParser} reads every one of them back as a
     * {@code Double}. A millisecond timestamp is past the range a double represents exactly, so a
     * numeric encoding would come back changed -- and only on the relay path, leaving a state that
     * no longer compares equal to the one the same device published through a continuation.</p>
     */
    @Test
    public void aMillisecondTimestampSurvivesJsonExactly() throws Exception {
        AppState state = new AppState().setTimestamp(1763512345678L).setSequence(9007199254740993L);

        AppState back = StateCodec.fromJson(StateCodec.toJson(state));

        assertEquals(1763512345678L, back.getTimestamp());
        assertEquals(9007199254740993L, back.getSequence());
    }

    @Test
    public void externalizableRoundTripPreservesEveryField() throws Exception {
        Util.register(AppState.OBJECT_ID, AppState.class);
        AppState state = sample();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        Util.writeObject(state, out);
        out.close();
        Object read = Util.readObject(
                new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertTrue(read instanceof AppState);
        AppState back = (AppState) read;
        assertEquals(Arrays.asList("/home", "/users/42"), back.getRoutes());
        assertEquals("Ada", back.getPayload().get("name"));
        assertEquals("device-a", back.getDeviceId());
        assertEquals("Editing Ada", back.getTitle());
        assertEquals(7L, back.getSequence());
        assertEquals(1700000000123L, back.getTimestamp());
    }

    @Test
    public void aNestedPayloadSurvivesTheMapForm() {
        Map<String, Object> inner = new HashMap<String, Object>();
        inner.put("street", "Sesame");
        List<Object> list = new ArrayList<Object>();
        list.add("a");
        list.add(Integer.valueOf(2));
        list.add(Boolean.TRUE);
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("address", inner);
        payload.put("tags", list);

        AppState back = StateCodec.fromMap(StateCodec.toMap(new AppState().setPayload(payload)));

        assertNotNull(back);
        Object address = back.getPayload().get("address");
        assertTrue(address instanceof Map);
        assertEquals("Sesame", ((Map<?, ?>) address).get("street"));
        assertEquals(3, ((List<?>) back.getPayload().get("tags")).size());
    }

    @Test
    public void anUnrepresentableValueIsRefusedWithItsKey() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("when", new java.util.Date());

        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        new AppState().setPayload(payload);
                    }
                });

        assertTrue(err.getMessage().contains("when"), err.getMessage());
        assertTrue(err.getMessage().contains("java.util.Date"), err.getMessage());
    }

    @Test
    public void anUnrepresentableValueNestedInsideAListNamesItsPath() {
        List<Object> list = new ArrayList<Object>();
        list.add("fine");
        list.add(new Object());
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("items", list);

        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        new AppState().setPayload(payload);
                    }
                });

        assertTrue(err.getMessage().contains("items[1]"), err.getMessage());
    }

    /**
     * A cycle looks exactly like a very deep tree until the stack runs out, and neither
     * destination format can represent one.
     */
    @Test
    public void aCyclicPayloadIsRefusedRatherThanOverflowingTheStack() {
        Map<String, Object> payload = new HashMap<String, Object>();
        List<Object> loop = new ArrayList<Object>();
        loop.add(loop);
        payload.put("loop", loop);

        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        new AppState().setPayload(payload);
                    }
                });

        assertTrue(err.getMessage().contains("cycle"), err.getMessage());
    }

    @Test
    public void aMapKeyThatIsNotAStringIsRefused() {
        Map<Object, Object> inner = new HashMap<Object, Object>();
        inner.put(Integer.valueOf(1), "one");
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("byNumber", inner);

        IllegalArgumentException err = assertThrows(IllegalArgumentException.class,
                new org.junit.jupiter.api.function.Executable() {
                    public void execute() {
                        new AppState().setPayload(payload);
                    }
                });

        assertTrue(err.getMessage().contains("byNumber"), err.getMessage());
    }

    /**
     * A payload arriving from another device is NOT validated.
     *
     * <p>It was validated where it was produced. Refusing it here would turn a remote build's
     * mistake into an exception on this device, at a moment the user cannot connect to anything
     * they did.</p>
     */
    @Test
    public void anArrivingPayloadIsNotRevalidated() {
        Map<String, Object> wire = new HashMap<String, Object>();
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("odd", new Object());
        wire.put("payload", payload);
        wire.put("device", "other");

        AppState back = StateCodec.fromMap(wire);

        assertNotNull(back);
        assertEquals("other", back.getDeviceId());
    }

    @Test
    public void anUnknownFieldFromANewerBuildIsIgnoredRatherThanFailing() throws Exception {
        AppState back = StateCodec.fromJson(
                "{\"routes\":[\"/home\"],\"device\":\"x\",\"somethingNew\":{\"a\":1}}");

        assertNotNull(back);
        assertEquals(Arrays.asList("/home"), back.getRoutes());
    }

    @Test
    public void emptyAndNullDocumentsProduceNoState() throws Exception {
        assertNull(StateCodec.fromJson(null));
        assertNull(StateCodec.fromJson("   "));
        assertNull(StateCodec.fromMap(null));
    }

    @Test
    public void blankRoutePathsAreDropped() {
        AppState state = new AppState().setRoutes(Arrays.asList("/a", null, "", "/b"));

        assertEquals(Arrays.asList("/a", "/b"), state.getRoutes());
    }

    @Test
    public void aStateWithNoRoutesAndNoPayloadIsEmpty() {
        assertTrue(new AppState().isEmpty());
        assertFalse(new AppState().setRoutes(Arrays.asList("/a")).isEmpty());
    }

    private static AppState sample() {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("name", "Ada");
        return new AppState()
                .setRoutes(Arrays.asList("/home", "/users/42"))
                .setPayload(payload)
                .setDeviceId("device-a")
                .setTitle("Editing Ada")
                .setSequence(7L)
                .setTimestamp(1700000000123L);
    }
}
