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
package com.codename1.hardening;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The frame-hierarchy resolver must find common superclasses from the supplied
 * classloader (not the engine's own), and must fall back to Object rather than
 * throw when a type is unresolvable -- otherwise COMPUTE_FRAMES would abort
 * hardening on any class with a merge between application types (Codex P1).
 */
public class FrameClassWriterTest {

    // Same package, so the protected getCommonSuperClass is directly callable.
    private String common(ClassLoader cl, String a, String b) {
        return new FrameClassWriter(0, cl).getCommonSuperClass(a, b);
    }

    @Test
    public void resolvesCommonSuperFromLoader() {
        ClassLoader cl = getClass().getClassLoader();
        assertEquals("java/lang/Number", common(cl, "java/lang/Integer", "java/lang/Long"));
        assertEquals("java/util/AbstractList", common(cl, "java/util/ArrayList", "java/util/Vector"));
        assertEquals("java/lang/Object", common(cl, "java/lang/String", "java/lang/Integer"));
    }

    @Test
    public void identicalTypeReturnsItself() {
        assertEquals("java/lang/String", common(getClass().getClassLoader(),
                "java/lang/String", "java/lang/String"));
    }

    @Test
    public void unresolvableTypeFallsBackToObjectNotThrow() {
        // A type absent from the loader (e.g. a renamed app class not on the engine classpath)
        // must NOT crash frame computation.
        assertEquals("java/lang/Object",
                common(getClass().getClassLoader(), "totally/Missing", "java/lang/String"));
    }

    @Test
    public void nullLoaderIsSafe() {
        assertEquals("java/lang/Object", common(null, "a/B", "c/D"));
        assertEquals("a/B", common(null, "a/B", "a/B"));
    }
}
