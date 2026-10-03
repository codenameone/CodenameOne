/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.analyze.Program;
import com.codename1.dart.transpiler.analyze.StubRegistry;
import com.codename1.dart.transpiler.api.Diagnostic;
import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.api.GeneratedFile;
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.codegen.JavaEmitter;
import com.codename1.dart.transpiler.parser.AstBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CounterTranspileTest {

    private String readResource(String name) throws Exception {
        InputStream in = getClass().getResourceAsStream(name);
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buf.write(chunk, 0, n);
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    public void counterAppTranspilesWithoutErrors() throws Exception {
        Diagnostics diags = new Diagnostics();
        AstBuilder builder = new AstBuilder(diags);
        Program program = new Program();
        program.add(builder.parse("main.dart", readResource("/fixtures/counter_main.dart")));

        StubRegistry stubs = StubRegistry.loadEmbedded(diags);
        JavaEmitter emitter = new JavaEmitter(program, stubs, diags, "com.codename1.generated.flutter");
        List<GeneratedFile> files = emitter.emit();

        StringBuilder all = new StringBuilder();
        for (GeneratedFile f : files) {
            all.append("// ===== ").append(f.relativePath).append(" =====\n").append(f.content).append('\n');
        }
        System.out.println(all);
        for (Diagnostic d : diags.asList()) {
            System.out.println("DIAG: " + d);
        }
        assertTrue(!diags.hasErrors(), "diagnostics: " + diags.asList());
        assertEquals(5, files.size(), "MyApp, MyHomePage, _MyHomePageState, MainLib, FlutterRegistry");
    }
}
