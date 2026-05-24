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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The terminal response from a chat call. For streaming requests, the
/// `ChatResponse` carries the *aggregated* final assistant message —
/// the individual deltas were delivered through [StreamingListener]
/// before this object was produced.
///
/// `finishReason` is one of: `"stop"`, `"length"`, `"tool_calls"`,
/// `"content_filter"`, `"error"` (normalized across providers).
public final class ChatResponse {
    private final ChatMessage assistantMessage;
    private final List<ToolCall> toolCalls;
    private final String finishReason;
    private final Usage usage;
    private final String modelUsed;

    public ChatResponse(ChatMessage assistantMessage, List<ToolCall> toolCalls,
                        String finishReason, Usage usage, String modelUsed) {
        this.assistantMessage = assistantMessage;
        this.toolCalls = toolCalls == null ? Collections.<ToolCall>emptyList()
                : Collections.unmodifiableList(new ArrayList<ToolCall>(toolCalls));
        this.finishReason = finishReason;
        this.usage = usage;
        this.modelUsed = modelUsed;
    }

    public ChatMessage getAssistantMessage() {
        return assistantMessage;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    /// Convenience: the assembled assistant text. Equivalent to
    /// `getAssistantMessage().getText()` when there is one.
    public String getText() {
        return assistantMessage == null ? "" : assistantMessage.getText();
    }

    public String getFinishReason() {
        return finishReason;
    }

    public Usage getUsage() {
        return usage;
    }

    public String getModelUsed() {
        return modelUsed;
    }
}
