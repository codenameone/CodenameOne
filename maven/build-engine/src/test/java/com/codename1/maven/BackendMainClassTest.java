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
package com.codename1.maven;

import com.codename1.build.BuildFailureException;
import com.codename1.build.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendMainClassTest {
    @TempDir
    Path tmp;

    /// Writes `name` (internal form) into `dir`, with a `main` when asked.
    private static void writeClass(File dir, String name, boolean withMain) throws IOException {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        if (withMain) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main",
                    "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        File f = new File(dir, name + ".class");
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), cw.toByteArray());
    }

    private static BackendMainClass finder() {
        return new BackendMainClass(new SystemStreamLog(), "-Pcn1.backend.mainClass");
    }

    /// Gradle compiles Java and Kotlin into separate directories: one main
    /// between them is found, and one in each is as ambiguous as two in one.
    @Test
    void theMainClassIsFoundAcrossClassDirectoriesAndAmbiguityIsReported() throws Exception {
        File java = new File(tmp.toFile(), "java");
        File kotlin = new File(tmp.toFile(), "kotlin");
        writeClass(java, "a/Helper", false);
        writeClass(kotlin, "a/Server", true);
        assertEquals("a.Server", finder().findMainClass(Arrays.asList(java, kotlin)));

        writeClass(java, "a/Other", true);
        BuildFailureException ex = assertThrows(BuildFailureException.class,
                () -> finder().findMainClass(Arrays.asList(java, kotlin)));
        assertTrue(ex.getMessage().contains("a.Server") && ex.getMessage().contains("a.Other"), ex.getMessage());
    }

    @Test
    void noMainAnywhereSaysHowToChooseOne() {
        BuildFailureException ex = assertThrows(BuildFailureException.class,
                () -> finder().findMainClass(Arrays.asList(new File(tmp.toFile(), "none"))));
        assertTrue(ex.getMessage().contains("-Pcn1.backend.mainClass"), ex.getMessage());
    }
}
