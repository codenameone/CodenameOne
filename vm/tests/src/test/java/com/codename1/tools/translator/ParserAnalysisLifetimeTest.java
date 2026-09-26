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

import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.LabelInstruction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.LabelNode;
import static org.junit.jupiter.api.Assertions.*;

class ParserAnalysisLifetimeTest {
    @Test
    void retainedControlFlowLabelsDoNotRetainTemporaryAnalysisTrees() throws Exception {
        Parser.cleanup();
        Path directory = Files.createTempDirectory("parser-analysis-lifetime");
        Path source = directory.resolve("ControlFlow.java");
        Files.write(source, ("public class ControlFlow { static int run(int n) { int sum=0;"
                + "try { for(int i=0;i<n;i++) { if(i%2==0) sum+=i; else sum-=i; } }"
                + "catch(RuntimeException e) { return -1; } return sum; } }")
                .getBytes(StandardCharsets.UTF_8));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-source", "8", "-target", "8", "-g", "-d", directory.toString(), source.toString()));
        try {
            Parser.parse(directory.resolve("ControlFlow.class").toFile());
            int labels = 0;
            for (BytecodeMethod method : Parser.getClassObject("ControlFlow").getMethods()) {
                for (Instruction instruction : method.getInstructions()) {
                    if (instruction instanceof LabelInstruction) {
                        labels++;
                        assertFalse(((LabelInstruction) instruction).getLabel().info instanceof LabelNode,
                                "A retained bytecode label still owns the linked ASM analysis tree");
                    }
                }
            }
            assertTrue(labels >= 5, "Exercise branches and exception-handler labels");
        } finally {
            Parser.cleanup();
        }
    }
}
