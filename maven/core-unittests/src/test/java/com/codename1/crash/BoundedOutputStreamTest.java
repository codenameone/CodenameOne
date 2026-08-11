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
package com.codename1.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintStream;
import org.junit.jupiter.api.Test;

/** The bounded buffer that caps raw-stack capture so a huge trace cannot OOM during crash handling. */
class BoundedOutputStreamTest {

    @Test
    void keepsFirstBytesAndDiscardsTheRest() throws Exception {
        CrashProtection.BoundedOutputStream b = new CrashProtection.BoundedOutputStream(10);
        b.write("hello".getBytes("UTF-8"), 0, 5);
        b.write("world!!!".getBytes("UTF-8"), 0, 8); // only 5 more fit
        assertEquals("helloworld", b.toUtf8());
        b.write('x'); // past capacity: discarded
        assertEquals("helloworld", b.toUtf8());
    }

    @Test
    void capsAHugePrintStreamRenderingAtCapacity() throws Exception {
        // Simulate a pathologically large rendering: far more than the cap is written, but the buffer
        // never grows past its fixed capacity, so crash handling cannot allocate without bound.
        int cap = 4096;
        CrashProtection.BoundedOutputStream b = new CrashProtection.BoundedOutputStream(cap);
        PrintStream ps = new PrintStream(b, true, "UTF-8");
        for (int i = 0; i < 100000; i++) {
            ps.print("0123456789");
        }
        ps.flush();
        assertEquals(cap, b.toUtf8().length());
    }
}
