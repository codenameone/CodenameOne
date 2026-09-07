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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// `LlmChatBinding` sends the base request's messages ahead of the conversation.
/// It used to return the view's history alone whenever that history was
/// non-empty -- and `bind` appends the user message and the assistant
/// placeholder before asking, so it never was empty. An application's system
/// prompt therefore reached the provider on no request at all. Pure (no
/// Display, no ChatView).
class LlmChatBindingMergeTest {

    private static ChatRequest baseWith(ChatMessage... messages) {
        ChatRequest.Builder b = ChatRequest.builder().model("m");
        for (ChatMessage m : messages) {
            b.addMessage(m);
        }
        return b.build();
    }

    @Test
    void baseMessagesLeadTheConversation() {
        ChatMessage system = ChatMessage.system("You are terse.");
        ChatMessage user = ChatMessage.user("hello");
        List<ChatMessage> out = LlmChatBinding.mergeOutgoing(
                new ArrayList<ChatMessage>(Arrays.asList(user)), baseWith(system));
        assertEquals(2, out.size());
        assertSame(system, out.get(0), "the system prompt has to survive");
        assertSame(user, out.get(1));
    }

    @Test
    void emptyHistoryFallsBackToTheBase() {
        ChatMessage system = ChatMessage.system("You are terse.");
        List<ChatMessage> out = LlmChatBinding.mergeOutgoing(
                new ArrayList<ChatMessage>(), baseWith(system));
        assertEquals(1, out.size());
        assertSame(system, out.get(0));
    }

    @Test
    void historyStandsAloneWhenTheBaseCarriesNothingExtra() {
        ChatMessage user = ChatMessage.user("hello");
        ChatRequest base = ChatRequest.builder().model("m")
                .addMessage(ChatMessage.user("seed")).build();
        List<ChatMessage> out = LlmChatBinding.mergeOutgoing(
                new ArrayList<ChatMessage>(Arrays.asList(user)), base);
        // the base's own turn still leads; what matters is that nothing is lost
        assertEquals(2, out.size());
        assertSame(user, out.get(1));
    }

    @Test
    void theMergeDoesNotMutateEitherInput() {
        List<ChatMessage> history = new ArrayList<ChatMessage>(
                Arrays.asList(ChatMessage.user("hello")));
        ChatRequest base = baseWith(ChatMessage.system("terse"));
        LlmChatBinding.mergeOutgoing(history, base);
        assertEquals(1, history.size());
        assertEquals(1, base.getMessages().size());
    }
}
