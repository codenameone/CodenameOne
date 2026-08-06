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

import java.util.Arrays;
import org.junit.Test;

/** Retrace of obfuscated frames through a ProGuard mapping, single and chained. */
public class MappingFileTest {

    private static final String MAPPING =
            "# Codename One App Hardening mapping\n"
            + "# engine: 1.0.0\n"
            + "com.example.MyForm -> zqaaaa:\n"
            + "    int counter -> a\n"
            + "    void onClick() -> b\n"
            + "    142:145:java.lang.String render(int) -> c\n"
            + "com.example.util.Helper -> zqaaab:\n"
            + "    int square(int) -> a\n";

    @Test
    public void retracesClassAndMethod() throws Exception {
        MappingFile mf = MappingFile.parse(MAPPING);
        assertEquals(2, mf.size());
        Frame in = new Frame("zqaaaa", "b", "zqaaaa.java", 5);
        Frame out = mf.retrace(in);
        assertEquals("com.example.MyForm", out.getClassName());
        assertEquals("onClick", out.getMethodName());
        assertEquals("MyForm.java", out.getFileName());
    }

    @Test
    public void retracesMethodByLineRange() throws Exception {
        MappingFile mf = MappingFile.parse(MAPPING);
        Frame out = mf.retrace(new Frame("zqaaaa", "c", "zqaaaa.java", 143));
        assertEquals("render", out.getMethodName());
        assertEquals("com.example.MyForm", out.getClassName());
    }

    @Test
    public void unknownClassPassesThroughUnchanged() throws Exception {
        MappingFile mf = MappingFile.parse(MAPPING);
        Frame in = new Frame("some.Other", "x", "Other.java", 9);
        assertEquals(in, mf.retrace(in));
    }

    @Test
    public void chainAppliesInOrder() throws Exception {
        // Stage 1 (device-nearest, e.g. R8): b0 -> zqaaaa ; Stage 2 (cross-platform): zqaaaa -> MyForm.
        MappingFile stage1 = MappingFile.parse("zqaaaa -> b0:\n    void b() -> a\n");
        MappingFile stage2 = MappingFile.parse(MAPPING);
        MappingChain chain = new MappingChain(Arrays.asList(stage1, stage2));
        Frame device = new Frame("b0", "a", "b0.java", 5);
        Frame out = chain.retrace(device);
        assertEquals("com.example.MyForm", out.getClassName());
        assertEquals("onClick", out.getMethodName());
    }
}
