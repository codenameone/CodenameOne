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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Flutter adds letterSpacing BETWEEN glyphs - n-1 gaps for n characters, with nothing
 * trailing the last one. Being one gap out is a whole space of drift on a short label,
 * which is exactly where the gallery uses it (headers and button captions).
 */
class LetterSpacingTest {

    @Test
    void spacingAddsOneGapFewerThanCharacters() {
        assertEquals(58.0, TextRenderElement.spacedWidth(50.0, 5, 2.0));
    }

    @Test
    void aSingleCharacterGetsNoSpacing() {
        assertEquals(10.0, TextRenderElement.spacedWidth(10.0, 1, 7.0));
    }

    @Test
    void zeroSpacingIsTheBareStringWidth() {
        assertEquals(50.0, TextRenderElement.spacedWidth(50.0, 5, 0.0));
    }

    @Test
    void theEmptyStringHasNoWidth() {
        assertEquals(0.0, TextRenderElement.spacedWidth(0.0, 0, 4.0));
    }

    @Test
    void negativeSpacingTightens() {
        assertEquals(42.0, TextRenderElement.spacedWidth(50.0, 5, -2.0));
    }
}
