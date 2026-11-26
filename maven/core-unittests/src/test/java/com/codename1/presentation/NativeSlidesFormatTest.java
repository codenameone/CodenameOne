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
package com.codename1.presentation;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `.cn1slides` keeps everything the model holds.
class NativeSlidesFormatTest extends UITestBase {
    @Test
    void everythingRoundTripsExactly() throws IOException {
        Presentation p = Decks.everything();
        byte[] b = p.toBytes(Presentation.Format.NATIVE);
        assertEquals(Presentation.Format.NATIVE, Presentation.detect(b));
        Presentation back = Presentation.read(b);
        assertEquals(p.slides.size(), back.slides.size());
        for (int i = 0; i < p.slides.size(); i++) {
            Slide a = p.slides.get(i);
            Slide c = back.slides.get(i);
            for (int j = 0; j < a.elements.size(); j++) {
                assertEquals(a.elements.get(j), c.elements.get(j), "slide " + i + " element " + j);
            }
            assertEquals(a, c, "slide " + i);
        }
        assertEquals(p, back);
        assertEquals("Deck & co", back.title);
        assertEquals(100, back.nextId);
        assertTrue(back.slides.get(2).hidden);
    }

    @Test
    void secondWriteIsByteIdentical() throws IOException {
        byte[] a = Decks.everything().toBytes(Presentation.Format.NATIVE);
        byte[] b = Presentation.read(a).toBytes(Presentation.Format.NATIVE);
        assertEquals(new String(a, "ISO-8859-1").length(), new String(b, "ISO-8859-1").length());
    }

    @Test
    void notAPresentationIsRefused() {
        assertThrows(IOException.class, () -> Presentation.read(new ByteArrayInputStream(
                new byte[] {'P', 'K', 3, 4, 0, 0, 0, 0}), Presentation.Format.NATIVE));
        assertThrows(IOException.class, () -> Presentation.detect(new byte[] {1, 2, 3}));
    }
}
