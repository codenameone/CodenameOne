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

import com.sun.net.httpserver.HttpExchange;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CloudPush against a stub of the push service on a loopback port: what goes on
 * the wire, and how each kind of answer comes back.
 */
class CloudPushTest {
    private static final String APP_ID = "4f9c2d36-8a1e-4b7d-9c55-0e3b7a1f2c64";
    private static final String KEY = "srv_test_key";

    private com.sun.net.httpserver.HttpServer stub;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> method = new AtomicReference<String>();
    private final AtomicReference<String> path = new AtomicReference<String>();
    private final AtomicReference<String> authorization = new AtomicReference<String>();
    private final AtomicReference<String> contentType = new AtomicReference<String>();
    private final AtomicReference<String> body = new AtomicReference<String>();

    private volatile int answerStatus = 202;
    private volatile String answerBody = "{\"deliveryIds\":[\"d-1\",\"d-2\"],"
            + "\"quota\":{\"used\":2,\"limit\":1000,\"seats\":1,\"period\":\"2026-10\"}}";
    private volatile String answerRetryAfter;

    @BeforeEach
    void startStub() throws IOException {
        stub = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/", (HttpExchange exchange) -> {
            requests.incrementAndGet();
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8));
            byte[] answer = answerBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            if (answerRetryAfter != null) {
                exchange.getResponseHeaders().set("Retry-After", answerRetryAfter);
            }
            exchange.sendResponseHeaders(answerStatus, answer.length);
            OutputStream out = exchange.getResponseBody();
            out.write(answer);
            out.close();
        });
        stub.start();
    }

    @AfterEach
    void stopStub() {
        stub.stop(0);
    }

    private CloudPush push() {
        return new CloudPush(APP_ID, KEY).withEndpoint("http://127.0.0.1:"
                + stub.getAddress().getPort() + "/api/v3/push/messages");
    }

    private static PushNotification hello() {
        return PushNotification.builder().title("Hi").body("There").build();
    }

    @Test
    @DisplayName("the request is the documented V3 call, byte for byte")
    void requestOnTheWire() throws Exception {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("status", "arriving");
        state.put("eta", Integer.valueOf(5));
        PushNotification message = PushNotification.builder()
                .title("Hi")
                .body("There")
                .surface(PushNotification.liveUpdate("trip-42", state))
                .data("tripId", "42")
                .build();

        PushResult result = push().send(message,
                PushTarget.of("gcm", "tok-1"), PushTarget.fromDeviceKey("cn1-ios-tok-2"));

        assertEquals("POST", method.get());
        assertEquals("/api/v3/push/messages", path.get());
        assertEquals("Bearer " + KEY, authorization.get());
        assertEquals("application/json", contentType.get());
        assertEquals("{\"appId\":\"" + APP_ID + "\","
                + "\"targets\":[{\"provider\":\"fcm\",\"token\":\"tok-1\"},"
                + "{\"provider\":\"apns\",\"token\":\"tok-2\"}],"
                + "\"message\":{\"schema\":3,\"title\":\"Hi\",\"body\":\"There\","
                + "\"surface\":{\"operation\":\"live-update\",\"id\":\"trip-42\","
                + "\"state\":\"{\\\"eta\\\":5,\\\"status\\\":\\\"arriving\\\"}\"},"
                + "\"data\":{\"tripId\":\"42\"}}}", body.get());

        assertEquals(202, result.getStatus());
        assertTrue(result.isAccepted());
        assertEquals(Arrays.asList("d-1", "d-2"), result.getDeliveryIds());
        assertNull(result.getError());
        assertEquals(-1, result.getRetryAfterSeconds());
    }

    @Test
    @DisplayName("a 400 is a result carrying the service's error and message")
    void badRequest() throws Exception {
        answerStatus = 400;
        answerBody = "{\"error\":\"invalid_target\",\"message\":\"Unknown provider\"}";
        PushResult result = push().send(hello(), PushTarget.of("fcm", "t"));
        assertEquals(400, result.getStatus());
        assertFalse(result.isAccepted());
        assertEquals("invalid_target: Unknown provider", result.getError());
        assertTrue(result.getDeliveryIds().isEmpty());
    }

    @Test
    @DisplayName("a 401 is a refused result, and an error code alone is reported as the code")
    void unauthorized() throws Exception {
        answerStatus = 401;
        answerBody = "{\"error\":\"unauthorized\"}";
        PushResult result = push().send(hello(), PushTarget.of("fcm", "t"));
        assertEquals(401, result.getStatus());
        assertFalse(result.isAccepted());
        // Not the body's text on this arm: HttpURLConnection, in the fixed-length
        // streaming mode Web's Java SE twin posts with, disconnects on a 401 and
        // hands back no error stream at all (measured: the same POST without
        // streaming mode reads the body). So the result falls back to the status.
        // The packaged arm reads the body; the parsing is checked directly below.
        assertTrue(result.getError() != null && result.getError().length() > 0);
        PushResult parsed = CloudPush.toResult(401, answerBody, null);
        assertEquals("unauthorized", parsed.getError());
        assertEquals(-1, parsed.getRetryAfterSeconds());
    }

    @Test
    @DisplayName("a 429 reports Retry-After in seconds")
    void rateLimited() throws Exception {
        answerStatus = 429;
        answerRetryAfter = "30";
        answerBody = "{\"error\":\"rate_limited\",\"message\":\"Slow down\"}";
        PushResult result = push().send(hello(), PushTarget.of("fcm", "t"));
        assertEquals(429, result.getStatus());
        assertEquals(30, result.getRetryAfterSeconds());
        assertEquals("rate_limited: Slow down", result.getError());
    }

    @Test
    @DisplayName("an answer that isn't JSON still reports its status")
    void notJson() throws Exception {
        answerStatus = 502;
        answerBody = "<html>bad gateway</html>";
        PushResult result = push().send(hello(), PushTarget.of("fcm", "t"));
        assertEquals(502, result.getStatus());
        assertEquals("HTTP 502", result.getError());
    }

    @Test
    @DisplayName("no answer at all is an IOException")
    void transferFailure() throws Exception {
        int port;
        try (ServerSocket closed = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            port = closed.getLocalPort();
        }
        CloudPush unreachable = new CloudPush(APP_ID, KEY)
                .withEndpoint("http://127.0.0.1:" + port + "/api/v3/push/messages");
        IOException err = assertThrows(IOException.class,
                () -> unreachable.send(hello(), PushTarget.of("fcm", "secret-token")));
        assertFalse(String.valueOf(err.getMessage()).contains(KEY));
        assertFalse(String.valueOf(err.getMessage()).contains("secret-token"));
    }

    @Test
    @DisplayName("arguments are checked before any request is made")
    void validation() {
        assertThrows(IllegalArgumentException.class, () -> new CloudPush(null, KEY));
        assertThrows(IllegalArgumentException.class, () -> new CloudPush(" ", KEY));
        assertThrows(IllegalArgumentException.class, () -> new CloudPush(APP_ID, ""));
        assertThrows(IllegalArgumentException.class, () -> new CloudPush(APP_ID, KEY).withEndpoint(""));
        CloudPush push = push();
        PushTarget target = PushTarget.of("fcm", "t");
        assertThrows(IllegalArgumentException.class, () -> push.send(null, target));
        assertThrows(IllegalArgumentException.class,
                () -> push.send(PushNotification.builder().title("only a title").build(), target));
        assertThrows(IllegalArgumentException.class, () -> push.send(hello()));
        assertThrows(IllegalArgumentException.class,
                () -> push.send(hello(), (PushTarget[]) null));
        assertThrows(IllegalArgumentException.class,
                () -> push.send(hello(), new ArrayList<PushTarget>()));
        List raw = new ArrayList();
        raw.add("cn1-fcm-token");
        assertThrows(IllegalArgumentException.class, () -> push.send(hello(), raw));
        assertEquals(0, requests.get());
    }

    @Test
    @DisplayName("neither the key nor a token appears in toString")
    void nothingSecretInToString() {
        assertFalse(push().toString().contains(KEY));
        assertFalse(PushTarget.of("fcm", "secret-token").toString().contains("secret-token"));
        assertEquals(CloudPush.DEFAULT_ENDPOINT, new CloudPush(APP_ID, KEY).getEndpoint());
    }

    @Test
    @DisplayName("Retry-After takes delta seconds and nothing else")
    void retryAfterParsing() {
        assertEquals(120, CloudPush.parseRetryAfter(" 120 "));
        assertEquals(-1, CloudPush.parseRetryAfter(null));
        assertEquals(-1, CloudPush.parseRetryAfter(""));
        assertEquals(-1, CloudPush.parseRetryAfter("Wed, 21 Oct 2026 07:28:00 GMT"));
        assertEquals(-1, CloudPush.parseRetryAfter("-5"));
        assertEquals(-1, CloudPush.parseRetryAfter("99999999999"));
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) > 0) {
            out.write(chunk, 0, n);
        }
        return out.toByteArray();
    }

    @Test
    @DisplayName("a Turkish default locale doesn't change provider matching")
    void turkishLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("apns", PushTarget.of("IOS", "t").getProvider());
            assertEquals("huawei", PushTarget.of("HMS", "t").getProvider());
        } finally {
            Locale.setDefault(saved);
        }
    }
}
