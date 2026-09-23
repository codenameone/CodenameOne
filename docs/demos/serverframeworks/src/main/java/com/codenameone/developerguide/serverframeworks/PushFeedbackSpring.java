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
// Guide samples for server stacks that are not Codename One. This directory is
// NOT a Maven module and nothing builds it: the guide's snippet validator skips
// includes under src/main/java, so these carry no dependencies and are read
// rather than compiled.

package com.codenameone.developerguide.serverframeworks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Spring receiver for the delivery-feedback digest. */
// tag::push-notifications-java-spring[]
@RestController
public class PushFeedbackSpring {

    private static final long MAX_AGE_MS = 5 * 60 * 1000L;

    // An injected collaborator, NOT a method on this class. Spring's
    // transaction management is proxy-based, so a call from one method of this
    // bean to another of its own never passes through the interceptor, and a
    // @Transactional there would do nothing at all -- without saying so.
    private final PushFeedbackApplier applier;
    private final byte[] secret;

    public PushFeedbackSpring(PushFeedbackApplier applier,
            @Value("${cn1.push.callback.secret}") String secret) {
        this.applier = applier;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    // byte[], not a mapped type: the signature covers the bytes as sent, and
    // anything Jackson parses and re-serialises is a different byte string that
    // never verifies.
    @PostMapping(path = "/push/feedback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> feedback(
            @RequestHeader(name = "X-CN1-Signature", required = false) String signature,
            @RequestBody byte[] raw) throws Exception {
        String body = new String(raw, StandardCharsets.UTF_8);
        if (!verified(signature, body, secret)) {
            // Anything but 2xx keeps the window at the sender and resends it.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad signature");
        }
        JsonNode digest = new ObjectMapper().readTree(body);
        for (JsonNode event : digest.path("events")) {
            applier.apply(event);
        }
        // 2xx advances the sender's watermark, so it is returned only after
        // every event above has committed.
        return ResponseEntity.ok("ok");
    }

    static boolean verified(String header, String body, byte[] secret) throws Exception {
        if (header == null) {
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
            return false;                      // bound replay by age
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        byte[] expected = mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        // MessageDigest.isEqual is the JDK's constant-time comparison. Never
        // Arrays.equals or String.equals here: both return at the first
        // differing byte, and that timing is enough to forge a signature.
        return MessageDigest.isEqual(expected, HexFormat.of().parseHex(provided));
    }
}

@Component
class PushFeedbackApplier {

    private final DeviceStore store;

    PushFeedbackApplier(DeviceStore store) {
        this.store = store;
    }

    /**
     * One transaction per event: the deduplication marker and the deletion have
     * to commit together. A marker written first turns a retry into a silent
     * skip; a deletion without one is applied twice.
     */
    @Transactional
    public void apply(JsonNode event) {
        String deliveryId = event.path("deliveryId").asText();
        if (store.alreadyApplied(deliveryId)) {
            return;                            // digests are at-least-once
        }
        if ("INVALID_TARGET".equals(event.path("reason").asText())) {
            // token is absent for web push, which reports endpoint instead.
            String key = event.hasNonNull("token")
                    ? event.get("token").asText()
                    : event.path("endpoint").asText();
            store.removeKey(key);
        }
        store.markApplied(deliveryId);
    }
}
// end::push-notifications-java-spring[]
