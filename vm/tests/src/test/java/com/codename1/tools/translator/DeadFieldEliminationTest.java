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

import com.codename1.tools.translator.bytecodes.InlinableConstructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DeadFieldEliminationTest {
    @TempDir
    Path directory;

    @BeforeEach
    @AfterEach
    void cleanParser() {
        Parser.cleanup();
    }

    /**
     * Constructors with matching signatures in different classes must all have
     * their cached plans refreshed. Otherwise an allocation still writes fields
     * that dead-field elimination removed from the generated struct (#5959).
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void refreshesEveryConstructorAfterRemovingFields(boolean reverseOrder) throws Exception {
        StringBuilder source = new StringBuilder();
        for (String name : new String[]{"DeadFieldFirst", "DeadFieldSecond"}) {
            source.append("class ").append(name).append(" {\n")
                    .append("  boolean runnable = true;\n")
                    .append("  Object headers = null;\n")
                    .append("  int live;\n")
                    .append("  ").append(name).append("() { live = 1; }\n")
                    .append("  ").append(name).append("(int value) { live = value; }\n")
                    .append("  int readLive() { return live; }\n")
                    .append("}\n");
        }
        Path sourceFile = directory.resolve("DeadFields.java");
        Files.write(sourceFile, source.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-source", "8", "-target", "8", "-d", directory.toString(), sourceFile.toString()));
        List<String> names = Arrays.asList("DeadFieldFirst", "DeadFieldSecond");
        if (reverseOrder) {
            Collections.reverse(names);
        }
        for (String name : names) {
            Parser.parse(directory.resolve(name + ".class").toFile());
        }
        List<ByteCodeClass> classes = Arrays.asList(
                Parser.getClassObject(names.get(0)), Parser.getClassObject(names.get(1)));
        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod method : cls.getMethods()) {
                if (method.isConstructor()) {
                    assertNotNull(method.getInlinableConstructorPlan());
                    assertEquals(3, method.getInlinableConstructorPlan().storeCount());
                }
            }
        }

        DeadFieldElimination.run(classes, new String[0]);

        assertEquals(4, DeadFieldElimination.removedFields);
        assertEquals(8, DeadFieldElimination.rewrittenStores);
        int constructors = 0;
        for (ByteCodeClass cls : classes) {
            assertEquals(1, cls.getFields().size());
            assertEquals("live", cls.getFields().get(0).getFieldName());
            for (BytecodeMethod method : cls.getMethods()) {
                if (!method.isConstructor()) {
                    continue;
                }
                constructors++;
                InlinableConstructor plan = method.getInlinableConstructorPlan();
                assertNotNull(plan, "removing dead stores should preserve constructor inlining");
                StringBuilder emitted = new StringBuilder();
                plan.appendStores(emitted, "__ibp", new String[]{"liveArgument"});
                String code = emitted.toString();
                String context = cls.getClsName() + method.getSignature() + ":\n" + code;
                assertFalse(code.contains("_runnable"), context);
                assertFalse(code.contains("_headers"), context);
                assertTrue(code.contains(cls.getClsName() + "_live = "
                        + ("()V".equals(method.getSignature()) ? "1" : "liveArgument") + ";"), context);
                assertEquals(1, plan.storeCount(), context);
            }
        }
        assertEquals(4, constructors);
    }
}
