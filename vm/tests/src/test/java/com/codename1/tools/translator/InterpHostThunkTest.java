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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The device runtime host build has to be able to construct a framework object
 * from interpreted code — {@code new Form()} written in a pushed program has to
 * reach {@code com_codename1_ui_Form}'s real constructor.
 *
 * <p>Every other method is reachable through the on-device-debug invoke thunks,
 * but those deliberately skip constructors: jdb never constructs, so a debug
 * build has no use for them. An interp-host build does, and a constructor thunk
 * is not just "the method thunk applied to &lt;init&gt;" — it has to allocate
 * first, pass the fresh object as the receiver, and hand it back as the thunk's
 * value even though the Java return type is {@code void}.</p>
 *
 * <p>Asserted on the emitted C because the alternative is discovering it as a
 * link error, or worse a wrong-arity call, during a device build.</p>
 */
class InterpHostThunkTest {

    private static final String HOST = "com/example/InterpCtorHost";
    private static final String MANGLED = "com_example_InterpCtorHost";

    private boolean previousInterpHost;
    private boolean previousOnDeviceDebug;
    private boolean previousOptimizer;

    @BeforeEach
    void enableInterpHost() {
        Parser.cleanup();
        previousInterpHost = BytecodeMethod.interpHost;
        previousOnDeviceDebug = BytecodeMethod.onDeviceDebug;
        previousOptimizer = BytecodeMethod.optimizerOn;
        // Mirrors what the cn1.interpHost system property does in
        // BytecodeMethod's static initialiser.
        BytecodeMethod.interpHost = true;
        BytecodeMethod.onDeviceDebug = true;
        BytecodeMethod.optimizerOn = false;
    }

    @AfterEach
    void restore() {
        BytecodeMethod.interpHost = previousInterpHost;
        BytecodeMethod.onDeviceDebug = previousOnDeviceDebug;
        BytecodeMethod.optimizerOn = previousOptimizer;
        Parser.cleanup();
    }

    /**
     * The thunk allocates through {@code __NEW_<cls>} and runs {@code <init>}
     * against that object. Without the allocation there is nothing to construct
     * into; the debugger's method thunks get their receiver handed to them.
     */
    @Test
    void aConstructorThunkAllocatesBeforeRunningInit() throws Exception {
        String thunk = ctorThunkFor("()V");

        assertTrue(thunk.contains("JAVA_OBJECT __r = __NEW_" + MANGLED + "(threadStateData);"),
                "the ctor thunk should allocate the receiver, was:\n" + thunk);
        assertTrue(thunk.contains(MANGLED + "___INIT____(threadStateData, __r)"),
                "the ctor thunk should run <init> against the allocated object, was:\n" + thunk);
    }

    /**
     * A constructor's Java return type is void, but the whole point of the
     * thunk is the object it produces. Returning 'V' would leave the
     * interpreter with a successfully constructed object it cannot reach.
     */
    @Test
    void aConstructorThunkReturnsTheConstructedObject() throws Exception {
        String thunk = ctorThunkFor("()V");

        assertTrue(thunk.contains("result->type = 'L';"),
                "the ctor thunk should report an object result, was:\n" + thunk);
        assertTrue(thunk.contains("result->value.o = __r;"),
                "the ctor thunk should hand back the allocated object, was:\n" + thunk);
        assertFalse(thunk.contains("result->type = 'V';"),
                "the ctor thunk must not report a void result, was:\n" + thunk);
    }

    /**
     * Constructors are non-static and non-private, which is exactly the shape
     * that normally earns a {@code virtual_} alias — but they are never
     * dispatched virtually, so no such symbol is emitted and calling it would
     * not link.
     */
    @Test
    void aConstructorThunkCallsTheDirectSymbolNotTheVirtualAlias() throws Exception {
        String thunk = ctorThunkFor("()V");

        assertFalse(thunk.contains("virtual_" + MANGLED + "___INIT__"),
                "a ctor has no virtual_ alias to call, was:\n" + thunk);
    }

    /**
     * Arguments still come out of the uniform {@code cn1_invoke_arg} array, and
     * the receiver slot is the freshly allocated object rather than the
     * caller-supplied {@code thisObj} — passing both would be a wrong-arity
     * call that fails at compile time on a device build.
     */
    @Test
    void anArgumentTakingConstructorPassesTheFreshObjectThenItsArguments() throws Exception {
        String descriptor = "(ILjava/lang/Object;)V";
        String thunk = ctorThunkFor(descriptor);

        assertTrue(thunk.contains(MANGLED + "___INIT____" + cSuffixFor(descriptor)
                        + "(threadStateData, __r, args[0].i, args[1].o)"),
                "the ctor thunk should pass __r then the unpacked args, was:\n" + thunk);
        assertFalse(thunk.contains(", thisObj,"),
                "the ctor thunk must not also pass the caller's receiver, was:\n" + thunk);
    }

    /**
     * A plain on-device-debug build keeps its current, smaller output. The
     * thunks are per-method C functions across the whole closed world, so
     * emitting constructors unconditionally would grow every debug build for a
     * consumer (jdb) that never constructs.
     */
    @Test
    void aPlainDebugBuildEmitsNoConstructorThunks() throws Exception {
        BytecodeMethod.interpHost = false;

        String generated = translateHost();

        assertFalse(generated.contains("__NEW_" + MANGLED + "(threadStateData);\n        "
                        + MANGLED + "___INIT__"),
                "a debug build should not emit constructor thunks, was:\n" + generated);
        assertTrue(generated.contains("__cn1_dbg_invoke_"),
                "a debug build should still emit ordinary method thunks");
    }

