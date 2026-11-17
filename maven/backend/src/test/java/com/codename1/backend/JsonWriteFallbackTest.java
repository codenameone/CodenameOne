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
package com.codename1.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Issue 6240: a class with no codec was written as its toString().
class JsonWriteFallbackTest {
    static final class NoCodec { public String toString() { return "NoCodec@1"; } }
    enum Color { RED }

    @Test void applicationClassWithoutCodecIsRefused() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> Json.write(java.util.Collections.singletonMap("x", new NoCodec())));
        assertTrue(error.getMessage().contains("No JSON codec"));
        ByteSink sink = new ByteSink(16);
        assertThrows(IllegalStateException.class, () -> Json.write(new NoCodec(), sink));
    }

    @Test void jdkValuesStillWriteAsStrings() {
        assertEquals("\"RED\"", Json.write(Color.RED));
        assertEquals("\"1.50\"", Json.write(new java.math.BigDecimal("1.50")));
    }
}
