/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.codenameone.examples.bazaar.merchant;

import com.codename1.ai.ChatMessage;
import com.codename1.ai.ChatRequest;
import com.codename1.ai.ChatResponse;
import com.codename1.ai.LlmClient;
import com.codename1.ai.Tool;
import com.codename1.ai.ToolCall;
import com.codename1.util.AsyncResult;
import java.util.ArrayList;
import java.util.List;

/// One question to the merchant assistant: the conversation goes to the
/// model, every tool the model asks for runs here over the store's numbers
/// ([AssistantTools]) and goes back, until the model answers in words.
///
/// The first system message names the task (`bazaar:assistant`); the server
/// replaces it with its own instructions, holds the provider key, and answers
/// by rules when no provider is configured.
final class AssistantRun {
    /// The task marker the server expects.
    static final String TASK = "bazaar:assistant";
    /// Rounds of tool calls before the run gives up.
    static final int MAX_ROUNDS = 4;

    /// The outcome: the answer, or the failure.
    interface Done {
        void answered(String text);

        void failed(Throwable error);
    }

    private final LlmClient client;
    private final AssistantTools tools;
    private final List<Tool> toolList;

    AssistantRun(LlmClient client, AssistantTools tools) {
        this.client = client;
        this.tools = tools;
        this.toolList = tools.tools();
    }

    /// Asks `question` after the earlier turns in `history` (user and
    /// assistant messages, oldest first).
    void ask(List<ChatMessage> history, String question, Done done) {
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        messages.add(ChatMessage.system(TASK));
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(ChatMessage.user(question));
        round(messages, 0, done);
    }

    private void round(final List<ChatMessage> messages, final int round, final Done done) {
        ChatRequest req = ChatRequest.builder().messages(messages).tools(toolList).build();
        client.chat(req).onResult(new AsyncResult<ChatResponse>() {
            @Override
            public void onReady(ChatResponse r, Throwable error) {
                if (error != null) {
                    done.failed(error);
                    return;
                }
                List<ToolCall> calls = r.getToolCalls();
                if (calls == null || calls.isEmpty()) {
                    String text = r.getText();
                    done.answered(text == null ? "" : text.trim());
                    return;
                }
                if (round + 1 >= MAX_ROUNDS) {
                    done.answered("");
                    return;
                }
                messages.add(r.getAssistantMessage());
                for (int iter = 0; iter < calls.size(); iter++) {
                    ToolCall call = calls.get(iter);
                    messages.add(ChatMessage.toolResult(call.getId(), tools.run(call)));
                }
                round(messages, round + 1, done);
            }
        });
    }
}
