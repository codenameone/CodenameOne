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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scanner reports CONSTRUCTOR calls through
 * {@link Executor.ClassScanner#usesClassMethodWithDescriptor}.
 *
 * <p>It did not, and nothing said so. Both the ASM path and the no-ASM
 * fallback delivered that callback only when the method name was not
 * {@code <init>}, so every consumer that matched a constructor by descriptor
 * was unreachable code that compiled, passed its own unit tests -- which called
 * the predicate directly -- and never once fired in a build.</p>
 *
 * <p>The consumer that made this matter is the location button's:
 * {@code new MapComponent()} looks up a location and
 * {@code new MapComponent(provider, centre, zoom)} does not, and only the
 * descriptor tells them apart. This test drives the REAL scan over a real class
 * file, because a test of the predicate proves the predicate and this bug was
 * in the wiring.</p>
 */
class ConstructorDescriptorScanTest {

    /** Executor is abstract; none of these hooks are exercised here. */
    private static final class TestExecutor extends Executor {
        @Override
        protected String getDeviceIdCode() {
            return "";
        }

        @Override
        protected String generatePeerComponentCreationCode(String call) {
            return "";
        }

        @Override
        protected String convertPeerComponentToNative(String param) {
            return "";
        }

        @Override
        public boolean build(File sourceZip, BuildRequest request) {
            return false;
        }
    }

    /** Every (owner, name, descriptor) the scan reported. */
    private static final class Recorder implements Executor.ClassScanner {
        private final List<String> calls = new ArrayList<String>();

        @Override
        public void usesClass(String cls) {
        }

        @Override
        public void implementsInterface(String cls, String iface) {
        }

        @Override
        public void usesClassMethod(String cls, String method) {
        }

        @Override
        public void usesClassMethodWithDescriptor(String cls, String method,
                String descriptor) {
            calls.add(cls + "#" + method + descriptor);
        }
    }

    /**
     * Writes a class whose one method constructs {@code owner} twice, with a
     * no-argument constructor and a one-String constructor.
     */
    private static void writeConstructingClass(File at, String owner)
            throws Exception {
        at.getParentFile().mkdirs();
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Builder", null,
                "java/lang/Object", null);
        MethodVisitor m = w.visitMethod(Opcodes.ACC_PUBLIC, "make", "()V",
                null, null);
        m.visitCode();
        m.visitTypeInsn(Opcodes.NEW, owner);
        m.visitInsn(Opcodes.DUP);
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", "()V", false);
        m.visitInsn(Opcodes.POP);
        m.visitTypeInsn(Opcodes.NEW, owner);
        m.visitInsn(Opcodes.DUP);
        m.visitLdcInsn("centre");
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>",
                "(Ljava/lang/String;)V", false);
        m.visitInsn(Opcodes.POP);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(3, 1);
        m.visitEnd();
        w.visitEnd();
        OutputStream out = new FileOutputStream(at);
        try {
            out.write(w.toByteArray());
        } finally {
            out.close();
        }
    }

    @Test
    void constructorsAreReportedWithTheirDescriptors() throws Exception {
        File root = Files.createTempDirectory("cn1-ctor-scan").toFile();
        writeConstructingClass(new File(root, "com/example/Builder.class"),
                "com/example/Widget");
        Recorder recorder = new Recorder();
        new TestExecutor().scanClassesForPermissions(root, recorder);
        assertTrue(recorder.calls.contains("com/example/Widget#<init>()V"),
                "the no-argument constructor must be reported: "
                + recorder.calls);
        assertTrue(recorder.calls.contains(
                "com/example/Widget#<init>(Ljava/lang/String;)V"),
                "and the one-argument constructor, distinctly: "
                + recorder.calls);
    }
}
