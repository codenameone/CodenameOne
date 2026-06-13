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
package com.codename1.components;

import com.codename1.ai.ChatMessage;
import com.codename1.ai.Role;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI coverage for {@link ChatView}: the message/history bookkeeping, the
 * streaming helpers, typing indicator, and the input-event delegation to the
 * embedded {@link ChatInput}.
 */
class ChatViewTest extends UITestBase {

    @FormTest
    void freshViewHasEmptyHistoryAndAnInput() {
        ChatView v = new ChatView();
        assertEquals("ChatView", v.getUIID());
        assertNotNull(v.getInput());
        assertTrue(v.getHistory().isEmpty());
    }

    @FormTest
    void addMessageReturnsBubbleAndRecordsHistory() {
        ChatView v = new ChatView();
        ChatMessage m = ChatMessage.user("hi");
        ChatBubble b = v.addMessage(m);
        assertNotNull(b);
        assertEquals("hi", b.getBubbleText());
        List<ChatMessage> history = v.getHistory();
        assertEquals(1, history.size());
        assertSame(m, history.get(0));
    }

    @FormTest
    void historyViewIsUnmodifiable() {
        final ChatView v = new ChatView();
        v.addMessage(ChatMessage.user("hi"));
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                v.getHistory().add(ChatMessage.user("nope"));
            }
        });
    }

    @FormTest
    void messagesAccumulateInOrder() {
        ChatView v = new ChatView();
        v.addMessage(ChatMessage.user("one"));
        v.addMessage(ChatMessage.assistant("two"));
        v.addMessage(ChatMessage.system("three"));
        assertEquals(3, v.getHistory().size());
        assertEquals(Role.USER, v.getHistory().get(0).getRole());
        assertEquals(Role.ASSISTANT, v.getHistory().get(1).getRole());
        assertEquals(Role.SYSTEM, v.getHistory().get(2).getRole());
    }

    @FormTest
    void beginAssistantStreamAddsEmptyAssistantBubble() {
        ChatView v = new ChatView();
        ChatBubble b = v.beginAssistantStream();
        assertNotNull(b);
        assertEquals("", b.getBubbleText());
        assertEquals(1, v.getHistory().size());
        assertEquals(Role.ASSISTANT, v.getHistory().get(0).getRole());
    }

    @FormTest
    void appendToLastMessageStreamsIntoNewestBubble() {
        ChatView v = new ChatView();
        ChatBubble b = v.beginAssistantStream();
        v.appendToLastMessage("Hel");
        v.appendToLastMessage("lo");
        flushSerialCalls();
        assertEquals("Hello", b.getBubbleText());
    }

    @FormTest
    void appendToLastMessageIsNoOpWithoutBubbles() {
        ChatView v = new ChatView();
        // No bubble has been added; this must not throw.
        v.appendToLastMessage("ignored");
        assertTrue(v.getHistory().isEmpty());
    }

    @FormTest
    void typingIndicatorVisibilityToggles() {
        ChatView v = new ChatView();
        v.setTypingIndicatorVisible(true);
        flushSerialCalls();
        // Just exercising the EDT-marshalled path; no exception means success.
        v.setTypingIndicatorVisible(false);
        flushSerialCalls();
    }

    @FormTest
    void setOnSendDelegatesToInputAndFires() {
        ChatView v = new ChatView();
        final AtomicInteger fired = new AtomicInteger();
        v.setOnSend(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.incrementAndGet();
            }
        });
        assertTrue(v.getInput().getSendButton().isVisible());
        v.getInput().setText("hello");
        v.getInput().getSendButton().pressed();
        v.getInput().getSendButton().released();
        flushSerialCalls();
        assertEquals(1, fired.get());
    }

    @FormTest
    void setOnAttachAndVoiceDelegateToInput() {
        ChatView v = new ChatView();
        v.setOnAttach(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        });
        v.setOnVoice(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            }
        });
        assertTrue(v.getInput().getAttachButton().isVisible());
        assertTrue(v.getInput().getVoiceButton().isVisible());
    }

    @FormTest
    void customBubbleRendererIsUsed() {
        final AtomicInteger created = new AtomicInteger();
        ChatView v = new ChatView() {
            @Override
            protected ChatBubble createBubble(ChatMessage message) {
                created.incrementAndGet();
                return new ChatBubble(message);
            }
        };
        v.addMessage(ChatMessage.user("hi"));
        assertEquals(1, created.get());
    }
}
