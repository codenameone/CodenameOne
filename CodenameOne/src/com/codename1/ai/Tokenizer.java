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

/// Rough best-effort token counting. Useful for the common case of
/// "am I likely to exceed this model's context window?" without
/// shipping the full BPE table (cl100k_base is ~1.7 MB which is
/// substantial for a mobile binary).
///
/// The rule of thumb is **1 token ~= 4 characters** of English text,
/// which holds within ~10-15% for typical chat traffic. For non-Latin
/// scripts the ratio is closer to 1:1, so we clamp the lower bound at
/// the rough number of words. Apps that need exact accounting should
/// fetch a usage value from the API response and adjust their budget.
public final class Tokenizer {

    private Tokenizer() {
    }

    /// Approximate token count for `text`.
    public static int estimate(String text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        int characters = text.length();
        int byChars = Math.max(1, characters / 4);
        int words = 0;
        boolean inWord = false;
        for (int i = 0; i < characters; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                inWord = false;
            } else if (!inWord) {
                inWord = true;
                words++;
            }
        }
        return Math.max(byChars, words);
    }

    /// Estimate the prompt-tokens cost of an entire conversation.
    /// Adds a small fixed overhead per message to approximate the
    /// role / formatting tokens the provider includes.
    public static int estimateMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ChatMessage m : messages) {
            total += 4; // role + framing overhead
            total += estimate(m.getText());
        }
        return total + 2; // priming
    }
}
