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

import com.codename1.components.ChatBubble;
import com.codename1.components.ChatView;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;

/// Convenience wiring that turns a [ChatView] into an LLM-driven
/// chat surface in one call. Pulls the user's input out of the
/// view's [com.codename1.components.ChatInput], appends it as a USER
/// message, opens a streaming chat call against the supplied
/// [LlmClient], and pipes deltas into a freshly-appended assistant
/// bubble.
///
/// `ChatView` itself has no dependency on the AI package, so apps
/// that want a peer-to-peer messaging UI (a WhatsApp clone, for
/// example) can keep using `ChatView` without pulling LlmClient
/// onto their classpath -- it's only when you call
/// [#bind(ChatView, LlmClient, ChatRequest)] that the binding is
/// established.
///
/// #### Example
///
/// ```
/// LlmClient client = LlmClient.openai(SecureStorage.getInstance().get("openai_key"));
/// ChatRequest base = ChatRequest.builder()
///         .model("gpt-4o-mini")
///         .addMessage(ChatMessage.system("You are a terse assistant."))
///         .build();
/// ChatView view = new ChatView();
/// LlmChatBinding.bind(view, client, base);
/// // ...add view to a Form and that's it.
/// ```
///
/// The view's accumulated history is replayed on every turn so the
/// model has full conversation context. The original `baseRequest`
/// is treated as a template -- its model, tools, temperature, etc.
/// are preserved across turns; its messages are used only when the
/// view's own history is empty (e.g. to seed a system prompt).
public final class LlmChatBinding {

    private LlmChatBinding() {
    }

    public static void bind(final ChatView view,
                            final LlmClient client,
                            final ChatRequest baseRequest) {
        view.setOnSend(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String text = view.getInput().getText();
                if (text == null || text.length() == 0) {
                    return;
                }
                view.getInput().clear();
                view.addMessage(ChatMessage.user(text));
                view.setTypingIndicatorVisible(true);
                final ChatBubble assistant = view.beginAssistantStream();
                ChatRequest replay = baseRequest.toBuilder()
                        .messages(buildOutgoingMessages(view, baseRequest))
                        .build();
                AsyncResource<ChatResponse> result = client.chatStream(replay,
                        new StreamingListener() {
                            @Override
                            public void onContentDelta(String textDelta) {
                                assistant.appendText(textDelta);
                            }

                            @Override
                            public void onToolCallDelta(int index, String id, String name, String argumentsFragment) {
                                // The default binding doesn't surface
                                // tool calls -- apps that use tools
                                // should wire up their own handler
                                // around client.chatStream(...).
                            }

                            @Override
                            public void onUsage(Usage usage) {
                            }

                            @Override
                            public void onError(Throwable t) {
                                assistant.appendText("\n\n[error: " + t.getMessage() + "]");
                            }
                        });
                result.ready(new SuccessCallback<ChatResponse>() {
                    @Override
                    public void onSucess(ChatResponse arg) {
                        view.setTypingIndicatorVisible(false);
                    }
                });
            }
        });
    }

    private static List<ChatMessage> buildOutgoingMessages(ChatView view, ChatRequest baseRequest) {
        List<ChatMessage> history = view.getHistory();
        if (history.isEmpty()) {
            return baseRequest.getMessages();
        }
        return new ArrayList<ChatMessage>(history);
    }
}
