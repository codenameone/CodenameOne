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
package com.codename1.tools.translator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A method that catches anything must not keep its operand stack pointer live
 * across the {@code setjmp}.
 *
 * <p>A try/catch is compiled into a {@code setjmp} region, so the edges leaving
 * it are abnormal ones. {@code SP} is modified all through that region -- every
 * push and pop moves it. C99 7.13.2.1p3 makes the value of a non-volatile
 * automatic that was modified since the {@code setjmp} <em>indeterminate</em>
 * after a {@code longjmp}, and {@code gcc} on musl rejected the whole
 * translation unit with {@code internal compiler error: SSA corruption} --
 * "Unable to coalesce ssa_names 57 and 58 which are marked as MUST COALESCE,
 * SP_57(ab) and SP_58(ab), during RTL pass: expand" -- because two versions of
 * {@code SP} were live across an abnormal edge. A short-circuit condition
 * inside a {@code try} was enough to trigger it.</p>
 *
 * <p>The first fix declared {@code SP} {@code volatile}, which sent every push
 * and pop of such a method through memory. The fix now is that {@code SP} is
 * never live there: {@code CN1_TRY_ENTRY} assigns it from the frame's const
 * {@code cn1SpEntry} on both returns of the {@code setjmp}, and the catch
 * blocks assign it again before reading it. These tests pin both halves: the
 * frame keeps {@code SP} unqualified, and the macros restart it.</p>
 */
class SetjmpFrameVolatileTest {

    private static final String HOST = "com/example/SjHost";

    @BeforeEach
    void cleanParser() {
        Parser.cleanup();
    }

    /** Neither frame declares a volatile SP; the catching one enters a try. */
    @Test
    void setjmpFramesKeepTheStackPointerInARegister() throws Exception {
        String code = translateHost();

        String guarded = cFunctionBody(code, "_catches___");
        assertTrue(guarded.contains("CN1_TRY_ENTRY()"), "the catching method enters a try, was:\n" + guarded);
        assertFalse(hasVolatileStackPointer(guarded),
                "SP is restarted after the setjmp, not kept in memory, was:\n" + guarded);

        String plain = cFunctionBody(code, "_plain___");
        assertFalse(hasVolatileStackPointer(plain), "a method with no catch, was:\n" + plain);
    }

    /**
     * The try entry restarts SP after the setjmp, from a const every frame
     * declares, and the catch block assigns it before it reads it.
     */
    @Test
    void theTryEntryRestartsTheStackPointer() throws Exception {
        String h = new String(Files.readAllBytes(Paths.get("..", "ByteCodeTranslator", "src", "cn1_globals.h")),
                StandardCharsets.US_ASCII).replaceAll("\\s+", " ");
        String entry = macro(h, "#define CN1_TRY_ENTRY()");
        assertTrue(entry.indexOf("SP = cn1SpEntry;") > entry.indexOf("CN1_TRY_SETJMP"),
                "CN1_TRY_ENTRY must assign SP after the setjmp, was: " + entry);
        assertTrue(macro(h, "#define CN1_DECLARE_SP(").contains("struct elementStruct* const cn1SpEntry"),
                "every frame declares the const SP starts from");
        String catchBlock = macro(h, "#define DEFINE_CATCH_BLOCK(");
        assertTrue(catchBlock.contains("SP = &stack[1];"), "the catch block assigns SP, was: " + catchBlock);
    }

    /** The text of the macro starting at {@code head}, up to the next directive. */
    private static String macro(String flat, String head) {
        int at = flat.indexOf(head);
        assertTrue(at >= 0, "cn1_globals.h has no " + head);
        int end = flat.indexOf("#define", at + head.length());
        return flat.substring(at, end < 0 ? flat.length() : end);
    }

    /** Whether this frame declares SP as a volatile pointer, in any spelling. */
    private boolean hasVolatileStackPointer(String body) {
        String flat = body.replaceAll("\\s+", " ");
        return flat.contains("elementStruct* volatile SP")
                || flat.contains("elementStruct * volatile SP")
                || flat.contains("_VSP(");
    }

