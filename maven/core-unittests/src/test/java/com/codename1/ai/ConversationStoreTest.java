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
package com.codename1.ai;

import com.codename1.io.Storage;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip and edge-case coverage for {@link ConversationStore}, the
 * JSON-backed persistent chat history. Uses the in-memory {@link Storage}
 * provided by the test implementation.
 */
class ConversationStoreTest extends UITestBase {

    private static final String KEY = "conversation-store-test";

    @Test
    void constructorRejectsNullKey() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new ConversationStore(null);
            }
        });
    }

    @Test
    void constructorRejectsEmptyKey() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new ConversationStore("");
            }
        });
    }

    @Test
    void getStorageKeyReturnsTheConfiguredKey() {
        assertEquals(KEY, new ConversationStore(KEY).getStorageKey());
    }

    @Test
    void loadOnMissingKeyReturnsEmptyList() throws Exception {
        ConversationStore store = new ConversationStore("never-written-key");
        store.clear();
        List<ChatMessage> loaded = store.load();
        assertNotNull(loaded);
        assertTrue(loaded.isEmpty());
    }

    @Test
    void textMessagesRoundTripWithRoles() throws Exception {
        ConversationStore store = new ConversationStore(KEY);
        store.clear();
        List<ChatMessage> in = new ArrayList<ChatMessage>();
        in.add(ChatMessage.system("be helpful"));
        in.add(ChatMessage.user("hello"));
        in.add(ChatMessage.assistant("hi there"));
        store.save(in);

        List<ChatMessage> out = store.load();
        assertEquals(3, out.size());
        assertEquals(Role.SYSTEM, out.get(0).getRole());
        assertEquals("be helpful", out.get(0).getText());
        assertEquals(Role.USER, out.get(1).getRole());
        assertEquals("hello", out.get(1).getText());
        assertEquals(Role.ASSISTANT, out.get(2).getRole());
        assertEquals("hi there", out.get(2).getText());
    }

    @Test
    void toolResultPartsRoundTrip() throws Exception {
        ConversationStore store = new ConversationStore(KEY);
        store.clear();
        store.save(Arrays.asList(ChatMessage.toolResult("call_42", "{\"temp\":21}")));

        List<ChatMessage> out = store.load();
        assertEquals(1, out.size());
        ChatMessage m = out.get(0);
        assertEquals(Role.TOOL, m.getRole());
        assertEquals("call_42", m.getToolCallId());
        assertEquals(1, m.getParts().size());
        MessagePart p = m.getParts().get(0);
        assertTrue(p instanceof ToolResultPart);
        ToolResultPart trp = (ToolResultPart) p;
        assertEquals("call_42", trp.getToolCallId());
        assertEquals("{\"temp\":21}", trp.getResultJson());
    }

    @Test
    void imagePartsArePersistedAsLossyPlaceholder() throws Exception {
        ConversationStore store = new ConversationStore(KEY);
        store.clear();
        ImagePart img = new ImagePart(new byte[]{1, 2, 3}, "image/png");
        store.save(Arrays.asList(ChatMessage.userWithImage("look", img)));

        List<ChatMessage> out = store.load();
        assertEquals(1, out.size());
        // The text part survives verbatim; the image collapses to "[image]".
        assertEquals("look\n[image]", out.get(0).getText());
        for (MessagePart p : out.get(0).getParts()) {
            assertTrue(p instanceof TextPart, "image part should reload as a text placeholder");
        }
    }

    @Test
    void emptyAndNullMessageListsSaveAsEmptyHistory() throws Exception {
        ConversationStore store = new ConversationStore(KEY);
        store.save(null);
        assertTrue(store.load().isEmpty());
        store.save(new ArrayList<ChatMessage>());
        assertTrue(store.load().isEmpty());
    }

    @Test
    void unknownRoleNameFallsBackToUser() throws Exception {
        // Hand-craft a payload carrying a role string that no longer maps to a
        // Role constant; load() must degrade to USER rather than throwing.
        String json = "{\"messages\":[{\"role\":\"WIZARD\",\"parts\":"
                + "[{\"kind\":\"t\",\"text\":\"abracadabra\"}]}]}";
        Storage.getInstance().writeObject(KEY, json.getBytes("UTF-8"));

        List<ChatMessage> out = new ConversationStore(KEY).load();
        assertEquals(1, out.size());
        assertEquals(Role.USER, out.get(0).getRole());
        assertEquals("abracadabra", out.get(0).getText());
    }

    @Test
    void nonByteArrayPayloadIsTreatedAsEmpty() throws Exception {
        // A non-byte[] object under the key (e.g. an old/foreign format) must
        // not crash load(); it yields an empty conversation.
        Storage.getInstance().writeObject(KEY, "this is not a byte array");
        assertTrue(new ConversationStore(KEY).load().isEmpty());
    }

    @Test
    void clearRemovesPersistedConversation() throws Exception {
        ConversationStore store = new ConversationStore(KEY);
        store.save(Arrays.asList(ChatMessage.user("temp")));
        assertFalse(store.load().isEmpty());
        store.clear();
        assertTrue(store.load().isEmpty());
    }
}
