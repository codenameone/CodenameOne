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
package com.codename1.debug.proxy;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class SymbolTableTest {

    @Test
    public void originalJvmNamePreservesInnerClassesAndUnderscores() throws Exception {
        SymbolTable table = load(
                "version\t1\n"
                        + "class\t17\tcom_example_my_app_Main_1\tMain.java\t-1\tcom/example/my_app/Main$1\n"
        );

        SymbolTable.ClassInfo inner = table.classById(17);
        assertNotNull(inner);
        assertEquals("com/example/my_app/Main$1", inner.jvmName);
        assertEquals("Lcom/example/my_app/Main$1;", inner.jvmSignature());
        assertSame(inner, table.classByJvmSignature("Lcom/example/my_app/Main$1;"));
        assertNull(table.classByJvmSignature("Lcom/example/my/app/Main/1;"));
    }

    @Test
    public void legacyRowsRetainBestEffortSignatureConversion() throws Exception {
        SymbolTable table = load(
                "version\t1\n"
                        + "class\t3\tjava_lang_String\tString.java\t-1\n"
        );

        assertEquals("Ljava/lang/String;", table.classById(3).jvmSignature());
    }

    private SymbolTable load(String text) throws Exception {
        return SymbolTable.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
}
