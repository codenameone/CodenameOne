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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The generated background-listener factory. The point of generating it is
 * that the binding is a direct {@code new} rather than a reflective lookup,
 * so these cases assert on exactly that.
 */
class HealthListenerBindingsTest {

    private static List<String> list(String... values) {
        List<String> out = new ArrayList<String>();
        for (String v : values) {
            out.add(v);
        }
        return out;
    }

    /**
     * The whole reason this generator exists: a direct constructor call is
     * a real reference that shrinking and obfuscation follow, whereas a
     * Class.forName on a persisted name is not.
     */
    @Test
    void generatedFactoryConstructsListenersDirectly() {
        String src = HealthListenerBindings.generate(
                list("com.example.StepWatcher"));
        assertNotNull(src);
        assertTrue(src.contains("return new com.example.StepWatcher();"),
                "the binding must be a direct constructor call");
        assertFalse(src.contains("Class.forName"),
                "the generated factory must never reflect");
        assertTrue(src.contains(
                "\"com.example.StepWatcher\".equals(className)"));
    }

    @Test
    void generatedFactoryImplementsTheRuntimeInterface() {
        String src = HealthListenerBindings.generate(
                list("com.example.StepWatcher"));
        assertTrue(src.contains(
                "implements HealthBackgroundListenerFactory"));
        assertTrue(src.contains(
                "HealthStore.setBackgroundListenerFactory"));
        assertTrue(src.contains("public static void install()"));
        assertTrue(src.startsWith("package "
                + HealthListenerBindings.PACKAGE + ";"));
    }

    @Test
    void severalListenersEachGetABinding() {
        String src = HealthListenerBindings.generate(
                list("com.example.B", "com.example.A"));
        assertTrue(src.contains("return new com.example.A();"));
        assertTrue(src.contains("return new com.example.B();"));
        // Sorted, so the generated source is stable build to build.
        assertTrue(src.indexOf("com.example.A") < src.indexOf("com.example.B"));
    }

    @Test
    void unknownClassNameReturnsNullRatherThanThrowing() {
        String src = HealthListenerBindings.generate(
                list("com.example.StepWatcher"));
        assertTrue(src.contains("return null;"));
    }

    /**
     * An app with no background listeners generates nothing and registers
     * nothing. The runtime then defers any background delivery instead of
     * losing it, so this is a safe no-op rather than a broken build.
     */
    @Test
    void noListenersGeneratesNothing() {
        assertNull(HealthListenerBindings.generate(null));
        assertNull(HealthListenerBindings.generate(new ArrayList<String>()));
        assertNull(HealthListenerBindings.installStatement(
                new ArrayList<String>()));
    }

    @Test
    void installStatementTargetsTheGeneratedClass() {
        String stmt = HealthListenerBindings.installStatement(
                list("com.example.StepWatcher"));
        assertNotNull(stmt);
        assertTrue(stmt.startsWith(HealthListenerBindings.FQCN + ".install()"));
    }

    @Test
    void sourcePathMatchesThePackage() {
        assertEquals("com/codename1/health/generated/"
                        + "CN1HealthListenerBindings.java",
                HealthListenerBindings.sourcePath());
    }
}
