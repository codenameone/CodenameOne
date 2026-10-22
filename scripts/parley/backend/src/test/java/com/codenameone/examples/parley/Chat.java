/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codename1.backend.test.MockRequestBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// What the messaging tests do over and over: register a device, start a
/// conversation, send to it and read what arrived.
///
/// The keys are fixed P-256 keys made once with the JDK, so the tests need no
/// key generator and run the same on the JVM and compiled. Every device
/// shares them, which the server neither checks nor needs to: it verifies the
/// signed prekey's signature and stores the rest.
final class Chat {
    static final String ID_PRIVATE = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCA+cgwhxC0w35Hc"
            + "yMmVWGYL/gzzYdopjf076fYLCMevew==";
    static final String ID_PUBLIC = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE6mmSTMk3o4Wyv4aajmhn"
            + "irxsRZ44N+A+Ggqoqt4RU474gaGUh3LjD5d9kAkGBh2fWfVQ3wUzaFDB/1NTe6qsvQ==";
    static final String[] PRE = {
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEaikjMmq5FjqzWcn/BaUxt/a8h6dyUy/wdyZoLCUysfrE"
                + "QEMeQBGq/pnoyhTPu4wutcB/WedqUFPQ5YToe0QZHg==",
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE0ibZeHK/8+xCnv06AbiJAQRRIQqMmrdticrBxnjZsB/N"
                + "8eWgZYb5S8oIzRIULsjDX7pE64ibITzKtTA/YYjGkg==",
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEJKs30j5f8PRr0dzUrr+znuT4POCRe7UQs11OWYY2qVCR"
                + "LbdAfz32+e3shvzcmbj+/OPRUy+tx+ZHrO7Um4zY0g==",
    };

    private Chat() {
    }

    static MockRequestBuilder body(MockRequestBuilder request, Object value) {
        return request.contentType("application/json")
                .content(value instanceof String ? (String) value : Json.write(value));
    }

    static Map json(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return Json.parseObject(ok(mvc, request));
    }

    static List list(MockMvc mvc, MockRequestBuilder request) throws Exception {
        Object value = Json.parse(ok(mvc, request));
        return value instanceof List ? (List) value : null;
    }

    /// The body of a request that must succeed; a failure says what the
    /// server answered, which is what a test that broke needs to know.
    private static String ok(MockMvc mvc, MockRequestBuilder request) throws Exception {
        MockResponse response = mvc.perform(request).andReturn().getResponse();
        String body = response.getContentAsString();
        assertEquals(200, response.getStatus(), body);
        return body;
    }

    static int statusOf(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    static long number(Map object, String field) {
        Object value = object.get(field);
        return value instanceof Number ? ((Number) value).longValue() : -1L;
    }

    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int iter = 0; iter < pairs.length; iter += 2) {
            out.put((String) pairs[iter], pairs[iter + 1]);
        }
        return out;
    }

    /// A device registration whose signed prekey is `signedKey`, signed over
    /// `signedOver`: the same key for an honest one.
    static Map<String, Object> registration(String signedKey, String signedOver) throws Exception {
        String signature = Base64.encode(Crypto.sign(Crypto.ES256, Base64.decode(ID_PRIVATE),
                Base64.decode(signedOver)));
        List<Object> oneTime = new ArrayList<>();
        oneTime.add(map("id", 1, "publicKey", PRE[1]));
        oneTime.add(map("id", 2, "publicKey", PRE[2]));
        return map("name", "Test phone", "platform", "simulator", "identityKey", ID_PUBLIC,
                "signedPreKey", map("id", 1, "publicKey", signedKey, "signature", signature),
                "oneTimePreKeys", oneTime);
    }

    /// Registers a device for `username` and answers its id.
    static String device(MockMvc mvc, String username) throws Exception {
        Map device = json(mvc, People.asUser(body(post("/api/devices"),
                registration(PRE[0], PRE[0])), username));
        return (String) device.get("id");
    }

    /// Starts a conversation and answers its id.
    static String conversation(MockMvc mvc, String owner, String kind, String title,
            String... members) throws Exception {
        List<String> others = new ArrayList<>();
        for (String member : members) {
            others.add(member);
        }
        Map made = json(mvc, People.asUser(body(post("/api/conversations"),
                map("kind", kind, "title", title, "members", others)), owner));
        return (String) made.get("id");
    }

    /// A copy of a message for one device: the header and ciphertext are
    /// stand-ins, which the server cannot tell from real ones.
    static Map<String, Object> copy(String username, String deviceId) {
        return map("username", username, "deviceId", deviceId, "header", "h:" + deviceId,
                "ciphertext", sealed(deviceId));
    }

    static String sealed(String deviceId) {
        return Base64.encode(("sealed for " + deviceId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /// Sends a MESSAGE with the given copies; answers the send result.
    static Map send(MockMvc mvc, String username, String device, String conversation,
            String clientId, List<Object> copies) throws Exception {
        return json(mvc, People.asUser(body(post("/api/messages"), map("conversationId",
                conversation, "senderDevice", device, "clientId", clientId, "kind", "MESSAGE",
                "copies", copies, "plaintext", "")), username));
    }

    /// Sends plain text to a bridged or bot conversation.
    static Map say(MockMvc mvc, String username, String device, String conversation,
            String clientId, String text) throws Exception {
        return json(mvc, People.asUser(body(post("/api/messages"), map("conversationId",
                conversation, "senderDevice", device, "clientId", clientId, "kind", "MESSAGE",
                "copies", new ArrayList<Object>(), "plaintext", text)), username));
    }

    static List<Object> copies(Object... copies) {
        List<Object> out = new ArrayList<>();
        for (Object copy : copies) {
            out.add(copy);
        }
        return out;
    }

    /// The envelopes waiting for a device.
    static List envelopes(MockMvc mvc, String username, String device) throws Exception {
        Map sync = json(mvc, People.asUser(get("/api/messages/" + device + "?limit=100"), username));
        return (List) sync.get("envelopes");
    }

    /// The text of a plain-text envelope.
    static String text(Map envelope) {
        byte[] bytes = Base64.decode((String) envelope.get("ciphertext"));
        return bytes == null ? null : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /// Waits up to five seconds for an envelope from `sender` to reach a device;
    /// answers it, or null.
    static Map await(MockMvc mvc, String username, String device, String sender)
            throws Exception {
        long until = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < until) {
            for (Object each : envelopes(mvc, username, device)) {
                if (each instanceof Map && sender.equals(((Map) each).get("sender"))
                        && "MESSAGE".equals(((Map) each).get("kind"))) {
                    return (Map) each;
                }
            }
            Thread.sleep(100);
        }
        return null;
    }
}
