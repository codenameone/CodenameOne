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

    @Test
    public void resolvesSharedBaseFromBytesWhenBaseCannotBeLoaded() {
        // app/A and app/B both extend app/Base, but Base is supplied only by the target platform and is
        // absent from the hierarchy. Loading A or B fails (their super can't be linked), so the resolver
        // must read the super_class name from the bytes and return app/Base -- NOT Object, which is not
        // assignable to Base and would fail StackMapTable verification for a merge used as a Base.
        java.util.Map<String, byte[]> res = new java.util.HashMap<String, byte[]>();
        res.put("app/A.class", classExtending("app/A", "app/Base"));
        res.put("app/B.class", classExtending("app/B", "app/Base"));
        ClassLoader hierarchy = new BytesLoader(res);   // app/Base intentionally absent
        assertEquals("app/Base", common(hierarchy, "app/A", "app/B"));
    }

    @Test
    public void resolvesImplementedInterfaceFromBytesWhenSuperclassIsAbsent() {
        // C implements app/I but extends an absent app/Base. Merging the interface-typed value with C must
        // resolve to app/I (read from C's interfaces[] in the bytes), NOT Object -- else a subsequent
        // invokeinterface on the merge gets an incompatible stack-map type.
        java.util.Map<String, byte[]> res = new java.util.HashMap<String, byte[]>();
        res.put("app/I.class", interfaceClass("app/I"));
        res.put("app/C.class", classExtendingImplementing("app/C", "app/Base", "app/I"));
        ClassLoader hierarchy = new BytesLoader(res);   // app/Base absent
        assertEquals("app/I", common(hierarchy, "app/I", "app/C"));
        assertEquals("app/I", common(hierarchy, "app/C", "app/I"));
    }

    private static byte[] classExtending(String internal, String superName) {
        return classExtendingImplementing(internal, superName, (String[]) null);
    }

    private static byte[] classExtendingImplementing(String internal, String superName, String... itfs) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internal, null, superName, itfs);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] interfaceClass(String internal) {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cw.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_ABSTRACT | org.objectweb.asm.Opcodes.ACC_INTERFACE,
                internal, null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** A loader that serves the given {@code name.class -> bytes} as resources (parent = bootstrap). */
    private static final class BytesLoader extends ClassLoader {
        private final java.util.Map<String, byte[]> resources;

        BytesLoader(java.util.Map<String, byte[]> resources) {
            super(null);
            this.resources = resources;
        }

        @Override
        public java.io.InputStream getResourceAsStream(String name) {
            byte[] b = resources.get(name);
            return b != null ? new java.io.ByteArrayInputStream(b) : super.getResourceAsStream(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] b = resources.get(name.replace('.', '/') + ".class");
            if (b == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, b, 0, b.length);
        }
    }
}
