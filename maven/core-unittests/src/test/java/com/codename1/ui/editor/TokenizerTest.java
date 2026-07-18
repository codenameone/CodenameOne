/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {
    @Test
    void languageSpecificLexersAvoidKnownFalsePositives() {
        SyntaxHighlightResult css = new Tokenizer(LanguageDef.forName("css"))
                .tokenize("color: red; // not a CSS comment", 0);
        assertFalse(hasKind(css, Tokenizer.COMMENT));
        assertTrue(hasKind(css, Tokenizer.PROPERTY));

        SyntaxHighlightResult json = new Tokenizer(LanguageDef.forName("json"))
                .tokenize("{'not-a-json-string': true, \"real\": -1.5e2}", 0);
        assertEquals(1, countKind(json, Tokenizer.STRING) + countKind(json, Tokenizer.PROPERTY));
        assertTrue(hasKind(json, Tokenizer.NUMBER));

        SyntaxHighlightResult binary = new Tokenizer(LanguageDef.forName("java"))
                .tokenize("int n = 0b102;", 0);
        SyntaxToken number = first(binary, Tokenizer.NUMBER);
        assertEquals(4, number.length);
    }

    @Test
    void multilineAndMarkupStatesAreCarriedBetweenLines() {
        Tokenizer python = new Tokenizer(LanguageDef.forName("python"));
        SyntaxHighlightResult first = python.tokenize("value = \"\"\"hello", 0);
        assertEquals(Tokenizer.STATE_TRIPLE_DOUBLE, first.endState);
        SyntaxHighlightResult second = python.tokenize("world\"\"\"", first.endState);
        assertEquals(Tokenizer.STATE_NORMAL, second.endState);
        assertTrue(hasKind(second, Tokenizer.STRING));

        Tokenizer xml = new Tokenizer(LanguageDef.forName("xml"));
        SyntaxHighlightResult tag = xml.tokenize("<widget id=\"main\">", 0);
        assertTrue(hasKind(tag, Tokenizer.TYPE));
        assertTrue(hasKind(tag, Tokenizer.PROPERTY));
        assertTrue(hasKind(tag, Tokenizer.STRING));
    }

    @Test
    void thirdPartyTokensCanProvideThemeColors() {
        SyntaxToken token = new SyntaxToken(1, 2, 1234, 0x112233, 0x445566);
        assertEquals(0x112233, token.lightColor);
        assertEquals(0x445566, token.darkColor);
    }

    private static boolean hasKind(SyntaxHighlightResult line, int kind) {
        return first(line, kind) != null;
    }

    private static int countKind(SyntaxHighlightResult line, int kind) {
        int count = 0;
        for (SyntaxToken token : line.tokens) if (token.kind == kind) count++;
        return count;
    }

    private static SyntaxToken first(SyntaxHighlightResult line, int kind) {
        for (SyntaxToken token : line.tokens) if (token.kind == kind) return token;
        return null;
    }
}
