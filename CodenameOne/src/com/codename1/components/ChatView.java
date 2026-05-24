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
import com.codename1.ai.ChatRequest;
import com.codename1.ai.ChatResponse;
import com.codename1.ai.LlmClient;
import com.codename1.ai.Role;
import com.codename1.ai.StreamingListener;
import com.codename1.ai.Usage;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;

/// A scrollable, theme-aware chat surface.
///
/// `ChatView` holds a vertical stack of [ChatBubble]s rendered from
/// [ChatMessage]s plus a [ChatInput] strip at the bottom. The stack
/// auto-scrolls to the latest message when one is added.
///
/// #### Streaming
///
/// During an LLM streaming call, the typical pattern is:
///
/// ```
/// chatView.addMessage(ChatMessage.user(userText));
/// ChatBubble assistant = chatView.beginAssistantStream();
/// client.chatStream(req, new StreamingListener.Adapter() {
///     public void onContentDelta(String d) { assistant.appendText(d); }
/// });
/// ```
///
/// Use [#bindToLlm(LlmClient, ChatRequest)] to wire the entire
/// flow in one call when the surrounding logic is straightforward.
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
    public ChatBubble addMessage(final ChatMessage message) {
        final ChatBubble[] out = new ChatBubble[1];
        Runnable r = new Runnable() {
            public void run() {
                ChatBubble b = createBubble(message);
                history.add(message);
                bubbles.add(b);
                messages.add(b);
                messages.revalidateLater();
                messages.scrollComponentToVisible(b);
                out[0] = b;
            }
        };
        if (Display.getInstance().isEdt()) {
            r.run();
        } else {
            // Wait for the EDT to insert so we can return the bubble
            // synchronously to the caller. The caller is on a worker
            // thread by definition (we just checked); blocking here
            // is fine.
            Display.getInstance().callSeriallyAndWait(r);
        }
        return out[0];
    }

    /// Convenience that appends an empty assistant bubble for an
    /// upcoming streaming response. Returns the bubble so the
    /// caller's `StreamingListener` can [ChatBubble#appendText] into
    /// it.
    public ChatBubble beginAssistantStream() {
        return addMessage(new ChatMessage(Role.ASSISTANT,
                java.util.Arrays.<com.codename1.ai.MessagePart>asList(
                        new com.codename1.ai.TextPart(""))));
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
        return java.util.Collections.unmodifiableList(history);
    }

    /// Wires this view to an [LlmClient] so the user can type in the
    /// input bar and see streamed responses appear automatically.
    /// `baseRequest` carries model, temperature, tools, etc.; the
    /// view's accumulated history is substituted for `messages` on
    /// every turn so [#addMessage(ChatMessage)] calls (and any
    /// initial system message you've already supplied) participate.
    public void bindToLlm(final LlmClient client, final ChatRequest baseRequest) {
        setOnSend(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                final String text = input.getText();
                if (text == null || text.length() == 0) {
                    return;
                }
                input.clear();
                addMessage(ChatMessage.user(text));
                setTypingIndicatorVisible(true);
                final ChatBubble assistant = beginAssistantStream();
                ChatRequest replay = baseRequest.toBuilder()
                        .messages(buildOutgoingMessages(baseRequest))
                        .build();
                AsyncResource<ChatResponse> result = client.chatStream(replay,
                        new StreamingListener() {
                            public void onContentDelta(String textDelta) {
                                assistant.appendText(textDelta);
                            }

                            public void onToolCallDelta(int index, String id, String name, String argumentsFragment) {
                                // Default binding doesn't surface
                                // tool calls -- apps that use tools
                                // should wire up their own handler.
                            }

                            public void onUsage(Usage usage) {
                            }

                            public void onError(Throwable t) {
                                assistant.appendText("\n\n[error: " + t.getMessage() + "]");
                            }
                        });
                result.ready(new SuccessCallback<ChatResponse>() {
                    public void onSucess(ChatResponse arg) {
                        setTypingIndicatorVisible(false);
                    }
                });
            }
        });
    }

    private List<ChatMessage> buildOutgoingMessages(ChatRequest baseRequest) {
        // Prefer the live history (which now includes the just-added
        // user message). Fall back to whatever was in the base
        // request if the view has nothing yet -- useful when the
        // app pre-loads a system prompt via baseRequest.
        if (history.isEmpty()) {
            return baseRequest.getMessages();
        }
        return new ArrayList<ChatMessage>(history);
    }

    /// Override to swap in a custom bubble renderer (e.g. one that
    /// understands markdown). Default delegates to [ChatBubble].
    protected ChatBubble createBubble(ChatMessage message) {
        return new ChatBubble(message);
    }
}
