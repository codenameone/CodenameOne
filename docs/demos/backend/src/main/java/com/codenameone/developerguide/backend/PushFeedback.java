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

import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RestController;
import com.codename1.io.JSONParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Receives the delivery-feedback digest and prunes the device keys the
 * providers rejected. Referenced from the Push notifications chapter.
 */
// tag::push-feedback-backend[]
@RestController
@RequestMapping("/push")
public class PushFeedback {

    /**
     * Your organization-scoped durable store. Store provider and bare token (or
     * web endpoint) alongside the original push key; backfill existing rows before
     * enabling cleanup. Both operations belong in ONE transaction: a marker
     * written before the deletion commits turns a retry into a silent skip,
     * and a deletion without a marker is applied twice.
     */
    public interface DeviceStore {
        boolean alreadyApplied(String eventKey);

        // provider + target match normalized registration columns, NOT Push.getPushKey().
        // Enforce a unique eventKey and delete in the same transaction.
        void removeTargetAndMarkApplied(String eventKey, String provider, String target)
                throws Exception;
    }

    /** Assigned once at start-up; there is no dependency injection here. */
    static DeviceStore store;

    /** The signing secret shown in Push > Settings. Read it from configuration. */
    private static final String SECRET = System.getenv("CN1_PUSH_CALLBACK_SECRET");

    /** Reject a digest whose timestamp is older than this, to bound replay. */
    private static final long MAX_AGE_MS = 5 * 60 * 1000L;

    @PostMapping("/feedback")
    public HttpServer.Response feedback(HttpServer.Request request) throws Exception {
        String body = request.getBody();
        if (!verified(request.getHeader("X-CN1-Signature"), body)) {
            // Anything but 2xx keeps the window at the sender and resends it,
            // which is what you want while a secret rotation is half-applied.
            return new HttpServer.Response(401, "text/plain",
                    "bad signature".getBytes(StandardCharsets.UTF_8));
        }
        Map digest = JSONParser.parseJSON(body);
        List events = (List) digest.get("events");
        if (events != null) {
            for (Object entry : events) {
                Map event = (Map) entry;
                if (!"INVALID_TARGET".equals(event.get("reason"))) {
                    continue;
                }
                String provider = (String) event.get("provider");
                String target = (String) event.get("token");
                if (target == null || target.length() == 0) {
                    target = (String) event.get("endpoint");
                }
                if (provider == null || provider.length() == 0
                        || target == null || target.length() == 0) {
                    throw new IllegalArgumentException("Missing push target");
                }
                String deliveryId = (String) event.get("deliveryId");
                String eventKey;
                if (deliveryId != null && deliveryId.length() > 0) {
                    eventKey = "delivery:" + deliveryId;
                } else {
                    Object at = event.get("at");
                    if (at == null) {
                        throw new IllegalArgumentException("Missing classic event timestamp");
                    }
                    eventKey = "classic:" + provider + ":" + target.length()
                            + ":" + target + ":" + at;
                }
                if (!store.alreadyApplied(eventKey)) {
                    store.removeTargetAndMarkApplied(eventKey, provider, target);
                }
            }
        }
        // 2xx acknowledges that this window is durably applied: it advances the
        // sender's watermark and the window is never sent again. Answer it
        // after the writes above have committed, not alongside them.
        return new HttpServer.Response(200, "text/plain",
                "ok".getBytes(StandardCharsets.UTF_8));
    }

    // throws, because String.getBytes(Charset) is a CHECKED throw in the
    // ParparVM class library even though it is not one on a JVM.
    private static boolean verified(String header, String body) throws Exception {
        if (header == null || SECRET == null) {
            return false;
        }
        long timestamp = 0;
        String provided = null;
        // Parsed by hand: java.lang.String has no split() in the ParparVM class
        // library, so the obvious version of this compiles for the development
        // run and fails the native package step.
        int cursor = 0;
        while (cursor < header.length()) {
            int comma = header.indexOf(',', cursor);
            if (comma < 0) {
                comma = header.length();
            }
            String part = header.substring(cursor, comma);
            cursor = comma + 1;
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
        // Crypto, not javax.crypto: this class is recompiled against the
        // ParparVM class library by cn1:backend-package, and that library has
        // no JCE. equalsConstantTime is here for the same reason a hand-written
        // loop would be -- an early exit on the first differing byte lets a MAC
        // be forged one byte at a time.
        byte[] expected = Crypto.hmacSha256(SECRET.getBytes(StandardCharsets.UTF_8),
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        return Crypto.equalsConstantTime(expected, decodeHex(provided));
    }

    private static byte[] decodeHex(String value) {
        if (value.length() % 2 != 0) {
            return new byte[0];
        }
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return new byte[0];
            }
            out[i] = (byte) ((high << 4) | low);
        }
        return out;
    }
}
// end::push-feedback-backend[]
