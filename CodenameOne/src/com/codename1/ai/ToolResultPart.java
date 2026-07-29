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

/// The result of a tool invocation, sent back to the model so it can
/// continue reasoning. Pairs with the originating [ToolCall] via the
/// `toolCallId`. The carrying [ChatMessage] should use [Role#TOOL].
/// Result content sent back to a model after executing a requested tool call.
/// The result is JSON text so providers receive the original structured value.
public final class ToolResultPart extends MessagePart {
    private final String toolCallId;
    private final String resultJson;

    /// `resultJson` is the literal JSON string the tool produced. If
    /// the tool result isn't valid JSON, wrap it like
    /// `"{\"text\":\"...\"}"` -- the providers expect JSON-shaped values.
    /// Creates a tool result part.
    /// @param toolCallId id of the {@link ToolCall} being answered
    /// @param resultJson JSON value returned by the application tool
    public ToolResultPart(String toolCallId, String resultJson) {
        this.toolCallId = toolCallId;
        this.resultJson = resultJson == null ? "" : resultJson;
    }

    /// @return provider tool-call id this result answers
    public String getToolCallId() {
        return toolCallId;
    }

    /// @return application result encoded as JSON
    public String getResultJson() {
        return resultJson;
    }
}