    private String translateHost() throws Exception {
        Parser.parse(writeHostClass().toFile());

        ByteCodeClass objectClass =
                new ByteCodeClass("java_lang_Object", "java/lang/Object");
        ByteCodeClass host = Parser.getClassObject("com_example_SjHost");
        host.setBaseClassObject(objectClass);
        host.setBaseInterfacesObject(Collections.<ByteCodeClass>emptyList());
        host.updateAllDependencies();

        List<ByteCodeClass> classes = Arrays.asList(objectClass, host);
        return host.generateCCode(classes);
    }

    /** The text of the first generated C function whose name contains {@code marker}. */
    private String cFunctionBody(String code, String marker) {
        int nameAt = code.indexOf(marker);
        assertTrue(nameAt >= 0,
                "generated code has no function matching " + marker + ":\n" + code);
        int start = code.indexOf('{', nameAt);
        int end = code.indexOf("\n}", start + 1);
        assertTrue(start >= 0 && end > start,
                "could not delimit the body of " + marker);
        return code.substring(start, end);
    }

    /**
     * Two static methods that differ only in whether they catch.
     *
     * <p>{@code catches(a, b)} is {@code try { return f(a) && f(b); } catch
     * (Throwable t) { return false; }} -- the shape from the original report,
     * where the short-circuit temporary is what ends up live across the region.
     * {@code plain(a, b)} is the same condition with no handler.</p>
     */
    private Path writeHostClass() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                HOST, null, "java/lang/Object", null);

        MethodVisitor probe = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "probe", "(I)Z",
                null, null);
        probe.visitCode();
        probe.visitVarInsn(Opcodes.ILOAD, 0);
        Label zero = new Label();
        probe.visitJumpInsn(Opcodes.IFEQ, zero);
        probe.visitInsn(Opcodes.ICONST_1);
        probe.visitInsn(Opcodes.IRETURN);
        probe.visitLabel(zero);
        probe.visitInsn(Opcodes.ICONST_0);
        probe.visitInsn(Opcodes.IRETURN);
        probe.visitMaxs(2, 1);
        probe.visitEnd();

        emitShortCircuit(cw, "catches", true);
        emitShortCircuit(cw, "plain", false);

        cw.visitEnd();
        return writeClassFile(HOST, cw);
    }

    /** {@code probe(a) && probe(b)}, optionally wrapped in a catch-all. */
    private void emitShortCircuit(ClassWriter cw, String name,
            boolean guarded) {
        MethodVisitor m = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, "(II)Z",
                null, null);
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label falseArm = new Label();
        m.visitCode();
        if (guarded) {
            m.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
        }
        m.visitLabel(start);
        m.visitVarInsn(Opcodes.ILOAD, 0);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, HOST, "probe", "(I)Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, falseArm);
        m.visitVarInsn(Opcodes.ILOAD, 1);
        m.visitMethodInsn(Opcodes.INVOKESTATIC, HOST, "probe", "(I)Z", false);
        m.visitJumpInsn(Opcodes.IFEQ, falseArm);
        m.visitInsn(Opcodes.ICONST_1);
        m.visitInsn(Opcodes.IRETURN);
        m.visitLabel(falseArm);
        m.visitInsn(Opcodes.ICONST_0);
        m.visitInsn(Opcodes.IRETURN);
        m.visitLabel(end);
        if (guarded) {
            m.visitLabel(handler);
            m.visitInsn(Opcodes.POP);
            m.visitInsn(Opcodes.ICONST_0);
            m.visitInsn(Opcodes.IRETURN);
        }
        m.visitMaxs(3, 3);
        m.visitEnd();
    }

    private Path writeClassFile(String internalName, ClassWriter cw)
            throws Exception {
        Path outputDir = Files.createTempDirectory("parparvm-setjmp-volatile");
        Path classFile = outputDir.resolve(internalName + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, cw.toByteArray());
        return classFile;
    }
}
