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
package com.codename1.gpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the {@link Texture} GPU handle: immutable
 * dimensions, the wrap/filter defaults and their chaining setters, and the
 * opaque backend-handle accessor.
 */
class TextureTest {

    @Test
    void dimensionsAreStored() {
        Texture t = new Texture(256, 128);
        assertEquals(256, t.getWidth());
        assertEquals(128, t.getHeight());
    }

    @Test
    void wrapAndFilterDefaults() {
        Texture t = new Texture(1, 1);
        assertEquals(Texture.Wrap.CLAMP, t.getWrap());
        assertEquals(Texture.Filter.LINEAR, t.getFilter());
    }

    @Test
    void wrapAndFilterSettersChainAndRoundTrip() {
        Texture t = new Texture(1, 1);
        assertSame(t, t.setWrap(Texture.Wrap.REPEAT));
        assertSame(t, t.setFilter(Texture.Filter.NEAREST));
        assertEquals(Texture.Wrap.REPEAT, t.getWrap());
        assertEquals(Texture.Filter.NEAREST, t.getFilter());
    }

    @Test
    void handleDefaultsNullAndRoundTrips() {
        Texture t = new Texture(1, 1);
        assertNull(t.getHandle());
        Object handle = new Object();
        t.setHandle(handle);
        assertSame(handle, t.getHandle());
    }

    @Test
    void enumsExposeTheExpectedConstants() {
        assertEquals(2, Texture.Wrap.values().length);
        assertEquals(2, Texture.Filter.values().length);
        assertEquals(Texture.Wrap.CLAMP, Texture.Wrap.valueOf("CLAMP"));
        assertEquals(Texture.Filter.NEAREST, Texture.Filter.valueOf("NEAREST"));
    }
}
