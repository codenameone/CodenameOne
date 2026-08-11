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
package com.codename1.retrace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Golden tests for {@link ParparVmTraceParser}. This is the shared reference the
 * on-device {@code java.lang.Throwable.getStackTrace()} in {@code vm/JavaAPI}
 * mirrors, so these cases double as the contract for that hand-inlined copy.
 */
public class ParparVmTraceParserTest {

    @Test
    public void parsesStandardTrace() {
        String s = "java.lang.NullPointerException\n"
                + "    at com.example.MyForm.onClick:142\n"
                + "    at com.codename1.ui.Button.released:88\n";
        List<Frame> frames = ParparVmTraceParser.parse(s);
        assertEquals(2, frames.size());
        assertEquals("com.example.MyForm", frames.get(0).getClassName());
        assertEquals("onClick", frames.get(0).getMethodName());
        assertEquals("MyForm.java", frames.get(0).getFileName());
        assertEquals(142, frames.get(0).getLineNumber());
        assertEquals("com.codename1.ui.Button", frames.get(1).getClassName());
        assertEquals(88, frames.get(1).getLineNumber());
    }

    @Test
    public void parsesInitClinitInnerClassAndNegativeLine() {
        String s = "java.lang.RuntimeException\n"
                + "    at com.example.Foo.<init>:42\n"
                + "    at com.example.Bar.<clinit>:-1\n"
                + "    at a.b$c.run:7\n";
        List<Frame> frames = ParparVmTraceParser.parse(s);
        assertEquals(3, frames.size());
        assertEquals("<init>", frames.get(0).getMethodName());
        assertEquals("<clinit>", frames.get(1).getMethodName());
        assertEquals(-1, frames.get(1).getLineNumber());
        // Inner class a.b$c resolves its source file to the outer simple name.
        assertEquals("a.b$c", frames.get(2).getClassName());
        assertEquals("b.java", frames.get(2).getFileName());
        assertEquals(7, frames.get(2).getLineNumber());
    }

    @Test
    public void framesAreNeverFlaggedNative() {
        List<Frame> frames = ParparVmTraceParser.parse(
                "E\n    at com.example.A.b:1\n");
        assertEquals(1, frames.size());
        assertFalse("a ParparVM frame must not look native",
                frames.get(0).getFileName() == null);
    }

    @Test
    public void rejectsV8JavaScriptStack() {
        // V8 frames carry parentheses and URLs; a no-function frame is a bare URL.
        String s = "Error: boom\n"
                + "    at onClick (http://localhost/app.js:100:5)\n"
                + "    at http://localhost/app.js:1:2\n";
        assertTrue("V8 Error().stack must yield no frames, never fabricated ones",
                ParparVmTraceParser.parse(s).isEmpty());
    }

    @Test
    public void rejectsSpiderMonkeyJavaScriptStack() {
        String s = "onClick@http://localhost/app.js:100:5\n"
                + "run@http://localhost/app.js:1:2\n";
        assertTrue(ParparVmTraceParser.parse(s).isEmpty());
    }

    @Test
    public void emptyNullAndHeaderOnlyYieldNoFrames() {
        assertTrue(ParparVmTraceParser.parse(null).isEmpty());
        assertTrue(ParparVmTraceParser.parse("").isEmpty());
        assertTrue(ParparVmTraceParser.parse("java.lang.IllegalStateException\n").isEmpty());
    }
}