    /**
     * Registration is what makes a thunk reachable — an emitted but
     * unregistered thunk is dead code the interpreter can never call.
     */
    @Test
    void everyEmittedConstructorThunkIsRegistered() throws Exception {
        String generated = translateHost();

        Matcher m = Pattern.compile(
                "static void __cn1_dbg_invoke_(\\d+)\\([^)]*\\) \\{\\n"
                        + "    \\(void\\)args; \\(void\\)thisObj;[\\s\\S]*?"
                        + "JAVA_OBJECT __r = __NEW_").matcher(generated);
        int ctorThunks = 0;
        while (m.find()) {
            ctorThunks++;
            String id = m.group(1);
            assertTrue(generated.contains(
                            "cn1_debugger_register_invoke_thunk(" + id + ", &__cn1_dbg_invoke_" + id + ");"),
                    "ctor thunk " + id + " is emitted but never registered");
        }
        assertEquals(2, ctorThunks,
                "both fixture constructors should get a thunk, was:\n" + generated);
    }

    /**
     * Returns the body of the thunk generated for the constructor with the
     * given descriptor, located by the {@code __NEW_} allocation that only a
     * constructor thunk contains.
     */
    private String ctorThunkFor(String descriptor) throws Exception {
        String generated = translateHost();
        String argSuffix = cSuffixFor(descriptor);
        String initCall = MANGLED + "___INIT____" + argSuffix + "(threadStateData, __r";

        int call = generated.indexOf(initCall);
        assertTrue(call > 0,
                "no ctor thunk for " + descriptor + " (looked for " + initCall + ") in:\n" + generated);
        int start = generated.lastIndexOf("static void __cn1_dbg_invoke_", call);
        assertTrue(start >= 0, "ctor thunk body has no header, in:\n" + generated);
        int end = generated.indexOf("\nstatic void __cn1_dbg_invoke_", call);
        if (end < 0) {
            end = generated.indexOf("\n__attribute__((constructor))", call);
        }
        assertTrue(end > start, "could not delimit the ctor thunk body, in:\n" + generated);
        return generated.substring(start, end);
    }

    /** The translator's C name suffix for the fixture's two descriptors. */
    private String cSuffixFor(String descriptor) {
        if ("()V".equals(descriptor)) {
            return "";
        }
        if ("(ILjava/lang/Object;)V".equals(descriptor)) {
            // each arg contributes its own leading underscore
            return "_int_java_lang_Object";
        }
        throw new IllegalArgumentException(descriptor);
    }

    private String translateHost() throws Exception {
        Parser.cleanup();
        Parser.parse(writeHostClass().toFile());

        ByteCodeClass objectClass =
                new ByteCodeClass("java_lang_Object", "java/lang/Object");
        ByteCodeClass host = Parser.getClassObject(MANGLED);
        assertNotNull(host, "fixture class should have parsed");
        host.setBaseClassObject(objectClass);
        host.setBaseInterfacesObject(Collections.<ByteCodeClass>emptyList());
        host.updateAllDependencies();

        List<ByteCodeClass> classes = Arrays.asList(objectClass, host);
        // A real build assigns globally unique method offsets in
        // generateClassAndMethodIndexHeader before emitting any C. Thunks are
        // named and registered by that offset, so without this every thunk here
        // would be __cn1_dbg_invoke_0 -- duplicate definitions that would not
        // compile, and a registry with one entry instead of three.
        int offset = 0;
        for (ByteCodeClass bc : classes) {
            offset = bc.updateMethodOffsets(offset);
        }
        return host.generateCCode(classes);
    }

    /**
     * A concrete class with two constructors — a no-arg one and one taking a
     * primitive plus a reference, so the argument-unpacking path is covered —
     * and one ordinary method to contrast against.
     */
    private Path writeHostClass() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                HOST, null, "java/lang/Object", null);

        MethodVisitor noArg = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        noArg.visitCode();
        noArg.visitVarInsn(Opcodes.ALOAD, 0);
        noArg.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        noArg.visitInsn(Opcodes.RETURN);
        noArg.visitMaxs(1, 1);
        noArg.visitEnd();

        MethodVisitor withArgs = cw.visitMethod(
                Opcodes.ACC_PUBLIC, "<init>", "(ILjava/lang/Object;)V", null, null);
        withArgs.visitCode();
        withArgs.visitVarInsn(Opcodes.ALOAD, 0);
        withArgs.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        withArgs.visitInsn(Opcodes.RETURN);
        withArgs.visitMaxs(1, 3);
        withArgs.visitEnd();

        MethodVisitor ping = cw.visitMethod(Opcodes.ACC_PUBLIC, "ping", "()I", null, null);
        ping.visitCode();
        ping.visitInsn(Opcodes.ICONST_1);
        ping.visitInsn(Opcodes.IRETURN);
        ping.visitMaxs(1, 1);
        ping.visitEnd();

        cw.visitEnd();

        Path dir = Files.createTempDirectory("cn1-interp-host");
        Path classFile = dir.resolve("InterpCtorHost.class");
        Files.write(classFile, cw.toByteArray());
        return classFile;
    }
}
