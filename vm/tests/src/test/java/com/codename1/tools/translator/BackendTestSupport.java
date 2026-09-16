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
        // PRINTED, because a skip is otherwise invisible. Surefire reports one as
        // "Tests run: 1, Skipped: 1" and keeps the reason in a report file nobody
        // opens, so these tests skipped in CI for as long as they have existed and
        // the job stayed green: a missing link library looked exactly like a
        // developer machine without a toolchain. The reason is what says which.
        System.out.println("SKIPPING a backend test: " + reason);
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
        boolean[] timedOut = new boolean[1];
        String log = awaitOutput(p, 20, TimeUnit.MINUTES, timedOut);
        boolean ok = !timedOut[0] && p.exitValue() == 0
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
        boolean[] timedOut = new boolean[1];
        String out = awaitOutput(p, timeoutSeconds, TimeUnit.SECONDS, timedOut);
        if (timedOut[0]) {
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
            boolean[] timedOut = new boolean[1];
            awaitOutput(p, 20, TimeUnit.SECONDS, timedOut);
            return !timedOut[0] && p.exitValue() == 0;
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
            boolean[] timedOut = new boolean[1];
            String out = awaitOutput(p, timeoutSeconds, TimeUnit.SECONDS, timedOut);
            if (timedOut[0]) {
                return null;
            }
            return p.exitValue() == 0 ? out : null;
        } catch (Exception err) {
            return null;
        }
    }

    /**
     * Waits for a process with a timeout WHILE DRAINING ITS OUTPUT, and returns
     * what it produced.
     *
     * <p>Reading the stream to EOF first and only then calling waitFor is the
     * shape this replaces, and its timeout is unreachable: EOF arrives when the
     * child closes its stdout, which for a hung child is never, so the read
     * blocks forever and the waitFor below it never runs. The backend has
     * processes that hang for real -- a wedged server loop, a collector waiting
     * on a mutator -- so the branch that exists to kill one and print a
     * diagnostic was exactly the branch that could not be reached, and the job
     * spent its whole workflow timeout instead of failing in minutes.
     *
     * <p>Whatever was produced before the kill is returned rather than
     * discarded: on the timeout path it is the only evidence of where the
     * process stopped.
     *
     * @param timedOut set to true when the process had to be killed
     */
    static String awaitOutput(Process p, long timeout, TimeUnit unit, boolean[] timedOut)
            throws Exception {
        final InputStream in = p.getInputStream();
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        Thread drain = new Thread(new Runnable() {
            public void run() {
                byte[] buffer = new byte[8192];
                int n;
                try {
                    while ((n = in.read(buffer)) > 0) {
                        synchronized (raw) {
                            raw.write(buffer, 0, n);
                        }
                    }
                } catch (IOException err) {
                    // The pipe closed because the process died, which is the
                    // normal end of this thread on the kill path.
                }
            }
        });
        // A daemon, so a drain that somehow outlives the kill cannot stop the
        // JVM from exiting and turn a test failure into a hung build.
        drain.setDaemon(true);
        drain.start();
        boolean exited = p.waitFor(timeout, unit);
        if (!exited) {
            p.destroyForcibly();
            // Killing the process is what closes the pipe and ends the drain.
            p.waitFor(30, TimeUnit.SECONDS);
        }
        drain.join(30000);
        timedOut[0] = !exited;
        synchronized (raw) {
            return new String(raw.toByteArray(), StandardCharsets.UTF_8);
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
