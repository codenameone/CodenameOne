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

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Runs the shared backend's real socket path with less heap than the ZIP payload. */
class BackendZipStreamingTest {
    @Test
    void boundedDownloadsAndCancellationCloseSources() throws Exception {
        assumeFalse(CompilerHelper.isWindows(), "POSIX backend fixture");
        Path backend = Paths.get("..", "backend").toAbsolutePath().normalize();
        Path work = Files.createTempDirectory("backend-zip-streaming-");
        Path classes = Files.createDirectories(work.resolve("classes"));
        Path jdk = CompilerHelper.getAvailableCompilers("1.8").get(0).jdkHome;
        List<String> compile = new ArrayList<String>(Arrays.asList(jdk.resolve("bin/javac").toString(),
                "-encoding", "UTF-8", "-d", classes.toString()));
        for (Path root : Arrays.asList(backend.resolve("src"), backend.resolve("impl/javase"),
                backend.resolve("../tests/src/test/resources/com/codename1/tools/translator/zipserver"))) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> compile.add(p.toString()));
            }
        }
        String shared = run(backend, work, Arrays.asList("./shared-sources.sh"));
        for (String file : shared.split("\\r?\\n")) {
            if (!file.isEmpty()) compile.add(file);
        }
        run(backend, work, compile);
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        Path serverLog = work.resolve("server.log");
        Process server = new ProcessBuilder(jdk.resolve("bin/java").toString(), "-Xmx32m", "-cp",
                classes.toString(), "com.demo.ZipTestServer", String.valueOf(port))
                .redirectErrorStream(true).redirectOutput(serverLog.toFile()).start();
        try {
            String output = run(backend, work, Arrays.asList("python3", "tests/zip-streaming.py",
                    String.valueOf(port), work.resolve("artifacts").toString()));
            assertTrue(output.contains("ZIP_STREAMING_OK"), output);
            assertTrue(server.isAlive(), new String(Files.readAllBytes(serverLog), StandardCharsets.UTF_8));
        } finally {
            server.destroy();
            if (!server.waitFor(5, TimeUnit.SECONDS)) server.destroyForcibly();
        }
    }

    private static String run(Path directory, Path work, List<String> command) throws Exception {
        Path log = Files.createTempFile(work, "command-", ".log");
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(180, TimeUnit.SECONDS), "Timed out: " + command);
            String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
