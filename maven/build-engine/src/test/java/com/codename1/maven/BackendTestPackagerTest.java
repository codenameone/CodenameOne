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
package com.codename1.maven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which test sources the compiled run leaves out for using Mockito.
class BackendTestPackagerTest {
    @Test
    void codeThatUsesMockitoIsLeftOut() {
        assertTrue(BackendTestPackager.usesMockito("import org.mockito.Mockito;\nclass A {}\n"));
        assertTrue(BackendTestPackager.usesMockito("import static org.mockito.Mockito.when;\n"));
        assertTrue(BackendTestPackager.usesMockito("class A { Object m = org.mockito.Mockito.mock(A.class); }"));
    }

    @Test
    void aSourceIsSelectedByTheClassItsPathNames() {
        java.io.File root = new java.io.File("/tmp/project/src/test/java");
        assertTrue("com.acme.ApiTest".equals(BackendTestPackager.binaryName(root,
                new java.io.File(root, "com/acme/ApiTest.java"))));
        assertTrue("TopLevelTest".equals(BackendTestPackager.binaryName(root,
                new java.io.File(root, "TopLevelTest.java"))), "the default package");
    }

    @Test
    void theWordsInCommentsAndStringsAreNotAUse() {
        assertFalse(BackendTestPackager.usesMockito("// unlike org.mockito, this needs nothing\nclass A {}\n"));
        assertFalse(BackendTestPackager.usesMockito("/** Not org.mockito: a fake. */\nclass A {}\n"));
        assertFalse(BackendTestPackager.usesMockito(
                "class A { String s = \"org.mockito.Mockito\"; char c = '\"'; }\n"));
        assertFalse(BackendTestPackager.usesMockito(
                "class A { String s = \"say \\\"org.mockito\\\"\"; }\n"));
        assertTrue(BackendTestPackager.usesMockito(
                "class A { String s = \"x\"; Object m = org.mockito.Mockito.mock(A.class); }\n"),
                "code after a string is still read");
    }
}
