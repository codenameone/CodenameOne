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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The generated background-listener factory. The point of generating it is
 * that the binding is a direct {@code new} rather than a reflective lookup,
 * so these cases assert on exactly that.
 */
class HealthListenerBindingsTest {

    /**
     * Binary names whose source names need no separator translation --
     * top-level classes, where the two are the same string.
     */
    private static Map<String, String> list(String... values) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (String v : values) {
            out.put(v, v);
        }
        return out;
    }

    /** One listener whose binary and source names differ. */
    private static Map<String, String> named(String binary, String source) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        out.put(binary, source);
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
        assertNull(HealthListenerBindings.generate(
                new LinkedHashMap<String, String>()));
        assertNull(HealthListenerBindings.installStatement(
                new LinkedHashMap<String, String>()));
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

    /**
     * A second translation root gets a factory of its own.
     *
     * <p>The factory names every listener in a {@code new} expression, which the translator
     * follows, so one shared factory drags each target's listeners into the other target's binary
     * -- the watch carrying the phone's health graph it can never be asked for. Distinct class
     * name, distinct file, distinct install statement: the two are compiled by the same javac pass
     * and separated only by which stub installs which.</p>
     */
    @Test
    void aSecondRootGetsAFactoryOfItsOwn() {
        String suffix = HealthListenerBindings.WATCH_SUFFIX;
        String src = HealthListenerBindings.generate(
                list("com.example.WristWatcher"), suffix);
        assertNotNull(src);
        assertTrue(src.contains("public final class CN1HealthListenerBindings"
                + suffix + " implements"),
                "the watch factory must not collide with the phone's: " + src);
        assertTrue(src.contains("new CN1HealthListenerBindings" + suffix + "()"),
                "and must install ITSELF, not the phone's factory: " + src);
        assertTrue(src.contains("new com.example.WristWatcher()"), src);

        assertEquals(HealthListenerBindings.FQCN + suffix + ".install();\n",
                HealthListenerBindings.installStatement(
                        list("com.example.WristWatcher"), suffix));
        assertEquals("com/codename1/health/generated/CN1HealthListenerBindings"
                        + suffix + ".java",
                HealthListenerBindings.sourcePath(suffix));
    }

    /** No suffix is the single-translation case, unchanged. */
    @Test
    void theUnsuffixedFormIsTheSingleTranslationCase() {
        assertEquals(HealthListenerBindings.generate(list("com.example.StepWatcher")),
                HealthListenerBindings.generate(list("com.example.StepWatcher"), ""));
        assertEquals(HealthListenerBindings.sourcePath(),
                HealthListenerBindings.sourcePath(""));
    }

    /**
     * A nested listener is the common case -- developers put it inside the
     * class that subscribes. The persisted key must stay the binary name,
     * since that is what Class.getName() returns, but the constructor call
     * needs the source name: `new Outer$Inner()` does not compile.
     */
    @Test
    void nestedListenersUseSourceNamesInTheConstructorCall() {
        String src = HealthListenerBindings.generate(
                named("com.example.Outer$Inner", "com.example.Outer.Inner"));
        assertNotNull(src);
        assertTrue(src.contains("\"com.example.Outer$Inner\".equals(className)"),
                "the key must be the binary name Class.getName() returns");
        assertTrue(src.contains("return new com.example.Outer.Inner();"),
                "the constructor call must use the dotted source name");
        assertFalse(src.contains("new com.example.Outer$Inner()"),
                "new Outer$Inner() is not valid Java source");
    }

    @Test
    void dollarsInATopLevelNameAreNotSeparators() {
        // A dollar is a legal Java identifier character, so this is an
        // ordinary top-level class -- and translating the dollar emitted
        // `new app.Step.Listener()`, naming a type that does not exist and
        // failing javac. The caller supplies the source name from the
        // class file's InnerClasses metadata; the generator does not
        // guess.
        String src = HealthListenerBindings.generate(
                named("app.Step$Listener", "app.Step$Listener"));
        assertNotNull(src);
        assertTrue(src.contains("\"app.Step$Listener\".equals(className)"),
                "the key must be the binary name");
        assertTrue(src.contains("return new app.Step$Listener();"),
                "a dollar that separates nothing must survive");
        assertFalse(src.contains("new app.Step.Listener()"),
                "app.Step is not a type");
    }

    /** A nested class may itself carry a dollar in its simple name. */
    @Test
    void aNestedNameKeepsItsOwnDollar() {
        String src = HealthListenerBindings.generate(
                named("app.Outer$Step$Listener", "app.Outer.Step$Listener"));
        assertTrue(src.contains("return new app.Outer.Step$Listener();"));
    }
}
