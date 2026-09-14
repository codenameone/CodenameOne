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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Shared plumbing for the tests that drive a translated server binary: find a JDK
 * 8, build vm/backend with its own build script, and wait for the port.
 *
 * The build goes through the real build.sh rather than a reimplementation of it. A
 * test that builds differently from the product is testing something else.
 */
final class BackendTestSupport {

    private BackendTestSupport() {
    }

    /**
     * Set CN1_BACKEND_REQUIRED=1 where the backend is expected to build -- CI, in
     * particular. Without it a missing toolchain skips these tests, which is right
     * on a developer machine and wrong on a build machine: a suite that quietly
     * stops running is worse than no suite, because it still reports green.
     */
    static boolean isRequired() {
        return "1".equals(System.getenv("CN1_BACKEND_REQUIRED"));
    }

    /**
     * Skips, or fails when the backend is required here. Every abort in these
     * tests goes through this so no single one can be forgotten.
     */
    static void skipOrFail(String reason) {
        if (isRequired()) {
            org.junit.jupiter.api.Assertions.fail(
                    "CN1_BACKEND_REQUIRED is set, so this must not be skipped: " + reason);
        }
        org.junit.jupiter.api.Assumptions.abort(reason);
    }

    /** The assume/abort pair, routed through skipOrFail. */
    static void require(boolean condition, String reason) {
        if (!condition) {
            skipOrFail(reason);
        }
    }

    static Path backendDir() {
        return Paths.get("..", "backend").normalize().toAbsolutePath();
    }

    /** Builds one demo into `binary`. Returns null on success, or the reason. */
    static String build(String mainClass, String demoDir, Path binary, Path jdk8) throws Exception {
        ProcessBuilder build = new ProcessBuilder("./build.sh", mainClass, "com.demo",
                binary.toString());
        build.directory(backendDir().toFile());
        build.environment().put("JDK_8_HOME", jdk8.toString());
        build.environment().put("JAVA_HOME", jdk8.toString());
        build.environment().put("CN1_BACKEND_DEMO", demoDir);
        build.redirectErrorStream(true);
        Process p = build.start();
        String log = readFully(p.getInputStream());
        boolean ok = p.waitFor(20, TimeUnit.MINUTES) && p.exitValue() == 0
                && Files.isExecutable(binary);
        if (ok) {
            return null;
        }
        return "could not build " + mainClass + ":\n"
                + (log.length() > 3000 ? log.substring(log.length() - 3000) : log);
    }

    static Process start(Path binary, Map<String, String> env, Path logFile) throws IOException {
        ProcessBuilder run = new ProcessBuilder(binary.toString());
        run.environment().putAll(env);
        run.redirectErrorStream(true);
        run.redirectOutput(logFile.toFile());
        return run.start();
    }

    static void stop(Process server) {
        if (server == null) {
            return;
        }
        server.destroy();
        try {
            if (!server.waitFor(10, TimeUnit.SECONDS)) {
                server.destroyForcibly();
            }
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }

    static int freePort() throws IOException {
        ServerSocket probe = new ServerSocket(0);
        try {
            return probe.getLocalPort();
        } finally {
            probe.close();
        }
    }

    static boolean waitForPort(int port, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
                return true;
            } catch (IOException err) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // closing a probe socket that never connected
                }
            }
        }
        return false;
    }

    /**
     * The JDBC jars on this test run's own classpath, as a path list.
     *
     * The local Java SE arm of the backend reaches SQLite through a driver, and
     * hunting for one in ~/.m2 makes a test depend on whatever some other build
     * happened to leave there. These are declared dependencies of this module, so
     * they are the same jars on every machine.
     */
    static String jdbcJars() {
        StringBuilder out = new StringBuilder();
        String separator = System.getProperty("path.separator", ":");
        String[] entries = System.getProperty("java.class.path", "").split(java.util.regex.Pattern.quote(separator));
        for (String entry : entries) {
            String name = new java.io.File(entry).getName();
            if (name.startsWith("sqlite-jdbc") || name.startsWith("slf4j-api")) {
                if (out.length() > 0) {
                    out.append(separator);
                }
                out.append(entry);
            }
        }
        return out.toString();
    }

    /**
     * Runs one of vm/backend's own scripts, with the environment those scripts
     * read already filled in. Returns the combined output; `exitCode` is written
     * into `status[0]` so a caller can tell a failed run from a quiet one.
     */
    static String runBackendScript(List<String> command, Map<String, String> env,
            long timeoutSeconds, int[] status) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(backendDir().toFile());
        pb.environment().putAll(env);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = readFully(p.getInputStream());
        if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            status[0] = -1;
            return out;
        }
        status[0] = p.exitValue();
        return out;
    }

    /** Whether a command exists on PATH, so a test can skip rather than fail. */
    static boolean hasCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            readFully(p.getInputStream());
            return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception err) {
            return false;
        }
    }

    /** Runs a command and returns its combined output, or null when it failed. */
    static String run(List<String> command, long timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = readFully(p.getInputStream());
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            return p.exitValue() == 0 ? out : null;
        } catch (Exception err) {
            return null;
        }
    }

    static String readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        try {
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException err) {
            // a read timeout means the peer said nothing more
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    static Path findJdk8() {
        String env = System.getenv("JDK_8_HOME");
        if (env != null && Files.isExecutable(Paths.get(env, "bin", "javac"))) {
            return Paths.get(env);
        }
        List<Path> roots = new ArrayList<Path>();
        String home = System.getProperty("user.home");
        roots.add(Paths.get("/Library/Java/JavaVirtualMachines"));
        roots.add(Paths.get(home, "Library", "Java", "JavaVirtualMachines"));
        roots.add(Paths.get("/usr/lib/jvm"));
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try {
                java.util.Iterator<Path> it = Files.list(root).iterator();
                while (it.hasNext()) {
                    Path entry = it.next();
                    String name = entry.getFileName().toString().toLowerCase();
                    if (name.indexOf("1.8") < 0 && name.indexOf("-8") < 0 && name.indexOf("jdk8") < 0) {
                        continue;
                    }
                    if (Files.isExecutable(entry.resolve("Contents/Home/bin/javac"))) {
                        return entry.resolve("Contents/Home");
                    }
                    if (Files.isExecutable(entry.resolve("bin/javac"))) {
                        return entry;
                    }
                }
            } catch (IOException err) {
                // unreadable directory; try the next root
            }
        }
        return null;
    }
}
