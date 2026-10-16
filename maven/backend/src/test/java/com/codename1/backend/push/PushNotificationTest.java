/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The envelope, the surface commands and the device targets, without a network.
 */
class PushNotificationTest {

    @Test
    @DisplayName("every builder field lands in the schema-3 envelope")
    void envelope() {
        Map<String, Object> fcm = new LinkedHashMap<String, Object>();
        fcm.put("priority", "high");
        PushNotification message = PushNotification.builder()
                .id("m-1")
                .title("T")
                .body("B")
                .imageUrl("https://example.com/i.png")
                .deepLink("app://trip/1")
                .collapseKey("trip-1")
                .ttlSeconds(600)
                .silent(false)
                .data("n", Integer.valueOf(3))
                .data("gone", "x")
                .data("gone", null)
                .platform("GCM", fcm)
                .build();
        assertEquals("{\"schema\":3,\"id\":\"m-1\",\"title\":\"T\",\"body\":\"B\","
                + "\"image\":\"https://example.com/i.png\",\"deepLink\":\"app://trip/1\","
                + "\"collapseKey\":\"trip-1\",\"ttl\":600,\"silent\":false,"
                + "\"data\":{\"n\":3},\"platform\":{\"fcm\":{\"priority\":\"high\"}}}",
                message.toJson());
    }

    @Test
    @DisplayName("a built notification can't be changed through toMap or the builder")
    void immutable() {
        PushNotification.Builder builder = PushNotification.builder().body("first");
        PushNotification message = builder.build();
        builder.body("second");
        Map<String, Object> map = message.toMap();
        map.put("body", "third");
        assertEquals("first", message.toMap().get("body"));
    }

    @Test
    @DisplayName("ttl must be positive, keys must be clean, providers must exist")
    void validation() {
        PushNotification.Builder builder = PushNotification.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.ttlSeconds(0));
        assertThrows(IllegalArgumentException.class, () -> builder.ttlSeconds(-1));
        assertThrows(IllegalArgumentException.class, () -> builder.data("", "v"));
        assertThrows(IllegalArgumentException.class, () -> builder.data(" k", "v"));
        assertThrows(IllegalArgumentException.class,
                () -> builder.platform("telegraph", new LinkedHashMap<String, Object>()));
        Map<String, Object> bogus = new LinkedHashMap<String, Object>();
        bogus.put("operation", "explode");
        assertThrows(IllegalArgumentException.class, () -> builder.surface(bogus));
        assertThrows(IllegalArgumentException.class,
                () -> PushNotification.liveUpdate("", null));
        assertThrows(IllegalArgumentException.class,
                () -> PushNotification.widget("kind", " "));
    }

    @Test
    @DisplayName("liveUpdate serializes its state to a string with sorted keys")
    void liveUpdate() {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("zeta", Boolean.TRUE);
        state.put("alpha", "a\"b");
        state.put("mid", Integer.valueOf(7));
        Map<String, Object> command = PushNotification.liveUpdate("ride-9", state);
        assertEquals("live-update", command.get("operation"));
        assertEquals("ride-9", command.get("id"));
        assertEquals("{\"alpha\":\"a\\\"b\",\"mid\":7,\"zeta\":true}", command.get("state"));
        assertEquals(3, command.size());
        assertEquals("{}", PushNotification.liveUpdate("ride-9", null).get("state"));
    }

    @Test
    @DisplayName("liveEnd carries the final state only when there is one")
    void liveEnd() {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("status", "done");
        Map<String, Object> withState = PushNotification.liveEnd("ride-9", state, false);
        assertEquals("live-end", withState.get("operation"));
        assertEquals("{\"status\":\"done\"}", withState.get("state"));
        assertEquals(Boolean.FALSE, withState.get("dismissImmediately"));

        Map<String, Object> bare = PushNotification.liveEnd("ride-9", null, true);
        assertFalse(bare.containsKey("state"));
        assertEquals(Boolean.TRUE, bare.get("dismissImmediately"));
    }

    @Test
    @DisplayName("widget passes the timeline through as a string")
    void widget() {
        Map<String, Object> command = PushNotification.widget("eta", "{\"entries\":[]}");
        assertEquals("widget", command.get("operation"));
        assertEquals("eta", command.get("kind"));
        assertEquals("{\"entries\":[]}", command.get("timeline"));
        PushNotification message = PushNotification.builder().surface(command).build();
        assertEquals("{\"schema\":3,\"surface\":{\"operation\":\"widget\",\"kind\":\"eta\","
                + "\"timeline\":\"{\\\"entries\\\":[]}\"}}", message.toJson());
    }

    @Test
    @DisplayName("provider aliases normalize to the service's names")
    void targets() {
        assertEquals("fcm", PushTarget.of("fcm", "t").getProvider());
        assertEquals("fcm", PushTarget.of("GCM", "t").getProvider());
        assertEquals("apns", PushTarget.of("ios", "t").getProvider());
        assertEquals("apns", PushTarget.of("APNS", "t").getProvider());
        assertEquals("huawei", PushTarget.of("hms", "t").getProvider());
        assertEquals("huawei", PushTarget.of("Huawei", "t").getProvider());
        assertEquals("wns", PushTarget.of("wns", "t").getProvider());
        assertEquals("web", PushTarget.of("WEB", "t").getProvider());
        assertEquals("t", PushTarget.of("fcm", "t").getToken());
        assertThrows(IllegalArgumentException.class, () -> PushTarget.of("sms", "t"));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.of(null, "t"));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.of("fcm", ""));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.of("fcm", "  "));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.of("fcm", null));
    }

    @Test
    @DisplayName("classic device keys lose their prefix and keep every dash of the token")
    void deviceKeys() {
        PushTarget fcm = PushTarget.fromDeviceKey("cn1-fcm-abc:def-ghi");
        assertEquals("fcm", fcm.getProvider());
        assertEquals("abc:def-ghi", fcm.getToken());
        assertEquals("fcm", PushTarget.fromDeviceKey("cn1-gcm-x").getProvider());
        PushTarget ios = PushTarget.fromDeviceKey("cn1-ios-0a1b2c");
        assertEquals("apns", ios.getProvider());
        assertEquals("0a1b2c", ios.getToken());
        assertEquals("huawei", PushTarget.fromDeviceKey("cn1-hms-y").getProvider());
        assertThrows(IllegalArgumentException.class, () -> PushTarget.fromDeviceKey(null));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.fromDeviceKey("abc"));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.fromDeviceKey("cn1-fcm"));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.fromDeviceKey("cn1--tok"));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.fromDeviceKey("cn1-fcm-"));
        assertThrows(IllegalArgumentException.class, () -> PushTarget.fromDeviceKey("cn1-pager-t"));
    }

    @Test
    @DisplayName("a result copies its ids and never answers null")
    void result() {
        PushResult none = new PushResult(202, null, null, -1);
        assertEquals(0, none.getDeliveryIds().size());
        List<String> ids = new java.util.ArrayList<String>();
        ids.add("a");
        PushResult one = new PushResult(202, ids, null, -1);
        ids.add("b");
        assertEquals(1, one.getDeliveryIds().size());
        assertThrows(UnsupportedOperationException.class, () -> one.getDeliveryIds().add("c"));
    }
}
