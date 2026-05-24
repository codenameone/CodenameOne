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
package com.codename1.ai;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ChatRequestBuilderTest {

    @Test
    void buildsRequestWithAllFields() {
        ChatRequest req = ChatRequest.builder()
                .model("gpt-4o-mini")
                .addMessage(ChatMessage.system("be terse"))
                .addMessage(ChatMessage.user("hi"))
                .temperature(0.7f)
                .maxTokens(256)
                .topP(0.95f)
                .seed(42L)
                .stopSequences(Arrays.asList("STOP"))
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();
        assertEquals("gpt-4o-mini", req.getModel());
        assertEquals(2, req.getMessages().size());
        assertEquals(0.7f, req.getTemperature().floatValue(), 1e-6);
        assertEquals(256, req.getMaxTokens().intValue());
        assertEquals(ResponseFormat.JSON_OBJECT, req.getResponseFormat());
    }

    @Test
    void rejectsEmptyMessageList() {
        // A request with no messages is meaningless and every
        // provider rejects it with a 400 — fail fast on the client
        // instead so the user gets a stack trace pointing at the
        // builder rather than a network round-trip.
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() throws Throwable {
                ChatRequest.builder().model("gpt-4o-mini").build();
            }
        });
    }

    @Test
    void toBuilderRoundTripsAllFields() {
        ChatRequest original = ChatRequest.builder()
                .model("gpt-4o-mini")
                .addMessage(ChatMessage.user("hello"))
                .temperature(0.5f)
                .maxTokens(100)
                .build();
        ChatRequest derived = original.toBuilder()
                .temperature(0.9f)
                .build();
        // Reasoning: toBuilder() lets callers replay a request with
        // one field changed. The original should be untouched.
        assertEquals(0.5f, original.getTemperature().floatValue(), 1e-6);
        assertEquals(0.9f, derived.getTemperature().floatValue(), 1e-6);
        assertEquals(original.getModel(), derived.getModel());
        assertEquals(original.getMessages().size(), derived.getMessages().size());
    }

    @Test
    void nullNumericFieldsMeansDontSend() {
        // The wire-format contract is: don't emit a field when its
        // boxed value is null, so providers fall back to their own
        // defaults instead of one we picked. JsonHelper.serialize()
        // is responsible for the omission; ChatRequest just has to
        // preserve nullness.
        ChatRequest req = ChatRequest.builder()
                .model("m")
                .addMessage(ChatMessage.user("x"))
                .build();
        assertNull(req.getTemperature());
        assertNull(req.getMaxTokens());
        assertNull(req.getTopP());
        assertNull(req.getSeed());
    }

    @Test
    void chatMessageGetTextConcatenatesTextParts() {
        ChatMessage m = new ChatMessage(Role.USER, Arrays.<MessagePart>asList(
                new TextPart("hello"),
                new ImagePart("https://example.com/img.png"),
                new TextPart("world")));
        // Image parts are skipped; text parts join on newlines so
        // ChatView can render a stripped-down preview safely.
        assertEquals("hello\nworld", m.getText());
    }
}
