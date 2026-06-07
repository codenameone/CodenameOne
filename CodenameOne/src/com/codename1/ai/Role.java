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

/// Author of a [ChatMessage]. Maps directly to the role string sent on
/// the wire by every supported LLM provider.
public enum Role {
    /// Developer-supplied instructions that steer the assistant. Sent
    /// once at the head of the conversation. Gemini receives these as
    /// `systemInstruction` rather than a normal message; the Gemini
    /// client handles the conversion internally.
    SYSTEM,

    /// End-user content (text, images, tool results).
    USER,

    /// Model output. When the assistant calls a tool the
    /// [ChatMessage] also carries one or more [ToolCall] entries.
    ASSISTANT,

    /// Result of a function/tool invocation, sent back to the model so
    /// it can continue reasoning. Paired with the originating
    /// [ToolCall] via [ChatMessage#getToolCallId()].
    TOOL
}
