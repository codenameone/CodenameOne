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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// A single turn in a chat conversation. Holds a [Role], one or more
/// [MessagePart]s, and (for assistant turns) any [ToolCall]s the model
/// produced. Construct via the static helpers ([#user(String)],
/// [#system(String)], etc.) for the common case, or pass parts
/// directly for multi-modal messages.
public final class ChatMessage {
    private final Role role;
    private final List<MessagePart> parts;
    private final List<ToolCall> toolCalls;
    private final String name;
    private final String toolCallId;

    public ChatMessage(Role role, List<MessagePart> parts) {
        this(role, parts, null, null, null);
    }

    public ChatMessage(Role role, List<MessagePart> parts, List<ToolCall> toolCalls,
                       String name, String toolCallId) {
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        this.role = role;
        this.parts = parts == null ? Collections.<MessagePart>emptyList()
                : Collections.unmodifiableList(new ArrayList<MessagePart>(parts));
        this.toolCalls = toolCalls == null ? Collections.<ToolCall>emptyList()
                : Collections.unmodifiableList(new ArrayList<ToolCall>(toolCalls));
        this.name = name;
        this.toolCallId = toolCallId;
    }

    public static ChatMessage system(String text) {
        return single(Role.SYSTEM, new TextPart(text));
    }

    public static ChatMessage user(String text) {
        return single(Role.USER, new TextPart(text));
    }

    public static ChatMessage assistant(String text) {
        return single(Role.ASSISTANT, new TextPart(text));
    }

    /// Builds a USER message containing both a text and image part --
    /// the common multi-modal pattern.
    public static ChatMessage userWithImage(String text, ImagePart image) {
        List<MessagePart> parts = new ArrayList<MessagePart>(2);
        if (text != null && text.length() > 0) {
            parts.add(new TextPart(text));
        }
        parts.add(image);
        return new ChatMessage(Role.USER, parts);
    }

    /// Builds a TOOL message wrapping the result of a previous tool call.
    public static ChatMessage toolResult(String toolCallId, String resultJson) {
        return new ChatMessage(Role.TOOL,
                Arrays.<MessagePart>asList(new ToolResultPart(toolCallId, resultJson)),
                null, null, toolCallId);
    }

    private static ChatMessage single(Role r, MessagePart p) {
        List<MessagePart> parts = new ArrayList<MessagePart>(1);
        parts.add(p);
        return new ChatMessage(r, parts);
    }

    public Role getRole() {
        return role;
    }

    public List<MessagePart> getParts() {
        return parts;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public String getName() {
        return name;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    /// Convenience: concatenates the text of every [TextPart]. Image
    /// and tool-result parts are skipped. Useful for `ChatView`
    /// rendering when you don't care about multi-modal content.
    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (MessagePart p : parts) {
            if (p instanceof TextPart) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(((TextPart) p).getText());
            }
        }
        return sb.toString();
    }
}
