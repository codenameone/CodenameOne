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
package com.codename1.tools.translator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Starts the CI test server, scripts/hellocodenameone/backend, for the native
 * desktop suites: it receives their screenshots over the websocket the device side
 * dials (ws://127.0.0.1:8765) and serves the REST surface their networking tests
 * drive.
 *
 * Two ways, chosen by the environment. With {@code CN1SS_SERVER_DIST} naming a copy
 * made by {@code server.sh dist} -- what a job with no Maven repository of its own
 * is handed, such as the Windows run -- it is plain java over that directory.
 * Otherwise {@code server.sh run} builds it here and runs it.
 */
final class Cn1ssTestServer {
    /** What the server prints once it accepts connections. */
    static final String READY = "listening on http port";

    private Cn1ssTestServer() {
    }

    /**
     * What server.sh needs from a test JVM's environment. The tests module names
     * its JDKs JDK_17_HOME and so on, where server.sh reads JAVA17_HOME; and a
     * checkout's own .m2-repo, when it has one, is where CI installed the backend
     * runtime and where a developer's concurrent checkouts cannot clobber it.
     */
    static void serverShEnvironment(java.util.Map<String, String> env) {
        String jdk17 = System.getenv("JDK_17_HOME");
        if (env.get("JAVA17_HOME") == null && jdk17 != null && !jdk17.trim().isEmpty()) {
            env.put("JAVA17_HOME", jdk17.trim());
        }
        Path m2 = Paths.get("..", "..", ".m2-repo").toAbsolutePath().normalize();
        String opts = env.get("MAVEN_OPTS");
        if (Files.isDirectory(m2) && (opts == null || !opts.contains("maven.repo.local"))) {
            env.put("MAVEN_OPTS", (opts == null ? "" : opts + " ") + "-Dmaven.repo.local=" + m2);
        }
    }

    static ProcessBuilder processBuilder(Path jdkHome, int port, Path outDir) throws Exception {
        Path repo = Paths.get("..", "..").toAbsolutePath().normalize();
        ProcessBuilder pb;
        String dist = System.getenv("CN1SS_SERVER_DIST");
        if (dist != null && !dist.trim().isEmpty()) {
            Path root = Paths.get(dist.trim()).toAbsolutePath();
            String main = new String(Files.readAllBytes(root.resolve("classes/META-INF/cn1-backend-main")),
                    "UTF-8").trim();
            String java = jdkHome.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();
            pb = new ProcessBuilder(java, "-cp", root.resolve("classes") + File.pathSeparator
                    + root.resolve("lib") + File.separator + "*", main);
            pb.directory(repo.resolve("scripts/hellocodenameone/backend").toFile());
        } else {
            pb = new ProcessBuilder(Arrays.asList("bash",
                    repo.resolve("scripts/hellocodenameone/backend/server.sh").toString(),
                    "run", "--jvm", "--port", String.valueOf(port), "--out", outDir.toString()));
            pb.environment().put("JAVA_HOME", jdkHome.toString());
            serverShEnvironment(pb.environment());
        }
        pb.environment().put("CN1SS_OUT", outDir.toString());
        pb.environment().put("CN1_SERVER_PORT", String.valueOf(port));
        pb.redirectErrorStream(true);
        return pb;
    }
}
