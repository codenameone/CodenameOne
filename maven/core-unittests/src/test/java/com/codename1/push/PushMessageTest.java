/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PushMessageTest {
    @Test
    void roundTripsTheTypedEnvelope() throws Exception {
        PushMessage original = PushMessage.builder()
                .id("message-1")
                .title("Order shipped")
                .body("Order 4815 is on its way")
                .deepLink("myapp://orders/4815")
                .data("orderId", "4815")
                .platform("fcm", Collections.<String, Object>singletonMap("priority", "high"))
                .build();

        PushMessage parsed = PushMessage.parse(original.toJson());

        assertEquals("message-1", parsed.getId());
        assertEquals("Order shipped", parsed.getTitle());
        assertEquals("4815", parsed.getData().get("orderId"));
        assertEquals("high", ((java.util.Map) parsed.getPlatformOptions().get("fcm")).get("priority"));
    }

    @Test
    void rejectsAnEnvelopeWithoutTheCurrentSchema() {
        IOException error = assertThrows(IOException.class,
                () -> PushMessage.parse("{\"schema\":2,\"body\":\"old\"}"));
        assertTrue(error.getMessage().contains("schema"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void nestedEnvelopeValuesAreImmutableCopies() {
        Map<String, Object> surface = new LinkedHashMap<String, Object>();
        surface.put("operation", "widget");
        PushMessage message = PushMessage.builder().data("mutable", surface)
                .surface(surface).build();

        surface.put("operation", "changed");
        assertEquals("widget", message.getSurface().get("operation"));
        Map nested = (Map) message.getData().get("mutable");
        assertEquals("widget", nested.get("operation"));
        assertThrows(UnsupportedOperationException.class,
                () -> nested.put("operation", "changed"));
    }

    @Test
    void nullBuilderMapsRemoveOptionalSections() {
        PushMessage.Builder builder = PushMessage.builder()
                .platform("fcm", Collections.<String, Object>singletonMap(
                        "priority", "high"))
                .surface(Collections.<String, Object>singletonMap(
                        "operation", "widget"));

        builder.build();
        PushMessage message = builder.platform("fcm", null)
                .surface(null)
                .build();

        assertTrue(message.getPlatformOptions().isEmpty());
        assertTrue(message.getSurface().isEmpty());
    }
}
