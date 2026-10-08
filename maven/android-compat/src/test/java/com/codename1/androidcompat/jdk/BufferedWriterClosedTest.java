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

import java.io.IOException;
import java.io.Writer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/// A closed `BufferedWriter` used to accept writes into its buffer, and a
/// second close handed them to the underlying writer. Writing or flushing
/// after close now throws, and a second close does nothing.
public class BufferedWriterClosedTest {

    /// Records what reaches it; its close does nothing, like a writer over a
    /// shared sink.
    private static final class Sink extends Writer {
        final StringBuilder text = new StringBuilder();
        int closes;

        @Override
        public void write(char[] cbuf, int off, int len) {
            text.append(cbuf, off, len);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
            closes++;
        }
    }

    @Test
    public void writesAfterCloseAreRejected() throws IOException {
        Sink sink = new Sink();
        BufferedWriter w = new BufferedWriter(sink);
        w.write("ab");
        w.close();
        try {
            w.write('c');
            fail("write after close");
        } catch (IOException expected) {
            assertEquals("Stream closed", expected.getMessage());
        }
        try {
            w.write("cd");
            fail("write after close");
        } catch (IOException expected) {
            assertEquals("Stream closed", expected.getMessage());
        }
        try {
            w.flush();
            fail("flush after close");
        } catch (IOException expected) {
            assertEquals("Stream closed", expected.getMessage());
        }
        w.close();
        assertEquals("ab", sink.text.toString());
        assertEquals(1, sink.closes);
    }
}
