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
package com.codename1.io;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Desktop code compiled against `java.lang.Process` and
/// `java.lang.ProcessBuilder` is moved onto the core classes by renaming
/// those two classes and nothing else, so every JavaSE member must exist here
/// under the descriptor the rename produces -- or be listed as one the device
/// cannot have. Members newer than the running JDK are simply not seen.
class ProcessApiShapeTest {
    private static final Set<String> PROCESS_ABSENT = new HashSet<String>(Arrays.asList(
            "toHandle()", "onExit()", "info()", "children()", "descendants()",
            "supportsNormalTermination()",
            "inputReader()", "inputReader(java.nio.charset.Charset)",
            "errorReader()", "errorReader(java.nio.charset.Charset)",
            "outputWriter()", "outputWriter(java.nio.charset.Charset)"));
    private static final Set<String> BUILDER_ABSENT = new HashSet<String>(Arrays.asList(
            "directory()", "directory(java.io.File)", "inheritIO()",
            "redirectInput()", "redirectOutput()", "redirectError()",
            "redirectInput(java.lang.ProcessBuilder$Redirect)",
            "redirectOutput(java.lang.ProcessBuilder$Redirect)",
            "redirectError(java.lang.ProcessBuilder$Redirect)",
            "redirectInput(java.io.File)", "redirectOutput(java.io.File)", "redirectError(java.io.File)",
            "startPipeline(java.util.List)"));

    @Test void everyJavaSEProcessMemberHasACounterpart() {
        assertEquals(new ArrayList<String>(), missing(java.lang.Process.class, Process.class, PROCESS_ABSENT));
    }

    @Test void everyJavaSEProcessBuilderMemberHasACounterpart() {
        assertEquals(new ArrayList<String>(),
                missing(java.lang.ProcessBuilder.class, ProcessBuilder.class, BUILDER_ABSENT));
        assertTrue(Modifier.isFinal(ProcessBuilder.class.getModifiers()));
        for (Constructor<?> c : java.lang.ProcessBuilder.class.getConstructors()) {
            try {
                ProcessBuilder.class.getConstructor(c.getParameterTypes());
            } catch (NoSuchMethodException ex) {
                fail("No constructor " + c);
            }
        }
    }

    /// A subclass written against JavaSE implements only what is abstract
    /// there, so nothing may be abstract here that is concrete there.
    @Test void aJavaSESubclassImplementsEveryAbstractMethod() throws Exception {
        for (Method m : Process.class.getMethods()) {
            if (Modifier.isAbstract(m.getModifiers())) {
                Method jdk = java.lang.Process.class.getMethod(m.getName(), m.getParameterTypes());
                assertTrue(Modifier.isAbstract(jdk.getModifiers()), m + " is concrete in JavaSE");
            }
        }
    }

    private static List<String> missing(Class<?> jdk, Class<?> core, Set<String> absent) {
        List<String> out = new ArrayList<String>();
        for (Method m : jdk.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            String key = key(m);
            if (absent.contains(key)) {
                continue;
            }
            Method mine;
            try {
                mine = core.getMethod(m.getName(), m.getParameterTypes());
            } catch (NoSuchMethodException ex) {
                out.add(key);
                continue;
            }
            if (!renamed(m.getReturnType()).equals(mine.getReturnType().getName())
                    || Modifier.isStatic(m.getModifiers()) != Modifier.isStatic(mine.getModifiers())) {
                out.add(key + " differs");
            }
        }
        return out;
    }

    private static String renamed(Class<?> type) {
        if (type == java.lang.Process.class) {
            return Process.class.getName();
        }
        if (type == java.lang.ProcessBuilder.class) {
            return ProcessBuilder.class.getName();
        }
        return type.getName();
    }

    private static String key(Method m) {
        StringBuilder sb = new StringBuilder(m.getName()).append('(');
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(params[i].getName());
        }
        return sb.append(')').toString();
    }
}
