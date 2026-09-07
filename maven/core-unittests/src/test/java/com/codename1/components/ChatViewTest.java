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
import java.util.Arrays;
import com.codename1.ai.ToolCall;
import com.codename1.ai.TextPart;
import com.codename1.ai.MessagePart;
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
    void streamedTextReachesTheStoredMessage() {
        // A streamed reply only ever reached the bubble: the ChatMessage stored
        // when beginAssistantStream() created it stayed empty, so anything
        // building a request from getHistory() -- LlmChatBinding included --
        // sent a blank assistant turn for every completed reply.
        ChatView v = new ChatView();
        ChatBubble bubble = v.beginAssistantStream();
        bubble.appendText("Hello");
        bubble.appendText(", world");
        List<ChatMessage> history = v.getHistory();
        assertEquals("Hello, world", history.get(history.size() - 1).getText());
        assertEquals(Role.ASSISTANT, history.get(history.size() - 1).getRole());
    }

    @FormTest
    void annotationsAreShownButStayOutOfHistory() {
        // LlmChatBinding appends "[error: ...]" to the bubble when a stream
        // fails. That is for the reader, not for the model: replaying it would
        // send a network failure back as if the assistant had written it.
        ChatView v = new ChatView();
        ChatBubble bubble = v.beginAssistantStream();
        bubble.appendText("partial answer");
        bubble.appendAnnotation("\n\n[error: connection reset]");
        List<ChatMessage> history = v.getHistory();
        assertEquals("partial answer", history.get(history.size() - 1).getText());
    }

    @FormTest
    void appendingToANonEmptyBubbleKeepsWhatItStartedWith() {
        // A bubble created from a message already shows that text, so the first
        // append must extend it rather than replace the stored message with just
        // the delta.
        ChatView v = new ChatView();
        ChatBubble bubble = v.addMessage(ChatMessage.assistant("Hello"));
        bubble.appendText(" world");
        List<ChatMessage> history = v.getHistory();
        assertEquals("Hello world", history.get(history.size() - 1).getText());
    }

    @FormTest
    void theBubblesOwnMessageTracksItsText() {
        // getMessage() and getHistory() are two ways of asking the same
        // question, so a streamed reply has to reach both.
        ChatView v = new ChatView();
        ChatBubble bubble = v.beginAssistantStream();
        bubble.appendText("Hello");
        assertEquals("Hello", bubble.getMessage().getText());
        assertEquals("Hello", v.getHistory().get(v.getHistory().size() - 1).getText());

        bubble.appendAnnotation(" [note]");
        assertEquals("Hello", bubble.getMessage().getText(),
                "an annotation is not part of the message either");
    }

    @FormTest
    void textAppendedAfterAnAnnotationDoesNotDragItIntoHistory() {
        // The annotation stays in the bubble, so deriving the conversation from
        // the rendered body folds it in on the very next delta -- excluding it
        // for exactly one append and no longer.
        ChatView v = new ChatView();
        ChatBubble bubble = v.beginAssistantStream();
        bubble.appendText("the answer so far");
        bubble.appendAnnotation(" [reconnecting]");
        bubble.appendText(" and the rest");

        List<ChatMessage> history = v.getHistory();
        assertEquals("the answer so far and the rest",
                history.get(history.size() - 1).getText());
    }

    @FormTest
    void setTextReplacesTheConversationOutright() {
        ChatView v = new ChatView();
        ChatBubble bubble = v.beginAssistantStream();
        bubble.appendText("draft");
        bubble.appendAnnotation(" [stale]");
        bubble.setText("final answer");
        bubble.appendText(" plus more");

        List<ChatMessage> history = v.getHistory();
        assertEquals("final answer plus more",
                history.get(history.size() - 1).getText());
    }

    @FormTest
    void annotationsStayOutOfHistoryWhenAppendedOffTheEdt() throws Exception {
        // The exclusion has to survive the hop to the EDT. appendText only
        // queues the update when it is called from another thread, so an
        // exclusion held in a field is already cleared by the time the queued
        // work runs -- and the error annotation lands in history after all.
        // LlmChatBinding's error callback is exactly this case.
        ChatView v = new ChatView();
        final ChatBubble bubble = v.beginAssistantStream();
        bubble.appendText("partial answer");

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                bubble.appendAnnotation("\n\n[error: connection reset]");
            }
        });
        t.start();
        t.join();
        flushSerialCalls();

        List<ChatMessage> history = v.getHistory();
        assertEquals("partial answer", history.get(history.size() - 1).getText());
    }

    @FormTest
    void streamedTextOffTheEdtStillReachesHistory() {
        // The other half of the same dispatch: a queued conversational append
        // must still be recorded.
        ChatView v = new ChatView();
        final ChatBubble bubble = v.beginAssistantStream();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                bubble.appendText("from a worker");
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
        flushSerialCalls();

        List<ChatMessage> history = v.getHistory();
        assertEquals("from a worker", history.get(history.size() - 1).getText());
    }

    @FormTest
    void replacingTheTextKeepsToolCallMetadata() {
        // A message can be a single text part and still carry tool calls. The
        // following tool result refers to their ids, so losing them makes the
        // replay an incomplete sequence that a provider rejects.
        ToolCall call = new ToolCall("call-1", "get_weather", "{}");
        ChatMessage withCall = new ChatMessage(Role.ASSISTANT,
                Arrays.<MessagePart>asList(new TextPart("")),
                Arrays.asList(call), "assistant-1", null);
        ChatView v = new ChatView();
        ChatBubble bubble = v.addMessage(withCall);
        bubble.appendText("checking the weather");
        ChatMessage stored = v.getHistory().get(v.getHistory().size() - 1);
        assertEquals("checking the weather", stored.getText());
        assertEquals(1, stored.getToolCalls().size());
        assertEquals("call-1", stored.getToolCalls().get(0).getId());
        assertEquals("assistant-1", stored.getName());
    }

    @FormTest
    void appendToLastMessageAlsoUpdatesHistory() {
        ChatView v = new ChatView();
        v.beginAssistantStream();
        v.appendToLastMessage("streamed");
        List<ChatMessage> history = v.getHistory();
        assertEquals("streamed", history.get(history.size() - 1).getText());
    }

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
