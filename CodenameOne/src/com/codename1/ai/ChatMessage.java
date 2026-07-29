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

    /// Creates a message with no assistant tool calls or provider name.
    /// @param role speaker role; required
    /// @param parts ordered multimodal content; {@code null} becomes empty
    public ChatMessage(Role role, List<MessagePart> parts) {
        this(role, parts, null, null, null);
    }

    /// Creates a fully specified normalized message.
    /// @param role speaker role; required
    /// @param parts ordered content parts; {@code null} becomes empty
    /// @param toolCalls assistant tool calls; {@code null} becomes empty
    /// @param name optional provider-visible participant name
    /// @param toolCallId tool call answered by a {@link Role#TOOL} message
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

    /// @param text system instruction text
    /// @return a single-part system message
    public static ChatMessage system(String text) {
        return single(Role.SYSTEM, new TextPart(text));
    }

    /// @param text user input text
    /// @return a single-part user message
    public static ChatMessage user(String text) {
        return single(Role.USER, new TextPart(text));
    }

    /// @param text assistant output text
    /// @return a single-part assistant message
    public static ChatMessage assistant(String text) {
        return single(Role.ASSISTANT, new TextPart(text));
    }

    /// Builds a USER message containing both a text and image part --
    /// the common multi-modal pattern.
    /// @param text optional text accompanying the image
    /// @param image image content sent to the model
    /// @return multimodal user message
    public static ChatMessage userWithImage(String text, ImagePart image) {
        List<MessagePart> parts = new ArrayList<MessagePart>(2);
        if (text != null && text.length() > 0) {
            parts.add(new TextPart(text));
        }
        parts.add(image);
        return new ChatMessage(Role.USER, parts);
    }

    /// Builds a TOOL message wrapping the result of a previous tool call.
    /// @param toolCallId provider id of the answered tool call
    /// @param resultJson application result encoded as JSON
    /// @return tool-role result message
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

    /// @return speaker role
    public Role getRole() {
        return role;
    }

    /// @return immutable content parts in message order
    public List<MessagePart> getParts() {
        return parts;
    }

    /// @return immutable assistant tool calls
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    /// @return optional participant name, or {@code null}
    public String getName() {
        return name;
    }

    /// @return answered tool-call id for tool messages, or {@code null}
    public String getToolCallId() {
        return toolCallId;
    }

    /// Convenience: concatenates the text of every [TextPart]. Image
    /// and tool-result parts are skipped. Useful for `ChatView`
    /// rendering when you don't care about multi-modal content.
    /// @return text parts joined with newlines
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
