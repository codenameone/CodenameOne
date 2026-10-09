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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/// A closed `BufferedReader` or `BufferedInputStream` throws rather than
/// handing out the characters or bytes it had already buffered, and a closed
/// `BufferedOutputStream` refuses writes, as the JDK classes do.
public class BufferedStreamsClosedTest {

    private interface Op {
        void run() throws IOException;
    }

    private static void assertClosed(Op op) {
        try {
            op.run();
            fail("expected IOException after close");
        } catch (IOException e) {
            assertEquals("Stream closed", e.getMessage());
        }
    }

    @Test
    public void closedReaderRejectsBufferedReads() throws IOException {
        final BufferedReader r = new BufferedReader(new StringReader("first\nsecond\nthird"));
        r.mark(100);
        assertEquals("first", r.readLine());
        r.close();
        r.close();
        assertClosed(new Op() { public void run() throws IOException { r.read(); } });
        assertClosed(new Op() { public void run() throws IOException { r.read(new char[4], 0, 4); } });
        assertClosed(new Op() { public void run() throws IOException { r.readLine(); } });
        assertClosed(new Op() { public void run() throws IOException { r.ready(); } });
        assertClosed(new Op() { public void run() throws IOException { r.skip(1); } });
        assertClosed(new Op() { public void run() throws IOException { r.reset(); } });
    }

    @Test
    public void closedStreamRejectsBufferedReads() throws IOException {
        final BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}));
        in.mark(10);
        assertEquals(1, in.read());
        in.close();
        in.close();
        assertClosed(new Op() { public void run() throws IOException { in.read(); } });
        assertClosed(new Op() { public void run() throws IOException { in.read(new byte[4], 0, 4); } });
        assertClosed(new Op() { public void run() throws IOException { in.skip(1); } });
        assertClosed(new Op() { public void run() throws IOException { in.available(); } });
        assertClosed(new Op() { public void run() throws IOException { in.reset(); } });
    }

    @Test
    public void closedOutputStreamRejectsWrites() throws IOException {
        // ByteArrayOutputStream's close is a no-op, so a buffered write after
        // close would reach it on the next flush or close.
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        final BufferedOutputStream out = new BufferedOutputStream(sink);
        out.write(1);
        out.close();
        out.close();
        assertEquals(1, sink.size());
        assertClosed(new Op() { public void run() throws IOException { out.write(2); } });
        assertClosed(new Op() { public void run() throws IOException { out.write(new byte[] {3, 4}, 0, 2); } });
        assertClosed(new Op() { public void run() throws IOException { out.flush(); } });
        out.close();
        assertEquals(1, sink.size());
    }
}
