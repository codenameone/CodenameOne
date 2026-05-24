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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {

    @Test
    void emptyOrNullReturnsZero() {
        assertEquals(0, Tokenizer.estimate(null));
        assertEquals(0, Tokenizer.estimate(""));
    }

    @Test
    void englishApproximatesOneTokenPerFourChars() {
        // The 4-chars-per-token rule of thumb. Aim for within 25%
        // — we're not promising precision, just usefulness.
        String text = "This is a reasonably short sentence for token counting.";
        int estimate = Tokenizer.estimate(text);
        // Real cl100k_base BPE counts this at ~11 tokens. Our
        // estimator should land in a similar ballpark.
        assertTrue(estimate >= 9 && estimate <= 15,
                "estimate was " + estimate + " for a ~11-token sentence");
    }

    @Test
    void messagesAccountForRoleOverhead() {
        // The per-message overhead matters because providers count
        // it; under-budgeting the prompt is what triggers the
        // dreaded "ContextLengthExceeded right at production peak".
        int single = Tokenizer.estimate("hi");
        int withOverhead = Tokenizer.estimateMessages(Arrays.asList(ChatMessage.user("hi")));
        assertTrue(withOverhead > single,
                "estimateMessages should add framing tokens on top of estimate");
    }
}
