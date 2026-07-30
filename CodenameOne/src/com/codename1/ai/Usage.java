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

/// Token accounting returned by the provider. Any field that the
/// provider didn't return is `-1`.
/// Token counts reported for one provider operation. Providers may count
/// tokens differently, so use these values for billing and diagnostics rather
/// than comparing tokenizers across services.
public final class Usage {
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;

    /// Creates a usage record from provider counters.
    /// @param promptTokens tokens consumed by request input
    /// @param completionTokens tokens generated in the response
    /// @param totalTokens provider-reported total token count
    public Usage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    /// @return tokens consumed by request input
    public int getPromptTokens() {
        return promptTokens;
    }

    /// @return tokens generated in the response
    public int getCompletionTokens() {
        return completionTokens;
    }

    /// @return provider-reported total token count
    public int getTotalTokens() {
        return totalTokens;
    }
}
