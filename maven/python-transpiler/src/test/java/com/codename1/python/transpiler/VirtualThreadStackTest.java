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
package com.codename1.python.transpiler;

import com.codename1.python.transpiler.api.PythonTranspiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The `python.virtualThreadStack` build hint reaches the generated server.
class VirtualThreadStackTest {
    @TempDir
    Path tmp;

    /// A served program, transpiled against a class path that has the
    /// backend's `HttpServer` -- the one thing that makes it a backend build.
    private String backendServer(int stack) throws Exception {
        Path cls = tmp.resolve("com/codename1/backend/HttpServer.class");
        if (!Files.exists(cls)) {
            // A stand-in class: only its presence is read.
            Path src = tmp.resolve("src/com/codename1/backend/HttpServer.java");
            Files.createDirectories(src.getParent());
            Files.write(src, "package com.codename1.backend; public class HttpServer { }"
                    .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            assertEquals(0, javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "-d", tmp.toString(), src.toString()));
        }
        Map<String, String> sources = new TreeMap<String, String>();
        sources.put("main.py", "import _cn1server\n");
        Map<String, String> out = PythonTranspiler.transpile(sources, "main",
                Collections.singletonList(tmp.toFile()), Collections.<String, String>emptyMap(), stack);
        String server = out.get("com/codename1/generated/python/BackendServer.java");
        assertTrue(server != null, String.valueOf(out.keySet()));
        return server;
    }

    @Test
    void theDefaultIsOneMegabyte() throws Exception {
        assertTrue(backendServer(PythonTranspiler.DEFAULT_VIRTUAL_THREAD_STACK)
                .contains("HttpServer.requireVirtualThreadStack(1048576);"));
    }

    @Test
    void theHintSetsTheStack() throws Exception {
        String server = backendServer(PythonTranspiler.parseStackSize("4MB"));
        assertTrue(server.contains("HttpServer.requireVirtualThreadStack(4194304);"), server);
    }

    @Test
    void sizesParse() {
        assertEquals(4096, PythonTranspiler.parseStackSize("4096"));
        assertEquals(512 * 1024, PythonTranspiler.parseStackSize(" 512 kb "));
        assertEquals(2 * 1024 * 1024, PythonTranspiler.parseStackSize("2MB"));
        assertEquals(-1, PythonTranspiler.parseStackSize(""));
        assertEquals(-1, PythonTranspiler.parseStackSize("0KB"));
        assertEquals(-1, PythonTranspiler.parseStackSize("1.5MB"));
        assertEquals(-1, PythonTranspiler.parseStackSize("4096MB"));
        assertEquals(-1, PythonTranspiler.parseStackSize(new File("x").getName()));
    }
}
