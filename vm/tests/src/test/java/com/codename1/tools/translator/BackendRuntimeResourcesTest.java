/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BackendRuntimeResourcesTest {
    @Test
    void translationDoesNotRewriteSharedRuntimeResources() throws Exception {
        BackendTestSupport.require(!CompilerHelper.isWindows(), "backend build.sh requires POSIX");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available");
        Path backend = Paths.get("..", "backend").toAbsolutePath().normalize();
        Path translator = Paths.get("..", "ByteCodeTranslator", "target", "classes");
        String[] names = {"cn1_globals.m", "cn1_virtual_thread.h"};
        FileTime[] modified = new FileTime[names.length];
        byte[][] contents = new byte[names.length][];
        for (int i = 0; i < names.length; i++) {
            Path resource = translator.resolve(names[i]);
            modified[i] = Files.getLastModifiedTime(resource);
            contents[i] = Files.readAllBytes(resource);
        }
        Path work = Files.createTempDirectory("cn1-backend-runtime-resources-");
        Path demo = Files.createDirectories(work.resolve("demo"));
        Files.write(demo.resolve("RuntimeResourceProbe.java"),
                ("package com.demo; public class RuntimeResourceProbe { "
                + "public static void main(String[] args) { System.out.println(42); } }")
                .getBytes(StandardCharsets.UTF_8));
        Path output = work.resolve("translated");
        Path log = work.resolve("build.log");
        ProcessBuilder build = new ProcessBuilder("bash", "build.sh", "RuntimeResourceProbe",
                "com.demo", work.resolve("unused-binary").toString());
        build.directory(backend.toFile());
        build.environment().put("JDK_8_HOME", jdk8.toString());
        build.environment().put("JAVA_HOME", jdk8.toString());
        build.environment().put("CN1_BACKEND_DEMO", demo.toString());
        build.environment().put("CN1_BACKEND_STANDALONE_DEMO", "1");
        build.environment().put("CN1_BACKEND_SRC_OUT", output.toString());
        build.environment().put("CN1_BACKEND_HTTPS", "1");
        build.environment().put("CN1_BACKEND_SQLITE", "0");
        build.redirectErrorStream(true);
        build.redirectOutput(log.toFile());
        Process process = build.start();
        try {
            assertTrue(process.waitFor(5, TimeUnit.MINUTES), "backend translation timed out: " + log);
            assertEquals(0, process.exitValue(), new String(Files.readAllBytes(log), StandardCharsets.UTF_8));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
        for (int i = 0; i < names.length; i++) {
            Path resource = translator.resolve(names[i]);
            assertArrayEquals(contents[i], Files.readAllBytes(resource), names[i]);
            // Checking only bytes misses cp's truncate-and-rewrite window: other
            // translator processes can observe an empty file during that window.
            assertEquals(modified[i], Files.getLastModifiedTime(resource),
                    "backend builds must not rewrite the shared classpath resource " + names[i]);
        }
        assertArrayEquals(Files.readAllBytes(Paths.get("..", "ByteCodeTranslator", "src", "cn1_virtual_thread.h")),
                Files.readAllBytes(output.resolve("cn1_virtual_thread.h")),
                "the translated project must receive the current runtime header");
    }
}
