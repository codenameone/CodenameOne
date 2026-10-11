/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code wrap}'s package-private {@code paragraphEndsAfter} overload (the four-arg
 * form, in the same package as {@link TextRenderElement} -- {@code TextWrapTest},
 * one package up, can only reach the public three-arg one). See
 * {@link TextClampTest} for how {@code clamp} reads this list.
 */
class TextWrapParagraphTest {

    private static final Funcs.Func1<String, Double> TEN_PER_CHAR =
            new Funcs.Func1<String, Double>() {
                @Override
                public Double call(String s) {
                    return s.length() * 10.0;
                }
            };

    @Test
    void aParagraphThatFitsOnOneLineIsMarkedEndedThere() {
        // The gallery's own data (lib/studies/reply/model/email_store.dart):
        // "Cucumber Mask Facial has shipped.\n\nKeep an eye out ..." -- a BLANK
        // line between paragraphs, which splits into three: the text, an EMPTY
        // paragraph for the blank line, then the next paragraph. clamp() with
        // maxLines: 1 only ever looks at index 0, so the empty middle one does
        // not change what matters here: index 0 is still marked ended.
        List<Boolean> ends = new ArrayList<Boolean>();
        List<String> lines = TextRenderElement.wrap("Cucumber Mask Facial has shipped.\n\n"
                + "Keep an eye out for a package to arrive.", TEN_PER_CHAR, 1000, ends);
        assertEquals(3, lines.size());
        assertEquals("Cucumber Mask Facial has shipped.", lines.get(0));
        assertEquals(Boolean.TRUE, ends.get(0), "index 0 -- the line clamp() keeps -- ends a paragraph");
    }

    @Test
    void aSoftWrappedContinuationIsNotAParagraphEnd() {
        // 10 chars fit per line; "hello world" is 11, so this is a WIDTH cut, not a \n.
        List<Boolean> ends = new ArrayList<Boolean>();
        List<String> lines = TextRenderElement.wrap("hello world", TEN_PER_CHAR, 100, ends);
        assertEquals(2, lines.size());
        assertEquals(Arrays.asList(Boolean.FALSE, Boolean.FALSE), ends);
    }

    @Test
    void theLastLineOfTheWholeTextIsNeverMarkedAParagraphEnd() {
        // Nothing follows "c", so there is nothing an ellipsis would ever need
        // to skip past it for -- clamp() never even reaches this index.
        List<Boolean> ends = new ArrayList<Boolean>();
        TextRenderElement.wrap("a\nb\nc", TEN_PER_CHAR, 1000, ends);
        assertEquals(Arrays.asList(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE), ends);
    }

    @Test
    void aNullOutListIsSimplyIgnored() {
        List<String> lines = TextRenderElement.wrap("a\nb", TEN_PER_CHAR, 1000, null);
        assertEquals(Arrays.asList("a", "b"), lines);
    }
}
