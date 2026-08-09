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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The output verifier double-checks every hardened class, but a class whose superclass is supplied only by
 * the target platform (absent from the supplied jars) must not be rejected just because ASM's load-based
 * data-flow verifier cannot resolve it -- its frames are already computed from the bytes.
 */
public class OutputVerifierTest {

    @Test
    public void unresolvedSuperclassIsAcceptedViaStructuralFallback() throws Exception {
        // app/C extends an absent app/Base. The data-flow verifier throws trying to resolve Base; verify()
        // must fall back to structural verification (which needs no hierarchy) and NOT reject the class.
        byte[] c = classWithSuperCtor("app/C", "app/Base");
        Map<String, byte[]> resources = new HashMap<String, byte[]>();
        resources.put("app/C.class", c);
        ClassLoader hierarchy = new BytesLoader(resources);   // app/Base intentionally absent

        Map<String, byte[]> classes = new LinkedHashMap<String, byte[]>();
        classes.put("app/C", c);
        OutputVerifier.verify(classes, hierarchy);   // must not throw
    }

    @Test
    public void structurallyValidClassWithResolvableHierarchyStillPasses() throws Exception {
        // A class whose hierarchy DOES resolve goes through the full data-flow verification and passes.
        Map<String, byte[]> classes = new LinkedHashMap<String, byte[]>();
        classes.put("app/Ok", classWithSuperCtor("app/Ok", "java/lang/Object"));
        OutputVerifier.verify(classes, getClass().getClassLoader());   // must not throw
    }

    private static byte[] classWithSuperCtor(String internal, String superName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internal, null, superName, null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** A loader that serves the given {@code name.class -> bytes} as resources (parent = bootstrap). */
    private static final class BytesLoader extends ClassLoader {
        private final Map<String, byte[]> resources;

        BytesLoader(Map<String, byte[]> resources) {
            super(null);
            this.resources = resources;
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            byte[] b = resources.get(name);
            return b != null ? new ByteArrayInputStream(b) : super.getResourceAsStream(name);
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
