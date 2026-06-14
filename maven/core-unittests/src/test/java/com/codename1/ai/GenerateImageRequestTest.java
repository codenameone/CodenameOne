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
package com.codename1.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the {@link GenerateImageRequest} fluent builder:
 * required-prompt validation, defaults, chaining identity, and the count floor.
 */
class GenerateImageRequestTest {

    @Test
    void promptIsRequired() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new GenerateImageRequest(null);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new GenerateImageRequest("");
            }
        });
    }

    @Test
    void defaultsAreSensible() {
        GenerateImageRequest r = new GenerateImageRequest("a cat");
        assertEquals("a cat", r.getPrompt());
        assertEquals("1024x1024", r.getSize());
        assertEquals(1, r.getCount());
        assertNull(r.getModel());
        assertNull(r.getStyle());
        assertNull(r.getQuality());
        assertNull(r.getSeed());
    }

    @Test
    void settersChainAndRoundTrip() {
        GenerateImageRequest r = new GenerateImageRequest("prompt");
        assertSame(r, r.setModel("dall-e-3"));
        assertSame(r, r.setSize("1792x1024"));
        assertSame(r, r.setStyle("vivid"));
        assertSame(r, r.setQuality("hd"));
        assertSame(r, r.setSeed(Long.valueOf(42L)));
        assertSame(r, r.setCount(3));

        assertEquals("dall-e-3", r.getModel());
        assertEquals("1792x1024", r.getSize());
        assertEquals("vivid", r.getStyle());
        assertEquals("hd", r.getQuality());
        assertEquals(Long.valueOf(42L), r.getSeed());
        assertEquals(3, r.getCount());
    }

    @Test
    void countFloorsAtOne() {
        assertEquals(1, new GenerateImageRequest("p").setCount(0).getCount());
        assertEquals(1, new GenerateImageRequest("p").setCount(-5).getCount());
    }

    @Test
    void seedCanBeClearedBackToNull() {
        GenerateImageRequest r = new GenerateImageRequest("p").setSeed(Long.valueOf(9L));
        assertEquals(Long.valueOf(9L), r.getSeed());
        r.setSeed(null);
        assertNull(r.getSeed());
    }
}
