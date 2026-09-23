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
package com.codenameone.developerguide.backend;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RestController;
import com.codename1.io.JSONParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Receives the delivery-feedback digest and prunes the device keys the
 * providers rejected. Referenced from the Push notifications chapter.
 */
// tag::push-feedback-backend[]
@RestController
@RequestMapping("/push")
public class PushFeedback {

    /** The signing secret shown in Push > Settings. Read it from configuration. */
    private static final String SECRET = System.getenv("CN1_PUSH_CALLBACK_SECRET");

    /** Reject a digest whose timestamp is older than this, to bound replay. */
    private static final long MAX_AGE_MS = 5 * 60 * 1000L;

    /** Stand-in for your device table. */
    private final Set<String> deviceKeys = ConcurrentHashMap.newKeySet();

    /** Digests already applied, so a resend changes nothing. */
    private final Set<String> seenDeliveries = ConcurrentHashMap.newKeySet();

    @PostMapping("/feedback")
    public HttpServer.Response feedback(HttpServer.Request request) throws Exception {
        String body = request.getBody();
        if (!verified(request.getHeader("X-CN1-Signature"), body)) {
            // Anything but 2xx makes the sender keep the window and resend it,
            // which is what you want while a secret rotation is half-applied.
            return new HttpServer.Response(401, "text/plain",
                    "bad signature".getBytes(StandardCharsets.UTF_8));
        }
        Map digest = JSONParser.parseJSON(body);
        List events = (List) digest.get("events");
        if (events != null) {
            for (Object entry : events) {
                Map event = (Map) entry;
                // Delivery ids repeat: a digest is at-least-once, and a
                // truncated one is followed by the next page immediately.
                if (!seenDeliveries.add((String) event.get("deliveryId"))) {
                    continue;
                }
                if ("INVALID_TARGET".equals(event.get("reason"))) {
                    // The provider says this key is dead. Match on token
                    // rather than device: cn1-gcm- and cn1-fcm- are accepted
                    // aliases for the same key and the digest reports the
                    // canonical one.
                    deviceKeys.remove((String) event.get("token"));
                }
            }
        }
        // 2xx is an acknowledgement that this window is durably applied: it
        // advances the sender's watermark and the window is never sent again.
        // Answer it after the writes above have committed, not before.
        return new HttpServer.Response(200, "text/plain",
                "ok".getBytes(StandardCharsets.UTF_8));
    }

    private static boolean verified(String header, String body) throws Exception {
        if (header == null || SECRET == null) {
            return false;
        }
        long timestamp = 0;
        String provided = null;
        for (String part : header.split(",")) {
            int equals = part.indexOf('=');
            if (equals < 1) {
                continue;
            }
            String name = part.substring(0, equals).trim();
            String value = part.substring(equals + 1).trim();
            if ("t".equals(name)) {
                timestamp = Long.parseLong(value);
            } else if ("v1".equals(name)) {
                provided = value;
            }
        }
        if (provided == null
                || Math.abs(System.currentTimeMillis() - timestamp) > MAX_AGE_MS) {
            return false;
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        StringBuilder expected = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            expected.append(Character.forDigit((b >> 4) & 0xf, 16))
                    .append(Character.forDigit(b & 0xf, 16));
        }
        return constantTimeEquals(expected.toString(), provided);
    }

    /** Never compare a signature with equals(): it returns on the first
     * differing byte and that timing is enough to recover one. */
    private static boolean constantTimeEquals(String expected, String provided) {
        if (expected.length() != provided.length()) {
            return false;
        }
        int difference = 0;
        for (int i = 0; i < expected.length(); i++) {
            difference |= expected.charAt(i) ^ provided.charAt(i);
        }
        return difference == 0;
    }
}
// end::push-feedback-backend[]
