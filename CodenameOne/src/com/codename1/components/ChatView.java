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
package com.codename1.components;

import com.codename1.ai.ChatMessage;
import com.codename1.ai.MessagePart;
import com.codename1.ai.Role;
import com.codename1.ai.TextPart;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// A scrollable, theme-aware chat surface.
///
/// `ChatView` is a **pure UI component** -- it has no knowledge of
/// LLMs or any specific chat backend. It accepts [ChatMessage]
/// instances (the same data envelope the AI client uses, but
/// equally usable for peer-to-peer messaging) and renders them as
/// styled bubbles. Routing the user's input to a backend and
/// piping responses back into the view is the caller's job. For
/// LLM use there's a ready-made binding -- see
/// `com.codename1.ai.LlmChatBinding` -- but the same surface
/// trivially supports a WhatsApp-style peer chat:
///
/// ```
/// ChatView view = new ChatView();
/// view.setOnSend(evt -> {
///     String text = view.getInput().getText();
///     view.getInput().clear();
///     view.addMessage(ChatMessage.user(text));          // outgoing
///     chatService.send(text, peerReply -> {
///         // peer responses render as "assistant" bubbles
///         view.addMessage(ChatMessage.assistant(peerReply));
///     });
/// });
/// // System / "X joined the chat" notices use ChatMessage.system.
/// ```
///
/// The `USER` / `ASSISTANT` / `SYSTEM` roles map naturally to
/// self / peer / system-notice in a peer chat; rename via CSS
/// (the bubble UIIDs are `ChatBubbleUser`, `ChatBubbleAssistant`,
/// `ChatBubbleSystem`) if the visual treatment needs to differ.
/// Apps that need a totally different message data type can
/// subclass and override [#createBubble(ChatMessage)] to render
/// however they like while still using `ChatMessage` as the
/// transport.
///
/// #### Default UIIDs
///
/// `ChatView`, `ChatViewMessages`, `ChatBubbleUser`,
/// `ChatBubbleAssistant`, `ChatBubbleSystem`, `ChatBubbleText`,
/// `ChatTypingIndicator`, `ChatInput`, `ChatInputField`,
/// `ChatSendButton`, `ChatAttachButton`, `ChatVoiceButton`.
public class ChatView extends Container {
    private final Container messages;
    private final ChatInput input;
    private final Label typing;
    private final List<ChatMessage> history = new ArrayList<ChatMessage>();
    private final List<ChatBubble> bubbles = new ArrayList<ChatBubble>();

    public ChatView() {
        super(new BorderLayout());
        setUIID("ChatView");
        messages = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        messages.setUIID("ChatViewMessages");
        messages.setScrollableY(true);
        typing = new Label("...");
        typing.setUIID("ChatTypingIndicator");
        typing.setVisible(false);
        input = new ChatInput();

        Container bottom = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        bottom.add(typing);
        bottom.add(input);

        add(BorderLayout.CENTER, messages);
        add(BorderLayout.SOUTH, bottom);
    }

    /// Renders `message` as a new [ChatBubble] at the bottom of the
    /// list and scrolls into view. Safe to call from any thread.
    ///
    /// The bubble itself is laid out inside a single-row FlowLayout
    /// container whose alignment is driven by the message role:
    /// USER -> right, ASSISTANT -> left, SYSTEM -> centre. That keeps
    /// the bubble's *visible width* anchored to its text content
    /// rather than stretching across the full chat surface, which is
    /// what makes a chat bubble actually look like a bubble.
    public ChatBubble addMessage(final ChatMessage message) {
        final ChatBubble[] out = new ChatBubble[1];
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ChatBubble b = createBubble(message);
                Container row = new Container(new FlowLayout(alignmentFor(message.getRole())));
                row.setUIID("ChatBubbleRow");
                row.add(b);
                history.add(message);
                bubbles.add(b);
                messages.add(row);
                messages.revalidateLater();
                messages.scrollComponentToVisible(b);
                out[0] = b;
            }
        };
        if (Display.getInstance().isEdt()) {
            r.run();
        } else {
            // Wait for the EDT to insert so we can return the bubble
            // synchronously to the caller.
            Display.getInstance().callSeriallyAndWait(r);
        }
        return out[0];
    }

    /// Appends an empty assistant/peer bubble that subsequent
    /// [ChatBubble#appendText] calls (or [#appendToLastMessage]) can
    /// stream tokens into. Returns the bubble so the caller can
    /// retain the reference -- the typical pattern is to capture it
    /// before opening a streaming network call.
    public ChatBubble beginAssistantStream() {
        return addMessage(new ChatMessage(Role.ASSISTANT,
                Arrays.<MessagePart>asList(new TextPart(""))));
    }

    /// Append a streaming token delta to the most recently added
    /// bubble. Safe to call off-EDT. No-op when there is no
    /// bubble yet.
    public void appendToLastMessage(String delta) {
        if (bubbles.isEmpty()) {
            return;
        }
        bubbles.get(bubbles.size() - 1).appendText(delta);
    }

    public void setTypingIndicatorVisible(final boolean v) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                typing.setVisible(v);
                typing.getParent().revalidateLater();
            }
        };
        if (Display.getInstance().isEdt()) {
            r.run();
        } else {
            Display.getInstance().callSerially(r);
        }
    }

    public void setOnSend(ActionListener listener) {
        input.setOnSend(listener);
    }

    public void setOnAttach(ActionListener listener) {
        input.setOnAttach(listener);
    }

    public void setOnVoice(ActionListener listener) {
        input.setOnVoice(listener);
    }

    public ChatInput getInput() {
        return input;
    }

    public List<ChatMessage> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /// Override to swap in a custom bubble renderer (e.g. one that
    /// understands markdown, shows avatars, or formats peer
    /// messages differently from LLM responses). Default delegates
    /// to [ChatBubble].
    protected ChatBubble createBubble(ChatMessage message) {
        return new ChatBubble(message);
    }

    private static int alignmentFor(Role role) {
        if (role == Role.USER) {
            return com.codename1.ui.Component.RIGHT;
        }
        if (role == Role.SYSTEM) {
            return com.codename1.ui.Component.CENTER;
        }
        return com.codename1.ui.Component.LEFT;
    }
}
