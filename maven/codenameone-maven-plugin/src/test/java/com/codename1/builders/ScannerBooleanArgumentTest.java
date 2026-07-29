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
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the class scanner can tell about a boolean argument.
 *
 * <p>Real bytecode rather than a hand-driven visitor, because the whole
 * question is what javac emits and what ASM reports: a setter that
 * switches a feature off looks exactly like one that switches it on
 * unless the constant before the call is read, and the build's decision
 * between "Bluetooth only" and "Health Connect, permissions, privacy
 * policy, Play review" hangs on the difference.</p>
 */
class ScannerBooleanArgumentTest {

    private static final String TARGET =
            "com/codename1/health/sensors/SensorSessionOptions";

    /** Records what the scanner reported for each call. */
    private static final class Recorder implements Executor.ClassScanner {
        private final List<String> calls = new ArrayList<String>();

        @Override
        public void usesClass(String cls) {
        }

        @Override
        public void usesClassMethod(String cls, String method) {
        }

        @Override
        public void implementsInterface(String cls, String iface) {
        }

        @Override
        public void usesClassMethodWithBooleanArgument(String cls,
                String method, Boolean value) {
            if (TARGET.equals(cls)) {
                calls.add(method + "=" + value);
            }
        }
    }

    /** Executor is abstract; the scan itself needs none of these. */
    private static final class Scanner extends Executor {
        @Override
        public boolean build(File sourceZip, BuildRequest request) {
            return false;
        }

        @Override
        protected String getDeviceIdCode() {
            return "";
        }

        @Override
        protected String generatePeerComponentCreationCode(
                String methodCallString) {
            return "";
        }

        @Override
        protected String convertPeerComponentToNative(String param) {
            return "";
        }
    }

    /**
     * Emits a class whose single method makes the calls described by
     * {@code args}: TRUE and FALSE become literals, null becomes a value
     * loaded from a parameter, which is what a flag computed at runtime
     * looks like.
     */
    private static void writeCaller(File dir, String name, Boolean... args)
            throws Exception {
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "app/" + name, null,
                "java/lang/Object", null);
        MethodVisitor m = w.visitMethod(Opcodes.ACC_PUBLIC
                | Opcodes.ACC_STATIC, "run", "(Z)V", null, null);
        m.visitCode();
        for (Boolean arg : args) {
            m.visitTypeInsn(Opcodes.NEW, TARGET);
            if (arg == null) {
                m.visitVarInsn(Opcodes.ILOAD, 0);
            } else {
                m.visitInsn(arg.booleanValue()
                        ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
            }
            m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TARGET,
                    "setWriteToStore", "(Z)L" + TARGET + ";", false);
            m.visitInsn(Opcodes.POP);
        }
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(3, 1);
        m.visitEnd();
        w.visitEnd();

        File pkg = new File(dir, "app");
        assertTrue(pkg.isDirectory() || pkg.mkdirs());
        OutputStream out = new FileOutputStream(new File(pkg, name + ".class"));
        try {
            out.write(w.toByteArray());
        } finally {
            out.close();
        }
    }

    private static List<String> scan(File dir) throws Exception {
        Recorder r = new Recorder();
        new Scanner().scanClassesForPermissions(dir, r);
        return r.calls;
    }

    @Test
    void aLiteralArgumentIsReported(@TempDir File dir) throws Exception {
        writeCaller(dir, "Literals", Boolean.TRUE, Boolean.FALSE);
        List<String> calls = scan(dir);
        assertEquals(2, calls.size(), "both calls must be reported: " + calls);
        assertTrue(calls.contains("setWriteToStore=true"), calls.toString());
        assertTrue(calls.contains("setWriteToStore=false"), calls.toString());
    }

    /**
     * A value that is not a literal reads as unknown, never as false: the
     * caller treats unknown as the feature being on, and a false here
     * would silently drop the permissions an app really needs.
     */
    @Test
    void aComputedArgumentIsReportedAsUnknown(@TempDir File dir)
            throws Exception {
        writeCaller(dir, "Computed", new Boolean[] {null});
        assertEquals("[setWriteToStore=null]", scan(dir).toString());
    }

    /**
     * A constant that is merely the last thing pushed before a merge point
     * is not this call's argument.
     *
     * <p>This is the shape javac emits for
     * {@code setWriteToStore(flag ? true : false)}: the false arm is laid
     * out last, so {@code ICONST_0} is the instruction physically before
     * the call with only a label between them. Read as the argument, an
     * app that enables write-through on one path would be built without
     * the health stack.</p>
     */
    @Test
    void aConstantReachingTheCallThroughABranchIsNotRead(@TempDir File dir)
            throws Exception {
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "app/Branchy", null,
                "java/lang/Object", null);
        MethodVisitor m = w.visitMethod(Opcodes.ACC_PUBLIC
                | Opcodes.ACC_STATIC, "run", "(Z)V", null, null);
        m.visitCode();
        m.visitTypeInsn(Opcodes.NEW, TARGET);
        m.visitVarInsn(Opcodes.ILOAD, 0);
        Label otherwise = new Label();
        Label join = new Label();
        m.visitJumpInsn(Opcodes.IFEQ, otherwise);
        m.visitInsn(Opcodes.ICONST_1);
        m.visitJumpInsn(Opcodes.GOTO, join);
        m.visitLabel(otherwise);
        m.visitInsn(Opcodes.ICONST_0);
        m.visitLabel(join);
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TARGET,
                "setWriteToStore", "(Z)L" + TARGET + ";", false);
        m.visitInsn(Opcodes.POP);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(3, 1);
        m.visitEnd();
        w.visitEnd();

        File pkg = new File(dir, "app");
        assertTrue(pkg.isDirectory() || pkg.mkdirs());
        OutputStream out = new FileOutputStream(
                new File(pkg, "Branchy.class"));
        try {
            out.write(w.toByteArray());
        } finally {
            out.close();
        }

        assertEquals("[setWriteToStore=null]", scan(dir).toString(),
                "a constant that reaches the call through a branch must"
                        + " not be read as the argument");
    }
}
