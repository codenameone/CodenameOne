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
package com.codename1.androidcompat.jdk;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Code compiled against `java.io.BufferedReader` marks ahead before choosing
/// how to parse; the remapped reader must honour `mark`/`reset` across
/// buffer refills, as the JDK class does.
public class BufferedReaderMarkTest {

    @Test
    public void resetReturnsToTheMarkAcrossRefills() throws IOException {
        BufferedReader r = new BufferedReader(new StringReader("header\nabcdefghijklmnop\nrest"), 4);
        assertTrue(r.markSupported());
        assertEquals("header", r.readLine());
        r.mark(64);
        assertEquals("abcdefghijklmnop", r.readLine());
        r.reset();
        assertEquals('a', r.read());
        assertEquals("bcdefghijklmnop", r.readLine());
        assertEquals("rest", r.readLine());
    }

    @Test
    public void markAfterCarriageReturnKeepsTheLineFeedSkip() throws IOException {
        BufferedReader r = new BufferedReader(new StringReader("a\r\nb"), 2);
        assertEquals("a", r.readLine());
        r.mark(8);
        assertEquals('b', r.read());
        r.reset();
        assertEquals('b', r.read());
        assertEquals(-1, r.read());
    }

    @Test
    public void readingPastTheLimitInvalidatesTheMark() throws IOException {
        BufferedReader r = new BufferedReader(new StringReader("0123456789abcdef"), 4);
        r.mark(2);
        char[] c = new char[16];
        int n = 0;
        while (n < 10) {
            n += r.read(c, n, 10 - n);
        }
        try {
            r.reset();
            fail("reset after reading past the read-ahead limit");
        } catch (IOException expected) {
            assertEquals("Mark invalid", expected.getMessage());
        }
    }
}
