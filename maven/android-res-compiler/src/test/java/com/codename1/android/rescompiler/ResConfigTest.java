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
package com.codename1.android.rescompiler;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResConfigTest {

    private static String canon(String q) {
        List<String> errors = new ArrayList<String>();
        ResConfig c = ResConfig.parse(q, errors);
        assertNotNull(q + " -> " + errors, c);
        return c.canonical();
    }

    @Test
    public void canonicalOrderAndSpelling() {
        assertEquals("", canon(""));
        assertEquals("night", canon("night"));
        assertEquals("fr-rCA", canon("fr-rCA"));
        assertEquals("sw600dp-land-v21", canon("sw600dp-land-v21"));
        assertEquals("xxhdpi", canon("xxhdpi"));
        assertEquals("b+sr+Latn", canon("b+sr+Latn"));
        assertEquals("en-night-v26", canon("en-night-v26"));
    }

    @Test
    public void carIsUiModeNotALanguage() {
        List<String> errors = new ArrayList<String>();
        ResConfig c = ResConfig.parse("car", errors);
        assertNotNull(c);
        assertNull(c.language);
        assertEquals("car", c.unsupported);
        assertTrue(c.canonical().startsWith("x+"));
    }

    @Test
    public void outOfOrderAndUnknownAreErrors() {
        List<String> errors = new ArrayList<String>();
        assertNull(ResConfig.parse("v21-land", errors));
        assertEquals(1, errors.size());
        errors.clear();
        assertNull(ResConfig.parse("bogusqualifier", errors));
        assertEquals(1, errors.size());
    }
}
