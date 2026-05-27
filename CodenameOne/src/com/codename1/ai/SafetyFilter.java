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

import java.util.List;

/// Pre-flight gate that inspects messages before they're sent to the
/// model. Implementations may call a moderation API, run a local
/// profanity match, or anything else. Returning a non-null reason
/// blocks the chat call and propagates an [LlmInvalidRequestException].
public interface SafetyFilter {
    /// Returns `null` to allow the call, or a human-readable reason
    /// string to block it.
    String check(List<ChatMessage> messages);

    /// A built-in filter that allows everything. Useful as a default
    /// or as a base for composition. Use `SafetyFilters.openai(key)`
    /// (in `com.codename1.ai.filters` or a separate cn1lib) for the
    /// OpenAI Moderation gate.
    SafetyFilter ALLOW_ALL = new SafetyFilter() {
        @Override
        public String check(List<ChatMessage> messages) {
            return null;
        }
    };
}
