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

import java.io.Writer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// A closed `PrintWriter` kept its delegate, so over a writer whose close is
/// a no-op every later print still landed in it and `checkError` reported
/// nothing. Like the JDK it now suppresses those writes and reports them.
public class PrintWriterClosedTest {

    /// Records what reaches it; its close does nothing, like a `StringWriter`.
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
    public void writesAfterCloseAreSuppressedAndReported() {
        Sink sink = new Sink();
        PrintWriter w = new PrintWriter(sink);
        w.print("ab");
        assertFalse(w.checkError());
        w.close();
        w.print("cd");
        w.write('e');
        w.write(new char[] {'f'});
        w.println("g");
        w.flush();
        w.close();
        assertEquals("ab", sink.text.toString());
        assertEquals(1, sink.closes);
        assertTrue(w.checkError());
    }

    @Test
    public void closeAloneIsNotAnError() {
        PrintWriter w = new PrintWriter(new Sink());
        w.close();
        assertFalse(w.checkError());
    }
}
